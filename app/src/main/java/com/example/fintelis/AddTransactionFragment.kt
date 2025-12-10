package com.example.fintelis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.fintelis.R
import com.example.fintelis.data.Transaction
import com.example.fintelis.data.TransactionType
import com.example.fintelis.databinding.FragmentAddTransactionBinding
import com.example.fintelis.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddTransactionFragment : Fragment() {
    private var _binding: FragmentAddTransactionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TransactionViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        val categories = resources.getStringArray(R.array.transaction_categories)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        binding.etCategory.setAdapter(adapter)

        binding.btnSave.setOnClickListener {
            val title = binding.etTitle.text.toString()
            val amountStr = binding.etAmount.text.toString()
            val category = binding.etCategory.text.toString()

            if (title.isBlank() || amountStr.isBlank() || category.isBlank()) {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val type = if (binding.rbIncome.isChecked) TransactionType.INCOME else TransactionType.EXPENSE
            val date = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date())

            val transaction = Transaction(
                title = title,
                amount = amountStr.toDouble(),
                type = type,
                date = date,
                category = category
            )
            viewModel.addTransaction(transaction)
            Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}