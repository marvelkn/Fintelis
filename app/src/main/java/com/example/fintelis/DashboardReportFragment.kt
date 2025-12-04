package com.example.fintelis

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import android.widget.PopupMenu
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

class DashboardReportFragment : Fragment() {

    private lateinit var lineChartTrend: LineChart
    private lateinit var barChartScore: BarChart
    private lateinit var hbarChartBranches: HorizontalBarChart
    private lateinit var tabTimeFilter: TabLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard_report, container, false)

        lineChartTrend = view.findViewById(R.id.lineChartTrend)
        barChartScore = view.findViewById(R.id.barChartScore)
        hbarChartBranches = view.findViewById(R.id.hbarChartBranches)
        tabTimeFilter = view.findViewById(R.id.tab_time_filter)

        setupTabs()
        setupSampleCharts()
        setupExportMenu(view) // 🔹 ganti dari setupExportButtons() ke popup menu

        return view
    }

    private fun setupTabs() {
        tabTimeFilter.addTab(tabTimeFilter.newTab().setText("WEEKLY"))
        tabTimeFilter.addTab(tabTimeFilter.newTab().setText("MONTHLY"))
        tabTimeFilter.addTab(tabTimeFilter.newTab().setText("YEARLY"))

        tabTimeFilter.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> loadDataFor("weekly")
                    1 -> loadDataFor("monthly")
                    2 -> loadDataFor("yearly")
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
        tabTimeFilter.getTabAt(1)?.select()
    }

    private fun loadDataFor(range: String) {
        when (range) {
            "weekly" -> setupSampleCharts("weekly")
            "monthly" -> setupSampleCharts("monthly")
            "yearly" -> setupSampleCharts("yearly")
        }
    }

    private fun setupSampleCharts(range: String = "monthly") {
        setupLineChart(range)
        setupBarScoreChart(range)
        setupBranchesChart(range)
    }

    private fun setupLineChart(range: String) {
        val labels = when (range) {
            "weekly" -> listOf("W1","W2","W3","W4")
            "monthly" -> listOf("Jan","Feb","Mar","Apr","May","Jun")
            else -> listOf("2019","2020","2021","2022","2023","2024")
        }

        val vals = when (range) {
            "weekly" -> listOf(60f, 65f, 70f, 75f)
            "monthly" -> listOf(60f, 62f, 68f, 70f, 72f, 74f)
            else -> listOf(55f, 60f, 66f, 70f, 72f, 75f)
        }

        val entries = vals.mapIndexed { i, v -> Entry(i.toFloat(), v) }
        val set = LineDataSet(entries, "Approval (%)")
        set.color = resources.getColor(android.R.color.holo_blue_dark)
        set.lineWidth = 2f
        set.setDrawCircles(true)
        set.circleRadius = 4f
        set.setDrawValues(false)

        val lineData = LineData(set)
        lineChartTrend.data = lineData

        val xAxis = lineChartTrend.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.valueFormatter = object: ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val idx = value.toInt()
                return if (idx >= 0 && idx < labels.size) labels[idx] else ""
            }
        }

        lineChartTrend.axisRight.isEnabled = false
        lineChartTrend.description.isEnabled = false
        lineChartTrend.legend.isEnabled = false
        lineChartTrend.invalidate()
    }

    private fun setupBarScoreChart(range: String) {
        val labels = listOf("Low", "Medium", "Good", "Excellent")
        val vals = when(range) {
            "weekly" -> listOf(20f, 40f, 30f, 10f)
            "monthly" -> listOf(15f, 45f, 30f, 10f)
            else -> listOf(10f, 50f, 30f, 10f)
        }
        val entries = vals.mapIndexed { i, v -> BarEntry(i.toFloat(), v) }
        val set = BarDataSet(entries, "")
        set.colors = listOf(
            resources.getColor(android.R.color.holo_orange_light),
            resources.getColor(android.R.color.holo_blue_light),
            resources.getColor(android.R.color.holo_blue_dark),
            resources.getColor(android.R.color.holo_green_light)
        )
        set.valueTextSize = 12f
        set.setDrawValues(true)

        val data = BarData(set)
        data.barWidth = 0.6f
        barChartScore.data = data
        barChartScore.description.isEnabled = false
        barChartScore.legend.isEnabled = false

        val xAxis = barChartScore.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)
        xAxis.valueFormatter = object: ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val idx = value.toInt()
                return if (idx >= 0 && idx < labels.size) labels[idx] else ""
            }
        }

        barChartScore.axisRight.isEnabled = false
        barChartScore.invalidate()
    }

    private fun setupBranchesChart(range: String) {
        val labels = listOf("Branch A", "Branch B", "Branch C", "Branch D")
        val vals = when(range) {
            "weekly" -> listOf(40f, 55f, 30f, 60f)
            "monthly" -> listOf(50f, 45f, 35f, 60f)
            else -> listOf(60f, 50f, 40f, 70f)
        }
        val entries = vals.mapIndexed { i, v -> BarEntry(i.toFloat(), v) }
        val set = BarDataSet(entries, "")
        set.color = resources.getColor(android.R.color.holo_blue_dark)
        set.valueTextSize = 12f
        val data = BarData(set)
        data.barWidth = 0.6f
        hbarChartBranches.data = data

        val xAxis = hbarChartBranches.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.valueFormatter = object: ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val idx = value.toInt()
                return if (idx >= 0 && idx < labels.size) labels[idx] else ""
            }
        }
        hbarChartBranches.axisRight.isEnabled = false
        hbarChartBranches.description.isEnabled = false
        hbarChartBranches.invalidate()
    }

    // 🔹 Popup menu logic untuk export
    private fun setupExportMenu(view: View) {
        val btnExport = view.findViewById<MaterialButton>(R.id.btn_export)
        btnExport.setOnClickListener {
            val popup = PopupMenu(requireContext(), btnExport)
            popup.menuInflater.inflate(R.menu.menu_export_options, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.export_pdf -> {
                        exportToPdf(view)
                        Toast.makeText(context, "Export to PDF", Toast.LENGTH_SHORT).show()
                        true
                    }
                    R.id.export_csv -> {
                        exportToCsv(requireContext())
                        Toast.makeText(context, "Export to CSV", Toast.LENGTH_SHORT).show()
                        true
                    }
                    R.id.export_excel -> {
                        exportToExcel(requireContext())
                        Toast.makeText(context, "Export to Excel", Toast.LENGTH_SHORT).show()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    private fun exportToPdf(viewRoot: View) {
        try {
            val scroll = viewRoot.findViewById<View>(R.id.scrollMain)
            val totalHeight = scroll.height
            val totalWidth = scroll.width

            val bitmap = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            scroll.draw(canvas)

            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(totalWidth, totalHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val pdfCanvas = page.canvas
            pdfCanvas.drawBitmap(bitmap, 0f, 0f, Paint())
            pdfDocument.finishPage(page)

            val fileName = "report_${timeStamp()}.pdf"
            val file = File(requireContext().getExternalFilesDir(null), fileName)
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()

            Toast.makeText(requireContext(), "PDF saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "PDF export failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun exportToCsv(ctx: Context) {
        try {
            val csvBuilder = StringBuilder()
            csvBuilder.append("Label, Value\n")
            csvBuilder.append("Jan,60\nFeb,62\nMar,68\nApr,70\nMay,72\nJun,74\n")

            val fileName = "report_${timeStamp()}.csv"
            val file = File(ctx.getExternalFilesDir(null), fileName)
            val writer = OutputStreamWriter(FileOutputStream(file))
            writer.write(csvBuilder.toString())
            writer.flush()
            writer.close()

            Toast.makeText(ctx, "CSV saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(ctx, "CSV export failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun exportToExcel(ctx: Context) {
        try {
            val workbook: Workbook = XSSFWorkbook()
            val sheet: Sheet = workbook.createSheet("Report")

            val headerStyle: CellStyle = workbook.createCellStyle()
            val headerFont: Font = workbook.createFont()
            headerFont.bold = true
            headerStyle.setFont(headerFont)
            headerStyle.fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            headerStyle.fillPattern = FillPatternType.SOLID_FOREGROUND

            val headerRow = sheet.createRow(0)
            val cellLabel = headerRow.createCell(0)
            val cellValue = headerRow.createCell(1)
            cellLabel.setCellValue("Label")
            cellValue.setCellValue("Value")
            cellLabel.cellStyle = headerStyle
            cellValue.cellStyle = headerStyle

            val data = listOf(
                Pair("Jan", 60.0),
                Pair("Feb", 62.0),
                Pair("Mar", 68.0),
                Pair("Apr", 70.0),
                Pair("May", 72.0),
                Pair("Jun", 74.0)
            )
            for ((i, row) in data.withIndex()) {
                val r = sheet.createRow(i + 1)
                r.createCell(0).setCellValue(row.first)
                r.createCell(1).setCellValue(row.second)
            }
            for (i in 0..1) sheet.autoSizeColumn(i)

            val fileName = "report_${timeStamp()}.xlsx"
            val file = File(ctx.getExternalFilesDir(null), fileName)
            val fos = FileOutputStream(file)
            workbook.write(fos)
            fos.flush()
            fos.close()
            workbook.close()

            Toast.makeText(ctx, "Excel saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(ctx, "Excel export failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun timeStamp(): String {
        val df = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return df.format(Date())
    }
}
