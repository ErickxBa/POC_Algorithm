package com.erickballas.pruebaconceptoalgoritmolpa.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Datos agnósticos (No dependen de Google Maps ni OSM)
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
    val error: String? = null,
    val zoom: Float = 15f
)

class MapViewModel : ViewModel() {

    private val _mapState = MutableStateFlow(MapState())
    val mapState: StateFlow<MapState> = _mapState

    fun setUserLocation(lat: Double, lng: Double) {
        _mapState.value = _mapState.value.copy(
            userLocation = GeoLocation(lat, lng)
        )
    }

    fun displayRoute(nodes: List<GeoLocation>) {
        _mapState.value = _mapState.value.copy(route = nodes)
    }

    fun addIncident(incident: MapIncident) {
        val currentIncidents = _mapState.value.incidents.toMutableList()
        currentIncidents.add(incident)
        _mapState.value = _mapState.value.copy(incidents = currentIncidents)
    }

    fun setIncidents(incidents: List<MapIncident>) {
        _mapState.value = _mapState.value.copy(incidents = incidents)
    }

    fun clearRoute() {
        _mapState.value = _mapState.value.copy(route = emptyList())
    }

    fun clearIncidents() {
        _mapState.value = _mapState.value.copy(incidents = emptyList())
    }

    fun setLoading(isLoading: Boolean) {
        _mapState.value = _mapState.value.copy(isLoading = isLoading)
    }

    fun setError(error: String?) {
        _mapState.value = _mapState.value.copy(error = error)
    }
}
