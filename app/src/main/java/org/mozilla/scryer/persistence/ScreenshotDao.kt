/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.persistence

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import android.arch.persistence.room.Update

@Dao
interface ScreenshotDao {

    @Query("SELECT * FROM screenshot")
    fun getScreenshots(): LiveData<List<ScreenshotModel>>


    @Query("SELECT * FROM screenshot WHERE collection_id = :collectionId")
    fun getScreenshots(collectionId: String): LiveData<List<ScreenshotModel>>

    @Insert
    fun addScreenshot(screenshot: ScreenshotModel)

    @Update
    fun updateScreenshot(screenshot: ScreenshotModel)

    @Query("SELECT screenshot.* FROM (SELECT id, max(date) AS max_date FROM screenshot GROUP BY collection_id) AS latest INNER JOIN screenshot ON latest.id = screenshot.id AND screenshot.date = latest.max_date")
    fun getCollectionCovers(): LiveData<List<ScreenshotModel>>
}
