/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import org.mozilla.scryer.persistence.CategoryModel
import org.mozilla.scryer.persistence.ScreenshotModel

class ScreenshotViewModel(private val repository: ScreenshotRepository) : ViewModel() {

    fun getCategories(): LiveData<List<CategoryModel>> {
        return repository.getCategories()
    }

    fun addCategory(category: CategoryModel) {
        repository.addCategory(category)
    }

    fun getScreenshots(): LiveData<List<ScreenshotModel>> {
        return repository.getScreenshots()
    }

    fun getScreenshots(categoryId: String): LiveData<List<ScreenshotModel>> {
        return repository.getScreenshots(categoryId)
    }
}

class ScreenshotViewModelFactory(private val repository: ScreenshotRepository)
    : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ScreenshotViewModel(repository) as T
    }
}