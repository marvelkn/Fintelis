package com.example.fintelis.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.fintelis.data.Customer
import com.example.fintelis.data.RiskCategory
import com.example.fintelis.data.Status

class CustomerViewModel : ViewModel() {

    private val _customers = MutableLiveData<List<Customer>>()
    val customers: LiveData<List<Customer>> get() = _customers

    init {
        // Hanya inisialisasi dengan daftar kosong jika datanya masih null
        if (_customers.value == null) {
            _customers.value = generateDummyData()
        }
    }

    // Fungsi untuk menambahkan nasabah baru
    fun addCustomer(newCustomer: Customer) {
        val currentList = _customers.value?.toMutableList() ?: mutableListOf()
        currentList.add(newCustomer)
        _customers.value = currentList // Ini akan memicu update di Fragment
    }

    fun updateCustomerStatus(customerId: String, newStatus: Status) {
        val currentList = _customers.value?.toMutableList() ?: return
        val customerIndex = currentList.indexOfFirst { it.id == customerId }

        if (customerIndex != -1) {
            val updatedCustomer = currentList[customerIndex].copy(status = newStatus)
            currentList[customerIndex] = updatedCustomer
            _customers.value = currentList
        }
    }

    private fun generateDummyData(): List<Customer> {
        return listOf(
            Customer("ID-20251012001", "Budi Santoso", "Oct 12, 2025", 780, RiskCategory.LOW, Status.PENDING),
            Customer("ID-20251011002", "Citra Lestari", "Oct 11, 2025", 420, RiskCategory.HIGH, Status.PENDING),
            Customer("ID-20251010003", "Agus Wijaya", "Oct 10, 2025", 650, RiskCategory.MEDIUM, Status.PENDING)
        )
    }
}
