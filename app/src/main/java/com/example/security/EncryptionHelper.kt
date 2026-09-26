package com.example.security

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object EncryptionHelper {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BIT = 128
    private const val IV_SIZE = 12

    // Derive a stable 256-bit AES key from a passphrase or password
    fun deriveKey(passphrase: String): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(passphrase.toByteArray(Charsets.UTF_8))
    }

    // Encrypt message using AES-GCM
    fun encrypt(plainText: String, secretKey: ByteArray): String {
        val keySpec = SecretKeySpec(secretKey, "AES")
        val cipher = Cipher.getInstance(ALGORITHM)
        val iv = ByteArray(IV_SIZE).apply {
            java.security.SecureRandom().nextBytes(this)
        }
        val gcmSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val combined = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    // Decrypt message using AES-GCM
    fun decrypt(encryptedText: String, secretKey: ByteArray): String {
        val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
        require(combined.size >= IV_SIZE) { "Invalid encrypted payload size" }

        val iv = ByteArray(IV_SIZE)
        System.arraycopy(combined, 0, iv, 0, IV_SIZE)

        val cipherTextSize = combined.size - IV_SIZE
        val cipherText = ByteArray(cipherTextSize)
        System.arraycopy(combined, IV_SIZE, cipherText, 0, cipherTextSize)

        val keySpec = SecretKeySpec(secretKey, "AES")
        val cipher = Cipher.getInstance(ALGORITHM)
        val gcmSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)

        return String(cipher.doFinal(cipherText), Charsets.UTF_8)
    }
}
