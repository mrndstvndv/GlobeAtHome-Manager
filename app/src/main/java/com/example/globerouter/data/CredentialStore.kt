package com.example.globerouter.data

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Encrypted credential storage backed by Android Keystore + AES-256-GCM. */
class CredentialStore(context: Context) {
  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
  private val keystore = KeyStore.getInstance(KEYSTORE_TYPE).apply { load(null) }

  /** Persist [username] and [password] encrypted with a Keystore-backed AES key. */
  fun save(username: String, password: String) {
    val key = getOrCreateKey()
    val cipher = Cipher.getInstance(AES_GCM)
    cipher.init(Cipher.ENCRYPT_MODE, key)
    val iv = cipher.iv
    val plaintext = "$username:$password".encodeToByteArray()
    val ciphertext = cipher.doFinal(plaintext)
    prefs.edit()
      .putString(KEY_IV, Base64.encodeToString(iv, Base64.DEFAULT))
      .putString(KEY_DATA, Base64.encodeToString(ciphertext, Base64.DEFAULT))
      .putBoolean(KEY_SAVED, true)
      .apply()
  }

  /** Read saved credentials. Returns null if none saved or decryption fails. */
  fun load(): Pair<String, String>? {
    if (!prefs.getBoolean(KEY_SAVED, false)) return null
    val ivB64 = prefs.getString(KEY_IV, null) ?: return null
    val dataB64 = prefs.getString(KEY_DATA, null) ?: return null
    return try {
      val key = getOrCreateKey()
      val cipher = Cipher.getInstance(AES_GCM)
      cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, Base64.decode(ivB64, Base64.DEFAULT)))
      val plaintext = cipher.doFinal(Base64.decode(dataB64, Base64.DEFAULT))
      val decoded = plaintext.decodeToString()
      val sep = decoded.indexOf(':')
      if (sep == -1) return null
      Pair(decoded.substring(0, sep), decoded.substring(sep + 1))
    } catch (_: Exception) {
      // Key invalidated (e.g. device lock changed) or data corrupted — clear and return null
      clear()
      null
    }
  }

  /** True if saved credentials exist. */
  fun hasSaved(): Boolean = prefs.getBoolean(KEY_SAVED, false)

  /** Wipe saved credentials. */
  fun clear() {
    prefs.edit().clear().apply()
  }

  private fun getOrCreateKey(): SecretKey {
    if (keystore.containsAlias(KEY_ALIAS)) {
      return (keystore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }
    val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_TYPE)
    keyGen.init(
      KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setKeySize(256)
        .build()
    )
    return keyGen.generateKey()
  }

  companion object {
    private const val PREF_NAME = "globe_credentials"
    private const val KEYSTORE_TYPE = "AndroidKeyStore"
    private const val KEY_ALIAS = "globe_router_aes_key"
    private const val AES_GCM = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private const val KEY_IV = "iv"
    private const val KEY_DATA = "data"
    private const val KEY_SAVED = "saved"
  }
}
