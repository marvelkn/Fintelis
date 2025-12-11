package com.example.fintelis

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.fintelis.data.Wallet
import com.example.fintelis.databinding.FragmentDashboardBinding
import com.example.fintelis.viewmodel.DashboardViewModel
import com.example.fintelis.viewmodel.TransactionViewModel
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import java.text.NumberFormat
import java.util.Locale

class DashboardFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var tvGreeting: TextView

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

        auth = FirebaseAuth.getInstance()

        tvGreeting = view.findViewById(R.id.tv_greeting)

        if(auth.currentUser != null){
            val user = auth.currentUser
            displayUserName(user)
        }

        setupPieChart()

        dashboardViewModel.totalBalance.observe(viewLifecycleOwner) { totalBalance ->
            binding.tvBalanceNominal.text = dashboardViewModel.formatRupiah(totalBalance ?: 0.0)
        }

        dashboardViewModel.wallets.observe(viewLifecycleOwner) { wallets ->
            if (wallets == null) return@observe

            val cardMap = mapOf(
                "BCA" to binding.cardBca.root,
                "BLU" to binding.cardBlu.root,
                "BNI" to binding.cardBni.root,
                "MANDIRI" to binding.cardMandiri.root,
                "DANA" to binding.cardDana.root,
                "GOPAY" to binding.cardGopay.root,
                "OVO" to binding.cardOvo.root,
                "SPAY" to binding.cardSpay.root
            )

            cardMap.values.forEach { it.visibility = View.GONE }

            wallets.forEach { wallet ->
                val cardView = cardMap[wallet.name.uppercase()]
                cardView?.let {
                    it.visibility = View.VISIBLE
                    updateCard(it, wallet)
                }
            }
        }

        binding.cardAddWallet.root.setOnClickListener {
            showAddWalletDialog()
        }

        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

        transactionViewModel.incomePercentage.observe(viewLifecycleOwner) { percentage ->
            binding.tvIncomePercentage.text = String.format("%.0f%%", percentage)
        }

        transactionViewModel.expensePercentage.observe(viewLifecycleOwner) { percentage ->
            binding.tvExpensePercentage.text = String.format("%.0f%%", percentage)
        }

        transactionViewModel.incomeNominal.observe(viewLifecycleOwner) { nominal ->
            binding.tvIncomeNominal.text = format.format(nominal)
        }

        transactionViewModel.expenseNominal.observe(viewLifecycleOwner) { nominal ->
            binding.tvExpenseNominal.text = format.format(nominal)
        }

        transactionViewModel.incomeExpensePieData.observe(viewLifecycleOwner) { pieEntries ->
            val dataSet = PieDataSet(pieEntries, "").apply {
                colors = listOf(
                    ContextCompat.getColor(requireContext(), R.color.yellow_500),
                    ContextCompat.getColor(requireContext(), R.color.red_500)
                )
                setDrawValues(false)
            }

            val pieData = PieData(dataSet)
            binding.pieChartFinancial.data = pieData
            binding.pieChartFinancial.invalidate()
        }
    }

    private fun displayUserName(user: FirebaseUser?) {
        // Ambil nama pengguna (displayName)
        val userName = user?.displayName

        // Cek jika nama tidak kosong, jika kosong, gunakan sapaan default
        if (!userName.isNullOrEmpty()) {
            tvGreeting.text = "Hi, $userName!"
        } else {
            // Fallback jika nama tidak ada
            tvGreeting.text = "Hi, Fintelis Buddy!"
        }
    }

    private fun setupPieChart() {
        binding.pieChartFinancial.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            legend.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            setTransparentCircleColor(Color.TRANSPARENT)
            setTransparentCircleAlpha(0)
            holeRadius = 80f
            transparentCircleRadius = 80f
            setDrawCenterText(false)
            rotationAngle = 0f
            isRotationEnabled = false
            isHighlightPerTapEnabled = false
            animateY(1400, Easing.EaseInOutQuad)
        }
    }

    private fun updateCard(cardView: View, wallet: Wallet) {
        val tvWalletName = cardView.findViewById<TextView>(R.id.tv_wallet_name)
        val tvWalletBalance = cardView.findViewById<TextView>(R.id.tv_wallet_balance)

        tvWalletName?.text = wallet.name
        tvWalletBalance?.text = dashboardViewModel.formatRupiah(wallet.balance)

        cardView.setOnClickListener {
            transactionViewModel.setActiveWallet(wallet.id)
            findNavController().navigate(R.id.action_mainDashboard_to_customerListFragment)
        }
    }

    private fun showAddWalletDialog() {
        val allPossibleWallets = listOf("BCA", "BLU", "BNI", "MANDIRI", "DANA", "GOPAY", "OVO", "SPAY")
        val existingWalletNames = dashboardViewModel.wallets.value?.map { it.name.uppercase() } ?: emptyList()
        val availableWallets = allPossibleWallets.filter { !existingWalletNames.contains(it) }

        val dialogOptions = (availableWallets + "Other...").toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Add New Wallet")
            .setItems(dialogOptions) { dialog, which ->
                val selected = dialogOptions[which]
                if (selected == "Other...") {
                    showCustomWalletDialog()
                } else {
                    dashboardViewModel.addNewWallet(selected)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCustomWalletDialog() {
        val editText = TextInputEditText(requireContext())
        editText.hint = "e.g., Jenius"

        AlertDialog.Builder(requireContext())
            .setTitle("Add Custom Wallet")
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