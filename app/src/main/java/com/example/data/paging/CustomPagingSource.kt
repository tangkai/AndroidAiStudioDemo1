package com.example.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.data.api.FriendlyError
import com.example.data.api.RetrofitClient
import com.example.data.api.RetryUtils
import com.example.data.model.PagingItem
import com.example.data.repository.PagingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * PagingSource implementation that supports middle-starting key positioning
 * and bidirectional loading (forward and backward navigation) using OkHttp & Retrofit.
 * Handles simulated connection states and applies exponential backoff retry.
 */
class CustomPagingSource(
    private val repository: PagingRepository,
    private val initialOffset: Int = 120 // Defaults to middle of 300 records
) : PagingSource<Int, PagingItem>() {

    override fun getRefreshKey(state: PagingState<Int, PagingItem>): Int? {
        // Return closest anchor page key for seamless UI reload upon invalidation
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(state.config.pageSize)
                ?: anchorPage?.nextKey?.minus(state.config.pageSize)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PagingItem> {
        return withContext(Dispatchers.IO) {
            try {
                val currentOffset = params.key ?: initialOffset
                val loadSize = params.loadSize

                repository.addLog("API Request: Load dynamic chunk (Offset: $currentOffset, Size: $loadSize, Mode: ${params::class.simpleName})")

                // Execute loading utilizing real RetrofitClient with retry utility & exponential backoff
                val items = RetryUtils.retryWithBackoff(
                    initialDelayMillis = 800L,
                    maxDelayMillis = 4000L,
                    maxAttempts = 3,
                    onRetryAttempt = { attempt, delayMs, error ->
                        val friendly = FriendlyError.from(error)
                        repository.addLog("Retry $attempt/3 of failed load state after ${delayMs}ms (Cause: ${friendly.title})")
                    }
                ) {
                    when (params) {
                        is LoadParams.Prepend -> {
                            val offset = Math.max(0, currentOffset - loadSize)
                            val actualSize = currentOffset - offset
                            if (actualSize <= 0) {
                                emptyList()
                            } else {
                                RetrofitClient.apiService.getItems(offset, actualSize)
                            }
                        }
                        is LoadParams.Append -> {
                            RetrofitClient.apiService.getItems(currentOffset, loadSize)
                        }
                        is LoadParams.Refresh -> {
                            RetrofitClient.apiService.getItems(currentOffset, loadSize)
                        }
                    }
                }

                // Setup pagination keys matching the bidirectional model
                val prevKey = when (params) {
                    is LoadParams.Prepend -> {
                        val offset = Math.max(0, currentOffset - loadSize)
                        if (offset <= 0) null else offset
                    }
                    is LoadParams.Append -> {
                        currentOffset
                    }
                    is LoadParams.Refresh -> {
                        if (currentOffset <= 0) null else Math.max(0, currentOffset - loadSize)
                    }
                }

                val nextKey = when (params) {
                    is LoadParams.Prepend -> {
                        currentOffset
                    }
                    is LoadParams.Append -> {
                        val nextOffset = currentOffset + items.size
                        if (items.isEmpty() || items.size < loadSize || nextOffset >= repository.getTotalCount()) null else nextOffset
                    }
                    is LoadParams.Refresh -> {
                        val nextOffset = currentOffset + items.size
                        if (items.isEmpty() || items.size < loadSize || nextOffset >= repository.getTotalCount()) null else nextOffset
                    }
                }

                repository.addLog("API Success: Loaded ${items.size} records. (PrevKey: $prevKey, NextKey: $nextKey)")

                LoadResult.Page(
                    data = items,
                    prevKey = prevKey,
                    nextKey = nextKey
                )
            } catch (exception: Exception) {
                val friendly = FriendlyError.from(exception)
                repository.addLog("API Failure: Load failed. (Reason: ${friendly.title} - ${friendly.description})")
                LoadResult.Error(exception)
            }
        }
    }
}
