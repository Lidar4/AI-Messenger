package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.security.EncryptionHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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
    fun `validate secure GCM encryption and decryption`() {
        val originalText = "Test secure payload"
        val secretKey = EncryptionHelper.deriveKey("SECURE_PASSPHRASE_TEST")
        
        val encryptedText = EncryptionHelper.encrypt(originalText, secretKey)
        assertNotEquals(originalText, encryptedText)
        
        val decryptedText = EncryptionHelper.decrypt(encryptedText, secretKey)
        assertEquals(originalText, decryptedText)
    }
}
