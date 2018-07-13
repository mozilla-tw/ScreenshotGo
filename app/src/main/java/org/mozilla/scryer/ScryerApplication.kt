/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer

import android.app.Application

import org.mozilla.scryer.repository.ScreenshotRepository

class ScryerApplication : Application() {
    companion object {
        lateinit var instance: ScryerApplication
            private set
    }

    lateinit var screenshotRepository: ScreenshotRepository

    override fun onCreate() {
        super.onCreate()
        instance = this
        screenshotRepository = ScreenshotRepository.createRepository(this) {
            screenshotRepository.setupDefaultContent()
        }
    }
}
