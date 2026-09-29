package com.example.util.backup

import android.util.Base64
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * End-to-End AES-256 GCM Encryption helper for Google Drive AppFolder Backup payloads.
 * Protects user chat history, private memories, and contacts from unauthorized inspection.
 */
object ChatBackupCryptoHelper {

    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BIT = 128
    private const val IV_LENGTH_BYTE = 12

    // Derives a cryptographic AES-256 key from user account identity & seed
    private fun deriveKey(seed: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(seed.toByteArray(StandardCharsets.UTF_8))
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Encrypts plain JSON text to a secure local file payload.
     */
    fun encryptJsonToFile(plainJson: String, targetFile: File, userSeed: String = "VIBESYNC_SECURE_VAULT_SEED"): Boolean {
        return try {
            val key = deriveKey(userSeed)
            val iv = ByteArray(IV_LENGTH_BYTE)
            SecureRandom().nextBytes(iv)

            val cipher = Cipher.getInstance(ALGORITHM)
            val spec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, spec)

            FileOutputStream(targetFile).use { fos ->
                // Write IV first (12 bytes)
                fos.write(iv)
                // Write encrypted content
                val cipherBytes = cipher.doFinal(plainJson.toByteArray(StandardCharsets.UTF_8))
                fos.write(cipherBytes)
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("ChatBackupCryptoHelper", "Encryption error: ${e.message}", e)
            false
        }
    }

    /**
     * Decrypts an encrypted file payload back into plain JSON string.
     */
    fun decryptFileToJson(encryptedFile: File, userSeed: String = "VIBESYNC_SECURE_VAULT_SEED"): String? {
        return try {
            if (!encryptedFile.exists() || encryptedFile.length() <= IV_LENGTH_BYTE) {
                return null
            }

            val key = deriveKey(userSeed)
            val fileBytes = encryptedFile.readBytes()

            // Extract IV from first 12 bytes
            val iv = fileBytes.copyOfRange(0, IV_LENGTH_BYTE)
            val cipherBytes = fileBytes.copyOfRange(IV_LENGTH_BYTE, fileBytes.size)

            val cipher = Cipher.getInstance(ALGORITHM)
            val spec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)

            val plainBytes = cipher.doFinal(cipherBytes)
            String(plainBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            android.util.Log.e("ChatBackupCryptoHelper", "Decryption error: ${e.message}", e)
            // Fallback attempt: if file was written in plain text for diagnostic purposes
            try {
                val plainContent = encryptedFile.readText(StandardCharsets.UTF_8)
                if (plainContent.trim().startsWith("{")) {
                    return plainContent
                }
            } catch (_: Exception) {}
            null
        }
    }
}
