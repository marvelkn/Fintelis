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
        "Smart insights for better credit decisions.",
        "Track your spending and save efficiently.",
        "Get started now and manage your finances easily."
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
                startActivity(Intent(this, AuthActivity::class.java))
                finish()
            }
        }
    }

    private fun updateOnboardingPage() {
        imageOnboard.setImageResource(images[currentIndex])
        textDescription.text = descriptions[currentIndex]
        btnNext.text = if (currentIndex == images.size - 1) "Get Started" else "Next"
    }
}
