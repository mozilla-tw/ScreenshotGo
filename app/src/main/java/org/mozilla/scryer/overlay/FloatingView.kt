/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PointF
import android.util.DisplayMetrics
import android.view.*
import android.widget.RelativeLayout

// TODO:
// state save/restore (e.g. view position)
// parameterize position

class FloatingView(context: Context) : RelativeLayout(context) {
    private var isAddedToWindow = false
    private var windowController: WindowController =
            WindowController(context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)

    private var clickListener: OnClickListener? = null

    init {
        init()
    }

    private fun init() {
        //setFocusableInTouchMode(true); // hardware back button presses.
    }

    override fun setOnClickListener(l: OnClickListener?) {
        clickListener = l
    }

    fun addToWindow() {
        if (isAddedToWindow) {
            return
        }

        windowController.addView(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true, this)
        isAddedToWindow = true

        val dragHelper = DragHelper(this, windowController)
        dragHelper.dragListener = object : DragHelper.DragListener {
            override fun onTap() {
                clickListener?.onClick(this@FloatingView)
            }

            override fun onRelease() {

            }
        }
        setOnTouchListener(dragHelper)

        post {
            val metrics = DisplayMetrics()
            display.getMetrics(metrics)
            windowController.moveViewTo(this@FloatingView,
                    metrics.widthPixels - measuredWidth + (measuredWidth * 0.1).toInt(),
                    metrics.heightPixels / 3)
        }
    }

    fun removeFromWindow() {
        isAddedToWindow = false
        windowController.removeView(this)
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
                    } else if (shouldStartDrag(dragDeltaX, dragDeltaY)) {
                        dragging = true
                    }

                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragging) {
                        dragListener?.onTap()
                    } else {
                        dragListener?.onRelease()
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
            fun onRelease()
        }
    }
}
