/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.persistence

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import java.util.*

@Entity(tableName = "collection")
data class CollectionModel constructor(
        @ColumnInfo(name = "name") val name: String,
        @ColumnInfo(name = "date") val date: Long,
        @PrimaryKey(autoGenerate = false) val id: String = UUID.randomUUID().toString()) {

    companion object {
        const val CATEGORY_NONE = ""
    }
}