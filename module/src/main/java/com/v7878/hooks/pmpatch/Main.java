package com.v7878.hooks.pmpatch;

import android.util.Log;

import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotObfuscateType;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.r8.annotations.DoNotShrinkType;
import com.v7878.zygisk.ZygoteLoader;

@DoNotShrinkType
@DoNotObfuscateType
public class Main {
    public static String TAG = "PM_PATCH";

    @SuppressWarnings({"unused", "ConfusingMainMethod"})
    @DoNotShrink
    @DoNotObfuscate
    public static void main() throws Throwable {
        Log.w(TAG, "Injected into " + ZygoteLoader.getPackageName());
        try {
            MethodAndArgsCallerHook.init();
        } catch (Throwable th) {
            Log.e(TAG, "Exception", th);
        }
        Log.w(TAG, "Done");
    }
}
