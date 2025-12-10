package com.example.fintelis

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.fintelis.data.Transaction
import com.example.fintelis.data.TransactionType
import com.example.fintelis.viewmodel.TransactionViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

class VisualizationFragment : Fragment() {

    private lateinit var lineChartTrend: LineChart
    private lateinit var barChartScore: BarChart
    private lateinit var hbarChartBranches: HorizontalBarChart
    private lateinit var tabTimeFilter: TabLayout

    private val viewModel: TransactionViewModel by activityViewModels()

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
        setupExportMenu(view)

        // Observe data
        viewModel.displayedTransactions.observe(viewLifecycleOwner) { transactions ->
            updateCharts(transactions, tabTimeFilter.selectedTabPosition)
        }

        return view
    }

    private fun setupTabs() {
        tabTimeFilter.addTab(tabTimeFilter.newTab().setText("WEEKLY"))
        tabTimeFilter.addTab(tabTimeFilter.newTab().setText("MONTHLY"))
        tabTimeFilter.addTab(tabTimeFilter.newTab().setText("YEARLY"))

        tabTimeFilter.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewModel.displayedTransactions.value?.let { transactions ->
                    updateCharts(transactions, tab.position)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
        tabTimeFilter.getTabAt(1)?.select()
    }

    private fun updateCharts(transactions: List<Transaction>, tabPosition: Int) {
        val range = when (tabPosition) {
            0 -> "weekly"
            1 -> "monthly"
            else -> "yearly"
        }
        setupLineChart(transactions, range)
        setupBarScoreChart(transactions, range) // Using this for Income vs Expense
        setupBranchesChart(transactions, range) // Using this for Categories
    }

    private fun setupLineChart(transactions: List<Transaction>, range: String) {
        // Group transactions by date/time based on range
        // For simplicity, let's just show income trend
        val groupedData = groupTransactionsByDate(transactions, range)
        val sortedKeys = groupedData.keys.sorted()
        
        val entries = ArrayList<Entry>()
        val labels = ArrayList<String>()

        sortedKeys.forEachIndexed { index, key ->
            val amount = groupedData[key] ?: 0.0
            entries.add(Entry(index.toFloat(), amount.toFloat()))
            labels.add(key)
        }

        val set = LineDataSet(entries, "Income Trend")
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
        xAxis.valueFormatter = object : ValueFormatter() {
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

    private fun setupBarScoreChart(transactions: List<Transaction>, range: String) {
        // Show Income vs Expense comparison
        var totalIncome = 0.0
        var totalExpense = 0.0

        transactions.forEach {
            if (it.type == TransactionType.INCOME) totalIncome += it.amount
            else totalExpense += it.amount
        }

        val entries = listOf(
            BarEntry(0f, totalIncome.toFloat()),
            BarEntry(1f, totalExpense.toFloat())
        )

        val set = BarDataSet(entries, "Income vs Expense")
        set.colors = listOf(
            Color.parseColor("#FFC107"), // Income Yellow
            Color.parseColor("#EF5350")  // Expense Red
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
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return when (value.toInt()) {
                    0 -> "Income"
                    1 -> "Expense"
                    else -> ""
                }
            }
        }

        barChartScore.axisRight.isEnabled = false
        barChartScore.invalidate()
    }

    private fun setupBranchesChart(transactions: List<Transaction>, range: String) {
        // Show Expenses by Category
        val expenseByCategory = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        expenseByCategory.entries.forEachIndexed { index, entry ->
            entries.add(BarEntry(index.toFloat(), entry.value.toFloat()))
            labels.add(entry.key)
        }

        val set = BarDataSet(entries, "Expense by Category")
        set.color = resources.getColor(android.R.color.holo_blue_dark)
        set.valueTextSize = 12f
        val data = BarData(set)
        data.barWidth = 0.6f
        hbarChartBranches.data = data

        val xAxis = hbarChartBranches.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val idx = value.toInt()
                return if (idx >= 0 && idx < labels.size) labels[idx] else ""
            }
        }
        hbarChartBranches.axisRight.isEnabled = false
        hbarChartBranches.description.isEnabled = false
        hbarChartBranches.invalidate()
    }

    private fun groupTransactionsByDate(transactions: List<Transaction>, range: String): Map<String, Double> {
        val dateFormat = when (range) {
            "weekly" -> SimpleDateFormat("w", Locale.US) // Week of year
            "monthly" -> SimpleDateFormat("MMM", Locale.US) // Month name
            else -> SimpleDateFormat("yyyy", Locale.US) // Year
        }
        
        // We need to parse the stored date string first
        val inputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)

        return transactions
            .filter { it.type == TransactionType.INCOME }
            .groupBy { 
                try {
                    val date = inputFormat.parse(it.date) ?: Date()
                    dateFormat.format(date)
                } catch (e: Exception) {
                    "Unknown"
                }
            }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
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
                        true
                    }
                    R.id.export_csv -> {
                        exportToCsv(requireContext())
                        true
                    }
                    R.id.export_excel -> {
                        exportToExcel(requireContext())
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
            // Draw background to avoid black background in PDF
            canvas.drawColor(Color.WHITE) 
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
            csvBuilder.append("Date, Title, Amount, Type, Category\n")
            
            val transactions = viewModel.displayedTransactions.value ?: emptyList()
            for (t in transactions) {
                csvBuilder.append("${t.date},${t.title},${t.amount},${t.type},${t.category}\n")
            }

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
            val sheet: Sheet = workbook.createSheet("Transactions")

            val headerStyle: CellStyle = workbook.createCellStyle()
            val headerFont: Font = workbook.createFont()
            headerFont.bold = true
            headerStyle.setFont(headerFont)
            headerStyle.fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            headerStyle.fillPattern = FillPatternType.SOLID_FOREGROUND

            val headerRow = sheet.createRow(0)
            val headers = listOf("Date", "Title", "Amount", "Type", "Category")
            for ((i, header) in headers.withIndex()) {
                val cell = headerRow.createCell(i)
                cell.setCellValue(header)
                cell.cellStyle = headerStyle
            }

            val transactions = viewModel.displayedTransactions.value ?: emptyList()
            for ((i, t) in transactions.withIndex()) {
                val row = sheet.createRow(i + 1)
                row.createCell(0).setCellValue(t.date)
                row.createCell(1).setCellValue(t.title)
                row.createCell(2).setCellValue(t.amount)
                row.createCell(3).setCellValue(t.type.toString())
                row.createCell(4).setCellValue(t.category)
            }
            
            for (i in headers.indices) sheet.autoSizeColumn(i)

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
