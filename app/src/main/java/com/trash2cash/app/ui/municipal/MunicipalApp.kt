package com.trash2cash.app.ui.municipal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.trash2cash.app.data.*
import com.trash2cash.app.viewmodel.Trash2CashViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MunicipalApp(
    user: User,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: Trash2CashViewModel = viewModel { Trash2CashViewModel(context) }

    var currentTab by remember { mutableStateOf(MunicipalTab.VERIFY) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Municipal Dashboard", fontWeight = FontWeight.Bold)
                },
                actions = {
                    // Status indicator
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.VerifiedUser,
                                contentDescription = "Verified",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = user.name,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                MunicipalTab.values().forEach { tab ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                getMunicipalTabIcon(tab),
                                contentDescription = tab.name,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text(getMunicipalTabLabel(tab)) },
                        selected = currentTab == tab,
                        onClick = { currentTab = tab }
                    )
                }
            }
        }
    ) { paddingValues ->
        when (currentTab) {
            MunicipalTab.VERIFY -> MunicipalVerifyScreen(viewModel, user, paddingValues)
            MunicipalTab.IMPACT -> MunicipalImpactScreen(viewModel, user, paddingValues)
            MunicipalTab.MAP -> MunicipalMapScreen(viewModel, user, paddingValues)
            MunicipalTab.PROFILE -> MunicipalProfileScreen(viewModel, user, paddingValues)
        }
    }
}

@Composable
fun MunicipalVerifyScreen(
    viewModel: Trash2CashViewModel,
    user: User,
    paddingValues: PaddingValues
) {
    val pendingSubmissions by viewModel.pendingVerifications.collectAsState(initial = emptyList())
    val municipalStats by viewModel.municipalStats.collectAsState(initial = null)

    var selectedSubmission by remember { mutableStateOf<WasteSubmission?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stats header
        item {
            MunicipalStatsCard(stats = municipalStats, user = user)
        }

        item {
            Text(
                text = "🔍 Pending Verifications",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        if (pendingSubmissions.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.CheckCircle,
                    title = "All caught up!",
                    description = "No pending submissions to verify at the moment."
                )
            }
        } else {
            items(pendingSubmissions) { submission ->
                VerificationSubmissionCard(
                    submission = submission,
                    onVerify = { selectedSubmission = submission }
                )
            }
        }
    }

    // Verification dialog
    selectedSubmission?.let { submission ->
        VerificationDialog(
            submission = submission,
            onDismiss = { selectedSubmission = null },
            onApprove = { wasteType, weight, comments ->
                viewModel.verifyWasteSubmission(
                    submissionId = submission.id,
                    isApproved = true,
                    actualWasteType = wasteType,
                    actualWeight = weight,
                    comments = comments,
                    rejectionReason = ""
                )
                selectedSubmission = null
            },
            onReject = { reason, comments ->
                viewModel.verifyWasteSubmission(
                    submissionId = submission.id,
                    isApproved = false,
                    actualWasteType = "",
                    actualWeight = 0f,
                    comments = comments,
                    rejectionReason = reason
                )
                selectedSubmission = null
            }
        )
    }
}

@Composable
fun MunicipalImpactScreen(
    viewModel: Trash2CashViewModel,
    user: User,
    paddingValues: PaddingValues
) {
    val globalStats by viewModel.globalStats.collectAsState(initial = null)
    val verifiedSubmissions by viewModel.getSubmissionsVerifiedBy(user.id)
        .collectAsState(initial = emptyList())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "📊 Impact Analytics",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            globalStats?.let { stats ->
                ImpactAnalyticsCard(stats = stats)
            }
        }

        item {
            Text(
                text = "Your Recent Verifications",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        items(verifiedSubmissions.take(10)) { submission ->
            VerifiedSubmissionCard(submission = submission)
        }
    }
}

@Composable
fun MunicipalMapScreen(
    viewModel: Trash2CashViewModel,
    user: User,
    paddingValues: PaddingValues
) {
    val assignedZones by viewModel.getMunicipalAssignedZones().collectAsState(initial = emptyList())
    val allZones by viewModel.allZones.collectAsState(initial = emptyList())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "🗺️ Waste Zone Monitoring",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "🌍 Interactive Map & Heatmap",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Real-time waste hotspot tracking with exact locations and submission counts coming in next version.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        if (assignedZones.isNotEmpty()) {
            item {
                Text(
                    text = "Your Assigned Zones",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            items(assignedZones) { zone ->
                MunicipalZoneCard(zone = zone, isAssigned = true)
            }
        }

        item {
            Text(
                text = "All Waste Zones",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        items(allZones.sortedByDescending { it.totalWasteCollected }) { zone ->
            MunicipalZoneCard(zone = zone, isAssigned = false)
        }
    }
}

@Composable
fun MunicipalProfileScreen(
    viewModel: Trash2CashViewModel,
    user: User,
    paddingValues: PaddingValues
) {
    val municipalStats by viewModel.municipalStats.collectAsState(initial = null)
    val municipalChallenges by viewModel.getMunicipalChallenges()
        .collectAsState(initial = emptyList())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile header
        item {
            MunicipalProfileCard(user = user, stats = municipalStats)
        }

        // Performance metrics
        item {
            MunicipalPerformanceCard(stats = municipalStats)
        }

        // Municipal challenges
        item {
            Text(
                text = "🎯 Department Challenges",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        items(municipalChallenges) { challenge ->
            MunicipalChallengeCard(challenge = challenge)
        }
    }
}

// Municipal-specific composables

@Composable
fun MunicipalStatsCard(stats: MunicipalStats?, user: User) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Verification Dashboard",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${stats?.pendingVerifications ?: 0}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text("Pending", style = MaterialTheme.typography.bodySmall)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${stats?.verifiedToday ?: 0}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("Today", style = MaterialTheme.typography.bodySmall)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${stats?.totalVerifications ?: 0}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Total", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationSubmissionCard(
    submission: WasteSubmission,
    onVerify: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onVerify
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Actual Image from submission
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (submission.imageUri.isNotEmpty()) {
                        androidx.compose.foundation.Image(
                            painter = rememberAsyncImagePainter(
                                model = android.net.Uri.parse(submission.imageUri)
                            ),
                            contentDescription = "Waste Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = "No Photo",
                            modifier = Modifier
                                .size(40.dp)
                                .align(Alignment.Center),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Submission #${submission.id.take(8)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = submission.locationName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Submitted: ${
                            SimpleDateFormat(
                                "MMM dd, HH:mm",
                                Locale.getDefault()
                            ).format(submission.timestamp)
                        }",
                        style = MaterialTheme.typography.bodySmall
                    )

                    // AI Analysis results
                    if (submission.aiDetectedWasteType.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "AI: ${submission.aiDetectedWasteType} (${
                                String.format(
                                    "%.2f",
                                    submission.aiEstimatedWeight
                                )
                            }kg)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Confidence: ${(submission.aiConfidenceScore * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = getStatusEmoji(submission.verificationStatus),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onVerify,
                        modifier = Modifier.width(80.dp)
                    ) {
                        Text("Verify")
                    }
                }
            }

            if (submission.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Note: ${submission.description}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationDialog(
    submission: WasteSubmission,
    onDismiss: () -> Unit,
    onApprove: (String, Float, String) -> Unit,
    onReject: (String, String) -> Unit
) {
    var wasteType by remember { mutableStateOf(submission.aiDetectedWasteType) }
    var weight by remember { mutableStateOf(submission.aiEstimatedWeight.toString()) }
    var comments by remember { mutableStateOf("") }
    var rejectionReason by remember { mutableStateOf("") }
    var showRejectDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Verify Submission",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Show the actual submitted image
                item {
                    if (submission.imageUri.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp)
                            ) {
                                androidx.compose.foundation.Image(
                                    painter = rememberAsyncImagePainter(
                                        model = android.net.Uri.parse(submission.imageUri)
                                    ),
                                    contentDescription = "Submitted Waste Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                )
                            }
                        }
                    }
                }

                item {
                    Column {
                        Text(
                            text = "Location: ${submission.locationName}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        if (submission.aiDetectedWasteType.isNotEmpty()) {
                            Text(
                                text = "AI Detected: ${submission.aiDetectedWasteType} (${
                                    String.format(
                                        "%.2f",
                                        submission.aiEstimatedWeight
                                    )
                                }kg)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = "AI Confidence: ${(submission.aiConfidenceScore * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }

                item {
                    Divider()
                }

                item {
                    Text(
                        text = "Adjust Values (if needed)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    OutlinedTextField(
                        value = wasteType,
                        onValueChange = { wasteType = it },
                        label = { Text("Actual Waste Type *") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g., Bottle, Bag, Container") },
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text("Actual Weight (kg) *") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g., 0.5") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        ),
                        supportingText = {
                            val w = weight.toFloatOrNull() ?: 0f
                            if (w > 0) {
                                val points = calculatePoints(w)
                                Text(
                                    text = "Points to be awarded: $points",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = comments,
                        onValueChange = { comments = it },
                        label = { Text("Comments (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Add any notes about this submission") },
                        maxLines = 3
                    )
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(
                    onClick = { showRejectDialog = true },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Reject")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        val actualWeight = weight.toFloatOrNull() ?: 0f
                        if (wasteType.isNotBlank() && actualWeight > 0) {
                            onApprove(wasteType, actualWeight, comments)
                        }
                    },
                    enabled = wasteType.isNotBlank() && weight.toFloatOrNull() != null && weight.toFloat() > 0
                ) {
                    Text("Approve")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // Rejection dialog
    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Reject Submission") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Please provide a reason for rejection:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        label = { Text("Rejection Reason") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g., Not plastic waste, unclear image, duplicate submission") },
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rejectionReason.isNotBlank()) {
                            onReject(rejectionReason, comments)
                            showRejectDialog = false
                        }
                    },
                    enabled = rejectionReason.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Reject")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ImpactAnalyticsCard(stats: GlobalStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "🌍 Environmental Impact",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ImpactMetric(
                    title = "Plastic Removed",
                    value = "${String.format("%.1f", stats.totalPlasticCollected)} kg",
                    icon = Icons.Default.Delete
                )
                ImpactMetric(
                    title = "CO₂ Saved",
                    value = "${String.format("%.1f", stats.totalCO2Reduced)} kg",
                    icon = Icons.Default.Eco
                )
                ImpactMetric(
                    title = "Citizens",
                    value = "${stats.totalUsers}",
                    icon = Icons.Default.People
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Today's Activity",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${stats.todaySubmissions} submissions",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun ImpactMetric(
    title: String,
    value: String,
    icon: ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = title,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun MunicipalZoneCard(zone: Zone, isAssigned: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (isAssigned) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = zone.name + if (isAssigned) " (Assigned)" else "",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${zone.city}, ${zone.state}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = zone.description,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${zone.totalSubmissions}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("submissions", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Text(
                    text = "Waste Level: ",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = zone.wasteLevel.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = when (zone.wasteLevel) {
                        WasteLevel.LOW -> Color.Green
                        WasteLevel.MEDIUM -> Color(0xFFFFA500) // Orange
                        WasteLevel.HIGH -> Color.Red
                        WasteLevel.CRITICAL -> Color.Magenta
                    }
                )
            }
        }
    }
}

@Composable
fun VerifiedSubmissionCard(submission: WasteSubmission) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (submission.verificationStatus == VerificationStatus.VERIFIED)
                    Icons.Default.CheckCircle
                else
                    Icons.Default.Cancel,
                contentDescription = "Status",
                tint = if (submission.verificationStatus == VerificationStatus.VERIFIED)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${submission.actualWasteType} (${
                        String.format(
                            "%.2f",
                            submission.actualWeight
                        )
                    }kg)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = submission.locationName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Verified: ${
                        SimpleDateFormat(
                            "MMM dd, HH:mm",
                            Locale.getDefault()
                        ).format(submission.verifiedAt)
                    }",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (submission.verificationStatus == VerificationStatus.VERIFIED) "✅" else "❌",
                    style = MaterialTheme.typography.titleMedium
                )
                if (submission.rewardPoints > 0) {
                    Text(
                        text = "${submission.rewardPoints} pts",
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
fun MunicipalProfileCard(user: User, stats: MunicipalStats?) {
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
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.name.take(1),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Municipal Verification Officer",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MunicipalPerformanceCard(stats: MunicipalStats?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "📈 Performance Metrics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PerformanceMetric(
                    title = "This Week",
                    value = "${stats?.verifiedThisWeek ?: 0}",
                    subtitle = "verifications"
                )
                PerformanceMetric(
                    title = "Accuracy",
                    value = "${String.format("%.1f", stats?.verificationAccuracy ?: 0f)}%",
                    subtitle = "success rate"
                )
                PerformanceMetric(
                    title = "Avg Time",
                    value = "${stats?.avgVerificationTime ?: 0}m",
                    subtitle = "per verification"
                )
            }
        }
    }
}

@Composable
fun PerformanceMetric(
    title: String,
    value: String,
    subtitle: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MunicipalChallengeCard(challenge: Challenge) {
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
            Text(
                text = "Department Reward: ${challenge.rewardPoints} recognition points",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    description: String
) {
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
                icon,
                contentDescription = title,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Helper functions
private fun getMunicipalTabIcon(tab: MunicipalTab): ImageVector {
    return when (tab) {
        MunicipalTab.VERIFY -> Icons.Default.VerifiedUser
        MunicipalTab.IMPACT -> Icons.Default.Analytics
        MunicipalTab.MAP -> Icons.Default.Map
        MunicipalTab.PROFILE -> Icons.Default.Person
    }
}

private fun getMunicipalTabLabel(tab: MunicipalTab): String {
    return when (tab) {
        MunicipalTab.VERIFY -> "Verify"
        MunicipalTab.IMPACT -> "Impact"
        MunicipalTab.MAP -> "Map"
        MunicipalTab.PROFILE -> "Profile"
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

enum class MunicipalTab {
    VERIFY, IMPACT, MAP, PROFILE
}

// Helper function to calculate points based on weight
private fun calculatePoints(weight: Float): Int {
    // Simple calculation: 10 points per kg
    return (weight * 10).toInt().coerceAtLeast(1) // Minimum 1 point
}