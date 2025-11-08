package com.trash2cash.app.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trash2cash.app.data.UserRole
import com.trash2cash.app.viewmodel.AuthViewModel
import com.trash2cash.app.viewmodel.LoginFormState
import com.trash2cash.app.viewmodel.RegistrationField
import com.trash2cash.app.viewmodel.RegistrationFormState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandingScreen(
    authViewModel: AuthViewModel,
    onNavigateToApp: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        // Clean navigation - show only ONE screen based on currentScreen state
        when (uiState.currentScreen) {
            com.trash2cash.app.viewmodel.AuthScreen.WELCOME -> {
                WelcomeScreen(
                    onGetStarted = { authViewModel.showRoleSelection() }
                )
            }

            com.trash2cash.app.viewmodel.AuthScreen.ROLE_SELECTION -> {
                RoleSelectionScreen(
                    onRoleSelected = { role ->
                        authViewModel.updateLoginRole(role)
                        authViewModel.showLogin()
                    },
                    onBack = { authViewModel.hideRoleSelection() }
                )
            }

            com.trash2cash.app.viewmodel.AuthScreen.LOGIN -> {
                LoginScreen(
                    authViewModel = authViewModel,
                    onNavigateToRegistration = { authViewModel.showRegistration() },
                    onBack = { authViewModel.showRoleSelection() },
                    onLoginSuccess = onNavigateToApp
                )
            }

            com.trash2cash.app.viewmodel.AuthScreen.REGISTRATION -> {
                RegistrationScreen(
                    authViewModel = authViewModel,
                    onNavigateToLogin = { authViewModel.showLogin() },
                    onBack = { authViewModel.showRoleSelection() }
                )
            }
        }
    }
}

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Logo - Using actual launcher icon
        Image(
            painter = painterResource(id = com.trash2cash.app.R.mipmap.ic_launcher),
            contentDescription = "Trash2Cash Logo",
            modifier = Modifier.size(120.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // App Title
        Text(
            text = "Trash2Cash",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Tagline
        Text(
            text = "Digital Waste Reward Wallet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Description
        Text(
            text = "Turn your waste collection efforts into rewards!\nEarn points for every verified plastic waste submission.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Get Started Button
        Button(
            onClick = onGetStarted,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Get Started",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Features preview
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FeatureCard(
                icon = Icons.Default.CameraAlt,
                title = "Capture",
                description = "Photo waste"
            )
            FeatureCard(
                icon = Icons.Default.Verified,
                title = "Verify",
                description = "AI checks"
            )
            FeatureCard(
                icon = Icons.Default.Stars,
                title = "Earn",
                description = "Get points"
            )
            FeatureCard(
                icon = Icons.Default.CardGiftcard,
                title = "Redeem",
                description = "Use rewards"
            )
        }
    }
}

@Composable
fun FeatureCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(70.dp)
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
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Text(
            text = description,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleSelectionScreen(
    onRoleSelected: (UserRole) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Title
        Text(
            text = "Choose Your Role",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Select how you want to participate in the Trash2Cash ecosystem",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Role Cards
        RoleCard(
            icon = Icons.Default.Person,
            title = "Citizen",
            description = "Collect waste, earn rewards, and make an environmental impact",
            features = listOf(
                "📸 Submit waste photos",
                "💰 Earn reward points",
                "🏆 Join leaderboards",
                "🎁 Redeem vouchers"
            ),
            onClick = { onRoleSelected(UserRole.CITIZEN) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        RoleCard(
            icon = Icons.Default.VerifiedUser,
            title = "Municipal Worker",
            description = "Verify waste submissions and monitor environmental impact",
            features = listOf(
                "✅ Verify submissions",
                "📊 Track impact metrics",
                "🗺️ Monitor waste zones",
                "📈 Generate reports"
            ),
            onClick = { onRoleSelected(UserRole.MUNICIPAL_WORKER) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleCard(
    icon: ImageVector,
    title: String,
    description: String,
    features: List<String>,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = title,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Features list
            features.forEach { feature ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = feature,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onNavigateToRegistration: () -> Unit,
    onBack: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val loginState by authViewModel.loginState.collectAsState()
    val uiState by authViewModel.uiState.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }

    // Handle successful login
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            if (it.contains("Welcome back")) {
                kotlinx.coroutines.delay(1000)
                onLoginSuccess()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Title
        Text(
            text = "Welcome Back!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Sign in as ${
                loginState.selectedRole.name.lowercase().replaceFirstChar { it.uppercase() }
            }",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Email field
        OutlinedTextField(
            value = loginState.email,
            onValueChange = { authViewModel.updateLoginEmail(it) },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            isError = loginState.emailError != null,
            supportingText = loginState.emailError?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password field
        OutlinedTextField(
            value = loginState.password,
            onValueChange = { authViewModel.updateLoginPassword(it) },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            isError = loginState.passwordError != null,
            supportingText = loginState.passwordError?.let { { Text(it) } },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true
        )

        // Login error
        loginState.loginError?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Login button
        Button(
            onClick = { authViewModel.login() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !loginState.isLoading,
            shape = RoundedCornerShape(16.dp)
        ) {
            if (loginState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    text = "Sign In",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Demo credentials
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Demo Credentials",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                val (email, password) = authViewModel.getDemoCredentials(loginState.selectedRole)
                Text(
                    text = "Email: $email\nPassword: $password",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Sign up link
        Row {
            Text(
                text = "Don't have an account? ",
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(onClick = onNavigateToRegistration) {
                Text(
                    text = "Sign Up",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    authViewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onBack: () -> Unit
) {
    val registrationState by authViewModel.registrationState.collectAsState()
    val uiState by authViewModel.uiState.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Handle successful registration - redirect to login
    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null && uiState.successMessage!!.contains("Registration")) {
            kotlinx.coroutines.delay(1500) // Show success message briefly
            authViewModel.clearMessages()
            onNavigateToLogin()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Back button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // App Logo/Icon with recycling symbol
            Spacer(modifier = Modifier.height(16.dp))

            // App Logo - Using actual launcher icon
            Image(
                painter = painterResource(id = com.trash2cash.app.R.mipmap.ic_launcher),
                contentDescription = "Trash2Cash Logo",
                modifier = Modifier.size(100.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = "Create Account",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "Join as ${
                    if (registrationState.selectedRole == UserRole.CITIZEN) "Citizen"
                    else "Municipal Worker"
                }",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Branding text
            Text(
                text = "Trash2Cash",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Digital Waste Reward Wallet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline
            Text(
                text = "Turn your waste collection efforts into rewards!\nEarn points for every verified plastic waste submission.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Name field
            OutlinedTextField(
                value = registrationState.name,
                onValueChange = {
                    authViewModel.updateRegistrationField(
                        RegistrationField.NAME,
                        it
                    )
                },
                label = { Text("Full Name") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                isError = registrationState.nameError != null,
                supportingText = registrationState.nameError?.let { { Text(it) } },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Email field
            OutlinedTextField(
                value = registrationState.email,
                onValueChange = {
                    authViewModel.updateRegistrationField(
                        RegistrationField.EMAIL,
                        it
                    )
                },
                label = { Text("Email") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                isError = registrationState.emailError != null,
                supportingText = registrationState.emailError?.let { { Text(it) } },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Phone field
            OutlinedTextField(
                value = registrationState.phoneNumber,
                onValueChange = {
                    authViewModel.updateRegistrationField(
                        RegistrationField.PHONE,
                        it
                    )
                },
                label = { Text("Phone Number") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                isError = registrationState.phoneError != null,
                supportingText = registrationState.phoneError?.let { { Text(it) } },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Municipal worker specific fields
            if (registrationState.selectedRole == UserRole.MUNICIPAL_WORKER) {
                OutlinedTextField(
                    value = registrationState.employeeId,
                    onValueChange = {
                        authViewModel.updateRegistrationField(
                            RegistrationField.EMPLOYEE_ID,
                            it
                        )
                    },
                    label = { Text("Employee ID") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Badge,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    isError = registrationState.employeeIdError != null,
                    supportingText = registrationState.employeeIdError?.let { { Text(it) } },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = registrationState.department,
                    onValueChange = {
                        authViewModel.updateRegistrationField(
                            RegistrationField.DEPARTMENT,
                            it
                        )
                    },
                    label = { Text("Department") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Business,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    isError = registrationState.departmentError != null,
                    supportingText = registrationState.departmentError?.let { { Text(it) } },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = registrationState.designation,
                    onValueChange = {
                        authViewModel.updateRegistrationField(
                            RegistrationField.DESIGNATION,
                            it
                        )
                    },
                    label = { Text("Designation") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Work,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    isError = registrationState.designationError != null,
                    supportingText = registrationState.designationError?.let { { Text(it) } },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Password field
            OutlinedTextField(
                value = registrationState.password,
                onValueChange = {
                    authViewModel.updateRegistrationField(
                        RegistrationField.PASSWORD,
                        it
                    )
                },
                label = { Text("Password") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                isError = registrationState.passwordError != null,
                supportingText = registrationState.passwordError?.let { { Text(it) } },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                shape = RoundedCornerShape(12.dp),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Confirm Password field
            OutlinedTextField(
                value = registrationState.confirmPassword,
                onValueChange = {
                    authViewModel.updateRegistrationField(
                        RegistrationField.CONFIRM_PASSWORD,
                        it
                    )
                },
                label = { Text("Confirm Password") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                isError = registrationState.confirmPasswordError != null,
                supportingText = registrationState.confirmPasswordError?.let { { Text(it) } },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                shape = RoundedCornerShape(12.dp),
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true
            )

            // Registration error
            registrationState.registrationError?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Success message
            uiState.successMessage?.let { message ->
                if (message.contains("Registration")) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Register button
            Button(
                onClick = { authViewModel.register() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !registrationState.isLoading,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (registrationState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Get Started",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sign in link
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = onNavigateToLogin) {
                    Text(
                        text = "Sign In",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Feature highlights at bottom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FeatureHighlight(
                    icon = Icons.Default.CameraAlt,
                    title = "Capture",
                    subtitle = "Photo waste"
                )
                FeatureHighlight(
                    icon = Icons.Default.Verified,
                    title = "Verify",
                    subtitle = "AI checks"
                )
                FeatureHighlight(
                    icon = Icons.Default.Stars,
                    title = "Earn",
                    subtitle = "Get points"
                )
                FeatureHighlight(
                    icon = Icons.Default.CardGiftcard,
                    title = "Redeem",
                    subtitle = "Use rewards"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun FeatureHighlight(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(70.dp)
    ) {
        Icon(
            icon,
            contentDescription = title,
            modifier = Modifier.size(28.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            fontSize = 10.sp
        )
    }
}

@Composable
fun MessageCard(
    message: String,
    isError: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
        )
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            }
        )
    }
}