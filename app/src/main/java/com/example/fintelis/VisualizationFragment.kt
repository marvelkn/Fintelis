package com.example.fintelis

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.fintelis.data.Transaction
import com.example.fintelis.data.TransactionType
import com.example.fintelis.viewmodel.TransactionViewModel
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class VisualizationFragment : Fragment() {

    private val viewModel: TransactionViewModel by activityViewModels()

    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart
    private lateinit var lineChart: LineChart

    // Teks Summary
    private lateinit var tvTopExpense: TextView
    private lateinit var tvTotalIncome: TextView
    private lateinit var tvTotalExpense: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_visualization, container, false)

        pieChart = view.findViewById(R.id.pieChart)
        barChart = view.findViewById(R.id.barChart)
        lineChart = view.findViewById(R.id.lineChart)

        tvTopExpense = view.findViewById(R.id.tvTopExpense)
        tvTotalIncome = view.findViewById(R.id.tvTotalIncome)
        tvTotalExpense = view.findViewById(R.id.tvTotalExpense)

        setupCharts()
        observeData()

        return view
    }

    private fun setupCharts() {
        // ==========================================================
        // 1. SETUP PIE CHART (Donut Style Pro)
        // ==========================================================
        pieChart.setUsePercentValues(true)
        pieChart.description.isEnabled = false
        // Jarak agar label luar tidak terpotong
        pieChart.setExtraOffsets(20f, 0f, 20f, 0f)

        // Setting Lubang Donut
        pieChart.dragDecelerationFrictionCoef = 0.95f
        pieChart.isDrawHoleEnabled = true
        pieChart.setHoleColor(Color.WHITE)
        pieChart.transparentCircleRadius = 61f
        pieChart.holeRadius = 58f

        // Teks di tengah Donut
        pieChart.setDrawCenterText(true)
        pieChart.centerText = "Total\nPengeluaran"
        pieChart.setCenterTextSize(12f)
        pieChart.setCenterTextColor(Color.parseColor("#718096"))

        // Matikan Legend (Keterangan warna di bawah)
        pieChart.legend.isEnabled = false

        // Animasi
        pieChart.animateY(1400, Easing.EaseInOutQuad)


        // ==========================================================
        // 2. SETUP LINE CHART (Clean & Minimalist)
        // ==========================================================
        lineChart.description.isEnabled = false
        lineChart.setDrawGridBackground(false)

        // Sumbu Kanan dimatikan
        lineChart.axisRight.isEnabled = false

        // Sumbu Kiri (Y-Axis) - Format Angka Singkat
        val leftAxis = lineChart.axisLeft
        leftAxis.textColor = Color.parseColor("#718096")
        leftAxis.setDrawGridLines(true)
        leftAxis.enableGridDashedLine(10f, 10f, 0f) // Garis putus-putus
        leftAxis.gridColor = Color.parseColor("#E2E8F0")
        leftAxis.axisMinimum = 0f // Opsional, agar grafik tidak melayang

        // Custom Formatter: Ubah 1000000 jadi "1jt"
        leftAxis.valueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                return if (value >= 1000000) {
                    String.format("%.0fjt", value / 1000000)
                } else if (value >= 1000) {
                    String.format("%.0frb", value / 1000)
                } else {
                    String.format("%.0f", value)
                }
            }
        }

        // Sumbu Bawah (X-Axis)
        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.textColor = Color.parseColor("#718096")
        xAxis.setDrawGridLines(false) // Bersih dari garis vertikal
        xAxis.granularity = 1f

        // Formatter Tanggal
        xAxis.valueFormatter = object : ValueFormatter() {
            private val format = SimpleDateFormat("dd MMM", Locale.US)
            override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                // Pastikan value valid timestamp
                return try { format.format(Date(value.toLong())) } catch(e: Exception) { "" }
            }
        }

        // Marker View (Popup saat ditekan) tetap dipasang
        val markerView = CustomMarkerView(requireContext(), R.layout.custom_marker_view)
        markerView.chartView = lineChart
        lineChart.marker = markerView


        // ==========================================================
        // 3. SETUP BAR CHART
        // ==========================================================
        barChart.description.isEnabled = false
        barChart.setDrawGridBackground(false)
        barChart.setPinchZoom(false)
        barChart.axisRight.isEnabled = false

        barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        barChart.xAxis.setDrawGridLines(false)
        barChart.xAxis.textColor = Color.parseColor("#718096")

        barChart.axisLeft.textColor = Color.parseColor("#718096")
        barChart.axisLeft.enableGridDashedLine(10f, 10f, 0f)
        barChart.axisLeft.gridColor = Color.parseColor("#E2E8F0")

        // Pindahkan Legend Bar Chart ke atas kanan
        barChart.legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        barChart.legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
        barChart.legend.orientation = Legend.LegendOrientation.HORIZONTAL
        barChart.legend.setDrawInside(true)
        barChart.legend.yOffset = 0f
        barChart.legend.xOffset = 10f
        barChart.legend.yEntrySpace = 0f
        barChart.legend.textSize = 10f
    }

    private fun observeData() {
        viewModel.displayedTransactions.observe(viewLifecycleOwner) { transactions ->
            if (transactions != null && transactions.isNotEmpty()) {
                updatePieChart(transactions)
                updateBarChart(transactions)
                updateLineChart(transactions)
                updateSummaryText(transactions) // Fungsi baru untuk update teks
            } else {
                pieChart.clear()
                barChart.clear()
                lineChart.clear()
            }
        }
    }

    // Logic baru: Update teks ringkasan
    private fun updateSummaryText(transactions: List<Transaction>) {
        val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

        // Hitung Total
        val totalInc = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExp = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        tvTotalIncome.text = formatRp.format(totalInc)
        tvTotalExpense.text = formatRp.format(totalExp)

        // Cari kategori paling boros
        val expenseMap = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val maxEntry = expenseMap.maxByOrNull { it.value }
        if (maxEntry != null) {
            tvTopExpense.text = "Terboros: ${maxEntry.key} (${formatRp.format(maxEntry.value)})"
        } else {
            tvTopExpense.text = "Belum ada pengeluaran"
        }
    }

    private fun updatePieChart(transactions: List<Transaction>) {
        val entries = viewModel.getExpenseByCategoryData(transactions)
        if (entries.isEmpty()) { pieChart.clear(); return }

        val dataSet = PieDataSet(entries, "")

        // WARNA MODERN
        dataSet.colors = listOf(
            Color.parseColor("#4C6EF5"), // Indigo
            Color.parseColor("#22B8CF"), // Cyan
            Color.parseColor("#FAB005"), // Kuning
            Color.parseColor("#FF6B6B"), // Merah
            Color.parseColor("#BE4BDB")  // Ungu
        )

        // CONFIG LABEL DI LUAR (OUTSIDE)
        dataSet.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        dataSet.xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        dataSet.valueLinePart1OffsetPercentage = 80f
        dataSet.valueLinePart1Length = 0.4f
        dataSet.valueLinePart2Length = 0.4f
        dataSet.valueLineWidth = 1f
        dataSet.valueLineColor = Color.parseColor("#718096") // Warna garis penunjuk

        // Text Styling
        dataSet.valueTextColor = Color.parseColor("#1A202C") // Warna teks angka hitam
        dataSet.valueTextSize = 11f
        dataSet.sliceSpace = 2f

        val data = PieData(dataSet)
        data.setValueFormatter(PercentFormatter(pieChart))
        pieChart.data = data

        // Highlight nilai tengah (Total Amount)
        val totalExpense = entries.sumOf { it.value.toDouble() }
        val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        pieChart.centerText = "Total\n${formatRp.format(totalExpense)}"

        pieChart.invalidate()
    }

    private fun updateLineChart(transactions: List<Transaction>) {
        val entries = viewModel.getFinancialTrendData(transactions)
        if (entries.isEmpty()) { lineChart.clear(); return }

        val dataSet = LineDataSet(entries, "Saldo")
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        dataSet.cubicIntensity = 0.2f
        dataSet.color = ContextCompat.getColor(requireContext(), R.color.chart_line)
        dataSet.lineWidth = 2f
        dataSet.setDrawCircles(false)
        dataSet.setDrawValues(false)
        dataSet.setDrawFilled(true)
        dataSet.fillDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.gradient_chart)
        // Penting: Aktifkan highlight agar marker muncul saat disentuh
        dataSet.isHighlightEnabled = true
        dataSet.highLightColor = Color.GRAY

        val data = LineData(dataSet)
        lineChart.data = data
        lineChart.invalidate()
        lineChart.animateX(1000)
    }

    private fun updateBarChart(transactions: List<Transaction>) {
        val (labels, entries) = viewModel.getIncomeExpenseBarData(transactions)
        if (entries.isEmpty()) { barChart.clear(); return }

        val dataSet = BarDataSet(entries, "")
        dataSet.colors = listOf(
            ContextCompat.getColor(requireContext(), R.color.chart_income),
            ContextCompat.getColor(requireContext(), R.color.chart_expense)
        )
        dataSet.stackLabels = arrayOf("In", "Out") // Label lebih pendek agar muat
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 10f

        val data = BarData(dataSet)
        data.barWidth = 0.5f

        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        barChart.data = data
        barChart.setFitBars(true)
        barChart.invalidate()
        barChart.animateY(1000)
    }
}