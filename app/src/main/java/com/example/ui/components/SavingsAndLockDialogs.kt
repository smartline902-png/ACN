package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.domain.CurrencyOption
import com.example.domain.MoneyMath

private val GOAL_COLORS = listOf(
  0xFF0B4F3BL,
  0xFFB8860BL,
  0xFF1565C0L,
  0xFF6A1B9AL,
  0xFF00796BL,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSavingsGoalSheet(
  currency: CurrencyOption,
  onDismiss: () -> Unit,
  onCreate: (name: String, targetMinor: Long, colorHex: Long) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var name by remember { mutableStateOf("") }
  var targetText by remember { mutableStateOf("") }
  var selectedColor by remember { mutableLongStateOf(GOAL_COLORS.first()) }
  var errorText by remember { mutableStateOf<String?>(null) }

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Text(text = "إنشاء هدف ادخاري جديد", style = MaterialTheme.typography.headlineSmall)

      OutlinedTextField(
        value = name,
        onValueChange = {
          name = it
          errorText = null
        },
        label = { Text("اسم الهدف") },
        placeholder = { Text("مثال: صندوق الطوارئ / شراء سيارة / عمرة") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("goal_name_input"),
      )

      OutlinedTextField(
        value = targetText,
        onValueChange = {
          targetText = it
          errorText = null
        },
        label = { Text("المبلغ المستهدف (${currency.symbol})") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth().testTag("goal_target_input"),
      )

      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        GOAL_COLORS.forEach { hex ->
          val selected = selectedColor == hex
          Box(
            modifier =
              Modifier.size(38.dp)
                .clip(CircleShape)
                .background(Color(hex))
                .then(
                  if (selected) Modifier.border(BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface), CircleShape)
                  else Modifier
                )
                .clickable { selectedColor = hex },
            contentAlignment = Alignment.Center,
          ) {
            if (selected) {
              Icon(Icons.Filled.Check, contentDescription = "محدد", tint = Color.White, modifier = Modifier.size(18.dp))
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
            if (name.trim().isEmpty()) {
              errorText = "يرجى إدخال اسم الهدف."
              return@Button
            }
            val targetMinor = MoneyMath.parseToMinorUnits(targetText)
            if (targetMinor == null || targetMinor <= 0L) {
              errorText = "يرجى إدخال مبلغ مستهدف صحيح أكبر من صفر."
              return@Button
            }
            onCreate(name.trim(), targetMinor, selectedColor)
            onDismiss()
          },
          modifier = Modifier.weight(1.3f).minimumInteractiveComponentSize().testTag("save_goal_button"),
        ) {
          Text("حفظ الهدف")
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContributeSavingsGoalSheet(
  goal: SavingsGoalEntity,
  accounts: List<AccountEntity>,
  currency: CurrencyOption,
  onDismiss: () -> Unit,
  onSubmit: (goalId: Long, accountId: Long, amountMinor: Long, isWithdrawal: Boolean) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var isWithdrawal by remember { mutableStateOf(false) }
  var amountText by remember { mutableStateOf("") }
  var selectedAccountId by remember {
    mutableLongStateOf(accounts.firstOrNull { it.isDefault }?.id ?: accounts.firstOrNull()?.id ?: 0L)
  }
  var errorText by remember { mutableStateOf<String?>(null) }

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Text(text = goal.name, style = MaterialTheme.typography.headlineSmall)
      Text(
        text = "المدخر حالياً: ${MoneyMath.formatMinor(goal.savedAmountMinor, currency)} من أصل ${MoneyMath.formatMinor(goal.targetAmountMinor, currency)}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
          selected = !isWithdrawal,
          onClick = { isWithdrawal = false },
          shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
        ) {
          Text("إيداع في الهدف")
        }
        SegmentedButton(
          selected = isWithdrawal,
          onClick = { isWithdrawal = true },
          shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
        ) {
          Text("سحب للحساب")
        }
      }

      if (accounts.isEmpty()) {
        Text(
          text = "يرجى إضافة حساب مالي أولاً لتنفيذ الإيداع أو السحب.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error,
        )
      } else {
        OutlinedTextField(
          value = amountText,
          onValueChange = {
            amountText = it
            errorText = null
          },
          label = { Text("المبلغ (${currency.symbol})") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.fillMaxWidth().testTag("goal_contribution_amount_input"),
        )

        Text(text = "الحساب المرتبط", style = MaterialTheme.typography.titleSmall)
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

        errorText?.let { err ->
          Text(text = err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }

        Button(
          onClick = {
            val minor = MoneyMath.parseToMinorUnits(amountText)
            if (minor == null || minor <= 0L) {
              errorText = "يرجى إدخال مبلغ صحيح أكبر من صفر."
              return@Button
            }
            onSubmit(goal.id, selectedAccountId, minor, isWithdrawal)
            onDismiss()
          },
          modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("confirm_goal_contribution_button"),
        ) {
          Text(if (isWithdrawal) "تأكيد السحب" else "تأكيد الإيداع")
        }
      }
    }
  }
}

@Composable
fun BiometricLockScreen(
  errorMessage: String?,
  onUnlockClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background,
  ) {
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(
            Brush.verticalGradient(
              colors =
                listOf(
                  MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                  MaterialTheme.colorScheme.background,
                )
            )
          )
          .padding(28.dp),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Box(
          modifier =
            Modifier.size(88.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = "الخزنة مقفلة",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(42.dp),
          )
        }

        Text(
          text = "محاسبك الشخصي - KMA",
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onBackground,
          textAlign = TextAlign.Center,
        )

        Text(
          text = "بياناتك المالية محمية عبر مفتاح التشفير في Android Keystore والتحقق البيومتري.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )

        if (!errorMessage.isNullOrBlank()) {
          Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.errorContainer,
          ) {
            Text(
              text = errorMessage,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
              textAlign = TextAlign.Center,
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
          onClick = onUnlockClick,
          modifier = Modifier.fillMaxWidth(0.75f).minimumInteractiveComponentSize().testTag("biometric_unlock_button"),
        ) {
          Icon(Icons.Filled.Fingerprint, contentDescription = null, modifier = Modifier.size(22.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("فتح بالبصمة / رمز الجهاز")
        }
      }
    }
  }
}
