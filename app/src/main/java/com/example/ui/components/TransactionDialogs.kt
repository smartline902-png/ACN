package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AccountType
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import com.example.domain.ArabicDateFormatter
import com.example.domain.CurrencyOption
import com.example.domain.MoneyMath

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionEditorSheet(
  accounts: List<AccountEntity>,
  categories: List<CategoryEntity>,
  currency: CurrencyOption,
  initialType: TransactionType = TransactionType.EXPENSE,
  existingTransaction: TransactionEntity? = null,
  onDismiss: () -> Unit,
  onCreateAccountRequested: () -> Unit,
  onSaveNew: (
    type: TransactionType,
    amountMinor: Long,
    accountId: Long,
    toAccountId: Long?,
    categoryId: Long?,
    title: String,
    note: String,
    timestamp: Long,
    allowOverdraft: Boolean,
  ) -> Unit,
  onUpdateExisting: (
    existing: TransactionEntity,
    type: TransactionType,
    amountMinor: Long,
    accountId: Long,
    toAccountId: Long?,
    categoryId: Long?,
    title: String,
    note: String,
    timestamp: Long,
  ) -> Unit,
  onDeleteExisting: ((Long) -> Unit)? = null,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  var selectedType by remember { mutableStateOf(existingTransaction?.type ?: initialType) }
  var amountText by remember {
    mutableStateOf(
      existingTransaction?.let { MoneyMath.formatMinorPlainInput(it.amountMinor) } ?: ""
    )
  }
  var title by remember { mutableStateOf(existingTransaction?.title ?: "") }
  var note by remember { mutableStateOf(existingTransaction?.note ?: "") }
  var selectedAccountId by remember {
    mutableLongStateOf(
      existingTransaction?.accountId
        ?: accounts.firstOrNull { it.isDefault }?.id
        ?: accounts.firstOrNull()?.id
        ?: 0L
    )
  }
  var selectedToAccountId by remember {
    mutableLongStateOf(
      existingTransaction?.toAccountId
        ?: accounts.firstOrNull { it.id != selectedAccountId }?.id
        ?: 0L
    )
  }
  val filteredCategories =
    remember(categories, selectedType) {
      categories.filter { it.type == selectedType }
    }
  var selectedCategoryId by remember(selectedType) {
    mutableLongStateOf(
      if (existingTransaction != null && existingTransaction.type == selectedType) {
        existingTransaction.categoryId ?: filteredCategories.firstOrNull()?.id ?: 0L
      } else {
        filteredCategories.firstOrNull()?.id ?: 0L
      }
    )
  }
  var dayOffset by remember { mutableIntStateOf(0) } // 0 = today, -1 = yesterday, -2 = 2 days ago
  var allowOverdraft by remember { mutableStateOf(false) }
  var validationError by remember { mutableStateOf<String?>(null) }
  var showDeleteConfirm by remember { mutableStateOf(false) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp)
          .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = if (existingTransaction == null) "تسجيل معاملة جديدة" else "تعديل المعاملة",
          style = MaterialTheme.typography.headlineSmall,
        )
        if (existingTransaction != null && onDeleteExisting != null) {
          TextButton(
            onClick = { showDeleteConfirm = true },
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.testTag("delete_transaction_button"),
          ) {
            Icon(Icons.Filled.Delete, contentDescription = "حذف المعاملة", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("حذف")
          }
        }
      }

      if (accounts.isEmpty()) {
        EmptyStatePanel(
          icon = Icons.Filled.AccountBalanceWallet,
          title = "أضف حساباً مالياً أولاً",
          description = "لتسجيل المصروفات أو الدخل بدقة، يجب إنشاء حساب أو محفظة واحدة على الأقل.",
          actionLabel = "إنشاء حساب جديد",
          onAction = {
            onDismiss()
            onCreateAccountRequested()
          },
          actionTestTag = "create_account_from_tx_button",
        )
        return@Column
      }

      val availableTypes =
        if (accounts.size >= 2) TransactionType.entries
        else listOf(TransactionType.EXPENSE, TransactionType.INCOME)

      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        availableTypes.forEachIndexed { index, txType ->
          SegmentedButton(
            selected = selectedType == txType,
            onClick = {
              selectedType = txType
              validationError = null
            },
            shape = SegmentedButtonDefaults.itemShape(index = index, count = availableTypes.size),
            modifier = Modifier.testTag("tx_type_${txType.name.lowercase()}"),
          ) {
            Text(txType.arabicLabel)
          }
        }
      }

      OutlinedTextField(
        value = amountText,
        onValueChange = {
          amountText = it
          validationError = null
        },
        label = { Text("المبلغ (${currency.symbol})") },
        placeholder = { Text("0.00") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth().testTag("tx_amount_input"),
      )

      OutlinedTextField(
        value = title,
        onValueChange = {
          title = it
          validationError = null
        },
        label = { Text("الوصف / العنوان") },
        placeholder = {
          Text(
            when (selectedType) {
              TransactionType.EXPENSE -> "مثال: مشتريات السوبرماركت"
              TransactionType.INCOME -> "مثال: راتب شهر نوفمبر"
              TransactionType.TRANSFER -> "مثال: تغذية المحفظة من البنك"
            }
          )
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("tx_title_input"),
      )

      // Source Account
      Text(
        text = if (selectedType == TransactionType.TRANSFER) "من حساب" else "الحساب",
        style = MaterialTheme.typography.titleSmall,
      )
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        accounts.forEach { acc ->
          val selected = acc.id == selectedAccountId
          FilterChip(
            selected = selected,
            onClick = {
              selectedAccountId = acc.id
              if (selectedToAccountId == acc.id) {
                selectedToAccountId = accounts.firstOrNull { it.id != acc.id }?.id ?: 0L
              }
              validationError = null
            },
            label = {
              Text("${acc.name} (${MoneyMath.formatMinor(acc.balanceMinor, currency)})")
            },
            leadingIcon = {
              Icon(accountIconFor(acc.type), contentDescription = null, modifier = Modifier.size(18.dp))
            },
            modifier = Modifier.testTag("tx_account_chip_${acc.id}"),
          )
        }
      }

      if (selectedType == TransactionType.TRANSFER) {
        Text(text = "إلى حساب", style = MaterialTheme.typography.titleSmall)
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          accounts.filter { it.id != selectedAccountId }.forEach { acc ->
            FilterChip(
              selected = acc.id == selectedToAccountId,
              onClick = {
                selectedToAccountId = acc.id
                validationError = null
              },
              label = { Text(acc.name) },
              leadingIcon = {
                Icon(accountIconFor(acc.type), contentDescription = null, modifier = Modifier.size(18.dp))
              },
              modifier = Modifier.testTag("tx_to_account_chip_${acc.id}"),
            )
          }
        }
      } else {
        Text(text = "التصنيف", style = MaterialTheme.typography.titleSmall)
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          filteredCategories.forEach { cat ->
            FilterChip(
              selected = cat.id == selectedCategoryId,
              onClick = { selectedCategoryId = cat.id },
              label = { Text(cat.name) },
              leadingIcon = {
                Icon(categoryIconFor(cat.iconKey), contentDescription = null, modifier = Modifier.size(16.dp))
              },
              modifier = Modifier.testTag("tx_category_chip_${cat.id}"),
            )
          }
        }
      }

      if (existingTransaction == null) {
        Text(text = "التاريخ", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf(0 to "اليوم", -1 to "أمس", -2 to "قبل يومين").forEach { (offset, label) ->
            FilterChip(
              selected = dayOffset == offset,
              onClick = { dayOffset = offset },
              label = { Text(label) },
            )
          }
        }
      }

      OutlinedTextField(
        value = note,
        onValueChange = { note = it },
        label = { Text("ملاحظات إضافية (اختياري)") },
        maxLines = 2,
        modifier = Modifier.fillMaxWidth().testTag("tx_note_input"),
      )

      val selectedAccount = accounts.firstOrNull { it.id == selectedAccountId }
      val parsedMinor = MoneyMath.parseToMinorUnits(amountText)
      if (existingTransaction == null &&
        (selectedType == TransactionType.EXPENSE || selectedType == TransactionType.TRANSFER) &&
        selectedAccount != null &&
        selectedAccount.type != AccountType.CREDIT_CARD &&
        parsedMinor != null &&
        parsedMinor > selectedAccount.balanceMinor
      ) {
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.errorContainer,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Checkbox(
              checked = allowOverdraft,
              onCheckedChange = { allowOverdraft = it },
              modifier = Modifier.testTag("tx_allow_overdraft_checkbox"),
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "المبلغ يتجاوز الرصيد المتاح في «${selectedAccount.name}»",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onErrorContainer,
              )
              Text(
                text = "فعّل هذا الخيار فقط إذا أردت تسجيل العملية على المكشوف.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
              )
            }
          }
        }
      }

      validationError?.let { err ->
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            Icons.Filled.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = err,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedButton(
          onClick = onDismiss,
          modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
        ) {
          Text("إلغاء")
        }
        Button(
          onClick = {
            val amount = MoneyMath.parseToMinorUnits(amountText)
            if (amount == null || amount <= 0L) {
              validationError = "يرجى إدخال مبلغ صحيح أكبر من صفر."
              return@Button
            }
            if (title.trim().isEmpty()) {
              validationError = "يرجى كتابة عنوان أو وصف للمعاملة."
              return@Button
            }
            if (selectedType == TransactionType.TRANSFER &&
              (selectedToAccountId == 0L || selectedToAccountId == selectedAccountId)
            ) {
              validationError = "يرجى اختيار الحساب المستلم للتحويل."
              return@Button
            }
            if (existingTransaction == null) {
              val timestamp =
                if (dayOffset == 0) System.currentTimeMillis()
                else ArabicDateFormatter.addDays(System.currentTimeMillis(), dayOffset)
              onSaveNew(
                selectedType,
                amount,
                selectedAccountId,
                if (selectedType == TransactionType.TRANSFER) selectedToAccountId else null,
                if (selectedType == TransactionType.TRANSFER) null else selectedCategoryId.takeIf { it > 0L },
                title.trim(),
                note.trim(),
                timestamp,
                allowOverdraft,
              )
            } else {
              onUpdateExisting(
                existingTransaction,
                selectedType,
                amount,
                selectedAccountId,
                if (selectedType == TransactionType.TRANSFER) selectedToAccountId else null,
                if (selectedType == TransactionType.TRANSFER) null else selectedCategoryId.takeIf { it > 0L },
                title.trim(),
                note.trim(),
                existingTransaction.timestamp,
              )
            }
            onDismiss()
          },
          modifier = Modifier.weight(1.4f).minimumInteractiveComponentSize().testTag("save_transaction_button"),
        ) {
          Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(if (existingTransaction == null) "حفظ المعاملة" else "تحديث المعاملة")
        }
      }
    }
  }

  if (showDeleteConfirm && existingTransaction != null && onDeleteExisting != null) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirm = false },
      title = { Text("تأكيد حذف المعاملة") },
      text = { Text("سيتم حذف هذه المعاملة وعكس تأثيرها المالي على رصيد الحساب فوراً.") },
      confirmButton = {
        Button(
          onClick = {
            onDeleteExisting(existingTransaction.id)
            showDeleteConfirm = false
            onDismiss()
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          modifier = Modifier.testTag("confirm_delete_tx_button"),
        ) {
          Text("حذف نهائياً")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteConfirm = false }) { Text("تراجع") }
      },
    )
  }
}
