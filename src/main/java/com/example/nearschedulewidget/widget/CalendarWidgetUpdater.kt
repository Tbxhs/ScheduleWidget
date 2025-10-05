package com.example.nearschedulewidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.pm.PackageManager
import com.example.nearschedulewidget.R
import com.example.nearschedulewidget.ScheduleEvent
import com.example.nearschedulewidget.TimeWindowCalculator
import com.example.nearschedulewidget.data.CalendarRepo
import com.example.nearschedulewidget.render.CalendarRenderer
import com.example.nearschedulewidget.util.TimeUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CalendarWidgetUpdater {

    const val UPDATE_ACTION = "com.example.nearschedulewidget.UPDATE"

    fun updateNow(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val hasPerm = ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        val repo = CalendarRepo(context)
        val now = System.currentTimeMillis()

        // Fetch a wide range of events once, as the renderer expects the full list.
        val startQuery = now - 4 * 60 * 60 * 1000L
        val endQuery = now + 36 * 60 * 60 * 1000L
        val events = if (hasPerm) repo.loadEvents(startQuery, endQuery) else emptyList()

        // The new calculator needs an eventProvider. We can wrap the pre-fetched list.
        val eventProvider = { start: Long, end: Long ->
            events.filter { it.begin < end && it.end > start }
                  .map { ScheduleEvent(it.begin, it.end, it.allDay) }
        }

        // Use the new TimeWindowCalculator
        val timeWindows = TimeWindowCalculator.calculateWindows(now, eventProvider)

        // Determine if the right column is showing tomorrow to display the header
        val tomorrowStart = TimeUtils.getTomorrowStart()
        val showTomorrowHeader = timeWindows.right.startMillis >= tomorrowStart

        val size = getWidgetSize(context, appWidgetManager, appWidgetId)

        // A size of 0x0 can happen on first load. Don't attempt to render.
        if (size.width <= 0 || size.height <= 0) {
            return
        }

        val bmp: Bitmap = CalendarRenderer.render(
            context,
            size,
            timeWindows.left,
            timeWindows.right,
            events, // Pass the original list of EventItems
            showTomorrowHeader,
            now
        )

        // Create an Intent to open the calendar app
        val calendarUriBuilder = CalendarContract.CONTENT_URI.buildUpon()
        calendarUriBuilder.appendPath("time")
        ContentUris.appendId(calendarUriBuilder, now)
        val openCalendarIntent = Intent(Intent.ACTION_VIEW).setData(calendarUriBuilder.build())
        val openCalendarPendingIntent = PendingIntent.getActivity(context, 0, openCalendarIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Create an Intent for the refresh button
        val refreshIntent = Intent(context, CalendarWidgetProvider::class.java).apply {
            action = UPDATE_ACTION
            // Make sure the intent is unique for this widget instance
            data = Uri.parse("widget://update/$appWidgetId")
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(context, appWidgetId, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val formattedTime = sdf.format(Date(now))
        val updateTimeText = "$formattedTime 刷新"

        val rv = RemoteViews(context.packageName, R.layout.widget_calendar).apply {
            setImageViewBitmap(R.id.ivCanvas, bmp)
            setOnClickPendingIntent(R.id.ivCanvas, openCalendarPendingIntent)
            setOnClickPendingIntent(R.id.btn_refresh, refreshPendingIntent)
            setTextViewText(R.id.tv_last_updated, updateTimeText)

            // Ensure the final state is correct
            setViewVisibility(R.id.btn_refresh, View.VISIBLE)
        }
        appWidgetManager.updateAppWidget(appWidgetId, rv)
    }

    private fun getWidgetSize(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int): CalendarRenderer.Size {
        val options: Bundle = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val dm = context.resources.displayMetrics

        // Get the width and height in dp. The host will report the available size.
        val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
        val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)

        // The renderer needs size in pixels.
        val widthPx = (width * dm.density).toInt()
        val heightPx = (height * dm.density).toInt()

        return CalendarRenderer.Size(widthPx, heightPx, dm.density)
    }
}
