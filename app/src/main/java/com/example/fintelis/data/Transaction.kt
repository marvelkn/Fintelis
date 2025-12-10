package com.example.fintelis.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

enum class TransactionType { INCOME, EXPENSE }

@Parcelize
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey val id: String,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val date: String,
    val category: String,
    val wallet: String = "Cash"
) : Parcelable