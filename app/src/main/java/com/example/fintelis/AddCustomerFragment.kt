package com.example.fintelis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.fintelis.data.Customer
import com.example.fintelis.data.RiskCategory
import com.example.fintelis.data.Status
import com.example.fintelis.databinding.FragmentAddCustomerBinding
import com.example.fintelis.viewmodel.CustomerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddCustomerFragment : Fragment() {

    private var _binding: FragmentAddCustomerBinding? = null
    private val binding get() = _binding!!
    private val customerViewModel: CustomerViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddCustomerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup listener untuk tombol kembali di toolbar
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSave.setOnClickListener {
            saveNewCustomer()
        }
    }

    private fun saveNewCustomer() {
        // Mengambil teks dari TextInputEditText di dalam TextInputLayout
        val name = binding.tilName.editText?.text.toString().trim()
        val id = binding.tilId.editText?.text.toString().trim()
        val scoreText = binding.tilCreditScore.editText?.text.toString().trim()

        if (name.isEmpty() || id.isEmpty() || scoreText.isEmpty()) {
            Toast.makeText(requireContext(), "All fields must be filled", Toast.LENGTH_SHORT).show()
            return
        }

        val score = scoreText.toInt()
        val riskCategory = when {
            score > 670 -> RiskCategory.LOW
            score >= 580 -> RiskCategory.MEDIUM
            else -> RiskCategory.HIGH
        }
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        val currentDate = sdf.format(Date())

        val newCustomer = Customer(
            id = id,
            name = name,
            submissionDate = currentDate,
            creditScore = score,
            riskCategory = riskCategory,
            status = Status.PENDING
        )

        customerViewModel.addCustomer(newCustomer)
        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}