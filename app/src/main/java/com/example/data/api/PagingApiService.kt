package com.example.data.api

import com.example.data.model.PagingItem
import retrofit2.http.GET
import retrofit2.http.Query

interface PagingApiService {
    @GET("api/items")
    suspend fun getItems(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int
    ): List<PagingItem>
}
