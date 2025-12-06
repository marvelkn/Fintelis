// File: app/src/main/java/com/example/fintelis/DashboardActivity.kt
package com.example.fintelis

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.fintelis.databinding.ActivityDashboardBinding
import com.example.fintelis.viewmodel.TransactionViewModel

class DashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBinding

    // ViewModel ini akan otomatis memuat data dari Database (atau dummy jika kosong)
    // saat DashboardActivity dibuat.
    private val transactionViewModel: TransactionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Navigasi Bawah
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)

        // Pemicu agar data dimuat (init block di ViewModel jalan)
        transactionViewModel.displayedTransactions
    }
}