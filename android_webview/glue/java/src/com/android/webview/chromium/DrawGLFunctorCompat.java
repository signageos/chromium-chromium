package com.android.webview.chromium;

import android.graphics.Canvas;
import android.view.View;

import androidx.annotation.RequiresApi;

import com.android.webview.chromium.WebViewDelegateFactory.WebViewDelegate;

import org.chromium.android_webview.AwContents;
import org.chromium.base.annotations.DoNotInline;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import static android.os.Build.VERSION.SDK_INT;

final class DrawGLFunctorCompat implements AwContents.NativeDrawGLFunctor {
    private final Object mDelegate;
    private final WebViewDelegate mWebViewDelegate;

    private int mNativeDrawGLFunctor;

    private DrawGLFunctorCompat(int viewContext, WebViewDelegate webViewDelegate) {
        try {
            mWebViewDelegate = webViewDelegate;
            mDelegate = createDrawGLFunctor(viewContext);
            mNativeDrawGLFunctor = getNativeDrawGLFunctor(mDelegate);
        } catch (Exception e) {
            throw new RuntimeException("Invalid reflection", e);
        }
    }

    private static Object createDrawGLFunctor(int viewContext) throws ReflectiveOperationException {
        return getDrawGLFunctorClass()
                .getConstructor(int.class)
                .newInstance(viewContext);
    }

    private static int getNativeDrawGLFunctor(Object delegate) throws ReflectiveOperationException {
        final Field destroyRunnableField = delegate.getClass()
                .getDeclaredField("mDestroyRunnable");
        destroyRunnableField.setAccessible(true);
        final Object destroyRunnable = destroyRunnableField.get(delegate);
        final Field nativeDrawGLFunctorField = destroyRunnable.getClass()
                .getDeclaredField("mNativeDrawGLFunctor");
        nativeDrawGLFunctorField.setAccessible(true);
        return nativeDrawGLFunctorField.getInt(destroyRunnable);
    }

    @Override
    public boolean requestDrawGL(Canvas canvas, Runnable releasedCallback) {
        if (mNativeDrawGLFunctor == 0) {
            throw new RuntimeException("requestDrawGL on already destroyed DrawGLFunctor");
        }
        assert canvas != null;
        assert releasedCallback == null;
        mWebViewDelegate.callDrawGlFunction(canvas, mNativeDrawGLFunctor);
        return true;
    }

    @Override
    public boolean requestInvokeGL(View containerView, boolean waitForCompletion) {
        if (mNativeDrawGLFunctor == 0) {
            throw new RuntimeException("requestInvokeGL on already destroyed DrawGLFunctor");
        }
        if (!mWebViewDelegate.canInvokeDrawGlFunctor(containerView)) {
            return false;
        }

        mWebViewDelegate.invokeDrawGlFunctor(
                containerView, mNativeDrawGLFunctor, waitForCompletion);
        return true;
    }

    @Override
    public boolean supportsDrawGLFunctorReleasedCallback() {
        return false;
    }

    @Override
    public void detach(View containerView) {
        if (mNativeDrawGLFunctor == 0) {
            throw new RuntimeException("detach on already destroyed DrawGLFunctor");
        }
        mWebViewDelegate.detachDrawGlFunctor(containerView, mNativeDrawGLFunctor);
    }

    @Override
    public void destroy() {
        if (mNativeDrawGLFunctor != 0) {
            mNativeDrawGLFunctor = 0;
            destroy(mDelegate);
        }
    }

    private static void destroy(Object delegate) {
        try {
            delegate.getClass()
                    .getMethod("destroy")
                    .invoke(delegate);
        } catch (Exception e) {
            throw new RuntimeException("Invalid reflection", e);
        }
    }

    public static AwContents.NativeDrawGLFunctor create(
            long viewContext, WebViewDelegate webViewDelegate) {
        if (SDK_INT >= 21) {
            return Api21.create(viewContext, webViewDelegate);
        } else {
            BigDecimal.valueOf(viewContext).intValueExact();
            return new DrawGLFunctorCompat((int) viewContext, webViewDelegate);
        }
    }

    public static void setChromiumAwDrawGLFunction(long functionPointer) {
        if (SDK_INT >= 21) {
            Api21.setChromiumAwDrawGLFunction(functionPointer);
        } else {
            BigDecimal.valueOf(functionPointer).intValueExact();
            setChromiumAwDrawGLFunctionInt((int) functionPointer);
        }
    }

    @SuppressWarnings("JavaReflectionMemberAccess")
    private static void setChromiumAwDrawGLFunctionInt(int functionPointer) {
        try {
            getDrawGLFunctorClass()
                    .getMethod("setChromiumAwDrawGLFunction", int.class)
                    .invoke(null, functionPointer);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Invalid reflection", e);
        }
    }

    /**
     * Prevent R8 from inlining
     * {@code Class.forName("com.android.webview.chromium.DrawGLFunctor")}
     * as package-private {@code DrawGLFunctor.class} from AOSP.
     */
    private static Class<?> getDrawGLFunctorClass() throws ClassNotFoundException {
        char[] chars = "dpn/boespje/xfcwjfx/dispnjvn/EsbxHMGvodups".toCharArray();
        for (int i = 0, size = chars.length; i < size; i++) {
            chars[i] = (char) (chars[i] - 1);
        }
        final String name = new String(chars);
        return Class.forName(name);
    }

    private DrawGLFunctorCompat() {
        throw new AssertionError();
    }

    @DoNotInline
    @RequiresApi(21)
    @SuppressWarnings("deprecation")
    private static final class Api21 {
        // Indirection to avoid class verification error on Kitkat.

        public static AwContents.NativeDrawGLFunctor create(
                long viewContext, WebViewDelegate webViewDelegate) {
            return new DrawGLFunctor(viewContext, webViewDelegate);
        }

        public static void setChromiumAwDrawGLFunction(long functionPointer) {
            DrawGLFunctor.setChromiumAwDrawGLFunction(functionPointer);
        }

        private Api21() {
            throw new AssertionError();
        }
    }
}
