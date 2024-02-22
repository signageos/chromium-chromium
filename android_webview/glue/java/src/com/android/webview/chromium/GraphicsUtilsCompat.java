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
            return (int) getGraphicsUtilsClass()
                    .getMethod("getDrawSWFunctionTable")
                    .invoke(null);
        } catch (Exception e) {
            throw new RuntimeException("Invalid reflection", e);
        }
    }

    private static long getDrawGLFunctionTableInt() {
        try {
            return (int) getGraphicsUtilsClass()
                    .getMethod("getDrawGLFunctionTable")
                    .invoke(null);
        } catch (Exception e) {
            throw new RuntimeException("Invalid reflection", e);
        }
    }

    /**
     * Prevent R8 from inlining
     * {@code Class.forName("com.android.webview.chromium.GraphicsUtils")}
     * as package-private {@code GraphicsUtils.class} from AOSP.
     */
    private static Class<?> getGraphicsUtilsClass() throws ClassNotFoundException {
        char[] chars = "dpn/boespje/xfcwjfx/dispnjvn/HsbqijdtVujmt".toCharArray();
        for (int i = 0, size = chars.length; i < size; i++) {
            chars[i] = (char) (chars[i] - 1);
        }
        final String name = new String(chars);
        return Class.forName(name);
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
