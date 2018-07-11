/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.mozilla.scryer

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ChooseCollectionActivity : AppCompatActivity() {
    companion object {
        private const val GRID_SPAN_COUNT = 2
        private const val GRID_CELL_SPACE_DP = 6f

        const val EXTRA_PATH = "path"
    }

    private val adapter: ChooseCollectionAdapter by lazy {
        ChooseCollectionAdapter(this, this::onItemClicked)
    }

    private val recyclerView: RecyclerView by lazy { findViewById<RecyclerView>(R.id.recycler_view) }
    private lateinit var categoryViewModel: ScreenshotViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_collection)

        val factory = ScreenshotViewModelFactory(getScreenshotRepository())
        categoryViewModel = ViewModelProviders.of(this, factory).get(ScreenshotViewModel::class.java)
        val data = categoryViewModel.getCategories()
        data.observe(this, Observer { collections ->
            collections?.let {
                adapter.categoryList = it
                adapter.notifyDataSetChanged()
            }
        })

        initRecyclerView()

        val path = intent.getStringExtra(EXTRA_PATH)
        val file = File(path)
        if (file.exists()) {
            val bmp = BitmapFactory.decodeFile(file.absolutePath)
            findViewById<ImageView>(R.id.image_view).setImageBitmap(bmp)
        }
    }

    private fun getScreenshotRepository(): ScreenshotRepository {
        return ScreenshotRepository.from(this)
    }

    private fun initRecyclerView() {
        recyclerView.layoutManager = GridLayoutManager(this, GRID_SPAN_COUNT,
                GridLayoutManager.VERTICAL,
                false)
        recyclerView.addItemDecoration(SpacesItemDecoration(dp2px(this, GRID_CELL_SPACE_DP)))

        recyclerView.adapter = this.adapter
    }

    private fun onItemClicked(category: CategoryModel) {
        Toast.makeText(this, "save to ${category.name}", Toast.LENGTH_SHORT).show()

        val date = SimpleDateFormat("yyyy-MM-dd-HH:mm:ss", Locale.ENGLISH)
        // TODO: use an unique id instead of category name for identifying category
        val screenshot = ScreenshotModel("screenshot_${date.format(Date())}", category.name)
        ScreenshotRepository.from(this).addScreenshot(screenshot)

        finish()
    }

    fun onNewCollectionClicked() {
        val dialogView = View.inflate(this, R.layout.dialog_add_collection, null)
        val editText = dialogView.findViewById<EditText>(R.id.edit_text)

        val dialog = AlertDialog.Builder(this, R.style.Theme_AppCompat_Light_Dialog_Alert)
                .setView(dialogView)
                .setPositiveButton("DONE") { _, _ ->
                    categoryViewModel.addCategory(CategoryModel(editText.text.toString()))
                    recyclerView.adapter.notifyItemInserted(recyclerView.adapter.itemCount - 1)
                }
                .create()
        dialog.show()
        editText.requestFocus()
        dialog.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }
}

class ChooseCollectionAdapter(private var activity: ChooseCollectionActivity,
                        private var itemClickListener: (CategoryModel) -> Unit)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val TYPE_FIXED_ITEM = 0
        private const val TYPE_DATA_ITEM = 1

        private const val POSITION_NEW_COLLECTION = 0
    }

     var categoryList: List<CategoryModel>? = null
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
        return fixedItems.size + (categoryList?.size?:0)
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < fixedItems.size) TYPE_FIXED_ITEM else TYPE_DATA_ITEM
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is DataViewHolder) {
            (holder.itemView as TextView).text = categoryList?.let {
                it[position - fixedItems.size].name
            }
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
                categoryList?.let {
                    itemClickListener.invoke(it[position - fixedItems.size])
                }
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

fun dp2px(context: Context, dp: Float): Int {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics).toInt()
}