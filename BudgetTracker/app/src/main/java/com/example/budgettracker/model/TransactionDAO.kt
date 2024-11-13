package com.example.budgettracker.model

import androidx.room.*

@Dao
interface TransactionDAO {

    @Query("SELECT * FROM transactions")
    fun findAllItems(): List<Transaction>

    @Query("SELECT * FROM transactions WHERE date LIKE :yearMonth || '%'")
    fun findTransactionsForMonth(yearMonth: String): List<Transaction>

    @Insert
    fun insertItem(item: Transaction): Long

    @Delete
    fun deleteItem(item: Transaction)

    @Update
    fun updateItem(item: Transaction)
}