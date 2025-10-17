package com.example.fintelis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.fintelis.databinding.FragmentSignupBinding

class SignupFragment : Fragment() {

    private lateinit var binding: FragmentSignupBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvLogin.setOnClickListener {
            findNavController().navigate(R.id.action_signup_to_login)
        }

        binding.btnSignUp.setOnClickListener {
            val name = binding.etName.text.toString()
            val phone = binding.etPhone.text.toString()
            val password = binding.etPassword.text.toString()
            val termsChecked = binding.checkTerms.isChecked

            if (name.isEmpty() || phone.isEmpty() || password.isEmpty() || !termsChecked) {
                Toast.makeText(context, "Please fill all fields and accept terms", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Account created", Toast.LENGTH_SHORT).show()

                // Arahkan ke CreditResultFragment
                findNavController().navigate(R.id.creditResultFragment)
            }
        }
    }
}
