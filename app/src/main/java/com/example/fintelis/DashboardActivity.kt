package com.example.fintelis

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.fintelis.databinding.ActivityDashboardBinding
import com.example.fintelis.utils.NotificationScheduler
import com.example.fintelis.viewmodel.TransactionViewModel

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val transactionViewModel: TransactionViewModel by viewModels()
    private lateinit var navController: NavController //

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            NotificationScheduler.setReminderStatus(this, true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Navigasi
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController //

        // Hubungkan Bottom Navigation
        binding.bottomNavigation.setupWithNavController(navController) //

        // --- SOLUSI: Paksa kembali ke Home saat tab Home ditekan ---
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.mainDashboard -> {
                    // Membersihkan backstack hingga ke mainDashboard
                    navController.popBackStack(R.id.mainDashboard, false)
                    true
                }
                else -> {
                    // Navigasi standar untuk menu lainnya
                    navController.navigate(item.itemId)
                    true
                }
            }
        }

        // Handle klik ulang pada tab yang sama
        binding.bottomNavigation.setOnItemReselectedListener { item ->
            if (item.itemId == R.id.mainDashboard) {
                navController.popBackStack(R.id.mainDashboard, false)
            }
        }

        transactionViewModel.displayedTransactions
        checkAndRequestNotificationPermission()
    }

    // Memungkinkan navigasi "Up" (Kembali) di toolbar
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            when {
                ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                    NotificationScheduler.scheduleDailyReminder(this)
                }
                else -> {
                    requestPermissionLauncher.launch(permission)
                }
            }
        }
    }
}