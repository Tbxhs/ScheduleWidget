package com.example.nearschedulewidget.util

import android.icu.util.ChineseCalendar
import java.util.Calendar
import java.util.GregorianCalendar

/**
 * A utility for converting Gregorian dates to Chinese Lunar Calendar dates using Android's ICU library.
 */
object LunarCalendar {

    private val LUNAR_DAY_NAMES = arrayOf(
        "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
        "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
        "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
    )

    private val LUNAR_MONTH_NAMES = arrayOf(
        "正月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "冬月", "腊月"
    )

    /**
     * Converts a Gregorian date (year, month, day) to its Lunar date string representation.
     * Month is 1-indexed (1 for January, 12 for December).
     */
    fun getLunarDate(year: Int, month: Int, day: Int): String {
        val gregorianCalendar = GregorianCalendar(year, month - 1, day) // Month is 0-indexed for GregorianCalendar
        val chineseCalendar = ChineseCalendar(gregorianCalendar.time)

        val lunarMonth = chineseCalendar.get(ChineseCalendar.MONTH) // 0-indexed
        val lunarDay = chineseCalendar.get(ChineseCalendar.DAY_OF_MONTH) // 1-indexed

        // Check if it's the first day of a lunar month
        if (lunarDay == 1) {
            return LUNAR_MONTH_NAMES[lunarMonth] // lunarMonth is already 0-indexed for LUNAR_MONTH_NAMES
        }

        return LUNAR_DAY_NAMES[lunarDay - 1] // lunarDay is 1-indexed, LUNAR_DAY_NAMES is 0-indexed
    }
}
