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
import android.os.Build;
/**
 * Constants and helper functions for use with {@link OsCompat}.
 */
public final class OsConstantsCompat {
    private OsConstantsCompat() {
    }
    public static final int EPERM;
    public static final int ESRCH;
    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            EPERM = android.system.OsConstants.EPERM;
            ESRCH = android.system.OsConstants.ESRCH;
        } else {
            EPERM = libcore.io.OsConstants.EPERM;
            ESRCH = libcore.io.OsConstants.ESRCH;
        }
    }
    /**
     * Returns the string name of an errno value.
     * For example, "EACCES". See {@link Os#strerror} for human-readable errno descriptions.
     */
    public static String errnoName(int errno) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return android.system.OsConstants.errnoName(errno);
        } else {
            return libcore.io.OsConstants.errnoName(errno);
        }
    }
}
