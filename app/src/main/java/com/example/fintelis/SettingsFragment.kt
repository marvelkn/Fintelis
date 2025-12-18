package com.example.fintelis

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.fintelis.databinding.FragmentSettingsBinding
import com.example.fintelis.utils.NotificationScheduler
import com.example.fintelis.receiver.ReminderReceiver
import com.example.fintelis.viewmodel.SettingsViewModel
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    private lateinit var tvUserEmailDisplay: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var switchNotifications: SwitchMaterial

    // --- 1. Launcher untuk meminta izin notifikasi (Android 13+) ---
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // User menekan "Allow"
            NotificationScheduler.setReminderStatus(requireContext(), true)
            Toast.makeText(context, "Pengingat harian diaktifkan", Toast.LENGTH_SHORT).show()
        } else {
            // User menekan "Don't Allow"
            // Kita kembalikan switch ke posisi OFF karena izin ditolak
            binding.switchNotifications.isChecked = false
            NotificationScheduler.setReminderStatus(requireContext(), false)
            Toast.makeText(context, "Izin notifikasi diperlukan untuk fitur ini", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvUserEmailDisplay = binding.tvUserEmailDisplay
        progressBar = binding.progressBar
        switchNotifications = binding.switchNotifications

        setupObservers()
        setupMenuNavigation()
        setupNotificationSwitch() // Setup logika switch
        setupLogout()

        // Di dalam onViewCreated SettingsFragment
        binding.tvSettingsTitle.setOnClickListener {
            // KITA PAKSA PANGGIL RECEIVER SEKARANG
            val intent = Intent(requireContext(), ReminderReceiver::class.java)
            requireContext().sendBroadcast(intent)
            Toast.makeText(requireContext(), "Mencoba memunculkan notifikasi...", Toast.LENGTH_SHORT).show()
        }
        binding.layoutSetLimit.setOnClickListener {
            showSetLimitDialog()
        }
    }

    // --- 2. Logika Switch Notifikasi ---
    private fun setupNotificationSwitch() {
        // A. Set posisi awal switch berdasarkan data yang tersimpan (SharedPreferences)
        val isEnabled = NotificationScheduler.isReminderEnabled(requireContext())
        switchNotifications.isChecked = isEnabled

        // B. Listener saat user klik switch
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // User mau menyalakan (ON)
                checkPermissionAndEnable()
            } else {
                // User mau mematikan (OFF)
                NotificationScheduler.setReminderStatus(requireContext(), false)
                Toast.makeText(context, "Pengingat dimatikan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPermissionAndEnable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Cek apakah izin sudah diberikan?
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Izin sudah ada, langsung aktifkan
                NotificationScheduler.setReminderStatus(requireContext(), true)
            } else {
                // Izin belum ada, minta izin (Popup muncul)
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            // Android 12 ke bawah (Tidak perlu izin runtime)
            NotificationScheduler.setReminderStatus(requireContext(), true)
        }
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.isVisible = isLoading
            binding.layoutEditProfile.isEnabled = !isLoading
            binding.layoutChangePassword.isEnabled = !isLoading
            binding.btnLogout.isEnabled = !isLoading
            binding.layoutAboutApp.isEnabled = !isLoading
            binding.switchNotifications.isEnabled = !isLoading // Matikan switch saat loading
        }

        viewModel.userProfile.observe(viewLifecycleOwner) { user ->
            tvUserEmailDisplay.text = "Securely logged in as ${user.email}"
        }

        viewModel.statusMessage.observe(viewLifecycleOwner) { msg ->
            if (msg.isNotEmpty()) {
                // Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupMenuNavigation() {
        binding.layoutEditProfile.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_settingsFragment_to_editProfileFragment)
            } catch (e: IllegalArgumentException) { }
        }

        binding.layoutChangePassword.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_settingsFragment_to_changePasswordFragment)
            } catch (e: IllegalArgumentException) { }
        }

        binding.layoutAboutApp.setOnClickListener {
            Toast.makeText(context, "Financial App v1.0.2", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupLogout() {
        binding.btnLogout.setOnClickListener {
            logoutUser()
        }
    }

    private fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
        val sharedPreferences = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()

        // Juga matikan reminder saat logout (Opsional, tergantung kebutuhan bisnis)
        // NotificationScheduler.setReminderStatus(requireContext(), false)

        Toast.makeText(requireContext(), "You're Logged Out", Toast.LENGTH_SHORT).show()

        val intent = Intent(requireActivity(), AuthActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        requireActivity().finish()
    }

    private fun showSetLimitDialog() {
        val context = requireContext()

        // 1. Buat EditText untuk input angka
        val input = EditText(context)
        input.inputType = InputType.TYPE_CLASS_NUMBER // Hanya boleh angka
        input.hint = "Example: 1000000"
        input.setPadding(50, 30, 50, 30) // Sedikit padding agar rapi

        // 2. Buat Dialog
        val dialog = AlertDialog.Builder(context)
            .setTitle("Set your monthly limit")
            .setMessage("Set your money spending limit")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val limitStr = input.text.toString()
                if (limitStr.isNotEmpty()) {
                    val limitAmount = limitStr.toDouble()
                    saveLimitLocally(limitAmount)
                } else {
                    Toast.makeText(context, "You need to enter your monthly limit", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun saveLimitLocally(limit: Double) {
        val sharedPref = requireContext().getSharedPreferences("FinancialPrefs", android.content.Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putLong("monthly_limit", limit.toLong())
            apply()
        }
        Toast.makeText(context, "Your monthly limit has been set", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}