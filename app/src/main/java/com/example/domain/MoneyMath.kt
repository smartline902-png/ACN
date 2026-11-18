package com.example.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

/**
 * High-precision financial calculation and formatting engine.
 * All monetary amounts are stored as exact minor units (1/100 of main currency unit)
 * to eliminate IEEE-754 floating-point rounding drift.
 */
object MoneyMath {

  private const val MAX_MINOR_UNITS = 999_999_999_999L // ~10 billion units

  fun normalizeDigits(raw: String): String {
    val sb = StringBuilder(raw.length)
    for (ch in raw) {
      when (ch) {
        in '٠'..'٩' -> sb.append(('0'.code + (ch.code - '٠'.code)).toChar())
        in '۰'..'۹' -> sb.append(('0'.code + (ch.code - '۰'.code)).toChar())
        '\u066B' -> sb.append('.') // Arabic decimal separator ٫
        '\u066C' -> Unit // Arabic thousands separator ٬ -> strip
        ' ', '\u00A0' -> Unit
        else -> sb.append(ch)
      }
    }
    return sb.toString()
  }

  /**
   * Parses a user-entered monetary string into exact minor units (Long).
   * Returns null if malformed, negative, or exceeding bounds.
   */
  fun parseToMinorUnits(rawInput: String, allowZero: Boolean = false): Long? {
    val trimmed = rawInput.trim()
    if (trimmed.isEmpty()) return null

    var normalized = normalizeDigits(trimmed)
    // Handle comma usage cleanly:
    // - If both ',' and '.' exist, ',' is thousands separator.
    // - If only ',' exists and appears once with <= 2 trailing digits, treat as decimal separator.
    if (normalized.contains(',') && normalized.contains('.')) {
      normalized = normalized.replace(",", "")
    } else if (normalized.contains(',')) {
      val commaCount = normalized.count { it == ',' }
      val afterLastComma = normalized.substringAfterLast(',')
      normalized =
        if (commaCount == 1 && afterLastComma.length in 1..2) {
          normalized.replace(',', '.')
        } else {
          normalized.replace(",", "")
        }
    }

    if (normalized.startsWith("+")) {
      normalized = normalized.removePrefix("+")
    }
    if (normalized.startsWith("-")) return null

    return try {
      val bd = BigDecimal(normalized).setScale(2, RoundingMode.HALF_UP)
      val minor = bd.movePointRight(2).longValueExact()
      if (minor < 0L) return null
      if (!allowZero && minor == 0L) return null
      if (minor > MAX_MINOR_UNITS) return null
      minor
    } catch (_: Exception) {
      null
    }
  }

  fun formatMinor(
    minorUnits: Long,
    currency: CurrencyOption,
    showSign: Boolean = false,
    masked: Boolean = false,
  ): String {
    if (masked) {
      return "••••• ${currency.symbol}"
    }
    val absMinor = abs(minorUnits)
    val bd = BigDecimal.valueOf(absMinor, 2)
    val symbols = DecimalFormatSymbols(Locale.US).apply {
      groupingSeparator = ','
      decimalSeparator = '.'
    }
    val formatter = DecimalFormat("#,##0.00", symbols)
    val numberPart = formatter.format(bd)
    val sign =
      when {
        minorUnits < 0L -> "-"
        showSign && minorUnits > 0L -> "+"
        else -> ""
      }
    // LRM (\u200E) keeps sign bound cleanly to the numeric value inside RTL layouts
    return "\u200E$sign$numberPart\u200E ${currency.symbol}"
  }

  fun formatMinorPlainInput(minorUnits: Long): String {
    val bd = BigDecimal.valueOf(minorUnits, 2)
    return bd.toPlainString()
  }

  /**
   * Splits [totalMinor] into [count] installments whose exact sum equals [totalMinor].
   * Any remainder cents/piastres are distributed +1 minor unit to the earliest installments.
   */
  fun splitIntoInstallments(totalMinor: Long, count: Int): List<Long> {
    require(totalMinor > 0L) { "Total amount must be positive" }
    require(count >= 1) { "Installment count must be >= 1" }
    val base = totalMinor / count
    val remainder = (totalMinor % count).toInt()
    return List(count) { index ->
      if (index < remainder) base + 1L else base
    }
  }

  /**
   * Computes exact percentage in [0f, 1f] using BigDecimal.
   */
  fun ratio(partMinor: Long, totalMinor: Long): Float {
    if (totalMinor <= 0L || partMinor <= 0L) return 0f
    val part = BigDecimal.valueOf(partMinor)
    val total = BigDecimal.valueOf(totalMinor)
    return part.divide(total, 4, RoundingMode.HALF_UP)
      .toFloat()
      .coerceIn(0f, 1f)
  }

  fun percentInt(partMinor: Long, totalMinor: Long): Int {
    if (totalMinor <= 0L || partMinor <= 0L) return 0
    val part = BigDecimal.valueOf(partMinor)
    val total = BigDecimal.valueOf(totalMinor)
    return part
      .multiply(BigDecimal(100))
      .divide(total, 0, RoundingMode.HALF_UP)
      .toInt()
      .coerceAtLeast(0)
  }
}
