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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fintelis.adapter.WalletAdapter
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

    // --- Components Button & Layouts ---
    private lateinit var btnSeeDetail: MaterialButton
    private lateinit var layoutDetailButton: LinearLayout // Wrapper tombol detail
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

    // --- Switch Controls (BARU: Tambah btnSwitchComparison) ---
    private lateinit var btnSwitchExpense: TextView
    private lateinit var btnSwitchIncome: TextView
    private lateinit var btnSwitchComparison: TextView

    // --- Wallet Components ---
    private lateinit var walletAdapter: WalletAdapter
    private lateinit var rvWallets: RecyclerView

    // --- BARU: State Variables Menggunakan Enum ---
    private enum class ChartMode { EXPENSE, INCOME, COMPARISON }
    private var currentMode = ChartMode.EXPENSE // Default

    private var currentTransactions: List<Transaction> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_visualization, container, false)
        initializeViews(view)

        setupWalletAdapter()
        setupCharts()
        setupMonthNavigation()
        setupCategorySwitch()
        setupDetailButton()
        observeData()

        return view
    }

    private fun initializeViews(view: View) {
        pieChart = view.findViewById(R.id.pieChart)
        lineChart = view.findViewById(R.id.lineChart)

        btnSeeDetail = view.findViewById(R.id.btnSeeDetail)
        layoutDetailButton = view.findViewById(R.id.layoutDetailButton) // Ambil LinearLayout wrapper

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
        btnSwitchComparison = view.findViewById(R.id.btnSwitchComparison) // Init tombol baru

        rvWallets = view.findViewById(R.id.rv_wallets)

        layoutCharts.layoutTransition = LayoutTransition()
    }

    private fun setupWalletAdapter() {
        walletAdapter = WalletAdapter(requireContext(), emptyList()) { wallet ->
            viewModel.setActiveWallet(wallet?.id)
        }
        rvWallets.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = walletAdapter
        }
    }

    // --- LOGIC TOMBOL DETAIL ---
    private fun setupDetailButton() {
        // Panggil saat inisialisasi
        updateDetailButtonUI()

        btnSeeDetail.setOnClickListener {
            // Hanya kirim Expense atau Income. Comparison mode tombolnya disembunyikan.
            val isExpense = (currentMode == ChartMode.EXPENSE)
            val bundle = Bundle().apply {
                putBoolean("isExpense", isExpense)
            }
            findNavController().navigate(R.id.action_visualization_to_detailCashflow, bundle)
        }
    }

    private fun updateDetailButtonUI() {
        // Tombol Detail hanya muncul jika bukan mode COMPARISON
        if (currentMode == ChartMode.COMPARISON) {
            layoutDetailButton.isVisible = false
            return
        }

        layoutDetailButton.isVisible = true
        val typeText = if (currentMode == ChartMode.EXPENSE) "Expense" else "Income"
        btnSeeDetail.text = "View Full $typeText List"
        btnSeeDetail.setIconResource(R.drawable.ic_chevron_right)

        if (currentMode == ChartMode.EXPENSE) {
            btnSeeDetail.setBackgroundColor(Color.parseColor("#C53030")) // Merah
        } else {
            btnSeeDetail.setBackgroundColor(Color.parseColor("#38A169")) // Hijau
        }
    }

    // --- LOGIC SWITCH CATEGORY (UPDATED UNTUK 3 MODE) ---
    private fun setupCategorySwitch() {
        btnSwitchExpense.setOnClickListener {
            if (currentMode != ChartMode.EXPENSE) {
                currentMode = ChartMode.EXPENSE
                updateSwitchUI()
                updatePieChart(currentTransactions)
                updateDetailButtonUI()
            }
        }

        btnSwitchIncome.setOnClickListener {
            if (currentMode != ChartMode.INCOME) {
                currentMode = ChartMode.INCOME
                updateSwitchUI()
                updatePieChart(currentTransactions)
                updateDetailButtonUI()
            }
        }

        // BARU: Tombol Comparison
        btnSwitchComparison.setOnClickListener {
            if (currentMode != ChartMode.COMPARISON) {
                currentMode = ChartMode.COMPARISON
                updateSwitchUI()
                updatePieChart(currentTransactions)
                updateDetailButtonUI()
            }
        }
    }

    private fun updateSwitchUI() {
        // Reset semua tombol ke transparan/abu-abu dulu
        btnSwitchExpense.setBackgroundColor(Color.TRANSPARENT); btnSwitchExpense.setTextColor(Color.parseColor("#718096"))
        btnSwitchIncome.setBackgroundColor(Color.TRANSPARENT); btnSwitchIncome.setTextColor(Color.parseColor("#718096"))
        btnSwitchComparison.setBackgroundColor(Color.TRANSPARENT); btnSwitchComparison.setTextColor(Color.parseColor("#718096"))

        // Highlight tombol yang aktif
        when (currentMode) {
            ChartMode.EXPENSE -> {
                btnSwitchExpense.setBackgroundResource(R.drawable.bg_pill_danger)
                btnSwitchExpense.setTextColor(Color.WHITE)
            }
            ChartMode.INCOME -> {
                btnSwitchIncome.setBackgroundResource(R.drawable.bg_pill_success)
                btnSwitchIncome.setTextColor(Color.WHITE)
            }
            ChartMode.COMPARISON -> {
                // --- PERBAIKAN DI SINI (TAMBAHKAN .mutate()) ---
                btnSwitchComparison.setBackgroundResource(R.drawable.bg_pill_white_outline)

                // mutate() memastikan perubahan warna HANYA berlaku untuk tombol ini,
                // tidak menular ke container induknya.
                btnSwitchComparison.background.mutate().setTint(Color.parseColor("#3182CE")) // Biru

                btnSwitchComparison.setTextColor(Color.WHITE)
            }
        }
    }

    // --- OBSERVE DATA ---
    private fun observeData() {
        viewModel.wallets.observe(viewLifecycleOwner) { wallets ->
            walletAdapter.updateWallets(wallets ?: emptyList())
        }

        viewModel.activeWalletId.observe(viewLifecycleOwner) { walletId ->
            walletAdapter.setSelectedWallet(walletId)
        }

        viewModel.displayedTransactions.observe(viewLifecycleOwner) { transactions ->
            if (transactions != null) {
                currentTransactions = transactions
                updateSummaryText(transactions)

                if (transactions.isNotEmpty()) {
                    layoutCharts.isVisible = true
                    layoutEmptyState.isVisible = false

                    updatePieChart(transactions)
                    updateLineChart(transactions)
                    updateDetailButtonUI() // Cek apakah tombol harus muncul

                } else {
                    layoutCharts.isVisible = false
                    layoutEmptyState.isVisible = true
                    pieChart.clear()
                    lineChart.clear()
                    layoutDetailButton.isVisible = false
                }
            }
        }
    }

    // --- HELPER FUNCTIONS ---

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
        pieChart.setUsePercentValues(true); pieChart.description.isEnabled = false
        pieChart.setExtraOffsets(30f, 10f, 30f, 10f)
        pieChart.dragDecelerationFrictionCoef = 0.95f; pieChart.isDrawHoleEnabled = true
        pieChart.setHoleColor(Color.WHITE); pieChart.transparentCircleRadius = 61f; pieChart.holeRadius = 58f
        pieChart.setDrawCenterText(true); pieChart.setCenterTextSize(12f); pieChart.setCenterTextColor(Color.parseColor("#718096"))
        pieChart.legend.isEnabled = false; pieChart.setDrawEntryLabels(true)
        pieChart.setEntryLabelColor(Color.parseColor("#2D3748")); pieChart.setEntryLabelTextSize(11f)
        pieChart.animateY(1400, Easing.EaseInOutQuad)

        lineChart.description.isEnabled = false
        lineChart.legend.isEnabled = false
        lineChart.setDrawGridBackground(false)
        lineChart.setDrawBorders(false)

        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.textColor = Color.parseColor("#A0AEC0")
        xAxis.textSize = 10f
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(false)
        xAxis.granularity = 1f
        xAxis.yOffset = 10f
        xAxis.valueFormatter = object : ValueFormatter() {
            private val format = SimpleDateFormat("dd", Locale.US)
            override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                return try { format.format(Date(value.toLong())) } catch(e: Exception) { "" }
            }
        }

        val leftAxis = lineChart.axisLeft
        leftAxis.textColor = Color.parseColor("#A0AEC0")
        leftAxis.textSize = 10f
        leftAxis.setDrawGridLines(true)
        leftAxis.gridColor = Color.parseColor("#F7FAFC")
        leftAxis.enableGridDashedLine(10f, 10f, 0f)
        leftAxis.setDrawAxisLine(false)
        leftAxis.axisMinimum = 0f

        leftAxis.valueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                return if (value >= 1000000) String.format("%.0fjt", value / 1000000)
                else if (value >= 1000) String.format("%.0frb", value / 1000)
                else String.format("%.0f", value)
            }
        }

        lineChart.axisRight.isEnabled = false
        lineChart.setTouchEnabled(true)
        lineChart.isDragEnabled = true
        lineChart.setScaleEnabled(false)
        lineChart.setPinchZoom(false)

        try {
            val markerView = CustomMarkerView(requireContext(), R.layout.custom_marker_view)
            markerView.chartView = lineChart
            lineChart.marker = markerView
        } catch (e: Exception) { }
    }

    // --- CHART LOGIC UTAMA (UPDATED) ---
    private fun updatePieChart(transactions: List<Transaction>) {
        val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        pieChart.highlightValues(null)

        val entries: List<PieEntry>
        val colors: List<Int>
        val centerTextLabel: String
        val totalAmountForCenter: Double

        when (currentMode) {
            // MODE 1: EXPENSE (Per Kategori)
            ChartMode.EXPENSE -> {
                val filteredList = transactions.filter { it.type == TransactionType.EXPENSE }
                if (filteredList.isEmpty()) { showEmptyChart("No Expense"); return }

                totalAmountForCenter = filteredList.sumOf { it.amount }
                val grouped = filteredList.groupBy { it.category }.mapValues { it.value.sumOf { t -> t.amount } }

                // Set Top Category Text
                val maxEntry = grouped.maxByOrNull { it.value }
                tvTopCategory.text = if (maxEntry != null) "${maxEntry.key} (${formatRp.format(maxEntry.value)})" else "-"

                entries = grouped.map { PieEntry(it.value.toFloat(), it.key) }
                colors = listOf(Color.parseColor("#E53E3E"), Color.parseColor("#ED8936"), Color.parseColor("#ECC94B"), Color.parseColor("#805AD5"), Color.parseColor("#38B2AC"))
                centerTextLabel = "Expenses"
            }
            // MODE 2: INCOME (Per Kategori)
            ChartMode.INCOME -> {
                val filteredList = transactions.filter { it.type == TransactionType.INCOME }
                if (filteredList.isEmpty()) { showEmptyChart("No Income"); return }

                totalAmountForCenter = filteredList.sumOf { it.amount }
                val grouped = filteredList.groupBy { it.category }.mapValues { it.value.sumOf { t -> t.amount } }

                val maxEntry = grouped.maxByOrNull { it.value }
                tvTopCategory.text = if (maxEntry != null) "${maxEntry.key} (${formatRp.format(maxEntry.value)})" else "-"

                entries = grouped.map { PieEntry(it.value.toFloat(), it.key) }
                colors = listOf(Color.parseColor("#38A169"), Color.parseColor("#3182CE"), Color.parseColor("#319795"), Color.parseColor("#D69E2E"), Color.parseColor("#805AD5"))
                centerTextLabel = "Incomes"
            }
            // MODE 3: COMPARISON (Income vs Expense)
            ChartMode.COMPARISON -> {
                val totalInc = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                val totalExp = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                val balance = totalInc - totalExp

                if (totalInc == 0.0 && totalExp == 0.0) { showEmptyChart("No Data"); return }

                tvTopCategory.text = "Balance: ${formatRp.format(balance)}"

                entries = listOf(
                    PieEntry(totalInc.toFloat(), "Income"),
                    PieEntry(totalExp.toFloat(), "Expense")
                )
                // Hijau untuk Income, Merah untuk Expense
                colors = listOf(Color.parseColor("#38A169"), Color.parseColor("#E53E3E"))

                totalAmountForCenter = balance // Tampilkan saldo bersih di tengah
                centerTextLabel = "Cashflow"
            }
        }

        // Render Chart
        val dataSet = PieDataSet(entries, "")
        dataSet.colors = colors
        dataSet.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        dataSet.xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        dataSet.valueLinePart1OffsetPercentage = 80f; dataSet.valueLinePart1Length = 0.4f; dataSet.valueLinePart2Length = 0.4f
        dataSet.valueLineWidth = 1f; dataSet.valueLineColor = Color.parseColor("#CBD5E0"); dataSet.valueTextColor = Color.parseColor("#2D3748"); dataSet.valueTextSize = 11f; dataSet.sliceSpace = 2f

        val data = PieData(dataSet); data.setValueFormatter(PercentFormatter(pieChart)); pieChart.data = data

        val defaultCenterText = "$centerTextLabel\n${formatRp.format(totalAmountForCenter)}"
        pieChart.centerText = defaultCenterText

        pieChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) { if (e is PieEntry) { pieChart.centerText = "${e.label}\n${formatRp.format(e.value)}" } }
            override fun onNothingSelected() { pieChart.centerText = defaultCenterText }
        })
        pieChart.animateY(1000, Easing.EaseInOutQuad); pieChart.invalidate()
    }

    private fun showEmptyChart(msg: String) {
        pieChart.clear()
        pieChart.centerText = msg
        tvTopCategory.text = "-"
    }

    private fun updateLineChart(transactions: List<Transaction>) {
        val entries = viewModel.getFinancialTrendData(transactions)
        val sortedEntries = entries.sortedBy { it.x }

        if (sortedEntries.isEmpty()) {
            lineChart.clear()
            return
        }

        val dataSet = LineDataSet(sortedEntries, "Saldo")
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        dataSet.cubicIntensity = 0.2f
        dataSet.color = Color.parseColor("#805AD5")
        dataSet.lineWidth = 3f
        dataSet.setDrawCircles(false)
        dataSet.setDrawValues(false)
        dataSet.setDrawHorizontalHighlightIndicator(false)
        dataSet.highLightColor = Color.parseColor("#805AD5")
        dataSet.highlightLineWidth = 1.5f
        dataSet.setDrawFilled(true)
        dataSet.fillDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.gradient_chart_purple)

        val data = LineData(dataSet)
        lineChart.data = data
        lineChart.invalidate()
        lineChart.animateX(1200, Easing.EaseInOutSine)
    }
}