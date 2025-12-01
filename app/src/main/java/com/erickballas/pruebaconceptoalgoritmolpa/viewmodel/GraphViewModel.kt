package com.erickballas.pruebaconceptoalgoritmolpa.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GraphViewModel : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _nodeCount = MutableStateFlow(7)
    val nodeCount: StateFlow<Int> = _nodeCount

    private val _edgeCount = MutableStateFlow(8)
    val edgeCount: StateFlow<Int> = _edgeCount

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        refreshGraphData()
    }

    fun refreshGraphData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _nodeCount.value = 7
                _edgeCount.value = 8
                _error.value = null
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }
}
