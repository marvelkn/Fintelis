package com.example.fintelis

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.fintelis.data.Transaction
import com.example.fintelis.data.TransactionType
import com.example.fintelis.databinding.FragmentAddTransactionBinding
import com.example.fintelis.viewmodel.TransactionViewModel
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AddTransactionFragment : Fragment() {
    private var _binding: FragmentAddTransactionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TransactionViewModel by activityViewModels()
    private var imageUri: Uri? = null
    private lateinit var currentPhotoPath: String

    // --- Permissions & Launchers ---
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) openCamera()
        else Toast.makeText(context, "Camera permission is required.", Toast.LENGTH_SHORT).show()
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val file = File(currentPhotoPath)
            imageUri = Uri.fromFile(file)
            showImagePreview()
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            imageUri = result.data?.data
            showImagePreview()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Setup Toolbar
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        // 2. Setup Default State (Expense Mode)
        updateCategoryList(isExpense = true)

        // 3. Listener Perubahan Tipe (Expense/Income)
        binding.rgType.setOnCheckedChangeListener { _, checkedId ->
            val isExpense = checkedId == R.id.rb_expense

            if (isExpense) {
                // Mode Expense: Text Putih, Income Abu-abu
                binding.rbExpense.setTextColor(Color.WHITE)
                binding.rbIncome.setTextColor(Color.parseColor("#64748B"))
                updateCategoryList(true)
            } else {
                // Mode Income: Text Putih, Expense Abu-abu
                binding.rbExpense.setTextColor(Color.parseColor("#64748B"))
                binding.rbIncome.setTextColor(Color.WHITE)
                updateCategoryList(false)
            }
        }

        // 4. Listener Upload & Hapus Gambar
        binding.layoutPlaceholder.setOnClickListener { showImageSourceDialog() }
        binding.btnRemoveImage.setOnClickListener { removeImage() }

        // 5. Listener Save
        binding.btnSave.setOnClickListener { saveTransaction() }
    }

    private fun updateCategoryList(isExpense: Boolean) {
        val arrayResId = if (isExpense) R.array.expense_categories else R.array.income_categories

        // Pastikan array ini ada di strings.xml
        try {
            val categories = resources.getStringArray(arrayResId)
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
            binding.etCategory.setAdapter(adapter)
            binding.etCategory.setText("") // Reset text saat ganti tipe
        } catch (e: Exception) {
            // Fallback jika array belum dibuat
        }
    }

    // --- LOGIKA GAMBAR ---
    private fun showImageSourceDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        AlertDialog.Builder(requireContext())
            .setTitle("Add Receipt Image")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> checkCameraPermissionAndOpenCamera()
                    1 -> openGallery()
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun checkCameraPermissionAndOpenCamera() {
        if (requireContext().checkSelfPermission(Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File? = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun openCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(requireActivity().packageManager)?.also {
                val photoFile: File? = try { createImageFile() } catch (ex: IOException) { null }
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(requireContext(), "com.example.fintelis.fileprovider", it)
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    cameraLauncher.launch(takePictureIntent)
                }
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun showImagePreview() {
        imageUri?.let {
            binding.layoutPlaceholder.isVisible = false
            binding.ivReceiptPreview.isVisible = true
            binding.btnRemoveImage.isVisible = true
            Glide.with(this).load(it).into(binding.ivReceiptPreview)
        }
    }

    private fun removeImage() {
        imageUri = null
        binding.ivReceiptPreview.setImageDrawable(null)
        binding.ivReceiptPreview.isVisible = false
        binding.btnRemoveImage.isVisible = false
        binding.layoutPlaceholder.isVisible = true
    }

    // --- LOGIKA SIMPAN ---
    private fun saveTransaction() {
        val title = binding.etTitle.text.toString().trim()
        val amountStr = binding.etAmount.text.toString().trim()
        val category = binding.etCategory.text.toString().trim()

        if (title.isBlank() || amountStr.isBlank() || category.isBlank()) {
            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSave.isEnabled = false // Disable tombol saat proses

        var localImageUrl: String? = null
        if (imageUri != null) {
            try {
                localImageUrl = saveImageToInternalStorage(imageUri!!)
            } catch (e: IOException) {
                Toast.makeText(context, "Failed to save image.", Toast.LENGTH_SHORT).show()
                binding.btnSave.isEnabled = true
                return
            }
        }

        val type = if (binding.rbIncome.isChecked) TransactionType.INCOME else TransactionType.EXPENSE
        val date = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date())

        val transaction = Transaction(
            title = title,
            amount = amountStr.toDouble(),
            type = type,
            date = date,
            category = category,
            walletId = viewModel.activeWalletId.value ?: "",
            imageUrl = localImageUrl
        )

        viewModel.addTransaction(transaction)
        Toast.makeText(context, "Transaction Saved", Toast.LENGTH_SHORT).show()
        findNavController().popBackStack()
    }

    @Throws(IOException::class)
    private fun saveImageToInternalStorage(uri: Uri): String {
        val inputStream = requireContext().contentResolver.openInputStream(uri)!!
        val receiptsDir = File(requireContext().filesDir, "receipts")
        if (!receiptsDir.exists()) receiptsDir.mkdirs()

        val destinationFile = File(receiptsDir, "${UUID.randomUUID()}.jpg")
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val outputStream = FileOutputStream(destinationFile)

        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
        outputStream.flush()
        outputStream.close()
        inputStream.close()

        return Uri.fromFile(destinationFile).toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}