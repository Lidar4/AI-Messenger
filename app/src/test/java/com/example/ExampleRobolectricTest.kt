package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.network.nearby.MeshRouter
import com.example.network.nearby.P2PMessageDto
import com.example.security.EncryptionHelper
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("AI Messenger", appName)
    }

    @Test
    fun `validate device ID persistence`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("ai_messenger_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        val id1 = prefs.getString("device_id", null) ?: run {
            val newId = UUID.randomUUID().toString()
            prefs.edit().putString("device_id", newId).commit()
            newId
        }

        val id2 = prefs.getString("device_id", null)
        assertEquals(id1, id2)
        assertNotNull(id2)
    }

    @Test
    fun `validate P2P message DTO json serialization and deserialization`() {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(P2PMessageDto::class.java)

        val dto = P2PMessageDto(
            protocolVersion = 1,
            messageId = "msg_123",
            senderDeviceId = "dev_abc",
            senderName = "Alice",
            conversationId = "conv_1",
            timestamp = 1000000L,
            ttl = 3,
            messageType = "TEXT",
            encryptedPayload = "cipherdata",
            attachmentMetadata = null
        )

        val json = adapter.toJson(dto)
        assertNotNull(json)

        val decoded = adapter.fromJson(json)
        assertNotNull(decoded)
        assertEquals(dto.messageId, decoded?.messageId)
        assertEquals(dto.senderDeviceId, decoded?.senderDeviceId)
        assertEquals(dto.encryptedPayload, decoded?.encryptedPayload)
        assertEquals(dto.ttl, decoded?.ttl)
    }

    @Test
    fun `validate secure GCM encryption and decryption`() {
        val originalText = "Test secure payload"
        val secretKey = EncryptionHelper.deriveKey("SECURE_PASSPHRASE_TEST")
        
        val encryptedText = EncryptionHelper.encrypt(originalText, secretKey)
        assertNotEquals(originalText, encryptedText)
        
        val decryptedText = EncryptionHelper.decrypt(encryptedText, secretKey)
        assertEquals(originalText, decryptedText)
    }

    @Test(expected = Exception::class)
    fun `validate encryption failure handling without silent plaintext fallback`() {
        val secretKey = EncryptionHelper.deriveKey("TEST_KEY")
        EncryptionHelper.decrypt("invalid_ciphertext_malformed", secretKey)
    }

    @Test(expected = Exception::class)
    fun `validate tampered ciphertext rejection`() {
        val originalText = "Secret message"
        val secretKey = EncryptionHelper.deriveKey("PASSPHRASE")
        val encryptedText = EncryptionHelper.encrypt(originalText, secretKey)
        
        val tamperedText = encryptedText.dropLast(2) + "=="
        EncryptionHelper.decrypt(tamperedText, secretKey)
    }

    @Test(expected = Exception::class)
    fun `validate wrong key rejection`() {
        val originalText = "Confidential data"
        val correctKey = EncryptionHelper.deriveKey("CORRECT_KEY")
        val wrongKey = EncryptionHelper.deriveKey("WRONG_KEY")
        
        val encryptedText = EncryptionHelper.encrypt(originalText, correctKey)
        EncryptionHelper.decrypt(encryptedText, wrongKey)
    }

    @Test
    fun `validate mesh router duplicate message protection`() {
        val msgId = "msg_test_123"
        val firstCheck = MeshRouter.registerMessageAndCheckDuplicate(msgId)
        assertTrue("First time message ID should be registered as new", firstCheck)

        val duplicateCheck = MeshRouter.registerMessageAndCheckDuplicate(msgId)
        assertFalse("Duplicate message ID should be detected and blocked", duplicateCheck)
    }

    @Test
    fun `validate mesh routing TTL hop limit`() {
        assertTrue("Hop count 2 should be allowed to forward", MeshRouter.shouldForward(2))
        assertFalse("Hop count 5 (max limit) should not be forwarded", MeshRouter.shouldForward(5))
        assertFalse("Hop count 6 should not be forwarded", MeshRouter.shouldForward(6))
    }
}
