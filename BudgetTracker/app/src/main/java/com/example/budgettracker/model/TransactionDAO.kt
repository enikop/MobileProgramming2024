package com.example.budgettracker.model

import androidx.room.*

@Dao
interface TransactionDAO {

    @Query("SELECT * FROM transactions")
    fun findAllItems(): List<Transaction>

    //Egy elem beszúrása
    @Insert
    fun insertItem(item: Transaction): Long
    //Egy törlése
    @Delete
    fun deleteItem(item: Transaction)
    //Egy módosítása
    @Update
    fun updateItem(item: Transaction)
}