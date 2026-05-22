package com.example.data.api

import android.util.Log

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
    
    // Simple callback to notify ViewModel or UI when simulated error state is updated.
    var onErrorModeChanged: ((ErrorMode) -> Unit)? = null
    
    fun setMode(mode: ErrorMode) {
        activeErrorMode = mode
        Log.i(TAG, "Simulated network mode changed to: $mode")
        onErrorModeChanged?.invoke(mode)
    }
}
