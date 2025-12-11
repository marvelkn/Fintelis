package com.example.fintelis

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.fintelis.utils.NotificationScheduler // Perhatikan import ini

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Pastikan Alarm Terdaftar (Silent check)
        // Ini memastikan alarm tetap hidup meskipun user jarang buka app
        if (NotificationScheduler.isReminderEnabled(this)) {
            NotificationScheduler.scheduleDailyReminder(this)
        }

        // 2. Logika Routing
        val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val onboardingCompleted = sharedPref.getBoolean("onboarding_completed", false)
        val userLoggedIn = sharedPref.getBoolean("user_logged_in", false)

        val targetActivity = when {
            !onboardingCompleted -> OnboardingActivity::class.java
            !userLoggedIn -> AuthActivity::class.java
            else -> DashboardActivity::class.java
        }

        // 3. Pindah Halaman
        val intent = Intent(this, targetActivity)
        startActivity(intent)
        finish()
    }
}