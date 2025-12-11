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
import com.github.mikephil.charting.data.BarEntry
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
import java.util.UUID

enum class SortOrder { DATE_DESC, DATE_ASC, AMOUNT_DESC, AMOUNT_ASC }
enum class FilterType { ALL, INCOME, EXPENSE }

class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _allData = MutableLiveData<List<Transaction>>()
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

        val supportedWalletNames = setOf("BCA", "BLU", "BNI", "MANDIRI", "DANA", "GOPAY", "OVO", "SPAY")

        firestore.collection("users").document(userId).collection("wallets")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("TransactionViewModel", "Wallets listen failed.", e)
                    return@addSnapshotListener
                }
                val allWallets = snapshot?.toObjects<Wallet>() ?: emptyList()

                val supportedWallets = allWallets.filter { supportedWalletNames.contains(it.name.uppercase()) }
                wallets.value = supportedWallets

                if (_activeWalletId.value == null) {
                    _activeWalletId.value = "ALL"
                }
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
            if (_allData.value?.isEmpty() == true) {
                seedFirebaseIfEmpty()
            }
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
        val walletId = _activeWalletId.value ?: run {
            Log.e("TransactionViewModel", "Cannot add transaction without an active wallet.")
            return
        }

        if (walletId == "ALL") {
            Log.e("TransactionViewModel", "An active wallet must be selected to add a transaction.")
            return
        }

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

    private fun processList(list: List<Transaction>) {
        val cal = _currentMonth.value ?: return

        val filteredList = list.filter { transaction ->
            val transCal = Calendar.getInstance()
            try {
                transCal.time = parseDate(transaction.date)
            } catch (e: Exception) {
                return@filter false
            }
            val dateMatch = transCal.get(Calendar.MONTH) == cal.get(Calendar.MONTH) && transCal.get(Calendar.YEAR) == cal.get(Calendar.YEAR)
            val searchMatch = if (currentSearchQuery.isBlank()) true else {
                transaction.title.contains(currentSearchQuery, true) ||
                        transaction.category.contains(currentSearchQuery, true)
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

        val inc = sorted.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val exp = sorted.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val totalTransactions = inc + exp

        income.postValue(inc)
        expense.postValue(exp)
        total.postValue(inc - exp)

        incomeNominal.postValue(inc)
        expenseNominal.postValue(exp)

        if (totalTransactions > 0) {
            incomePercentage.postValue(((inc / totalTransactions) * 100).toFloat())
            expensePercentage.postValue(((exp / totalTransactions) * 100).toFloat())
        } else {
            incomePercentage.postValue(0f)
            expensePercentage.postValue(0f)
        }

        val pieEntries = mutableListOf<PieEntry>()
        if (inc > 0) {
            pieEntries.add(PieEntry(inc.toFloat(), "Income"))
        }
        if (exp > 0) {
            pieEntries.add(PieEntry(exp.toFloat(), "Expense"))
        }
        incomeExpensePieData.postValue(pieEntries)
    }

    private fun seedFirebaseIfEmpty() {
        val cal = Calendar.getInstance()
        val dummyList = mutableListOf<Transaction>()
        cal.time = Date()
        dummyList.add(Transaction(UUID.randomUUID().toString(), "Initial Balance", 0.0, TransactionType.INCOME, formatDate(cal), "System", "Cash"))
        dummyList.forEach { addTransaction(it) }
    }

    private fun formatDate(calendar: Calendar): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        return sdf.format(calendar.time)
    }

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
            entries.add(BarEntry(index, floatArrayOf(income, expense)))
            labels.add(dateLabel)
            index++
        }
        return Pair(labels, entries)
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

    fun parseDatePublic(d: String): Date {
        return parseDate(d)
    }

    private fun parseDate(d: String): Date {
        return SimpleDateFormat("MMM dd, yyyy", Locale.US).parse(d)!!
    }

    override fun onCleared() {
        super.onCleared()
        transactionListener?.remove()
    }
}