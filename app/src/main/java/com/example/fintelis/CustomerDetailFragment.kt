package com.example.fintelis

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.example.fintelis.databinding.FragmentCustomerDetailBinding

class CustomerDetailFragment : Fragment() {

    private var _binding: FragmentCustomerDetailBinding? = null
    private val binding get() = _binding!!

    // Mengambil data customer yang dikirim melalui Safe Args
    private val args: CustomerDetailFragmentArgs by navArgs()

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

        val infoText = """
            Submission Date: ${customer.submissionDate}
            Credit Score: ${customer.creditScore}
            Risk Category: ${customer.riskCategory.name}
            Status: ${customer.status.name}
        """.trimIndent()

        binding.tvDetailInfo.text = infoText
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
