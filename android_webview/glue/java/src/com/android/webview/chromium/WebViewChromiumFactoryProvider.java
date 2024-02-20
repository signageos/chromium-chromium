// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.android.webview.chromium;

import android.webkit.WebViewFactory;

/**
 * Entry point to the WebView. The system framework talks to this class to get instances of the
 * implementation classes.
 *
 * <p>The exact initialization process depends on the platform OS level:
 * <ul>
 *
 * <li>On API 21 (no longer supported), the platform invoked a parameterless constructor. Since we
 * didn't have a WebViewDelegate instance, this required us to invoke WebViewDelegate methods via
 * reflection. This constructor has been removed from the code as we no longer support Android
 * 21.</li>
 *
 * <li>From API 22 through API 25, the platform instead directly calls the constructor with a
 * WebViewDelegate parameter (See internal CL http://ag/577188 or the public AOSP cherrypick
 * https://r.android.com/114870). API 22 (no longer supported) would fallback to the
 * parameterless constructor if the first constructor call throws an exception, however this
 * fallback was removed in API 23.</li>
 *
 * <li>Starting in API 26, the platform calls {@link #create} instead of calling the constructor
 * directly (see internal CLs http://ag/1334128 and http://ag/1846560).</li>
 *
 * <li>From API 27 onward, the platform code is updated during each release to use the {@code
 * WebViewChromiumFactoryProviderForX} subclass, where "X" is replaced by the actual platform API
 * version (ex. "ForOMR1"). It still invokes the {@link #create} method on the subclass. While the
 * OS version is still under development, the "ForX" subclass implements the new platform APIs (in a
 * private codebase). Once the APIs for that version have been finalized, we eventually roll these
 * implementations into this class and the "ForX" subclass just calls directly into this
 * implementation.</li>
 *
 * </ul>
 */
public class WebViewChromiumFactoryProvider extends WebViewChromiumFactoryProviderBase {

    /**
     * Entry point for Android 26 (Oreo) and above. See class docs for initialization details.
     */
    public static WebViewChromiumFactoryProvider create(android.webkit.WebViewDelegate delegate) {
        return new WebViewChromiumFactoryProvider(delegate);
    }

    /**
     * Constructor called by the API 21 version of {@link WebViewFactory} and earlier.
     */
    public WebViewChromiumFactoryProvider() {
        super(WebViewDelegateFactory.createApi21CompatibilityDelegate());
    }

    /**
     * Entry point for Android 22 (LMR1) through Android 25 (NMR1). Although this is still invoked
     * by {@link #create}, this constructor was invoked directly before {@link #create} was defined.
     * See class docs for initialization details.
     */
    public WebViewChromiumFactoryProvider(android.webkit.WebViewDelegate delegate) {
        super(WebViewDelegateFactory.createProxyDelegate(delegate));
    }
}
