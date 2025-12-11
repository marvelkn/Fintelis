package com.example.fintelis.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.fintelis.utils.NotificationScheduler

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Cek dulu, apakah user sebelumnya mengaktifkan reminder?
            if (NotificationScheduler.isReminderEnabled(context)) {
                // Jika ya, jadwalkan ulang alarmnya
                NotificationScheduler.scheduleDailyReminder(context)
            }
        }
    }
}