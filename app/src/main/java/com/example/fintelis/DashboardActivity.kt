package com.example.fintelis

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.fintelis.databinding.ActivityDashboardBinding

class DashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 1. Inflate layout menggunakan ViewBinding
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2. Setup ViewPager Adapter
        val pagerAdapter = DashboardPagerAdapter(this)
        binding.viewPagerDashboard.adapter = pagerAdapter

        // 3. Hubungkan BottomNavigationView dengan ViewPager
        // Aksi saat item di navbar diklik -> Pindahkan halaman ViewPager
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.mainDashboard -> binding.viewPagerDashboard.currentItem = 0
                R.id.customerListFragment -> binding.viewPagerDashboard.currentItem = 1
                R.id.nav_analysis -> binding.viewPagerDashboard.currentItem = 2
                R.id.nav_visualization -> binding.viewPagerDashboard.currentItem = 3
                R.id.settingsFragment -> binding.viewPagerDashboard.currentItem = 4
                else -> false // Penting untuk menangani kasus lain
            }
            true // Item terpilih
        }

        // 4. Hubungkan ViewPager dengan BottomNavigationView
        // Aksi saat halaman digeser (swipe) -> Tandai item di navbar
        binding.viewPagerDashboard.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.bottomNavigation.menu.getItem(position).isChecked = true
            }
        })
    }

    /**
     * Adapter untuk mengelola fragment-fragment di dalam ViewPager2.
     */
    private inner class DashboardPagerAdapter(activity: AppCompatActivity) :
        FragmentStateAdapter(activity) {
        // Jumlah total halaman/fragment
        override fun getItemCount(): Int = 5

        // Membuat fragment yang sesuai untuk setiap posisi
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> DashboardFragment() // Ganti dengan nama fragment Anda yang benar
                1 -> CustomerListFragment() // Contoh
                2 -> AnalysisFragment() // Contoh
                3 -> VisualizationFragment() // Contoh
                4 -> SettingsFragment() // Contoh
                else -> throw IllegalStateException("Posisi tidak valid: $position")
            }
        }
    }
}