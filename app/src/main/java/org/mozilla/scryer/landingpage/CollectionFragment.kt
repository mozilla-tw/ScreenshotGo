/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.landingpage

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import org.mozilla.scryer.detailpage.DetailPageActivity
import org.mozilla.scryer.R
import org.mozilla.scryer.ScryerApplication
import org.mozilla.scryer.capture.GridItemDecoration
import org.mozilla.scryer.capture.dp2px
import org.mozilla.scryer.getSupportActionBar
import org.mozilla.scryer.persistence.ScreenshotModel
import org.mozilla.scryer.viewmodel.ScreenshotViewModel
import org.mozilla.scryer.viewmodel.ScreenshotViewModelFactory
import java.io.File

class CollectionFragment : Fragment() {
    companion object {
        const val ARG_COLLECTION_ID = "collection_id"
    }

    private lateinit var screenshotListView: RecyclerView
    private val screenshotAdapter = ScreenshotAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val layout = inflater.inflate(R.layout.fragment_collection, container, false)
        screenshotListView = layout.findViewById(R.id.screenshot_list)
        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initScreenshotList(view.context)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
        (activity as? AppCompatActivity)?.setSupportActionBar(view?.findViewById(R.id.toolbar))
        getSupportActionBar(activity)?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        item?.let {
            when(it.itemId) {
                android.R.id.home -> Navigation.findNavController(view!!).navigateUp()
                else -> return super.onOptionsItemSelected(item)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initScreenshotList(context: Context) {
        val manager = GridLayoutManager(context, 2, GridLayoutManager.VERTICAL, false)
        screenshotListView.layoutManager = manager
        screenshotListView.adapter = screenshotAdapter
        screenshotListView.addItemDecoration(GridItemDecoration(dp2px(context, 8f), 2))
        val factory = ScreenshotViewModelFactory(ScryerApplication.instance.screenshotRepository)
        ViewModelProviders.of(this, factory).get(ScreenshotViewModel::class.java)
                .getScreenshots(arguments?.getString(ARG_COLLECTION_ID)?:"")
                .observe(this, Observer { screenshots ->
                    screenshots?.let {
                        screenshotAdapter.setScreenshotList(it)
                        screenshotAdapter.notifyDataSetChanged()
                        getSupportActionBar(activity)?.setSubtitle("${it.size} shots")
                    }
                })
    }
}

open class ScreenshotAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var screenshotList: List<ScreenshotModel> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_screenshot, parent, false)
        view.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT

        view.layoutParams.height = (parent.measuredWidth / 2f).toInt()
        view.setPadding(0, 0, 0, 0)

        val holder = ScreenshotItemHolder(view)
        holder.title = view.findViewById(R.id.title)
        holder.image = view.findViewById(R.id.image_view)
        holder.itemView.setOnClickListener { _ ->
            holder.adapterPosition.takeIf { position ->
                position != RecyclerView.NO_POSITION

            }?.let { position: Int ->
                DetailPageActivity.showDetailPage(parent.context, screenshotList[position].path, holder.image)
            }
        }
        return holder
    }

    override fun getItemCount(): Int {
        return screenshotList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as? ScreenshotItemHolder)?.apply {
            title?.text = getItemFileName(position)
            image?.let {
                Glide.with(holder.itemView.context)
                        .load(File(screenshotList[position].path))
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
        return item.path.substring(item.path.lastIndexOf(File.separator) + 1)
    }

    open fun setScreenshotList(list: List<ScreenshotModel>) {
        screenshotList = list
    }
}

class ScreenshotItemHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
    var title: TextView? = null
    var image: ImageView? = null
}