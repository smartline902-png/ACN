package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class TransactionType(val arabicLabel: String) {
  EXPENSE("مصروف"),
  INCOME("دخل"),
  TRANSFER("تحويل"),
}

@Serializable
@Entity(tableName = "categories")
data class CategoryEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val name: String,
  val type: TransactionType,
  val iconKey: String,
  val colorHex: Long,
  val monthlyBudgetMinor: Long? = null,
  val isSystemDefault: Boolean = false,
)
