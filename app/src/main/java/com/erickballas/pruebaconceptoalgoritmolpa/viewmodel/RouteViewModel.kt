package com.erickballas.pruebaconceptoalgoritmolpa.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erickballas.pruebaconceptoalgoritmolpa.model.SafetyProfile
import com.erickballas.pruebaconceptoalgoritmolpa.repository.GraphRepository
import com.erickballas.pruebaconceptoalgoritmolpa.service.GraphNode
import com.erickballas.pruebaconceptoalgoritmolpa.service.NominatimClient
import com.erickballas.pruebaconceptoalgoritmolpa.service.NominatimResult
import com.erickballas.pruebaconceptoalgoritmolpa.service.RetrofitClient
import com.erickballas.pruebaconceptoalgoritmolpa.service.RouteData
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class RouteState(
    val isLoading: Boolean = false,
    val route: RouteData? = null,
    val error: String? = null,
    val safetyProfile: String = "balanced"
)

class RouteViewModel : ViewModel() {

    private val repository = GraphRepository(RetrofitClient.apiService)
    private val nominatimService = NominatimClient.service

    private val _routeState = MutableStateFlow(RouteState())
    val routeState: StateFlow<RouteState> = _routeState

    // Sugerencias de búsqueda
    private val _suggestions = MutableStateFlow<List<NominatimResult>>(emptyList())
    val suggestions: StateFlow<List<NominatimResult>> = _suggestions

    private var searchJob: Job? = null

    // Cache de nodos para cálculos
    private var cachedNodes: List<GraphNode> = emptyList()

    // 1. Buscar lugares (Autocompletado)
    fun searchLocation(query: String) {
        if (query.length < 3) {
            _suggestions.value = emptyList()
            return
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(800) // Debounce
            try {
                val results = nominatimService.search(query)
                _suggestions.value = results
            } catch (e: Exception) {
                // Error silencioso en búsqueda
            }
        }
    }

    fun clearSuggestions() { _suggestions.value = emptyList() }

    // 2. Calcular ruta usando Coordenadas (Lat/Lng)
    fun calculateRouteFromCoordinates(
        startLat: Double, startLng: Double,
        endLat: Double, endLng: Double,
        safetyProfileStr: String = "balanced"
    ) {
        viewModelScope.launch {
            _routeState.value = _routeState.value.copy(isLoading = true, error = null)
            try {
                // A. Cargar nodos si no están en caché
                if (cachedNodes.isEmpty()) {
                    val graphResponse = repository.loadCityGraph()
                    cachedNodes = graphResponse.nodes ?: emptyList()
                }

                if (cachedNodes.isEmpty()) throw Exception("No hay red de nodos cargada. Ve al mapa y 'Crea la Red' primero.")

                // B. Encontrar nodos más cercanos
                val startNode = findNearestNode(startLat, startLng, cachedNodes)
                val endNode = findNearestNode(endLat, endLng, cachedNodes)

                if (startNode == null || endNode == null) throw Exception("Ubicación fuera del área de cobertura")

                // C. Calcular ruta con los IDs encontrados
                val profile = when(safetyProfileStr) {
                    "fastest" -> SafetyProfile.FASTEST
                    "safest" -> SafetyProfile.SAFEST
                    else -> SafetyProfile.BALANCED
                }

                val result = repository.calculateRoute(startNode.nodeId, endNode.nodeId, profile)

                _routeState.value = _routeState.value.copy(
                    route = result,
                    isLoading = false,
                    safetyProfile = safetyProfileStr
                )

            } catch (e: Exception) {
                _routeState.value = _routeState.value.copy(error = e.message, isLoading = false)
            }
        }
    }

    // Matemática pura: Distancia entre dos puntos
    private fun findNearestNode(lat: Double, lng: Double, nodes: List<GraphNode>): GraphNode? {
        return nodes.minByOrNull { node ->
            val r = 6371 // Radio tierra km
            val dLat = Math.toRadians(node.latitude - lat)
            val dLon = Math.toRadians(node.longitude - lng)
            val a = sin(dLat/2).pow(2) + cos(Math.toRadians(lat)) * cos(Math.toRadians(node.latitude)) * sin(dLon/2).pow(2)
            val c = 2 * atan2(sqrt(a), sqrt(1-a))
            r * c * 1000
        }
    }
}