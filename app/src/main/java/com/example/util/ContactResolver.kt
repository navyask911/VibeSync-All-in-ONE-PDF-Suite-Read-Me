package com.example.util

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.model.PhoneContact
import com.example.data.model.ProfileEntity
import java.util.concurrent.ConcurrentHashMap

/**
 * ContactResolver
 *
 * Utility class using ContactsContract to fetch local contact names by phone number,
 * exposing functions to match these names against the current chat session's participants,
 * overriding any encrypted profile strings, raw phone numbers, or hash values.
 */
class ContactResolver(private val context: Context) {

    private val appContext: Context = context.applicationContext

    init {
        appContextRef = appContext
    }

    /**
     * Queries ContactsContract.CommonDataKinds.Phone for all contacts and returns
     * a lookup map connecting various phone number representations to their local display name.
     */
    fun queryContacts(): Map<String, String> {
        return fetchLocalContactNames(appContext)
    }

    /**
     * Fetches local contact names by phone number using ContactsContract.
     */
    fun fetchLocalContactNames(): Map<String, String> {
        return fetchLocalContactNames(appContext)
    }

    /**
     * Resolves a local display name for a specific phone number.
     */
    fun fetchContactNameByPhoneNumber(phoneNumber: String): String? {
        return fetchContactNameByPhoneNumber(appContext, phoneNumber)
    }

    /**
     * Matches a single participant from the current chat session against device contacts,
     * overriding any encrypted profile strings or raw numbers.
     */
    fun matchChatSessionParticipant(participant: ProfileEntity): ProfileEntity {
        return matchChatSessionParticipant(appContext, participant)
    }

    /**
     * Matches a list of participants from the current chat session against device contacts,
     * overriding any encrypted profile strings or raw numbers.
     */
    fun matchChatSessionParticipants(participants: List<ProfileEntity>): List<ProfileEntity> {
        return matchChatSessionParticipants(appContext, participants)
    }

    /**
     * Resolves a local display name for the given phone number or ID.
     */
    fun resolve(phoneOrId: String?): String? {
        if (phoneOrId.isNullOrBlank()) return null
        return fetchContactNameByPhoneNumber(appContext, phoneOrId) ?: resolveContactName(phoneOrId)
    }

    companion object {
        private const val TAG = "ContactResolver"

        @Volatile
        var appContextRef: Context? = null

        // In-memory cache for ultra-fast UI lookups
        private val cachedPhoneToNameMap = ConcurrentHashMap<String, String>()
        private var isInitialized = false

        /**
         * Initializes or reloads contacts from ContentResolver.
         */
        fun init(context: Context) {
            appContextRef = context.applicationContext
            reload(context)
        }

        /**
         * Reloads phonebook contacts by querying ContactsContract.CommonDataKinds.Phone.
         */
        fun reload(context: Context) {
            try {
                appContextRef = context.applicationContext
                val contacts = fetchLocalContactNames(context)
                cachedPhoneToNameMap.putAll(contacts)
                isInitialized = true
                Log.i(TAG, "Loaded ${cachedPhoneToNameMap.size} cached contact entries.")
            } catch (e: Exception) {
                Log.w(TAG, "Error reloading contacts: ${e.message}")
            }
        }

        /**
         * Fetches all local contact names from ContactsContract.CommonDataKinds.Phone.
         */
        fun fetchLocalContactNames(context: Context): Map<String, String> {
            val contactMap = mutableMapOf<String, String>()
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                Log.d(TAG, "READ_CONTACTS permission not granted yet.")
                // Populate demo contacts for seamless testing if no permissions
                for (demo in PhonebookHelper.getDemoPhonebookContacts()) {
                    indexNumber(demo.phoneNumber, demo.name, contactMap)
                }
                return contactMap
            }

            try {
                val projection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                )

                val cursor = context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    null,
                    null,
                    "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
                )

                cursor?.use {
                    val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                    while (it.moveToNext()) {
                        val rawName = if (nameIndex >= 0) it.getString(nameIndex)?.trim() ?: "" else ""
                        val rawNumber = if (numberIndex >= 0) it.getString(numberIndex)?.trim() ?: "" else ""

                        if (rawName.isNotBlank() && rawNumber.isNotBlank()) {
                            indexNumber(rawNumber, rawName, contactMap)
                        }
                    }
                }
                Log.i(TAG, "ContactResolver queried ${contactMap.size} contact mappings from device phonebook.")
            } catch (e: Exception) {
                Log.w(TAG, "Error querying ContactsContract: ${e.message}")
            }

            // Also index demo/seed contacts as fallback
            for (demo in PhonebookHelper.getDemoPhonebookContacts()) {
                indexNumber(demo.phoneNumber, demo.name, contactMap)
            }

            return contactMap
        }

        /**
         * Fetches a contact's saved name for a specific phone number using ContactsContract.PhoneLookup
         * and CommonDataKinds.Phone.
         */
        fun fetchContactNameByPhoneNumber(context: Context?, phoneNumber: String): String? {
            if (phoneNumber.isBlank()) return null
            val clean = phoneNumber.trim()

            // 1. Check in-memory cached map
            resolveContactName(clean)?.let { return it }

            val ctx = context ?: appContextRef ?: return findInFallbackContacts(clean)

            val hasPermission = ContextCompat.checkSelfPermission(
                ctx,
                android.Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                return findInFallbackContacts(clean)
            }

            // 2. Query ContactsContract.PhoneLookup (Android's standard dialer phone matcher)
            try {
                val lookupUri = Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    Uri.encode(clean)
                )
                val projection = arrayOf(
                    ContactsContract.PhoneLookup.DISPLAY_NAME
                )
                ctx.contentResolver.query(lookupUri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                        if (nameIndex >= 0) {
                            val resolvedName = cursor.getString(nameIndex)?.trim()
                            if (!resolvedName.isNullOrBlank()) {
                                indexContact(clean, resolvedName)
                                return resolvedName
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "PhoneLookup query exception for $clean: ${e.message}")
            }

            // 3. Query ContactsContract.CommonDataKinds.Phone by last 10 digits
            try {
                val digits = clean.filter { it.isDigit() }
                if (digits.length >= 7) {
                    val last10 = digits.takeLast(10)
                    val projection = arrayOf(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    )
                    val selection = "${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?"
                    val selectionArgs = arrayOf("%$last10%")
                    ctx.contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        projection,
                        selection,
                        selectionArgs,
                        null
                    )?.use { cursor ->
                        val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        while (cursor.moveToNext()) {
                            if (nameIdx >= 0) {
                                val name = cursor.getString(nameIdx)?.trim()
                                if (!name.isNullOrBlank()) {
                                    indexContact(clean, name)
                                    return name
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "CommonDataKinds.Phone query exception for $clean: ${e.message}")
            }

            return findInFallbackContacts(clean)
        }

        private fun findInFallbackContacts(phoneNumber: String): String? {
            val digits = phoneNumber.filter { it.isDigit() }
            val last10 = if (digits.length >= 10) digits.takeLast(10) else digits

            for (demo in PhonebookHelper.getDemoPhonebookContacts()) {
                val demoDigits = demo.phoneNumber.filter { it.isDigit() }
                val demoLast10 = if (demoDigits.length >= 10) demoDigits.takeLast(10) else demoDigits
                if ((last10.isNotBlank() && last10 == demoLast10) || demo.phoneNumber == phoneNumber) {
                    return demo.name
                }
            }
            return null
        }

        /**
         * Checks whether a phone number or participant is saved in the local address book.
         */
        fun isLocalAddressBookContact(context: Context? = null, phoneOrId: String?): Boolean {
            if (phoneOrId.isNullOrBlank()) return false
            val ctx = context ?: appContextRef
            val name = if (ctx != null) fetchContactNameByPhoneNumber(ctx, phoneOrId) else resolveContactName(phoneOrId)
            return !name.isNullOrBlank()
        }

        /**
         * Formats a phone number or contact identifier into clean national/E.164 display format.
         * e.g., "+91 98765 43210" or "+1 (555) 014-2345".
         * Never displays raw cipher strings or hash noise.
         */
        fun formatPhoneNumberForDisplay(phoneOrId: String?): String {
            if (phoneOrId.isNullOrBlank()) return "VibeSync User"
            val trimmed = phoneOrId.trim()

            // If phoneOrId itself is an encrypted ciphertext or hash, DO NOT extract digits from it!
            if (isGarbledOrEncrypted(trimmed)) return "VibeSync User"

            val digits = trimmed.filter { it.isDigit() }
            if (digits.length < 7) {
                return if (trimmed.isNotBlank() && !isGarbledOrEncrypted(trimmed)) trimmed else "VibeSync User"
            }

            return when {
                // 10 digits (Standard Indian national mobile)
                digits.length == 10 -> "+91 ${digits.substring(0, 5)} ${digits.substring(5)}"

                // 12 digits starting with country code 91
                digits.length == 12 && digits.startsWith("91") -> {
                    val sub = digits.substring(2)
                    "+91 ${sub.substring(0, 5)} ${sub.substring(5)}"
                }

                // 11 digits starting with trunk 0
                digits.length == 11 && digits.startsWith("0") -> {
                    val sub = digits.substring(1)
                    "+91 ${sub.substring(0, 5)} ${sub.substring(5)}"
                }

                // 11 digits starting with 1 (North American format)
                digits.length == 11 && digits.startsWith("1") -> {
                    "+1 (${digits.substring(1, 4)}) ${digits.substring(4, 7)}-${digits.substring(7)}"
                }

                // E.164 starting with '+'
                trimmed.startsWith("+") -> {
                    val ccLen = if (digits.length > 10) digits.length - 10 else 2
                    val cc = digits.take(ccLen)
                    val rest = digits.drop(ccLen)
                    "+$cc ${rest.chunked(5).joinToString(" ")}"
                }

                // Any number with >= 10 digits
                digits.length >= 10 -> {
                    val last10 = digits.takeLast(10)
                    "+91 ${last10.substring(0, 5)} ${last10.substring(5)}"
                }

                else -> "+$digits"
            }
        }

        /**
         * Exposes a function to match local contact names against the current chat session's participants,
         * overriding any encrypted profile strings, raw phone numbers, or hash noise.
         *
         * If the contact is not saved in the local address book, it cleanly falls back
         * to displaying the clean decrypted phone number (formatted national/E.164 number)
         * instead of raw cipher strings.
         */
        fun matchChatSessionParticipant(context: Context? = null, participant: ProfileEntity): ProfileEntity {
            val ctx = context ?: appContextRef
            val phoneKey = participant.phoneNumber.ifBlank { participant.id }

            // 1. Try to resolve using phoneNumber or participant ID from ContactsContract
            var localSavedName: String? = null
            if (ctx != null) {
                localSavedName = fetchContactNameByPhoneNumber(ctx, phoneKey)
            }
            if (localSavedName.isNullOrBlank()) {
                localSavedName = resolveContactName(phoneKey)
            }

            // 2. If participant.name looks like a phone number, extract digits and try to resolve in phonebook
            if (localSavedName.isNullOrBlank() && isPhoneNumberString(participant.name)) {
                val extractedDigits = participant.name.filter { it.isDigit() }
                if (ctx != null && extractedDigits.isNotBlank()) {
                    localSavedName = fetchContactNameByPhoneNumber(ctx, extractedDigits)
                }
                if (localSavedName.isNullOrBlank() && extractedDigits.isNotBlank()) {
                    localSavedName = resolveContactName(extractedDigits)
                }
            }

            // 3. If local saved contact name was found, the contact exists in local address book!
            if (!localSavedName.isNullOrBlank()) {
                val cleanPhone = if (isGarbledOrEncrypted(participant.phoneNumber)) {
                    formatPhoneNumberForDisplay(phoneKey)
                } else {
                    participant.phoneNumber
                }
                return participant.copy(name = localSavedName, phoneNumber = cleanPhone)
            }

            // 4. Contact is NOT saved in local address book (Unsaved Contact):
            // Fall back to displaying the clean decrypted phone number (formatted national/E.164 number)
            // instead of raw cipher strings or garbled text!
            val rawName = participant.name
            val isCipherOrGarbled = isGarbledOrEncrypted(rawName) || (rawName.length > 25 && !rawName.contains(" "))

            // Extract the cleanest available phone number representation
            val candidatePhone = when {
                !isGarbledOrEncrypted(participant.phoneNumber) && participant.phoneNumber.any { it.isDigit() } -> participant.phoneNumber
                !isGarbledOrEncrypted(participant.id) && participant.id.any { it.isDigit() } -> participant.id
                else -> phoneKey
            }
            val formattedCleanPhone = formatPhoneNumberForDisplay(candidatePhone)

            val resolvedName = if (isCipherOrGarbled || isPhoneNumberString(rawName)) {
                formattedCleanPhone
            } else {
                rawName.trim()
            }

            val sanitizedPhone = if (isGarbledOrEncrypted(participant.phoneNumber)) {
                formattedCleanPhone
            } else {
                participant.phoneNumber
            }

            return participant.copy(
                name = resolvedName,
                phoneNumber = sanitizedPhone
            )
        }

        /**
         * Exposes a function to match local contact names against all participants in the chat session,
         * overriding any encrypted profile strings.
         */
        fun matchChatSessionParticipants(
            context: Context? = null,
            participants: List<ProfileEntity>
        ): List<ProfileEntity> {
            val ctx = context ?: appContextRef
            return participants.map { matchChatSessionParticipant(ctx, it) }
        }

        /**
         * Resolves a participant's display name from phonebook.
         * If the contact is not saved in the local address book, it cleanly falls back
         * to displaying the formatted national/E.164 phone number instead of raw cipher strings.
         */
        fun resolveParticipantDisplayName(
            context: Context? = null,
            phoneOrId: String,
            currentName: String? = null
        ): String {
            val ctx = context ?: appContextRef
            val cleanKey = sanitizePhone(phoneOrId)
            val cleanCurrentName = sanitizeName(currentName)

            // 1. Try fetching saved name from phonebook by phoneOrId
            val fromPhone = if (ctx != null && cleanKey.isNotBlank()) {
                fetchContactNameByPhoneNumber(ctx, cleanKey)
            } else if (cleanKey.isNotBlank()) {
                resolveContactName(cleanKey)
            } else null

            if (!fromPhone.isNullOrBlank() && !fromPhone.equals("null", ignoreCase = true)) {
                return fromPhone.trim()
            }

            // 2. If currentName contains a phone number, try fetching saved name for that number
            if (cleanCurrentName.isNotBlank() && isPhoneNumberString(cleanCurrentName)) {
                val fromCurrentPhone = if (ctx != null) {
                    fetchContactNameByPhoneNumber(ctx, cleanCurrentName)
                } else {
                    resolveContactName(cleanCurrentName)
                }
                if (!fromCurrentPhone.isNullOrBlank() && !fromCurrentPhone.equals("null", ignoreCase = true)) {
                    return fromCurrentPhone.trim()
                }
            }

            // 3. If currentName is clean human text (NOT encrypted, NOT a raw cipher string, NOT placeholder, NOT "null"), use it
            if (cleanCurrentName.isNotBlank() && !isGarbledOrEncrypted(cleanCurrentName) && !isPhoneNumberString(cleanCurrentName) && cleanCurrentName.length <= 30) {
                return cleanCurrentName.trim()
            }

            // 4. Contact is NOT in local address book: Fall back to clean formatted phone number
            if (cleanKey.isNotBlank() && cleanKey.any { it.isDigit() }) {
                val formatted = formatPhoneNumberForDisplay(cleanKey)
                if (formatted.isNotBlank() && !formatted.equals("null", ignoreCase = true)) {
                    return formatted
                }
            }

            return "VibeSync Contact"
        }

        /**
         * Indexes a phone number into multiple lookup keys for instant matching.
         */
        fun indexContact(rawNumber: String, displayName: String) {
            if (displayName.isBlank() || rawNumber.isBlank()) return
            indexNumber(rawNumber, displayName, cachedPhoneToNameMap)
        }

        private fun indexNumber(rawNumber: String, displayName: String, targetMap: MutableMap<String, String>) {
            val cleanName = displayName.trim()
            val trimmedNumber = rawNumber.trim()
            targetMap[trimmedNumber] = cleanName

            // Index 16-character phone hash of the raw number
            val rawHash = com.example.util.PhonebookHasher.generate16CharHash(trimmedNumber)
            if (rawHash.isNotBlank()) {
                targetMap[rawHash] = cleanName
            }

            // Digits-only indexing
            val digitsOnly = trimmedNumber.filter { it.isDigit() }
            if (digitsOnly.isNotBlank()) {
                targetMap[digitsOnly] = cleanName
                val digitsHash = com.example.util.PhonebookHasher.generate16CharHash(digitsOnly)
                if (digitsHash.isNotBlank()) {
                    targetMap[digitsHash] = cleanName
                }
                if (digitsOnly.length >= 10) {
                    val last10 = digitsOnly.takeLast(10)
                    targetMap[last10] = cleanName
                    val last10Hash = com.example.util.PhonebookHasher.generate16CharHash(last10)
                    if (last10Hash.isNotBlank()) {
                        targetMap[last10Hash] = cleanName
                    }
                }
            }

            // Without country code '+' indexing
            val withoutPlus = trimmedNumber.replace("+", "").trim()
            if (withoutPlus.isNotBlank()) {
                targetMap[withoutPlus] = cleanName
                val withoutPlusHash = com.example.util.PhonebookHasher.generate16CharHash(withoutPlus)
                if (withoutPlusHash.isNotBlank()) {
                    targetMap[withoutPlusHash] = cleanName
                }
            }

            // Standardized with +91 and 91
            if (digitsOnly.length == 10) {
                targetMap["+91$digitsOnly"] = cleanName
                targetMap["91$digitsOnly"] = cleanName
                targetMap["+91 $digitsOnly"] = cleanName
            }
        }

        /**
         * Resolves a local contact name from phone number or user ID from memory cache.
         */
        fun resolveContactName(phoneOrId: String?): String? {
            if (phoneOrId.isNullOrBlank()) return null
            val query = phoneOrId.trim()

            // 1. Direct match
            cachedPhoneToNameMap[query]?.let { return it }

            // 2. Digits-only match (full & last 10 digits)
            val digits = query.filter { it.isDigit() }
            if (digits.isNotBlank()) {
                cachedPhoneToNameMap[digits]?.let { return it }
                if (digits.length >= 10) {
                    cachedPhoneToNameMap[digits.takeLast(10)]?.let { return it }
                }
            }

            // 3. Without plus
            val withoutPlus = query.replace("+", "").trim()
            if (withoutPlus.isNotBlank()) {
                cachedPhoneToNameMap[withoutPlus]?.let { return it }
            }

            return null
        }

        /**
         * Checks if a string is a phone number rather than a contact's display name.
         */
        fun isPhoneNumberString(text: String?): Boolean {
            if (text.isNullOrBlank()) return false
            val trimmed = text.trim()
            val digits = trimmed.filter { it.isDigit() }
            if (digits.length >= 7 && trimmed.all { it.isDigit() || it in "+ -()./ " }) return true
            if (trimmed.startsWith("Contact (") && trimmed.endsWith(")")) return true
            if (trimmed.startsWith("+") && digits.length >= 7) return true
            return false
        }

        /**
         * Sanitizes a name string to eliminate literal "null" or blank values.
         */
        fun sanitizeName(name: String?, fallback: String = ""): String {
            if (name.isNullOrBlank()) return fallback
            val trimmed = name.trim()
            val lower = trimmed.lowercase()
            if (lower == "null" || lower == "null null" || lower.startsWith("null ") || lower.startsWith("null •") || lower == "null • online") {
                return fallback
            }
            return trimmed
        }

        /**
         * Sanitizes a phone number to eliminate literal "null" or blank values.
         */
        fun sanitizePhone(phone: String?, fallback: String = ""): String {
            if (phone.isNullOrBlank()) return fallback
            val trimmed = phone.trim()
            val lower = trimmed.lowercase()
            if (lower == "null" || lower == "null null" || lower == "null • online") {
                return fallback
            }
            return trimmed
        }

        /**
         * Detects if a string is encrypted ciphertext, hash noise, literal "null", or generic placeholder.
         */
        fun isGarbledOrEncrypted(text: String?): Boolean {
            if (text.isNullOrBlank()) return true
            val trimmed = text.trim()
            val lower = trimmed.lowercase()

            if (lower == "null" || lower == "null null" || lower.startsWith("null ") || lower.startsWith("null •") || lower == "null • online") return true
            if (lower.contains("null") && trimmed.length <= 15) return true
            if (trimmed.startsWith("V1_ENC:") || trimmed.startsWith("V2_SIG:") || trimmed.startsWith("ENC:") || trimmed.startsWith("AES:") || trimmed.startsWith("SIG_")) return true
            if (trimmed.contains('\uFFFD')) return true
            if (trimmed.any { it.isISOControl() && it != '\n' && it != '\r' && it != '\t' }) return true
            if (trimmed.contains("?") && trimmed.count { it == '?' } >= 3) return true
            if (trimmed.equals("VibeSync Member", ignoreCase = true) ||
                trimmed.equals("VibeSync User", ignoreCase = true) ||
                trimmed.equals("registered user", ignoreCase = true) ||
                trimmed.equals("VibeSync Match", ignoreCase = true) ||
                trimmed.equals("Direct Contact", ignoreCase = true) ||
                trimmed.equals("VibeSync Contact", ignoreCase = true) ||
                trimmed.equals("Member", ignoreCase = true) ||
                trimmed.equals("User", ignoreCase = true)
            ) {
                return true
            }

            // Base64 ciphertext / hash detection (e.g. L0QwzzZ+TW6mgfmAST5kFGQxjLBAiFDx...)
            if (trimmed.length >= 16 && !trimmed.contains(" ")) {
                val isAllBase64Chars = trimmed.all {
                    it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' || it == '+' || it == '/' || it == '=' || it == '-' || it == '_'
                }
                if (isAllBase64Chars) {
                    val hasSymbols = trimmed.contains("+") || trimmed.contains("/") || trimmed.contains("=") || trimmed.contains("_") || trimmed.contains("-")
                    val hasMixedCaseDigits = trimmed.any { it.isUpperCase() } && trimmed.any { it.isLowerCase() } && trimmed.any { it.isDigit() }
                    if (hasSymbols || hasMixedCaseDigits) {
                        return true
                    }
                }
                // Hex hash check (16, 32, or 64 character hex string)
                if ((trimmed.length == 16 || trimmed.length == 32 || trimmed.length == 64) && trimmed.all { it in "0123456789abcdefABCDEF" }) {
                    return true
                }
            }

            // Punctuation noise heuristic
            if (trimmed.length > 16 && !trimmed.contains(" ") && trimmed.any { it in "@#$%^&*()<>{}[]|/\\~`" }) {
                return true
            }

            return false
        }

        /**
         * Legacy helper: returns local display name, overriding encrypted strings.
         */
        fun getDisplayName(phoneOrId: String, currentName: String? = null): String {
            return resolveParticipantDisplayName(appContextRef, phoneOrId, currentName)
        }

        /**
         * Injects local display name into a ProfileEntity, overriding remote encrypted strings.
         */
        fun injectContactName(profile: ProfileEntity): ProfileEntity {
            return matchChatSessionParticipant(appContextRef, profile)
        }

        /**
         * Injects local display names into a list of PhoneContacts.
         */
        fun injectContactNames(contacts: List<PhoneContact>): List<PhoneContact> {
            return contacts.map { contact ->
                val phoneKey = contact.phoneNumber.ifBlank { contact.id }
                val resolvedName = resolveParticipantDisplayName(appContextRef, phoneKey, contact.name)
                if (resolvedName != contact.name) {
                    contact.copy(name = resolvedName)
                } else {
                    contact
                }
            }
        }

        /**
         * Fetches a contact's photo URI (either thumbnail or full size) using their phone number from the local Android contacts provider.
         */
        fun fetchContactPhotoByPhoneNumber(context: android.content.Context, phoneNumber: String): String? {
            if (phoneNumber.isBlank() || phoneNumber.equals("null", ignoreCase = true)) return null
            val clean = phoneNumber.trim()

            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_CONTACTS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                return null
            }

            // 1. Query ContactsContract.PhoneLookup for standard fast photo lookup
            try {
                val uri = android.net.Uri.withAppendedPath(
                    android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    android.net.Uri.encode(clean)
                )
                val projection = arrayOf(
                    android.provider.ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI,
                    android.provider.ContactsContract.PhoneLookup.PHOTO_URI
                )
                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val thumbIdx = cursor.getColumnIndex(android.provider.ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI)
                        val fullIdx = cursor.getColumnIndex(android.provider.ContactsContract.PhoneLookup.PHOTO_URI)
                        
                        var photoUri: String? = null
                        if (thumbIdx >= 0) {
                            photoUri = cursor.getString(thumbIdx)
                        }
                        if (photoUri.isNullOrBlank() && fullIdx >= 0) {
                            photoUri = cursor.getString(fullIdx)
                        }
                        if (!photoUri.isNullOrBlank()) {
                            return photoUri
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.d(TAG, "Error fetching photo URI via PhoneLookup for $clean: ${e.message}")
            }

            // 2. Query ContactsContract.CommonDataKinds.Phone by last 10 digits as a robust fallback
            try {
                val digits = clean.filter { it.isDigit() }
                if (digits.length >= 7) {
                    val last10 = digits.takeLast(10)
                    val projection = arrayOf(
                        android.provider.ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
                        android.provider.ContactsContract.CommonDataKinds.Phone.PHOTO_URI
                    )
                    val selection = "${android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?"
                    val selectionArgs = arrayOf("%$last10%")
                    context.contentResolver.query(
                        android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        projection,
                        selection,
                        selectionArgs,
                        null
                    )?.use { cursor ->
                        val thumbIdx = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)
                        val fullIdx = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                        while (cursor.moveToNext()) {
                            var photoUri: String? = null
                            if (thumbIdx >= 0) photoUri = cursor.getString(thumbIdx)
                            if (photoUri.isNullOrBlank() && fullIdx >= 0) photoUri = cursor.getString(fullIdx)
                            if (!photoUri.isNullOrBlank()) return photoUri
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.d(TAG, "Error searching photo URI by last 10 digits for $clean: ${e.message}")
            }

            return null
        }
    }
}
