package com.example.fintelis.data

import androidx.lifecycle.LiveData

class TransactionRepository(private val dao: TransactionDao) {

    val allTransactions: LiveData<List<Transaction>> = dao.getAllTransactions()
    val totalIncome: LiveData<Double?> = dao.getTotalIncome()
    val totalExpense: LiveData<Double?> = dao.getTotalExpense()
    val monthlyExpense: LiveData<Double?> = dao.getMonthlyExpense()

    suspend fun insertTransaction(transaction: Transaction) {
        dao.insertTransaction(transaction)
    }

    suspend fun insertAll(transactions: List<Transaction>) {
        dao.insertAll(transactions)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        dao.deleteTransaction(transaction)
    }
    
    suspend fun getCount(): Int {
        return dao.getCount()
    }
}