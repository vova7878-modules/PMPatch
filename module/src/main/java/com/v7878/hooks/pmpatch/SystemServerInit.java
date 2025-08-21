package com.v7878.hooks.pmpatch;

import static android.os.Build.VERSION.SDK_INT;
import static com.v7878.hooks.pmpatch.Main.TAG;
import static com.v7878.unsafe.Reflection.getHiddenMethod;

import android.util.Log;

import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.unsafe.ClassUtils;
import com.v7878.unsafe.invoke.EmulatedStackFrame;
import com.v7878.unsafe.invoke.Transformers;
import com.v7878.vmtools.Hooks;
import com.v7878.vmtools.Hooks.EntryPointType;

public class SystemServerInit {
    private static final String SYSTEM_SERVER = "com.android.server.SystemServer";
    private static final String RUNTIME_INIT = "com.android.internal.os.RuntimeInit";

    private static void runForSystemServer(EmulatedStackFrame frame) {
        var accessor = frame.accessor();
        if (SYSTEM_SERVER.equals(accessor.getReference(0))) {
            ClassLoader loader = accessor.getReference(2);
            EntryPoint.mainSystemServer(loader);
        }
    }

    @DoNotShrink
    public static void init() {
        Class<?> init_class = ClassUtils.sysClass(RUNTIME_INIT);

        String method_name = SDK_INT == 26 ? "invokeStaticMain" : "findStaticMain";
        var method = getHiddenMethod(init_class, method_name,
                String.class, String[].class, ClassLoader.class);

        Hooks.hook(method, EntryPointType.CURRENT, (original, frame) -> {
            try {
                runForSystemServer(frame);
            } catch (Throwable th) {
                Log.e(TAG, "Exception", th);
            }
            Transformers.invokeExact(original, frame);
        }, EntryPointType.DIRECT);
    }
}
