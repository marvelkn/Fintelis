package com.example.fintelis

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.fintelis.databinding.FragmentAnalysisResultBinding
import com.example.fintelis.databinding.FragmentEditProfileBinding
import com.example.fintelis.databinding.FragmentSettingsBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlin.getValue

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.layoutEditProfile.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.settingsFragment) {
                findNavController().navigate(R.id.action_settingsFragment_to_editProfileFragment)
            }
        }

        binding.layoutChangePassword.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.settingsFragment) {
                findNavController().navigate(R.id.action_settingsFragment_to_changePasswordFragment)
            }
        }

        binding.btnLogout.setOnClickListener {
            logoutUser()
        }

    }

    private fun logoutUser() {
        // Hapus sesi pengguna dari SharedPreferences
        val sharedPreferences = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()

        // Buat Intent untuk memulai AuthActivity
        val intent = Intent(requireActivity(), AuthActivity::class.java)

        // Tambahkan "flags" untuk membersihkan semua activity sebelumnya
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

        // Jalankan Intent
        startActivity(intent)

        // Selesaikan MainActivity agar tidak bisa kembali
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
