package me.alvince.android.devicecompat;

import android.util.Log;

/**
 * Created by alvince on 2018/7/27
 *
 * @author alvince.zy@gmail.com
 */
public class L {

    static void d(String tag, String msg) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, msg);
        }
    }

    static void e(String tag, String msg, Throwable err) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, msg, err);
        }
    }
}
