package com.example.nearschedulewidget

import com.example.nearschedulewidget.util.TimeUtils
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

data class TimeWindows(val left: TimeWindow, val right: TimeWindow)

object TimeWindowCalculator {

    private val FOUR_HOURS_IN_MILLIS = TimeUnit.HOURS.toMillis(4)
    private val SEVEN_HOURS_IN_MILLIS = TimeUnit.HOURS.toMillis(7)

    fun calculateWindows(
        now: Long,
        eventProvider: (startMillis: Long, endMillis: Long) -> List<ScheduleEvent>
    ): TimeWindows {
        val leftColumn = calculateLeftColumn(now)

        val todayStart = TimeUtils.getTodayStart()
        val todayEnd = TimeUtils.getTodayEnd()
        val tomorrowStart = TimeUtils.getTomorrowStart()
        val tomorrowEnd = TimeUtils.getTomorrowEnd()

        val eventsToday = eventProvider(todayStart, todayEnd)

        val showTomorrow = shouldShowTomorrowPreview(now, leftColumn.endMillis, eventsToday)

        val rightColumn = if (showTomorrow) {
            val eventsTomorrow = eventProvider(tomorrowStart, tomorrowEnd)
            calculateRightColumn(leftColumn.endMillis, true, eventsTomorrow)
        } else {
            calculateRightColumn(leftColumn.endMillis, false, emptyList())
        }

        return TimeWindows(left = leftColumn, right = rightColumn)
    }

    internal fun calculateLeftColumn(now: Long): TimeWindow {
        val todayStart = TimeUtils.getTodayStart()
        val todayEnd = TimeUtils.getTodayEnd()

        val candidate = TimeUtils.floorToHour(now)

        // Clamp the start time to ensure the 4-hour window does not cross midnight
        val startL = max(todayStart, min(candidate, todayEnd - FOUR_HOURS_IN_MILLIS))

        val endL = startL + FOUR_HOURS_IN_MILLIS

        return TimeWindow(startL, endL)
    }

    internal fun shouldShowTomorrowPreview(
        now: Long,
        leftColumnEnd: Long,
        eventsToday: List<ScheduleEvent>
    ): Boolean {
        val todayEnd = TimeUtils.getTodayEnd()

        // Condition 1: Today has less than 4 hours left after the left column ends.
        val isTimeRemainingShort = (todayEnd - leftColumnEnd) < FOUR_HOURS_IN_MILLIS
        if (isTimeRemainingShort) {
            return true
        }

        // Condition 2: No non-all-day events between now and the end of today.
        val hasNoUpcomingEventsToday = eventsToday.none { event ->
            !event.isAllDay && event.startMillis < todayEnd && event.endMillis > now
        }
        
        return hasNoUpcomingEventsToday
    }

    internal fun calculateRightColumn(
        leftColumnEnd: Long,
        showTomorrow: Boolean,
        tomorrowEvents: List<ScheduleEvent>
    ): TimeWindow {
        if (showTomorrow) {
            // T→M (Tomorrow Preview)
            val tomorrowStart = TimeUtils.getTomorrowStart()
            val tomorrowEnd = TimeUtils.getTomorrowEnd()

            val firstTomorrowEvent = tomorrowEvents.firstOrNull { !it.isAllDay }

            return if (firstTomorrowEvent != null) {
                // If there are non-all-day events tomorrow, anchor to the first one
                val anchor = TimeUtils.floorToHour(firstTomorrowEvent.startMillis)
                val startR = anchor
                val endR = min(anchor + SEVEN_HOURS_IN_MILLIS, tomorrowEnd)
                TimeWindow(startR, endR)
            } else {
                // If there are no non-all-day events tomorrow, show a default 4h window
                val startR = tomorrowStart
                val endR = min(tomorrowStart + FOUR_HOURS_IN_MILLIS, tomorrowEnd)
                TimeWindow(startR, endR)
            }
        } else {
            // T→T (Today Continuation)
            val todayEnd = TimeUtils.getTodayEnd()
            val startR = leftColumnEnd
            val endR = min(leftColumnEnd + FOUR_HOURS_IN_MILLIS, todayEnd)
            return TimeWindow(startR, endR)
        }
    }
}
