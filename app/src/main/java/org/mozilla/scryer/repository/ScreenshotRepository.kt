/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.repository

import android.arch.lifecycle.LiveData
import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context
import org.mozilla.scryer.persistence.CollectionModel
import org.mozilla.scryer.persistence.ScreenshotDatabase
import org.mozilla.scryer.persistence.ScreenshotModel
import java.util.*
import java.util.concurrent.Executors

abstract class ScreenshotRepository {
    companion object Factory {
        fun createRepository(context: Context, onCreated: () -> Unit): ScreenshotRepository {
            val callback = object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    onCreated()
                }
            }
            return ScreenshotDatabaseRepository(Room.databaseBuilder(context.applicationContext,
                    ScreenshotDatabase::class.java, "screenshot-db")
                    .addCallback(callback)
                    .build())
        }
    }

    abstract fun addCollection(collection: CollectionModel)
    abstract fun getCollections(): LiveData<List<CollectionModel>>

    abstract fun addScreenshot(screenshot: ScreenshotModel)
    abstract fun getScreenshots(): LiveData<List<ScreenshotModel>>
    abstract fun getScreenshots(collectionId: String): LiveData<List<ScreenshotModel>>
    abstract fun setupDefaultContent()
}

class ScreenshotDatabaseRepository(private val database: ScreenshotDatabase) : ScreenshotRepository() {
    private val executor = Executors.newSingleThreadExecutor()

    private var collectionListData = database.collectionDao().getCollections()
    private val screenshotListData = database.screenshotDao().getScreenshots()

    override fun addScreenshot(screenshot: ScreenshotModel) {
        executor.submit {
            database.screenshotDao().addScreenshot(screenshot)
        }
    }

    override fun getScreenshots(collectionId: String): LiveData<List<ScreenshotModel>> {
        return database.screenshotDao().getScreenshots(collectionId)
    }

    override fun getScreenshots(): LiveData<List<ScreenshotModel>> {
        return screenshotListData
    }

    override fun getCollections(): LiveData<List<CollectionModel>> {
        return collectionListData
    }

    override fun addCollection(collection: CollectionModel) {
        executor.submit {
            database.collectionDao().addCollection(collection)
        }
    }

    override fun setupDefaultContent() {
        val music = CollectionModel(UUID.randomUUID().toString(), "Music", System.currentTimeMillis())
        val shopping = CollectionModel(UUID.randomUUID().toString(), "Shopping", System.currentTimeMillis())
        val secret = CollectionModel(UUID.randomUUID().toString(), "Secret", System.currentTimeMillis())
        addCollection(music)
        addCollection(shopping)
        addCollection(secret)
    }
}