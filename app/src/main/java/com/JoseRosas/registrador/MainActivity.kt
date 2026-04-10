package com.JoseRosas.registrador

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.JoseRosas.registrador.service.BridgeForegroundService
import com.JoseRosas.registrador.ui.theme.RegistradorTheme
import com.JoseRosas.registrador.util.AppLogger
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import com.JoseRosas.registrador.network.AppConfig
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }

        val serviceIntent = Intent(this, BridgeForegroundService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)

        setContent {
            RegistradorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        onStopApp = {
                            stopService(Intent(this, BridgeForegroundService::class.java))
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(onStopApp: () -> Unit) {
    val context = LocalContext.current
    val logs by AppLogger.logs.collectAsState()

    var showExitDialog by remember { mutableStateOf(false) }
    var showConfigDialog by remember { mutableStateOf(false) }

    var currentIp by remember { mutableStateOf(AppConfig.getServerIp(context)) }
    var currentPort by remember { mutableStateOf(AppConfig.getServerPort(context).toString()) }

    BackHandler {
        showExitDialog = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Registrador activo",
                style = MaterialTheme.typography.headlineSmall
            )

            Button(onClick = {
                currentIp = AppConfig.getServerIp(context)
                currentPort = AppConfig.getServerPort(context).toString()
                showConfigDialog = true
            }) {
                Text("Configurar")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Eventos recientes",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { AppLogger.clear() }) {
                Text("Limpiar logs")
            }

            OutlinedButton(onClick = { showExitDialog = true }) {
                Text("Cerrar app")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        Text("IP actual: ${AppConfig.getServerIp(context)}")
        Text("Puerto actual: ${AppConfig.getServerPort(context)}")

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(logs.reversed()) { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }

    if (showConfigDialog) {
        AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            title = { Text("Configurar servidor") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = currentIp,
                        onValueChange = { currentIp = it },
                        label = { Text("IP de la PC") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = currentPort,
                        onValueChange = { currentPort = it },
                        label = { Text("Puerto") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Text("Después de guardar, cierra y vuelve a abrir la app para aplicar los cambios.")
                }
            },
            confirmButton = {
                Button(onClick = {
                    val portInt = currentPort.toIntOrNull() ?: 9000
                    AppConfig.setServerIp(context, currentIp.trim())
                    AppConfig.setServerPort(context, portInt)
                    showConfigDialog = false
                    AppLogger.d("Configuración guardada: ${currentIp.trim()}:$portInt")
                }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfigDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Cerrar aplicación") },
            text = { Text("¿Estás seguro de cerrar la app? El bridge dejará de estar activo.") },
            confirmButton = {
                Button(onClick = {
                    showExitDialog = false
                    onStopApp()
                }) {
                    Text("Sí, cerrar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showExitDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}