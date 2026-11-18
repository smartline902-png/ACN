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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.DebtDirection
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.DebtStatus
import com.example.domain.ArabicDateFormatter
import com.example.domain.MoneyMath
import com.example.ui.components.EmptyStatePanel
import com.example.ui.theme.LocalKmaFinancialColors
import com.example.ui.viewmodel.AccountantUiState

private enum class DebtTabFilter(val label: String) {
  ALL("الكل"),
  OWED_BY_ME("ديون عليّ"),
  OWED_TO_ME("ديون لي"),
}

@Composable
fun DebtsScreen(
  state: AccountantUiState,
  onCreateDebt: (DebtDirection) -> Unit,
  onOpenDebt: (DebtEntity) -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedTab by remember { mutableStateOf(DebtTabFilter.ALL) }
  var showPaidOnly by remember { mutableStateOf(false) }

  val currency = state.preferences.currency
  val masked = state.preferences.hideBalances
  val financialColors = LocalKmaFinancialColors.current

  val installmentsByDebt =
    remember(state.installments) { state.installments.groupBy { it.debtId } }

  val filteredDebts =
    remember(state.debts, selectedTab, showPaidOnly) {
      state.debts.filter { debt ->
        val matchesTab =
          when (selectedTab) {
            DebtTabFilter.ALL -> true
            DebtTabFilter.OWED_BY_ME -> debt.direction == DebtDirection.OWED_BY_ME
            DebtTabFilter.OWED_TO_ME -> debt.direction == DebtDirection.OWED_TO_ME
          }
        val matchesStatus =
          if (showPaidOnly) debt.status == DebtStatus.PAID else debt.status != DebtStatus.PAID
        matchesTab && matchesStatus
      }
    }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(text = "الديون والأقساط", style = MaterialTheme.typography.headlineMedium)
        Button(
          onClick = {
            val defaultDir =
              if (selectedTab == DebtTabFilter.OWED_TO_ME) DebtDirection.OWED_TO_ME
              else DebtDirection.OWED_BY_ME
            onCreateDebt(defaultDir)
          },
          modifier = Modifier.minimumInteractiveComponentSize().testTag("add_debt_button"),
        ) {
          Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("إضافة دين")
        }
      }
    }

    item {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
          modifier = Modifier.weight(1f),
          shape = MaterialTheme.shapes.large,
          color = financialColors.expenseContainer,
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("إجمالي الديون عليّ", style = MaterialTheme.typography.labelMedium, color = financialColors.expense)
            Text(
              text = MoneyMath.formatMinor(state.totalPayablesMinor, currency, masked = masked),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = financialColors.expense,
            )
          }
        }

        Surface(
          modifier = Modifier.weight(1f),
          shape = MaterialTheme.shapes.large,
          color = financialColors.incomeContainer,
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("إجمالي المستحقات لي", style = MaterialTheme.typography.labelMedium, color = financialColors.income)
            Text(
              text = MoneyMath.formatMinor(state.totalReceivablesMinor, currency, masked = masked),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = financialColors.income,
            )
          }
        }
      }
    }

    item {
      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        DebtTabFilter.entries.forEachIndexed { index, tab ->
          SegmentedButton(
            selected = selectedTab == tab,
            onClick = { selectedTab = tab },
            shape = SegmentedButtonDefaults.itemShape(index = index, count = DebtTabFilter.entries.size),
            modifier = Modifier.testTag("debt_tab_${tab.name.lowercase()}"),
          ) {
            Text(tab.label)
          }
        }
      }
    }

    item {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
          selected = !showPaidOnly,
          onClick = { showPaidOnly = false },
          label = { Text("الديون النشطة") },
        )
        FilterChip(
          selected = showPaidOnly,
          onClick = { showPaidOnly = true },
          label = { Text("المسددة بالكامل") },
        )
      }
    }

    if (filteredDebts.isEmpty()) {
      item {
        EmptyStatePanel(
          icon = Icons.Filled.Handshake,
          title = if (showPaidOnly) "لا توجد ديون مسددة في هذا القسم" else "لا توجد ديون أو أقساط نشطة",
          description = "يمكنك تسجيل الديون التي عليك أو التي لك مع جدولة الأقساط الشهرية وربط السداد بحساباتك.",
          actionLabel = if (!showPaidOnly) "تسجيل دين أو قسط" else null,
          onAction =
            if (!showPaidOnly) {
              {
                val defaultDir =
                  if (selectedTab == DebtTabFilter.OWED_TO_ME) DebtDirection.OWED_TO_ME
                  else DebtDirection.OWED_BY_ME
                onCreateDebt(defaultDir)
              }
            } else null,
          actionTestTag = "debts_empty_add_button",
        )
      }
    } else {
      items(filteredDebts, key = { it.id }) { debt ->
        val debtInstallments = installmentsByDebt[debt.id].orEmpty()
        val paidCount = debtInstallments.count { it.isPaid }
        val nextDue = debtInstallments.firstOrNull { !it.isPaid }
        val progress = MoneyMath.ratio(debt.paidAmountMinor, debt.totalAmountMinor)
        val isOwedByMe = debt.direction == DebtDirection.OWED_BY_ME
        val accent = if (isOwedByMe) financialColors.expense else financialColors.income

        Surface(
          modifier =
            Modifier.fillMaxWidth()
              .clickable { onOpenDebt(debt) }
              .testTag("debt_card_${debt.id}"),
          shape = MaterialTheme.shapes.large,
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = debt.personName,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = debt.direction.arabicLabel,
                  style = MaterialTheme.typography.labelMedium,
                  color = accent,
                )
              }

              if (debt.status == DebtStatus.PAID) {
                Surface(
                  shape = MaterialTheme.shapes.small,
                  color = financialColors.incomeContainer,
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = financialColors.income, modifier = Modifier.size(14.dp))
                    Text("مسدد", style = MaterialTheme.typography.labelSmall, color = financialColors.income)
                  }
                }
              } else {
                Column(horizontalAlignment = Alignment.End) {
                  Text("المتبقي", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Text(
                    text = MoneyMath.formatMinor(debt.remainingMinor, currency, masked = masked),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                  )
                }
              }
            }

            LinearProgressIndicator(
              progress = { progress },
              color = accent,
              modifier = Modifier.fillMaxWidth(),
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = "الأقساط: $paidCount من ${debt.installmentsCount}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text =
                  if (nextDue != null) {
                    "الاستحقاق التالي: ${ArabicDateFormatter.formatShortDate(nextDue.dueDate)}"
                  } else {
                    "الإجمالي: ${MoneyMath.formatMinor(debt.totalAmountMinor, currency, masked = masked)}"
                  },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(72.dp)) }
  }
}
