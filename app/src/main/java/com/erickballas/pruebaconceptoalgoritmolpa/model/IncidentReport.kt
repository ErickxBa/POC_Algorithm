package com.erickballas.pruebaconceptoalgoritmolpa.model

import java.time.LocalDateTime

/**
 * Representa un reporte de incidente que afecta el nivel de riesgo de una calle
 */
data class IncidentReport(
    val reportId: Long,
    val streetId: Long,
    val incidentType: IncidentType,
    val severity: Int = 5, // 1-10
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val reportedAt: LocalDateTime = LocalDateTime.now(),
    val riskIncrement: Double = 0.0
)

enum class IncidentType {
    ACCIDENT,           // Accidente
    ROBBERY,            // Robo
    CONSTRUCTION,       // Construcción
    HEAVY_TRAFFIC,      // Tráfico pesado
    FLOODING,           // Inundación
    ROAD_DAMAGE,        // Daño en la calzada
    PROTEST,            // Protesta
    OTHER
}
