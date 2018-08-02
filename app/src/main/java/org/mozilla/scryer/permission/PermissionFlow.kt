/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.permission

import android.content.Context
import android.preference.PreferenceManager
import android.support.v4.app.FragmentActivity
import org.mozilla.scryer.MainActivity

class PermissionFlow(private var permissionState: PermissionStateProvider,
                     private var pageState: PageStateProvider,
                     private val viewDelegate: ViewDelegate) {
    companion object {
        private const val KEY_WELCOME_PAGE_SHOWN = "welcome_page_shown"
        private const val KEY_OVERLAY_PAGE_SHOWN = "overlay_page_shown"
        private const val KEY_CAPTURE_PAGE_SHOWN = "capture_page_shown"

        fun createDefaultPermissionProvider(activity: FragmentActivity?): PermissionStateProvider {
            return object : PermissionFlow.PermissionStateProvider {
                override fun isStorageGranted(): Boolean {
                    return activity?.let {
                        PermissionHelper.hasStoragePermission(it)
                    }?: false
                }

                override fun isOverlayGranted(): Boolean {
                    return activity?.let {
                        PermissionHelper.hasOverlayPermission(it)
                    }?: false
                }

                override fun shouldShowStorageRational(): Boolean {
                    return activity?.let {
                        PermissionHelper.shouldShowStorageRational(it)
                    }?: false
                }
            }
        }

        fun createDefaultPageStateProvider(context: Context?): PageStateProvider {
            return object : PermissionFlow.PageStateProvider {
                private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

                override fun isWelcomePageShown(): Boolean {
                    return prefs.getBoolean(KEY_WELCOME_PAGE_SHOWN, false)
                }

                override fun isOverlayPageShown(): Boolean {
                    return prefs.getBoolean(KEY_OVERLAY_PAGE_SHOWN, false)
                }

                override fun isCapturePageShown(): Boolean {
                    return prefs.getBoolean(KEY_CAPTURE_PAGE_SHOWN, false)
                }

                override fun setWelcomePageShown() {
                    updatePrefs(KEY_WELCOME_PAGE_SHOWN, true)
                }

                override fun setOverlayPageShown() {
                    updatePrefs(KEY_OVERLAY_PAGE_SHOWN, true)
                }

                override fun setCapturePageShown() {
                    updatePrefs(KEY_CAPTURE_PAGE_SHOWN, true)
                }

                private fun updatePrefs(key: String, value: Boolean) {
                    prefs.edit().putBoolean(key, value).apply()
                }
            }
        }
    }

    fun start() {
        startStorageFlow()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onPermissionResult(requestCode: Int, results: BooleanArray) {
        when (requestCode) {
            MainActivity.REQUEST_CODE_WRITE_EXTERNAL_PERMISSION -> {
                pageState.setWelcomePageShown()
            }
        }
    }

    private fun startStorageFlow() {
        if (permissionState.isStorageGranted()) {
            viewDelegate.onStorageGranted()
            startOverlayFlow()

        } else if (pageState.isWelcomePageShown()) {
            requestStoragePermission()

        } else {
            showWelcomePage()
        }
    }

    private fun startOverlayFlow() {
        val overlayShown = pageState.isOverlayPageShown()
        if (!overlayShown) {
            pageState.setOverlayPageShown()
        }

        if (permissionState.isOverlayGranted()) {
            viewDelegate.onOverlayGranted()

            if (!overlayShown) {
                startCaptureFlow()
            } else {
                viewDelegate.onPermissionFlowFinish()
            }

        } else if (!overlayShown) {
            viewDelegate.showOverlayPermissionView(Runnable {
                viewDelegate.requestOverlayPermission()
            }, Runnable {
                viewDelegate.onOverlayDenied()
                viewDelegate.onPermissionFlowFinish()
            })
        } else {
            viewDelegate.onOverlayDenied()
            viewDelegate.onPermissionFlowFinish()
        }
    }

    private fun startCaptureFlow() {
        if (!pageState.isCapturePageShown()) {
            viewDelegate.showCapturePermissionView(Runnable {
                viewDelegate.onPermissionFlowFinish()

            }, Runnable {
                viewDelegate.onPermissionFlowFinish()
            })
            pageState.setCapturePageShown()
        } else {
            viewDelegate.onPermissionFlowFinish()
        }
    }

    private fun showWelcomePage() {
        viewDelegate.showWelcomePage(Runnable {
            viewDelegate.requestStoragePermission()
        })
    }

    private fun requestStoragePermission() {
        val shouldShowRational = permissionState.shouldShowStorageRational()
        val title = if (shouldShowRational) {
            "oops! something wrong"
        } else {
            "go to setting and enable permission"
        }

        viewDelegate.showStoragePermissionView(title, Runnable {
            if (shouldShowRational) {
                viewDelegate.requestStoragePermission()
            } else {
                viewDelegate.launchSystemSettingPage()
            }
        })
    }

    interface ViewDelegate {
        fun showWelcomePage(action: Runnable)

        fun showStoragePermissionView(title: String, action: Runnable)
        fun showOverlayPermissionView(action: Runnable, negativeAction: Runnable)
        fun showCapturePermissionView(action: Runnable, negativeAction: Runnable)

        fun onStorageGranted()
        fun onOverlayGranted()
        fun onOverlayDenied()

        fun onPermissionFlowFinish()

        fun requestStoragePermission()
        fun requestOverlayPermission()

        fun launchSystemSettingPage()
    }

    interface PermissionStateProvider {
        fun isStorageGranted(): Boolean
        fun isOverlayGranted(): Boolean
        fun shouldShowStorageRational(): Boolean
    }

    interface PageStateProvider {
        fun isWelcomePageShown(): Boolean
        fun isOverlayPageShown(): Boolean
        fun isCapturePageShown(): Boolean

        fun setWelcomePageShown()
        fun setOverlayPageShown()
        fun setCapturePageShown()
    }
}
