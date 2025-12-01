package com.erickballas.pruebaconceptoalgoritmolpa.service

import com.erickballas.pruebaconceptoalgoritmolpa.model.Location
import com.erickballas.pruebaconceptoalgoritmolpa.model.Route
import com.erickballas.pruebaconceptoalgoritmolpa.model.SafetyProfile
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Body

/**
 * Interfaz para comunicación con el API de Nest.js
 */
interface ApiService {

    /**
     * Calcula una ruta usando LPA*
     */
    @POST("/routing/calculate")
    suspend fun calculateRoute(
        @Body request: RouteRequest
    ): RouteResponse

    /**
     * Obtiene el grafo de la ciudad
     */
    @GET("/graph/load")
    suspend fun loadCityGraph(): GraphResponse

    /**
     * Obtiene incidentes cercanos
     */
    @GET("/incidents/nearby")
    suspend fun getNearbyIncidents(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radiusMeters") radiusMeters: Double = 5000.0
    ): IncidentsResponse
}

/**
 * Request para calcular ruta
 */
data class RouteRequest(
    val startNodeId: Long,
    val endNodeId: Long,
    val safetyProfile: SafetyProfile,
    val alpha: Double,
    val beta: Double
)

/**
 * Response con la ruta calculada
 */
data class RouteResponse(
    val success: Boolean,
    val data: Route?,
    val message: String?
)

/**
 * Response con el grafo
 */
data class GraphResponse(
    val success: Boolean,
    val nodes: List<Location>?,
    val edges: List<EdgeData>?,
    val message: String?
)

/**
 * Datos de un edge del grafo
 */
data class EdgeData(
    val streetId: Long,
    val fromNodeId: Long,
    val toNodeId: Long,
    val distanceMeters: Double,
    val currentRiskScore: Double,
    val speedLimitKmh: Int
)

/**
 * Response con incidentes
 */
data class IncidentsResponse(
    val success: Boolean,
    val incidents: List<IncidentData>?,
    val message: String?
)

/**
 * Datos de un incidente
 */
data class IncidentData(
    val reportId: Long,
    val streetId: Long,
    val incidentType: String,
    val severity: Int,
    val latitude: Double,
    val longitude: Double
)
