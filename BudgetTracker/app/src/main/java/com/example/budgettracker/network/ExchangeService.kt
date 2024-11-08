package com.example.budgettracker.network

import com.example.budgettracker.model.ExchangeResult
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ExchangeService {
    var retrofit: Retrofit
    var api: ExchangeAPI
    constructor() {
        retrofit = Retrofit.Builder()
            .baseUrl("http://data.fixer.io")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        api = retrofit.create(ExchangeAPI::class.java)
    }

    fun getRates(callback: RatesCallback) {
        val call = api.getRates("969c37b5a73f8cb2d12c081dcbdc35e6")
        call.enqueue(object : Callback<ExchangeResult> {
            override fun onResponse(call: Call<ExchangeResult>, response: Response<ExchangeResult>) {
                if (response.body() != null) {
                    callback.onRatesReceived(response.body() as ExchangeResult)
                } else {
                    callback.onError(Throwable("No rates available"))
                }
            }

            override fun onFailure(call: Call<ExchangeResult>, t: Throwable) {
                callback.onError(t)
            }
        })
    }
}