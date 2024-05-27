// Copyright 2023 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.content_public.browser;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.concurrent.atomic.AtomicReference;

/**
 * An {@link ActionMode.Callback2} adapter that adds APIs that are not dependent on
 * {@link ActionMode}, {@link android.view.Menu} or {@link android.view.MenuItem}.
 */
public abstract class ActionModeCallback implements ActionMode.Callback {
    /**
     * Callback for handling drop-down menu item clicks.
     * @param groupId the id of the group that the item belongs to.
     * @param id the id of item that was clicked.
     * @param intent the intent of the item that was clicked.
     * @param clickListener the custom click listener for the item that was clicked.
     * @return true if this callback handled the event, false if the standard handling should
     *         continue.
     */
    public abstract boolean onDropdownItemClicked(int groupId, int id, @Nullable Intent intent,
            @Nullable View.OnClickListener clickListener);

    /**
     * Called when an ActionMode needs to be positioned on screen, potentially occluding view
     * content. Note this may be called on a per-frame basis.
     *
     * @param mode The ActionMode that requires positioning.
     * @param view The View that originated the ActionMode, in whose coordinates the Rect should
     *          be provided.
     * @param outRect The Rect to be populated with the content position. Use this to specify
     *          where the content in your app lives within the given view. This will be used
     *          to avoid occluding the given content Rect with the created ActionMode.
     */
    public void onGetContentRect(ActionMode mode, View view, Rect outRect) {
        if (view != null) {
            outRect.set(0, 0, view.getWidth(), view.getHeight());
        } else {
            outRect.set(0, 0, 0, 0);
        }
    }

    private final AtomicReference<ActionMode.Callback2> mCallback2 = new AtomicReference<>();

    @RequiresApi(Build.VERSION_CODES.M)
    public final ActionMode.Callback2 getCallback2() {
        ActionMode.Callback2 callback = mCallback2.get();
        if (mCallback2 == null) {
            callback = new Callback2Adapter();
            mCallback2.set(callback);
        }
        return callback;
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private class Callback2Adapter extends ActionMode.Callback2 {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return ActionModeCallback.this.onCreateActionMode(mode, menu);
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return ActionModeCallback.this.onPrepareActionMode(mode, menu);
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return ActionModeCallback.this.onActionItemClicked(mode, item);
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            ActionModeCallback.this.onDestroyActionMode(mode);
        }
    }
}
