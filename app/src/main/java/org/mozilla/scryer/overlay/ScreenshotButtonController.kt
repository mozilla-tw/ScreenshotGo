/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.overlay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.RelativeLayout
import org.mozilla.scryer.Observer
import org.mozilla.scryer.R
import org.mozilla.scryer.ScryerApplication
import org.mozilla.scryer.extension.dpToPx

class ScreenshotButtonController(private val context: Context) {
    companion object {
        const val BUTTON_SIZE_DP = 40f

        const val ALPHA_ACTIVE = 1f
        const val ALPHA_INACTIVE = 0.50f
    }

    private lateinit var screen: Screen
    private lateinit var sideDock: Dock
    private lateinit var exitDock: Dock

    private lateinit var buttonView: FloatingView
    private lateinit var exitView: FloatingView
    private lateinit var exitAnchor: View
    private lateinit var exitBorder: View

    private var clickListener: ClickListener? = null

    private var windowController: WindowController = WindowController(
            context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)

    private val metrics = context.resources.displayMetrics
    private val buttonSize = BUTTON_SIZE_DP.dpToPx(metrics)

    private val settingObserver = Observer<Boolean> {
        if (it) {
            show()
        } else {
            hide()
        }
    }

    fun init() {
        screen = Screen(context, windowController)
        sideDock = Dock(screen)
        initExitView()
        buttonView = initScreenshotButton(screen, sideDock)

        screen.onBoundaryUpdateListener = Runnable {
            buttonView.moveTo(sideDock)
        }

        ScryerApplication.getSettingsRepository().floatingEnableObservable.observeForever(settingObserver)
        if (!ScryerApplication.getSettingsRepository().floatingEnable) {
            hide()
        }
    }

    fun setOnClickListener(listener: ClickListener) {
        this.clickListener = listener
    }

    fun show() {
        screen.containerView.visibility = View.VISIBLE
    }

    fun hide() {
        screen.containerView.visibility = View.INVISIBLE
    }

    fun destroy() {
        ScryerApplication.getSettingsRepository().floatingEnableObservable.removeObserver(settingObserver)
        buttonView.detachFromWindow()
        screen.detachFromWindow()
    }

    private fun initScreenshotButton(screen: Screen, dock: Dock): FloatingView {
        val buttonView = FloatingView.create(createButtonView(context), dock, buttonSize, buttonSize,
                true, windowController)
        buttonView.alpha = ALPHA_INACTIVE

        buttonView.dragListener = object : DragHelper.DragListener {
            override fun onTouch() {
                buttonView.alpha = ALPHA_ACTIVE
            }

            override fun onTap() {
                clickListener?.onScreenshotButtonClicked()
            }

            override fun onLongPress() {

            }

            override fun onDrag(x: Float, y: Float) {
                exitView.visibility = View.VISIBLE

                val reactRadius: Float = buttonSize * 3f
                val anchorX: Double = (exitAnchor.x + buttonSize / 2).toDouble()
                val anchorY: Double = (exitAnchor.y + buttonSize / 2).toDouble()
                val distance = Math.sqrt((anchorX - x) * (anchorX - x) + (anchorY - y) * (anchorY - y))
                val percentage = distance / reactRadius

                if (percentage < 1) {
                    if (!buttonView.stickToCurrentPosition) {
                        buttonView.stickToCurrentPosition = true
                        buttonView.animateTo(exitDock, OvershootInterpolator(), 300)
                    }
                } else {
                    buttonView.stickToCurrentPosition = false
                }
            }

            override fun onRelease(x: Float, y: Float) {
                buttonView.alpha = ALPHA_INACTIVE

                val reactRadius: Float = buttonSize * 3f
                val anchorX: Double = (exitAnchor.x + buttonSize / 2).toDouble()
                val anchorY: Double = (exitAnchor.y + buttonSize / 2).toDouble()
                val distance = Math.sqrt((anchorX - x) * (anchorX - x) + (anchorY - y) * (anchorY - y))
                val percentage = distance / reactRadius

                if (percentage >= 1) {
                    dock.updatePosition(x.toInt(), y.toInt())
                    buttonView.animateTo(dock, OvershootInterpolator(), 500)
                } else if (percentage < 1) {
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
        val exitViewContent = createExitView(context)
        exitView = FloatingView.create(exitViewContent, sideDock, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT, false, windowController)
        val trashParams = RelativeLayout.LayoutParams(ViewGroup.MarginLayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)
        trashParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        exitView.visibility = View.INVISIBLE
        screen.containerView.addView(exitView, trashParams)

        exitAnchor = exitViewContent.findViewById<View>(R.id.exit_button_anchor_view)
        exitBorder = exitViewContent.findViewById<View>(R.id.border)

        exitDock = object : Dock(screen) {
            override fun resolveX(targetSize: Int): Float {
                return (exitAnchor.left + exitAnchor.right) / 2f
            }

            override fun resolveY(targetSize: Int): Float {
                return (exitAnchor.top + exitAnchor.bottom) / 2f
            }

            override fun updatePosition(x: Int, y: Int) {

            }
        }
    }

    private fun createButtonView(context: Context): View {
        return View.inflate(context, R.layout.view_capture_button, null)
    }

    private fun createExitView(context: Context): View {
        return View.inflate(context, R.layout.view_capture_button_exit, null)
    }

    interface ClickListener {
        fun onScreenshotButtonClicked()
        fun onScreenshotButtonLongClicked()
    }
}
