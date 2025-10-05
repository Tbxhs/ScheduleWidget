package com.example.nearschedulewidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import com.example.nearschedulewidget.widget.CalendarWidgetProvider
import com.example.nearschedulewidget.widget.WidgetUpdateScheduler

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val radioGroup = findViewById<RadioGroup>(R.id.rgUpdateInterval)
        val prefs = getSharedPreferences("widget_settings", Context.MODE_PRIVATE)

        // Load saved preference
        val currentInterval = prefs.getLong("update_interval", 15 * 60 * 1000L) // Default to 15 minutes
        val currentRadioButtonId = when (currentInterval) {
            5 * 60 * 1000L -> R.id.rb5min
            10 * 60 * 1000L -> R.id.rb10min
            30 * 60 * 1000L -> R.id.rb30min
            else -> R.id.rb15min
        }
        radioGroup.check(currentRadioButtonId)

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val selectedInterval = when (checkedId) {
                R.id.rb5min -> 5 * 60 * 1000L
                R.id.rb10min -> 10 * 60 * 1000L
                R.id.rb15min -> 15 * 60 * 1000L
                R.id.rb30min -> 30 * 60 * 1000L
                else -> 15 * 60 * 1000L
            }

            // Save the new interval
            prefs.edit().putLong("update_interval", selectedInterval).apply()

            // Reschedule the widget update using the scheduler
            WidgetUpdateScheduler.scheduleWidgetUpdate(this)

            // Also trigger an immediate update to reflect changes
            triggerImmediateUpdate()
        }
    }

    private fun triggerImmediateUpdate() {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val componentName = ComponentName(this, CalendarWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        val intent = Intent(this, CalendarWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
        }
        sendBroadcast(intent)
    }
}
