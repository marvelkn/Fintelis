package com.example.fintelis.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.fintelis.R
import com.example.fintelis.data.Wallet
import com.example.fintelis.databinding.ItemWalletChipBinding

class WalletAdapter(
    private val context: Context,
    private var wallets: List<Wallet>,
    private val onWalletSelected: (Wallet?) -> Unit
) : RecyclerView.Adapter<WalletAdapter.WalletViewHolder>() {

    private var selectedPosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalletViewHolder {
        val binding = ItemWalletChipBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WalletViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WalletViewHolder, position: Int) {
        val wallet = wallets[position]
        holder.bind(wallet, position == selectedPosition)
    }

    override fun getItemCount() = wallets.size

    fun updateWallets(newWallets: List<Wallet>) {
        wallets = listOf(Wallet(id = "ALL", name = "All Wallets")) + newWallets
        notifyDataSetChanged()
    }

    inner class WalletViewHolder(private val binding: ItemWalletChipBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(wallet: Wallet, isSelected: Boolean) {
            binding.tvWalletName.text = wallet.name

            if (isSelected) {
                binding.root.strokeWidth = 2
                binding.root.setCardBackgroundColor(ContextCompat.getColor(context, R.color.fintelis_primary_light))
            } else {
                binding.root.strokeWidth = 0
                binding.root.setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
            }

            binding.root.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = adapterPosition
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)

                if (wallet.id == "ALL") {
                    onWalletSelected(null) // Pass null for "All Wallets"
                } else {
                    onWalletSelected(wallet)
                }
            }
        }
    }
}