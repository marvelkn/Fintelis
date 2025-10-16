package com.example.fintelis.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// Enum untuk kategori risiko
enum class RiskCategory {
    LOW, MEDIUM, HIGH
}

// Enum untuk status analisis
enum class Status {
    APPROVED, REJECTED, PENDING
}

@Parcelize
data class Customer(
    val id: String,
    val name: String,
    val submissionDate: String,
    val creditScore: Int,
    val riskCategory: RiskCategory, // Menggunakan enum
    val status: Status            // Menggunakan enum
) : Parcelable

