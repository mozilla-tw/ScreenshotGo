/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PointF
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.RelativeLayout

// TODO:
// state save/restore (e.g. view position)
// parameterize position

class FloatingView(context: Context) : RelativeLayout(context) {
    private var isAddedToWindow = false
    private var windowController: WindowController =
            WindowController(context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)

    private var clickListener: OnClickListener? = null

    private val dragHelper: DragHelper = DragHelper(this, windowController)
    var dragListener: DragHelper.DragListener? = null

    init {
        init()
    }

    private fun init() {
        //setFocusableInTouchMode(true); // hardware back button presses.
    }

    override fun setOnClickListener(l: OnClickListener?) {
        clickListener = l
    }

    fun addToWindow(width: Int, height: Int, touchable: Boolean) {
        if (isAddedToWindow) {
            return
        }

        dragHelper.dragListener = dragListener
        setOnTouchListener(dragHelper)

        windowController.addView(width, height, touchable, this)
        isAddedToWindow = true
    }

    fun removeFromWindow() {
        isAddedToWindow = false
        windowController.removeView(this)
    }

    fun moveTo(x: Int, y: Int) {
        windowController.moveViewTo(this@FloatingView, x, y)
    }

    class DragHelper(private val targetView: View,
                     private val windowController: WindowController) : OnTouchListener {
        var dragListener: DragListener? = null

        private var dragging: Boolean = false

        private var oldPosition = PointF()
        private var currentPosition = PointF()
        private val oldTouchPosition = PointF()

        private val tmpPosition = PointF()

        private val dragSlop: Int by lazy {
            ViewConfiguration.get(targetView.context).scaledTouchSlop
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dragging = false
                    getViewCenter(oldPosition)
                    currentPosition.set(oldPosition.x, oldPosition.y)
                    oldTouchPosition.set(event.rawX, event.rawY)
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
                    }

                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragging) {
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
            tmpPosition.set(originX.toFloat(), originY.toFloat())
            return originToCenter(tmpPosition, result)
        }

        private fun originToCenter(origin: PointF, result: PointF) {
            result.set(origin.x + targetView.width / 2, origin.y + targetView.height / 2)
        }

        private fun centerToOrigin(centerPosition: PointF, result: PointF) {
            return result.set(centerPosition.x - targetView.width / 2,
                    centerPosition.y - targetView.height / 2)
        }

        private fun moveViewTo(centerPosition: PointF) {
            centerToOrigin(centerPosition, tmpPosition)
            windowController.moveViewTo(targetView, tmpPosition.x.toInt(), tmpPosition.y.toInt())
        }

        interface DragListener {
            fun onTap()
            fun onRelease(x: Float, y: Float)
            fun onDrag(x: Float, y: Float)
        }
    }
}
