package com.example.fintelis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.fintelis.data.Customer
import com.example.fintelis.data.RiskCategory
import com.example.fintelis.data.Status
import com.example.fintelis.databinding.FragmentCustomerDetailBinding
import com.example.fintelis.viewmodel.CustomerViewModel

class CustomerDetailFragment : Fragment() {

    private var _binding: FragmentCustomerDetailBinding? = null
    private val binding get() = _binding!!

    private val args: CustomerDetailFragmentArgs by navArgs()
    private val customerViewModel: CustomerViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCustomerDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        val customer = args.customerData
        displayCustomerData(customer)
        setupAnalysisButton(customer)
    }

    private fun displayCustomerData(customer: Customer) {
        binding.tvDetailName.text = customer.name
        binding.tvDetailId.text = customer.id
        binding.tvDetailDate.text = customer.submissionDate
        binding.tvDetailScore.text = customer.creditScore.toString()
        binding.tvDetailRisk.text = customer.riskCategory.name
        binding.tvDetailStatus.text = customer.status.name

        val riskColor = when (customer.riskCategory) {
            RiskCategory.HIGH -> R.color.status_rejected
            RiskCategory.MEDIUM -> R.color.status_pending
            RiskCategory.LOW -> R.color.status_approved
        }
        binding.tvDetailRisk.setTextColor(ContextCompat.getColor(requireContext(), riskColor))
    }

    private fun setupAnalysisButton(customer: Customer) {
        if (customer.status == Status.PENDING) {
            binding.btnAnalyze.isVisible = true
            binding.btnAnalyze.setOnClickListener {
                performAnalysis(customer.id, customer.riskCategory)
            }
        } else {
            binding.btnAnalyze.isVisible = false
        }
    }

    private fun performAnalysis(customerId: String, risk: RiskCategory) {
        val finalStatus = if (risk == RiskCategory.HIGH) Status.REJECTED else Status.APPROVED

        // Update status di ViewModel
        customerViewModel.updateCustomerStatus(customerId, finalStatus)

        // PERBAIKAN UTAMA: "Minta" data yang sudah di-update dari ViewModel
        val updatedCustomer = customerViewModel.getCustomerById(customerId)

        if (updatedCustomer != null) {
            val action = CustomerDetailFragmentDirections.actionToAnalysisResult(updatedCustomer)
            findNavController().navigate(action)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}