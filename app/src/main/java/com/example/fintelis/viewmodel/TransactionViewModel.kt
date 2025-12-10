package com.example.fintelis.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fintelis.data.AppDatabase
import com.example.fintelis.data.Transaction
import com.example.fintelis.data.TransactionType
// Import Library Chart
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class SortOrder { DATE_DESC, DATE_ASC, AMOUNT_DESC, AMOUNT_ASC }
enum class FilterType { ALL, INCOME, EXPENSE }

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).transactionDao()
    private val _allData = dao.getAllTransactions()

    // Jika Anda ingin grafik mengambil semua data tanpa filter bulan, gunakan ini di Fragment:
    // val allTransactions: LiveData<List<Transaction>> = _allData

    val displayedTransactions = MediatorLiveData<List<Transaction>>()
    val income = MediatorLiveData<Double>()
    val expense = MediatorLiveData<Double>()
    val total = MediatorLiveData<Double>()

    private val _currentMonth = MutableLiveData<Calendar>()
    val currentMonth: LiveData<Calendar> = _currentMonth

    var currentSortOrder = SortOrder.DATE_DESC
        private set
    var currentFilterType = FilterType.ALL
        private set
    private var currentSearchQuery = ""

    init {
        // Default to the current local month
        _currentMonth.value = Calendar.getInstance()

        seedDatabaseIfEmpty()
        displayedTransactions.addSource(_allData) { list -> processList(list) }
        displayedTransactions.addSource(_currentMonth) { _allData.value?.let { list -> processList(list) } }
    }

    fun changeMonth(amount: Int) {
        val calendar = _currentMonth.value ?: return
        val newCal = calendar.clone() as Calendar
        newCal.add(Calendar.MONTH, amount)
        _currentMonth.value = newCal
    }

    private fun seedDatabaseIfEmpty() {
        viewModelScope.launch(Dispatchers.IO) {
            if (dao.getCount() == 0) {
                val transactions = mutableListOf<Transaction>()
                val cal = Calendar.getInstance() // Use a single calendar instance

                // --- CURRENT MONTH (HEAVY) ---
                cal.time = Date() // Reset to today
                transactions.add(Transaction(UUID.randomUUID().toString(), "Gaji Bulan Ini", 12000000.0, TransactionType.INCOME, formatDate(cal), "Gaji"))
                cal.add(Calendar.DAY_OF_MONTH, -1)
                transactions.add(Transaction(UUID.randomUUID().toString(), "Sewa Kost", 2500000.0, TransactionType.EXPENSE, formatDate(cal), "Sewa"))
                cal.add(Calendar.DAY_OF_MONTH, -3)
                transactions.add(Transaction(UUID.randomUUID().toString(), "Belanja Mingguan", 750000.0, TransactionType.EXPENSE, formatDate(cal), "Belanja"))
                cal.add(Calendar.DAY_OF_MONTH, -5)
                transactions.add(Transaction(UUID.randomUUID().toString(), "Makan di Luar", 150000.0, TransactionType.EXPENSE, formatDate(cal), "Makanan"))

                // --- LAST MONTH ---
                cal.time = Date()
                cal.add(Calendar.MONTH, -1)
                transactions.add(Transaction(UUID.randomUUID().toString(), "Bonus Proyek Lama", 5000000.0, TransactionType.INCOME, formatDate(cal), "Bonus"))
                transactions.add(Transaction(UUID.randomUUID().toString(), "Tagihan Bulan Lalu", 1800000.0, TransactionType.EXPENSE, formatDate(cal), "Tagihan"))

                // --- NEXT MONTH ---
                cal.time = Date()
                cal.add(Calendar.MONTH, 1)
                transactions.add(Transaction(UUID.randomUUID().toString(), "Gaji Bulan Depan", 12000000.0, TransactionType.INCOME, formatDate(cal), "Gaji"))
                transactions.add(Transaction(UUID.randomUUID().toString(), "Tiket Konser", 1200000.0, TransactionType.EXPENSE, formatDate(cal), "Hiburan"))

                dao.insertAll(transactions)
            }
        }
    }

    fun addTransaction(t: Transaction) = viewModelScope.launch(Dispatchers.IO) { dao.insertTransaction(t) }
    fun deleteTransactions(items: Set<Transaction>) = viewModelScope.launch(Dispatchers.IO) { items.forEach { dao.deleteTransaction(it) } }

    fun searchTransactions(query: String) {
        currentSearchQuery = query
        _allData.value?.let { processList(it) }
    }

    fun setDisplayOptions(sort: SortOrder, filter: FilterType) {
        currentSortOrder = sort
        currentFilterType = filter
        _allData.value?.let { processList(it) }
    }

    private fun processList(list: List<Transaction>) {
        val cal = _currentMonth.value ?: return

        val filteredList = list.filter { transaction ->
            val transCal = Calendar.getInstance()
            transCal.time = parseDate(transaction.date)
            val dateMatch = transCal.get(Calendar.MONTH) == cal.get(Calendar.MONTH) && transCal.get(Calendar.YEAR) == cal.get(Calendar.YEAR)

            val searchMatch = if (currentSearchQuery.isBlank()) true else {
                transaction.title.contains(currentSearchQuery, true) || transaction.category.contains(currentSearchQuery, true)
            }

            val typeMatch = when (currentFilterType) {
                FilterType.INCOME -> transaction.type == TransactionType.INCOME
                FilterType.EXPENSE -> transaction.type == TransactionType.EXPENSE
                else -> true
            }

            dateMatch && searchMatch && typeMatch
        }

        val sorted = when (currentSortOrder) {
            SortOrder.AMOUNT_DESC -> filteredList.sortedByDescending { it.amount }
            SortOrder.AMOUNT_ASC -> filteredList.sortedBy { it.amount }
            SortOrder.DATE_ASC -> filteredList.sortedBy { parseDate(it.date) }
            else -> filteredList.sortedByDescending { parseDate(it.date) }
        }
        displayedTransactions.postValue(sorted)

        var inc = 0.0
        var exp = 0.0
        sorted.forEach { if (it.type == TransactionType.INCOME) inc += it.amount else exp += it.amount }
        income.postValue(inc)
        expense.postValue(exp)
        total.postValue(inc - exp)
    }

    // =================================================================================
    // CHART DATA PROCESSING FUNCTIONS (Added for VisualizationFragment)
    // =================================================================================

    // 1. Data untuk Donut Chart (Pengeluaran per Kategori)
    fun getExpenseByCategoryData(transactions: List<Transaction>): List<PieEntry> {
        val expenseMap = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val entries = ArrayList<PieEntry>()
        for ((category, amount) in expenseMap) {
            entries.add(PieEntry(amount.toFloat(), category))
        }
        return entries
    }

    // 2. Data untuk Bar Chart (Pemasukan vs Pengeluaran per Bulan)
    fun getIncomeExpenseBarData(transactions: List<Transaction>): Pair<List<String>, List<BarEntry>> {
        val fmt = SimpleDateFormat("MMM yyyy", Locale.US)

        val sortedList = transactions.sortedBy { parseDate(it.date) }
        val groupedData = sortedList.groupBy { fmt.format(parseDate(it.date)) }

        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        var index = 0f

        for ((dateLabel, transList) in groupedData) {
            val income = transList.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }.toFloat()
            val expense = transList.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }.toFloat()

            // Stacked Bar (Income, Expense)
            entries.add(BarEntry(index, floatArrayOf(income, expense)))
            labels.add(dateLabel)
            index++
        }
        return Pair(labels, entries)
    }

    // 3. Data untuk Line Chart (Tren Saldo/Keuangan)
    fun getFinancialTrendData(transactions: List<Transaction>): List<Entry> {
        val sortedList = transactions.sortedBy { parseDate(it.date) }
        val entries = ArrayList<Entry>()

        var currentBalance = 0.0
        val groupedByDay = sortedList.groupBy { parseDate(it.date) }
        val sortedDates = groupedByDay.keys.sorted()

        for (date in sortedDates) {
            val dailyTrans = groupedByDay[date] ?: continue
            for (t in dailyTrans) {
                if (t.type == TransactionType.INCOME) currentBalance += t.amount
                else currentBalance -= t.amount
            }
            entries.add(Entry(date.time.toFloat(), currentBalance.toFloat()))
        }
        return entries
    }

    // Helper Public untuk akses parseDate dari luar jika diperlukan
    fun parseDatePublic(d: String): Date {
        return parseDate(d)
    }

    // =================================================================================
    // END CHART FUNCTIONS
    // =================================================================================

    private fun parseDate(d: String): Date {
        return try {
            SimpleDateFormat("MMM dd, yyyy", Locale.US).parse(d) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }

    private fun formatDate(calendar: Calendar): String {
        return SimpleDateFormat("MMM dd, yyyy", Locale.US).format(calendar.time)
    }
}