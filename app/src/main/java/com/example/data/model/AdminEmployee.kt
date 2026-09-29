package com.example.data.model

data class AdminEmployee(
    val id: String,
    val name: String,
    val email: String,
    val roleId: String,
    val roleName: String,
    val status: String = "ACTIVE", // ACTIVE or SUSPENDED
    val joinedDate: String = "Today"
)

object DefaultAdminEmployees {
    val sampleStaff = listOf(
        AdminEmployee(
            id = "emp_01",
            name = "Sarah Jenkins",
            email = "sarah.j@vibesync.internal",
            roleId = "role_super_admin",
            roleName = "Super Admin (Owner)",
            status = "ACTIVE",
            joinedDate = "Sept 2026"
        ),
        AdminEmployee(
            id = "emp_02",
            name = "Devon Vance",
            email = "devon.v@vibesync.internal",
            roleId = "role_anti_spam",
            roleName = "Anti-Spam Specialist",
            status = "ACTIVE",
            joinedDate = "Sept 2026"
        ),
        AdminEmployee(
            id = "emp_03",
            name = "Elena Rostova",
            email = "elena.r@vibesync.internal",
            roleId = "role_moderator",
            roleName = "Content Moderator",
            status = "ACTIVE",
            joinedDate = "Sept 2026"
        )
    )
}
