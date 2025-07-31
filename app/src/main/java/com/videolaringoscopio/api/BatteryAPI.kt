package com.videolaringoscopio.api

import android.util.Log
import kotlinx.coroutines.*
import java.net.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * API para comunicação de bateria com o videolaringoscópio
 * Gerencia rotas e comandos UDP relacionados à bateria
 */
class BatteryAPI {
    
    companion object {
        private const val TAG = "BatteryAPI"
        
        // Rotas/Comandos da API de Bateria
        const val ROUTE_GET_BATTERY = "GET_BATTERY"
        const val ROUTE_BATTERY_MONITOR_ON = "BATTERY_MON_ON"
        const val ROUTE_BATTERY_MONITOR_OFF = "BATTERY_MON_OFF"
        const val ROUTE_BATTERY_RESET = "BATTERY_RESET"
        const val ROUTE_BATTERY_CALIBRATE = "BATTERY_CAL"
        
        // Endpoints UDP
        const val DEFAULT_IP = "192.168.4.1"
        const val DEFAULT_PORT = 8888
        const val BATTERY_PORT = 8889  // Porta dedicada para bateria (opcional)
        
        // Timeouts
        const val CONNECTION_TIMEOUT = 5000
        const val READ_TIMEOUT = 3000
    }
    
    private var udpSocket: DatagramSocket? = null
    private var deviceAddress: InetAddress? = null
    private var isConnected = false
    
    /**
     * Conecta à API de bateria do videolaringoscópio
     */
    suspend fun connect(ip: String = DEFAULT_IP, port: Int = DEFAULT_PORT): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                deviceAddress = InetAddress.getByName(ip)
                udpSocket = DatagramSocket()
                udpSocket?.soTimeout = READ_TIMEOUT
                
                // Testar conexão enviando comando de status
                val testResult = sendCommand(ROUTE_GET_BATTERY)
                isConnected = testResult
                
                Log.d(TAG, "Conexão com API de bateria: ${if (isConnected) "SUCESSO" else "FALHA"}")
                isConnected
                
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao conectar API de bateria: ${e.message}")
                isConnected = false
                false
            }
        }
    }
    
    /**
     * Desconecta da API de bateria
     */
    fun disconnect() {
        try {
            if (isConnected) {
                // Desativar monitoramento antes de desconectar
                runBlocking {
                    sendCommand(ROUTE_BATTERY_MONITOR_OFF)
                }
            }
            
            udpSocket?.close()
            isConnected = false
            Log.d(TAG, "API de bateria desconectada")
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao desconectar API de bateria: ${e.message}")
        }
    }
    
    /**
     * Envia comando para a API de bateria
     */
    suspend fun sendCommand(command: String, parameters: Map<String, Any>? = null): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!isConnected || udpSocket == null || deviceAddress == null) {
                    Log.w(TAG, "API não conectada")
                    return@withContext false
                }
                
                // Construir payload do comando
                val payload = buildCommandPayload(command, parameters)
                val data = payload.toByteArray()
                
                // Enviar comando UDP
                val packet = DatagramPacket(data, data.size, deviceAddress, DEFAULT_PORT)
                udpSocket?.send(packet)
                
                Log.d(TAG, "Comando enviado: $command")
                true
                
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao enviar comando $command: ${e.message}")
                false
            }
        }
    }
    
    /**
     * Solicita status atual da bateria
     */
    suspend fun getBatteryStatus(): BatteryStatusResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val success = sendCommand(ROUTE_GET_BATTERY)
                if (!success) return@withContext null
                
                // Aguardar resposta
                val buffer = ByteArray(1024)
                val packet = DatagramPacket(buffer, buffer.size)
                udpSocket?.receive(packet)
                
                // Processar resposta
                parseBatteryStatusResponse(packet.data, packet.length)
                
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao obter status da bateria: ${e.message}")
                null
            }
        }
    }
    
    /**
     * Ativa monitoramento contínuo de bateria
     */
    suspend fun startBatteryMonitoring(intervalSeconds: Int = 5): Boolean {
        val parameters = mapOf("interval" to intervalSeconds)
        return sendCommand(ROUTE_BATTERY_MONITOR_ON, parameters)
    }
    
    /**
     * Desativa monitoramento de bateria
     */
    suspend fun stopBatteryMonitoring(): Boolean {
        return sendCommand(ROUTE_BATTERY_MONITOR_OFF)
    }
    
    /**
     * Reset do sistema de bateria
     */
    suspend fun resetBatterySystem(): Boolean {
        return sendCommand(ROUTE_BATTERY_RESET)
    }
    
    /**
     * Calibra sensor de bateria
     */
    suspend fun calibrateBattery(): Boolean {
        return sendCommand(ROUTE_BATTERY_CALIBRATE)
    }
    
    /**
     * Constrói payload do comando
     */
    private fun buildCommandPayload(command: String, parameters: Map<String, Any>?): String {
        val payload = StringBuilder(command)
        
        parameters?.let { params ->
            if (params.isNotEmpty()) {
                payload.append("?")
                params.entries.forEachIndexed { index, entry ->
                    if (index > 0) payload.append("&")
                    payload.append("${entry.key}=${entry.value}")
                }
            }
        }
        
        return payload.toString()
    }
    
    /**
     * Processa resposta de status da bateria
     */
    private fun parseBatteryStatusResponse(data: ByteArray, length: Int): BatteryStatusResponse? {
        try {
            if (length < 16) return null
            
            val buffer = ByteBuffer.wrap(data, 0, length)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            
            // Formato da resposta:
            // [eventType:1][level:1][voltage:4][temperature:4][charging:1][reserved:5]
            val eventType = buffer.get().toInt()
            val level = buffer.get().toInt() and 0xFF
            val voltage = buffer.float
            val temperature = buffer.float
            val charging = buffer.get().toInt() == 1
            
            return BatteryStatusResponse(
                level = level,
                voltage = voltage,
                temperature = temperature,
                isCharging = charging,
                timestamp = System.currentTimeMillis()
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao processar resposta: ${e.message}")
            return null
        }
    }
    
    /**
     * Verifica se a API está conectada
     */
    fun isConnected(): Boolean = isConnected
    
    /**
     * Obtém informações da conexão
     */
    fun getConnectionInfo(): ConnectionInfo {
        return ConnectionInfo(
            ip = deviceAddress?.hostAddress ?: "N/A",
            port = DEFAULT_PORT,
            connected = isConnected,
            lastActivity = System.currentTimeMillis()
        )
    }
}

/**
 * Resposta de status da bateria
 */
data class BatteryStatusResponse(
    val level: Int,           // Percentual 0-100
    val voltage: Float,       // Voltagem em V
    val temperature: Float,   // Temperatura em °C
    val isCharging: Boolean,  // Status de carregamento
    val timestamp: Long       // Timestamp da leitura
)

/**
 * Informações da conexão
 */
data class ConnectionInfo(
    val ip: String,
    val port: Int,
    val connected: Boolean,
    val lastActivity: Long
)

/**
 * Singleton para acesso global à API
 */
object BatteryAPIManager {
    private var apiInstance: BatteryAPI? = null
    
    fun getInstance(): BatteryAPI {
        if (apiInstance == null) {
            apiInstance = BatteryAPI()
        }
        return apiInstance!!
    }
    
    fun resetInstance() {
        apiInstance?.disconnect()
        apiInstance = null
    }
}

