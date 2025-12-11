package com.example.fintelis

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fintelis.adapter.TransactionAdapter
import com.example.fintelis.adapter.WalletAdapter
import com.example.fintelis.databinding.DialogDisplayOptionsBinding
import com.example.fintelis.databinding.FragmentTransactionListBinding
import com.example.fintelis.viewmodel.FilterType
import com.example.fintelis.viewmodel.SortOrder
import com.example.fintelis.viewmodel.TransactionViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionListFragment : Fragment() {
    private var _binding: FragmentTransactionListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TransactionViewModel by activityViewModels()
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var walletAdapter: WalletAdapter
    private var isDeleteMode = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTransactionListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        setupMenu()
        setupAdapters()

        // Observers
        viewModel.wallets.observe(viewLifecycleOwner) { wallets ->
            walletAdapter.updateWallets(wallets ?: emptyList())
        }
        viewModel.displayedTransactions.observe(viewLifecycleOwner) { transactionAdapter.updateData(it) }
        viewModel.currentMonth.observe(viewLifecycleOwner) { calendar ->
            val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
            binding.tvCurrentMonth.text = monthFormat.format(calendar.time)
        }

        val fmt = NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("in").setRegion("ID").build())
        viewModel.income.observe(viewLifecycleOwner) { binding.tvSummaryIncome.text = fmt.format(it) }
        viewModel.expense.observe(viewLifecycleOwner) { binding.tvSummaryExpense.text = fmt.format(it) }
        viewModel.total.observe(viewLifecycleOwner) { binding.tvSummaryTotal.text = fmt.format(it) }

        // Click Listeners
        binding.fabAddTransaction.setOnClickListener {
            if (viewModel.activeWalletId.value == null || viewModel.activeWalletId.value == "ALL") {
                Toast.makeText(context, "Please select a specific wallet to add a transaction", Toast.LENGTH_SHORT).show()
            } else {
                val action = TransactionListFragmentDirections.actionTransactionListFragmentToAddTransactionFragment()
                findNavController().navigate(action)
            }
        }
        binding.btnPrevMonth.setOnClickListener { viewModel.changeMonth(-1) }
        binding.btnNextMonth.setOnClickListener { viewModel.changeMonth(1) }

        binding.searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { viewModel.searchTransactions(s.toString()) }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupAdapters() {
        // Transaction list adapter
        transactionAdapter = TransactionAdapter(mutableListOf()) {
            if (!isDeleteMode) {
                val action = TransactionListFragmentDirections.actionTransactionListFragmentToTransactionDetailFragment(it)
                findNavController().navigate(action)
            }
        }
        binding.recyclerViewTransactions.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = transactionAdapter
        }

        // Wallet slider adapter
        walletAdapter = WalletAdapter(requireContext(), emptyList()) { wallet ->
            viewModel.setActiveWallet(wallet?.id)
        }
        binding.rvWallets.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = walletAdapter
        }
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.transaction_list_menu, menu)
            }

            override fun onPrepareMenu(menu: Menu) {
                val deleteItem = menu.findItem(R.id.action_delete)
                val filterItem = menu.findItem(R.id.action_filter)

                deleteItem.setIcon(if (isDeleteMode) R.drawable.ic_check_circle else R.drawable.ic_delete)
                filterItem.isVisible = !isDeleteMode
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_filter -> { showDisplayOptionsDialog(); true }
                    R.id.action_delete -> {
                        if (!isDeleteMode) {
                            isDeleteMode = true
                            transactionAdapter.toggleDeleteMode(true)
                            requireActivity().invalidateOptionsMenu()
                        } else {
                            if (transactionAdapter.selectedItems.isNotEmpty()) {
                                showDeleteConfirmationDialog()
                            } else {
                                isDeleteMode = false
                                transactionAdapter.toggleDeleteMode(false)
                                requireActivity().invalidateOptionsMenu()
                            }
                        }
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun showDisplayOptionsDialog() {
        val dialogBinding = DialogDisplayOptionsBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Display Options")
            .setView(dialogBinding.root)
            .setPositiveButton("Apply") { _, _ ->
                val newFilter = when (dialogBinding.rgFilter.checkedRadioButtonId) {
                    R.id.rb_filter_income -> FilterType.INCOME
                    R.id.rb_filter_expense -> FilterType.EXPENSE
                    else -> FilterType.ALL
                }
                val newSort = when (dialogBinding.rgSort.checkedRadioButtonId) {
                    R.id.rb_sort_date_asc -> SortOrder.DATE_ASC
                    R.id.rb_sort_amount_desc -> SortOrder.AMOUNT_DESC
                    R.id.rb_sort_amount_asc -> SortOrder.AMOUNT_ASC
                    else -> SortOrder.DATE_DESC
                }
                viewModel.setDisplayOptions(newSort, newFilter)
            }
            .setNegativeButton("Cancel", null)
            .create()

        when (viewModel.currentFilterType) {
            FilterType.INCOME -> dialogBinding.rbFilterIncome.isChecked = true
            FilterType.EXPENSE -> dialogBinding.rbFilterExpense.isChecked = true
            else -> dialogBinding.rbFilterAll.isChecked = true
        }
        when (viewModel.currentSortOrder) {
            SortOrder.DATE_ASC -> dialogBinding.rbSortDateAsc.isChecked = true
            SortOrder.AMOUNT_DESC -> dialogBinding.rbSortAmountDesc.isChecked = true
            SortOrder.AMOUNT_ASC -> dialogBinding.rbSortAmountAsc.isChecked = true
            else -> dialogBinding.rbSortDateDesc.isChecked = true
        }

        dialog.show()
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext()).setTitle("Confirm Delete")
            .setMessage("Are you sure you want to delete the selected items?")
            .setPositiveButton("Yes") { _, _ ->
                viewModel.deleteTransactions(transactionAdapter.selectedItems.toSet())
                isDeleteMode = false
                transactionAdapter.toggleDeleteMode(false)
                requireActivity().invalidateOptionsMenu()
            }
            .setNegativeButton("No", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}