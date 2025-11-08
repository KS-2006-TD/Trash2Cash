package com.trash2cash.app.services

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.trash2cash.app.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.*

class AuthenticationService(private val context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "trash2cash_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_LAST_LOGIN = "last_login"
    }

    // Session Management
    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getCurrentUserId(): String? {
        return sharedPreferences.getString(KEY_USER_ID, null)
    }

    fun getCurrentUserRole(): UserRole? {
        val roleString = sharedPreferences.getString(KEY_USER_ROLE, null)
        return roleString?.let { UserRole.valueOf(it) }
    }

    fun getCurrentUserEmail(): String? {
        return sharedPreferences.getString(KEY_USER_EMAIL, null)
    }

    suspend fun saveUserSession(user: User) {
        withContext(Dispatchers.IO) {
            sharedPreferences.edit().apply {
                putString(KEY_USER_ID, user.id)
                putString(KEY_USER_EMAIL, user.email)
                putString(KEY_USER_ROLE, user.role.name)
                putBoolean(KEY_IS_LOGGED_IN, true)
                putLong(KEY_LAST_LOGIN, System.currentTimeMillis())
                apply()
            }
        }
    }

    suspend fun clearUserSession() {
        withContext(Dispatchers.IO) {
            sharedPreferences.edit().apply {
                remove(KEY_USER_ID)
                remove(KEY_USER_EMAIL)
                remove(KEY_USER_ROLE)
                putBoolean(KEY_IS_LOGGED_IN, false)
                remove(KEY_LAST_LOGIN)
                apply()
            }
        }
    }

    // Password Hashing
    suspend fun hashPassword(
        password: String,
        salt: String = generateSalt()
    ): Pair<String, String> {
        return withContext(Dispatchers.Default) {
            val messageDigest = MessageDigest.getInstance("SHA-256")
            val saltedPassword = "$password$salt"
            val hashedBytes = messageDigest.digest(saltedPassword.toByteArray())
            val hashedPassword = hashedBytes.joinToString("") { "%02x".format(it) }
            Pair(hashedPassword, salt)
        }
    }

    suspend fun verifyPassword(password: String, hashedPassword: String, salt: String): Boolean {
        return withContext(Dispatchers.Default) {
            val (computedHash, _) = hashPassword(password, salt)
            computedHash == hashedPassword
        }
    }

    private fun generateSalt(): String {
        return UUID.randomUUID().toString()
    }

    // Input Validation
    fun validateEmail(email: String): ValidationResult {
        return when {
            email.isBlank() -> ValidationResult.Error("Email cannot be empty")
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                ValidationResult.Error("Please enter a valid email address")

            else -> ValidationResult.Success
        }
    }

    fun validatePassword(password: String): ValidationResult {
        return when {
            password.isBlank() -> ValidationResult.Error("Password cannot be empty")
            password.length < 6 -> ValidationResult.Error("Password must be at least 6 characters")
            !password.any { it.isDigit() } -> ValidationResult.Error("Password must contain at least one number")
            !password.any { it.isLetter() } -> ValidationResult.Error("Password must contain at least one letter")
            else -> ValidationResult.Success
        }
    }

    fun validateName(name: String): ValidationResult {
        return when {
            name.isBlank() -> ValidationResult.Error("Name cannot be empty")
            name.length < 2 -> ValidationResult.Error("Name must be at least 2 characters")
            !name.all { it.isLetter() || it.isWhitespace() } ->
                ValidationResult.Error("Name can only contain letters and spaces")

            else -> ValidationResult.Success
        }
    }

    fun validatePhoneNumber(phoneNumber: String): ValidationResult {
        return when {
            phoneNumber.isBlank() -> ValidationResult.Error("Phone number cannot be empty")
            phoneNumber.length != 10 -> ValidationResult.Error("Phone number must be 10 digits")
            !phoneNumber.all { it.isDigit() } -> ValidationResult.Error("Phone number can only contain digits")
            else -> ValidationResult.Success
        }
    }

    fun validateEmployeeId(employeeId: String): ValidationResult {
        return when {
            employeeId.isBlank() -> ValidationResult.Error("Employee ID cannot be empty")
            employeeId.length < 4 -> ValidationResult.Error("Employee ID must be at least 4 characters")
            else -> ValidationResult.Success
        }
    }

    fun validateRegistrationData(data: RegistrationData): List<String> {
        val errors = mutableListOf<String>()

        when (val nameResult = validateName(data.name)) {
            is ValidationResult.Error -> errors.add(nameResult.message)
            else -> {}
        }

        when (val emailResult = validateEmail(data.email)) {
            is ValidationResult.Error -> errors.add(emailResult.message)
            else -> {}
        }

        when (val passwordResult = validatePassword(data.password)) {
            is ValidationResult.Error -> errors.add(passwordResult.message)
            else -> {}
        }

        when (val phoneResult = validatePhoneNumber(data.phoneNumber)) {
            is ValidationResult.Error -> errors.add(phoneResult.message)
            else -> {}
        }

        // Municipal worker specific validation
        if (data.role == UserRole.MUNICIPAL_WORKER) {
            when (val employeeIdResult = validateEmployeeId(data.employeeId)) {
                is ValidationResult.Error -> errors.add(employeeIdResult.message)
                else -> {}
            }

            if (data.department.isBlank()) {
                errors.add("Department cannot be empty")
            }

            if (data.designation.isBlank()) {
                errors.add("Designation cannot be empty")
            }
        }

        return errors
    }

    // Generate unique IDs
    fun generateUserId(): String {
        return "user_${UUID.randomUUID().toString().replace("-", "").take(12)}"
    }

    fun generateMunicipalWorkerId(): String {
        return "municipal_${UUID.randomUUID().toString().replace("-", "").take(12)}"
    }

    // Session timeout management
    fun isSessionExpired(): Boolean {
        val lastLogin = sharedPreferences.getLong(KEY_LAST_LOGIN, 0)
        val currentTime = System.currentTimeMillis()
        val sessionTimeout = 7 * 24 * 60 * 60 * 1000L // 7 days in milliseconds
        return (currentTime - lastLogin) > sessionTimeout
    }

    suspend fun refreshSession() {
        withContext(Dispatchers.IO) {
            sharedPreferences.edit().apply {
                putLong(KEY_LAST_LOGIN, System.currentTimeMillis())
                apply()
            }
        }
    }

    // Logout with cleanup
    suspend fun logout() {
        clearUserSession()
        // Additional cleanup can be added here
        // e.g., clear cached data, cancel ongoing uploads, etc.
    }

    // Security utilities
    fun obfuscateEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return email

        val username = parts[0]
        val domain = parts[1]

        val obfuscatedUsername = if (username.length <= 2) {
            username
        } else {
            "${username.first()}${"*".repeat(username.length - 2)}${username.last()}"
        }

        return "$obfuscatedUsername@$domain"
    }

    fun generatePasswordResetToken(): String {
        return UUID.randomUUID().toString()
    }

    // Demo mode utilities (for testing)
    fun enableDemoMode() {
        sharedPreferences.edit().apply {
            putBoolean("demo_mode", true)
            apply()
        }
    }

    fun isDemoMode(): Boolean {
        return sharedPreferences.getBoolean("demo_mode", false)
    }
}

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}

// Authentication states
sealed class AuthState {
    object Loading : AuthState()
    object NotAuthenticated : AuthState()
    data class Authenticated(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}

// Login/Registration results
sealed class AuthResult {
    data class Success(val user: User) : AuthResult()
    data class Error(val message: String) : AuthResult()
    object Loading : AuthResult()
}