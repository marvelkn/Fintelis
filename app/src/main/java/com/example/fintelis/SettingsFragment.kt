package com.example.fintelis

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.fintelis.databinding.FragmentSettingsBinding
import com.example.fintelis.viewmodel.SettingsViewModel
import com.google.firebase.auth.FirebaseAuth

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    // Menggunakan ViewModel yang sudah terintegrasi Firebase
    private val viewModel: SettingsViewModel by viewModels()

    // Deklarasi views yang dimanipulasi
    private lateinit var tvUserEmailDisplay: TextView
    private lateinit var progressBar: ProgressBar


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inisialisasi views
        tvUserEmailDisplay = binding.tvUserEmailDisplay
        progressBar = binding.progressBar

        setupObservers()
        setupMenuNavigation()
        setupLogout()
    }

    private fun setupObservers() {
        // 1. Loading State (Menampilkan/Menyembunyikan ProgressBar)
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.isVisible = isLoading

            // Kunci navigasi/tombol saat loading
            binding.layoutEditProfile.isEnabled = !isLoading
            binding.layoutChangePassword.isEnabled = !isLoading
            binding.btnLogout.isEnabled = !isLoading
            binding.layoutAboutApp.isEnabled = !isLoading

            // Note: Perlu diperhatikan bahwa di layout Anda tidak ada switchNotifications,
            // jika ada, tambahkan binding.switchNotifications.isEnabled = !isLoading
        }

        // 2. Data User (Profile)
        viewModel.userProfile.observe(viewLifecycleOwner) { user ->
            // Menampilkan email user secara dinamis dari LiveData
            val emailText = user.email
            tvUserEmailDisplay.text = "Securely logged in as $emailText"

            // Note: Jika Anda ingin menampilkan nama pengguna, Anda perlu
            // TextView terpisah di layout (misal tvUserNameDisplay) dan mengisinya dengan user.fullName
        }

        // 3. Status Message (Opsional: untuk menangkap error load profil)
        viewModel.statusMessage.observe(viewLifecycleOwner) { msg ->
            if (msg.isNotEmpty()) {
                // Toast.makeText(requireContext(), "Status: $msg", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupMenuNavigation() {
        // Navigasi ke Fragment Edit Profile
        binding.layoutEditProfile.setOnClickListener {
            // Kita navigasi ke Fragment Edit Profile
            try {
                findNavController().navigate(R.id.action_settingsFragment_to_editProfileFragment)
            } catch (e: IllegalArgumentException) {
                Toast.makeText(context, "Navigasi Edit Profile gagal. Cek NavGraph.", Toast.LENGTH_SHORT).show()
            }
        }

        // Navigasi ke Fragment Change Password
        binding.layoutChangePassword.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_settingsFragment_to_changePasswordFragment)
            } catch (e: IllegalArgumentException) {
                Toast.makeText(context, "Navigasi Change Password gagal. Cek NavGraph.", Toast.LENGTH_SHORT).show()
            }
        }

        // Klik About App (Jika diperlukan)
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
        // 1. Sign out dari Firebase (Wajib)
        FirebaseAuth.getInstance().signOut()

        // 2. Bersihkan sesi lokal (SharedPreferences)
        val sharedPreferences = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()

        Toast.makeText(requireContext(), "Anda telah logout", Toast.LENGTH_SHORT).show()

        // 3. Pindah ke Halaman Login (AuthActivity)
        // Perlu dipastikan class AuthActivity ada dan siap menerima pengguna
        val intent = Intent(requireActivity(), AuthActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}