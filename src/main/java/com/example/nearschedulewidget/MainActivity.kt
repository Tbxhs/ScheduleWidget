package com.example.nearschedulewidget

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioGroup
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import com.example.nearschedulewidget.settings.WidgetSettings
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import androidx.core.view.children

class MainActivity : ComponentActivity() {

    // Views for the new layout
    private lateinit var permissionSection: LinearLayout
    private lateinit var btnGrant: Button
    private lateinit var calendarChipGroup: ChipGroup
    private lateinit var timeWindowRadioGroup: RadioGroup
    private lateinit var eveningLookAheadSwitch: SwitchMaterial
    private lateinit var themeSwitch: SwitchMaterial
    private lateinit var transparencySlider: Slider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        permissionSection = findViewById(R.id.permission_section)
        btnGrant = findViewById(R.id.btnGrant)
        calendarChipGroup = findViewById(R.id.calendar_chip_group)
        timeWindowRadioGroup = findViewById(R.id.time_window_radio_group)
        eveningLookAheadSwitch = findViewById(R.id.evening_look_ahead_switch)
        themeSwitch = findViewById(R.id.theme_switch)
        transparencySlider = findViewById(R.id.transparency_slider)

        // Setup listeners
        setupPermissionButton()
        setupSettingListeners()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionUI()
    }

    private fun setupPermissionButton() {
        btnGrant.setOnClickListener {
            if (!hasCalendarReadPermission()) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CALENDAR), 100)
            } else {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", packageName, null)
                startActivity(intent)
            }
        }
    }

    private fun setupSettingListeners() {
        // TODO: Implement listeners for the other settings controls
    }

    private fun hasCalendarReadPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
    }

    private fun updatePermissionUI() {
        if (hasCalendarReadPermission()) {
            permissionSection.visibility = View.GONE
            loadAndDisplayCalendars()
        } else {
            permissionSection.visibility = View.VISIBLE
            calendarChipGroup.removeAllViews()
        }
    }

    @SuppressLint("Range")
    private fun loadAndDisplayCalendars() {
        if (!hasCalendarReadPermission()) return
        calendarChipGroup.removeAllViews() // Clear old views before loading new ones

        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR
        )

        val cursor = contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            "${CalendarContract.Calendars.VISIBLE} = 1",
            null,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME + " ASC"
        )

        val allCalendarIds = mutableSetOf<String>()
        val calendarData = mutableListOf<Triple<Long, String, Int>>()

        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndex(CalendarContract.Calendars._ID))
                val name = it.getString(it.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME))
                val color = it.getInt(it.getColumnIndex(CalendarContract.Calendars.CALENDAR_COLOR))
                allCalendarIds.add(id.toString())
                calendarData.add(Triple(id, name, color))
            }
        }

        var selectedIds = WidgetSettings.loadSelectedCalendarIds(this)
        // If no settings are saved yet (first run), select all calendars by default
        if (selectedIds == null) {
            selectedIds = allCalendarIds
            WidgetSettings.saveSelectedCalendarIds(this, selectedIds)
        }

        for ((id, name, color) in calendarData) {
            val chip = Chip(this).apply {
                text = name
                isCheckable = true
                isChecked = selectedIds.contains(id.toString())
                tag = id

                val colorDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                    setStroke(1, 0x2F000000)
                }
                chipIcon = colorDrawable
                chipIconSize = 48f
                iconStartPadding = 16f

                setOnCheckedChangeListener { _, _ ->
                    val currentSelectedIds = calendarChipGroup.children
                        .filter { (it as Chip).isChecked }
                        .map { (it as Chip).tag.toString() }
                        .toSet()
                    WidgetSettings.saveSelectedCalendarIds(this@MainActivity, currentSelectedIds)
                }
            }
            calendarChipGroup.addView(chip)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            updatePermissionUI()
        }
    }
}
