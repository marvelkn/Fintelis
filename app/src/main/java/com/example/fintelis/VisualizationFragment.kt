package com.example.fintelis

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.fintelis.data.Transaction
import com.example.fintelis.data.TransactionType
import com.example.fintelis.viewmodel.TransactionViewModel
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class VisualizationFragment : Fragment() {

    private val viewModel: TransactionViewModel by activityViewModels()

    // --- Components Chart ---
    private lateinit var pieChart: PieChart
    private lateinit var lineChart: LineChart

    // --- Containers (Layout Baru) ---
    private lateinit var layoutCharts: LinearLayout
    private lateinit var layoutEmptyState: LinearLayout

    // --- TextViews Summary ---
    private lateinit var tvTopExpense: TextView
    private lateinit var tvTotalIncome: TextView
    private lateinit var tvTotalExpense: TextView

    // --- Navigation Controls (Fitur Baru) ---
    private lateinit var btnPrevMonth: ImageButton
    private lateinit var btnNextMonth: ImageButton
    private lateinit var tvMonthName: TextView
    private lateinit var tvYearNumber: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_visualization, container, false)

        // 1. Inisialisasi View (Sesuai XML Baru)
        pieChart = view.findViewById(R.id.pieChart)
        lineChart = view.findViewById(R.id.lineChart)

        layoutCharts = view.findViewById(R.id.layoutCharts)
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState)

        tvTopExpense = view.findViewById(R.id.tvTopExpense)
        tvTotalIncome = view.findViewById(R.id.tvTotalIncome)
        tvTotalExpense = view.findViewById(R.id.tvTotalExpense)

        btnPrevMonth = view.findViewById(R.id.btnPrevMonth)
        btnNextMonth = view.findViewById(R.id.btnNextMonth)
        tvMonthName = view.findViewById(R.id.tvMonthName)
        tvYearNumber = view.findViewById(R.id.tvYearNumber)

        // 2. Setup Logic
        setupCharts()          // Menggunakan Styling Kode Lama
        setupMonthNavigation() // Menggunakan Logika Kode Baru
        observeData()          // Menggunakan Logika Gabungan (Visibility)

        return view
    }

    // --- SETUP NAVIGASI BULAN (DARI KODE BARU) ---
    private fun setupMonthNavigation() {
        btnPrevMonth.setOnClickListener { viewModel.changeMonth(-1) }
        btnNextMonth.setOnClickListener { viewModel.changeMonth(1) }

        viewModel.currentMonth.observe(viewLifecycleOwner) { calendar ->
            val fmtMonth = SimpleDateFormat("MMMM", Locale.US)
            val fmtYear = SimpleDateFormat("yyyy", Locale.US)
            tvMonthName.text = fmtMonth.format(calendar.time)
            tvYearNumber.text = fmtYear.format(calendar.time)
        }
    }

    // --- SETUP CHARTS (DARI KODE LAMA - STYLING DETAIL) ---
    private fun setupCharts() {
        // 1. SETUP PIE CHART (Donut Style Pro)
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

        // Matikan Legend
        pieChart.legend.isEnabled = false
        pieChart.animateY(1400, Easing.EaseInOutQuad)

        // 2. SETUP LINE CHART (Clean & Minimalist - Kode Lama)
        lineChart.description.isEnabled = false
        lineChart.setDrawGridBackground(false)
        lineChart.axisRight.isEnabled = false

        // Sumbu Kiri (Y-Axis) - Format Angka Singkat
        val leftAxis = lineChart.axisLeft
        leftAxis.textColor = Color.parseColor("#718096")
        leftAxis.setDrawGridLines(true)
        leftAxis.enableGridDashedLine(10f, 10f, 0f) // Garis putus-putus
        leftAxis.gridColor = Color.parseColor("#E2E8F0")
        leftAxis.axisMinimum = 0f

        // Custom Formatter dari Kode Lama
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
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f

        // Formatter Tanggal
        xAxis.valueFormatter = object : ValueFormatter() {
            private val format = SimpleDateFormat("dd MMM", Locale.US)
            override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                return try { format.format(Date(value.toLong())) } catch(e: Exception) { "" }
            }
        }

        // Marker View (Tetap dipasang sesuai kode lama)
        // Pastikan layout 'custom_marker_view' ada di project Anda
        try {
            val markerView = CustomMarkerView(requireContext(), R.layout.custom_marker_view)
            markerView.chartView = lineChart
            lineChart.marker = markerView
        } catch (e: Exception) {
            // Fallback jika CustomMarkerView belum dibuat class-nya
        }
    }

    // --- OBSERVE DATA (GABUNGAN LOGIKA) ---
    private fun observeData() {
        viewModel.displayedTransactions.observe(viewLifecycleOwner) { transactions ->
            if (transactions != null) {
                // Update teks ringkasan selalu
                updateSummaryText(transactions)

                if (transactions.isNotEmpty()) {
                    // Tampilkan grafik, sembunyikan empty state
                    layoutCharts.isVisible = true
                    layoutEmptyState.isVisible = false

                    updatePieChart(transactions)
                    updateLineChart(transactions)
                } else {
                    // Sembunyikan grafik, tampilkan empty state
                    layoutCharts.isVisible = false
                    layoutEmptyState.isVisible = true

                    pieChart.clear()
                    lineChart.clear()
                }
            }
        }
    }

    // --- UPDATE TEXT (DARI KODE LAMA) ---
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
            tvTopExpense.text = "${maxEntry.key} (${formatRp.format(maxEntry.value)})"
            tvTopExpense.isVisible = true
        } else {
            tvTopExpense.text = "Belum ada pengeluaran"
            // Opsional: Sembunyikan jika tidak ada expense, sesuai selera
            // tvTopExpense.isVisible = false
        }
    }

    // --- UPDATE PIE CHART (DARI KODE LAMA) ---
    private fun updatePieChart(transactions: List<Transaction>) {
        val entries = viewModel.getExpenseByCategoryData(transactions)
        if (entries.isEmpty()) { pieChart.clear(); return }

        val dataSet = PieDataSet(entries, "")

        // WARNA MODERN (Kode Lama)
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
        dataSet.valueLineColor = Color.parseColor("#718096")

        dataSet.valueTextColor = Color.parseColor("#1A202C")
        dataSet.valueTextSize = 11f
        dataSet.sliceSpace = 2f

        val data = PieData(dataSet)
        data.setValueFormatter(PercentFormatter(pieChart))
        pieChart.data = data

        // Highlight nilai tengah
        val totalExpense = entries.sumOf { it.value.toDouble() }
        val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        pieChart.centerText = "Total\n${formatRp.format(totalExpense)}"

        pieChart.invalidate()
    }

    // --- UPDATE LINE CHART (DARI KODE LAMA) ---
    private fun updateLineChart(transactions: List<Transaction>) {
        val entries = viewModel.getFinancialTrendData(transactions)

        // Sorting agar grafik tidak berantakan
        val sortedEntries = entries.sortedBy { it.x }

        if (sortedEntries.isEmpty()) { lineChart.clear(); return }

        val dataSet = LineDataSet(sortedEntries, "Saldo")
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        dataSet.cubicIntensity = 0.2f
        dataSet.color = ContextCompat.getColor(requireContext(), R.color.chart_line)
        dataSet.lineWidth = 2f
        dataSet.setDrawCircles(false)
        dataSet.setDrawValues(false)
        dataSet.setDrawFilled(true)
        dataSet.fillDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.gradient_chart)
        dataSet.isHighlightEnabled = true
        dataSet.highLightColor = Color.GRAY

        val data = LineData(dataSet)
        lineChart.data = data
        lineChart.invalidate()
        lineChart.animateX(1000)
    }
}