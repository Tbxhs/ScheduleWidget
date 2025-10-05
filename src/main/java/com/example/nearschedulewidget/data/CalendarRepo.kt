
package com.example.nearschedulewidget.data

import android.content.Context
import android.database.Cursor
import android.provider.CalendarContract
import com.example.nearschedulewidget.EventItem

class CalendarRepo(private val ctx: Context) {

    fun loadEvents(start: Long, end: Long): List<EventItem> {
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(start.toString())
            .appendPath(end.toString())
            .build()

        val proj = arrayOf(
            CalendarContract.Instances.EVENT_ID,          // 0
            CalendarContract.Instances.BEGIN,             // 1
            CalendarContract.Instances.END,               // 2
            CalendarContract.Instances.TITLE,             // 3
            CalendarContract.Instances.ALL_DAY,           // 4
            CalendarContract.Instances.DISPLAY_COLOR,     // 5
            CalendarContract.Instances.SELF_ATTENDEE_STATUS, // 6 (may be null)
            CalendarContract.Instances.AVAILABILITY       // 7 (freebusy)
        )

        val list = mutableListOf<EventItem>()
        val cr = ctx.contentResolver
        cr.query(uri, proj, null, null, CalendarContract.Instances.BEGIN + " ASC")?.use { c: Cursor ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                val begin = c.getLong(1)
                val finish = c.getLong(2)
                val title = c.getString(3) ?: ""
                val allDay = c.getInt(4) == 1
                val color = c.getInt(5)
                val selfStatus = try { c.getInt(6) } catch (_: Exception) { 0 }
                val avail = try { c.getInt(7) } catch (_: Exception) { 0 }

                // Map status: tentative=1, accepted=2? The constants vary; we map roughly.
                val statusMapped = when (selfStatus) {
                    CalendarContract.Attendees.ATTENDEE_STATUS_TENTATIVE -> 1
                    CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED -> 3
                    else -> {
                        // if availability=FREE, mark as free
                        if (avail == CalendarContract.Events.AVAILABILITY_FREE) 2 else 0
                    }
                }

                list += EventItem(id, title, begin, finish, allDay, color, statusMapped)
            }
        }
        return list
    }

    fun firstTimedEventTomorrow(events: List<EventItem>, tomorrowStart: Long, tomorrowEnd: Long): Long? {
        return events
            .filter { !it.allDay && it.begin >= tomorrowStart && it.begin < tomorrowEnd }
            .minByOrNull { it.begin }?.begin
    }
}
