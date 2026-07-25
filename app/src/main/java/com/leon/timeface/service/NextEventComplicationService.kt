package com.leon.timeface.service

import android.text.format.DateFormat
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationText
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.leon.timeface.CalendarRepository
import java.util.Calendar

class NextEventComplicationService : ComplicationDataSourceService() {

    override fun onComplicationRequest(
        request: ComplicationRequest,
        listener: ComplicationRequestListener
    ) {
        val event = CalendarRepository.upcomingEvents(applicationContext, limit = 1).firstOrNull()
        val now = System.currentTimeMillis()

        if (event == null) {
            listener.onComplicationData(null)
            return
        }

        val timeStr = DateFormat.getTimeFormat(applicationContext).format(Calendar.getInstance().apply {
            timeInMillis = event.startMillis
        }.time)
        val titleLine = "$timeStr  ${event.title}"

        val data: ComplicationData = when (request.complicationType) {
            ComplicationType.RANGED_VALUE -> {
                // Progress represents how far through the event's own duration "window" we
                // are: from event start (0) to event end (1). Before the event starts this
                // clamps to 0, so the renderer can draw the full-length arc as "time until/through".
                val total = (event.endMillis - event.startMillis).coerceAtLeast(60_000L)
                val elapsed = (now - event.startMillis).coerceIn(0L, total)
                RangedValueComplicationData.Builder(
                    value = elapsed.toFloat(),
                    min = 0f,
                    max = total.toFloat(),
                    contentDescription = PlainComplicationText.Builder(titleLine).build()
                )
                    .setText(PlainComplicationText.Builder(event.title).build())
                    .setTitle(PlainComplicationText.Builder(timeStr).build())
                    .build()
            }
            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(titleLine).build(),
                    contentDescription = PlainComplicationText.Builder(titleLine).build()
                )
                    .setTitle(PlainComplicationText.Builder(event.location ?: "").build())
                    .build()
            }
            else -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(event.title).build(),
                    contentDescription = PlainComplicationText.Builder(titleLine).build()
                )
                    .setTitle(PlainComplicationText.Builder(timeStr).build())
                    .build()
            }
        }

        listener.onComplicationData(data)
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val sampleTitle = "Team meeting"
        val sampleTime = "10:00 AM"
        return when (type) {
            ComplicationType.RANGED_VALUE -> RangedValueComplicationData.Builder(
                value = 10f, min = 0f, max = 30f,
                contentDescription = PlainComplicationText.Builder(sampleTitle).build()
            ).setText(PlainComplicationText.Builder(sampleTitle).build())
                .setTitle(PlainComplicationText.Builder(sampleTime).build())
                .build()
            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                text = PlainComplicationText.Builder("$sampleTime  $sampleTitle").build(),
                contentDescription = PlainComplicationText.Builder(sampleTitle).build()
            ).build()
            else -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(sampleTitle).build(),
                contentDescription = PlainComplicationText.Builder(sampleTitle).build()
            ).setTitle(PlainComplicationText.Builder(sampleTime).build()).build()
        }
    }
}
