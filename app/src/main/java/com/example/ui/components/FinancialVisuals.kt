package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AccountType
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import com.example.domain.ArabicDateFormatter
import com.example.domain.CurrencyOption
import com.example.domain.MoneyMath
import com.example.ui.theme.LocalKmaFinancialColors
import com.example.ui.viewmodel.CategoryBreakdownItem

fun categoryIconFor(iconKey: String?): ImageVector =
  when (iconKey) {
    "restaurant" -> Icons.Filled.Restaurant
    "directions_car" -> Icons.Filled.DirectionsCar
    "receipt_long" -> Icons.Filled.ReceiptLong
    "home" -> Icons.Filled.Home
    "shopping_bag" -> Icons.Filled.ShoppingBag
    "medical_services" -> Icons.Filled.MedicalServices
    "school" -> Icons.Filled.School
    "handshake" -> Icons.Filled.Handshake
    "payments" -> Icons.Filled.Payments
    "work" -> Icons.Filled.Work
    "trending_up" -> Icons.AutoMirrored.Filled.TrendingUp
    "account_balance_wallet" -> Icons.Filled.AccountBalanceWallet
    "card_giftcard" -> Icons.Filled.CardGiftcard
    else -> Icons.Filled.Category
  }

fun accountIconFor(type: AccountType): ImageVector =
  when (type) {
    AccountType.CASH -> Icons.Filled.AccountBalanceWallet
    AccountType.BANK -> Icons.Filled.AccountBalance
    AccountType.E_WALLET -> Icons.Filled.PhoneAndroid
    AccountType.SAVINGS -> Icons.Filled.Savings
    AccountType.CREDIT_CARD -> Icons.Filled.CreditCard
  }

@Composable
fun HeroNetWorthCard(
  netWorthMinor: Long,
  totalLiquidityMinor: Long,
  totalReceivablesMinor: Long,
  totalPayablesMinor: Long,
  currency: CurrencyOption,
  hideBalances: Boolean,
  onToggleHideBalances: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val financialColors = LocalKmaFinancialColors.current
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.extraLarge,
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
  ) {
    Box(modifier = Modifier.fillMaxWidth()) {
      Image(
        painter = painterResource(id = R.drawable.img_wallet_hero_bg_1795011011824),
        contentDescription = null,
        modifier = Modifier.matchParentSize(),
        contentScale = ContentScale.Crop,
      )
      Box(
        modifier =
          Modifier.matchParentSize()
            .background(
              Brush.linearGradient(
                colors =
                  listOf(
                    Color.Black.copy(alpha = 0.25f),
                    Color.Black.copy(alpha = 0.45f),
                  )
              )
            )
      )

      Column(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(
              text = "صافي الثروة الشخصية",
              style = MaterialTheme.typography.titleSmall,
              color = Color.White.copy(alpha = 0.85f),
            )
            Text(
              text = MoneyMath.formatMinor(netWorthMinor, currency, masked = hideBalances),
              style = MaterialTheme.typography.displaySmall,
              fontWeight = FontWeight.Bold,
              color = Color.White,
            )
          }

          IconButton(
            onClick = onToggleHideBalances,
            modifier =
              Modifier.minimumInteractiveComponentSize()
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.16f))
                .testTag("toggle_hide_balances_button"),
          ) {
            Icon(
              imageVector = if (hideBalances) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
              contentDescription = if (hideBalances) "إظهار الأرصدة" else "إخفاء الأرصدة",
              tint = Color.White,
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          HeroMetricPill(
            label = "السيولة",
            value = MoneyMath.formatMinor(totalLiquidityMinor, currency, masked = hideBalances),
            accentColor = Color.White,
            modifier = Modifier.weight(1f),
          )
          HeroMetricPill(
            label = "لي عند الغير",
            value = MoneyMath.formatMinor(totalReceivablesMinor, currency, masked = hideBalances),
            accentColor = financialColors.goldAccent.copy(alpha = 0.95f),
            modifier = Modifier.weight(1f),
          )
          HeroMetricPill(
            label = "عليّ التزامات",
            value = MoneyMath.formatMinor(totalPayablesMinor, currency, masked = hideBalances),
            accentColor = Color(0xFFFFB4AB),
            modifier = Modifier.weight(1f),
          )
        }
      }
    }
  }
}

@Composable
private fun HeroMetricPill(
  label: String,
  value: String,
  accentColor: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = MaterialTheme.shapes.medium,
    color = Color.White.copy(alpha = 0.12f),
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White.copy(alpha = 0.78f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = value,
        style = MaterialTheme.typography.labelLarge,
        color = accentColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun TransactionRowItem(
  transaction: TransactionEntity,
  account: AccountEntity?,
  toAccount: AccountEntity?,
  category: CategoryEntity?,
  currency: CurrencyOption,
  hideBalances: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val financialColors = LocalKmaFinancialColors.current
  val (tintColor, containerColor, prefixSign) =
    when (transaction.type) {
      TransactionType.INCOME ->
        Triple(financialColors.income, financialColors.incomeContainer, true)
      TransactionType.EXPENSE ->
        Triple(financialColors.expense, financialColors.expenseContainer, false)
      TransactionType.TRANSFER ->
        Triple(financialColors.transfer, financialColors.transferContainer, false)
    }

  val icon =
    when (transaction.type) {
      TransactionType.TRANSFER -> Icons.Filled.SwapHoriz
      else -> categoryIconFor(category?.iconKey)
    }

  val signedMinor =
    when (transaction.type) {
      TransactionType.INCOME -> transaction.amountMinor
      TransactionType.EXPENSE -> -transaction.amountMinor
      TransactionType.TRANSFER -> transaction.amountMinor
    }

  Surface(
    modifier =
      modifier
        .fillMaxWidth()
        .clip(MaterialTheme.shapes.medium)
        .clickable(onClick = onClick)
        .testTag("transaction_item_${transaction.id}"),
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Box(
        modifier = Modifier.size(44.dp).clip(CircleShape).background(containerColor),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = transaction.type.arabicLabel,
          tint = tintColor,
          modifier = Modifier.size(22.dp),
        )
      }

      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
          text = transaction.title,
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        val subtitle =
          when (transaction.type) {
            TransactionType.TRANSFER ->
              "${account?.name ?: "حساب"} ← ${toAccount?.name ?: "حساب"} • ${ArabicDateFormatter.formatShortDate(transaction.timestamp)}"
            else ->
              "${category?.name ?: transaction.type.arabicLabel} • ${account?.name ?: "حساب"} • ${ArabicDateFormatter.formatShortDate(transaction.timestamp)}"
          }
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Text(
        text = MoneyMath.formatMinor(signedMinor, currency, showSign = prefixSign, masked = hideBalances),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = tintColor,
      )
    }
  }
}

@Composable
fun CategoryDonutChart(
  breakdown: List<CategoryBreakdownItem>,
  totalExpenseMinor: Long,
  currency: CurrencyOption,
  hideBalances: Boolean,
  modifier: Modifier = Modifier,
) {
  val animatedProgress by animateFloatAsState(
    targetValue = 1f,
    animationSpec = tween(durationMillis = 550),
    label = "donut_anim",
  )

  val fallbackTrack = MaterialTheme.colorScheme.surfaceVariant
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Box(
      modifier = Modifier.size(136.dp),
      contentAlignment = Alignment.Center,
    ) {
      Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        val strokeWidth = 22.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)

        if (breakdown.isEmpty() || totalExpenseMinor <= 0L) {
          drawArc(
            color = fallbackTrack,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
          )
        } else {
          var startAngle = -90f
          for (item in breakdown) {
            val sweep = 360f * item.shareRatio * animatedProgress
            if (sweep > 0.5f) {
              drawArc(
                color = Color(item.category.colorHex),
                startAngle = startAngle,
                sweepAngle = (sweep - 2f).coerceAtLeast(1f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
              )
              startAngle += sweep
            }
          }
        }
      }

      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = "مصروف الشهر",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = MoneyMath.formatMinor(totalExpenseMinor, currency, masked = hideBalances),
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
    }

    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      if (breakdown.isEmpty()) {
        Text(
          text = "لا توجد مصروفات مسجلة في هذا الشهر بعد.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        breakdown.take(4).forEach { item ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Box(
              modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(item.category.colorHex))
            )
            Text(
              text = item.category.name,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.weight(1f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "${MoneyMath.percentInt(item.spentMinor, totalExpenseMinor)}%",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}

@Composable
fun EmptyStatePanel(
  icon: ImageVector,
  title: String,
  description: String,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
  actionTestTag: String = "empty_state_action",
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.large,
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Box(
        modifier =
          Modifier.size(56.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(28.dp),
        )
      }
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      if (actionLabel != null && onAction != null) {
        Spacer(modifier = Modifier.height(4.dp))
        Button(
          onClick = onAction,
          modifier = Modifier.minimumInteractiveComponentSize().testTag(actionTestTag),
        ) {
          Text(text = actionLabel)
        }
      }
    }
  }
}
