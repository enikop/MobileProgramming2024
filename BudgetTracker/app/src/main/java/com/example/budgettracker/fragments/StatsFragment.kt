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
import com.example.budgettracker.model.Currency
import com.example.budgettracker.model.ExchangeResult
import com.example.budgettracker.model.Rates
import com.example.budgettracker.model.Transaction
import com.example.budgettracker.network.ExchangeService
import com.example.budgettracker.network.RatesCallback
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.android.synthetic.main.fragment_stats.view.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale


class StatsFragment : Fragment(), RatesCallback {
    private var result: ExchangeResult? = null
    private lateinit var spendingData: List<Float>
    private lateinit var savingsData: List<Float>
    private lateinit var labels: List<String>

    private lateinit var barChart: BarChart
    private lateinit var spDiagramCurrency: Spinner

    private var monthsShown = 12
    private var chartTextSize = 15f
    private var exchangeService = ExchangeService()
    private var monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
    private var selectedCurrency = Currency.EUR

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_stats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        barChart = view.barChart
        spDiagramCurrency = view.spDiagramCurrency
        barChart.setNoDataText("Loading diagram...")
        barChart.setNoDataTextColor(Color.BLACK)
        val currencies = Currency.values().map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, currencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        val selectionPosition = adapter.getPosition(selectedCurrency.name)
        spDiagramCurrency.adapter = adapter
        spDiagramCurrency.setSelection(selectionPosition)
        spDiagramCurrency.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (result == null) exchangeService.getRates(this@StatsFragment)
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
            savingsData = stats.map { it.first }

            activity?.runOnUiThread {
                setupChart()
            }
        }
        dbThread.start()
    }

    private fun setupChart() {
        val spendingEntries = spendingData.mapIndexed { index, value -> BarEntry(index.toFloat(), value) }
        val savingEntries = savingsData.mapIndexed { index, value -> BarEntry(index.toFloat(), value) }


        val spendingDataSet = BarDataSet(spendingEntries, "Spending ("+selectedCurrency.name+")").apply {
            color = ContextCompat.getColor(requireContext(), R.color.spendingBarColour)
            valueTextSize = chartTextSize
        }
        val savingDataSet = BarDataSet(savingEntries, "Income ("+selectedCurrency.name+")").apply {
            color = ContextCompat.getColor(requireContext(), R.color.incomeBarColour)
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
        if(result.success != null && result.success) {
            this.result = result
            initTransactions()
        } else {
            barChart.setNoDataText("The diagram could not access the current exchange rates.")
            barChart.invalidate()
        }
    }

    override fun onError(error: Throwable) {
        barChart.setNoDataText("The diagram could not access the current exchange rates.")
        barChart.invalidate()
    }
}