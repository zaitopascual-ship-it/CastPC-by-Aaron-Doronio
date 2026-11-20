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
    }

    private var display: RtmpDisplay? = null
    private val ui = Handler(Looper.getMainLooper())

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

        // Landscape 720p-class capture that keeps the phone's aspect ratio
        val dm = resources.displayMetrics
        val ratio = maxOf(dm.widthPixels, dm.heightPixels).toFloat() /
                minOf(dm.widthPixels, dm.heightPixels)
        val landscape = intent.getBooleanExtra("landscape", true)
        val side = 1080
        val long = ((side * ratio).toInt() / 2) * 2
        val w = if (landscape) long else side
        val h = if (landscape) side else long

        val d = RtmpDisplay(this, false, checker)
        d.setIntentResult(code, data)
        val ready = d.prepareInternalAudio(128_000, 44100, true, false, false) &&
                d.prepareVideo(w, h, 60, 16_000_000, 0, dm.densityDpi)
        if (!ready) { toast("Encoder setup failed"); stopSelf(); return START_NOT_STICKY }

        display = d
        d.startStream(url)
        running = true
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
        display?.let { if (it.isStreaming) it.stopStream() }
        display = null
        running = false
        super.onDestroy()
    }
}
