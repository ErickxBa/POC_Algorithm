package com.erickballas.pruebaconceptoalgoritmolpa.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erickballas.pruebaconceptoalgoritmolpa.repository.GraphRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

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

@HiltViewModel
class RouteViewModel @Inject constructor(
    private val repository: GraphRepository
) : ViewModel() {

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
                repository.calculateRoute(startNodeId, goalNodeId, safetyProfile)
                    .collect { route ->
                        _routeState.value = _routeState.value.copy(
                            route = route,
                            isLoading = false,
                            safetyProfile = safetyProfile
                        )
                    }
            } catch (e: Exception) {
                _routeState.value = _routeState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }

    fun clearRoute() {
        _routeState.value = RouteState()
    }
}
