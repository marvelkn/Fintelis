package com.example.fintelis.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.fintelis.data.AppDatabase
import androidx.lifecycle.ViewModel
import com.example.fintelis.data.Transaction
import com.example.fintelis.data.TransactionType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObjects
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
class TransactionViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _allData = MutableLiveData<List<Transaction>>()

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
        _currentMonth.value = Calendar.getInstance()
        fetchTransactions()

        displayedTransactions.addSource(_allData) { list -> processList(list) }
        displayedTransactions.addSource(_currentMonth) { _allData.value?.let { list -> processList(list) } }
    }

    private fun fetchTransactions() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId).collection("transactions")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("TransactionViewModel", "Listen failed.", e)
                    return@addSnapshotListener
                }
                snapshot?.let { _allData.value = it.toObjects() }
            }
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
    fun addTransaction(t: Transaction) {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId).collection("transactions").add(t)
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
}