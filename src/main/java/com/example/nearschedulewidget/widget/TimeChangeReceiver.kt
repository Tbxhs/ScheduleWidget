
package com.example.nearschedulewidget.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent

class TimeChangeReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, CalendarWidgetProvider::class.java))
        for (appWidgetId in appWidgetIds) {
            CalendarWidgetUpdater.updateNow(context, appWidgetManager, appWidgetId)
        }
    }
}
