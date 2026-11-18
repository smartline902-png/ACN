package com.example.security

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

enum class BiometricSupportStatus {
  STRONG_BIOMETRIC_READY,
  DEVICE_CREDENTIAL_ONLY,
  NOT_ENROLLED,
  NO_HARDWARE,
  UNAVAILABLE,
}

data class SealedVerificationPayload(
  val ciphertextBase64: String,
  val ivBase64: String,
)

/**
 * Hardware-backed Android Keystore + BiometricPrompt manager.
 * Protects an AES-GCM key inside AndroidKeyStore requiring user biometric / credential
 * verification before sealing or unsealing the app-lock verification token.
 */
class BiometricKeyManager(private val context: Context) {

  companion object {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "kma_personal_accountant_vault_key_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private const val VERIFICATION_MAGIC = "KMA_PERSONAL_ACCOUNTANT_UNLOCKED_V1"
  }

  fun getSupportStatus(): BiometricSupportStatus {
    val biometricManager = BiometricManager.from(context)
    val strongStatus = biometricManager.canAuthenticate(BIOMETRIC_STRONG)
    if (strongStatus == BiometricManager.BIOMETRIC_SUCCESS) {
      return BiometricSupportStatus.STRONG_BIOMETRIC_READY
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      val combinedStatus = biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
      if (combinedStatus == BiometricManager.BIOMETRIC_SUCCESS) {
        return BiometricSupportStatus.DEVICE_CREDENTIAL_ONLY
      }
    } else {
      val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
      if (keyguard?.isDeviceSecure == true && strongStatus == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
        return BiometricSupportStatus.DEVICE_CREDENTIAL_ONLY
      }
    }

    return when (strongStatus) {
      BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricSupportStatus.NOT_ENROLLED
      BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
      BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricSupportStatus.NO_HARDWARE
      else -> BiometricSupportStatus.UNAVAILABLE
    }
  }

  fun canUseLock(): Boolean {
    val status = getSupportStatus()
    return status == BiometricSupportStatus.STRONG_BIOMETRIC_READY ||
      status == BiometricSupportStatus.DEVICE_CREDENTIAL_ONLY
  }

  fun hasKeyInKeystore(): Boolean {
    return try {
      val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
      keyStore.containsAlias(KEY_ALIAS)
    } catch (_: Exception) {
      false
    }
  }

  fun deleteKey() {
    try {
      val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
      if (keyStore.containsAlias(KEY_ALIAS)) {
        keyStore.deleteEntry(KEY_ALIAS)
      }
    } catch (_: Exception) {
      // Ignore cleanup failure
    }
  }

  private fun getOrCreateSecretKey(requireStrongBiometricOnly: Boolean): SecretKey {
    val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
    if (existing != null) return existing

    val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
    val builder =
      KeyGenParameterSpec.Builder(
        KEY_ALIAS,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
      )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setKeySize(256)
        .setUserAuthenticationRequired(true)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      val allowedAuth =
        if (requireStrongBiometricOnly) {
          KeyProperties.AUTH_BIOMETRIC_STRONG
        } else {
          KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL
        }
      builder.setUserAuthenticationParameters(0, allowedAuth)
    } else {
      @Suppress("DEPRECATION")
      builder.setUserAuthenticationValidityDurationSeconds(-1)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      builder.setInvalidatedByBiometricEnrollment(true)
    }

    keyGenerator.init(builder.build())
    return keyGenerator.generateKey()
  }

  /**
   * Prompts the user via BiometricPrompt with a CryptoObject encrypting our vault verification token.
   */
  fun authenticateAndSealLockToken(
    activity: FragmentActivity,
    title: String,
    subtitle: String,
    onSuccess: (SealedVerificationPayload) -> Unit,
    onError: (String) -> Unit,
  ) {
    val status = getSupportStatus()
    if (!canUseLock()) {
      onError("الجهاز لا يحتوي على وسيلة قفل بيومترية أو رمز أمان مُفعّل.")
      return
    }

    val useStrongOnly =
      status == BiometricSupportStatus.STRONG_BIOMETRIC_READY &&
        Build.VERSION.SDK_INT < Build.VERSION_CODES.R

    val cipher =
      try {
        deleteKey()
        val secretKey = getOrCreateSecretKey(requireStrongBiometricOnly = useStrongOnly)
        Cipher.getInstance(TRANSFORMATION).apply {
          init(Cipher.ENCRYPT_MODE, secretKey)
        }
      } catch (e: Exception) {
        // Fallback if CryptoObject per-use key is unsupported on specific OEM/emulator HAL
        launchPromptWithoutCrypto(
          activity = activity,
          title = title,
          subtitle = subtitle,
          onSuccess = {
            onSuccess(
              SealedVerificationPayload(
                ciphertextBase64 = Base64.encodeToString(VERIFICATION_MAGIC.toByteArray(), Base64.NO_WRAP),
                ivBase64 = "FALLBACK_NO_CRYPTO",
              )
            )
          },
          onError = onError,
        )
        return
      }

    showCryptoPrompt(
      activity = activity,
      title = title,
      subtitle = subtitle,
      cipher = cipher,
      allowDeviceCredentialFallback = !useStrongOnly && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
      onCryptoSuccess = { authedCipher ->
        try {
          val encrypted = authedCipher.doFinal(VERIFICATION_MAGIC.toByteArray(StandardCharsets.UTF_8))
          val iv = authedCipher.iv
          onSuccess(
            SealedVerificationPayload(
              ciphertextBase64 = Base64.encodeToString(encrypted, Base64.NO_WRAP),
              ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP),
            )
          )
        } catch (e: Exception) {
          onError("تعذر تشفير مفتاح التحقق: ${e.localizedMessage ?: "خطأ غير معروف"}")
        }
      },
      onError = onError,
    )
  }

  /**
   * Prompts the user via BiometricPrompt and unseals the Keystore-protected verification token.
   */
  fun authenticateAndVerifyUnlock(
    activity: FragmentActivity,
    title: String,
    subtitle: String,
    storedCiphertextBase64: String?,
    storedIvBase64: String?,
    onUnlocked: () -> Unit,
    onError: (String) -> Unit,
  ) {
    if (!canUseLock()) {
      // Never lock out the user permanently if biometric hardware was disabled externally
      onUnlocked()
      return
    }

    if (storedCiphertextBase64.isNullOrBlank() ||
      storedIvBase64.isNullOrBlank() ||
      storedIvBase64 == "FALLBACK_NO_CRYPTO"
    ) {
      launchPromptWithoutCrypto(
        activity = activity,
        title = title,
        subtitle = subtitle,
        onSuccess = onUnlocked,
        onError = onError,
      )
      return
    }

    val status = getSupportStatus()
    val useStrongOnly =
      status == BiometricSupportStatus.STRONG_BIOMETRIC_READY &&
        Build.VERSION.SDK_INT < Build.VERSION_CODES.R

    val cipher =
      try {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val secretKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (secretKey == null) {
          launchPromptWithoutCrypto(activity, title, subtitle, onUnlocked, onError)
          return
        }
        val iv = Base64.decode(storedIvBase64, Base64.NO_WRAP)
        Cipher.getInstance(TRANSFORMATION).apply {
          init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        }
      } catch (_: Exception) {
        // Key may be invalidated by new biometric enrollment -> fall back safely to prompt re-auth
        launchPromptWithoutCrypto(activity, title, subtitle, onUnlocked, onError)
        return
      }

    showCryptoPrompt(
      activity = activity,
      title = title,
      subtitle = subtitle,
      cipher = cipher,
      allowDeviceCredentialFallback = !useStrongOnly && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
      onCryptoSuccess = { authedCipher ->
        try {
          val cipherBytes = Base64.decode(storedCiphertextBase64, Base64.NO_WRAP)
          val plain = String(authedCipher.doFinal(cipherBytes), StandardCharsets.UTF_8)
          if (plain == VERIFICATION_MAGIC) {
            onUnlocked()
          } else {
            onError("فشل التحقق من مفتاح الأمان المحلي.")
          }
        } catch (e: Exception) {
          onError("تعذر فك تشفير رمز الأمان: ${e.localizedMessage ?: "خطأ غير معروف"}")
        }
      },
      onError = onError,
    )
  }

  private fun showCryptoPrompt(
    activity: FragmentActivity,
    title: String,
    subtitle: String,
    cipher: Cipher,
    allowDeviceCredentialFallback: Boolean,
    onCryptoSuccess: (Cipher) -> Unit,
    onError: (String) -> Unit,
  ) {
    val executor = ContextCompat.getMainExecutor(activity)
    val prompt =
      BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
          override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            val authedCipher = result.cryptoObject?.cipher
            if (authedCipher != null) {
              onCryptoSuccess(authedCipher)
            } else {
              onError("لم يتم استلام كائن التشفير من النظام.")
            }
          }

          override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
              errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
              errorCode != BiometricPrompt.ERROR_CANCELED
            ) {
              onError(errString.toString())
            } else {
              onError("تم إلغاء التحقق البيومتري.")
            }
          }
        },
      )

    val infoBuilder =
      BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setConfirmationRequired(false)

    if (allowDeviceCredentialFallback && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      infoBuilder.setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
    } else {
      infoBuilder.setAllowedAuthenticators(BIOMETRIC_STRONG)
      infoBuilder.setNegativeButtonText("إلغاء")
    }

    prompt.authenticate(infoBuilder.build(), BiometricPrompt.CryptoObject(cipher))
  }

  private fun launchPromptWithoutCrypto(
    activity: FragmentActivity,
    title: String,
    subtitle: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
  ) {
    val executor = ContextCompat.getMainExecutor(activity)
    val prompt =
      BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
          override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            onSuccess()
          }

          override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
              errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
              errorCode != BiometricPrompt.ERROR_CANCELED
            ) {
              onError(errString.toString())
            } else {
              onError("تم إلغاء التحقق.")
            }
          }
        },
      )

    val infoBuilder =
      BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setConfirmationRequired(false)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      infoBuilder.setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
    } else {
      val strongAvailable =
        BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
      if (strongAvailable) {
        infoBuilder.setAllowedAuthenticators(BIOMETRIC_STRONG)
        infoBuilder.setNegativeButtonText("إلغاء")
      } else {
        @Suppress("DEPRECATION")
        infoBuilder.setDeviceCredentialAllowed(true)
      }
    }

    prompt.authenticate(infoBuilder.build())
  }
}
