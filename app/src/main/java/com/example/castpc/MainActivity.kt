package com.example.castpc

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var ipBox: EditText
    private lateinit var btn: FrameLayout
    private lateinit var btnText: TextView
    private lateinit var btnArrow: TextView
    private lateinit var statusText: TextView
    private lateinit var statusDot: View
    private lateinit var landscapeBtn: RadioButton
    private lateinit var portraitBtn: RadioButton
    private lateinit var settingsBody: LinearLayout
    private lateinit var settingsArrow: TextView

    // Stream settings: saved in SharedPreferences and sent to StreamService when casting starts
    private val resOptions = intArrayOf(480, 540, 720, 1080)      // short edge, px
    private val fpsOptions = intArrayOf(30, 60)
    private val audioOptions = intArrayOf(64, 96, 128, 192)       // kbps
    private val bufferOptions = intArrayOf(30, 50, 100)           // send-queue items (~0.3 / 0.5 / 1 s)
    private var resIdx = 0
    private var fpsIdx = 0
    private var audioIdx = 0
    private var bufferIdx = 0
    private var videoKbps = StreamService.DEF_VIDEO_KBPS
    private var autoAdjust = StreamService.DEF_AUTO

    private val blue = Color.parseColor("#2F8BFF")
    private val cyan = Color.parseColor("#3FD0FF")
    private val slate = Color.parseColor("#8FA3C7")

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun dpf(v: Float) = v * resources.displayMetrics.density
    private fun withAlpha(c: Int, a: Int) = Color.argb(a, Color.red(c), Color.green(c), Color.blue(c))
    private fun lp(w: Int, h: Int, top: Int = 0) =
        LinearLayout.LayoutParams(w, h).apply { topMargin = dp(top) }

    private fun rect(fill: Int, radiusDp: Int, strokeDp: Float = 0f, stroke: Int = 0) =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpf(radiusDp.toFloat())
            setColor(fill)
            if (strokeDp > 0f) setStroke(maxOf(1, dpf(strokeDp).toInt()), stroke)
        }

    // Dark panel with a soft neon edge
    private fun glowBox(fill: Int, radiusDp: Int, color: Int): LayerDrawable {
        val outer = rect(Color.TRANSPARENT, radiusDp + 6, 5f, withAlpha(color, 35))
        val mid = rect(Color.TRANSPARENT, radiusDp + 3, 3f, withAlpha(color, 80))
        val inner = rect(fill, radiusDp, 1.5f, withAlpha(color, 230))
        return LayerDrawable(arrayOf(outer, mid, inner)).apply {
            setLayerInset(1, dp(3), dp(3), dp(3), dp(3))
            setLayerInset(2, dp(6), dp(6), dp(6), dp(6))
        }
    }

    private fun buttonBg(on: Boolean): LayerDrawable {
        val c = if (on) Color.parseColor("#FF4D4D") else blue
        val colors = if (on)
            intArrayOf(0xFFB71C1C.toInt(), 0xFFFF5252.toInt(), 0xFFB71C1C.toInt())
        else
            intArrayOf(0xFF1F5CFF.toInt(), 0xFF2FA3FF.toInt(), 0xFF1F5CFF.toInt())
        val outer = rect(Color.TRANSPARENT, 34, 5f, withAlpha(c, 35))
        val mid = rect(Color.TRANSPARENT, 31, 3f, withAlpha(c, 80))
        val pill = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors).apply {
            cornerRadius = dpf(28f)
            setStroke(dp(1), Color.argb(150, 255, 255, 255))
        }
        return LayerDrawable(arrayOf(outer, mid, pill)).apply {
            setLayerInset(1, dp(3), dp(3), dp(3), dp(3))
            setLayerInset(2, dp(6), dp(6), dp(6), dp(6))
        }
    }

    private fun iconRow(res: Int, label: String, topDp: Int, size: Float = 14f) =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = lp(-1, -2, topDp)
            addView(ImageView(this@MainActivity).apply {
                setImageResource(res)
                layoutParams = LinearLayout.LayoutParams(dp(22), dp(22))
            })
            addView(TextView(this@MainActivity).apply {
                text = label
                setTextColor(slate)
                textSize = size
                setPadding(dp(12), 0, 0, 0)
            })
        }

    private fun radio(label: String) = RadioButton(this).apply {
        text = label
        textSize = 16f
        setTextColor(Color.WHITE)
        buttonTintList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(blue, slate)
        )
    }

    private fun fadeLine(toRight: Boolean) = View(this).apply {
        val colors = intArrayOf(Color.TRANSPARENT, withAlpha(blue, 170))
        if (!toRight) colors.reverse()
        background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors)
        layoutParams = LinearLayout.LayoutParams(0, dp(1), 1f)
    }

    private fun sectionLabel(text: String, topDp: Int) = TextView(this).apply {
        this.text = text
        setTextColor(slate)
        textSize = 13f
        layoutParams = lp(-1, -2, topDp)
    }

    // A row of equal-width pill buttons where exactly one is selected
    private fun chipRow(labels: List<String>, selected: Int, topDp: Int, onPick: (Int) -> Unit): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = lp(-1, -2, topDp)
        }
        val chips = mutableListOf<TextView>()
        fun paint(sel: Int) {
            chips.forEachIndexed { i, t ->
                val on = i == sel
                t.background = rect(
                    if (on) withAlpha(blue, 90) else Color.TRANSPARENT,
                    12, 1.5f, if (on) blue else withAlpha(slate, 120)
                )
                t.setTextColor(if (on) Color.WHITE else slate)
            }
        }
        labels.forEachIndexed { i, label ->
            val chip = TextView(this).apply {
                text = label
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(dp(2), dp(10), dp(2), dp(10))
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply {
                    if (i > 0) leftMargin = dp(8)
                }
                setOnClickListener { paint(i); onPick(i) }
            }
            chips += chip
            row.addView(chip)
        }
        paint(selected)
        return row
    }

    private fun setEnabledDeep(v: View, on: Boolean) {
        v.isEnabled = on
        if (v is ViewGroup) for (i in 0 until v.childCount) setEnabledDeep(v.getChildAt(i), on)
    }

    private fun indexOrDefault(options: IntArray, value: Int, default: Int): Int {
        val i = options.indexOf(value)
        return if (i >= 0) i else options.indexOf(default)
    }

    private fun loadSettings(p: SharedPreferences) {
        resIdx = indexOrDefault(resOptions, p.getInt("side", StreamService.DEF_SIDE), StreamService.DEF_SIDE)
        fpsIdx = indexOrDefault(fpsOptions, p.getInt("fps", StreamService.DEF_FPS), StreamService.DEF_FPS)
        audioIdx = indexOrDefault(audioOptions, p.getInt("abitrate", StreamService.DEF_AUDIO_KBPS), StreamService.DEF_AUDIO_KBPS)
        bufferIdx = indexOrDefault(bufferOptions, p.getInt("buffer", StreamService.DEF_QUEUE), StreamService.DEF_QUEUE)
        videoKbps = p.getInt("vbitrate", StreamService.DEF_VIDEO_KBPS).coerceIn(1000, 20000)
        autoAdjust = p.getBoolean("auto", StreamService.DEF_AUTO)
    }

    private fun saveSettings() {
        getSharedPreferences("castpc", MODE_PRIVATE).edit()
            .putInt("side", resOptions[resIdx])
            .putInt("fps", fpsOptions[fpsIdx])
            .putInt("vbitrate", videoKbps)
            .putInt("abitrate", audioOptions[audioIdx])
            .putInt("buffer", bufferOptions[bufferIdx])
            .putBoolean("auto", autoAdjust)
            .apply()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#040813")
        window.navigationBarColor = Color.parseColor("#020510")
        val prefs = getSharedPreferences("castpc", MODE_PRIVATE)
        loadSettings(prefs)

        // ---- header ----
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.logo_cast)
            layoutParams = lp(dp(84), dp(84), 16)
        }
        val title = TextView(this).apply {
            val s = SpannableString("Cast2PC")
            s.setSpan(ForegroundColorSpan(blue), 4, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            text = s
            setTextColor(Color.WHITE)
            textSize = 44f
            typeface = Typeface.create("sans-serif-black", Typeface.ITALIC)
            setShadowLayer(dpf(10f), 0f, 0f, withAlpha(blue, 200))
            layoutParams = lp(-2, -2, 4)
        }
        val subtitle = TextView(this).apply {
            text = "Stream your phone to your PC"
            setTextColor(slate)
            textSize = 15f
            layoutParams = lp(-2, -2, 2)
        }
        val accent = View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.TRANSPARENT, cyan, Color.TRANSPARENT)
            )
            layoutParams = lp(dp(140), dp(2), 14)
        }

        // ---- card ----
        ipBox = EditText(this).apply {
            hint = "192.168.1.10"
            setHintTextColor(Color.parseColor("#4A5A7A"))
            setTextColor(Color.WHITE)
            textSize = 22f
            setText(prefs.getString("ip", ""))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine()
            background = null
            setPadding(0, dp(6), 0, dp(10))
            layoutParams = lp(-1, -2, 6)
        }
        val ipLine = View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(blue, cyan, blue)
            )
            layoutParams = lp(-1, dp(2), 0)
        }

        landscapeBtn = radio("Landscape")
        portraitBtn = radio("Portrait")
        val group = RadioGroup(this).apply {
            orientation = RadioGroup.HORIZONTAL
            layoutParams = lp(-1, -2, 8)
            addView(landscapeBtn, RadioGroup.LayoutParams(-2, -2).apply { rightMargin = dp(40) })
            addView(portraitBtn, RadioGroup.LayoutParams(-2, -2))
        }
        landscapeBtn.isChecked = prefs.getBoolean("landscape", true)
        portraitBtn.isChecked = !landscapeBtn.isChecked

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = glowBox(Color.argb(235, 8, 18, 42), 24, blue)
            setPadding(dp(30), dp(26), dp(30), dp(24))
            layoutParams = lp(-1, -2, 28)
            addView(iconRow(R.drawable.ic_monitor, "PC IP address", 0))
            addView(ipBox)
            addView(ipLine)
            addView(iconRow(R.drawable.ic_gamepad, "Cast orientation", 26))
            addView(group)
            addView(iconRow(R.drawable.ic_info, "Match the orientation your game is in.", 6, 12f))
        }

        // ---- settings card (collapsed by default) ----
        val bitrateLabel = TextView(this).apply {
            setTextColor(slate)
            textSize = 13f
            layoutParams = lp(-1, -2, 20)
        }
        fun showBitrate() { bitrateLabel.text = "Video bitrate: ${videoKbps / 1000} Mbps" }
        showBitrate()
        val bitrateBar = SeekBar(this).apply {
            max = 19                                   // 1..20 Mbps
            progress = (videoKbps / 1000 - 1).coerceIn(0, 19)
            progressTintList = ColorStateList.valueOf(blue)
            thumbTintList = ColorStateList.valueOf(cyan)
            layoutParams = lp(-1, -2, 4)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, p: Int, fromUser: Boolean) {
                    videoKbps = (p + 1) * 1000
                    showBitrate()
                    if (fromUser) saveSettings()
                }
                override fun onStartTrackingTouch(bar: SeekBar?) {}
                override fun onStopTrackingTouch(bar: SeekBar?) {}
            })
        }
        val checkedState = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
        val autoSwitch = Switch(this).apply {
            text = "Auto-adjust bitrate"
            textSize = 15f
            setTextColor(Color.WHITE)
            isChecked = autoAdjust
            thumbTintList = ColorStateList(checkedState, intArrayOf(cyan, slate))
            trackTintList = ColorStateList(checkedState, intArrayOf(withAlpha(blue, 140), withAlpha(slate, 90)))
            layoutParams = lp(-1, -2, 22)
            setOnCheckedChangeListener { _, on -> autoAdjust = on; saveSettings() }
        }
        val resetLink = TextView(this).apply {
            text = "Reset to defaults"
            setTextColor(cyan)
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(4))
            layoutParams = lp(-1, -2, 18)
            setOnClickListener {
                getSharedPreferences("castpc", MODE_PRIVATE).edit()
                    .putString("ip", ipBox.text.toString().trim())
                    .putBoolean("landscape", landscapeBtn.isChecked)
                    .remove("side").remove("fps").remove("vbitrate")
                    .remove("abitrate").remove("buffer").remove("auto")
                    .apply()
                recreate()
            }
        }

        settingsBody = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            addView(sectionLabel("Resolution", 18))
            addView(chipRow(resOptions.map { "${it}p" }, resIdx, 8) { resIdx = it; saveSettings() })
            addView(sectionLabel("Frame rate", 18))
            addView(chipRow(fpsOptions.map { "$it fps" }, fpsIdx, 8) { fpsIdx = it; saveSettings() })
            addView(bitrateLabel)
            addView(bitrateBar)
            addView(sectionLabel("Audio bitrate (kbps)", 18))
            addView(chipRow(audioOptions.map { "$it" }, audioIdx, 8) { audioIdx = it; saveSettings() })
            addView(sectionLabel("Delay buffer", 18))
            addView(chipRow(listOf("Lowest", "Balanced", "Smooth"), bufferIdx, 8) { bufferIdx = it; saveSettings() })
            addView(iconRow(R.drawable.ic_info, "Lowest = least delay, but more dropped frames on weak Wi-Fi.", 8, 12f))
            addView(autoSwitch)
            addView(iconRow(R.drawable.ic_info, "Lowers quality on its own when the Wi-Fi can't keep up.", 6, 12f))
            addView(resetLink)
        }
        settingsArrow = TextView(this).apply {
            text = "\u25BE"
            setTextColor(slate)
            textSize = 18f
        }
        val settingsHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = lp(-1, -2, 0)
            addView(TextView(this@MainActivity).apply {
                text = "Stream settings"
                setTextColor(Color.WHITE)
                textSize = 16f
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            })
            addView(settingsArrow)
            setOnClickListener {
                val open = settingsBody.visibility != View.VISIBLE
                settingsBody.visibility = if (open) View.VISIBLE else View.GONE
                settingsArrow.text = if (open) "\u25B4" else "\u25BE"
            }
        }
        val settingsCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = glowBox(Color.argb(235, 8, 18, 42), 24, blue)
            setPadding(dp(26), dp(20), dp(26), dp(20))
            layoutParams = lp(-1, -2, 16)
            addView(settingsHeader)
            addView(settingsBody)
        }

        // ---- button ----
        btnText = TextView(this).apply {
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(-1, -1)
        }
        btnArrow = TextView(this).apply {
            textSize = 24f
            setTextColor(Color.WHITE)
            layoutParams = FrameLayout.LayoutParams(-2, -2).apply {
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
                rightMargin = dp(34)
            }
        }
        btn = FrameLayout(this).apply {
            layoutParams = lp(-1, dp(68), 26)
            isClickable = true
            setOnClickListener { toggle() }
            addView(btnText)
            addView(btnArrow)
        }

        // ---- status ----
        statusDot = View(this).apply {
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(slate) }
            layoutParams = LinearLayout.LayoutParams(dp(10), dp(10))
        }
        statusText = TextView(this).apply {
            setTextColor(slate)
            textSize = 15f
            setPadding(dp(14), 0, 0, 0)
        }
        val statusRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(6), 0, 0, 0)
            layoutParams = lp(-1, -2, 22)
            addView(statusDot)
            addView(statusText)
        }

        // ---- footer ----
        val credit = TextView(this).apply {
            val s = SpannableString("by: Aaron Doronio")
            s.setSpan(ForegroundColorSpan(blue), 4, s.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            text = s
            setTextColor(slate)
            textSize = 13f
            setPadding(dp(14), 0, dp(14), 0)
        }
        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = lp(-1, -2, 16)
            addView(fadeLine(true))
            addView(credit)
            addView(fadeLine(false))
        }
        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(32), dp(24), dp(20))
            addView(logo)
            addView(title)
            addView(subtitle)
            addView(accent)
            addView(card)
            addView(settingsCard)
            addView(btn)
            addView(statusRow)
            addView(spacer)
            addView(footer)
        }

        val bg = ImageView(this).apply {
            setImageResource(R.drawable.bg_main)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(content, FrameLayout.LayoutParams(-1, -1))
        }
        setContentView(FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#020510"))
            addView(bg, FrameLayout.LayoutParams(-1, -1))
            addView(scroll, FrameLayout.LayoutParams(-1, -1))
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
        btnText.text = if (on) "Stop casting" else "Start casting"
        btnArrow.text = if (on) "\u25A0" else "\u2192"
        btn.background = buttonBg(on)
        statusText.text = if (on) "Casting. Switch to your game." else "Ready."
        (statusDot.background as GradientDrawable)
            .setColor(if (on) Color.parseColor("#3DDC84") else slate)
        ipBox.isEnabled = !on
        landscapeBtn.isEnabled = !on
        portraitBtn.isEnabled = !on
        setEnabledDeep(settingsBody, !on)
        settingsBody.alpha = if (on) 0.45f else 1f
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
                putExtra("side", resOptions[resIdx])
                putExtra("fps", fpsOptions[fpsIdx])
                putExtra("vbitrate", videoKbps)
                putExtra("abitrate", audioOptions[audioIdx])
                putExtra("queue", bufferOptions[bufferIdx])
                putExtra("auto", autoAdjust)
            })
            btn.postDelayed({ updateUi() }, 800)
        }
    }
}