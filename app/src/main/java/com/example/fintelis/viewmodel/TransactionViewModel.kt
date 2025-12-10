package com.example.fintelis.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
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

    private val _activeWalletId = MutableLiveData<String?>()
    val activeWalletId: LiveData<String?> = _activeWalletId

    var currentSortOrder = SortOrder.DATE_DESC
        private set
    var currentFilterType = FilterType.ALL
        private set
    private var currentSearchQuery = ""

    init {
        _currentMonth.value = Calendar.getInstance()
        // Observer for active wallet changes
        activeWalletId.observeForever { walletId ->
            if (walletId != null) {
                fetchTransactions(walletId)
            }
        }

        displayedTransactions.addSource(_allData) { list -> processList(list) }
        displayedTransactions.addSource(_currentMonth) { _allData.value?.let { list -> processList(list) } }
    }

    fun setActiveWallet(walletId: String) {
        _activeWalletId.value = walletId
    }

    private fun fetchTransactions(walletId: String) {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId).collection("transactions")
            .whereEqualTo("walletId", walletId)
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

    fun addTransaction(t: Transaction) {
        val userId = auth.currentUser?.uid ?: return
        val walletId = _activeWalletId.value ?: return
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