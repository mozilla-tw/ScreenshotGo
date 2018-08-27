/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.filemonitor

import android.os.Environment
import android.os.FileObserver
import android.os.Handler
import java.io.File

class FileObserverDelegate(private val handler: Handler) : FileMonitorDelegate {

    private var observer: FileObserver? = null

    override fun startMonitor(listener: FileMonitor.ChangeListener) {
        val monitorDir = "${Environment.getExternalStorageDirectory()}" +
                File.separator + "Pictures" +
                File.separator + "Screenshots"
        observer = object : FileObserver(monitorDir,
                FileObserver.CREATE or FileObserver.CLOSE_WRITE) {
            override fun onEvent(event: Int, path: String?) {
                path?.apply {
                    val filePath = monitorDir + File.separator + path
                    val eventCode = event and FileObserver.ALL_EVENTS
                    when (eventCode) {
                        FileObserver.CREATE -> handler.post { listener.onChangeStart(filePath) }
                        FileObserver.CLOSE_WRITE -> handler.post { listener.onChangeFinish(filePath) }
                    }
                }
            }
        }.apply {
            startWatching()
        }
    }

    override fun stopMonitor() {
        observer?.stopWatching()
    }
}