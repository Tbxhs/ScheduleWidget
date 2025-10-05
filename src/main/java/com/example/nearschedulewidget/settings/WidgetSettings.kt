package com.example.nearschedulewidget.settings

import android.content.Context

object WidgetSettings {

    private const val PREFS_NAME = "widget_settings"
    private const val KEY_SELECTED_CALENDARS = "selected_calendar_ids"

    fun saveSelectedCalendarIds(context: Context, calendarIds: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_SELECTED_CALENDARS, calendarIds).apply()
    }

    fun loadSelectedCalendarIds(context: Context): Set<String>? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_SELECTED_CALENDARS, null)
    }
}
