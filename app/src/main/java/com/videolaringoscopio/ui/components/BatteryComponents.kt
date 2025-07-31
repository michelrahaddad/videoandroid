package com.videolaringoscopio.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.videolaringoscopio.battery.BatteryMonitoringViewModel

/**
 * COMPONENTES INTEGRADOS DE BATERIA
 * Para usar nas telas EXISTENTES sem criar novas telas
 */

/**
 * 1. INDICADOR DE BATERIA NO HEADER
 * Mostra nível de bateria discretamente no topo da tela
 */
@Composable
fun BatteryHeaderIndicator(
    viewModel: BatteryMonitoringViewModel,
    modifier: Modifier = Modifier
) {
    val batteryLevel by viewModel.batteryLevel.observeAsState(0)
    val isCharging by viewModel.isCharging.observeAsState(false)
    val batteryStatus by viewModel.batteryStatus.observeAsState(BatteryMonitoringViewModel.BatteryStatus.UNKNOWN)
    val isConnected by viewModel.isConnected.observeAsState(false)

    if (isConnected) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Ícone de bateria com cor dinâmica
            Icon(
                imageVector = when {
                    isCharging -> Icons.Default.Bolt
                    batteryLevel > 80 -> Icons.Default.BatteryFull
                    else -> Icons.Default.Warning
                },
                contentDescription = "Bateria",
                tint = when {
                    isCharging -> Color(0xFF4CAF50)
                    batteryLevel <= 10 -> Color(0xFFFF1744)
                    batteryLevel <= 20 -> Color(0xFFFF9800)
                    else -> Color(0xFF2196F3)
                },
                modifier = Modifier.size(20.dp)
            )
            
            // Percentual
            Text(
                text = "$batteryLevel%",
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    isCharging -> Color(0xFF4CAF50)
                    batteryLevel <= 10 -> Color(0xFFFF1744)
                    batteryLevel <= 20 -> Color(0xFFFF9800)
                    else -> Color.White
                },
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * 2. ALERTA FLUTUANTE DE BATERIA BAIXA
 * Aparece discretamente quando bateria está baixa
 */
@Composable
fun BatteryFloatingAlert(
    viewModel: BatteryMonitoringViewModel,
    modifier: Modifier = Modifier
) {
    val batteryLevel by viewModel.batteryLevel.observeAsState(0)
    val batteryStatus by viewModel.batteryStatus.observeAsState(BatteryMonitoringViewModel.BatteryStatus.UNKNOWN)
    val isConnected by viewModel.isConnected.observeAsState(false)

    // Mostrar alerta apenas se conectado e bateria baixa/crítica
    if (isConnected && (batteryStatus == BatteryMonitoringViewModel.BatteryStatus.LOW || 
                       batteryStatus == BatteryMonitoringViewModel.BatteryStatus.CRITICAL)) {
        
        val backgroundColor = if (batteryStatus == BatteryMonitoringViewModel.BatteryStatus.CRITICAL) 
            Color(0xFFFF1744) else Color(0xFFFF9800)
        
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor.copy(alpha = 0.9f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (batteryStatus == BatteryMonitoringViewModel.BatteryStatus.CRITICAL) 
                        Icons.Default.Warning else Icons.Default.Warning,
                    contentDescription = "Alerta de bateria",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                
                Text(
                    text = if (batteryStatus == BatteryMonitoringViewModel.BatteryStatus.CRITICAL)
                        "CRÍTICO: Bateria muito baixa ($batteryLevel%)" 
                        else "Bateria baixa ($batteryLevel%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * 3. CARD DE STATUS DE BATERIA INTEGRADO
 * Para adicionar na tela principal junto com outros controles
 */
@Composable
fun BatteryStatusCard(
    viewModel: BatteryMonitoringViewModel,
    modifier: Modifier = Modifier
) {
    val batteryLevel by viewModel.batteryLevel.observeAsState(0)
    val batteryVoltage by viewModel.batteryVoltage.observeAsState(0f)
    val isCharging by viewModel.isCharging.observeAsState(false)
    val isConnected by viewModel.isConnected.observeAsState(false)

    if (isConnected) {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E2A3A)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header do card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bateria",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (isCharging) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Carregando",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                // Nível principal
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "$batteryLevel",
                        style = MaterialTheme.typography.headlineMedium,
                        color = when {
                            isCharging -> Color(0xFF4CAF50)
                            batteryLevel <= 10 -> Color(0xFFFF1744)
                            batteryLevel <= 20 -> Color(0xFFFF9800)
                            else -> Color.White
                        },
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "%",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
                
                // Voltagem (se disponível)
                if (batteryVoltage > 0) {
                    Text(
                        text = "${String.format("%.1f", batteryVoltage)}V",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

/**
 * 4. BARRA DE PROGRESSO DE BATERIA
 * Para usar como indicador linear discreto
 */
@Composable
fun BatteryProgressBar(
    viewModel: BatteryMonitoringViewModel,
    modifier: Modifier = Modifier,
    height: Int = 4
) {
    val batteryLevel by viewModel.batteryLevel.observeAsState(0)
    val isCharging by viewModel.isCharging.observeAsState(false)
    val isConnected by viewModel.isConnected.observeAsState(false)

    if (isConnected) {
        val animatedProgress by animateFloatAsState(
            targetValue = batteryLevel / 100f,
            animationSpec = tween(durationMillis = 500),
            label = "battery_progress"
        )
        
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height.dp)
                .clip(RoundedCornerShape(height.dp / 2))
                .background(Color.Gray.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(RoundedCornerShape(height.dp / 2))
                    .background(
                        when {
                            isCharging -> Color(0xFF4CAF50)
                            batteryLevel <= 10 -> Color(0xFFFF1744)
                            batteryLevel <= 20 -> Color(0xFFFF9800)
                            else -> Color(0xFF2196F3)
                        }
                    )
            )
        }
    }
}

/**
 * 5. MINI INDICADOR DE BATERIA
 * Para usar em qualquer lugar da interface
 */
@Composable
fun BatteryMiniIndicator(
    viewModel: BatteryMonitoringViewModel,
    showPercentage: Boolean = true,
    size: Int = 16
) {
    val batteryLevel by viewModel.batteryLevel.observeAsState(0)
    val isCharging by viewModel.isCharging.observeAsState(false)
    val isConnected by viewModel.isConnected.observeAsState(false)

    if (isConnected) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isCharging) Icons.Default.Bolt 
                             else Icons.Default.BatteryFull,
                contentDescription = "Bateria",
                tint = when {
                    isCharging -> Color(0xFF4CAF50)
                    batteryLevel <= 10 -> Color(0xFFFF1744)
                    batteryLevel <= 20 -> Color(0xFFFF9800)
                    else -> Color(0xFF2196F3)
                },
                modifier = Modifier.size(size.dp)
            )
            
            if (showPercentage) {
                Text(
                    text = "$batteryLevel%",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontSize = (size - 4).sp
                )
            }
        }
    }
}

/**
 * 6. NOTIFICAÇÃO TOAST DE BATERIA
 * Para alertas temporários
 */
@Composable
fun BatteryToastNotification(
    viewModel: BatteryMonitoringViewModel = viewModel()
) {
    val batteryStatus by viewModel.batteryStatus.observeAsState(BatteryMonitoringViewModel.BatteryStatus.UNKNOWN)
    val batteryLevel by viewModel.batteryLevel.observeAsState(0)
    
    // Estado para controlar visibilidade do toast
    var showToast by remember { mutableStateOf(false) }
    var lastStatus by remember { mutableStateOf(BatteryMonitoringViewModel.BatteryStatus.UNKNOWN) }
    
    // Detectar mudanças críticas de status
    LaunchedEffect(batteryStatus) {
        if (batteryStatus != lastStatus) {
            when (batteryStatus) {
                BatteryMonitoringViewModel.BatteryStatus.CRITICAL,
                BatteryMonitoringViewModel.BatteryStatus.LOW -> {
                    showToast = true
                }
                else -> { /* Não mostrar toast para outros status */ }
            }
            lastStatus = batteryStatus
        }
    }
    
    // Auto-hide toast após 3 segundos
    LaunchedEffect(showToast) {
        if (showToast) {
            kotlinx.coroutines.delay(3000)
            showToast = false
        }
    }
    
    if (showToast) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when (batteryStatus) {
                        BatteryMonitoringViewModel.BatteryStatus.CRITICAL -> Color(0xFFFF1744)
                        else -> Color(0xFFFF9800)
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Alerta",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Column {
                        Text(
                            text = if (batteryStatus == BatteryMonitoringViewModel.BatteryStatus.CRITICAL)
                                "Bateria Crítica!" else "Bateria Baixa!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Nível atual: $batteryLevel%",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}

