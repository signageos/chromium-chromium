package com.android.webview.chromium;

import android.os.Build;
import android.text.TextUtils;
import android.webkit.WebResourceResponse;

import androidx.annotation.DeprecatedSinceApi;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import org.chromium.components.embedder_support.util.WebResourceResponseInfo;

import java.io.InputStream;
import java.util.Map;

final class ImmutableWebResourceResponse extends WebResourceResponse {

    private int mStatusCode;
    private String mReasonPhrase;
    private Map<String, String> mResponseHeaders;

    @DeprecatedSinceApi(api = Build.VERSION_CODES.LOLLIPOP)
    private ImmutableWebResourceResponse(String mimeType, String encoding,
                                         int statusCode, String reasonPhrase,
                                         Map<String, String> responseHeaders,
                                         InputStream data) {
        super(mimeType, encoding, data);
        mStatusCode = statusCode;
        mReasonPhrase = reasonPhrase;
        mResponseHeaders = responseHeaders;
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @DeprecatedSinceApi(api = Build.VERSION_CODES.M)
    private ImmutableWebResourceResponse(Object newApiMarker,
                                         String mimeType, String encoding,
                                         int statusCode, String reasonPhrase,
                                         Map<String, String> responseHeaders,
                                         InputStream data) {
        // We cannot pass a null or empty reasonPhrase, because this version of the
        // WebResourceResponse constructor will throw. But we may legitimately not
        // receive a reasonPhrase in the HTTP response, since HTTP/2 removed
        // Reason-Phrase from the spec (and discourages it). Instead, assign some dummy
        // value to avoid the crash. See http://crbug.com/925887.
        super(
                mimeType, encoding, statusCode,
                TextUtils.isEmpty(reasonPhrase) ? "UNKNOWN" : reasonPhrase,
                responseHeaders, data);
        mStatusCode = statusCode;
        mReasonPhrase = reasonPhrase;
        mResponseHeaders = responseHeaders;
    }

    @Override
    public void setMimeType(String mimeType) {
        checkImmutable();
    }

    @Override
    public void setEncoding(String encoding) {
        checkImmutable();
    }

    @Override
    public void setStatusCodeAndReasonPhrase(int statusCode, @NonNull String reasonPhrase) {
        checkImmutable();
    }

    @Override
    public int getStatusCode() {
        return mStatusCode;
    }

    @Override
    public String getReasonPhrase() {
        return mReasonPhrase;
    }

    @Override
    public void setResponseHeaders(Map<String, String> headers) {
        checkImmutable();
    }

    @Override
    public Map<String, String> getResponseHeaders() {
        return mResponseHeaders;
    }

    @Override
    public void setData(InputStream data) {
        checkImmutable();
    }

    private void checkImmutable() {
        throw new IllegalStateException("This WebResourceResponse instance is immutable");
    }

    static WebResourceResponse from(WebResourceResponseInfo response) {
        // Note: we use the @SystemApi constructor here because it relaxes several
        // requirements:
        // * response.getReasonPhrase() may legitimately be empty because HTTP/2 removed
        //   Reason-Phrase from the spec (https://crbug.com/925887).
        // * response.getStatusCode() may be out of the valid range if the web server is not
        //   obeying the HTTP spec (ex. http://b/235960500).
        //
        // Immutability is not strictly necessary, but apps should not not need to modify
        // the WebResourceResponse received in this callback (they can always construct
        // their own instance).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return new WebResourceResponse(/* immutable= */ true, response.getMimeType(),
                    response.getCharset(), response.getStatusCode(),
                    response.getReasonPhrase(), response.getResponseHeaders(),
                    response.getData());
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return new ImmutableWebResourceResponse(/* newApiMarker */ null, response.getMimeType(),
                    response.getCharset(), response.getStatusCode(),
                    response.getReasonPhrase(), response.getResponseHeaders(),
                    response.getData());
        } else {
            return new ImmutableWebResourceResponse(response.getMimeType(),
                    response.getCharset(), response.getStatusCode(),
                    response.getReasonPhrase(), response.getResponseHeaders(),
                    response.getData());
        }
    }
}
