package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
  tableName = "transactions",
  foreignKeys = [
    ForeignKey(
      entity = AccountEntity::class,
      parentColumns = ["id"],
      childColumns = ["accountId"],
      onDelete = ForeignKey.CASCADE,
    ),
    ForeignKey(
      entity = CategoryEntity::class,
      parentColumns = ["id"],
      childColumns = ["categoryId"],
      onDelete = ForeignKey.SET_NULL,
    ),
  ],
  indices = [
    Index("accountId"),
    Index("toAccountId"),
    Index("categoryId"),
    Index("timestamp"),
  ],
)
data class TransactionEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val type: TransactionType,
  val amountMinor: Long,
  val accountId: Long,
  val toAccountId: Long? = null,
  val categoryId: Long? = null,
  val title: String,
  val note: String = "",
  val timestamp: Long = System.currentTimeMillis(),
  val relatedDebtId: Long? = null,
  val relatedGoalId: Long? = null,
)
