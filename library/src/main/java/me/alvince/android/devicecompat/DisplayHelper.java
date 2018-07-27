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
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

/**
 * Screen display utils wrapper
 * <p>
 * Created by alvince on 2018/7/27
 *
 * @author alvince.zy@gmail.com
 */
public class DisplayHelper {

    private static final String IDENTIFIER_STATUS_BAR_SIZE = "status_bar_height";
    private static final String IDENTIFIER_NAV_BAR_HEIGHT = "navigation_bar_height";
    private static final String IDENTIFIER_NAV_BAR_HEIGHT_LANDSCAPE = "navigation_bar_height_landscape";

    /**
     * 获取系统状态栏高度
     */
    public static int getStatusBarSize(@NonNull Context context) {
        return getInternalDimensionSize(context.getResources(), IDENTIFIER_STATUS_BAR_SIZE);
    }

    /**
     * 获取手机导航栏（虚拟按键）高度
     */
    public static int getNavigationBarSize(@NonNull Context context) {
        int size = 0;
        if (DeviceHelper.hasNavigationBar(context)) {
            String key;
            Resources res = context.getResources();
            boolean curPortrait = res.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
            if (curPortrait) {
                key = IDENTIFIER_NAV_BAR_HEIGHT;
            } else {
                key = IDENTIFIER_NAV_BAR_HEIGHT_LANDSCAPE;
            }
            size = getInternalDimensionSize(res, key);
        }
        return size;
    }

    /**
     * 检查是否需要在凹口屏下优化视图位置
     */
    public static boolean isNotchSinkingEnable(Context context) {
        if (context == null) {
            return false;
        }
        if (context instanceof Activity) {
            boolean isNotchScreen = DeviceHelper.hasNotchInScreen(context);
            return isNotchScreen && isInFullscreen((Activity) context);
        }
        return false;
    }

    /**
     * 检查指定Activity是否运行在全屏模式
     */
    public static boolean isInFullscreen(Activity activity) {
        if (activity == null) {
            return false;
        }

        boolean uiFullscreen = false;
        View decorView = activity.getWindow().getDecorView();
        if (decorView != null) {
            int uiVisibility = decorView.getSystemUiVisibility();
            boolean visibilityFullscreen = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                    && (uiVisibility & View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN) == View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            if (visibilityFullscreen) {
                int[] loc = new int[2];
                decorView.getLocationOnScreen(loc);
                boolean layoutFullscreen = loc[0] == 0 && loc[1] == 0;
                boolean layoutNoPadding = decorView.getPaddingTop() == 0
                        && decorView.getPaddingBottom() == 0
                        && decorView.getPaddingLeft() == 0
                        && decorView.getPaddingRight() == 0;
                uiFullscreen = layoutFullscreen && layoutNoPadding;
            }
        }

        return uiFullscreen;
    }

    /**
     * Convert dimens from dip to px.
     */
    public static float fromDip(@Nullable Context context, float dip) {
        Resources r = context != null ? context.getResources() : Resources.getSystem();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, r.getDisplayMetrics());
    }

    /**
     * Convert dimens from sp to px.
     */
    public static float fromSp(Context context, float spValue) {
        Resources r = context != null ? context.getResources() : Resources.getSystem();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, spValue, r.getDisplayMetrics());
    }

    /**
     * Obtain screen physical size in pixel.
     */
    public static Point obtainScreenSize(@Nullable Context context) {
        Resources res = context != null ? context.getResources() : Resources.getSystem();
        DisplayMetrics displayMetrics = res.getDisplayMetrics();
        int screenWidthDp = res.getConfiguration().screenWidthDp;
        int screenHeightDp = res.getConfiguration().screenHeightDp;

        return new Point((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, screenWidthDp, displayMetrics),
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, screenHeightDp, displayMetrics));
    }

    /**
     * 改变屏幕自动亮度设置
     */
    public static void changeAutoBrightness(@NonNull Context context, boolean enableAutomatic) {
        Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, enableAutomatic
                ? Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC : Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
    }

    /**
     * 查看当前环境是否设置自动亮度
     */
    public static boolean isAutoBrightness(@NonNull Context context) {
        try {
            return Settings.System.getInt(context.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 获取系统设置屏幕亮度(0-255)
     */
    public static int getScreenBrightness(@NonNull Context context) {
        int brightnessDef = 255 / 2;
        ContentResolver resolver = context.getContentResolver();
        return Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS, brightnessDef);
    }

    /**
     * 获取当前窗口的亮度
     *
     * @return 0-255
     */
    public static int getScreenBrightness(Activity activity) {
        if (activity != null && activity.getWindow() != null) {
            Window window = activity.getWindow();
            WindowManager.LayoutParams winAttrs = window.getAttributes();
            float winBrightness = winAttrs.screenBrightness;
            return winBrightness >= 0 ? (int) (winBrightness * 255F) : -1;
        }
        return -1;
    }

    /**
     * 更改当前 {@link Activity} 窗口亮度
     *
     * @param activity   变更屏幕亮的的活动
     * @param brightness 亮度 0~255
     */
    public static void updateBrightness(Activity activity, int brightness) {
        Window window = activity != null ? activity.getWindow() : null;
        if (window != null) {
            WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
            lp.screenBrightness = brightness > 0
                    ? brightness / 255F : WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
            activity.getWindow().setAttributes(lp);
        }
    }

    /**
     * 隐藏系统 System Ui (状态栏|导航栏)
     *
     * @param view       目标 {@link View}
     * @param fullscreen 全屏标志为 {@code true} 隐藏状态栏
     */
    public static void hideSystemUi(View view, boolean fullscreen) {
        if (view == null) {
            return;
        }
        int uiVisibility = view.getSystemUiVisibility();
        uiVisibility |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            uiVisibility |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                uiVisibility |= View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            }

            if (fullscreen) {
                uiVisibility |= View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            }
        }
        view.setSystemUiVisibility(uiVisibility);
    }

    /**
     * 调整系统状态栏色调
     *
     * @param dark light mode
     * @return if system ui stay light mode
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    public static boolean reverseSystemUIStandard(@NonNull Activity activity, boolean dark) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Window window = activity.getWindow();
            View decorView = window.getDecorView();
            if (dark) {
                decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            } else {
                int systemUiVisibility = decorView.getSystemUiVisibility();
                if ((systemUiVisibility & View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR) == View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR) {
                    decorView.setSystemUiVisibility(systemUiVisibility ^ View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                }
            }
            return dark;
        }
        return false;
    }

    private static int getInternalDimensionSize(Resources res, String key) {
        int resourceId = res.getIdentifier(key, "dimen", "android");
        return resourceId > 0 ? res.getDimensionPixelSize(resourceId) : 0;
    }
}
