package com.android.webview.chromium;

import android.os.Build;

import androidx.annotation.RequiresApi;

import org.chromium.build.annotations.DoNotInline;

final class GraphicsUtilsCompat {

    static long getDrawSWFunctionTable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return Api21.getDrawSWFunctionTable();
        } else {
            return getDrawSWFunctionTableInt();
        }
    }

    static long getDrawGLFunctionTable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return Api21.getDrawGLFunctionTable();
        } else {
            return getDrawGLFunctionTableInt();
        }
    }

    private static long getDrawSWFunctionTableInt() {
        try {
            return (int) Class.forName("com.android.webview.chromium.GraphicsUtils")
                    .getMethod("getDrawSWFunctionTable")
                    .invoke(null);
        } catch (Exception e) {
            throw new RuntimeException("Invalid reflection", e);
        }
    }

    private static long getDrawGLFunctionTableInt() {
        try {
            return (int) Class.forName("com.android.webview.chromium.GraphicsUtils")
                    .getMethod("getDrawGLFunctionTable")
                    .invoke(null);
        } catch (Exception e) {
            throw new RuntimeException("Invalid reflection", e);
        }
    }

    private GraphicsUtilsCompat() {
        throw new AssertionError();
    }

    @DoNotInline
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @SuppressWarnings("deprecation")
    private static final class Api21 {
        // Indirection to avoid class verification error on Kitkat.

        static long getDrawSWFunctionTable() {
            return GraphicsUtils.getDrawSWFunctionTable();
        }

        static long getDrawGLFunctionTable() {
            return GraphicsUtils.getDrawGLFunctionTable();
        }

        private Api21() {
            throw new AssertionError();
        }
    }
}
