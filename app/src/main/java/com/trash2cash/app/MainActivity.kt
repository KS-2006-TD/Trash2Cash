package com.trash2cash.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trash2cash.app.data.*
import com.trash2cash.app.services.AuthState
import com.trash2cash.app.ui.auth.LandingScreen
import com.trash2cash.app.ui.citizen.CitizenApp
import com.trash2cash.app.ui.municipal.MunicipalApp
import com.trash2cash.app.ui.theme.Trash2CashTheme
import com.trash2cash.app.viewmodel.AuthViewModel
import com.trash2cash.app.viewmodel.Trash2CashViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        android.util.Log.d("MainActivity", "=== STARTING TRASH2CASH ===")

        setContent {
            MainApp()
        }
    }

    @Composable
    fun MainApp() {
        var isDarkTheme by remember { mutableStateOf(false) }

        Trash2CashTheme(darkTheme = isDarkTheme) {
            // Wait a moment before initializing heavy components
            var isReady by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(300) // Small delay for smooth startup
                isReady = true
            }

            if (!isReady) {
                // Show simple loading
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Initialize ViewModels only after UI is ready
                val context = androidx.compose.ui.platform.LocalContext.current
                val authViewModel: AuthViewModel = viewModel { AuthViewModel(context) }
                val trash2CashViewModel: Trash2CashViewModel =
                    viewModel { Trash2CashViewModel(context) }

                MainContent(
                    authViewModel = authViewModel,
                    trash2CashViewModel = trash2CashViewModel,
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = { isDarkTheme = !isDarkTheme }
                )
            }
        }
    }

    @Composable
    fun MainContent(
        authViewModel: AuthViewModel,
        trash2CashViewModel: Trash2CashViewModel,
        isDarkTheme: Boolean,
        onThemeToggle: () -> Unit
    ) {
        val authState by authViewModel.authState.collectAsState()

        when (val currentState = authState) {
            is AuthState.NotAuthenticated -> {
                LandingScreen(
                    authViewModel = authViewModel,
                    onNavigateToApp = { }
                )
            }
            is AuthState.Authenticated -> {
                when (currentState.user.role) {
                    UserRole.CITIZEN -> {
                        CitizenApp(
                            user = currentState.user,
                            onLogout = { authViewModel.logout() },
                            isDarkTheme = isDarkTheme,
                            onThemeToggle = onThemeToggle
                        )
                    }

                    UserRole.MUNICIPAL_WORKER -> {
                        MunicipalApp(
                            user = currentState.user,
                            onLogout = { authViewModel.logout() }
                        )
                    }

                    UserRole.ADMIN -> {
                        MunicipalApp(
                            user = currentState.user,
                            onLogout = { authViewModel.logout() }
                        )
                    }
                }
            }

            is AuthState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is AuthState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Error: ${currentState.message}")
                        Button(onClick = { authViewModel.logout() }) {
                            Text("Try Again")
                        }
                    }
                }
            }
        }
    }
}