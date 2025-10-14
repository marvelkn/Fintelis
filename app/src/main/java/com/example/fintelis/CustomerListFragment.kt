package com.example.fintelis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fintelis.adapter.CustomerAdapter
import com.example.fintelis.data.Customer
import com.example.fintelis.data.RiskCategory
import com.example.fintelis.data.Status
import com.example.fintelis.databinding.FragmentCustomerListBinding

class CustomerListFragment : Fragment() {

    private var _binding: FragmentCustomerListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomerListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dummyCustomers = generateDummyData()

        val customerAdapter = CustomerAdapter(dummyCustomers) { selectedCustomer ->
            // Aksi saat item diklik: navigasi ke detail
            val action = CustomerListFragmentDirections.actionCustomerListFragmentToCustomerDetailFragment(selectedCustomer)
            findNavController().navigate(action)
        }

        binding.recyclerViewCustomers.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = customerAdapter
        }
    }

    private fun generateDummyData(): List<Customer> {
        return listOf(
            Customer("ID-20251012001", "Budi Santoso", "Oct 12, 2025", 780, RiskCategory.LOW, Status.APPROVED),
            Customer("ID-20251011002", "Citra Lestari", "Oct 11, 2025", 420, RiskCategory.HIGH, Status.REJECTED),
            Customer("ID-20251010003", "Agus Wijaya", "Oct 10, 2025", 650, RiskCategory.MEDIUM, Status.PENDING),
            Customer("ID-20251009004", "Dewi Anggraini", "Oct 09, 2025", 810, RiskCategory.LOW, Status.APPROVED)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
