package com.example.skybeacon.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.skybeacon.R
import com.example.skybeacon.data.AppDatabase
import com.example.skybeacon.data.ShopProfile
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class MyShopFragment : Fragment(R.layout.fragment_my_shop) {
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(AppDatabase.getDatabase(requireContext()))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvShops = view.findViewById<RecyclerView>(R.id.rv_my_shops)
        val fabAddShop = view.findViewById<FloatingActionButton>(R.id.fab_add_shop)

        fabAddShop.setOnClickListener {
            showAddShopDialog()
        }

        val adapter = ShopAdapter { shop ->
            showEditShopDialog(shop)
        }

        rvShops.layoutManager = LinearLayoutManager(requireContext())
        rvShops.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allShops.collect { shops ->
                adapter.submitList(shops)
            }
        }
    }

    private fun showAddShopDialog() {
        val context = requireContext()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val nameInput = EditText(context).apply { hint = "Shop Name (e.g. Starbucks)" }
        val categoryInput = EditText(context).apply { hint = "Category (e.g. Cafe)" }
        val terminalInput = EditText(context).apply { hint = "Terminal (e.g. T2)" }
        val emailInput = EditText(context).apply { hint = "Contact Email" }

        layout.addView(nameInput)
        layout.addView(categoryInput)
        layout.addView(terminalInput)
        layout.addView(emailInput)

        AlertDialog.Builder(context)
            .setTitle("Add New Shop")
            .setView(layout)
            .setPositiveButton("Add") { _, _ ->
                val name = nameInput.text.toString()
                if (name.isNotBlank()) {
                    val newShop = ShopProfile(
                        shopName = name,
                        category = categoryInput.text.toString(),
                        terminal = terminalInput.text.toString(),
                        contactEmail = emailInput.text.toString(),
                        ownerEmail = "admin@skybeacon.com"
                    )
                    viewModel.insertShop(newShop)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditShopDialog(shop: ShopProfile) {
        val context = requireContext()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val nameInput = EditText(context).apply { setText(shop.shopName); hint = "Shop Name" }
        val categoryInput = EditText(context).apply { setText(shop.category); hint = "Category" }
        val terminalInput = EditText(context).apply { setText(shop.terminal); hint = "Terminal" }

        layout.addView(nameInput)
        layout.addView(categoryInput)
        layout.addView(terminalInput)

        AlertDialog.Builder(context)
            .setTitle("Edit Shop Details")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val name = nameInput.text.toString()
                if (name.isNotBlank()) {
                    val updatedShop = shop.copy(
                        shopName = name,
                        category = categoryInput.text.toString(),
                        terminal = terminalInput.text.toString()
                    )
                    viewModel.updateShop(updatedShop)
                }
            }
            .setNeutralButton("Delete") { _, _ ->
                viewModel.deleteShop(shop)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
