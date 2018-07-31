/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat
import android.support.v4.app.FragmentActivity
import org.mozilla.scryer.MainActivity
import org.mozilla.scryer.overlay.OverlayPermission

class PermissionFlow(private val activity: FragmentActivity, private val viewDelegate: ViewDelegate,
                     private val prefs: SharedPreferences = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)) {
    companion object {
        private const val PREF_NAME = "perm_flow"

        private const val KEY_WELCOME_PAGE_SHOWN = "welcome_page_shown"
        private const val KEY_OVERLAY_PAGE_SHOWN = "overlay_page_shown"
        private const val KEY_CAPTURE_PAGE_SHOWN = "capture_page_shown"
    }

    fun start() {
        startStorageFlow()
    }

    private fun startStorageFlow() {
        if (PermissionHelper.hasStoragePermission(activity)) {
            viewDelegate.onStorageGranted()
            startOverlayFlow()

        } else if (isWelcomePageShown()) {
            requestStoragePermission()

        } else {
            showWelcomePage()
        }
    }

    private fun startOverlayFlow() {
        val overlayShown = isOverlayPageShown()

        if (PermissionHelper.hasOverlayPermission(activity)) {
            viewDelegate.onOverlayGranted()

            if (!overlayShown) {
                startCaptureFlow()
            }

        } else if (!overlayShown) {
            viewDelegate.askForOverlayPermission(Runnable {
                requestOverlayPermission()
            }, Runnable {
            })
            prefs.edit().putBoolean(KEY_OVERLAY_PAGE_SHOWN, true).apply()
        }
    }

    private fun startCaptureFlow() {
        if (!isCapturePageShown()) {
            viewDelegate.askForCapturePermission(Runnable {

            }, Runnable {

            })
            prefs.edit().putBoolean(KEY_CAPTURE_PAGE_SHOWN, true).apply()
        }
    }

    private fun showWelcomePage() {
        viewDelegate.showWelcomePage(Runnable {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    MainActivity.REQUEST_CODE_WRITE_EXTERNAL_PERMISSION)
            prefs.edit().putBoolean(KEY_WELCOME_PAGE_SHOWN, true).apply()
        })
    }

    private fun requestStoragePermission() {
        val shouldShowRational = PermissionHelper.shouldShowStorageRational(activity)
        val title = if (shouldShowRational) "oops! something wrong" else "go to setting and enable permission"
        viewDelegate.askForStoragePermission(title, Runnable {
            if (shouldShowRational) {
                ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        MainActivity.REQUEST_CODE_WRITE_EXTERNAL_PERMISSION)
            } else {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", activity.packageName, null)
                activity.startActivity(intent)
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestOverlayPermission() {
        val intent = OverlayPermission.createPermissionIntent(activity)
        activity.startActivityForResult(intent, MainActivity.REQUEST_CODE_OVERLAY_PERMISSION)
    }

    private fun isWelcomePageShown(): Boolean {
        return prefs.getBoolean(KEY_WELCOME_PAGE_SHOWN, false)
    }

    private fun isOverlayPageShown(): Boolean {
        return prefs.getBoolean(KEY_OVERLAY_PAGE_SHOWN, false)
    }

    private fun isCapturePageShown(): Boolean {
        return prefs.getBoolean(KEY_CAPTURE_PAGE_SHOWN, false)
    }

    interface ViewDelegate {
        fun showWelcomePage(action: Runnable)

        fun askForStoragePermission(title: String, action: Runnable)
        fun askForOverlayPermission(action: Runnable, negativeAction: Runnable)
        fun askForCapturePermission(action: Runnable, negativeAction: Runnable)

        fun onStorageGranted()
        fun onOverlayGranted()
    }
}
