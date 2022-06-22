package com.android.webview.chromium;

import android.annotation.SuppressLint;
import android.os.Looper;

import static android.os.Build.VERSION.SDK_INT;

final class PlatformThreadUtils {
    @SuppressLint("PrivateApi")
    public static void setUiThread(Looper looper) {
        if (SDK_INT < 21) {
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
