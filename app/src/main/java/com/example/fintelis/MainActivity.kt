package com.example.fintelis

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.fintelis.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Dapatkan status dari SharedPreferences
        val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val onboardingCompleted = sharedPref.getBoolean("onboarding_completed", false)
        val userLoggedIn = sharedPref.getBoolean("user_logged_in", false) // Status baru yang perlu disimpan saat login

        // 2. Tentukan tujuan berdasarkan status
        val targetActivity = when {
            !onboardingCompleted -> OnboardingActivity::class.java // Tujuan #1: Onboarding
            !userLoggedIn -> AuthActivity::class.java      // Tujuan #2: Login/Register
            else -> DashboardActivity::class.java           // Tujuan #3: Halaman Utama
        }

        // 3. Jalankan Activity yang sesuai dan tutup MainActivity
        startActivity(Intent(this, targetActivity))
        finish() // Penting! Agar pengguna tidak bisa kembali ke layar ini
    }
}
