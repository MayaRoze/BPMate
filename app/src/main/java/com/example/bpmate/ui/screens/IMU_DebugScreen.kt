package com.example.bpmate.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bpmate.R
import com.example.bpmate.ui.theme.TranslucentDarkCyan
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun IMU_DebugScreen(
    onBack: () -> Unit = {},
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

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background_home),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("IMU Debug", fontWeight = FontWeight.Bold, color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = TranslucentDarkCyan.copy(alpha = 0.6f))
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Surface(
                    color = TranslucentDarkCyan.copy(alpha = 0.6f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Connection Status: $connectionStatus", color = Color.White)
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
                            Text(
                                "Bluetooth permissions are required to scan for devices.",
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    color = TranslucentDarkCyan.copy(alpha = 0.6f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Discovered Devices:", color = Color.White, fontWeight = FontWeight.Bold)
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(discoveredDevices) { device ->
                                DiscoveredDeviceItem(device, onItemClick = { address ->
                                    bluetoothViewModel.connectToDevice(address)
                                })
                                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("IMU Data:", color = Color.White, fontWeight = FontWeight.Bold)
                        Column {
                            Text("Timestamp: ${imuData.timestamp}", color = Color.White.copy(alpha = 0.8f))
                            Text("Accel X: ${imuData.accelX}", color = Color.White.copy(alpha = 0.8f))
                            Text("Accel Y: ${imuData.accelY}", color = Color.White.copy(alpha = 0.8f))
                            Text("Accel Z: ${imuData.accelZ}", color = Color.White.copy(alpha = 0.8f))
                            Text("Gyro X: ${imuData.gyroX}", color = Color.White.copy(alpha = 0.8f))
                            Text("Gyro Y: ${imuData.gyroY}", color = Color.White.copy(alpha = 0.8f))
                            Text("Gyro Z: ${imuData.gyroZ}", color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun DiscoveredDeviceItem(device: BluetoothDevice, onItemClick: (String) -> Unit) {
    ListItem(
        headlineContent = { Text(device.name ?: "Unknown Device", color = Color.White) },
        supportingContent = { Text(device.address, color = Color.White.copy(alpha = 0.7f)) },
        modifier = Modifier.clickable { onItemClick(device.address) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
