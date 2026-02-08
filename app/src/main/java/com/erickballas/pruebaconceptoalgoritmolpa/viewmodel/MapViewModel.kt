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
import kotlin.math.*

data class GeoLocation(val latitude: Double, val longitude: Double)

data class MapIncident(val id: String, val location: GeoLocation, val type: String, val severity: Int)

data class MapState(
    val isLoading: Boolean = false,
    val userLocation: GeoLocation? = null,
    val route: List<GeoLocation> = emptyList(),
    val incidents: List<MapIncident> = emptyList(),
    val nodes: List<GraphNode> = emptyList(),
    val edges: List<GraphEdge> = emptyList(),
    val error: String? = null
)

class MapViewModel : ViewModel() {

    private val repository = GraphRepository(RetrofitClient.apiService)
    private val _mapState = MutableStateFlow(MapState())
    val mapState: StateFlow<MapState> = _mapState

    fun setUserLocation(lat: Double, lng: Double) {
        _mapState.value = _mapState.value.copy(userLocation = GeoLocation(lat, lng))
        loadGraphData()
        loadNearbyIncidents(lat, lng)
    }

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
            } catch (e: Exception) {}
        }
    }

    fun initializeGraphAtLocation(lat: Double, lng: Double) {
        viewModelScope.launch {
            try {
                _mapState.value = _mapState.value.copy(isLoading = true)
                repository.initializeGraph(lat, lng)
                loadGraphData()
                loadNearbyIncidents(lat, lng)
            } catch (e: Exception) {
                _mapState.value = _mapState.value.copy(error = e.message)
            } finally {
                _mapState.value = _mapState.value.copy(isLoading = false)
            }
        }
    }

    private fun loadNearbyIncidents(lat: Double, lng: Double) {
        viewModelScope.launch {
            try {
                val incidents = repository.getNearbyIncidents(lat, lng)
                _mapState.value = _mapState.value.copy(
                    incidents = incidents.map { MapIncident(it.reportId.toString(), GeoLocation(it.latitude, it.longitude), it.incidentType, it.severity) }
                )
            } catch (e: Exception) {}
        }
    }

    // --- CÁLCULO DE RUTA SIMPLIFICADO ---
    fun calculateRouteToDestination(destLat: Double, destLng: Double) {
        val userLoc = _mapState.value.userLocation ?: return
        val nodes = _mapState.value.nodes
        if (nodes.isEmpty()) return

        viewModelScope.launch {
            _mapState.value = _mapState.value.copy(isLoading = true)
            try {
                val startNode = findNearestNode(userLoc.latitude, userLoc.longitude, nodes)
                val endNode = findNearestNode(destLat, destLng, nodes)

                if (startNode != null && endNode != null) {
                    val routeData = repository.calculateRoute(
                        startNode.nodeId,
                        endNode.nodeId,
                        com.erickballas.pruebaconceptoalgoritmolpa.model.SafetyProfile.BALANCED
                    )

                    // AHORA ES DIRECTO: El backend ya nos manda las coordenadas
                    val routeCoords = routeData.path.map {
                        GeoLocation(it.latitude, it.longitude)
                    }

                    _mapState.value = _mapState.value.copy(route = routeCoords)
                }
            } catch (e: Exception) {
                _mapState.value = _mapState.value.copy(error = "Error ruta: ${e.message}")
            } finally {
                _mapState.value = _mapState.value.copy(isLoading = false)
            }
        }
    }

    fun getNearestStreetId(lat: Double, lng: Double): Int {
        val nodes = _mapState.value.nodes
        val edges = _mapState.value.edges
        if (nodes.isEmpty()) return 0
        val nearestNode = findNearestNode(lat, lng, nodes) ?: return 0
        return edges.find { it.fromNodeId == nearestNode.nodeId || it.toNodeId == nearestNode.nodeId }?.streetId?.toInt() ?: 0
    }

    private fun findNearestNode(lat: Double, lng: Double, nodes: List<GraphNode>): GraphNode? {
        return nodes.minByOrNull {
            val dLat = it.latitude - lat; val dLon = it.longitude - lng
            dLat*dLat + dLon*dLon
        }
    }

    fun clearRoute() { _mapState.value = _mapState.value.copy(route = emptyList()) }
}