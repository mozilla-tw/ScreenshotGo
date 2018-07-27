/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer

import android.annotation.TargetApi
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.support.v4.app.NotificationCompat
import org.mozilla.scryer.capture.ChooseCollectionActivity
import org.mozilla.scryer.capture.RequestCaptureActivity
import org.mozilla.scryer.capture.ScreenCaptureListener
import org.mozilla.scryer.capture.ScreenCaptureManager
import org.mozilla.scryer.filemonitor.FileMonitor
import org.mozilla.scryer.filemonitor.MediaProviderDelegate
import org.mozilla.scryer.overlay.OverlayPermission
import org.mozilla.scryer.overlay.ScreenshotButtonController
import org.mozilla.scryer.persistence.ScreenshotModel
import java.util.*


class ScryerService : Service(), ScreenshotButtonController.ClickListener, ScreenCaptureListener {
    companion object {
        // TODO: temp id
        private const val ID_FOREGROUND = 9487
        private const val ID_SCREENSHOT_DETECTED = 9488

        private const val ACTION_CAPTURE_SCREEN = "action_capture"
        private const val ACTION_STOP = "action_stop"
        private const val ACTION_LAUNCH_APP = "action_launch_app"

        private const val DELAY_CAPTURE_NOTIFICATION = 1000L
        private const val DELAY_CAPTURE_FAB = 0L
    }

    private var isRunning: Boolean = false
    private val floatingButtonController: ScreenshotButtonController by lazy {
        ScreenshotButtonController(applicationContext)
    }

    private var screenCapturePermissionIntent: Intent? = null
    private var screenCaptureManager: ScreenCaptureManager? = null
    private lateinit var requestCaptureFilter: IntentFilter
    private lateinit var requestCaptureReceiver: BroadcastReceiver

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
                postNotification(getScreenshotDetectedNotification())
                val model = ScreenshotModel(UUID.randomUUID().toString(), path,
                        System.currentTimeMillis(),
                        "")
                ScryerApplication.getScreenshotRepository().addScreenshot(listOf(model))
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
        if (screenCapturePermissionIntent != null) {
            screenCaptureManager?.captureScreen()
        } else {

            requestCaptureFilter = IntentFilter(RequestCaptureActivity.getResultBroadcastAction(applicationContext))
            requestCaptureReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    applicationContext.unregisterReceiver(requestCaptureReceiver)

                    //val resultCode = intent.getIntExtra(RequestCaptureActivity.RESULT_EXTRA_CODE,
                    //        Activity.RESULT_CANCELED)
                    screenCapturePermissionIntent = intent.getParcelableExtra(RequestCaptureActivity.RESULT_EXTRA_DATA)
                    screenCaptureManager = ScreenCaptureManager(applicationContext, screenCapturePermissionIntent!!, this@ScryerService)

                    if (intent.getBooleanExtra(RequestCaptureActivity.RESULT_EXTRA_PROMPT_SHOWN, true)) {
                        // Delay capture until after the permission dialog is gone.
                        handler.postDelayed({ screenCaptureManager?.captureScreen() }, 500)
                    } else {
                        screenCaptureManager?.captureScreen()
                    }
                }
            }

            applicationContext.registerReceiver(requestCaptureReceiver, requestCaptureFilter)
            applicationContext.startActivity(Intent(applicationContext, RequestCaptureActivity::class.java))
        }
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
        return ID_FOREGROUND
    }

    private fun getForegroundNotification(): Notification? {
        val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createForegroundChannel()
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

    private fun getScreenshotDetectedNotification(): Notification {
        val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createMessageChannel()
        } else {
            ""
        }

        val tapIntent = Intent(ACTION_LAUNCH_APP)
        tapIntent.setClass(this, MainActivity::class.java)
        val tapPendingIntent = PendingIntent.getActivity(this, 0, tapIntent, 0)

        return NotificationCompat.Builder(this, channelId)
                .setCategory(Notification.CATEGORY_PROMO)
                .setSmallIcon(android.R.drawable.ic_menu_upload)
                .setContentTitle("Smart Screenshot")
                .setContentText("Click to collect captured screens")
                .setContentIntent(tapPendingIntent)
                .build()
    }

    private fun postNotification(notification: Notification) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(ID_SCREENSHOT_DETECTED, notification)
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createForegroundChannel(): String {
        val channelId = "foreground_channel"
        val channelName = "ScreenshotPlus Service"
        val channel = NotificationChannel(channelId, channelName,
                NotificationManager.IMPORTANCE_NONE)

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
        return channelId
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createMessageChannel(): String {
        val channelId = "message_channel"
        val channelName = "ScreenshotPlus Message"
        val channel = NotificationChannel(channelId, channelName,
                NotificationManager.IMPORTANCE_HIGH)

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
        return channelId
    }
}
