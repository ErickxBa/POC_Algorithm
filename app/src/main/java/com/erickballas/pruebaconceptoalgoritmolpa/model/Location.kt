package com.erickballas.pruebaconceptoalgoritmolpa.model

/**
 * Representa un nodo en el grafo de la ciudad (intersección de calles)
 */
data class Location(
    val nodeId: Long,
    val latitude: Double,
    val longitude: Double,
    val name: String = "",
    val description: String = ""
) {
    /**
     * Calcula la distancia euclidiana entre dos ubicaciones
     */
    fun distanceTo(other: Location): Double {
        val dLat = other.latitude - latitude
        val dLon = other.longitude - longitude
        return kotlin.math.sqrt(dLat * dLat + dLon * dLon)
    }
}
