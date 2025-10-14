package com.example.fintelis

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
    private lateinit var adapter: OnboardingAdapter

    private val items = listOf(
        OnboardItem(R.drawable.onb1, "Trusted by millions of people, part of one part"),
        OnboardItem(R.drawable.onb2, "Smart insights for better credit decisions."),
        OnboardItem(R.drawable.onb3, "Receive Money From Anywhere In The World"),
        OnboardItem(R.drawable.onb4, "Let's get started! Welcome to Fintelis!")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        binding.viewPager.unregisterOnPageChangeCallback(pageChangeCallback)
    }
}
