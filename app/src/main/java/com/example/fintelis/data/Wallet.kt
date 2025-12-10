package com.example.fintelis.data

data class Wallet(
    val id: String = "",
    val name: String = "",
    val saldo: Double = 0.0,
    val type: WalletType = WalletType.CASH
)
