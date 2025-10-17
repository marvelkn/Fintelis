package com.example.fintelis

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.tabs.TabLayout
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.Font
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [VisualizationFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class VisualizationFragment : Fragment() {
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
        setupSampleCharts() // initial with monthly
        setupExportButtons(view)

        return view
    }

    private fun setupTabs() {
        tabTimeFilter.addTab(tabTimeFilter.newTab().setText("MINGGUAN"))
        tabTimeFilter.addTab(tabTimeFilter.newTab().setText("BULANAN"))
        tabTimeFilter.addTab(tabTimeFilter.newTab().setText("TAHUNAN"))

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
        // select monthly by default
        tabTimeFilter.getTabAt(1)?.select()
    }

    private fun loadDataFor(range: String) {
        // Replace with real data retrieval. For demo, regenerate sample data depending on range.
        when (range) {
            "weekly" -> setupSampleCharts(range = "weekly")
            "monthly" -> setupSampleCharts(range = "monthly")
            "yearly" -> setupSampleCharts(range = "yearly")
        }
    }

    private fun setupSampleCharts(range: String = "monthly") {
        setupLineChart(range)
        setupBarScoreChart(range)
        setupBranchesChart(range)
    }

    private fun setupLineChart(range: String) {
        // Example: x = labels (months or weeks), y = approval %
        val labels = when (range) {
            "weekly" -> listOf("M1","M2","M3","M4")
            "monthly" -> listOf("Jan","Feb","Mar","Apr","Mei","Jun")
            else -> listOf("2019","2020","2021","2022","2023","2024")
        }

        val vals = when (range) {
            "weekly" -> listOf(60f, 65f, 70f, 75f)
            "monthly" -> listOf(60f, 62f, 68f, 70f, 72f, 74f)
            else -> listOf(55f, 60f, 66f, 70f, 72f, 75f)
        }

        val entries = vals.mapIndexed { i, v -> Entry(i.toFloat(), v) }
        val set = LineDataSet(entries, "Persetujuan (%)")
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
        // categories: Rendah, Menengah, Baik, Sangat Baik
        val labels = listOf("Rendah", "Menengah", "Baik", "Sangat Baik")
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
        // horizontal bars: Cabang A..D
        val labels = listOf("Cabang A", "Cabang B", "Cabang C", "Cabang D")
        val vals = when(range) {
            "weekly" -> listOf(40f, 55f, 30f, 60f)
            "monthly" -> listOf(50f, 45f, 35f, 60f)
            else -> listOf(60f, 50f, 40f, 70f)
        }
        // For HorizontalBarChart, we want entries with y = value
        val entries = vals.mapIndexed { i, v -> BarEntry(i.toFloat(), v) }
        val set = BarDataSet(entries, "")
        set.color = resources.getColor(android.R.color.holo_blue_dark)
        set.valueTextSize = 12f
        val data = BarData(set)
        data.barWidth = 0.6f
        hbarChartBranches.data = data

        // formatting x labels vertically (we use index -> label)
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

    private fun setupExportButtons(root: View) {
        val btnPdf = root.findViewById<Button>(R.id.btn_export_pdf)
        val btnCsv = root.findViewById<Button>(R.id.btn_export_csv)
        val btnExcel = root.findViewById<Button>(R.id.btn_export_excel)

        btnPdf.setOnClickListener {
            exportToPdf(root)
        }
        btnCsv.setOnClickListener {
            exportToCsv(requireContext())
        }
        btnExcel.setOnClickListener {
            exportToExcel(requireContext())
        }
    }

    // --------- Export PDF: capture scroll content to pdf ----------
    private fun exportToPdf(viewRoot: View) {
        try {
            // Render view to bitmap: measure full height of ScrollView content
            val scroll = viewRoot.findViewById<View>(R.id.scrollMain)
            // create bitmap of content
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

            Toast.makeText(requireContext(), "PDF disimpan: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Gagal ekspor PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // --------- Export CSV ----------
    private fun exportToCsv(ctx: Context) {
        try {
            // build CSV header + some sample rows (replace with real data)
            val csvBuilder = StringBuilder()
            csvBuilder.append("Label, Value\n")
            // sample: monthly trend (you may replace with actual values)
            csvBuilder.append("Jan,60\nFeb,62\nMar,68\nApr,70\nMei,72\nJun,74\n")

            val fileName = "report_${timeStamp()}.csv"
            val file = File(ctx.getExternalFilesDir(null), fileName)
            val writer = OutputStreamWriter(FileOutputStream(file))
            writer.write(csvBuilder.toString())
            writer.flush()
            writer.close()

            Toast.makeText(ctx, "CSV disimpan: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(ctx, "Gagal ekspor CSV: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // --------- Export Excel (.xlsx) using Apache POI ----------
    private fun exportToExcel(ctx: Context) {
        try {
            val workbook: Workbook = XSSFWorkbook()
            val sheet: Sheet = workbook.createSheet("Report")

            // Header style
            val headerStyle: CellStyle = workbook.createCellStyle()
            val headerFont: Font = workbook.createFont()
            headerFont.bold = true
            headerStyle.setFont(headerFont)
            headerStyle.fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            headerStyle.fillPattern = FillPatternType.SOLID_FOREGROUND  // ✅ pakai FillPatternType, bukan CellStyle.SOLID_FOREGROUND

            // Buat header row
            val headerRow = sheet.createRow(0)
            val cellLabel = headerRow.createCell(0)
            val cellValue = headerRow.createCell(1)

            cellLabel.setCellValue("Label")
            cellValue.setCellValue("Value")

            cellLabel.cellStyle = headerStyle
            cellValue.cellStyle = headerStyle

            // sample data
            val data = listOf(
                Pair("Jan", 60.0),
                Pair("Feb", 62.0),
                Pair("Mar", 68.0),
                Pair("Apr", 70.0),
                Pair("Mei", 72.0),
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

            Toast.makeText(ctx, "Excel disimpan: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(ctx, "Gagal ekspor Excel: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun timeStamp(): String {
        val df = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return df.format(Date())
    }
}