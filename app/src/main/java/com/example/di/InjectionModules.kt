package com.example.di

import com.example.data.repository.PagingRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Modern DI dependency declaration.
 * This class describes Hilt-modular injection declarations for the project.
 */
@Module
@InstallIn(SingletonComponent::class)
object InjectionModules {

    @Provides
    @Singleton
    fun providePagingRepository(): PagingRepository {
        return PagingRepository()
    }
}
