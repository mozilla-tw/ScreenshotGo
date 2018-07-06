/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.mozilla.scryer

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.TypedValue
import android.view.*
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

class ChooseCollectionActivity : AppCompatActivity() {
    companion object {
        private const val GRID_SPAN_COUNT = 2
        private const val GRID_CELL_SPACE_DP = 6f
    }

    private val collectionModel = CollectionModel()
    private val adapter: CollectionAdapter by lazy {
        CollectionAdapter(this, collectionModel.items, this::onItemClicked)
    }

    private val recyclerView: RecyclerView by lazy { findViewById<RecyclerView>(R.id.recycler_view) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_collection)

        initRecyclerView()
    }

    private fun initRecyclerView() {
        recyclerView.layoutManager = GridLayoutManager(this, GRID_SPAN_COUNT,
                GridLayoutManager.VERTICAL,
                false)
        recyclerView.addItemDecoration(SpacesItemDecoration(dp2px(this, GRID_CELL_SPACE_DP)))

        recyclerView.adapter = this.adapter
    }

    private fun onItemClicked(item: CollectionItem) {
        Toast.makeText(this, "save to ${item.title}", Toast.LENGTH_SHORT).show()
        finish()
    }

    fun onNewCollectionClicked() {
        val dialogView = View.inflate(this, R.layout.dialog_add_collection, null)
        val editText = dialogView.findViewById<EditText>(R.id.edit_text)

        val dialog = AlertDialog.Builder(this, R.style.Theme_AppCompat_Light_Dialog_Alert)
                .setView(dialogView)
                .setPositiveButton("DONE", { _, _ ->
                    collectionModel.addNewCollection(CollectionItem(editText.text.toString()))
                    recyclerView.adapter.notifyItemInserted(recyclerView.adapter.itemCount - 1)
                })
                .create()
        dialog.show()
        editText.requestFocus()
        dialog.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }
}

class CollectionModel {
    fun addNewCollection(item: CollectionItem) {
        items.add(item)
    }

    val items: MutableList<CollectionItem> = mutableListOf(CollectionItem("Shopping"),
            CollectionItem("Music"),
            CollectionItem("Secret"))
}

class CollectionItem(val title: String)

class CollectionAdapter(private var activity: ChooseCollectionActivity,
                        private var list: MutableList<CollectionItem>,
                        private var itemClickListener: (CollectionItem) -> Unit)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val TYPE_FIXED_ITEM = 0
        private const val TYPE_DATA_ITEM = 1

        private const val POSITION_NEW_COLLECTION = 0
    }

    private val fixedItems = listOf("Create new collection")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            TYPE_FIXED_ITEM -> {
                return createFixedViewHolder(parent)
            }

            TYPE_DATA_ITEM -> {
                return createDataViewHolder(parent)
            }
        }
        throw IllegalStateException("unexpected item type $viewType")
    }

    override fun getItemCount(): Int {
        return fixedItems.size + list.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < fixedItems.size) TYPE_FIXED_ITEM else TYPE_DATA_ITEM
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is DataViewHolder) {
            (holder.itemView as TextView).text = list[position - fixedItems.size].title
        } else if (holder is FixedViewHolder) {
            (holder.itemView as TextView).text = fixedItems[position]
        }
    }

    private fun createFixedViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val view = TextView(parent.context)
        view.setBackgroundColor(Color.LTGRAY)
        view.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 500)
        view.setTextColor(Color.BLACK)
        view.gravity = Gravity.CENTER

        val holder = FixedViewHolder(view)
        holder.itemView.setOnClickListener { _ ->
            holder.adapterPosition.takeIf { position ->
                position != RecyclerView.NO_POSITION

            }?.let { position: Int ->
                onFixedItemClicked(position)
            }
        }
        return holder
    }

    private fun createDataViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val view = TextView(parent.context)
        view.setBackgroundColor(Color.LTGRAY)
        view.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 500)
        view.setTextColor(Color.BLACK)
        view.gravity = Gravity.CENTER

        val holder = DataViewHolder(view)
        holder.itemView.setOnClickListener { _ ->
            holder.adapterPosition.takeIf { position ->
                position != RecyclerView.NO_POSITION

            }?.let { position: Int ->
                itemClickListener.invoke(list[position - fixedItems.size])
            }
        }
        return holder
    }

    private fun createNewCollection() {
        activity.onNewCollectionClicked()
    }

    private fun onFixedItemClicked(position :Int) {
        when (position) {
            POSITION_NEW_COLLECTION -> {
                createNewCollection()
            }
        }
    }
}

class DataViewHolder(view: View) : RecyclerView.ViewHolder(view)
class FixedViewHolder(view: View) : RecyclerView.ViewHolder(view)

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

private fun dp2px(context: Context, dp: Float): Int {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics).toInt()
}