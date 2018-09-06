package org.mozilla.scryer.setting


import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.support.v7.preference.PreferenceViewHolder
import android.support.v7.preference.SwitchPreferenceCompat
import android.util.AttributeSet
import android.widget.TextView
import org.mozilla.scryer.R
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.*

class TelemetrySwitchPreference : SwitchPreferenceCompat {

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        val learnMore = holder.itemView.findViewById<TextView>(R.id.learnMore)
        learnMore.setOnClickListener { v ->
            // This is a hardcoded link: if we ever end up needing more of these links, we should
            // move the link into an xml parameter, but there's no advantage to making it configurable now.
            val url = getSumoUrlForTopic(context, "usage-data")

            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.data = Uri.parse(url)
            context.startActivity(intent)
        }

        super.onBindViewHolder(holder)
    }

    private fun getSumoUrlForTopic(context: Context, topic: String): String {
        val escapedTopic: String
        try {
            escapedTopic = URLEncoder.encode(topic, "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            throw IllegalStateException("utf-8 should always be available", e)
        }

        val appVersion: String
        try {
            appVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            // This should be impossible - we should always be able to get information about ourselves:
            throw IllegalStateException("Unable find package details for Focus", e)
        }

        val osTarget = "Android"
        val langTag = Locale.getDefault().language + "-" + Locale.getDefault().country

        return "https://support.mozilla.org/1/mobile/$appVersion/$osTarget/$langTag/$escapedTopic"
    }
}
