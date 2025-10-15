package com.example.fintelis

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.fintelis.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cek apakah onboarding sudah selesai sebelumnya
        val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val onboardingCompleted = sharedPref.getBoolean("onboarding_completed", false)

        if (onboardingCompleted) {
            // Jika sudah, langsung ke Dashboard dan lewati onboarding
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
            finish()
            return // Penting untuk menghentikan eksekusi sisa onCreate
        }

        // Jika belum, lanjutkan dengan menampilkan layout onboarding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        adapter = OnboardingAdapter(items) { position ->
            onNextClicked(position)
        }

        binding.viewPager.adapter = adapter
        binding.viewPager.registerOnPageChangeCallback(pageChangeCallback)

        setupDots()
    }

    private fun onNextClicked(position: Int) {
        val next = position + 1
        if (next < items.size) {
            binding.viewPager.currentItem = next
        } else {
            // Tandai bahwa onboarding sudah selesai
            val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
            with(sharedPref.edit()) {
                putBoolean("onboarding_completed", true)
                apply()
            }
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
            // Misal onboarding selesai → tutup activity
            finish()
        }
    }

    private fun setupDots() {
        updateDots(0)
    }

    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            updateDots(position)
        }
    }

    private fun updateDots(activePosition: Int) {
        val recyclerView = binding.viewPager.getChildAt(0) as? RecyclerView ?: return
        val layoutManager = recyclerView.layoutManager ?: return

        for (i in 0 until adapter.itemCount) {
            val child = layoutManager.findViewByPosition(i) ?: continue
            val dotContainer = child.findViewById<LinearLayout>(R.id.layoutDots) ?: continue

            dotContainer.removeAllViews()

            for (d in 0 until adapter.itemCount) {
                val iv = ImageView(this).apply {
                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        leftMargin = 6
                        rightMargin = 6
                    }
                    layoutParams = params
                    setImageResource(
                        if (d == activePosition) R.drawable.dot_active
                        else R.drawable.dot_inactive
                    )
                }
                dotContainer.addView(iv)
            }

            val btn = child.findViewById<Button>(R.id.btnNext)
            btn.text = if (activePosition == adapter.itemCount - 1) "Finish" else "Next"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Check if 'binding' has been initialized before using it
        if (::binding.isInitialized) {
            // Your cleanup code that uses the binding object
            // For example: binding.recyclerView.adapter = null
        }
    }
}
