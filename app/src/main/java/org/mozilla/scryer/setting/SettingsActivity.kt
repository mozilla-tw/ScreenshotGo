/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.setting

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.SwitchCompat
import org.mozilla.scryer.Observer
import org.mozilla.scryer.R
import org.mozilla.scryer.ScryerApplication
import org.mozilla.scryer.ScryerService

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val repository = ScryerApplication.getSettingsRepository()

        // Capture Service
        // TODO: Also switched off floating button option when this one is switched off?
        val serviceSwitch = findViewById<SwitchCompat>(R.id.service_enabled_switch)
        serviceSwitch.isChecked = repository.serviceEnabled
        serviceSwitch.setOnCheckedChangeListener { _, isChecked ->
            val intent = Intent(this, ScryerService::class.java)
            if (isChecked) {
                this.startService(intent)
            } else {
                this.stopService(intent)
            }
            repository.serviceEnabled = isChecked
        }
        ScryerApplication.getSettingsRepository().serviceEnabledObserver.observe(this, Observer {
            serviceSwitch.isChecked = it
        })

        // Floating Button
        // TODO: Switch to launch system settings if overlay permission is not yet granted
        val floatingSwitch = findViewById<SwitchCompat>(R.id.floating_enabled_switch)
        floatingSwitch.isChecked = repository.floatingEnable
        floatingSwitch.setOnCheckedChangeListener { _, isChecked ->
            repository.floatingEnable = isChecked
        }
    }
}
