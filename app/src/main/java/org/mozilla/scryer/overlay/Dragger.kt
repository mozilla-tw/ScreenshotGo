/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.overlay

import android.content.Context
import android.view.View
import android.view.ViewGroup

class Dragger(private val context: Context,
              private val targetView: View,
              private val width: Int,
              private val height: Int,
              private val windowCtrl: WindowController) {

    private lateinit var dragView: View
    private lateinit var dragHelper: DragHelper

    var dragListener: DragHelper.DragListener? = null

    init {
        initDragView()
    }

    fun updatePosition() {
        dragView.parent?.let {
            windowCtrl.moveViewTo(dragView, targetView.x.toInt(), targetView.y.toInt())
        }
    }

    private fun initDragView() {
        this.dragView = View(context)
        this.dragView.layoutParams = ViewGroup.LayoutParams(width, height)
        //this.dragView.setBackgroundColor(Color.parseColor("#88ff0000"))

        this.dragHelper = DragHelper(dragView, windowCtrl)
        this.dragHelper.dragListener = object : DragHelper.DragListener {
            override fun onTap() {
                dragListener?.onTap()
            }

            override fun onLongPress() {
                dragListener?.onLongPress()
            }

            override fun onRelease(x: Float, y: Float) {
                dragListener?.onRelease(x, y)
            }

            override fun onDrag(x: Float, y: Float) {
                dragListener?.onDrag(x, y)
            }
        }

        this.dragView.setOnTouchListener(dragHelper)
    }

    fun attachToWindow() {
        windowCtrl.addView(width, height, true, dragView)
        windowCtrl.moveViewTo(dragView, targetView.x.toInt(), targetView.y.toInt())
    }

    fun detachFromWindow() {
        windowCtrl.removeView(dragView)
    }
}