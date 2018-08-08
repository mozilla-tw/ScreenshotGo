/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.capture

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
    }

    private val sortingPanel: SortingPanel by lazy { findViewById<SortingPanel>(R.id.sorting_panel) }

    private val screenshotViewModel: ScreenshotViewModel by lazy {
        ScreenshotViewModel.get(this)
    }

    private lateinit var screenshotModel: ScreenshotModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sorting_panel)

        getValidModel(intent)?.let {
            onNewModelAvailable(it)
        } ?: finish()

        sortingPanel.collectionSource = screenshotViewModel.getCollections()
        sortingPanel.callback = object : SortingPanel.Callback {
            override fun onClick(collection: CollectionModel) {
                onItemClicked(collection)
            }

            override fun onNewCollectionClick() {
                onNewCollectionClicked()
            }
        }
        lifecycle.addObserver(sortingPanel)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        getValidModel(intent)?.let {
            onNewModelAvailable(it)
        } ?: finish()
    }

    private fun onNewModelAvailable(model: ScreenshotModel) {
        screenshotModel = model
        sortingPanel.screenshot = screenshotModel
        screenshotViewModel.addScreenshot(listOf(screenshotModel))
    }

    private fun getValidModel(intent: Intent?): ScreenshotModel? {
        return intent?.let {
            val path = getFilePath(it)
            if (path.isEmpty()) {
                return null
            }

            return ScreenshotModel(null, path, System.currentTimeMillis(), CollectionModel.CATEGORY_NONE)
        }
    }

    private fun getFilePath(intent: Intent): String {
        val path = intent.getStringExtra(EXTRA_PATH)
        val file = File(path)
        return if (file.exists()) file.absolutePath else ""
    }

    private fun onItemClicked(collection: CollectionModel) {
        ScryerToast.makeText(this, "Added to \"${collection.name}\"", Toast.LENGTH_SHORT).show()

        screenshotModel.collectionId = collection.id
        screenshotViewModel.updateScreenshot(screenshotModel)
        finishAndRemoveTask()
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
}
