package com.example.fintelis

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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

    // === STATE TRACKING ===
    // 1. Main Balance State
    private var isMainBalanceVisible = false
    private var actualMainBalanceFormatted: String = "IDR •••••••••••"

    // 2. Wallet Visibility State Map (Key: WalletID, Value: IsVisible)
    // We use this so that when data updates, the toggle state doesn't reset to false.
    private val walletVisibilityStates = mutableMapOf<String, Boolean>()

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

        if (auth.currentUser != null) {
            val user = auth.currentUser
            displayUserName(user)
        }

        setupPieChart()

        // Observe total balance and store real value
        dashboardViewModel.totalBalance.observe(viewLifecycleOwner) { totalBalance ->
            val formatted = dashboardViewModel.formatRupiah(totalBalance ?: 0.0)
            actualMainBalanceFormatted = formatted
            updateMainBalanceDisplay()
        }

        // Observe wallets and update UI
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

            // Hide all initially
            cardMap.values.forEach { it.visibility = View.GONE }

            // Show and update only active wallets
            wallets.forEach { wallet ->
                val cardView = cardMap[wallet.name.uppercase()]
                cardView?.let {
                    it.visibility = View.VISIBLE
                    updateCard(it, wallet)
                }
            }
        }

        // Add Wallet button
        binding.cardAddWallet.root.setOnClickListener {
            showAddWalletDialog()
        }

        // Financial data observers
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

        // === SETUP MAIN TOGGLE BUTTON ===
        binding.imgToggleBalance.setOnClickListener {
            isMainBalanceVisible = !isMainBalanceVisible
            updateMainBalanceDisplay()
        }
    }

    private fun displayUserName(user: FirebaseUser?) {
        val userName = user?.displayName
        if (!userName.isNullOrEmpty()) {
            tvGreeting.text = "Hi, $userName!"
        } else {
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

    // === UPDATED FUNCTION: ROBUST WALLET TOGGLE ===
    private fun updateCard(cardView: View, wallet: Wallet) {
        // Use safe calls (?) to prevent crashes if IDs are missing in some XMLs
        val tvWalletName = cardView.findViewById<TextView>(R.id.tv_wallet_name)
        val tvWalletBalance = cardView.findViewById<TextView>(R.id.tv_wallet_balance)
        val imgToggleWallet = cardView.findViewById<ImageView>(R.id.img_toggle_wallet_balance)

        tvWalletName?.text = wallet.name

        val realBalance = dashboardViewModel.formatRupiah(wallet.balance)
        val hiddenBalance = "Rp •••••••"

        // 1. Get current state from Map, default to false (hidden) if not set
        var isVisible = walletVisibilityStates[wallet.id] ?: false

        // 2. Helper to set UI based on boolean state
        fun setUIState(visible: Boolean) {
            if (visible) {
                tvWalletBalance?.text = realBalance
                imgToggleWallet?.setImageResource(R.drawable.ic_visibility)
            } else {
                tvWalletBalance?.text = hiddenBalance
                imgToggleWallet?.setImageResource(R.drawable.ic_visibility_off)
            }
        }

        // 3. Apply initial state immediately
        setUIState(isVisible)

        // 4. Toggle Click Listener
        imgToggleWallet?.setOnClickListener {
            isVisible = !isVisible // Flip state
            walletVisibilityStates[wallet.id] = isVisible // Save to map
            setUIState(isVisible) // Update UI
        }

        // 5. Card Navigation Click Listener
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
            .setItems(dialogOptions) { _, which ->
                val selected = dialogOptions[which]
                if (selected == "Other...") {
                    showCustomWalletDialog()
                } else {
                    dashboardViewModel.addNewWallet(selected)
                }
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
            .setPositiveButton("Add") { _, _ ->
                val walletName = editText.text.toString().trim()
                if (walletName.isNotEmpty()) {
                    dashboardViewModel.addNewWallet(walletName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // === MAIN BALANCE UPDATE ===
    private fun updateMainBalanceDisplay() {
        if (isMainBalanceVisible) {
            binding.tvBalanceNominal.text = actualMainBalanceFormatted
            binding.imgToggleBalance.setImageResource(R.drawable.ic_visibility)
        } else {
            binding.tvBalanceNominal.text = "IDR •••••••••••"
            binding.imgToggleBalance.setImageResource(R.drawable.ic_visibility_off)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}