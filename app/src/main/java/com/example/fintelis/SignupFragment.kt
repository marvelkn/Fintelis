package com.example.fintelis

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.fintelis.databinding.FragmentLoginBinding
import com.example.fintelis.databinding.FragmentSignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

class SignupFragment : Fragment() {

    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inisialisasi Firebase Auth
        auth = FirebaseAuth.getInstance()
        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvLogin.setOnClickListener {
            findNavController().navigate(R.id.action_signup_to_login)
        }

        binding.btnSignUp.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etemail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val termsChecked = binding.checkTerms.isChecked

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || !termsChecked) {
                Toast.makeText(
                    context,
                    "Please fill all fields and accept terms",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (!termsChecked) {
                Toast.makeText(context, "Please accept the terms", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(context, "Password minimum 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 3. Proses Buat Akun ke Firebase
            // Tampilkan loading jika ada (binding.progressBar.visibility = View.VISIBLE)

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {
                        // Akun berhasil dibuat, TAPI nama belum tersimpan.
                        // Kita harus update profil user untuk menyimpan namanya.
                        val user = auth.currentUser
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build()

                        user?.updateProfile(profileUpdates)
                            ?.addOnCompleteListener { updateTask ->
                                // Sembunyikan loading
                                if (updateTask.isSuccessful) {
                                    Toast.makeText(
                                        context,
                                        "Account created successfully!",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    // Pindah ke Login atau langsung ke Dashboard
                                    // Karena di fragment Login Anda ada logika simpan SharedPref,
                                    // sebaiknya arahkan ke Login dulu agar user login manual,
                                    // ATAU Anda bisa jalankan logika login otomatis di sini.

                                    findNavController().navigate(R.id.action_signup_to_login)
                                }
                            }
                    } else {
                        // Sembunyikan loading
                        // Gagal buat akun (Email duplikat atau format salah)
                        Toast.makeText(
                            context,
                            "Signup Failed: ${task.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }
}
