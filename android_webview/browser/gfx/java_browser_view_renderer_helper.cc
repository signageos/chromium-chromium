// Copyright (c) 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "android_webview/browser/gfx/java_browser_view_renderer_helper.h"

#include <android/bitmap.h>
#include <memory>

#include "android_webview/browser_jni_headers/JavaBrowserViewRendererHelper_jni.h"
#include "android_webview/public/browser/draw_sw.h"
#include "android_webview/public/browser/draw_sw_kitkat.h"
#include "base/android/build_info.h"
#include "base/android/scoped_java_ref.h"
#include "base/trace_event/trace_event.h"
#include "third_party/skia/include/core/SkBitmap.h"
#include "third_party/skia/include/core/SkRefCnt.h"
#include "third_party/skia/include/utils/SkCanvasStateUtils.h"

using base::android::ScopedJavaLocalRef;

namespace android_webview {

namespace {

// START Added Kitkat support
enum RasterConfigs {
  kUnknown_RasterConfig   = 0,
  kRGB_565_RasterConfig   = 1,
  kARGB_8888_RasterConfig = 2
};
typedef int32_t RasterConfig;

enum CanvasBackends {
    kUnknown_CanvasBackend = 0,
    kRaster_CanvasBackend  = 1,
    kGPU_CanvasBackend     = 2,
    kPDF_CanvasBackend     = 3
};
typedef int32_t CanvasBackend;

struct ClipRect {
    int32_t left, top, right, bottom;
};

struct SkMCState {
    float matrix[9];
    // NOTE: this only works for non-antialiased clips
    int32_t clipRectCount;
    ClipRect* clipRects;
};

// NOTE: If you add more members, create a new subclass of SkCanvasState with a
// new CanvasState::version.
struct SkCanvasLayerState {
    CanvasBackend type;
    int32_t x, y;
    int32_t width;
    int32_t height;

    SkMCState mcState;

    union {
        struct {
            RasterConfig config; // pixel format: a value from RasterConfigs.
            uint64_t rowBytes;   // Number of bytes from start of one line to next.
            void* pixels;        // The pixels, all (height * rowBytes) of them.
        } raster;
        struct {
            int32_t textureID;
        } gpu;
    };
};

static void setup_canvas_from_MC_state(const SkMCState& state, SkCanvas* canvas) {
    // reconstruct the matrix
    SkMatrix matrix;
    for (int i = 0; i < 9; i++) {
        matrix.set(i, state.matrix[i]);
    }

    // only realy support 1 rect, so if the caller (legacy?) sent us more, we just take the bounds
    // of what they sent.
    SkIRect bounds = SkIRect::MakeEmpty();
    if (state.clipRectCount > 0) {
        bounds.setLTRB(state.clipRects[0].left,
                       state.clipRects[0].top,
                       state.clipRects[0].right,
                       state.clipRects[0].bottom);
        for (int i = 1; i < state.clipRectCount; ++i) {
            bounds.join({state.clipRects[i].left,
                         state.clipRects[i].top,
                         state.clipRects[i].right,
                         state.clipRects[i].bottom});
        }
    }

    canvas->clipRect(SkRect::Make(bounds));
    canvas->concat(matrix);
}

static std::unique_ptr<SkCanvas>
make_canvas_from_canvas_layer(const SkCanvasLayerState& layerState) {
    SkASSERT(kRaster_CanvasBackend == layerState.type);

    SkBitmap bitmap;
    SkColorType colorType =
        layerState.raster.config == kARGB_8888_RasterConfig ? kN32_SkColorType :
        layerState.raster.config == kRGB_565_RasterConfig ? kRGB_565_SkColorType :
        kUnknown_SkColorType;

    if (colorType == kUnknown_SkColorType) {
        return nullptr;
    }

    bitmap.installPixels(SkImageInfo::Make(layerState.width, layerState.height,
                                           colorType, kPremul_SkAlphaType),
                         layerState.raster.pixels, (size_t) layerState.raster.rowBytes);

    SkASSERT(!bitmap.empty());
    SkASSERT(!bitmap.isNull());

    std::unique_ptr<SkCanvas> canvas(new SkCanvas(bitmap));

    // setup the matrix and clip
    setup_canvas_from_MC_state(layerState.mcState, canvas.get());

    return canvas;
}
// END Added Kitkat support

// Provides software rendering functions from the Android glue layer.
// Allows preventing extra copies of data when rendering.
AwDrawSWFunctionTable* g_sw_draw_functions = NULL;
AwDrawSWFunctionTableKitkat* g_sw_draw_functions_kitkat = NULL;

class JavaCanvasHolder : public SoftwareCanvasHolder {
 public:
  JavaCanvasHolder(JNIEnv* env,
                   jobject java_canvas,
                   const gfx::Vector2d& scroll_correction);
  ~JavaCanvasHolder() override;

  SkCanvas* GetCanvas() override;

 private:
  void* pixels_;
  std::unique_ptr<SkCanvas> canvas_;
  DISALLOW_COPY_AND_ASSIGN(JavaCanvasHolder);
};

JavaCanvasHolder::JavaCanvasHolder(JNIEnv* env,
                                   jobject java_canvas,
                                   const gfx::Vector2d& scroll)
    : pixels_(nullptr) {
  if (g_sw_draw_functions) {
    AwPixelInfo* pixels = g_sw_draw_functions->access_pixels(env, java_canvas);
    if (!pixels || !pixels->state)
      return;

    canvas_ = SkCanvasStateUtils::MakeFromCanvasState(pixels->state);
    pixels_ = reinterpret_cast<void*>(pixels);
  } else if (g_sw_draw_functions_kitkat) {
    AwPixelInfoKitkat* pixels = g_sw_draw_functions_kitkat->access_pixels(env, java_canvas);
    if (!pixels || !pixels->pixels)
      return;

    RasterConfig config = reinterpret_cast<RasterConfig>((pixels->config % 2) - 1);
    SkCanvasLayerState layer = {
      .type = kRaster_CanvasBackend,
      .x = 0,
      .y = 0,
      .width = pixels->width,
      .height = pixels->height,
      .mcState = {
        .matrix = {
          pixels->matrix[0], pixels->matrix[1], pixels->matrix[2],
          pixels->matrix[3], pixels->matrix[4], pixels->matrix[5],
          pixels->matrix[6], pixels->matrix[7], pixels->matrix[8]
        },
        .clipRectCount = pixels->clip_rect_count,
        .clipRects = reinterpret_cast<ClipRect*>(pixels->clip_rects),
      },
      .raster = {
        config,
        pixels->row_bytes,
        pixels->pixels,
      },
    };
    canvas_ = make_canvas_from_canvas_layer(layer);
    pixels_ = reinterpret_cast<void*>(pixels);
  }
  // Workarounds for http://crbug.com/271096: SW draw only supports
  // translate & scale transforms, and a simple rectangular clip.
  if (canvas_ && (!canvas_->isClipRect() ||
                  (canvas_->getTotalMatrix().getType() &
                   ~(SkMatrix::kTranslate_Mask | SkMatrix::kScale_Mask)))) {
    canvas_.reset();
  }
  if (canvas_) {
    canvas_->translate(scroll.x(), scroll.y());
  }
}

JavaCanvasHolder::~JavaCanvasHolder() {
  if (pixels_) {
    if (g_sw_draw_functions) {
      AwPixelInfo* pixels = reinterpret_cast<AwPixelInfo*>(pixels_);
      g_sw_draw_functions->release_pixels(pixels);
    } else if (g_sw_draw_functions_kitkat) {
      AwPixelInfoKitkat* pixels = reinterpret_cast<AwPixelInfoKitkat*>(pixels_);
      g_sw_draw_functions_kitkat->release_pixels(pixels);
    }
  }
  pixels_ = nullptr;
}

SkCanvas* JavaCanvasHolder::GetCanvas() {
  return canvas_.get();
}

class AuxiliaryCanvasHolder : public SoftwareCanvasHolder {
 public:
  AuxiliaryCanvasHolder(JNIEnv* env,
                        jobject java_canvas,
                        const gfx::Vector2d& scroll_correction,
                        const gfx::Size size);
  ~AuxiliaryCanvasHolder() override;

  SkCanvas* GetCanvas() override;

 private:
  ScopedJavaLocalRef<jobject> jcanvas_;
  ScopedJavaLocalRef<jobject> jbitmap_;
  gfx::Vector2d scroll_;
  std::unique_ptr<SkBitmap> bitmap_;
  std::unique_ptr<SkCanvas> canvas_;
  DISALLOW_COPY_AND_ASSIGN(AuxiliaryCanvasHolder);
};

AuxiliaryCanvasHolder::AuxiliaryCanvasHolder(
    JNIEnv* env,
    jobject java_canvas,
    const gfx::Vector2d& scroll_correction,
    const gfx::Size size)
    : jcanvas_(env, java_canvas), scroll_(scroll_correction) {
  DCHECK(size.width() > 0);
  DCHECK(size.height() > 0);
  jbitmap_ = Java_JavaBrowserViewRendererHelper_createBitmap(
      env, size.width(), size.height(), jcanvas_);
  if (!jbitmap_.obj())
    return;

  AndroidBitmapInfo bitmap_info;
  if (AndroidBitmap_getInfo(env, jbitmap_.obj(), &bitmap_info) < 0) {
    LOG(ERROR) << "Error getting java bitmap info.";
    return;
  }

  void* pixels = nullptr;
  if (AndroidBitmap_lockPixels(env, jbitmap_.obj(), &pixels) < 0) {
    LOG(ERROR) << "Error locking java bitmap pixels.";
    return;
  }

  SkImageInfo info =
      SkImageInfo::MakeN32Premul(bitmap_info.width, bitmap_info.height);
  bitmap_.reset(new SkBitmap);
  bitmap_->installPixels(info, pixels, bitmap_info.stride);
  canvas_ = std::make_unique<SkCanvas>(*bitmap_);
}

AuxiliaryCanvasHolder::~AuxiliaryCanvasHolder() {
  bitmap_.reset();

  JNIEnv* env = base::android::AttachCurrentThread();
  if (AndroidBitmap_unlockPixels(env, jbitmap_.obj()) < 0) {
    LOG(ERROR) << "Error unlocking java bitmap pixels.";
    return;
  }

  Java_JavaBrowserViewRendererHelper_drawBitmapIntoCanvas(
      env, jbitmap_, jcanvas_, scroll_.x(), scroll_.y());
}

SkCanvas* AuxiliaryCanvasHolder::GetCanvas() {
  return canvas_.get();
}

}  // namespace

void RasterHelperSetAwDrawSWFunctionTable(void* table) {
  if (base::android::BuildInfo::GetInstance()->sdk_int() >= base::android::SDK_VERSION_LOLLIPOP) {
    g_sw_draw_functions = reinterpret_cast<AwDrawSWFunctionTable*>(table);
  } else {
    g_sw_draw_functions_kitkat = reinterpret_cast<AwDrawSWFunctionTableKitkat*>(table);
  }
}

// static
std::unique_ptr<SoftwareCanvasHolder> SoftwareCanvasHolder::Create(
    jobject java_canvas,
    const gfx::Vector2d& scroll_correction,
    const gfx::Size& auxiliary_bitmap_size,
    bool force_auxiliary_bitmap) {
  JNIEnv* env = base::android::AttachCurrentThread();
  std::unique_ptr<SoftwareCanvasHolder> holder;
  if (!force_auxiliary_bitmap) {
    holder.reset(new JavaCanvasHolder(env, java_canvas, scroll_correction));
  }
  if (!holder.get() || !holder->GetCanvas()) {
    holder.reset();
    holder.reset(new AuxiliaryCanvasHolder(env, java_canvas, scroll_correction,
                                           auxiliary_bitmap_size));
  }
  if (!holder->GetCanvas()) {
    holder.reset();
  }
  return holder;
}

}  // namespace android_webview
