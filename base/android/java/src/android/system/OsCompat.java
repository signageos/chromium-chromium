/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.system;
import android.annotation.TargetApi;
import android.os.Build;
import android.system.Os;
import libcore.io.Libcore;
/**
 * Access to low-level system functionality. Most of these are system calls. Most users will want
 * to use higher-level APIs where available, but this class provides access to the underlying
 * primitives used to implement the higher-level APIs.
 *
 * <p>The corresponding constants can be found in {@link OsConstantsCompat}.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public final class OsCompat {
  private OsCompat() {}
  /**
   * See <a href="http://man7.org/linux/man-pages/man2/chmod.2.html">chmod(2)</a>.
   */
  public static void chmod(String path, int mode) throws ErrnoException {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      Os.chmod(path, mode);
      return;
    }
    try {
      Libcore.os.chmod(path, mode);
    } catch (libcore.io.ErrnoException e) {
      throw new ErrnoException("chmod", e.errno);
    }
  }
  /**
   * See <a href="http://man7.org/linux/man-pages/man2/kill.2.html">kill(2)</a>.
   */
  public static void kill(int pid, int signal) throws ErrnoException {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      Os.kill(pid, signal);
      return;
    }
    try {
      Libcore.os.kill(pid, signal);
    } catch (libcore.io.ErrnoException e) {
      throw new ErrnoException("kill", e.errno);
    }
  }
  /**
   * See <a href="http://man7.org/linux/man-pages/man3/setenv.3.html">setenv(3)</a>.
   */
  public static void setenv(String name, String value, boolean overwrite) throws ErrnoException {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      Os.setenv(name, value, overwrite);
      return;
    }
    try {
      Libcore.os.setenv(name, value, overwrite);
    } catch (libcore.io.ErrnoException e) {
      throw new ErrnoException("setenv", e.errno);
    }
  }
  /**
   * See <a href="http://man7.org/linux/man-pages/man2/symlink.2.html">symlink(2)</a>.
   */
  public static void symlink(String oldPath, String newPath) throws ErrnoException {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      Os.symlink(oldPath, newPath);
      return;
    }
    try {
      Libcore.os.symlink(oldPath, newPath);
    } catch (libcore.io.ErrnoException e) {
      throw new ErrnoException("symlink", e.errno);
    }
  }
}
