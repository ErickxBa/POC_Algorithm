package com.erickballas.pruebaconceptoalgoritmolpa.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erickballas.pruebaconceptoalgoritmolpa.model.SafetyProfile
import com.erickballas.pruebaconceptoalgoritmolpa.repository.GraphRepository
import com.erickballas.pruebaconceptoalgoritmolpa.service.RetrofitClient
import com.erickballas.pruebaconceptoalgoritmolpa.service.RouteData // Importamos la clase del servicio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class RouteState(
    val isLoading: Boolean = false,
    val route: RouteData? = null,
    val error: String? = null,
    val safetyProfile: String = "balanced"
)

class RouteViewModel : ViewModel() {

    private val repository = GraphRepository(RetrofitClient.apiService)

    private val _routeState = MutableStateFlow(RouteState())
    val routeState: StateFlow<RouteState> = _routeState

    fun calculateRoute(
        startNodeId: Int,
        goalNodeId: Int,
        safetyProfileStr: String = "balanced"
    ) {
        viewModelScope.launch {
            _routeState.value = _routeState.value.copy(isLoading = true, error = null)
            try {
                val profile = when(safetyProfileStr) {
                    "fastest" -> SafetyProfile.FASTEST
                    "safest" -> SafetyProfile.SAFEST
                    else -> SafetyProfile.BALANCED
                }

                val result = repository.calculateRoute(
                    startNodeId.toLong(),
                    goalNodeId.toLong(),
                    profile
                )

                _routeState.value = _routeState.value.copy(
                    route = result,
                    isLoading = false,
                    safetyProfile = safetyProfileStr
                )
            } catch (e: Exception) {
                _routeState.value = _routeState.value.copy(
                    error = e.message ?: "Error al calcular ruta",
                    isLoading = false
                )
            }
        }
    }

    fun clearRoute() {
        _routeState.value = RouteState()
    }
}