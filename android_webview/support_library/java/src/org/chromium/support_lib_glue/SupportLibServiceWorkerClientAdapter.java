// Copyright 2018 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.support_lib_glue;

import android.os.Build;
import android.webkit.WebResourceResponse;

import androidx.annotation.RequiresApi;

import com.android.webview.chromium.WebResourceRequestAdapter;

import org.chromium.android_webview.AwContentsClient.AwWebResourceRequest;
import org.chromium.android_webview.AwServiceWorkerClient;
import org.chromium.components.embedder_support.util.WebResourceResponseInfo;
import org.chromium.support_lib_boundary.ServiceWorkerClientBoundaryInterface;
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;
import org.chromium.support_lib_boundary.util.Features;

import java.util.Map;

/**
 * Adapter between ServiceWorkerClientBoundaryInterface and AwServiceWorkerClient.
 */
class SupportLibServiceWorkerClientAdapter extends AwServiceWorkerClient {
    ServiceWorkerClientBoundaryInterface mImpl;

    SupportLibServiceWorkerClientAdapter(ServiceWorkerClientBoundaryInterface impl) {
        mImpl = impl;
    }

    @Override
    public WebResourceResponseInfo shouldInterceptRequest(AwWebResourceRequest request) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // WebResourceRequest isn't available on Kitkat.
            return null;
        }
        if (!BoundaryInterfaceReflectionUtil.containsFeature(mImpl.getSupportedFeatures(),
                    Features.SERVICE_WORKER_SHOULD_INTERCEPT_REQUEST)) {
            // If the shouldInterceptRequest callback isn't supported, return null;
            return null;
        }
        WebResourceResponse response =
                mImpl.shouldInterceptRequest(new WebResourceRequestAdapter(request));
        if (response == null) {
            return null;
        }
        return new WebResourceResponseInfo(response.getMimeType(), response.getEncoding(),
                response.getData(), Api21.getStatusCode(response), Api21.getReasonPhrase(response),
                Api21.getResponseHeaders(response));
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private static final class Api21 {

        // No instances.
        private Api21() {}

        static String getReasonPhrase(WebResourceResponse response) {
            return response.getReasonPhrase();
        }

        static Map<String, String> getResponseHeaders(WebResourceResponse response) {
            return response.getResponseHeaders();
        }

        static int getStatusCode(WebResourceResponse response) {
            return response.getStatusCode();
        }
    }
}
