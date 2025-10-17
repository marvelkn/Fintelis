package com.example.fintelis

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.fintelis.databinding.FragmentCreditResultBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter

class CreditResultFragment : Fragment() {

    private var _binding: FragmentCreditResultBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreditResultBinding.inflate(inflater, container, false)
        setupBarChart()
        return binding.root
    }

    private fun setupBarChart() {
        val barChart = binding.barChart

        val entries = listOf(
            BarEntry(0f, 25f),
            BarEntry(1f, 30f),
            BarEntry(2f, 20f),
            BarEntry(3f, 15f),
            BarEntry(4f, 10f)
        )

        val dataSet = BarDataSet(entries, "Faktor Penilaian")

        // 🎨 Gradasi biru elegan
        val startColor = Color.parseColor("#4A90E2") // biru muda
        val endColor = Color.parseColor("#003C8F")   // biru tua
        dataSet.setGradientColor(startColor, endColor)

        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 12f

        // Formatter untuk menampilkan nilai di atas bar
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "${value.toInt()}%"
            }
        }

        val data = BarData(dataSet)
        data.barWidth = 0.6f
        barChart.data = data

        val labels = listOf("Income", "History", "Assets", "Expenses", "Other")
        val xAxis = barChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)
        xAxis.labelRotationAngle = -25f
        xAxis.textSize = 11f
        xAxis.textColor = Color.DKGRAY

        val leftAxis = barChart.axisLeft
        leftAxis.axisMinimum = 0f
        leftAxis.textColor = Color.DKGRAY
        leftAxis.gridColor = Color.parseColor("#E0E0E0")

        barChart.axisRight.isEnabled = false
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false

        barChart.setDrawBarShadow(false)
        barChart.setFitBars(true)
        barChart.animateY(1000)
        barChart.invalidate()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
