package com.example.budgettracker.helpers

import androidx.room.TypeConverter
import com.example.budgettracker.model.Currency

class CurrencyConverter {
    @TypeConverter
    fun fromCurrency(value: String): Currency {
        return Currency.valueOf(value)
    }

    @TypeConverter
    fun currencyToString(currency: Currency): String {
        return currency.name
    }
}