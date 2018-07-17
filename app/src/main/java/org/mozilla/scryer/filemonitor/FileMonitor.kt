/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.filemonitor

import android.util.Log

class FileMonitor(private val delegate: FileMonitorDelegate): FileMonitorDelegate by delegate {
    companion object {
        private const val TAG = "FileMonitor"
    }

    override fun startMonitor(listener: ChangeListener) {
        delegate.startMonitor(object : ChangeListener {
            override fun onChangeStart(path: String) {
                Log.d(TAG, "${delegate.javaClass.simpleName} onChangeStart, $path")
                listener.onChangeStart(path)
            }

            override fun onChangeFinish(path: String) {
                Log.d(TAG, "${delegate.javaClass.simpleName} onChangeFinish, $path")
                listener.onChangeFinish(path)
            }
        })
    }

    interface ChangeListener {
        fun onChangeStart(path: String) {}
        fun onChangeFinish(path: String) {}
    }
}

interface FileMonitorDelegate {
    fun startMonitor(listener: FileMonitor.ChangeListener)
}
