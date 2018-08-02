/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.sortingpanel

import android.graphics.Color
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.mozilla.scryer.R
import org.mozilla.scryer.persistence.CollectionModel

class SortingPanelAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val TYPE_NEW_COLLECTION = 0
        private const val TYPE_COLLECTION_ITEM = 1

        private const val POSITION_NEW_COLLECTION = 0
    }

    var collections: List<CollectionModel>? = null
    var callback: SortingPanel.Callback? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            TYPE_NEW_COLLECTION -> {
                return createNewCollectionHolder(parent)
            }

            TYPE_COLLECTION_ITEM -> {
                return createCollectionHolder(parent)
            }
        }
        throw IllegalStateException("unexpected item type $viewType")
    }

    override fun getItemCount(): Int {
        return 1 + (collections?.size ?: 0)
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            POSITION_NEW_COLLECTION -> TYPE_NEW_COLLECTION
            else -> TYPE_COLLECTION_ITEM
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (position) {
            POSITION_NEW_COLLECTION -> {
                (holder as NewItemHolder).title?.text = holder.itemView.resources.getString(R.string.ac_new_collection)
            }
            else -> {
                (holder as ItemHolder).title?.text = collections?.get(position - 1)?.name
                val color = collections?.get(position - 1)?.color ?: Color.WHITE
                DrawableCompat.setTint(holder.itemView.background, color)
            }
        }
    }

    private fun createNewCollectionHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.view_sorting_panel_item, parent, false)
        val holder = NewItemHolder(view)
        holder.title = view.findViewById(R.id.title)
        holder.itemView.setOnClickListener { _ ->
            ensurePosition(holder) {
                callback?.onNewCollectionClick()
            }
        }
        holder.itemView.background = DrawableCompat.wrap(holder.itemView.background)
        return holder
    }

    private fun createCollectionHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.view_sorting_panel_item, parent, false)
        val holder = ItemHolder(view)
        holder.title = view.findViewById(R.id.title)
        holder.itemView.setOnClickListener { _ ->
            ensurePosition(holder) { position ->
                collections?.let {
                    callback?.onClick(it[position - 1])
                }
            }
        }
        holder.itemView.background = DrawableCompat.wrap(holder.itemView.background)
        return holder
    }

    private class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var title: TextView? = null
    }

    private class NewItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var title: TextView? = null
    }

    private fun ensurePosition(holder: RecyclerView.ViewHolder, action: (Int) -> Unit) {
        if (holder.adapterPosition != RecyclerView.NO_POSITION) {
            action(holder.adapterPosition)
        }
    }
}