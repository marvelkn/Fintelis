package com.example.fintelis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.fintelis.databinding.FragmentAnalysisResultBinding

class AnalysisResultFragment : Fragment() {

    private var _binding: FragmentAnalysisResultBinding? = null
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

        val status = args.analysisStatus
        binding.tvResultStatus.text = status

        if (status == "Approved") {
            binding.tvResultStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_approved))
            binding.ivResultIcon.setImageResource(R.drawable.ic_check_circle) // Ganti dengan ikon centang Anda
        } else {
            binding.tvResultStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_rejected))
            binding.ivResultIcon.setImageResource(R.drawable.ic_cancel) // Ganti dengan ikon silang Anda
        }

        binding.btnOk.setOnClickListener {
            // Kembali ke halaman daftar nasabah
            findNavController().popBackStack(R.id.customerListFragment, false)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}