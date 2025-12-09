plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    // Daftarkan plugin Safe Args di sini menggunakan alias baru
    alias(libs.plugins.kotlinSafeargs) apply false
    // Add the dependency for the Google services Gradle plugin
    id("com.google.gms.google-services") version "4.4.4" apply false

}
