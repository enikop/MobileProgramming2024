package com.example.budgettracker

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import com.example.budgettracker.adapter.BudgetTrackerAdapter
import com.example.budgettracker.database.AppDatabase
import com.example.budgettracker.dialog.TransactionDialog
import com.example.budgettracker.model.ExchangeResult
import com.example.budgettracker.model.Transaction
import com.example.budgettracker.network.ExchangeService
import com.example.budgettracker.network.RatesCallback
import com.example.budgettracker.touch.TransactionTouchHelperCallback
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_stats.*
import kotlinx.android.synthetic.main.activity_stats.toolbar
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*

class StatsActivity: AppCompatActivity(), RatesCallback {
    private lateinit var transactions: List<Transaction>
    private var exchangeService = ExchangeService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)
        setSupportActionBar(toolbar)
        //Új elem hozzáadásakor hívódik meg, a rózsaszín levél ikonos gomb eseménykezelője
        //A ShoppingTimeDialog-ot hívja meg (jeleníti meg)
        btnMain.setOnClickListener {
            // Create an Intent to start StatsActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        //exchangeService.getRates(this)

        initTransactions()
        setupChart()
    }

    private fun initTransactions() {
        val dbThread = Thread {
            transactions = AppDatabase.getInstance(this).transactionDao().findTransactionsForMonth("2024-11")
            runOnUiThread{
                tvRates.text = transactions.size.toString();
            }
        }
        dbThread.start()
    }

    private fun setupChart() {
        // Create sample data
        val currentMonth = LocalDate.now().month
        val labels = (2 downTo 0).map {
            currentMonth.minus(it.toLong()).getDisplayName(TextStyle.SHORT, Locale.getDefault())
        }

        // Sample data for spendings and savings
        val spendingsData = listOf(500f, 450f, 600f) // Example spending values
        val savingsData = listOf(200f, 300f, 250f) // Example saving values

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

    override fun onRatesReceived(result: ExchangeResult) {
        tvRates.text = result!!.rates!!.HUF.toString()
    }

    override fun onError(error: Throwable) {
        tvRates.text = error.localizedMessage
    }
}