package com.videolaringoscopio.api

/**
 * Definições de rotas e endpoints da API de bateria
 * Centraliza todas as rotas de comunicação com o videolaringoscópio
 */
object BatteryRoutes {
    
    // ========================================
    // ROTAS PRINCIPAIS DE BATERIA
    // ========================================
    
    /**
     * Rota para obter status atual da bateria
     * Retorna: nível, voltagem, temperatura, status de carregamento
     */
    const val GET_BATTERY_STATUS = "GET_BATTERY"
    
    /**
     * Rota para ativar monitoramento contínuo
     * Parâmetros: interval (segundos entre atualizações)
     */
    const val START_BATTERY_MONITORING = "BATTERY_MON_ON"
    
    /**
     * Rota para desativar monitoramento contínuo
     */
    const val STOP_BATTERY_MONITORING = "BATTERY_MON_OFF"
    
    /**
     * Rota para obter histórico de bateria
     * Parâmetros: hours (últimas N horas)
     */
    const val GET_BATTERY_HISTORY = "BATTERY_HISTORY"
    
    /**
     * Rota para configurar alertas de bateria
     * Parâmetros: low_threshold, critical_threshold
     */
    const val SET_BATTERY_ALERTS = "BATTERY_ALERTS"
    
    // ========================================
    // ROTAS DE CONFIGURAÇÃO
    // ========================================
    
    /**
     * Rota para calibrar sensor de bateria
     */
    const val CALIBRATE_BATTERY = "BATTERY_CAL"
    
    /**
     * Rota para resetar sistema de bateria
     */
    const val RESET_BATTERY_SYSTEM = "BATTERY_RESET"
    
    /**
     * Rota para configurar parâmetros de bateria
     * Parâmetros: capacity, voltage_min, voltage_max
     */
    const val SET_BATTERY_CONFIG = "BATTERY_CONFIG"
    
    /**
     * Rota para obter configurações atuais
     */
    const val GET_BATTERY_CONFIG = "GET_BATTERY_CONFIG"
    
    // ========================================
    // ROTAS DE DIAGNÓSTICO
    // ========================================
    
    /**
     * Rota para executar teste de bateria
     */
    const val RUN_BATTERY_TEST = "BATTERY_TEST"
    
    /**
     * Rota para obter informações detalhadas
     */
    const val GET_BATTERY_INFO = "BATTERY_INFO"
    
    /**
     * Rota para verificar saúde da bateria
     */
    const val CHECK_BATTERY_HEALTH = "BATTERY_HEALTH"
    
    // ========================================
    // ROTAS DE ENERGIA
    // ========================================
    
    /**
     * Rota para controlar carregamento
     * Parâmetros: enable (true/false)
     */
    const val CONTROL_CHARGING = "CHARGING_CTRL"
    
    /**
     * Rota para modo de economia de energia
     * Parâmetros: mode (low, normal, high)
     */
    const val SET_POWER_MODE = "POWER_MODE"
    
    /**
     * Rota para desligar dispositivo
     */
    const val SHUTDOWN_DEVICE = "SHUTDOWN"
    
    // ========================================
    // ENDPOINTS DE REDE
    // ========================================
    
    object Endpoints {
        const val DEFAULT_IP = "192.168.4.1"
        const val DEFAULT_PORT = 8888
        const val BATTERY_PORT = 8889
        const val WEBSOCKET_PORT = 8890
        
        // URLs completas (para futuras implementações HTTP)
        const val BASE_URL = "http://$DEFAULT_IP:$DEFAULT_PORT"
        const val BATTERY_BASE_URL = "$BASE_URL/battery"
        const val API_VERSION = "v1"
        
        fun getBatteryEndpoint(route: String): String {
            return "$BATTERY_BASE_URL/$API_VERSION/$route"
        }
    }
    
    // ========================================
    // PARÂMETROS PADRÃO
    // ========================================
    
    object DefaultParams {
        const val MONITORING_INTERVAL = 5  // segundos
        const val LOW_BATTERY_THRESHOLD = 20  // %
        const val CRITICAL_BATTERY_THRESHOLD = 10  // %
        const val CONNECTION_TIMEOUT = 5000  // ms
        const val READ_TIMEOUT = 3000  // ms
        const val MAX_RETRIES = 3
    }
    
    // ========================================
    // CÓDIGOS DE RESPOSTA
    // ========================================
    
    object ResponseCodes {
        const val SUCCESS = 200
        const val BAD_REQUEST = 400
        const val UNAUTHORIZED = 401
        const val NOT_FOUND = 404
        const val INTERNAL_ERROR = 500
        const val DEVICE_OFFLINE = 503
        const val TIMEOUT = 504
    }
    
    // ========================================
    // EVENTOS UDP
    // ========================================
    
    object UDPEvents {
        const val BATTERY_STATUS = 20
        const val BATTERY_LOW = 21
        const val BATTERY_CRITICAL = 22
        const val BATTERY_CHARGING = 23
        const val BATTERY_FULL = 24
        const val BATTERY_ERROR = 25
        const val BATTERY_DISCONNECTED = 26
        const val BATTERY_CALIBRATION_COMPLETE = 27
    }
    
    // ========================================
    // HELPER FUNCTIONS
    // ========================================
    
    /**
     * Constrói comando UDP com parâmetros
     */
    fun buildCommand(route: String, params: Map<String, Any>? = null): String {
        val command = StringBuilder(route)
        
        params?.let { parameters ->
            if (parameters.isNotEmpty()) {
                command.append("?")
                parameters.entries.forEachIndexed { index, entry ->
                    if (index > 0) command.append("&")
                    command.append("${entry.key}=${entry.value}")
                }
            }
        }
        
        return command.toString()
    }
    
    /**
     * Valida se uma rota é válida
     */
    fun isValidRoute(route: String): Boolean {
        return when (route) {
            GET_BATTERY_STATUS,
            START_BATTERY_MONITORING,
            STOP_BATTERY_MONITORING,
            GET_BATTERY_HISTORY,
            SET_BATTERY_ALERTS,
            CALIBRATE_BATTERY,
            RESET_BATTERY_SYSTEM,
            SET_BATTERY_CONFIG,
            GET_BATTERY_CONFIG,
            RUN_BATTERY_TEST,
            GET_BATTERY_INFO,
            CHECK_BATTERY_HEALTH,
            CONTROL_CHARGING,
            SET_POWER_MODE,
            SHUTDOWN_DEVICE -> true
            else -> false
        }
    }
    
    /**
     * Obtém descrição da rota
     */
    fun getRouteDescription(route: String): String {
        return when (route) {
            GET_BATTERY_STATUS -> "Obter status atual da bateria"
            START_BATTERY_MONITORING -> "Iniciar monitoramento contínuo"
            STOP_BATTERY_MONITORING -> "Parar monitoramento contínuo"
            GET_BATTERY_HISTORY -> "Obter histórico de bateria"
            SET_BATTERY_ALERTS -> "Configurar alertas de bateria"
            CALIBRATE_BATTERY -> "Calibrar sensor de bateria"
            RESET_BATTERY_SYSTEM -> "Resetar sistema de bateria"
            SET_BATTERY_CONFIG -> "Configurar parâmetros"
            GET_BATTERY_CONFIG -> "Obter configurações atuais"
            RUN_BATTERY_TEST -> "Executar teste de bateria"
            GET_BATTERY_INFO -> "Obter informações detalhadas"
            CHECK_BATTERY_HEALTH -> "Verificar saúde da bateria"
            CONTROL_CHARGING -> "Controlar carregamento"
            SET_POWER_MODE -> "Configurar modo de energia"
            SHUTDOWN_DEVICE -> "Desligar dispositivo"
            else -> "Rota desconhecida"
        }
    }
}

