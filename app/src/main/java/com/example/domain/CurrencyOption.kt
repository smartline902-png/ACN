package com.example.domain

enum class CurrencyOption(
  val code: String,
  val symbol: String,
  val arabicName: String,
) {
  EGP("EGP", "ج.م", "جنيه مصري"),
  SAR("SAR", "ر.س", "ريال سعودي"),
  AED("AED", "د.إ", "درهم إماراتي"),
  KWD("KWD", "د.ك", "دينار كويتي"),
  USD("USD", "$", "دولار أمريكي"),
  EUR("EUR", "€", "يورو");

  companion object {
    fun fromCode(code: String?): CurrencyOption =
      entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: EGP
  }
}
