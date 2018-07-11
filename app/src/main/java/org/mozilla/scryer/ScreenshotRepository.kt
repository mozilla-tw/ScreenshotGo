/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context

abstract class ScreenshotRepository {
    companion object Factory {
        fun from(context: Context): ScreenshotRepository {
            return InMemoryScreenshotRepository.Holder.INSTANCE
        }
    }

    abstract fun getCategories(): LiveData<List<CategoryModel>>
    abstract fun addCategory(category: CategoryModel)
    abstract fun getScreenshots(): LiveData<List<ScreenshotModel>>
}

class InMemoryScreenshotRepository : ScreenshotRepository() {
    object Holder {
        val INSTANCE = InMemoryScreenshotRepository()
    }

    private val categoryList = mutableListOf<CategoryModel>()
    private val categoryListData: MutableLiveData<List<CategoryModel>> = MutableLiveData()

    private val screenshotList = mutableListOf<ScreenshotModel>()
    private val screenshotListData: MutableLiveData<List<ScreenshotModel>> = MutableLiveData()

    init {
        categoryList.add(CategoryModel("Music"))
        categoryList.add(CategoryModel("Shopping"))
        categoryList.add(CategoryModel("Secret"))
        for (i in 1..20) {
            categoryList.add(CategoryModel("C$i"))
        }
        categoryListData.value = categoryList

        for (i in 1..5) {
            val screenshot = ScreenshotModel("music$i", "Music")
            screenshotList.add(screenshot)
        }

        for (i in 1..3) {
            val screenshot = ScreenshotModel("shopping$i", "Shopping")
            screenshotList.add(screenshot)
        }

        for (i in 1..2) {
            val screenshot = ScreenshotModel("secret$i", "Secret")
            screenshotList.add(screenshot)
        }
        screenshotListData.value = screenshotList
    }

    override fun getCategories(): LiveData<List<CategoryModel>> {
        return categoryListData
    }

    override fun addCategory(category: CategoryModel) {
        categoryList.add(category)
        categoryListData.value = categoryList
    }

    override fun getScreenshots(): LiveData<List<ScreenshotModel>> {
        return screenshotListData
    }
}