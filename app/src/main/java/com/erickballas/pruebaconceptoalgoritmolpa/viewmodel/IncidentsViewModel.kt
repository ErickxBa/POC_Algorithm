package com.erickballas.pruebaconceptoalgoritmolpa.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erickballas.pruebaconceptoalgoritmolpa.repository.GraphRepository
import com.erickballas.pruebaconceptoalgoritmolpa.service.IncidentData
import com.erickballas.pruebaconceptoalgoritmolpa.service.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class IncidentsState(
    val isLoading: Boolean = false,
    val incidents: List<IncidentData> = emptyList(),
    val lastReportId: String? = null,
    val error: String? = null
)

class IncidentsViewModel : ViewModel() {

    private val repository = GraphRepository(RetrofitClient.apiService)
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
                val response = repository.reportIncident(streetId, incidentType, severity, latitude, longitude, description)
                _incidentsState.value = _incidentsState.value.copy(
                    lastReportId = response.reportId.toString(),
                    isLoading = false
                )
            } catch (e: Exception) {
                _incidentsState.value = _incidentsState.value.copy(error = e.message, isLoading = false)
            }
        }
    }
}