package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.AccountEntity
import com.example.domain.MoneyMath
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.accountIconFor
import com.example.ui.viewmodel.AccountantUiState

@Composable
fun AccountsScreen(
  state: AccountantUiState,
  onAddAccount: () -> Unit,
  onEditAccount: (AccountEntity) -> Unit,
  onSetDefaultAccount: (Long) -> Unit,
  onTransferBetweenAccounts: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val currency = state.preferences.currency
  val masked = state.preferences.hideBalances

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
        Text(
          text = "الحسابات والمحافظ",
          style = MaterialTheme.typography.headlineMedium,
        )
        Button(
          onClick = onAddAccount,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("add_account_button"),
        ) {
          Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("حساب جديد")
        }
      }
    }

    item {
      Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = "إجمالي السيولة عبر جميع الحسابات (${state.accounts.size})",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
          )
          Text(
            text = MoneyMath.formatMinor(state.totalAccountsBalanceMinor, currency, masked = masked),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
          )
          if (state.accounts.size >= 2) {
            OutlinedButton(
              onClick = onTransferBetweenAccounts,
              modifier = Modifier.minimumInteractiveComponentSize().testTag("accounts_transfer_button"),
            ) {
              Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("تحويل رصيد بين حسابين")
            }
          }
        }
      }
    }

    if (state.accounts.isEmpty()) {
      item {
        EmptyStatePanel(
          icon = Icons.Filled.AccountBalanceWallet,
          title = "لا توجد حسابات مالية بعد",
          description = "أضف محفظة الكاش، الحساب البنكي، أو المحفظة الإلكترونية لتنظيم أموالك.",
          actionLabel = "إضافة حساب الآن",
          onAction = onAddAccount,
          actionTestTag = "accounts_empty_add_button",
        )
      }
    } else {
      items(state.accounts, key = { it.id }) { account ->
        val accentColor = Color(account.colorHex)
        Surface(
          modifier =
            Modifier.fillMaxWidth()
              .clickable { onEditAccount(account) }
              .testTag("account_card_${account.id}"),
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
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Box(
                modifier =
                  Modifier.size(46.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = accountIconFor(account.type),
                  contentDescription = account.type.arabicLabel,
                  tint = accentColor,
                )
              }

              Column(modifier = Modifier.weight(1f)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                  )
                  if (account.isDefault) {
                    Surface(
                      shape = MaterialTheme.shapes.small,
                      color = MaterialTheme.colorScheme.tertiaryContainer,
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                      ) {
                        Icon(
                          Icons.Filled.Star,
                          contentDescription = null,
                          tint = MaterialTheme.colorScheme.onTertiaryContainer,
                          modifier = Modifier.size(12.dp),
                        )
                        Text(
                          text = "افتراضي",
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                      }
                    }
                  }
                }
                Text(
                  text = account.type.arabicLabel,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }

              Text(
                text = MoneyMath.formatMinor(account.balanceMinor, currency, masked = masked),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color =
                  if (account.balanceMinor < 0L) MaterialTheme.colorScheme.error
                  else MaterialTheme.colorScheme.onSurface,
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End,
            ) {
              if (!account.isDefault) {
                TextButton(onClick = { onSetDefaultAccount(account.id) }) {
                  Text("تعيين كافتراضي")
                }
              }
              TextButton(onClick = { onEditAccount(account) }) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("تعديل")
              }
            }
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(72.dp)) }
  }
}
