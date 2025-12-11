package com.example.fintelis.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.fintelis.data.User
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
// --- PERBAIKAN IMPORT ---
import com.google.firebase.firestore.FirebaseFirestore // Gunakan ini
import com.google.firebase.firestore.SetOptions
// ------------------------

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()

    // --- PERBAIKAN INISIALISASI ---
    private val db = FirebaseFirestore.getInstance()

    // --- LiveData untuk UI ---
    private val _userProfile = MutableLiveData<User>()
    val userProfile: LiveData<User> = _userProfile

    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> = _statusMessage

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        loadUserProfile()
    }

    // 1. LOAD DATA PROFILE
    fun loadUserProfile() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        _isLoading.value = true

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = document.toObject(User::class.java)
                    // Pastikan email yang ditampilkan adalah yang asli dari sistem Auth
                    val finalUser = user?.copy(
                        id = userId,
                        email = currentUser.email ?: ""
                    )
                    _userProfile.value = finalUser!!
                } else {
                    val newUser = User(
                        id = userId,
                        fullName = currentUser.displayName ?: "",
                        email = currentUser.email ?: "",
                        phoneNumber = currentUser.phoneNumber ?: ""
                    )
                    _userProfile.value = newUser
                }
                _isLoading.value = false
            }
            .addOnFailureListener {
                _statusMessage.value = "Gagal memuat profil: ${it.message}"
                _isLoading.value = false
            }
    }

    // 2. UPDATE PROFILE (Hanya Nama dan No. HP)
    // Variabel 'email' dari input form DIIKUTKAN, tetapi HANYA UNTUK SINKRONISASI
    fun updateProfile(fullName: String, phone: String, email: String) {
        val user = auth.currentUser ?: return
        val userId = user.uid

        _isLoading.value = true

        // A. Update Display Name di Auth
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(fullName)
            .build()

        user.updateProfile(profileUpdates).addOnCompleteListener { task ->
            if (task.isSuccessful) {

                // KITA HAPUS SEMUA LOGIKA UPDATE EMAIL.
                // Email di Firestore DITETAPKAN menggunakan email dari Auth user saat ini.
                val userData = hashMapOf(
                    "fullName" to fullName,
                    "phoneNumber" to phone,
                    "email" to user.email // Ambil email dari Auth, BUKAN dari parameter input
                )

                db.collection("users").document(userId)
                    .set(userData, SetOptions.merge())
                    .addOnSuccessListener {
                        // Profil (Nama & HP) berhasil diperbarui!
                        _statusMessage.value = "Profil berhasil diperbarui!"
                        _isLoading.value = false
                    }
                    .addOnFailureListener {
                        _statusMessage.value = "Gagal simpan DB: ${it.message}"
                        _isLoading.value = false
                    }
            } else {
                _statusMessage.value = "Gagal update Auth: ${task.exception?.message}"
                _isLoading.value = false
            }
        }
    }

    // 3. CHANGE PASSWORD
    fun changePassword(currentPass: String, newPass: String) {
        val user = auth.currentUser ?: return
        if (user.email == null) return

        _isLoading.value = true

        val credential = EmailAuthProvider.getCredential(user.email!!, currentPass)

        user.reauthenticate(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                user.updatePassword(newPass).addOnCompleteListener { updateTask ->
                    if (updateTask.isSuccessful) {
                        _statusMessage.value = "Password diganti!"
                    } else {
                        _statusMessage.value = "Gagal: ${updateTask.exception?.message}"
                    }
                    _isLoading.value = false
                }
            } else {
                _statusMessage.value = "Password lama salah!"
                _isLoading.value = false
            }
        }
    }
}