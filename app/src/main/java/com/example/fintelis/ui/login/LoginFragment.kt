package com.example.fintelis

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.fintelis.databinding.FragmentLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Toast.makeText(context, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inisialisasi Firebase Auth
        auth = FirebaseAuth.getInstance()
        _binding = FragmentLoginBinding.inflate(inflater, container, false)

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

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
                            navigateToDashboard()
                        } else {
                            // Login gagal
                            Toast.makeText(context, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
        }

        binding.btnGoogleLogin.setOnClickListener {
            // Sign out from Google to force account selection dialog
            googleSignInClient.signOut().addOnCompleteListener(requireActivity()) {
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        binding.progressBar.visibility = View.VISIBLE
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                binding.progressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    navigateToDashboard()
                } else {
                    Toast.makeText(context, "Authentication Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun navigateToDashboard(){
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
