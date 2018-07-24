/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.overlay

import android.content.Context
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.RelativeLayout

class Screen(context: Context, private val windowCtrl: WindowController) {
    private val onLayoutChangeListener = View.OnLayoutChangeListener {
        _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
        if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
            onBoundaryUpdateListener?.run()
        }
    }

    val containerView = object : RelativeLayout(context) {
        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            addOnLayoutChangeListener(onLayoutChangeListener)
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            removeOnLayoutChangeListener(onLayoutChangeListener)
        }
    }

    val width: Int
        get() = containerView.width

    val height: Int
        get() = containerView.height

    var onBoundaryUpdateListener: Runnable? = null

    init {
        windowCtrl.addView(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, false,
                containerView)
    }

    fun addView(view: FloatingView, width: Int, height: Int) {
        containerView.addView(view, width, height)
    }

    fun detachFromWindow() {
        windowCtrl.removeView(containerView)
    }
}