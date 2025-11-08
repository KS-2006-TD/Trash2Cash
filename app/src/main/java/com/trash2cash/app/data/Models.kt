package com.trash2cash.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

// Authentication and User Management
@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val email: String,
    val name: String,
    val phoneNumber: String = "",
    val role: UserRole,
    val isActive: Boolean = true,
    val isVerified: Boolean = false,
    val createdAt: Date,
    val lastLoginAt: Date? = null,
    val profileImageUri: String = "",
    // Citizen specific fields
    val totalPoints: Int = 0,
    val totalWasteCollected: Float = 0f, // in kg
    val totalCO2Saved: Float = 0f, // in kg
    val level: Int = 1,
    val adoptedZones: List<String> = emptyList(),
    // Municipal specific fields
    val municipalId: String? = null,
    val assignedAreas: List<String> = emptyList(),
    val verificationCount: Int = 0
)

@Entity(tableName = "waste_submissions")
data class WasteSubmission(
    @PrimaryKey val id: String,
    val citizenId: String,
    val imageUri: String,
    val latitude: Double,
    val longitude: Double,
    val locationName: String,
    val address: String = "",
    val timestamp: Date,
    val description: String = "",
    // AI Analysis Results
    val aiDetectedWasteType: String = "",
    val aiEstimatedWeight: Float = 0f,
    val aiConfidenceScore: Float = 0f,
    val aiProcessedAt: Date? = null,
    // Municipal Assignment and Verification
    val assignedMunicipalId: String? = null, // Assigned based on location
    val verificationStatus: VerificationStatus = VerificationStatus.PENDING,
    val verifiedByMunicipalId: String? = null,
    val verifiedAt: Date? = null,
    val municipalComments: String = "",
    val actualWasteType: String = "",
    val actualWeight: Float = 0f,
    val rewardPoints: Int = 0,
    val impactScore: Float = 0f, // CO2 reduction score
    val isRejected: Boolean = false,
    val rejectionReason: String = ""
)

@Entity(tableName = "municipal_workers")
data class MunicipalWorker(
    @PrimaryKey val id: String,
    val userId: String, // References User.id
    val employeeId: String,
    val department: String,
    val designation: String,
    val assignedAreas: List<String> = emptyList(),
    val supervisorId: String? = null,
    val isActive: Boolean = true,
    val verificationCount: Int = 0,
    val accuracy: Float = 0f, // Verification accuracy percentage
    val joinDate: Date
)

@Entity(tableName = "reward_transactions")
data class RewardTransaction(
    @PrimaryKey val id: String,
    val citizenId: String,
    val type: TransactionType,
    val points: Int,
    val description: String,
    val timestamp: Date,
    val relatedSubmissionId: String? = null,
    val voucherCode: String? = null,
    val expiryDate: Date? = null
)

@Entity(tableName = "vouchers")
data class Voucher(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val pointsCost: Int,
    val category: VoucherCategory,
    val partnerName: String,
    val partnerImageUri: String = "",
    val validUntil: Date,
    val termsAndConditions: String,
    val imageUri: String = "",
    val isActive: Boolean = true,
    val maxRedemptions: Int = -1, // -1 for unlimited
    val currentRedemptions: Int = 0,
    val discountPercentage: Float = 0f,
    val discountAmount: Float = 0f
)

@Entity(tableName = "leaderboard_entries")
data class LeaderboardEntry(
    @PrimaryKey val citizenId: String,
    val userName: String,
    val points: Int,
    val wasteCollected: Float,
    val co2Saved: Float,
    val rank: Int,
    val profileImageUri: String = "",
    val level: Int = 1,
    val submissionsThisMonth: Int = 0
)

@Entity(tableName = "zones")
data class Zone(
    @PrimaryKey val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Float, // in meters
    val address: String,
    val city: String,
    val state: String,
    val pincode: String,
    val adoptedByCitizenId: String? = null,
    val assignedMunicipalId: String? = null,
    val wasteLevel: WasteLevel = WasteLevel.MEDIUM,
    val lastCleaned: Date? = null,
    val totalSubmissions: Int = 0,
    val totalWasteCollected: Float = 0f,
    val description: String = "",
    val isActive: Boolean = true
)

@Entity(tableName = "challenges")
data class Challenge(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val imageUri: String = "",
    val startDate: Date,
    val endDate: Date,
    val targetPoints: Int,
    val targetWaste: Float, // in kg
    val rewardPoints: Int,
    val type: ChallengeType,
    val participantIds: List<String> = emptyList(),
    val isActive: Boolean = true,
    val organizationName: String = "",
    val sponsorName: String = "",
    val maxParticipants: Int = -1,
    val currentParticipants: Int = 0
)

@Entity(tableName = "impact_analytics")
data class ImpactAnalytics(
    @PrimaryKey val id: String,
    val date: Date,
    val totalWasteCollected: Float,
    val totalCO2Saved: Float,
    val totalSubmissions: Int,
    val totalVerifications: Int,
    val totalUsers: Int,
    val avgVerificationTime: Long, // in minutes
    val topZoneId: String = "",
    val topCitizenId: String = ""
)

// Enums
enum class UserRole {
    CITIZEN, MUNICIPAL_WORKER, ADMIN
}

enum class VerificationStatus {
    PENDING, // Waiting for municipal verification
    AI_PROCESSED, // AI has analyzed, waiting for municipal review
    VERIFIED, // Municipal worker approved
    REJECTED, // Municipal worker rejected
    DISPUTED // Citizen disputed rejection
}

enum class TransactionType {
    EARNED, // Points earned from waste submission
    REDEEMED, // Points spent on vouchers
    BONUS, // Bonus points from challenges
    PENALTY, // Points deducted (if any)
    REFUND // Points refunded from cancelled redemption
}

enum class VoucherCategory {
    FOOD_DELIVERY,
    SHOPPING,
    TRANSPORTATION,
    MOBILE_RECHARGE,
    ENTERTAINMENT,
    EDUCATION,
    HEALTHCARE,
    GROCERY,
    OTHER
}

enum class WasteLevel {
    LOW, MEDIUM, HIGH, CRITICAL
}

enum class ChallengeType {
    INDIVIDUAL, // Individual challenge
    GROUP, // College/organization challenge
    COMMUNITY, // Area-based challenge
    MUNICIPAL // Municipal department challenge
}

// Data classes for API responses and UI state
data class WasteVerificationResult(
    val isPlastic: Boolean,
    val confidence: Float,
    val wasteType: String,
    val estimatedWeight: Float,
    val co2Impact: Float,
    val rewardPoints: Int,
    val message: String
)

data class MunicipalVerificationRequest(
    val submissionId: String,
    val isApproved: Boolean,
    val actualWasteType: String,
    val actualWeight: Float,
    val comments: String,
    val rejectionReason: String = ""
)

data class CitizenStats(
    val totalPoints: Int,
    val totalWasteCollected: Float,
    val totalCO2Saved: Float,
    val rank: Int,
    val level: Int,
    val submissionsThisWeek: Int,
    val submissionsThisMonth: Int,
    val submissionsToday: Int,
    val pendingVerifications: Int,
    val streakDays: Int
)

data class MunicipalStats(
    val totalVerifications: Int,
    val verificationAccuracy: Float,
    val avgVerificationTime: Long,
    val pendingVerifications: Int,
    val verifiedToday: Int,
    val verifiedThisWeek: Int,
    val verifiedThisMonth: Int,
    val totalImpactCreated: Float
)

data class GlobalStats(
    val totalPlasticCollected: Float,
    val totalCO2Reduced: Float,
    val totalUsers: Int,
    val totalMunicipalWorkers: Int,
    val topContributors: List<LeaderboardEntry>,
    val wasteHotspots: List<Zone>,
    val todaySubmissions: Int,
    val todayVerifications: Int
)

data class LoginCredentials(
    val email: String,
    val password: String
)

data class RegistrationData(
    val name: String,
    val email: String,
    val password: String,
    val phoneNumber: String,
    val role: UserRole,
    // Municipal specific
    val employeeId: String = "",
    val department: String = "",
    val designation: String = ""
)