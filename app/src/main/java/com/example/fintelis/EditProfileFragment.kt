package com.example.fintelis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.fintelis.databinding.FragmentEditProfileBinding // Pastikan nama Binding Class benar
import com.example.fintelis.viewmodel.SettingsViewModel

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    // Menggunakan activityViewModels() untuk berbagi instance SettingsViewModel
    // dengan SettingsFragment yang merupakan parent di NavGraph
    private val viewModel: SettingsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pastikan data profil dimuat ulang saat Fragment ini dibuka
        // Ini memastikan data terbaru (Show Data) muncul di field.
        viewModel.loadUserProfile()

        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        // 1. Mengisi Form (Show Data) saat data user dimuat
        viewModel.userProfile.observe(viewLifecycleOwner) { user ->
            binding.etFullName.setText(user.fullName)
            binding.etEmail.setText(user.email) // Tetap tampil tapi tidak bisa diedit
            binding.etPhone.setText(user.phoneNumber)
        }

        // 2. Status Message (Menampilkan notifikasi sukses/gagal)
        viewModel.statusMessage.observe(viewLifecycleOwner) { msg ->
            if (msg.contains("berhasil diperbarui")) {
                // Jika update sukses, kembali ke halaman Settings utama
                Toast.makeText(context, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } else if (msg.isNotEmpty() && !msg.contains("Gagal update Auth")) {
                // Tampilkan error umum jika bukan error yang sudah ditangani di ViewModel
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }

        // 3. Loading State
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
            binding.btnSaveProfile.isEnabled = !isLoading

            // Mengunci field yang bisa diedit saat proses berlangsung
            binding.etFullName.isEnabled = !isLoading
            binding.etPhone.isEnabled = !isLoading
            // etEmail tetap false karena sudah diatur di XML
        }
    }

    private fun setupListeners() {
        binding.btnSaveProfile.setOnClickListener {
            val name = binding.etFullName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim() // Ambil nilai email yang tampil (untuk dikirim ke ViewModel, meskipun tidak diupdate)
            val phone = binding.etPhone.text.toString().trim()

            if (name.isNotEmpty() && email.isNotEmpty()) {
                // Panggil fungsi update di ViewModel.
                // ViewModel akan mengabaikan nilai email dari sini (karena sudah diubah logikanya)
                viewModel.updateProfile(name, phone, email)
            } else {
                Toast.makeText(context, "Nama dan Email wajib diisi!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}