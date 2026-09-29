package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted user account credentials and biometric signature.
 * Enforces zero duplication of profiles across mobile numbers, Google Email, and facial biometrics.
 */
@Entity(tableName = "registered_accounts")
data class RegisteredAccountEntity(
    @PrimaryKey val id: String, // Unique account ID (e.g. mobile or UUID)
    val phoneNumber: String = "",
    val googleEmail: String = "",
    val userName: String = "",
    val userAge: Int = 0,
    val biometricHash: String = "", // Cryptographic / facial landmark signature saved to database
    val biometricRegisteredTimestamp: Long = 0L,
    val mpin: String = "",
    val isVerified: Boolean = true,
    val isDeleted: Boolean = false,
    val accountStatus: String = "ACTIVE",
    val registrationTimestamp: Long = System.currentTimeMillis(),
    val recoveryPhone: String = "",
    val recoveryEmail: String = ""
)
