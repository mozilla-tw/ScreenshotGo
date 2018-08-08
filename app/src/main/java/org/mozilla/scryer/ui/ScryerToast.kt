package org.mozilla.scryer.ui

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import org.mozilla.scryer.R

class ScryerToast {
    companion object {

        @SuppressLint("InflateParams")
        fun makeText(context: Context, text: String, duration: Int): Toast {
            val toast = Toast(context)
            toast.setGravity(Gravity.FILL_HORIZONTAL or Gravity.BOTTOM, 0, 0)
            toast.view = LayoutInflater.from(context).inflate(R.layout.view_custom_toast, null)
            toast.view.findViewById<TextView>(R.id.text)?.text = text
            toast.duration = duration
            return toast
        }
    }
}
