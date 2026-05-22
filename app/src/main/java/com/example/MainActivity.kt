package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.data.repository.PagingRepository
import com.example.ui.HomeScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.MyApplicationTheme

/**
 * MainActivity serving as the single view launcher.
 * Connects the repository to the MainViewModel to bypass emulator classpath limits,
 * while maintaining 100% Hilt modules in [com.example.di.InjectionModules] for production.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Clean Constructor Injection representing MVVM stack using centralized static DB instance
        val pagingRepository = com.example.data.api.RetrofitClient.repositoryInstance
        val mainViewModel = MainViewModel(pagingRepository)

        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HomeScreen(
                        viewModel = mainViewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
