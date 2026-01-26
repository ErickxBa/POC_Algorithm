package com.erickballas.pruebaconceptoalgoritmolpa.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erickballas.pruebaconceptoalgoritmolpa.model.SafetyProfile
import com.erickballas.pruebaconceptoalgoritmolpa.repository.GraphRepository
import com.erickballas.pruebaconceptoalgoritmolpa.service.GraphEdge
import com.erickballas.pruebaconceptoalgoritmolpa.service.GraphNode
import com.erickballas.pruebaconceptoalgoritmolpa.service.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Collections.emptyList
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
    val nodes: List<GraphNode> = emptyList(),
    val edges: List<GraphEdge> = emptyList(),
    val error: String? = null,
    val zoom: Float = 15f
)

class MapViewModel : ViewModel() {

    private val repository = GraphRepository(RetrofitClient.apiService)
    private val _mapState = MutableStateFlow(MapState())
    val mapState: StateFlow<MapState> = _mapState

    // ESTA VARIABLE ES LA CLAVE: Guarda la ruta mientras el GPS inicia
    private var pendingRouteRequest: Pair<Double, Double>? = null

    fun setUserLocation(lat: Double, lng: Double) {
        val current = _mapState.value.userLocation
        _mapState.value = _mapState.value.copy(userLocation = GeoLocation(lat, lng))

        // Cargar datos si es la primera vez
        if (current == null || calculateDistance(current.latitude, current.longitude, lat, lng) > 10.0) {
            Log.d("MapViewModel", "📍 GPS Actualizado: $lat, $lng")
            if (_mapState.value.nodes.isEmpty()) {
                loadGraphData()
            }
            loadNearbyIncidents(lat, lng)
        }

        // CORRECCIÓN: Si había una ruta esperando por el GPS, ¡ejecútala ahora!
        pendingRouteRequest?.let { (destLat, destLng) ->
            Log.d("MapViewModel", "🚦 GPS Listo. Ejecutando ruta PENDIENTE hacia: $destLat, $destLng")
            calculateRouteToDestination(destLat, destLng)
            pendingRouteRequest = null // Ya no está pendiente
        }
    }

    private fun loadGraphData() {
        viewModelScope.launch {
            try {
                val response = repository.loadCityGraph()
                if (response.success) {
                    val nodes = response.nodes ?: emptyList()
                    val edges = response.edges ?: emptyList()
                    _mapState.value = _mapState.value.copy(nodes = nodes, edges = edges)
                    Log.d("MapViewModel", "✅ Grafo cargado: ${nodes.size} nodos.")

                    // Si la ruta falló antes por falta de nodos, reintentamos aquí
                    val userLoc = _mapState.value.userLocation
                    if (userLoc != null && pendingRouteRequest != null) {
                        val (dLat, dLng) = pendingRouteRequest!!
                        calculateRouteToDestination(dLat, dLng)
                        pendingRouteRequest = null
                    }
                }
            } catch (e: Exception) {
                Log.e("MapViewModel", "❌ Error cargando grafo: ${e.message}")
            }
        }
    }

    fun initializeGraphAtLocation(lat: Double, lng: Double) {
        viewModelScope.launch {
            try {
                Log.d("MapViewModel", "🔨 Inicializando red...")
                _mapState.value = _mapState.value.copy(isLoading = true)
                repository.initializeGraph(lat, lng)
                loadGraphData()
            } catch (e: Exception) {
                Log.e("MapViewModel", "❌ Error inicializando: ${e.message}")
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
            } catch (e: Exception) { /* Error silencioso */ }
        }
    }

    fun calculateRouteToDestination(destLat: Double, destLng: Double) {
        val userLoc = _mapState.value.userLocation
        Log.d("MapViewModel", "🚀 Solicitud de ruta hacia: $destLat, $destLng")

        // 1. SI NO HAY GPS: GUARDAR Y ESPERAR
        if (userLoc == null) {
            Log.w("MapViewModel", "⏳ GPS no listo. Ruta guardada como PENDIENTE.")
            pendingRouteRequest = Pair(destLat, destLng)
            _mapState.value = _mapState.value.copy(isLoading = true)
            return
        }

        // 2. SI NO HAY NODOS: CARGAR Y ESPERAR
        if (_mapState.value.nodes.isEmpty()) {
            Log.w("MapViewModel", "⚠️ Grafo no cargado. Cargando y esperando...")
            pendingRouteRequest = Pair(destLat, destLng)
            loadGraphData()
            return
        }

        // 3. SI TODO ESTÁ LISTO: CALCULAR
        performRouteCalculation(userLoc.latitude, userLoc.longitude, destLat, destLng, _mapState.value.nodes)
    }

    private fun performRouteCalculation(
        startLat: Double, startLng: Double,
        endLat: Double, endLng: Double,
        nodes: List<GraphNode>
    ) {
        viewModelScope.launch {
            _mapState.value = _mapState.value.copy(isLoading = true, error = null)
            try {
                val startNode = findNearestNode(startLat, startLng, nodes)
                val endNode = findNearestNode(endLat, endLng, nodes)

                if (startNode == null || endNode == null) {
                    Log.e("MapViewModel", "⛔ Nodos no encontrados cerca.")
                    _mapState.value = _mapState.value.copy(error = "Fuera de cobertura")
                    return@launch
                }

                Log.d("MapViewModel", "📡 Enviando a Backend: ${startNode.nodeId} -> ${endNode.nodeId}")

                val routeData = repository.calculateRoute(
                    startNode.nodeId,
                    endNode.nodeId,
                    SafetyProfile.BALANCED
                )

                Log.d("MapViewModel", "✅ Ruta recibida: ${routeData.path.size} puntos")

                val routeCoords = routeData.path.mapNotNull { nodeId ->
                    nodes.find { it.nodeId == nodeId }?.let { GeoLocation(it.latitude, it.longitude) }
                }

                if (routeCoords.isNotEmpty()) {
                    _mapState.value = _mapState.value.copy(route = routeCoords)
                } else {
                    Log.w("MapViewModel", "⚠️ La ruta recibida está vacía")
                }

            } catch (e: Exception) {
                Log.e("MapViewModel", "❌ Error cálculo: ${e.message}")
                _mapState.value = _mapState.value.copy(error = e.message)
            } finally {
                _mapState.value = _mapState.value.copy(isLoading = false)
            }
        }
    }

    // Métodos auxiliares sin cambios
    fun getNearestStreetId(lat: Double, lng: Double): Int {
        val edges = _mapState.value.edges
        val nodes = _mapState.value.nodes
        if (edges.isEmpty() || nodes.isEmpty()) return 0
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
        val r = 6371
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c * 1000
    }

    fun clearRoute() {
        _mapState.value = _mapState.value.copy(route = emptyList())
    }
}