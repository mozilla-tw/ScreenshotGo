/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.capture

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import org.mozilla.scryer.R
import org.mozilla.scryer.ScryerApplication
import org.mozilla.scryer.persistence.CollectionModel
import org.mozilla.scryer.persistence.ScreenshotModel
import org.mozilla.scryer.ui.GridItemDecoration
import org.mozilla.scryer.ui.dpToPx
import org.mozilla.scryer.viewmodel.ScreenshotViewModel
import org.mozilla.scryer.viewmodel.ScreenshotViewModelFactory
import java.io.File
import java.util.*

class ChooseCollectionActivity : AppCompatActivity() {
    companion object {
        private const val GRID_SPAN_COUNT = 2
        private const val GRID_CELL_SPACE_DP = 6f

        const val EXTRA_PATH = "path"
    }

    private val adapter = ChooseCollectionAdapter(this, this::onItemClicked)

    private val recyclerView: RecyclerView by lazy { findViewById<RecyclerView>(R.id.recycler_view) }
    private val screenshotViewModel: ScreenshotViewModel by lazy {
        val factory = ScreenshotViewModelFactory(ScryerApplication.instance.screenshotRepository)
        ViewModelProviders.of(this, factory).get(ScreenshotViewModel::class.java)
    }

    private lateinit var screenshotModel: ScreenshotModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_collection)

        getValidModel(intent)?.let {
            onNewModelAvailable(it)
        } ?: finish()

        initRecyclerView()

        screenshotViewModel.getCollections()
                .observe(this, Observer { collections ->
                    collections?.filter {
                        it.id != CollectionModel.CATEGORY_NONE
                    }?.let {
                        adapter.collectionList = it
                        adapter.notifyDataSetChanged()
                    }
                })
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        getValidModel(intent)?.let {
            onNewModelAvailable(it)
        } ?: finish()
    }

    private fun onNewModelAvailable(model: ScreenshotModel) {
        screenshotModel = model
        Glide.with(this).load(File(screenshotModel.path)).into(findViewById(R.id.image_view))
        screenshotViewModel.addScreenshot(screenshotModel)
    }

    private fun getValidModel(intent: Intent?): ScreenshotModel? {
        return intent?.let {
            val path = getFilePath(it)
            if (path.isEmpty()) {
                return null
            }

            return ScreenshotModel(UUID.randomUUID().toString(), path,
                    System.currentTimeMillis(),
                    "")
        }
    }

    private fun getFilePath(intent: Intent): String {
        val path = intent.getStringExtra(EXTRA_PATH)
        val file = File(path)
        return if (file.exists()) file.absolutePath else ""
    }

    private fun initRecyclerView() {
        recyclerView.layoutManager = GridLayoutManager(this, GRID_SPAN_COUNT,
                GridLayoutManager.VERTICAL,
                false)

        recyclerView.addItemDecoration(GridItemDecoration(GRID_SPAN_COUNT,
                GRID_CELL_SPACE_DP.dpToPx(this.resources.displayMetrics)))
        recyclerView.adapter = this.adapter
    }

    private fun onItemClicked(collection: CollectionModel) {
        Toast.makeText(this, "save to ${collection.name}", Toast.LENGTH_SHORT).show()
        screenshotModel.collectionId = collection.id
        screenshotViewModel.updateScreenshot(screenshotModel)
        finish()
    }

    private fun onNewCollectionClicked() {
        val dialogView = View.inflate(this, R.layout.dialog_add_collection, null)
        val editText = dialogView.findViewById<EditText>(R.id.edit_text)

        val dialog = AlertDialog.Builder(this, R.style.Theme_AppCompat_Light_Dialog_Alert)
                .setView(dialogView)
                .setPositiveButton("DONE") { _, _ ->
                    val model = CollectionModel(editText.text.toString(), System.currentTimeMillis())
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

    private class ChooseCollectionAdapter(private var activity: ChooseCollectionActivity,
                                          private var itemClickListener: (CollectionModel) -> Unit)
        : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        companion object {
            private const val TYPE_FIXED_ITEM = 0
            private const val TYPE_DATA_ITEM = 1

            private const val POSITION_NEW_COLLECTION = 0
        }

        var collectionList: List<CollectionModel>? = null
        private val fixedItems = listOf("Create new collection")

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            when (viewType) {
                TYPE_FIXED_ITEM -> {
                    return createFixedViewHolder(parent)
                }

                TYPE_DATA_ITEM -> {
                    return createDataViewHolder(parent)
                }
            }
            throw IllegalStateException("unexpected item type $viewType")
        }

        override fun getItemCount(): Int {
            return fixedItems.size + (collectionList?.size?:0)
        }

        override fun getItemViewType(position: Int): Int {
            return if (position < fixedItems.size) TYPE_FIXED_ITEM else TYPE_DATA_ITEM
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is DataViewHolder) {
                (holder.itemView as TextView).text = collectionList?.let {
                    it[position - fixedItems.size].name
                }
            } else if (holder is FixedViewHolder) {
                (holder.itemView as TextView).text = fixedItems[position]
            }
        }

        private fun createFixedViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
            val view = TextView(parent.context)
            view.setBackgroundColor(Color.LTGRAY)
            view.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 500)
            view.setTextColor(Color.BLACK)
            view.gravity = Gravity.CENTER

            val holder = FixedViewHolder(view)
            holder.itemView.setOnClickListener { _ ->
                holder.adapterPosition.takeIf { position ->
                    position != RecyclerView.NO_POSITION

                }?.let { position: Int ->
                    onFixedItemClicked(position)
                }
            }
            return holder
        }

        private fun createDataViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
            val view = TextView(parent.context)
            view.setBackgroundColor(Color.LTGRAY)
            view.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 500)
            view.setTextColor(Color.BLACK)
            view.gravity = Gravity.CENTER

            val holder = DataViewHolder(view)
            holder.itemView.setOnClickListener { _ ->
                holder.adapterPosition.takeIf { position ->
                    position != RecyclerView.NO_POSITION

                }?.let { position: Int ->
                    collectionList?.let {
                        itemClickListener.invoke(it[position - fixedItems.size])
                    }
                }
            }
            return holder
        }

        private fun createNewCollection() {
            activity.onNewCollectionClicked()
        }

        private fun onFixedItemClicked(position :Int) {
            when (position) {
                POSITION_NEW_COLLECTION -> {
                    createNewCollection()
                }
            }
        }

        class DataViewHolder(view: View) : RecyclerView.ViewHolder(view)
        class FixedViewHolder(view: View) : RecyclerView.ViewHolder(view)
    }
}
