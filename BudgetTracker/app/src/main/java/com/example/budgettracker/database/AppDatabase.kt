package com.example.budgettracker.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.budgettracker.helpers.CurrencyConverter
import com.example.budgettracker.helpers.DateConverter
import com.example.budgettracker.model.Transaction
import com.example.budgettracker.model.TransactionDAO

@Database(entities = [Transaction::class], version = 1)
@TypeConverters(DateConverter::class, CurrencyConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDAO

    companion object {
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            if (INSTANCE == null) {
                INSTANCE = Room.databaseBuilder(context.applicationContext,
                    AppDatabase::class.java, "budgetTracker.db")
                    .build()
            }
            return INSTANCE!!
        }

        fun destroyInstance() {
            INSTANCE = null
        }
    }
}