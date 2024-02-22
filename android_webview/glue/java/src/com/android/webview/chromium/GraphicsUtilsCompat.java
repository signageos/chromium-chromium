package com.android.webview.chromium;

import androidx.annotation.RequiresApi;

import org.chromium.base.annotations.DoNotInline;

import static android.os.Build.VERSION.SDK_INT;

final class GraphicsUtilsCompat {

    static long getDrawSWFunctionTable() {
        if (SDK_INT >= 21) {
            return Api21.getDrawSWFunctionTable();
        } else {
            return getDrawSWFunctionTableInt();
        }
    }

    static long getDrawGLFunctionTable() {
        if (SDK_INT >= 21) {
            return Api21.getDrawGLFunctionTable();
        } else {
            return getDrawGLFunctionTableInt();
        }
    }

    private static int getDrawSWFunctionTableInt() {
        try {
            return (int) getGraphicsUtilsClass()
                    .getMethod("getDrawSWFunctionTable")
                    .invoke(null);
        } catch (Exception e) {
            throw new RuntimeException("Invalid reflection", e);
        }
    }

    private static int getDrawGLFunctionTableInt() {
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
    @RequiresApi(21)
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
