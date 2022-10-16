package com.live.location.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.live.location.R
import com.live.location.domain.GetLiveLocationUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import okhttp3.*
import okio.ByteString
import javax.inject.Inject

@AndroidEntryPoint
class LiveLocationService : LifecycleService() {

    @Inject
    lateinit var liveLocation: GetLiveLocationUseCase

    @Inject
    lateinit var okHttpClient: OkHttpClient

    private var mWebSocket: WebSocket? = null

    private val webSocketRequest by lazy {
        Request.Builder().url("ws://192.168.5.75:40510").build()
    }

    private val webSocketListener by lazy {

        object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(WEB_SOCKET_TAG, "onOpen")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(WEB_SOCKET_TAG, "onTextMessage")
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                Log.d(WEB_SOCKET_TAG, "onBytesMessage")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(WEB_SOCKET_TAG, "onClosing")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(WEB_SOCKET_TAG, "onClosed")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.d(WEB_SOCKET_TAG, t.message.toString())
                opeWebSocketConnection(retry = true)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        opeWebSocketConnection()
        liveLocationServiceIsRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> start()
            ACTION_STOP -> stop()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        closeWebSocketConnection()
        liveLocationServiceIsRunning = false
    }

    private fun start() {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Tracking Location...")
            .setContentText("Waiting for location...")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setDefaults(Notification.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Location",
                NotificationManager.IMPORTANCE_HIGH,
            )
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val notificationManager = getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        notificationManager.notify(NOTIFICATION_ID, notification.build())

        liveLocation()
            .catch { e -> e.printStackTrace() }
            .onEach { location ->
                location?.let {
                    val lat = it.latitude.toString()
                    val lng = it.longitude.toString()
                    val updatedNotification = notification.setContentText(
                        "Location: ($lat , $lng)"
                    )
                    notificationManager.notify(NOTIFICATION_ID, updatedNotification.build())

                    mWebSocket?.send("Location: ($lat , $lng)")
                }
            }.launchIn(lifecycleScope)

        startForeground(
            NOTIFICATION_ID,
            notification.build()
        )
    }

    private fun stop() {
        stopForeground(NOTIFICATION_ID)
        stopSelf()
    }

    fun opeWebSocketConnection(retry: Boolean = false) {
        lifecycleScope.launch(Dispatchers.IO) {
            mWebSocket?.let {
                closeWebSocketConnection()
            }
            if (retry) {
                delay(3000)
            }
            okHttpClient.apply {
                mWebSocket = newWebSocket(webSocketRequest, webSocketListener)
            }
        }
    }

    private fun closeWebSocketConnection() {
        lifecycleScope.launch(Dispatchers.IO) {
            mWebSocket?.close(1000, "serviceDestroyed")
            mWebSocket = null
        }
    }

    companion object {
        const val WEB_SOCKET_TAG = "WebSocket"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val NOTIFICATION_CHANNEL_ID = "location"
        var liveLocationServiceIsRunning by mutableStateOf(false)
    }
}