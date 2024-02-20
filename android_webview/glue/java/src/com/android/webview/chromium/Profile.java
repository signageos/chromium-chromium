// Copyright 2023 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.android.webview.chromium;

import android.os.Build;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.ServiceWorkerController;
import android.webkit.WebStorage;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import org.chromium.android_webview.AwBrowserContext;
import org.chromium.android_webview.common.Lifetime;
import org.chromium.base.ThreadUtils;

import java.util.concurrent.atomic.AtomicReference;

/**
 * An abstraction of {@link AwBrowserContext}, this class reflects
 * the state needed for the multi-profile public API.
 */
@Lifetime.Profile
public class Profile {
    @NonNull
    private final String mName;

    @NonNull
    private final CookieManager mCookieManager;

    @NonNull
    private final WebStorage mWebStorage;

    @NonNull
    private final GeolocationPermissions mGeolocationPermissions;

    @NonNull
    private final AtomicReference<ServiceWorkerController> mServiceWorkerController =
            new AtomicReference<>();

    public Profile(@NonNull final AwBrowserContext browserContext) {
        assert ThreadUtils.runningOnUiThread();
        WebViewChromiumFactoryProviderBase factory = WebViewChromiumFactoryProviderBase.getSingleton();
        mName = browserContext.getName();

        if (browserContext.isDefaultAwBrowserContext()) {
            mCookieManager = factory.getCookieManager();
            mWebStorage = factory.getWebStorage();
            mGeolocationPermissions = factory.getGeolocationPermissions();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mServiceWorkerController.set(factory.getServiceWorkerController());
            }
        } else {
            mCookieManager = new CookieManagerAdapter2(browserContext.getCookieManager());
            mWebStorage = new WebStorageAdapter2(factory, browserContext.getQuotaManagerBridge());
            mGeolocationPermissions = new GeolocationPermissionsAdapter2(
                    factory, browserContext.getGeolocationPermissions());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mServiceWorkerController.set(
                        new ServiceWorkerControllerAdapter(browserContext.getServiceWorkerController()));
            }
        }
    }

    @NonNull
    public String getName() {
        return mName;
    }

    @NonNull
    public CookieManager getCookieManager() {
        return mCookieManager;
    }

    @NonNull
    public WebStorage getWebStorage() {
        return mWebStorage;
    }

    @NonNull
    public GeolocationPermissions getGeolocationPermissions() {
        return mGeolocationPermissions;
    }

    @RequiresApi(Build.VERSION_CODES.N)
    @NonNull
    public ServiceWorkerController getServiceWorkerController() {
        return mServiceWorkerController.get();
    }
}
