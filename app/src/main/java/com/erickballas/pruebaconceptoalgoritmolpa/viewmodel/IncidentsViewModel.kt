package com.erickballas.pruebaconceptoalgoritmolpa.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

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

class IncidentsViewModel : ViewModel() {

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
                // Generar ID único para el reporte
                val reportId = UUID.randomUUID().toString()

                // Simular envío al backend (más adelante se integrará con ApiService)
                val incident = IncidentData(
                    reportId = reportId,
                    streetId = streetId,
                    incidentType = incidentType,
                    severity = severity,
                    latitude = latitude,
                    longitude = longitude,
                    description = description,
                    reportedAt = System.currentTimeMillis().toString()
                )

                _incidentsState.value = _incidentsState.value.copy(
                    lastReportId = reportId,
                    isLoading = false
                )

                // Recargar incidentes cercanos
                loadNearbyIncidents(latitude, longitude)
            } catch (e: Exception) {
                _incidentsState.value = _incidentsState.value.copy(
                    error = e.message ?: "Error al reportar incidente",
                    isLoading = false
                )
            }
        }
    }

    fun loadNearbyIncidents(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            try {
                // Simular datos de incidentes cercanos
                val mockIncidents = emptyList<IncidentData>()

                _incidentsState.value = _incidentsState.value.copy(incidents = mockIncidents)
            } catch (e: Exception) {
                _incidentsState.value = _incidentsState.value.copy(
                    error = e.message ?: "Error al cargar incidentes cercanos"
                )
            }
        }
    }
}
