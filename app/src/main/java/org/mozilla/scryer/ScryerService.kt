/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer

import android.annotation.TargetApi
import android.app.*
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.support.v4.app.NotificationCompat
import org.mozilla.scryer.capture.ChooseCollectionActivity
import org.mozilla.scryer.capture.ScreenCaptureListener
import org.mozilla.scryer.capture.ScreenCaptureManager
import org.mozilla.scryer.filemonitor.FileMonitor
import org.mozilla.scryer.filemonitor.MediaProviderDelegate
import org.mozilla.scryer.overlay.OverlayPermission
import org.mozilla.scryer.overlay.ScreenshotButtonController


class ScryerService : Service(), ScreenshotButtonController.ClickListener, ScreenCaptureListener {
    companion object {
        // TODO: temp id
        private const val NOTIFICATION_ID_FOREGROUND = 9487
        private const val ACTION_CAPTURE_SCREEN = "action_capture"
        private const val ACTION_STOP = "action_stop"

        private const val DELAY_CAPTURE_NOTIFICATION = 1000L
        private const val DELAY_CAPTURE_FAB = 0L

        const val SCREEN_CAPTURE_PERMISSION_RESULT_KEY = "SCREEN_CAPTURE_PERMISSION_RESULT"
    }

    private var isRunning: Boolean = false
    private val floatingButtonController: ScreenshotButtonController by lazy {
        ScreenshotButtonController(applicationContext)
    }

    private lateinit var screenCapturePermissionIntent: Intent
    private val screenCaptureManager: ScreenCaptureManager by lazy {
        ScreenCaptureManager(applicationContext, screenCapturePermissionIntent, this)
    }

    private val fileMonitor: FileMonitor by lazy {
        //FileMonitor(FileObserverDelegate(Handler(Looper.getMainLooper())))
        FileMonitor(MediaProviderDelegate(this, Handler(Looper.getMainLooper())))
    }

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        startForeground(getForegroundNotificationId(), getForegroundNotification())
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!OverlayPermission.hasPermission(applicationContext)) {
            stopSelf()
            return Service.START_NOT_STICKY
        }

        if (null == intent) {
            stopSelf()
            return Service.START_NOT_STICKY
        }

        if (!isRunning) {
            isRunning = true
            initFloatingButton()
            initFileMonitors()
            screenCapturePermissionIntent = intent.extras.getParcelable(SCREEN_CAPTURE_PERMISSION_RESULT_KEY)

        } else when (intent.action) {
            ACTION_CAPTURE_SCREEN -> postTakeScreenshot(DELAY_CAPTURE_NOTIFICATION)
            ACTION_STOP -> {
                stopSelf()
                sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        if (isRunning) {
            isRunning = false
            floatingButtonController.destroy()
        }
        super.onDestroy()
    }

    private fun initFloatingButton() {
        floatingButtonController.setOnClickListener(this)
        floatingButtonController.init()
    }

    private fun initFileMonitors() {
        fileMonitor.startMonitor(object : FileMonitor.ChangeListener {
            override fun onChangeFinish(path: String) {
                startChooseCollectionActivity(path)
            }
        })
    }

    override fun onScreenshotButtonClicked() {
        postTakeScreenshot(DELAY_CAPTURE_FAB)
    }

    override fun onScreenshotButtonLongClicked() {
        stopSelf()
    }

    private fun postTakeScreenshot(delayed: Long) {
        handler.postDelayed({
            floatingButtonController.hide()
            takeScreenshot()
        }, delayed)
    }

    private fun takeScreenshot() {
        screenCaptureManager.captureScreen()
    }

    override fun onScreenShotTaken(path: String) {
        floatingButtonController.show()
        startChooseCollectionActivity(path)
    }

    private fun startChooseCollectionActivity(path: String) {
        val intent = Intent(this, ChooseCollectionActivity::class.java)
        intent.putExtra(ChooseCollectionActivity.EXTRA_PATH, path)
        intent.flags = intent.flags or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    private fun getForegroundNotificationId(): Int {
        return NOTIFICATION_ID_FOREGROUND
    }

    private fun getForegroundNotification(): Notification? {
        val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        } else {
            ""
        }

        val tapIntent = Intent(ACTION_CAPTURE_SCREEN)
        tapIntent.setClass(this, ScryerService::class.java)
        val tapPendingIntent = PendingIntent.getService(this, 0, tapIntent, 0)

        val stopIntent = Intent(ACTION_STOP)
        stopIntent.setClass(this, ScryerService::class.java)
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, 0)
        val stopAction = NotificationCompat.Action(android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                stopPendingIntent)

        val style = NotificationCompat.BigTextStyle()
        style.bigText("Tap to take screenshot")
        return NotificationCompat.Builder(this, channelId)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setContentTitle("Hello Screenshot+")
                .setContentText("Tap to take screenshot")
                .setContentIntent(tapPendingIntent)
                .setStyle(style)
                .addAction(stopAction)
                .build()
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): String {
        val channelId = "scryer_ongoing_id"
        val channelName = "Screenshot+ Service"
        val channel = NotificationChannel(channelId, channelName,
                NotificationManager.IMPORTANCE_NONE)

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
        return channelId
    }
}
