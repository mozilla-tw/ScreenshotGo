package org.mozilla.scryer.setting

import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.ActionBar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.mozilla.scryer.R
import org.mozilla.scryer.getSupportActionBar


class AboutFragment : Fragment() {
    companion object {
        const val TAG: String = "AboutFragment"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view: View = inflater.inflate(R.layout.fragment_about, container, false)

        val versionText = view.findViewById<TextView>(R.id.about_text_version)
        versionText?.text = getString(R.string.about_content_version, getAppVersion())

        return view
    }

    private fun getAppVersion(): String {
        var appVersion = ""
        try {
            appVersion = context?.packageManager?.getPackageInfo(context?.packageName, 0)?.versionName ?: ""
        } catch (e: PackageManager.NameNotFoundException) {
            // Nothing to do if we can't find the package name.
        }
        return appVersion
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupActionBar()
    }

    private fun setupActionBar() {
        getSupportActionBar(activity).apply {
            setDisplayHomeAsUpEnabled(true)
            updateActionBarTitle(this)
        }
    }

    private fun updateActionBarTitle(actionBar: ActionBar) {
        actionBar.title = getString(R.string.settings_list_about)
    }
}