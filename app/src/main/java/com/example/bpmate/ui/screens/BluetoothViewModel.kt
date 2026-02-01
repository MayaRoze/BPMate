package com.example.bpmate.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpmate.data.IMU_Data
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlin.math.sqrt

@SuppressLint("MissingPermission")
class BluetoothViewModel(application: Application) : AndroidViewModel(application) {

    private val bluetoothManager: BluetoothManager = application.getSystemService(BluetoothManager::class.java)
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices

    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus

    private val _imuData = MutableStateFlow(IMU_Data())
    val imuData: StateFlow<IMU_Data> = _imuData

    private val _cadence = MutableStateFlow(0f)
    val cadence: StateFlow<Float> = _cadence

    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    // Step detection variables
    private var lastMagnitude = 0f
    private var lastStepTime = 0L
    private val stepTimestamps = mutableListOf<Long>()
    private val CADENCE_WINDOW_MS = 10000L // 10 seconds window for cadence calculation
    private val STEP_THRESHOLD = 13.5f // Adjusted threshold for peak detection
    private val STEP_COOLDOWN_MS = 250L // Minimum time between steps (~240 steps/min max)

    init {
        // Periodic cadence update to ensure it drops to 0 when movement stops
        viewModelScope.launch(Dispatchers.Main) {
            while (true) {
                delay(1000)
                updateCadence(System.currentTimeMillis())
            }
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    device?.let { newDevice ->
                        if (_discoveredDevices.value.none { it.address == newDevice.address }) {
                            _discoveredDevices.value = _discoveredDevices.value + newDevice
                        }
                    }
                }
            }
        }
    }

    fun startScan() {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val hasPermissions = requiredPermissions.all {
            ActivityCompat.checkSelfPermission(getApplication(), it) == PackageManager.PERMISSION_GRANTED
        }

        if (!hasPermissions) {
            Log.e("BluetoothViewModel", "Required permissions for scanning are not granted.")
            return
        }

        if (bluetoothAdapter?.isEnabled == true) {
            _discoveredDevices.value = emptyList()
            val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getApplication<Application>().registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                getApplication<Application>().registerReceiver(receiver, filter)
            }

            val started = bluetoothAdapter.startDiscovery()
            Log.d("BluetoothViewModel", "Discovery started: $started")
        }
    }
    fun getPairedDevices() {
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        _discoveredDevices.value = pairedDevices?.toList() ?: emptyList()
    }

    fun connectToDevice(deviceAddress: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                listOf(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                listOf(Manifest.permission.BLUETOOTH)
            }

            val hasPermissions = requiredPermissions.all {
                ActivityCompat.checkSelfPermission(getApplication(), it) == PackageManager.PERMISSION_GRANTED
            }

            if (!hasPermissions) {
                Log.e("BluetoothViewModel", "Required permissions for connecting are not granted.")
                _connectionStatus.value = "Permission Denied"
                return@launch
            }

            _connectionStatus.value = "Connecting..."
            val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)

            try {
                val sppUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                bluetoothSocket = device?.createInsecureRfcommSocketToServiceRecord(sppUuid)
                bluetoothAdapter?.cancelDiscovery()
                bluetoothSocket?.connect()

                inputStream = bluetoothSocket?.inputStream
                outputStream = bluetoothSocket?.outputStream

                _connectionStatus.value = "Connected"
                Log.d("BT_DEBUG", "Successfully connected to ${device?.address}")

                sendCommand("START")
                startDataStream()
            } catch (e: IOException) {
                Log.e("BT_DEBUG", "Connection failed: ${e.message}")
                _connectionStatus.value = "Disconnected"
            }
        }
    }

    private fun startDataStream() {
        viewModelScope.launch(Dispatchers.IO) {
            val reader = inputStream?.bufferedReader()
            while (true) {
                try {
                    val line = reader?.readLine() ?: break
                    if (line.isNotEmpty()) {
                        parseAndDisplayData(line)
                    }
                } catch (e: IOException) {
                    Log.e("BluetoothViewModel", "Stream error: ${e.message}")
                    _connectionStatus.value = "Disconnected"
                    break
                }
            }
        }
    }

    private suspend fun parseAndDisplayData(line: String) {
        val cleanLine = line.trim()
        val values = cleanLine.split(",")

        if (values.size == 7) {
            try {
                val data = IMU_Data(
                    timestamp = values[0].toFloatOrNull() ?: 0f,
                    accelX = values[1].toFloatOrNull() ?: 0f,
                    accelY = values[2].toFloatOrNull() ?: 0f,
                    accelZ = values[3].toFloatOrNull() ?: 0f,
                    gyroX = values[4].toFloatOrNull() ?: 0f,
                    gyroY = values[5].toFloatOrNull() ?: 0f,
                    gyroZ = values[6].toFloatOrNull() ?: 0f
                )

                withContext(Dispatchers.Main) {
                    _imuData.value = data
                    detectStep(data)
                }
            } catch (e: Exception) {
                Log.e("BT_DATA", "Parsing error: ${e.message}")
            }
        }
    }

    private fun detectStep(data: IMU_Data) {
        val magnitude = sqrt(data.accelX * data.accelX + data.accelY * data.accelY + data.accelZ * data.accelZ)
        val currentTime = System.currentTimeMillis()

        // Simple peak detection: if magnitude cross threshold and then starts decreasing
        if (magnitude > STEP_THRESHOLD && magnitude < lastMagnitude && (currentTime - lastStepTime) > STEP_COOLDOWN_MS) {
            // We detected a peak
            lastStepTime = currentTime
            stepTimestamps.add(currentTime)
            updateCadence(currentTime)
        }
        lastMagnitude = magnitude
    }

    private fun updateCadence(currentTime: Long) {
        // Remove old steps outside the window
        stepTimestamps.removeAll { it < currentTime - CADENCE_WINDOW_MS }

        if (stepTimestamps.size < 2) {
            _cadence.value = 0f
            return
        }

        // Calculate steps per minute based on the window
        // (number of steps / window duration in minutes)
        // However, it's better to use the time between first and last step in window for better accuracy if many steps
        val durationMs = stepTimestamps.last() - stepTimestamps.first()
        if (durationMs > 0) {
            val stepsCount = stepTimestamps.size - 1
            val cadenceValue = (stepsCount.toFloat() / (durationMs.toFloat() / 60000f))
            _cadence.value = cadenceValue
        } else {
            _cadence.value = 0f
        }
    }

    fun sendCommand(command: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                outputStream?.write(command.toByteArray())
            } catch (e: IOException) {
                Log.e("BluetoothViewModel", "OutputStream error", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            bluetoothAdapter?.cancelDiscovery()
            getApplication<Application>().unregisterReceiver(receiver)
            inputStream?.close()
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: Exception) {
            Log.e("BluetoothViewModel", "Error on cleared", e)
        }
    }
}
