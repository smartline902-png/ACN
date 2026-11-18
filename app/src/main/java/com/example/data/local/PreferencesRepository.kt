package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.CurrencyOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kma_settings")

enum class ThemeMode(val arabicLabel: String) {
  SYSTEM("تلقائي (حسب النظام)"),
  LIGHT("الوضع الفاتح"),
  DARK("الوضع الداكن"),
}

data class AppPreferences(
  val currency: CurrencyOption = CurrencyOption.EGP,
  val hideBalances: Boolean = false,
  val biometricLockEnabled: Boolean = false,
  val themeMode: ThemeMode = ThemeMode.SYSTEM,
  val encryptedLockToken: String? = null,
  val encryptedLockIv: String? = null,
)

class PreferencesRepository(private val context: Context) {

  private object Keys {
    val CURRENCY_CODE = stringPreferencesKey("currency_code")
    val HIDE_BALANCES = booleanPreferencesKey("hide_balances")
    val BIOMETRIC_LOCK_ENABLED = booleanPreferencesKey("biometric_lock_enabled")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val LOCK_TOKEN = stringPreferencesKey("lock_token")
    val LOCK_IV = stringPreferencesKey("lock_iv")
  }

  val preferencesFlow: Flow<AppPreferences> =
    context.dataStore.data.map { prefs ->
      AppPreferences(
        currency = CurrencyOption.fromCode(prefs[Keys.CURRENCY_CODE]),
        hideBalances = prefs[Keys.HIDE_BALANCES] ?: false,
        biometricLockEnabled = prefs[Keys.BIOMETRIC_LOCK_ENABLED] ?: false,
        themeMode =
          runCatching { ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM),
        encryptedLockToken = prefs[Keys.LOCK_TOKEN],
        encryptedLockIv = prefs[Keys.LOCK_IV],
      )
    }

  suspend fun setCurrency(currency: CurrencyOption) {
    context.dataStore.edit { it[Keys.CURRENCY_CODE] = currency.code }
  }

  suspend fun setHideBalances(hide: Boolean) {
    context.dataStore.edit { it[Keys.HIDE_BALANCES] = hide }
  }

  suspend fun setThemeMode(mode: ThemeMode) {
    context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
  }

  suspend fun enableBiometricLock(ciphertextBase64: String, ivBase64: String) {
    context.dataStore.edit { prefs ->
      prefs[Keys.BIOMETRIC_LOCK_ENABLED] = true
      prefs[Keys.LOCK_TOKEN] = ciphertextBase64
      prefs[Keys.LOCK_IV] = ivBase64
    }
  }

  suspend fun disableBiometricLock() {
    context.dataStore.edit { prefs ->
      prefs[Keys.BIOMETRIC_LOCK_ENABLED] = false
      prefs.remove(Keys.LOCK_TOKEN)
      prefs.remove(Keys.LOCK_IV)
    }
  }
}
