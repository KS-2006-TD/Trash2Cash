package com.trash2cash.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trash2cash.app.data.*
import com.trash2cash.app.repository.Trash2CashRepository
import com.trash2cash.app.services.AuthResult
import com.trash2cash.app.services.AuthState
import com.trash2cash.app.services.AuthenticationService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class AuthViewModel(context: Context) : ViewModel() {

    private val authService = AuthenticationService(context)
    private val repository = Trash2CashRepository(context)

    // Authentication State
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // UI State
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // Form States
    private val _loginState = MutableStateFlow(LoginFormState())
    val loginState: StateFlow<LoginFormState> = _loginState.asStateFlow()

    private val _registrationState = MutableStateFlow(RegistrationFormState())
    val registrationState: StateFlow<RegistrationFormState> = _registrationState.asStateFlow()

    init {
        checkAuthenticationStatus()
    }

    private fun checkAuthenticationStatus() {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                
                // For demo/hackathon: Always start at welcome screen
                // Comment this section and uncomment below to enable auto-login
                _authState.value = AuthState.NotAuthenticated
                
                /* Uncomment this for production with persistent login:
                if (authService.isLoggedIn() && !authService.isSessionExpired()) {
                    val userId = authService.getCurrentUserId()
                    if (userId != null) {
                        val user = repository.getUserById(userId)
                        if (user != null) {
                            authService.refreshSession()
                            _authState.value = AuthState.Authenticated(user)
                        } else {
                            authService.clearUserSession()
                            _authState.value = AuthState.NotAuthenticated
                        }
                    } else {
                        _authState.value = AuthState.NotAuthenticated
                    }
                } else {
                    if (authService.isSessionExpired()) {
                        authService.clearUserSession()
                    }
                    _authState.value = AuthState.NotAuthenticated
                }
                */
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Failed to check authentication: ${e.message}")
            }
        }
    }

    // Login Functions
    fun updateLoginEmail(email: String) {
        _loginState.value = _loginState.value.copy(
            email = email,
            emailError = null
        )
    }

    fun updateLoginPassword(password: String) {
        _loginState.value = _loginState.value.copy(
            password = password,
            passwordError = null
        )
    }

    fun updateLoginRole(role: UserRole) {
        _loginState.value = _loginState.value.copy(selectedRole = role)
    }

    fun login() {
        viewModelScope.launch {
            try {
                val currentState = _loginState.value

                // Validate inputs
                val emailValidation = authService.validateEmail(currentState.email)
                val passwordValidation = authService.validatePassword(currentState.password)

                val hasErrors = when (emailValidation) {
                    is com.trash2cash.app.services.ValidationResult.Error -> {
                        _loginState.value = currentState.copy(emailError = emailValidation.message)
                        true
                    }

                    else -> false
                } || when (passwordValidation) {
                    is com.trash2cash.app.services.ValidationResult.Error -> {
                        _loginState.value =
                            currentState.copy(passwordError = passwordValidation.message)
                        true
                    }

                    else -> false
                }

                if (hasErrors) return@launch

                _loginState.value = currentState.copy(isLoading = true)
                _authState.value = AuthState.Loading

                // Attempt login
                val user = repository.authenticateUser(
                    currentState.email,
                    currentState.password,
                    currentState.selectedRole
                )

                if (user != null) {
                    // Save session
                    authService.saveUserSession(user)

                    // Update last login
                    repository.updateLastLogin(user.id, Date())

                    _loginState.value = currentState.copy(isLoading = false)
                    _authState.value = AuthState.Authenticated(user)

                    _uiState.value = _uiState.value.copy(
                        successMessage = "Welcome back, ${user.name}!"
                    )
                } else {
                    _loginState.value = currentState.copy(
                        isLoading = false,
                        loginError = "Invalid email, password, or role"
                    )
                    _authState.value = AuthState.NotAuthenticated
                }

            } catch (e: Exception) {
                _loginState.value = _loginState.value.copy(
                    isLoading = false,
                    loginError = "Login failed: ${e.message}"
                )
                _authState.value = AuthState.Error("Login failed: ${e.message}")
            }
        }
    }

    // Registration Functions
    fun updateRegistrationField(field: RegistrationField, value: String) {
        _registrationState.value = when (field) {
            RegistrationField.NAME -> _registrationState.value.copy(name = value, nameError = null)
            RegistrationField.EMAIL -> _registrationState.value.copy(
                email = value,
                emailError = null
            )

            RegistrationField.PASSWORD -> _registrationState.value.copy(
                password = value,
                passwordError = null
            )

            RegistrationField.CONFIRM_PASSWORD -> _registrationState.value.copy(
                confirmPassword = value,
                confirmPasswordError = null
            )

            RegistrationField.PHONE -> _registrationState.value.copy(
                phoneNumber = value,
                phoneError = null
            )

            RegistrationField.EMPLOYEE_ID -> _registrationState.value.copy(
                employeeId = value,
                employeeIdError = null
            )

            RegistrationField.DEPARTMENT -> _registrationState.value.copy(
                department = value,
                departmentError = null
            )

            RegistrationField.DESIGNATION -> _registrationState.value.copy(
                designation = value,
                designationError = null
            )
        }
    }

    fun updateRegistrationRole(role: UserRole) {
        _registrationState.value = _registrationState.value.copy(selectedRole = role)
    }

    fun register() {
        viewModelScope.launch {
            try {
                val currentState = _registrationState.value

                // Create registration data
                val registrationData = RegistrationData(
                    name = currentState.name,
                    email = currentState.email,
                    password = currentState.password,
                    phoneNumber = currentState.phoneNumber,
                    role = currentState.selectedRole,
                    employeeId = currentState.employeeId,
                    department = currentState.department,
                    designation = currentState.designation
                )

                // Validate all fields
                val validationErrors =
                    authService.validateRegistrationData(registrationData).toMutableList()

                // Check password confirmation
                if (currentState.password != currentState.confirmPassword) {
                    validationErrors.add("Passwords do not match")
                }

                if (validationErrors.isNotEmpty()) {
                    updateRegistrationErrors(validationErrors)
                    return@launch
                }

                _registrationState.value = currentState.copy(isLoading = true)

                // Check if user already exists
                val existingUser =
                    repository.getUserByEmailAndRole(currentState.email, currentState.selectedRole)
                if (existingUser != null) {
                    _registrationState.value = currentState.copy(
                        isLoading = false,
                        registrationError = "User with this email and role already exists"
                    )
                    return@launch
                }

                // Hash password
                val (hashedPassword, salt) = authService.hashPassword(currentState.password)

                // Create user
                val userId = authService.generateUserId()
                val user = User(
                    id = userId,
                    email = currentState.email,
                    name = currentState.name,
                    phoneNumber = currentState.phoneNumber,
                    role = currentState.selectedRole,
                    createdAt = Date(),
                    isVerified = currentState.selectedRole == UserRole.CITIZEN // Citizens auto-verified, municipal workers need approval
                )

                // Save user
                repository.createUser(user, hashedPassword, salt)

                // If municipal worker, create municipal worker record
                if (currentState.selectedRole == UserRole.MUNICIPAL_WORKER) {
                    val municipalWorkerId = authService.generateMunicipalWorkerId()
                    val municipalWorker = MunicipalWorker(
                        id = municipalWorkerId,
                        userId = userId,
                        employeeId = currentState.employeeId,
                        department = currentState.department,
                        designation = currentState.designation,
                        joinDate = Date()
                    )
                    repository.createMunicipalWorker(municipalWorker)
                }

                _registrationState.value = currentState.copy(isLoading = false)

                _uiState.value = _uiState.value.copy(
                    successMessage = if (currentState.selectedRole == UserRole.CITIZEN) {
                        "Registration successful! You can now login."
                    } else {
                        "Registration submitted! Your account will be activated after admin approval."
                    }
                )

            } catch (e: Exception) {
                _registrationState.value = _registrationState.value.copy(
                    isLoading = false,
                    registrationError = "Registration failed: ${e.message}"
                )
            }
        }
    }

    private fun updateRegistrationErrors(errors: List<String>) {
        var state = _registrationState.value

        errors.forEach { error ->
            when {
                error.contains("Name") -> state = state.copy(nameError = error)
                error.contains("Email") -> state = state.copy(emailError = error)
                error.contains("Password") && error.contains("match") -> state =
                    state.copy(confirmPasswordError = error)

                error.contains("Password") -> state = state.copy(passwordError = error)
                error.contains("Phone") -> state = state.copy(phoneError = error)
                error.contains("Employee") -> state = state.copy(employeeIdError = error)
                error.contains("Department") -> state = state.copy(departmentError = error)
                error.contains("Designation") -> state = state.copy(designationError = error)
                else -> state = state.copy(registrationError = error)
            }
        }

        _registrationState.value = state
    }

    // Navigation Functions
    fun showLogin() {
        _uiState.value = _uiState.value.copy(currentScreen = AuthScreen.LOGIN)
        _loginState.value = _loginState.value.copy(
            email = "",
            password = "",
            emailError = null,
            passwordError = null,
            loginError = null
        )
    }

    fun showRegistration() {
        _uiState.value = _uiState.value.copy(currentScreen = AuthScreen.REGISTRATION)
        _registrationState.value = _registrationState.value.copy(
            selectedRole = _loginState.value.selectedRole,
            name = "",
            email = "",
            password = "",
            confirmPassword = "",
            phoneNumber = "",
            employeeId = "",
            department = "",
            designation = "",
            nameError = null,
            emailError = null,
            passwordError = null,
            confirmPasswordError = null,
            phoneError = null,
            employeeIdError = null,
            departmentError = null,
            designationError = null,
            registrationError = null
        )
    }

    fun showRoleSelection() {
        _uiState.value = _uiState.value.copy(currentScreen = AuthScreen.ROLE_SELECTION)
    }

    fun hideRoleSelection() {
        _uiState.value = _uiState.value.copy(currentScreen = AuthScreen.WELCOME)
    }

    // Logout
    fun logout() {
        viewModelScope.launch {
            try {
                authService.logout()
                _authState.value = AuthState.NotAuthenticated
                _uiState.value = AuthUiState() // Reset UI state
                clearAllForms()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Logout failed: ${e.message}"
                )
            }
        }
    }

    // Clear Functions
    fun clearLoginForm() {
        _loginState.value = LoginFormState()
    }

    fun clearRegistrationForm() {
        _registrationState.value = RegistrationFormState()
    }

    fun clearAllForms() {
        clearLoginForm()
        clearRegistrationForm()
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }

    // Utility Functions
    fun getDemoCredentials(role: UserRole): Pair<String, String> {
        return when (role) {
            UserRole.CITIZEN -> Pair("citizen@demo.com", "demo123")
            UserRole.MUNICIPAL_WORKER -> Pair("municipal@demo.com", "demo123")
            UserRole.ADMIN -> Pair("admin@demo.com", "demo123")
        }
    }
}

// Data Classes for UI State
data class AuthUiState(
    val currentScreen: AuthScreen = AuthScreen.WELCOME,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    // Keep these for backward compatibility
    val showLogin: Boolean = false,
    val showRoleSelection: Boolean = false
)

enum class AuthScreen {
    WELCOME,
    ROLE_SELECTION,
    LOGIN,
    REGISTRATION
}

data class LoginFormState(
    val email: String = "",
    val password: String = "",
    val selectedRole: UserRole = UserRole.CITIZEN,
    val isLoading: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val loginError: String? = null
)

data class RegistrationFormState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val phoneNumber: String = "",
    val selectedRole: UserRole = UserRole.CITIZEN,
    // Municipal worker specific fields
    val employeeId: String = "",
    val department: String = "",
    val designation: String = "",
    // State
    val isLoading: Boolean = false,
    // Error states
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val phoneError: String? = null,
    val employeeIdError: String? = null,
    val departmentError: String? = null,
    val designationError: String? = null,
    val registrationError: String? = null
)

enum class RegistrationField {
    NAME, EMAIL, PASSWORD, CONFIRM_PASSWORD, PHONE, EMPLOYEE_ID, DEPARTMENT, DESIGNATION
}