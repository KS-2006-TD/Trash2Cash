package com.trash2cash.app.data

import androidx.room.*
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow
import java.util.Date

// Type converters for Room database
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return value.joinToString(",")
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return if (value.isEmpty()) emptyList() else value.split(",")
    }

    @TypeConverter
    fun fromUserRole(role: UserRole): String {
        return role.name
    }

    @TypeConverter
    fun toUserRole(role: String): UserRole {
        return UserRole.valueOf(role)
    }

    @TypeConverter
    fun fromVerificationStatus(status: VerificationStatus): String {
        return status.name
    }

    @TypeConverter
    fun toVerificationStatus(status: String): VerificationStatus {
        return VerificationStatus.valueOf(status)
    }

    @TypeConverter
    fun fromTransactionType(type: TransactionType): String {
        return type.name
    }

    @TypeConverter
    fun toTransactionType(type: String): TransactionType {
        return TransactionType.valueOf(type)
    }

    @TypeConverter
    fun fromVoucherCategory(category: VoucherCategory): String {
        return category.name
    }

    @TypeConverter
    fun toVoucherCategory(category: String): VoucherCategory {
        return VoucherCategory.valueOf(category)
    }

    @TypeConverter
    fun fromWasteLevel(level: WasteLevel): String {
        return level.name
    }

    @TypeConverter
    fun toWasteLevel(level: String): WasteLevel {
        return WasteLevel.valueOf(level)
    }

    @TypeConverter
    fun fromChallengeType(type: ChallengeType): String {
        return type.name
    }

    @TypeConverter
    fun toChallengeType(type: String): ChallengeType {
        return ChallengeType.valueOf(type)
    }
}

// DAO Interfaces

// User Authentication DAO
@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email AND role = :role")
    suspend fun getUserByEmailAndRole(email: String, role: UserRole): User?

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): User?

    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserByIdFlow(userId: String): Flow<User?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Query("UPDATE users SET lastLoginAt = :loginTime WHERE id = :userId")
    suspend fun updateLastLogin(userId: String, loginTime: Date)

    @Query("UPDATE users SET totalPoints = totalPoints + :points WHERE id = :userId")
    suspend fun addPoints(userId: String, points: Int)

    @Query("UPDATE users SET totalWasteCollected = totalWasteCollected + :weight WHERE id = :userId")
    suspend fun addWasteCollected(userId: String, weight: Float)

    @Query("UPDATE users SET totalCO2Saved = totalCO2Saved + :co2 WHERE id = :userId")
    suspend fun addCO2Saved(userId: String, co2: Float)

    @Query("SELECT COUNT(*) FROM users WHERE role = 'CITIZEN'")
    suspend fun getCitizenCount(): Int

    @Query("SELECT COUNT(*) FROM users WHERE role = 'MUNICIPAL_WORKER'")
    suspend fun getMunicipalWorkerCount(): Int
}

// Municipal Worker DAO
@Dao
interface MunicipalWorkerDao {
    @Query("SELECT * FROM municipal_workers WHERE userId = :userId")
    suspend fun getMunicipalWorkerByUserId(userId: String): MunicipalWorker?

    @Query("SELECT * FROM municipal_workers WHERE id = :id")
    suspend fun getMunicipalWorkerById(id: String): MunicipalWorker?

    @Query("SELECT * FROM municipal_workers")
    suspend fun getAllMunicipalWorkers(): List<MunicipalWorker>

    @Insert
    suspend fun insertMunicipalWorker(worker: MunicipalWorker)

    @Update
    suspend fun updateMunicipalWorker(worker: MunicipalWorker)

    @Query("UPDATE municipal_workers SET verificationCount = verificationCount + 1 WHERE id = :workerId")
    suspend fun incrementVerificationCount(workerId: String)

    @Query("SELECT * FROM municipal_workers WHERE isActive = 1")
    fun getActiveMunicipalWorkers(): Flow<List<MunicipalWorker>>
}

// Waste Submission DAO
@Dao
interface WasteSubmissionDao {
    @Query("SELECT * FROM waste_submissions ORDER BY timestamp DESC")
    fun getAllSubmissions(): Flow<List<WasteSubmission>>

    @Query("SELECT * FROM waste_submissions WHERE citizenId = :citizenId ORDER BY timestamp DESC")
    fun getSubmissionsByCitizen(citizenId: String): Flow<List<WasteSubmission>>

    @Query("SELECT * FROM waste_submissions WHERE verificationStatus = 'PENDING' OR verificationStatus = 'AI_PROCESSED' ORDER BY timestamp ASC")
    fun getPendingVerifications(): Flow<List<WasteSubmission>>

    @Query("SELECT * FROM waste_submissions WHERE assignedMunicipalId = :municipalId AND (verificationStatus = 'PENDING' OR verificationStatus = 'AI_PROCESSED') ORDER BY timestamp ASC")
    fun getAssignedSubmissions(municipalId: String): Flow<List<WasteSubmission>>

    @Query("SELECT * FROM waste_submissions WHERE verificationStatus = 'VERIFIED' ORDER BY timestamp DESC")
    fun getVerifiedSubmissions(): Flow<List<WasteSubmission>>

    @Query("SELECT * FROM waste_submissions WHERE verifiedByMunicipalId = :municipalId ORDER BY timestamp DESC")
    fun getSubmissionsVerifiedBy(municipalId: String): Flow<List<WasteSubmission>>

    @Query("SELECT * FROM waste_submissions WHERE id = :id")
    suspend fun getSubmissionById(id: String): WasteSubmission?

    @Insert
    suspend fun insertSubmission(submission: WasteSubmission)

    @Update
    suspend fun updateSubmission(submission: WasteSubmission)

    @Delete
    suspend fun deleteSubmission(submission: WasteSubmission)

    @Query("SELECT COUNT(*) FROM waste_submissions WHERE citizenId = :citizenId AND timestamp > :weekStart")
    suspend fun getWeeklySubmissionCount(citizenId: String, weekStart: Long): Int

    @Query("SELECT COUNT(*) FROM waste_submissions WHERE citizenId = :citizenId AND timestamp > :monthStart")
    suspend fun getMonthlySubmissionCount(citizenId: String, monthStart: Long): Int

    @Query("SELECT COUNT(*) FROM waste_submissions WHERE citizenId = :citizenId AND timestamp > :dayStart")
    suspend fun getDailySubmissionCount(citizenId: String, dayStart: Long): Int

    @Query("SELECT COUNT(*) FROM waste_submissions WHERE verificationStatus = 'PENDING'")
    suspend fun getPendingVerificationCount(): Int

    @Query("SELECT COUNT(*) FROM waste_submissions WHERE verifiedByMunicipalId = :municipalId AND verifiedAt > :todayStart")
    suspend fun getTodayVerificationCount(municipalId: String, todayStart: Long): Int

    @Query("SELECT COUNT(*) FROM waste_submissions WHERE timestamp > :todayStart")
    suspend fun getTodaySubmissionCount(todayStart: Long): Int
}

// Reward Transaction DAO
@Dao
interface RewardTransactionDao {
    @Query("SELECT * FROM reward_transactions WHERE citizenId = :citizenId ORDER BY timestamp DESC")
    fun getTransactionsByCitizen(citizenId: String): Flow<List<RewardTransaction>>

    @Insert
    suspend fun insertTransaction(transaction: RewardTransaction)

    @Query("SELECT SUM(points) FROM reward_transactions WHERE citizenId = :citizenId AND type = 'EARNED'")
    suspend fun getTotalEarnedPoints(citizenId: String): Int?

    @Query("SELECT SUM(points) FROM reward_transactions WHERE citizenId = :citizenId AND type = 'REDEEMED'")
    suspend fun getTotalRedeemedPoints(citizenId: String): Int?
}

// Voucher DAO
@Dao
interface VoucherDao {
    @Query("SELECT * FROM vouchers WHERE isActive = 1 ORDER BY pointsCost ASC")
    fun getActiveVouchers(): Flow<List<Voucher>>

    @Query("SELECT * FROM vouchers WHERE category = :category AND isActive = 1")
    fun getVouchersByCategory(category: VoucherCategory): Flow<List<Voucher>>

    @Query("SELECT * FROM vouchers WHERE id = :id")
    suspend fun getVoucherById(id: String): Voucher?

    @Insert
    suspend fun insertVoucher(voucher: Voucher)

    @Update
    suspend fun updateVoucher(voucher: Voucher)
}

// Leaderboard DAO
@Dao
interface LeaderboardDao {
    @Query("SELECT * FROM leaderboard_entries ORDER BY points DESC LIMIT 100")
    fun getTopUsers(): Flow<List<LeaderboardEntry>>

    @Query("SELECT * FROM leaderboard_entries WHERE citizenId = :citizenId")
    suspend fun getUserRank(citizenId: String): LeaderboardEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeaderboardEntry(entry: LeaderboardEntry)

    @Query("DELETE FROM leaderboard_entries")
    suspend fun clearLeaderboard()

    @Query("SELECT * FROM leaderboard_entries ORDER BY points DESC LIMIT 10")
    fun getTop10Users(): Flow<List<LeaderboardEntry>>
}

// Zone DAO
@Dao
interface ZoneDao {
    @Query("SELECT * FROM zones WHERE isActive = 1")
    fun getAllZones(): Flow<List<Zone>>

    @Query("SELECT * FROM zones WHERE adoptedByCitizenId = :citizenId")
    fun getZonesByCitizen(citizenId: String): Flow<List<Zone>>

    @Query("SELECT * FROM zones WHERE assignedMunicipalId = :municipalId")
    fun getZonesByMunicipal(municipalId: String): Flow<List<Zone>>

    @Query("SELECT * FROM zones WHERE id = :id")
    suspend fun getZoneById(id: String): Zone?

    @Insert
    suspend fun insertZone(zone: Zone)

    @Update
    suspend fun updateZone(zone: Zone)

    @Query("UPDATE zones SET adoptedByCitizenId = :citizenId WHERE id = :zoneId")
    suspend fun adoptZone(zoneId: String, citizenId: String)

    @Query("SELECT * FROM zones ORDER BY totalWasteCollected DESC LIMIT 5")
    fun getTopWasteZones(): Flow<List<Zone>>
}

// Challenge DAO
@Dao
interface ChallengeDao {
    @Query("SELECT * FROM challenges WHERE isActive = 1 ORDER BY endDate ASC")
    fun getActiveChallenges(): Flow<List<Challenge>>

    @Query("SELECT * FROM challenges WHERE id = :id")
    suspend fun getChallengeById(id: String): Challenge?

    @Insert
    suspend fun insertChallenge(challenge: Challenge)

    @Update
    suspend fun updateChallenge(challenge: Challenge)

    @Query("SELECT * FROM challenges WHERE type = 'INDIVIDUAL' AND isActive = 1")
    fun getIndividualChallenges(): Flow<List<Challenge>>

    @Query("SELECT * FROM challenges WHERE type = 'MUNICIPAL' AND isActive = 1")
    fun getMunicipalChallenges(): Flow<List<Challenge>>
}

// Impact Analytics DAO
@Dao
interface ImpactAnalyticsDao {
    @Query("SELECT * FROM impact_analytics ORDER BY date DESC LIMIT 30")
    fun getLast30DaysAnalytics(): Flow<List<ImpactAnalytics>>

    @Query("SELECT * FROM impact_analytics WHERE date = :date")
    suspend fun getAnalyticsByDate(date: Date): ImpactAnalytics?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalytics(analytics: ImpactAnalytics)

    @Query("SELECT SUM(totalWasteCollected) FROM impact_analytics")
    suspend fun getTotalWasteCollected(): Float?

    @Query("SELECT SUM(totalCO2Saved) FROM impact_analytics")
    suspend fun getTotalCO2Saved(): Float?
}

// Main Database
@Database(
    entities = [
        User::class,
        MunicipalWorker::class,
        WasteSubmission::class,
        RewardTransaction::class,
        Voucher::class,
        LeaderboardEntry::class,
        Zone::class,
        Challenge::class,
        ImpactAnalytics::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class Trash2CashDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun municipalWorkerDao(): MunicipalWorkerDao
    abstract fun wasteSubmissionDao(): WasteSubmissionDao
    abstract fun rewardTransactionDao(): RewardTransactionDao
    abstract fun voucherDao(): VoucherDao
    abstract fun leaderboardDao(): LeaderboardDao
    abstract fun zoneDao(): ZoneDao
    abstract fun challengeDao(): ChallengeDao
    abstract fun impactAnalyticsDao(): ImpactAnalyticsDao

    companion object {
        const val DATABASE_NAME = "trash2cash_database"
    }
}