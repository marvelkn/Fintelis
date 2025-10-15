package com.example.fintelis

import android.graphics.Color
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
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
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

        val dataSet = PieDataSet(entries, "")
        // Warna tema biru (2 warna kontras tapi serasi)
        dataSet.colors = listOf(
            Color.parseColor("#1E88E5"), // Biru utama untuk Disetujui
            Color.parseColor("#90CAF9")  // Biru muda untuk Ditolak
        )

        val data = PieData(dataSet)
        data.setValueTextColor(Color.DKGRAY) // Nilai 70 dan 30 jadi putih
        data.setValueTextSize(14f)

        pieChart.data = data
        pieChart.description.isEnabled = false
        pieChart.setCenterTextColor(Color.parseColor("#1E88E5"))
        pieChart.setEntryLabelColor(Color.DKGRAY) // label di dalam chart juga putih
        pieChart.animateY(1000)

        // Aktifkan legend (info warna otomatis)
        val legend = pieChart.legend
        legend.isEnabled = true
        legend.textColor = Color.DKGRAY
        legend.textSize = 13f
        legend.form = Legend.LegendForm.CIRCLE
        legend.formSize = 12f
        legend.xEntrySpace = 10f
        legend.yEntrySpace = 5f
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        legend.setDrawInside(false)

        pieChart.invalidate()
    }

    private fun setupBarChart() {
        val entries = listOf(
            BarEntry(1f, 50f),
            BarEntry(2f, 30f),
            BarEntry(3f, 20f)
        )

        val dataSet = BarDataSet(entries, "")
        // Warna nuansa biru bertingkat
        dataSet.colors = listOf(
            Color.parseColor("#1565C0"), // Biru tua → Risiko Tinggi
            Color.parseColor("#1E88E5"), // Biru utama → Risiko Sedang
            Color.parseColor("#90CAF9")  // Biru muda → Risiko Rendah
        )

        val data = BarData(dataSet)
        data.setValueTextColor(Color.WHITE) // Nilai pada batang jadi putih (kontras)
        data.setValueTextSize(13f)

        barChart.data = data
        barChart.description.isEnabled = false
        barChart.axisLeft.textColor = Color.DKGRAY
        barChart.axisRight.isEnabled = false
        barChart.xAxis.textColor = Color.DKGRAY
        barChart.animateY(1000)
        barChart.setExtraOffsets(16f, 16f, 16f, 24f)

        // Aktifkan legend dan beri info arti warna
        val legend = barChart.legend
        legend.isEnabled = true
        legend.textColor = Color.DKGRAY
        legend.textSize = 13f
        legend.form = Legend.LegendForm.SQUARE
        legend.formSize = 12f
        legend.xEntrySpace = 10f
        legend.yEntrySpace = 5f
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        legend.setDrawInside(false)

        // Set label legend manual agar sesuai warna
        legend.setCustom(
            listOf(
                LegendEntry(
                    "Risiko Tinggi",
                    Legend.LegendForm.SQUARE,
                    10f,
                    2f,
                    null,
                    Color.parseColor("#1565C0")
                ),
                LegendEntry("Risiko Sedang", Legend.LegendForm.SQUARE, 10f, 2f, null, Color.parseColor("#1E88E5")),
                LegendEntry("Risiko Rendah", Legend.LegendForm.SQUARE, 10f, 2f, null, Color.parseColor("#90CAF9"))
            )
        )

        barChart.invalidate()
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
