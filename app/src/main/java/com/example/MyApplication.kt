package com.example

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Main application class.
 * Marked with @HiltAndroidApp to serve as the dependency container injection base.
 */
@HiltAndroidApp
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
