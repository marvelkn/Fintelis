package com.example.fintelis

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fintelis.adapter.CustomerAdapter
import com.example.fintelis.databinding.FragmentCustomerListBinding
import com.example.fintelis.viewmodel.CustomerViewModel
import com.example.fintelis.viewmodel.SortOrder

class CustomerListFragment : Fragment() {
    companion object { private const val TAG = "ListFragment_DEBUG" }

    private var _binding: FragmentCustomerListBinding? = null
    private val binding get() = _binding!!
    private val customerViewModel: CustomerViewModel by activityViewModels()
    private lateinit var customerAdapter: CustomerAdapter
    private var isDeleteMode = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCustomerListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupSearch()

        customerViewModel.sortedCustomers.observe(viewLifecycleOwner) { customerList ->
            Log.d(TAG, "--> LiveData OBSERVED! List size is ${customerList.size}. Updating adapter.")
            customerAdapter.updateData(customerList)
        }

        binding.fabAddCustomer.setOnClickListener {
            findNavController().navigate(R.id.action_customerListFragment_to_addCustomerFragment)
        }
    }

    private fun setupToolbar() {
        // KUNCI #1: Jadikan toolbar ini sebagai action bar untuk fragment
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        setHasOptionsMenu(true) // Memberitahu fragment bahwa ia punya menu
    }

    // KUNCI #2: Gunakan callback standar ini untuk MEMBUAT menu
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.customer_list_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    // KUNCI #3: Gunakan callback standar ini untuk MENANGANI klik menu (HANYA SATU INI)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_filter -> {
                showSortDialog()
                true
            }
            R.id.action_delete -> {
                // Jika sedang dalam mode hapus, konfirmasi penghapusan
                if (isDeleteMode && customerAdapter.selectedItems.isNotEmpty()) {
                    showDeleteConfirmationDialog()
                } else {
                    // Jika tidak, masuk/keluar mode hapus
                    toggleDeleteMode()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Callback ini akan dipanggil setiap kali menu digambar ulang
    override fun onPrepareOptionsMenu(menu: Menu) {
        val deleteItem = menu.findItem(R.id.action_delete)
        val filterItem = menu.findItem(R.id.action_filter)

        if (isDeleteMode) {
            binding.toolbar.title = "Select Items"
            filterItem?.isVisible = false
            deleteItem?.setIcon(R.drawable.ic_check_circle)
        } else {
            binding.toolbar.title = "Customer Database"
            filterItem?.isVisible = true
            deleteItem?.setIcon(R.drawable.ic_delete)
        }
        super.onPrepareOptionsMenu(menu)
    }

    private fun toggleDeleteMode() {
        isDeleteMode = !isDeleteMode
        customerAdapter.toggleDeleteMode(isDeleteMode)
        Log.d(TAG, "toggleDeleteMode CALLED. isDeleteMode is now: $isDeleteMode")
        // Perbarui tampilan menu
        activity?.invalidateOptionsMenu()
    }

    private fun showDeleteConfirmationDialog() {
        Log.d(TAG, "showDeleteConfirmationDialog CALLED.")
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Deletion")
            .setMessage("Are you sure you want to delete ${customerAdapter.selectedItems.size} customer(s)?")
            .setPositiveButton("Delete") { dialog, _ ->
                Log.d(TAG, "DELETE button clicked. Calling ViewModel to delete.")
                customerViewModel.deleteCustomers(customerAdapter.selectedItems)
                toggleDeleteMode() // Keluar dari mode hapus setelah konfirmasi
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                Log.d(TAG, "CANCEL button clicked.")
                toggleDeleteMode() // Keluar dari mode hapus jika dibatalkan
                dialog.dismiss()
            }
            .setOnCancelListener { toggleDeleteMode() }
            .show()
    }

    // --- Sisa fungsi (setupSearch, showSortDialog, setupRecyclerView) tetap sama ---
    private fun setupSearch() {
        binding.searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                customerViewModel.searchCustomers(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun showSortDialog() {
        val sortOptions = arrayOf("Default", "Date (Newest)", "Date (Oldest)", "Score (High to Low)", "Score (Low to High)", "Risk Category", "Status")
        AlertDialog.Builder(requireContext()).setTitle("Sort By").setItems(sortOptions) { dialog, which ->
            val selectedOrder = when (which) {
                1 -> SortOrder.DATE_DESC; 2 -> SortOrder.DATE_ASC; 3 -> SortOrder.SCORE_DESC
                4 -> SortOrder.SCORE_ASC; 5 -> SortOrder.RISK_CATEGORY; 6 -> SortOrder.STATUS
                else -> SortOrder.NONE
            }
            customerViewModel.sortCustomers(selectedOrder)
            dialog.dismiss()
        }.show()
    }

    private fun setupRecyclerView() {
        customerAdapter = CustomerAdapter(mutableListOf()) { customer ->
            if (!isDeleteMode) {
                val action = CustomerListFragmentDirections.actionCustomerListFragmentToCustomerDetailFragment(customer)
                findNavController().navigate(action)
            }
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