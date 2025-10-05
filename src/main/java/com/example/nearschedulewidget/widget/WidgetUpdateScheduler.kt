package com.example.nearschedulewidget.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object WidgetUpdateScheduler {

    private const val UPDATE_ACTION = "com.example.nearschedulewidget.UPDATE"

    fun scheduleWidgetUpdate(context: Context) {
        val prefs = context.getSharedPreferences("widget_settings", Context.MODE_PRIVATE)
        val intervalMillis = prefs.getLong("update_interval", 15 * 60 * 1000L) // Default to 15 minutes

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, CalendarWidgetProvider::class.java).apply {
            action = UPDATE_ACTION
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Set the new inexact repeating alarm
        alarmManager.setInexactRepeating(
            AlarmManager.RTC,
            System.currentTimeMillis() + intervalMillis,
            intervalMillis,
            pendingIntent
        )
    }

    fun cancelWidgetUpdate(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, CalendarWidgetProvider::class.java).apply {
            action = UPDATE_ACTION
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
