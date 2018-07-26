/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer

import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import org.mozilla.scryer.permission.PermissionViewModel

class MainActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_CODE_OVERLAY_PERMISSION = 1000
        const val REQUEST_CODE_WRITE_EXTERNAL_PERMISSION = 1001
        private const val REQUEST_CODE_SCREEN_CAPTURE_PERMISSION = 1002
    }

    private var screenCaptureRequested = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CODE_SCREEN_CAPTURE_PERMISSION -> {
                val screenshotMenuIntent = Intent(applicationContext, ScryerService::class.java)
                screenshotMenuIntent.putExtra(ScryerService.SCREEN_CAPTURE_PERMISSION_RESULT_KEY, data)
                applicationContext.startService(screenshotMenuIntent)
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val viewModel = ViewModelProviders.of(this).get(PermissionViewModel::class.java)
        viewModel.permission(requestCode).onResult(grantResults)
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

