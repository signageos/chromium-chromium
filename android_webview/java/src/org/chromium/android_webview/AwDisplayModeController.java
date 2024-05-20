// Copyright 2020 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.android_webview;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Build;
import android.view.View;
import android.view.ViewParent;

import androidx.annotation.DeprecatedSinceApi;
import androidx.annotation.RequiresApi;

import org.chromium.android_webview.common.Lifetime;
import org.chromium.base.Log;
import org.chromium.blink.mojom.DisplayMode;

import java.lang.reflect.Field;

/**
 * Display mode controller for WebView.
 *
 * Display mode will be used for display cutout controller's internal implementation since we only
 * apply display cutout to fullscreen mode. Also, display mode will be reported as CSS property.
 */
@Lifetime.WebView
public class AwDisplayModeController {
    private static final boolean DEBUG = false;
    private static final String TAG = "DisplayMode";

    /**
     * This is a delegate that the embedder needs to implement.
     */
    public interface Delegate {
        /** @return The display width. */
        int getDisplayWidth();
        /** @return The display height. */
        int getDisplayHeight();
    }

    private Delegate mDelegate;
    private View mContainerView;

    // Reuse these structures to minimize memory impact.
    private static final int[] sCachedLocationOnScreen = {0, 0};
    private static final Rect sCachedViewRect = new Rect();
    private static final Rect sCachedWindowRect = new Rect();
    private static final Rect sCachedDisplayRect = new Rect();
    private static final Matrix sCachedMatrix = new Matrix();

    /**
     * Constructor for AwDisplayModeController.
     *
     * @param delegate The delegate.
     * @param containerView The container view (WebView).
     */
    public AwDisplayModeController(Delegate delegate, View containerView) {
        mContainerView = containerView;
        mDelegate = delegate;
    }

    public int getDisplayMode() {
        // We currently do not support other display modes.
        return isDisplayInFullscreen() ? DisplayMode.FULLSCREEN : DisplayMode.BROWSER;
    }

    private boolean isDisplayInFullscreen() {
        getViewRectOnScreen(mContainerView, sCachedViewRect);
        getViewRectOnScreen(mContainerView.getRootView(), sCachedWindowRect);

        // Get display coordinates.
        int displayWidth = mDelegate.getDisplayWidth();
        int displayHeight = mDelegate.getDisplayHeight();
        sCachedDisplayRect.set(0, 0, displayWidth, displayHeight);

        if (DEBUG) {
            Log.i(TAG,
                    "isDisplayInFullscreen. view rect: " + sCachedViewRect + ", display rect: "
                            + sCachedDisplayRect + ", window rect: " + sCachedWindowRect);
        }

        // Display is in fullscreen only when webview is occupying the entire window and display.
        // Checking the window rect is more complicated and therefore not doing it for now, but
        // there can still be cases where the window is a bit off.
        if (!sCachedViewRect.equals(sCachedDisplayRect)) {
            if (DEBUG) {
                Log.i(TAG, "WebView is not occupying the entire screen.");
            }
            return false;
        } else if (!sCachedViewRect.equals(sCachedWindowRect)) {
            if (DEBUG) {
                Log.i(TAG, "WebView is not occupying the entire window.");
            }
            return false;
        } else if (hasTransform()) {
            if (DEBUG) {
                Log.i(TAG, "WebView is rotated or scaled.");
            }
            return false;
        }
        return true;
    }

    private static void getViewRectOnScreen(View view, Rect rect) {
        if (view == null) {
            rect.set(0, 0, 0, 0);
            return;
        }
        view.getLocationOnScreen(sCachedLocationOnScreen);
        int width = view.getMeasuredWidth();
        int height = view.getMeasuredHeight();

        rect.set(sCachedLocationOnScreen[0], sCachedLocationOnScreen[1],
                sCachedLocationOnScreen[0] + width, sCachedLocationOnScreen[1] + height);
    }

    private boolean hasTransform() {
        sCachedMatrix.reset(); // set to identity
        // It seems that we can call this on P most of the time, but adding try-catch just in case.
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Api21.transformMatrixToGlobal(mContainerView, sCachedMatrix);
            } else {
                Api19.transformMatrixToGlobal(mContainerView, sCachedMatrix);
            }
        } catch (Throwable e) {
            Log.w(TAG, "Error checking transform for display mode: ", e);
            return true;
        }
        return !sCachedMatrix.isIdentity();
    }

    /**
     * Set the current container view.
     *
     * @param containerView The current container view.
     */
    public void setCurrentContainerView(View containerView) {
        if (DEBUG) Log.i(TAG, "setCurrentContainerView: " + containerView);
        mContainerView = containerView;
    }

    /**
     * Check if a view coordinates transforms to screen coordinates that is not an identity
     * matrix, which means that view is rotated or scaled in regards to the screen.
     * This API got hidden from L, and readded in API 29 (Q).
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @TargetApi(Build.VERSION_CODES.Q) // need this exception since we will try using Q API in P
    private static final class Api21 {

        static void transformMatrixToGlobal(View v, Matrix m) {
            v.transformMatrixToGlobal(m);
        }

        private Api21() {
        }
    }

    /**
     * @deprecated Use {@link android.view.View#transformMatrixToGlobal(Matrix)} instead,
     * which was {@code @hide} since API level 21 and is public since API level 29.
     */
    @DeprecatedSinceApi(api = Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("PrivateApi")
    private final static class Api19 {

        private static final Class<?> mViewRootImplClass;
        private static final Field mCurScrollYField;
        private static final Field mAttachInfoField;
        private static final Field mWindowLeftField;
        private static final Field mWindowTopField;

        static {
            try {
                mViewRootImplClass = Class.forName("android.view.ViewRootImpl");
                mCurScrollYField = mViewRootImplClass.getDeclaredField("mCurScrollY");
                mCurScrollYField.setAccessible(true);
                mAttachInfoField = mViewRootImplClass.getDeclaredField("mAttachInfo");
                mAttachInfoField.setAccessible(true);
                Class<?> attachInfoClass = mAttachInfoField.getType();
                mWindowLeftField = attachInfoClass.getDeclaredField("mWindowLeft");
                mWindowLeftField.setAccessible(true);
                mWindowTopField = attachInfoClass.getDeclaredField("mWindowTop");
                mWindowTopField.setAccessible(true);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }

        static void transformMatrixToGlobal(View v, Matrix m) {
            final ViewParent parent = v.getParent();
            if (parent instanceof View) {
                final View vp = (View) parent;
                transformMatrixToGlobal(vp, m);
                m.preTranslate(-vp.getScrollX(), -vp.getScrollY());
            } else if (mViewRootImplClass.isInstance(parent)) {
                final Object vr = parent;
                transformMatrixToGlobalViewRootImpl(vr, m);
                m.preTranslate(0, -getCurScrollYViewRootImpl(vr));
            }

            m.preTranslate(v.getLeft(), v.getTop());

            if (!hasIdentityMatrix(v)) {
                m.preConcat(v.getMatrix());
            }
        }

        private static void transformMatrixToGlobalViewRootImpl(Object vr, Matrix m) {
            try {
                Object attachInfo = mAttachInfoField.get(vr);
                int windowLeft = mWindowLeftField.getInt(attachInfo);
                int windowTop = mWindowTopField.getInt(attachInfo);
                m.preTranslate(windowLeft, windowTop);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        private static int getCurScrollYViewRootImpl(Object vr) {
            try {
                return mCurScrollYField.getInt(vr);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        private static boolean hasIdentityMatrix(View v) {
            return v.getMatrix().isIdentity();
        }

        private Api19() {
            // No instances.
        }
    }
}