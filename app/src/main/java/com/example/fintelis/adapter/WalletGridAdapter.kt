package com.example.fintelis.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fintelis.data.Wallet
import com.example.fintelis.databinding.ItemWalletCardBinding
import com.example.fintelis.viewmodel.DashboardViewModel

class WalletGridAdapter(
    private var wallets: List<Wallet>,
    private val viewModel: DashboardViewModel,
    private val onWalletClicked: (Wallet) -> Unit
) : RecyclerView.Adapter<WalletGridAdapter.WalletViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalletViewHolder {
        val binding = ItemWalletCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WalletViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WalletViewHolder, position: Int) {
        holder.bind(wallets[position])
    }

    override fun getItemCount() = wallets.size

    fun updateWallets(newWallets: List<Wallet>) {
        wallets = newWallets
        notifyDataSetChanged()
    }

    inner class WalletViewHolder(private val binding: ItemWalletCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(wallet: Wallet) {
            binding.tvWalletName.text = wallet.name
            binding.tvWalletBalance.text = viewModel.formatRupiah(wallet.balance)
            binding.root.setOnClickListener {
                onWalletClicked(wallet)
            }
        }
    }
}