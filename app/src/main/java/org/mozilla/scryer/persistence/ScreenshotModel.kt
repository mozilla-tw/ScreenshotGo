/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.persistence

import android.arch.persistence.room.*

@Entity(tableName = "screenshot",
//        foreignKeys = [(ForeignKey(
//                entity = CollectionModel::class,
//                parentColumns = ["id"],
//                childColumns = ["collection_id"]))],
        indices = [Index("collection_id"), Index("absolute_path", unique = true)])
data class ScreenshotModel constructor (
        @PrimaryKey(autoGenerate = true) var id: Long?,
        @ColumnInfo(name = "absolute_path") var absolutePath: String,
        @ColumnInfo(name = "last_modified") var lastModified: Long,
        @ColumnInfo(name = "collection_id") var collectionId: String)
