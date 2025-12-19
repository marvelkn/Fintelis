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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var tvGreeting: TextView

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val transactionViewModel: TransactionViewModel by activityViewModels()

    // === MAIN BALANCE ===
    private var isMainBalanceVisible = false
    private var actualMainBalanceFormatted = "IDR •••••••••••"

    // === WALLET VISIBILITY ===
    private val walletVisibilityStates = mutableMapOf<String, Boolean>()

    // === MONTHLY LIMIT ===
    private var monthlyLimit = 1_000_000
    private var usedAmount = 0 // total expenses from all wallets

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



        auth.currentUser?.let { displayUserName(it) }

        setupPieChart()

        // === TOTAL BALANCE ===
        dashboardViewModel.totalBalance.observe(viewLifecycleOwner) {
            actualMainBalanceFormatted =
                dashboardViewModel.formatRupiah(it ?: 0.0)
            updateMainBalanceDisplay()
        }

        // === WALLETS ===
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
                cardMap[wallet.name.uppercase()]?.let {
                    it.visibility = View.VISIBLE
                    updateCard(it, wallet)
                }
            }
        }

        binding.cardAddWallet.root.setOnClickListener {
            showAddWalletDialog()
        }

        val currencyFormat =
            NumberFormat.getCurrencyInstance(Locale("id", "ID"))

        // Set ke "ALL" agar konsisten mengambil semua data
        transactionViewModel.setActiveWallet("ALL")

        // PENTING: Observasi displayedTransactions agar MediatorLiveData aktif dan processList berjalan
        // Tanpa ini, incomePercentage dll tidak akan terupdate karena processList tidak dipanggil
        transactionViewModel.displayedTransactions.observe(viewLifecycleOwner) {
            // Data list transaksi tidak ditampilkan di sini, tapi perlu diobservasi
        }
        // === FINANCIAL DATA ===
        transactionViewModel.incomePercentage.observe(viewLifecycleOwner) {
            binding.tvIncomePercentage.text = "${it.toInt()}%"
        }

        transactionViewModel.expensePercentage.observe(viewLifecycleOwner) {
            binding.tvExpensePercentage.text = "${it.toInt()}%"
        }

        transactionViewModel.incomeNominal.observe(viewLifecycleOwner) {
            binding.tvIncomeNominal.text = currencyFormat.format(it)
        }

        // === EXPENSES (ALL WALLET) ===
        transactionViewModel.expenseNominal.observe(viewLifecycleOwner) { expense ->
            binding.tvExpenseNominal.text = currencyFormat.format(expense)

            usedAmount = expense.toInt()
            updateMonthlyLimitUI()
        }

        transactionViewModel.incomeExpensePieData.observe(viewLifecycleOwner) {
            val dataSet = PieDataSet(it, "").apply {
                colors = listOf(
                    ContextCompat.getColor(requireContext(), R.color.status_approved),
                    ContextCompat.getColor(requireContext(), R.color.red_500)
                )
                setDrawValues(false)
            }

            val pieData = PieData(dataSet)
            // Mengatur warna teks label (Income/Expense) menjadi HITAM
            pieData.setValueTextColor(Color.BLACK)
            binding.pieChartFinancial.data = pieData
            binding.pieChartFinancial.invalidate()
        }

        // === MAIN BALANCE TOGGLE ===
        binding.imgToggleBalance.setOnClickListener {
            isMainBalanceVisible = !isMainBalanceVisible
            updateMainBalanceDisplay()
        }

        // === TAP LIMIT TO SET MANUAL ===
        binding.progressBarLimit.setOnClickListener {
            showSetLimitDialog()
        }

        updateMonthlyLimitUI()

        // Set current date
        val currentDate = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.US).format(Date())
        binding.tvDate.text = currentDate
    }

    private fun setupPieChart() {
        binding.pieChartFinancial.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            legend.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 80f
            setDrawCenterText(false)
            isRotationEnabled = false
            animateY(1400, Easing.EaseInOutQuad)

            // Mengatur warna label entry (Income/Expense) menjadi HITAM
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(13f)
        }
    }

    // === MONTHLY LIMIT UI ===
    private fun updateMonthlyLimitUI() {
        val percentage =
            if (monthlyLimit > 0)
                ((usedAmount.toFloat() / monthlyLimit) * 100)
                    .toInt()
                    .coerceIn(0, 100)
            else 0

        binding.progressBarLimit.progress = percentage
        binding.tvLimitPercentage.text = "$percentage%"
        if (percentage >= 90) {
            binding.progressBarLimit.progressDrawable.setColorFilter(
                android.graphics.Color.RED, android.graphics.PorterDuff.Mode.SRC_IN
            )
            binding.tvLimitPercentage.setTextColor(android.graphics.Color.RED)
        } else if(percentage >= 50 && percentage < 90){
            binding.progressBarLimit.progressDrawable.setColorFilter(
                android.graphics.Color.YELLOW, android.graphics.PorterDuff.Mode.SRC_IN
            )
            binding.tvLimitPercentage.setTextColor(android.graphics.Color.YELLOW)
        } else if(percentage >= 10 && percentage < 50) {
            val approvedColor = ContextCompat.getColor(requireContext(), R.color.status_approved)
            binding.progressBarLimit.progressDrawable.setColorFilter(
                approvedColor, android.graphics.PorterDuff.Mode.SRC_IN
            )
            binding.tvLimitPercentage.setTextColor(approvedColor)
        } else {
            // Reset warna (misal ke default biru/hijau)
            binding.progressBarLimit.progressDrawable.clearColorFilter()
        }
        binding.tvLimitInfo.text =
            "IDR ${formatRupiah(usedAmount)} of IDR ${formatRupiah(monthlyLimit)}"
    }

    // === SET LIMIT DIALOG ===
    private fun showSetLimitDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_set_monthly_limit, null, false)

        val etLimit = dialogView.findViewById<TextInputEditText>(R.id.etLimit)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val input = etLimit.text?.toString()?.trim()

                if (input.isNullOrEmpty()) {
                    etLimit.error = "Limit cannot be empty"
                    return@setOnClickListener
                }

                val value = input.toIntOrNull()
                if (value == null || value <= 0) {
                    etLimit.error = "Invalid amount"
                    return@setOnClickListener
                }

                monthlyLimit = value
                updateMonthlyLimitUI()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun updateCard(cardView: View, wallet: Wallet) {
        val tvName = cardView.findViewById<TextView>(R.id.tv_wallet_name)
        val tvBalance = cardView.findViewById<TextView>(R.id.tv_wallet_balance)
        val imgToggle =
            cardView.findViewById<ImageView>(R.id.img_toggle_wallet_balance)

        tvName?.text = wallet.name

        val real = dashboardViewModel.formatRupiah(wallet.balance)
        val hidden = "Rp •••••••"

        var visible = walletVisibilityStates[wallet.id] ?: false

        fun render() {
            tvBalance?.text = if (visible) real else hidden
            imgToggle?.setImageResource(
                if (visible) R.drawable.ic_visibility
                else R.drawable.ic_visibility_off
            )
        }

        render()

        imgToggle?.setOnClickListener {
            visible = !visible
            walletVisibilityStates[wallet.id] = visible
            render()
        }

        cardView.setOnClickListener {
            transactionViewModel.setActiveWallet(wallet.id)
            findNavController()
                .navigate(R.id.action_mainDashboard_to_customerListFragment)
        }
    }

    private fun displayUserName(user: FirebaseUser) {
        tvGreeting.text =
            user.displayName?.let { "Hi, $it!" }
                ?: "Hi, Fintelis Buddy!"
    }

    private fun updateMainBalanceDisplay() {
        if (isMainBalanceVisible) {
            binding.tvBalanceNominal.text = actualMainBalanceFormatted
            binding.imgToggleBalance.setImageResource(R.drawable.ic_visibility)
        } else {
            binding.tvBalanceNominal.text = "IDR •••••••••••"
            binding.imgToggleBalance.setImageResource(R.drawable.ic_visibility_off)
        }
    }

    private fun formatRupiah(amount: Int): String {
        return NumberFormat.getInstance(Locale("id", "ID")).format(amount)
    }

    private fun showAddWalletDialog() {
        val wallets =
            listOf("BCA", "BLU", "BNI", "MANDIRI", "DANA", "GOPAY", "OVO", "SPAY")

        val existing =
            dashboardViewModel.wallets.value
                ?.map { it.name.uppercase() }
                ?: emptyList()

        val options =
            (wallets.filterNot { it in existing } + "Other...")
                .toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Add New Wallet")
            .setItems(options) { _, which ->
                if (options[which] == "Other...") showCustomWalletDialog()
                else dashboardViewModel.addNewWallet(options[which])
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCustomWalletDialog() {
        val input = TextInputEditText(requireContext())
        input.hint = "e.g., Jenius"

        AlertDialog.Builder(requireContext())
            .setTitle("Add Custom Wallet")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                input.text?.toString()?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { dashboardViewModel.addNewWallet(it) }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
