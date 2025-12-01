package com.erickballas.pruebaconceptoalgoritmolpa.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.erickballas.pruebaconceptoalgoritmolpa.repository.GraphRepository

data class MapIncident(
    val id: String,
    val location: LatLng,
    val type: String,
    val severity: Int
)

data class MapState(
    val isLoading: Boolean = false,
    val userLocation: LatLng? = null,
    val route: List<LatLng> = emptyList(),
    val incidents: List<MapIncident> = emptyList(),
    val error: String? = null,
    val zoom: Float = 15f
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val repository: GraphRepository
) : ViewModel() {

    private val _mapState = MutableStateFlow(MapState())
    val mapState: StateFlow<MapState> = _mapState

    fun setUserLocation(lat: Double, lng: Double) {
        _mapState.value = _mapState.value.copy(
            userLocation = LatLng(lat, lng)
        )
    }

    fun displayRoute(nodeIds: List<Int>) {
        viewModelScope.launch {
            try {
                val nodes = repository.getGraphNodes()
                val route = nodeIds.mapNotNull { id ->
                    nodes.find { it.nodeId == id }?.let { 
                        LatLng(it.latitude, it.longitude)
                    }
                }
                _mapState.value = _mapState.value.copy(route = route)
            } catch (e: Exception) {
                _mapState.value = _mapState.value.copy(error = e.message)
            }
        }
    }

    fun loadNearbyIncidents(lat: Double, lng: Double, radiusMeters: Int = 5000) {
        viewModelScope.launch {
            try {
                val incidents = repository.getNearbyIncidents(lat, lng, radiusMeters)
                val mapIncidents = incidents.map { incident ->
                    MapIncident(
                        id = incident.reportId,
                        location = LatLng(incident.latitude, incident.longitude),
                        type = incident.incidentType,
                        severity = incident.severity
                    )
                }
                _mapState.value = _mapState.value.copy(incidents = mapIncidents)
            } catch (e: Exception) {
                _mapState.value = _mapState.value.copy(error = e.message)
            }
        }
    }

    fun clearRoute() {
        _mapState.value = _mapState.value.copy(route = emptyList())
    }
}
