package com.example.fintelis

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fintelis.adapter.CustomerAdapter
import com.example.fintelis.databinding.FragmentCustomerListBinding
import com.example.fintelis.viewmodel.CustomerViewModel
import com.example.fintelis.viewmodel.SortOrder

class CustomerListFragment : Fragment() {

    private var _binding: FragmentCustomerListBinding? = null
    private val binding get() = _binding!!
    private val customerViewModel: CustomerViewModel by activityViewModels()
    private lateinit var customerAdapter: CustomerAdapter
    /*override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }*/

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
        setupToolbar()
        setupSearch() // Panggil fungsi setup untuk search bar

        customerViewModel.sortedCustomers.observe(viewLifecycleOwner) { customerList ->
            customerAdapter.updateData(customerList)
        }

        binding.fabAddCustomer.setOnClickListener {
            findNavController().navigate(R.id.action_customerListFragment_to_addCustomerFragment)
        }
    }

    private fun setupToolbar() {
        binding.toolbar.inflateMenu(R.menu.customer_list_menu)
        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_filter) {
                showSortDialog()
                true
            } else {
                false
            }
        }
    }

    // FUNGSI BARU UNTUK MENG-HANDLE SEARCH BAR
    private fun setupSearch() {
        binding.searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Setiap kali teks berubah, panggil fungsi search di ViewModel
                customerViewModel.searchCustomers(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun showSortDialog() {
        val sortOptions = arrayOf(
            "Default", "Date (Newest First)", "Date (Oldest First)",
            "Score (High to Low)", "Score (Low to High)", "Risk Category", "Status"
        )
        AlertDialog.Builder(requireContext())
            .setTitle("Sort By")
            .setItems(sortOptions) { dialog, which ->
                val selectedOrder = when (which) {
                    1 -> SortOrder.DATE_DESC
                    2 -> SortOrder.DATE_ASC
                    3 -> SortOrder.SCORE_DESC
                    4 -> SortOrder.SCORE_ASC
                    5 -> SortOrder.RISK_CATEGORY
                    6 -> SortOrder.STATUS
                    else -> SortOrder.NONE
                }
                customerViewModel.sortCustomers(selectedOrder)
                dialog.dismiss()
            }
            .show()
        }
    private fun setupRecyclerView() {
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