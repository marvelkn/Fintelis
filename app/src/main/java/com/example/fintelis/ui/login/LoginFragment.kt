package com.example.fintelis

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.fintelis.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inisialisasi Firebase Auth
        auth = FirebaseAuth.getInstance()
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvSignup.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_signup)
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etemailLogin.text.toString().trim()
            val password = binding.etPasswordLogin.text.toString().trim()
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "Please fill email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else {
                // Tampilkan loading
                binding.progressBar.visibility = View.VISIBLE
                binding.btnLogin.isEnabled = false

                // Fungsi Login Firebase
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(requireActivity()) { task ->

                        binding.progressBar.visibility = View.GONE
                        binding.btnLogin.isEnabled = true

                        //Login berhasil
                        if (task.isSuccessful) {
                            Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()
                            // 2. Simpan status bahwa pengguna sudah login
                            val sharedPref = requireActivity().getSharedPreferences("app_prefs",
                                AppCompatActivity.MODE_PRIVATE)
                            with(sharedPref.edit()) {
                                putBoolean("user_logged_in", true)
                                apply()
                            }
                            // 3. Arahkan ke DashboardActivity (BUKAN MainActivity)
                            val intent = Intent(requireActivity(), DashboardActivity::class.java)
                            startActivity(intent)
                            requireActivity().finish() // Tutup AuthActivity agar tidak bisa kembali

                        } else {
                            // Login gagal
                            Toast.makeText(context, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
