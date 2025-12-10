package com.example.fintelis.data

import com.google.firebase.firestore.Exclude

data class User(
    // ID dokumen Firestore (tidak disimpan di field, tapi di-set manual di ViewModel)
    @get:Exclude var id: String = "",

    val fullName: String = "",
    val email: String = "",
    val phoneNumber: String = "",

    // Field opsional
    val photoUrl: String = ""
)