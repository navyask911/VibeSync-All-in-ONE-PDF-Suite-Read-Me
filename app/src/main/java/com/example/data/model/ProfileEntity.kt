package com.example.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val age: Int,
    val occupation: String = "",
    val city: String = "",
    val distanceMiles: Int = 0,
    val bio: String = "",
    val interests: String = "", // Comma separated, e.g. "Photography, Rock Climbing, Vinyl"
    val relationshipGoal: String = "Long-term relationship", // e.g. "Long-term love", "Dating casually", "New friends"
    val promptQuestion: String = "About me",
    val promptAnswer: String = "",
    val gradientColorStart: Long = 0xFFFF5E62,
    val gradientColorEnd: Long = 0xFFFF9966,
    val avatarEmoji: String = "✨",
    val avatarUrl: String = "",
    val isVerified: Boolean = true,
    val likedMe: Boolean = false, // Has this person already liked current user? (100% Free to view!)
    val isSuperLikedMe: Boolean = false,
    val activityStatus: String = "Active today",
    val height: String = "5'7\"",
    val zodiacSign: String = "Gemini",
    // Anti-Spam & Developer Backend Controls
    val isBanned: Boolean = false,
    val isFlaggedSpam: Boolean = false,
    val isDeleted: Boolean = false,
    val accountStatus: String = "ACTIVE",
    val spamReportCount: Int = 0,
    val trustScore: Int = 98,
    val isMobileVerified: Boolean = true,
    val isRealFaceVerified: Boolean = true,
    val moderationNote: String = "Genuine Profile",
    // Relationship Ethics, Value Transparency & Global Demographics
    val breakupCount: Int = 0,
    val friendsCount: Int = 12,
    val country: String = "United States",
    val countryFlag: String = "🇺🇸",
    val state: String = "California",
    val district: String = "San Francisco County",
    val taluk: String = "San Francisco",
    val place: String = "Downtown Metro",
    val qualification: String = "Master of Science",
    val maritalStatus: String = "Single (Never Married)",
    val gender: String = "Female",
    val isDatingGoal: Boolean = true,
    val isOpenForDating: Boolean = true,
    val locationVisibility: String = "NO_ONE", // "EVERYONE", "PHONE_CONTACTS", "FRIENDS_ONLY", "SELECTED_PERSONS", "NO_ONE"
    val allowedLocationUserIds: String = "", // Comma-separated list of profile IDs allowed to view location
    val friendIds: String = "", // Comma-separated list of confirmed friend profile IDs
    val phoneNumber: String = "",
    val email: String = ""
) {
    fun getInterestList(): List<String> {
        return interests.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun getFriendIdList(): List<String> {
        return friendIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun getPhotoList(): List<String> {
        val list = mutableListOf<String>()
        if (avatarUrl.isNotBlank()) {
            if (avatarUrl.contains("|||")) {
                list.addAll(avatarUrl.split("|||").map { it.trim() }.filter { it.isNotBlank() })
            } else if (avatarUrl.contains(",")) {
                list.addAll(avatarUrl.split(",").map { it.trim() }.filter { it.isNotBlank() })
            } else {
                list.add(avatarUrl)
            }
        }
        if (list.isEmpty()) {
            val fallback = getEffectiveAvatarUrl()
            if (fallback.isNotBlank()) {
                list.add(fallback)
            }
        }
        return list.distinct()
    }

    fun getEffectiveAvatarUrl(): String {
        if (avatarUrl.isNotBlank()) {
            val first = avatarUrl.split("|||", ",").firstOrNull()?.trim() ?: ""
            if (first.isNotBlank()) return first
        }
        return when (id) {
            "p_maya" -> "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=800&q=80"
            "p_alex" -> "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=800&q=80"
            "p_sophia" -> "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=800&q=80"
            "p_marcus" -> "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=800&q=80"
            "p_elena" -> "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=800&q=80"
            "p_chloe" -> "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=800&q=80"
            "p_daniel" -> "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?auto=format&fit=crop&w=800&q=80"
            "p_liam" -> "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=800&q=80"
            "p_olivia" -> "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=800&q=80"
            "p_aisha" -> "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&w=800&q=80"
            "p_devon" -> "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?auto=format&fit=crop&w=800&q=80"
            "p_zara" -> "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&w=800&q=80"
            "p_rohan" -> "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?auto=format&fit=crop&w=800&q=80"
            "p_isabella" -> "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=800&q=80"
            "p_lukas" -> "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=800&q=80"
            "p_kenji" -> "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=800&q=80"
            "p_amber" -> "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=800&q=80"
            else -> {
                val isWoman = gender.contains("woman", ignoreCase = true) || gender.contains("female", ignoreCase = true)
                if (isWoman) {
                    val femaleList = listOf(
                        "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=800&q=80",
                        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=800&q=80",
                        "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=800&q=80",
                        "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=800&q=80",
                        "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=800&q=80"
                    )
                    femaleList[Math.abs(id.hashCode()) % femaleList.size]
                } else {
                    val maleList = listOf(
                        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=800&q=80",
                        "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=800&q=80",
                        "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?auto=format&fit=crop&w=800&q=80",
                        "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=800&q=80",
                        "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?auto=format&fit=crop&w=800&q=80"
                    )
                    maleList[Math.abs(id.hashCode()) % maleList.size]
                }
            }
        }
    }
}
