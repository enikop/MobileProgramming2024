package com.example.budgettracker

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
    private var result: ExchangeResult? = null
    private lateinit var spendingData: List<Float>
    private lateinit var savingsData: List<Float>
    private lateinit var labels: List<String>

    private var monthsShown = 12
    private var chartTextSize = 15f
    private var exchangeService = ExchangeService()
    private var monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
    private var selectedCurrency = Currency.EUR

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)
        setSupportActionBar(toolbar)
        barChart.setNoDataText("Loading diagram...")
        barChart.setNoDataTextColor(Color.BLACK)
        btnMain.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        val currencies = Currency.values().map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        val selectionPosition = adapter.getPosition(selectedCurrency.name)
        spDiagramCurrency.adapter = adapter
        spDiagramCurrency.setSelection(selectionPosition)
        spDiagramCurrency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if(result == null) exchangeService.getRates(this@StatsActivity)
                else {
                    val selectedCurrencyName = spDiagramCurrency.selectedItem as String
                    selectedCurrency = Currency.valueOf(selectedCurrencyName)
                    initTransactions()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Optional: handle the case where no item is selected, if needed
            }
        }
    }

    private fun initTransactions() {
        val dbThread = Thread {
            //Last year
            val today = LocalDate.now()
            val months = (monthsShown-1 downTo 0).map {
                today.minusMonths(it.toLong()).format(monthFormatter)
            }
            labels = (monthsShown-1 downTo 0).map {
                today.month.minus(it.toLong()).getDisplayName(TextStyle.SHORT, Locale.getDefault())
            }
            val stats = mutableListOf<Pair<Float, Float>>()
            for(i in 0 until monthsShown) {
                val transactions = AppDatabase.getInstance(this).transactionDao().findTransactionsForMonth(months[i])
                stats.add(calculateMonthlyStat(transactions, selectedCurrency))
            }

            spendingData = stats.map{ it.second }
            savingsData = stats.map{ it.first }

            runOnUiThread{
                setupChart()
            }
        }
        dbThread.start()
    }

    private fun setupChart() {

        val spendingEntries = spendingData.mapIndexed { index, value -> BarEntry(index.toFloat(), value) }
        val savingEntries = savingsData.mapIndexed { index, value -> BarEntry(index.toFloat(), value) }


        val spendingDataSet = BarDataSet(spendingEntries, "Spending ("+selectedCurrency.name+")").apply {
            color = ContextCompat.getColor(this@StatsActivity, R.color.spendingBarColour)
            valueTextSize = chartTextSize
        }
        val savingDataSet = BarDataSet(savingEntries, "Income ("+selectedCurrency.name+")").apply {
            color = ContextCompat.getColor(this@StatsActivity, R.color.incomeBarColour)
            valueTextSize = chartTextSize
        }

        // Group the data sets
        val data = BarData(spendingDataSet, savingDataSet)
        data.barWidth = 0.3f

        barChart.data = data
        barChart.description.isEnabled = false

        //Legend format
        barChart.legend.textSize = chartTextSize * 1.2f
        barChart.legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        barChart.legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
        barChart.legend.orientation = Legend.LegendOrientation.HORIZONTAL
        barChart.legend.yOffset = 20f
        barChart.setExtraOffsets(0f, 0f, 0f, 10f)

        // Enable scaling and dragging
        barChart.setScaleEnabled(true)
        barChart.isDragEnabled = true

        // X-axis labels and format
        val xAxis = barChart.xAxis
        xAxis.textSize = chartTextSize * 1.2f
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.axisMinimum = 0.0f
        xAxis.axisMaximum = monthsShown.toFloat()
        xAxis.granularity = 1.0f
        xAxis.setCenterAxisLabels(true)
        barChart.setVisibleXRangeMinimum(3f)
        barChart.setVisibleXRangeMaximum(3f)

        // Y-axis format
        val leftAxis = barChart.axisLeft
        leftAxis.textSize = chartTextSize * 1.2f
        leftAxis.axisMinimum = 0f // Start Y-axis at zero
        barChart.axisRight.isEnabled = false // Disable right Y-axis

        // Spacing between bars
        barChart.groupBars(0.0f, 0.4f, 0f)

        barChart.invalidate()

        //Scroll to this month
        val lastIndex = savingEntries.size
        barChart.moveViewToX(lastIndex.toFloat() - 1)
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
        barChart.setNoDataText("The diagram could not access the current exchange rates.")
    }
}