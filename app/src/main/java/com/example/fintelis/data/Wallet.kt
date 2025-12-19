package com.example.fintelis.data

import com.google.firebase.firestore.DocumentId

data class Wallet(
    @DocumentId val id: String = "",
    val name: String = "",
    val balance: Double = 0.0
)
