package com.example

import com.example.domain.CurrencyOption
import com.example.domain.MoneyMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyMathTest {

  @Test
  fun `parse Arabic and Western digits to exact minor units`() {
    assertEquals(125050L, MoneyMath.parseToMinorUnits("1,250.50"))
    assertEquals(125050L, MoneyMath.parseToMinorUnits("١٢٥٠٫٥٠"))
    assertEquals(30000L, MoneyMath.parseToMinorUnits("٣٠٠"))
    assertEquals(0L, MoneyMath.parseToMinorUnits("0", allowZero = true))
    assertNull(MoneyMath.parseToMinorUnits("0", allowZero = false))
    assertNull(MoneyMath.parseToMinorUnits("-50"))
    assertNull(MoneyMath.parseToMinorUnits("abc"))
  }

  @Test
  fun `splitIntoInstallments preserves exact total with zero rounding drift`() {
    val splits = MoneyMath.splitIntoInstallments(100_000L, 3) // 1000.00 / 3
    assertEquals(3, splits.size)
    assertEquals(33_334L, splits[0])
    assertEquals(33_333L, splits[1])
    assertEquals(33_333L, splits[2])
    assertEquals(100_000L, splits.sum())
  }

  @Test
  fun `formatMinor handles positive negative and masked outputs`() {
    assertEquals("\u200E1,250.50\u200E ج.م", MoneyMath.formatMinor(125050L, CurrencyOption.EGP))
    assertEquals("\u200E+500.00\u200E ر.س", MoneyMath.formatMinor(50000L, CurrencyOption.SAR, showSign = true))
    assertEquals("\u200E-75.25\u200E $", MoneyMath.formatMinor(-7525L, CurrencyOption.USD))
    assertEquals("••••• ج.م", MoneyMath.formatMinor(125050L, CurrencyOption.EGP, masked = true))
  }
}
