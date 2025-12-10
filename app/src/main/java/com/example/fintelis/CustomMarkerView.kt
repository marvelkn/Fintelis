package com.example.fintelis

import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class CustomMarkerView(context: Context, layoutResource: Int) : MarkerView(context, layoutResource) {

    private val tvContent: TextView = findViewById(R.id.tvContent)
    private val tvDate: TextView = findViewById(R.id.tvDate)
    private val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)

    // Method ini dipanggil setiap kali grafik disentuh
    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e == null) return

        // Format Rupiah
        tvContent.text = formatRp.format(e.y.toDouble())

        // Format Tanggal (ambil dari X axis)
        tvDate.text = dateFormat.format(Date(e.x.toLong()))

        super.refreshContent(e, highlight)
    }

    // Posisi marker agar tepat di atas titik jari
    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat() - 20f)
    }
}