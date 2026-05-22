package com.example.data.api

import com.example.data.repository.PagingRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object RetrofitClient {
    // Repository instance reference used by our dynamic MockNetworkInterceptor to simulate physical DB access
    val repositoryInstance = PagingRepository()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(MockNetworkInterceptor())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://localhost/") // Intercepted internally
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val apiService: PagingApiService = retrofit.create(PagingApiService::class.java)
}
