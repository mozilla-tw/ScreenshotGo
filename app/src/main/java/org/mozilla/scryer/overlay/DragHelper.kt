/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.overlay

import android.annotation.SuppressLint
import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration

class DragHelper(private val targetView: View,
                 private val windowController: WindowController) : View.OnTouchListener {
    companion object {
        const val DURATION_LONG_PRESS = 500L
    }

    var dragListener: DragListener? = null

    private var dragging: Boolean = false
    private var longPressed = false

    private var oldPosition = PointF()
    private var currentPosition = PointF()
    private val oldTouchPosition = PointF()

    private val tmpPosition = PointF()

    private val handler = Handler(Looper.getMainLooper())
    private val longClickRunnable  = Runnable {
        longPressed = true
        dragging = false
        dragListener?.onLongPress()
    }

    private val dragSlop: Int by lazy {
        ViewConfiguration.get(targetView.context).scaledTouchSlop
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                dragging = false
                longPressed = false
                getViewCenter(oldPosition)
                currentPosition.set(oldPosition.x, oldPosition.y)
                oldTouchPosition.set(event.rawX, event.rawY)
                handler.postDelayed(longClickRunnable, DURATION_LONG_PRESS)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dragDeltaX = event.rawX - oldTouchPosition.x
                val dragDeltaY = event.rawY - oldTouchPosition.y
                currentPosition.set(oldPosition.x + dragDeltaX, oldPosition.y + dragDeltaY)

                if (dragging) {
                    moveViewTo(currentPosition)
                    dragListener?.onDrag(currentPosition.x, currentPosition.y)
                } else if (shouldStartDrag(dragDeltaX, dragDeltaY)) {
                    dragging = true
                    handler.removeCallbacks(longClickRunnable)
                }

                return true
            }
            MotionEvent.ACTION_UP -> {
                handler.removeCallbacks(longClickRunnable)
                if (!dragging && !longPressed) {
                    dragListener?.onTap()
                } else {
                    dragListener?.onRelease(currentPosition.x, currentPosition.y)
                }

                return true
            }
            else -> return false
        }
    }

    private fun shouldStartDrag(dx: Float, dy: Float): Boolean {
        val distance = Math.sqrt(Math.pow(dx.toDouble(), 2.0) + Math.pow(dy.toDouble(), 2.0))
        return distance >= dragSlop
    }

    private fun getViewCenter(result: PointF) {
        val originX = windowController.getViewPositionX(targetView)
        val originY = windowController.getViewPositionY(targetView)
        convertToCenter(tmpPosition.apply { set(originX.toFloat(), originY.toFloat()) }, targetView)
        result.set(tmpPosition.x, tmpPosition.y)
    }

    private fun moveViewTo(centerPosition: PointF) {
        convertToOrigin(tmpPosition.apply { set(centerPosition) }, targetView)
        windowController.moveViewTo(targetView, tmpPosition.x.toInt(), tmpPosition.y.toInt())
    }

    interface DragListener {
        fun onTap() {}
        fun onLongPress() {}
        fun onRelease(x: Float, y: Float) {}
        fun onDrag(x: Float, y: Float) {}
    }
}