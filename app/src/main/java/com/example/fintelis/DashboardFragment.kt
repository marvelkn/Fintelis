package com.example.fintelis

import android.graphics.Color
import java.text.NumberFormat
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.fintelis.data.TransactionType
import com.example.fintelis.viewmodel.TransactionViewModel
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    // Firebase Auth
    private lateinit var auth: FirebaseAuth

    // ViewModel
    private val viewModel: TransactionViewModel by activityViewModels()

    // UI Variables
    private var isBalanceVisible = false
    private var currentBalance: Double = 0.0
    
    private lateinit var tvBalance: TextView
    private lateinit var btnToggle: ImageView
    private lateinit var pieChart: PieChart
    private lateinit var tvGreeting: TextView
    private lateinit var tvIncome: TextView
    private lateinit var tvExpense: TextView
    private lateinit var tvDate: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize Views
        tvBalance = view.findViewById(R.id.tv_balance_nominal)
        btnToggle = view.findViewById(R.id.img_toggle_balance)
        pieChart = view.findViewById(R.id.pieChartFinancial)
        
        tvIncome = view.findViewById(R.id.tv_income_nominal)
        tvExpense = view.findViewById(R.id.tv_expense_nominal)
        tvGreeting = view.findViewById(R.id.tv_greeting)
        tvDate = view.findViewById(R.id.tv_date)

        // Set Current Date
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.US)
        tvDate.text = dateFormat.format(Date())

        if (auth.currentUser != null) {
            displayUserName(auth.currentUser)
        }

        setupBalanceVisibility()
        observeTransactions()
    }

    private fun displayUserName(user: FirebaseUser?) {
        val userName = user?.displayName
        if (!userName.isNullOrEmpty()) {
            tvGreeting.text = "Hi, $userName!"
        } else {
            tvGreeting.text = "Hi, Fintelis Buddy!"
        }
    }

    private fun observeTransactions() {
        viewModel.allTransactions.observe(viewLifecycleOwner) { transactions ->
            var totalIncome = 0.0
            var totalExpense = 0.0

            for (transaction in transactions) {
                if (transaction.type == TransactionType.INCOME) {
                    totalIncome += transaction.amount
                } else {
                    totalExpense += transaction.amount
                }
            }

            currentBalance = totalIncome - totalExpense
            updateBalanceView()

            // Update Income/Expense Text
            tvIncome.text = formatToRupiah(totalIncome)
            tvExpense.text = formatToRupiah(totalExpense)

            updatePieChart(totalIncome.toFloat(), totalExpense.toFloat())
        }
    }

    private fun setupBalanceVisibility() {
        updateBalanceView()

        btnToggle.setOnClickListener {
            isBalanceVisible = !isBalanceVisible
            updateBalanceView()
        }
    }

    private fun updateBalanceView() {
        if (isBalanceVisible) {
            tvBalance.text = formatToRupiah(currentBalance)
            btnToggle.setImageResource(R.drawable.ic_visibility)
        } else {
            tvBalance.text = "Rp •••••••••••"
            btnToggle.setImageResource(R.drawable.ic_visibility_off)
        }
    }

    private fun updatePieChart(income: Float, expense: Float) {
        if (income == 0f && expense == 0f) {
            pieChart.clear()
            pieChart.invalidate()
            return
        }

        val entries = ArrayList<PieEntry>()
        entries.add(PieEntry(income, "Income"))
        entries.add(PieEntry(expense, "Expense"))

        val colors = ArrayList<Int>()
        colors.add(Color.parseColor("#FFC107")) // Yellow (Income)
        colors.add(Color.parseColor("#EF5350")) // Red (Expense)

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
            animateY(1000)
            invalidate()
        }
    }

    private fun formatToRupiah(number: Double): String {
        val localeID = Locale("in", "ID")
        val formatter = NumberFormat.getCurrencyInstance(localeID)
        formatter.maximumFractionDigits = 0
        return formatter.format(number)
    }
}
