package com.erickballas.pruebaconceptoalgoritmolpa.service

import com.erickballas.pruebaconceptoalgoritmolpa.model.SafetyProfile
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @POST("routing/calculate")
    suspend fun calculateRoute(@Body request: RouteRequest): RouteResponse

    @GET("graph/status")
    suspend fun loadCityGraph(): GraphResponse

    @POST("graph/initialize")
    suspend fun initializeGraph(@Body request: InitGraphRequest): GraphResponse

    @POST("incidents/report")
    suspend fun reportIncident(@Body request: ReportIncidentRequest): IncidentApiResponse

    @GET("incidents/nearby")
    suspend fun getNearbyIncidents(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radiusMeters") radiusMeters: Double = 5000.0
    ): IncidentsListResponse
}

// --- DTOs (Data Transfer Objects) ---

data class RouteRequest(
    val startNodeId: Long,
    val goalNodeId: Long,
    val safetyProfile: SafetyProfile,
    val alpha: Double,
    val beta: Double
)

data class RouteResponse(
    val success: Boolean,
    val data: RouteData?,
    val message: String?
)

data class RouteData(
    val routeId: String,
    val path: List<RouteNode>,
    val totalDistance: Double,
    val totalCost: Double,
    val description: String
)

data class RouteNode(
    val nodeId: Long,
    val latitude: Double,
    val longitude: Double
)

data class GraphResponse(
    val success: Boolean,
    val nodes: List<GraphNode>?,
    val edges: List<GraphEdge>?,
    val message: String?
)

data class GraphNode(val nodeId: Long, val latitude: Double, val longitude: Double)
data class GraphEdge(val streetId: Long, val fromNodeId: Long, val toNodeId: Long, val distanceMeters: Double, val currentRiskScore: Double)

data class InitGraphRequest(val latitude: Double, val longitude: Double)

data class ReportIncidentRequest(
    val streetId: Int,
    val incidentType: String,
    val severity: Int,
    val latitude: Double,
    val longitude: Double,
    val description: String
)

data class IncidentApiResponse(
    val success: Boolean,
    val data: IncidentResponse?, // Aquí corregimos para usar la clase concreta
    val message: String?
)

// --- ESTA ES LA CLASE QUE FALTABA ---
data class IncidentResponse(
    val reportId: Any,
    val streetId: Int,
    val previousRiskScore: Double,
    val newRiskScore: Double,
    val riskIncrement: Double,
    val message: String
)

data class IncidentsListResponse(
    val success: Boolean,
    val incidents: List<IncidentData>?,
    val data: List<IncidentData>?,
    val message: String?
)

data class IncidentData(
    val reportId: Any,
    val streetId: Long,
    val incidentType: String,
    val severity: Int,
    val latitude: Double,
    val longitude: Double
)