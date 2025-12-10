package com.example.fintelis.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.fintelis.data.Transaction
import com.example.fintelis.data.TransactionType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObjects
import java.text.NumberFormat
import java.util.Locale

class DashboardViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    data class Wallet(val id: String = "", val name: String = "", val balance: Double = 0.0)

    private val _wallets = MutableLiveData<List<Wallet>>()
    val wallets: LiveData<List<Wallet>> = _wallets

    private val _totalBalance = MutableLiveData<Double>()
    val totalBalance: LiveData<Double> = _totalBalance

    init {
        fetchWalletsAndTransactions()
    }

    private fun fetchWalletsAndTransactions() {
        val userId = auth.currentUser?.uid ?: return
        val userDocRef = firestore.collection("users").document(userId)

        userDocRef.collection("wallets").addSnapshotListener { walletSnapshot, e ->
            if (e != null) return@addSnapshotListener
            val walletList = walletSnapshot?.toObjects<Wallet>() ?: emptyList()

            userDocRef.collection("transactions").addSnapshotListener { transactionSnapshot, e2 ->
                if (e2 != null) return@addSnapshotListener
                val allTransactions = transactionSnapshot?.toObjects<Transaction>() ?: emptyList()

                updateBalances(walletList, allTransactions)
            }
        }
    }

    private fun updateBalances(wallets: List<Wallet>, transactions: List<Transaction>) {
        val walletBalances = wallets.associate { it.id to 0.0 }.toMutableMap()
        var total = 0.0

        for (trx in transactions) {
            val amount = if (trx.type == TransactionType.EXPENSE) -trx.amount else trx.amount
            if (walletBalances.containsKey(trx.walletId)) {
                walletBalances[trx.walletId] = walletBalances.getValue(trx.walletId) + amount
            }
            total += amount
        }

        val updatedWallets = wallets.map { it.copy(balance = walletBalances[it.id] ?: 0.0) }
        _wallets.value = updatedWallets
        _totalBalance.value = total
    }

    fun formatRupiah(number: Double): String {
        val localeID = Locale("id", "ID")
        val format = NumberFormat.getCurrencyInstance(localeID)
        return format.format(number).replace("Rp", "IDR ")
    }
}