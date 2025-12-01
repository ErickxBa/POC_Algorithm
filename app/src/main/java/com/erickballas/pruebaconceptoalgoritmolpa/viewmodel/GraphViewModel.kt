package com.erickballas.pruebaconceptoalgoritmolpa.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.erickballas.pruebaconceptoalgoritmolpa.repository.GraphRepository

data class GraphStatus(
    val status: String,
    val nodeCount: Int,
    val edgeCount: Int,
    val incidentCount: Int,
    val loadedAt: String
)

data class GraphNode(
    val nodeId: Int,
    val latitude: Double,
    val longitude: Double
)

data class GraphEdge(
    val edgeId: Int,
    val fromNodeId: Int,
    val toNodeId: Int,
    val distanceMeters: Int,
    val currentRiskScore: Double,
    val speedLimitKmh: Int
)

data class GraphUIState(
    val isLoading: Boolean = false,
    val status: GraphStatus? = null,
    val nodes: List<GraphNode> = emptyList(),
    val edges: List<GraphEdge> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class GraphViewModel @Inject constructor(
    private val repository: GraphRepository
) : ViewModel() {

    private val _graphState = MutableStateFlow(GraphUIState())
    val graphState: StateFlow<GraphUIState> = _graphState

    init {
        refreshGraphData()
    }

    fun refreshGraphData() {
        viewModelScope.launch {
            _graphState.value = _graphState.value.copy(isLoading = true)
            try {
                val status = repository.getGraphStatus()
                val nodes = repository.getGraphNodes()
                val edges = repository.getGraphEdges()

                _graphState.value = _graphState.value.copy(
                    status = status,
                    nodes = nodes,
                    edges = edges,
                    isLoading = false
                )
            } catch (e: Exception) {
                _graphState.value = _graphState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }
}
