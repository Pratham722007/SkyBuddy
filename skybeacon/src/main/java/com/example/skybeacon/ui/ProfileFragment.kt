package com.example.skybeacon.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.skybeacon.MainActivity
import com.example.skybeacon.R

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireActivity().getSharedPreferences("skybeacon_prefs", Context.MODE_PRIVATE)

        val btnLogout = view.findViewById<Button>(R.id.btn_logout)
        val btnSaveSos = view.findViewById<Button>(R.id.btn_save_sos)
        val etSosMessage = view.findViewById<EditText>(R.id.et_sos_message)

        val currentSosMsg = prefs.getString("custom_sos_message", "SkyBeacon:SOS|EMERGENCY ALERT - Please contact airport staff immediately")
        etSosMessage.setText(currentSosMsg)

        btnSaveSos.setOnClickListener {
            val newSosMsg = etSosMessage.text.toString()
            if (newSosMsg.isNotBlank()) {
                prefs.edit().putString("custom_sos_message", newSosMsg).apply()
                Toast.makeText(requireContext(), "SOS Message saved", Toast.LENGTH_SHORT).show()
            }
        }

        btnLogout.setOnClickListener {
            // Stop any active broadcasts on logout
            (requireActivity() as MainActivity).haltBroadcast()

            // Clear session
            requireActivity().getSharedPreferences("skybeacon_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("is_logged_in", false).apply()

            findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
        }
    }
}
