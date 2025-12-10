package com.example.fintelis.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.fintelis.data.Transaction
import com.example.fintelis.data.TransactionType
import java.text.NumberFormat
import java.util.Locale

class DashboardViewModel : ViewModel() {

    // Helper class untuk menampung total saldo
    data class WalletBalances(
        val totalAll: Double = 0.0,
        val bca: Double = 0.0,
        val blu: Double = 0.0,
        val bni: Double = 0.0,
        val mandiri: Double = 0.0,
        val dana: Double = 0.0,
        val gopay: Double = 0.0,
        val ovo: Double = 0.0,
        val spay: Double = 0.0,
        val mainCash: Double = 0.0
    )

    private val _walletBalances = MutableLiveData<WalletBalances>()
    val walletBalances: LiveData<WalletBalances> = _walletBalances

    fun loadTransactionData() {
        val transactions = getMockTransactions()
        val balances = calculateBalances(transactions)
        _walletBalances.value = balances
    }

    private fun calculateBalances(transactions: List<Transaction>): WalletBalances {
        var totalAll = 0.0
        var bca = 0.0
        var blu = 0.0
        var bni = 0.0
        var mandiri = 0.0
        var dana = 0.0
        var gopay = 0.0
        var ovo = 0.0
        var spay = 0.0
        var mainCash = 0.0

        for (trx in transactions) {
            // Cek tipe transaksi (INCOME tambah, EXPENSE kurang)
            val amount = if (trx.type == TransactionType.EXPENSE) {
                -trx.amount
            } else {
                trx.amount
            }

            // Masukkan ke dompet yang sesuai berdasarkan nama wallet
            // Menggunakan uppercase agar tidak sensitif huruf besar/kecil
            when (trx.wallet.uppercase()) {
                "BCA" -> bca += amount
                "BLU" -> blu += amount
                "BNI" -> bni += amount
                "MANDIRI" -> mandiri += amount
                "DANA" -> dana += amount
                "GOPAY" -> gopay += amount
                "OVO" -> ovo += amount
                "SPAY" -> spay += amount
                "MAIN", "CASH" -> mainCash += amount // Handle default "Cash" atau "Main"
                else -> mainCash += amount // Default ke cash jika wallet tidak dikenali
            }

            // Tambah ke total global
            totalAll += amount
        }

        return WalletBalances(totalAll, bca, blu, bni, mandiri, dana, gopay, ovo, spay, mainCash)
    }

    // Helper format Rupiah
    fun formatRupiah(number: Double): String {
        val localeID = Locale("id", "ID")
        val format = NumberFormat.getCurrencyInstance(localeID)
        return format.format(number).replace("Rp", "IDR ")
    }

    // Mock Data disesuaikan dengan Transaction.kt yang baru
    private fun getMockTransactions(): List<Transaction> {
        return listOf(
            Transaction(
                id = "1", // Ubah Int ke String
                title = "Gaji Bulanan",
                amount = 10000000.0,
                type = TransactionType.INCOME,
                date = "2025-12-01", // Ubah Long ke String
                category = "Salary", // Tambahkan Category
                wallet = "BCA"
            ),
            Transaction(
                id = "2",
                title = "Makan Siang",
                amount = 50000.0,
                type = TransactionType.EXPENSE,
                date = "2025-12-02",
                category = "Food",
                wallet = "GOPAY"
            ),
            Transaction(
                id = "3",
                title = "Topup E-Money",
                amount = 500000.0,
                type = TransactionType.EXPENSE,
                date = "2025-12-03",
                category = "Topup",
                wallet = "BCA" // Uang keluar dari BCA
            ),
            Transaction(
                id = "4",
                title = "Terima Topup",
                amount = 500000.0,
                type = TransactionType.INCOME,
                date = "2025-12-03",
                category = "Topup",
                wallet = "DANA" // Uang masuk ke DANA
            ),
            Transaction(
                id = "5",
                title = "Proyek Freelance",
                amount = 2000000.0,
                type = TransactionType.INCOME,
                date = "2025-12-04",
                category = "Freelance",
                wallet = "MAIN" // Masuk ke Main/Cash
            )
        )
    }
}