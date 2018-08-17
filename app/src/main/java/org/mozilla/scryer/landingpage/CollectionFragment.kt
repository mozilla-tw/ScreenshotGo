/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.landingpage

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.ActionBar
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import org.mozilla.scryer.Observer
import org.mozilla.scryer.R
import org.mozilla.scryer.capture.SortingPanelActivity
import org.mozilla.scryer.detailpage.DetailPageActivity
import org.mozilla.scryer.getSupportActionBar
import org.mozilla.scryer.persistence.CollectionModel
import org.mozilla.scryer.persistence.ScreenshotModel
import org.mozilla.scryer.setSupportActionBar
import org.mozilla.scryer.ui.ScryerToast
import org.mozilla.scryer.viewmodel.ScreenshotViewModel
import java.io.File

class CollectionFragment : Fragment() {
    companion object {
        const val ARG_COLLECTION_ID = "collection_id"
        const val ARG_COLLECTION_NAME = "collection_name"

        private const val SPAN_COUNT = 3
    }

    private lateinit var screenshotListView: RecyclerView
    private lateinit var subtitleView: TextView

    private val screenshotAdapter = ScreenshotAdapter()

    private val collectionId: String? by lazy {
        arguments?.getString(ARG_COLLECTION_ID)
    }

    private val collectionName: String? by lazy {
        arguments?.getString(ARG_COLLECTION_NAME)
    }

    private var sortMenuItem: MenuItem? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val layout = inflater.inflate(R.layout.fragment_collection, container, false)
        screenshotListView = layout.findViewById(R.id.screenshot_list)
        subtitleView = layout.findViewById(R.id.subtitle)
        return layout
    }

    override fun getView(): View {
        return super.getView()!!
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
        setupActionBar()
        initScreenshotList(view.context)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> Navigation.findNavController(view).navigateUp()
            R.id.action_sort -> {
                collectionId?.takeIf {
                    screenshotAdapter.getScreenshotList().isNotEmpty()
                }?.let {
                    startSortingActivity(it)
                }
            }
            R.id.action_search -> {
                context?.let {
                    ScryerToast.makeText(it, "Not implement", Toast.LENGTH_SHORT).show()
                }
            }
            else -> return super.onOptionsItemSelected(item)
        }

        return super.onOptionsItemSelected(item)
    }

    private fun startSortingActivity(collectionId: String) {
        context?.let {
            startActivity(SortingPanelActivity.sortCollection(it, collectionId))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_collection, menu)
        sortMenuItem = menu.findItem(R.id.action_sort)
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun setupActionBar() {
        setSupportActionBar(activity, view.findViewById(R.id.toolbar))
        getSupportActionBar(activity).apply {
            setDisplayHomeAsUpEnabled(true)
            updateActionBarTitle(this)
        }
    }

    private fun updateActionBarTitle(actionBar: ActionBar) {
        actionBar.title = collectionName?.let { it } ?: "All"
    }

    private fun initScreenshotList(context: Context) {
        val manager = GridLayoutManager(context, SPAN_COUNT, GridLayoutManager.VERTICAL, false)
        screenshotListView.layoutManager = manager
        screenshotListView.adapter = screenshotAdapter

        val itemSpace = context.resources.getDimensionPixelSize(R.dimen.collection_item_space)
        val horizontalPadding = context.resources.getDimensionPixelSize(R.dimen.collection_horizontal_padding)

        val listViewPadding = horizontalPadding - itemSpace
        screenshotListView.setPadding(listViewPadding, 0, listViewPadding, 0)

        screenshotListView.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                val position = parent.getChildViewHolder(view).adapterPosition
                if (position < 0) {
                    return
                }

                outRect.left = itemSpace / 2
                outRect.right = itemSpace / 2
                outRect.bottom = itemSpace
            }
        })

        val viewModel = ScreenshotViewModel.get(this)
        val liveData = collectionId?.let {
            val idList = if (it == CollectionModel.CATEGORY_NONE) {
                listOf(CollectionModel.UNCATEGORIZED, CollectionModel.CATEGORY_NONE)
            } else {
                listOf(it)
            }
            viewModel.getScreenshots(idList)

        } ?: viewModel.getScreenshots()

        liveData.observe(this, Observer { screenshots ->
            sortMenuItem?.isVisible = screenshots.isNotEmpty()

            screenshots.sortedByDescending { it.lastModified }.let { sorted ->
                screenshotAdapter.setScreenshotList(sorted)
                screenshotAdapter.notifyDataSetChanged()
                subtitleView.text = getString(R.string.collection_page_subtitle, sorted.size)
            }
        })
    }
}

open class ScreenshotAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var screenshotList: List<ScreenshotModel> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_screenshot, parent, false)

        val holder = ScreenshotItemHolder(view)
        holder.title = view.findViewById(R.id.title)
        holder.image = view.findViewById(R.id.image_view)
        holder.itemView.setOnClickListener { _ ->
            holder.adapterPosition.takeIf { position ->
                position != RecyclerView.NO_POSITION

            }?.let { position: Int ->
                DetailPageActivity.showDetailPage(parent.context, screenshotList[position].absolutePath, holder.image)
            }
        }
        return holder
    }

    override fun getItemCount(): Int {
        return screenshotList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as? ScreenshotItemHolder)?.apply {
            title?.text = screenshotList[position].collectionId
            image?.let {
                Glide.with(holder.itemView.context)
                        .load(File(screenshotList[position].absolutePath))
                        .into(it)
            }
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        (holder as? ScreenshotItemHolder)?.apply {
            image?.let {
                Glide.with(holder.itemView.context)
                        .clear(it)
            }
        }
    }

    fun getItemFileName(position: Int): String {
        val item = screenshotList[position]
        return item.absolutePath.substring(item.absolutePath.lastIndexOf(File.separator) + 1)
    }

    fun getItem(position: Int): ScreenshotModel {
        return screenshotList[position]
    }

    open fun setScreenshotList(list: List<ScreenshotModel>) {
        screenshotList = list
    }

    fun getScreenshotList(): List<ScreenshotModel> {
        return screenshotList
    }
}

class ScreenshotItemHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
    var title: TextView? = null
    var image: ImageView? = null
}