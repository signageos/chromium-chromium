/*
 * Copyright 2013 Google Inc.
 *
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

#ifndef SkCanvasStateUtilsKitkat_DEFINED
#define SkCanvasStateUtilsKitkat_DEFINED

#include "third_party/skia/include/utils/SkCanvasStateUtils.h"

class SkCanvasState;

/**
 * A set of functions that are useful for copying the state of an SkCanvas
 * across a library boundary where the Skia library on the other side of the
 * boundary may be newer. The expected usage is outline below...
 *
 *                          Lib Boundary
 * CaptureCanvasState(...)      |||
 *   SkCanvas --> SkCanvasState |||
 *                              ||| CreateFromCanvasState(...)
 *                              |||   SkCanvasState --> SkCanvas`
 *                              ||| Draw into SkCanvas`
 *                              ||| Unref SkCanvas`
 * ReleaseCanvasState(...)      |||
 *
 */
class SK_API SkCanvasStateUtilsKitkat {
public:
    /**
     * Create a new SkCanvas from the captured state of another SkCanvas. The
     * function will return NULL in the event that one of the
     * following conditions are true.
     *  1) the captured state is in an unrecognized format
     *  2) the captured canvas device type is not supported
     *
     * @param state Opaque object created by CaptureCanvasState.
     * @return NULL or an SkCanvas* whose devices and matrix/clip state are
     *         identical to the captured canvas. The caller is responsible for
     *         calling unref on the SkCanvas.
     */
    static std::unique_ptr<SkCanvas> MakeFromCanvasState(const SkCanvasState* state);
};

#endif // SkCanvasStateUtilsKitkat_DEFINED
