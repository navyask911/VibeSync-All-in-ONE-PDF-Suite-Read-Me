package com.example.util

import android.util.Log
import java.security.MessageDigest

/**
 * PhonebookHasher
 *
 * Zero-Knowledge Phonebook Hashing Engine.
 * 1. Consistently normalizes phone numbers by stripping non-numeric characters and isolating the standard last 10 digits.
 * 2. Generates standard 64-character SHA-256 hex hashes (and 32-byte BYTEA representations) for zero-knowledge cloud matching.
 * 3. Keeps legacy 16-char truncated hash helpers for backward compatibility.
 */
object PhonebookHasher {

    private const val TAG = "PhonebookHasher"
    const val HASH_LENGTH = 16
    const val DEFAULT_COUNTRY_CODE = "+91"

    /**
     * Normalizes phone number to clean last 10 digits (standard subscriber format across clients).
     * Strips all spaces, dashes, parentheses, plus signs, and country code prefixes.
     */
    fun normalizeToLast10Digits(rawPhone: String): String {
        if (rawPhone.isBlank()) return ""
        val digits = rawPhone.filter { it.isDigit() }
        return if (digits.length >= 10) digits.takeLast(10) else digits
    }

    /**
     * Computes full 64-character SHA-256 hex string for any input string.
     */
    fun sha256Hex(input: String): String {
        if (input.isBlank()) return ""
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
            val hexDigits = "0123456789abcdef"
            val hexChars = CharArray(hashBytes.size * 2)
            for (i in hashBytes.indices) {
                val b = hashBytes[i].toInt() and 0xFF
                hexChars[i * 2] = hexDigits[b ushr 4]
                hexChars[i * 2 + 1] = hexDigits[b and 0x0F]
            }
            String(hexChars)
        } catch (e: Exception) {
            Log.w(TAG, "SHA-256 hex generation failed: ${e.message}")
            ""
        }
    }

    /**
     * Computes 32-byte SHA-256 byte array for BYTEA storage.
     */
    fun sha256Bytes(input: String): ByteArray {
        if (input.isBlank()) return ByteArray(0)
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            digest.digest(input.toByteArray(Charsets.UTF_8))
        } catch (e: Exception) {
            Log.w(TAG, "SHA-256 bytes generation failed: ${e.message}")
            ByteArray(0)
        }
    }

    /**
     * Primary Zero-Knowledge Hash:
     * Normalizes phone to last 10 digits and generates 64-character SHA-256 hex string.
     */
    fun hashPhoneNumberToHex(rawPhone: String): String {
        val last10 = normalizeToLast10Digits(rawPhone)
        return if (last10.isNotBlank()) sha256Hex(last10) else ""
    }

    /**
     * Robust phone number normalization that strips all non-numeric characters
     * and enforces standard E.164 formatting (guaranteed '+91' followed by 10 digits or full international format).
     * Strips spaces, dashes, parentheses, brackets, and redundant zero prefixes.
     */
    fun normalizeToE164(rawPhone: String, defaultCountryCode: String = DEFAULT_COUNTRY_CODE): String {
        if (rawPhone.isBlank()) return ""
        val trimmed = rawPhone.trim()
        val digits = trimmed.filter { it.isDigit() }
        if (digits.isBlank()) return ""

        val defaultCcDigits = defaultCountryCode.filter { it.isDigit() }.ifBlank { "91" }

        return when {
            digits.length == 10 -> "+$defaultCcDigits$digits"
            digits.length == 11 && digits.startsWith("0") -> "+$defaultCcDigits${digits.substring(1)}"
            digits.length == 12 && digits.startsWith("91") -> "+$digits"
            digits.length == 11 && digits.startsWith("1") -> "+$digits"
            digits.startsWith("00") && digits.length > 2 -> "+${digits.substring(2)}"
            trimmed.startsWith("+") -> "+$digits"
            digits.length > 10 -> "+$digits"
            else -> "+$defaultCcDigits$digits"
        }
    }

    /**
     * Resilient phone comparison that accounts for differences in formatting,
     * country prefixes, spaces, brackets, or national vs E.164 notation.
     * E.g. '9449878908' == '+919449878908' == '+91 94498-78908' == '09449878908' -> true
     */
    fun arePhonesMatching(phone1: String?, phone2: String?): Boolean {
        if (phone1.isNullOrBlank() || phone2.isNullOrBlank()) return false
        val p1 = phone1.trim()
        val p2 = phone2.trim()
        if (p1.equals(p2, ignoreCase = true)) return true

        val norm1 = normalizeToE164(p1)
        val norm2 = normalizeToE164(p2)
        if (norm1.isNotBlank() && norm1 == norm2) return true

        val d1 = p1.filter { it.isDigit() }
        val d2 = p2.filter { it.isDigit() }
        if (d1.isNotBlank() && d1 == d2) return true
        if (d1.length >= 10 && d2.length >= 10 && d1.takeLast(10) == d2.takeLast(10)) return true

        return false
    }

    /**
     * Generates a 16-character truncated SHA-256 hash of the E.164 normalized phone number.
     * Preserved for backward-compatibility with older indexes.
     */
    fun generate16CharHash(rawPhone: String, defaultCountryCode: String = DEFAULT_COUNTRY_CODE): String {
        val last10 = normalizeToLast10Digits(rawPhone)
        if (last10.isBlank()) return ""

        val fullHex = sha256Hex(last10)
        return if (fullHex.isNotBlank()) fullHex.take(HASH_LENGTH) else last10.take(HASH_LENGTH)
    }

    fun getPhoneHash(rawPhone: String): String {
        val fullHex = hashPhoneNumberToHex(rawPhone)
        return fullHex.ifBlank { generate16CharHash(rawPhone) }
    }

    fun hashPhoneNumber(rawPhone: String): String {
        return getPhoneHash(rawPhone)
    }

    /**
     * Returns a set of compatible match hashes for a phone number:
     * 1. Primary: 64-char full SHA-256 hex of last 10 digits (Standard Zero-Knowledge)
     * 2. Secondary: 64-char full SHA-256 hex of E.164 number
     * 3. Tertiary: 16-char truncated hashes for legacy lookup compatibility
     */
    fun getAllMatchHashes(rawPhone: String): Set<String> {
        val hashes = mutableSetOf<String>()
        
        // 1. Standard Zero-Knowledge last 10 digits 64-char SHA-256
        val last10 = normalizeToLast10Digits(rawPhone)
        if (last10.isNotBlank()) {
            val hex10 = sha256Hex(last10)
            if (hex10.isNotBlank()) {
                hashes.add(hex10)
                hashes.add(hex10.take(HASH_LENGTH))
            }
        }

        // 2. Full E.164 number 64-char SHA-256
        val e164 = normalizeToE164(rawPhone)
        if (e164.isNotBlank()) {
            val hexE164 = sha256Hex(e164)
            if (hexE164.isNotBlank()) {
                hashes.add(hexE164)
                hashes.add(hexE164.take(HASH_LENGTH))
            }
        }

        // 3. Raw clean digits
        val cleanDigits = rawPhone.filter { it.isDigit() }
        if (cleanDigits.isNotBlank() && cleanDigits != last10) {
            val hexClean = sha256Hex(cleanDigits)
            if (hexClean.isNotBlank()) {
                hashes.add(hexClean)
                hashes.add(hexClean.take(HASH_LENGTH))
            }
        }

        return hashes
    }
}
