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
package com.doctoror.fuckoffmusicplayer.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.ImageButton;

/**
 * Created by Yaroslav Mytkalyk on 22.10.16.
 */

public class PressFeedbackImageButton extends ImageButton {

    private final ColorFilter mColorFilter = new PorterDuffColorFilter(
            0x50ffffff, PorterDuff.Mode.SRC_ATOP);

    public PressFeedbackImageButton(final Context context) {
        super(context);
    }

    public PressFeedbackImageButton(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public PressFeedbackImageButton(final Context context, final AttributeSet attrs,
            final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public PressFeedbackImageButton(final Context context, final AttributeSet attrs,
            final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        setPressed(isClickable() && isPressed());
    }

    @Override
    public void setPressed(final boolean pressed) {
        super.setPressed(pressed);
        final Drawable background = getBackground();
        if (background != null && !isRipple(background)) {
            background.setColorFilter(pressed ? mColorFilter : null);
        }
    }

    public static boolean isRipple(@Nullable final Drawable drawable) {
        //noinspection IfMayBeConditional
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return drawable instanceof RippleDrawable;
        } else {
            return false;
        }
    }
}
