package com.example.fintelis.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.fintelis.R
import com.example.fintelis.data.Customer
import com.example.fintelis.data.Status
import com.example.fintelis.databinding.ItemCustomerBinding

class CustomerAdapter(
    // Ubah menjadi var agar bisa di-update
    private var customerList: MutableList<Customer>,
    private val onCustomerClicked: (Customer) -> Unit
) : RecyclerView.Adapter<CustomerAdapter.CustomerViewHolder>() {

    // FUNGSI BARU UNTUK UPDATE DATA
    fun updateData(newCustomerList: List<Customer>) {
        customerList.clear()
        customerList.addAll(newCustomerList)
        notifyDataSetChanged() // Memberitahu RecyclerView untuk refresh
    }

    inner class CustomerViewHolder(private val binding: ItemCustomerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(customer: Customer) {
            binding.tvCustomerName.text = customer.name
            binding.tvCustomerId.text = customer.id
            binding.tvSubmissionDate.text = customer.submissionDate
            binding.tvStatus.text = customer.status.name

            val statusColorRes = when (customer.status) {
                Status.APPROVED -> R.color.status_approved
                Status.REJECTED -> R.color.status_rejected
                Status.PENDING -> R.color.status_pending
            }
            binding.tvStatus.setTextColor(ContextCompat.getColor(itemView.context, statusColorRes))

            itemView.setOnClickListener { onCustomerClicked(customer) }
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

