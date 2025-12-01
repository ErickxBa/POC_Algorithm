package com.erickballas.pruebaconceptoalgoritmolpa.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erickballas.pruebaconceptoalgoritmolpa.repository.GraphRepository
import com.erickballas.pruebaconceptoalgoritmolpa.service.GraphEdge
import com.erickballas.pruebaconceptoalgoritmolpa.service.GraphNode
import com.erickballas.pruebaconceptoalgoritmolpa.service.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class GeoLocation(val latitude: Double, val longitude: Double)

data class MapIncident(
    val id: String,
    val location: GeoLocation,
    val type: String,
    val severity: Int
)

data class MapState(
    val isLoading: Boolean = false,
    val userLocation: GeoLocation? = null,
    val route: List<GeoLocation> = emptyList(),
    val incidents: List<MapIncident> = emptyList(),
    val nodes: List<GraphNode> = emptyList(), // Necesitamos los nodos en memoria
    val edges: List<GraphEdge> = emptyList(), // Necesitamos las calles en memoria
    val error: String? = null,
    val zoom: Float = 15f
)

class MapViewModel : ViewModel() {

    private val repository = GraphRepository(RetrofitClient.apiService)
    private val _mapState = MutableStateFlow(MapState())
    val mapState: StateFlow<MapState> = _mapState

    fun setUserLocation(lat: Double, lng: Double) {
        _mapState.value = _mapState.value.copy(userLocation = GeoLocation(lat, lng))
        // Al tener ubicación, cargamos/actualizamos los incidentes y el grafo
        loadGraphData()
        loadNearbyIncidents(lat, lng)
    }

    // Cargar grafo para poder calcular "el más cercano"
    private fun loadGraphData() {
        viewModelScope.launch {
            try {
                val response = repository.loadCityGraph()
                if (response.success) {
                    _mapState.value = _mapState.value.copy(
                        nodes = response.nodes ?: emptyList(),
                        edges = response.edges ?: emptyList()
                    )
                }
            } catch (e: Exception) {
                // Error silencioso o log
            }
        }
    }

    fun initializeGraphAtLocation(lat: Double, lng: Double) {
        viewModelScope.launch {
            try {
                _mapState.value = _mapState.value.copy(isLoading = true)
                repository.initializeGraph(lat, lng)
                loadGraphData() // Recargar grafo nuevo
                loadNearbyIncidents(lat, lng)
            } catch (e: Exception) {
                _mapState.value = _mapState.value.copy(error = "Error creando red: ${e.message}")
            } finally {
                _mapState.value = _mapState.value.copy(isLoading = false)
            }
        }
    }

    private fun loadNearbyIncidents(lat: Double, lng: Double) {
        viewModelScope.launch {
            try {
                val incidentsData = repository.getNearbyIncidents(lat, lng)
                val mapIncidents = incidentsData.map {
                    MapIncident(
                        id = it.reportId.toString(),
                        location = GeoLocation(it.latitude, it.longitude),
                        type = it.incidentType,
                        severity = it.severity
                    )
                }
                _mapState.value = _mapState.value.copy(incidents = mapIncidents)
            } catch (e: Exception) {
                println("Error cargando incidentes: ${e.message}")
            }
        }
    }

    // --- LÓGICA DE RUTEADO Y SELECCIÓN ---

    fun calculateRouteToDestination(destLat: Double, destLng: Double) {
        val userLoc = _mapState.value.userLocation ?: return
        val nodes = _mapState.value.nodes

        if (nodes.isEmpty()) {
            _mapState.value = _mapState.value.copy(error = "No hay red de nodos cargada. Crea la red primero.")
            return
        }

        // 1. Encontrar nodo más cercano al usuario (Inicio)
        val startNode = findNearestNode(userLoc.latitude, userLoc.longitude, nodes)

        // 2. Encontrar nodo más cercano al destino seleccionado (Fin)
        val endNode = findNearestNode(destLat, destLng, nodes)

        if (startNode == null || endNode == null) return

        viewModelScope.launch {
            _mapState.value = _mapState.value.copy(isLoading = true)
            try {
                // Perfil por defecto 'balanced', podrías pasarlo como parámetro
                val routeData = repository.calculateRoute(startNode.nodeId, endNode.nodeId, com.erickballas.pruebaconceptoalgoritmolpa.model.SafetyProfile.BALANCED)

                // Convertir la lista de IDs de nodos a coordenadas para pintar
                val routeCoords = routeData.path.mapNotNull { nodeId ->
                    nodes.find { it.nodeId == nodeId }?.let { GeoLocation(it.latitude, it.longitude) }
                }

                _mapState.value = _mapState.value.copy(route = routeCoords)
            } catch (e: Exception) {
                _mapState.value = _mapState.value.copy(error = "No se pudo trazar la ruta: ${e.message}")
            } finally {
                _mapState.value = _mapState.value.copy(isLoading = false)
            }
        }
    }

    // Encuentra la calle (edge) más cercana a donde el usuario puso el pin
    fun getNearestStreetId(lat: Double, lng: Double): Int {
        val edges = _mapState.value.edges
        val nodes = _mapState.value.nodes
        if (edges.isEmpty() || nodes.isEmpty()) return 0

        // Lógica simple: encontrar el nodo más cercano y devolver una arista conectada a él
        // (Para una implementación perfecta se necesitaría proyección punto-segmento, pero esto basta para la POC)
        val nearestNode = findNearestNode(lat, lng, nodes) ?: return 0
        val edge = edges.find { it.fromNodeId == nearestNode.nodeId || it.toNodeId == nearestNode.nodeId }
        return edge?.streetId?.toInt() ?: 0
    }

    private fun findNearestNode(lat: Double, lng: Double, nodes: List<GraphNode>): GraphNode? {
        return nodes.minByOrNull { node ->
            calculateDistance(lat, lng, node.latitude, node.longitude)
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371 // Radio de la tierra en km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c * 1000 // Metros
    }

    fun clearRoute() {
        _mapState.value = _mapState.value.copy(route = emptyList())
    }
}