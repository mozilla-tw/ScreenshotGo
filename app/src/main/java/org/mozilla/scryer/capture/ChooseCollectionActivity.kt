/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.capture

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import org.mozilla.scryer.*
import org.mozilla.scryer.persistence.CollectionModel
import org.mozilla.scryer.persistence.ScreenshotModel
import org.mozilla.scryer.repository.ScreenshotRepository
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

    private val adapter: ChooseCollectionAdapter by lazy {
        ChooseCollectionAdapter(this, this::onItemClicked)
    }

    private val recyclerView: RecyclerView by lazy { findViewById<RecyclerView>(R.id.recycler_view) }
    private lateinit var screenshotViewModel: ScreenshotViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_collection)

        val factory = ScreenshotViewModelFactory(getScreenshotRepository())
        screenshotViewModel = ViewModelProviders.of(this, factory).get(ScreenshotViewModel::class.java)
        val data = screenshotViewModel.getCollections()
        data.observe(this, Observer { collections ->
            collections?.let {
                adapter.collectionList = it
                adapter.notifyDataSetChanged()
            }
        })

        initRecyclerView()

        val path = intent.getStringExtra(EXTRA_PATH)
        val file = File(path)
        if (file.exists()) {
            val bmp = BitmapFactory.decodeFile(file.absolutePath)
            findViewById<ImageView>(R.id.image_view).setImageBitmap(bmp)
        }
        getImagePath()?.let {
            val bmp = BitmapFactory.decodeFile(file.absolutePath)
            findViewById<ImageView>(R.id.image_view).setImageBitmap(bmp)
        } ?: finish()
    }

    private fun getImagePath(): String? {
        val path = intent.getStringExtra(EXTRA_PATH)
        val file = File(path)
        if (file.exists()) {
            return file.absolutePath
        }
        return null
    }

    private fun getScreenshotRepository(): ScreenshotRepository {
        return ScryerApplication.instance.screenshotRepository
    }

    private fun initRecyclerView() {
        recyclerView.layoutManager = GridLayoutManager(this, GRID_SPAN_COUNT,
                GridLayoutManager.VERTICAL,
                false)
        recyclerView.addItemDecoration(GridItemDecoration(dp2px(this, GRID_CELL_SPACE_DP), GRID_SPAN_COUNT))

        recyclerView.adapter = this.adapter
    }

    private fun onItemClicked(collection: CollectionModel) {
        Toast.makeText(this, "save to ${collection.name}", Toast.LENGTH_SHORT).show()

        val path = getImagePath()
        path?.let {
            val screenshot = ScreenshotModel(UUID.randomUUID().toString(), it,
                    System.currentTimeMillis(),
                    collection.id)
            getScreenshotRepository().addScreenshot(screenshot)
        }

        finish()
    }

    fun onNewCollectionClicked() {
        val dialogView = View.inflate(this, R.layout.dialog_add_collection, null)
        val editText = dialogView.findViewById<EditText>(R.id.edit_text)

        val dialog = AlertDialog.Builder(this, R.style.Theme_AppCompat_Light_Dialog_Alert)
                .setView(dialogView)
                .setPositiveButton("DONE") { _, _ ->
                    val model = CollectionModel(UUID.randomUUID().toString(), editText.text.toString(), System.currentTimeMillis())
                    screenshotViewModel.addCollection(model)
                    //recyclerView.adapter.notifyItemInserted(recyclerView.adapter.itemCount - 1)
                }
                .create()
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
    }

    class DataViewHolder(view: View) : RecyclerView.ViewHolder(view)
    class FixedViewHolder(view: View) : RecyclerView.ViewHolder(view)
}

class GridItemDecoration(private val space: Int, private val span: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View,
                                parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view) % span
        outRect.left = if (position == 0) space else space / 2
        outRect.right = if (position == span - 1) space else space / 2
        outRect.bottom = space

        if (parent.getChildLayoutPosition(view) < span) {
            outRect.top = space
        } else {
            outRect.top = 0
        }
    }
}

fun dp2px(context: Context, dp: Float): Int {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics).toInt()
}