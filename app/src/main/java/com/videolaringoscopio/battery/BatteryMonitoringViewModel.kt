package com.videolaringoscopio.battery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import java.net.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import android.util.Log

/**
 * ViewModel para monitoramento de bateria do videolaringoscópio
 * Implementa comunicação UDP para receber dados de bateria em tempo real
 */
class BatteryMonitoringViewModel(app: Application) : AndroidViewModel(app) {
    
    companion object {
        private const val TAG = "BatteryMonitoring"
        
        // Novos eventos UDP para bateria
        const val UDP_EVENT_BATTERY_STATUS = 20
        const val UDP_EVENT_BATTERY_LOW = 21
        const val UDP_EVENT_BATTERY_CRITICAL = 22
        const val UDP_EVENT_BATTERY_CHARGING = 23
        
        // Comandos para solicitar informações de bateria
        const val CMD_REQUEST_BATTERY = "GET_BATTERY"
        const val CMD_BATTERY_MONITORING_ON = "BATTERY_MON_ON"
        const val CMD_BATTERY_MONITORING_OFF = "BATTERY_MON_OFF"
        
        // Níveis de bateria
        const val BATTERY_LEVEL_CRITICAL = 10
        const val BATTERY_LEVEL_LOW = 20
        const val BATTERY_LEVEL_NORMAL = 50
    }
    
    // LiveData para observar o status da bateria
    private val _batteryLevel = MutableLiveData<Int>(0)
    val batteryLevel: LiveData<Int> get() = _batteryLevel
    
    private val _batteryVoltage = MutableLiveData<Float>(0f)
    val batteryVoltage: LiveData<Float> get() = _batteryVoltage
    
    private val _isCharging = MutableLiveData<Boolean>(false)
    val isCharging: LiveData<Boolean> get() = _isCharging
    
    private val _batteryTemperature = MutableLiveData<Float>(0f)
    val batteryTemperature: LiveData<Float> get() = _batteryTemperature
    
    private val _batteryStatus = MutableLiveData<BatteryStatus>(BatteryStatus.UNKNOWN)
    val batteryStatus: LiveData<BatteryStatus> get() = _batteryStatus
    
    private val _isConnected = MutableLiveData<Boolean>(false)
    val isConnected: LiveData<Boolean> get() = _isConnected
    
    // Configuração de rede
    private var udpSocket: DatagramSocket? = null
    private var droneAddress: InetAddress? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var batteryMonitoringJob: Job? = null
    
    /**
     * Enum para status da bateria
     */
    enum class BatteryStatus {
        UNKNOWN,
        NORMAL,
        LOW,
        CRITICAL,
        CHARGING,
        FULL,
        ERROR
    }
    
    /**
     * Data class para dados completos da bateria
     */
    data class BatteryData(
        val level: Int,           // Percentual (0-100)
        val voltage: Float,       // Voltagem em V
        val temperature: Float,   // Temperatura em °C
        val isCharging: Boolean,  // Status de carregamento
        val status: BatteryStatus,// Status geral
        val timeRemaining: Int    // Tempo restante em minutos
    )
    
    /**
     * Conecta ao videolaringoscópio e inicia monitoramento de bateria
     */
    fun connectAndStartBatteryMonitoring(ip: String, port: Int = 8888) {
        coroutineScope.launch {
            try {
                droneAddress = InetAddress.getByName(ip)
                udpSocket = DatagramSocket()
                _isConnected.postValue(true)
                
                // Ativar monitoramento de bateria no dispositivo
                sendBatteryCommand(CMD_BATTERY_MONITORING_ON)
                
                // Iniciar loop de monitoramento
                startBatteryMonitoring()
                
                Log.d(TAG, "Monitoramento de bateria iniciado para $ip:$port")
                
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao conectar: ${e.message}")
                _isConnected.postValue(false)
            }
        }
    }
    
    /**
     * Desconecta e para o monitoramento
     */
    fun disconnectBatteryMonitoring() {
        coroutineScope.launch {
            try {
                // Desativar monitoramento no dispositivo
                sendBatteryCommand(CMD_BATTERY_MONITORING_OFF)
                
                // Parar job de monitoramento
                batteryMonitoringJob?.cancel()
                
                // Fechar socket
                udpSocket?.close()
                _isConnected.postValue(false)
                
                Log.d(TAG, "Monitoramento de bateria desconectado")
                
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao desconectar: ${e.message}")
            }
        }
    }
    
    /**
     * Solicita informações de bateria manualmente
     */
    fun requestBatteryStatus() {
        coroutineScope.launch {
            sendBatteryCommand(CMD_REQUEST_BATTERY)
            Log.d(TAG, "Solicitação de status de bateria enviada")
        }
    }
    
    /**
     * Simula dados de bateria para testes
     */
    fun simulateBatteryData(level: Int = 75, voltage: Float = 12.6f, temperature: Float = 25.0f, charging: Boolean = false) {
        _batteryLevel.postValue(level)
        _batteryVoltage.postValue(voltage)
        _batteryTemperature.postValue(temperature)
        _isCharging.postValue(charging)
        _isConnected.postValue(true)
        
        val status = when {
            charging -> BatteryStatus.CHARGING
            level >= 95 -> BatteryStatus.FULL
            level <= BATTERY_LEVEL_CRITICAL -> BatteryStatus.CRITICAL
            level <= BATTERY_LEVEL_LOW -> BatteryStatus.LOW
            else -> BatteryStatus.NORMAL
        }
        _batteryStatus.postValue(status)
        
        Log.d(TAG, "Dados simulados: $level%, ${voltage}V, ${temperature}°C, Carregando: $charging")
    }
    
    /**
     * Inicia o loop de monitoramento de bateria
     */
    private fun startBatteryMonitoring() {
        batteryMonitoringJob = coroutineScope.launch {
            val buffer = ByteArray(1024)
            
            try {
                while (_isConnected.value == true && isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    udpSocket?.receive(packet)
                    
                    // Processar dados recebidos
                    processBatteryData(packet.data, packet.length)
                    
                    // Pequeno delay para não sobrecarregar
                    delay(100)
                }
            } catch (e: Exception) {
                if (isActive) {
                    Log.e(TAG, "Erro no monitoramento: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Processa os dados de bateria recebidos via UDP
     */
    private fun processBatteryData(data: ByteArray, length: Int) {
        try {
            if (length < 4) return
            
            val buffer = ByteBuffer.wrap(data, 0, length)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            
            // Primeiro byte: tipo de evento
            val eventType = buffer.get().toInt()
            
            when (eventType) {
                UDP_EVENT_BATTERY_STATUS -> {
                    if (length >= 16) {
                        val level = buffer.get().toInt() and 0xFF
                        val voltage = buffer.float
                        val temperature = buffer.float
                        val charging = buffer.get().toInt() == 1
                        
                        // Atualizar LiveData
                        _batteryLevel.postValue(level)
                        _batteryVoltage.postValue(voltage)
                        _batteryTemperature.postValue(temperature)
                        _isCharging.postValue(charging)
                        
                        // Determinar status
                        val status = when {
                            charging -> BatteryStatus.CHARGING
                            level >= 95 -> BatteryStatus.FULL
                            level <= BATTERY_LEVEL_CRITICAL -> BatteryStatus.CRITICAL
                            level <= BATTERY_LEVEL_LOW -> BatteryStatus.LOW
                            else -> BatteryStatus.NORMAL
                        }
                        _batteryStatus.postValue(status)
                        
                        Log.d(TAG, "Bateria: $level%, ${voltage}V, ${temperature}°C, Carregando: $charging")
                    }
                }
                
                UDP_EVENT_BATTERY_LOW -> {
                    _batteryStatus.postValue(BatteryStatus.LOW)
                    Log.w(TAG, "Alerta: Bateria baixa!")
                }
                
                UDP_EVENT_BATTERY_CRITICAL -> {
                    _batteryStatus.postValue(BatteryStatus.CRITICAL)
                    Log.e(TAG, "CRÍTICO: Bateria muito baixa!")
                }
                
                UDP_EVENT_BATTERY_CHARGING -> {
                    _isCharging.postValue(true)
                    _batteryStatus.postValue(BatteryStatus.CHARGING)
                    Log.i(TAG, "Bateria carregando")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao processar dados de bateria: ${e.message}")
        }
    }
    
    /**
     * Envia comando relacionado à bateria
     */
    private suspend fun sendBatteryCommand(command: String) {
        try {
            val data = command.toByteArray()
            val packet = DatagramPacket(data, data.size, droneAddress, 8888)
            udpSocket?.send(packet)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao enviar comando: ${e.message}")
        }
    }
    
    /**
     * Obtém dados completos da bateria
     */
    fun getCurrentBatteryData(): BatteryData {
        return BatteryData(
            level = _batteryLevel.value ?: 0,
            voltage = _batteryVoltage.value ?: 0f,
            temperature = _batteryTemperature.value ?: 0f,
            isCharging = _isCharging.value ?: false,
            status = _batteryStatus.value ?: BatteryStatus.UNKNOWN,
            timeRemaining = calculateTimeRemaining()
        )
    }
    
    /**
     * Calcula tempo restante estimado (em minutos)
     */
    private fun calculateTimeRemaining(): Int {
        val level = _batteryLevel.value ?: 0
        val isCharging = _isCharging.value ?: false
        
        return when {
            isCharging -> -1 // Carregando
            level <= 0 -> 0
            level <= 10 -> 15  // ~15 min
            level <= 20 -> 45  // ~45 min
            level <= 50 -> 120 // ~2 horas
            else -> 240        // ~4 horas
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        disconnectBatteryMonitoring()
    }
}

