package com.example.fintelis

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.fintelis.data.Status
import com.example.fintelis.databinding.FragmentAnalysisResultBinding
import kotlin.getValue

// TODO: Rename parameter arguments, choose names that match
class AnalysisFragment : Fragment() {
    // TODO: Rename and change types of parameters
    /*private var _binding: FragmentAnalysisResultBinding? = null
    private val binding get() = _binding!!
    private val args: AnalysisResultFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalysisResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val customer = args.customerData

        // Mengisi data nasabah di kartu summary
        binding.tvResultCustomerName.text = customer.name
        binding.tvResultScore.text = customer.creditScore.toString()
        binding.tvResultRisk.text = customer.riskCategory.name

        // Mengisi status utama
        binding.tvResultStatus.text = customer.status.name

        // Logika untuk menampilkan visual yang sesuai
        if (customer.status == Status.APPROVED) {
            binding.ivResultIcon.setImageResource(R.drawable.ic_check_circle)
            binding.ivResultIcon.imageTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.status_approved)
            binding.tvResultStatus.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.status_approved
                )
            )
            binding.tvContextMessage.text =
                "Based on their profile, this customer is eligible for a loan."
        } else {
            binding.ivResultIcon.setImageResource(R.drawable.ic_cancel)
            binding.ivResultIcon.imageTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.status_rejected)
            binding.tvResultStatus.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.status_rejected
                )
            )
            binding.tvContextMessage.text =
                "The customer's profile indicates a high risk of default."
        }

        binding.btnOk.setOnClickListener {
            findNavController().popBackStack(R.id.customerListFragment, false)
        }
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
         * @return A new instance of fragment AnalysisFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            AnalysisFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }*/
*/}