package com.example.budgettracker.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.budgettracker.R
import com.example.budgettracker.database.AppDatabase
import com.example.budgettracker.helpers.MonthlyBarChartBuilder
import com.example.budgettracker.model.Currency
import com.example.budgettracker.model.ExchangeResult
import com.example.budgettracker.model.Rates
import com.example.budgettracker.model.Transaction
import com.example.budgettracker.network.ExchangeService
import com.example.budgettracker.network.RatesCallback
import kotlinx.android.synthetic.main.fragment_stats.view.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale


class StatsFragment : Fragment(), RatesCallback {
    private lateinit var spendingData: List<Float>
    private lateinit var incomeData: List<Float>
    private lateinit var labels: List<String>
    private lateinit var spDiagramCurrency: Spinner
    private lateinit var chartBuilder: MonthlyBarChartBuilder

    private var monthsShown = 12
    private var exchangeService = ExchangeService()
    private var exchangeResult: ExchangeResult? = null
    private var selectedCurrency = Currency.EUR
    private var monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_stats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val spendingColor = ContextCompat.getColor(requireContext(), R.color.spendingBarColour)
        val incomeColor = ContextCompat.getColor(requireContext(), R.color.incomeBarColour)
        chartBuilder = MonthlyBarChartBuilder( view.barChart, 15f, spendingColor, incomeColor)
        spDiagramCurrency = view.spDiagramCurrency
        chartBuilder.setNoDataText("Loading diagram...", Color.BLACK)
        val currencies = Currency.values().map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, currencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        val selectionPosition = adapter.getPosition(selectedCurrency.name)
        spDiagramCurrency.adapter = adapter
        spDiagramCurrency.setSelection(selectionPosition)
        spDiagramCurrency.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (exchangeResult == null) exchangeService.getRates(this@StatsFragment)
                else {
                    val selectedCurrencyName = parent.selectedItem as String
                    selectedCurrency = Currency.valueOf(selectedCurrencyName)
                    initTransactions()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Handle no selection case
            }
        }
    }

    private fun initTransactions() {
        val dbThread = Thread {
            val today = LocalDate.now()
            val months = (monthsShown - 1 downTo 0).map {
                today.minusMonths(it.toLong()).format(monthFormatter)
            }
            labels = (monthsShown - 1 downTo 0).map {
                today.month.minus(it.toLong()).getDisplayName(TextStyle.SHORT, Locale.getDefault())
            }

            val stats = mutableListOf<Pair<Float, Float>>()
            for (i in 0 until monthsShown) {
                val transactions = AppDatabase.getInstance(requireContext()).transactionDao().findTransactionsForMonth(months[i])
                stats.add(calculateMonthlyStat(transactions, selectedCurrency))
            }

            spendingData = stats.map { it.second }
            incomeData = stats.map { it.first }

            activity?.runOnUiThread {
                setupChart()
            }
        }
        dbThread.start()
    }

    private fun setupChart() {
        chartBuilder.build(spendingData, incomeData, labels, selectedCurrency)
    }

    private fun convertAmount(amount: Double, fromCurrency: Currency, toCurrency: Currency, rates: Rates): Double {
        val rateToBase = rates.getRate(fromCurrency) ?: 1.0
        val rateFromBase = rates.getRate(toCurrency) ?: 1.0
        return amount / rateToBase * rateFromBase
    }

    private fun calculateMonthlyStat(
        transactions: List<Transaction>,
        targetCurrency: Currency
    ): Pair<Float, Float> {

        val incomingSum = transactions
            .filter { it.isIncoming }
            .sumOf { convertAmount(it.amount, it.currency, targetCurrency, exchangeResult!!.rates!!) }

        val outgoingSum = transactions
            .filter { !it.isIncoming }
            .sumOf { convertAmount(it.amount, it.currency, targetCurrency, exchangeResult!!.rates!!) }

        return Pair(incomingSum.toFloat(), outgoingSum.toFloat())
    }

    override fun onRatesReceived(result: ExchangeResult) {
        if(result.success != null && result.success) {
            this.exchangeResult = result
            initTransactions()
        } else {
            chartBuilder.setNoDataText("The diagram could not access the current exchange rates.", Color.BLACK)
        }
    }

    override fun onError(error: Throwable) {
        chartBuilder.setNoDataText("The diagram could not access the current exchange rates.", Color.BLACK)
    }
}