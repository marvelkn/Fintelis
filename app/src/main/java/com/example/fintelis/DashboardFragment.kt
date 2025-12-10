package com.example.fintelis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.fintelis.R
import com.example.fintelis.data.Wallet
import com.example.fintelis.databinding.FragmentDashboardBinding
import com.example.fintelis.viewmodel.DashboardViewModel
import com.example.fintelis.viewmodel.TransactionViewModel
import com.google.android.material.textfield.TextInputEditText

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val transactionViewModel: TransactionViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dashboardViewModel.totalBalance.observe(viewLifecycleOwner) { totalBalance ->
            binding.tvBalanceNominal.text = dashboardViewModel.formatRupiah(totalBalance ?: 0.0)
        }

        dashboardViewModel.wallets.observe(viewLifecycleOwner) { wallets ->
            if (wallets == null) return@observe

            // Using binding.root.findViewById is the most robust way to get views from <include> tags
            val cardMap = mapOf(
                "BCA" to binding.root.findViewById<View>(R.id.card_bca),
                "BLU" to binding.root.findViewById<View>(R.id.card_blu),
                "BNI" to binding.root.findViewById<View>(R.id.card_bni),
                "MANDIRI" to binding.root.findViewById<View>(R.id.card_mandiri),
                "DANA" to binding.root.findViewById<View>(R.id.card_dana),
                "GOPAY" to binding.root.findViewById<View>(R.id.card_gopay),
                "OVO" to binding.root.findViewById<View>(R.id.card_ovo),
                "SPAY" to binding.root.findViewById<View>(R.id.card_spay)
            )

            // Hide all cards initially so we only show the ones that exist
            cardMap.values.forEach { it.visibility = View.GONE }

            wallets.forEach { wallet ->
                val cardView = cardMap[wallet.name.uppercase()]
                cardView?.let {
                    it.visibility = View.VISIBLE
                    updateCard(it, wallet)
                }
            }
        }

        val addWalletButton = binding.root.findViewById<View>(R.id.card_add_wallet)
        addWalletButton.setOnClickListener {
            showAddWalletDialog()
        }
    }

    private fun updateCard(cardView: View, wallet: Wallet) {
        val tvWalletName = cardView.findViewById<TextView>(R.id.tv_wallet_name)
        val tvWalletBalance = cardView.findViewById<TextView>(R.id.tv_wallet_balance)

        tvWalletName?.text = wallet.name
        tvWalletBalance?.text = dashboardViewModel.formatRupiah(wallet.balance)

        cardView.setOnClickListener {
            transactionViewModel.setActiveWallet(wallet.id)
            findNavController().navigate(R.id.customerListFragment)
        }
    }

    private fun showAddWalletDialog() {
        val editText = TextInputEditText(requireContext())
        editText.hint = "e.g., Jenius"

        AlertDialog.Builder(requireContext())
            .setTitle("Add New Wallet")
            .setView(editText)
            .setPositiveButton("Add") { dialog, _ ->
                val walletName = editText.text.toString().trim()
                if (walletName.isNotEmpty()) {
                    dashboardViewModel.addNewWallet(walletName)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}