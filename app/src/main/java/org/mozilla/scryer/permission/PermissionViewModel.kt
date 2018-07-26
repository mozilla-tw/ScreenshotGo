/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.permission

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.content.pm.PackageManager

class PermissionViewModel : ViewModel() {
    private val map = HashMap<Int, PermissionLiveData>()
    val size: Int
        get() = map.size

    fun permission(requestCode: Int): PermissionLiveData {
        val result = map[requestCode]
        return result?: run {
            val liveData = PermissionLiveData()
            map[requestCode] = liveData
            return liveData
        }
    }

    fun consume(requestCode: Int) {
        map.remove(requestCode)
    }
}

class PermissionLiveData: MutableLiveData<BooleanArray>() {
    fun onResult(results: IntArray) {
        value = results.map { it == PackageManager.PERMISSION_GRANTED }.toBooleanArray()
    }
}
