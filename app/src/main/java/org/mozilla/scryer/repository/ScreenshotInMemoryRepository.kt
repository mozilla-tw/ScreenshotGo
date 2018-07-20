/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.repository

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import org.mozilla.scryer.persistence.CollectionModel
import org.mozilla.scryer.persistence.ScreenshotModel

@Suppress("unused")
class ScreenshotInMemoryRepository : ScreenshotRepository {
    private val collectionData = MutableLiveData<List<CollectionModel>>()
    private val collectionList = mutableListOf<CollectionModel>()
    private val screenshotData = MutableLiveData<List<ScreenshotModel>>()
    private val screenshotList = mutableListOf<ScreenshotModel>()

    init {
        collectionData.value = collectionList
        screenshotData.value = screenshotList
    }

    override fun addCollection(collection: CollectionModel) {
        collectionList.add(collection)
        collectionData.value = collectionList
    }

    override fun getCollections(): LiveData<List<CollectionModel>> {
        return collectionData
    }

    override fun getCollectionCovers(): LiveData<Map<String, ScreenshotModel>> {
        return MutableLiveData<Map<String, ScreenshotModel>>()
    }

    override fun addScreenshot(screenshot: ScreenshotModel) {
        screenshotList.add(screenshot)
        screenshotData.value = screenshotList
    }

    override fun updateScreenshot(screenshot: ScreenshotModel) {
        screenshotData.value = screenshotList
    }

    override fun getScreenshots(): LiveData<List<ScreenshotModel>> {
        return screenshotData
    }

    override fun getScreenshots(collectionId: String): LiveData<List<ScreenshotModel>> {
        return screenshotData
    }
}
