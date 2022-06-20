// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.android.webview.chromium;

import android.content.pm.PackageInfo;
import android.webkit.WebViewFactory;

class WebViewChromiumFactoryProviderForK extends WebViewChromiumFactoryProviderBase
        implements LoadedPackageInfoOwner {
    /**
     * Entry point for the API 19 version of {@link WebViewFactory}.
     */
    public static WebViewChromiumFactoryProviderForK create(PackageInfo loadedPackageInfo) {
        return new WebViewChromiumFactoryProviderForK(loadedPackageInfo);
    }

    protected WebViewChromiumFactoryProviderForK(PackageInfo loadedPackageInfo) {
        super(WebViewDelegateFactory.createApi19CompatibilityDelegate(loadedPackageInfo));
    }

    @Override
    public PackageInfo getLoadedPackageInfo() {
        return ((LoadedPackageInfoOwner) getWebViewDelegate()).getLoadedPackageInfo();
    }
}
