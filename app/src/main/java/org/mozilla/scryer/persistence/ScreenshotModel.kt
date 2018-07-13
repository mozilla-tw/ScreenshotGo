/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.persistence

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.PrimaryKey

@Entity(tableName = "screenshot",
        foreignKeys = [(ForeignKey(
                entity = CategoryModel::class,
                parentColumns = ["id"],
                childColumns = ["category_id"]))])
data class ScreenshotModel(
        @PrimaryKey(autoGenerate = false) var id: String,
        @ColumnInfo(name = "path") var path: String,
        @ColumnInfo(name = "date") var date: Long,
        @ColumnInfo(name = "category_id") var categoryId: String)