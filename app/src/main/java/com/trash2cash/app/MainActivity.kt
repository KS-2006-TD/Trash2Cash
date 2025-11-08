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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
        if (result.resultCode == RESULT_OK) {
            val imageUri = result.data?.getParcelableExtra<Uri>(CameraActivity.EXTRA_IMAGE_URI)
            imageUri?.let { uri ->
                // TODO: Get actual location - for now using dummy coordinates
                handleImageCapture(uri.toString(), 28.6139, 77.2090, "Current Location")
            }
        }
    }

    private lateinit var trash2CashViewModel: Trash2CashViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Theme state managed at top level
            var isDarkTheme by remember { mutableStateOf(false) }

            Trash2CashTheme(darkTheme = isDarkTheme) {
                val context = LocalContext.current
                val authViewModel: AuthViewModel = viewModel { AuthViewModel(context) }
                trash2CashViewModel = viewModel { Trash2CashViewModel(context) }

                Trash2CashMainApp(
                    authViewModel = authViewModel,
                    viewModel = trash2CashViewModel,
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = { isDarkTheme = !isDarkTheme }
                )
            }
        }
    }

    private fun handleImageCapture(imageUri: String, latitude: Double, longitude: Double, locationName: String) {
        trash2CashViewModel.submitWastePhoto(imageUri, latitude, longitude, locationName)
    }

    private fun launchCamera() {
        val intent = CameraActivity.newIntent(this)
        cameraLauncher.launch(intent)
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
                    onNavigateToApp = {
                        // This will be called when authentication is successful
                        // The state change will automatically navigate to the correct app
                    }
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
                        // Admin interface can be added later
                        MunicipalApp(
                            user = currentState.user,
                            onLogout = { authViewModel.logout() }
                        )
                    }
                }
            }
            is AuthState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is AuthState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Authentication Error",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = currentState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { authViewModel.logout() }) {
                            Text("Try Again")
                        }
                    }
                }
            }
        }
    }
}