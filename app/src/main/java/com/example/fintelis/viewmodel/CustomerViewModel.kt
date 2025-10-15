package com.example.fintelis.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.fintelis.data.Customer
import com.example.fintelis.data.RiskCategory
import com.example.fintelis.data.Status
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

enum class SortOrder {
    NONE, DATE_DESC, DATE_ASC, SCORE_DESC, SCORE_ASC, RISK_CATEGORY, STATUS
}

class CustomerViewModel : ViewModel() {

    private val _allCustomers = MutableLiveData<List<Customer>>()
    private val _sortedAndFilteredCustomers = MutableLiveData<List<Customer>>()
    val sortedCustomers: LiveData<List<Customer>> get() = _sortedAndFilteredCustomers

    private var currentSortOrder = SortOrder.DATE_DESC // Default sort
    private var currentSearchQuery = "" // Variabel untuk menyimpan teks pencarian

    init {
        if (_allCustomers.value == null) {
            _allCustomers.value = generateDummyData()
            updateList() // Panggil fungsi update utama
        }
    }

    // ==========================================================
    // FUNGSI BARU YANG HILANG - INI PERBAIKANNYA
    // ==========================================================
    fun searchCustomers(query: String) {
        currentSearchQuery = query
        updateList()
    }
    // ==========================================================

    fun sortCustomers(order: SortOrder) {
        currentSortOrder = order
        updateList()
    }

    // FUNGSI PUSAT UNTUK FILTER DAN SORTIR
    private fun updateList() {
        val fullList = _allCustomers.value ?: return

        // 1. Filter dulu berdasarkan query
        val filteredList = if (currentSearchQuery.isBlank()) {
            fullList
        } else {
            fullList.filter { customer ->
                customer.name.contains(currentSearchQuery, ignoreCase = true)
            }
        }

        // 2. Sortir hasil filter
        val sortedList = when (currentSortOrder) {
            SortOrder.DATE_DESC -> filteredList.sortedByDescending { parseDate(it.submissionDate) }
            SortOrder.DATE_ASC -> filteredList.sortedBy { parseDate(it.submissionDate) }
            SortOrder.SCORE_DESC -> filteredList.sortedByDescending { it.creditScore }
            SortOrder.SCORE_ASC -> filteredList.sortedBy { it.creditScore }
            SortOrder.RISK_CATEGORY -> filteredList.sortedBy { it.riskCategory }
            SortOrder.STATUS -> filteredList.sortedBy { it.status }
            else -> filteredList
        }

        _sortedAndFilteredCustomers.value = sortedList
    }

    fun addCustomer(newCustomer: Customer) {
        val currentList = _allCustomers.value?.toMutableList() ?: mutableListOf()
        currentList.add(newCustomer)
        _allCustomers.value = currentList
        updateList()
    }

    fun updateCustomerStatus(customerId: String, newStatus: Status) {
        val currentList = _allCustomers.value?.toMutableList() ?: return
        val customerIndex = currentList.indexOfFirst { it.id == customerId }
        if (customerIndex != -1) {
            val updatedCustomer = currentList[customerIndex].copy(status = newStatus)
            currentList[customerIndex] = updatedCustomer
            _allCustomers.value = currentList
            updateList()
        }
    }

    fun getCustomerById(customerId: String): Customer? {
        return _allCustomers.value?.find { it.id == customerId }
    }

    private fun parseDate(dateString: String): Long {
        return try { SimpleDateFormat("MMM dd, yyyy", Locale.US).parse(dateString)?.time ?: 0 }
        catch (e: Exception) { 0 }
    }

    private fun generateDummyData(): List<Customer> {
        return listOf(
            Customer("ID-20251012001", "Budi Santoso", "Oct 12, 2025", 780, RiskCategory.LOW, Status.PENDING),
            Customer("ID-20251011002", "Citra Lestari", "Nov 11, 2025", 420, RiskCategory.HIGH, Status.PENDING),
            Customer("ID-20251010003", "Agus Wijaya", "Sep 10, 2025", 650, RiskCategory.MEDIUM, Status.PENDING)
        )
    }
}