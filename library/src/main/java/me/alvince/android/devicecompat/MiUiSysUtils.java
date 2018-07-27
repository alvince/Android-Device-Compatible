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
import android.view.Window;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by alvince on 2017/5/16.
 *
 * @author alvince.zy@gmail.com
 */
class MiUiSysUtils {

    private static final String MIUI_INCREMENTAL_DARK_SYSUI_DEPRECATED = "7.7.13";

    private static final String KEY_BUILD_VERSION_INCREMENTAL = "ro.build.version.incremental";
    private static final String KEY_MIUI_INTERNAL_STORAGE = "ro.miui.internal.storage";
    private static final String KEY_MIUI_VERSION_CODE = "ro.miui.ui.version.code";
    private static final String KEY_MIUI_VERSION_NAME = "ro.miui.ui.version.name";

    private DeviceHelper.BuildProperties props;

    public MiUiSysUtils(DeviceHelper.BuildProperties props) {
        this.props = props;
    }

    /**
     * MIUI 状态栏黑色字符.
     *
     * @see <a href="https://dev.mi.com/doc/p=10416/index.html">https://dev.mi.com/doc/p=10416/index.html</a>
     */
    public static boolean setStatusBarDarkMode(boolean darkMode, Activity activity) {
        Class<? extends Window> clazz = activity.getWindow().getClass();
        try {
            Class<?> layoutParams = Class.forName("android.view.MiuiWindowManager$LayoutParams");
            Field field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE");
            int darkModeFlag = field.getInt(layoutParams);
            Method extraFlagField = clazz.getMethod("setExtraFlags", int.class, int.class);
            extraFlagField.invoke(activity.getWindow(), darkMode ? darkModeFlag : 0, darkModeFlag);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * @return MIUI Version. e.g, V8
     */
    public int getMIUIVersion() {
        String version = props.getProperty(KEY_MIUI_VERSION_NAME);
        return Integer.parseInt(version.substring(1));
    }

    public String getMIUIVerCode() {
        return props.getProperty(KEY_MIUI_VERSION_CODE);
    }

    public String getMIUIVerIncremental() {
        return props.getProperty(KEY_BUILD_VERSION_INCREMENTAL);
    }

    public boolean isSupportSysUiModeStandard() {
        if (getMIUIVersion() >= 9) {
            return true;
        }
        MIUIIncrementalVersion v = new MIUIIncrementalVersion(getMIUIVerIncremental());
        return v.compare(MIUI_INCREMENTAL_DARK_SYSUI_DEPRECATED) >= 0;
    }


    /*
     * MIUI version
     * e.g, x.x.x
     */
    private static class MIUIIncrementalVersion {
        int fst, sec, thd;

        MIUIIncrementalVersion(String version) {
            if (version.matches("\\d+.\\d+.\\d+")) {
                String[] incremental = version.split("\\.");
                int[] code = new int[incremental.length];
                for (int i = 0; i < incremental.length; i++) {
                    code[i] = Integer.parseInt(incremental[i]);
                }
                fst = code[0];
                sec = code[1];
                thd = code[2];
            }
        }

        int compare(String incremental) {
            return compare(new MIUIIncrementalVersion(incremental));
        }

        int compare(MIUIIncrementalVersion v) {
            if (fst == v.fst) {
                if (sec == v.sec) {
                    if (thd == v.thd) {
                        return 0;
                    } else {
                        return thd > v.thd ? 1 : -1;
                    }
                } else {
                    return sec > v.sec ? 1 : -1;
                }
            } else {
                return fst > v.fst ? 1 : -1;
            }
        }
    }
}
