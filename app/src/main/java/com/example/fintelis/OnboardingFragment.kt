//package com.example.fintelis
//
//import android.os.Bundle
//import android.view.View
//import android.widget.Button
//import android.widget.ImageView
//import android.widget.LinearLayout
//import androidx.appcompat.app.AppCompatActivity
//import androidx.recyclerview.widget.RecyclerView
//import androidx.viewpager2.widget.ViewPager2
//import com.example.fintelis.databinding.ActivityMainBinding
//
//class MainActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivityMainBinding
//    private lateinit var adapter: OnboardingAdapter
//
//    private val items = listOf(
//        OnboardItem(R.drawable.onb1, "Trusted by millions of people, part of one part"),
//        OnboardItem(R.drawable.onb2, "Smart insights for better credit decisions."),
//        OnboardItem(R.drawable.onb3, "Receive Money From Anywhere In The World"),
//        OnboardItem(R.drawable.onb4, "Welcome to Fintelis!")
//    )
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        adapter = OnboardingAdapter(items) { position ->
//            onNextClicked(position)
//        }
//
//        binding.viewPager.adapter = adapter
//        binding.viewPager.registerOnPageChangeCallback(pageChangeCallback)
//
//        setupDots()
//    }
//
//    private fun onNextClicked(position: Int) {
//        val next = position + 1
//        if (next < items.size) {
//            binding.viewPager.currentItem = next
//        } else {
//            // Misal onboarding selesai → tutup activity
//            finish()
//        }
//    }
//
//    private fun setupDots() {
//        updateDots(0)
//    }
//
//    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
//        override fun onPageSelected(position: Int) {
//            super.onPageSelected(position)
//            updateDots(position)
//        }
//    }
//
//    private fun updateDots(activePosition: Int) {
//        val recyclerView = binding.viewPager.getChildAt(0) as? RecyclerView ?: return
//        val layoutManager = recyclerView.layoutManager ?: return
//
//        for (i in 0 until adapter.itemCount) {
//            val child = layoutManager.findViewByPosition(i) ?: continue
//            val dotContainer = child.findViewById<LinearLayout>(R.id.layoutDots) ?: continue
//
//            dotContainer.removeAllViews()
//
//            for (d in 0 until adapter.itemCount) {
//                val iv = ImageView(this).apply {
//                    val params = LinearLayout.LayoutParams(
//                        LinearLayout.LayoutParams.WRAP_CONTENT,
//                        LinearLayout.LayoutParams.WRAP_CONTENT
//                    ).apply {
//                        leftMargin = 6
//                        rightMargin = 6
//                    }
//                    layoutParams = params
//                    setImageResource(
//                        if (d == activePosition) R.drawable.dot_active
//                        else R.drawable.dot_inactive
//                    )
//                }
//                dotContainer.addView(iv)
//            }
//
//            val btn = child.findViewById<Button>(R.id.btnNext)
//            btn.text = if (activePosition == adapter.itemCount - 1) "Finish" else "Next"
//        }
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        binding.viewPager.unregisterOnPageChangeCallback(pageChangeCallback)
//    }
//}
package com.example.fintelis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.fintelis.databinding.FragmentOnboardingBinding

// Ganti Fragment() menjadi kelas dasar
class OnboardingFragment : Fragment() {
/*
    // Gunakan View Binding untuk Fragment
    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: OnboardingAdapter
    private val items = listOf(
        OnboardItem(R.drawable.onb1, "Trusted by millions of people, part of one part"),
        OnboardItem(R.drawable.onb2, "Smart insights for better credit decisions."),
        OnboardItem(R.drawable.onb3, "Receive Money From Anywhere In The World"),
        OnboardItem(R.drawable.onb4, "Welcome to Fintelis!")
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Logika dipindahkan ke onViewCreated
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
            // TODO: Ganti ini untuk navigasi ke halaman Login saat sudah siap
            // findNavController().navigate(R.id.action_onboardingFragment_to_loginFragment)
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
                // Gunakan requireContext() di dalam Fragment
                val iv = ImageView(requireContext()).apply {
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

    override fun onDestroyView() {
        super.onDestroyView()
        binding.viewPager.unregisterOnPageChangeCallback(pageChangeCallback)
        _binding = null // Penting untuk Fragment
    }
*/}