package com.leon.timeface.service

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.text.format.DateFormat
import android.view.SurfaceHolder
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import com.leon.timeface.CalendarRepository
import com.leon.timeface.UpcomingEvent
import java.time.ZonedDateTime
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class TimeFaceRenderer(
    private val context: Context,
    surfaceHolder: SurfaceHolder,
    currentUserStyleRepository: CurrentUserStyleRepository,
    watchState: WatchState
) : Renderer.CanvasRenderer(
    surfaceHolder,
    currentUserStyleRepository,
    watchState,
    CanvasType.HARDWARE,
    /* interactiveDrawModeUpdateDelayMillis = */ 1000
) {
    private val bg = Paint().apply { color = Color.parseColor("#121418"); isAntiAlias = true }
    private val tickMinor = Paint().apply { color = Color.parseColor("#3A3D42"); strokeWidth = 2f; isAntiAlias = true }
    private val tickHour = Paint().apply { color = Color.WHITE; strokeWidth = 4f; isAntiAlias = true }
    private val numberPaint = Paint().apply { color = Color.WHITE; isAntiAlias = true; isFakeBoldText = true; textAlign = Paint.Align.CENTER }
    private val hourHand = Paint().apply { color = Color.WHITE; strokeWidth = 12f; strokeCap = Paint.Cap.ROUND; isAntiAlias = true }
    private val minuteHand = Paint().apply { color = Color.WHITE; strokeWidth = 8f; strokeCap = Paint.Cap.ROUND; isAntiAlias = true }
    private val centerDot = Paint().apply { color = Color.LTGRAY; isAntiAlias = true }
    private val accentBlue = Color.parseColor("#3BC4FF")
    private val eventArc = Paint().apply { color = accentBlue; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; isAntiAlias = true }
    private val eventLine = Paint().apply { color = accentBlue; strokeWidth = 3f; isAntiAlias = true }
    private val chipBg = Paint().apply { color = accentBlue; isAntiAlias = true }
    private val chipText = Paint().apply { color = Color.parseColor("#0A1420"); isAntiAlias = true; isFakeBoldText = true; textAlign = Paint.Align.CENTER }
    private val digitalTime = Paint().apply { color = Color.WHITE; isAntiAlias = true; isFakeBoldText = true; textAlign = Paint.Align.CENTER }
    private val nextLabel = Paint().apply { color = Color.parseColor("#9AA3AD"); isAntiAlias = true; textAlign = Paint.Align.CENTER }
    private val cardBg = Paint().apply { color = Color.parseColor("#CC1A1E24"); isAntiAlias = true }
    private val cardTime = Paint().apply { color = accentBlue; isAntiAlias = true; isFakeBoldText = true }
    private val cardTitle = Paint().apply { color = Color.WHITE; isAntiAlias = true }
    private val cardDivider = Paint().apply { color = Color.parseColor("#33FFFFFF"); strokeWidth = 1.5f }
    private val pillBg = Paint().apply { color = Color.parseColor("#E6141820"); isAntiAlias = true }
    private val pillBorder = Paint().apply { color = accentBlue; style = Paint.Style.STROKE; strokeWidth = 2f; isAntiAlias = true }
    private val pillText = Paint().apply { color = Color.parseColor("#FFC94D"); isAntiAlias = true; isFakeBoldText = true }
    private val pillDot = Paint().apply { color = Color.parseColor("#FFC94D"); isAntiAlias = true }

    // Cache calendar reads; content-provider queries are relatively slow, no need every second.
    private var cachedEvents: List<UpcomingEvent> = emptyList()
    private var cacheTimeMs = 0L

    private fun events(): List<UpcomingEvent> {
        val now = System.currentTimeMillis()
        if (now - cacheTimeMs > 60_000) {
            cachedEvents = CalendarRepository.upcomingEvents(context, limit = 3)
            cacheTimeMs = now
        }
        return cachedEvents
    }

    override fun renderHighlightLayer(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime) {
        canvas.drawColor(Color.TRANSPARENT)
    }

    override fun render(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime) {
        val ambient = renderParameters.drawMode == androidx.wear.watchface.DrawMode.AMBIENT
        val cx = bounds.exactCenterX()
        val cy = bounds.exactCenterY()
        val r = min(bounds.width(), bounds.height()) / 2f

        canvas.drawColor(Color.parseColor("#000000"))
        canvas.drawCircle(cx, cy, r, bg)

        val cal = Calendar.getInstance().apply { timeInMillis = zonedDateTime.toInstant().toEpochMilli() }
        val hour24 = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val second = cal.get(Calendar.SECOND)
        val hourFraction = (hour24 % 12) + minute / 60.0

        drawTicksAndNumbers(canvas, cx, cy, r)

        val upcoming = if (!ambient) events() else emptyList()
        val next = upcoming.firstOrNull()
        if (next != null) drawEventArc(canvas, cx, cy, r, next)

        drawHands(canvas, cx, cy, r, hourFraction, minute, second, ambient)
        canvas.drawCircle(cx, cy, r * 0.03f, centerDot)

        drawDigitalTime(canvas, cx, cy, r, cal, next)

        if (!ambient) drawEventCards(canvas, bounds, upcoming)
    }

    private fun angleForHourFraction(hourFraction: Double): Double {
        // 0 = 12 o'clock (top), clockwise. Convert to standard canvas-arc degrees
        // where 0deg = 3 o'clock and positive = clockwise.
        val clockDeg = (hourFraction / 12.0) * 360.0
        return clockDeg - 90.0
    }

    private fun point(cx: Float, cy: Float, radius: Float, angleDeg: Double): FloatArray {
        val rad = Math.toRadians(angleDeg)
        return floatArrayOf(cx + (radius * cos(rad)).toFloat(), cy + (radius * sin(rad)).toFloat())
    }

    private fun drawTicksAndNumbers(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        numberPaint.textSize = r * 0.16f
        for (i in 0 until 60) {
            val angle = angleForHourFraction(i / 5.0)
            val isHour = i % 5 == 0
            val outer = r * 0.92f
            val inner = if (isHour) r * 0.82f else r * 0.88f
            val p1 = point(cx, cy, outer, angle)
            val p2 = point(cx, cy, inner, angle)
            canvas.drawLine(p1[0], p1[1], p2[0], p2[1], if (isHour) tickHour else tickMinor)
        }
        for (h in 1..12) {
            val angle = angleForHourFraction(h.toDouble())
            val p = point(cx, cy, r * 0.68f, angle)
            val fm = numberPaint.fontMetrics
            canvas.drawText(h.toString(), p[0], p[1] - (fm.ascent + fm.descent) / 2f, numberPaint)
        }
    }

    private fun drawEventArc(canvas: Canvas, cx: Float, cy: Float, r: Float, event: UpcomingEvent) {
        val startCal = Calendar.getInstance().apply { timeInMillis = event.startMillis }
        val endCal = Calendar.getInstance().apply { timeInMillis = event.endMillis }
        val startFrac = (startCal.get(Calendar.HOUR_OF_DAY) % 12) + startCal.get(Calendar.MINUTE) / 60.0
        val endFrac = (endCal.get(Calendar.HOUR_OF_DAY) % 12) + endCal.get(Calendar.MINUTE) / 60.0

        val startAngle = angleForHourFraction(startFrac)
        var sweep = angleForHourFraction(endFrac) - startAngle
        if (sweep <= 0) sweep += 360.0
        sweep = sweep.coerceAtLeast(3.0) // keep short events visible

        val arcRadius = r * 0.92f
        eventArc.strokeWidth = r * 0.06f
        val rect = RectF(cx - arcRadius, cy - arcRadius, cx + arcRadius, cy + arcRadius)
        canvas.drawArc(rect, startAngle.toFloat(), sweep.toFloat(), false, eventArc)

        // Thin indicator line from center toward the event start.
        val lineEnd = point(cx, cy, r * 0.5f, startAngle)
        canvas.drawLine(cx, cy, lineEnd[0], lineEnd[1], eventLine)

        // Rounded chip near the arc's start, labelled with the event's start hour.
        val chipCenter = point(cx, cy, r * 0.78f, startAngle)
        val chipR = r * 0.11f
        canvas.drawCircle(chipCenter[0], chipCenter[1], chipR, chipBg)
        chipText.textSize = chipR * 1.1f
        val label = DateFormat.format("h", startCal).toString()
        val fm = chipText.fontMetrics
        canvas.drawText(label, chipCenter[0], chipCenter[1] - (fm.ascent + fm.descent) / 2f, chipText)
    }

    private fun drawHands(
        canvas: Canvas, cx: Float, cy: Float, r: Float,
        hourFraction: Double, minute: Int, second: Int, ambient: Boolean
    ) {
        val hourAngle = angleForHourFraction(hourFraction)
        val minuteAngle = angleForHourFraction((minute + second / 60.0) / 60.0 * 12.0 % 12.0)

        val hp = point(cx, cy, r * 0.5f, hourAngle)
        canvas.drawLine(cx, cy, hp[0], hp[1], hourHand)

        val mp = point(cx, cy, r * 0.72f, minuteAngle)
        canvas.drawLine(cx, cy, mp[0], mp[1], minuteHand)
    }

    private fun drawDigitalTime(canvas: Canvas, cx: Float, cy: Float, r: Float, cal: Calendar, next: UpcomingEvent?) {
        digitalTime.textSize = r * 0.22f
        val timeStr = DateFormat.format(if (DateFormat.is24HourFormat(context)) "H:mm" else "h:mm", cal).toString()
        canvas.drawText(timeStr, cx, cy - r * 0.28f, digitalTime)

        if (next != null) {
            nextLabel.textSize = r * 0.08f
            val nextCal = Calendar.getInstance().apply { timeInMillis = next.startMillis }
            val nextTimeStr = DateFormat.getTimeFormat(context).format(nextCal.time)
            canvas.drawText("next: $nextTimeStr ${next.title}".take(34), cx, cy - r * 0.16f, nextLabel)
        }
    }

    private fun drawEventCards(canvas: Canvas, bounds: Rect, upcoming: List<UpcomingEvent>) {
        if (upcoming.isEmpty()) return
        val cx = bounds.exactCenterX()
        val r = min(bounds.width(), bounds.height()) / 2f
        val cardWidth = r * 1.5f
        val cardLeft = cx - cardWidth / 2f
        val cardTop = bounds.exactCenterY() + r * 0.28f
        val rowHeight = r * 0.26f
        val rows = min(upcoming.size, 2)
        val cardHeight = rowHeight * rows + r * 0.08f
        val cardRect = RectF(cardLeft, cardTop, cardLeft + cardWidth, cardTop + cardHeight)
        canvas.drawRoundRect(cardRect, r * 0.1f, r * 0.1f, cardBg)

        cardTime.textSize = r * 0.11f
        cardTitle.textSize = r * 0.11f
        val textLeft = cardLeft + r * 0.1f
        for (i in 0 until rows) {
            val e = upcoming[i]
            val rowCenterY = cardTop + r * 0.06f + rowHeight * i + rowHeight / 2f
            val cal = Calendar.getInstance().apply { timeInMillis = e.startMillis }
            val timeStr = DateFormat.getTimeFormat(context).format(cal.time)
            canvas.drawText(timeStr, textLeft, rowCenterY - r * 0.02f, cardTime)
            canvas.drawText(e.title.take(20), textLeft + r * 0.32f, rowCenterY - r * 0.02f, cardTitle)
            if (i == 0 && !e.location.isNullOrBlank()) {
                val locPaint = Paint(cardTitle).apply { color = Color.parseColor("#FFC94D"); textSize = r * 0.08f }
                canvas.drawText(e.location.take(24), textLeft, rowCenterY + r * 0.09f, locPaint)
            }
            if (i < rows - 1) {
                val dividerY = cardTop + r * 0.06f + rowHeight * (i + 1)
                canvas.drawLine(cardLeft + r * 0.06f, dividerY, cardLeft + cardWidth - r * 0.06f, dividerY, cardDivider)
            }
        }

        // Location pill overlapping the bottom edge of the card, mirrors the mock-up.
        val firstLoc = upcoming.firstOrNull { !it.location.isNullOrBlank() }?.location
        if (firstLoc != null) {
            pillText.textSize = r * 0.09f
            val textWidth = pillText.measureText(firstLoc)
            val pillWidth = textWidth + r * 0.34f
            val pillHeight = r * 0.16f
            val pillLeft = cx - pillWidth / 2f
            val pillTop = cardRect.bottom - pillHeight / 2f
            val pillRect = RectF(pillLeft, pillTop, pillLeft + pillWidth, pillTop + pillHeight)
            canvas.drawRoundRect(pillRect, pillHeight / 2f, pillHeight / 2f, pillBg)
            canvas.drawRoundRect(pillRect, pillHeight / 2f, pillHeight / 2f, pillBorder)
            canvas.drawCircle(pillLeft + r * 0.14f, pillTop + pillHeight / 2f, r * 0.03f, pillDot)
            val fm = pillText.fontMetrics
            canvas.drawText(
                firstLoc,
                pillLeft + r * 0.22f,
                pillTop + pillHeight / 2f - (fm.ascent + fm.descent) / 2f,
                pillText
            )
        }
    }
}
