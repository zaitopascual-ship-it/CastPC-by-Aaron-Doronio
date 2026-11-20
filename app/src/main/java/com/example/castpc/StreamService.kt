package com.example.castpc

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import com.pedro.common.ConnectChecker
import com.pedro.library.rtmp.RtmpDisplay

class StreamService : Service() {

    companion object {
        @Volatile var running = false

        // Defaults (the app's settings panel overrides these through the start Intent)
        const val DEF_SIDE = 1080          // short edge of the capture, in px
        const val DEF_FPS = 60
        const val DEF_VIDEO_KBPS = 8_000
        const val DEF_AUDIO_KBPS = 128
        const val DEF_QUEUE = 50           // send-queue items, see below
        const val DEF_AUTO = true

        // RootEncoder's own default send queue holds 200 items (video + audio), about 2 seconds
        // at 60 fps. When the link is even slightly too slow the queue sits full, and that
        // backlog IS the stream delay. A smaller queue caps the delay (30 items is roughly
        // 0.3 s, 50 roughly 0.5 s, 100 roughly 1 s) at the cost of dropping frames sooner on
        // bad Wi-Fi.

        // Shorter keyframe interval so the picture recovers fast after a dropped frame.
        private const val KEYFRAME_SECONDS = 1
        private const val CHECK_INTERVAL_MS = 500L
    }

    private var display: RtmpDisplay? = null
    private val ui = Handler(Looper.getMainLooper())

    // Adaptive bitrate state. The bitrate chosen in the app is the ceiling; when auto-adjust is
    // on, the stream backs off below it if the Wi-Fi can't keep up, then creeps back up.
    private var autoAdjust = DEF_AUTO
    private var maxBitrate = DEF_VIDEO_KBPS * 1000
    private var minBitrate = 1_000_000
    private var bitrate = maxBitrate
    private var lastDropped = 0L
    private var healthyTicks = 0

    // Runs every CHECK_INTERVAL_MS while casting. If frames back up in the send queue, ask for
    // a fresh keyframe (when frames were lost) and, with auto-adjust on, lower the bitrate. If
    // the link has been clear for a while, raise the bitrate again.
    private val monitor = object : Runnable {
        override fun run() {
            val d = display ?: return
            val client = d.getStreamClient()

            val dropped = client.getDroppedVideoFrames()
            val newDrops = dropped > lastDropped
            lastDropped = dropped

            if (newDrops || client.hasCongestion(30f)) {
                healthyTicks = 0
                if (newDrops) d.requestKeyFrame()
                if (autoAdjust) setBitrate(d, (bitrate * 0.75).toInt())
            } else if (autoAdjust && client.getItemsInCache() <= 3) {
                if (++healthyTicks >= 6) {
                    healthyTicks = 0
                    setBitrate(d, bitrate + 500_000)
                }
            } else {
                healthyTicks = 0
            }
            ui.postDelayed(this, CHECK_INTERVAL_MS)
        }
    }

    private fun setBitrate(d: RtmpDisplay, target: Int) {
        val next = target.coerceIn(minBitrate, maxBitrate)
        if (next == bitrate) return
        bitrate = next
        d.setVideoBitrateOnFly(next)
    }

    private fun toast(msg: String) {
        ui.post { Toast.makeText(this, msg, Toast.LENGTH_LONG).show() }
    }

    private val checker = object : ConnectChecker {
        override fun onConnectionStarted(url: String) {}
        override fun onConnectionSuccess() = toast("Connected - casting")
        override fun onConnectionFailed(reason: String) {
            toast("Connection failed: $reason")
            ui.post { stopSelf() }
        }
        override fun onNewBitrate(bitrate: Long) {}
        override fun onDisconnect() = toast("Disconnected")
        override fun onAuthError() = toast("Auth error")
        override fun onAuthSuccess() {}
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY
        startAsForeground()

        val code = intent.getIntExtra("code", 0)
        val data: Intent? = if (Build.VERSION.SDK_INT >= 33)
            intent.getParcelableExtra("data", Intent::class.java)
        else @Suppress("DEPRECATION") intent.getParcelableExtra("data")
        val url = intent.getStringExtra("url")
        if (data == null || url == null) { stopSelf(); return START_NOT_STICKY }

        // Settings chosen in the app
        val side = intent.getIntExtra("side", DEF_SIDE)
        val fps = intent.getIntExtra("fps", DEF_FPS)
        val videoKbps = intent.getIntExtra("vbitrate", DEF_VIDEO_KBPS)
        val audioKbps = intent.getIntExtra("abitrate", DEF_AUDIO_KBPS)
        val queueItems = intent.getIntExtra("queue", DEF_QUEUE)
        autoAdjust = intent.getBooleanExtra("auto", DEF_AUTO)

        maxBitrate = videoKbps * 1000
        minBitrate = maxOf(1_000_000, maxBitrate / 4).coerceAtMost(maxBitrate)
        bitrate = maxBitrate

        // Capture that keeps the phone's aspect ratio (side is the short edge)
        val dm = resources.displayMetrics
        val ratio = maxOf(dm.widthPixels, dm.heightPixels).toFloat() /
                minOf(dm.widthPixels, dm.heightPixels)
        val landscape = intent.getBooleanExtra("landscape", true)
        val long = ((side * ratio).toInt() / 2) * 2
        val w = if (landscape) long else side
        val h = if (landscape) side else long

        val d = RtmpDisplay(this, false, checker)
        d.setIntentResult(code, data)
        val ready = d.prepareInternalAudio(audioKbps * 1000, 44100, true, false, false) &&
                d.prepareVideo(w, h, fps, bitrate, 0, dm.densityDpi, -1, -1, KEYFRAME_SECONDS)
        if (!ready) { toast("Encoder setup failed"); stopSelf(); return START_NOT_STICKY }

        // Must happen before startStream(): shrink the send queue so backlog can't build up,
        // and stop logging every packet.
        d.getStreamClient().apply {
            setLogs(false)
            resizeCache(queueItems)
        }

        display = d
        d.startStream(url)
        running = true

        lastDropped = 0L
        healthyTicks = 0
        ui.postDelayed(monitor, CHECK_INTERVAL_MS)
        return START_NOT_STICKY
    }

    private fun startAsForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("cast", "Casting", NotificationManager.IMPORTANCE_LOW)
        )
        val n: Notification = Notification.Builder(this, "cast")
            .setContentTitle("Casting to PC")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()
        startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
    }

    override fun onDestroy() {
        ui.removeCallbacks(monitor)
        display?.let { if (it.isStreaming) it.stopStream() }
        display = null
        running = false
        super.onDestroy()
    }
}