package com.v7878.hooks.pmpatch;

import static com.v7878.hooks.pmpatch.Main.TAG;
import static com.v7878.unsafe.invoke.EmulatedStackFrame.RETURN_VALUE_IDX;

import android.util.Log;

import com.v7878.unsafe.invoke.EmulatedStackFrame;
import com.v7878.unsafe.invoke.Transformers;
import com.v7878.vmtools.HookTransformer;

public class HTF {
    public static class StackException extends Exception {
    }

    public static void printStackTrace(EmulatedStackFrame frame) {
        if (BuildConfig.DEBUG) {
            Log.e(TAG, frame.toString(), new StackException());
        }
    }

    public static final HookTransformer NOP = (original, frame) -> {
        printStackTrace(frame);
    };

    public static final HookTransformer FALSE = NOP; // default value

    public static final HookTransformer TRUE = (original, frame) -> {
        printStackTrace(frame);

        var ret = frame.type().returnType();
        if (ret == boolean.class) {
            frame.accessor().setBoolean(RETURN_VALUE_IDX, true);
        } else if (ret == void.class) {
            // nop
        } else {
            Log.e(TAG, "Unexpected return type: " + ret, new StackException());
            // run original
            Transformers.invokeExactWithFrame(original, frame);
        }
    };

    public static HookTransformer constant(Object value) {
        return (original, frame) -> {
            printStackTrace(frame);

            if (frame.type().returnType() != void.class) {
                frame.accessor().setValue(RETURN_VALUE_IDX, value);
            }
        };
    }

    private static boolean contains(String[] array, String value) {
        for (String tmp : array) {
            if (value.equals(tmp)) {
                return true;
            }
        }
        return false;
    }

    public static HookTransformer constant(Object value, String[] run, String[] exclude) {
        return (original, frame) -> {
            printStackTrace(frame);

            boolean run_flag = run == null;
            boolean exclude_flag = false;
            var trace = Thread.currentThread().getStackTrace();

            for (var element : trace) {
                String name = element.getMethodName();

                if (!run_flag && contains(run, name)) {
                    run_flag = true;
                }
                if (exclude != null && contains(exclude, name)) {
                    exclude_flag = true;
                    break;
                }
            }

            if (!run_flag || exclude_flag) {
                Transformers.invokeExactWithFrame(original, frame);
            } else {
                if (frame.type().returnType() != void.class) {
                    frame.accessor().setValue(RETURN_VALUE_IDX, value);
                }
            }
        };
    }
}
