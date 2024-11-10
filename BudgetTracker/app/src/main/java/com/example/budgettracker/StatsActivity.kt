package com.example.budgettracker

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.budgettracker.database.AppDatabase
import com.example.budgettracker.model.Currency
import com.example.budgettracker.model.ExchangeResult
import com.example.budgettracker.model.Rates
import com.example.budgettracker.model.Transaction
import com.example.budgettracker.network.ExchangeService
import com.example.budgettracker.network.RatesCallback
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.android.synthetic.main.activity_stats.*
import kotlinx.android.synthetic.main.activity_stats.toolbar
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class StatsActivity: AppCompatActivity(), RatesCallback {
    private lateinit var result: ExchangeResult
    private lateinit var spendingsData: List<Float>
    private lateinit var savingsData: List<Float>
    private lateinit var labels: List<String>

    private var exchangeService = ExchangeService()
    private var monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)
        setSupportActionBar(toolbar)
        btnMain.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        exchangeService.getRates(this)
    }

    private fun initTransactions() {
        val dbThread = Thread {
            val today = LocalDate.now()
            val months = (2 downTo 0).map {
                today.minusMonths(it.toLong()).format(monthFormatter)
            }
            labels = (2 downTo 0).map {
                today.month.minus(it.toLong()).getDisplayName(TextStyle.SHORT, Locale.getDefault())
            }
            val transactions1 = AppDatabase.getInstance(this).transactionDao().findTransactionsForMonth(months[0])
            val transactions2 = AppDatabase.getInstance(this).transactionDao().findTransactionsForMonth(months[1])
            val transactions3 = AppDatabase.getInstance(this).transactionDao().findTransactionsForMonth(months[2])

            val stat1 = calculateMonthlyStat(transactions1, Currency.EUR)
            val stat2 = calculateMonthlyStat(transactions2, Currency.EUR)
            val stat3 = calculateMonthlyStat(transactions3, Currency.EUR)

            spendingsData = listOf(stat1.second, stat2.second, stat3.second)
            savingsData = listOf(stat1.first, stat2.first, stat3.first)

            runOnUiThread{
                setupChart();
            }
        }
        dbThread.start()
    }

    private fun setupChart() {

        // Bar entries for each data point
        val spendingEntries = spendingsData.mapIndexed { index, value -> BarEntry(index.toFloat()*1.25f, value) }
        val savingEntries = savingsData.mapIndexed { index, value -> BarEntry(index.toFloat()*1.25f, value) }

        // Data sets for spendings (red) and savings (green)
        val spendingDataSet = BarDataSet(spendingEntries, "Spendings (EUR)").apply { color = Color.RED; valueTextSize = 12f}
        val savingDataSet = BarDataSet(savingEntries, "Savings (EUR)").apply { color = Color.GREEN; valueTextSize = 12f}

        // Group the data sets
        val data = BarData(spendingDataSet, savingDataSet)
        data.barWidth = 0.3f // Width of each individual bar

        barChart.data = data
        barChart.description.isEnabled = false // Hide the description

        barChart.legend.textSize = 14f
        barChart.legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        barChart.legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        barChart.legend.orientation = Legend.LegendOrientation.HORIZONTAL
        barChart.legend.yOffset = 20f
        barChart.setExtraOffsets(0f, 0f, 0f, 10f)

        // Set X-axis labels and format
        val xAxis = barChart.xAxis
        xAxis.textSize = 14f
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.axisMinimum = 0.0f
        xAxis.granularity = 1.0f
        xAxis.setCenterAxisLabels(true)

        // Configure Y-axis
        val leftAxis = barChart.axisLeft
        leftAxis.textSize = 14f
        leftAxis.axisMinimum = 0f // Start Y-axis at zero
        barChart.axisRight.isEnabled = false // Disable right Y-axis

        // Adjust the spacing between bars (group spacing)
        barChart.groupBars(0.0f, 0.4f, 0f)

        // Refresh chart with new data
        barChart.invalidate()
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
            .sumOf { convertAmount(it.amount, it.currency, targetCurrency, result!!.rates!!) }

        val outgoingSum = transactions
            .filter { !it.isIncoming }
            .sumOf { convertAmount(it.amount, it.currency, targetCurrency, result!!.rates!!) }

        return Pair(incomingSum.toFloat(), outgoingSum.toFloat())
    }

    override fun onRatesReceived(result: ExchangeResult) {
        this.result = result
        initTransactions()
    }

    override fun onError(error: Throwable) {
        //TODO
    }
}