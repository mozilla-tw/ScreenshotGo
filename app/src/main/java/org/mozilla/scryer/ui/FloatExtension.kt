/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.scryer.ui

import android.util.DisplayMetrics
import kotlin.math.ceil
import kotlin.math.floor

fun Float.dpToPx(displayMetrics: DisplayMetrics): Int = (this * displayMetrics.density).round()
private fun Float.round(): Int = (if (this < 0) ceil(this - 0.5f) else floor(this + 0.5f)).toInt()
