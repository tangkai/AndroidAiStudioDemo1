package com.example.data.api

import android.util.Log
import kotlinx.coroutines.delay

object RetryUtils {
    private const val TAG = "RetryUtils"

    /**
     * Centralized exponential backoff retry runner.
     * Log messages are pushed back to the main repository logs stream so they materialize visually for the user.
     */
    suspend fun <T> retryWithBackoff(
        initialDelayMillis: Long = 1000L,
        maxDelayMillis: Long = 6000L,
        maxAttempts: Int = 3,
        backoffFactor: Double = 2.0,
        onRetryAttempt: (attempt: Int, delayMs: Long, error: Throwable) -> Unit = { _, _, _ -> },
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelayMillis
        for (attempt in 1..maxAttempts) {
            try {
                return block()
            } catch (throwable: Throwable) {
                if (attempt == maxAttempts) {
                    Log.e(TAG, "All $maxAttempts attempts failed. Final failure thrown.")
                    throw throwable
                }
                
                Log.w(TAG, "Attempt $attempt failed: ${throwable.message}. Retrying in ${currentDelay}ms...")
                onRetryAttempt(attempt, currentDelay, throwable)
                
                delay(currentDelay)
                currentDelay = (currentDelay * backoffFactor).toLong().coerceAtMost(maxDelayMillis)
            }
        }
        throw IllegalStateException("Fatal: Unreachable retry logic end-state")
    }
}
