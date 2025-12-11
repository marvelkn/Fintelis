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

    // Arguments
    private var isExpenseMode: Boolean = true

    companion object {
        const val ARG_IS_EXPENSE = "arg_is_expense"

        fun newInstance(isExpense: Boolean): DetailCashflowFragment {
            val fragment = DetailCashflowFragment()
            val args = Bundle()
            args.putBoolean(ARG_IS_EXPENSE, isExpense)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            isExpenseMode = it.getBoolean(ARG_IS_EXPENSE, true)
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

        // Init Views
        val viewHeaderBg: View = view.findViewById(R.id.viewHeaderBg)
        val tvPageTitle: TextView = view.findViewById(R.id.tvPageTitle)
        val tvTotalAmount: TextView = view.findViewById(R.id.tvTotalAmount)
        val tvDateFilter: TextView = view.findViewById(R.id.tvDateFilter)
        val btnBack: ImageButton = view.findViewById(R.id.btnBack)
        val rvDetailTransactions: RecyclerView = view.findViewById(R.id.rvDetailTransactions)

        // 1. Setup UI Theme based on Type
        if (isExpenseMode) {
            viewHeaderBg.setBackgroundColor(Color.parseColor("#C53030")) // Merah
            tvPageTitle.text = "Expense Details"
        } else {
            viewHeaderBg.setBackgroundColor(Color.parseColor("#38A169")) // Hijau
            tvPageTitle.text = "Income Details"
        }

        // 2. Setup Back Button
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 3. Setup RecyclerView
        transactionAdapter = TransactionAdapter(mutableListOf()) { /* Handle click item */ }
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

                // Filter Data
                val filteredList = transactions
                    .filter { it.type == targetType }
                    .sortedByDescending { viewModel.parseDatePublic(it.date) }

                // Update Total Text
                val total: Double = filteredList.sumOf { it.amount }
                val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                tvTotalAmount.text = formatRp.format(total)

                // Update List
                transactionAdapter.updateData(filteredList)
            }
        }
    }
}