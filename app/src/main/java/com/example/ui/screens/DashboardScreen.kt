package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.DebtDirection
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import com.example.domain.ArabicDateFormatter
import com.example.domain.MoneyMath
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.HeroNetWorthCard
import com.example.ui.components.TransactionRowItem
import com.example.ui.theme.LocalKmaFinancialColors
import com.example.ui.viewmodel.AccountantUiState

@Composable
fun DashboardScreen(
  state: AccountantUiState,
  onToggleHideBalances: () -> Unit,
  onLockNow: () -> Unit,
  onQuickTransaction: (TransactionType) -> Unit,
  onCreateAccount: () -> Unit,
  onCreateDebt: () -> Unit,
  onOpenTransaction: (TransactionEntity) -> Unit,
  onOpenDebt: (DebtEntity) -> Unit,
  onNavigateToTransactions: () -> Unit,
  onNavigateToDebts: () -> Unit,
  onNavigateToPlanning: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val currency = state.preferences.currency
  val masked = state.preferences.hideBalances
  val financialColors = LocalKmaFinancialColors.current
  val accountMap = state.accounts.associateBy { it.id }
  val categoryMap = state.categories.associateBy { it.id }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = "محاسبك الشخصي",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Text(
            text = "A KMA Product • ${ArabicDateFormatter.formatMonthYear(System.currentTimeMillis())}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        if (state.preferences.biometricLockEnabled) {
          IconButton(
            onClick = onLockNow,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("dashboard_lock_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Lock,
              contentDescription = "قفل التطبيق فوراً",
              tint = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }
    }

    item {
      HeroNetWorthCard(
        netWorthMinor = state.netWorthMinor,
        totalLiquidityMinor = state.totalAccountsBalanceMinor,
        totalReceivablesMinor = state.totalReceivablesMinor,
        totalPayablesMinor = state.totalPayablesMinor,
        currency = currency,
        hideBalances = masked,
        onToggleHideBalances = onToggleHideBalances,
      )
    }

    if (state.accounts.isEmpty()) {
      item {
        EmptyStatePanel(
          icon = Icons.Filled.AccountBalanceWallet,
          title = "ابدأ بإنشاء أول حساب مالي",
          description = "أنشئ محفظتك النقدية أو حسابك البنكي لتسجيل المعاملات ومراقبة الأرصدة بدقة.",
          actionLabel = "إضافة حساب جديد",
          onAction = onCreateAccount,
          actionTestTag = "dashboard_first_account_button",
        )
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        QuickActionCard(
          label = "مصروف",
          icon = Icons.Filled.ArrowUpward,
          tint = financialColors.expense,
          container = financialColors.expenseContainer,
          onClick = { onQuickTransaction(TransactionType.EXPENSE) },
          testTag = "quick_expense_button",
          modifier = Modifier.weight(1f),
        )
        QuickActionCard(
          label = "دخل",
          icon = Icons.Filled.ArrowDownward,
          tint = financialColors.income,
          container = financialColors.incomeContainer,
          onClick = { onQuickTransaction(TransactionType.INCOME) },
          testTag = "quick_income_button",
          modifier = Modifier.weight(1f),
        )
        QuickActionCard(
          label = "تحويل",
          icon = Icons.Filled.SwapHoriz,
          tint = financialColors.transfer,
          container = financialColors.transferContainer,
          onClick = { onQuickTransaction(TransactionType.TRANSFER) },
          testTag = "quick_transfer_button",
          modifier = Modifier.weight(1f),
        )
        QuickActionCard(
          label = "دين جديد",
          icon = Icons.Filled.Handshake,
          tint = financialColors.debtWarning,
          container = financialColors.debtWarningContainer,
          onClick = onCreateDebt,
          testTag = "quick_debt_button",
          modifier = Modifier.weight(1f),
        )
      }
    }

    // Monthly Cashflow Summary
    item {
      Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "ملخص الشهر الحالي",
              style = MaterialTheme.typography.titleMedium,
            )
            val netMonth = state.monthlyIncomeMinor - state.monthlyExpenseMinor
            Text(
              text = "الصافي: ${MoneyMath.formatMinor(netMonth, currency, showSign = true, masked = masked)}",
              style = MaterialTheme.typography.labelLarge,
              color = if (netMonth >= 0L) financialColors.income else financialColors.expense,
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Surface(
              modifier = Modifier.weight(1f),
              shape = MaterialTheme.shapes.medium,
              color = financialColors.incomeContainer,
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text("إجمالي الدخل", style = MaterialTheme.typography.labelMedium, color = financialColors.income)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = MoneyMath.formatMinor(state.monthlyIncomeMinor, currency, masked = masked),
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = financialColors.income,
                )
              }
            }

            Surface(
              modifier = Modifier.weight(1f),
              shape = MaterialTheme.shapes.medium,
              color = financialColors.expenseContainer,
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text("إجمالي المصروف", style = MaterialTheme.typography.labelMedium, color = financialColors.expense)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = MoneyMath.formatMinor(state.monthlyExpenseMinor, currency, masked = masked),
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = financialColors.expense,
                )
              }
            }
          }
        }
      }
    }

    // Expense Analytics Card
    item {
      Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(text = "توزيع مصروفات الشهر", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onNavigateToPlanning) {
              Text("الميزانيات والادخار")
              Spacer(modifier = Modifier.width(4.dp))
              Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
          }
          CategoryDonutChart(
            breakdown = state.monthlyCategoryBreakdown,
            totalExpenseMinor = state.monthlyExpenseMinor,
            currency = currency,
            hideBalances = masked,
          )
        }
      }
    }

    // Upcoming Installments
    if (state.upcomingInstallments.isNotEmpty()) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(text = "أقساط واستحقاقات قادمة", style = MaterialTheme.typography.titleMedium)
          TextButton(onClick = onNavigateToDebts) {
            Text("كل الديون")
          }
        }
      }
      items(state.upcomingInstallments.take(3), key = { "inst_${it.installment.id}" }) { item ->
        Surface(
          modifier =
            Modifier.fillMaxWidth()
              .clickable { onOpenDebt(item.debt) }
              .testTag("upcoming_installment_${item.installment.id}"),
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.surface,
          border =
            BorderStroke(
              1.dp,
              if (item.isOverdue) financialColors.expense.copy(alpha = 0.6f)
              else MaterialTheme.colorScheme.outlineVariant,
            ),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.Schedule,
              contentDescription = null,
              tint = if (item.isOverdue) financialColors.expense else financialColors.debtWarning,
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "${item.debt.personName} • قسط #${item.installment.installmentNumber}",
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text =
                  "${if (item.debt.direction == DebtDirection.OWED_BY_ME) "سداد مستحق" else "تحصيل مستحق"} • ${ArabicDateFormatter.formatShortDate(item.installment.dueDate)}${if (item.isOverdue) " (متأخر)" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = if (item.isOverdue) financialColors.expense else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Text(
              text = MoneyMath.formatMinor(item.installment.remainingMinor, currency, masked = masked),
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = if (item.isOverdue) financialColors.expense else MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      }
    }

    // Recent Transactions Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(text = "أحدث المعاملات", style = MaterialTheme.typography.titleMedium)
        if (state.transactions.isNotEmpty()) {
          TextButton(onClick = onNavigateToTransactions) {
            Text("عرض السجل الكامل")
          }
        }
      }
    }

    if (state.transactions.isEmpty()) {
      item {
        EmptyStatePanel(
          icon = Icons.Filled.ReceiptLong,
          title = "سجل المعاملات فارغ حالياً",
          description = "كل معاملة تسجلها هنا تُحدّث أرصدة حساباتك وتظهر في تحليلاتك فوراً.",
          actionLabel = if (state.accounts.isEmpty()) "أضف حساباً أولاً" else "تسجيل أول معاملة",
          onAction = {
            if (state.accounts.isEmpty()) onCreateAccount()
            else onQuickTransaction(TransactionType.EXPENSE)
          },
          actionTestTag = "dashboard_empty_tx_action",
        )
      }
    } else {
      items(state.transactions.take(5), key = { "dash_tx_${it.id}" }) { tx ->
        TransactionRowItem(
          transaction = tx,
          account = accountMap[tx.accountId],
          toAccount = tx.toAccountId?.let { accountMap[it] },
          category = tx.categoryId?.let { categoryMap[it] },
          currency = currency,
          hideBalances = masked,
          onClick = { onOpenTransaction(tx) },
        )
      }
    }

    item { Spacer(modifier = Modifier.height(72.dp)) }
  }
}

@Composable
private fun QuickActionCard(
  label: String,
  icon: ImageVector,
  tint: Color,
  container: Color,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.minimumInteractiveComponentSize().clickable(onClick = onClick).testTag(testTag),
    shape = MaterialTheme.shapes.medium,
    color = container,
  ) {
    Column(
      modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = tint,
        modifier = Modifier.size(22.dp),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = tint,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
