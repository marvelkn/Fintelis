package com.example.fintelis.data

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize

enum class TransactionType { INCOME, EXPENSE }

@Parcelize
data class Transaction(
    @DocumentId val id: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val type: TransactionType = TransactionType.EXPENSE,
    val date: String = "",
    val category: String = ""
) : Parcelable