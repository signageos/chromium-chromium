// Copyright 2021 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.ui.util;

import android.content.Context;
import android.content.res.Resources.Theme;
import android.os.Build;
import android.util.TypedValue;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DeprecatedSinceApi;
import androidx.annotation.RequiresApi;
import androidx.appcompat.content.res.AppCompatResources;

/** Helper functions for working with attributes. */
public final class AttrUtils {
    /** Private constructor to stop instantiation. */
    private AttrUtils() {}

    /** Returns the given boolean attribute from the theme. */
    public static boolean resolveBoolean(Theme theme, @AttrRes int attrRes) {
        TypedValue typedValue = new TypedValue();
        theme.resolveAttribute(attrRes, typedValue, /*resolveRefs=*/true);
        return typedValue.data != 0;
    }

    /** Returns the given color attribute from the theme. */
    @RequiresApi(Build.VERSION_CODES.M)
    public static @ColorInt int resolveColor(Theme theme, @AttrRes int attrRes) {
        TypedValue typedValue = new TypedValue();
        theme.resolveAttribute(attrRes, typedValue, /*resolveRefs=*/true);
        if (typedValue.resourceId != 0) {
            // Color State List
            return theme.getResources().getColor(typedValue.resourceId, theme);
        } else {
            // Color Int
            return typedValue.data;
        }
    }

    /**
     * Returns the given color attribute from the theme or resolves and returns the given default
     * resource if the attribute is not set in the theme.
     */
    @RequiresApi(Build.VERSION_CODES.M)
    public static @ColorInt int resolveColor(
            Theme theme, @AttrRes int attrRes, @ColorRes int defaultColorRes) {
        TypedValue typedValue = new TypedValue();
        if (theme.resolveAttribute(attrRes, typedValue, /*resolveRefs=*/true)) {
            return typedValue.data;
        } else {
            return theme.getResources().getColor(defaultColorRes, theme);
        }
    }

    /** Returns the given color attribute from the theme. */
    @DeprecatedSinceApi(api = Build.VERSION_CODES.M)
    public static @ColorInt int resolveColor(Context context, @AttrRes int attrRes) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attrRes, typedValue, /*resolveRefs=*/true);
        if (typedValue.resourceId != 0) {
            // Color State List
            return AppCompatResources.getColorStateList(context, typedValue.resourceId)
                    .getDefaultColor();
        } else {
            // Color Int
            return typedValue.data;
        }
    }

    /**
     * Returns the given color attribute from the theme or resolves and returns the given default
     * resource if the attribute is not set in the theme.
     */
    @DeprecatedSinceApi(api = Build.VERSION_CODES.M)
    public static @ColorInt int resolveColor(
            Context context, @AttrRes int attrRes, @ColorRes int defaultColorRes) {
        TypedValue typedValue = new TypedValue();
        if (context.getTheme().resolveAttribute(attrRes, typedValue, /*resolveRefs=*/true)) {
            return AppCompatResources.getColorStateList(context, typedValue.resourceId)
                    .getDefaultColor();
        } else {
            return AppCompatResources.getColorStateList(context, defaultColorRes)
                    .getDefaultColor();
        }
    }
}
