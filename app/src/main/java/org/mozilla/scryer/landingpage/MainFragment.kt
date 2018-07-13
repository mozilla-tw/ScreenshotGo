/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.landingpage

import android.annotation.SuppressLint
import android.app.SearchManager
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.view.*
import android.widget.*
import androidx.navigation.Navigation
import org.mozilla.scryer.*
import org.mozilla.scryer.capture.GridItemDecoration
import org.mozilla.scryer.capture.dp2px
import org.mozilla.scryer.persistence.CollectionModel
import org.mozilla.scryer.persistence.ScreenshotModel
import org.mozilla.scryer.repository.ScreenshotRepository
import org.mozilla.scryer.viewmodel.ScreenshotViewModel
import org.mozilla.scryer.viewmodel.ScreenshotViewModelFactory

class MainFragment : Fragment() {
    private lateinit var quickAccessListView: RecyclerView
    private val quickAccessAdapter: QuickAccessAdapter = QuickAccessAdapter()

    private lateinit var mainListView: RecyclerView
    private val mainAdapter: MainAdapter = MainAdapter()

    private lateinit var searchListView: RecyclerView
    private val searchListAdapter: SearchAdapter = SearchAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val layout = inflater.inflate(R.layout.fragment_main, container, false)
        mainListView = layout.findViewById(R.id.main_list)
        quickAccessListView = RecyclerView(inflater.context)
        searchListView = layout.findViewById(R.id.search_list)
        return layout
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
        (activity as? AppCompatActivity)?.setSupportActionBar(view?.findViewById(R.id.toolbar))
        getSupportActionBar(activity)?.setDisplayHomeAsUpEnabled(false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initQuickAccessList(view.context)
        initCollectionList(view.context)
        initSearchList(view.context)
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        menu?.let {
            createOptionsMenuSearchView(menu)
        }
    }

    private fun createOptionsMenuSearchView(menu: Menu) {
        activity?.run {
            menuInflater.inflate(R.menu.menu_main, menu)
            val searchItem = menu.findItem(R.id.action_search)

            val searchView = searchItem.actionView as SearchView
            searchView.setIconifiedByDefault(true)
            searchView.findViewById<View>(R.id.search_plate)?.setBackgroundColor(Color.TRANSPARENT)

            val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
            searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))

            searchView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewDetachedFromWindow(v: View?) {
                    searchListView.visibility = View.INVISIBLE
                    mainListView.visibility = View.VISIBLE
                }

                override fun onViewAttachedToWindow(v: View?) {
                    searchListView.visibility = View.VISIBLE
                    mainListView.visibility = View.INVISIBLE
                }

            })

            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    searchListAdapter.filter.filter(query)
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    searchListAdapter.filter.filter(newText)
                    return false
                }
            })
        }
    }

    private fun initQuickAccessList(context: Context) {
        val manager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        quickAccessListView.layoutManager = manager
        quickAccessListView.adapter = quickAccessAdapter

        val factory = ScreenshotViewModelFactory(getScreenshotRepository())
        ViewModelProviders.of(this, factory).get(ScreenshotViewModel::class.java)
                .getScreenshots()
                .observe(this, Observer { screenshots ->
                    screenshots?.let { newList ->
                        updateQuickAccessListView(newList)
                    }
                })
    }

    private fun initCollectionList(context: Context) {
        val manager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        mainListView.layoutManager = manager
        mainListView.adapter = mainAdapter
        mainListView.recycledViewPool.setMaxRecycledViews(MainAdapter.TYPE_QUICK_ACCESS, 1)
        mainAdapter.quickAccessListView = quickAccessListView

        val factory = ScreenshotViewModelFactory(getScreenshotRepository())
        ViewModelProviders.of(this, factory).get(ScreenshotViewModel::class.java)
                .getCollections()
                .observe(this, Observer { collections ->
                    collections?.let { newData ->
                        updateCollectionListView(newData)
                    }
                })
    }

    private fun initSearchList(context: Context) {
        val manager = GridLayoutManager(context, 2, GridLayoutManager.VERTICAL, false)
        searchListView.layoutManager = manager
        searchListView.adapter = searchListAdapter
        searchListView.addItemDecoration(GridItemDecoration(dp2px(context, 8f), 2))
        val factory = ScreenshotViewModelFactory(getScreenshotRepository())
        ViewModelProviders.of(this, factory).get(ScreenshotViewModel::class.java)
                .getScreenshots()
                .observe(this, Observer { screenshots ->
                    screenshots?.let {newData ->
                        searchListAdapter.setScreenshotList(newData)
                    }
                })
    }

    private fun updateQuickAccessListView(screenshots: List<ScreenshotModel>) {
        quickAccessAdapter.list = screenshots
        quickAccessAdapter.notifyDataSetChanged()
    }

    private fun updateCollectionListView(collections: List<CollectionModel>) {
        mainAdapter.collectionList = collections
        mainAdapter.notifyDataSetChanged()
    }

    private fun getScreenshotRepository(): ScreenshotRepository {
        return ScryerApplication.getInstance().screenshotRepository
    }

    private class MainAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        companion object {
            const val TYPE_SECTION_NAME = 0
            const val TYPE_QUICK_ACCESS = 1
            const val TYPE_ROW = 2

            const val POS_QUICK_ACCESS_TITLE = 0
            const val POS_QUICK_ACCESS_LIST = 1
            const val POS_COLLECTION_LIST_TITLE = 2

            const val FIXED_ITEM_COUNT = 3
            const val COLUMN_COUNT = 2
        }

        var collectionList: List<CollectionModel> = emptyList()
        lateinit var quickAccessListView: RecyclerView

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            when (viewType) {
                TYPE_SECTION_NAME ->
                    return createSectionNameHolder(parent)
                TYPE_QUICK_ACCESS ->
                    return createQuickAccessHolder()
                TYPE_ROW ->
                    return createRowHolder(parent)
            }
            throw IllegalArgumentException("unexpected view type: $viewType")
        }

        override fun getItemCount(): Int {
            return FIXED_ITEM_COUNT + ((collectionList.size - 1) / COLUMN_COUNT) + 1
        }

        override fun getItemViewType(position: Int): Int {
            return when (position) {
                POS_QUICK_ACCESS_TITLE -> TYPE_SECTION_NAME
                POS_QUICK_ACCESS_LIST -> TYPE_QUICK_ACCESS
                POS_COLLECTION_LIST_TITLE -> TYPE_SECTION_NAME
                else -> TYPE_ROW
            }
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (position) {
                POS_QUICK_ACCESS_TITLE -> (holder as SectionNameHolder).title?.text = "Quick Access"
                POS_QUICK_ACCESS_LIST -> return
                POS_COLLECTION_LIST_TITLE -> (holder as SectionNameHolder).title?.text = "Collections"
                else -> bindRowHolder(holder, position)
            }
        }

        private fun createSectionNameHolder(parent: ViewGroup): SectionNameHolder {
            val textView = TextView(parent.context)
            val padding = dp2px(parent.context, 10f)
            textView.setPadding(padding, padding, padding, padding)
            textView.setTextColor(Color.BLACK)

            val holder = SectionNameHolder(textView)
            holder.title = textView
            return holder
        }

        private fun createQuickAccessHolder(): RecyclerView.ViewHolder {
            return SimpleHolder(quickAccessListView)
        }

        private fun createRowHolder(parent: ViewGroup): RowHolder {
            val inflater = LayoutInflater.from(parent.context)

            val rowLayout = LinearLayout(parent.context)
            val itemHolders = mutableListOf<RowItemHolder>()
            val rowHolder = RowHolder(rowLayout, itemHolders)

            val params = LinearLayout.LayoutParams(0, dp2px(parent.context, 200f))
            params.weight = 1f

            val padding = dp2px(parent.context, 8f)
            for (i in 0 until COLUMN_COUNT) {
                val container = FrameLayout(parent.context)
                container.setPadding(if (i == 0) padding else padding / 2, 0,
                        if (i == COLUMN_COUNT - 1) padding else padding / 2, padding)
                rowLayout.addView(container, params)

                val view = inflater.inflate(R.layout.item_collection, container, true)
                view.setOnClickListener {_ ->
                    rowHolder.adapterPosition.takeIf { position ->
                        position != RecyclerView.NO_POSITION

                    }?.let { position: Int ->
                        val row = position - FIXED_ITEM_COUNT
                        val startIndex = row * COLUMN_COUNT
                        val bundle = Bundle()
                        bundle.putString(CollectionFragment.ARG_COLLECTION_ID, collectionList[startIndex + i].id)
                        Navigation.findNavController(parent).navigate(R.id.action_navigate_to_collection, bundle)
                    }
                }

                val itemHolder = RowItemHolder(view)
                itemHolder.title = view.findViewById(R.id.title)
                itemHolders.add(itemHolder)
            }
            return rowHolder
        }

        private fun bindRowHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val row = position - FIXED_ITEM_COUNT
            val rowHolder = holder as RowHolder

            val startIndex = row * COLUMN_COUNT
            for (i in 0 until COLUMN_COUNT) {
                val offsetIndex = startIndex + i
                if (offsetIndex < collectionList.size) {
                    bindRowItem(rowHolder.holderList[i], collectionList[offsetIndex])
                } else {
                    hideRowItem(rowHolder.holderList[i])
                }
            }
        }

        private fun bindRowItem(item: RowItemHolder, collectionModel: CollectionModel) {
            item.itemView.visibility = View.VISIBLE
            item.title?.text = collectionModel.name
        }

        private fun hideRowItem(item: RowItemHolder) {
            item.itemView.visibility = View.INVISIBLE
        }

        class SimpleHolder(itemView: View): RecyclerView.ViewHolder(itemView)

        class SectionNameHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
            var title: TextView? = null
        }

        class RowHolder(itemView: View,
                        val holderList: List<RowItemHolder>) : RecyclerView.ViewHolder(itemView)

        class RowItemHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
            var title: TextView? = null
        }
    }
}
