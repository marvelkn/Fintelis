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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fintelis.adapter.TransactionAdapter
import com.example.fintelis.data.Transaction
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
            viewHeaderBg.setBackgroundColor(Color.parseColor("#C53030")) // Merah
            tvPageTitle.text = "Expense Details"
        } else {
            viewHeaderBg.setBackgroundColor(Color.parseColor("#38A169")) // Hijau
            tvPageTitle.text = "Income Details"
        }

        // 2. Setup Back Button
        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // 3. Setup RecyclerView
        transactionAdapter = TransactionAdapter(mutableListOf()) { /* Click Listener kosong */ }
        rvDetailTransactions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionAdapter
        }

        // 4. Observe Month
        viewModel.currentMonth.observe(viewLifecycleOwner) { calendar ->
            val fmt = SimpleDateFormat("MMMM yyyy", Locale.US)
            tvDateFilter.text = fmt.format(calendar.time)
        }

        // 5. Observe Data (FIXED: Explicit Type Added)
        // Kita tambahkan ": List<Transaction>?" agar compiler tidak bingung
        viewModel.transactions.observe(viewLifecycleOwner) { listData: List<Transaction>? ->
            val transactions = listData ?: emptyList()

            // Tentukan tipe yang mau diambil berdasarkan mode halaman
            val typeToFilter = if (isExpenseMode) TransactionType.EXPENSE else TransactionType.INCOME

            val filteredList = transactions
                .filter { it.type == typeToFilter }
                .sortedByDescending {
                    try {
                        // Parsing manual di sini tanpa panggil viewModel
                        SimpleDateFormat("MMM dd, yyyy", Locale.US).parse(it.date)
                    } catch(e: Exception) {
                        Date()
                    }
                }

            // Update Total
            val total: Double = filteredList.sumOf { it.amount }
            val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            tvTotalAmount.text = formatRp.format(total)

            // Update List
            transactionAdapter.updateData(filteredList)
        }
    }
}