package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.ThemeMode
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionType
import com.example.domain.ArabicDateFormatter
import com.example.domain.CurrencyOption
import com.example.domain.MoneyMath
import com.example.security.BiometricSupportStatus
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.categoryIconFor
import com.example.ui.theme.LocalKmaFinancialColors
import com.example.ui.viewmodel.AccountantUiState

@Composable
fun PlanningAndSecurityScreen(
  state: AccountantUiState,
  onCreateSavingsGoal: () -> Unit,
  onContributeGoal: (SavingsGoalEntity) -> Unit,
  onDeleteGoal: (Long) -> Unit,
  onUpdateCategoryBudget: (CategoryEntity, Long?) -> Unit,
  onToggleBiometricLock: (Boolean) -> Unit,
  onSelectCurrency: (CurrencyOption) -> Unit,
  onSelectThemeMode: (ThemeMode) -> Unit,
  onExportBackup: () -> Unit,
  onResetAllData: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val currency = state.preferences.currency
  val masked = state.preferences.hideBalances
  val financialColors = LocalKmaFinancialColors.current

  var budgetEditCategory by remember { mutableStateOf<CategoryEntity?>(null) }
  var showResetConfirm by remember { mutableStateOf(false) }

  val expenseCategories =
    remember(state.categories) { state.categories.filter { it.type == TransactionType.EXPENSE } }
  val spentByCategory =
    remember(state.monthlyCategoryBreakdown) {
      state.monthlyCategoryBreakdown.associate { it.category.id to it.spentMinor }
    }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      Text(
        text = "التخطيط، الادخار والأمان",
        style = MaterialTheme.typography.headlineMedium,
      )
    }

    // --- Section 1: Savings Goals ---
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(text = "الأهداف الادخارية", style = MaterialTheme.typography.titleLarge)
        Button(
          onClick = onCreateSavingsGoal,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("add_goal_button"),
        ) {
          Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("هدف جديد")
        }
      }
    }

    if (state.savingsGoals.isEmpty()) {
      item {
        EmptyStatePanel(
          icon = Icons.Filled.Savings,
          title = "لا توجد أهداف ادخارية بعد",
          description = "أنشئ صندوق طوارئ أو هدفاً ادخارياً وتابع تقدمك من حساباتك مباشرة.",
          actionLabel = "إضافة هدف ادخاري",
          onAction = onCreateSavingsGoal,
          actionTestTag = "planning_empty_goal_button",
        )
      }
    } else {
      items(state.savingsGoals, key = { "goal_${it.id}" }) { goal ->
        val progress = MoneyMath.ratio(goal.savedAmountMinor, goal.targetAmountMinor)
        val percent = MoneyMath.percentInt(goal.savedAmountMinor, goal.targetAmountMinor)
        val goalColor = Color(goal.colorHex)

        Surface(
          modifier =
            Modifier.fillMaxWidth()
              .clickable { onContributeGoal(goal) }
              .testTag("goal_card_${goal.id}"),
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
                Text(text = goal.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                  text =
                    "${MoneyMath.formatMinor(goal.savedAmountMinor, currency, masked = masked)} / ${MoneyMath.formatMinor(goal.targetAmountMinor, currency, masked = masked)}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Text(
                text = "$percent%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = goalColor,
              )
              IconButton(onClick = { onDeleteGoal(goal.id) }) {
                Icon(Icons.Filled.Delete, contentDescription = "حذف الهدف", tint = MaterialTheme.colorScheme.error)
              }
            }

            LinearProgressIndicator(
              progress = { progress },
              color = goalColor,
              modifier = Modifier.fillMaxWidth(),
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
              OutlinedButton(onClick = { onContributeGoal(goal) }) {
                Text("إيداع / سحب")
              }
            }
          }
        }
      }
    }

    // --- Section 2: Monthly Category Budgets ---
    item {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = "الميزانية الشهرية للتصنيفات", style = MaterialTheme.typography.titleLarge)
        Text(
          text = "اضغط على أي تصنيف لتحديد حد إنفاق شهري ومراقبته.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    item {
      Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          expenseCategories.forEachIndexed { index, cat ->
            val spent = spentByCategory[cat.id] ?: 0L
            val budget = cat.monthlyBudgetMinor
            val overBudget = budget != null && budget > 0L && spent > budget

            Column(
              modifier =
                Modifier.fillMaxWidth()
                  .clickable { budgetEditCategory = cat }
                  .padding(14.dp)
                  .testTag("budget_category_${cat.id}"),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Icon(
                  imageVector = categoryIconFor(cat.iconKey),
                  contentDescription = null,
                  tint = Color(cat.colorHex),
                  modifier = Modifier.size(20.dp),
                )
                Text(
                  text = cat.name,
                  style = MaterialTheme.typography.titleSmall,
                  modifier = Modifier.weight(1f),
                )
                Text(
                  text =
                    if (budget != null && budget > 0L) {
                      "${MoneyMath.formatMinor(spent, currency, masked = masked)} / ${MoneyMath.formatMinor(budget, currency, masked = masked)}"
                    } else {
                      "صُرف ${MoneyMath.formatMinor(spent, currency, masked = masked)} • بلا حد"
                    },
                  style = MaterialTheme.typography.bodySmall,
                  color = if (overBudget) financialColors.expense else MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }

              if (budget != null && budget > 0L) {
                LinearProgressIndicator(
                  progress = { MoneyMath.ratio(spent, budget) },
                  color = if (overBudget) financialColors.expense else Color(cat.colorHex),
                  modifier = Modifier.fillMaxWidth(),
                )
              }
            }
            if (index < expenseCategories.lastIndex) {
              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
          }
        }
      }
    }

    // --- Section 3: BiometricPrompt & Keystore Security ---
    item {
      Text(text = "الأمان وحماية المفاتيح (BiometricPrompt)", style = MaterialTheme.typography.titleLarge)
    }

    item {
      val supportText =
        when (state.biometricSupportStatus) {
          BiometricSupportStatus.STRONG_BIOMETRIC_READY ->
            "البصمة / المصادقة البيومترية القوية مدعومة وجاهزة على هذا الجهاز."
          BiometricSupportStatus.DEVICE_CREDENTIAL_ONLY ->
            "يمكن استخدام قفل الشاشة (PIN / نمط) لحماية الخزنة؛ لا توجد بصمة مسجلة حالياً."
          BiometricSupportStatus.NOT_ENROLLED ->
            "الجهاز يدعم البصمة ولكن لم يتم تسجيل بصمة في إعدادات النظام بعد."
          BiometricSupportStatus.NO_HARDWARE ->
            "هذا الجهاز لا يحتوي على مستشعر بيومتري مدعوم."
          BiometricSupportStatus.UNAVAILABLE ->
            "التحقق البيومتري غير متاح حالياً على هذا الجهاز."
        }

      val canEnable =
        state.biometricSupportStatus == BiometricSupportStatus.STRONG_BIOMETRIC_READY ||
          state.biometricSupportStatus == BiometricSupportStatus.DEVICE_CREDENTIAL_ONLY

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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.Fingerprint,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(28.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(text = "قفل التطبيق بالبصمة وحماية المفاتيح", style = MaterialTheme.typography.titleSmall)
              Text(
                text = supportText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Switch(
              checked = state.preferences.biometricLockEnabled,
              onCheckedChange = { wantEnable -> onToggleBiometricLock(wantEnable) },
              enabled = canEnable || state.preferences.biometricLockEnabled,
              modifier = Modifier.testTag("biometric_lock_switch"),
            )
          }

          Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = if (state.isKeystoreKeyPresent) Icons.Filled.VerifiedUser else Icons.Filled.Security,
                contentDescription = null,
                tint = if (state.isKeystoreKeyPresent) financialColors.income else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
              )
              Text(
                text =
                  if (state.isKeystoreKeyPresent) {
                    "مفتاح التشفير AES-256-GCM نشط ومربوط بـ Android Keystore."
                  } else {
                    "مفتاح الخزنة في Android Keystore سيُنشأ فور تفعيل القفل البيومتري."
                  },
                style = MaterialTheme.typography.bodySmall,
              )
            }
          }
        }
      }
    }

    // --- Section 4: Currency, Theme & Backup ---
    item {
      Text(text = "الإعدادات والنسخ الاحتياطي", style = MaterialTheme.typography.titleLarge)
    }

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
          Text(text = "العملة الافتراضية", style = MaterialTheme.typography.titleSmall)
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            CurrencyOption.entries.forEach { option ->
              FilterChip(
                selected = state.preferences.currency == option,
                onClick = { onSelectCurrency(option) },
                label = { Text("${option.arabicName} (${option.symbol})") },
                modifier = Modifier.testTag("currency_chip_${option.code.lowercase()}"),
              )
            }
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

          Text(text = "مظهر التطبيق", style = MaterialTheme.typography.titleSmall)
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            ThemeMode.entries.forEach { mode ->
              FilterChip(
                selected = state.preferences.themeMode == mode,
                onClick = { onSelectThemeMode(mode) },
                label = { Text(mode.arabicLabel) },
                modifier = Modifier.testTag("theme_chip_${mode.name.lowercase()}"),
              )
            }
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
              onClick = onExportBackup,
              modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("export_backup_button"),
            ) {
              Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("تصدير JSON")
            }
            OutlinedButton(
              onClick = { showResetConfirm = true },
              colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
              modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("reset_data_button"),
            ) {
              Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("مسح البيانات")
            }
          }
        }
      }
    }

    // --- KMA Brand Footer ---
    item {
      Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = "My Personal Accountant - KMA | محاسبك الشخصي",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
          )
          Text(
            text = "A KMA Product • تطوير خالد محمد الكحلاوي",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
          )
          Text(
            text = "تخزين محلي دقيق عبر Room Database • ${ArabicDateFormatter.formatFullDate(System.currentTimeMillis())}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
          )
        }
      }
    }

    item { Spacer(modifier = Modifier.height(72.dp)) }
  }

  budgetEditCategory?.let { cat ->
    var budgetInput by remember(cat.id) {
      mutableStateOf(cat.monthlyBudgetMinor?.let { MoneyMath.formatMinorPlainInput(it) } ?: "")
    }
    var budgetError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
      onDismissRequest = { budgetEditCategory = null },
      title = { Text("ميزانية تصنيف «${cat.name}»") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("اترك الحقل فارغاً أو أدخل 0 لإلغاء الحد الشهري.", style = MaterialTheme.typography.bodySmall)
          OutlinedTextField(
            value = budgetInput,
            onValueChange = {
              budgetInput = it
              budgetError = null
            },
            label = { Text("الحد الشهري (${currency.symbol})") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          )
          budgetError?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (budgetInput.trim().isEmpty()) {
              onUpdateCategoryBudget(cat, null)
              budgetEditCategory = null
            } else {
              val minor = MoneyMath.parseToMinorUnits(budgetInput, allowZero = true)
              if (minor == null) {
                budgetError = "يرجى إدخال قيمة صحيحة."
              } else {
                onUpdateCategoryBudget(cat, minor.takeIf { it > 0L })
                budgetEditCategory = null
              }
            }
          }
        ) {
          Text("حفظ")
        }
      },
      dismissButton = {
        TextButton(onClick = { budgetEditCategory = null }) { Text("إلغاء") }
      },
    )
  }

  if (showResetConfirm) {
    AlertDialog(
      onDismissRequest = { showResetConfirm = false },
      title = { Text("مسح جميع السجلات المالية؟") },
      text = { Text("سيتم حذف جميع الحسابات والمعاملات والديون والأقساط والأهداف نهائياً دون إمكانية التراجع.") },
      confirmButton = {
        Button(
          onClick = {
            onResetAllData()
            showResetConfirm = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) {
          Text("نعم، امسح الكل")
        }
      },
      dismissButton = {
        TextButton(onClick = { showResetConfirm = false }) { Text("إلغاء") }
      },
    )
  }
}
