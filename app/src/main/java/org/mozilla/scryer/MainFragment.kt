/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer

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
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.view.*
import android.widget.*
import androidx.navigation.Navigation

class MainFragment : Fragment() {
    private lateinit var quickAccessListView: RecyclerView
    private val quickAccessListAdapter: QuickAccessListAdapter = QuickAccessListAdapter()

    private lateinit var mainListView: RecyclerView
    private val mainListAdapter: MainListAdapter = MainListAdapter()

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
        initCategoryList(view.context)
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
        quickAccessListView.adapter = quickAccessListAdapter

        val factory = ScreenshotViewModelFactory(getScreenshotRepository(context))
        ViewModelProviders.of(this, factory).get(ScreenshotViewModel::class.java)
                .getScreenshots()
                .observe(this, Observer { screenshots ->
                    screenshots?.let { newList ->
                        updateQuickAccessListView(newList)
                    }
                })
    }

    private fun initCategoryList(context: Context) {
        val manager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        mainListView.layoutManager = manager
        mainListView.adapter = mainListAdapter
        mainListView.recycledViewPool.setMaxRecycledViews(MainListAdapter.TYPE_QUICK_ACCESS, 1)
        mainListAdapter.quickAccessListView = quickAccessListView

        val factory = ScreenshotViewModelFactory(getScreenshotRepository(context))
        ViewModelProviders.of(this, factory).get(ScreenshotViewModel::class.java)
                .getCategories()
                .observe(this, Observer { collections ->
                    collections?.let { newData ->
                        updateCategoryListView(newData)
                    }
                })
    }

    private fun initSearchList(context: Context) {
        val manager = GridLayoutManager(context, 2, GridLayoutManager.VERTICAL, false)
        searchListView.layoutManager = manager
        searchListView.adapter = searchListAdapter
        searchListView.addItemDecoration(SpacesItemDecoration(dp2px(context, 8f)))
        val factory = ScreenshotViewModelFactory(getScreenshotRepository(context))
        ViewModelProviders.of(this, factory).get(ScreenshotViewModel::class.java)
                .getScreenshots()
                .observe(this, Observer { screenshots ->
                    screenshots?.let {newData ->
                        searchListAdapter.setScreenshotList(newData)
                    }
                })
    }

    private fun updateQuickAccessListView(screenshots: List<ScreenshotModel>) {
        quickAccessListAdapter.list = screenshots
        quickAccessListAdapter.notifyDataSetChanged()
    }

    private fun updateCategoryListView(categories: List<CategoryModel>) {
        mainListAdapter.categoryList = categories
        mainListAdapter.notifyDataSetChanged()
    }

    private fun getScreenshotRepository(context: Context): ScreenshotRepository {
        return ScreenshotRepository.from(context)
    }
}

class QuickAccessListAdapter: RecyclerView.Adapter<ScreenshotItemHolder>() {
    lateinit var list: List<ScreenshotModel>

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScreenshotItemHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_screenshot, parent, false)

        val holder = ScreenshotItemHolder(view)
        holder.title = view.findViewById(R.id.title)
        holder.itemView.setOnClickListener { _ ->
            holder.adapterPosition.takeIf { position ->
                position != RecyclerView.NO_POSITION

            }?.let { position: Int ->
                Toast.makeText(parent.context, "Item ${list[position].name} clicked",
                        Toast.LENGTH_SHORT).show()
            }
        }
        return holder
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ScreenshotItemHolder, position: Int) {
        holder.title?.text = list[position].name
    }
}

class MainListAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        const val TYPE_SECTION_NAME = 0
        const val TYPE_QUICK_ACCESS = 1
        const val TYPE_ROW = 2

        const val POS_QUICK_ACCESS_TITLE = 0
        const val POS_QUICK_ACCESS_LIST = 1
        const val POS_CATEGORY_TITLE = 2

        const val FIXED_ITEM_COUNT = 3
        const val COLUMN_COUNT = 2
    }

    lateinit var categoryList: List<CategoryModel>
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
        return FIXED_ITEM_COUNT + ((categoryList.size - 1) / COLUMN_COUNT) + 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            POS_QUICK_ACCESS_TITLE -> TYPE_SECTION_NAME
            POS_QUICK_ACCESS_LIST -> TYPE_QUICK_ACCESS
            POS_CATEGORY_TITLE -> TYPE_SECTION_NAME
            else -> TYPE_ROW
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (position) {
            POS_QUICK_ACCESS_TITLE -> (holder as SectionNameHolder).title?.text = "Quick Access"
            POS_QUICK_ACCESS_LIST -> return
            POS_CATEGORY_TITLE -> (holder as SectionNameHolder).title?.text = "Category"
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

            val view = inflater.inflate(R.layout.item_category, container, true)
            view.setOnClickListener {_ ->
                rowHolder.adapterPosition.takeIf { position ->
                    position != RecyclerView.NO_POSITION

                }?.let { position: Int ->
                    val row = position - FIXED_ITEM_COUNT
                    val startIndex = row * COLUMN_COUNT
                    val bundle = Bundle()
                    bundle.putString("category_name", categoryList[startIndex + i].name)
                    Navigation.findNavController(parent).navigate(R.id.action_navigate_to_category, bundle)
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
            if (offsetIndex < categoryList.size) {
                bindRowItem(rowHolder.holderList[i], categoryList[offsetIndex])
            } else {
                hideRowItem(rowHolder.holderList[i])
            }
        }
    }

    private fun bindRowItem(item: RowItemHolder, categoryModel: CategoryModel) {
        item.itemView.visibility = View.VISIBLE
        item.title?.text = categoryModel.name
    }

    private fun hideRowItem(item: RowItemHolder) {
        item.itemView.visibility = View.INVISIBLE
    }
}

class SearchAdapter : ScreenshotAdapter(), Filterable {
    private lateinit var originalList: List<ScreenshotModel>
    private var searchTarget: String = ""

    override fun setScreenshotList(list: List<ScreenshotModel>) {
        originalList = list
        super.setScreenshotList(list)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val titleView = (holder as ScreenshotItemHolder).title
        titleView?.apply {
            val item = getItemAt(position)
            val spannable = SpannableString(item.name)
            val start = item.name.indexOf(searchTarget)
            spannable.setSpan(BackgroundColorSpan(Color.RED), start, start + searchTarget.length,
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            titleView.text = spannable
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val newList = mutableListOf<ScreenshotModel>()
                constraint?.takeIf { it.isNotEmpty() }?.let {
                    for (category in originalList) {
                        if (category.name.toLowerCase().contains(constraint.toString().toLowerCase())) {
                            newList.add(category)
                        }
                    }
                }
                // TODO: How to reference to super here?
                setScreenshotListToSuper(newList)
                val result = FilterResults()
                result.values = newList

                return result
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                searchTarget = constraint.toString()
                notifyDataSetChanged()
            }
        }
    }

    private fun setScreenshotListToSuper(list: List<ScreenshotModel>) {
        super.setScreenshotList(list)
    }
}

class ScreenshotItemHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
    var title: TextView? = null
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