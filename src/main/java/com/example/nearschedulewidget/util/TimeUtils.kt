package com.example.nearschedulewidget.util

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

object TimeUtils {

    fun getCurrentTime(): Long = System.currentTimeMillis()

    fun getTodayStart(zoneId: ZoneId = ZoneId.systemDefault()): Long {
        return LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun getTodayEnd(zoneId: ZoneId = ZoneId.systemDefault()): Long {
        return LocalDate.now(zoneId).plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun getTomorrowStart(zoneId: ZoneId = ZoneId.systemDefault()): Long {
        return LocalDate.now(zoneId).plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun getTomorrowEnd(zoneId: ZoneId = ZoneId.systemDefault()): Long {
        return LocalDate.now(zoneId).plusDays(2).atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun floorToHour(time: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long {
        val zonedDateTime = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(time), zoneId)
        return zonedDateTime.withMinute(0).withSecond(0).withNano(0).toInstant().toEpochMilli()
    }
}
