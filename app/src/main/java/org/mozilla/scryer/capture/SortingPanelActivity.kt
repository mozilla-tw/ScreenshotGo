/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.capture

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import org.mozilla.scryer.R
import org.mozilla.scryer.persistence.CollectionModel
import org.mozilla.scryer.persistence.ScreenshotModel
import org.mozilla.scryer.sortingpanel.SortingPanel
import org.mozilla.scryer.ui.ScryerToast
import org.mozilla.scryer.viewmodel.ScreenshotViewModel
import java.io.File
import java.util.*

class SortingPanelActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_PATH = "path"
        const val EXTRA_SCREENSHOT_ID = "screenshot_id"
        const val EXTRA_COLLECTION_ID = "collection_id"

        fun sortCollection(context: Context, collectionId: String): Intent {
            val intent = Intent(context, SortingPanelActivity::class.java)
            intent.putExtra(SortingPanelActivity.EXTRA_COLLECTION_ID, collectionId)
            return intent
        }

        fun sortNewScreenshot(context: Context, path: String): Intent {
            val intent = Intent(context, SortingPanelActivity::class.java)
            intent.putExtra(SortingPanelActivity.EXTRA_PATH, path)
            return intent
        }

        fun sortOldScreenshot(context: Context, screenshotId: String): Intent {
            val intent = Intent(context, SortingPanelActivity::class.java)
            intent.putExtra(SortingPanelActivity.EXTRA_SCREENSHOT_ID, screenshotId)
            return intent
        }
    }

    private val sortingPanel: SortingPanel by lazy { findViewById<SortingPanel>(R.id.sorting_panel) }

    private val screenshotViewModel: ScreenshotViewModel by lazy {
        ScreenshotViewModel.get(this)
    }

    private val unsortedScreenshots = LinkedList<ScreenshotModel>()
    private val sortedScreenshots = LinkedList<ScreenshotModel>()
    private var currentScreenshot: ScreenshotModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sorting_panel)

        loadScreenshots(intent, this::onLoadScreenshotsSuccess)
        initSortingPanel()
    }

    override fun onStart() {
        super.onStart()
        this.lifecycle.addObserver(this.sortingPanel)
    }

    override fun onStop() {
        super.onStop()
        this.lifecycle.removeObserver(this.sortingPanel)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        loadScreenshots(intent, this::onLoadScreenshotsSuccess)
    }

    private fun initSortingPanel() {
        sortingPanel.collectionSource = screenshotViewModel.getCollections()
        sortingPanel.callback = object : SortingPanel.Callback {
            override fun onClick(collection: CollectionModel) {
                onCollectionClicked(collection)
            }

            override fun onNewCollectionClick() {
                onNewCollectionClicked()
            }

            override fun onNextClick() {
                onNextClicked()
            }
        }
    }

    private fun onNewModelAvailable() {
        if (unsortedScreenshots.isEmpty()) {
            finishAndRemoveTask()
            return
        }

        currentScreenshot = this.unsortedScreenshots.removeFirst()?.apply {
            sortedScreenshots.addLast(this)
            onScreenshotViewed(this)

            sortingPanel.screenshot = this
            sortingPanel.setProgress(sortedScreenshots.size, sortedScreenshots.size + unsortedScreenshots.size)
        }
    }

    private fun onScreenshotViewed(screenshot: ScreenshotModel) {
        if (screenshot.collectionId == CollectionModel.UNCATEGORIZED) {
            screenshot.collectionId = CollectionModel.CATEGORY_NONE
            screenshotViewModel.updateScreenshot(screenshot)
        }
    }

    private fun loadScreenshots(intent: Intent?, onFinished: (List<ScreenshotModel>) -> Unit) {
        intent?: run {
            onLoadScreenshotsFailed()
            return
        }

        val viewModel = this.screenshotViewModel

        when {
            intent.hasExtra(EXTRA_PATH) -> {
                createNewScreenshot(intent)?.apply {
                    val result = listOf(this)
                    viewModel.addScreenshot(result)
                    onFinished.invoke(result)
                }?: onLoadScreenshotsFailed()
            }

            intent.hasExtra(EXTRA_SCREENSHOT_ID) -> {
                val id = intent.getStringExtra(EXTRA_SCREENSHOT_ID)
                viewModel.getScreenshot(id)?.apply {
                    onFinished.invoke(listOf(this))
                }?: onLoadScreenshotsFailed()
            }

            intent.hasExtra(EXTRA_COLLECTION_ID) -> {
                val id = intent.getStringExtra(EXTRA_COLLECTION_ID)
                val idList = if (id == CollectionModel.CATEGORY_NONE) {
                    listOf(CollectionModel.UNCATEGORIZED, CollectionModel.CATEGORY_NONE)
                } else {
                    listOf(id)
                }
                viewModel.getScreenshotList(idList) {
                    onFinished.invoke(it)
                }
            }

            else -> onLoadScreenshotsFailed()
        }
    }

    private fun onLoadScreenshotsSuccess(screenshots: List<ScreenshotModel>) {
        this.sortedScreenshots.clear()

        this.unsortedScreenshots.clear()
        this.unsortedScreenshots.addAll(screenshots.sortedByDescending { it.lastModified })

        if (screenshots.size == 1) {
            sortingPanel.setActionText(getString(R.string.ac_cancel))
            sortingPanel.setProgressVisibility(View.INVISIBLE)
        } else {
            sortingPanel.setActionText(getString(R.string.ac_next))
            sortingPanel.setProgressVisibility(View.VISIBLE)
        }

        onNewModelAvailable()
    }

    private fun onLoadScreenshotsFailed() {
        finishAndRemoveTask()
    }

    private fun onCollectionClicked(collection: CollectionModel) {
        ScryerToast.makeText(this, "Added to \"${collection.name}\"", Toast.LENGTH_SHORT).show()

        currentScreenshot?.let {
            it.collectionId = collection.id
            screenshotViewModel.addScreenshot(listOf(it))
        }
        onNewModelAvailable()
    }

    private fun onNewCollectionClicked() {
        val dialogView = View.inflate(this, R.layout.dialog_add_collection, null)
        val editText = dialogView.findViewById<EditText>(R.id.edit_text)

        val random = Random()
        val dialog = AlertDialog.Builder(this, R.style.Theme_AppCompat_Light_Dialog_Alert)
                .setView(dialogView)
                .setPositiveButton("DONE") { _, _ ->
                    val model = CollectionModel(editText.text.toString(), System.currentTimeMillis(), Color.argb(255, random.nextInt(255), random.nextInt(255), random.nextInt(255)))
                    screenshotViewModel.addCollection(model)
                }.create()

        dialog.setOnShowListener { dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false }
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = !s.isNullOrEmpty()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        dialog.show()
        editText.requestFocus()
        dialog.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }

    private fun onNextClicked() {
        onNewModelAvailable()
    }

    private fun createNewScreenshot(intent: Intent): ScreenshotModel? {
        val path = getFilePath(intent)
        if (path.isNotEmpty()) {
            return ScreenshotModel(path, System.currentTimeMillis(), CollectionModel.CATEGORY_NONE)
        }
        return null
    }

    private fun getFilePath(intent: Intent): String {
        val path = intent.getStringExtra(EXTRA_PATH)
        val file = File(path)
        return if (file.exists()) file.absolutePath else ""
    }
}
