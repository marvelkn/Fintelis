package com.example.fintelis.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.fintelis.data.Customer
import com.example.fintelis.data.RiskCategory
import com.example.fintelis.data.Status
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SortOrder {
    NONE, DATE_DESC, DATE_ASC, SCORE_DESC, SCORE_ASC, RISK_CATEGORY, STATUS
}

class CustomerViewModel : ViewModel() {
    companion object { private const val TAG = "ViewModel_DEBUG" }

    private val _allCustomers = MutableLiveData<List<Customer>>()
    private val _sortedAndFilteredCustomers = MutableLiveData<List<Customer>>()
    val sortedCustomers: LiveData<List<Customer>> get() = _sortedAndFilteredCustomers

    private var currentSortOrder = SortOrder.DATE_DESC
    private var currentSearchQuery = ""

    init {
        if (_allCustomers.value == null) {
            _allCustomers.value = generateDummyData()
            updateList()
            Log.d(TAG, "ViewModel initialized with ${_allCustomers.value?.size} dummy items.")
        }
    }

    fun addCustomer(newCustomer: Customer) {
        val currentList = _allCustomers.value?.toMutableList() ?: mutableListOf()
        currentList.add(newCustomer)
        _allCustomers.value = currentList
        updateList()
    }

    fun deleteCustomers(customersToDelete: Set<Customer>) {
        Log.d(TAG, "--- deleteCustomers CALLED ---")
        Log.d(TAG, "Attempting to delete ${customersToDelete.size} items.")

        val currentList = _allCustomers.value?.toMutableList() ?: return
        Log.d(TAG, "List size BEFORE delete: ${currentList.size}")

        val idsToDelete = customersToDelete.map { it.id }.toSet()
        val newList = currentList.filterNot { idsToDelete.contains(it.id) }

        Log.d(TAG, "List size AFTER delete: ${newList.size}")

        _allCustomers.value = newList
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

    fun searchCustomers(query: String) {
        currentSearchQuery = query
        updateList()
    }

    fun sortCustomers(order: SortOrder) {
        currentSortOrder = order
        updateList()
    }

    private fun updateList() {
        val fullList = _allCustomers.value ?: return
        val filteredList = if (currentSearchQuery.isBlank()) {
            fullList
        } else {
            fullList.filter { customer ->
                customer.name.contains(currentSearchQuery, ignoreCase = true)
            }
        }
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
        Log.d(TAG, "updateList finished. Final list size: ${sortedList.size}")
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