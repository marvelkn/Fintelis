package com.example.fintelis.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.fintelis.R
import com.example.fintelis.data.Customer
import com.example.fintelis.data.Status
import com.example.fintelis.databinding.ItemCustomerBinding
import com.example.fintelis.data.RiskCategory

class CustomerAdapter(
    private var customerList: MutableList<Customer>,
    private val onCustomerClicked: (Customer) -> Unit
) : RecyclerView.Adapter<CustomerAdapter.CustomerViewHolder>() {

    fun updateData(newCustomerList: List<Customer>) {
        customerList.clear()
        customerList.addAll(newCustomerList)
        notifyDataSetChanged()
    }

    inner class CustomerViewHolder(private val binding: ItemCustomerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(customer: Customer) {
            // Kode yang sudah ada (biarkan)
            binding.tvCustomerName.text = customer.name
            binding.tvCustomerId.text = customer.id
            binding.tvSubmissionDate.text = customer.submissionDate
            binding.tvCreditScore.text = customer.creditScore.toString()
            binding.tvStatus.text = customer.status.name

            // Mengatur warna status (biarkan)
            val statusColorRes = when (customer.status) {
                Status.APPROVED -> R.color.status_approved
                Status.REJECTED -> R.color.status_rejected
                Status.PENDING -> R.color.status_pending
            }
            binding.tvStatus.setTextColor(ContextCompat.getColor(itemView.context, statusColorRes))

            // ==========================================================
            // PENAMBAHAN KODE UNTUK RISK CATEGORY
            // ==========================================================
            binding.tvRiskCategory.text = customer.riskCategory.name

            // Tentukan warna berdasarkan Risk Category
            val riskColorRes = when (customer.riskCategory) {
                RiskCategory.LOW -> R.color.status_approved  // Hijau
                RiskCategory.MEDIUM -> R.color.status_pending // Oranye
                RiskCategory.HIGH -> R.color.status_rejected   // Merah
            }
            // Terapkan warna ke TextView
            binding.tvRiskCategory.setTextColor(ContextCompat.getColor(itemView.context, riskColorRes))
            // ==========================================================
            // AKHIR DARI PENAMBAHAN
            // ==========================================================

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