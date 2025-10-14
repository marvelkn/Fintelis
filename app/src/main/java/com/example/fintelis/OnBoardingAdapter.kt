package com.example.fintelis

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class OnboardingAdapter(
    private val items: List<OnboardItem>,
    private val onNextClicked: (position: Int) -> Unit
) : RecyclerView.Adapter<OnboardingAdapter.OnboardViewHolder>() {

    inner class OnboardViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.imageOnboard)
        val title: TextView = view.findViewById(R.id.textDescription)
        val btnNext: Button = view.findViewById(R.id.btnNext)
        val layoutDots: LinearLayout = view.findViewById(R.id.layoutDots)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_onboarding, parent, false)
        return OnboardViewHolder(view)
    }

    override fun onBindViewHolder(holder: OnboardViewHolder, position: Int) {
        val item = items[position]
        holder.img.setImageResource(item.imageRes)
        holder.title.text = item.title

        holder.btnNext.setOnClickListener {
            onNextClicked(position)
        }
    }

    override fun getItemCount(): Int = items.size
}
