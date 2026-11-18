package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.DebtDirection
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.DebtStatus
import com.example.data.local.entity.InstallmentEntity
import com.example.domain.ArabicDateFormatter
import com.example.domain.CurrencyOption
import com.example.domain.MoneyMath
import com.example.ui.theme.LocalKmaFinancialColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateDebtSheet(
  accounts: List<AccountEntity>,
  currency: CurrencyOption,
  initialDirection: DebtDirection = DebtDirection.OWED_BY_ME,
  onDismiss: () -> Unit,
  onCreateDebt: (
    personName: String,
    phone: String,
    direction: DebtDirection,
    totalAmountMinor: Long,
    installmentsCount: Int,
    firstDueDateMillis: Long,
    note: String,
    linkedAccountId: Long?,
  ) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var direction by remember { mutableStateOf(initialDirection) }
  var personName by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf("") }
  var amountText by remember { mutableStateOf("") }
  var installmentsCountText by remember { mutableStateOf("1") }
  var firstDueDaysOffset by remember { mutableIntStateOf(30) }
  var note by remember { mutableStateOf("") }
  var affectAccountBalance by remember { mutableStateOf(false) }
  var selectedAccountId by remember {
    mutableLongStateOf(accounts.firstOrNull { it.isDefault }?.id ?: accounts.firstOrNull()?.id ?: 0L)
  }
  var errorText by remember { mutableStateOf<String?>(null) }

  val parsedTotalMinor = MoneyMath.parseToMinorUnits(amountText)
  val parsedCount = installmentsCountText.trim().toIntOrNull()?.coerceIn(1, 120) ?: 1

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp)
          .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(text = "تسجيل دين أو التزام بأقساط", style = MaterialTheme.typography.headlineSmall)

      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        DebtDirection.entries.forEachIndexed { index, dir ->
          SegmentedButton(
            selected = direction == dir,
            onClick = { direction = dir },
            shape = SegmentedButtonDefaults.itemShape(index = index, count = DebtDirection.entries.size),
            modifier = Modifier.testTag("debt_direction_${dir.name.lowercase()}"),
          ) {
            Text(dir.arabicLabel)
          }
        }
      }

      OutlinedTextField(
        value = personName,
        onValueChange = {
          personName = it
          errorText = null
        },
        label = { Text("اسم الشخص أو الجهة") },
        placeholder = { Text("مثال: شركة التمويل / أحمد محمود") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("debt_person_input"),
      )

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = amountText,
          onValueChange = {
            amountText = it
            errorText = null
          },
          label = { Text("إجمالي المبلغ (${currency.symbol})") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.weight(1.3f).testTag("debt_amount_input"),
        )
        OutlinedTextField(
          value = installmentsCountText,
          onValueChange = {
            installmentsCountText = it.filter { ch -> ch.isDigit() }.take(3)
            errorText = null
          },
          label = { Text("عدد الأقساط") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.weight(0.9f).testTag("debt_installments_input"),
        )
      }

      if (parsedTotalMinor != null && parsedTotalMinor > 0L) {
        val splits = remember(parsedTotalMinor, parsedCount) {
          MoneyMath.splitIntoInstallments(parsedTotalMinor, parsedCount)
        }
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text = "معاينة الجدولة الدقيقة ($parsedCount ${if (parsedCount == 1) "دفعة" else "أقساط شهرية"})",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
              text =
                if (parsedCount == 1) {
                  "قيمة السداد: ${MoneyMath.formatMinor(splits.first(), currency)}"
                } else {
                  "القسط الأول: ${MoneyMath.formatMinor(splits.first(), currency)} • باقي الأقساط: ${MoneyMath.formatMinor(splits.last(), currency)} (المجموع مطابق تماماً)"
                },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
          }
        }
      }

      Text(text = "تاريخ أول استحقاق", style = MaterialTheme.typography.titleSmall)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(7 to "بعد أسبوع", 15 to "بعد 15 يوم", 30 to "بعد شهر").forEach { (days, label) ->
          FilterChip(
            selected = firstDueDaysOffset == days,
            onClick = { firstDueDaysOffset = days },
            label = { Text(label) },
          )
        }
      }

      OutlinedTextField(
        value = phone,
        onValueChange = { phone = it },
        label = { Text("رقم الهاتف (اختياري)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        modifier = Modifier.fillMaxWidth(),
      )

      OutlinedTextField(
        value = note,
        onValueChange = { note = it },
        label = { Text("ملاحظات (اختياري)") },
        maxLines = 2,
        modifier = Modifier.fillMaxWidth(),
      )

      if (accounts.isNotEmpty()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Checkbox(
            checked = affectAccountBalance,
            onCheckedChange = { affectAccountBalance = it },
          )
          Text(
            text =
              if (direction == DebtDirection.OWED_BY_ME) {
                "إيداع المبلغ المُقترض في أحد حساباتي الآن"
              } else {
                "خصم المبلغ المُقرض من أحد حساباتي الآن"
              },
            style = MaterialTheme.typography.bodyMedium,
          )
        }
        if (affectAccountBalance) {
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            accounts.forEach { acc ->
              FilterChip(
                selected = acc.id == selectedAccountId,
                onClick = { selectedAccountId = acc.id },
                label = { Text("${acc.name} (${MoneyMath.formatMinor(acc.balanceMinor, currency)})") },
              )
            }
          }
        }
      }

      errorText?.let { err ->
        Text(text = err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
      }

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
          onClick = onDismiss,
          modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
        ) {
          Text("إلغاء")
        }
        Button(
          onClick = {
            if (personName.trim().isEmpty()) {
              errorText = "يرجى إدخال اسم الشخص أو الجهة."
              return@Button
            }
            val total = MoneyMath.parseToMinorUnits(amountText)
            if (total == null || total <= 0L) {
              errorText = "يرجى إدخال مبلغ صحيح أكبر من صفر."
              return@Button
            }
            val count = installmentsCountText.trim().toIntOrNull()
            if (count == null || count < 1 || count > 120) {
              errorText = "عدد الأقساط يجب أن يكون بين 1 و 120."
              return@Button
            }
            val firstDue = ArabicDateFormatter.addDays(System.currentTimeMillis(), firstDueDaysOffset)
            onCreateDebt(
              personName.trim(),
              phone.trim(),
              direction,
              total,
              count,
              firstDue,
              note.trim(),
              if (affectAccountBalance && selectedAccountId > 0L) selectedAccountId else null,
            )
            onDismiss()
          },
          modifier = Modifier.weight(1.3f).minimumInteractiveComponentSize().testTag("save_debt_button"),
        ) {
          Text("حفظ السجل")
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtDetailAndPaymentSheet(
  debt: DebtEntity,
  installments: List<InstallmentEntity>,
  accounts: List<AccountEntity>,
  currency: CurrencyOption,
  onDismiss: () -> Unit,
  onRecordPayment: (debtId: Long, paymentMinor: Long, accountId: Long, note: String, allowOverdraft: Boolean) -> Unit,
  onDeleteDebt: (Long) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val financialColors = LocalKmaFinancialColors.current

  val nextUnpaid = installments.firstOrNull { !it.isPaid && it.remainingMinor > 0L }
  var paymentText by remember {
    mutableStateOf(
      MoneyMath.formatMinorPlainInput(nextUnpaid?.remainingMinor ?: debt.remainingMinor)
    )
  }
  var selectedAccountId by remember {
    mutableLongStateOf(accounts.firstOrNull { it.isDefault }?.id ?: accounts.firstOrNull()?.id ?: 0L)
  }
  var note by remember { mutableStateOf("") }
  var allowOverdraft by remember { mutableStateOf(false) }
  var errorText by remember { mutableStateOf<String?>(null) }
  var showDeleteConfirm by remember { mutableStateOf(false) }

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp)
          .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(text = debt.personName, style = MaterialTheme.typography.headlineSmall)
          Text(
            text = debt.direction.arabicLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        TextButton(
          onClick = { showDeleteConfirm = true },
          colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
          Icon(Icons.Filled.Delete, contentDescription = "حذف الدين", modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("حذف")
        }
      }

      val progress = MoneyMath.ratio(debt.paidAmountMinor, debt.totalAmountMinor)
      Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("المدفوع: ${MoneyMath.formatMinor(debt.paidAmountMinor, currency)}", style = MaterialTheme.typography.bodyMedium)
            Text(
              "المتبقي: ${MoneyMath.formatMinor(debt.remainingMinor, currency)}",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
            )
          }
          LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }

      if (debt.status != DebtStatus.PAID && debt.remainingMinor > 0L) {
        Text(
          text = if (debt.direction == DebtDirection.OWED_BY_ME) "تسجيل سداد دفعة / قسط" else "تسجيل تحصيل دفعة",
          style = MaterialTheme.typography.titleMedium,
        )

        if (accounts.isEmpty()) {
          Text(
            text = "يرجى إضافة حساب مالي أولاً لربط حركة السداد أو التحصيل به.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
          )
        } else {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (nextUnpaid != null && nextUnpaid.remainingMinor < debt.remainingMinor) {
              OutlinedButton(
                onClick = { paymentText = MoneyMath.formatMinorPlainInput(nextUnpaid.remainingMinor) },
              ) {
                Text("القسط التالي")
              }
            }
            OutlinedButton(
              onClick = { paymentText = MoneyMath.formatMinorPlainInput(debt.remainingMinor) },
            ) {
              Text("سداد كامل المتبقي")
            }
          }

          OutlinedTextField(
            value = paymentText,
            onValueChange = {
              paymentText = it
              errorText = null
            },
            label = { Text("مبلغ الدفعة (${currency.symbol})") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth().testTag("debt_payment_amount_input"),
          )

          Text(text = "الحساب المرتبط بالعملية", style = MaterialTheme.typography.titleSmall)
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            accounts.forEach { acc ->
              FilterChip(
                selected = acc.id == selectedAccountId,
                onClick = { selectedAccountId = acc.id },
                label = { Text("${acc.name} (${MoneyMath.formatMinor(acc.balanceMinor, currency)})") },
              )
            }
          }

          OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("ملاحظة الدفعة (اختياري)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )

          if (debt.direction == DebtDirection.OWED_BY_ME) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Checkbox(checked = allowOverdraft, onCheckedChange = { allowOverdraft = it })
              Text("السماح بالسداد حتى لو تجاوز الرصيد المتاح", style = MaterialTheme.typography.bodySmall)
            }
          }

          errorText?.let { err ->
            Text(text = err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
          }

          Button(
            onClick = {
              val payMinor = MoneyMath.parseToMinorUnits(paymentText)
              if (payMinor == null || payMinor <= 0L) {
                errorText = "يرجى إدخال مبلغ صحيح أكبر من صفر."
                return@Button
              }
              if (payMinor > debt.remainingMinor) {
                errorText = "المبلغ المدخل يتجاوز المتبقي على هذا الدين."
                return@Button
              }
              onRecordPayment(debt.id, payMinor, selectedAccountId, note.trim(), allowOverdraft)
              onDismiss()
            },
            modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("confirm_debt_payment_button"),
          ) {
            Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (debt.direction == DebtDirection.OWED_BY_ME) "تأكيد السداد" else "تأكيد التحصيل")
          }
        }
      }

      HorizontalDivider()
      Text(text = "جدول الأقساط (${installments.size})", style = MaterialTheme.typography.titleMedium)

      installments.forEach { inst ->
        val isOverdue = !inst.isPaid && inst.dueDate < System.currentTimeMillis()
        Surface(
          shape = MaterialTheme.shapes.small,
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = if (inst.isPaid) Icons.Filled.CheckCircle else Icons.Filled.Schedule,
              contentDescription = null,
              tint =
                when {
                  inst.isPaid -> financialColors.income
                  isOverdue -> financialColors.expense
                  else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Column(modifier = Modifier.weight(1f)) {
              Text("القسط رقم ${inst.installmentNumber}", style = MaterialTheme.typography.titleSmall)
              Text(
                text =
                  if (inst.isPaid && inst.paidAt != null) {
                    "تم السداد في ${ArabicDateFormatter.formatShortDate(inst.paidAt)}"
                  } else {
                    "الاستحقاق: ${ArabicDateFormatter.formatShortDate(inst.dueDate)}${if (isOverdue) " (متأخر)" else ""}"
                  },
                style = MaterialTheme.typography.bodySmall,
                color = if (isOverdue) financialColors.expense else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = MoneyMath.formatMinor(inst.amountMinor, currency),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
              )
              if (!inst.isPaid && inst.paidAmountMinor > 0L) {
                Text(
                  text = "متبقي ${MoneyMath.formatMinor(inst.remainingMinor, currency)}",
                  style = MaterialTheme.typography.labelSmall,
                  color = financialColors.debtWarning,
                )
              }
            }
          }
        }
      }
    }
  }

  if (showDeleteConfirm) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirm = false },
      title = { Text("حذف سجل الدين؟") },
      text = { Text("سيتم حذف سجل الدين وجدول أقساطه نهائياً.") },
      confirmButton = {
        Button(
          onClick = {
            onDeleteDebt(debt.id)
            showDeleteConfirm = false
            onDismiss()
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) {
          Text("حذف")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteConfirm = false }) { Text("إلغاء") }
      },
    )
  }
}
