package com.trash2cash.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trash2cash.app.data.*
import com.trash2cash.app.services.AuthState
import com.trash2cash.app.ui.auth.LandingScreen
import com.trash2cash.app.ui.camera.CameraActivity
import com.trash2cash.app.ui.citizen.CitizenApp
import com.trash2cash.app.ui.municipal.MunicipalApp
import com.trash2cash.app.ui.theme.Trash2CashTheme
import com.trash2cash.app.viewmodel.AuthViewModel
import com.trash2cash.app.viewmodel.Trash2CashViewModel

class MainActivity : ComponentActivity() {

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Handle camera result
    }

    private var trash2CashViewModel: Trash2CashViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        android.util.Log.d("MainActivity", "onCreate called")

        setContent {
            MainScreen()
        }
    }

    @Composable
    fun MainScreen() {
        // Theme state
        var isDarkTheme by remember { mutableStateOf(false) }
        var isInitialized by remember { mutableStateOf(false) }
        var initError by remember { mutableStateOf<String?>(null) }

        Trash2CashTheme(darkTheme = isDarkTheme) {
            if (initError != null) {
                // Show error
                ErrorScreen(error = initError ?: "Unknown error")
            } else if (!isInitialized) {
                // Show loading
                LoadingScreen()

                // Initialize in background
                LaunchedEffect(Unit) {
                    try {
                        kotlinx.coroutines.delay(500) // Give UI time to render
                        android.util.Log.d("MainActivity", "Initialization complete")
                        isInitialized = true
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Init failed", e)
                        initError = e.message
                    }
                }
            } else {
                // Show main app
                var viewModelError by remember { mutableStateOf<String?>(null) }

                if (viewModelError != null) {
                    ErrorScreen(error = "Failed to load: $viewModelError")
                } else {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val authViewModel: AuthViewModel =
                        androidx.lifecycle.viewmodel.compose.viewModel {
                            AuthViewModel(context)
                        }

                    if (trash2CashViewModel == null) {
                        trash2CashViewModel = androidx.lifecycle.viewmodel.compose.viewModel {
                            Trash2CashViewModel(context)
                        }
                    }

                    Trash2CashMainApp(
                        authViewModel = authViewModel,
                        viewModel = trash2CashViewModel!!,
                        isDarkTheme = isDarkTheme,
                        onThemeToggle = { isDarkTheme = !isDarkTheme }
                    )
                }
            }
        }
    }

    @Composable
    fun LoadingScreen() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Loading Trash2Cash...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }

    @Composable
    fun ErrorScreen(error: String) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "⚠️ Error",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { recreate() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Restart App")
                }
            }
        }
    }

    @Composable
    fun Trash2CashMainApp(
        authViewModel: AuthViewModel,
        viewModel: Trash2CashViewModel,
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
                LoadingScreen()
            }
            is AuthState.Error -> {
                ErrorScreen(error = currentState.message)
            }
        }
    }
}