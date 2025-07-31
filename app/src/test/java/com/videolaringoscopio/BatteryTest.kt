package com.videolaringoscopio

import com.videolaringoscopio.battery.BatteryMonitoringViewModel
import com.videolaringoscopio.api.BatteryAPI
import com.videolaringoscopio.api.BatteryRoutes
import org.junit.Test
import org.junit.Assert.*

/**
 * Testes unitários para funcionalidade de bateria
 */
class BatteryTest {
    
    @Test
    fun testBatteryRoutes() {
        // Testar se as rotas são válidas
        assertTrue(BatteryRoutes.isValidRoute(BatteryRoutes.GET_BATTERY_STATUS))
        assertTrue(BatteryRoutes.isValidRoute(BatteryRoutes.START_BATTERY_MONITORING))
        assertTrue(BatteryRoutes.isValidRoute(BatteryRoutes.STOP_BATTERY_MONITORING))
        
        // Testar rota inválida
        assertFalse(BatteryRoutes.isValidRoute("INVALID_ROUTE"))
    }
    
    @Test
    fun testBatteryCommandBuilder() {
        // Testar construção de comando sem parâmetros
        val command1 = BatteryRoutes.buildCommand(BatteryRoutes.GET_BATTERY_STATUS)
        assertEquals("GET_BATTERY", command1)
        
        // Testar construção de comando com parâmetros
        val params = mapOf("interval" to 5, "threshold" to 20)
        val command2 = BatteryRoutes.buildCommand(BatteryRoutes.START_BATTERY_MONITORING, params)
        assertTrue(command2.contains("BATTERY_MON_ON"))
        assertTrue(command2.contains("interval=5"))
        assertTrue(command2.contains("threshold=20"))
    }
    
    @Test
    fun testBatteryStatusEnum() {
        // Testar enum de status
        val status = BatteryMonitoringViewModel.BatteryStatus.LOW
        assertNotNull(status)
        assertEquals("LOW", status.name)
    }
    
    @Test
    fun testBatteryLevels() {
        // Testar constantes de níveis
        assertEquals(10, BatteryMonitoringViewModel.BATTERY_LEVEL_CRITICAL)
        assertEquals(20, BatteryMonitoringViewModel.BATTERY_LEVEL_LOW)
        assertEquals(50, BatteryMonitoringViewModel.BATTERY_LEVEL_NORMAL)
    }
    
    @Test
    fun testUDPEvents() {
        // Testar eventos UDP
        assertEquals(20, BatteryRoutes.UDPEvents.BATTERY_STATUS)
        assertEquals(21, BatteryRoutes.UDPEvents.BATTERY_LOW)
        assertEquals(22, BatteryRoutes.UDPEvents.BATTERY_CRITICAL)
        assertEquals(23, BatteryRoutes.UDPEvents.BATTERY_CHARGING)
    }
    
    @Test
    fun testDefaultParameters() {
        // Testar parâmetros padrão
        assertEquals(5, BatteryRoutes.DefaultParams.MONITORING_INTERVAL)
        assertEquals(20, BatteryRoutes.DefaultParams.LOW_BATTERY_THRESHOLD)
        assertEquals(10, BatteryRoutes.DefaultParams.CRITICAL_BATTERY_THRESHOLD)
    }
    
    @Test
    fun testEndpoints() {
        // Testar endpoints
        assertEquals("192.168.4.1", BatteryRoutes.Endpoints.DEFAULT_IP)
        assertEquals(8888, BatteryRoutes.Endpoints.DEFAULT_PORT)
        assertEquals(8889, BatteryRoutes.Endpoints.BATTERY_PORT)
        
        val endpoint = BatteryRoutes.Endpoints.getBatteryEndpoint("status")
        assertTrue(endpoint.contains("battery"))
        assertTrue(endpoint.contains("v1"))
        assertTrue(endpoint.contains("status"))
    }
}

