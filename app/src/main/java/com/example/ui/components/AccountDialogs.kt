package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AccountType
import com.example.domain.CurrencyOption
import com.example.domain.MoneyMath

private val ACCOUNT_COLOR_PRESETS = listOf(
  0xFF0B4F3BL,
  0xFF1565C0L,
  0xFF6A1B9AL,
  0xFFB8860BL,
  0xFF00695CL,
  0xFFC62828L,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AccountEditorSheet(
  currency: CurrencyOption,
  existingAccount: AccountEntity? = null,
  onDismiss: () -> Unit,
  onCreate: (name: String, type: AccountType, initialBalanceMinor: Long, colorHex: Long, isDefault: Boolean) -> Unit,
  onUpdate: (account: AccountEntity, name: String, type: AccountType, colorHex: Long) -> Unit,
  onDelete: ((Long) -> Unit)? = null,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var name by remember { mutableStateOf(existingAccount?.name ?: "") }
  var selectedType by remember { mutableStateOf(existingAccount?.type ?: AccountType.CASH) }
  var balanceText by remember {
    mutableStateOf(
      existingAccount?.let { MoneyMath.formatMinorPlainInput(it.balanceMinor) } ?: "0.00"
    )
  }
  var selectedColor by remember {
    mutableLongStateOf(existingAccount?.colorHex ?: ACCOUNT_COLOR_PRESETS.first())
  }
  var setAsDefault by remember { mutableStateOf(existingAccount?.isDefault ?: false) }
  var errorText by remember { mutableStateOf<String?>(null) }
  var showDeleteConfirm by remember { mutableStateOf(false) }

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
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
          text = if (existingAccount == null) "إضافة حساب أو محفظة" else "تعديل الحساب",
          style = MaterialTheme.typography.headlineSmall,
        )
        if (existingAccount != null && onDelete != null) {
          TextButton(
            onClick = { showDeleteConfirm = true },
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.testTag("delete_account_button"),
          ) {
            Icon(Icons.Filled.Delete, contentDescription = "حذف الحساب", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("حذف")
          }
        }
      }

      OutlinedTextField(
        value = name,
        onValueChange = {
          name = it
          errorText = null
        },
        label = { Text("اسم الحساب") },
        placeholder = { Text("مثال: محفظة الكاش / البنك الأهلي / فودافون كاش") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("account_name_input"),
      )

      Text(text = "نوع الحساب", style = MaterialTheme.typography.titleSmall)
      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        AccountType.entries.forEach { accType ->
          FilterChip(
            selected = selectedType == accType,
            onClick = { selectedType = accType },
            label = { Text(accType.arabicLabel) },
            leadingIcon = {
              Icon(accountIconFor(accType), contentDescription = null, modifier = Modifier.size(16.dp))
            },
            modifier = Modifier.testTag("account_type_${accType.name.lowercase()}"),
          )
        }
      }

      if (existingAccount == null) {
        OutlinedTextField(
          value = balanceText,
          onValueChange = {
            balanceText = it
            errorText = null
          },
          label = { Text("الرصيد الافتتاحي الحالي (${currency.symbol})") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.fillMaxWidth().testTag("account_initial_balance_input"),
        )
      }

      Text(text = "لون التمييز", style = MaterialTheme.typography.titleSmall)
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ACCOUNT_COLOR_PRESETS.forEach { colorHex ->
          val isSelected = selectedColor == colorHex
          Box(
            modifier =
              Modifier.size(40.dp)
                .clip(CircleShape)
                .background(Color(colorHex))
                .then(
                  if (isSelected) Modifier.border(BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface), CircleShape)
                  else Modifier
                )
                .clickable { selectedColor = colorHex },
            contentAlignment = Alignment.Center,
          ) {
            if (isSelected) {
              Icon(Icons.Filled.Check, contentDescription = "محدد", tint = Color.White, modifier = Modifier.size(18.dp))
            }
          }
        }
      }

      if (existingAccount == null) {
        Row(
          modifier = Modifier.fillMaxWidth().clickable { setAsDefault = !setAsDefault },
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Checkbox(checked = setAsDefault, onCheckedChange = { setAsDefault = it })
          Text("تعيين كحساب افتراضي للمعاملات السريعة", style = MaterialTheme.typography.bodyMedium)
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
            if (name.trim().isEmpty()) {
              errorText = "يرجى إدخال اسم الحساب."
              return@Button
            }
            if (existingAccount == null) {
              val initialMinor = MoneyMath.parseToMinorUnits(balanceText, allowZero = true)
              if (initialMinor == null) {
                errorText = "يرجى إدخال رصيد افتتاحي صحيح (0 أو أكثر)."
                return@Button
              }
              onCreate(name.trim(), selectedType, initialMinor, selectedColor, setAsDefault)
            } else {
              onUpdate(existingAccount, name.trim(), selectedType, selectedColor)
            }
            onDismiss()
          },
          modifier = Modifier.weight(1.3f).minimumInteractiveComponentSize().testTag("save_account_button"),
        ) {
          Text(if (existingAccount == null) "حفظ الحساب" else "تحديث البيانات")
        }
      }
    }
  }

  if (showDeleteConfirm && existingAccount != null && onDelete != null) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirm = false },
      title = { Text("حذف حساب «${existingAccount.name}»؟") },
      text = { Text("سيؤدي حذف الحساب إلى حذف جميع المعاملات المرتبطة به نهائياً من قاعدة البيانات.") },
      confirmButton = {
        Button(
          onClick = {
            onDelete(existingAccount.id)
            showDeleteConfirm = false
            onDismiss()
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          modifier = Modifier.testTag("confirm_delete_account_button"),
        ) {
          Text("حذف الحساب")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteConfirm = false }) { Text("إلغاء") }
      },
    )
  }
}
