
package com.example.nearschedulewidget.layout

import com.example.nearschedulewidget.EventItem
import com.example.nearschedulewidget.TimeWindow
import kotlin.math.max
import kotlin.math.min

data class Box(val id: Long, val left: Float, val top: Float, val right: Float, val bottom: Float, val color: Int, val status: Int, val title: String, val startLabel: String)

object OverlapLayout {
    const val K_MAX = 3
    const val GAP_DP = 6f
    const val MIN_HEIGHT_MIN = 12

    fun layout(window: TimeWindow, events: List<EventItem>, hourHeightPx: Float, contentLeft: Float, contentRight: Float, toPx: (Float)->Float, timeFmt: (Long)->String): Pair<List<Box>, Int> {
        // Clip events to window
        val segs = events.filter { !it.allDay }.mapNotNull { e ->
            val start = max(e.begin, window.startMillis)
            val end = min(e.end, window.endMillis)
            if (start >= end) null else e.copy(begin = start, end = end)
        }.sortedBy { it.begin }

        data class Slot(var end: Long, val col: Int, val id: Long)
        val active = mutableListOf<Slot>()
        val colsFor = mutableMapOf<Long, Int>()
        segs.forEach { e ->
            active.removeAll { it.end <= e.begin }
            val used = active.map { it.col }.toSet().toMutableSet()
            var col = 0
            while (used.contains(col)) col++
            active += Slot(e.end, col, e.id)
            colsFor[e.id] = col
        }
        val k = (colsFor.values.maxOrNull() ?: -1) + 1
        val kEff = min(k, K_MAX)
        val gap = toPx(GAP_DP)
        val totalW = contentRight - contentLeft
        val colW = (totalW - gap * (kEff - 1)) / kEff

        var overflow = 0
        val boxes = mutableListOf<Box>()
        segs.forEach { e ->
            val col = (colsFor[e.id] ?: 0).coerceAtMost(K_MAX - 1)
            if (k > K_MAX && col == K_MAX - 1) {
                overflow++
                return@forEach
            }
            val l = contentLeft + col * (colW + gap)
            val r = l + colW
            val top = hourHeightPx * ((e.begin - window.startMillis).toFloat() / 3_600_000f)
            val bottom = max(top + hourHeightPx * (MIN_HEIGHT_MIN / 60f), hourHeightPx * ((e.end - window.startMillis).toFloat() / 3_600_000f))
            boxes += Box(e.id, l, top, r, bottom, e.color, e.status, e.title, timeFmt(e.begin))
        }
        return boxes to overflow
    }
}
