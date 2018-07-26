/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.landingpage

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.SearchManager
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.util.Log
import android.view.*
import android.widget.Button
import androidx.navigation.Navigation
import org.mozilla.scryer.*
import org.mozilla.scryer.detailpage.DetailPageActivity
import org.mozilla.scryer.extension.dpToPx
import org.mozilla.scryer.overlay.OverlayPermission
import org.mozilla.scryer.permission.PermissionFlow
import org.mozilla.scryer.permission.PermissionViewModel
import org.mozilla.scryer.persistence.CollectionModel
import org.mozilla.scryer.persistence.ScreenshotModel
import org.mozilla.scryer.ui.GridItemDecoration
import org.mozilla.scryer.viewmodel.ScreenshotViewModel

class MainFragment : Fragment(), PermissionFlow.ViewDelegate {
    companion object {
        private const val LOG_TAG = "MainFragment"

        const val COLLECTION_COLUMN_COUNT = 2
        const val QUICK_ACCESS_ITEM_COUNT = 5
    }

    private lateinit var quickAccessListView: RecyclerView
    private val quickAccessAdapter: QuickAccessAdapter = QuickAccessAdapter()

    private lateinit var mainListView: RecyclerView
    private val mainAdapter: MainAdapter = MainAdapter()

    private lateinit var searchListView: RecyclerView
    private val searchListAdapter: SearchAdapter = SearchAdapter()

    private lateinit var permissionFlow: PermissionFlow
    private var storagePermissionView: View? = null

    private val viewModel: ScreenshotViewModel by lazy {
        ScreenshotViewModel.get(this)
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
        setSupportActionBar(activity, view!!.findViewById(R.id.toolbar))
        getSupportActionBar(activity).setDisplayHomeAsUpEnabled(false)
    }

    override fun onResume() {
        super.onResume()
        permissionFlow = PermissionFlow(activity!!, this)
        permissionFlow.start()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initQuickAccessList(view.context)
        initCollectionList(view.context)
        initSearchList(view.context)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        activity?.let {
            createOptionsMenuSearchView(it, menu, inflater)
        }
    }

    override fun askForStoragePermission() {
        val activity = activity?: return

        val viewModel = ViewModelProviders.of(activity).get(PermissionViewModel::class.java)
        val liveData = viewModel.permission(MainActivity.REQUEST_CODE_WRITE_EXTERNAL_PERMISSION)
        log(LOG_TAG, "storage viewModel: $viewModel, liveData: $liveData")

        liveData.observe(activity, Observer {
            viewModel.consume(MainActivity.REQUEST_CODE_WRITE_EXTERNAL_PERMISSION)
            log(LOG_TAG, "storage liveData observed: $liveData")
            permissionFlow.next()
        })

        storagePermissionView?.let {
            it.visibility = View.VISIBLE

        }?: apply {
            val stub = view!!.findViewById<ViewStub>(R.id.storage_permission_stub)
            val permissionView = stub.inflate()
            val buttonView = permissionView.findViewById<Button>(R.id.action_button)
            buttonView.setOnClickListener {
                requestPermissionsViaActivity(activity)
            }
            storagePermissionView = permissionView
        }
    }

    override fun onStorageGranted() {
        log(LOG_TAG, "onStorageGranted")
        storagePermissionView?.visibility = View.GONE
    }

    private var overlayPermissionDialog: AlertDialog? = null

    @RequiresApi(Build.VERSION_CODES.M)
    override fun askForOverlayPermission() {
        val activity = activity?: return

        overlayPermissionDialog?.show() ?: run {
            val dialog = AlertDialog.Builder(context, R.style.Theme_AppCompat_Light_Dialog_Alert)
                    .setTitle("YOU SHALL NOT PASS!!!!")
                    .setPositiveButton("PLEASE!!!!") { _, _ ->
                        requestOverlayPermissionViaActivity(activity)
                    }
                    .create()

            dialog.setCanceledOnTouchOutside(false)
            dialog.setOnKeyListener { _, keyCode, _ -> keyCode == KeyEvent.KEYCODE_BACK }
            dialog.show()

            overlayPermissionDialog = dialog
        }
    }

    override fun onOverlayGranted() {
        log(LOG_TAG, "onOverlayGranted")
        overlayPermissionDialog?.dismiss()
        context?.applicationContext?.apply {
            val intent = Intent(this, ScryerService::class.java)
            this.startService(intent)
        }
    }

    private fun requestPermissionsViaActivity(activity: Activity) {
        log(LOG_TAG, "request permission: ${MainActivity.REQUEST_CODE_WRITE_EXTERNAL_PERMISSION}")
        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                MainActivity.REQUEST_CODE_WRITE_EXTERNAL_PERMISSION)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestOverlayPermissionViaActivity(activity: Activity) {
        val intent = OverlayPermission.createPermissionIntent(activity)
        activity.startActivityForResult(intent, MainActivity.REQUEST_CODE_OVERLAY_PERMISSION)
    }

    private fun createOptionsMenuSearchView(activity: Activity, menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
        val searchItem = menu.findItem(R.id.action_search)

        val searchView = searchItem.actionView as SearchView
        searchView.setIconifiedByDefault(true)
        searchView.findViewById<View>(R.id.search_plate)?.setBackgroundColor(Color.TRANSPARENT)

        val searchManager = activity.getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchView.setSearchableInfo(searchManager.getSearchableInfo(activity.componentName))

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

    private fun initQuickAccessList(context: Context) {
        val manager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        quickAccessListView.layoutManager = manager
        quickAccessListView.adapter = quickAccessAdapter
        quickAccessAdapter.clickListener = object : QuickAccessAdapter.ItemClickListener {
            override fun onItemClick(screenshotModel: ScreenshotModel, holder: ScreenshotItemHolder) {
                DetailPageActivity.showDetailPage(context, screenshotModel.path, holder.image)
            }

            override fun onMoreClick(holder: ScreenshotItemHolder) {
                Navigation.findNavController(holder.itemView).navigate(R.id.action_navigate_to_collection, Bundle())
            }
        }

        val space = 8f.dpToPx(context.resources.displayMetrics)
        quickAccessListView.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                val position = parent.getChildAdapterPosition(view)
                if (position == 0) {
                    outRect.left = space
                }
                outRect.right = space
            }
        })

        viewModel.getScreenshots().observe(this, Observer { screenshots ->
            screenshots?.let { newList ->
                val finalList = newList.sortedByDescending { it.date }
                        .subList(0, Math.min(newList.size, QUICK_ACCESS_ITEM_COUNT + 1))
                updateQuickAccessListView(finalList)
            }
        })
    }

    private fun initCollectionList(context: Context) {
        val manager = GridLayoutManager(context, COLLECTION_COLUMN_COUNT, GridLayoutManager.VERTICAL, false)
        manager.spanSizeLookup = MainAdapter.SpanSizeLookup(COLLECTION_COLUMN_COUNT)
        mainListView.layoutManager = manager

        mainAdapter.quickAccessListView = quickAccessListView
        mainListView.adapter = mainAdapter

        val space = 8f.dpToPx(context.resources.displayMetrics)
        mainListView.addItemDecoration(MainAdapter.ItemDecoration(COLLECTION_COLUMN_COUNT, space))

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
        val manager = GridLayoutManager(context, COLLECTION_COLUMN_COUNT, GridLayoutManager.VERTICAL, false)
        searchListView.layoutManager = manager
        searchListView.adapter = searchListAdapter
        val space = 8f.dpToPx(context.resources.displayMetrics)
        searchListView.addItemDecoration(GridItemDecoration(COLLECTION_COLUMN_COUNT, space))
    }

    private fun updateQuickAccessListView(screenshots: List<ScreenshotModel>) {
        quickAccessAdapter.list = screenshots
        quickAccessAdapter.notifyDataSetChanged()
    }

    private fun updateCollectionListView(collections: List<CollectionModel>) {
        mainAdapter.collectionList = collections
        mainAdapter.notifyDataSetChanged()
    }

    private fun log(tag: String, msg: String) {
        Log.d(tag, msg)
    }
}
