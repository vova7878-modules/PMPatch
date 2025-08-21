package com.v7878.hooks.pmpatch;

import static com.v7878.unsafe.Reflection.getHiddenMethod;
import static com.v7878.unsafe.access.AccessLinker.ExecutableAccessKind.VIRTUAL;
import static com.v7878.unsafe.access.AccessLinker.FieldAccess;
import static com.v7878.unsafe.access.AccessLinker.FieldAccessKind.INSTANCE_GETTER;

import android.util.Log;

import com.v7878.r8.annotations.DoNotOptimize;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.r8.annotations.DoNotShrinkType;
import com.v7878.unsafe.ClassUtils;
import com.v7878.unsafe.access.AccessLinker;
import com.v7878.unsafe.access.AccessLinker.ExecutableAccess;
import com.v7878.unsafe.invoke.EmulatedStackFrame;
import com.v7878.unsafe.invoke.Transformers;
import com.v7878.vmtools.Hooks;
import com.v7878.vmtools.Hooks.EntryPointType;

import java.lang.reflect.Method;
import java.util.List;

public class ApplicationInit {
    private static final String LOADED_APK = "android.app.LoadedApk";

    @DoNotShrinkType
    @DoNotOptimize
    private abstract static class AccessI {
        @FieldAccess(kind = INSTANCE_GETTER, klass = LOADED_APK, name = "mClassLoader")
        abstract ClassLoader mClassLoader(Object instance);

        @ExecutableAccess(kind = VIRTUAL, klass = LOADED_APK, name = "getPackageName", args = {})
        abstract String getPackageName(Object instance);

        static final AccessI INSTANCE = AccessLinker.generateImpl(AccessI.class);
    }

    private static void runForApplication(EmulatedStackFrame frame) {
        var thiz = frame.accessor().getReference(0);

        String package_name = AccessI.INSTANCE.getPackageName(thiz);
        ClassLoader loader = AccessI.INSTANCE.mClassLoader(thiz);

        EntryPoint.mainApplication(package_name, loader);
    }

    @DoNotShrink
    public static void init() {
        Class<?> apk_class = ClassUtils.sysClass(LOADED_APK);
        Method target = getHiddenMethod(apk_class,
                "createOrUpdateClassLoaderLocked", List.class);

        Hooks.hook(target, EntryPointType.CURRENT, (original, frame) -> {
            Transformers.invokeExact(original, frame);
            try {
                runForApplication(frame);
            } catch (Throwable th) {
                Log.e(Main.TAG, "Exception", th);
            }
        }, EntryPointType.DIRECT);
    }
}
