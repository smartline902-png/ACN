package com.example

import android.app.Application
import com.example.data.local.KmaDatabase
import com.example.data.local.PreferencesRepository
import com.example.data.repository.AccountantRepository
import com.example.security.BiometricKeyManager

class AppContainer(application: Application) {
  val database: KmaDatabase by lazy { KmaDatabase.getInstance(application) }
  val preferencesRepository: PreferencesRepository by lazy { PreferencesRepository(application) }
  val accountantRepository: AccountantRepository by lazy {
    AccountantRepository(database.accountantDao())
  }
  val biometricKeyManager: BiometricKeyManager by lazy { BiometricKeyManager(application) }
}

class KmaApplication : Application() {
  lateinit var container: AppContainer
    private set

  override fun onCreate() {
    super.onCreate()
    container = AppContainer(this)
  }
}
