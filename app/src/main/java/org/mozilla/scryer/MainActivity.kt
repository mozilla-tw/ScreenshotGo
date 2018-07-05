/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity

import org.mozilla.scryer.overlay.ScreenshotMenuService
import org.mozilla.scryer.overlay.OverlayPermission

class MainActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_CODE_OVERLAY_PERMISSION = 1000
    }

    private var permissionRequested = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ensureOverlayPermission()
    }

    override fun onResume() {
        super.onResume()
        if (OverlayPermission.hasPermission(this)) {
            applicationContext.startService(Intent(applicationContext, ScreenshotMenuService::class.java))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
            permissionRequested = true
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun ensureOverlayPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || OverlayPermission.hasPermission(this)) {
            permissionRequested = true

        } else if (!permissionRequested) {
            val intent = OverlayPermission.createPermissionIntent(this)
            startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION)
        }
    }
}
