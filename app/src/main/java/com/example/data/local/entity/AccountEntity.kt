package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class AccountType(val arabicLabel: String) {
  CASH("محفظة نقدية"),
  BANK("حساب بنكي"),
  E_WALLET("محفظة إلكترونية"),
  SAVINGS("حساب ادخار"),
  CREDIT_CARD("بطاقة ائتمان"),
}

@Serializable
@Entity(tableName = "accounts")
data class AccountEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val name: String,
  val type: AccountType,
  val balanceMinor: Long,
  val initialBalanceMinor: Long,
  val colorHex: Long,
  val isDefault: Boolean = false,
  val createdAt: Long = System.currentTimeMillis(),
)
