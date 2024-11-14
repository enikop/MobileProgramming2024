package com.example.budgettracker.helpers

import com.example.budgettracker.model.Currency
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class MonthlyBarChartBuilder(
    private val barChart: BarChart,
    private val chartTextSize : Float,
    private val spendingColour: Int,
    private val incomeColour: Int
) {
    fun setNoDataText(text:String, colour: Int) {
        barChart.setNoDataText(text)
        barChart.setNoDataTextColor(colour)
        barChart.invalidate()
    }
    fun build(spendingData: List<Float>, incomeData: List<Float>,
              labels: List<String>, currency: Currency) {
        val spendingEntries = spendingData.mapIndexed { index, value -> BarEntry(index.toFloat(), value) }
        val incomeEntries = incomeData.mapIndexed { index, value -> BarEntry(index.toFloat(), value) }


        val spendingDataSet = BarDataSet(spendingEntries, "Spending ("+currency.name+")").apply {
            color = spendingColour
            valueTextSize = chartTextSize
        }
        val incomeDataSet = BarDataSet(incomeEntries, "Income ("+currency.name+")").apply {
            color = incomeColour
            valueTextSize = chartTextSize
        }

        val data = BarData(spendingDataSet, incomeDataSet)
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
        xAxis.axisMaximum = spendingData.size.toFloat()
        xAxis.granularity = 1.0f
        xAxis.setCenterAxisLabels(true)
        barChart.setVisibleXRangeMinimum(3f)
        barChart.setVisibleXRangeMaximum(3f)

        // Y-axis format
        val leftAxis = barChart.axisLeft
        leftAxis.textSize = chartTextSize * 1.2f
        leftAxis.axisMinimum = 0f
        barChart.axisRight.isEnabled = false

        // Spacing between bars
        barChart.groupBars(0.0f, 0.4f, 0f)

        barChart.invalidate()

        //Scroll to this month
        val lastIndex = incomeEntries.size
        barChart.moveViewToX(lastIndex.toFloat() - 1)
    }
}