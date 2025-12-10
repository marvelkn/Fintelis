package com.example.fintelis.data

import com.google.firebase.firestore.DocumentId

data class Wallet(
    @DocumentId val id: String = "",
    val name: String = "",
    // This balance is for display and will be calculated, not stored directly in Firestore
    val balance: Double = 0.0
)
