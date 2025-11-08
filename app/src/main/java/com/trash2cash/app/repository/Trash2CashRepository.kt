package com.trash2cash.app.repository

import android.content.Context
import androidx.room.Room
import com.trash2cash.app.data.*
import com.trash2cash.app.services.AuthenticationService
import com.trash2cash.app.services.WasteVerificationService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

class Trash2CashRepository(private val context: Context) {
    
    private val database = Room.databaseBuilder(
        context,
        Trash2CashDatabase::class.java,
        Trash2CashDatabase.DATABASE_NAME
    )
        .fallbackToDestructiveMigration() // Recreate DB on schema changes during development
        .build()

    // Services
    private val wasteVerificationService = WasteVerificationService(context)
    private val authService = AuthenticationService(context)

    // DAOs
    private val userDao = database.userDao()
    private val municipalWorkerDao = database.municipalWorkerDao()
    private val wasteSubmissionDao = database.wasteSubmissionDao()
    private val rewardTransactionDao = database.rewardTransactionDao()
    private val voucherDao = database.voucherDao()
    private val leaderboardDao = database.leaderboardDao()
    private val zoneDao = database.zoneDao()
    private val challengeDao = database.challengeDao()
    private val impactAnalyticsDao = database.impactAnalyticsDao()

    // Simple map to store password hashes (in production, this would be properly secured)
    private val passwordHashes =
        mutableMapOf<String, Pair<String, String>>() // userId -> (hashedPassword, salt)

    // Authentication operations
    suspend fun getUserById(userId: String): User? {
        return userDao.getUserById(userId)
    }

    suspend fun getUserByEmailAndRole(email: String, role: UserRole): User? {
        return userDao.getUserByEmailAndRole(email, role)
    }

    suspend fun createUser(user: User, hashedPassword: String, salt: String) {
        userDao.insertUser(user)
        passwordHashes[user.id] = Pair(hashedPassword, salt)
    }

    suspend fun authenticateUser(email: String, password: String, role: UserRole): User? {
        android.util.Log.d("Trash2CashAuth", "Attempting login - Email: $email, Role: $role")

        // Ensure demo users exist before attempting authentication
        if (email == "citizen@demo.com" || email == "municipal@demo.com") {
            val userId = if (email == "citizen@demo.com") "citizen_demo" else "municipal_demo"
            val userExists = getUserById(userId)
            if (userExists == null) {
                android.util.Log.d("Trash2CashAuth", "Demo user not found, creating...")
                createSampleUsers()
            }
        }

        val user = getUserByEmailAndRole(email, role)
        android.util.Log.d("Trash2CashAuth", "User found: ${user != null}, ID: ${user?.id}")

        if (user != null && user.isVerified) {
            val passwordData = passwordHashes[user.id]
            android.util.Log.d("Trash2CashAuth", "Password hash found: ${passwordData != null}")

            if (passwordData != null) {
                val (storedHash, salt) = passwordData
                val isPasswordValid = authService.verifyPassword(password, storedHash, salt)
                android.util.Log.d("Trash2CashAuth", "Password valid: $isPasswordValid")

                if (isPasswordValid) {
                    android.util.Log.d("Trash2CashAuth", "Login successful!")
                    return user
                }
            } else {
                android.util.Log.e("Trash2CashAuth", "Password hash not found for user: ${user.id}")
                android.util.Log.e("Trash2CashAuth", "Available hashes: ${passwordHashes.keys}")
            }
        } else {
            android.util.Log.e("Trash2CashAuth", "User not found or not verified")
        }

        android.util.Log.e("Trash2CashAuth", "Login failed")
        return null
    }

    suspend fun updateLastLogin(userId: String, loginTime: Date) {
        userDao.updateLastLogin(userId, loginTime)
    }

    suspend fun createMunicipalWorker(municipalWorker: MunicipalWorker) {
        municipalWorkerDao.insertMunicipalWorker(municipalWorker)
    }

    // Current user operations (based on session)
    fun getCurrentUserFlow(): Flow<User?> {
        val currentUserId =
            authService.getCurrentUserId() ?: return kotlinx.coroutines.flow.flowOf(null)
        return userDao.getUserByIdFlow(currentUserId)
    }

    suspend fun getCurrentUser(): User? {
        val currentUserId = authService.getCurrentUserId() ?: return null
        return userDao.getUserById(currentUserId)
    }

    suspend fun updateUserProfile(name: String, email: String, phoneNumber: String) {
        val currentUserId = authService.getCurrentUserId() ?: return
        val user = getCurrentUser()
        user?.let {
            val updatedUser = it.copy(name = name, email = email, phoneNumber = phoneNumber)
            userDao.updateUser(updatedUser)
        }
    }

    // Waste submission operations for Citizens
    suspend fun submitWasteForVerification(
        imageUri: String,
        latitude: Double,
        longitude: Double,
        locationName: String,
        address: String = "",
        description: String = ""
    ): WasteSubmission {
        val currentUserId =
            authService.getCurrentUserId() ?: throw IllegalStateException("User not logged in")

        // Create initial submission
        val submission = WasteSubmission(
            id = UUID.randomUUID().toString(),
            citizenId = currentUserId,
            imageUri = imageUri,
            latitude = latitude,
            longitude = longitude,
            locationName = locationName,
            address = address,
            timestamp = Date(),
            description = description,
            verificationStatus = VerificationStatus.AI_PROCESSED
        )

        // Save to database
        wasteSubmissionDao.insertSubmission(submission)

        // Process with AI
        try {
            val verificationResult =
                wasteVerificationService.verifyWasteImage(imageUri, latitude, longitude)
            
            val updatedSubmission = submission.copy(
                aiDetectedWasteType = verificationResult.wasteType,
                aiEstimatedWeight = verificationResult.estimatedWeight,
                aiConfidenceScore = verificationResult.confidence,
                aiProcessedAt = Date(),
                verificationStatus = VerificationStatus.PENDING // Ready for municipal verification
            )

            wasteSubmissionDao.updateSubmission(updatedSubmission)
            val nearestMunicipalId = findNearestMunicipalWorker(latitude, longitude)
            if (nearestMunicipalId != null) {
                assignToNearestMunicipal(updatedSubmission.id, latitude, longitude)
            }
            return updatedSubmission

        } catch (e: Exception) {
            // If AI processing fails, still keep submission for manual review
            val failedSubmission = submission.copy(
                verificationStatus = VerificationStatus.PENDING
            )
            wasteSubmissionDao.updateSubmission(failedSubmission)
            val nearestMunicipalId = findNearestMunicipalWorker(latitude, longitude)
            if (nearestMunicipalId != null) {
                assignToNearestMunicipal(failedSubmission.id, latitude, longitude)
            }
            return failedSubmission
        }
    }

    // Municipal worker operations
    suspend fun verifyWasteSubmission(
        submissionId: String,
        municipalWorkerId: String,
        isApproved: Boolean,
        actualWasteType: String,
        actualWeight: Float,
        comments: String,
        rejectionReason: String = ""
    ): WasteSubmission? {
        val submission = wasteSubmissionDao.getSubmissionById(submissionId) ?: return null

        val updatedSubmission = if (isApproved) {
            val rewardPoints = calculateRewardPoints(actualWeight)
            val impactScore = calculateImpactScore(actualWeight, actualWasteType)

            submission.copy(
                verificationStatus = VerificationStatus.VERIFIED,
                verifiedByMunicipalId = municipalWorkerId,
                verifiedAt = Date(),
                municipalComments = comments,
                actualWasteType = actualWasteType,
                actualWeight = actualWeight,
                rewardPoints = rewardPoints,
                impactScore = impactScore,
                isRejected = false
            )
        } else {
            submission.copy(
                verificationStatus = VerificationStatus.REJECTED,
                verifiedByMunicipalId = municipalWorkerId,
                verifiedAt = Date(),
                municipalComments = comments,
                isRejected = true,
                rejectionReason = rejectionReason
            )
        }

        wasteSubmissionDao.updateSubmission(updatedSubmission)

        // If approved, add rewards to citizen
        if (isApproved) {
            addRewardPoints(
                submission.citizenId,
                updatedSubmission.rewardPoints,
                "Waste Collection Verified",
                submissionId
            )
            userDao.addWasteCollected(submission.citizenId, actualWeight)
            userDao.addCO2Saved(submission.citizenId, updatedSubmission.impactScore)
        }

        // Increment municipal worker verification count
        municipalWorkerDao.incrementVerificationCount(municipalWorkerId)

        return updatedSubmission
    }

    // Get submissions for verification (Municipal workers)
    fun getPendingVerifications(): Flow<List<WasteSubmission>> {
        return wasteSubmissionDao.getPendingVerifications()
    }

    fun getAllSubmissions(): Flow<List<WasteSubmission>> {
        return wasteSubmissionDao.getAllSubmissions()
    }

    fun getSubmissionsVerifiedBy(municipalId: String): Flow<List<WasteSubmission>> {
        return wasteSubmissionDao.getSubmissionsVerifiedBy(municipalId)
    }

    // Citizen specific data
    fun getCitizenSubmissions(): Flow<List<WasteSubmission>> {
        val currentUserId =
            authService.getCurrentUserId() ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return wasteSubmissionDao.getSubmissionsByCitizen(currentUserId)
    }

    fun getCitizenTransactions(): Flow<List<RewardTransaction>> {
        val currentUserId =
            authService.getCurrentUserId() ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return rewardTransactionDao.getTransactionsByCitizen(currentUserId)
    }

    // Reward operations
    private suspend fun addRewardPoints(
        citizenId: String,
        points: Int,
        description: String,
        relatedSubmissionId: String? = null
    ) {
        val transaction = RewardTransaction(
            id = UUID.randomUUID().toString(),
            citizenId = citizenId,
            type = TransactionType.EARNED,
            points = points,
            description = description,
            timestamp = Date(),
            relatedSubmissionId = relatedSubmissionId
        )
        rewardTransactionDao.insertTransaction(transaction)
        userDao.addPoints(citizenId, points)
    }

    suspend fun redeemVoucher(voucherId: String): Boolean {
        val currentUserId = authService.getCurrentUserId() ?: return false
        val voucher = voucherDao.getVoucherById(voucherId)
        val user = getCurrentUser()
        
        if (voucher != null && user != null && user.totalPoints >= voucher.pointsCost) {
            // Create redemption transaction
            val transaction = RewardTransaction(
                id = UUID.randomUUID().toString(),
                citizenId = currentUserId,
                type = TransactionType.REDEEMED,
                points = -voucher.pointsCost,
                description = "Redeemed: ${voucher.title}",
                timestamp = Date(),
                voucherCode = generateVoucherCode(),
                expiryDate = Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000) // 30 days
            )
            
            rewardTransactionDao.insertTransaction(transaction)
            userDao.addPoints(currentUserId, -voucher.pointsCost)
            
            // Update voucher redemption count
            val updatedVoucher = voucher.copy(currentRedemptions = voucher.currentRedemptions + 1)
            voucherDao.updateVoucher(updatedVoucher)
            
            return true
        }
        return false
    }

    // Voucher operations
    fun getActiveVouchers(): Flow<List<Voucher>> {
        return voucherDao.getActiveVouchers()
    }

    fun getVouchersByCategory(category: VoucherCategory): Flow<List<Voucher>> {
        return voucherDao.getVouchersByCategory(category)
    }

    // Leaderboard operations
    fun getLeaderboard(): Flow<List<LeaderboardEntry>> {
        return leaderboardDao.getTopUsers()
    }

    suspend fun updateLeaderboard() {
        // In a real app, this would sync with server
        createSampleLeaderboardData()
    }

    // Zone operations
    fun getAllZones(): Flow<List<Zone>> {
        return zoneDao.getAllZones()
    }

    suspend fun adoptZone(zoneId: String): Boolean {
        val currentUserId = authService.getCurrentUserId() ?: return false
        val zone = zoneDao.getZoneById(zoneId)
        if (zone != null && zone.adoptedByCitizenId == null) {
            zoneDao.adoptZone(zoneId, currentUserId)
            return true
        }
        return false
    }

    fun getCitizenAdoptedZones(): Flow<List<Zone>> {
        val currentUserId =
            authService.getCurrentUserId() ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return zoneDao.getZonesByCitizen(currentUserId)
    }

    fun getMunicipalAssignedZones(): Flow<List<Zone>> {
        val currentUserId =
            authService.getCurrentUserId() ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return zoneDao.getZonesByMunicipal(currentUserId)
    }

    // Challenge operations
    fun getActiveChallenges(): Flow<List<Challenge>> {
        return challengeDao.getActiveChallenges()
    }

    fun getIndividualChallenges(): Flow<List<Challenge>> {
        return challengeDao.getIndividualChallenges()
    }

    fun getMunicipalChallenges(): Flow<List<Challenge>> {
        return challengeDao.getMunicipalChallenges()
    }

    // Stats operations
    suspend fun getCitizenStats(): CitizenStats {
        val currentUserId =
            authService.getCurrentUserId() ?: return CitizenStats(0, 0f, 0f, 0, 1, 0, 0, 0, 0, 0)
        val user = getCurrentUser() ?: return CitizenStats(0, 0f, 0f, 0, 1, 0, 0, 0, 0, 0)

        val now = System.currentTimeMillis()
        val dayStart = now - (24 * 60 * 60 * 1000)
        val weekStart = now - (7 * 24 * 60 * 60 * 1000)
        val monthStart = now - (30 * 24 * 60 * 60 * 1000)

        val dailySubmissions = wasteSubmissionDao.getDailySubmissionCount(currentUserId, dayStart)
        val weeklySubmissions = wasteSubmissionDao.getWeeklySubmissionCount(currentUserId, weekStart)
        val monthlySubmissions = wasteSubmissionDao.getMonthlySubmissionCount(currentUserId, monthStart)
        val pendingVerifications = wasteSubmissionDao.getPendingVerificationCount()
        
        val rank = leaderboardDao.getUserRank(currentUserId)?.rank ?: 0

        return CitizenStats(
            totalPoints = user.totalPoints,
            totalWasteCollected = user.totalWasteCollected,
            totalCO2Saved = user.totalCO2Saved,
            rank = rank,
            level = calculateUserLevel(user.totalPoints),
            submissionsThisWeek = weeklySubmissions,
            submissionsThisMonth = monthlySubmissions,
            submissionsToday = dailySubmissions,
            pendingVerifications = pendingVerifications,
            streakDays = 0 // TODO: Calculate streak
        )
    }

    suspend fun getMunicipalStats(): MunicipalStats {
        val currentUserId =
            authService.getCurrentUserId() ?: return MunicipalStats(0, 0f, 0, 0, 0, 0, 0, 0f)
        val municipalWorker = municipalWorkerDao.getMunicipalWorkerByUserId(currentUserId)
            ?: return MunicipalStats(0, 0f, 0, 0, 0, 0, 0, 0f)

        val now = System.currentTimeMillis()
        val dayStart = now - (24 * 60 * 60 * 1000)
        val weekStart = now - (7 * 24 * 60 * 60 * 1000)
        val monthStart = now - (30 * 24 * 60 * 60 * 1000)

        val todayVerifications =
            wasteSubmissionDao.getTodayVerificationCount(currentUserId, dayStart)
        val pendingVerifications = wasteSubmissionDao.getPendingVerificationCount()

        return MunicipalStats(
            totalVerifications = municipalWorker.verificationCount,
            verificationAccuracy = municipalWorker.accuracy,
            avgVerificationTime = 5, // TODO: Calculate actual time
            pendingVerifications = pendingVerifications,
            verifiedToday = todayVerifications,
            verifiedThisWeek = 0, // TODO: Calculate
            verifiedThisMonth = 0, // TODO: Calculate
            totalImpactCreated = 0f // TODO: Calculate
        )
    }

    suspend fun getGlobalStats(): GlobalStats {
        val leaderboard = leaderboardDao.getTopUsers().first()
        val zones = zoneDao.getAllZones().first()
        val citizenCount = userDao.getCitizenCount()
        val municipalCount = userDao.getMunicipalWorkerCount()

        val now = System.currentTimeMillis()
        val dayStart = now - (24 * 60 * 60 * 1000)

        val todaySubmissions = wasteSubmissionDao.getTodaySubmissionCount(dayStart)

        return GlobalStats(
            totalPlasticCollected = leaderboard.sumOf { it.wasteCollected.toDouble() }.toFloat(),
            totalCO2Reduced = leaderboard.sumOf { it.co2Saved.toDouble() }.toFloat(),
            totalUsers = citizenCount,
            totalMunicipalWorkers = municipalCount,
            topContributors = leaderboard.take(10),
            wasteHotspots = zones.sortedByDescending { it.totalWasteCollected }.take(5),
            todaySubmissions = todaySubmissions,
            todayVerifications = 0 // TODO: Calculate
        )
    }

    // Helper functions
    private fun calculateRewardPoints(weight: Float): Int {
        // Simple calculation: 10 points per kg
        return (weight * 10).toInt().coerceAtLeast(1) // Minimum 1 point
    }

    private fun calculateImpactScore(weight: Float, wasteType: String): Float {
        // CO2 impact calculation based on weight and type
        return weight * when (wasteType.lowercase()) {
            "bottle" -> 3.2f     // Higher impact for bottles
            "container" -> 2.8f   // Medium-high for containers
            "bag" -> 2.1f         // Lower impact but still significant
            "wrapper" -> 1.5f     // Minimal but measurable
            "cup" -> 2.0f         // Medium impact
            else -> 2.5f          // Default multiplier
        }
    }

    private fun calculateUserLevel(points: Int): Int {
        return when {
            points < 100 -> 1
            points < 500 -> 2
            points < 1000 -> 3
            points < 2500 -> 4
            points < 5000 -> 5
            else -> 6
        }
    }

    private fun generateVoucherCode(): String {
        return "T2C${UUID.randomUUID().toString().take(8).uppercase()}"
    }

    // Find nearest municipal worker based on location
    private suspend fun findNearestMunicipalWorker(latitude: Double, longitude: Double): String? {
        // Get all municipal workers
        val allMunicipalWorkers = municipalWorkerDao.getAllMunicipalWorkers()

        if (allMunicipalWorkers.isEmpty()) {
            android.util.Log.w("Trash2Cash", "No municipal workers found, using default assignment")
            return null
        }

        // For now, do round-robin assignment
        // In production, this would calculate actual distance based on worker locations
        val randomWorker = allMunicipalWorkers.random()
        android.util.Log.d(
            "Trash2Cash",
            "Assigned submission to municipal worker: ${randomWorker.id}"
        )

        return randomWorker.id
    }

    // Assign submission to nearest municipal worker
    private suspend fun assignToNearestMunicipal(
        submissionId: String,
        latitude: Double,
        longitude: Double
    ) {
        val nearestMunicipalId = findNearestMunicipalWorker(latitude, longitude)
        if (nearestMunicipalId != null) {
            // Update submission with assigned municipal worker
            val submission = wasteSubmissionDao.getSubmissionById(submissionId)
            submission?.let {
                val updated = it.copy(assignedMunicipalId = nearestMunicipalId)
                wasteSubmissionDao.updateSubmission(updated)
                android.util.Log.d(
                    "Trash2Cash",
                    "Submission $submissionId assigned to $nearestMunicipalId"
                )
            }
        }
    }

    // Sample data creation
    private suspend fun createSampleLeaderboardData() {
        leaderboardDao.clearLeaderboard()
        
        val sampleUsers = listOf(
            LeaderboardEntry("user1", "EcoChampion", 2500, 45.5f, 22.7f, 1, level = 6),
            LeaderboardEntry("user2", "GreenWarrior", 2200, 38.2f, 19.1f, 2, level = 5),
            LeaderboardEntry("user3", "PlasticSlayer", 1800, 32.1f, 16.0f, 3, level = 4),
            LeaderboardEntry("user4", "CleanupHero", 1500, 28.5f, 14.2f, 4, level = 3),
            LeaderboardEntry("user5", "WasteHunter", 1200, 22.3f, 11.1f, 5, level = 3)
        )
        
        sampleUsers.forEach { entry ->
            leaderboardDao.insertLeaderboardEntry(entry)
        }

        // Add current user if exists
        val currentUser = getCurrentUser()
        if (currentUser != null && currentUser.role == UserRole.CITIZEN) {
            val userEntry = LeaderboardEntry(
                citizenId = currentUser.id,
                userName = currentUser.name,
                points = currentUser.totalPoints,
                wasteCollected = currentUser.totalWasteCollected,
                co2Saved = currentUser.totalCO2Saved,
                rank = 6,
                level = calculateUserLevel(currentUser.totalPoints)
            )
            leaderboardDao.insertLeaderboardEntry(userEntry)
        }
    }

    suspend fun initializeSampleData() {
        createSampleUsers()
        createSampleVouchers()
        createSampleZones()
        createSampleChallenges()
        createSampleLeaderboardData()
    }

    private suspend fun createSampleUsers() {
        // Create demo citizen
        val citizenId = "citizen_demo"
        val citizenExists = getUserById(citizenId)

        if (citizenExists == null) {
            val citizen = User(
                id = citizenId,
                email = "citizen@demo.com",
                name = "Demo Citizen",
                phoneNumber = "9876543210",
                role = UserRole.CITIZEN,
                createdAt = Date(),
                isVerified = true,
                totalPoints = 150
            )
            val (hash, salt) = authService.hashPassword("demo123")
            createUser(citizen, hash, salt)
        } else {
            // User exists in database, but we need to recreate password hash in memory
            val (hash, salt) = authService.hashPassword("demo123")
            passwordHashes[citizenId] = Pair(hash, salt)
        }

        // Create demo municipal worker
        val municipalId = "municipal_demo"
        val municipalExists = getUserById(municipalId)

        if (municipalExists == null) {
            val municipal = User(
                id = municipalId,
                email = "municipal@demo.com",
                name = "Demo Municipal Worker",
                phoneNumber = "9876543211",
                role = UserRole.MUNICIPAL_WORKER,
                createdAt = Date(),
                isVerified = true
            )
            val (hash, salt) = authService.hashPassword("demo123")
            createUser(municipal, hash, salt)

            val municipalWorker = MunicipalWorker(
                id = "mw_demo",
                userId = municipalId,
                employeeId = "EMP001",
                department = "Waste Management",
                designation = "Verification Officer",
                joinDate = Date()
            )
            createMunicipalWorker(municipalWorker)
        } else {
            // User exists in database, but we need to recreate password hash in memory
            val (hash, salt) = authService.hashPassword("demo123")
            passwordHashes[municipalId] = Pair(hash, salt)
        }
    }

    private suspend fun createSampleVouchers() {
        val sampleVouchers = listOf(
            Voucher(
                id = "v1",
                title = "₹25 Mobile Recharge",
                description = "Free mobile recharge worth ₹25",
                pointsCost = 250,
                category = VoucherCategory.MOBILE_RECHARGE,
                partnerName = "RechargeNow",
                validUntil = Date(System.currentTimeMillis() + 90L * 24 * 60 * 60 * 1000),
                termsAndConditions = "Valid for all networks"
            ),
            Voucher(
                id = "v2",
                title = "₹50 Food Voucher",
                description = "Get ₹50 off on food delivery",
                pointsCost = 500,
                category = VoucherCategory.FOOD_DELIVERY,
                partnerName = "FoodApp",
                validUntil = Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000),
                termsAndConditions = "Valid for orders above ₹200"
            ),
            Voucher(
                id = "v3",
                title = "₹100 Shopping Voucher",
                description = "₹100 discount on online shopping",
                pointsCost = 1000,
                category = VoucherCategory.SHOPPING,
                partnerName = "ShopOnline",
                validUntil = Date(System.currentTimeMillis() + 60L * 24 * 60 * 60 * 1000),
                termsAndConditions = "Valid for orders above ₹500"
            )
        )
        
        sampleVouchers.forEach { voucher ->
            try {
                voucherDao.insertVoucher(voucher)
            } catch (e: Exception) {
                // Voucher already exists, skip
            }
        }
    }

    private suspend fun createSampleZones() {
        val sampleZones = listOf(
            Zone(
                id = "z1",
                name = "City Park",
                latitude = 28.6139,
                longitude = 77.2090,
                radius = 500f,
                address = "Central Park, Connaught Place",
                city = "New Delhi",
                state = "Delhi",
                pincode = "110001",
                wasteLevel = WasteLevel.MEDIUM,
                totalSubmissions = 15,
                description = "Popular park area needing regular cleanup"
            ),
            Zone(
                id = "z2",
                name = "Beach Area",
                latitude = 19.0760,
                longitude = 72.8777,
                radius = 1000f,
                address = "Marine Drive",
                city = "Mumbai",
                state = "Maharashtra",
                pincode = "400020",
                wasteLevel = WasteLevel.HIGH,
                totalSubmissions = 32,
                description = "Coastal area with plastic waste issues"
            ),
            Zone(
                id = "z3",
                name = "University Campus",
                latitude = 12.9716,
                longitude = 77.5946,
                radius = 800f,
                address = "Indian Institute of Science",
                city = "Bangalore",
                state = "Karnataka",
                pincode = "560012",
                wasteLevel = WasteLevel.LOW,
                totalSubmissions = 8,
                description = "Educational campus maintained by students"
            )
        )
        
        sampleZones.forEach { zone ->
            try {
                zoneDao.insertZone(zone)
            } catch (e: Exception) {
                // Zone already exists, skip
            }
        }
    }

    private suspend fun createSampleChallenges() {
        val now = Date()
        val oneWeekLater = Date(now.time + 7 * 24 * 60 * 60 * 1000)
        val oneMonthLater = Date(now.time + 30 * 24 * 60 * 60 * 1000)
        
        val sampleChallenges = listOf(
            Challenge(
                id = "c1",
                title = "Weekly Warrior",
                description = "Collect 5kg of plastic waste this week",
                startDate = now,
                endDate = oneWeekLater,
                targetPoints = 500,
                targetWaste = 5.0f,
                rewardPoints = 100,
                type = ChallengeType.INDIVIDUAL
            ),
            Challenge(
                id = "c2",
                title = "Campus Cleanup Champion",
                description = "College vs College cleanup competition",
                startDate = now,
                endDate = oneMonthLater,
                targetPoints = 2000,
                targetWaste = 50.0f,
                rewardPoints = 500,
                type = ChallengeType.GROUP,
                organizationName = "Green College Initiative"
            ),
            Challenge(
                id = "c3",
                title = "Municipal Excellence",
                description = "Complete 100 verifications with 95% accuracy",
                startDate = now,
                endDate = oneMonthLater,
                targetPoints = 1000,
                targetWaste = 0f,
                rewardPoints = 200,
                type = ChallengeType.MUNICIPAL,
                organizationName = "Municipal Department"
            )
        )
        
        sampleChallenges.forEach { challenge ->
            try {
                challengeDao.insertChallenge(challenge)
            } catch (e: Exception) {
                // Challenge already exists, skip
            }
        }
    }
}