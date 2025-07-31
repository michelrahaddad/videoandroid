@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.droneapp

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.videolaringoscopio.battery.BatteryMonitoringViewModel
import com.videolaringoscopio.ui.components.*

class MainComposeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val composeView = ComposeView(this)
        composeView.setContent {
            DroneControlScreen()
        }
        setContentView(composeView)
    }
}

@Composable
fun DroneControlScreen() {
    var connected by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }
    
    // ✅ NOVO: ViewModel de bateria
    val batteryViewModel: BatteryMonitoringViewModel = viewModel()
    
    // ✅ NOVO: Inicializar dados simulados para demonstração
    LaunchedEffect(Unit) {
        batteryViewModel.simulateBatteryData(level = 75, voltage = 12.6f, temperature = 25.0f, charging = false)
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // ✅ NOVO: Header com indicador de bateria
        TopAppBar(
            title = { Text("Videolaringoscópio") },
            actions = {
                BatteryHeaderIndicator(viewModel = batteryViewModel)
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF1E2A3A),
                titleContentColor = Color.White
            )
        )
        
        // ✅ NOVO: Alertas flutuantes de bateria
        BatteryFloatingAlert(viewModel = batteryViewModel)
        
        // Conteúdo principal existente
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Controles existentes em layout de grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Coluna esquerda - Controles existentes
                Column(
                    modifier = Modifier.weight(2f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = {
                            connected = true
                            // ✅ NOVO: Conectar monitoramento de bateria
                            batteryViewModel.connectAndStartBatteryMonitoring("192.168.4.1")
                            Log.d("Compose", "Conectado ao videolaringoscópio")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (connected) "Conectado" else "Conectar")
                    }

                    Button(
                        onClick = {
                            recording = !recording
                            Log.d("Compose", if (recording) "Gravando..." else "Gravação parada.")
                        },
                        enabled = connected,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (recording) "Parar Gravação" else "Iniciar Gravação")
                    }
                    
                    // ✅ NOVO: Botões de teste de bateria
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                batteryViewModel.simulateBatteryData(level = 15, charging = false)
                                Log.d("Compose", "Simulando bateria baixa")
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                        ) {
                            Text("Teste Baixa", color = Color.White)
                        }
                        
                        Button(
                            onClick = {
                                batteryViewModel.simulateBatteryData(level = 5, charging = false)
                                Log.d("Compose", "Simulando bateria crítica")
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744))
                        ) {
                            Text("Teste Crítica", color = Color.White)
                        }
                    }
                    
                    Button(
                        onClick = {
                            batteryViewModel.simulateBatteryData(level = 45, charging = true)
                            Log.d("Compose", "Simulando carregamento")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("Teste Carregando", color = Color.White)
                    }
                }
                
                // ✅ NOVO: Coluna direita - Card de bateria
                BatteryStatusCard(
                    viewModel = batteryViewModel,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // ✅ NOVO: Barra de progresso no rodapé
        BatteryProgressBar(
            viewModel = batteryViewModel,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
    
    // ✅ NOVO: Notificações toast
    BatteryToastNotification(viewModel = batteryViewModel)
}