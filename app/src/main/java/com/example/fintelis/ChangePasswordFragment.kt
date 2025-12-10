package com.example.fintelis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.fintelis.databinding.FragmentChangePasswordBinding
import com.example.fintelis.viewmodel.SettingsViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class ChangePasswordFragment : Fragment() {

    private var _binding: FragmentChangePasswordBinding? = null
    // Menggunakan FragmentChangePasswordBinding yang dihasilkan dari layout Anda
    private val binding get() = _binding!!

    // Menggunakan activityViewModels() untuk berbagi instance SettingsViewModel
    private val viewModel: SettingsViewModel by activityViewModels()

    // Views (Bisa diambil langsung dari binding, namun kita deklarasikan untuk clarity)
    private lateinit var etCurrentPass: TextInputEditText
    private lateinit var etNewPass: TextInputEditText
    private lateinit var etConfirmPass: TextInputEditText
    private lateinit var btnUpdatePassword: MaterialButton
    private lateinit var ivBackButton: ImageView
    private lateinit var progressBar: ProgressBar // Tambahkan ini di layout jika belum ada

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChangePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews()
        setupObservers()
        setupListeners()
    }

    private fun initializeViews() {
        // Inisialisasi views dari binding
        etCurrentPass = binding.etCurrentPassword
        etNewPass = binding.etNewPassword
        etConfirmPass = binding.etConfirmPassword
        btnUpdatePassword = binding.btnUpdatePassword
        ivBackButton = binding.headerLayout.findViewById(R.id.iv_back_button)

        // Asumsi Anda menambahkan ProgressBar di ConstraintLayout utama atau di dalam ScrollView:
        // Jika belum ada ProgressBar di layout, Anda harus menambahkannya secara manual
        progressBar = view?.findViewById(R.id.progressBar) ?: ProgressBar(requireContext()).apply {
            // Jika ProgressBar tidak ditemukan, gunakan yang baru (ini hanya fallback, sebaiknya tambahkan di XML)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            (view as? ViewGroup)?.addView(this)
            visibility = View.GONE
        }
    }

    private fun setupObservers() {
        // 1. Status Message (Menampilkan notifikasi sukses/gagal)
        viewModel.statusMessage.observe(viewLifecycleOwner) { msg ->
            if (msg.contains("Password berhasil diganti")) {
                // Jika sukses, tampilkan toast dan kembali ke Settings
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } else if (msg.isNotEmpty()) {
                // Tampilkan pesan error (misal: "Password lama salah!")
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }

        // 2. Loading State
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.isVisible = isLoading

            // Kunci tombol saat proses berlangsung
            btnUpdatePassword.isEnabled = !isLoading
            etCurrentPass.isEnabled = !isLoading
            etNewPass.isEnabled = !isLoading
            etConfirmPass.isEnabled = !isLoading

            // Bersihkan field jika proses selesai (loading false)
            if (!isLoading) {
                etCurrentPass.text?.clear()
                etNewPass.text?.clear()
                etConfirmPass.text?.clear()
            }
        }
    }

    private fun setupListeners() {
        // Tombol Kembali
        ivBackButton.setOnClickListener {
            findNavController().popBackStack()
        }

        // Tombol Update Password
        btnUpdatePassword.setOnClickListener {
            val currentPass = etCurrentPass.text.toString().trim()
            val newPass = etNewPass.text.toString().trim()
            val confirmPass = etConfirmPass.text.toString().trim()

            // --- VALIDASI INPUT ---
            if (currentPass.isEmpty()) {
                binding.tilCurrentPassword.error = "Wajib diisi"
                return@setOnClickListener
            }
            if (newPass.length < 6) {
                binding.tilNewPassword.error = "Minimal 6 karakter"
                return@setOnClickListener
            }
            if (newPass != confirmPass) {
                binding.tilConfirmPassword.error = "Password tidak cocok"
                return@setOnClickListener
            }
            if (currentPass == newPass) {
                binding.tilNewPassword.error = "Password baru tidak boleh sama dengan yang lama"
                return@setOnClickListener
            }

            // Hapus error state
            binding.tilCurrentPassword.error = null
            binding.tilNewPassword.error = null
            binding.tilConfirmPassword.error = null

            // Panggil fungsi di ViewModel untuk update di Firebase
            viewModel.changePassword(currentPass, newPass)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}