package com.example.fintelis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.fintelis.databinding.FragmentEditProfileBinding
import com.example.fintelis.viewmodel.SettingsViewModel

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

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

        // Muat data profil saat pertama kali dibuka
        viewModel.loadUserProfile()

        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        // Observasi data profil untuk mengisi EditText (Show Data)
        viewModel.userProfile.observe(viewLifecycleOwner) { user ->
            binding.etFullName.setText(user.fullName)
            binding.etEmail.setText(user.email)
            binding.etPhone.setText(user.phoneNumber)
        }

        // Observasi status pesan (berhasil/gagal)
        viewModel.statusMessage.observe(viewLifecycleOwner) { msg ->
            if (msg.isNotEmpty()) {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                if (msg.contains("berhasil diperbarui")) {
                    // Kembali ke halaman sebelumnya jika sukses
                    findNavController().popBackStack()
                }
            }
        }

        // Observasi status loading untuk tombol dan progress bar (jika ada)
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnSaveProfile.isEnabled = !isLoading
            binding.etFullName.isEnabled = !isLoading
            binding.etPhone.isEnabled = !isLoading
            // Anda bisa menambahkan binding.progressBar.isVisible = isLoading
            // jika ProgressBar ditambahkan kembali ke XML
        }
    }

    private fun setupListeners() {
        // FUNGSI TOMBOL BACK (Sesuai id: iv_back_button di XML Anda)
        binding.ivBackButton.setOnClickListener {
            findNavController().popBackStack()
        }

        // FUNGSI TOMBOL SAVE (Sesuai id: btnSaveProfile di XML Anda)
        binding.btnSaveProfile.setOnClickListener {
            val name = binding.etFullName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()

            if (name.isNotEmpty() && email.isNotEmpty()) {
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