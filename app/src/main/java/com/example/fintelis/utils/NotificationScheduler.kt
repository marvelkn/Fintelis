package com.example.fintelis.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.fintelis.receiver.ReminderReceiver // Import Receiver
import java.util.Calendar

object NotificationScheduler {

    private const val ID_SIANG = 1001
    private const val ID_MALAM = 1002

    private const val PREF_NAME = "reminder_pref"
    private const val KEY_IS_ENABLED = "is_enabled"

    // --- BAGIAN 1: STATUS PENYIMPANAN ---

    fun setReminderStatus(context: Context, isEnabled: Boolean) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_ENABLED, isEnabled).apply()

        if (isEnabled) {
            scheduleDailyReminder(context)
        } else {
            cancelReminder(context)
        }
    }

    fun isReminderEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_ENABLED, false)
    }

    // --- BAGIAN 2: LOGIKA ALARM ---

    fun scheduleDailyReminder(context: Context) {
        // SET WAKTU DISINI (Format 24 Jam)
        // Jam 1 Siang
        scheduleAlarm(context, 13, 4, ID_SIANG)
        // Jam 7 Malam
        scheduleAlarm(context, 19, 0, ID_MALAM)
    }

    private fun scheduleAlarm(context: Context, hour: Int, minute: Int, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Jika waktu sudah lewat, jadwalkan untuk BESOK
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        try {
            // Gunakan setExactAndAllowWhileIdle agar alarm bunyi TEPAT WAKTU (Agresif)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun cancelReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)

        val pendingIntentSiang = PendingIntent.getBroadcast(
            context, ID_SIANG, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntentSiang)

        val pendingIntentMalam = PendingIntent.getBroadcast(
            context, ID_MALAM, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntentMalam)
    }
}