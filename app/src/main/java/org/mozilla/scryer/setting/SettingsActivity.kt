/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.setting

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.SwitchCompat
import org.mozilla.scryer.R
import org.mozilla.scryer.ScryerApplication

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val repository = ScryerApplication.getSettingsRepository()

        val switch = findViewById<SwitchCompat>(R.id.floating_enabled_switch)
        switch.isChecked = repository.floatingEnable
        switch.setOnCheckedChangeListener { _, isChecked ->
            repository.floatingEnable = isChecked
        }
    }
}
