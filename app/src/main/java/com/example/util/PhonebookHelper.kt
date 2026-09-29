package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import com.example.data.model.PhoneContact
import com.example.data.model.ProfileEntity
import java.security.MessageDigest
import java.util.UUID

/**
 * PhonebookHelper
 *
 * Provides robust phone number normalization enforcing E.164 formatting,
 * 16-character SHA-256 hash generation for the matching engine,
 * and zero-cost address book matching against registered Firestore profiles.
 */
object PhonebookHelper {

    private const val TAG = "PhonebookHelper"
    const val HASH_LENGTH = 16
    const val DEFAULT_COUNTRY_CODE = "+91"

    const val DEFAULT_INVITE_MESSAGE =
        "Hey! Join me on VibeSync — the verified local chat & connection app! Connect with real people safely: https://vibesync.app/join"

    /**
     * Robust phone number normalization that strips all non-numeric characters
     * and enforces E.164 formatting (+country_code followed by digits).
     */
    fun normalizeToE164(rawPhone: String, defaultCountryCode: String = DEFAULT_COUNTRY_CODE): String {
        return PhonebookHasher.normalizeToE164(rawPhone, defaultCountryCode)
    }

    /**
     * Generates a 16-character truncated SHA-256 hash of the E.164 normalized phone number.
     * Consistently set to exactly 16 characters for the matching engine.
     */
    fun generatePhoneHash(rawPhone: String, defaultCountryCode: String = DEFAULT_COUNTRY_CODE): String {
        return PhonebookHasher.generate16CharHash(rawPhone, defaultCountryCode)
    }

    /**
     * Backward-compatible helper for hash generation.
     */
    fun getPhoneHash(rawPhone: String): String {
        return generatePhoneHash(rawPhone)
    }

    /**
     * Reads contacts from Android device ContactsContract if permission is granted.
     */
    fun fetchDeviceContacts(context: Context): List<PhoneContact> {
        val contacts = mutableListOf<PhoneContact>()
        try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.use {
                val idIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val seenNumbers = mutableSetOf<String>()

                while (it.moveToNext()) {
                    val id = if (idIdx >= 0) it.getString(idIdx) else UUID.randomUUID().toString()
                    val name = if (nameIdx >= 0) it.getString(nameIdx) else "Contact"
                    val number = if (numberIdx >= 0) it.getString(numberIdx) else ""
                    val normalizedE164 = normalizeToE164(number)

                    if (normalizedE164.isNotBlank() && seenNumbers.add(normalizedE164)) {
                        val uniqueContactId = "c_${id}_${normalizedE164.filter { it.isDigit() }}"
                        val hash16 = generatePhoneHash(normalizedE164)
                        contacts.add(
                            PhoneContact(
                                id = uniqueContactId,
                                name = name.trim(),
                                phoneNumber = number.trim(),
                                phoneHash = hash16
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Notice querying device contacts: ${e.message}")
        }
        return contacts
    }

    /**
     * Demo / Seed contacts ensuring seamless testing on emulator/browser and devices.
     */
    fun getDemoPhonebookContacts(): List<PhoneContact> {
        return listOf(
            PhoneContact(
                id = "phone_contact_1",
                name = "Maya Lin",
                phoneNumber = "+1 555-0142",
                isOnVibeSync = true,
                vibeSyncProfileId = "prof_maya_1",
                avatarEmoji = "🎨",
                statusTagline = "Working on ceramic sculptures • Online",
                phoneHash = generatePhoneHash("+1 555-0142")
            ),
            PhoneContact(
                id = "phone_contact_2",
                name = "Liam Davis",
                phoneNumber = "+1 555-0187",
                isOnVibeSync = true,
                vibeSyncProfileId = "prof_liam_2",
                avatarEmoji = "☕",
                statusTagline = "Specialty coffee roaster ☕ • Available on VibeSync",
                phoneHash = generatePhoneHash("+1 555-0187")
            ),
            PhoneContact(
                id = "phone_contact_3",
                name = "Chloe Bennett",
                phoneNumber = "+1 555-0193",
                isOnVibeSync = true,
                vibeSyncProfileId = "prof_chloe_3",
                avatarEmoji = "🌿",
                statusTagline = "Nature photography & trail running • Active",
                phoneHash = generatePhoneHash("+1 555-0193")
            ),
            PhoneContact(
                id = "phone_contact_4",
                name = "Ethan Brooks",
                phoneNumber = "+1 555-0164",
                isOnVibeSync = true,
                vibeSyncProfileId = "prof_ethan_4",
                avatarEmoji = "🎸",
                statusTagline = "Indie music producer & songwriter • Online",
                phoneHash = generatePhoneHash("+1 555-0164")
            ),
            PhoneContact(
                id = "phone_contact_5",
                name = "Aria Patel",
                phoneNumber = "+1 555-0115",
                isOnVibeSync = true,
                vibeSyncProfileId = "prof_aria_5",
                avatarEmoji = "📚",
                statusTagline = "Literature professor & bookstore explorer • Online",
                phoneHash = generatePhoneHash("+1 555-0115")
            ),
            PhoneContact(
                id = "phone_contact_6",
                name = "Lucas Scott",
                phoneNumber = "+1 555-0128",
                isOnVibeSync = false,
                statusTagline = "Not yet on VibeSync",
                phoneHash = generatePhoneHash("+1 555-0128")
            ),
            PhoneContact(
                id = "phone_contact_7",
                name = "Zoe Washington",
                phoneNumber = "+1 555-0176",
                isOnVibeSync = false,
                statusTagline = "Not yet on VibeSync",
                phoneHash = generatePhoneHash("+1 555-0176")
            ),
            PhoneContact(
                id = "phone_contact_8",
                name = "Oliver King",
                phoneNumber = "+1 555-0133",
                isOnVibeSync = false,
                statusTagline = "Not yet on VibeSync",
                phoneHash = generatePhoneHash("+1 555-0133")
            ),
            PhoneContact(
                id = "phone_contact_9",
                name = "Sophia Martinez",
                phoneNumber = "+1 555-0158",
                isOnVibeSync = false,
                statusTagline = "Not yet on VibeSync",
                phoneHash = generatePhoneHash("+1 555-0158")
            ),
            PhoneContact(
                id = "phone_contact_10",
                name = "James Wilson",
                phoneNumber = "+1 555-0199",
                isOnVibeSync = false,
                statusTagline = "Not yet on VibeSync",
                phoneHash = generatePhoneHash("+1 555-0199")
            )
        )
    }

    /**
     * Resolves the phonebook contacts list with zero-cost 16-character SHA-256 E.164 hash matching.
     */
    fun matchPhoneContactsWithProfiles(
        rawContacts: List<PhoneContact>,
        availableProfiles: List<ProfileEntity>,
        registeredUsersMap: Map<String, ProfileEntity> = emptyMap(),
        myPhoneNumber: String = ""
    ): List<PhoneContact> {
        return filterAndMatchContactsWithVibeSync(rawContacts, availableProfiles, registeredUsersMap, myPhoneNumber)
    }

    /**
     * Resolves the phonebook contacts list with zero-cost 16-character SHA-256 E.164 hash matching.
     */
    fun filterAndMatchContactsWithVibeSync(
        rawContacts: List<PhoneContact>,
        availableProfiles: List<ProfileEntity>,
        registeredUsersMap: Map<String, ProfileEntity> = emptyMap(),
        myPhoneNumber: String = ""
    ): List<PhoneContact> {
        val myDigits = myPhoneNumber.filter { it.isDigit() }
        val myPhone10 = if (myDigits.length >= 10) myDigits.takeLast(10) else myDigits

        val filteredRawContacts = if (myPhone10.isNotBlank()) {
            rawContacts.filterNot { contact ->
                val cDigits = contact.phoneNumber.filter { it.isDigit() }
                val c10 = if (cDigits.length >= 10) cDigits.takeLast(10) else cDigits
                c10.isNotBlank() && c10 == myPhone10
            }
        } else {
            rawContacts
        }

        val exactPhoneMap = mutableMapOf<String, ProfileEntity>()
        val phone10Map = mutableMapOf<String, ProfileEntity>()
        val nameMap = mutableMapOf<String, ProfileEntity>()

        fun indexProfile(profile: ProfileEntity) {
            val n = profile.name.trim().lowercase()
            if (n.isNotBlank() && n != "vibesync user" && n != "registered user") {
                nameMap.putIfAbsent(n, profile)
            }

            // Zero-Knowledge 16-char E.164 Hash Indexing
            if (profile.phoneNumber.isNotBlank()) {
                val hashes = PhonebookHasher.getAllMatchHashes(profile.phoneNumber)
                for (h in hashes) {
                    exactPhoneMap.putIfAbsent(h, profile)
                }
            }

            if (profile.id.length == 16) {
                exactPhoneMap.putIfAbsent(profile.id, profile)
            }

            val idHashes = PhonebookHasher.getAllMatchHashes(profile.id)
            for (h in idHashes) {
                exactPhoneMap.putIfAbsent(h, profile)
            }

            val idDigits = profile.id.filter { it.isDigit() }
            if (idDigits.isNotBlank()) {
                exactPhoneMap.putIfAbsent(idDigits, profile)
                if (idDigits.length >= 10) {
                    phone10Map.putIfAbsent(idDigits.takeLast(10), profile)
                }
            }

            val phoneDigits = profile.phoneNumber.filter { it.isDigit() }
            if (phoneDigits.isNotBlank()) {
                exactPhoneMap.putIfAbsent(phoneDigits, profile)
                if (phoneDigits.length >= 10) {
                    phone10Map.putIfAbsent(phoneDigits.takeLast(10), profile)
                }
            }

            if (profile.phoneNumber.isNotBlank()) {
                exactPhoneMap.putIfAbsent(profile.phoneNumber.trim(), profile)
                val cleanWithoutPlus = profile.phoneNumber.replace("+", "").trim()
                if (cleanWithoutPlus.isNotBlank()) {
                    exactPhoneMap.putIfAbsent(cleanWithoutPlus, profile)
                }
            }
            if (profile.email.isNotBlank()) {
                exactPhoneMap.putIfAbsent(profile.email.trim().lowercase(), profile)
            }
        }

        // 1. Index local Room profiles
        for (profile in availableProfiles) {
            indexProfile(profile)
        }

        // 2. Index registered Firestore users
        for ((key, profile) in registeredUsersMap) {
            indexProfile(profile)
            val hashes = PhonebookHasher.getAllMatchHashes(key)
            for (h in hashes) {
                exactPhoneMap.putIfAbsent(h, profile)
            }
            val keyDigits = key.filter { it.isDigit() }
            if (keyDigits.isNotBlank()) {
                exactPhoneMap.putIfAbsent(keyDigits, profile)
                if (keyDigits.length >= 10) {
                    phone10Map.putIfAbsent(keyDigits.takeLast(10), profile)
                }
            }
        }

        val resultContacts = filteredRawContacts.map { contact ->
            val cleanDigits = contact.phoneNumber.filter { it.isDigit() }
            val matchHashes = PhonebookHasher.getAllMatchHashes(contact.phoneNumber)

            // Match against indexed 16-character SHA-256 hashes
            var matchedProfile: ProfileEntity? = null
            for (h in matchHashes) {
                matchedProfile = exactPhoneMap[h]
                if (matchedProfile != null) break
            }

            // Fallback matching against digits, 10-digit national, email, or name
            if (matchedProfile == null) {
                matchedProfile = exactPhoneMap[cleanDigits]
                    ?: (if (cleanDigits.length >= 10) phone10Map[cleanDigits.takeLast(10)] else null)
                    ?: exactPhoneMap[contact.phoneNumber.trim()]
                    ?: (if (contact.email.isNotBlank()) exactPhoneMap[contact.email.trim().lowercase()] else null)
                    ?: nameMap[contact.name.trim().lowercase()]
            }

            val finalName = ContactResolver.getDisplayName(contact.phoneNumber.ifBlank { contact.id }, contact.name)
            val finalHash = PhonebookHasher.generate16CharHash(contact.phoneNumber)

            if (matchedProfile != null) {
                contact.copy(
                    name = finalName,
                    isOnVibeSync = true,
                    vibeSyncProfileId = matchedProfile.id,
                    vibeSyncUser = matchedProfile,
                    avatarEmoji = matchedProfile.avatarEmoji.ifBlank { "✨" },
                    photoUrl = matchedProfile.avatarUrl,
                    statusTagline = "${matchedProfile.relationshipGoal.ifBlank { "Available on VibeSync" }} • ${matchedProfile.city.ifBlank { "Active" }}",
                    phoneHash = finalHash
                )
            } else {
                contact.copy(name = finalName, phoneHash = finalHash)
            }
        }.toMutableList()

        // 3. Ensure any registered Firestore / app profile is discoverable in the contact list (EXCEPT self profile)
        val seenIds = resultContacts.mapNotNull { it.vibeSyncProfileId }.toMutableSet()
        val seenPhones = resultContacts.map { it.phoneNumber.filter { d -> d.isDigit() }.takeLast(10) }.filter { it.length == 10 }.toMutableSet()

        val allCandidateProfiles = (availableProfiles + registeredUsersMap.values).distinctBy { it.id }
        for (profile in allCandidateProfiles) {
            val profPhone10 = profile.phoneNumber.filter { it.isDigit() }.takeLast(10)
            val profId10 = profile.id.filter { it.isDigit() }.takeLast(10)
            val isSelfNumber = (myPhone10.length >= 10) && (profPhone10 == myPhone10 || profId10 == myPhone10)

            val isAlreadyPresent = (profPhone10.length == 10 && seenPhones.contains(profPhone10)) ||
                    (profId10.length == 10 && seenPhones.contains(profId10)) ||
                    seenIds.contains(profile.id)

            if (!isSelfNumber && profile.id != "current_user" && !profile.id.startsWith("current_user") && !profile.id.startsWith("seed_") && !profile.id.startsWith("demo_") && !isAlreadyPresent) {
                seenIds.add(profile.id)
                if (profPhone10.length == 10) seenPhones.add(profPhone10)

                // Match against saved address book to prefer user's saved contact name
                val savedContact = filteredRawContacts.firstOrNull { raw ->
                    val raw10 = raw.phoneNumber.filter { it.isDigit() }.takeLast(10)
                    (profPhone10.length == 10 && raw10 == profPhone10) ||
                    (profId10.length == 10 && raw10 == profId10) ||
                    (raw.phoneNumber.isNotBlank() && profile.phoneNumber.isNotBlank() && raw.phoneNumber.trim() == profile.phoneNumber.trim())
                }

                val finalDisplayName = ContactResolver.getDisplayName(
                    profile.phoneNumber.ifBlank { profile.id },
                    savedContact?.name ?: profile.name
                )
                val profHash = PhonebookHasher.generate16CharHash(profile.phoneNumber.ifBlank { profile.id })

                resultContacts.add(
                    0,
                    PhoneContact(
                        id = "reg_${profile.id}",
                        name = finalDisplayName,
                        phoneNumber = profile.phoneNumber.ifBlank { profile.id },
                        email = profile.email,
                        isOnVibeSync = true,
                        vibeSyncProfileId = profile.id,
                        vibeSyncUser = profile,
                        avatarEmoji = profile.avatarEmoji.ifBlank { "✨" },
                        photoUrl = profile.avatarUrl,
                        statusTagline = "${profile.relationshipGoal.ifBlank { "Available on VibeSync" }} • ${profile.city.ifBlank { "Active" }}",
                        phoneHash = profHash
                    )
                )
            }
        }

        // Sort: VibeSync users first (alphabetical), then non-users (alphabetical)
        return resultContacts.sortedWith(
            compareByDescending<PhoneContact> { it.isOnVibeSync }
                .thenBy { it.name.lowercase() }
        )
    }

    /**
     * Launches direct messaging invitation with pre-filled VibeSync invitation text.
     */
    fun sendInstantMessagingInvite(context: Context, phoneNumber: String, message: String = DEFAULT_INVITE_MESSAGE): Boolean {
        return try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(sendIntent, "Invite via VibeSync Direct").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
            true
        } catch (_: Exception) {
            sendSmsInvite(context, phoneNumber, message)
        }
    }

    /**
     * Fallback standard SMS intent.
     */
    fun sendSmsInvite(context: Context, phoneNumber: String, message: String = DEFAULT_INVITE_MESSAGE): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${phoneNumber.filter { it.isDigit() || it == '+' }}")
                putExtra("sms_body", message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
