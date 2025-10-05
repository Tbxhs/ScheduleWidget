package com.example.nearschedulewidget.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.example.nearschedulewidget.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CalendarWidgetProvider: AppWidgetProvider() {

    private val TAG = "CalendarWidgetProvider"
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.d(TAG, "onEnabled: First widget placed, scheduling updates.")
        WidgetUpdateScheduler.scheduleWidgetUpdate(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Log.d(TAG, "onUpdate: Updating widgets.")
        for (appWidgetId in appWidgetIds) {
            CalendarWidgetUpdater.updateNow(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.d(TAG, "onDisabled: Last widget removed, cancelling updates.")
        WidgetUpdateScheduler.cancelWidgetUpdate(context)
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle?) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        Log.d(TAG, "onAppWidgetOptionsChanged: Options changed, updating widget.")
        CalendarWidgetUpdater.updateNow(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "onReceive: action = $action")

        if (CalendarWidgetUpdater.UPDATE_ACTION == action) {
            val appWidgetId = intent.data?.lastPathSegment?.toIntOrNull()
            if (appWidgetId != null) {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                // Temporarily hide the refresh button to indicate that a refresh is in progress.
                val remoteViews = RemoteViews(context.packageName, R.layout.widget_calendar).apply {
                    setViewVisibility(R.id.btn_refresh, View.GONE)
                }
                appWidgetManager.partiallyUpdateAppWidget(appWidgetId, remoteViews)

                val pendingResult = goAsync()
                coroutineScope.launch {
                    try {
                        CalendarWidgetUpdater.updateNow(context, appWidgetManager, appWidgetId)
                    } finally {
                        pendingResult.finish()
                    }
                }
                return // We handled this broadcast, no need for super
            }
        }

        super.onReceive(context, intent) // For all other actions, let the default implementation handle it.

        if (action == Intent.ACTION_CONFIGURATION_CHANGED) {
            Log.d(TAG, "Theme changed. Triggering widget update for all widgets.")
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, CalendarWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }
}
