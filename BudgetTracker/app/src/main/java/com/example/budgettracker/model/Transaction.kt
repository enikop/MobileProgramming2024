package com.example.budgettracker.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) var id: Long?,
    @ColumnInfo(name = "label") var label: String,
    @ColumnInfo(name = "amount") var amount: Double,
    @ColumnInfo(name = "incoming") var isIncoming: Boolean,
    @ColumnInfo(name = "completed") var isCompleted: Boolean,
    @ColumnInfo(name = "note") var note: String,
    @ColumnInfo(name = "date") var date: LocalDate = LocalDate.now(),
    @ColumnInfo(name = "currency") var currency: Currency = Currency.EUR
    ) : Serializable {

    val dateFormatted : String
        get() = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

}