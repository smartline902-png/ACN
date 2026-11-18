package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class DebtDirection(val arabicLabel: String) {
  OWED_BY_ME("دين عليّ (التزام)"),
  OWED_TO_ME("دين لي (مستحق)"),
}

@Serializable
enum class DebtStatus(val arabicLabel: String) {
  ACTIVE("نشط"),
  PAID("مسدد بالكامل"),
}

@Serializable
@Entity(tableName = "debts")
data class DebtEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val personName: String,
  val phone: String = "",
  val direction: DebtDirection,
  val totalAmountMinor: Long,
  val paidAmountMinor: Long = 0L,
  val installmentsCount: Int = 1,
  val dueDate: Long,
  val createdAt: Long = System.currentTimeMillis(),
  val note: String = "",
  val status: DebtStatus = DebtStatus.ACTIVE,
) {
  val remainingMinor: Long
    get() = (totalAmountMinor - paidAmountMinor).coerceAtLeast(0L)
}

@Serializable
@Entity(
  tableName = "installments",
  foreignKeys = [
    ForeignKey(
      entity = DebtEntity::class,
      parentColumns = ["id"],
      childColumns = ["debtId"],
      onDelete = ForeignKey.CASCADE,
    )
  ],
  indices = [Index("debtId"), Index("dueDate")],
)
data class InstallmentEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val debtId: Long,
  val installmentNumber: Int,
  val amountMinor: Long,
  val paidAmountMinor: Long = 0L,
  val dueDate: Long,
  val paidAt: Long? = null,
  val isPaid: Boolean = false,
) {
  val remainingMinor: Long
    get() = (amountMinor - paidAmountMinor).coerceAtLeast(0L)
}
