package com.example.fintelis

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.example.fintelis.databinding.FragmentCreditResultBinding
import java.nio.FloatBuffer
import java.util.Collections

class CreditResultFragment : Fragment() {

    private var _binding: FragmentCreditResultBinding? = null
    private val binding get() = _binding!!

    // Variabel ONNX
    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreditResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Inisialisasi Model
        initModel()

        // 2. Tombol Prediksi
        binding.btnPredict.setOnClickListener {
            predict()
        }
    }

    private fun initModel() {
        try {
            ortEnv = OrtEnvironment.getEnvironment()
            // Pastikan nama file .onnx benar di folder assets
            ortSession = ortEnv?.createSession(requireContext().assets.open("model_best20_final.onnx").readBytes())
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Gagal memuat model: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun predict() {
        if (ortSession == null) {
            Toast.makeText(requireContext(), "Model AI belum siap.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // --- BAGIAN A: AMBIL DATA (SAFE GETTERS) ---

            // Helper: Ambil angka dari EditText
            fun getVal(text: android.text.Editable?): Float {
                return text.toString().toFloatOrNull() ?: 0.0f
            }

            // Helper: Ambil boolean dari Checkbox
            fun getBool(isChecked: Boolean): Float {
                return if (isChecked) 1.0f else 0.0f
            }

            // 1. GENDER (KOREKSI: Kembali menggunakan CheckBox cbIsFemale)
            // Jika CheckBox dicentang (Wanita) = 1.0, Jika tidak (Pria) = 0.0
            val valGender = if (binding.cbIsFemale.isChecked) 1.0f else 0.0f

            // 2. INPUT UTAMA & NORMALISASI
            val valAnnuity = normalize(getVal(binding.etAnnuity.text), 1000.0f, 250000.0f)
            val valDebt    = normalize(getVal(binding.etDebt.text), 0.0f, 50000000.0f)
            val valRating  = normalize(getVal(binding.etRegionRating.text), 1.0f, 3.0f)
            val valDays    = normalize(getVal(binding.etDaysDrawing.text), 0.0f, 4000.0f)

            // 3. INPUT TEKNIS (Hidden Inputs)
            val valExt3    = getVal(binding.etExtSource3.text)
            val valExt2    = getVal(binding.etExtSource2.text)
            val valExt1    = getVal(binding.etExtSource1.text)
            val valYears   = getVal(binding.etYearsBuild.text)
            val valEntrances = getVal(binding.etEntrances.text)


            // --- BAGIAN B: SUSUN ARRAY 20 FITUR (WAJIB URUT) ---
            val inputData = floatArrayOf(
                valExt3,                        // 1. EXT_SOURCE_3
                valExt2,                        // 2. EXT_SOURCE_2
                valExt1,                        // 3. EXT_SOURCE_1
                valGender,                      // 4. GENDER (Sekarang Benar)
                valAnnuity,                     // 5. ANNUITY
                getBool(binding.cbEducationSec.isChecked), // 6. EDUCATION
                getBool(binding.cbNoCar.isChecked),        // 7. NO CAR
                getBool(binding.cbIsMarried.isChecked),    // 8. MARRIED
                valDebt,                        // 9. DEBT
                getBool(binding.cbCashLoan.isChecked),     // 10. CASH LOAN
                getBool(binding.cbDoc3.isChecked),         // 11. DOC 3
                getBool(binding.cbGoodsXNA.isChecked),     // 12. GOODS XNA
                getBool(binding.cbWorking.isChecked),      // 13. WORKING
                getBool(binding.cbRepeater.isChecked),     // 14. REPEATER
                getBool(binding.cbEmpPhone.isChecked),     // 15. PHONE
                valRating,                      // 16. RATING
                valDays,                        // 17. DAYS DRAWING
                getBool(binding.cbRejectXAP.isChecked),    // 18. REJECT XAP
                valYears,                       // 19. YEARS BUILD
                valEntrances                    // 20. ENTRANCES
            )

            // --- BAGIAN C: RUN MODEL ONNX ---
            val inputName = ortSession?.inputNames?.iterator()?.next() ?: "float_input"
            val floatBuffer = FloatBuffer.wrap(inputData)
            val inputTensor = OnnxTensor.createTensor(ortEnv, floatBuffer, longArrayOf(1, 20))

            val results = ortSession?.run(Collections.singletonMap(inputName, inputTensor))
            val labelResult = results?.get(0)?.value as LongArray
            val prediction = labelResult[0] // 0 atau 1

            // --- BAGIAN D: TAMPILKAN HASIL ---

            if (prediction == 0L) {
                // HASIL: DITERIMA (HIJAU)
                binding.tvResult.text = "✅ Approved\nLow Risk Profile"
                binding.tvResult.setTextColor(Color.parseColor("#1B5E20")) // Hijau Tua
                binding.cardResult.setCardBackgroundColor(Color.parseColor("#F0FFF4"))
            } else {
                // HASIL: DITOLAK (MERAH)
                binding.tvResult.text = "⚠️ High Risk Detected\nReview Required"
                binding.tvResult.setTextColor(Color.parseColor("#C53030")) // Merah
                binding.cardResult.setCardBackgroundColor(Color.parseColor("#FFF5F5"))
            }

            inputTensor.close()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Terjadi kesalahan: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Fungsi Normalisasi
    private fun normalize(value: Float, min: Float, max: Float): Float {
        if (max == min) return 0.0f
        val result = (value - min) / (max - min)
        return when {
            result < 0.0f -> 0.0f
            result > 1.0f -> 1.0f
            else -> result
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}