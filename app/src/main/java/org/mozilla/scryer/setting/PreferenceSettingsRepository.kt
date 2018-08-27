/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.setting

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.preference.PreferenceManager

class PreferenceSettingsRepository(context: Context) : SettingsRepository {

    companion object {
        private const val KEY_SERVICE_ENABLED = "settings_service_enabled"
        private const val KEY_FLOATING_ENABLED = "settings_floating_enabled"
        private const val KEY_ADD_TO_COLLECTION_ENABLED = "settings_add_to_collection_enabled"

        @Volatile
        private var instance: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository =
                instance ?: synchronized(this) {
                    instance ?: PreferenceSettingsRepository(context)
                }
    }

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    private val serviceLiveData = MutableLiveData<Boolean>()
    private val floatingLiveData = MutableLiveData<Boolean>()
    private val addToCollectionLiveData = MutableLiveData<Boolean>()

    override var serviceEnabled: Boolean = prefs.getBoolean(KEY_SERVICE_ENABLED, true)
        get() = prefs.getBoolean(KEY_SERVICE_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_SERVICE_ENABLED, value).apply()
            notifyChange(serviceLiveData, field, value)
            field = value
        }

    override val serviceEnabledObserver: LiveData<Boolean>
        get() = serviceLiveData

    override var floatingEnable: Boolean = prefs.getBoolean(KEY_FLOATING_ENABLED, true)
        get() = prefs.getBoolean(KEY_FLOATING_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_FLOATING_ENABLED, value).apply()
            notifyChange(floatingLiveData, field, value)
            field = value
        }

    override val floatingEnableObservable: LiveData<Boolean>
        get() = floatingLiveData


    override var addToCollectionEnable: Boolean = prefs.getBoolean(KEY_ADD_TO_COLLECTION_ENABLED, true)
        get() = prefs.getBoolean(KEY_ADD_TO_COLLECTION_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_ADD_TO_COLLECTION_ENABLED, value).apply()
            notifyChange(addToCollectionLiveData, field, value)
            field = value
        }
    override val addToCollectionEnableObservable: LiveData<Boolean>
        get() = addToCollectionLiveData


    private fun <T> notifyChange(data: MutableLiveData<T>, old: T, new: T) {
        if (old != new) {
            data.value = new
        }
    }
}

//abstract class PreferenceLiveData<T>(private val pref: SharedPreferences,
//                                     private val key: String,
//                                     defaultValue: T) : MutableLiveData<T>() {
//
//    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
//        if (key == this@PreferenceLiveData.key) {
//            value = getTypedValue(key, defaultValue)
//        }
//    }
//
//    override fun onActive() {
//        super.onActive()
//        pref.registerOnSharedPreferenceChangeListener(listener)
//    }
//
//    override fun onInactive() {
//        super.onInactive()
//        pref.unregisterOnSharedPreferenceChangeListener(listener)
//    }
//
//    abstract fun getTypedValue(key: String, defaultValue: T): T
//}
//
//class BooleanLiveData(private val pref: SharedPreferences, key: String, defaultValue: Boolean)
//    : PreferenceLiveData<Boolean>(pref, key, defaultValue) {
//
//    override fun getTypedValue(key: String, defaultValue: Boolean): Boolean {
//        return pref.getBoolean(key, defaultValue)
//    }
//}
