package com.example.skybeacon.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.skybeacon.MainActivity
import com.example.skybeacon.R
import com.example.skybeacon.data.AppDatabase
import com.example.skybeacon.data.ShopProfile
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(AppDatabase.getDatabase(requireContext()))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvShops = view.findViewById<RecyclerView>(R.id.rv_shops)
        val rvRecentMessages = view.findViewById<RecyclerView>(R.id.rv_recent_messages)

        val shopAdapter = ShopAdapter { shop ->
            val bundle = Bundle().apply { putInt("shopId", shop.id) }
            findNavController().navigate(R.id.action_dashboardFragment_to_broadcastManagementFragment, bundle)
        }

        val recentAdapter = LogAdapter(
            onBroadcast = { log ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val payload = "SB:${log.shopName.replace(" ", "")}|${log.content}".take(61)
                    (requireActivity() as MainActivity).triggerBroadcast(payload)
                    
                    // Also log the re-broadcast
                    viewModel.insertLog(log.copy(id = 0, createdAt = System.currentTimeMillis()))
                }
            }
        )

        rvShops.layoutManager = LinearLayoutManager(requireContext())
        rvShops.adapter = shopAdapter

        rvRecentMessages.layoutManager = LinearLayoutManager(requireContext())
        rvRecentMessages.adapter = recentAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allShops.collect { shops ->
                shopAdapter.submitList(shops)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.recentLogs.collect { logs ->
                recentAdapter.submitList(logs)
            }
        }
    }
}
