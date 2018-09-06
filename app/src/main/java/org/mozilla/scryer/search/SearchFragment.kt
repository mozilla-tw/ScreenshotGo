/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.search

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.navigation.Navigation
import org.mozilla.scryer.R
import org.mozilla.scryer.getSupportActionBar
import org.mozilla.scryer.setSupportActionBar

class SearchFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val layout = inflater.inflate(R.layout.fragment_search_empty, container, false)

        layout.findViewById<Button>(R.id.positive_button).setOnClickListener { Navigation.findNavController(view).navigateUp() }
        layout.findViewById<Button>(R.id.negative_button).setOnClickListener { Navigation.findNavController(view).navigateUp() }

        return layout
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
        setupActionBar()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> Navigation.findNavController(view).navigateUp()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun getView(): View {
        return super.getView()!!
    }

    private fun setupActionBar() {
        setSupportActionBar(activity, view.findViewById(R.id.toolbar))
        getSupportActionBar(activity).apply {
            setDisplayHomeAsUpEnabled(true)
        }
    }
}
