package com.example.fintelis

import android.animation.LayoutTransition
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
import androidx.navigation.fragment.findNavController
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
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.material.button.MaterialButton
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class VisualizationFragment : Fragment() {

    private val viewModel: TransactionViewModel by activityViewModels()

    // --- Components Chart ---
    private lateinit var pieChart: PieChart
    private lateinit var lineChart: LineChart

    // --- Components Button ---
    private lateinit var btnSeeDetail: MaterialButton

    // --- Containers ---
    private lateinit var layoutCharts: LinearLayout
    private lateinit var layoutEmptyState: LinearLayout

    // --- TextViews Summary ---
    private lateinit var tvTopCategory: TextView
    private lateinit var tvTotalIncome: TextView
    private lateinit var tvTotalExpense: TextView

    // --- Navigation Controls ---
    private lateinit var btnPrevMonth: ImageButton
    private lateinit var btnNextMonth: ImageButton
    private lateinit var tvMonthName: TextView
    private lateinit var tvYearNumber: TextView

    // --- Switch Controls ---
    private lateinit var btnSwitchExpense: TextView
    private lateinit var btnSwitchIncome: TextView

    // --- State Variables ---
    private var isExpenseMode = true // Default to Expense
    private var currentTransactions: List<Transaction> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_visualization, container, false)
        initializeViews(view)

        setupCharts()
        setupMonthNavigation()
        setupCategorySwitch()
        setupDetailButton() // Logic Navigasi Baru
        observeData()

        return view
    }

    private fun initializeViews(view: View) {
        pieChart = view.findViewById(R.id.pieChart)
        lineChart = view.findViewById(R.id.lineChart)
        btnSeeDetail = view.findViewById(R.id.btnSeeDetail)

        layoutCharts = view.findViewById(R.id.layoutCharts)
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState)

        tvTopCategory = view.findViewById(R.id.tvTopCategory)
        tvTotalIncome = view.findViewById(R.id.tvTotalIncome)
        tvTotalExpense = view.findViewById(R.id.tvTotalExpense)

        btnPrevMonth = view.findViewById(R.id.btnPrevMonth)
        btnNextMonth = view.findViewById(R.id.btnNextMonth)
        tvMonthName = view.findViewById(R.id.tvMonthName)
        tvYearNumber = view.findViewById(R.id.tvYearNumber)

        btnSwitchExpense = view.findViewById(R.id.btnSwitchExpense)
        btnSwitchIncome = view.findViewById(R.id.btnSwitchIncome)

        // Animasi layout (opsional, untuk kehalusan jika ada perubahan visibilitas)
        layoutCharts.layoutTransition = LayoutTransition()
    }

    // --- LOGIC TOMBOL DETAIL (NAVIGASI KE HALAMAN BARU) ---
    private fun setupDetailButton() {
        updateDetailButtonUI()

        btnSeeDetail.setOnClickListener {
            // Siapkan data yang mau dikirim
            val bundle = Bundle().apply {
                putBoolean("isExpense", isExpenseMode)
            }

            // Pindah halaman menggunakan NavController (Sesuai ID di nav_graph_dash.xml)
            findNavController().navigate(R.id.action_visualization_to_detailCashflow, bundle)
        }
    }

    // Update Teks dan Warna tombol berdasarkan mode
    private fun updateDetailButtonUI() {
        val typeText = if (isExpenseMode) "Expense" else "Income"
        btnSeeDetail.text = "View Full $typeText List"

        // Icon Panah ke Kanan (Indikasi pindah halaman)
        btnSeeDetail.setIconResource(R.drawable.ic_chevron_right)

        if (isExpenseMode) {
            btnSeeDetail.setBackgroundColor(Color.parseColor("#C53030")) // Merah
        } else {
            btnSeeDetail.setBackgroundColor(Color.parseColor("#38A169")) // Hijau
        }
    }

    // --- LOGIC SWITCH CATEGORY ---
    private fun setupCategorySwitch() {
        btnSwitchExpense.setOnClickListener {
            if (!isExpenseMode) {
                isExpenseMode = true
                updateSwitchUI()
                updatePieChart(currentTransactions)
                updateDetailButtonUI() // Update tombol agar merah
            }
        }

        btnSwitchIncome.setOnClickListener {
            if (isExpenseMode) {
                isExpenseMode = false
                updateSwitchUI()
                updatePieChart(currentTransactions)
                updateDetailButtonUI() // Update tombol agar hijau
            }
        }
    }

    private fun updateSwitchUI() {
        if (isExpenseMode) {
            // Expense Active
            btnSwitchExpense.setBackgroundResource(R.drawable.bg_pill_danger); btnSwitchExpense.setTextColor(Color.WHITE)
            btnSwitchIncome.setBackgroundColor(Color.TRANSPARENT); btnSwitchIncome.setTextColor(Color.parseColor("#718096"))
        } else {
            // Income Active
            btnSwitchExpense.setBackgroundColor(Color.TRANSPARENT); btnSwitchExpense.setTextColor(Color.parseColor("#718096"))
            btnSwitchIncome.setBackgroundResource(R.drawable.bg_pill_success); btnSwitchIncome.setTextColor(Color.WHITE)
        }
    }

    // --- OBSERVE DATA ---
    private fun observeData() {
        viewModel.displayedTransactions.observe(viewLifecycleOwner) { transactions ->
            if (transactions != null) {
                currentTransactions = transactions
                updateSummaryText(transactions)

                if (transactions.isNotEmpty()) {
                    layoutCharts.isVisible = true
                    layoutEmptyState.isVisible = false

                    updatePieChart(transactions)
                    updateLineChart(transactions)

                    // Button selalu terlihat jika ada data
                    btnSeeDetail.isVisible = true

                } else {
                    layoutCharts.isVisible = false
                    layoutEmptyState.isVisible = true
                    pieChart.clear()
                    lineChart.clear()

                    // Sembunyikan tombol detail jika tidak ada data
                    btnSeeDetail.isVisible = false
                }
            }
        }
    }

    // --- HELPER FUNCTIONS (Sama seperti sebelumnya) ---

    private fun updateSummaryText(transactions: List<Transaction>) {
        val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        val totalInc = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExp = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        tvTotalIncome.text = formatRp.format(totalInc)
        tvTotalExpense.text = formatRp.format(totalExp)
    }

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

    private fun setupCharts() {
        // ... (Kode Pie Chart biarkan saja seperti sebelumnya) ...
        // Konfigurasi Pie Chart tetap sama...
        pieChart.setUsePercentValues(true); pieChart.description.isEnabled = false
        pieChart.setExtraOffsets(30f, 10f, 30f, 10f)
        pieChart.dragDecelerationFrictionCoef = 0.95f; pieChart.isDrawHoleEnabled = true
        pieChart.setHoleColor(Color.WHITE); pieChart.transparentCircleRadius = 61f; pieChart.holeRadius = 58f
        pieChart.setDrawCenterText(true); pieChart.setCenterTextSize(12f); pieChart.setCenterTextColor(Color.parseColor("#718096"))
        pieChart.legend.isEnabled = false; pieChart.setDrawEntryLabels(true)
        pieChart.setEntryLabelColor(Color.parseColor("#2D3748")); pieChart.setEntryLabelTextSize(11f)
        pieChart.animateY(1400, Easing.EaseInOutQuad)

        // --- KONFIGURASI LINE CHART (BARU) ---
        lineChart.description.isEnabled = false
        lineChart.legend.isEnabled = false // Hilangkan legend kotak di bawah (biar bersih)

        // Hapus border kotak di sekeliling chart
        lineChart.setDrawGridBackground(false)
        lineChart.setDrawBorders(false)

        // --- Sumbu X (Bawah) ---
        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.textColor = Color.parseColor("#A0AEC0") // Abu-abu lembut
        xAxis.textSize = 10f
        xAxis.setDrawGridLines(false) // Hilangkan garis grid vertikal
        xAxis.setDrawAxisLine(false)  // Hilangkan garis sumbu hitam
        xAxis.granularity = 1f
        xAxis.yOffset = 10f // Beri jarak sedikit dari grafik
        xAxis.valueFormatter = object : ValueFormatter() {
            private val format = SimpleDateFormat("dd", Locale.US) // Tampilkan Tanggal saja (lebih singkat)
            override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                return try { format.format(Date(value.toLong())) } catch(e: Exception) { "" }
            }
        }

        // --- Sumbu Y Kiri (Nominal) ---
        val leftAxis = lineChart.axisLeft
        leftAxis.textColor = Color.parseColor("#A0AEC0")
        leftAxis.textSize = 10f
        leftAxis.setDrawGridLines(true) // Tetap tampilkan grid horizontal
        leftAxis.gridColor = Color.parseColor("#F7FAFC") // Grid warna sangat muda (hampir putih)
        leftAxis.enableGridDashedLine(10f, 10f, 0f) // Grid putus-putus
        leftAxis.setDrawAxisLine(false) // Hilangkan garis sumbu vertikal
        leftAxis.axisMinimum = 0f // Mulai dari 0

        // Custom Formatter (Jt/Rb)
        leftAxis.valueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                return if (value >= 1000000) String.format("%.0fjt", value / 1000000)
                else if (value >= 1000) String.format("%.0frb", value / 1000)
                else String.format("%.0f", value)
            }
        }

        // Matikan Sumbu Kanan
        lineChart.axisRight.isEnabled = false

        // Interaksi
        lineChart.setTouchEnabled(true)
        lineChart.isDragEnabled = true
        lineChart.setScaleEnabled(false) // Matikan zoom agar tampilan tetap rapi
        lineChart.setPinchZoom(false)

        // Marker (Tooltip saat ditekan)
        try {
            val markerView = CustomMarkerView(requireContext(), R.layout.custom_marker_view)
            markerView.chartView = lineChart
            lineChart.marker = markerView
        } catch (e: Exception) { }
    }

    private fun updatePieChart(transactions: List<Transaction>) {
        val targetType = if (isExpenseMode) TransactionType.EXPENSE else TransactionType.INCOME
        val filteredList = transactions.filter { it.type == targetType }
        val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        pieChart.highlightValues(null)

        if (filteredList.isEmpty()) { pieChart.clear(); pieChart.centerText = if(isExpenseMode) "No Expense" else "No Income"; tvTopCategory.text = "-"; return }

        val totalAmount = filteredList.sumOf { it.amount }
        val groupedData = filteredList.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.amount } }
        val maxEntry = groupedData.maxByOrNull { it.value }
        if (maxEntry != null) { tvTopCategory.text = "${maxEntry.key} (${formatRp.format(maxEntry.value)})" }

        val entries = groupedData.map { PieEntry(it.value.toFloat(), it.key) }
        val dataSet = PieDataSet(entries, "")
        if (isExpenseMode) {
            dataSet.colors = listOf(Color.parseColor("#E53E3E"), Color.parseColor("#ED8936"), Color.parseColor("#ECC94B"), Color.parseColor("#805AD5"), Color.parseColor("#38B2AC"))
        } else {
            dataSet.colors = listOf(Color.parseColor("#38A169"), Color.parseColor("#3182CE"), Color.parseColor("#319795"), Color.parseColor("#D69E2E"), Color.parseColor("#805AD5"))
        }
        dataSet.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE; dataSet.xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        dataSet.valueLinePart1OffsetPercentage = 80f; dataSet.valueLinePart1Length = 0.4f; dataSet.valueLinePart2Length = 0.4f
        dataSet.valueLineWidth = 1f; dataSet.valueLineColor = Color.parseColor("#CBD5E0"); dataSet.valueTextColor = Color.parseColor("#2D3748"); dataSet.valueTextSize = 11f; dataSet.sliceSpace = 2f

        val data = PieData(dataSet); data.setValueFormatter(PercentFormatter(pieChart)); pieChart.data = data
        val defaultCenterText = "${if(isExpenseMode) "Pengeluaran" else "Pemasukan"}\n${formatRp.format(totalAmount)}"
        pieChart.centerText = defaultCenterText

        pieChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) { if (e is PieEntry) { pieChart.centerText = "${e.label}\n${formatRp.format(e.value)}" } }
            override fun onNothingSelected() { pieChart.centerText = defaultCenterText }
        })
        pieChart.animateY(1000, Easing.EaseInOutQuad); pieChart.invalidate()
    }

    private fun updateLineChart(transactions: List<Transaction>) {
        val entries = viewModel.getFinancialTrendData(transactions)
        val sortedEntries = entries.sortedBy { it.x }

        // Jika data kosong, bersihkan chart
        if (sortedEntries.isEmpty()) {
            lineChart.clear()
            return
        }

        val dataSet = LineDataSet(sortedEntries, "Saldo")

        // --- GAYA GARIS (STYLE) ---
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER // Garis melengkung halus (Curve)
        dataSet.cubicIntensity = 0.2f

        // Warna Garis (Ungu Solid)
        dataSet.color = Color.parseColor("#805AD5")
        dataSet.lineWidth = 3f // Garis lebih tebal

        // --- BAGIAN TITIK (CIRCLES) ---
        dataSet.setDrawCircles(false) // Hilangkan titik-titik (clean look)
        dataSet.setDrawValues(false)  // Hilangkan teks angka di atas garis

        // Jika ingin titik muncul HANYA saat di-klik (highlight):
        dataSet.setDrawHorizontalHighlightIndicator(false)
        dataSet.highLightColor = Color.parseColor("#805AD5") // Warna garis sorot
        dataSet.highlightLineWidth = 1.5f

        // --- AREA ISI (GRADIENT FILL) ---
        dataSet.setDrawFilled(true) // Aktifkan isi warna di bawah garis
        // Gunakan Drawable Gradient yang baru dibuat
        dataSet.fillDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.gradient_chart_purple)
        // (Opsional) Jika drawable gagal load, gunakan warna solid transparan:
        // dataSet.fillColor = Color.parseColor("#805AD5")
        // dataSet.fillAlpha = 50

        val data = LineData(dataSet)
        lineChart.data = data

        // Refresh & Animate
        lineChart.invalidate()
        lineChart.animateX(1200, Easing.EaseInOutSine) // Animasi muncul dari kiri
    }
}