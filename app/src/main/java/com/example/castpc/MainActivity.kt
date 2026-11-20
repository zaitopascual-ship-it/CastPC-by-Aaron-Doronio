package com.example.castpc

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.widget.*

class MainActivity : Activity() {

    private lateinit var ipBox: EditText
    private lateinit var btn: Button
    private lateinit var status: TextView
    private lateinit var landscapeBtn: RadioButton
    private lateinit var portraitBtn: RadioButton

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun label(s: String, size: Float, color: Int, topDp: Int = 0) =
        TextView(this).apply {
            this.text = s
            textSize = size
            setTextColor(color)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(topDp) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("castpc", MODE_PRIVATE)
        val grey = Color.parseColor("#9AA0A6")

        ipBox = EditText(this).apply {
            hint = "192.168.1.10"
            setHintTextColor(Color.parseColor("#5F6368"))
            setTextColor(Color.WHITE)
            setText(prefs.getString("ip", ""))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine()
            layoutParams = LinearLayout.LayoutParams(-1, -2)
        }

        landscapeBtn = RadioButton(this).apply { text = "Landscape"; setTextColor(Color.WHITE) }
        portraitBtn = RadioButton(this).apply { text = "Portrait"; setTextColor(Color.WHITE) }
        val group = RadioGroup(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(landscapeBtn)
            addView(portraitBtn)
            layoutParams = LinearLayout.LayoutParams(-1, -2)
        }
        landscapeBtn.isChecked = prefs.getBoolean("landscape", true)
        portraitBtn.isChecked = !landscapeBtn.isChecked

        btn = Button(this).apply {
            setTextColor(Color.WHITE)
            textSize = 16f
            isAllCaps = false
            setOnClickListener { toggle() }
            layoutParams = LinearLayout.LayoutParams(-1, dp(56)).apply { topMargin = dp(32) }
        }
        status = label("", 14f, grey, 16)

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#121212"))
            setPadding(dp(24), dp(48), dp(24), dp(24))
            addView(label("CastPC", 28f, Color.WHITE))
            addView(label("Stream your phone to your PC", 14f, grey, 4))
            addView(label("PC IP address", 13f, grey, 32))
            addView(ipBox)
            addView(label("Cast orientation", 13f, grey, 24))
            addView(group)
            addView(label("Match the orientation your game is in.", 12f, grey, 4))
            addView(btn)
            addView(status)
        })

        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33) perms += Manifest.permission.POST_NOTIFICATIONS
        requestPermissions(perms.toTypedArray(), 0)
    }

    override fun onResume() {
        super.onResume()
        updateUi()
    }

    private fun updateUi() {
        val on = StreamService.running
        btn.text = if (on) "Stop casting" else "Start casting"
        btn.background = GradientDrawable().apply {
            cornerRadius = dp(12).toFloat()
            setColor(Color.parseColor(if (on) "#C62828" else "#1E88E5"))
        }
        status.text = if (on) "Casting. Switch to your game." else "Ready."
        ipBox.isEnabled = !on
        landscapeBtn.isEnabled = !on
        portraitBtn.isEnabled = !on
    }

    private fun toggle() {
        if (StreamService.running) {
            stopService(Intent(this, StreamService::class.java))
            btn.postDelayed({ updateUi() }, 800)
            return
        }
        val ip = ipBox.text.toString().trim()
        if (ip.isEmpty()) { ipBox.error = "Enter your PC's IP"; return }
        getSharedPreferences("castpc", MODE_PRIVATE).edit()
            .putString("ip", ip)
            .putBoolean("landscape", landscapeBtn.isChecked)
            .apply()
        val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        @Suppress("DEPRECATION")
        startActivityForResult(mpm.createScreenCaptureIntent(), 1)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            val ip = ipBox.text.toString().trim()
            startForegroundService(Intent(this, StreamService::class.java).apply {
                putExtra("code", resultCode)
                putExtra("data", data)
                putExtra("url", "rtmp://$ip:1935/cod")
                putExtra("landscape", landscapeBtn.isChecked)
            })
            btn.postDelayed({ updateUi() }, 800)
        }
    }
}