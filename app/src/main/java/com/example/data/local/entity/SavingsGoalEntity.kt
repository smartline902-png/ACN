package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val name: String,
  val targetAmountMinor: Long,
  val savedAmountMinor: Long = 0L,
  val targetDate: Long? = null,
  val colorHex: Long,
  val createdAt: Long = System.currentTimeMillis(),
  val isCompleted: Boolean = false,
) {
  val remainingMinor: Long
    get() = (targetAmountMinor - savedAmountMinor).coerceAtLeast(0L)
}
