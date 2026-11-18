package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import com.example.ui.KmaAccountantApp
import com.example.ui.viewmodel.AccountantViewModel

class MainActivity : FragmentActivity() {

  private val viewModel: AccountantViewModel by viewModels {
    val container = (application as KmaApplication).container
    AccountantViewModel.Factory(
      repository = container.accountantRepository,
      preferencesRepository = container.preferencesRepository,
      biometricKeyManager = container.biometricKeyManager,
    )
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      KmaAccountantApp(
        viewModel = viewModel,
        activity = this,
      )
    }
  }
}
