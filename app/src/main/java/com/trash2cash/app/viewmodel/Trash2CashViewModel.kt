package com.trash2cash.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trash2cash.app.data.*
import com.trash2cash.app.repository.Trash2CashRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class Trash2CashViewModel(context: Context) : ViewModel() {

    private val repository = Trash2CashRepository(context)

    // UI State
    private val _uiState = MutableStateFlow(Trash2CashUiState())
    val uiState: StateFlow<Trash2CashUiState> = _uiState.asStateFlow()

    // User data flows
    val currentUser = repository.getCurrentUserFlow()
    val userSubmissions = repository.getCitizenSubmissions()
    val userTransactions = repository.getCitizenTransactions()

    // Global data flows
    val activeVouchers = repository.getActiveVouchers()
    val leaderboard = repository.getLeaderboard()
    val allZones = repository.getAllZones()
    val activeChallenges = repository.getActiveChallenges()

    // Municipal worker specific flows
    val pendingVerifications = repository.getPendingVerifications()

    // Stats
    private val _userStats = MutableStateFlow<CitizenStats?>(null)
    val userStats: StateFlow<CitizenStats?> = _userStats.asStateFlow()

    private val _municipalStats = MutableStateFlow<MunicipalStats?>(null)
    val municipalStats: StateFlow<MunicipalStats?> = _municipalStats.asStateFlow()

    private val _globalStats = MutableStateFlow<GlobalStats?>(null)
    val globalStats: StateFlow<GlobalStats?> = _globalStats.asStateFlow()

    init {
        initializeApp()
    }

    private fun initializeApp() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                // IMPORTANT: Initialize sample data on EVERY app start to ensure password hashes are in memory
                repository.initializeSampleData()

                // Load stats
                loadStats()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    statusMessage = "Welcome to Trash2Cash! Start collecting waste to earn rewards."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to initialize app: ${e.message}"
                )
            }
        }
    }

    // Waste submission functions
    fun submitWastePhoto(
        imageUri: String,
        latitude: Double,
        longitude: Double,
        locationName: String,
        description: String = ""
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isProcessingSubmission = true,
                    statusMessage = "Verifying your waste submission with AI..."
                )

                val submission = repository.submitWasteForVerification(
                    imageUri = imageUri,
                    latitude = latitude,
                    longitude = longitude,
                    locationName = locationName,
                    description = description
                )

                val message = when (submission.verificationStatus) {
                    VerificationStatus.PENDING ->
                        "📸 Submission received! Your waste photo is pending verification by municipal workers."

                    VerificationStatus.AI_PROCESSED ->
                        "🤖 AI analysis complete! Waiting for municipal verification."
                    VerificationStatus.VERIFIED ->
                        "🎉 Verified! You earned ${submission.rewardPoints} points for ${
                            String.format(
                                "%.2f",
                                submission.actualWeight
                            )
                        }kg of ${submission.actualWasteType}!"
                    VerificationStatus.REJECTED ->
                        "❌ Submission rejected. Please try with clearer images of plastic waste."

                    VerificationStatus.DISPUTED ->
                        "⚠️ Submission under review."
                }

                _uiState.value = _uiState.value.copy(
                    isProcessingSubmission = false,
                    statusMessage = message,
                    lastSubmissionResult = submission
                )

                // Refresh stats if verified
                if (submission.verificationStatus == VerificationStatus.VERIFIED) {
                    loadStats()
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessingSubmission = false,
                    errorMessage = "Failed to submit waste: ${e.message}"
                )
            }
        }
    }

    // Reward functions
    fun redeemVoucher(voucherId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                val success = repository.redeemVoucher(voucherId)

                if (success) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        statusMessage = "🎉 Voucher redeemed successfully! Check your transactions for the voucher code."
                    )
                    loadStats()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to redeem voucher. Insufficient points or voucher unavailable."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to redeem voucher: ${e.message}"
                )
            }
        }
    }

    // Zone functions
    fun adoptZone(zoneId: String) {
        viewModelScope.launch {
            try {
                val success = repository.adoptZone(zoneId)
                if (success) {
                    _uiState.value = _uiState.value.copy(
                        statusMessage = "🏆 Zone adopted! You're now responsible for keeping this area clean."
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to adopt zone. It may already be adopted by another user."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to adopt zone: ${e.message}"
                )
            }
        }
    }

    // Profile functions
    fun updateProfile(name: String, email: String, phoneNumber: String) {
        viewModelScope.launch {
            try {
                repository.updateUserProfile(name, email, phoneNumber)
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Profile updated successfully!"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to update profile: ${e.message}"
                )
            }
        }
    }

    // Stats functions
    private fun loadStats() {
        viewModelScope.launch {
            try {
                val currentUser = repository.getCurrentUser()

                when (currentUser?.role) {
                    UserRole.CITIZEN -> {
                        val citizenStats = repository.getCitizenStats()
                        _userStats.value = citizenStats
                    }

                    UserRole.MUNICIPAL_WORKER -> {
                        val municipalStats = repository.getMunicipalStats()
                        _municipalStats.value = municipalStats
                    }

                    else -> {}
                }

                val globalStats = repository.getGlobalStats()
                _globalStats.value = globalStats

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to load stats: ${e.message}"
                )
            }
        }
    }

    // Utility functions
    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearStatusMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }

    fun refreshData() {
        viewModelScope.launch {
            try {
                loadStats()
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Data refreshed!"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to refresh data: ${e.message}"
                )
            }
        }
    }

    fun refreshLeaderboard() {
        viewModelScope.launch {
            try {
                repository.updateLeaderboard()
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Leaderboard updated!"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to refresh leaderboard: ${e.message}"
                )
            }
        }
    }

    fun getVouchersByCategory(category: VoucherCategory): Flow<List<Voucher>> {
        return repository.getVouchersByCategory(category)
    }

    fun getCitizenAdoptedZones(): Flow<List<Zone>> {
        return repository.getCitizenAdoptedZones()
    }

    // Municipal worker specific functions
    fun verifyWasteSubmission(
        submissionId: String,
        isApproved: Boolean,
        actualWasteType: String,
        actualWeight: Float,
        comments: String,
        rejectionReason: String = ""
    ) {
        viewModelScope.launch {
            try {
                val currentUser = repository.getCurrentUser()
                if (currentUser?.role != UserRole.MUNICIPAL_WORKER) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Only municipal workers can verify submissions"
                    )
                    return@launch
                }

                _uiState.value = _uiState.value.copy(isLoading = true)

                val updatedSubmission = repository.verifyWasteSubmission(
                    submissionId = submissionId,
                    municipalWorkerId = currentUser.id,
                    isApproved = isApproved,
                    actualWasteType = actualWasteType,
                    actualWeight = actualWeight,
                    comments = comments,
                    rejectionReason = rejectionReason
                )

                if (updatedSubmission != null) {
                    val message = if (isApproved) {
                        "✅ Submission approved! Citizen earned ${updatedSubmission.rewardPoints} points."
                    } else {
                        "❌ Submission rejected with reason: $rejectionReason"
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        statusMessage = message
                    )

                    // Refresh municipal stats
                    loadStats()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to find submission"
                    )
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to verify submission: ${e.message}"
                )
            }
        }
    }

    fun getSubmissionsVerifiedBy(municipalId: String): Flow<List<WasteSubmission>> {
        return repository.getSubmissionsVerifiedBy(municipalId)
    }

    fun getMunicipalAssignedZones(): Flow<List<Zone>> {
        return repository.getMunicipalAssignedZones()
    }

    fun getMunicipalChallenges(): Flow<List<Challenge>> {
        return repository.getMunicipalChallenges()
    }

    // Navigation state
    fun setCurrentTab(tab: Trash2CashTab) {
        _uiState.value = _uiState.value.copy(currentTab = tab)
    }
}

// UI State data class
data class Trash2CashUiState(
    val isLoading: Boolean = false,
    val isProcessingSubmission: Boolean = false,
    val errorMessage: String? = null,
    val statusMessage: String? = null,
    val currentTab: Trash2CashTab = Trash2CashTab.HOME,
    val lastSubmissionResult: WasteSubmission? = null
)

// Navigation tabs
enum class Trash2CashTab {
    HOME,
    CAPTURE,
    REWARDS,
    LEADERBOARD,
    MAP,
    PROFILE
}