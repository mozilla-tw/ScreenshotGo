/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.overlay

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.RelativeLayout

// TODO:
// state save/restore (e.g. view position)
// parameterize position

class FloatingView(context: Context) : RelativeLayout(context) {
    private var isAddedToWindow = false
    private var windowController: WindowController =
            WindowController(context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)

    init {
        init()
    }

    private fun init() {
        //setFocusableInTouchMode(true); // hardware back button presses.
    }

    fun addToWindow() {
        if (isAddedToWindow) {
            return
        }

        windowController.addView(200, 200, true, this)
        isAddedToWindow = true

        post {
            val metrics = DisplayMetrics()
            display.getMetrics(metrics)
            windowController.moveViewTo(this@FloatingView,
                    metrics.widthPixels - 190,
                    metrics.heightPixels / 3)
        }
    }

    fun removeFromWindow() {
        isAddedToWindow = false
        windowController.removeView(this)
    }
}
