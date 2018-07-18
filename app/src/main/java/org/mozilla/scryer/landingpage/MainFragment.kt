/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.landingpage

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
import org.mozilla.scryer.R
import org.mozilla.scryer.ScryerApplication
import org.mozilla.scryer.capture.GridItemDecoration
import org.mozilla.scryer.capture.dp2px
import org.mozilla.scryer.getSupportActionBar
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

    private val viewModel: ScreenshotViewModel by lazy {
        val factory = ScreenshotViewModelFactory(getScreenshotRepository())
        ViewModelProviders.of(this, factory).get(ScreenshotViewModel::class.java)
    }

    private val searchObserver = Observer<List<ScreenshotModel>> { screenshots ->
        screenshots?.let { newData ->
            searchListAdapter.setScreenshotList(newData)
        }
    }

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
        (activity as AppCompatActivity).setSupportActionBar(view?.findViewById(R.id.toolbar))
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
                    viewModel.getScreenshots().removeObserver(searchObserver)
                }

                override fun onViewAttachedToWindow(v: View?) {
                    searchListView.visibility = View.VISIBLE
                    mainListView.visibility = View.INVISIBLE
                    viewModel.getScreenshots().observe(this@MainFragment, searchObserver)
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

        viewModel.getScreenshots().observe(this, Observer { screenshots ->
            screenshots?.let { newList ->
                val finalList = newList.sortedByDescending { it.date }
                        .subList(0, Math.min(newList.size, 5))
                updateQuickAccessListView(finalList)
            }
        })
    }

    private fun initCollectionList(context: Context) {
        val manager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        mainListView.layoutManager = manager
        mainListView.adapter = mainAdapter
        mainListView.recycledViewPool.setMaxRecycledViews(MainAdapter.TYPE_QUICK_ACCESS, 1)
        mainAdapter.quickAccessListView = quickAccessListView

        viewModel.getCollections().observe(this, Observer { collections ->
            collections?.let { newData ->
                updateCollectionListView(newData)
            }
        })

        viewModel.getCollectionCovers().observe(this, Observer { coverMap ->
            coverMap?.let { newData ->
                mainAdapter.coverList = newData
                mainAdapter.notifyDataSetChanged()
            }
        })
    }

    private fun initSearchList(context: Context) {
        val manager = GridLayoutManager(context, 2, GridLayoutManager.VERTICAL, false)
        searchListView.layoutManager = manager
        searchListView.adapter = searchListAdapter
        searchListView.addItemDecoration(GridItemDecoration(dp2px(context, 8f), 2))
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
        return ScryerApplication.instance.screenshotRepository
    }

}
