package com.example.fintelis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        val switchNotifications = view.findViewById<SwitchMaterial>(R.id.switchNotifications)
        val layoutEditProfile = view.findViewById<LinearLayout>(R.id.layoutEditProfile)
        val layoutChangePassword = view.findViewById<LinearLayout>(R.id.layoutChangePassword)
        val aboutApp = view.findViewById<TextView>(R.id.tvAboutApp)
        val btnLogout = view.findViewById<MaterialButton>(R.id.btnLogout)

        // Toggle Notifications
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            val message = if (isChecked) "Notifications enabled" else "Notifications disabled"
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }

        // Edit Profile
        layoutEditProfile.setOnClickListener {
            Toast.makeText(requireContext(), "Opening profile editor...", Toast.LENGTH_SHORT).show()
        }

        // Change Password
        layoutChangePassword.setOnClickListener {
            Toast.makeText(requireContext(), "Navigating to password settings...", Toast.LENGTH_SHORT).show()
        }

        // About App
        aboutApp.setOnClickListener {
            Toast.makeText(requireContext(), "Fintelis App v1.0.0", Toast.LENGTH_SHORT).show()
        }

        // Logout
        btnLogout.setOnClickListener {
            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
        }

        return view
    }
}
