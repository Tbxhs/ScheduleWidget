package com.example.nearschedulewidget.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Reschedule the widget update alarm when the device boots up
            WidgetUpdateScheduler.scheduleWidgetUpdate(context)
        }
    }
}
