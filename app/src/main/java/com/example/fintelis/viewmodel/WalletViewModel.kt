package com.example.fintelis.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.example.fintelis.data.TransactionRepository
import com.example.fintelis.data.TransactionType
import com.example.fintelis.data.WalletType

class WalletViewModel(private val repo: TransactionRepository) : ViewModel() {

    // 1. Calculate wallet balances from transactions
    val wallets: LiveData<List<WalletData>> = repo.allTransactions.map { transactions ->
        val balances = mutableMapOf<String, Double>()
        // Initialize supported wallets with 0
        val supportedWallets = listOf("Cash", "BCA", "Mandiri", "BNI", "OVO", "GoPay", "DANA", "ShopeePay", "SeaBank", "Blu")
        supportedWallets.forEach { balances[it] = 0.0 }

        transactions.forEach { t ->
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

    fun getWalletLimitProgress(wallet: String, limit: Double): LiveData<Int> {
        return repo.allTransactions.map { transactions ->
            // Filter transactions for this wallet, this month, type expense
            // For now, simplify to just total expense for this wallet to avoid complex date logic here
            // In a real app, inject a date provider
            val expense = transactions
                .filter { it.wallet == wallet && it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }
            
            if (limit > 0) {
                ((expense / limit) * 100).toInt().coerceIn(0, 100)
            } else {
                0
            }
        }
    }
}
