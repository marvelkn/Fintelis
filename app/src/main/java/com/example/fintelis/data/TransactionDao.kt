package com.example.fintelis.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): LiveData<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<Transaction>)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getCount(): Int

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'INCOME'")
    fun getTotalIncome(): LiveData<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE'")
    fun getTotalExpense(): LiveData<Double?>

    @Query("""
    SELECT SUM(amount) FROM transactions 
    WHERE type = 'EXPENSE' 
    AND strftime('%m', date) = strftime('%m', 'now') 
    AND strftime('%Y', date) = strftime('%Y', 'now')
    """)
    fun getMonthlyExpense(): LiveData<Double?>

}