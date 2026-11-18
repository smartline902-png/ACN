package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.KmaDatabase
import com.example.data.local.entity.AccountType
import com.example.data.local.entity.DebtDirection
import com.example.data.local.entity.DebtStatus
import com.example.data.local.entity.TransactionType
import com.example.data.repository.AccountantRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AccountantRepositoryRobolectricTest {

  private lateinit var db: KmaDatabase
  private lateinit var repository: AccountantRepository

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, KmaDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    repository = AccountantRepository(db.accountantDao())
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun `atomic transactions, transfers, overdraft guard, and deletion reversal work accurately`() = runTest {
    val cashId = repository.createAccount("نقدي", AccountType.CASH, 100_000L, 0xFF0B4F3BL, true).getOrThrow()
    val bankId = repository.createAccount("البنك", AccountType.BANK, 200_000L, 0xFF1565C0L, false).getOrThrow()

    // Overdraft must fail when balance is insufficient
    val overdraftResult =
      repository.addTransaction(
        type = TransactionType.EXPENSE,
        amountMinor = 150_000L,
        accountId = cashId,
        toAccountId = null,
        categoryId = null,
        title = "شراء كبير",
        note = "",
        timestamp = System.currentTimeMillis(),
        allowOverdraft = false,
      )
    assertTrue(overdraftResult.isFailure)

    // Transfer 500.00 from bank to cash
    val transferId =
      repository.addTransaction(
        type = TransactionType.TRANSFER,
        amountMinor = 50_000L,
        accountId = bankId,
        toAccountId = cashId,
        categoryId = null,
        title = "تحويل للكاش",
        note = "",
        timestamp = System.currentTimeMillis(),
        allowOverdraft = false,
      ).getOrThrow()

    var accounts = repository.accountsFlow.first().associateBy { it.id }
    assertEquals(150_000L, accounts[cashId]?.balanceMinor)
    assertEquals(150_000L, accounts[bankId]?.balanceMinor)

    // Delete transfer -> reverses both balances atomically
    repository.deleteTransaction(transferId)
    accounts = repository.accountsFlow.first().associateBy { it.id }
    assertEquals(100_000L, accounts[cashId]?.balanceMinor)
    assertEquals(200_000L, accounts[bankId]?.balanceMinor)
  }

  @Test
  fun `debt with 3 installments splits accurately and payment updates account and schedule`() = runTest {
    val accountId = repository.createAccount("حسابي", AccountType.BANK, 100_000L, 0xFF0B4F3BL, true).getOrThrow()
    val debtId =
      repository.createDebt(
        personName = "شركة تقسيط",
        phone = "",
        direction = DebtDirection.OWED_BY_ME,
        totalAmountMinor = 10_000L, // 100.00 across 3 installments -> 33.34 + 33.33 + 33.33
        installmentsCount = 3,
        firstDueDateMillis = System.currentTimeMillis() + 86_400_000L,
        note = "",
        linkedAccountId = null,
      ).getOrThrow()

    val installmentsBefore = repository.installmentsFlow.first().filter { it.debtId == debtId }
    assertEquals(3, installmentsBefore.size)
    assertEquals(3_334L, installmentsBefore[0].amountMinor)
    assertEquals(10_000L, installmentsBefore.sumOf { it.amountMinor })

    // Pay first installment (33.34)
    repository.recordDebtPayment(debtId, 3_334L, accountId, "سداد أول قسط", false).getOrThrow()

    val debtAfterFirst = repository.debtsFlow.first().first { it.id == debtId }
    assertEquals(3_334L, debtAfterFirst.paidAmountMinor)
    assertEquals(6_666L, debtAfterFirst.remainingMinor)
    assertEquals(DebtStatus.ACTIVE, debtAfterFirst.status)

    val accountAfter = repository.accountsFlow.first().first { it.id == accountId }
    assertEquals(96_666L, accountAfter.balanceMinor)

    // Pay remaining 66.66
    repository.recordDebtPayment(debtId, 6_666L, accountId, "سداد الباقي", false).getOrThrow()
    val debtFinal = repository.debtsFlow.first().first { it.id == debtId }
    assertEquals(DebtStatus.PAID, debtFinal.status)
    assertEquals(0L, debtFinal.remainingMinor)
  }
}
