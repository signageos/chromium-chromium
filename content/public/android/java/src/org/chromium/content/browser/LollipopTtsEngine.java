// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.content.browser;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import org.chromium.base.annotations.JNINamespace;
import org.chromium.content.browser.TtsPlatformImpl.TtsEngine;

/**
 * Subclass of TtsEngine for Lollipop to make use of newer APIs.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class LollipopTtsEngine extends TtsEngine {
    LollipopTtsEngine(long nativeTtsPlatformImplAndroid) {
        super(nativeTtsPlatformImplAndroid);
    }

    LollipopTtsEngine(String engineId) {
        super(engineId);
    }

    /**
     * Overrides TtsEngine because the API changed in Lollipop.
     */
    @Override
    protected int callSpeak(String text, float volume, int utteranceId) {
        Bundle params = new Bundle();
        if (volume != 1.0) {
            params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume);
        }
        return mTextToSpeech.speak(
                text, TextToSpeech.QUEUE_FLUSH, params, Integer.toString(utteranceId));
    }
}
