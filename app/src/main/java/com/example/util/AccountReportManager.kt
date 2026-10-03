package com.example.util

import android.util.Log
import com.example.data.model.ProfileEntity
import com.example.data.repository.SocialConnectRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AccountReportManager
 *
 * Logic class orchestrating profile reporting and automated Anti-Spam moderation triggers.
 * When an inappropriate profile is flagged, this controller:
 * 1. Formats and validates the report submission
 * 2. Invokes [AntiSpamManager.evaluateReportedAccount] to run rate-limiting and threshold checks
 * 3. Updates persistent database metrics (Trust Score, Spam Flag, Report Count)
 * 4. Executes emergency account blocking & encryption key revocation if requested
 * 5. Returns a comprehensive evaluation outcome for user feedback and admin oversight
 */
object AccountReportManager {
    private const val TAG = "AccountReportManager"

    enum class SeverityLevel(val label: String, val badgeColorHex: Long) {
        CRITICAL("Critical Safety Violation", 0xFFD32F2F),
        HIGH("High Risk", 0xFFFF5722),
        MODERATE("Moderate Concern", 0xFFFF9800),
        LOW("Standard Notice", 0xFF1976D2)
    }

    data class ReportCategory(
        val id: String,
        val title: String,
        val subtitle: String,
        val iconEmoji: String,
        val severity: SeverityLevel,
        val defaultPenalty: Int,
        val isCritical: Boolean = false
    )

    data class ReportSubmission(
        val targetProfile: ProfileEntity,
        val reportedByUserId: String,
        val category: ReportCategory,
        val detailedNotes: String,
        val selectedEvidence: List<String>,
        val alsoBlockAndPurge: Boolean = true,
        val timestamp: Long = System.currentTimeMillis()
    )

    data class ReportOutcome(
        val success: Boolean,
        val targetProfileId: String,
        val targetProfileName: String,
        val categoryTitle: String,
        val evaluationResult: AntiSpamManager.AccountEvaluationResult,
        val wasBlockedAndSevered: Boolean,
        val summaryMessage: String
    )

    // Standardized Community Safety & Anti-Spam Categories
    val REPORT_CATEGORIES = listOf(
        ReportCategory(
            id = "HARASSMENT",
            title = "Harassment or Bullying",
            subtitle = "Threatening, abusive, stalking, or disrespectful behavior",
            iconEmoji = "⚠️",
            severity = SeverityLevel.HIGH,
            defaultPenalty = 25,
            isCritical = false
        ),
        ReportCategory(
            id = "INAPPROPRIATE_MEDIA",
            title = "Inappropriate / Explicit Photos",
            subtitle = "Nudity, sexually explicit photos, or unsolicited adult media",
            iconEmoji = "🔞",
            severity = SeverityLevel.HIGH,
            defaultPenalty = 30,
            isCritical = false
        ),
        ReportCategory(
            id = "FAKE_PROFILE",
            title = "Fake Profile / Catfish / Bot",
            subtitle = "Stolen photos, bot-like automation, deceptive identity",
            iconEmoji = "🎭",
            severity = SeverityLevel.MODERATE,
            defaultPenalty = 20,
            isCritical = false
        ),
        ReportCategory(
            id = "SCAM_FRAUD",
            title = "Scam, Crypto or Financial Fraud",
            subtitle = "Soliciting money, crypto investments, wire transfers, or gift cards",
            iconEmoji = "💳",
            severity = SeverityLevel.CRITICAL,
            defaultPenalty = 45,
            isCritical = true
        ),
        ReportCategory(
            id = "UNDERAGE_USER",
            title = "Underage User (< 18 Years)",
            subtitle = "Account belongs to or portrays an individual under 18",
            iconEmoji = "🚸",
            severity = SeverityLevel.CRITICAL,
            defaultPenalty = 50,
            isCritical = true
        ),
        ReportCategory(
            id = "HATE_SPEECH",
            title = "Hate Speech or Discrimination",
            subtitle = "Slurs, racism, bigotry, violent threats, or discriminatory abuse",
            iconEmoji = "🛑",
            severity = SeverityLevel.CRITICAL,
            defaultPenalty = 45,
            isCritical = true
        ),
        ReportCategory(
            id = "COMMERCIAL_SPAM",
            title = "Commercial Spam / Solicitations",
            subtitle = "Advertising external services, social links, sales, or paid channels",
            iconEmoji = "📢",
            severity = SeverityLevel.MODERATE,
            defaultPenalty = 20,
            isCritical = false
        ),
        ReportCategory(
            id = "OFF_PLATFORM_COERCION",
            title = "Off-Platform Coercion / Threat",
            subtitle = "Urging immediate move to unmonitored external chat for suspicious activity",
            iconEmoji = "🚨",
            severity = SeverityLevel.HIGH,
            defaultPenalty = 30,
            isCritical = false
        )
    )

    val EVIDENCE_CHECKLIST_OPTIONS = listOf(
        "Violating bio, prompts, or personal details",
        "Offensive, aggressive, or unsolicited chat messages",
        "Suspicious, deepfake, or stolen stock photos",
        "Solicited money, wire transfers, crypto, or gift cards",
        "Sent external suspicious links, social handles, or off-platform contact info"
    )

    /**
     * Executes the complete report pipeline:
     * - Triggers [AntiSpamManager] to evaluate report frequencies and rate limits
     * - Commits penalty score and spam status to Room DB
     * - Blocks and purges connections if requested
     */
    suspend fun submitReport(
        submission: ReportSubmission,
        repository: SocialConnectRepository
    ): ReportOutcome = withContext(Dispatchers.IO) {
        val target = submission.targetProfile
        Log.i(TAG, "Processing account report against '${target.name}' (${target.id}) under category: ${submission.category.title}")

        // 1. Trigger AntiSpamManager to evaluate account metrics in real-time
        val evalResult = AntiSpamManager.evaluateReportedAccount(
            targetUserId = target.id,
            targetProfile = target,
            reasonCategory = submission.category.title,
            detailedReason = submission.detailedNotes,
            evidenceCount = submission.selectedEvidence.size,
            isCriticalViolation = submission.category.isCritical
        )

        // 2. Persist updated trust score and spam flag in database
        val isFlagged = evalResult.isFlaggedSuspicious
        val newTrustScore = evalResult.updatedTrustScore
        val note = "Reported for [${submission.category.title}]: ${submission.detailedNotes.ifBlank { "Flagged by user community" }}"

        repository.recordReportOnProfile(
            profileId = target.id,
            trustScore = newTrustScore,
            isFlagged = isFlagged,
            note = note
        )

        // 3. Handle immediate blocking & connection severing if requested or critical
        var wasBlocked = false
        if (submission.alsoBlockAndPurge || submission.category.isCritical || evalResult.severityLevel == "CRITICAL") {
            repository.blockAndReportUser(
                profileId = target.id,
                reason = submission.category.title,
                details = submission.detailedNotes
            )
            wasBlocked = true
            Log.w(TAG, "Emergency Block enacted for ${target.id}: E2EE session terminated & profile hidden.")
        }

        val summaryMsg = if (isFlagged) {
            "🚨 Report submitted: ${target.name}'s account has been flagged for automated Anti-Spam quarantine (Trust Score: $newTrustScore%)."
        } else {
            "✅ Safety report submitted. ${target.name}'s trust score reduced to $newTrustScore%. Our Anti-Spam engine is monitoring."
        }

        ReportOutcome(
            success = true,
            targetProfileId = target.id,
            targetProfileName = target.name,
            categoryTitle = submission.category.title,
            evaluationResult = evalResult,
            wasBlockedAndSevered = wasBlocked,
            summaryMessage = summaryMsg
        )
    }
}
