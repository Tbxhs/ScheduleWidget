
package com.example.nearschedulewidget.render

import android.content.Context
import android.graphics.*
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withClip
import androidx.core.graphics.withTranslation
import com.example.nearschedulewidget.EventItem
import com.example.nearschedulewidget.R
import com.example.nearschedulewidget.TimeWindow
import com.example.nearschedulewidget.layout.Box
import com.example.nearschedulewidget.layout.OverlapLayout
import com.example.nearschedulewidget.util.LunarCalendar
import java.text.SimpleDateFormat
import java.util.*

object CalendarRenderer {

    data class Size(val width: Int, val height: Int, val density: Float)

    private fun dp(ctx: Context, v: Float) = v * ctx.resources.displayMetrics.density

    fun render(ctx: Context, size: Size, left: TimeWindow, right: TimeWindow, events: List<EventItem>, showTomorrowHeader: Boolean, now: Long): Bitmap {
        val bmp = createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)

        val pad = dp(ctx, 8f)
        val topPad = dp(ctx, 20f)
        val card = RectF(0f, 0f, size.width.toFloat(), size.height.toFloat())
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(ctx, R.color.widget_background) }
        c.drawRoundRect(card, dp(ctx, 24f), dp(ctx, 24f), bg)

        // Header (Date, Lunar, Week)
        val dateTxt = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(ctx, R.color.widget_text_primary); textSize = dp(ctx, 28f); typeface = Typeface.DEFAULT }
        val cal = Calendar.getInstance().apply { timeInMillis = left.startMillis }
        val day = cal.get(Calendar.DAY_OF_MONTH).toString()
        val dayWidth = dateTxt.measureText(day)
        val dateBaseY = topPad + dp(ctx, 32f)
        c.drawText(day, pad, dateBaseY, dateTxt)
        val dateCenterY = dateBaseY + (dateTxt.fontMetrics.ascent + dateTxt.fontMetrics.descent) / 2f

        val separatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(ctx, R.color.widget_separator); strokeWidth = dp(ctx, 1f) }
        val gap = dp(ctx, 6f)
        var currentX = pad + dayWidth + gap

        val separatorLineHeight = dp(ctx, 20f)
        val separatorTop = dateCenterY - separatorLineHeight / 2f
        val separatorBottom = dateCenterY + separatorLineHeight / 2f
        c.drawLine(currentX, separatorTop, currentX, separatorBottom, separatorPaint)
        currentX += gap

        val lunarDate = LunarCalendar.getLunarDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
        val lunarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(ctx, R.color.widget_text_secondary); textSize = dp(ctx, 8f) }
        val lunarLineHeight = dp(ctx, 9f)
        val lunarBlockHeight = lunarDate.length * lunarLineHeight
        val lunarBlockTop = dateCenterY - lunarBlockHeight / 2f
        val lunarY = lunarBlockTop - lunarPaint.fontMetrics.top
        lunarDate.forEachIndexed { index, char -> c.drawText(char.toString(), currentX, lunarY + index * lunarLineHeight, lunarPaint) }
        currentX += lunarPaint.measureText(lunarDate.firstOrNull()?.toString() ?: "") + gap

        c.drawLine(currentX, separatorTop, currentX, separatorBottom, separatorPaint)
        currentX += gap

        val week = listOf("周日","周一","周二","周三","周四","周五","周六")[cal.get(Calendar.DAY_OF_WEEK)-1]
        val weekPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(ctx, R.color.widget_text_special); textSize = dp(ctx, 8f) }
        val weekLineHeight = dp(ctx, 9f)
        val weekBlockHeight = week.length * weekLineHeight
        val weekBlockTop = dateCenterY - weekBlockHeight / 2f
        val weekY = weekBlockTop - weekPaint.fontMetrics.top
        week.forEachIndexed { index, char -> c.drawText(char.toString(), currentX, weekY + index * weekLineHeight, weekPaint) }

        val leftHeaderBottom = dateBaseY + dateTxt.fontMetrics.descent + dp(ctx, 4f)

        // Column definitions
        val midX = card.centerX()
        val colPad = dp(ctx, 2f)
        val labelGutter = dp(ctx, 28f)
        val leftColLeft = pad
        val leftColRight = midX - colPad
        val rightColLeft = midX + colPad
        val rightColRight = card.right - pad

        // --- Content Top & All-Day Rendering (Mode-dependent) ---
        val allDayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(ctx, R.color.widget_allday_background) }
        val allDayTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(ctx, R.color.widget_allday_text); textSize = dp(ctx, 10f) }
        val allDayEvents = events.filter { it.allDay }
        val todayCal = Calendar.getInstance().apply { timeInMillis = left.startMillis }
        val todayAllDay = allDayEvents.firstOrNull { event ->
            val eventCal = Calendar.getInstance().apply { timeInMillis = event.begin }
            eventCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) && eventCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
        }
        val allDayPillHeight = dp(ctx, 18f)
        val allDayVSpace = dp(ctx, 4f)

        val leftContentTop: Float
        val rightContentTop: Float

        if (showTomorrowHeader) {
            // T-M Mode: Decoupled vertical layout
            val tPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(ctx, R.color.widget_text_tertiary); textSize = dp(ctx, 10f) }
            val tomorrowLabelY = topPad + dp(ctx, 18f)
            c.drawText("明天", rightColLeft + labelGutter, tomorrowLabelY, tPaint)
            val rightHeaderBottom = tomorrowLabelY + tPaint.fontMetrics.descent

            // Left column content top
            val todayAllDayAreaHeight = if (todayAllDay != null) allDayPillHeight + allDayVSpace else 0f
            leftContentTop = leftHeaderBottom + allDayVSpace + todayAllDayAreaHeight
            todayAllDay?.let {
                val rect = RectF(leftColLeft + labelGutter, leftHeaderBottom + allDayVSpace, leftColRight, leftHeaderBottom + allDayVSpace + allDayPillHeight)
                c.drawRoundRect(rect, dp(ctx, 3f), dp(ctx, 3f), allDayPaint)
                val text = if (TextUtils.isEmpty(it.title)) "假期/全天事件" else it.title
                val clippedText = TextUtils.ellipsize(text, allDayTextPaint, rect.width() - dp(ctx, 12f), TextUtils.TruncateAt.END)
                val textY = rect.centerY() - (allDayTextPaint.ascent() + allDayTextPaint.descent()) / 2f
                c.drawText(clippedText, 0, clippedText.length, rect.left + dp(ctx, 6f), textY, allDayTextPaint)
            }

            // Right column content top
            val tomorrowCal = Calendar.getInstance().apply { timeInMillis = right.startMillis }
            val tomorrowAllDay = allDayEvents.firstOrNull { event ->
                val eventCal = Calendar.getInstance().apply { timeInMillis = event.begin }
                eventCal.get(Calendar.YEAR) == tomorrowCal.get(Calendar.YEAR) && eventCal.get(Calendar.DAY_OF_YEAR) == tomorrowCal.get(Calendar.DAY_OF_YEAR)
            }
            val tomorrowAllDayAreaHeight = if (tomorrowAllDay != null) allDayPillHeight + allDayVSpace else 0f
            rightContentTop = rightHeaderBottom + allDayVSpace + tomorrowAllDayAreaHeight
            tomorrowAllDay?.let {
                val rect = RectF(rightColLeft + labelGutter, rightHeaderBottom + allDayVSpace, rightColRight, rightHeaderBottom + allDayVSpace + allDayPillHeight)
                c.drawRoundRect(rect, dp(ctx, 3f), dp(ctx, 3f), allDayPaint)
                val text = if (TextUtils.isEmpty(it.title)) "假期/全天事件" else it.title
                val clippedText = TextUtils.ellipsize(text, allDayTextPaint, rect.width() - dp(ctx, 12f), TextUtils.TruncateAt.END)
                val textY = rect.centerY() - (allDayTextPaint.ascent() + allDayTextPaint.descent()) / 2f
                c.drawText(clippedText, 0, clippedText.length, rect.left + dp(ctx, 6f), textY, allDayTextPaint)
            }
        } else {
            // T-T Mode: Aligned layout with full-width banner
            val allDayAreaHeight = if (todayAllDay != null) allDayPillHeight + allDayVSpace else 0f
            val unifiedContentTop = leftHeaderBottom + allDayVSpace + allDayAreaHeight
            leftContentTop = unifiedContentTop
            rightContentTop = unifiedContentTop
            todayAllDay?.let {
                val rect = RectF(leftColLeft + labelGutter, leftHeaderBottom + allDayVSpace, rightColRight, leftHeaderBottom + allDayVSpace + allDayPillHeight)
                c.drawRoundRect(rect, dp(ctx, 3f), dp(ctx, 3f), allDayPaint)
                val text = if (TextUtils.isEmpty(it.title)) "假期/全天事件" else it.title
                val clippedText = TextUtils.ellipsize(text, allDayTextPaint, rect.width() - dp(ctx, 12f), TextUtils.TruncateAt.END)
                val textY = rect.centerY() - (allDayTextPaint.ascent() + allDayTextPaint.descent()) / 2f
                c.drawText(clippedText, 0, clippedText.length, rect.left + dp(ctx, 6f), textY, allDayTextPaint)
            }
        }

        val contentBottom = card.bottom - dp(ctx, 30f)

        fun drawColumn(win: TimeWindow, leftX: Float, rightX: Float, contentTop: Float) {
            val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(ctx, R.color.widget_grid_line); strokeWidth = dp(ctx, 0.5f) }
            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(ctx, R.color.widget_text_secondary); textSize = dp(ctx, 8f); textAlign = Paint.Align.RIGHT }
            val hours = ((win.endMillis - win.startMillis) / 3_600_000f).toInt().coerceAtLeast(1)
            val hourHeight = (contentBottom - contentTop) / hours.toFloat()
            val startHour = SimpleDateFormat("H", Locale.getDefault()).format(Date(win.startMillis)).toInt()
            for (i in 0..hours) {
                val y = contentTop + i * hourHeight
                val lineX = leftX + labelGutter
                c.drawLine(lineX, y, rightX, y, gridPaint)
                val label = "${(startHour + i) % 24}时"
                val textY = y - (labelPaint.fontMetrics.ascent + labelPaint.fontMetrics.descent) / 2f
                c.drawText(label, lineX - dp(ctx, 4f), textY, labelPaint)
            }
        }

        drawColumn(left, leftColLeft, leftColRight, leftContentTop)
        drawColumn(right, rightColLeft, rightColRight, rightContentTop)

        val timeFmt = { t: Long -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(t)) }
        val leftEvents = events.filter { !it.allDay && it.begin < left.endMillis && it.end > left.startMillis }
        val rightEvents = events.filter { !it.allDay && it.begin < right.endMillis && it.end > right.startMillis }

        val leftHours = ((left.endMillis - left.startMillis) / 3_600_000f).toInt().coerceAtLeast(1)
        val leftHourHeight = (contentBottom - leftContentTop) / leftHours.toFloat()
        val leftBoxes = OverlapLayout.layout(left, leftEvents, leftHourHeight, leftColLeft + labelGutter, leftColRight, { dp(ctx, it) }, timeFmt)

        val rightHours = ((right.endMillis - right.startMillis) / 3_600_000f).toInt().coerceAtLeast(1)
        val rightHourHeight = (contentBottom - rightContentTop) / rightHours.toFloat()
        val rightBoxes = OverlapLayout.layout(right, rightEvents, rightHourHeight, rightColLeft + labelGutter, rightColRight, { dp(ctx, it) }, timeFmt)

        fun drawBoxes(boxes: List<Box>, now: Long, win: TimeWindow, hourHeight: Float, contentTop: Float) {
            val barColor = ContextCompat.getColor(ctx, R.color.widget_event_bar)
            val bgColor = ContextCompat.getColor(ctx, R.color.widget_event_background)
            val titleColor = ContextCompat.getColor(ctx, R.color.widget_event_title)
            val timeColor = ContextCompat.getColor(ctx, R.color.widget_event_time)

            boxes.forEach { b ->
                val rr = RectF(b.left, contentTop + b.top, b.right, contentTop + b.bottom)
                val radius = dp(ctx, 3f)
                val barWidth = dp(ctx, 1.5f)

                val clipPath = Path().apply { addRoundRect(rr, radius, radius, Path.Direction.CW) }
                c.withClip(clipPath) {
                    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor; alpha = 200 }
                    drawRect(rr, bgPaint)
                    val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (b.color != 0) b.color else barColor }
                    val barLeftPadding = dp(ctx, 2f)
                    val barVerticalPadding = dp(ctx, 2f)
                    drawRect(rr.left + barLeftPadding, rr.top + barVerticalPadding, rr.left + barLeftPadding + barWidth, rr.bottom - barVerticalPadding, barPaint)

                    if (b.status == 1) {
                        val stripes = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(ctx, R.color.widget_event_stripes); alpha = 80; strokeWidth = dp(ctx, 2f) }
                        var x = rr.left
                        while (x < rr.right) {
                            drawLine(x, rr.top, x + dp(ctx, 12f), rr.top + dp(ctx, 12f), stripes)
                            x += dp(ctx, 6f)
                        }
                    }

                    val titleP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = titleColor; textSize = dp(ctx, 10f); typeface = Typeface.DEFAULT_BOLD }
                    val timeP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = timeColor; textSize = dp(ctx, 9f); typeface = Typeface.DEFAULT }
                    val textPad = dp(ctx, 2f)
                    val textLeft = rr.left + barLeftPadding + barWidth + textPad
                    val textWidth = (rr.right - textLeft - textPad).toInt().coerceAtLeast(0)

                    val eventEndMillis = win.startMillis + (b.bottom / hourHeight * 3_600_000L).toLong()
                    val showTime = now < eventEndMillis

                    var timeLayout: StaticLayout? = null
                    var timeHeight = 0f
                    val timeSpacing = dp(ctx, 2f)

                    if (showTime) {
                        val timeTextPaint = TextPaint(timeP)
                        timeLayout = StaticLayout.Builder.obtain(b.startLabel, 0, b.startLabel.length, timeTextPaint, textWidth).build()
                        timeHeight = timeLayout.height.toFloat()
                    }

                    val titleTextPaint = TextPaint(titleP)
                    val titleLineHeight = titleTextPaint.fontMetrics.descent - titleTextPaint.fontMetrics.ascent
                    val availableHeightForTitle = rr.height() - (if (showTime) (timeHeight + timeSpacing) else 0f) - (textPad * 2)

                    val maxLines = (availableHeightForTitle / titleLineHeight).toInt().coerceAtLeast(1)
                    val titleLayout = StaticLayout.Builder.obtain(b.title, 0, b.title.length, titleTextPaint, textWidth)
                        .setMaxLines(maxLines)
                        .setEllipsize(TextUtils.TruncateAt.END)
                        .build()
                    val titleHeight = titleLayout.height.toFloat()

                    val totalContentHeight = titleHeight + if (showTime) timeSpacing + timeHeight else 0f
                    val contentTopY = rr.top + (rr.height() - totalContentHeight) / 2f

                    withTranslation(textLeft, contentTopY) { titleLayout.draw(this) }
                    if (timeLayout != null) { withTranslation(textLeft, contentTopY + titleHeight + timeSpacing) { timeLayout.draw(this) } }
                }
            }
        }

        drawBoxes(leftBoxes.first, now, left, leftHourHeight, leftContentTop)
        drawBoxes(rightBoxes.first, now, right, rightHourHeight, rightContentTop)

        fun drawNowLine(win: TimeWindow, leftX: Float, rightX: Float, hours: Int, contentTop: Float) {
            if (now in win.startMillis..win.endMillis) {
                val red = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(ctx, R.color.widget_now_line); strokeWidth = dp(ctx, 0.5f) }
                val hourHeight = (contentBottom - contentTop) / hours.toFloat()
                val y = contentTop + hourHeight * ((now - win.startMillis).toFloat() / 3_600_000f)
                val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = dp(ctx, 8f) }
                val textWidth = labelPaint.measureText("00时")
                val lineStartX = leftX + labelGutter - dp(ctx, 4f) - textWidth
                c.drawLine(lineStartX, y, rightX, y, red)
                c.drawCircle(lineStartX, y, dp(ctx, 1.5f), red)
            }
        }
        drawNowLine(left, leftColLeft, leftColRight, leftHours, leftContentTop)
        drawNowLine(right, rightColLeft, rightColRight, rightHours, rightContentTop)

        fun drawOverflow(overflow: Int, rightX: Float) {
            if (overflow <= 0) return
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(ctx, R.color.widget_text_tertiary); textSize = dp(ctx, 11f) }
            val txt = "其他 %d 个日程".format(overflow)
            val tw = p.measureText(txt)
            c.drawText(txt, rightX - tw - dp(ctx, 6f), contentBottom - dp(ctx, 4f), p)
        }
        drawOverflow(leftBoxes.second, leftColRight)
        drawOverflow(rightBoxes.second, rightColRight)

        return bmp
    }
}
