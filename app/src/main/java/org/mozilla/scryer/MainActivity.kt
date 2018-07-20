/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import org.mozilla.scryer.overlay.OverlayPermission

class MainActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_CODE_OVERLAY_PERMISSION = 1000
        private const val REQUEST_CODE_WRITE_EXTERNAL_PERMISSION = 1001
        private const val REQUEST_CODE_SCREEN_CAPTURE_PERMISSION = 1002
    }

    private var overlayRequested = false
    private var storageRequested = false
    private var screenCaptureRequested = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ensureOverlayPermission()
    }

    override fun onResume() {
        super.onResume()
        if (OverlayPermission.hasPermission(this)) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                if (!screenCaptureRequested) {
                    ensureScreenCapturePermission()
                }
            } else if (!storageRequested) {
                ensureStoragePermission()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CODE_OVERLAY_PERMISSION -> overlayRequested = true
            REQUEST_CODE_WRITE_EXTERNAL_PERMISSION -> {}
            REQUEST_CODE_SCREEN_CAPTURE_PERMISSION -> {
                val screenshotMenuIntent = Intent(applicationContext, ScryerService::class.java)
                screenshotMenuIntent.putExtra(ScryerService.SCREEN_CAPTURE_PERMISSION_RESULT_KEY, data)
                applicationContext.startService(screenshotMenuIntent)
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun ensureOverlayPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || OverlayPermission.hasPermission(this)) {
            overlayRequested = true

        } else if (!overlayRequested) {
            val intent = OverlayPermission.createPermissionIntent(this)
            startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION)
        }
    }

    private fun ensureStoragePermission() {
        storageRequested = true
        ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_CODE_WRITE_EXTERNAL_PERMISSION)
    }

    private fun ensureScreenCapturePermission() {
        screenCaptureRequested = true
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val it = mediaProjectionManager.createScreenCaptureIntent()
        startActivityForResult(it, REQUEST_CODE_SCREEN_CAPTURE_PERMISSION)
    }
}

fun setSupportActionBar(activity: FragmentActivity?, toolbar: Toolbar) {
    (activity as AppCompatActivity).setSupportActionBar(toolbar)
}

fun getSupportActionBar(activity: FragmentActivity?): ActionBar {
    val actionBar = (activity as AppCompatActivity).supportActionBar
    return actionBar?: throw RuntimeException("no action bar set")
}

