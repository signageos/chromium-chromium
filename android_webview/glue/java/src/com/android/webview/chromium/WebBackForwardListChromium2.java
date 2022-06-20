// Copyright 2014 The Chromium Authors. All rights reserved.
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
    private final List<WebHistoryItemChromium2> mHistroryItemList;
    private final int mCurrentIndex;

    /* package */ WebBackForwardListChromium2(NavigationHistory navHistory) {
        mCurrentIndex = navHistory.getCurrentEntryIndex();
        mHistroryItemList = new ArrayList<WebHistoryItemChromium2>(navHistory.getEntryCount());
        for (int i = 0; i < navHistory.getEntryCount(); ++i) {
            mHistroryItemList.add(new WebHistoryItemChromium2(navHistory.getEntryAtIndex(i)));
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
            return mHistroryItemList.get(index);
        }
    }

    /**
     * See {@link android.webkit.WebBackForwardList#getSize}.
     */
    @Override
    public synchronized int getSize() {
        return mHistroryItemList.size();
    }

    // Clone constructor.
    private WebBackForwardListChromium2(List<WebHistoryItemChromium2> list, int currentIndex) {
        mHistroryItemList = list;
        mCurrentIndex = currentIndex;
    }

    /**
     * See {@link android.webkit.WebBackForwardList#clone}.
     */
    @Override
    protected synchronized WebBackForwardListChromium2 clone() {
        List<WebHistoryItemChromium2> list = new ArrayList<WebHistoryItemChromium2>(getSize());
        for (int i = 0; i < getSize(); ++i) {
            list.add(mHistroryItemList.get(i).clone());
        }
        return new WebBackForwardListChromium2(list, mCurrentIndex);
    }
}
