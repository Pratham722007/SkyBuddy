package com.example.skybeacon.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.skybeacon.R

class LoginFragment : Fragment(R.layout.fragment_login) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireActivity().getSharedPreferences("skybeacon_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("is_logged_in", false)) {
            findNavController().navigate(R.id.action_loginFragment_to_dashboardFragment)
            return
        }

        val etEmail = view.findViewById<EditText>(R.id.et_email)
        val etPassword = view.findViewById<EditText>(R.id.et_password)
        val btnLogin = view.findViewById<Button>(R.id.btn_login)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Hardcoded offline login
            if (email == "admin@skybeacon.com" && password == "password") {
                prefs.edit().putBoolean("is_logged_in", true).apply()
                findNavController().navigate(R.id.action_loginFragment_to_dashboardFragment)
            } else {
                Toast.makeText(requireContext(), "Invalid credentials", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
