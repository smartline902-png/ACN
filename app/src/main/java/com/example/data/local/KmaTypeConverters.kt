package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.local.entity.AccountType
import com.example.data.local.entity.DebtDirection
import com.example.data.local.entity.DebtStatus
import com.example.data.local.entity.TransactionType

class KmaTypeConverters {
  @TypeConverter
  fun fromAccountType(value: AccountType): String = value.name

  @TypeConverter
  fun toAccountType(value: String): AccountType = AccountType.valueOf(value)

  @TypeConverter
  fun fromTransactionType(value: TransactionType): String = value.name

  @TypeConverter
  fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

  @TypeConverter
  fun fromDebtDirection(value: DebtDirection): String = value.name

  @TypeConverter
  fun toDebtDirection(value: String): DebtDirection = DebtDirection.valueOf(value)

  @TypeConverter
  fun fromDebtStatus(value: DebtStatus): String = value.name

  @TypeConverter
  fun toDebtStatus(value: String): DebtStatus = DebtStatus.valueOf(value)
}
