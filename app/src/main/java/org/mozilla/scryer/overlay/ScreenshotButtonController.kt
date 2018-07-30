/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.overlay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.Color
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

class ScreenshotButtonController(private val context: Context) {
    companion object {
        const val BUTTON_SIZE_DP = 75f
        const val EXIT_VIEW_HEIGHT_DP = 180f
    }

    private lateinit var screen: Screen
    private lateinit var sideDock: Dock
    private lateinit var exitDock: Dock

    private lateinit var buttonView: FloatingView
    private lateinit var exitView: FloatingView

    private var clickListener: ClickListener? = null

    private var windowController: WindowController = WindowController(
            context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)

    private val metrics = context.resources.displayMetrics
    private val buttonSize = BUTTON_SIZE_DP.dpToPx(metrics)

    fun setOnClickListener(listener: ClickListener) {
        this.clickListener = listener
    }

    fun init() {
        screen = Screen(context, windowController)
        sideDock = Dock(screen)
        initExitView()
        buttonView = initScreenshotButton(screen, sideDock)

        screen.onBoundaryUpdateListener = Runnable {
            buttonView.moveTo(sideDock)
        }
    }

    private fun initScreenshotButton(screen: Screen, dock: Dock): FloatingView {
        val buttonView = FloatingView.create(createButtonView(context), dock, buttonSize, buttonSize,
                true, windowController)

        buttonView.dragListener = object : DragHelper.DragListener {
            override fun onTap() {
                clickListener?.onScreenshotButtonClicked()
            }

            override fun onLongPress() {

            }

            override fun onDrag(x: Float, y: Float) {
                exitView.visibility = View.VISIBLE
                if (y >= exitView.y) {
                    if (!buttonView.stickToCurrentPosition) {
                        buttonView.stickToCurrentPosition = true
                        buttonView.animateTo(exitDock, OvershootInterpolator(), 300)
                    }
                } else {
                    buttonView.stickToCurrentPosition = false
                }
            }

            override fun onRelease(x: Float, y: Float) {
                if (y < exitView.top) {
                    dock.updatePosition(x.toInt(), y.toInt())
                    buttonView.animateTo(dock, OvershootInterpolator(), 500)
                } else {
                    val animator = buttonView.animate().scaleX(0f).scaleY(0f)
                    animator.duration = 200
                    animator.interpolator = AccelerateInterpolator()
                    animator.setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator?) {
                            clickListener?.onScreenshotButtonLongClicked()
                        }
                    })
                }
                exitView.visibility = View.INVISIBLE
            }
        }

        screen.addView(buttonView, buttonSize, buttonSize)
        buttonView.post {
            buttonView.moveTo(sideDock)
        }
        return buttonView
    }

    private fun initExitView()  {
        exitView = FloatingView.create(createExitView(context), sideDock, ViewGroup.LayoutParams.MATCH_PARENT,
                EXIT_VIEW_HEIGHT_DP.dpToPx(metrics), false, windowController)
        val trashParams = RelativeLayout.LayoutParams(ViewGroup.MarginLayoutParams.MATCH_PARENT,
                EXIT_VIEW_HEIGHT_DP.dpToPx(metrics))
        trashParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        exitView.visibility = View.INVISIBLE
        screen.containerView.addView(exitView, trashParams)

        exitDock = object : Dock(screen) {
            override fun resolveX(targetSize: Int): Float {
                return (exitView.left + exitView.right) / 2f
            }

            override fun resolveY(targetSize: Int): Float {
                return (exitView.top + exitView.bottom) / 2f
            }

            override fun updatePosition(x: Int, y: Int) {

            }
        }
    }

    fun destroy() {
        buttonView.detachFromWindow()
        screen.detachFromWindow()
    }

    fun show() {
        screen.containerView.visibility = View.VISIBLE
    }

    fun hide() {
        screen.containerView.visibility = View.INVISIBLE
    }

    private fun createButtonView(context: Context): View {
        val view = View(context)
        view.setBackgroundResource(R.drawable.circle_bg)
        return view
    }

    private fun createExitView(context: Context): View {
        val view = TextView(context)
        view.text = "X"
        view.textSize = 15f.dpToPx(metrics).toFloat()
        view.gravity = Gravity.CENTER
        view.setTextColor(Color.parseColor("#ff8800"))
        view.setBackgroundColor(Color.parseColor("#88000000"))
        return view
    }

    interface ClickListener {
        fun onScreenshotButtonClicked()
        fun onScreenshotButtonLongClicked()
    }
}