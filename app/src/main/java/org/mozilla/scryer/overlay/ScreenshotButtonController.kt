/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.overlay

import android.arch.lifecycle.DefaultLifecycleObserver
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import org.mozilla.scryer.R
import org.mozilla.scryer.extension.dpToPx

class ScreenshotButtonController(private val context: Context) : DefaultLifecycleObserver {
    private var floatingContainer: FloatingView? = null
    private var clickListener: ClickListener? = null

    var view: View? = null

    fun setOnClickListener(listener: ClickListener) {
        this.clickListener = listener
    }

    fun init() {
        floatingContainer = FloatingView(context).apply {
            setOnClickListener { clickListener?.onScreenshotButtonClicked() }
            val size = 50f.dpToPx(context.resources.displayMetrics)
            addView(onCreateView(context, this), size, size)
            addToWindow()
        }
    }

    fun destroy() {
        floatingContainer?.removeFromWindow()
    }

    fun show() {
        view?.visibility = View.VISIBLE
    }

    fun hide() {
        view?.visibility = View.INVISIBLE
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onCreateView(context: Context, container: ViewGroup): android.view.View {
        val view = ImageView(context)
        view.setImageResource(R.mipmap.ic_launcher_round)
        view.scaleType = ImageView.ScaleType.CENTER_INSIDE
        this.view = view
        return view
    }

    interface ClickListener {
        fun onScreenshotButtonClicked()
        fun onScreenshotButtonLongClicked()
    }
}