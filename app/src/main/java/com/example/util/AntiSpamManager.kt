package com.example.util

import android.util.Log
import com.example.data.model.ProfileEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * AntiSpamManager
 *
 * Real-time rate-limiter, interaction frequency tracker, and anti-spam detection engine.
 * Monitors:
 * 1. Match/Swipe Velocity (abnormal swipes/matches per minute)
 * 2. Report & Block Frequencies (excessive user safety reports)
 * 3. Rapid Messaging & Bot-like Activity
 * 4. Automatic backend threshold flagging for suspicious accounts
 */
object AntiSpamManager {
    private const val TAG = "AntiSpamManager"

    // Configuration Thresholds
    private const val MAX_SWIPES_PER_MINUTE_THRESHOLD = 30
    private const val MAX_MESSAGES_PER_MINUTE_THRESHOLD = 25
    private const val MAX_REPORTS_FLAG_THRESHOLD = 3
    private const val HIGH_SUSPICION_SCORE_THRESHOLD = 70 // 0-100 scale

    data class UserActivityMetrics(
        val userId: String,
        val swipeTimestamps: MutableList<Long> = mutableListOf(),
        val messageTimestamps: MutableList<Long> = mutableListOf(),
        val reportCount: AtomicInteger = AtomicInteger(0),
        val reportReasons: MutableList<String> = mutableListOf(),
        var matchCount: Int = 0,
        var trustScore: Int = 98,
        var isFlaggedSuspicious: Boolean = false,
        var flagReason: String? = null,
        var lastFlagTimestamp: Long = 0L
    )

    data class SpamScanResult(
        val scannedCount: Int,
        val flaggedCount: Int,
        val highRiskUsers: List<Pair<String, String>>, // userId to reason
        val scanTimestamp: Long = System.currentTimeMillis()
    )

    data class AccountEvaluationResult(
        val userId: String,
        val updatedTrustScore: Int,
        val totalReportCount: Int,
        val isFlaggedSuspicious: Boolean,
        val flagReason: String?,
        val severityLevel: String, // "CRITICAL", "HIGH", "MODERATE", "LOW"
        val suggestedAction: String,
        val evaluationLog: String
    )

    // Memory-efficient tracking registry
    private val userMetricsMap = ConcurrentHashMap<String, UserActivityMetrics>()

    private val _flaggedAccounts = MutableStateFlow<List<UserActivityMetrics>>(emptyList())
    val flaggedAccounts: StateFlow<List<UserActivityMetrics>> = _flaggedAccounts.asStateFlow()

    private val _liveScanLog = MutableStateFlow<List<String>>(emptyList())
    val liveScanLog: StateFlow<List<String>> = _liveScanLog.asStateFlow()

    /**
     * Records a swipe interaction and tests for swipe velocity / rate-limiting violations
     */
    fun recordSwipe(userId: String): Boolean {
        val metrics = getOrCreateMetrics(userId)
        val now = System.currentTimeMillis()
        
        synchronized(metrics.swipeTimestamps) {
            metrics.swipeTimestamps.add(now)
            // Retain only events within the last 60 seconds (Sliding Window)
            metrics.swipeTimestamps.removeAll { it < now - 60_000 }

            val velocity = metrics.swipeTimestamps.size
            if (velocity > MAX_SWIPES_PER_MINUTE_THRESHOLD) {
                flagUser(
                    metrics = metrics,
                    reason = "Abnormal swipe velocity ($velocity swipes/min > $MAX_SWIPES_PER_MINUTE_THRESHOLD max allowed)",
                    scorePenalty = 30
                )
                return false // Rate limit exceeded
            }
        }
        return true // Allowed
    }

    /**
     * Records a message sent and checks for rapid-fire / bot spam
     */
    fun recordMessageSent(userId: String): Boolean {
        val metrics = getOrCreateMetrics(userId)
        val now = System.currentTimeMillis()

        synchronized(metrics.messageTimestamps) {
            metrics.messageTimestamps.add(now)
            metrics.messageTimestamps.removeAll { it < now - 60_000 }

            val msgCount = metrics.messageTimestamps.size
            if (msgCount > MAX_MESSAGES_PER_MINUTE_THRESHOLD) {
                flagUser(
                    metrics = metrics,
                    reason = "Excessive chat messaging frequency ($msgCount msgs/min)",
                    scorePenalty = 25
                )
                return false // Throttled
            }
        }
        return true
    }

    /**
     * Records a community/safety report against a target account
     */
    fun recordUserReport(targetUserId: String, reason: String) {
        val metrics = getOrCreateMetrics(targetUserId)
        val reports = metrics.reportCount.incrementAndGet()
        metrics.reportReasons.add(reason)

        Log.w(TAG, "Report logged for $targetUserId. Total reports: $reports. Reason: $reason")

        if (reports >= MAX_REPORTS_FLAG_THRESHOLD) {
            flagUser(
                metrics = metrics,
                reason = "High report frequency: $reports safety reports logged (Latest: $reason)",
                scorePenalty = 40
            )
        } else {
            metrics.trustScore = (metrics.trustScore - 15).coerceAtLeast(0)
            updateFlaggedAccountsFlow()
        }
    }

    /**
     * Evaluates a reported account in real-time when flagged by a user through AccountReportScreen.
     * Evaluates report frequency, swipe velocity rate limiter, trust score, and safety severity.
     */
    fun evaluateReportedAccount(
        targetUserId: String,
        targetProfile: ProfileEntity?,
        reasonCategory: String,
        detailedReason: String,
        evidenceCount: Int = 1,
        isCriticalViolation: Boolean = false
    ): AccountEvaluationResult {
        val metrics = getOrCreateMetrics(targetUserId)
        if (targetProfile != null) {
            metrics.trustScore = targetProfile.trustScore
        }

        val reports = metrics.reportCount.incrementAndGet()
        val formattedReason = "[$reasonCategory] ${detailedReason.ifBlank { "Safety Concern" }}"
        metrics.reportReasons.add(formattedReason)

        // Penalty calculation based on severity & evidence
        val basePenalty = if (isCriticalViolation) 45 else 20
        val evidenceBonus = (evidenceCount * 5).coerceAtMost(20)
        val totalPenalty = basePenalty + evidenceBonus

        metrics.trustScore = (metrics.trustScore - totalPenalty).coerceAtLeast(0)

        var shouldFlag = false
        var flagReasonText = ""
        var severity = "LOW"
        var suggestedAction = "Logged for safety monitoring"

        if (isCriticalViolation || reports >= MAX_REPORTS_FLAG_THRESHOLD || metrics.trustScore < HIGH_SUSPICION_SCORE_THRESHOLD) {
            shouldFlag = true
            severity = if (isCriticalViolation || metrics.trustScore < 40) "CRITICAL" else "HIGH"
            flagReasonText = when {
                isCriticalViolation -> "Critical Safety Violation: $reasonCategory (${detailedReason.ifBlank { "Immediate Risk" }})"
                reports >= MAX_REPORTS_FLAG_THRESHOLD -> "High Report Frequency: $reports safety reports logged (Latest: $reasonCategory)"
                else -> "Trust Score fell below safety threshold (${metrics.trustScore}/100)"
            }
            suggestedAction = if (severity == "CRITICAL") "Immediate Account Suspension & Shadowban" else "Flagged for Human Moderation Review"
            flagUser(metrics, flagReasonText, 0)
        } else {
            severity = if (reports == 2) "MODERATE" else "LOW"
            suggestedAction = "Trust Score reduced by $totalPenalty pts. Monitored for repeat flags."
            updateFlaggedAccountsFlow()
        }

        val logEntry = "🚨 Anti-Spam Evaluation on $targetUserId: Severity=$severity, Reports=$reports, TrustScore=${metrics.trustScore}%, Action=$suggestedAction"
        _liveScanLog.value = (_liveScanLog.value + logEntry).takeLast(50)
        Log.w(TAG, logEntry)

        return AccountEvaluationResult(
            userId = targetUserId,
            updatedTrustScore = metrics.trustScore,
            totalReportCount = reports,
            isFlaggedSuspicious = shouldFlag || metrics.isFlaggedSuspicious,
            flagReason = metrics.flagReason ?: flagReasonText.ifBlank { null },
            severityLevel = severity,
            suggestedAction = suggestedAction,
            evaluationLog = logEntry
        )
    }

    /**
     * Runs comprehensive anti-spam threshold evaluation across a list of profile entities
     */
    fun evaluateProfiles(profiles: List<ProfileEntity>): SpamScanResult {
        var flagged = 0
        val highRisk = mutableListOf<Pair<String, String>>()
        val logs = mutableListOf<String>()

        profiles.forEach { profile ->
            val metrics = getOrCreateMetrics(profile.id)
            metrics.trustScore = profile.trustScore

            var shouldFlag = false
            var reason = ""

            // 1. Report frequency check
            if (profile.spamReportCount >= MAX_REPORTS_FLAG_THRESHOLD || metrics.reportCount.get() >= MAX_REPORTS_FLAG_THRESHOLD) {
                shouldFlag = true
                reason = "Excessive user safety reports (${profile.spamReportCount} reports)"
            }
            // 2. Trust Score threshold check
            else if (profile.trustScore < 50) {
                shouldFlag = true
                reason = "Critically degraded trust score (${profile.trustScore}/100)"
            }
            // 3. Unverified + high report count combination
            else if (!profile.isRealFaceVerified && profile.spamReportCount >= 2) {
                shouldFlag = true
                reason = "Unverified 3D face liveness with multiple community flags"
            }
            // 4. Rate limiter metrics violation
            else if (metrics.isFlaggedSuspicious) {
                shouldFlag = true
                reason = metrics.flagReason ?: "Rate-limit threshold violation"
            }

            if (shouldFlag) {
                flagUser(metrics, reason, 0)
                highRisk.add(profile.id to reason)
                logs.add("🚨 Flagged account '${profile.name}' (ID: ${profile.id}): $reason")
                flagged++
            } else {
                logs.add("✅ Profile '${profile.name}' verified clean (Trust: ${profile.trustScore}%)")
            }
        }

        _liveScanLog.value = logs.takeLast(50)
        Log.i(TAG, "Spam scan completed: ${profiles.size} scanned, $flagged flagged suspicious.")
        return SpamScanResult(
            scannedCount = profiles.size,
            flaggedCount = flagged,
            highRiskUsers = highRisk
        )
    }

    /**
     * Marks a user as suspicious and notifies backend moderation
     */
    private fun flagUser(metrics: UserActivityMetrics, reason: String, scorePenalty: Int) {
        metrics.isFlaggedSuspicious = true
        metrics.flagReason = reason
        metrics.lastFlagTimestamp = System.currentTimeMillis()
        if (scorePenalty > 0) {
            metrics.trustScore = (metrics.trustScore - scorePenalty).coerceAtLeast(0)
        }
        Log.w(TAG, "⚠️ Suspicious account flagged: User=${metrics.userId}, Reason=$reason, Score=${metrics.trustScore}")
        updateFlaggedAccountsFlow()
    }

    /**
     * Clears suspicious flag for an account upon admin review / manual unban
     */
    fun clearFlag(userId: String) {
        userMetricsMap[userId]?.let { metrics ->
            metrics.isFlaggedSuspicious = false
            metrics.flagReason = null
            metrics.trustScore = 95
            metrics.reportCount.set(0)
            updateFlaggedAccountsFlow()
            Log.i(TAG, "Cleared anti-spam flag for user: $userId")
        }
    }

    private fun getOrCreateMetrics(userId: String): UserActivityMetrics {
        return userMetricsMap.computeIfAbsent(userId) {
            UserActivityMetrics(userId = it)
        }
    }

    private fun updateFlaggedAccountsFlow() {
        _flaggedAccounts.value = userMetricsMap.values.filter { it.isFlaggedSuspicious }.toList()
    }
}
