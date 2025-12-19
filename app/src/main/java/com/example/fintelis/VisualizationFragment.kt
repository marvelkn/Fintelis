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
    private lateinit var tvYearNumber: TextView // Added

    // --- Switch Controls ---
    private lateinit var btnSwitchExpense: TextView
    private lateinit var btnSwitchIncome: TextView

    // --- Wallet List ---
    private lateinit var rvWallets: RecyclerView
    private lateinit var walletAdapter: WalletAdapter

    // --- State Variables ---
    private var isExpenseMode = true // Default to Expense
    private var currentTransactions: List<Transaction> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_visualization, container, false)
        initializeViews(view)

        setupWalletList() // Setup Wallet Adapter
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
        tvYearNumber = view.findViewById(R.id.tvYearNumber) // Initialize

        btnSwitchExpense = view.findViewById(R.id.btnSwitchExpense)
        btnSwitchIncome = view.findViewById(R.id.btnSwitchIncome)
        
        rvWallets = view.findViewById(R.id.rv_wallets) // Initialize RecyclerView

        // Animasi layout (opsional, untuk kehalusan jika ada perubahan visibilitas)
        layoutCharts.layoutTransition = LayoutTransition()
    }

    private fun setupWalletList() {
        walletAdapter = WalletAdapter(requireContext(), emptyList()) { wallet ->
            viewModel.setActiveWallet(wallet?.id)
        }
        rvWallets.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rvWallets.adapter = walletAdapter
    }

    // --- LOGIC TOMBOL DETAIL (NAVIGASI KE HALAMAN BARU) ---
    private fun setupDetailButton() {
        updateDetailButtonUI()

        btnSeeDetail.setOnClickListener {
            val detailFragment = DetailCashflowFragment.newInstance(isExpenseMode)

            // Gunakan 'requireActivity().supportFragmentManager'
            // Jangan gunakan 'parentFragmentManager' agar tidak konflik dengan NavHost
            requireActivity().supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    android.R.anim.slide_in_left,
                    android.R.anim.fade_out,
                    android.R.anim.fade_in,
                    android.R.anim.slide_out_right
                )
                // Gunakan android.R.id.content untuk menimpa satu layar penuh
                .replace(android.R.id.content, detailFragment)
                .addToBackStack(null)
                .commit()
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
        // Observe Wallets
        viewModel.wallets.observe(viewLifecycleOwner) { wallets ->
            walletAdapter.updateWallets(wallets ?: emptyList())
        }

        // Observe Active Wallet
        viewModel.activeWalletId.observe(viewLifecycleOwner) { walletId ->
            walletAdapter.setSelectedWallet(walletId)
        }

        viewModel.displayedTransactions.observe(viewLifecycleOwner) { transactions ->
            currentTransactions = transactions ?: emptyList()
            updateSummaryText(currentTransactions)

            if (currentTransactions.isNotEmpty()) {
                layoutCharts.isVisible = true
                layoutEmptyState.isVisible = false

                updatePieChart(currentTransactions)
                updateLineChart(currentTransactions)

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

    // --- HELPER FUNCTIONS (Sama seperti sebelumnya) ---

    private fun updateSummaryText(transactions: List<Transaction>) {
        val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        val totalInc: Double = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExp: Double = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
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
    }

    private fun updatePieChart(transactions: List<Transaction>) {
        val entries = ArrayList<PieEntry>()
        val filteredTransactions = if (isExpenseMode) {
            transactions.filter { it.type == TransactionType.EXPENSE }
        } else {
            transactions.filter { it.type == TransactionType.INCOME }
        }

        if (filteredTransactions.isEmpty()) {
            pieChart.clear()
            tvTopCategory.text = if (isExpenseMode) "No Expenses" else "No Income"
            return
        }

        tvTopCategory.text = if (isExpenseMode) "Top Expense Categories" else "Top Income Sources"

        val categoryMap = filteredTransactions
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { (_, value) -> value } // Urutkan dari terbesar
            .take(5) // Ambil top 5 saja
            .toMap()

        for ((category, amount) in categoryMap) {
            entries.add(PieEntry(amount.toFloat(), category))
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.sliceSpace = 3f
        dataSet.selectionShift = 5f

        // Warna-warni Modern
        val colors = ArrayList<Int>()
        colors.add(Color.parseColor("#FF6B6B")) // Merah Coral
        colors.add(Color.parseColor("#4ECDC4")) // Tosca
        colors.add(Color.parseColor("#FFE66D")) // Kuning Pastel
        colors.add(Color.parseColor("#1A535C")) // Hijau Gelap
        colors.add(Color.parseColor("#FF9F1C")) // Oranye

        dataSet.colors = colors

        val data = PieData(dataSet)
        data.setValueFormatter(PercentFormatter(pieChart))
        data.setValueTextSize(11f)
        data.setValueTextColor(Color.WHITE)

        pieChart.data = data
        pieChart.highlightValues(null)
        pieChart.invalidate()
    }

    private fun updateLineChart(transactions: List<Transaction>) {
        val entries = ArrayList<Entry>()

        // 1. Group transaksi berdasarkan tanggal
        val groupedByDate = transactions
            .sortedBy { viewModel.parseDatePublic(it.date) } // Pastikan urut tanggal
            .groupBy { viewModel.parseDatePublic(it.date) }

        var cumulativeAmount = 0.0

        // 2. Buat Entry untuk chart
        for ((date, transList) in groupedByDate) {
            // Hitung net untuk hari tersebut (Income - Expense)
            // Atau jika hanya ingin menampilkan Expense/Income saja sesuai mode:
            val dailyTotal = if (isExpenseMode) {
                transList.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            } else {
                transList.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            }

            // Opsi A: Tampilkan fluktuasi harian
             entries.add(Entry(date.time.toFloat(), dailyTotal.toFloat()))

            // Opsi B: Tampilkan akumulasi (meningkat terus) -> Uncomment jika ingin mode akumulasi
            // cumulativeAmount += dailyTotal
            // entries.add(Entry(date.time.toFloat(), cumulativeAmount.toFloat()))
        }

        if (entries.isEmpty()) {
            lineChart.clear()
            return
        }

        val dataSet = LineDataSet(entries, if (isExpenseMode) "Expense Trend" else "Income Trend")
        
        // Styling Garis
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER // Garis melengkung halus
        dataSet.cubicIntensity = 0.2f
        dataSet.setDrawFilled(true) // Isi area bawah garis
        dataSet.setDrawCircles(true) // Tampilkan titik
        dataSet.lineWidth = 2f
        dataSet.circleRadius = 4f
        dataSet.setDrawCircleHole(true)
        dataSet.circleHoleRadius = 2f
        
        if (isExpenseMode) {
            // Merah
            dataSet.color = Color.parseColor("#C53030")
            dataSet.setCircleColor(Color.parseColor("#C53030"))
            dataSet.fillColor = Color.parseColor("#FED7D7") // Merah muda transparan
            dataSet.fillAlpha = 100
        } else {
            // Hijau
            dataSet.color = Color.parseColor("#38A169")
            dataSet.setCircleColor(Color.parseColor("#38A169"))
            dataSet.fillColor = Color.parseColor("#C6F6D5") // Hijau muda transparan
            dataSet.fillAlpha = 100
        }

        // Hilangkan nilai di atas titik (biar bersih) -> User bisa tap untuk lihat detail
        dataSet.setDrawValues(false)

        val lineData = LineData(dataSet)
        lineChart.data = lineData
        lineChart.invalidate()
    }
}