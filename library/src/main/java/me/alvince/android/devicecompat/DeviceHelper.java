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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Properties;

/**
 * Created by alvince on 2018/7/27
 *
 * @author alvince.zy@gmail.com
 */
public class DeviceHelper {

    interface DeviceCompat {
        /**
         * 是否有屏幕凹陷（刘海屏）
         */
        boolean hasNotchInScreen(@NonNull Context context);

        /**
         * 获取屏幕凹槽高度
         */
        float getScreenNotchHeight(@NonNull Context context);
    }

    static {
        // Android allows a system property to override the presence of the navigation bar.
        // Used by the emulator.
        // See https://github.com/android/platform_frameworks_base/blob/master/policy/src/com/android/internal/policy/impl/PhoneWindowManager.java#L1076
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                @SuppressLint("PrivateApi")
                Class c = Class.forName("android.os.SystemProperties");
                //noinspection unchecked
                Method m = c.getDeclaredMethod("get", String.class);
                m.setAccessible(true);
                sNavBarOverride = (String) m.invoke(null, "qemu.hw.mainkeys");
            } catch (Throwable e) {
                sNavBarOverride = "";
            }
        }
    }

    public static final String ROM_HUAWEI_EMUI = "EMUI";
    public static final String ROM_XIAOMI_MIUI = "MIUI";
    public static final String ROM_MEIZU_FLYME = "FLYME";
    public static final String ROM_ONEPLUS = "ONEPLUS";
    public static final String ROM_OPPO = "OPPO";
    public static final String ROM_VIVO = "VIVO";

    private static final String CONFIG_SHOW_NAV_BAR_RES_NAME = "config_showNavigationBar";

    private static final String PROP_BUILD_OPPO_VERSION = "ro.build.version.opporom";
    private static final String PROP_BUILD_VIVO_VERSION = "ro.vivo.os.version";
    private static final String PROP_HUAWEI_BUILD_EMUI_VERSION = "ro.build.version.emui";
    private static final String PROP_HUAWEI_CONF_SYS_VERSION = "ro.confg.hw_systemversion";
    /**
     * Xiaomi - 值为 1 时则是 Notch 屏手机
     */
    private static final String PROP_XIAOMI_CONF_NOTCH = "ro.miui.notch";

    private static final Object S_LOCK = DeviceHelper.class;
    private static final String TAG = "DeviceHelper";

    private static String sNavBarOverride;

    private BuildProperties buildProps;
    private DeviceCompat IMPL;

    private DeviceHelper() {
        String template = "Device build >>>" +
                "\nisPad: [ %s ]" +
                "\nDevice: %s  ID: %s" +
                "\nBrand: %s\nManufacturer: %s\nModel: %s";
        L.d(TAG, String.format(template,
                isPad(),
                Build.DEVICE, Build.ID,
                Build.BRAND, Build.MANUFACTURER, Build.MODEL));
    }

    /**
     * 判断当前设备是否平板
     * <br/>
     * Refer to &gt; Google I/O App for Android
     */
    public static boolean isPad() {
        Resources res = Resources.getSystem();
        int screenLayoutConf = res.getConfiguration().screenLayout;
        return (screenLayoutConf & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    /**
     * 检查是否有虚拟按键栏
     */
    public static boolean hasNavigationBar(@NonNull Context context) {
        Resources res = context.getResources();
        int resourceId = res.getIdentifier(CONFIG_SHOW_NAV_BAR_RES_NAME, "bool", "android");
        if (resourceId != 0) {
            boolean hasNav = res.getBoolean(resourceId);
            // check override flag (see static block)
            if ("1".equals(sNavBarOverride)) {
                hasNav = false;
            } else if ("0".equals(sNavBarOverride)) {
                hasNav = true;
            }
            return hasNav;
        } else {  // fallback
            return !ViewConfiguration.get(context).hasPermanentMenuKey();
        }
    }

    /**
     * 隐藏虚拟按键栏
     *
     * @param contentView
     */
    public static void hideNavigationBar(final View contentView) {
        if (contentView == null) return;
        contentView.postDelayed(new Runnable() {
            @Override
            public void run() {
                int uiVisibility = contentView.getSystemUiVisibility();
                if ((uiVisibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) {
                    return;
                }
                if (Build.VERSION.SDK_INT >= 19) {
                    uiVisibility |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE;
                } else {
                    uiVisibility |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
                }
                contentView.setSystemUiVisibility(uiVisibility);
            }
        }, 200);

    }

    /**
     * 检查是否刘海屏
     */
    public static boolean hasNotchInScreen(@NonNull Context context) {
        return SingletonHolder.INSTANCE.getCompatImpl().hasNotchInScreen(context);
    }

    /**
     * 获取屏幕缺口（刘海）高度
     */
    public static float getScreenNotchHeight(@NonNull Context context) {
        return SingletonHolder.INSTANCE.getCompatImpl().getScreenNotchHeight(context);
    }

    /**
     * 检查手机 ROM
     */
    public static boolean validateRom(String rom) {
        if (rom == null) {
            return false;
        }
        BuildProperties props;
        switch (rom) {
            case ROM_HUAWEI_EMUI:
                props = getBuildProps();
                return !TextUtils.isEmpty(props.getProperty(PROP_HUAWEI_BUILD_EMUI_VERSION))
                        || !TextUtils.isEmpty(props.getProperty(PROP_HUAWEI_CONF_SYS_VERSION))
                        || TextUtils.equals("HUAWEI", Build.BRAND);
            case ROM_MEIZU_FLYME:
                try {
                    final Method method = Build.class.getMethod("hasSmartBar");
                    return method != null;
                } catch (final Exception e) {
                    return false;
                }
            case ROM_ONEPLUS:
                return "OnePlus".equals(Build.BRAND);
            case ROM_OPPO:
                return !TextUtils.isEmpty(getBuildProps().getProperty(PROP_BUILD_OPPO_VERSION))
                        || "OPPO".equalsIgnoreCase(Build.BRAND);
            case ROM_VIVO:
                return !TextUtils.isEmpty(getBuildProps().getProperty(PROP_BUILD_VIVO_VERSION))
                        || "VIVO".equalsIgnoreCase(Build.BRAND);
            case ROM_XIAOMI_MIUI:
                // see > https://dev.mi.com/docs/appsmarket/technical_docs/system&device_identification
                return TextUtils.equals(Build.MANUFACTURER, "Xiaomi");
            default:
                return false;
        }
    }

    private static BuildProperties getBuildProps() {
        DeviceHelper instance = SingletonHolder.INSTANCE;
        if (instance.buildProps == null) {
            synchronized (S_LOCK) {
                if (instance.buildProps == null) {
                    instance.buildProps = new BuildProperties();
                }
            }
        }
        return instance.buildProps;
    }

    private static DeviceCompat deviceCompatImpl() {
        if (validateRom(ROM_HUAWEI_EMUI)) {
            return new DeviceCompatHuawei();
        }
        if (validateRom(ROM_ONEPLUS)) {
            return new DeviceComaptOnePlus();
        }
        if (validateRom(ROM_OPPO)) {
            return new DeviceCompatOppo();
        }
        if (validateRom(ROM_VIVO)) {
            return new DeviceCompatVivo();
        }
        if (validateRom(ROM_XIAOMI_MIUI)) {
            return new DeviceCompatXiaomi();
        }
        return new DefaultCompat();
    }

    private DeviceCompat getCompatImpl() {
        if (IMPL == null) {
            IMPL = deviceCompatImpl();
        }
        return IMPL;
    }


    static class BuildProperties {

        private Properties props = new Properties();
        private boolean propsLoadFailed;

        BuildProperties() {
            InputStream is = null;
            try {
                is = new FileInputStream(new File(Environment.getRootDirectory(), "build.prop"));
                props.load(is);
            } catch (IOException e) {
                propsLoadFailed = true;
                L.e(TAG, String.format("Load build.prop error\n%s", Arrays.toString(e.getStackTrace())), e);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        String getProperty(String key) {
            String value = props.getProperty(key);
            if (value == null && propsLoadFailed) {
                value = loadFromSystemProp(key);
                if (!TextUtils.isEmpty(value)) {
                    props.setProperty(key, value);
                }
            }
            return value;
        }

        @SuppressLint("PrivateApi")
        private String loadFromSystemProp(String key) {
            String result = null;
            try {
                Class buildClazz = Build.class;
                //noinspection unchecked,JavaReflectionMemberAccess
                Method getString = buildClazz.getMethod("getString", String.class);
                result = (String) getString.invoke(null, key);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        }
    }

    private static final class SingletonHolder {
        static final DeviceHelper INSTANCE = new DeviceHelper();
    }

    static class DefaultCompat implements DeviceCompat {

        boolean hasScreenNotch = false;
        float screehNotchHeight = 0;

        DefaultCompat() {
            L.d(TAG, "Device Compat IMPL => " + getClass().getSimpleName());
        }

        @Override
        public boolean hasNotchInScreen(@NonNull Context context) {
            return false;
        }

        @Override
        public float getScreenNotchHeight(@NonNull Context context) {
            return 0;
        }
    }

    /**
     * 华为手机兼容适配
     * <p>
     * &gt;&nbsp;<a href="http://developer.huawei.com/consumer/cn/devservice/doc/50114">刘海屏手机安卓O版本适配指导</a>
     */
    private static class DeviceCompatHuawei extends DefaultCompat {
        @Override
        public boolean hasNotchInScreen(@NonNull Context context) {
            if (!hasScreenNotch) {
                try {
                    ClassLoader cl = context.getClassLoader();
                    Class HwNotchSizeUtil = cl.loadClass("com.huawei.android.util.HwNotchSizeUtil");
                    //noinspection unchecked
                    Method get = HwNotchSizeUtil.getMethod("hasNotchInScreen");
                    hasScreenNotch = (boolean) get.invoke(HwNotchSizeUtil);
                } catch (ClassNotFoundException e) {
                    L.e(TAG, "hasNotchInScreen ClassNotFoundException", e);
                } catch (NoSuchMethodException e) {
                    L.e(TAG, "hasNotchInScreen NoSuchMethodException", e);
                } catch (Exception e) {
                    L.e(TAG, "hasNotchInScreen Exception", e);
                }
            }
            return hasScreenNotch;
        }

        @Override
        public float getScreenNotchHeight(@NonNull Context context) {
            if (hasNotchInScreen(context) && screehNotchHeight == 0) {
                int[] ret;  // [ width, height ]
                try {
                    ClassLoader cl = context.getClassLoader();
                    Class HwNotchSizeUtil = cl.loadClass("com.huawei.android.util.HwNotchSizeUtil");
                    //noinspection unchecked
                    Method get = HwNotchSizeUtil.getMethod("getNotchSize");
                    ret = (int[]) get.invoke(HwNotchSizeUtil);
                    screehNotchHeight = ret[1];
                } catch (ClassNotFoundException e) {
                    L.e(TAG, "getNotchSize ClassNotFoundException", e);
                } catch (NoSuchMethodException e) {
                    L.e(TAG, "getNotchSize NoSuchMethodException", e);
                } catch (Exception e) {
                    L.e(TAG, "getNotchSize Exception", e);
                }
            }
            return screehNotchHeight;
        }
    }

    /**
     * Oppo 手机兼容适配
     * <p>
     * &gt;&nbsp;<a href="https://open.oppomobile.com/wiki/doc#id=10159">OPPO 凹形屏适配说明</a>
     */
    private static class DeviceCompatOppo extends DefaultCompat {
        @Override
        public boolean hasNotchInScreen(@NonNull Context context) {
            if (!hasScreenNotch) {
                hasScreenNotch = context.getPackageManager()
                        .hasSystemFeature("com.oppo.feature.screen.heteromorphism");
            }
            return hasScreenNotch;
        }

        @Override
        public float getScreenNotchHeight(@NonNull Context context) {
            // see > https://open.oppomobile.com/wiki/doc#id=10159
            return hasNotchInScreen(context) ? 80F : 0;
        }
    }

    /**
     * Vivo 手机兼容适配
     * <p>
     * &gt;&nbsp;<a href="https://dev.vivo.com.cn/doc/document/info?id=103">全面屏应用适配指南</a>
     */
    private static class DeviceCompatVivo extends DefaultCompat {
        @SuppressLint("PrivateApi")
        @Override
        public boolean hasNotchInScreen(@NonNull Context context) {
            if (!hasScreenNotch) {
                try {
                    ClassLoader cl = context.getClassLoader();
                    Class ftFeature = cl.loadClass("android.util.FtFeature");
                    Method[] methods = ftFeature.getDeclaredMethods();
                    if (methods != null) {
                        for (Method method : methods) {
                            if (method.getName().equalsIgnoreCase("isFeatureSupport")) {
                                hasScreenNotch = (boolean) method.invoke(ftFeature, 0x00000020);
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return hasScreenNotch;
        }

        @Override
        public float getScreenNotchHeight(@NonNull Context context) {
            return hasNotchInScreen(context) ? ScreenUtils.fromDip(context, 27) : 0;
        }
    }

    /**
     * OnePlus 手机兼容适配
     */
    private static class DeviceComaptOnePlus extends DefaultCompat {
        @Override
        public boolean hasNotchInScreen(@NonNull Context context) {
            return supportNotch() || super.hasNotchInScreen(context);
        }

        @Override
        public float getScreenNotchHeight(@NonNull Context context) {
            if (supportNotch()) {
                return ScreenUtils.getStatusBarSize(context);
            }
            return super.getScreenNotchHeight(context);
        }

        private boolean supportNotch() {
            return "OnePlus6".equals(Build.DEVICE);
        }
    }

    /**
     * Xiaomi 手机兼容适配
     * <p>
     * &gt;&nbsp;<a href="https://dev.mi.com/console/doc/detail?pId=1293">小米 MIUI Notch 屏 Android O 适配说明</a>
     */
    private static class DeviceCompatXiaomi extends DefaultCompat {
        @Override
        public boolean hasNotchInScreen(@NonNull Context context) {
            if (!hasScreenNotch) {
                hasScreenNotch = "1".equals(DeviceHelper.getBuildProps().getProperty(PROP_XIAOMI_CONF_NOTCH));
            }
            return hasScreenNotch;
        }

        @Override
        public float getScreenNotchHeight(@NonNull Context context) {
            return hasNotchInScreen(context) ? ScreenUtils.getStatusBarSize(context) : 0;
        }
    }
}
