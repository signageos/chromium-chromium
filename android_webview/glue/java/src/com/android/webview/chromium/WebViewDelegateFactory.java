// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.android.webview.chromium;

import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.os.Trace;
import android.util.SparseArray;
import android.view.View;

import org.chromium.android_webview.gfx.AwDrawFnImpl;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Factory class for {@link WebViewDelegate com.android.webview.chromium.WebViewDelegate}s.
 *
 * <p>{@link WebViewDelegate com.android.webview.chromium.WebViewDelegate}s provide the same
 * interface as {@link android.webkit.WebViewDelegate android.webkit.WebViewDelegate} but without
 * a dependency on the webkit class. Defining our own
 * {@link WebViewDelegate com.android.webview.chromium.WebViewDelegate} in frameworks/webview
 * allows the WebView apk to be binary compatible with the API 21 version of the framework, in
 * which {@link android.webkit.WebViewDelegate android.webkit.WebViewDelegate} had not yet been
 * introduced.
 *
 * <p>The {@link WebViewDelegate com.android.webview.chromium.WebViewDelegate} interface and this
 * factory class can be removed once we don't longer need to support WebView apk updates to devices
 * running the API 21 version of the framework. At that point, we should use
 * {@link android.webkit.WebViewDelegate android.webkit.WebViewDelegate} directly instead.
 */
class WebViewDelegateFactory {
    /**
     * Copy of {@link android.webkit.WebViewDelegate android.webkit.WebViewDelegate}'s interface.
     * See {@link WebViewDelegateFactory} for the reasons why this copy is needed.
     */
    interface WebViewDelegate extends AwDrawFnImpl.DrawFnAccess {
        /** @see android.webkit.WebViewDelegate.OnTraceEnabledChangeListener */
        interface OnTraceEnabledChangeListener {
            void onTraceEnabledChange(boolean enabled);
        }

        /** @see android.webkit.WebViewDelegate#setOnTraceEnabledChangeListener */
        void setOnTraceEnabledChangeListener(final OnTraceEnabledChangeListener listener);

        /** @see android.webkit.WebViewDelegate#isTraceTagEnabled */
        boolean isTraceTagEnabled();

        /** @see android.webkit.WebViewDelegate#canInvokeDrawGlFunctor */
        boolean canInvokeDrawGlFunctor(View containerView);

        /** @see android.webkit.WebViewDelegate#invokeDrawGlFunctor */
        void invokeDrawGlFunctor(
                View containerView, long nativeDrawGLFunctor, boolean waitForCompletion);

        /** @see android.webkit.WebViewDelegate#callDrawGlFunction. Available API level 23 and
         * below.
         */
        void callDrawGlFunction(Canvas canvas, long nativeDrawGLFunctor);

        /** @see android.webkit.WebViewDelegate#callDrawGlFunction. Available above API level 23
         * only. */
        void callDrawGlFunction(Canvas canvas, long nativeDrawGLFunctor, Runnable releasedRunnable);

        /** @see android.webkit.WebViewDelegate#detachDrawGlFunctor */
        void detachDrawGlFunctor(View containerView, long nativeDrawGLFunctor);

        /** @see android.webkit.WebViewDelegate#getPackageId */
        int getPackageId(Resources resources, String packageName);

        /** @see android.webkit.WebViewDelegate#getApplication */
        Application getApplication();

        /** @see android.webkit.WebViewDelegate#getErrorString */
        String getErrorString(Context context, int errorCode);

        /** @see android.webkit.WebViewDelegate#addWebViewAssetPath */
        void addWebViewAssetPath(Context context);

        /** @see android.webkit.WebViewDelegate#isMultiProcessEnabled */
        boolean isMultiProcessEnabled();

        /** @see android.webkit.WebViewDelegate#getDataDirectorySuffix */
        String getDataDirectorySuffix();
    }

    /**
     * Creates a {@link WebViewDelegate com.android.webview.chromium.WebViewDelegate} that proxies
     * requests to the given {@link android.webkit.WebViewDelegate android.webkit.WebViewDelegate}.
     *
     * @return the created delegate
     */
    static WebViewDelegate createProxyDelegate(android.webkit.WebViewDelegate delegate) {
        return new ProxyDelegate(delegate);
    }

    /**
     * Creates a {@link WebViewDelegate com.android.webview.chromium.WebViewDelegate} compatible
     * with the API 21 version of the framework in which
     * {@link android.webkit.WebViewDelegate android.webkit.WebViewDelegate} had not yet been
     * introduced.
     *
     * @return the created delegate
     */
    static WebViewDelegate createApi21CompatibilityDelegate() {
        return new Api21CompatibilityDelegate();
    }

    /**
     * Creates a {@link WebViewDelegate com.android.webview.chromium.WebViewDelegate} compatible
     * with the API 19 version of the framework in which
     * {@link android.webkit.WebViewDelegate android.webkit.WebViewDelegate} had not yet been
     * introduced.
     *
     * @return the created delegate
     */
    static WebViewDelegate createApi19CompatibilityDelegate(PackageInfo loadedPackageInfo) {
        return new Api19CompatibilityDelegate(loadedPackageInfo);
    }

    /**
     * A {@link WebViewDelegate com.android.webview.chromium.WebViewDelegate} that proxies requests
     * to a {@link android.webkit.WebViewDelegate android.webkit.WebViewDelegate}.
     */
    static class ProxyDelegate implements WebViewDelegate {
        android.webkit.WebViewDelegate mDelegate;

        ProxyDelegate(android.webkit.WebViewDelegate delegate) {
            mDelegate = delegate;
        }

        @Override
        public void setOnTraceEnabledChangeListener(final OnTraceEnabledChangeListener listener) {
            mDelegate.setOnTraceEnabledChangeListener(
                    new android.webkit.WebViewDelegate.OnTraceEnabledChangeListener() {
                        @Override
                        public void onTraceEnabledChange(boolean enabled) {
                            listener.onTraceEnabledChange(enabled);
                        }
                    });
        }

        @Override
        public boolean isTraceTagEnabled() {
            return mDelegate.isTraceTagEnabled();
        }

        @Override
        public boolean canInvokeDrawGlFunctor(View containerView) {
            return mDelegate.canInvokeDrawGlFunctor(containerView);
        }

        @Override
        public void invokeDrawGlFunctor(
                View containerView, long nativeDrawGLFunctor, boolean waitForCompletion) {
            mDelegate.invokeDrawGlFunctor(containerView, nativeDrawGLFunctor, waitForCompletion);
        }

        @Override
        public void callDrawGlFunction(Canvas canvas, long nativeDrawGLFunctor) {
            mDelegate.callDrawGlFunction(canvas, nativeDrawGLFunctor);
        }

        @Override
        public void callDrawGlFunction(
                Canvas canvas, long nativeDrawGLFunctor, Runnable releasedRunnable) {
            GlueApiHelperForN.callDrawGlFunction(
                    mDelegate, canvas, nativeDrawGLFunctor, releasedRunnable);
        }

        @Override
        public void detachDrawGlFunctor(View containerView, long nativeDrawGLFunctor) {
            mDelegate.detachDrawGlFunctor(containerView, nativeDrawGLFunctor);
        }

        @Override
        public int getPackageId(Resources resources, String packageName) {
            return mDelegate.getPackageId(resources, packageName);
        }

        @Override
        public Application getApplication() {
            return mDelegate.getApplication();
        }

        @Override
        public String getErrorString(Context context, int errorCode) {
            return mDelegate.getErrorString(context, errorCode);
        }

        @Override
        public void addWebViewAssetPath(Context context) {
            // In the Android framework (<= API level 23)
            // ContextThemeWrapper provides an implementation of
            // getResources() that may proxy to either the wrapped
            // context or a newly constructed context, but it does not
            // provide an implementation of getAssets() that overrides
            // the ContextWrapper implementation that always proxies
            // to the wrapped context. This means that getAssets() and
            // getResources().getAssets() may potentially return
            // different AssetManagers, confusing WebView.
            //
            // To work around this problem, we provide an additional
            // wrapper here here to avoid calling the getAssets()
            // proxy chain (which we cannot change because it is in
            // WebView framework code).
            mDelegate.addWebViewAssetPath(new ContextWrapper(context) {
                @Override
                public AssetManager getAssets() {
                    return getResources().getAssets();
                }
            });
        }

        @Override
        public boolean isMultiProcessEnabled() {
            return GlueApiHelperForO.isMultiProcessEnabled(mDelegate);
        }

        @Override
        public String getDataDirectorySuffix() {
            return GlueApiHelperForP.getDataDirectorySuffix(mDelegate);
        }

        @Override
        public void drawWebViewFunctor(Canvas canvas, int functor) {
            mDelegate.drawWebViewFunctor(canvas, functor);
        }
    }

    /**
     * A {@link WebViewDelegate com.android.webview.chromium.WebViewDelegate} compatible with the
     * API 21 version of the framework in which
     * {@link android.webkit.WebViewDelegate android.webkit.WebViewDelegate} had not yet been
     * introduced.
     *
     * <p>This class implements the
     * {@link WebViewDelegate com.android.webview.chromium.WebViewDelegate} functionality by using
     * reflection to call into hidden frameworks APIs released in the API-21 version of the
     * framework.
     */
    private static class Api21CompatibilityDelegate implements WebViewDelegate {
        /** Copy of Trace.TRACE_TAG_WEBVIEW */
        private static final long TRACE_TAG_WEBVIEW = 1L << 4;

        /** Hidden APIs released in the API 21 version of the framework */
        private final Method mIsTagEnabledMethod;
        private final Method mAddChangeCallbackMethod;
        private final Method mGetViewRootImplMethod;
        private final Method mInvokeFunctorMethod;
        private final Method mCallDrawGLFunctionMethod;
        private final Method mDetachFunctorMethod;
        private final Method mGetAssignedPackageIdentifiersMethod;
        private final Method mAddAssetPathMethod;
        private final Method mCurrentApplicationMethod;
        private final Method mGetStringMethod;
        private final Method mGetLoadedPackageInfoMethod;

        Api21CompatibilityDelegate() {
            try {
                // Important: This reflection essentially defines a snapshot of some hidden APIs
                // at version 21 of the framework for compatibility reasons, and the reflection
                // should not be changed even if those hidden APIs change in future releases.
                mIsTagEnabledMethod = Trace.class.getMethod("isTagEnabled", long.class);
                mAddChangeCallbackMethod = Class.forName("android.os.SystemProperties")
                                                   .getMethod("addChangeCallback", Runnable.class);
                mGetViewRootImplMethod = View.class.getMethod("getViewRootImpl");
                mInvokeFunctorMethod =
                        Class.forName("android.view.ViewRootImpl")
                                .getMethod("invokeFunctor", long.class, boolean.class);
                mDetachFunctorMethod = Class.forName("android.view.ViewRootImpl")
                                               .getMethod("detachFunctor", long.class);
                mCallDrawGLFunctionMethod = Class.forName("android.view.HardwareCanvas")
                                                    .getMethod("callDrawGLFunction", long.class);
                mGetAssignedPackageIdentifiersMethod =
                        AssetManager.class.getMethod("getAssignedPackageIdentifiers");
                mAddAssetPathMethod = AssetManager.class.getMethod("addAssetPath", String.class);
                mCurrentApplicationMethod =
                        Class.forName("android.app.ActivityThread").getMethod("currentApplication");
                mGetStringMethod = Class.forName("android.net.http.ErrorStrings")
                                           .getMethod("getString", int.class, Context.class);
                mGetLoadedPackageInfoMethod = Class.forName("android.webkit.WebViewFactory")
                                                      .getMethod("getLoadedPackageInfo");
            } catch (Exception e) {
                throw new RuntimeException("Invalid reflection", e);
            }
        }

        @Override
        public void setOnTraceEnabledChangeListener(final OnTraceEnabledChangeListener listener) {
            try {
                mAddChangeCallbackMethod.invoke(null, new Runnable() {
                    @Override
                    public void run() {
                        listener.onTraceEnabledChange(isTraceTagEnabled());
                    }
                });
            } catch (Exception e) {
                throw new RuntimeException("Invalid reflection", e);
            }
        }

        @Override
        public boolean isTraceTagEnabled() {
            try {
                return ((Boolean) mIsTagEnabledMethod.invoke(null, TRACE_TAG_WEBVIEW));
            } catch (Exception e) {
                throw new RuntimeException("Invalid reflection", e);
            }
        }

        @Override
        public boolean canInvokeDrawGlFunctor(View containerView) {
            try {
                Object viewRootImpl = mGetViewRootImplMethod.invoke(containerView);
                // viewRootImpl can be null during teardown when window is leaked.
                return viewRootImpl != null;
            } catch (Exception e) {
                throw new RuntimeException("Invalid reflection", e);
            }
        }

        @Override
        public void invokeDrawGlFunctor(
                View containerView, long nativeDrawGLFunctor, boolean waitForCompletion) {
            try {
                Object viewRootImpl = mGetViewRootImplMethod.invoke(containerView);
                if (viewRootImpl != null) {
                    mInvokeFunctorMethod.invoke(
                            viewRootImpl, nativeDrawGLFunctor, waitForCompletion);
                }
            } catch (Exception e) {
                throw new RuntimeException("Invalid reflection", e);
            }
        }

        @Override
        public void callDrawGlFunction(Canvas canvas, long nativeDrawGLFunctor) {
            try {
                mCallDrawGLFunctionMethod.invoke(canvas, nativeDrawGLFunctor);
            } catch (Exception e) {
                throw new RuntimeException("Invalid reflection", e);
            }
        }

        @Override
        public void callDrawGlFunction(
                Canvas canvas, long nativeDrawGLFunctor, Runnable releasedRunnable) {
            throw new RuntimeException("Call not supported");
        }

        @Override
        public void detachDrawGlFunctor(View containerView, long nativeDrawGLFunctor) {
            try {
                Object viewRootImpl = mGetViewRootImplMethod.invoke(containerView);
                if (viewRootImpl != null) {
                    mDetachFunctorMethod.invoke(viewRootImpl, nativeDrawGLFunctor);
                }
            } catch (Exception e) {
                throw new RuntimeException("Invalid reflection", e);
            }
        }

        @Override
        public int getPackageId(Resources resources, String packageName) {
            try {
                SparseArray packageIdentifiers =
                        (SparseArray) mGetAssignedPackageIdentifiersMethod.invoke(
                                resources.getAssets());
                for (int i = 0; i < packageIdentifiers.size(); i++) {
                    final String name = (String) packageIdentifiers.valueAt(i);

                    if (packageName.equals(name)) {
                        return packageIdentifiers.keyAt(i);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Invalid reflection", e);
            }
            throw new RuntimeException("Package not found: " + packageName);
        }

        @Override
        public Application getApplication() {
            try {
                return (Application) mCurrentApplicationMethod.invoke(null);
            } catch (Exception e) {
                throw new RuntimeException("Invalid reflection", e);
            }
        }

        @Override
        public String getErrorString(Context context, int errorCode) {
            try {
                return (String) mGetStringMethod.invoke(null, errorCode, context);
            } catch (Exception e) {
                throw new RuntimeException("Invalid reflection", e);
            }
        }

        @Override
        public void addWebViewAssetPath(Context context) {
            try {
                PackageInfo info = (PackageInfo) mGetLoadedPackageInfoMethod.invoke(null);
                // Avoid calling the ContextWrapper.getAssets() proxy
                // chain, which can return an unexpected AssetManager.
                mAddAssetPathMethod.invoke(
                        context.getResources().getAssets(), info.applicationInfo.sourceDir);
            } catch (Exception e) {
                throw new RuntimeException("Invalid reflection", e);
            }
        }

        @Override
        public boolean isMultiProcessEnabled() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getDataDirectorySuffix() {
            return null;
        }

        @Override
        public void drawWebViewFunctor(Canvas canvas, int functor) {
            throw new RuntimeException();
        }
    }

    /**
     * A {@link WebViewDelegate com.android.webview.chromium.WebViewDelegate} compatible with the
     * API 19 version of the framework in which
     * {@link android.webkit.WebViewDelegate android.webkit.WebViewDelegate} had not yet been
     * introduced.
     *
     * <p>This class implements the
     * {@link WebViewDelegate com.android.webview.chromium.WebViewDelegate} functionality by using
     * reflection to call into hidden frameworks APIs released in the API-19 version of the
     * framework.
     */
    private static class Api19CompatibilityDelegate implements WebViewDelegate,
            LoadedPackageInfoOwner {
        /** Copy of Trace.TRACE_TAG_WEBVIEW */
        private static final long TRACE_TAG_WEBVIEW = 1L << 4;

        /** Hidden APIs released in the API 19 version of the framework */
        private final Method mIsTagEnabledMethod;
        private final Method mAddChangeCallbackMethod;
        private final Method mGetViewRootImplMethod;
        private final Method mAttachFunctorMethod;
        private final Method mCallDrawGLFunctionMethod;
        private final Method mDetachFunctorMethod;
        private final Method mAddAssetPathMethod;
        private final Method mCurrentApplicationMethod;
        private final Method mGetStringMethod;

        private final Method mGetStringBlockCountMethod;
        private final Method mGetNativeStringBlockMethod;
        private final Field mStringBlocksField;
        private final Class<?> mStringBlockClass;
        private final Constructor<?> mStringBlockCtor;

        private final PackageInfo mLoadedPackageInfo;

        Api19CompatibilityDelegate(PackageInfo loadedPackageInfo) {
            try {
                // Important: This reflection essentially defines a snapshot of some hidden APIs
                // at version 19 of the framework for compatibility reasons, and the reflection
                // should not be changed even if those hidden APIs change in future releases.
                mIsTagEnabledMethod = Trace.class.getMethod("isTagEnabled", long.class);
                mAddChangeCallbackMethod = Class.forName("android.os.SystemProperties")
                                                   .getMethod("addChangeCallback", Runnable.class);
                mGetViewRootImplMethod = View.class.getMethod("getViewRootImpl");
                mAttachFunctorMethod =
                        Class.forName("android.view.ViewRootImpl")
                                .getMethod("attachFunctor", int.class);
                mDetachFunctorMethod = Class.forName("android.view.ViewRootImpl")
                                               .getMethod("detachFunctor", int.class);
                mCallDrawGLFunctionMethod = Class.forName("android.view.HardwareCanvas")
                                                    .getMethod("callDrawGLFunction", int.class);
                mAddAssetPathMethod = AssetManager.class.getMethod("addAssetPath", String.class);
                mCurrentApplicationMethod =
                        Class.forName("android.app.ActivityThread").getMethod("currentApplication");
                mGetStringMethod = Class.forName("android.net.http.ErrorStrings")
                                           .getMethod("getString", int.class, Context.class);

                mGetStringBlockCountMethod = AssetManager.class
                        .getDeclaredMethod("getStringBlockCount");
                mGetStringBlockCountMethod.setAccessible(true);
                mGetNativeStringBlockMethod = AssetManager.class
                        .getDeclaredMethod("getNativeStringBlock", int.class);
                mGetNativeStringBlockMethod.setAccessible(true);
                mStringBlocksField = AssetManager.class
                        .getDeclaredField("mStringBlocks");
                mStringBlocksField.setAccessible(true);
                mStringBlockClass = Class.forName("android.content.res.StringBlock");
                mStringBlockCtor = mStringBlockClass.getDeclaredConstructor(int.class, boolean.class);
                mStringBlockCtor.setAccessible(true);
            } catch (Exception e) {
                throw new RuntimeException("Invalid reflection", e);
            }
            mLoadedPackageInfo = loadedPackageInfo;
        }

        @Override
        public void setOnTraceEnabledChangeListener(final OnTraceEnabledChangeListener listener) {
            try {
                mAddChangeCallbackMethod.invoke(null, new Runnable() {
                    @Override
                    public void run() {
                        listener.onTraceEnabledChange(isTraceTagEnabled());
                    }
                });
            } catch (Exception e) {
                throw new RuntimeException("Invalid reflection", e);
            }
        }

        @Override
        public boolean isTraceTagEnabled() {
            try {
                return ((Boolean) mIsTagEnabledMethod.invoke(null, TRACE_TAG_WEBVIEW));
            } catch (Exception e) {
                throw new RuntimeException("Invalid reflection", e);
            }
        }

        @Override
        public boolean canInvokeDrawGlFunctor(View containerView) {
            return false;
        }

        @Override
        public void invokeDrawGlFunctor(
                View containerView, long nativeDrawGLFunctor, boolean waitForCompletion) {
            throw new RuntimeException("Call not supported");
        }

        @Override
        public void callDrawGlFunction(Canvas canvas, long nativeDrawGLFunctor) {
            try {
                mCallDrawGLFunctionMethod.invoke(canvas, (int) nativeDrawGLFunctor);
            } catch (Exception e) {
                throw new RuntimeException("Invalid reflection", e);
            }
        }

        @Override
        public void callDrawGlFunction(
                Canvas canvas, long nativeDrawGLFunctor, Runnable releasedRunnable) {
            throw new RuntimeException("Call not supported");
        }

        @Override
        public void detachDrawGlFunctor(View containerView, long nativeDrawGLFunctor) {
            try {
                Object viewRootImpl = mGetViewRootImplMethod.invoke(containerView);
                if (viewRootImpl != null) {
                    mDetachFunctorMethod.invoke(viewRootImpl, (int) nativeDrawGLFunctor);
                }
            } catch (Exception e) {
                throw new RuntimeException("Invalid reflection", e);
            }
        }

        @Override
        public int getPackageId(Resources resources, String packageName) {
            // Look for matching package by first resource of any type.
            // This covers both *.webview and *.webview_translations resources.
            // This package is always zero on Kitkat.
            for (int packageId = 0x00; packageId <= 0x7f; packageId++) {
                for (int resType = 0x01; resType <= 0xff; resType++) {
                    int resId = (packageId << 24) | (resType << 16);
                    try {
                        final String resourcePackageName = resources.getResourcePackageName(resId);
                        if (resourcePackageName.equals(packageName)) {
                            return packageId;
                        } else {
                            // Skip the rest of resources belonging to this package.
                            break;
                        }
                    } catch (Resources.NotFoundException ignore) {
                    }
                }
            }
            throw new RuntimeException("Package not found: " + packageName);
        }

        @Override
        public Application getApplication() {
            try {
                return (Application) mCurrentApplicationMethod.invoke(null);
            } catch (Exception e) {
                throw new RuntimeException("Invalid reflection", e);
            }
        }

        @Override
        public String getErrorString(Context context, int errorCode) {
            try {
                return (String) mGetStringMethod.invoke(null, errorCode, context);
            } catch (Exception e) {
                throw new RuntimeException("Invalid reflection", e);
            }
        }

        @Override
        public void addWebViewAssetPath(Context context) {
            try {
                PackageInfo info = mLoadedPackageInfo;
                // Avoid calling the ContextWrapper.getAssets() proxy
                // chain, which can return an unexpected AssetManager.
                final AssetManager assets = context.getResources().getAssets();
                synchronized (assets) {
                    mAddAssetPathMethod.invoke(assets, info.applicationInfo.sourceDir);
                    makeStringBlocks(assets, getStringBlocks(assets));
                }
            } catch (Exception e) {
                throw new RuntimeException("Invalid reflection", e);
            }
        }

        private Object[] getStringBlocks(AssetManager assets) throws Exception {
            return (Object[]) mStringBlocksField.get(assets);
        }

        private void setStringBlocks(AssetManager assets, Object[] stringBlocks) throws Exception {
            mStringBlocksField.set(assets, stringBlocks);
        }

        // https://android.googlesource.com/platform/frameworks/base/+/kitkat-release/core/java/android/content/res/AssetManager.java#256
        // https://android.googlesource.com/platform/frameworks/base/+/lollipop-release/core/java/android/content/res/AssetManager.java#255
        private void makeStringBlocks(AssetManager assets, Object[] seed) throws Exception {
            final int seedNum = (seed != null) ? seed.length : 0;
            final int num = getStringBlockCount(assets);
            final Object[] stringBlocks = (Object[]) Array.newInstance(mStringBlockClass, num);
            for (int i = 0; i < num; i++) {
                if (i < seedNum) {
                    stringBlocks[i] = seed[i];
                } else {
                    stringBlocks[i] = createStringBlock(getNativeStringBlock(assets, i), true);
                }
            }
            setStringBlocks(assets, stringBlocks);
        }

        // https://android.googlesource.com/platform/frameworks/base/+/kitkat-release/core/java/android/content/res/AssetManager.java#712
        private int getStringBlockCount(AssetManager assets) throws Exception {
            return (int) mGetStringBlockCountMethod.invoke(assets);
        }

        // https://android.googlesource.com/platform/frameworks/base/+/kitkat-release/core/java/android/content/res/AssetManager.java#713
        private int getNativeStringBlock(AssetManager assets, int block) throws Exception {
            return (int) mGetNativeStringBlockMethod.invoke(assets, block);
        }

        // https://android.googlesource.com/platform/frameworks/base/+/kitkat-release/core/java/android/content/res/StringBlock.java#477
        private Object createStringBlock(int obj, boolean useSparse) throws Exception {
            return mStringBlockCtor.newInstance(obj, useSparse);
        }

        @Override
        public boolean isMultiProcessEnabled() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getDataDirectorySuffix() {
            return null;
        }

        @Override
        public void drawWebViewFunctor(Canvas canvas, int functor) {
            throw new RuntimeException();
        }

        @Override
        public PackageInfo getLoadedPackageInfo() {
            return mLoadedPackageInfo;
        }
    }
}
