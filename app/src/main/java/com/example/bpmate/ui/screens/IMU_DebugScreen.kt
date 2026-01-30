package com.example.bpmate.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun IMU_DebugScreen(
    bluetoothViewModel: BluetoothViewModel = viewModel()
) {
    val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
        )
    } else {
        listOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
    val bluetoothPermissionsState = rememberMultiplePermissionsState(bluetoothPermissions)

    val discoveredDevices by bluetoothViewModel.discoveredDevices.collectAsState()
    val connectionStatus by bluetoothViewModel.connectionStatus.collectAsState()
    val imuData by bluetoothViewModel.imuData.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Connection Status: $connectionStatus")
        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    if (bluetoothPermissionsState.allPermissionsGranted) {
                        bluetoothViewModel.startScan()
                    } else {
                        bluetoothPermissionsState.launchMultiplePermissionRequest()
                    }
                },
            ) {
                Text(if (bluetoothPermissionsState.allPermissionsGranted) "Scan" else "Request Permissions")
            }
            Button(onClick = { bluetoothViewModel.sendCommand("START") }) {
                Text("START")
            }
            Button(onClick = { bluetoothViewModel.sendCommand("STOP") }) {
                Text("STOP")
            }
        }
        if (!bluetoothPermissionsState.allPermissionsGranted) {
            Text("Bluetooth permissions are required to scan for devices.")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Discovered Devices:")
        LazyColumn(modifier = Modifier.height(200.dp)) {
            items(discoveredDevices) { device ->
                DiscoveredDeviceItem(device, onItemClick = { address ->
                    bluetoothViewModel.connectToDevice(address)
                })
                HorizontalDivider()
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("IMU Data:")
        Text("Timestamp: ${imuData.timestamp}")
        Text("Accel X: ${imuData.accelX}")
        Text("Accel Y: ${imuData.accelY}")
        Text("Accel Z: ${imuData.accelZ}")
        Text("Gyro X: ${imuData.gyroX}")
        Text("Gyro Y: ${imuData.gyroY}")
        Text("Gyro Z: ${imuData.gyroZ}")
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun DiscoveredDeviceItem(device: BluetoothDevice, onItemClick: (String) -> Unit) {
    ListItem(
        headlineContent = { Text(device.name ?: "Unknown Device") },
        supportingContent = { Text(device.address) },
        modifier = Modifier.clickable { onItemClick(device.address) }
    )
}
