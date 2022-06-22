package com.android.webview.chromium;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Looper;

final class PlatformThreadUtils {
    @SuppressLint("PrivateApi")
    public static void setUiThread(Looper looper) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            try {
                Class.forName("com.android.org.chromium.base.ThreadUtils")
                        .getMethod("setUiThread", Looper.class)
                        .invoke(null, looper);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Invalid reflection", e);
            }
        }
    }

    private PlatformThreadUtils() {
        throw new AssertionError();
    }
}
