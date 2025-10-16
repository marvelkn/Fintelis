package com.example.fintelis

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.fintelis.databinding.ActivityDashboardBinding
import com.example.fintelis.viewmodel.CustomerViewModel

class DashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBinding

    private val customerViewModel: CustomerViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 1. Inflate layout menggunakan ViewBinding
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // 1. Cari NavController dari NavHostFragment yang sudah kita buat di XML
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // 2. Hubungkan BottomNavigationView dengan NavController
        // Baris ini secara otomatis menangani semua klik di BottomNavigationView!
        binding.bottomNavigation.setupWithNavController(navController)

        // Selesai! Tidak perlu ViewPager, Adapter, atau listener manual.
        // Memastikan ViewModel terbuat saat aplikasi dimulai.
        customerViewModel.sortedCustomers
    }
}