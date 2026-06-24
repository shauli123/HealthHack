package com.example.healthfake

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.text.input.KeyboardType
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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class MainActivity : ComponentActivity() {

    private var healthConnectClient: HealthConnectClient? = null

    private val healthPermissions = setOf(
        HealthPermission.getWritePermission(StepsRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class)
    )

    private val rawPermissionStrings: Set<String> =
        healthPermissions.map { it.permissionString }.toSet()

    private var onPermissionResult: ((String) -> Unit)? = null

    private val requestPermissionLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted: Set<String> ->
        val allGranted = granted.containsAll(rawPermissionStrings)
        onPermissionResult?.invoke(
            if (allGranted) "All permissions granted: $granted"
            else "Partial permissions — granted: $granted"
        )
    }

    private val openSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _: ActivityResult ->
        // Settings screen closed, no action needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            healthConnectClient = HealthConnectClient.getOrCreate(this)
        } catch (_: Exception) {
            healthConnectClient = null
        }

        setContent {
            HealthFakeApp()
        }
    }

    @Composable
    private fun HealthFakeApp() {
        var logText by remember { mutableStateOf("") }
        var customStepInput by remember { mutableStateOf("") }

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
                        onClick = { checkStatus(onResult = { appendLog(it) }) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Check Status")
                    }

                    Button(
                        onClick = { requestPermissions(onResult = { appendLog(it) }) },
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
                        onClick = { injectSteps(5000L, onResult = { appendLog(it) }) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF5722)
                        )
                    ) {
                        Text("Inject 5000 Steps")
                    }

                    Button(
                        onClick = { injectSteps(10000L, onResult = { appendLog(it) }) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF5722)
                        )
                    ) {
                        Text("Inject 10000 Steps")
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customStepInput,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 9) {
                                customStepInput = input
                            }
                        },
                        label = { Text("Custom steps") },
                        placeholder = { Text("e.g. 7500") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = {
                            val count = customStepInput.toLongOrNull()
                            if (count != null && count > 0) {
                                injectSteps(count, onResult = { appendLog(it) })
                            } else {
                                appendLog("ERROR: Enter a valid positive number")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF5722)
                        )
                    ) {
                        Text("Inject")
                    }
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = { openSettings(onResult = { appendLog(it) }) },
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

    private fun checkStatus(onResult: (String) -> Unit) {
        try {
            if (healthConnectClient != null) {
                onResult("SDK_AVAILABLE (client initialized)")
            } else {
                val intent = Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
                val canResolve = packageManager.resolveActivity(intent, 0) != null
                if (canResolve) {
                    onResult("SDK_INSTALLED but client init failed — try requesting permissions")
                } else {
                    onResult("SDK_UNAVAILABLE — install Health Connect from Google Play")
                }
            }
        } catch (e: Exception) {
            onResult("ERROR: ${e.message}\n${e.stackTraceToString()}")
        }
    }

    private fun requestPermissions(onResult: (String) -> Unit) {
        onPermissionResult = onResult
        try {
            requestPermissionLauncher.launch(rawPermissionStrings)
        } catch (e: Exception) {
            onResult("ERROR: ${e.message}\n${e.stackTraceToString()}")
        }
    }

    private fun injectSteps(count: Long, onResult: (String) -> Unit) {
        val client = healthConnectClient
        if (client == null) {
            onResult("ERROR: Health Connect not available. Tap 'Check Status' first.")
            return
        }

        val safeCount = count.coerceIn(1L, 100_000_000L)

        lifecycleScope.launch {
            try {
                val now = Instant.now()
                val startTime = now.minus(15, ChronoUnit.MINUTES)
                val endTime = now
                val zone = ZoneId.systemDefault()

                val record = StepsRecord(
                    count = safeCount,
                    startTime = startTime,
                    endTime = endTime,
                    startZoneOffset = zone.rules.getOffset(startTime),
                    endZoneOffset = zone.rules.getOffset(endTime)
                )

                client.insertRecords(listOf(record))
                onResult("Injected $safeCount steps successfully")
            } catch (e: Exception) {
                onResult("ERROR: ${e.message}\n${e.stackTraceToString()}")
            }
        }
    }

    private fun openSettings(onResult: (String) -> Unit) {
        try {
            val intent = Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
            openSettingsLauncher.launch(intent)
            onResult("Opened Health Connect settings")
        } catch (e: Exception) {
            onResult("ERROR: ${e.message}\n${e.stackTraceToString()}")
        }
    }
}
