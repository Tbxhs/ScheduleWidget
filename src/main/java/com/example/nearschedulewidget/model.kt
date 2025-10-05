
package com.example.nearschedulewidget

data class EventItem(
    val id: Long,
    val title: String,
    val begin: Long,
    val end: Long,
    val allDay: Boolean,
    val color: Int,
    val status: Int // 0=accepted, 1=tentative, 2=free, 3=declined (approx mapping)
)

enum class Mode { T_T, T_M }
