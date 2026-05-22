package com.example.data.api

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException
import java.net.SocketTimeoutException

class MockNetworkInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val mode = NetworkSimulationManager.activeErrorMode

        when (mode) {
            ErrorMode.NO_INTERNET -> {
                // Simulate physical network disruption
                throw IOException("No physical connection detected. Please verify your Wi-Fi or cellular network settings.")
            }
            ErrorMode.TIMEOUT -> {
                // Simulate gateway or network latency timeouts
                Thread.sleep(500)
                throw SocketTimeoutException("The connection request to the server timed out. Please try running the task again.")
            }
            ErrorMode.SERVER_ERROR_500 -> {
                // Simulate critical backend failures
                val errorJson = """
                    {
                        "error": "InternalServerError",
                        "message": "A critical database index failure occurred while processing this operation. StackTrace trace-id: REST-99120"
                    }
                """.trimIndent()
                return Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(500)
                    .message("Internal Server Error")
                    .body(errorJson.toResponseBody("application/json".toMediaTypeOrNull()))
                    .header("Cache-Control", "no-cache")
                    .build()
            }
            ErrorMode.RATE_LIMIT_429 -> {
                // Simulate rate limit or quota exceeded
                val errorJson = """
                    {
                        "error": "TooManyRequests",
                        "message": "API key usage quota exceeded. Please reduce request frequency. Rate limited to 15 req/min."
                    }
                """.trimIndent()
                return Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(429)
                    .message("Too Many Requests")
                    .body(errorJson.toResponseBody("application/json".toMediaTypeOrNull()))
                    .header("Retry-After", "10")
                    .build()
            }
            ErrorMode.NONE -> {
                // Proceed with normal pipeline. Wait, since we are calling a mock URL, we intercept and generate the success response ourselves
                // extracted from our database.
                val url = request.url
                if (url.encodedPath.contains("api/items")) {
                    val offset = url.queryParameter("offset")?.toIntOrNull() ?: 0
                    val limit = url.queryParameter("limit")?.toIntOrNull() ?: 15
                    
                    // We can query the repository to serve the static DB mock data
                    val repository = RetrofitClient.repositoryInstance
                    val items = repository.fetchPage(offset, limit)
                    
                    // Convert list structure into Moshi-friendly or simple JSON
                    val jsonResponse = buildJsonString(items)
                    
                    return Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(jsonResponse.toResponseBody("application/json".toMediaTypeOrNull()))
                        .build()
                }
            }
        }
        
        // Default fallback
        return chain.proceed(request)
    }

    private fun buildJsonString(items: List<com.example.data.model.PagingItem>): String {
        val sBuilder = StringBuilder()
        sBuilder.append("[")
        items.forEachIndexed { i, item ->
            sBuilder.append("{")
            sBuilder.append("\"id\":").append(item.id).append(",")
            sBuilder.append("\"title\":\"").append(escapeJson(item.title)).append("\",")
            sBuilder.append("\"subtitle\":\"").append(escapeJson(item.subtitle)).append("\",")
            sBuilder.append("\"timestamp\":").append(item.timestamp).append(",")
            sBuilder.append("\"type\":\"").append(item.type.name).append("\",")
            sBuilder.append("\"category\":\"").append(escapeJson(item.category)).append("\"")
            sBuilder.append("}")
            if (i < items.size - 1) {
                sBuilder.append(",")
            }
        }
        sBuilder.append("]")
        return sBuilder.toString()
    }

    private fun escapeJson(input: String): String {
        return input.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
    }
}
