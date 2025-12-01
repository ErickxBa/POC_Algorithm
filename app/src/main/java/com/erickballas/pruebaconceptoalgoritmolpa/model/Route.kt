package com.erickballas.pruebaconceptoalgoritmolpa.model

/**
 * Representa una ruta calculada entre dos puntos
 */
data class Route(
    val routeId: String,
    val startLocation: Location,
    val endLocation: Location,
    val waypoints: List<Location>,
    val streets: List<Street>,
    val totalDistance: Double,
    val totalRiskScore: Double,
    val estimatedTimeMinutes: Int,
    val safetyProfile: SafetyProfile,
    val calculatedAt: Long = System.currentTimeMillis(),
    val isOptimal: Boolean = true
) {
    /**
     * Calcula el costo total de la ruta
     */
    fun calculateTotalCost(alpha: Double = 0.5, beta: Double = 0.5): Double {
        return streets.sumOf { it.calculateCost(alpha, beta) }
    }

    /**
     * Retorna una descripción de la ruta
     */
    fun getDescription(): String {
        return "Ruta desde ${startLocation.name} a ${endLocation.name}\n" +
                "Distancia: ${(totalDistance / 1000).toInt()} km\n" +
                "Tiempo estimado: $estimatedTimeMinutes min\n" +
                "Nivel de riesgo: ${String.format("%.1f", totalRiskScore)}/10"
    }
}

enum class SafetyProfile {
    FASTEST,      // α=0.8, β=0.2
    BALANCED,     // α=0.5, β=0.5
    SAFEST        // α=0.2, β=0.8
}
