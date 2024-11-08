package com.example.budgettracker.network

import com.example.budgettracker.model.ExchangeResult

interface RatesCallback {
    fun onRatesReceived(result: ExchangeResult)
    fun onError(error: Throwable)
}