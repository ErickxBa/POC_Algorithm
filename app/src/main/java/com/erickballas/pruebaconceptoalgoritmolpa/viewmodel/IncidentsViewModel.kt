package com.erickballas.pruebaconceptoalgoritmolpa.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erickballas.pruebaconceptoalgoritmolpa.repository.GraphRepository
import com.erickballas.pruebaconceptoalgoritmolpa.service.IncidentData
import com.erickballas.pruebaconceptoalgoritmolpa.service.NominatimClient
import com.erickballas.pruebaconceptoalgoritmolpa.service.NominatimResult
import com.erickballas.pruebaconceptoalgoritmolpa.service.RetrofitClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private val nominatimService = NominatimClient.service // Cliente para búsqueda

    private val _incidentsState = MutableStateFlow(IncidentsState())
    val incidentsState: StateFlow<IncidentsState> = _incidentsState

    // Estado para las sugerencias de búsqueda
    private val _searchSuggestions = MutableStateFlow<List<NominatimResult>>(emptyList())
    val searchSuggestions: StateFlow<List<NominatimResult>> = _searchSuggestions

    private var searchJob: Job? = null

    // Función de búsqueda con "Debounce" (espera a que termines de escribir)
    fun searchLocation(query: String) {
        if (query.length < 3) {
            _searchSuggestions.value = emptyList()
            return
        }

        searchJob?.cancel() // Cancela búsqueda anterior si sigue escribiendo
        searchJob = viewModelScope.launch {
            delay(1000) // Esperar 1 segundo después de dejar de escribir
            try {
                val results = nominatimService.search(query)
                _searchSuggestions.value = results
            } catch (e: Exception) {
                println("Error buscando dirección: ${e.message}")
                _searchSuggestions.value = emptyList()
            }
        }
    }

    fun clearSuggestions() {
        _searchSuggestions.value = emptyList()
    }

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
                    lastReportId = response.reportId.toString(),
                    isLoading = false
                )
            } catch (e: Exception) {
                _incidentsState.value = _incidentsState.value.copy(
                    error = e.message ?: "Error desconocido",
                    isLoading = false
                )
            }
        }
    }
}