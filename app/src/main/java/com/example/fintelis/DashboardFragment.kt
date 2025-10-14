package com.example.fintelis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate

class DashboardFragment : Fragment() {

    private lateinit var tvTotalApps: TextView
    private lateinit var tvAvgScore: TextView
    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart
    private lateinit var btnNewAnalysis: Button
    private lateinit var btnImportData: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        tvTotalApps = view.findViewById(R.id.tv_total_apps)
        tvAvgScore = view.findViewById(R.id.tv_avg_score)
        pieChart = view.findViewById(R.id.pieChartApproval)
        barChart = view.findViewById(R.id.barChartRisk)
        btnNewAnalysis = view.findViewById(R.id.btn_new_analysis)
        btnImportData = view.findViewById(R.id.btn_import_data)

        setupMetrics()
        setupPieChart()
        setupBarChart()
        setupButtons()

        return view
    }

    private fun setupMetrics() {
        // Contoh data, bisa diganti dengan data real-time
        tvTotalApps.text = "1,200"
        tvAvgScore.text = "750"
    }

    private fun setupPieChart() {
        val entries = listOf(
            PieEntry(70f, "Disetujui"),
            PieEntry(30f, "Ditolak")
        )
        val dataSet = PieDataSet(entries, "Rasio Persetujuan")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.invalidate() // refresh
    }

    private fun setupBarChart() {
        val entries = listOf(
            BarEntry(1f, 50f),
            BarEntry(2f, 30f),
            BarEntry(3f, 20f)
        )
        val dataSet = BarDataSet(entries, "Distribusi Risiko")
        dataSet.colors = ColorTemplate.COLORFUL_COLORS.toList()
        val data = BarData(dataSet)
        barChart.data = data
        barChart.invalidate() // refresh
    }

    private fun setupButtons() {
        btnNewAnalysis.setOnClickListener {
            Toast.makeText(context, "Mulai Analisis Kredit Baru", Toast.LENGTH_SHORT).show()
        }
        btnImportData.setOnClickListener {
            Toast.makeText(context, "Mulai Impor Data Nasabah", Toast.LENGTH_SHORT).show()
        }
    }
}
