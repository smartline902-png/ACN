package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import com.example.domain.ArabicDateFormatter
import com.example.domain.MoneyMath
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.TransactionRowItem
import com.example.ui.theme.LocalKmaFinancialColors
import com.example.ui.viewmodel.AccountantUiState

private enum class TransactionFilter(val label: String) {
  ALL("الكل"),
  EXPENSE("المصروفات"),
  INCOME("الدخل"),
  TRANSFER("التحويلات"),
  THIS_MONTH("هذا الشهر"),
}

@Composable
fun TransactionsScreen(
  state: AccountantUiState,
  onAddTransaction: () -> Unit,
  onOpenTransaction: (TransactionEntity) -> Unit,
  modifier: Modifier = Modifier,
) {
  var searchQuery by remember { mutableStateOf("") }
  var selectedFilter by remember { mutableStateOf(TransactionFilter.ALL) }

  val currency = state.preferences.currency
  val masked = state.preferences.hideBalances
  val financialColors = LocalKmaFinancialColors.current
  val accountMap = remember(state.accounts) { state.accounts.associateBy { it.id } }
  val categoryMap = remember(state.categories) { state.categories.associateBy { it.id } }

  val monthStart = remember { ArabicDateFormatter.startOfCurrentMonthMillis() }
  val monthEnd = remember { ArabicDateFormatter.endOfCurrentMonthMillis() }

  val filteredTransactions =
    remember(state.transactions, searchQuery, selectedFilter, categoryMap) {
      state.transactions.filter { tx ->
        val matchesType =
          when (selectedFilter) {
            TransactionFilter.ALL -> true
            TransactionFilter.EXPENSE -> tx.type == TransactionType.EXPENSE
            TransactionFilter.INCOME -> tx.type == TransactionType.INCOME
            TransactionFilter.TRANSFER -> tx.type == TransactionType.TRANSFER
            TransactionFilter.THIS_MONTH -> tx.timestamp in monthStart..monthEnd
          }
        val query = searchQuery.trim()
        val catName = tx.categoryId?.let { categoryMap[it]?.name }.orEmpty()
        val matchesSearch =
          query.isEmpty() ||
            tx.title.contains(query, ignoreCase = true) ||
            tx.note.contains(query, ignoreCase = true) ||
            catName.contains(query, ignoreCase = true)
        matchesType && matchesSearch
      }
    }

  val filteredIncome =
    filteredTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amountMinor }
  val filteredExpense =
    filteredTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor }

  Column(modifier = modifier.fillMaxSize()) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = "سجل المعاملات المالية",
        style = MaterialTheme.typography.headlineMedium,
      )

      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("ابحث بالوصف، التصنيف، أو الملاحظة...") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "بحث") },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { searchQuery = "" }) {
              Icon(Icons.Filled.Close, contentDescription = "مسح البحث")
            }
          }
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("transactions_search_input"),
      )

      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        TransactionFilter.entries.forEach { filter ->
          FilterChip(
            selected = selectedFilter == filter,
            onClick = { selectedFilter = filter },
            label = { Text(filter.label) },
            modifier = Modifier.testTag("tx_filter_${filter.name.lowercase()}"),
          )
        }
      }

      if (filteredTransactions.isNotEmpty()) {
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = "العدد: ${filteredTransactions.size}",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = "دخل: ${MoneyMath.formatMinor(filteredIncome, currency, masked = masked)}",
              style = MaterialTheme.typography.labelLarge,
              color = financialColors.income,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "مصروف: ${MoneyMath.formatMinor(filteredExpense, currency, masked = masked)}",
              style = MaterialTheme.typography.labelLarge,
              color = financialColors.expense,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      if (filteredTransactions.isEmpty()) {
        item {
          EmptyStatePanel(
            icon = Icons.Filled.ReceiptLong,
            title = if (state.transactions.isEmpty()) "لا توجد معاملات مسجلة" else "لا توجد نتائج مطابقة",
            description =
              if (state.transactions.isEmpty()) {
                "ابدأ بتسجيل مصروفاتك أو دخلك لتتبع كل حركة مالية بدقة."
              } else {
                "جرّب تغيير فلتر البحث أو كلمة البحث."
              },
            actionLabel = if (state.transactions.isEmpty()) "تسجيل معاملة" else null,
            onAction = if (state.transactions.isEmpty()) onAddTransaction else null,
            actionTestTag = "transactions_empty_add_button",
          )
        }
      } else {
        items(filteredTransactions, key = { it.id }) { tx ->
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
}
