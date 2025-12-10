package com.example.fintelis

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.fintelis.data.Transaction
import com.example.fintelis.data.TransactionType
import com.example.fintelis.databinding.FragmentSignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SignupFragment : Fragment() {

    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
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
                Toast.makeText(context, "Please fill all fields and accept terms", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        val profileUpdates = UserProfileChangeRequest.Builder().setDisplayName(name).build()

                        user?.updateProfile(profileUpdates)?.addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                // Add dummy data for the new user
                                addWelcomeTransactions(user)

                                Toast.makeText(context, "Account created successfully!", Toast.LENGTH_SHORT).show()
                                findNavController().navigate(R.id.action_signup_to_login)
                            }
                        }
                    } else {
                        Toast.makeText(context, "Signup Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    private fun addWelcomeTransactions(user: FirebaseUser?) {
        val userId = user?.uid ?: return
        val batch = firestore.batch()
        val transactionsCollection = firestore.collection("users").document(userId).collection("transactions")

        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)

        // 1. Initial Deposit
        val welcomeDeposit = Transaction(
            title = "Initial Deposit",
            amount = 1500000.0,
            type = TransactionType.INCOME,
            date = sdf.format(cal.time),
            category = "Gaji"
        )
        batch.set(transactionsCollection.document(), welcomeDeposit)

        // 2. First Expense
        cal.add(Calendar.DAY_OF_MONTH, -1)
        val firstExpense = Transaction(
            title = "Setup Subscription",
            amount = 150000.0,
            type = TransactionType.EXPENSE,
            date = sdf.format(cal.time),
            category = "Tagihan"
        )
        batch.set(transactionsCollection.document(), firstExpense)

        batch.commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}