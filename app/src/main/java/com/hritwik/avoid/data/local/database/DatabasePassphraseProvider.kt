package com.hritwik.avoid.data.local.database

import android.content.Context
import android.util.Base64
import com.google.crypto.tink.Aead
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import java.security.SecureRandom

object DatabasePassphraseProvider {
    private const val PREFS_NAME = "void_db_prefs"
    private const val KEY_PASSPHRASE = "db_passphrase"
    private const val KEYSET_PREF = "void_tink_keyset"
    private const val KEYSET_NAME = "tink_keyset"
    private const val MASTER_KEY_URI = "android-keystore://void_tink_master_key"
    private const val AEAD_ASSOCIATED_DATA = "db_passphrase"

    private fun aead(context: Context): Aead {
        AeadConfig.register()
        val handle = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, KEYSET_PREF)
            .withKeyTemplate(com.google.crypto.tink.aead.AesGcmKeyManager.aes256GcmTemplate())
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
        return handle.getPrimitive(Aead::class.java)
    }

    fun getPassphrase(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val aead = aead(context)
        val existing = prefs.getString(KEY_PASSPHRASE, null)
        if (existing != null) {
            val cipherBytes = Base64.decode(existing, Base64.DEFAULT)
            return aead.decrypt(cipherBytes, AEAD_ASSOCIATED_DATA.toByteArray())
        }

        val randomBytes = ByteArray(32)
        SecureRandom().nextBytes(randomBytes)
        val cipher = aead.encrypt(randomBytes, AEAD_ASSOCIATED_DATA.toByteArray())
        val encoded = Base64.encodeToString(cipher, Base64.DEFAULT)
        prefs.edit().putString(KEY_PASSPHRASE, encoded).apply()
        return randomBytes
    }
}
