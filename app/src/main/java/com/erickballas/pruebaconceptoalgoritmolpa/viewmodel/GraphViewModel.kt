package com.erickballas.pruebaconceptoalgoritmolpa.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erickballas.pruebaconceptoalgoritmolpa.repository.GraphRepository
import com.erickballas.pruebaconceptoalgoritmolpa.service.GraphEdge
import com.erickballas.pruebaconceptoalgoritmolpa.service.GraphNode
import com.erickballas.pruebaconceptoalgoritmolpa.service.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GraphViewModel : ViewModel() {
    private val repository = GraphRepository(RetrofitClient.apiService)
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _nodes = MutableStateFlow<List<GraphNode>>(emptyList())
    val nodes: StateFlow<List<GraphNode>> = _nodes

    private val _edges = MutableStateFlow<List<GraphEdge>>(emptyList())
    val edges: StateFlow<List<GraphEdge>> = _edges

    fun loadGraphData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.loadCityGraph()
                if (response.success) {
                    _nodes.value = response.nodes ?: emptyList()
                    _edges.value = response.edges ?: emptyList()
                }
            } catch (e: Exception) {
                // Manejar error
            } finally {
                _isLoading.value = false
            }
        }
    }
}