/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.landingpage

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import org.mozilla.scryer.R
import org.mozilla.scryer.capture.dp2px
import org.mozilla.scryer.persistence.CollectionModel
import org.mozilla.scryer.persistence.ScreenshotModel
import java.io.File

class MainAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {
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

    lateinit var quickAccessListView: RecyclerView

    var collectionList: List<CollectionModel> = emptyList()
    var coverList: Map<String, ScreenshotModel> = HashMap()

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
            itemHolder.image = view.findViewById(R.id.image)
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

        coverList[collectionModel.id]?.let {
            Glide.with(item.itemView).load(File(it.path)).into(item.image!!)
        } ?: run {
            item.image?.setImageBitmap(null)
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is RowHolder) {
            for (itemHolder in holder.holderList) {
                itemHolder.image?.setImageBitmap(null)
            }
        }
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
        var image: ImageView? = null
        var title: TextView? = null
    }
}