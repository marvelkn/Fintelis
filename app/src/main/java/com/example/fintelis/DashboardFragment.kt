package com.example.fintelis

import android.graphics.Color
import java.text.NumberFormat
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
/*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
*/
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import java.util.Locale


class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    // Variabel Firebase
    private lateinit var auth: FirebaseAuth
    //private lateinit var database: FirebaseDatabase

    private val db = Firebase.firestore
    // Variabel untuk state saldo (Sembunyi/Tampil) dan saldo
    private var isBalanceVisible = false
    private var currentBalance: Long = 0
    private lateinit var tvBalance: TextView
    private lateinit var btnToggle: ImageView
    private lateinit var pieChart: PieChart
    private lateinit var tvGreeting: TextView
    // --- TAMBAHAN 1: Deklarasikan TextView untuk debugging ---
    private lateinit var tvDebugBalance: TextView
    private lateinit var tvDebugIncome: TextView
    private lateinit var tvDebugExpense: TextView
    private lateinit var tvDebugId: TextView



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inisialisasi Firebase
        auth = FirebaseAuth.getInstance()
        //database = FirebaseDatabase.getInstance()

        // Inisialisasi Views
        tvBalance = view.findViewById(R.id.tv_balance_nominal)
        btnToggle = view.findViewById(R.id.img_toggle_balance)
        pieChart = view.findViewById(R.id.pieChartFinancial)

        // --- TAMBAHAN 2: Inisialisasi TextView untuk debugging ---
        tvDebugIncome = view.findViewById(R.id.tv_income_nominal)
        tvDebugExpense = view.findViewById(R.id.tv_expense_nominal)


        tvGreeting = view.findViewById(R.id.tv_greeting)


        if (auth.currentUser != null) {
            val user = auth.currentUser
            displayUserName(user)
            setupFirestoreListener()
        }

        // --- 1. SETUP FITUR HIDE/UNHIDE SALDO ---
        setupBalanceVisibility()

        // --- 2. SETUP DONUT CHART ---
        //setupPieChart(view)
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

    private fun setupFirestoreListener() {
        // 1. Dapatkan ID pengguna yang sedang login dengan aman
        val userId = auth.currentUser?.uid

        // Jika karena suatu alasan userId null, hentikan fungsi agar tidak crash
        if (userId == null) {
            Toast.makeText(context, "Could not get user ID.", Toast.LENGTH_SHORT).show()
            return
        }


        // 1. Listener untuk data utama pengguna (balance, dll)
        db.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("Firestore", "Listen failed.", error)
                    Toast.makeText(context, "Gagal memuat data: ${error.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    // Ambil data balance dari document snapshot
                    val balance = snapshot.getLong("balance") ?: 0L
                    currentBalance = balance
                    updateBalanceView()

                } else {
                    Log.d("Firestore", "Current data: null")
                }
            }

        // 2. Listener untuk sub-koleksi 'transactions'
        db.collection("users").document(userId).collection("transaction")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("Firestore", "Listen failed for transactions.", error)
                    return@addSnapshotListener
                }

                var totalIncome: Long = 0
                var totalExpense: Long = 0

                // Loop melalui semua dokumen di dalam koleksi 'transactions'
                for (doc in snapshot!!.documents) {
                    val amount = doc.getLong("amount") ?: 0L
                    val type = doc.getString("type")

                    if (type == "income") {
                        totalIncome += amount
                    } else if (type == "expenses") {
                        totalExpense += amount
                    }
                }
                updatePieChart(totalIncome.toFloat(), totalExpense.toFloat())

                // Update debug text views
                tvDebugIncome.text = "Incomes: $totalIncome"
                tvDebugExpense.text = "Expenses: $totalExpense"
            }
    }

    private fun setupBalanceVisibility() {
        // Set state awal (Terkunci/Hidden)
        updateBalanceView()

        btnToggle.setOnClickListener {
            // Ubah status true <-> false
            isBalanceVisible = !isBalanceVisible
            updateBalanceView()
        }
    }

    private fun updateBalanceView() {
        if (isBalanceVisible) {
            // Jika Mode Terbuka, format angka menjadi Rupiah
            tvBalance.text = formatToRupiah(currentBalance)
            btnToggle.setImageResource(R.drawable.ic_visibility)
        } else {
            // Jika Mode Tersembunyi
            tvBalance.text = "Rp •••••••••••"
            btnToggle.setImageResource(R.drawable.ic_visibility_off)
        }
    }

    private fun updatePieChart(income: Float, expense: Float) {
        // Jika tidak ada income dan expense, tampilkan chart kosong atau pesan
        if (income == 0f && expense == 0f) {
            pieChart.clear()
            pieChart.invalidate()
            return
        }

        val entries = ArrayList<PieEntry>()
        entries.add(PieEntry(income, "Income")) // Tambahkan data income
        entries.add(PieEntry(expense, "Expense")) // Tambahkan data expense

        val colors = ArrayList<Int>()
        colors.add(Color.parseColor("#FFC107")) // Kuning (Income)
        colors.add(Color.parseColor("#EF5350")) // Merah (Expense)

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = colors
        dataSet.sliceSpace = 3f
        dataSet.setDrawValues(false)

        val pieData = PieData(dataSet)
        pieChart.data = pieData

        pieChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            holeRadius = 65f
            transparentCircleRadius = 70f
            setEntryLabelColor(Color.TRANSPARENT)
            animateY(1000) // Animasi lebih cepat
            invalidate()
        }
    }

    // Fungsi bantuan untuk format Angka ke Rupiah
    private fun formatToRupiah(number: Long): String {
        val localeID = Locale("in", "ID")
        val formatter = NumberFormat.getCurrencyInstance(localeID)
        formatter.maximumFractionDigits = 0 // Hilangkan desimal
        return formatter.format(number)
    }

    private fun setupPieChart(view: View) {
        val pieChart = view.findViewById<PieChart>(R.id.pieChartFinancial)

        // 1. Siapkan Data (Income, Expenses, Investment)
        val entries = ArrayList<PieEntry>()
        entries.add(PieEntry(35f)) // Income (35%)
        entries.add(PieEntry(45f)) // Expense (45%)
        entries.add(PieEntry(20f)) // Investment (20%)

        // 2. Siapkan Warna (Sesuai desain XML sebelumnya)
        val colors = ArrayList<Int>()
        colors.add(Color.parseColor("#FFC107")) // Kuning (Income)
        colors.add(Color.parseColor("#EF5350")) // Merah (Expense)
        colors.add(Color.parseColor("#26C6DA")) // Cyan (Investment)

        // 3. Konfigurasi DataSet
        val dataSet = PieDataSet(entries, "")
        dataSet.colors = colors
        dataSet.sliceSpace = 3f // Jarak antar potongan kue
        dataSet.setDrawValues(false) // Hilangkan angka di dalam chart agar bersih

        // 4. Konfigurasi Tampilan Chart (Donut Style)
        val pieData = PieData(dataSet)
        pieChart.data = pieData

        pieChart.apply {
            description.isEnabled = false // Matikan label deskripsi di pojok kanan bawah
            legend.isEnabled = false      // Matikan legend bawaan (karena kita buat custom di XML)

            isDrawHoleEnabled = true      // Aktifkan lubang tengah (Donut)
            setHoleColor(Color.TRANSPARENT) // Warna lubang transparan atau putih

            holeRadius = 65f              // Besar lubang dalam
            transparentCircleRadius = 70f // Efek bayangan lingkaran

            setEntryLabelColor(Color.TRANSPARENT) // Hilangkan text label di dalam chart
            animateY(1400) // Animasi putar saat loading
            invalidate() // Render ulang
        }
    }
}
