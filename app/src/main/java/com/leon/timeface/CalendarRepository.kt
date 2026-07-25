package com.leon.timeface

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat

data class UpcomingEvent(
    val title: String,
    val location: String?,
    val startMillis: Long,
    val endMillis: Long
)

/**
 * Reads the next couple of events from the on-device calendar provider.
 * Requires READ_CALENDAR, granted via MainActivity.
 */
object CalendarRepository {

    fun hasPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** Returns up to [limit] upcoming (or currently in-progress) events, soonest first. */
    fun upcomingEvents(context: Context, limit: Int = 5): List<UpcomingEvent> {
        if (!hasPermission(context)) return emptyList()

        val now = System.currentTimeMillis()
        val windowEnd = now + 24L * 60 * 60 * 1000 // look 24h ahead

        val uriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        android.content.ContentUris.appendId(uriBuilder, now)
        android.content.ContentUris.appendId(uriBuilder, windowEnd)

        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY
        )

        val results = mutableListOf<UpcomingEvent>()
        try {
            context.contentResolver.query(
                uriBuilder.build(),
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )?.use { cursor ->
                val titleIdx = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
                val locIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)
                val beginIdx = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
                val endIdx = cursor.getColumnIndex(CalendarContract.Instances.END)
                val allDayIdx = cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY)

                while (cursor.moveToNext() && results.size < limit) {
                    val allDay = allDayIdx >= 0 && cursor.getInt(allDayIdx) != 0
                    if (allDay) continue // skip all-day entries, not useful on a watch face

                    val end = if (endIdx >= 0) cursor.getLong(endIdx) else continue
                    if (end < now) continue // already finished

                    results.add(
                        UpcomingEvent(
                            title = if (titleIdx >= 0) cursor.getString(titleIdx) ?: "Event" else "Event",
                            location = if (locIdx >= 0) cursor.getString(locIdx) else null,
                            startMillis = if (beginIdx >= 0) cursor.getLong(beginIdx) else now,
                            endMillis = end
                        )
                    )
                }
            }
        } catch (_: SecurityException) {
            // Permission revoked between the check and the query; just return nothing.
        }
        return results
    }
}
