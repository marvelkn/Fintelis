package com.example.fintelis

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

// Pastikan import R sesuai dengan package name project Anda
// import com.example.namaaplikasi.R

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    // Variabel untuk state saldo (Sembunyi/Tampil)
    private var isBalanceVisible = false
    private val realBalance = "IDR 25.500.000" // Contoh data asli (nanti bisa dari database/API)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- 1. SETUP FITUR HIDE/UNHIDE SALDO ---
        setupBalanceVisibility(view)

        // --- 2. SETUP DONUT CHART ---
        setupPieChart(view)
    }

    private fun setupBalanceVisibility(view: View) {
        val tvBalance = view.findViewById<TextView>(R.id.tv_balance_nominal)
        val btnToggle = view.findViewById<ImageView>(R.id.img_toggle_balance)

        // Set state awal (Terkunci/Hidden)
        updateBalanceView(tvBalance, btnToggle)

        btnToggle.setOnClickListener {
            // Ubah status true <-> false
            isBalanceVisible = !isBalanceVisible
            updateBalanceView(tvBalance, btnToggle)
        }
    }

    private fun updateBalanceView(textView: TextView, iconView: ImageView) {
        if (isBalanceVisible) {
            // Jika Mode Terbuka
            textView.text = realBalance
            iconView.setImageResource(R.drawable.ic_visibility) // Pastikan icon mata terbuka ada di drawable
        } else {
            // Jika Mode Tersembunyi
            textView.text = "IDR •••••••••••"
            iconView.setImageResource(R.drawable.ic_visibility_off) // Pastikan icon mata coret ada di drawable
        }
    }

    private fun setupPieChart(view: View) {
        val pieChart = view.findViewById<PieChart>(R.id.pieChartFinancial)

        // 1. Siapkan Data (Income, Expenses, Investment)
        val entries = ArrayList<PieEntry>()
        entries.add(PieEntry(35f)) // Income (35%)
        entries.add(PieEntry(45f)) // Expense (45%)
        entries.add(PieEntry(20f)) // Investment (20%)

        // 2. Siapkan Warna (Sesuai desain XML sebelumnya)
        val colors = ArrayList<Int>()
        colors.add(Color.parseColor("#FFC107")) // Kuning (Income)
        colors.add(Color.parseColor("#EF5350")) // Merah (Expense)
        colors.add(Color.parseColor("#26C6DA")) // Cyan (Investment)

        // 3. Konfigurasi DataSet
        val dataSet = PieDataSet(entries, "")
        dataSet.colors = colors
        dataSet.sliceSpace = 3f // Jarak antar potongan kue
        dataSet.setDrawValues(false) // Hilangkan angka di dalam chart agar bersih

        // 4. Konfigurasi Tampilan Chart (Donut Style)
        val pieData = PieData(dataSet)
        pieChart.data = pieData

        pieChart.apply {
            description.isEnabled = false // Matikan label deskripsi di pojok kanan bawah
            legend.isEnabled = false      // Matikan legend bawaan (karena kita buat custom di XML)

            isDrawHoleEnabled = true      // Aktifkan lubang tengah (Donut)
            setHoleColor(Color.TRANSPARENT) // Warna lubang transparan atau putih

            holeRadius = 65f              // Besar lubang dalam
            transparentCircleRadius = 70f // Efek bayangan lingkaran

            setEntryLabelColor(Color.TRANSPARENT) // Hilangkan text label di dalam chart
            animateY(1400) // Animasi putar saat loading
            invalidate() // Render ulang
        }
    }
}
