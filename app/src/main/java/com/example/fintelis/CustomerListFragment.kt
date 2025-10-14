package com.example.fintelis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fintelis.adapter.CustomerAdapter
import com.example.fintelis.databinding.FragmentCustomerListBinding
import com.example.fintelis.viewmodel.CustomerViewModel

class CustomerListFragment : Fragment() {

    private var _binding: FragmentCustomerListBinding? = null
    private val binding get() = _binding!!

    // KUNCI: Menggunakan activityViewModels() untuk berbagi ViewModel
    private val customerViewModel: CustomerViewModel by activityViewModels()

    // Inisialisasi adapter sekali saja
    private lateinit var customerAdapter: CustomerAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomerListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        // KUNCI: Mengamati (observe) LiveData dari ViewModel
        customerViewModel.customers.observe(viewLifecycleOwner) { customerList ->
            // Saat data berubah, cukup update data di adapter.
            // Tidak perlu membuat adapter baru setiap saat.
            customerAdapter.updateData(customerList)
        }

        binding.fabAddCustomer.setOnClickListener {
            findNavController().navigate(R.id.action_customerListFragment_to_addCustomerFragment)
        }
    }

    private fun setupRecyclerView() {
        // Inisialisasi adapter dengan list kosong terlebih dahulu
        customerAdapter = CustomerAdapter(mutableListOf()) { customer ->
            val action = CustomerListFragmentDirections.actionCustomerListFragmentToCustomerDetailFragment(customer)
            findNavController().navigate(action)
        }
        binding.recyclerViewCustomers.apply {
            adapter = customerAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

