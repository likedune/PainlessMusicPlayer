/*
 * Copyright (C) 2016 Yaroslav Mytkalyk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.doctoror.fuckoffmusicplayer.wear.search;

import com.doctoror.fuckoffmusicplayer.wear.view.AlphaSelectionViewHolder;

import android.view.View;
import android.widget.TextView;

/**
 * View holder for {@link SearchResultsAdapter}
 */
final class SearchResultsViewHolder extends AlphaSelectionViewHolder {

    TextView textView;

    SearchResultsViewHolder(final View itemView) {
        super(itemView);
        textView = (TextView) itemView.findViewById(android.R.id.text1);
    }
}
