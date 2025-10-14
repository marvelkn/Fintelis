package com.example.fintelis

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.fintelis.viewmodel.CustomerViewModel

class MainActivity : AppCompatActivity() {

    // Baris ini SANGAT PENTING.
    // Ini membuat ViewModel di level Activity agar bisa di-share.
    private val customerViewModel: CustomerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Memastikan ViewModel terbuat saat aplikasi dimulai.
        customerViewModel.customers
    }
}