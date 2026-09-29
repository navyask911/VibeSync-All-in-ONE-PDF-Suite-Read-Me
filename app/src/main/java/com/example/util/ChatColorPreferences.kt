package com.example.util

import android.content.Context
import androidx.compose.ui.graphics.Color

/**
 * ChatColorPreferences
 *
 * Manages zero-database, 100% local-only chat text color preferences using SharedPreferences.
 * Supabase and cloud backends remain completely untouched.
 */
object ChatColorPreferences {
    private const val PREFS_NAME = "vibesync_chat_local_prefs"
    private const val KEY_CHAT_TEXT_COLOR = "local_chat_text_color_hex"
    const val DEFAULT_COLOR_HEX = "#111B21"

    data class ChatColorOption(
        val id: String,
        val name: String,
        val hex: String,
        val color: Color,
        val description: String
    )

    val PRESET_COLORS = listOf(
        ChatColorOption(
            id = "deep_charcoal",
            name = "Deep Charcoal",
            hex = "#111B21",
            color = Color(0xFF111B21),
            description = "Crisp, classic high-contrast styling"
        ),
        ChatColorOption(
            id = "midnight_navy",
            name = "Classic Midnight Navy",
            hex = "#0F172A",
            color = Color(0xFF0F172A),
            description = "Subtle deep slate blue tone"
        ),
        ChatColorOption(
            id = "forest_slate",
            name = "Forest Slate",
            hex = "#064E3B",
            color = Color(0xFF064E3B),
            description = "Calming rich emerald tint"
        ),
        ChatColorOption(
            id = "royal_violet",
            name = "Royal Violet",
            hex = "#3B0764",
            color = Color(0xFF3B0764),
            description = "Sophisticated regal plum essence"
        ),
        ChatColorOption(
            id = "warm_espresso",
            name = "Warm Espresso",
            hex = "#3E2723",
            color = Color(0xFF3E2723),
            description = "Rich, cozy chocolate earth tone"
        ),
        ChatColorOption(
            id = "obsidian_onyx",
            name = "Obsidian Onyx",
            hex = "#18181B",
            color = Color(0xFF18181B),
            description = "Modern dark-slate contrast"
        ),
        ChatColorOption(
            id = "crimson_plum",
            name = "Crimson Plum",
            hex = "#4A0E17",
            color = Color(0xFF4A0E17),
            description = "Warm romantic wine tone"
        ),
        ChatColorOption(
            id = "ocean_indigo",
            name = "Ocean Indigo",
            hex = "#1E1B4B",
            color = Color(0xFF1E1B4B),
            description = "Deep nautical sapphire tone"
        )
    )

    fun getSelectedColorHex(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CHAT_TEXT_COLOR, DEFAULT_COLOR_HEX) ?: DEFAULT_COLOR_HEX
    }

    fun setSelectedColorHex(context: Context, hex: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CHAT_TEXT_COLOR, hex).apply()
    }

    fun parseColor(hex: String): Color {
        return try {
            val cleanHex = hex.removePrefix("#")
            val colorLong = cleanHex.toLong(16)
            if (cleanHex.length == 6) {
                Color(0xFF000000 or colorLong)
            } else if (cleanHex.length == 8) {
                Color(colorLong)
            } else {
                Color(0xFF111B21)
            }
        } catch (_: Exception) {
            Color(0xFF111B21)
        }
    }
}
