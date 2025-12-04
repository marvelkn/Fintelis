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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomerDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // PERBAIKAN: Setup listener untuk tombol kembali di toolbar
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp() // This will navigate back to the previous screen
        }

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
                performAnalysis(customer)
            }
        } else {
            binding.btnAnalyze.isVisible = false
        }
    }

    private fun performAnalysis(customer: Customer) {
        val finalStatus = if (customer.riskCategory == RiskCategory.HIGH) {
            Status.REJECTED
        } else {
            Status.APPROVED
        }

        customerViewModel.updateCustomerStatus(customer.id, finalStatus)

        // 3. Buat salinan objek nasabah dengan status yang sudah diperbarui
        //    Gunakan fungsi copy() dari data class, ini cara yang paling efisien dan aman.
        val customerForAnalysis = customer.copy(status = finalStatus)

        // 4. Buat action dan navigasi dengan objek yang sudah lengkap dan diperbarui
        val action = CustomerDetailFragmentDirections.actionToAnalysisResult(customerForAnalysis)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

        /*companion object {
            /**
             * Use this factory method to create a new instance of
             * this fragment using the provided parameters.
             *
             * @param param1 Parameter 1.
             * @param param2 Parameter 2.
             * @return A new instance of fragment CustomerDetailFragment.
             */
            // TODO: Rename and change types and number of parameters
            @JvmStatic
            fun newInstance(param1: String, param2: String) =
                CustomerDetailFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, param1)
                        putString(ARG_PARAM2, param2)
                    }
                }
        }*/
}