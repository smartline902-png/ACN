package com.example.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AccountantDao
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.InstallmentEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType

@Database(
  entities = [
    AccountEntity::class,
    CategoryEntity::class,
    TransactionEntity::class,
    DebtEntity::class,
    InstallmentEntity::class,
    SavingsGoalEntity::class,
  ],
  version = 1,
  exportSchema = false,
)
@TypeConverters(KmaTypeConverters::class)
abstract class KmaDatabase : RoomDatabase() {
  abstract fun accountantDao(): AccountantDao

  companion object {
    @Volatile
    private var INSTANCE: KmaDatabase? = null

    fun getInstance(context: Context): KmaDatabase {
      return INSTANCE
        ?: synchronized(this) {
          INSTANCE
            ?: Room.databaseBuilder(
                context.applicationContext,
                KmaDatabase::class.java,
                "kma_personal_accountant.db",
              )
              .addCallback(SeedCategoriesCallback)
              .build()
              .also { INSTANCE = it }
        }
    }

    private object SeedCategoriesCallback : Callback() {
      override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        val defaults = listOf(
          Triple("طعام ومشروبات", TransactionType.EXPENSE.name, "restaurant" to 0xFFE65100L),
          Triple("مواصلات ووقود", TransactionType.EXPENSE.name, "directions_car" to 0xFF0277BDL),
          Triple("فواتير وخدمات", TransactionType.EXPENSE.name, "receipt_long" to 0xFF6A1B9AL),
          Triple("سكن وإيجار", TransactionType.EXPENSE.name, "home" to 0xFF4E342EL),
          Triple("تسوق ومستلزمات", TransactionType.EXPENSE.name, "shopping_bag" to 0xFFC2185BL),
          Triple("صحة وعلاج", TransactionType.EXPENSE.name, "medical_services" to 0xFFD32F2FL),
          Triple("تعليم وتطوير", TransactionType.EXPENSE.name, "school" to 0xFF283593L),
          Triple("التزامات وديون", TransactionType.EXPENSE.name, "handshake" to 0xFFB8860BL),
          Triple("أخرى (مصروف)", TransactionType.EXPENSE.name, "category" to 0xFF546E7AL),
          Triple("الراتب الشهري", TransactionType.INCOME.name, "payments" to 0xFF0E7C47L),
          Triple("عمل حر ومشاريع", TransactionType.INCOME.name, "work" to 0xFF0B4F3BL),
          Triple("أرباح واستثمار", TransactionType.INCOME.name, "trending_up" to 0xFF1B6B52L),
          Triple("تحصيل مستحقات", TransactionType.INCOME.name, "account_balance_wallet" to 0xFF2E7D32L),
          Triple("هدايا وإيرادات أخرى", TransactionType.INCOME.name, "card_giftcard" to 0xFF00838FL),
        )

        db.beginTransaction()
        try {
          for ((name, type, iconAndColor) in defaults) {
            val values = ContentValues().apply {
              put("name", name)
              put("type", type)
              put("iconKey", iconAndColor.first)
              put("colorHex", iconAndColor.second)
              putNull("monthlyBudgetMinor")
              put("isSystemDefault", 1)
            }
            db.insert("categories", SQLiteDatabase.CONFLICT_IGNORE, values)
          }
          db.setTransactionSuccessful()
        } finally {
          db.endTransaction()
        }
      }
    }
  }
}
