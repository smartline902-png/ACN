package com.example.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object ArabicDateFormatter {

  private val arabicLocale = Locale("ar", "EG")

  fun formatFullDate(timestampMillis: Long): String {
    val sdf = SimpleDateFormat("d MMMM yyyy", arabicLocale)
    return sdf.format(Date(timestampMillis))
  }

  fun formatShortDate(timestampMillis: Long): String {
    val sdf = SimpleDateFormat("d MMM yyyy", arabicLocale)
    return sdf.format(Date(timestampMillis))
  }

  fun formatMonthYear(timestampMillis: Long): String {
    val sdf = SimpleDateFormat("MMMM yyyy", arabicLocale)
    return sdf.format(Date(timestampMillis))
  }

  fun formatDateTime(timestampMillis: Long): String {
    val sdf = SimpleDateFormat("d MMM • hh:mm a", arabicLocale)
    return sdf.format(Date(timestampMillis))
  }

  fun startOfCurrentMonthMillis(nowMillis: Long = System.currentTimeMillis()): Long {
    val cal = Calendar.getInstance().apply {
      timeInMillis = nowMillis
      set(Calendar.DAY_OF_MONTH, 1)
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
  }

  fun endOfCurrentMonthMillis(nowMillis: Long = System.currentTimeMillis()): Long {
    val cal = Calendar.getInstance().apply {
      timeInMillis = nowMillis
      set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
      set(Calendar.HOUR_OF_DAY, 23)
      set(Calendar.MINUTE, 59)
      set(Calendar.SECOND, 59)
      set(Calendar.MILLISECOND, 999)
    }
    return cal.timeInMillis
  }

  fun addMonths(baseMillis: Long, monthsToAdd: Int): Long {
    val cal = Calendar.getInstance().apply {
      timeInMillis = baseMillis
      add(Calendar.MONTH, monthsToAdd)
    }
    return cal.timeInMillis
  }

  fun addDays(baseMillis: Long, daysToAdd: Int): Long {
    val cal = Calendar.getInstance().apply {
      timeInMillis = baseMillis
      add(Calendar.DAY_OF_YEAR, daysToAdd)
    }
    return cal.timeInMillis
  }
}
