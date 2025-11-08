package com.trash2cash.app.ui.citizen

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.trash2cash.app.data.*
import com.trash2cash.app.ui.camera.CameraActivity
import com.trash2cash.app.viewmodel.Trash2CashViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitizenApp(
    user: User,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: Trash2CashViewModel = viewModel { Trash2CashViewModel(context) }

    var currentTab by remember { mutableStateOf(CitizenTab.DASHBOARD) }
    var showSubmissionDialog by remember { mutableStateOf(false) }
    var submissionMessage by remember { mutableStateOf("") }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var locationName by remember { mutableStateOf("Current Location") }
    var showFullSubmissions by remember { mutableStateOf(false) }
    var userEstimatedWeight by remember { mutableStateOf("") }
    var showWeightVerificationDialog by remember { mutableStateOf(false) }
    var aiEstimatedWeight by remember { mutableStateOf(0f) }
    var finalLocationName by remember { mutableStateOf("") }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var showLeaderboard by remember { mutableStateOf(false) }
    var showChallenges by remember { mutableStateOf(false) }
    var isDarkTheme by remember { mutableStateOf(false) }

    // Observe UI state for submission status
    val uiState by viewModel.uiState.collectAsState()

    // Show dialog when submission completes
    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { message ->
            if (message.contains("Submission received") || message.contains("AI analysis") || message.contains(
                    "Verified"
                )
            ) {
                submissionMessage = message
                showSubmissionDialog = true
                viewModel.clearStatusMessage()
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val imageUri = result.data?.getParcelableExtra<Uri>(CameraActivity.EXTRA_IMAGE_URI)
            imageUri?.let { uri ->
                // Automatically get location and show dialog
                capturedImageUri = uri
                // TODO: Get actual GPS location
                locationName = "Current Location (GPS: 28.6139, 77.2090)"
                userEstimatedWeight = ""
                showLocationDialog = true
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Automatically get location and show dialog
            capturedImageUri = it
            locationName = "Current Location (GPS: 28.6139, 77.2090)"
            userEstimatedWeight = ""
            showLocationDialog = true
        }
    }

    fun launchCamera() {
        try {
            val intent = CameraActivity.newIntent(context)
            cameraLauncher.launch(intent)
        } catch (e: Exception) {
            android.util.Log.e("CitizenApp", "Failed to launch camera", e)
            android.widget.Toast.makeText(
                context,
                "Failed to open camera: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun launchGallery() {
        galleryLauncher.launch("image/*")
    }

    fun showImageSourcePicker() {
        showImageSourceDialog = true
    }

    fun proceedToAIVerification() {
        // Store location for later use
        finalLocationName = locationName

        // Simulate AI verification of weight
        val userWeight = userEstimatedWeight.toFloatOrNull() ?: 0f

        // Simulate AI estimation (AI estimates ±20% of user's estimate)
        val variation = (Math.random() * 0.4 - 0.2).toFloat() // -20% to +20%
        aiEstimatedWeight = if (userWeight > 0) {
            (userWeight * (1 + variation)).coerceAtLeast(0.1f)
        } else {
            // If user didn't enter weight, AI estimates between 0.1 and 2.0 kg
            (0.1f + Math.random() * 1.9).toFloat()
        }

        showLocationDialog = false
        showWeightVerificationDialog = true
    }

    fun submitPhoto(uri: Uri, location: String, weight: Float) {
        viewModel.submitWastePhoto(
            imageUri = uri.toString(),
            latitude = 28.6139, // TODO: Get from location service
            longitude = 77.2090, // TODO: Get from location service
            locationName = location,
            description = "User estimated: ${userEstimatedWeight.ifEmpty { "Not provided" }} kg"
        )
    }

    // Location Confirmation Dialog
    if (showLocationDialog && capturedImageUri != null) {
        AlertDialog(
            onDismissRequest = {
                showLocationDialog = false
                capturedImageUri = null
            },
            icon = {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "Waste Details",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "📍 Location Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    item {
                        Text(
                            text = "Location automatically detected",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "You can edit the location name below:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = locationName,
                            onValueChange = { locationName = it },
                            label = { Text("Location Name") },
                            placeholder = { Text("e.g., City Park, Beach, Street") },
                            leadingIcon = {
                                Icon(Icons.Default.LocationOn, contentDescription = null)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    item {
                        // GPS Coordinates Card with Use GPS button
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.MyLocation,
                                        contentDescription = "GPS",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "GPS: 28.6139, 77.2090",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "New Delhi, India",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Use GPS Button
                                Button(
                                    onClick = {
                                        // Update location to current GPS
                                        // TODO: Get actual GPS coordinates using FusedLocationProvider
                                        locationName = "Current Location (GPS: 28.6139, 77.2090)"
                                        android.widget.Toast.makeText(
                                            context,
                                            "Location updated to current GPS coordinates",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    modifier = Modifier.height(36.dp),
                                    contentPadding = PaddingValues(
                                        horizontal = 12.dp,
                                        vertical = 8.dp
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.GpsFixed,
                                        contentDescription = "Use GPS",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Use GPS",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    item {
                        Text(
                            text = "⚖️ Estimated Weight (Optional)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    item {
                        Text(
                            text = "Enter your estimated weight of the waste collected. AI will verify and suggest adjustments.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = userEstimatedWeight,
                            onValueChange = { userEstimatedWeight = it },
                            label = { Text("Estimated Weight (kg)") },
                            placeholder = { Text("e.g., 0.5, 1.0, 2.5") },
                            leadingIcon = {
                                Icon(Icons.Default.Scale, contentDescription = null)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                            ),
                            singleLine = true,
                            supportingText = {
                                if (userEstimatedWeight.isNotEmpty()) {
                                    val weight = userEstimatedWeight.toFloatOrNull()
                                    if (weight != null && weight > 0) {
                                        Text(
                                            text = "Estimated points: ${(weight * 10).toInt()} pts",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Text(
                                            text = "Please enter a valid weight",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                } else {
                                    Text("AI will estimate if not provided")
                                }
                            }
                        )
                    }

                    item {
                        // Info text
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(
                                    alpha = 0.5f
                                )
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = "Info",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "AI will verify your estimate and you can choose which value to submit",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        proceedToAIVerification()
                    },
                    enabled = locationName.isNotBlank()
                ) {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Verify with AI")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showLocationDialog = false
                    capturedImageUri = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // AI Weight Verification Dialog
    if (showWeightVerificationDialog && capturedImageUri != null) {
        AlertDialog(
            onDismissRequest = { },
            icon = {
                Icon(
                    Icons.Default.Psychology,
                    contentDescription = "AI",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "AI Weight Verification",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "AI has analyzed your waste photo. Choose which weight estimate to submit:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // User's Estimate Card
                    Card(
                        onClick = {
                            val userWeight =
                                userEstimatedWeight.toFloatOrNull() ?: aiEstimatedWeight
                            capturedImageUri?.let { uri ->
                                submitPhoto(uri, finalLocationName, userWeight)
                            }
                            showWeightVerificationDialog = false
                            capturedImageUri = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (userEstimatedWeight.isNotEmpty())
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                        enabled = userEstimatedWeight.isNotEmpty()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Your Estimate",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (userEstimatedWeight.isNotEmpty()) {
                                        val userWeight = userEstimatedWeight.toFloatOrNull() ?: 0f
                                        Text(
                                            text = "${String.format("%.2f", userWeight)} kg",
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Points: ${(userWeight * 10).toInt()} pts",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    } else {
                                        Text(
                                            text = "Not provided",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (userEstimatedWeight.isNotEmpty()) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = "User",
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    // AI's Estimate Card
                    Card(
                        onClick = {
                            capturedImageUri?.let { uri ->
                                submitPhoto(uri, finalLocationName, aiEstimatedWeight)
                            }
                            showWeightVerificationDialog = false
                            capturedImageUri = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "AI Estimate",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = "Recommended",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    Text(
                                        text = "${String.format("%.2f", aiEstimatedWeight)} kg",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = "Points: ${(aiEstimatedWeight * 10).toInt()} pts",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Confidence: ${(85 + Math.random() * 10).toInt()}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    Icons.Default.Psychology,
                                    contentDescription = "AI",
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }

                    // Comparison Info
                    if (userEstimatedWeight.isNotEmpty()) {
                        val userWeight = userEstimatedWeight.toFloatOrNull() ?: 0f
                        val difference = aiEstimatedWeight - userWeight
                        val percentDiff =
                            if (userWeight > 0) (difference / userWeight * 100) else 0f

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(
                                    alpha = 0.5f
                                )
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CompareArrows,
                                    contentDescription = "Difference",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (difference > 0)
                                            "AI suggests ${
                                                String.format(
                                                    "%.2f",
                                                    difference
                                                )
                                            } kg more"
                                        else
                                            "AI suggests ${
                                                String.format(
                                                    "%.2f",
                                                    -difference
                                                )
                                            } kg less",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "(${
                                            String.format(
                                                "%.0f",
                                                percentDiff
                                            )
                                        }% difference)",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showWeightVerificationDialog = false
                    showLocationDialog = true
                }) {
                    Text("Go Back")
                }
            }
        )
    }

    // Success Dialog
    if (showSubmissionDialog) {
        AlertDialog(
            onDismissRequest = { showSubmissionDialog = false },
            icon = {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "Submission Successful!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(submissionMessage)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "The photo has been sent to the nearest municipal worker for verification.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    showSubmissionDialog = false
                    // Refresh data after submission
                    viewModel.refreshData()
                }) {
                    Text("Got it!")
                }
            }
        )
    }

    // Image Source Picker Dialog
    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            icon = {
                Icon(
                    Icons.Default.AddAPhoto,
                    contentDescription = "Upload",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text("Choose Image Source", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            showImageSourceDialog = false
                            launchCamera()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = "Camera", modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Take Photo")
                    }
                    Button(
                        onClick = {
                            showImageSourceDialog = false
                            launchGallery()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Collections, contentDescription = "Gallery", modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose from Gallery")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showImageSourceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Loading Dialog during AI processing
    if (uiState.isProcessingSubmission) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text("Processing...")
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("AI is analyzing your waste photo...")
                }
            },
            confirmButton = { }
        )
    }

    // Full Screen My Submissions
    if (showFullSubmissions) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("My Submissions") },
                        navigationIcon = {
                            IconButton(onClick = { showFullSubmissions = false }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { padding ->
                CitizenMySubmissions(
                    viewModel = viewModel,
                    user = user,
                    paddingValues = padding
                )
            }
        }
    } else if (showLeaderboard) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Leaderboard") },
                        navigationIcon = {
                            IconButton(onClick = { showLeaderboard = false }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { padding ->
                CitizenLeaderboard(
                    viewModel = viewModel,
                    user = user,
                    paddingValues = padding
                )
            }
        }
    } else if (showChallenges) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Challenges") },
                        navigationIcon = {
                            IconButton(onClick = { showChallenges = false }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { padding ->
                CitizenChallenges(
                    viewModel = viewModel,
                    user = user,
                    paddingValues = padding
                )
            }
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text("Trash2Cash", fontWeight = FontWeight.Bold)
                    },
                    actions = {
                        // Points display
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = "Points",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${user.totalPoints}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Theme Toggle with Emoji
                        IconButton(
                            onClick = { isDarkTheme = !isDarkTheme }
                        ) {
                            Text(
                                text = if (isDarkTheme) "☀️" else "🌙",
                                style = MaterialTheme.typography.headlineMedium,
                                fontSize = 24.sp
                            )
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    CitizenTab.values().forEach { tab ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    getCitizenTabIcon(tab),
                                    contentDescription = tab.name,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = { Text(getCitizenTabLabel(tab)) },
                            selected = currentTab == tab,
                            onClick = {
                                if (tab == CitizenTab.SUBMIT) {
                                    showImageSourcePicker()
                                } else {
                                    currentTab = tab
                                }

                                // Launch full-screen leaderboard/challenges views if needed
                                if (tab == CitizenTab.DASHBOARD) {
                                    // No-op
                                }
                                // NOTE: CitizenTab.LEADERBOARD and CitizenTab.CHALLENGES are handled via dedicated variables
                            }
                        )
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showImageSourcePicker() },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Capture Waste")
                }
            }
        ) { paddingValues ->
            when (currentTab) {
                CitizenTab.DASHBOARD -> CitizenDashboard(
                    viewModel,
                    user,
                    paddingValues,
                    onCameraClick = { showImageSourcePicker() },
                    onViewAllSubmissions = { showFullSubmissions = true },
                    onNavigateToWallet = { currentTab = CitizenTab.WALLET },
                    onNavigateToLeaderboard = { showLeaderboard = true },
                    onNavigateToChallenges = { showChallenges = true }
                )
                CitizenTab.SUBMIT -> {
                    // This is handled by image source dialog, show dashboard instead
                    CitizenDashboard(
                        viewModel,
                        user,
                        paddingValues,
                        onCameraClick = { showImageSourcePicker() },
                        onViewAllSubmissions = { showFullSubmissions = true },
                        onNavigateToWallet = { currentTab = CitizenTab.WALLET },
                        onNavigateToLeaderboard = { showLeaderboard = true },
                        onNavigateToChallenges = { showChallenges = true }
                    )
                }

                CitizenTab.WALLET -> CitizenWallet(viewModel, user, paddingValues)
                CitizenTab.PROFILE -> CitizenProfile(viewModel, user, paddingValues, onLogout)
            }
        }
    }
}

@Composable
fun CitizenDashboard(
    viewModel: Trash2CashViewModel,
    user: User,
    paddingValues: PaddingValues,
    onCameraClick: () -> Unit,
    onViewAllSubmissions: () -> Unit,
    onNavigateToWallet: () -> Unit,
    onNavigateToLeaderboard: () -> Unit = {},
    onNavigateToChallenges: () -> Unit = {}
) {
    val userStats by viewModel.userStats.collectAsState(initial = null)
    val globalStats by viewModel.globalStats.collectAsState(initial = null)
    val userSubmissions by viewModel.userSubmissions.collectAsState(initial = emptyList())

    // Auto-refresh data periodically to show real-time updates
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(5000) // Refresh every 5 seconds
            viewModel.refreshData()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome card
        item {
            WelcomeCard(user = user)
        }

        // Stats cards
        item {
            StatsRow(user = user, userStats = userStats)
        }

        // Quick actions
        item {
            QuickActionsSection(
                onCameraClick = onCameraClick,
                onViewHistory = onViewAllSubmissions,
                onRedeemPoints = onNavigateToWallet,
                onLeaderboardClick = onNavigateToLeaderboard,
                onChallengesClick = onNavigateToChallenges
            )
        }

        // My Submissions Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📋 My Submissions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onViewAllSubmissions) {
                    Text("View All →")
                }
            }
        }

        // Summary stats
        if (userSubmissions.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MiniSubmissionStat(
                        title = "Verified",
                        count = userSubmissions.count { it.verificationStatus == VerificationStatus.VERIFIED },
                        icon = Icons.Default.CheckCircle,
                        color = Color(0xFF4CAF50)
                    )
                    MiniSubmissionStat(
                        title = "Pending",
                        count = userSubmissions.count {
                            it.verificationStatus == VerificationStatus.PENDING ||
                                    it.verificationStatus == VerificationStatus.AI_PROCESSED
                        },
                        icon = Icons.Default.HourglassEmpty,
                        color = Color(0xFFFFA726)
                    )
                    MiniSubmissionStat(
                        title = "Rejected",
                        count = userSubmissions.count { it.verificationStatus == VerificationStatus.REJECTED },
                        icon = Icons.Default.Cancel,
                        color = Color(0xFFEF5350)
                    )
                }
            }
        }

        // Recent 3 submissions
        items(userSubmissions.sortedByDescending { it.timestamp }.take(3)) { submission ->
            SubmissionCard(submission = submission)
        }

        if (userSubmissions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "No submissions",
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Submissions Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap the camera button to get started!",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Global impact
        item {
            globalStats?.let { stats ->
                GlobalImpactCard(stats = stats)
            }
        }
    }
}

@Composable
fun CitizenWallet(
    viewModel: Trash2CashViewModel,
    user: User,
    paddingValues: PaddingValues
) {
    val activeVouchers by viewModel.activeVouchers.collectAsState(initial = emptyList())
    val userTransactions by viewModel.userTransactions.collectAsState(initial = emptyList())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            WalletBalanceCard(user = user)
        }

        item {
            Text(
                text = "Available Rewards",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        items(activeVouchers) { voucher ->
            VoucherCard(
                voucher = voucher,
                canRedeem = user.totalPoints >= voucher.pointsCost,
                onRedeem = { viewModel.redeemVoucher(voucher.id) }
            )
        }

        item {
            Text(
                text = "Transaction History",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        items(userTransactions.take(10)) { transaction ->
            TransactionCard(transaction = transaction)
        }
    }
}

@Composable
fun CitizenLeaderboard(
    viewModel: Trash2CashViewModel,
    user: User,
    paddingValues: PaddingValues
) {
    val leaderboard by viewModel.leaderboard.collectAsState(initial = emptyList())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🏆 Leaderboard",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { viewModel.refreshLeaderboard() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        }

        itemsIndexed(leaderboard) { index, entry ->
            LeaderboardCard(
                entry = entry,
                rank = index + 1,
                isCurrentUser = entry.citizenId == user.id
            )
        }
    }
}

@Composable
fun CitizenChallenges(
    viewModel: Trash2CashViewModel,
    user: User,
    paddingValues: PaddingValues
) {
    val activeChallenges by viewModel.activeChallenges.collectAsState(initial = emptyList())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "🎯 Active Challenges",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        items(activeChallenges.filter { it.type != ChallengeType.MUNICIPAL }) { challenge ->
            ChallengeCard(challenge = challenge)
        }
    }
}

@Composable
fun CitizenMySubmissions(
    viewModel: Trash2CashViewModel,
    user: User,
    paddingValues: PaddingValues
) {
    val userSubmissions by viewModel.userSubmissions.collectAsState(initial = emptyList())
    var selectedSubmission by remember { mutableStateOf<WasteSubmission?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📋 My Submissions",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${userSubmissions.size} total",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            // Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SubmissionStat(
                        title = "Verified",
                        count = userSubmissions.count { it.verificationStatus == VerificationStatus.VERIFIED },
                        icon = Icons.Default.CheckCircle,
                        color = Color(0xFF4CAF50)
                    )
                    SubmissionStat(
                        title = "Pending",
                        count = userSubmissions.count {
                            it.verificationStatus == VerificationStatus.PENDING ||
                                    it.verificationStatus == VerificationStatus.AI_PROCESSED
                        },
                        icon = Icons.Default.HourglassEmpty,
                        color = Color(0xFFFFA726)
                    )
                    SubmissionStat(
                        title = "Rejected",
                        count = userSubmissions.count { it.verificationStatus == VerificationStatus.REJECTED },
                        icon = Icons.Default.Cancel,
                        color = Color(0xFFEF5350)
                    )
                }
            }
        }

        if (userSubmissions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "No submissions",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Submissions Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Start collecting waste to earn points!",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(userSubmissions.sortedByDescending { it.timestamp }) { submission ->
                DetailedSubmissionCard(
                    submission = submission,
                    onClick = { selectedSubmission = submission }
                )
            }
        }
    }

    // Submission Detail Dialog
    selectedSubmission?.let { submission ->
        SubmissionDetailDialog(
            submission = submission,
            onDismiss = { selectedSubmission = null }
        )
    }
}

// Helper composables
@Composable
fun WelcomeCard(user: User) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Welcome back, ${user.name}! 🌱",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Ready to make an impact? Collect plastic waste and earn rewards!",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun StatsRow(user: User, userStats: CitizenStats?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatsCard(
            title = "Points",
            value = "${user.totalPoints}",
            icon = Icons.Default.Star,
            modifier = Modifier.weight(1f)
        )
        StatsCard(
            title = "Waste (kg)",
            value = String.format("%.1f", user.totalWasteCollected),
            icon = Icons.Default.Delete,
            modifier = Modifier.weight(1f)
        )
        StatsCard(
            title = "CO₂ Saved",
            value = String.format("%.1f", user.totalCO2Saved),
            icon = Icons.Default.Eco,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun QuickActionsSection(
    onCameraClick: () -> Unit,
    onViewHistory: () -> Unit,
    onRedeemPoints: () -> Unit,
    onLeaderboardClick: () -> Unit = {},
    onChallengesClick: () -> Unit = {}
) {
    Column {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                QuickActionButton(
                    title = "Capture Waste",
                    icon = Icons.Default.CameraAlt,
                    onClick = onCameraClick
                )
            }
            item {
                QuickActionButton(
                    title = "View History",
                    icon = Icons.Default.History,
                    onClick = onViewHistory
                )
            }
            item {
                QuickActionButton(
                    title = "Redeem Points",
                    icon = Icons.Default.CardGiftcard,
                    onClick = onRedeemPoints
                )
            }
            item {
                QuickActionButton(
                    title = "Leaderboard",
                    icon = Icons.Default.Leaderboard,
                    onClick = onLeaderboardClick
                )
            }
            item {
                QuickActionButton(
                    title = "Challenges",
                    icon = Icons.Default.Flag,
                    onClick = onChallengesClick
                )
            }
        }
    }
}

// Reusable components
@Composable
fun StatsCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = title,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun QuickActionButton(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(120.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = title,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SubmissionCard(submission: WasteSubmission) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Waste",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = submission.actualWasteType.ifEmpty { "Waste Collection" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = submission.locationName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = SimpleDateFormat(
                        "MMM dd, HH:mm",
                        Locale.getDefault()
                    ).format(submission.timestamp),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = getStatusEmoji(submission.verificationStatus),
                    style = MaterialTheme.typography.titleMedium
                )
                if (submission.rewardPoints > 0) {
                    Text(
                        text = "+${submission.rewardPoints}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun WalletBalanceCard(user: User) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.AccountBalanceWallet,
                contentDescription = "Wallet",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Your Balance",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "${user.totalPoints} Points",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun VoucherCard(
    voucher: Voucher,
    canRedeem: Boolean,
    onRedeem: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = voucher.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = voucher.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "By ${voucher.partnerName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${voucher.pointsCost} pts",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onRedeem,
                        enabled = canRedeem,
                        modifier = Modifier.width(100.dp)
                    ) {
                        Text("Redeem")
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionCard(transaction: RewardTransaction) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                when (transaction.type) {
                    TransactionType.EARNED -> Icons.Default.Add
                    TransactionType.REDEEMED -> Icons.Default.Remove
                    TransactionType.BONUS -> Icons.Default.Star
                    TransactionType.PENALTY -> Icons.Default.Remove
                    TransactionType.REFUND -> Icons.Default.Refresh
                },
                contentDescription = transaction.type.name,
                tint = if (transaction.points >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = SimpleDateFormat(
                        "MMM dd, HH:mm",
                        Locale.getDefault()
                    ).format(transaction.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                transaction.voucherCode?.let { code ->
                    Text(
                        text = "Code: $code",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = "${if (transaction.points >= 0) "+" else ""}${transaction.points}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (transaction.points >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun LeaderboardCard(
    entry: LeaderboardEntry,
    rank: Int,
    isCurrentUser: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (isCurrentUser) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when (rank) {
                            1 -> Color(0xFFFFD700) // Gold
                            2 -> Color(0xFFC0C0C0) // Silver
                            3 -> Color(0xFFCD7F32) // Bronze
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#$rank",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (rank <= 3) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.userName + if (isCurrentUser) " (You)" else "",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${
                        String.format(
                            "%.1f",
                            entry.wasteCollected
                        )
                    }kg collected • ${String.format("%.1f", entry.co2Saved)}kg CO₂ saved",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "${entry.points} pts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ChallengeCard(challenge: Challenge) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = challenge.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = challenge.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Reward: ${challenge.rewardPoints} pts",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Target: ${challenge.targetWaste}kg",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun GlobalImpactCard(stats: GlobalStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Global Impact 🌍",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${stats.totalUsers}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Users", style = MaterialTheme.typography.bodySmall)
                }
                Column {
                    Text(
                        text = String.format("%.1f kg", stats.totalPlasticCollected),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Plastic Removed", style = MaterialTheme.typography.bodySmall)
                }
                Column {
                    Text(
                        text = String.format("%.1f kg", stats.totalCO2Reduced),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text("CO₂ Saved", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

// Helper functions
private fun getCitizenTabIcon(tab: CitizenTab): ImageVector {
    return when (tab) {
        CitizenTab.DASHBOARD -> Icons.Default.Dashboard
        CitizenTab.SUBMIT -> Icons.Default.CameraAlt
        CitizenTab.WALLET -> Icons.Default.AccountBalanceWallet
        CitizenTab.PROFILE -> Icons.Default.Person
    }
}

private fun getCitizenTabLabel(tab: CitizenTab): String {
    return when (tab) {
        CitizenTab.DASHBOARD -> "Dashboard"
        CitizenTab.SUBMIT -> "Submit"
        CitizenTab.WALLET -> "Wallet"
        CitizenTab.PROFILE -> "Profile"
    }
}

private fun getStatusEmoji(status: VerificationStatus): String {
    return when (status) {
        VerificationStatus.VERIFIED -> "✅"
        VerificationStatus.REJECTED -> "❌"
        VerificationStatus.PENDING -> "⏳"
        VerificationStatus.AI_PROCESSED -> "🔄"
        VerificationStatus.DISPUTED -> "⚠️"
    }
}

enum class CitizenTab {
    DASHBOARD, SUBMIT, WALLET, PROFILE
}

@Composable
fun SubmissionStat(
    title: String,
    count: Int,
    icon: ImageVector,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = title,
            tint = color,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun MiniSubmissionStat(
    title: String,
    count: Int,
    icon: ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Icon(
            icon,
            contentDescription = title,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 11.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailedSubmissionCard(
    submission: WasteSubmission,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = when (submission.verificationStatus) {
                VerificationStatus.VERIFIED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                VerificationStatus.REJECTED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = getStatusEmoji(submission.verificationStatus),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = getStatusText(submission.verificationStatus),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
                            .format(submission.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "📍 ${submission.locationName}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (submission.rewardPoints > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "+${submission.rewardPoints}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "points",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))

            // Details based on status
            when (submission.verificationStatus) {
                VerificationStatus.VERIFIED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DetailItem(
                            label = "Waste Type",
                            value = submission.actualWasteType
                        )
                        DetailItem(
                            label = "Weight",
                            value = "${String.format("%.2f", submission.actualWeight)} kg"
                        )
                        DetailItem(
                            label = "CO₂ Saved",
                            value = "${String.format("%.2f", submission.impactScore)} kg"
                        )
                    }

                    if (submission.municipalComments.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "💬 ${submission.municipalComments}",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                VerificationStatus.PENDING, VerificationStatus.AI_PROCESSED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (submission.aiDetectedWasteType.isNotEmpty()) {
                            DetailItem(
                                label = "AI Detected",
                                value = submission.aiDetectedWasteType
                            )
                            DetailItem(
                                label = "Est. Weight",
                                value = "${String.format("%.2f", submission.aiEstimatedWeight)} kg"
                            )
                            DetailItem(
                                label = "Confidence",
                                value = "${(submission.aiConfidenceScore * 100).toInt()}%"
                            )
                        } else {
                            Text(
                                text = "⏳ Waiting for municipal verification...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                VerificationStatus.REJECTED -> {
                    Text(
                        text = "❌ Reason: ${submission.rejectionReason}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    if (submission.municipalComments.isNotEmpty()) {
                        Text(
                            text = "💬 ${submission.municipalComments}",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {}
            }
        }
    }
}

@Composable
fun DetailItem(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SubmissionDetailDialog(
    submission: WasteSubmission,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Submission Details", fontWeight = FontWeight.Bold)
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    // Photo
                    if (submission.imageUri.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Image(
                                painter = rememberAsyncImagePainter(
                                    model = android.net.Uri.parse(submission.imageUri),
                                    placeholder = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_gallery)
                                ),
                                contentDescription = "Waste Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                    }
                }

                item {
                    InfoRow("Status", getStatusText(submission.verificationStatus))
                }

                item {
                    InfoRow(
                        "Submitted On",
                        SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(
                            submission.timestamp
                        )
                    )
                }

                item {
                    InfoRow("Location", submission.locationName)
                }

                if (submission.verificationStatus == VerificationStatus.VERIFIED) {
                    item {
                        Divider()
                    }
                    item {
                        InfoRow("Waste Type", submission.actualWasteType)
                    }
                    item {
                        InfoRow("Weight", "${String.format("%.2f", submission.actualWeight)} kg")
                    }
                    item {
                        InfoRow("Points Earned", "${submission.rewardPoints} points")
                    }
                    item {
                        InfoRow("CO₂ Saved", "${String.format("%.2f", submission.impactScore)} kg")
                    }
                    if (submission.municipalComments.isNotEmpty()) {
                        item {
                            InfoRow("Comments", submission.municipalComments)
                        }
                    }
                } else if (submission.verificationStatus == VerificationStatus.REJECTED) {
                    item {
                        Divider()
                    }
                    item {
                        InfoRow("Rejection Reason", submission.rejectionReason)
                    }
                    if (submission.municipalComments.isNotEmpty()) {
                        item {
                            InfoRow("Comments", submission.municipalComments)
                        }
                    }
                } else {
                    if (submission.aiDetectedWasteType.isNotEmpty()) {
                        item {
                            Divider()
                        }
                        item {
                            InfoRow("AI Detected", submission.aiDetectedWasteType)
                        }
                        item {
                            InfoRow(
                                "Estimated Weight",
                                "${String.format("%.2f", submission.aiEstimatedWeight)} kg"
                            )
                        }
                        item {
                            InfoRow(
                                "Confidence",
                                "${(submission.aiConfidenceScore * 100).toInt()}%"
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        )
    }
}

private fun getStatusText(status: VerificationStatus): String {
    return when (status) {
        VerificationStatus.VERIFIED -> "Verified"
        VerificationStatus.REJECTED -> "Rejected"
        VerificationStatus.PENDING -> "Pending Verification"
        VerificationStatus.AI_PROCESSED -> "AI Processed"
        VerificationStatus.DISPUTED -> "Disputed"
    }
}

@Composable
fun CitizenProfile(
    viewModel: Trash2CashViewModel,
    user: User,
    paddingValues: PaddingValues,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val userSubmissions by viewModel.userSubmissions.collectAsState(initial = emptyList())

    fun shareApp() {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Join Trash2Cash!")
            putExtra(
                Intent.EXTRA_TEXT,
                """
                🌱 Join me on Trash2Cash - Digital Waste Reward Wallet!
                
                Turn your plastic waste into rewards! 
                
                📸 Capture waste
                ✅ Get verified
                💰 Earn points
                🎁 Redeem vouchers
                
                Download the app and start earning rewards for a cleaner planet!
                
                📥 Download APK: https://github.com/KS-2006-TD/Trash2Cash/releases/latest
                🌐 GitHub: https://github.com/KS-2006-TD/Trash2Cash
                
                #Trash2Cash #Sustainability #GreenLiving #WasteManagement
                """.trimIndent()
            )
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Trash2Cash via"))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile Header with Avatar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar Circle
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.name.take(2).uppercase(),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (user.phoneNumber.isNotEmpty()) {
                        Text(
                            text = user.phoneNumber,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Member since badge
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = "Member since",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Member since ${
                                    SimpleDateFormat(
                                        "MMM yyyy",
                                        Locale.getDefault()
                                    ).format(user.createdAt)
                                }",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        // Stats Overview
        item {
            Text(
                text = "📊 Your Impact",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileStatCard(
                    title = "Total Points",
                    value = "${user.totalPoints}",
                    icon = Icons.Default.Star,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                ProfileStatCard(
                    title = "Waste Collected",
                    value = "${String.format("%.1f", user.totalWasteCollected)} kg",
                    icon = Icons.Default.Delete,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileStatCard(
                    title = "CO₂ Saved",
                    value = "${String.format("%.1f", user.totalCO2Saved)} kg",
                    icon = Icons.Default.Eco,
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
                ProfileStatCard(
                    title = "Submissions",
                    value = "${userSubmissions.size}",
                    icon = Icons.Default.PhotoCamera,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Achievements Section
        item {
            Text(
                text = "🏆 Achievements",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AchievementItem(
                        icon = "🌱",
                        title = "Eco Warrior",
                        description = "Collected ${
                            String.format(
                                "%.1f",
                                user.totalWasteCollected
                            )
                        } kg of waste",
                        isUnlocked = user.totalWasteCollected >= 1
                    )
                    Divider()
                    AchievementItem(
                        icon = "⭐",
                        title = "Point Master",
                        description = "Earned ${user.totalPoints} points",
                        isUnlocked = user.totalPoints >= 100
                    )
                    Divider()
                    AchievementItem(
                        icon = "📸",
                        title = "Photographer",
                        description = "Submitted ${userSubmissions.size} photos",
                        isUnlocked = userSubmissions.size >= 5
                    )
                    Divider()
                    AchievementItem(
                        icon = "✅",
                        title = "Verified Pro",
                        description = "${userSubmissions.count { it.verificationStatus == VerificationStatus.VERIFIED }} verified submissions",
                        isUnlocked = userSubmissions.count { it.verificationStatus == VerificationStatus.VERIFIED } >= 3
                    )
                }
            }
        }

        // Share App Section
        item {
            Text(
                text = "💚 Spread the Word",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Share",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Share Trash2Cash",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Help your friends earn rewards while saving the planet!",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { shareApp() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share App")
                    }
                }
            }
        }

        // App Settings
        item {
            Text(
                text = "⚙️ Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column {
                    ProfileActionItem(
                        icon = Icons.Default.Edit,
                        title = "Edit Profile",
                        subtitle = "Update your information",
                        onClick = {
                            android.widget.Toast.makeText(
                                context,
                                "Coming soon!",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                    Divider()
                    ProfileActionItem(
                        icon = Icons.Default.Notifications,
                        title = "Notifications",
                        subtitle = "Manage notification preferences",
                        onClick = {
                            android.widget.Toast.makeText(
                                context,
                                "Coming soon!",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                    Divider()
                    ProfileActionItem(
                        icon = Icons.Default.Info,
                        title = "About",
                        subtitle = "App version 1.0.0",
                        onClick = {
                            android.widget.Toast.makeText(
                                context,
                                "Trash2Cash v1.0.0\nDigital Waste Reward Wallet",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    )
                    Divider()
                    ProfileActionItem(
                        icon = Icons.Default.Logout,
                        title = "Logout",
                        subtitle = "Sign out of your account",
                        onClick = onLogout,
                        isDestructive = true
                    )
                }
            }
        }

        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

}

@Composable
fun ProfileStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = title,
                modifier = Modifier.size(32.dp),
                tint = color
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun AchievementItem(
    icon: String,
    title: String,
    description: String,
    isUnlocked: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isUnlocked) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Unlocked",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Icon(
                Icons.Default.Lock,
                contentDescription = "Locked",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeOptionCard(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Select",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}