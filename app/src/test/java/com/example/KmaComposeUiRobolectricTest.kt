package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class KmaComposeUiRobolectricTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun `dashboard renders in Arabic and bottom navigation switches between all tabs`() {
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("محاسبك الشخصي").assertIsDisplayed()
    composeTestRule.onNodeWithText("صافي الثروة الشخصية").assertIsDisplayed()

    composeTestRule.onNodeWithTag("nav_accounts").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("الحسابات والمحافظ").assertIsDisplayed()

    composeTestRule.onNodeWithTag("nav_debts").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("الديون والأقساط").assertIsDisplayed()

    composeTestRule.onNodeWithTag("nav_planning").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("التخطيط، الادخار والأمان").assertIsDisplayed()

    composeTestRule.onNodeWithTag("nav_transactions").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("سجل المعاملات المالية").assertIsDisplayed()
  }
}
