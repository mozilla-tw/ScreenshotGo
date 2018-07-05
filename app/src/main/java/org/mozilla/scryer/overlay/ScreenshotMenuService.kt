/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.overlay

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import org.mozilla.scryer.R

class ScreenshotMenuService : FloatingViewService() {
    companion object {
        // TODO: temp id
        private const val NOTIFICATION_ID_FOREGROUND = 9487
    }

    private var screenShotButton: View? = null

    override fun onCreateView(context: Context, container: ViewGroup): View {
        val view = ImageView(context)
        view.setImageResource(R.mipmap.ic_launcher_round)
        view.scaleType = ImageView.ScaleType.CENTER_INSIDE
        view.setOnClickListener { _ -> onScreenshotButtonClicked() }
        view.setOnLongClickListener { _ ->
            stopSelf()
            true
        }
        screenShotButton = view
        return view
    }

    override fun getForegroundNotificationId(): Int {
        return NOTIFICATION_ID_FOREGROUND
    }

    override fun getForegroundNotification(): Notification? {
        val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        } else {
            ""
        }

        return NotificationCompat.Builder(this, channelId)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setContentTitle("Hello Screenshot+")
                .setContentText("Screenshot+ is running")
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

    private fun onScreenshotButtonClicked() {
        screenShotButton?.visibility = View.INVISIBLE
        takeScreenshot()
    }

    private fun takeScreenshot() {
        screenShotButton?.postDelayed({ this.onScreenShotTaken() }, 1500)
    }

    private fun onScreenShotTaken() {
        screenShotButton?.visibility = View.VISIBLE
    }
}
