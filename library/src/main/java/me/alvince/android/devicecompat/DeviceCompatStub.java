/*
 * Copyright (c) 2018 alvince
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.alvince.android.devicecompat;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;

/**
 * Created by alvince on 2018/7/27
 *
 * @author alvince.zy@gmail.com
 */
class DeviceCompatStub implements DeviceHelper.DeviceCompat {

    private static final String TAG = "DeviceCompatStub";

    boolean hasScreenNotch = false;
    float screenNotchHeight = 0;

    DeviceCompatStub() {
        L.d(TAG, "Device Compat IMPL => " + getClass().getSimpleName());
    }

    @Override
    public boolean makeSystemUIReverse(@NonNull Activity activity, boolean dark) {
        return requireSysUiReverse()
                && DisplayHelper.reverseSystemUIStandard(activity, dark);
    }

    @Override
    public boolean hasNotchInScreen(@NonNull Context context) {
        return false;
    }

    @Override
    public float getScreenNotchHeight(@NonNull Context context) {
        return 0;
    }

    protected boolean requireSysUiReverse() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }
}
