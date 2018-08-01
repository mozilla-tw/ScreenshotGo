/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.setting

import android.arch.lifecycle.LiveData
import android.content.Context

interface SettingsRepository {
    companion object {
        fun createRepository(context: Context): SettingsRepository {
            return PreferenceSettingsRepository(context)
        }
    }

    var serviceEnabled: Boolean
    val serviceEnabledObserver: LiveData<Boolean>

    var floatingEnable: Boolean
    val floatingEnableObservable: LiveData<Boolean>
}
