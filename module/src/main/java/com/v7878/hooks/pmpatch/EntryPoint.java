package com.v7878.hooks.pmpatch;

import static com.v7878.hooks.pmpatch.Main.TAG;

import android.util.Log;

public class EntryPoint {
    public static void printLoader(ClassLoader loader) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "loader: " + loader);
        }
    }

    public static void premain() {
        // nop
    }

    public static void mainCommon() {
        var loader = ClassLoader.getSystemClassLoader();
        printLoader(loader);

        var hooks = new BulkHooker();
        HookList.initCommon(hooks);
        hooks.apply(loader);
    }

    public static void mainSystemServer(ClassLoader loader) {
        printLoader(loader);

        var hooks = new BulkHooker();
        HookList.initSystem(hooks);
        hooks.apply(loader);
    }

    public static void mainApplication(String package_name, ClassLoader loader) {
        printLoader(loader);

        var hooks = new BulkHooker();
        HookList.initApplication(package_name, hooks);
        hooks.apply(loader);
    }
}
