package com.example.budgettracker.touch

interface TransactionTouchHelperAdapter {
    fun onItemDismissed(position: Int)
    fun onItemMoved(fromPosition: Int, toPosition: Int)
}