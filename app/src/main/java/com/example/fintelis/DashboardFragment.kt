package com.example.fintelis

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.data.*
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment() {

    private lateinit var tvGreeting: TextView
    private lateinit var tvDate: TextView
    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var dateUpdater: Runnable

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        tvGreeting = view.findViewById(R.id.tv_greeting)
        tvDate = view.findViewById(R.id.tv_date)
        pieChart = view.findViewById(R.id.pieChartApproval)
        barChart = view.findViewById(R.id.barChartRisk)

        setupGreeting()
        setupPieChart()
        setupBarChart()
        setupButtons(view)
        startDateUpdater() // ✅ mulai update tanggal real-time

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(dateUpdater) // ❌ hentikan saat fragment hancur
    }

    @SuppressLint("SimpleDateFormat")
    private fun setupGreeting() {
        val username = "Zel"
        tvGreeting.text = "Hi, $username!"

        val currentDate = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
        tvDate.text = dateFormat.format(currentDate)
    }

    private fun startDateUpdater() {
        dateUpdater = object : Runnable {
            @SuppressLint("SimpleDateFormat")
            override fun run() {
                val currentDate = Calendar.getInstance().time
                val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
                tvDate.text = dateFormat.format(currentDate)

                // Ulangi setiap 60 detik (bisa ubah ke 1000L untuk tiap detik)
                handler.postDelayed(this, 60_000L)
            }
        }
        handler.post(dateUpdater)
    }

private fun setupPieChart() {
        val entries = listOf(
            PieEntry(70f, "Disetujui"),
            PieEntry(30f, "Ditolak")
        )

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(
            android.graphics.Color.parseColor("#1E88E5"),
            android.graphics.Color.parseColor("#90CAF9")
        )

        val data = PieData(dataSet)
        data.setValueTextColor(android.graphics.Color.DKGRAY)
        data.setValueTextSize(14f)

        pieChart.data = data
        pieChart.description.isEnabled = false
        pieChart.setCenterTextColor(android.graphics.Color.parseColor("#1E88E5"))
        pieChart.setEntryLabelColor(android.graphics.Color.DKGRAY)
        pieChart.animateY(1000)

        val legend = pieChart.legend
        legend.isEnabled = true
        legend.textColor = android.graphics.Color.DKGRAY
        legend.textSize = 13f
        legend.form = Legend.LegendForm.CIRCLE
        legend.formSize = 12f
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
        dataSet.colors = listOf(
            android.graphics.Color.parseColor("#1565C0"),
            android.graphics.Color.parseColor("#1E88E5"),
            android.graphics.Color.parseColor("#90CAF9")
        )

        val data = BarData(dataSet)
        data.setValueTextColor(android.graphics.Color.WHITE)
        data.setValueTextSize(13f)

        barChart.data = data
        barChart.description.isEnabled = false
        barChart.axisLeft.textColor = android.graphics.Color.DKGRAY
        barChart.axisRight.isEnabled = false
        barChart.xAxis.textColor = android.graphics.Color.DKGRAY
        barChart.animateY(1000)
        barChart.setExtraOffsets(16f, 16f, 16f, 24f)

        val legend = barChart.legend
        legend.isEnabled = true
        legend.textColor = android.graphics.Color.DKGRAY
        legend.textSize = 13f
        legend.form = Legend.LegendForm.SQUARE
        legend.formSize = 12f
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        legend.setDrawInside(false)

        legend.setCustom(
            listOf(
                LegendEntry("Risiko Tinggi", Legend.LegendForm.SQUARE, 10f, 2f, null, android.graphics.Color.parseColor("#1565C0")),
                LegendEntry("Risiko Sedang", Legend.LegendForm.SQUARE, 10f, 2f, null, android.graphics.Color.parseColor("#1E88E5")),
                LegendEntry("Risiko Rendah", Legend.LegendForm.SQUARE, 10f, 2f, null, android.graphics.Color.parseColor("#90CAF9"))
            )
        )

        barChart.invalidate()
    }

    private fun setupButtons(view: View) {
        val cardNewAnalysis = view.findViewById<LinearLayout>(R.id.card_new_analysis)
        val cardImportData = view.findViewById<LinearLayout>(R.id.card_import_data)

        cardNewAnalysis.setOnClickListener {
            Toast.makeText(context, "Mulai Analisis Kredit Baru", Toast.LENGTH_SHORT).show()
        }
        cardImportData.setOnClickListener {
            Toast.makeText(context, "Mulai Impor Data Nasabah", Toast.LENGTH_SHORT).show()
        }
    }

}
