package com.example.fintelis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.fintelis.data.Transaction
import com.example.fintelis.data.TransactionType
import com.example.fintelis.databinding.FragmentTransactionDetailBinding
import java.text.NumberFormat
import java.util.Locale

class TransactionDetailFragment : Fragment() {

    private var _binding: FragmentTransactionDetailBinding? = null
    private val binding get() = _binding!!

    // Menerima data 'transactionData' dari Safe Args
    private val args: TransactionDetailFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup Toolbar Back Button
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        // Ambil data dan tampilkan
        val transaction = args.transactionData
        displayTransactionData(transaction)
    }

    private fun displayTransactionData(item: Transaction) {
        binding.tvDetailTitle.text = item.title
        binding.tvDetailDate.text = item.date
        binding.tvDetailCategory.text = item.category
        binding.tvDetailWallet.text = item.walletId

        // Format Rupiah
        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        binding.tvDetailAmount.text = format.format(item.amount)

        // Atur warna & teks berdasarkan tipe transaksi
        if (item.type == TransactionType.INCOME) {
            binding.tvDetailType.text = "Income"
            binding.tvDetailAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_approved)) // Hijau
            // Opsional: Ganti icon jika ada
            // binding.ivDetailIcon.setImageResource(R.drawable.ic_arrow_up)
        } else {
            binding.tvDetailType.text = "Expense"
            binding.tvDetailAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_rejected)) // Merah
            // binding.ivDetailIcon.setImageResource(R.drawable.ic_arrow_down)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}