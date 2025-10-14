package com.example.fintelis.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Customer(
    val id: String,
    val name: String,
    val submissionDate: String,
    val creditScore: Int,
    val riskCategory: RiskCategory,
    val status: Status
) : Parcelable

enum class RiskCategory {
    LOW, MEDIUM, HIGH
}

enum class Status {
    APPROVED, REJECTED, PENDING
}
    
