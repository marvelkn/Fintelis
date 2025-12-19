package com.example.fintelis

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController // PENTING
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fintelis.adapter.TransactionAdapter
import com.example.fintelis.data.TransactionType
import com.example.fintelis.viewmodel.TransactionViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class DetailCashflowFragment : Fragment() {

    private val viewModel: TransactionViewModel by activityViewModels()
    private lateinit var transactionAdapter: TransactionAdapter

    private var isExpenseMode: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ambil data dari arguments Navigation Component
        arguments?.let {
            isExpenseMode = it.getBoolean("isExpense", true)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_detail_cashflow, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewHeaderBg: View = view.findViewById(R.id.viewHeaderBg)
        val tvPageTitle: TextView = view.findViewById(R.id.tvPageTitle)
        val tvTotalAmount: TextView = view.findViewById(R.id.tvTotalAmount)
        val tvDateFilter: TextView = view.findViewById(R.id.tvDateFilter)
        val btnBack: ImageButton = view.findViewById(R.id.btnBack)
        val rvDetailTransactions: RecyclerView = view.findViewById(R.id.rvDetailTransactions)

        // 1. Setup UI
        if (isExpenseMode) {
            viewHeaderBg.setBackgroundColor(Color.parseColor("#C53030"))
            tvPageTitle.text = "Expense Details"
        } else {
            viewHeaderBg.setBackgroundColor(Color.parseColor("#38A169"))
            tvPageTitle.text = "Income Details"
        }

        // 2. Setup Back Button (FIXED: Menggunakan NavController)
        btnBack.setOnClickListener {
            // Ini akan kembali ke halaman sebelumnya dalam graph (Visualization)
            // tanpa me-reset dashboard.
            findNavController().navigateUp()
        }

        // 3. Setup RecyclerView
        transactionAdapter = TransactionAdapter(mutableListOf()) { /* Click Listener */ }
        rvDetailTransactions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionAdapter
        }

        // 4. Observe Data
        viewModel.currentMonth.observe(viewLifecycleOwner) { calendar ->
            val fmt = SimpleDateFormat("MMMM yyyy", Locale.US)
            tvDateFilter.text = fmt.format(calendar.time)
        }

        viewModel.displayedTransactions.observe(viewLifecycleOwner) { transactions ->
            if (transactions != null) {
                val targetType = if (isExpenseMode) TransactionType.EXPENSE else TransactionType.INCOME
                val filteredList = transactions
                    .filter { it.type == targetType }
                    .sortedByDescending { viewModel.parseDatePublic(it.date) }

                val total: Double = filteredList.sumOf { it.amount }
                val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                tvTotalAmount.text = formatRp.format(total)

                transactionAdapter.updateData(filteredList)
            }
        }
    }
}