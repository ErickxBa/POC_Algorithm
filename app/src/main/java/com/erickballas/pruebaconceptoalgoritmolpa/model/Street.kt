package com.erickballas.pruebaconceptoalgoritmolpa.model

/**
 * Representa una calle (edge) entre dos nodos en el grafo
 */
data class Street(
    val streetId: Long,
    val fromNodeId: Long,
    val toNodeId: Long,
    val distanceMeters: Double,
    val currentRiskScore: Double = 0.0,
    val speedLimitKmh: Int = 50,
    val roadType: RoadType = RoadType.STREET
) {
    /**
     * Calcula el costo compuesto de transitar esta calle
     * Costo = (α × distancia) + (β × riesgo)
     */
    fun calculateCost(alpha: Double = 0.5, beta: Double = 0.5): Double {
        // Normalizar la distancia a escala similar al riesgo (0-10)
        val normalizedDistance = (distanceMeters / 1000.0).coerceIn(0.0, 10.0)
        return (alpha * normalizedDistance) + (beta * currentRiskScore)
    }
}

enum class RoadType {
    STREET,
    AVENUE,
    HIGHWAY,
    SECONDARY_STREET,
    ALLEY
}
