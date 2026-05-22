package com.example.data.api

import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

sealed class FriendlyError(
    val title: String,
    val description: String,
    val iconName: String
) {
    object NoInternet : FriendlyError(
        title = "No Network Connection",
        description = "It seems you are offline. Check your browser connection, Wi-Fi, or mobile cellular configuration.",
        iconName = "WifiOff"
    )

    object Timeout : FriendlyError(
        title = "Connection Timed Out",
        description = "Our dispatch server took too long to return records. Request timed out.",
        iconName = "Timer"
    )

    class ServerError(val code: Int, details: String) : FriendlyError(
        title = "Server Maintenance Failure ($code)",
        description = details,
        iconName = "CloudOff"
    )

    class Unknown(val tMessage: String) : FriendlyError(
        title = "Unexpected System Event",
        description = "An unhandled exception was captured: $tMessage",
        iconName = "BugReport"
    )

    companion object {
        fun from(throwable: Throwable): FriendlyError {
            return when (throwable) {
                is SocketTimeoutException -> Timeout
                is IOException -> {
                    // Check if is our specialized timeout
                    if (throwable.message?.contains("time out", ignoreCase = true) == true) {
                        Timeout
                    } else {
                        NoInternet
                    }
                }
                is HttpException -> {
                    val code = throwable.code()
                    val details = when (code) {
                        500 -> "Critical backend cluster crash. Database transaction failed to complete during query indexing."
                        429 -> "Usage threshold exceeded. API request frequency is temporarily restricted. Cool downactive."
                        else -> "HTTP request failed with status code $code. Remote host rejected this operation."
                    }
                    ServerError(code, details)
                }
                else -> {
                    if (throwable.message?.contains("timeout", ignoreCase = true) == true) {
                        Timeout
                    } else {
                        Unknown(throwable.localizedMessage ?: "Unknown compilation classpath layout mismatch")
                    }
                }
            }
        }
    }
}
