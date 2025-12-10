package com.example.fintelis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.fintelis.databinding.FragmentDashboardBinding
import com.example.fintelis.viewmodel.DashboardViewModel
import com.example.fintelis.viewmodel.TransactionViewModel

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    // ViewModel for this dashboard screen's data
    private val dashboardViewModel: DashboardViewModel by viewModels()
    // Shared ViewModel to communicate the active wallet to other fragments
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

        // Observe the total balance from the ViewModel
        dashboardViewModel.totalBalance.observe(viewLifecycleOwner) { totalBalance ->
            binding.tvBalanceNominal.text = dashboardViewModel.formatRupiah(totalBalance ?: 0.0)
        }

        // Observe the list of wallets and their balances
        dashboardViewModel.wallets.observe(viewLifecycleOwner) { wallets ->
            if (wallets == null) return@observe

            // A map to easily find the right UI card for each wallet from the data
            val cardMap = mapOf(
                "BCA" to binding.cardBca.root,
                "BLU" to binding.cardBlu.root,
                "BNI" to binding.cardBni.root,
                "MANDIRI" to binding.cardMandiri.root,
                "DANA" to binding.cardDana.root,
                "GOPAY" to binding.cardGopay.root,
                "OVO" to binding.cardOvo.root,
                "SPAY" to binding.cardSpay.root,
                "MAIN" to binding.cardMain.root,
                "CASH" to binding.cardMain.root
            )

            // Update each card with the balance and set a click listener
            wallets.forEach { wallet ->
                cardMap[wallet.name.uppercase()]?.let { card ->
                    updateCardBalance(card, wallet.balance)
                    card.setOnClickListener {
                        // Set the active wallet in the shared ViewModel
                        transactionViewModel.setActiveWallet(wallet.id)
                        // Navigate to the transaction list screen
                        findNavController().navigate(R.id.customerListFragment)
                    }
                }
            }
        }
    }

    private fun updateCardBalance(cardView: CardView, amount: Double) {
        val tvBalance = cardView.findViewById<TextView>(R.id.tv_balance_nominal)
        tvBalance?.text = dashboardViewModel.formatRupiah(amount)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}