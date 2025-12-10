package com.example.fintelis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.fintelis.databinding.FragmentDashboardBinding
import com.example.fintelis.viewmodel.DashboardViewModel

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    // Initialize ViewModel
    private val viewModel: DashboardViewModel by viewModels()

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

        // Trigger data load
        viewModel.loadTransactionData()

        // Observe the calculated balances
        viewModel.walletBalances.observe(viewLifecycleOwner) { balances ->

            // 1. Update the Big Total Balance Card
            binding.tvBalanceNominal.text = viewModel.formatRupiah(balances.totalAll)

            // 2. Update specific Wallet Cards
            // Accessing included layouts via binding ID

            // BCA
            updateCardBalance(binding.cardBca.root, balances.bca)

            // BLU
            updateCardBalance(binding.cardBlu.root, balances.blu)

            // BNI
            updateCardBalance(binding.cardBni.root, balances.bni)

            // Mandiri
            updateCardBalance(binding.cardMandiri.root, balances.mandiri)

            // Dana
            updateCardBalance(binding.cardDana.root, balances.dana)

            // Gopay
            updateCardBalance(binding.cardGopay.root, balances.gopay)

            // OVO
            updateCardBalance(binding.cardOvo.root, balances.ovo)

            // SPay
            updateCardBalance(binding.cardSpay.root, balances.spay)

            // Main Cash
            updateCardBalance(binding.cardMain.root, balances.mainCash)
        }
    }

    // Helper function to find the specific TextView inside the included card layout
    private fun updateCardBalance(cardView: CardView, amount: Double) {
        // Assuming the TextView ID inside item_card_*.xml is named "tv_card_balance"
        // If your ID is different (e.g. tv_balance_nominal), change it below.
        val tvBalance = cardView.findViewById<TextView>(R.id.tv_balance_nominal)

        if (tvBalance != null) {
            tvBalance.text = viewModel.formatRupiah(amount)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}