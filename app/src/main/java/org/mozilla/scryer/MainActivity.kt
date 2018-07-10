/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer

import android.Manifest
import android.app.SearchManager
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.view.*
import android.widget.TextView
import org.mozilla.scryer.overlay.OverlayPermission
import org.mozilla.scryer.overlay.ScreenshotMenuService

class MainActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_CODE_OVERLAY_PERMISSION = 1000
        private const val REQUEST_CODE_READ_EXTERNAL_PERMISSION = 1001

        private const val GRID_SPAN_COUNT = 2
        private const val GRID_CELL_SPACE_DP = 6f
    }

    private var overlayRequested = false
    private var storageRequested = false

    private val quickAccessListView: RecyclerView by lazy {
        findViewById<RecyclerView>(R.id.quick_access_list)
    }
    private val quickAccessListAdapter: QuickAccessListAdapter = QuickAccessListAdapter()

    private val categoryListView: RecyclerView by lazy {
        findViewById<RecyclerView>(R.id.category_list)
    }
    private val categoryListAdapter: CategoryListAdapter = CategoryListAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ensureOverlayPermission()

        initQuickAccessList()
        initCategoryList()
    }

    override fun onResume() {
        super.onResume()
        if (OverlayPermission.hasPermission(this)) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                applicationContext.startService(Intent(applicationContext, ScreenshotMenuService::class.java))
            } else if (!storageRequested) {
                ensureStoragePermission()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CODE_OVERLAY_PERMISSION -> overlayRequested = true
            REQUEST_CODE_READ_EXTERNAL_PERMISSION -> {}
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        createOptionsMenuSearchView(menu)
        return true
    }

    private fun createOptionsMenuSearchView(menu: Menu) {
        menuInflater.inflate(R.menu.menu_main, menu)
        val searchItem = menu.findItem(R.id.action_search)

        val searchView = searchItem.actionView as SearchView
        searchView.setIconifiedByDefault(true)
        searchView.findViewById<View>(R.id.search_plate)?.setBackgroundColor(Color.TRANSPARENT)

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
    }

    private fun ensureOverlayPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || OverlayPermission.hasPermission(this)) {
            overlayRequested = true

        } else if (!overlayRequested) {
            val intent = OverlayPermission.createPermissionIntent(this)
            startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION)
        }
    }

    private fun ensureStoragePermission() {
        storageRequested = true
        ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_CODE_READ_EXTERNAL_PERMISSION)
    }

    private fun initQuickAccessList() {
        val manager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        quickAccessListView.layoutManager = manager
        quickAccessListView.adapter = quickAccessListAdapter

        val factory = ScreenshotViewModelFactory(getScreenshotRepository())
        ViewModelProviders.of(this, factory).get(ScreenshotViewModel::class.java)
                .getScreenshots()
                .observe(this, Observer { screenshots ->
                    screenshots?.let { newList ->
                        updateQuickAccessListView(newList)
                    }
                })
    }

    private fun initCategoryList() {
        val manager = GridLayoutManager(this, GRID_SPAN_COUNT, GridLayoutManager.VERTICAL, false)
        categoryListView.layoutManager = manager
        categoryListView.adapter = categoryListAdapter
        categoryListView.addItemDecoration(SpacesItemDecoration(dp2px(this, GRID_CELL_SPACE_DP)))

        val factory = ScreenshotViewModelFactory(getScreenshotRepository())
        ViewModelProviders.of(this, factory).get(ScreenshotViewModel::class.java)
                .getCategories()
                .observe(this, Observer { collections ->
                    collections?.let { newData ->
                        updateCategoryListView(newData)
                    }
                })
    }

    private fun updateQuickAccessListView(screenshots: List<ScreenshotModel>) {
        quickAccessListAdapter.setData(screenshots)
        quickAccessListAdapter.notifyDataSetChanged()
    }

    private fun updateCategoryListView(categories: List<CategoryModel>) {
        categoryListAdapter.setData(categories)
        categoryListAdapter.notifyDataSetChanged()
    }

    private fun getScreenshotRepository(): ScreenshotRepository {
        return ScreenshotRepository.from(this)
    }
}

class QuickAccessListAdapter: RecyclerView.Adapter<ScreenshotHolder>() {
    private var list: List<ScreenshotModel>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScreenshotHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_quick_access, parent, false)

        val holder = ScreenshotHolder(view)
        holder.title = view.findViewById(R.id.title)
        holder.itemView.setOnClickListener { _ ->
            holder.adapterPosition.takeIf { position ->
                position != RecyclerView.NO_POSITION

            }?.let { _: Int ->

            }
        }
        return holder
    }

    override fun getItemCount(): Int {
        return list?.size?: 0
    }

    override fun onBindViewHolder(holder: ScreenshotHolder, position: Int) {
        list?.let {
            holder.title?.text = it[position].name
        }
    }

    fun setData(categories: List<ScreenshotModel>?) {
        list = categories
    }
}

class CategoryListAdapter: RecyclerView.Adapter<CollectionHolder>() {
    private var list: List<CategoryModel>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectionHolder {
        val view = TextView(parent.context)
        view.setBackgroundColor(Color.LTGRAY)
        view.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 500)
        view.setTextColor(Color.BLACK)
        view.gravity = Gravity.CENTER

        val holder = CollectionHolder(view)
        holder.title = holder.itemView as? TextView
        holder.itemView.setOnClickListener { _ ->
            holder.adapterPosition.takeIf { position ->
                position != RecyclerView.NO_POSITION

            }?.let { _: Int ->

            }
        }
        return holder
    }

    override fun getItemCount(): Int {
        return list?.size?: 0
    }

    override fun onBindViewHolder(holder: CollectionHolder, position: Int) {
        list?.let {
            holder.title?.text = it[position].name
        }
    }

    fun setData(categories: List<CategoryModel>?) {
        list = categories
    }

}

class ScreenshotHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
    var title: TextView? = null
}

class CollectionHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
    var title: TextView? = null
}