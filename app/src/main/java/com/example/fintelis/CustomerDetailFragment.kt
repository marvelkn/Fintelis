package com.example.fintelis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.fintelis.data.Status // Import enum Status
import com.example.fintelis.databinding.FragmentCustomerDetailBinding
import com.example.fintelis.viewmodel.CustomerViewModel

class CustomerDetailFragment : Fragment() {

    private var _binding: FragmentCustomerDetailBinding? = null
    private val binding get() = _binding!!

    private val args: CustomerDetailFragmentArgs by navArgs()
    private val customerViewModel: CustomerViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomerDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val customer = args.customerData

        binding.tvDetailName.text = customer.name
        binding.tvDetailId.text = customer.id

        // PERBAIKAN: Gunakan .name untuk menampilkan enum sebagai teks
        val details = """
            Submission Date: ${customer.submissionDate}
            Credit Score: ${customer.creditScore}
            Risk Category: ${customer.riskCategory.name}
            Status: ${customer.status.name}
        """.trimIndent()
        binding.tvDetailInfo.text = details

        // PERBAIKAN: Bandingkan dengan enum Status.PENDING
        if (customer.status == Status.PENDING) {
            binding.btnAnalyze.isVisible = true
            binding.btnAnalyze.setOnClickListener {
                performAnalysis(customer.id, customer.creditScore)
            }
        } else {
            binding.btnAnalyze.isVisible = false
        }
    }

    private fun performAnalysis(customerId: String, score: Int) {
        // PERBAIKAN: Tentukan status akhir sebagai objek enum
        val finalStatus: Status = if (score >= 580) Status.APPROVED else Status.REJECTED

        // 1. Update status di ViewModel menggunakan enum
        customerViewModel.updateCustomerStatus(customerId, finalStatus)

        // 2. Navigasi ke halaman hasil dengan mengirim status sebagai String (.name)
        val action = CustomerDetailFragmentDirections.actionToAnalysisResult(finalStatus.name)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

