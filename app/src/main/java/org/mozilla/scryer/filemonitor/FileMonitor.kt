/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.filemonitor

import android.os.Handler

class FileMonitor(var delegate: FileMonitorDelegate): FileMonitorDelegate by delegate {
    interface ChangeListener {
        fun onChangeStart(path: String) {}
        fun onChangeFinish(path: String) {}
    }
}

interface FileMonitorDelegate {
    fun startMonitor(handler: Handler, listener: FileMonitor.ChangeListener)
}
