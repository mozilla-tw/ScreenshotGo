/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.overlay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.arch.lifecycle.DefaultLifecycleObserver
import android.content.Context
import android.graphics.Color
import android.graphics.PointF
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.RelativeLayout
import android.widget.TextView
import org.mozilla.scryer.R
import org.mozilla.scryer.extension.dpToPx

class ScreenshotButtonController(private val context: Context) : DefaultLifecycleObserver {
    companion object {
        const val BUTTON_SIZE_DP = 75f
    }

    private var debugView = false

    private lateinit var buttonContainer: FloatingView
    private lateinit var buttonView: View
    private lateinit var trashView: View
    private lateinit var dragView: FloatingView

    private var clickListener: ClickListener? = null

    var view: View? = null

    private val metrics = context.resources.displayMetrics
    private val buttonSize = BUTTON_SIZE_DP.dpToPx(metrics)
    private val position: PointF = PointF()

    private val tmpPoint = PointF()

    private val dragListener = object : FloatingView.DragHelper.DragListener {
        override fun onTap() {
            clickListener?.onScreenshotButtonClicked()
        }

        override fun onLongPress() {
            trashView.visibility = View.VISIBLE
        }

        override fun onRelease(x: Float, y: Float) {
            tmpPoint.set(x, y)
            convertToOrigin(tmpPoint, buttonView)

            if (y >= trashView.y) {
                val animator = buttonView.animate().scaleX(0f).scaleY(0f)
                animator.duration = 200
                animator.interpolator = AccelerateInterpolator()
                animator.setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        clickListener?.onScreenshotButtonLongClicked()
                    }
                })
            } else {
                val targetX = getTargetX(tmpPoint.x)
                val animator = buttonView.animate().x(targetX).setUpdateListener {
                    dragView.moveTo(buttonView.x.toInt(), tmpPoint.y.toInt())
                }
                animator.duration = 500
                animator.interpolator = OvershootInterpolator()
            }
            trashView.visibility = View.GONE
        }

        override fun onDrag(x: Float, y: Float) {
            trashView.visibility = View.VISIBLE
            tmpPoint.set(x, y)
            convertToOrigin(tmpPoint, buttonView)
            buttonView.x = tmpPoint.x
            buttonView.y = tmpPoint.y
        }
    }

    private fun getTargetX(x: Float): Float {
        return if (x > metrics.widthPixels / 2) {
            metrics.widthPixels - buttonView.measuredWidth * 0.9f
        } else {
            -buttonView.measuredWidth * 0.1f
        }
    }

    fun setOnClickListener(listener: ClickListener) {
        this.clickListener = listener
    }

    fun init() {
        position.set(metrics.widthPixels - buttonSize * 0.9f, metrics.heightPixels / 3f)

        dragView = FloatingView(context)
        initDragView(dragView)

        buttonContainer = FloatingView(context)
        initButton(buttonContainer)
    }

    private fun initButton(buttonContainer: FloatingView)  {
        trashView = onCreateTrashView(context, buttonContainer)
        val trashParams = RelativeLayout.LayoutParams(ViewGroup.MarginLayoutParams.MATCH_PARENT,
                120f.dpToPx(metrics))
        trashParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        trashView.visibility = View.GONE
        buttonContainer.addView(trashView, trashParams)

        val size = 75f.dpToPx(context.resources.displayMetrics)
        buttonView = onCreateButtonView(context, buttonContainer)
        buttonContainer.addView(buttonView, size, size)
        buttonView.x = position.x
        buttonView.y = position.y

        buttonContainer.addToWindow(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                false)
        if (debugView) {
            buttonContainer.setBackgroundColor(Color.parseColor("#55ff0000"))
        }
    }

    private fun initDragView(dragView: FloatingView) {
        val size = 75f.dpToPx(context.resources.displayMetrics)
        val view = View(context)

        dragView.dragListener = dragListener
        dragView.addView(view, size, size)
        if (debugView) {
            dragView.setBackgroundColor(Color.parseColor("#88cccc00"))
        }
        dragView.addToWindow(WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                true)
        dragView.moveTo(position.x.toInt(), position.y.toInt())
    }

    fun destroy() {
        buttonContainer.removeFromWindow()
    }

    fun show() {
        view?.visibility = View.VISIBLE
    }

    fun hide() {
        view?.visibility = View.INVISIBLE
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onCreateButtonView(context: Context, container: ViewGroup): View {
        val view = View(context)
        view.setBackgroundResource(R.drawable.circle_bg)
        this.view = view
        return view
    }

    private fun onCreateTrashView(context: Context, container: ViewGroup): View {
        val view = TextView(context)
        view.text = "X"
        view.textSize = 15f.dpToPx(metrics).toFloat()
        view.gravity = Gravity.CENTER
        view.setTextColor(Color.parseColor("#ff8800"))
        view.setBackgroundColor(Color.parseColor("#88000000"))
        return view
    }

    private fun convertToOrigin(center: PointF, view: View) {
        val x = center.x
        val y = center.y
        val width = view.measuredWidth
        val height = view.measuredHeight
        center.set(x - width / 2f, y - height / 2f)
    }

    interface ClickListener {
        fun onScreenshotButtonClicked()
        fun onScreenshotButtonLongClicked()
    }
}