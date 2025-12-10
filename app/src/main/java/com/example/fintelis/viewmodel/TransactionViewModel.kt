package com.example.fintelis.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.fintelis.data.AppDatabase
import com.example.fintelis.data.Transaction
import com.example.fintelis.data.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class SortOrder { DATE_DESC, DATE_ASC, AMOUNT_DESC, AMOUNT_ASC }
enum class FilterType { ALL, INCOME, EXPENSE }

data class WalletData(
    val name: String,
    val balance: Double
)

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).transactionDao()
    private val _allData = dao.getAllTransactions()
    val allTransactions: LiveData<List<Transaction>> = _allData

    val displayedTransactions = MediatorLiveData<List<Transaction>>()
    val income = MediatorLiveData<Double>()
    val expense = MediatorLiveData<Double>()
    val total = MediatorLiveData<Double>()

    private val _currentMonth = MutableLiveData<Calendar>()
    val currentMonth: LiveData<Calendar> = _currentMonth

    // Wallet Balances
    val walletBalances: LiveData<List<WalletData>> = _allData.map { transactions ->
        val balances = mutableMapOf<String, Double>()
        // Initialize supported wallets with 0
        val supportedWallets = listOf("Cash", "BCA", "Mandiri", "BNI", "OVO", "GoPay", "DANA", "ShopeePay", "SeaBank", "Blu")
        supportedWallets.forEach { balances[it] = 0.0 }

        // Use the lambda parameter 'transactions' which is inferred as List<Transaction>
        for (t in transactions) {
            val walletName = t.wallet
            val current = balances[walletName] ?: 0.0
            if (t.type == TransactionType.INCOME) {
                balances[walletName] = current + t.amount
            } else {
                balances[walletName] = current - t.amount
            }
        }
        balances.map { WalletData(it.key, it.value) }.sortedByDescending { it.balance }
    }

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
                transactions.add(Transaction(UUID.randomUUID().toString(), "Gaji Bulan Ini", 12000000.0, TransactionType.INCOME, formatDate(cal), "Gaji", "BCA"))
                cal.add(Calendar.DAY_OF_MONTH, -1)
                transactions.add(Transaction(UUID.randomUUID().toString(), "Sewa Kost", 2500000.0, TransactionType.EXPENSE, formatDate(cal), "Sewa", "BCA"))
                cal.add(Calendar.DAY_OF_MONTH, -3)
                transactions.add(Transaction(UUID.randomUUID().toString(), "Belanja Mingguan", 750000.0, TransactionType.EXPENSE, formatDate(cal), "Belanja", "Cash"))
                cal.add(Calendar.DAY_OF_MONTH, -5)
                transactions.add(Transaction(UUID.randomUUID().toString(), "Makan di Luar", 150000.0, TransactionType.EXPENSE, formatDate(cal), "Makanan", "OVO"))

                // --- LAST MONTH ---
                cal.time = Date()
                cal.add(Calendar.MONTH, -1)
                transactions.add(Transaction(UUID.randomUUID().toString(), "Bonus Proyek Lama", 5000000.0, TransactionType.INCOME, formatDate(cal), "Bonus", "Mandiri"))
                transactions.add(Transaction(UUID.randomUUID().toString(), "Tagihan Bulan Lalu", 1800000.0, TransactionType.EXPENSE, formatDate(cal), "Tagihan", "BCA"))

                // --- NEXT MONTH ---
                cal.time = Date()
                cal.add(Calendar.MONTH, 1)
                transactions.add(Transaction(UUID.randomUUID().toString(), "Gaji Bulan Depan", 12000000.0, TransactionType.INCOME, formatDate(cal), "Gaji", "BCA"))
                transactions.add(Transaction(UUID.randomUUID().toString(), "Tiket Konser", 1200000.0, TransactionType.EXPENSE, formatDate(cal), "Hiburan", "GoPay"))

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