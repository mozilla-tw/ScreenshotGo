/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.viewmodel

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import org.mozilla.scryer.persistence.CollectionModel
import org.mozilla.scryer.persistence.ScreenshotModel
import org.mozilla.scryer.repository.ScreenshotRepository

class ScreenshotViewModel(private val repository: ScreenshotRepository) : ViewModel() {

    fun getCollections(): LiveData<List<CollectionModel>> {
        return repository.getCollections()
    }

    fun addCollection(collection: CollectionModel) {
        repository.addCollection(collection)
    }

    fun getScreenshots(): LiveData<List<ScreenshotModel>> {
        return repository.getScreenshots()
    }

    fun getScreenshots(collectionId: String): LiveData<List<ScreenshotModel>> {
        return repository.getScreenshots(collectionId)
    }
}

class ScreenshotViewModelFactory(private val repository: ScreenshotRepository)
    : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ScreenshotViewModel(repository) as T
    }
}