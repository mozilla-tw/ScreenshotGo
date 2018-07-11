/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.Toast
import androidx.navigation.Navigation

class CategoryFragment : Fragment() {
    private lateinit var categoryListView: RecyclerView
    private val categoryListAdapter = ScreenshotAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val layout = inflater.inflate(R.layout.fragment_category, container, false)
        categoryListView = layout.findViewById(R.id.category_list)
        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initCategoryList(view.context)
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

    private fun initCategoryList(context: Context) {
        val manager = GridLayoutManager(context, 2, GridLayoutManager.VERTICAL, false)
        categoryListView.layoutManager = manager
        categoryListView.adapter = categoryListAdapter
        categoryListView.addItemDecoration(SpacesItemDecoration(dp2px(context, 8f)))
        val factory = ScreenshotViewModelFactory(ScreenshotRepository.from(context))
        ViewModelProviders.of(this, factory).get(ScreenshotViewModel::class.java)
                .getScreenshots()
                .observe(this, Observer { screenshots ->
                    screenshots?.filter {
                        it.category == arguments?.getString("category_name")?:""
                    }?.let {
                        categoryListAdapter.list = it
                        categoryListAdapter.notifyDataSetChanged()
                        getSupportActionBar(activity)?.setSubtitle("${it.size} shots")
                    }
                })
    }

    private class ScreenshotAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        lateinit var list: List<ScreenshotModel>

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_screenshot, parent, false)
            view.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT

            val ratio = parent.rootView.measuredHeight / parent.measuredWidth.toFloat() / 2f
            view.layoutParams.height = ((parent.measuredWidth / 2f) * ratio).toInt()
            view.setPadding(0, 0, 0, 0)

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

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val titleView = (holder as ScreenshotItemHolder).title
            titleView?.apply {
                titleView.text = list[position].name
            }
        }
    }
}