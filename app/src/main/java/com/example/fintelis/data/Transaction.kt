package com.example.fintelis.data

import android.os.Parcelable
import com.google.firebase.firestore.Exclude
import kotlinx.parcelize.Parcelize

enum class TransactionType { INCOME, EXPENSE }

@Parcelize
data class Transaction(
    // ID Dokumen Firestore (Exclude agar tidak double simpan di field)
    @get:Exclude var id: String = "",

    val title: String = "",
    val amount: Double = 0.0,
    val type: TransactionType = TransactionType.EXPENSE,
    val date: String = "",
    val category: String = "",

    // Properti Baru: Menandakan transaksi ini ada di wallet mana
    val wallet: String = "Cash",

    val note: String = ""
) : Parcelable