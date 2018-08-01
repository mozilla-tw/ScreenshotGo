/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.landingpage

import android.app.Activity
import android.app.AlertDialog
import android.app.SearchManager
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v4.app.Fragment
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.TextView
import androidx.navigation.Navigation
import org.mozilla.scryer.*
import org.mozilla.scryer.detailpage.DetailPageActivity
import org.mozilla.scryer.extension.dpToPx
import org.mozilla.scryer.permission.PermissionFlow
import org.mozilla.scryer.permission.PermissionViewModel
import org.mozilla.scryer.persistence.CollectionModel
import org.mozilla.scryer.persistence.ScreenshotModel
import org.mozilla.scryer.setting.SettingsActivity
import org.mozilla.scryer.ui.GridItemDecoration
import org.mozilla.scryer.viewmodel.ScreenshotViewModel

class HomeFragment : Fragment(), PermissionFlow.ViewDelegate {
    companion object {
        private const val LOG_TAG = "HomeFragment"

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

    private var permissionDialog: AlertDialog? = null

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initQuickAccessList(view.context)
        initCollectionList(view.context)
        initSearchList(view.context)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initActionBar()
        permissionFlow = PermissionFlow(activity!!, this)
    }

    override fun onResume() {
        super.onResume()
        permissionFlow.start()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        activity?.let {
            inflater.inflate(R.menu.menu_main, menu)

            menu.findItem(R.id.action_settings).setOnMenuItemClickListener { _ ->
                startActivity(Intent(it, SettingsActivity::class.java))
                true
            }

            createOptionsMenuSearchView(it, menu)
        }
    }

    override fun showWelcomePage(action: Runnable) {
        showStoragePermissionView("welcome!!", action)
    }

    override fun showStoragePermissionView(title: String, action: Runnable) {
        val activity = activity?: return

        val model = ViewModelProviders.of(activity).get(PermissionViewModel::class.java)
        model.permissionRequest.observe(this, EventObserver {
            permissionFlow.onPermissionResult(MainActivity.REQUEST_CODE_WRITE_EXTERNAL_PERMISSION, it)
        })

        storagePermissionView?.let {
            it.visibility = View.VISIBLE

        }?: apply {
            val stub = view!!.findViewById<ViewStub>(R.id.storage_permission_stub)
            val permissionView = stub.inflate()
            storagePermissionView = permissionView
        }

        storagePermissionView?.findViewById<TextView>(R.id.title)?.text = title
        storagePermissionView?.findViewById<Button>(R.id.action_button)?.setOnClickListener {
            action.run()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun showOverlayPermissionView(action: Runnable, negativeAction: Runnable) {
        dismissPermissionDialog()

        val dialog = AlertDialog.Builder(context, R.style.Theme_AppCompat_Light_Dialog_Alert)
                .setTitle("Instant way to Screenshot")
                .setPositiveButton("GRANT") { _, _ ->
                    action.run()
                }
                .setNegativeButton("CANCEL") { _, _ ->
                    negativeAction.run()
                }
                .create()

        dialog.setCanceledOnTouchOutside(false)
        dialog.setOnKeyListener { _, keyCode, _ -> keyCode == KeyEvent.KEYCODE_BACK }
        dialog.show()

        permissionDialog = dialog
    }

    override fun showCapturePermissionView(action: Runnable, negativeAction: Runnable) {
        dismissPermissionDialog()

        val dialog = AlertDialog.Builder(context, R.style.Theme_AppCompat_Light_Dialog_Alert)
                .setTitle("Try the Screenshot Button")
                .setNegativeButton("LATER") { _, _ ->
                    negativeAction.run()
                }
                .create()

        dialog.setCanceledOnTouchOutside(false)
        dialog.setOnKeyListener { _, keyCode, _ -> keyCode == KeyEvent.KEYCODE_BACK }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                LocalBroadcastManager.getInstance(context).unregisterReceiver(this)
                dialog.dismiss()
            }
        }
        context?.let {
            LocalBroadcastManager.getInstance(it).registerReceiver(receiver,
                    IntentFilter(ScryerService.EVENT_TAKE_SCREENSHOT))
            dialog.setOnDismissListener { _ ->
                LocalBroadcastManager.getInstance(it).unregisterReceiver(receiver)
            }
        }

        dialog.show()
        permissionDialog = dialog
    }

    override fun onStorageGranted() {
        log(LOG_TAG, "onStorageGranted")
        storagePermissionView?.visibility = View.GONE

        // TODO: Currently this will sync screenshots to the database every time after onResume()
        // TODO: Find a better syncing strategy
        //readScreenshotsFromSdcard()
    }

    override fun onOverlayGranted() {
        log(LOG_TAG, "onOverlayGranted")
        dismissPermissionDialog()

        if (ScryerApplication.getSettingsRepository().serviceEnabled) {
            context?.startService(Intent(context, ScryerService::class.java))
        }
    }

    private fun dismissPermissionDialog() = permissionDialog?.takeIf { it.isShowing }?.dismiss()

    private fun initActionBar() {
        setHasOptionsMenu(true)
        setSupportActionBar(activity, view!!.findViewById(R.id.toolbar))
        getSupportActionBar(activity).setDisplayHomeAsUpEnabled(false)
    }

//    private fun readScreenshotsFromSdcard() {
//        // TODO: How to determine the path?
//        val monitorDir = "${Environment.getExternalStorageDirectory()}" +
//                File.separator + "Pictures" +
//                File.separator + "Screenshots"
//
//        val dir = File(monitorDir)
//        if (dir.exists()) {
//            val models = dir.listFiles().map {
//                ScreenshotModel(UUID.randomUUID().toString(), it.absolutePath, it.lastModified(), "")
//            }
//            ScreenshotViewModel.get(this).addScreenshot(models)
//        }
//    }

    private fun createOptionsMenuSearchView(activity: Activity, menu: Menu) {
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
                viewModel.getScreenshots().observe(this@HomeFragment, searchObserver)
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
