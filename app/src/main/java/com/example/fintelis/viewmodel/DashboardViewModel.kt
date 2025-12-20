package com.example.fintelis.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.fintelis.data.Wallet
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObjects
import java.text.NumberFormat
import java.util.Locale

class DashboardViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _wallets = MutableLiveData<List<Wallet>>()
    val wallets: LiveData<List<Wallet>> = _wallets

    private val _totalBalance = MutableLiveData<Double>()
    val totalBalance: LiveData<Double> = _totalBalance

    // === TAMBAHAN UNTUK MONTHLY LIMIT ===
    private val _monthlyLimit = MutableLiveData<Int>()
    val monthlyLimit: LiveData<Int> = _monthlyLimit

    init {
        fetchWalletsAndTransactions()
        fetchMonthlyLimit() // Ambil limit saat pertama kali load
    }

    private fun fetchWalletsAndTransactions() {
        val userId = auth.currentUser?.uid ?: return
        val userDocRef = firestore.collection("users").document(userId)

        userDocRef.collection("wallets").addSnapshotListener { walletSnapshot, e ->
            if (e != null) return@addSnapshotListener
            val walletList = walletSnapshot?.toObjects<Wallet>() ?: emptyList()

            _wallets.value = walletList
            _totalBalance.value = walletList.sumOf { it.balance }
        }
    }

    // === FUNGSI BARU: Ambil limit dari Firestore ===
    fun fetchMonthlyLimit() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null || !snapshot.exists()) {
                    _monthlyLimit.value = 0
                    return@addSnapshotListener
                }
                // Mengambil field "monthlyLimit" dari dokumen user
                val limit = snapshot.getLong("monthlyLimit")?.toInt() ?: 0
                _monthlyLimit.value = limit
            }
    }

    // Fungsi untuk menyimpan limit baru ke Firestore
    fun saveMonthlyLimit(limit: Int) {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId)
            .update("monthlyLimit", limit)
            .addOnFailureListener { e ->
                Log.e("DashboardViewModel", "Error saving limit", e)
            }
    }

    fun addNewWallet(walletName: String) {
        val userId = auth.currentUser?.uid ?: return
        val newWallet = Wallet(name = walletName, balance = 0.0)
        firestore.collection("users").document(userId).collection("wallets").add(newWallet)
    }

    fun updateWalletName(walletId: String, newName: String) {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId).collection("wallets").document(walletId)
            .update("name", newName)
    }

    fun deleteWallet(walletId: String) {
        val userId = auth.currentUser?.uid ?: return
        val userDocRef = firestore.collection("users").document(userId)
        val walletRef = userDocRef.collection("wallets").document(walletId)

        userDocRef.collection("transactions").whereEqualTo("walletId", walletId).get()
            .addOnSuccessListener { snapshot ->
                val batch = firestore.batch()
                snapshot.documents.forEach { doc -> batch.delete(doc.reference) }
                batch.delete(walletRef)
                batch.commit().addOnFailureListener {
                    Log.e("DashboardViewModel", "Failed to delete wallet and transactions", it)
                }
            }
            .addOnFailureListener {
                Log.e("DashboardViewModel", "Failed to query transactions for deletion", it)
            }
    }

    fun formatRupiah(number: Double): String {
        val localeID = Locale("id", "ID")
        val format = NumberFormat.getCurrencyInstance(localeID)
        return format.format(number).replace("Rp", "IDR ")
    }
}