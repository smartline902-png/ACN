package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.local.ThemeMode

@Immutable
data class KmaFinancialColors(
  val income: Color,
  val incomeContainer: Color,
  val expense: Color,
  val expenseContainer: Color,
  val debtWarning: Color,
  val debtWarningContainer: Color,
  val transfer: Color,
  val transferContainer: Color,
  val goldAccent: Color,
)

val LocalKmaFinancialColors = staticCompositionLocalOf {
  KmaFinancialColors(
    income = IncomeGreenLight,
    incomeContainer = IncomeGreenContainerLight,
    expense = ExpenseRedLight,
    expenseContainer = ExpenseRedContainerLight,
    debtWarning = DebtAmberLight,
    debtWarningContainer = DebtAmberContainerLight,
    transfer = TransferBlueLight,
    transferContainer = TransferBlueContainerLight,
    goldAccent = GoldAccentLight,
  )
}

private val LightColorScheme =
  lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldPrimaryContainer,
    onPrimaryContainer = EmeraldOnPrimaryContainer,
    secondary = ForestSecondary,
    onSecondary = ForestOnSecondary,
    secondaryContainer = ForestSecondaryContainer,
    onSecondaryContainer = ForestOnSecondaryContainer,
    tertiary = GoldTertiary,
    onTertiary = GoldOnTertiary,
    tertiaryContainer = GoldTertiaryContainer,
    onTertiaryContainer = GoldOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
  )

private val DarkColorScheme =
  darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = EmeraldOnPrimaryDark,
    primaryContainer = EmeraldPrimaryContainerDark,
    onPrimaryContainer = EmeraldOnPrimaryContainerDark,
    secondary = ForestSecondaryDark,
    onSecondary = ForestOnSecondaryDark,
    secondaryContainer = ForestSecondaryContainerDark,
    onSecondaryContainer = ForestOnSecondaryContainerDark,
    tertiary = GoldTertiaryDark,
    onTertiary = GoldOnTertiaryDark,
    tertiaryContainer = GoldTertiaryContainerDark,
    onTertiaryContainer = GoldOnTertiaryContainerDark,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
  )

private val LightFinancialColors =
  KmaFinancialColors(
    income = IncomeGreenLight,
    incomeContainer = IncomeGreenContainerLight,
    expense = ExpenseRedLight,
    expenseContainer = ExpenseRedContainerLight,
    debtWarning = DebtAmberLight,
    debtWarningContainer = DebtAmberContainerLight,
    transfer = TransferBlueLight,
    transferContainer = TransferBlueContainerLight,
    goldAccent = GoldAccentLight,
  )

private val DarkFinancialColors =
  KmaFinancialColors(
    income = IncomeGreenDark,
    incomeContainer = IncomeGreenContainerDark,
    expense = ExpenseRedDark,
    expenseContainer = ExpenseRedContainerDark,
    debtWarning = DebtAmberDark,
    debtWarningContainer = DebtAmberContainerDark,
    transfer = TransferBlueDark,
    transferContainer = TransferBlueContainerDark,
    goldAccent = GoldAccentDark,
  )

private val KmaShapes =
  Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
  )

@Composable
fun KmaAccountantTheme(
  themeMode: ThemeMode = ThemeMode.SYSTEM,
  content: @Composable () -> Unit,
) {
  val isDark =
    when (themeMode) {
      ThemeMode.SYSTEM -> isSystemInDarkTheme()
      ThemeMode.LIGHT -> false
      ThemeMode.DARK -> true
    }

  val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
  val financialColors = if (isDark) DarkFinancialColors else LightFinancialColors

  CompositionLocalProvider(
    LocalLayoutDirection provides LayoutDirection.Rtl,
    LocalKmaFinancialColors provides financialColors,
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = KmaTypography,
      shapes = KmaShapes,
      content = content,
    )
  }
}
