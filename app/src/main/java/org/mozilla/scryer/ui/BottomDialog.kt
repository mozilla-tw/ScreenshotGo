package org.mozilla.scryer.ui

import android.content.Context
import android.support.annotation.LayoutRes
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.BottomSheetDialog
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import org.mozilla.scryer.R

class BottomDialogFactory {
    companion object {
        fun create(context: Context, @LayoutRes layoutId: Int): BottomSheetDialog {
            val dialog = BottomSheetDialog(context, R.style.ScryerBottomSheetDialogTheme)
            val view = View.inflate(context, layoutId, null)
            dialog.setContentView(view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT))
            view.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    view.viewTreeObserver.removeOnPreDrawListener(this)
                    BottomSheetBehavior.from(view.parent as ViewGroup).peekHeight = view.measuredHeight
                    return false
                }
            })
            return dialog
        }
    }

}
