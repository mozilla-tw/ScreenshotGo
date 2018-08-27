/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.persistence

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy.REPLACE
import android.arch.persistence.room.Query
import android.arch.persistence.room.Update

@Dao
interface CollectionDao {

    @Query("SELECT * FROM collection")
    fun getCollections(): LiveData<List<CollectionModel>>

    @Query("SELECT * FROM collection")
    fun getCollectionList(): List<CollectionModel>

    @Insert(onConflict = REPLACE)
    fun addCollection(collection: CollectionModel)

    @Update(onConflict = REPLACE)
    fun updateCollection(collection: CollectionModel)
}
