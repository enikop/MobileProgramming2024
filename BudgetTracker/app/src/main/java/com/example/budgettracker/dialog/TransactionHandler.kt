package com.example.budgettracker.dialog

import android.os.Bundle
import com.example.budgettracker.MainActivity
import com.example.budgettracker.model.Transaction

interface TransactionHandler {
    fun transactionCreated(item: Transaction)
    fun transactionUpdated(item: Transaction)
    fun showEditItemDialog(itemToEdit: Transaction)
}