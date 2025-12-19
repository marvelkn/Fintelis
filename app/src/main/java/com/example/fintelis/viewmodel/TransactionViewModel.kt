package com.example.fintelis.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.example.fintelis.data.Transaction
import com.example.fintelis.data.TransactionType
import com.example.fintelis.data.Wallet
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.toObjects
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class SortOrder { DATE_DESC, DATE_ASC, AMOUNT_DESC, AMOUNT_ASC }
enum class FilterType { ALL, INCOME, EXPENSE }

class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Data Master (Semua data dari Firestore)
    private val _allData = MutableLiveData<List<Transaction>>()

    // Expose variabel ini agar Fragment bisa mengaksesnya
    val transactions: LiveData<List<Transaction>> = _allData

    val wallets = MutableLiveData<List<Wallet>>()

    val displayedTransactions = MediatorLiveData<List<Transaction>>()
    val income = MediatorLiveData<Double>()
    val expense = MediatorLiveData<Double>()
    val total = MediatorLiveData<Double>()

    val incomePercentage = MutableLiveData<Float>()
    val expensePercentage = MutableLiveData<Float>()
    val incomeNominal = MutableLiveData<Double>()
    val expenseNominal = MutableLiveData<Double>()
    val incomeExpensePieData = MutableLiveData<List<PieEntry>>()

    private val _currentMonth = MutableLiveData<Calendar>()
    val currentMonth: LiveData<Calendar> = _currentMonth

    private val _activeWalletId = MutableLiveData<String?>()
    val activeWalletId: LiveData<String?> = _activeWalletId

    private var transactionListener: ListenerRegistration? = null

    var currentSortOrder = SortOrder.DATE_DESC
        private set
    var currentFilterType = FilterType.ALL
        private set
    private var currentSearchQuery = ""

    init {
        _activeWalletId.value = "ALL"
        _currentMonth.value = Calendar.getInstance()
        fetchWallets()

        activeWalletId.observeForever { walletId ->
            fetchTransactions(walletId)
        }

        displayedTransactions.addSource(_allData) { list -> processList(list) }
        displayedTransactions.addSource(_currentMonth) {
            _allData.value?.let { list -> processList(list) }
        }
    }

    private fun fetchWallets() {
        val userId = auth.currentUser?.uid ?: return
        // Daftar nama wallet yang kita dukung di UI
        val supportedNames = setOf("BCA", "BLU", "BNI", "MANDIRI", "DANA", "GOPAY", "OVO", "SPAY")

        firestore.collection("users").document(userId).collection("wallets")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener

                val walletList = snapshot?.toObjects<Wallet>() ?: emptyList()
                // Filter agar hanya wallet yang namanya ada di UI yang masuk
                val filtered = walletList.filter { supportedNames.contains(it.name.uppercase()) }

                // Update LiveData wallets
                wallets.postValue(filtered)
            }
    }

    fun setActiveWallet(walletId: String?) {
        if (_activeWalletId.value != walletId) {
            _activeWalletId.value = walletId
        }
    }

    private fun fetchTransactions(walletId: String?) {
        transactionListener?.remove()
        val userId = auth.currentUser?.uid ?: return
        val query = if (walletId == null || walletId == "ALL") {
            firestore.collection("users").document(userId).collection("transactions")
        } else {
            firestore.collection("users").document(userId).collection("transactions")
                .whereEqualTo("walletId", walletId)
        }

        transactionListener = query.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("TransactionViewModel", "Transactions listen failed.", e)
                return@addSnapshotListener
            }
            _allData.value = snapshot?.toObjects() ?: emptyList()
        }
    }

    fun changeMonth(amount: Int) {
        val calendar = _currentMonth.value ?: return
        val newCal = calendar.clone() as Calendar
        newCal.add(Calendar.MONTH, amount)
        _currentMonth.value = newCal
    }

    fun addTransaction(t: Transaction) {
        val userId = auth.currentUser?.uid ?: return
        val walletId = _activeWalletId.value ?: return
        if (walletId == "ALL") return

        val transactionWithWallet = t.copy(walletId = walletId)
        firestore.collection("users").document(userId).collection("transactions").add(transactionWithWallet)
    }

    fun deleteTransactions(items: Set<Transaction>) {
        val userId = auth.currentUser?.uid ?: return
        val collection = firestore.collection("users").document(userId).collection("transactions")
        items.forEach { collection.document(it.id).delete() }
    }

    fun searchTransactions(query: String) {
        currentSearchQuery = query
        _allData.value?.let { processList(it) }
    }

    fun setDisplayOptions(sort: SortOrder, filter: FilterType) {
        currentSortOrder = sort
        currentFilterType = filter
        _allData.value?.let { processList(it) }
    }

    // --- FUNGSI UTAMA YANG DIPERBAIKI ---
    private fun processList(list: List<Transaction>) {
        val cal = _currentMonth.value ?: return

        // 1. Ambil data HANYA berdasarkan BULAN (Tanpa Filter Type/Search)
        val monthlyList = list.filter { transaction ->
            val transCal = Calendar.getInstance()
            try {
                transCal.time = parseDate(transaction.date)
                transCal.get(Calendar.MONTH) == cal.get(Calendar.MONTH) &&
                        transCal.get(Calendar.YEAR) == cal.get(Calendar.YEAR)
            } catch (e: Exception) {
                false
            }
        }

        // 2. Hitung Total Income & Expense (Data Murni)
        val inc = monthlyList.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val exp = monthlyList.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        // Update LiveData Summary
        income.postValue(inc)
        expense.postValue(exp)
        total.postValue(inc - exp)
        incomeNominal.postValue(inc)
        expenseNominal.postValue(exp)

        val totalTransactions = inc + exp
        if (totalTransactions > 0) {
            incomePercentage.postValue(((inc / totalTransactions) * 100).toFloat())
            expensePercentage.postValue(((exp / totalTransactions) * 100).toFloat())
        } else {
            incomePercentage.postValue(0f)
            expensePercentage.postValue(0f)
        }

        // --- BAGIAN INI YANG SEBELUMNYA HILANG ---
        // Kita harus mengisi incomeExpensePieData agar Chart di Dashboard muncul
        val pieEntries = ArrayList<PieEntry>()
        if (inc > 0) {
            pieEntries.add(PieEntry(inc.toFloat(), "Income"))
        }
        if (exp > 0) {
            pieEntries.add(PieEntry(exp.toFloat(), "Expense"))
        }
        incomeExpensePieData.postValue(pieEntries)
        // -----------------------------------------

        // 3. Filter berdasarkan Search & Filter Type (Untuk RecyclerView List Transaksi)
        val filteredList = monthlyList.filter { transaction ->
            val searchMatch = if (currentSearchQuery.isBlank()) true else {
                transaction.title.contains(currentSearchQuery, true) ||
                        transaction.category.contains(currentSearchQuery, true)
            }
            val typeMatch = when (currentFilterType) {
                FilterType.INCOME -> transaction.type == TransactionType.INCOME
                FilterType.EXPENSE -> transaction.type == TransactionType.EXPENSE
                else -> true
            }
            searchMatch && typeMatch
        }

        // 4. Sorting Data Filtered
        val sorted = when (currentSortOrder) {
            SortOrder.AMOUNT_DESC -> filteredList.sortedByDescending { it.amount }
            SortOrder.AMOUNT_ASC -> filteredList.sortedBy { it.amount }
            SortOrder.DATE_ASC -> filteredList.sortedBy { parseDate(it.date) }
            else -> filteredList.sortedByDescending { parseDate(it.date) }
        }

        displayedTransactions.postValue(sorted)
    }

    private fun parseDate(d: String): Date {
        return SimpleDateFormat("MMM dd, yyyy", Locale.US).parse(d)!!
    }

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

    override fun onCleared() {
        super.onCleared()
        transactionListener?.remove()
    }
}