package com.example.fintelis

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
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
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.android.material.button.MaterialButton
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
    private var actualMainBalanceFormatted = "Rp 0"

    // === WALLET VISIBILITY STATE ===
    private val walletVisibilityStates = mutableMapOf<String, Boolean>()

    // === MONTHLY LIMIT ===
    private var monthlyLimit = 0
    private var usedAmount = 0

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
        tvGreeting = binding.tvGreeting

        auth.currentUser?.let { displayUserName(it) }

        setupPieChart()

        // === 1. OBSERVE TOTAL BALANCE (UTAMA) ===
        dashboardViewModel.totalBalance.observe(viewLifecycleOwner) { balance ->
            actualMainBalanceFormatted = formatRupiah(balance ?: 0.0)
            updateMainBalanceDisplay()
        }

        // === 2. OBSERVE WALLETS (SINKRONISASI SALDO WALLET) ===
        transactionViewModel.wallets.observe(viewLifecycleOwner) { wallets ->
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
                cardMap[wallet.name.uppercase()]?.let { cardView ->
                    cardView.visibility = View.VISIBLE
                    updateCard(cardView, wallet)
                }
            }
        }

        // === 3. FINANCIAL CHART DATA ===
        transactionViewModel.setActiveWallet("ALL")

        transactionViewModel.displayedTransactions.observe(viewLifecycleOwner) {}

        transactionViewModel.incomePercentage.observe(viewLifecycleOwner) {
            binding.tvIncomePercentage.text = "${it.toInt()}%"
        }
        transactionViewModel.expensePercentage.observe(viewLifecycleOwner) {
            binding.tvExpensePercentage.text = "${it.toInt()}%"
        }
        transactionViewModel.incomeNominal.observe(viewLifecycleOwner) {
            binding.tvIncomeNominal.text = formatRupiah(it)
        }
        transactionViewModel.expenseNominal.observe(viewLifecycleOwner) { expense ->
            binding.tvExpenseNominal.text = formatRupiah(expense)
            usedAmount = expense.toInt()
            updateMonthlyLimitUI() // Update UI saat nominal pengeluaran berubah
        }

        // === 4. TAMBAHAN: OBSERVE MONTHLY LIMIT DARI FIRESTORE ===
        // Memastikan nilai limit tidak kembali ke 0 saat aplikasi restart
        dashboardViewModel.monthlyLimit.observe(viewLifecycleOwner) { limit ->
            monthlyLimit = limit
            updateMonthlyLimitUI() // Update UI saat nilai limit dari database masuk
        }

        transactionViewModel.incomeExpensePieData.observe(viewLifecycleOwner) { entries ->
            if (entries.isNullOrEmpty()) {
                binding.pieChartFinancial.clear()
                return@observe
            }

            val colors = ArrayList<Int>()
            for (entry in entries) {
                if (entry.label.equals("Income", ignoreCase = true)) {
                    colors.add(ContextCompat.getColor(requireContext(), R.color.status_approved))
                } else {
                    colors.add(ContextCompat.getColor(requireContext(), R.color.red_500))
                }
            }

            val dataSet = PieDataSet(entries, "").apply {
                this.colors = colors
                yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
                xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
                valueLinePart1OffsetPercentage = 80f
                valueLinePart1Length = 0.4f
                valueLinePart2Length = 0.4f
                valueLineWidth = 1f
                valueLineColor = Color.GRAY
                sliceSpace = 3f
            }

            val pieData = PieData(dataSet).apply {
                setValueFormatter(PercentFormatter(binding.pieChartFinancial))
                setValueTextSize(11f)
                setValueTextColor(Color.BLACK)
            }

            binding.pieChartFinancial.data = pieData
            binding.pieChartFinancial.animateY(1400, Easing.EaseInOutQuad)
            binding.pieChartFinancial.invalidate()
        }

        // === INTERAKSI UI ===
        binding.imgToggleBalance.setOnClickListener {
            isMainBalanceVisible = !isMainBalanceVisible
            updateMainBalanceDisplay()
        }

        binding.cardAddWallet.root.setOnClickListener { showAddWalletDialog() }

        // Memanggil dialog set limit dari teks instruksi yang baru
        binding.btnSetLimit.setOnClickListener { showSetLimitDialog() }

        // Memperbarui UI di awal secara manual (optional, karena sudah ada observer)
        updateMonthlyLimitUI()

        binding.tvDate.text = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("en", "EN")).format(Date())
    }

    private fun setupPieChart() {
        binding.pieChartFinancial.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            setExtraOffsets(35f, 10f, 35f, 10f)
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            holeRadius = 55f
            transparentCircleRadius = 60f
            legend.isEnabled = false
            setDrawEntryLabels(true)
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(11f)
        }
    }

    private fun updateCard(cardView: View, wallet: Wallet) {
        val tvName = cardView.findViewById<TextView>(R.id.tv_wallet_name)
        val tvBalance = cardView.findViewById<TextView>(R.id.tv_wallet_balance)
        val imgToggle = cardView.findViewById<ImageView>(R.id.img_toggle_wallet_balance)

        tvName?.text = wallet.name
        val real = formatRupiah(wallet.balance)
        val hidden = "IDR •••••••"

        var isVisible = walletVisibilityStates[wallet.id] ?: false

        fun render() {
            tvBalance?.text = if (isVisible) real else hidden

            if (wallet.balance < 0) {
                // Warna merah jika minus (menggunakan Color.RED atau kode hex)
                tvBalance?.setTextColor(Color.RED)
            } else {
                // Warna biru gelap/hitam standar jika saldo aman
                tvBalance?.setTextColor(Color.WHITE)
            }

            imgToggle?.setImageResource(
                if (isVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off
            )
        }
        render()

        imgToggle?.setOnClickListener {
            isVisible = !isVisible
            walletVisibilityStates[wallet.id] = isVisible
            render()
        }

        cardView.setOnClickListener {
            transactionViewModel.setActiveWallet(wallet.id)
            findNavController().navigate(R.id.action_mainDashboard_to_customerListFragment)
        }

        cardView.setOnLongClickListener {
            // Proteksi agar "Main Wallet" tidak bisa dihapus (opsional)
            if (wallet.name.lowercase() != "main wallet") {
                showDeleteConfirmation(wallet)
            } else {
                Toast.makeText(context, "Main wallet cannot be deleted", Toast.LENGTH_SHORT).show()
            }
            true // Return true agar event klik biasa tidak ikut terpicu
        }
    }

    private fun showDeleteConfirmation(wallet: Wallet) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Wallet")
            .setMessage("Are you sure you want to delete '${wallet.name}'? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                // Pastikan Anda sudah memiliki fungsi deleteWallet di DashboardViewModel
                dashboardViewModel.deleteWallet(wallet.id)
                Toast.makeText(context, "${wallet.name} deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateMainBalanceDisplay() {
        binding.tvBalanceNominal.text = if (isMainBalanceVisible) actualMainBalanceFormatted else "IDR •••••••••••"

        // Ambil angka murni dari dashboardViewModel untuk cek minus
        val totalVal = dashboardViewModel.totalBalance.value ?: 0.0
        if (totalVal < 0) {
            binding.tvBalanceNominal.setTextColor(Color.RED)
        } else {
            val myColor = Color.parseColor("#5459AC")
            binding.tvBalanceNominal.setTextColor(myColor)
        }

        binding.imgToggleBalance.setImageResource(
            if (isMainBalanceVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off
        )
    }

    private fun updateMonthlyLimitUI() {
        val percentage = if (monthlyLimit > 0) ((usedAmount.toFloat() / monthlyLimit) * 100).toInt().coerceIn(0, 100) else 0
        binding.progressBarLimit.progress = percentage
        binding.tvLimitPercentage.text = "$percentage%"

        val color = when {
            percentage >= 90 -> Color.RED
            percentage >= 50 -> Color.YELLOW
            else -> ContextCompat.getColor(requireContext(), R.color.status_approved)
        }

        binding.progressBarLimit.progressDrawable.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN)
        binding.tvLimitPercentage.setTextColor(color)
        binding.tvLimitInfo.text = "${formatRupiah(usedAmount)} of ${formatRupiah(monthlyLimit)}"
    }

    private fun formatRupiah(amount: Number): String {
        val doubleValue = amount.toDouble()
        val format = NumberFormat.getNumberInstance(Locale("id", "ID"))
        val formattedNumber = format.format(Math.abs(doubleValue)) // Gunakan absolut agar minusnya hilang dulu

        // Jika angka negatif, letakkan tanda minus setelah "IDR "
        return if (doubleValue < 0) {
            "IDR -$formattedNumber"
        } else {
            "IDR $formattedNumber"
        }
    }

    private fun showAddWalletDialog() {
        val allOptions = listOf("BCA", "BLU", "BNI", "MANDIRI", "DANA", "GOPAY", "OVO", "SPAY")
        val existing = transactionViewModel.wallets.value?.map { it.name.uppercase() } ?: emptyList()
        val available = allOptions.filterNot { it in existing } + "Other..."

        AlertDialog.Builder(requireContext())
            .setTitle("Add New Wallet")
            .setItems(available.toTypedArray()) { _, which ->
                if (available[which] == "Other...") showCustomWalletDialog()
                else dashboardViewModel.addNewWallet(available[which])
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun showCustomWalletDialog() {
        val input = TextInputEditText(requireContext()).apply { hint = "e.g., Jenius" }
        AlertDialog.Builder(requireContext()).setTitle("Add Custom Wallet").setView(input)
            .setPositiveButton("Add") { _, _ ->
                input.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { dashboardViewModel.addNewWallet(it) }
            }.setNegativeButton("Cancel", null).show()
    }

    private fun showSetLimitDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_set_monthly_limit, null)
        val etLimit = dialogView.findViewById<TextInputEditText>(R.id.etLimit)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btnSave)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)

        etLimit.addTextChangedListener(MoneyTextWatcher(etLimit))

        if (monthlyLimit > 0) {
            val formattedInitial = NumberFormat.getNumberInstance(Locale("id", "ID")).format(monthlyLimit)
            etLimit.setText(formattedInitial)
        }

        // Builder tanpa tombol default
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Klik tombol Batal kustom
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        // Klik tombol Simpan kustom
        btnSave.setOnClickListener {
            val rawValue = etLimit.text.toString().replace("[\\D]".toRegex(), "")
            val input = rawValue.toIntOrNull() ?: 0
            dashboardViewModel.saveMonthlyLimit(input)
            Toast.makeText(context, "Limit diperbarui", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun displayUserName(user: FirebaseUser) {
        binding.tvGreeting.text = "Hi, ${user.displayName ?: "Fintelis Buddy"}!"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}