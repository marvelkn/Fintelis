package com.example.fintelis

import android.content.Context
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

    private var isBalanceVisible = false
    private var currentBalance: Double = 0.0
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
        setupLimitCalculation()

        dashboardViewModel.totalBalance.observe(viewLifecycleOwner) { totalBalance ->
            currentBalance = totalBalance ?: 0.0
            updateBalanceUI()
        }

        binding.imgToggleBalance.setOnClickListener {
            isBalanceVisible = !isBalanceVisible
            updateBalanceUI()
            refreshWalletsUI()
        }

        dashboardViewModel.wallets.observe(viewLifecycleOwner) { wallets ->
            refreshWalletsUI()
        }

        binding.cardAddWallet.root.setOnClickListener {
            showAddWalletDialog()
        }

        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        
        // Set ke "ALL" agar konsisten mengambil semua data
        transactionViewModel.setActiveWallet("ALL")
        
        // PENTING: Observasi displayedTransactions agar MediatorLiveData aktif dan processList berjalan
        // Tanpa ini, incomePercentage dll tidak akan terupdate karena processList tidak dipanggil
        transactionViewModel.displayedTransactions.observe(viewLifecycleOwner) { 
            // Data list transaksi tidak ditampilkan di sini, tapi perlu diobservasi
        }

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
                    ContextCompat.getColor(requireContext(), R.color.status_approved),
                    ContextCompat.getColor(requireContext(), R.color.red_500)
                )
                setDrawValues(false) // Tidak menggambar nilai di dalam slice (karena sudah ada label)
            }

            val pieData = PieData(dataSet)
            // Mengatur warna teks label (Income/Expense) menjadi HITAM
            pieData.setValueTextColor(Color.BLACK) 
            
            binding.pieChartFinancial.data = pieData
            binding.pieChartFinancial.invalidate()
        }
        
        // Set current date
        val currentDate = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.US).format(Date())
        binding.tvDate.text = currentDate
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
            
            // Mengatur warna label entry (Income/Expense) menjadi HITAM
            setEntryLabelColor(Color.BLACK) 
            setEntryLabelTextSize(12f)
        }
    }

    private fun updateCard(cardView: View, wallet: Wallet) {
        val tvWalletName = cardView.findViewById<TextView>(R.id.tv_wallet_name)
        val tvWalletBalance = cardView.findViewById<TextView>(R.id.tv_wallet_balance)

        tvWalletName?.text = wallet.name
        if (isBalanceVisible) {
            // Jika mata terbuka: Tampilkan saldo asli
            tvWalletBalance?.text = dashboardViewModel.formatRupiah(wallet.balance)
        } else {
            // Jika mata tertutup: Tampilkan sensor
            tvWalletBalance?.text = "IDR •••••••"
        }

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

    private fun setupLimitCalculation() {
        // 1. Ambil LIMIT dari LOKAL (SharedPreferences)
        val sharedPref = requireContext().getSharedPreferences("FinancialPrefs", Context.MODE_PRIVATE)
        // Default 1 rupiah agar tidak error pembagian nol jika belum diset
        val limitLocal = sharedPref.getLong("monthly_limit", 0).toDouble()

        // Jika user belum pernah set limit, kita bisa set UI default atau return
        if (limitLocal == 0.0) {
            binding.tvLimitPercentage.text = "Set Limit"
            binding.progressBarLimit.progress = 0
        }

        // 2. Ambil PENGELUARAN dari FIREBASE
        // Kita gunakan data yang sudah ada di TransactionViewModel agar lebih efisien
        // Asumsi: TransactionViewModel sudah menghitung total expense dari database
        transactionViewModel.expenseNominal.observe(viewLifecycleOwner) { totalExpense ->

            // 3. Lakukan Perhitungan
            if (limitLocal > 0) {
                val percentage = ((totalExpense / limitLocal) * 100).toInt()

                // Update UI
                binding.progressBarLimit.progress = if (percentage > 100) 100 else percentage
                binding.tvLimitPercentage.text = "$percentage%"

                // Opsional: Ubah warna jika over limit
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
                } else if(percentage >= 10 && percentage < 50){
                    val approvedColor = ContextCompat.getColor(requireContext(), R.color.status_approved)
                    binding.progressBarLimit.progressDrawable.setColorFilter(
                        approvedColor, android.graphics.PorterDuff.Mode.SRC_IN
                    )
                    binding.tvLimitPercentage.setTextColor(approvedColor)
                } else {
                    // Reset warna (misal ke default biru/hijau)
                    binding.progressBarLimit.progressDrawable.clearColorFilter()
                    binding.tvLimitPercentage.setTextColor(ContextCompat.getColor(requireContext(), R.color.black)) // Atau warna default
                }

                // Tampilkan detail angka (misal: "500rb / 1Jt")
                // Pastikan Anda punya TextView untuk ini, misal binding.tvLimitDetails
                val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                binding.tvLimitNominal.text = "${dashboardViewModel.formatRupiah(totalExpense)} / ${dashboardViewModel.formatRupiah(limitLocal)}"
            }
        }
    }

    // Fungsi helper untuk mengatur tampilan teks dan icon
    private fun updateBalanceUI() {
        if (isBalanceVisible) {
            // TAMPILKAN SALDO
            // Format ke rupiah
            val formattedBalance = dashboardViewModel.formatRupiah(currentBalance)
            binding.tvBalanceNominal.text = "$formattedBalance"

            // Ganti icon menjadi mata terbuka (visibility on)
            // Pastikan Anda punya drawable ic_visibility_on
            binding.imgToggleBalance.setImageResource(R.drawable.ic_visibility_on)
        } else {
            // SEMBUNYIKAN SALDO
            binding.tvBalanceNominal.text = "IDR •••••••••••"

            // Ganti icon menjadi mata tertutup (visibility off)
            binding.imgToggleBalance.setImageResource(R.drawable.ic_visibility_off)
        }
    }

    private fun refreshWalletsUI() {
        // Ambil data wallet terakhir dari ViewModel
        val wallets = dashboardViewModel.wallets.value ?: return

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

        // Sembunyikan semua kartu dulu
        cardMap.values.forEach { it.visibility = View.GONE }

        // Tampilkan hanya kartu yang dimiliki user
        wallets.forEach { wallet ->
            val cardView = cardMap[wallet.name.uppercase()]
            cardView?.let {
                it.visibility = View.VISIBLE
                updateCard(it, wallet) // <-- Fungsi updateCard sekarang sudah pakai logika isBalanceVisible
            }
        }
    }


    override fun onResume() {
        super.onResume()
        // Memastikan setiap kali kembali ke halaman ini, mode kembali ke "All Wallets"
        transactionViewModel.setActiveWallet("ALL")

        // Refresh perhitungan limit (dari kode sebelumnya)
        setupLimitCalculation()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}