/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.setting

import android.content.Context
import android.preference.PreferenceManager

interface SettingsRepository {
    companion object {
        fun createRepository(context: Context): SettingsRepository {
            return PreferenceSettingsRepository(context)
        }
    }

    fun isFloatingEnabled(): Boolean
    fun setFloatingEnabled(enabled: Boolean)
}

class PreferenceSettingsRepository(context: Context) : SettingsRepository {
    companion object {
        private const val KEY_FLOATING_ENABLED = "settings_floating_enabled"
    }
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    override fun isFloatingEnabled(): Boolean {
        return prefs.getBoolean(KEY_FLOATING_ENABLED, true)
    }

    override fun setFloatingEnabled(enabled: Boolean) {
        return prefs.edit().putBoolean(KEY_FLOATING_ENABLED, enabled).apply()
    }
}