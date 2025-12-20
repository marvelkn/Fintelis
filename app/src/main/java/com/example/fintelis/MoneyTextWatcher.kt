package com.example.fintelis

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.text.NumberFormat
import java.util.Locale

class MoneyTextWatcher(private val editText: EditText) : TextWatcher {
    private var current = ""

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(s: Editable?) {
        // Mencegah infinite loop (perulangan tak terbatas)
        if (s.toString() != current) {
            editText.removeTextChangedListener(this)

            // 1. Hapus semua karakter yang bukan angka (titik, simbol, dll)
            val cleanString = s.toString().replace("[\\D]".toRegex(), "")

            if (cleanString.isNotEmpty()) {
                try {
                    val parsed = cleanString.toDouble()
                    // 2. Format ulang ke format angka Indonesia (titik ribuan)
                    val formatted = NumberFormat.getNumberInstance(Locale("id", "ID")).format(parsed)

                    current = formatted
                    editText.setText(formatted)
                    editText.setSelection(formatted.length) // Kursor tetap di posisi paling kanan
                } catch (e: Exception) { }
            }

            editText.addTextChangedListener(this)
        }
    }
}