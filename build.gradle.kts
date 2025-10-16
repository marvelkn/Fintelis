plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    // Daftarkan plugin Safe Args di sini menggunakan alias baru
    alias(libs.plugins.kotlinSafeargs) apply false
}
