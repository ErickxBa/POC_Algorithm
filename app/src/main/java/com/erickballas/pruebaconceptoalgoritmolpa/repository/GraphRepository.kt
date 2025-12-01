package com.erickballas.pruebaconceptoalgoritmolpa.repository

import com.erickballas.pruebaconceptoalgoritmolpa.model.Location
import com.erickballas.pruebaconceptoalgoritmolpa.model.Route
import com.erickballas.pruebaconceptoalgoritmolpa.model.SafetyProfile
import com.erickballas.pruebaconceptoalgoritmolpa.service.ApiService

/**
 * Repositorio para acceso a datos de rutas y grafo
 */
class GraphRepository(private val apiService: ApiService) {

    /**
     * Calcula una ruta entre dos ubicaciones
     */
    suspend fun calculateRoute(
        startLocation: Location,
        endLocation: Location,
        safetyProfile: SafetyProfile
    ): Route {
        // Mapear perfil de seguridad a valores de alpha y beta
        val (alpha, beta) = when (safetyProfile) {
            SafetyProfile.FASTEST -> 0.8 to 0.2
            SafetyProfile.BALANCED -> 0.5 to 0.5
            SafetyProfile.SAFEST -> 0.2 to 0.8
        }

        // Llamar al API
        val response = apiService.calculateRoute(
            com.erickballas.pruebaconceptoalgoritmolpa.service.RouteRequest(
                startNodeId = startLocation.nodeId,
                endNodeId = endLocation.nodeId,
                safetyProfile = safetyProfile,
                alpha = alpha,
                beta = beta
            )
        )

        return response.data ?: throw Exception("Failed to calculate route")
    }

    /**
     * Carga el grafo de la ciudad
     */
    suspend fun loadCityGraph() {
        val response = apiService.loadCityGraph()
        if (!response.success) {
            throw Exception(response.message ?: "Failed to load graph")
        }
        // Aquí se guardarían los datos en caché local si fuera necesario
    }

    /**
     * Obtiene incidentes cercanos a una ubicación
     */
    suspend fun getNearbyIncidents(
        latitude: Double,
        longitude: Double,
        radiusMeters: Double = 5000.0
    ) {
        val response = apiService.getNearbyIncidents(latitude, longitude, radiusMeters)
        if (!response.success) {
            throw Exception(response.message ?: "Failed to load incidents")
        }
        // Aquí se guardarían los incidentes en caché
    }
}
