package com.example.data.model

data class AdminRole(
    val id: String,
    val roleName: String,
    val description: String,
    val permissions: List<String>,
    val badgeColorHex: Long = 0xFFFF4081
) {
    val roleId: String get() = id
}

object DefaultAdminRoles {
    val SUPER_ADMIN = AdminRole(
        id = "role_super_admin",
        roleName = "Super Admin (Owner)",
        description = "Full unconstrained access to all dating platform backend controls.",
        permissions = listOf("MANAGE_STAFF", "BAN_USERS", "APPROVE_PROFILES", "AI_SPAM_SCANNER", "EDIT_SYSTEM"),
        badgeColorHex = 0xFFFF2A85
    )

    val MODERATOR = AdminRole(
        id = "role_moderator",
        roleName = "Content Moderator",
        description = "Review photo verifications, ban abusive accounts, purge spam suitors.",
        permissions = listOf("BAN_USERS", "APPROVE_PROFILES"),
        badgeColorHex = 0xFF7C4DFF
    )

    val ANTI_SPAM_SPECIALIST = AdminRole(
        id = "role_anti_spam",
        roleName = "Anti-Spam Specialist",
        description = "Execute AI bot scans, analyze bot behavior scores, and flag suspicious activities.",
        permissions = listOf("BAN_USERS", "AI_SPAM_SCANNER"),
        badgeColorHex = 0xFF00E5FF
    )

    val SUPPORT_AGENT = AdminRole(
        id = "role_support",
        roleName = "User Support Agent",
        description = "Handle user reports, assist with Mobile OTP issues, toggle genuine badges.",
        permissions = listOf("APPROVE_PROFILES"),
        badgeColorHex = 0xFF00E676
    )

    val allDefaultRoles = listOf(SUPER_ADMIN, MODERATOR, ANTI_SPAM_SPECIALIST, SUPPORT_AGENT)
}
