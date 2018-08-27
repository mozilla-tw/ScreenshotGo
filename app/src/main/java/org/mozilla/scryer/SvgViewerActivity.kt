package org.mozilla.scryer

import android.graphics.Color
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout

class SvgViewerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_svg_viewer)

        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false)
        recyclerView.adapter = IconAdapter()
    }

    private class IconAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        val iconIds = listOf(
                R.drawable.debug_broken_svg,
                R.drawable.add,
                R.drawable.back,
                R.drawable.capture,
                R.drawable.check,
                R.drawable.close_large,
                R.drawable.close_small,
                R.drawable.error,
                R.drawable.fab_ocr,
                R.drawable.more,
                R.drawable.move,
                R.drawable.search,
                R.drawable.share,
                R.drawable.viewmore
        )

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val frameLayout = FrameLayout(parent.context)
            val view = View(parent.context)
            frameLayout.addView(view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT))
            frameLayout.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    frameLayout.viewTreeObserver.removeOnPreDrawListener(this)
                    val params = frameLayout.layoutParams
                    params.height = frameLayout.measuredWidth
                    frameLayout.layoutParams = params
                    return false
                }
            })
            return object : RecyclerView.ViewHolder(frameLayout) {}
        }

        override fun getItemCount(): Int {
            return iconIds.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val iconId = iconIds[position]
            holder.itemView.setBackgroundColor(if (iconId == R.drawable.debug_broken_svg) Color.RED else Color.TRANSPARENT)
            (holder.itemView as ViewGroup).getChildAt(0).background = ContextCompat.getDrawable(holder.itemView.context, iconIds[position])
        }
    }
}
