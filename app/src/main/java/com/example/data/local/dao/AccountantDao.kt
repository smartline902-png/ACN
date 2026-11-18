package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.DebtDirection
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.DebtStatus
import com.example.data.local.entity.InstallmentEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountantDao {

  // --- Accounts ---
  @Query("SELECT * FROM accounts ORDER BY isDefault DESC, createdAt ASC")
  fun observeAccounts(): Flow<List<AccountEntity>>

  @Query("SELECT * FROM accounts ORDER BY isDefault DESC, createdAt ASC")
  suspend fun getAccountsOnce(): List<AccountEntity>

  @Query("SELECT * FROM accounts WHERE id = :id")
  suspend fun getAccountById(id: Long): AccountEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAccount(account: AccountEntity): Long

  @Update
  suspend fun updateAccount(account: AccountEntity)

  @Query("UPDATE accounts SET balanceMinor = balanceMinor + :deltaMinor WHERE id = :accountId")
  suspend fun adjustAccountBalance(accountId: Long, deltaMinor: Long)

  @Query("DELETE FROM accounts WHERE id = :accountId")
  suspend fun deleteAccountById(accountId: Long)

  @Query("UPDATE accounts SET isDefault = CASE WHEN id = :accountId THEN 1 ELSE 0 END")
  suspend fun setDefaultAccount(accountId: Long)

  // --- Categories ---
  @Query("SELECT * FROM categories ORDER BY type ASC, id ASC")
  fun observeCategories(): Flow<List<CategoryEntity>>

  @Query("SELECT * FROM categories ORDER BY type ASC, id ASC")
  suspend fun getCategoriesOnce(): List<CategoryEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCategory(category: CategoryEntity): Long

  @Update
  suspend fun updateCategory(category: CategoryEntity)

  @Query("DELETE FROM categories WHERE id = :categoryId AND isSystemDefault = 0")
  suspend fun deleteCustomCategory(categoryId: Long)

  // --- Transactions ---
  @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC")
  fun observeAllTransactions(): Flow<List<TransactionEntity>>

  @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC")
  suspend fun getTransactionsOnce(): List<TransactionEntity>

  @Query("SELECT * FROM transactions WHERE id = :id")
  suspend fun getTransactionById(id: Long): TransactionEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTransaction(transaction: TransactionEntity): Long

  @Update
  suspend fun updateTransaction(transaction: TransactionEntity)

  @Query("DELETE FROM transactions WHERE id = :id")
  suspend fun deleteTransactionById(id: Long)

  // --- Debts & Installments ---
  @Query("SELECT * FROM debts ORDER BY status ASC, dueDate ASC, id DESC")
  fun observeDebts(): Flow<List<DebtEntity>>

  @Query("SELECT * FROM debts ORDER BY status ASC, dueDate ASC, id DESC")
  suspend fun getDebtsOnce(): List<DebtEntity>

  @Query("SELECT * FROM debts WHERE id = :debtId")
  suspend fun getDebtById(debtId: Long): DebtEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertDebt(debt: DebtEntity): Long

  @Update
  suspend fun updateDebt(debt: DebtEntity)

  @Query("DELETE FROM debts WHERE id = :debtId")
  suspend fun deleteDebtById(debtId: Long)

  @Query("SELECT * FROM installments ORDER BY dueDate ASC, installmentNumber ASC")
  fun observeAllInstallments(): Flow<List<InstallmentEntity>>

  @Query("SELECT * FROM installments ORDER BY debtId ASC, installmentNumber ASC")
  suspend fun getInstallmentsOnce(): List<InstallmentEntity>

  @Query("SELECT * FROM installments WHERE debtId = :debtId ORDER BY installmentNumber ASC")
  suspend fun getInstallmentsForDebt(debtId: Long): List<InstallmentEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertInstallments(installments: List<InstallmentEntity>)

  @Update
  suspend fun updateInstallment(installment: InstallmentEntity)

  // --- Savings Goals ---
  @Query("SELECT * FROM savings_goals ORDER BY isCompleted ASC, createdAt DESC")
  fun observeSavingsGoals(): Flow<List<SavingsGoalEntity>>

  @Query("SELECT * FROM savings_goals ORDER BY isCompleted ASC, createdAt DESC")
  suspend fun getSavingsGoalsOnce(): List<SavingsGoalEntity>

  @Query("SELECT * FROM savings_goals WHERE id = :goalId")
  suspend fun getSavingsGoalById(goalId: Long): SavingsGoalEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSavingsGoal(goal: SavingsGoalEntity): Long

  @Update
  suspend fun updateSavingsGoal(goal: SavingsGoalEntity)

  @Query("DELETE FROM savings_goals WHERE id = :goalId")
  suspend fun deleteSavingsGoalById(goalId: Long)

  // --- Atomic Transactional Operations ---

  @Transaction
  suspend fun insertTransactionAndUpdateBalance(tx: TransactionEntity): Long {
    val insertedId = insertTransaction(tx)
    when (tx.type) {
      TransactionType.INCOME -> adjustAccountBalance(tx.accountId, tx.amountMinor)
      TransactionType.EXPENSE -> adjustAccountBalance(tx.accountId, -tx.amountMinor)
      TransactionType.TRANSFER -> {
        adjustAccountBalance(tx.accountId, -tx.amountMinor)
        tx.toAccountId?.let { targetId ->
          adjustAccountBalance(targetId, tx.amountMinor)
        }
      }
    }
    return insertedId
  }

  @Transaction
  suspend fun deleteTransactionAndReverseBalance(txId: Long) {
    val existing = getTransactionById(txId) ?: return
    when (existing.type) {
      TransactionType.INCOME -> adjustAccountBalance(existing.accountId, -existing.amountMinor)
      TransactionType.EXPENSE -> adjustAccountBalance(existing.accountId, existing.amountMinor)
      TransactionType.TRANSFER -> {
        adjustAccountBalance(existing.accountId, existing.amountMinor)
        existing.toAccountId?.let { targetId ->
          adjustAccountBalance(targetId, -existing.amountMinor)
        }
      }
    }
    existing.relatedGoalId?.let { goalId ->
      val goal = getSavingsGoalById(goalId)
      if (goal != null) {
        val delta = if (existing.type == TransactionType.EXPENSE) -existing.amountMinor else existing.amountMinor
        val updatedSaved = (goal.savedAmountMinor + delta).coerceAtLeast(0L)
        updateSavingsGoal(
          goal.copy(
            savedAmountMinor = updatedSaved,
            isCompleted = updatedSaved >= goal.targetAmountMinor,
          )
        )
      }
    }
    deleteTransactionById(txId)
  }

  @Transaction
  suspend fun updateTransactionAndAdjustBalances(updated: TransactionEntity) {
    val old = getTransactionById(updated.id) ?: return
    // Reverse old impact
    when (old.type) {
      TransactionType.INCOME -> adjustAccountBalance(old.accountId, -old.amountMinor)
      TransactionType.EXPENSE -> adjustAccountBalance(old.accountId, old.amountMinor)
      TransactionType.TRANSFER -> {
        adjustAccountBalance(old.accountId, old.amountMinor)
        old.toAccountId?.let { adjustAccountBalance(it, -old.amountMinor) }
      }
    }
    // Apply new impact
    when (updated.type) {
      TransactionType.INCOME -> adjustAccountBalance(updated.accountId, updated.amountMinor)
      TransactionType.EXPENSE -> adjustAccountBalance(updated.accountId, -updated.amountMinor)
      TransactionType.TRANSFER -> {
        adjustAccountBalance(updated.accountId, -updated.amountMinor)
        updated.toAccountId?.let { adjustAccountBalance(it, updated.amountMinor) }
      }
    }
    updateTransaction(updated)
  }

  @Transaction
  suspend fun createDebtWithInstallments(
    debt: DebtEntity,
    installments: List<InstallmentEntity>,
    linkedAccountId: Long?,
  ): Long {
    val debtId = insertDebt(debt)
    val itemsWithDebtId = installments.map { it.copy(debtId = debtId) }
    insertInstallments(itemsWithDebtId)

    if (linkedAccountId != null) {
      val txType =
        if (debt.direction == DebtDirection.OWED_BY_ME) {
          TransactionType.INCOME // Borrowed money enters my account
        } else {
          TransactionType.EXPENSE // Lent money leaves my account
        }
      val txTitle =
        if (debt.direction == DebtDirection.OWED_BY_ME) {
          "استلام تمويل / دين من ${debt.personName}"
        } else {
          "إقراض مبلغ إلى ${debt.personName}"
        }
      insertTransactionAndUpdateBalance(
        TransactionEntity(
          type = txType,
          amountMinor = debt.totalAmountMinor,
          accountId = linkedAccountId,
          title = txTitle,
          note = debt.note,
          timestamp = debt.createdAt,
          relatedDebtId = debtId,
        )
      )
    }
    return debtId
  }

  @Transaction
  suspend fun recordDebtPayment(
    debtId: Long,
    paymentMinor: Long,
    accountId: Long,
    timestamp: Long,
    note: String,
  ) {
    val debt = getDebtById(debtId) ?: return
    val effectivePayment = paymentMinor.coerceAtMost(debt.remainingMinor)
    if (effectivePayment <= 0L) return

    val installments = getInstallmentsForDebt(debtId)
    var remainingToAllocate = effectivePayment
    for (inst in installments) {
      if (remainingToAllocate <= 0L) break
      if (inst.isPaid || inst.remainingMinor <= 0L) continue
      val applied = remainingToAllocate.coerceAtMost(inst.remainingMinor)
      val newPaid = inst.paidAmountMinor + applied
      val nowPaid = newPaid >= inst.amountMinor
      updateInstallment(
        inst.copy(
          paidAmountMinor = newPaid,
          isPaid = nowPaid,
          paidAt = if (nowPaid) timestamp else inst.paidAt,
        )
      )
      remainingToAllocate -= applied
    }

    val newDebtPaid = (debt.paidAmountMinor + effectivePayment).coerceAtMost(debt.totalAmountMinor)
    val newStatus = if (newDebtPaid >= debt.totalAmountMinor) DebtStatus.PAID else DebtStatus.ACTIVE
    updateDebt(
      debt.copy(
        paidAmountMinor = newDebtPaid,
        status = newStatus,
      )
    )

    val txType =
      if (debt.direction == DebtDirection.OWED_BY_ME) {
        TransactionType.EXPENSE
      } else {
        TransactionType.INCOME
      }
    val txTitle =
      if (debt.direction == DebtDirection.OWED_BY_ME) {
        "سداد دفعة لـ ${debt.personName}"
      } else {
        "تحصيل دفعة من ${debt.personName}"
      }

    insertTransactionAndUpdateBalance(
      TransactionEntity(
        type = txType,
        amountMinor = effectivePayment,
        accountId = accountId,
        title = txTitle,
        note = note,
        timestamp = timestamp,
        relatedDebtId = debtId,
      )
    )
  }

  @Transaction
  suspend fun contributeToSavingsGoal(
    goalId: Long,
    accountId: Long,
    amountMinor: Long,
    isWithdrawal: Boolean,
    timestamp: Long,
  ) {
    val goal = getSavingsGoalById(goalId) ?: return
    val newSaved =
      if (isWithdrawal) {
        (goal.savedAmountMinor - amountMinor).coerceAtLeast(0L)
      } else {
        goal.savedAmountMinor + amountMinor
      }
    updateSavingsGoal(
      goal.copy(
        savedAmountMinor = newSaved,
        isCompleted = newSaved >= goal.targetAmountMinor,
      )
    )

    val txType = if (isWithdrawal) TransactionType.INCOME else TransactionType.EXPENSE
    val txTitle =
      if (isWithdrawal) {
        "سحب من هدف ادخار: ${goal.name}"
      } else {
        "إيداع في هدف ادخار: ${goal.name}"
      }
    insertTransactionAndUpdateBalance(
      TransactionEntity(
        type = txType,
        amountMinor = amountMinor,
        accountId = accountId,
        title = txTitle,
        timestamp = timestamp,
        relatedGoalId = goalId,
      )
    )
  }

  @Transaction
  suspend fun clearAllUserData() {
    deleteAllInstallments()
    deleteAllDebts()
    deleteAllTransactions()
    deleteAllSavingsGoals()
    deleteAllAccounts()
  }

  @Query("DELETE FROM installments")
  suspend fun deleteAllInstallments()

  @Query("DELETE FROM debts")
  suspend fun deleteAllDebts()

  @Query("DELETE FROM transactions")
  suspend fun deleteAllTransactions()

  @Query("DELETE FROM savings_goals")
  suspend fun deleteAllSavingsGoals()

  @Query("DELETE FROM accounts")
  suspend fun deleteAllAccounts()
}
