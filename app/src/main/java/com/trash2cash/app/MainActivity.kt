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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

        android.util.Log.d("MainActivity", "=== APP STARTING ===")

        try {
            setContent {
                SimpleScreen()
            }
            android.util.Log.d("MainActivity", "=== setContent SUCCESS ===")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "=== CRASH IN SETCONTENT ===", e)
            android.widget.Toast.makeText(
                this,
                "Error: ${e.message}",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    @Composable
    fun SimpleScreen() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF4CAF50)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "✅ Trash2Cash",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "App is running!",
                    fontSize = 18.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "If you see this, the app works!",
                    fontSize = 14.sp,
                    color = Color.White
                )
            }
        }
    }
}