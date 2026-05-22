package com.example.data.api

import android.util.Log
import kotlinx.coroutines.flow.asStateFlow

enum class ErrorMode {
    NONE,
    NO_INTERNET,
    SERVER_ERROR_500,
    RATE_LIMIT_429,
    TIMEOUT
}

object NetworkSimulationManager {
    private const val TAG = "NetworkSimManager"
    
    var activeErrorMode: ErrorMode = ErrorMode.NONE
    
    private val _errorModeFlow = kotlinx.coroutines.flow.MutableStateFlow(ErrorMode.NONE)
    val errorModeFlow = _errorModeFlow.asStateFlow()
    
    // Simple callback to notify ViewModel or UI when simulated error state is updated.
    var onErrorModeChanged: ((ErrorMode) -> Unit)? = null
    
    fun setMode(mode: ErrorMode) {
        activeErrorMode = mode
        _errorModeFlow.value = mode
        Log.i(TAG, "Simulated network mode changed to: $mode")
        onErrorModeChanged?.invoke(mode)
    }
}
