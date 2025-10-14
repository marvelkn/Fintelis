package com.example.fintelis

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Cukup tampilkan layout yang berisi NavHostFragment
        setContentView(R.layout.activity_main)
    }
}