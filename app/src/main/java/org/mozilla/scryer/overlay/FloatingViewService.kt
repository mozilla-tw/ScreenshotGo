/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.overlay

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.view.View
import android.view.ViewGroup

abstract class FloatingViewService : Service() {
    private var isRunning: Boolean = false
    private var floatingView: FloatingView? = null

    override fun onCreate() {
        super.onCreate()
        val foregroundNotification = getForegroundNotification()
        if (null != foregroundNotification) {
            val notificationId = getForegroundNotificationId()
            startForeground(notificationId, foregroundNotification)
        }
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
            initFloatingView()
        }

        return Service.START_STICKY
    }

    override fun onDestroy() {
        if (isRunning) {
            isRunning = false
            destroyFloatingView()
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun initFloatingView() {
        val context = applicationContext
        floatingView = FloatingView(context).apply {
            addView(onCreateView(context, this),
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
            addToWindow()
        }
    }

    private fun destroyFloatingView() {
        floatingView?.removeFromWindow()
    }

    open fun getForegroundNotificationId(): Int {
        return 123456789
    }


    open fun getForegroundNotification(): Notification? {
        return null
    }

    internal abstract fun onCreateView(context: Context, container: ViewGroup): View
}
