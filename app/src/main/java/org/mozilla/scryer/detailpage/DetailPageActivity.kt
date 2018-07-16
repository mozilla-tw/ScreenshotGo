/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.detailpage

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityOptionsCompat
import android.support.v4.view.ViewCompat
import android.view.View
import android.widget.ImageView
import org.mozilla.scryer.R

class DetailPageActivity : AppCompatActivity() {

    companion object Launcher {
        fun showDetailPage(context: Context, path: String, srcView: View?) {
            val intent = Intent(context, DetailPageActivity::class.java)
            val bundle = srcView?.let {
                val option = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        context as Activity, srcView, ViewCompat.getTransitionName(it))
                option.toBundle()
            } ?: null
            intent.putExtra("path", path)
            (context as AppCompatActivity).startActivity(intent, bundle)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_page)

        val imageView = findViewById<ImageView>(R.id.image_view)
        val path = intent.getStringExtra("path")
        imageView.setImageBitmap(BitmapFactory.decodeFile(path))
    }
}
