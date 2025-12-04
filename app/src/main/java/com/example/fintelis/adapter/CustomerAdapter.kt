package com.example.fintelis.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.fintelis.R
import com.example.fintelis.data.Customer
import com.example.fintelis.data.RiskCategory
import com.example.fintelis.data.Status
import com.example.fintelis.databinding.ItemCustomerBinding

class CustomerAdapter(
    private var customerList: MutableList<Customer>,
    private val onCustomerClicked: (Customer) -> Unit
) : RecyclerView.Adapter<CustomerAdapter.CustomerViewHolder>() {

    private var isDeleteMode = false
    val selectedItems = mutableSetOf<Customer>()

    fun toggleDeleteMode(isActive: Boolean) {
        isDeleteMode = isActive
        if (!isActive) {
            selectedItems.clear()
        }
        notifyDataSetChanged()
    }

    fun updateData(newCustomerList: List<Customer>) {
        customerList.clear()
        customerList.addAll(newCustomerList)
        notifyDataSetChanged()
    }

    inner class CustomerViewHolder(private val binding: ItemCustomerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(customer: Customer) {
            binding.tvCustomerName.text = customer.name
            binding.tvCustomerId.text = customer.id
            binding.tvStatus.text = customer.status.name

            // Mengisi data yang sebelumnya error
            binding.tvSubmissionDate.text = customer.submissionDate
            binding.tvCreditScore.text = customer.creditScore.toString()
            binding.tvRiskCategory.text = customer.riskCategory.name

            // Logika pewarnaan
            val statusColorRes = when (customer.status) {
                Status.APPROVED -> R.color.status_approved
                Status.REJECTED -> R.color.status_rejected
                Status.PENDING -> R.color.status_pending
            }
            binding.tvStatus.setTextColor(ContextCompat.getColor(itemView.context, statusColorRes))

            val riskColorRes = when (customer.riskCategory) {
                RiskCategory.LOW -> R.color.status_approved
                RiskCategory.MEDIUM -> R.color.status_pending
                RiskCategory.HIGH -> R.color.status_rejected
            }
            binding.tvRiskCategory.setTextColor(ContextCompat.getColor(itemView.context, riskColorRes))

            // Logika untuk CheckBox
            binding.checkboxDelete.isVisible = isDeleteMode
            binding.checkboxDelete.isChecked = selectedItems.contains(customer)

            itemView.setOnClickListener {
                if (isDeleteMode) {
                    binding.checkboxDelete.isChecked = !binding.checkboxDelete.isChecked
                } else {
                    onCustomerClicked(customer)
                }
            }

            binding.checkboxDelete.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedItems.add(customer)
                } else {
                    selectedItems.remove(customer)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerViewHolder {
        val binding = ItemCustomerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CustomerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CustomerViewHolder, position: Int) {
        holder.bind(customerList[position])
    }

    override fun getItemCount() = customerList.size
}