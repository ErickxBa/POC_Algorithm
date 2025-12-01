package com.erickballas.pruebaconceptoalgoritmolpa.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class RouteState(
    val isLoading: Boolean = false,
    val route: RouteData? = null,
    val error: String? = null,
    val safetyProfile: String = "balanced"
)

data class RouteData(
    val routeId: String,
    val path: List<Int>,
    val totalDistance: Int,
    val totalCost: Double,
    val expandedNodes: Int,
    val calculationTime: Int,
    val description: String
)

class RouteViewModel : ViewModel() {

    private val _routeState = MutableStateFlow(RouteState())
    val routeState: StateFlow<RouteState> = _routeState

    fun calculateRoute(
        startNodeId: Int,
        goalNodeId: Int,
        safetyProfile: String = "balanced"
    ) {
        viewModelScope.launch {
            _routeState.value = _routeState.value.copy(isLoading = true, error = null)
            try {
                // Simular cálculo de ruta
                val mockRoute = RouteData(
                    routeId = UUID.randomUUID().toString(),
                    path = listOf(startNodeId, 200, 300, goalNodeId),
                    totalDistance = 8500,
                    totalCost = 42.5,
                    expandedNodes = 6,
                    calculationTime = 45,
                    description = "Ruta segura desde nodo $startNodeId a $goalNodeId"
                )

                _routeState.value = _routeState.value.copy(
                    route = mockRoute,
                    isLoading = false,
                    safetyProfile = safetyProfile
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
