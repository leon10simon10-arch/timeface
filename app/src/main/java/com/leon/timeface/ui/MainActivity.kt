package com.leon.timeface.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * Launcher activity. Its only job is to request READ_CALENDAR permission
 * so the watch face's complication data source can read upcoming events.
 * Open this once after installing, tap "Grant access", then set TimeFace
 * as your watch face and add the "Next event" complication to its slot.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var status: TextView

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        updateStatus(granted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 64, 32, 32)
        }

        val title = TextView(this).apply {
            text = "TimeFace"
            textSize = 20f
        }
        status = TextView(this).apply {
            textSize = 14f
            setPadding(0, 16, 0, 24)
        }
        val button = Button(this).apply {
            text = "Grant calendar access"
            setOnClickListener {
                requestPermission.launch(Manifest.permission.READ_CALENDAR)
            }
        }

        root.addView(title)
        root.addView(status)
        root.addView(button)
        setContentView(root)

        val hasPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        updateStatus(hasPermission)
    }

    private fun updateStatus(granted: Boolean) {
        status.text = if (granted) {
            "Calendar access granted. Set TimeFace as your watch face from the watch face picker."
        } else {
            "Calendar access is needed to show your next event on the watch face."
        }
    }
}
