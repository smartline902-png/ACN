package com.example.data.repository

import com.example.data.local.dao.AccountantDao
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
import com.example.domain.ArabicDateFormatter
import com.example.domain.MoneyMath
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class KmaBackupSnapshot(
  val app: String = "My Personal Accountant - KMA",
  val exportedAtMillis: Long,
  val accounts: List<AccountEntity>,
  val categories: List<CategoryEntity>,
  val transactions: List<TransactionEntity>,
  val debts: List<DebtEntity>,
  val installments: List<InstallmentEntity>,
  val savingsGoals: List<SavingsGoalEntity>,
)

class AccountantRepository(private val dao: AccountantDao) {

  val accountsFlow: Flow<List<AccountEntity>> = dao.observeAccounts()
  val categoriesFlow: Flow<List<CategoryEntity>> = dao.observeCategories()
  val transactionsFlow: Flow<List<TransactionEntity>> = dao.observeAllTransactions()
  val debtsFlow: Flow<List<DebtEntity>> = dao.observeDebts()
  val installmentsFlow: Flow<List<InstallmentEntity>> = dao.observeAllInstallments()
  val savingsGoalsFlow: Flow<List<SavingsGoalEntity>> = dao.observeSavingsGoals()

  private val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
  }

  suspend fun createAccount(
    name: String,
    type: AccountType,
    initialBalanceMinor: Long,
    colorHex: Long,
    setAsDefault: Boolean,
  ): Result<Long> {
    val cleanName = name.trim()
    if (cleanName.isEmpty()) {
      return Result.failure(IllegalArgumentException("يرجى إدخال اسم الحساب."))
    }
    if (initialBalanceMinor < 0L && type != AccountType.CREDIT_CARD) {
      return Result.failure(IllegalArgumentException("لا يمكن أن يكون الرصيد الافتتاحي سالباً لهذا النوع من الحسابات."))
    }
    val existing = dao.getAccountsOnce()
    val shouldBeDefault = setAsDefault || existing.isEmpty()
    val id =
      dao.insertAccount(
        AccountEntity(
          name = cleanName,
          type = type,
          balanceMinor = initialBalanceMinor,
          initialBalanceMinor = initialBalanceMinor,
          colorHex = colorHex,
          isDefault = shouldBeDefault,
        )
      )
    if (shouldBeDefault && existing.isNotEmpty()) {
      dao.setDefaultAccount(id)
    }
    return Result.success(id)
  }

  suspend fun updateAccount(account: AccountEntity, newName: String, newType: AccountType, newColorHex: Long): Result<Unit> {
    val cleanName = newName.trim()
    if (cleanName.isEmpty()) {
      return Result.failure(IllegalArgumentException("يرجى إدخال اسم الحساب."))
    }
    dao.updateAccount(
      account.copy(
        name = cleanName,
        type = newType,
        colorHex = newColorHex,
      )
    )
    return Result.success(Unit)
  }

  suspend fun setDefaultAccount(accountId: Long) {
    dao.setDefaultAccount(accountId)
  }

  suspend fun deleteAccount(accountId: Long) {
    dao.deleteAccountById(accountId)
  }

  suspend fun addTransaction(
    type: TransactionType,
    amountMinor: Long,
    accountId: Long,
    toAccountId: Long?,
    categoryId: Long?,
    title: String,
    note: String,
    timestamp: Long,
    allowOverdraft: Boolean = false,
  ): Result<Long> {
    if (amountMinor <= 0L) {
      return Result.failure(IllegalArgumentException("يجب أن يكون المبلغ أكبر من صفر."))
    }
    val cleanTitle = title.trim()
    if (cleanTitle.isEmpty()) {
      return Result.failure(IllegalArgumentException("يرجى إدخال عنوان أو وصف مختصر للمعاملة."))
    }
    val sourceAccount =
      dao.getAccountById(accountId)
        ?: return Result.failure(IllegalArgumentException("الحساب المحدد غير موجود."))

    if (type == TransactionType.TRANSFER) {
      if (toAccountId == null || toAccountId == accountId) {
        return Result.failure(IllegalArgumentException("يرجى اختيار حساب مستلم مختلف عن الحساب المحوِّل."))
      }
      dao.getAccountById(toAccountId)
        ?: return Result.failure(IllegalArgumentException("الحساب المستلم غير موجود."))
    }

    if ((type == TransactionType.EXPENSE || type == TransactionType.TRANSFER) &&
      sourceAccount.type != AccountType.CREDIT_CARD &&
      !allowOverdraft &&
      sourceAccount.balanceMinor < amountMinor
    ) {
      return Result.failure(
        IllegalStateException("رصيد حساب «${sourceAccount.name}» غير كافٍ لإتمام هذه العملية.")
      )
    }

    val txId =
      dao.insertTransactionAndUpdateBalance(
        TransactionEntity(
          type = type,
          amountMinor = amountMinor,
          accountId = accountId,
          toAccountId = if (type == TransactionType.TRANSFER) toAccountId else null,
          categoryId = if (type == TransactionType.TRANSFER) null else categoryId,
          title = cleanTitle,
          note = note.trim(),
          timestamp = timestamp,
        )
      )
    return Result.success(txId)
  }

  suspend fun updateTransaction(
    existing: TransactionEntity,
    type: TransactionType,
    amountMinor: Long,
    accountId: Long,
    toAccountId: Long?,
    categoryId: Long?,
    title: String,
    note: String,
    timestamp: Long,
  ): Result<Unit> {
    if (amountMinor <= 0L) {
      return Result.failure(IllegalArgumentException("يجب أن يكون المبلغ أكبر من صفر."))
    }
    val cleanTitle = title.trim()
    if (cleanTitle.isEmpty()) {
      return Result.failure(IllegalArgumentException("يرجى إدخال وصف المعاملة."))
    }
    if (type == TransactionType.TRANSFER && (toAccountId == null || toAccountId == accountId)) {
      return Result.failure(IllegalArgumentException("يرجى اختيار حساب مستلم مختلف للتحويل."))
    }
    dao.updateTransactionAndAdjustBalances(
      existing.copy(
        type = type,
        amountMinor = amountMinor,
        accountId = accountId,
        toAccountId = if (type == TransactionType.TRANSFER) toAccountId else null,
        categoryId = if (type == TransactionType.TRANSFER) null else categoryId,
        title = cleanTitle,
        note = note.trim(),
        timestamp = timestamp,
      )
    )
    return Result.success(Unit)
  }

  suspend fun deleteTransaction(transactionId: Long) {
    dao.deleteTransactionAndReverseBalance(transactionId)
  }

  suspend fun addCategory(
    name: String,
    type: TransactionType,
    colorHex: Long,
    monthlyBudgetMinor: Long?,
  ): Result<Long> {
    val clean = name.trim()
    if (clean.isEmpty()) {
      return Result.failure(IllegalArgumentException("يرجى إدخال اسم التصنيف."))
    }
    val id =
      dao.insertCategory(
        CategoryEntity(
          name = clean,
          type = type,
          iconKey = if (type == TransactionType.INCOME) "payments" else "category",
          colorHex = colorHex,
          monthlyBudgetMinor = monthlyBudgetMinor,
          isSystemDefault = false,
        )
      )
    return Result.success(id)
  }

  suspend fun updateCategoryBudget(category: CategoryEntity, monthlyBudgetMinor: Long?) {
    dao.updateCategory(category.copy(monthlyBudgetMinor = monthlyBudgetMinor?.takeIf { it > 0L }))
  }

  suspend fun createDebt(
    personName: String,
    phone: String,
    direction: DebtDirection,
    totalAmountMinor: Long,
    installmentsCount: Int,
    firstDueDateMillis: Long,
    note: String,
    linkedAccountId: Long?,
  ): Result<Long> {
    val cleanPerson = personName.trim()
    if (cleanPerson.isEmpty()) {
      return Result.failure(IllegalArgumentException("يرجى إدخال اسم الشخص أو الجهة."))
    }
    if (totalAmountMinor <= 0L) {
      return Result.failure(IllegalArgumentException("يجب أن يكون إجمالي الدين أكبر من صفر."))
    }
    val safeCount = installmentsCount.coerceIn(1, 120)

    if (linkedAccountId != null && direction == DebtDirection.OWED_TO_ME) {
      val acc = dao.getAccountById(linkedAccountId)
        ?: return Result.failure(IllegalArgumentException("الحساب المحدد غير موجود."))
      if (acc.type != AccountType.CREDIT_CARD && acc.balanceMinor < totalAmountMinor) {
        return Result.failure(
          IllegalStateException("رصيد حساب «${acc.name}» غير كافٍ لإقراض هذا المبلغ.")
        )
      }
    }

    val splits = MoneyMath.splitIntoInstallments(totalAmountMinor, safeCount)
    val installments =
      splits.mapIndexed { index, installmentAmount ->
        InstallmentEntity(
          debtId = 0L,
          installmentNumber = index + 1,
          amountMinor = installmentAmount,
          dueDate = ArabicDateFormatter.addMonths(firstDueDateMillis, index),
        )
      }
    val finalDueDate = installments.last().dueDate
    val debt =
      DebtEntity(
        personName = cleanPerson,
        phone = phone.trim(),
        direction = direction,
        totalAmountMinor = totalAmountMinor,
        paidAmountMinor = 0L,
        installmentsCount = safeCount,
        dueDate = finalDueDate,
        note = note.trim(),
        status = DebtStatus.ACTIVE,
      )

    val debtId = dao.createDebtWithInstallments(debt, installments, linkedAccountId)
    return Result.success(debtId)
  }

  suspend fun recordDebtPayment(
    debtId: Long,
    paymentMinor: Long,
    accountId: Long,
    note: String,
    allowOverdraft: Boolean = false,
  ): Result<Unit> {
    if (paymentMinor <= 0L) {
      return Result.failure(IllegalArgumentException("يجب أن يكون مبلغ الدفعة أكبر من صفر."))
    }
    val debt =
      dao.getDebtById(debtId)
        ?: return Result.failure(IllegalArgumentException("السجل غير موجود."))
    if (debt.remainingMinor <= 0L) {
      return Result.failure(IllegalStateException("هذا الدين مسدد بالكامل بالفعل."))
    }
    if (paymentMinor > debt.remainingMinor) {
      return Result.failure(IllegalArgumentException("المبلغ المدخل أكبر من المتبقي على هذا الدين."))
    }
    val account =
      dao.getAccountById(accountId)
        ?: return Result.failure(IllegalArgumentException("يرجى اختيار حساب صالح."))

    if (debt.direction == DebtDirection.OWED_BY_ME &&
      account.type != AccountType.CREDIT_CARD &&
      !allowOverdraft &&
      account.balanceMinor < paymentMinor
    ) {
      return Result.failure(
        IllegalStateException("رصيد حساب «${account.name}» غير كافٍ لسداد هذه الدفعة.")
      )
    }

    dao.recordDebtPayment(
      debtId = debtId,
      paymentMinor = paymentMinor,
      accountId = accountId,
      timestamp = System.currentTimeMillis(),
      note = note.trim(),
    )
    return Result.success(Unit)
  }

  suspend fun deleteDebt(debtId: Long) {
    dao.deleteDebtById(debtId)
  }

  suspend fun createSavingsGoal(
    name: String,
    targetAmountMinor: Long,
    targetDateMillis: Long?,
    colorHex: Long,
  ): Result<Long> {
    val cleanName = name.trim()
    if (cleanName.isEmpty()) {
      return Result.failure(IllegalArgumentException("يرجى إدخال اسم الهدف الادخاري."))
    }
    if (targetAmountMinor <= 0L) {
      return Result.failure(IllegalArgumentException("يجب أن يكون المبلغ المستهدف أكبر من صفر."))
    }
    val id =
      dao.insertSavingsGoal(
        SavingsGoalEntity(
          name = cleanName,
          targetAmountMinor = targetAmountMinor,
          targetDate = targetDateMillis,
          colorHex = colorHex,
        )
      )
    return Result.success(id)
  }

  suspend fun contributeToSavingsGoal(
    goalId: Long,
    accountId: Long,
    amountMinor: Long,
    isWithdrawal: Boolean,
  ): Result<Unit> {
    if (amountMinor <= 0L) {
      return Result.failure(IllegalArgumentException("يجب أن يكون المبلغ أكبر من صفر."))
    }
    val goal =
      dao.getSavingsGoalById(goalId)
        ?: return Result.failure(IllegalArgumentException("الهدف غير موجود."))
    val account =
      dao.getAccountById(accountId)
        ?: return Result.failure(IllegalArgumentException("الحساب المحدد غير موجود."))

    if (isWithdrawal) {
      if (goal.savedAmountMinor < amountMinor) {
        return Result.failure(IllegalArgumentException("المبلغ المطلوب سحبه أكبر من المدخر في هذا الهدف."))
      }
    } else {
      if (account.type != AccountType.CREDIT_CARD && account.balanceMinor < amountMinor) {
        return Result.failure(IllegalStateException("رصيد حساب «${account.name}» غير كافٍ للتحويل للهدف."))
      }
    }

    dao.contributeToSavingsGoal(
      goalId = goalId,
      accountId = accountId,
      amountMinor = amountMinor,
      isWithdrawal = isWithdrawal,
      timestamp = System.currentTimeMillis(),
    )
    return Result.success(Unit)
  }

  suspend fun deleteSavingsGoal(goalId: Long) {
    dao.deleteSavingsGoalById(goalId)
  }

  suspend fun exportJsonBackup(): String {
    val snapshot =
      KmaBackupSnapshot(
        exportedAtMillis = System.currentTimeMillis(),
        accounts = dao.getAccountsOnce(),
        categories = dao.getCategoriesOnce(),
        transactions = dao.getTransactionsOnce(),
        debts = dao.getDebtsOnce(),
        installments = dao.getInstallmentsOnce(),
        savingsGoals = dao.getSavingsGoalsOnce(),
      )
    return json.encodeToString(snapshot)
  }

  suspend fun resetAllFinancialData() {
    dao.clearAllUserData()
  }
}
