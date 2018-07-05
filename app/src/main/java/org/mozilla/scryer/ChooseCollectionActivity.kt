/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.mozilla.scryer

import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_choose_collection.*


class ChooseCollectionActivity : AppCompatActivity() {
    private val collectionModel = CollectionModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_collection)

        initRecyclerView()
    }

    private fun initRecyclerView() {
        recyclerView.layoutManager = GridLayoutManager(this, 2,
                GridLayoutManager.VERTICAL, false)

        recyclerView.adapter = ChooseCollectionAdapter(this, collectionModel.items, this::onItemClicked)
        recyclerView.addItemDecoration(SpacesItemDecoration(20))
    }

    private fun onItemClicked(item: CollectionItem) {
        Toast.makeText(this, "save to ${item.title}", Toast.LENGTH_SHORT).show()
        finish()
    }

    fun onNewCollectionClicked() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_collection, null, false)

        val dialog = AlertDialog.Builder(this, R.style.Theme_AppCompat_Light_Dialog_Alert)
                .setView(dialogView)
                .setPositiveButton("DONE", { _, _ ->
                    collectionModel.addNewCollection(CollectionItem(dialogView.findViewById<EditText>(R.id.edit_text)?.text.toString()))
                    recyclerView.adapter.notifyItemInserted(recyclerView.adapter.itemCount - 1)
                })
                .create()
        dialog.show()
        dialogView.findViewById<EditText>(R.id.edit_text).requestFocus()
        dialog.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }
}

class CollectionModel {
    fun addNewCollection(item: CollectionItem) {
        items.add(item)
    }

    val items: MutableList<CollectionItem> = mutableListOf(CollectionItem("Create new collection"),
            CollectionItem("Shopping"),
            CollectionItem("Music"),
            CollectionItem("Secret"))
}

class CollectionItem(val title: String)

class ChooseCollectionAdapter(private var activity: ChooseCollectionActivity,
                              private var list: MutableList<CollectionItem>,
                              private var itemClickListener: (CollectionItem) -> Unit)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = TextView(parent.context)
        view.setBackgroundColor(Color.LTGRAY)
        view.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 500)
        view.setTextColor(Color.BLACK)
        view.gravity = Gravity.CENTER

        val holder = ItemHolder(view)
        holder.itemView.setOnClickListener {
            holder.adapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.run {
                if (this == 0) {
                    createNewCollection()
                } else {
                    itemClickListener.invoke(list[this])
                }
            }
        }
        return holder
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemHolder) {
            (holder.itemView as TextView).text = list[position].title
        }
    }

    private fun createNewCollection() {
        activity.onNewCollectionClicked()
    }
}

class ItemHolder(view: View) : RecyclerView.ViewHolder(view) {

}

class SpacesItemDecoration(private val space: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View,
                                parent: RecyclerView, state: RecyclerView.State) {
        outRect.left = space
        outRect.right = space
        outRect.bottom = space

        if (parent.getChildAdapterPosition(view) % 2 == 0) {
            outRect.right = space / 2
        } else {
            outRect.left = space / 2
        }

        if (parent.getChildLayoutPosition(view) < 2) {
            outRect.top = space
        } else {
            outRect.top = 0
        }
    }
}