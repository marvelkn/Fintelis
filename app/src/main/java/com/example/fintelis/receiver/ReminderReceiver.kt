package com.example.fintelis.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.fintelis.MainActivity
import com.example.fintelis.R
import com.example.fintelis.utils.NotificationScheduler // Import Scheduler dari utils
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // 1. Tampilkan Notifikasi
        showNotification(context)

        // 2. [PENTING] Jadwalkan Ulang untuk Besok
        // Karena setExact hanya bunyi sekali, kita harus set lagi setelah bunyi.
        if (NotificationScheduler.isReminderEnabled(context)) {
            NotificationScheduler.scheduleDailyReminder(context)
        }
    }

    private fun showNotification(context: Context) {
        val channelId = "daily_reminder_channel"
        val channelName = "Daily Financial Reminder"

        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val title: String
        val message: String
        val notifId: Int

        // Logika Pesan Siang vs Malam
        if (currentHour < 16) {
            title = "Afternoon Financial Reminder ☀️"
            message = "Have you had lunch? Don't forget to record your expenses!"
            notifId = 2001
        } else {
            title = "Evening Financial Reminder 🌙"
            message = "The day is almost over, Let's recap today's transasctions before bed."
            notifId = 2002
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(notifId, notification)
    }
}