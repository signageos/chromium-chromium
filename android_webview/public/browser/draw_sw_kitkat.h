// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#ifndef ANDROID_WEBVIEW_PUBLIC_BROWSER_DRAW_SW_KITKAT_H_
#define ANDROID_WEBVIEW_PUBLIC_BROWSER_DRAW_SW_KITKAT_H_

#include "draw_sw.h"

#ifndef __cplusplus
#error "Can't mix C and C++ when using jni.h"
#endif

// "vtable" for the functions declared in this file. An instance must be set via
// AwContents.setAwDrawSWFunctionTable
struct AwDrawSWFunctionTableKitkat {
  // no version
  AwAccessPixelsFunction* access_pixels;
  AwReleasePixelsFunction* release_pixels;
  AwCreatePictureFunction* create_picture;
  AwIsSkiaVersionCompatibleFunction* is_skia_version_compatible;
};

#endif  // ANDROID_WEBVIEW_PUBLIC_BROWSER_DRAW_SW_KITKAT_H_
