package com.example.ui.viewmodel

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppPreferences
import com.example.data.local.PreferencesRepository
import com.example.data.local.ThemeMode
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AccountType
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.DebtDirection
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.DebtStatus
import com.example.data.local.entity.InstallmentEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import com.example.data.repository.AccountantRepository
import com.example.domain.ArabicDateFormatter
import com.example.domain.CurrencyOption
import com.example.security.BiometricKeyManager
import com.example.security.BiometricSupportStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoryBreakdownItem(
  val category: CategoryEntity,
  val spentMinor: Long,
  val shareRatio: Float,
)

data class UpcomingInstallmentItem(
  val installment: InstallmentEntity,
  val debt: DebtEntity,
  val isOverdue: Boolean,
)

data class AccountantUiState(
  val isLoading: Boolean = true,
  val preferences: AppPreferences = AppPreferences(),
  val accounts: List<AccountEntity> = emptyList(),
  val categories: List<CategoryEntity> = emptyList(),
  val transactions: List<TransactionEntity> = emptyList(),
  val debts: List<DebtEntity> = emptyList(),
  val installments: List<InstallmentEntity> = emptyList(),
  val savingsGoals: List<SavingsGoalEntity> = emptyList(),
  val totalAccountsBalanceMinor: Long = 0L,
  val totalReceivablesMinor: Long = 0L,
  val totalPayablesMinor: Long = 0L,
  val totalSavingsMinor: Long = 0L,
  val netWorthMinor: Long = 0L,
  val monthlyIncomeMinor: Long = 0L,
  val monthlyExpenseMinor: Long = 0L,
  val monthlyCategoryBreakdown: List<CategoryBreakdownItem> = emptyList(),
  val upcomingInstallments: List<UpcomingInstallmentItem> = emptyList(),
  val biometricSupportStatus: BiometricSupportStatus = BiometricSupportStatus.UNAVAILABLE,
  val isKeystoreKeyPresent: Boolean = false,
)

sealed interface UiEvent {
  data class ShowMessage(val message: String, val isError: Boolean = false) : UiEvent
  data class ShareBackupJson(val jsonPayload: String) : UiEvent
}

private data class RawDataBundle(
  val prefs: AppPreferences,
  val accounts: List<AccountEntity>,
  val categories: List<CategoryEntity>,
  val transactions: List<TransactionEntity>,
  val debts: List<DebtEntity>,
  val installments: List<InstallmentEntity>,
  val goals: List<SavingsGoalEntity>,
)

class AccountantViewModel(
  private val repository: AccountantRepository,
  private val preferencesRepository: PreferencesRepository,
  private val biometricKeyManager: BiometricKeyManager,
) : ViewModel() {

  private val _events = MutableSharedFlow<UiEvent>()
  val events: SharedFlow<UiEvent> = _events.asSharedFlow()

  private val _isUnlocked = MutableStateFlow(false)
  val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

  private val _lockError = MutableStateFlow<String?>(null)
  val lockError: StateFlow<String?> = _lockError.asStateFlow()

  private var initialLockEvaluated = false

  private val firstFiveFlow =
    combine(
      preferencesRepository.preferencesFlow,
      repository.accountsFlow,
      repository.categoriesFlow,
      repository.transactionsFlow,
      repository.debtsFlow,
    ) { prefs, accounts, categories, transactions, debts ->
      arrayOf(prefs, accounts, categories, transactions, debts)
    }

  val uiState: StateFlow<AccountantUiState> =
    combine(
      firstFiveFlow,
      repository.installmentsFlow,
      repository.savingsGoalsFlow,
    ) { firstFive, installments, goals ->
      @Suppress("UNCHECKED_CAST")
      val bundle =
        RawDataBundle(
          prefs = firstFive[0] as AppPreferences,
          accounts = firstFive[1] as List<AccountEntity>,
          categories = firstFive[2] as List<CategoryEntity>,
          transactions = firstFive[3] as List<TransactionEntity>,
          debts = firstFive[4] as List<DebtEntity>,
          installments = installments,
          goals = goals,
        )

      if (!initialLockEvaluated) {
        initialLockEvaluated = true
        if (!bundle.prefs.biometricLockEnabled || !biometricKeyManager.canUseLock()) {
          _isUnlocked.value = true
        }
      }

      buildUiState(bundle)
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = AccountantUiState(),
    )

  private fun buildUiState(bundle: RawDataBundle): AccountantUiState {
    val now = System.currentTimeMillis()
    val monthStart = ArabicDateFormatter.startOfCurrentMonthMillis(now)
    val monthEnd = ArabicDateFormatter.endOfCurrentMonthMillis(now)

    val totalAccounts = bundle.accounts.sumOf { it.balanceMinor }
    val activeDebts = bundle.debts.filter { it.status != DebtStatus.PAID }
    val totalReceivables =
      activeDebts.filter { it.direction == DebtDirection.OWED_TO_ME }.sumOf { it.remainingMinor }
    val totalPayables =
      activeDebts.filter { it.direction == DebtDirection.OWED_BY_ME }.sumOf { it.remainingMinor }
    val totalSavings = bundle.goals.sumOf { it.savedAmountMinor }

    val netWorth = totalAccounts + totalReceivables + totalSavings - totalPayables

    val monthTx = bundle.transactions.filter { it.timestamp in monthStart..monthEnd }
    val monthlyIncome =
      monthTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amountMinor }
    val monthlyExpense =
      monthTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor }

    val categoryMap = bundle.categories.associateBy { it.id }
    val spentByCategory =
      monthTx
        .filter { it.type == TransactionType.EXPENSE && it.categoryId != null }
        .groupBy { it.categoryId!! }
        .mapValues { (_, list) -> list.sumOf { it.amountMinor } }

    val breakdown =
      spentByCategory.entries
        .mapNotNull { (catId, spent) ->
          val cat = categoryMap[catId] ?: return@mapNotNull null
          CategoryBreakdownItem(
            category = cat,
            spentMinor = spent,
            shareRatio =
              if (monthlyExpense > 0L) (spent.toFloat() / monthlyExpense.toFloat()).coerceIn(0f, 1f)
              else 0f,
          )
        }
        .sortedByDescending { it.spentMinor }

    val debtMap = bundle.debts.associateBy { it.id }
    val upcoming =
      bundle.installments
        .filter { !it.isPaid && it.remainingMinor > 0L }
        .mapNotNull { inst ->
          val debt = debtMap[inst.debtId] ?: return@mapNotNull null
          if (debt.status == DebtStatus.PAID) return@mapNotNull null
          UpcomingInstallmentItem(
            installment = inst,
            debt = debt,
            isOverdue = inst.dueDate < now,
          )
        }
        .sortedBy { it.installment.dueDate }

    return AccountantUiState(
      isLoading = false,
      preferences = bundle.prefs,
      accounts = bundle.accounts,
      categories = bundle.categories,
      transactions = bundle.transactions,
      debts = bundle.debts,
      installments = bundle.installments,
      savingsGoals = bundle.goals,
      totalAccountsBalanceMinor = totalAccounts,
      totalReceivablesMinor = totalReceivables,
      totalPayablesMinor = totalPayables,
      totalSavingsMinor = totalSavings,
      netWorthMinor = netWorth,
      monthlyIncomeMinor = monthlyIncome,
      monthlyExpenseMinor = monthlyExpense,
      monthlyCategoryBreakdown = breakdown,
      upcomingInstallments = upcoming,
      biometricSupportStatus = biometricKeyManager.getSupportStatus(),
      isKeystoreKeyPresent = biometricKeyManager.hasKeyInKeystore(),
    )
  }

  // --- Security & Biometrics ---

  fun requestBiometricUnlock(activity: FragmentActivity) {
    _lockError.value = null
    val prefs = uiState.value.preferences
    biometricKeyManager.authenticateAndVerifyUnlock(
      activity = activity,
      title = "فتح الخزنة المالية",
      subtitle = "تحقق بالبصمة أو رمز الجهاز للوصول إلى محاسبك الشخصي",
      storedCiphertextBase64 = prefs.encryptedLockToken,
      storedIvBase64 = prefs.encryptedLockIv,
      onUnlocked = {
        _isUnlocked.value = true
        _lockError.value = null
      },
      onError = { message ->
        _lockError.value = message
      },
    )
  }

  fun lockAppNow() {
    if (uiState.value.preferences.biometricLockEnabled && biometricKeyManager.canUseLock()) {
      _lockError.value = null
      _isUnlocked.value = false
    }
  }

  fun toggleBiometricLock(activity: FragmentActivity, enable: Boolean) {
    if (enable) {
      biometricKeyManager.authenticateAndSealLockToken(
        activity = activity,
        title = "تفعيل حماية المفاتيح البيومترية",
        subtitle = "سيتم ربط مفتاح التشفير في Android Keystore ببصمتك",
        onSuccess = { sealed ->
          viewModelScope.launch {
            preferencesRepository.enableBiometricLock(sealed.ciphertextBase64, sealed.ivBase64)
            _isUnlocked.value = true
            _events.emit(UiEvent.ShowMessage("تم تفعيل القفل البيومتري وحماية مفتاح التشفير بنجاح."))
          }
        },
        onError = { err ->
          viewModelScope.launch { _events.emit(UiEvent.ShowMessage(err, isError = true)) }
        },
      )
    } else {
      val prefs = uiState.value.preferences
      biometricKeyManager.authenticateAndVerifyUnlock(
        activity = activity,
        title = "تأكيد تعطيل القفل البيومتري",
        subtitle = "يرجى التحقق من هويتك لإلغاء القفل البيومتري",
        storedCiphertextBase64 = prefs.encryptedLockToken,
        storedIvBase64 = prefs.encryptedLockIv,
        onUnlocked = {
          viewModelScope.launch {
            biometricKeyManager.deleteKey()
            preferencesRepository.disableBiometricLock()
            _events.emit(UiEvent.ShowMessage("تم تعطيل القفل البيومتري وإزالة مفتاح الخزنة."))
          }
        },
        onError = { err ->
          viewModelScope.launch { _events.emit(UiEvent.ShowMessage(err, isError = true)) }
        },
      )
    }
  }

  // --- Preferences ---

  fun setCurrency(currency: CurrencyOption) {
    viewModelScope.launch {
      preferencesRepository.setCurrency(currency)
      _events.emit(UiEvent.ShowMessage("تم تغيير العملة إلى ${currency.arabicName}"))
    }
  }

  fun toggleHideBalances() {
    val current = uiState.value.preferences.hideBalances
    viewModelScope.launch { preferencesRepository.setHideBalances(!current) }
  }

  fun setThemeMode(mode: ThemeMode) {
    viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
  }

  // --- Accounts ---

  fun createAccount(
    name: String,
    type: AccountType,
    initialBalanceMinor: Long,
    colorHex: Long,
    setAsDefault: Boolean,
  ) {
    viewModelScope.launch {
      val result = repository.createAccount(name, type, initialBalanceMinor, colorHex, setAsDefault)
      result.fold(
        onSuccess = { _events.emit(UiEvent.ShowMessage("تم إنشاء الحساب «${name.trim()}» بنجاح.")) },
        onFailure = { _events.emit(UiEvent.ShowMessage(it.message ?: "تعذر إنشاء الحساب", isError = true)) },
      )
    }
  }

  fun updateAccount(account: AccountEntity, name: String, type: AccountType, colorHex: Long) {
    viewModelScope.launch {
      repository.updateAccount(account, name, type, colorHex).fold(
        onSuccess = { _events.emit(UiEvent.ShowMessage("تم تحديث بيانات الحساب.")) },
        onFailure = { _events.emit(UiEvent.ShowMessage(it.message ?: "تعذر التحديث", isError = true)) },
      )
    }
  }

  fun setDefaultAccount(accountId: Long) {
    viewModelScope.launch {
      repository.setDefaultAccount(accountId)
      _events.emit(UiEvent.ShowMessage("تم تعيين الحساب الافتراضي."))
    }
  }

  fun deleteAccount(accountId: Long) {
    viewModelScope.launch {
      repository.deleteAccount(accountId)
      _events.emit(UiEvent.ShowMessage("تم حذف الحساب ومعاملاته المرتبطة."))
    }
  }

  // --- Transactions ---

  fun addTransaction(
    type: TransactionType,
    amountMinor: Long,
    accountId: Long,
    toAccountId: Long?,
    categoryId: Long?,
    title: String,
    note: String,
    timestamp: Long,
    allowOverdraft: Boolean,
  ) {
    viewModelScope.launch {
      repository
        .addTransaction(
          type = type,
          amountMinor = amountMinor,
          accountId = accountId,
          toAccountId = toAccountId,
          categoryId = categoryId,
          title = title,
          note = note,
          timestamp = timestamp,
          allowOverdraft = allowOverdraft,
        )
        .fold(
          onSuccess = { _events.emit(UiEvent.ShowMessage("تم تسجيل المعاملة وتحديث الرصيد بدقة.")) },
          onFailure = { _events.emit(UiEvent.ShowMessage(it.message ?: "تعذر حفظ المعاملة", isError = true)) },
        )
    }
  }

  fun updateTransaction(
    existing: TransactionEntity,
    type: TransactionType,
    amountMinor: Long,
    accountId: Long,
    toAccountId: Long?,
    categoryId: Long?,
    title: String,
    note: String,
    timestamp: Long,
  ) {
    viewModelScope.launch {
      repository
        .updateTransaction(existing, type, amountMinor, accountId, toAccountId, categoryId, title, note, timestamp)
        .fold(
          onSuccess = { _events.emit(UiEvent.ShowMessage("تم تعديل المعاملة وإعادة احتساب الأرصدة.")) },
          onFailure = { _events.emit(UiEvent.ShowMessage(it.message ?: "تعذر التعديل", isError = true)) },
        )
    }
  }

  fun deleteTransaction(txId: Long) {
    viewModelScope.launch {
      repository.deleteTransaction(txId)
      _events.emit(UiEvent.ShowMessage("تم حذف المعاملة وعكس أثرها على الرصيد."))
    }
  }

  // --- Categories & Budgets ---

  fun addCategory(name: String, type: TransactionType, colorHex: Long, monthlyBudgetMinor: Long?) {
    viewModelScope.launch {
      repository.addCategory(name, type, colorHex, monthlyBudgetMinor).fold(
        onSuccess = { _events.emit(UiEvent.ShowMessage("تم إضافة التصنيف بنجاح.")) },
        onFailure = { _events.emit(UiEvent.ShowMessage(it.message ?: "تعذر الإضافة", isError = true)) },
      )
    }
  }

  fun updateCategoryBudget(category: CategoryEntity, monthlyBudgetMinor: Long?) {
    viewModelScope.launch {
      repository.updateCategoryBudget(category, monthlyBudgetMinor)
      _events.emit(UiEvent.ShowMessage("تم تحديث الميزانية الشهرية للتصنيف."))
    }
  }

  // --- Debts & Installments ---

  fun createDebt(
    personName: String,
    phone: String,
    direction: DebtDirection,
    totalAmountMinor: Long,
    installmentsCount: Int,
    firstDueDateMillis: Long,
    note: String,
    linkedAccountId: Long?,
  ) {
    viewModelScope.launch {
      repository
        .createDebt(
          personName = personName,
          phone = phone,
          direction = direction,
          totalAmountMinor = totalAmountMinor,
          installmentsCount = installmentsCount,
          firstDueDateMillis = firstDueDateMillis,
          note = note,
          linkedAccountId = linkedAccountId,
        )
        .fold(
          onSuccess = {
            _events.emit(
              UiEvent.ShowMessage(
                if (installmentsCount > 1) "تم تسجيل الدين وجدولة $installmentsCount أقساط بنجاح."
                else "تم تسجيل الدين بنجاح."
              )
            )
          },
          onFailure = { _events.emit(UiEvent.ShowMessage(it.message ?: "تعذر تسجيل الدين", isError = true)) },
        )
    }
  }

  fun recordDebtPayment(
    debtId: Long,
    paymentMinor: Long,
    accountId: Long,
    note: String,
    allowOverdraft: Boolean,
  ) {
    viewModelScope.launch {
      repository.recordDebtPayment(debtId, paymentMinor, accountId, note, allowOverdraft).fold(
        onSuccess = { _events.emit(UiEvent.ShowMessage("تم تسجيل الدفعة وتحديث الرصيد وجدول الأقساط.")) },
        onFailure = { _events.emit(UiEvent.ShowMessage(it.message ?: "تعذر تسجيل السداد", isError = true)) },
      )
    }
  }

  fun deleteDebt(debtId: Long) {
    viewModelScope.launch {
      repository.deleteDebt(debtId)
      _events.emit(UiEvent.ShowMessage("تم حذف سجل الدين."))
    }
  }

  // --- Savings Goals ---

  fun createSavingsGoal(
    name: String,
    targetAmountMinor: Long,
    targetDateMillis: Long?,
    colorHex: Long,
  ) {
    viewModelScope.launch {
      repository.createSavingsGoal(name, targetAmountMinor, targetDateMillis, colorHex).fold(
        onSuccess = { _events.emit(UiEvent.ShowMessage("تم إنشاء الهدف الادخاري.")) },
        onFailure = { _events.emit(UiEvent.ShowMessage(it.message ?: "تعذر الإنشاء", isError = true)) },
      )
    }
  }

  fun contributeToSavingsGoal(
    goalId: Long,
    accountId: Long,
    amountMinor: Long,
    isWithdrawal: Boolean,
  ) {
    viewModelScope.launch {
      repository.contributeToSavingsGoal(goalId, accountId, amountMinor, isWithdrawal).fold(
        onSuccess = {
          _events.emit(
            UiEvent.ShowMessage(
              if (isWithdrawal) "تم السحب من الهدف وتحويل المبلغ إلى الحساب."
              else "تم الإيداع في الهدف الادخاري بنجاح."
            )
          )
        },
        onFailure = { _events.emit(UiEvent.ShowMessage(it.message ?: "تعذر إتمام العملية", isError = true)) },
      )
    }
  }

  fun deleteSavingsGoal(goalId: Long) {
    viewModelScope.launch {
      repository.deleteSavingsGoal(goalId)
      _events.emit(UiEvent.ShowMessage("تم حذف الهدف الادخاري."))
    }
  }

  // --- Export & Reset ---

  fun exportBackup() {
    viewModelScope.launch {
      try {
        val json = repository.exportJsonBackup()
        _events.emit(UiEvent.ShareBackupJson(json))
      } catch (e: Exception) {
        _events.emit(UiEvent.ShowMessage("تعذر تصدير النسخة الاحتياطية.", isError = true))
      }
    }
  }

  fun resetAllData() {
    viewModelScope.launch {
      repository.resetAllFinancialData()
      _events.emit(UiEvent.ShowMessage("تم مسح جميع السجلات المالية والبدء من جديد."))
    }
  }

  class Factory(
    private val repository: AccountantRepository,
    private val preferencesRepository: PreferencesRepository,
    private val biometricKeyManager: BiometricKeyManager,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return AccountantViewModel(repository, preferencesRepository, biometricKeyManager) as T
    }
  }
}
