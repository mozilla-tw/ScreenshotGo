/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.permission

import android.content.Context

class PermissionFlow(private val context: Context, private val viewDelegate: ViewDelegate) {
    fun start() {
        if (PermissionHelper.hasStoragePermission(context)) {
            viewDelegate.onStorageGranted()
            checkOverlayPermission()
        } else {
            viewDelegate.askForStoragePermission()
        }
    }

    fun next() {
        start()
    }

    private fun checkOverlayPermission() {
        if (PermissionHelper.hasOverlayPermission(context)) {
            viewDelegate.onOverlayGranted()
        } else {
            viewDelegate.askForOverlayPermission()
        }
    }

    interface ViewDelegate {
        fun askForStoragePermission()
        fun askForOverlayPermission()

        fun onStorageGranted()
        fun onOverlayGranted()
    }
}
