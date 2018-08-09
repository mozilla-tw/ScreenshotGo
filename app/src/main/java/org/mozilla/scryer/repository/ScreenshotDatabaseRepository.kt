/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.repository

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import org.mozilla.scryer.persistence.CollectionModel
import org.mozilla.scryer.persistence.ScreenshotDatabase
import org.mozilla.scryer.persistence.ScreenshotModel
import java.util.concurrent.Executors

class ScreenshotDatabaseRepository(private val database: ScreenshotDatabase) : ScreenshotRepository {
    private val executor = Executors.newSingleThreadExecutor()

    private var collectionListData = database.collectionDao().getCollections()
    private val screenshotListData = database.screenshotDao().getScreenshots()

    private val handler = Handler(Looper.getMainLooper())

    override fun addScreenshot(screenshots: List<ScreenshotModel>) {
        executor.submit {
            database.screenshotDao().addScreenshot(screenshots)
        }
    }

    override fun updateScreenshot(screenshot: ScreenshotModel) {
        executor.submit {
            database.screenshotDao().updateScreenshot(screenshot)
        }
    }

    override fun getScreenshot(screenshotId: String): ScreenshotModel? {
        return database.screenshotDao().getScreenshot(screenshotId)
    }

    override fun getScreenshots(collectionIds: List<String>): LiveData<List<ScreenshotModel>> {
        return database.screenshotDao().getScreenshots(collectionIds)
    }

    override fun getScreenshots(): LiveData<List<ScreenshotModel>> {
        return screenshotListData
    }

    override fun deleteScreenshot(screenshot: ScreenshotModel) {
        database.screenshotDao().deleteScreenshot(screenshot)
    }

    override fun getCollections(): LiveData<List<CollectionModel>> {
        return collectionListData
    }

    override fun addCollection(collection: CollectionModel) {
        executor.submit {
            database.collectionDao().addCollection(collection)
        }
    }

    override fun getCollectionCovers(): LiveData<Map<String, ScreenshotModel>> {
        return Transformations.switchMap(database.screenshotDao().getCollectionCovers()) { models ->
            MutableLiveData<Map<String, ScreenshotModel>>().apply {
                value = models.map { it.collectionId to it }.toMap()
            }
        }
    }

    override fun setupDefaultContent() {
        val none = CollectionModel(CollectionModel.CATEGORY_NONE, "Unsorted", 0, 0)
        val shopping = CollectionModel("Shopping", System.currentTimeMillis(), Color.parseColor("#f28dd2"))
        val music = CollectionModel("Music", System.currentTimeMillis(), Color.parseColor("#fdd757"))
        val secret = CollectionModel("Secret", System.currentTimeMillis(), Color.parseColor("#8ce2d0"))
        addCollection(none)
        addCollection(shopping)
        addCollection(music)
        addCollection(secret)
    }

    override fun getScreenshotList(callback: (List<ScreenshotModel>) -> Unit) {
        executor.execute {
            val list = database.screenshotDao().getScreenshotList()
            handler.post {
                callback(list)
            }
        }
    }

    override fun getScreenshotList(collectionIds: List<String>, callback: (List<ScreenshotModel>) -> Unit) {
        executor.execute {
            val list = database.screenshotDao().getScreenshotList(collectionIds)
            handler.post {
                callback(list)
            }
        }
    }
}