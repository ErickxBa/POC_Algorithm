package com.erickballas.pruebaconceptoalgoritmolpa.repository

import com.erickballas.pruebaconceptoalgoritmolpa.model.SafetyProfile
import com.erickballas.pruebaconceptoalgoritmolpa.service.*

class GraphRepository(private val apiService: ApiService) {

    suspend fun calculateRoute(
        startNodeId: Long,
        endNodeId: Long,
        safetyProfile: SafetyProfile
    ): RouteData {
        val (alpha, beta) = when (safetyProfile) {
            SafetyProfile.FASTEST -> 0.8 to 0.2
            SafetyProfile.BALANCED -> 0.5 to 0.5
            SafetyProfile.SAFEST -> 0.2 to 0.8
        }

        val request = RouteRequest(startNodeId, endNodeId, safetyProfile, alpha, beta)
        val response = apiService.calculateRoute(request)
        return response.data ?: throw Exception(response.message ?: "Error al calcular ruta")
    }

    suspend fun loadCityGraph(): GraphResponse {
        val response = apiService.loadCityGraph()
        if (!response.success) throw Exception(response.message)
        return response
    }

    suspend fun initializeGraph(lat: Double, lng: Double) {
        val response = apiService.initializeGraph(InitGraphRequest(lat, lng))
        if (!response.success) throw Exception(response.message)
    }

    suspend fun reportIncident(
        streetId: Int,
        incidentType: String,
        severity: Int,
        latitude: Double,
        longitude: Double,
        description: String
    ): IncidentResponse {
        val request = ReportIncidentRequest(streetId, incidentType, severity, latitude, longitude, description)
        val response = apiService.reportIncident(request)

        if (!response.success) throw Exception(response.message ?: "Error al reportar")
        return response.data ?: throw Exception("Respuesta vacía del servidor")
    }

    suspend fun getNearbyIncidents(
        latitude: Double,
        longitude: Double,
        radiusMeters: Double = 5000.0
    ): List<IncidentData> {
        val response = apiService.getNearbyIncidents(latitude, longitude, radiusMeters)
        if (!response.success) throw Exception(response.message)
        return response.incidents ?: response.data ?: emptyList()
    }
}