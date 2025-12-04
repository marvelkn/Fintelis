package com.example.fintelis

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class OnboardingActivity : AppCompatActivity() {

    private lateinit var imageOnboard: ImageView
    private lateinit var textDescription: TextView
    private lateinit var btnNext: Button

    private val images = listOf(
        R.drawable.onb1,
        R.drawable.onb2,
        R.drawable.onb3
    )

    private val descriptions = listOf(
        "Welcome to Fintelis, your place to take control of money with confidence.",
        "Track spending, set targets, and watch your progress in real time.",
        "Let’s start building healthier financial habits, one smart move at a time."
    )

    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        imageOnboard = findViewById(R.id.imageOnboard)
        textDescription = findViewById(R.id.textDescription)
        btnNext = findViewById(R.id.btnNext)

        updateOnboardingPage()

        btnNext.setOnClickListener {
            if (currentIndex < images.size - 1) {
                currentIndex++
                updateOnboardingPage()
            } else {
                onFinishOnboarding()
            }
        }
    }

    private fun updateOnboardingPage() {
        imageOnboard.setImageResource(images[currentIndex])
        textDescription.text = descriptions[currentIndex]
        btnNext.text = if (currentIndex == images.size - 1) "Get Started" else "Next"
    }

    /**
     * Fungsi ini dijalankan HANYA saat onboarding selesai.
     * Tugasnya: menyimpan status dan pindah ke AuthActivity.
     */
    private fun onFinishOnboarding() {
        // 1. Simpan status bahwa onboarding sudah selesai
        val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("onboarding_completed", true)
            apply()
        }

        // 2. SELALU pergi ke AuthActivity setelah selesai
        val intent = Intent(this, AuthActivity::class.java)
        startActivity(intent)
        finish() // Tutup OnboardingActivity agar tidak bisa kembali
    }
}
