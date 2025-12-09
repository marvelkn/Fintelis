package com.example.fintelis.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.fintelis.R
import com.example.fintelis.data.Transaction
import com.example.fintelis.data.TransactionType
import com.example.fintelis.databinding.ItemTransactionBinding
import java.text.NumberFormat
import java.util.Locale

class TransactionAdapter(
    private var list: MutableList<Transaction>,
    private val onClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    private var isDeleteMode = false
    val selectedItems = mutableSetOf<Transaction>()

    fun toggleDeleteMode(active: Boolean) {
        isDeleteMode = active
        if (!active) selectedItems.clear()
        notifyDataSetChanged()
    }

    fun updateData(newList: List<Transaction>) {
        list.clear(); list.addAll(newList); notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Transaction) {
            val context = itemView.context
            binding.tvTitle.text = item.title
            binding.tvDateCat.text = "${item.date} • ${item.category}"

            val fmt = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
            binding.tvAmount.text = fmt.format(item.amount)

            val (amountColor, iconRes, iconBgRes) = if (item.type == TransactionType.INCOME) {
                Triple(R.color.status_approved, R.drawable.ic_income, R.drawable.bg_icon_income)
            } else {
                Triple(R.color.status_rejected, R.drawable.ic_expense, R.drawable.bg_icon_expense)
            }

            binding.tvAmount.setTextColor(ContextCompat.getColor(context, amountColor))
            binding.ivIcon.setImageResource(iconRes)
            binding.ivIcon.background = ContextCompat.getDrawable(context, iconBgRes)

            // Set icon tint to match the amount color
            binding.ivIcon.setColorFilter(ContextCompat.getColor(context, amountColor))

            binding.checkboxDelete.setOnCheckedChangeListener(null)
            binding.checkboxDelete.isVisible = isDeleteMode
            binding.checkboxDelete.isChecked = selectedItems.contains(item)

            val params = binding.contentLayout.layoutParams as ViewGroup.MarginLayoutParams
            params.marginStart = if (isDeleteMode) 16 else 0
            binding.contentLayout.layoutParams = params

            binding.checkboxDelete.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedItems.add(item) else selectedItems.remove(item)
            }
            itemView.setOnClickListener {
                if (isDeleteMode) {
                    binding.checkboxDelete.isChecked = !binding.checkboxDelete.isChecked
                } else {
                    onClick(item)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(list[position])

    override fun getItemCount() = list.size
}