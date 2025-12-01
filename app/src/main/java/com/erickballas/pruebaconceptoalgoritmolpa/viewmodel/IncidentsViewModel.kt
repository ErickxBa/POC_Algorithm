package com.erickballas.pruebaconceptoalgoritmolpa.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.erickballas.pruebaconceptoalgoritmolpa.repository.GraphRepository

data class IncidentsState(
    val isLoading: Boolean = false,
    val incidents: List<IncidentData> = emptyList(),
    val lastReportId: String? = null,
    val error: String? = null
)

data class IncidentData(
    val reportId: String,
    val streetId: Int,
    val incidentType: String,
    val severity: Int,
    val latitude: Double,
    val longitude: Double,
    val description: String,
    val reportedAt: String
)

@HiltViewModel
class IncidentsViewModel @Inject constructor(
    private val repository: GraphRepository
) : ViewModel() {

    private val _incidentsState = MutableStateFlow(IncidentsState())
    val incidentsState: StateFlow<IncidentsState> = _incidentsState

    fun reportIncident(
        streetId: Int,
        incidentType: String,
        severity: Int,
        latitude: Double,
        longitude: Double,
        description: String = ""
    ) {
        viewModelScope.launch {
            _incidentsState.value = _incidentsState.value.copy(isLoading = true, error = null)
            try {
                val response = repository.reportIncident(
                    streetId, incidentType, severity, latitude, longitude, description
                )
                _incidentsState.value = _incidentsState.value.copy(
                    lastReportId = response.reportId,
                    isLoading = false
                )
                // Recargar incidentes cercanos
                loadNearbyIncidents(latitude, longitude)
            } catch (e: Exception) {
                _incidentsState.value = _incidentsState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }

    fun loadNearbyIncidents(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            try {
                val incidents = repository.getNearbyIncidents(latitude, longitude)
                _incidentsState.value = _incidentsState.value.copy(incidents = incidents)
            } catch (e: Exception) {
                _incidentsState.value = _incidentsState.value.copy(error = e.message)
            }
        }
    }
}
