package com.example.budgettracker.network
import com.example.budgettracker.model.ExchangeResult
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ExchangeAPI {
    @GET("/api/latest")
    fun getRates(@Query("access_key") key: String) : Call<ExchangeResult>
}