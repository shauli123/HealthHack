package com.example.healthfake

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.sdk.SdkStatus
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class MainActivity : ComponentActivity() {

    private lateinit var healthConnectClient: HealthConnectClient

    private val permissionContract =
        PermissionController.createRequestPermissionResultContract()

    private val permissions = setOf(
        HealthPermission.WRITE_STEPS,
        HealthPermission.READ_STEPS
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        healthConnectClient = HealthConnectClient.getOrCreate(this)

        setContent {
            HealthFakeApp()
        }
    }

    @Composable
    private fun HealthFakeApp() {
        var logText by remember { mutableStateOf("") }

        fun appendLog(msg: String) {
            logText = logText + msg + "\n"
        }

        MaterialTheme(
            colorScheme = MaterialTheme.colorScheme.copy(
                background = Color(0xFF121212),
                surface = Color(0xFF1E1E1E),
                primary = Color(0xFF4CAF50),
                onPrimary = Color.White,
                onBackground = Color.White,
                onSurface = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "HealthFake",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            try {
                                val status = checkHealthConnectStatus()
                                appendLog("Status: $status")
                            } catch (e: Exception) {
                                appendLog("ERROR: ${e.message}\n${e.stackTraceToString()}")
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Check Status")
                    }

                    Button(
                        onClick = {
                            try {
                                requestPermissions()
                                appendLog("Permission request launched")
                            } catch (e: Exception) {
                                appendLog("ERROR: ${e.message}\n${e.stackTraceToString()}")
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3)
                        )
                    ) {
                        Text("Request Perms")
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            try {
                                injectSteps(5000L)
                                appendLog("Injected 5000 steps")
                            } catch (e: Exception) {
                                appendLog("ERROR: ${e.message}\n${e.stackTraceToString()}")
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF5722)
                        )
                    ) {
                        Text("Inject 5000 Steps")
                    }

                    Button(
                        onClick = {
                            try {
                                injectSteps(10000L)
                                appendLog("Injected 10000 steps")
                            } catch (e: Exception) {
                                appendLog("ERROR: ${e.message}\n${e.stackTraceToString()}")
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF5722)
                        )
                    ) {
                        Text("Inject 10000 Steps")
                    }
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        try {
                            openSettings()
                            appendLog("Opened Health Connect settings")
                        } catch (e: Exception) {
                            appendLog("ERROR: ${e.message}\n${e.stackTraceToString()}")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF9C27B0)
                    )
                ) {
                    Text("Open Health Connect Settings")
                }

                Spacer(Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(
                            Color(0xFF0D0D0D),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                ) {
                    val scrollState = rememberScrollState()
                    Text(
                        text = logText.ifEmpty { "Log output will appear here..." },
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState),
                        color = Color(0xFF00FF00),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }

    private fun checkHealthConnectStatus(): String {
        return when (healthConnectClient.sdkStatus) {
            SdkStatus.SDK_AVAILABLE -> "SDK_AVAILABLE"
            SdkStatus.SDK_UNAVAILABLE -> "SDK_UNAVAILABLE"
            SdkStatus.SDK_INSTALLED -> "SDK_INSTALLED (update required)"
            else -> "UNKNOWN"
        }
    }

    private fun requestPermissions() {
        permissionContract.createIntent(this, permissions).let { intent ->
            startActivity(intent)
        }
    }

    private fun injectSteps(count: Long) {
        val now = Instant.now()
        val startTime = now.minus(15, ChronoUnit.MINUTES)
        val endTime = now
        val zone = ZoneId.systemDefault()

        val record = StepsRecord(
            count = count,
            startTime = startTime,
            endTime = endTime,
            startZoneOffset = zone.rules.getOffset(startTime),
            endZoneOffset = zone.rules.getOffset(endTime)
        )

        healthConnectClient.insertRecords(listOf(record))
    }

    private fun openSettings() {
        val intent = Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }
}
