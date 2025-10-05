package com.example.nearschedulewidget

data class TimeWindow(val startMillis: Long, val endMillis: Long)

data class ScheduleEvent(
    val startMillis: Long,
    val endMillis: Long,
    val isAllDay: Boolean
)
