// Copyright 2014 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.android.webview.chromium;

import android.webkit.WebBackForwardList;
import android.webkit.WebHistoryItem;

import org.chromium.content_public.browser.NavigationHistory;

import java.util.ArrayList;
import java.util.List;

/**
 * WebView Chromium implementation of WebBackForwardList. Simple immutable
 * wrapper around NavigationHistory.
 */
@SuppressWarnings("NoSynchronizedMethodCheck")
public class WebBackForwardListChromium2 extends WebBackForwardList {
    private final List<WebHistoryItemChromium2> mHistoryItemList;
    private final int mCurrentIndex;

    /* package */ WebBackForwardListChromium2(NavigationHistory navHistory) {
        boolean onInitialEntry =
                (navHistory.getEntryCount() == 1 && navHistory.getEntryAtIndex(0).isInitialEntry());
        if (onInitialEntry) {
            // The initial NavigationEntry should not be exposed in the WebBackForwardList.
            mCurrentIndex = -1;
            mHistoryItemList = new ArrayList<WebHistoryItemChromium2>(0);
            return;
        }
        mCurrentIndex = navHistory.getCurrentEntryIndex();
        mHistoryItemList = new ArrayList<WebHistoryItemChromium2>(navHistory.getEntryCount());
        for (int i = 0; i < navHistory.getEntryCount(); ++i) {
            mHistoryItemList.add(new WebHistoryItemChromium2(navHistory.getEntryAtIndex(i)));
        }
    }

    /**
     * See {@link android.webkit.WebBackForwardList#getCurrentItem}.
     */
    @Override
    public synchronized WebHistoryItem getCurrentItem() {
        if (getSize() == 0) {
            return null;
        } else {
            return getItemAtIndex(getCurrentIndex());
        }
    }

    /**
     * See {@link android.webkit.WebBackForwardList#getCurrentIndex}.
     */
    @Override
    public synchronized int getCurrentIndex() {
        return mCurrentIndex;
    }

    /**
     * See {@link android.webkit.WebBackForwardList#getItemAtIndex}.
     */
    @Override
    public synchronized WebHistoryItem getItemAtIndex(int index) {
        if (index < 0 || index >= getSize()) {
            return null;
        } else {
            return mHistoryItemList.get(index);
        }
    }

    /**
     * See {@link android.webkit.WebBackForwardList#getSize}.
     */
    @Override
    public synchronized int getSize() {
        return mHistoryItemList.size();
    }

    // Clone constructor.
    private WebBackForwardListChromium2(List<WebHistoryItemChromium2> list, int currentIndex) {
        mHistoryItemList = list;
        mCurrentIndex = currentIndex;
    }

    /**
     * See {@link android.webkit.WebBackForwardList#clone}.
     */
    @Override
    protected synchronized WebBackForwardListChromium2 clone() {
        List<WebHistoryItemChromium2> list = new ArrayList<WebHistoryItemChromium2>(getSize());
        for (int i = 0; i < getSize(); ++i) {
            list.add(mHistoryItemList.get(i).clone());
        }
        return new WebBackForwardListChromium2(list, mCurrentIndex);
    }
}
