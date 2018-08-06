/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.persistence

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.*
import android.arch.persistence.room.OnConflictStrategy.IGNORE
import android.arch.persistence.room.OnConflictStrategy.REPLACE

@Dao
interface ScreenshotDao {

    @Query("SELECT * FROM screenshot")
    fun getScreenshots(): LiveData<List<ScreenshotModel>>


    @Query("SELECT * FROM screenshot WHERE collection_id = :collectionId")
    fun getScreenshots(collectionId: String): LiveData<List<ScreenshotModel>>

    @Insert(onConflict = REPLACE)
    fun addScreenshot(screenshot: List<ScreenshotModel>): List<Long>

    @Update(onConflict = REPLACE)
    fun updateScreenshot(screenshot: ScreenshotModel)

    @Delete
    fun deleteScreenshot(screenshot: ScreenshotModel)

    @Query("SELECT screenshot.* FROM (SELECT id, max(last_modified) AS max_date FROM screenshot GROUP BY collection_id) AS latest INNER JOIN screenshot ON latest.id = screenshot.id AND screenshot.last_modified = latest.max_date")
    fun getCollectionCovers(): LiveData<List<ScreenshotModel>>
}
