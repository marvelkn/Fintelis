// File: app/src/main/java/com/example/fintelis/DashboardActivity.kt
package com.example.fintelis

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.fintelis.databinding.ActivityDashboardBinding
import com.example.fintelis.utils.NotificationScheduler // Perhatikan import ini
import com.example.fintelis.viewmodel.TransactionViewModel

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val transactionViewModel: TransactionViewModel by viewModels()

    // Setup Launcher Izin (Android 13+)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Jika diizinkan, aktifkan alarm & simpan status
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
        val navController = navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)

        // Load Data
        transactionViewModel.displayedTransactions

        // Cek Izin Notifikasi
        checkAndRequestNotificationPermission()
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS

            when {
                ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                    // Izin sudah ada, pastikan alarm terjadwal
                    NotificationScheduler.scheduleDailyReminder(this)
                }
                else -> {
                    // Minta izin
                    requestPermissionLauncher.launch(permission)
                }
            }
        }
    }
}