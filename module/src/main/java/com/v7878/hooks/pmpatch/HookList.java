package com.v7878.hooks.pmpatch;

import static android.content.pm.PackageManager.SIGNATURE_MATCH;
import static android.os.Build.VERSION.SDK_INT;
import static com.v7878.unsafe.Reflection.fieldOffset;
import static com.v7878.unsafe.Reflection.getDeclaredField;
import static com.v7878.unsafe.invoke.EmulatedStackFrame.RETURN_VALUE_IDX;

import com.v7878.unsafe.AndroidUnsafe;
import com.v7878.unsafe.invoke.Transformers;
import com.v7878.vmtools.HookTransformer;
import com.v7878.zygisk.ZygoteLoader;

import java.security.Signature;

public class HookList {
    private static boolean booleanProperty(String name) {
        if (BuildConfig.USE_CONFIG) {
            return Boolean.parseBoolean(ZygoteLoader.getProperties()
                    .getOrDefault(name, "true"));
        }
        return true;
    }

    public static void initPreliminary(BulkHooker hooks) {
        if (BuildConfig.PATCH_1 && booleanProperty("PATCH_1")) {
            int state_offset = fieldOffset(getDeclaredField(Signature.class, "state"));

            HookTransformer verify_impl = (original, frame) -> {
                HTF.printStackTrace(frame);
                var accessor = frame.accessor();

                Signature thiz = accessor.getReference(0);
                switch (thiz.getAlgorithm().toLowerCase()) {
                    case "rsa-sha1", "sha1withrsa", "sha256withdsa", "sha256withrsa" -> {
                        int state = AndroidUnsafe.getIntO(thiz, state_offset);
                        if (state == 3 /* Signature.VERIFY */) {
                            frame.accessor().setBoolean(RETURN_VALUE_IDX, true);
                            return;
                        }
                    }
                }

                Transformers.invokeExactWithFrame(original, frame);
            };

            hooks.addExact(verify_impl, "java.security.Signature", "verify", "boolean", "byte[]");
            hooks.addExact(verify_impl, "java.security.Signature", "verify", "boolean", "byte[]", "int", "int");

            hooks.addExact(HTF.TRUE, "com.android.org.conscrypt.OpenSSLSignature", "engineVerify", "boolean", "byte[]");
        }

        if (BuildConfig.PATCH_2 && booleanProperty("PATCH_2")) {
            hooks.addExact(HTF.TRUE, "java.security.MessageDigest", "isEqual", "boolean", "byte[]", "byte[]");
        }
    }

    public static void initSystem(BulkHooker hooks) {
        if (BuildConfig.PATCH_3 && booleanProperty("PATCH_3")) {
            if (SDK_INT >= 28) {
                var impl = SDK_INT < 33 ? HTF.TRUE : HTF.constant(true, new String[]{"installPackagesLI", "preparePackageLI"}, null);
                // 28 - >>
                hooks.addAll(impl, "android.content.pm.PackageParser$SigningDetails", "checkCapability");
            }
            if (SDK_INT >= 33) {
                // 33 - >>
                hooks.addAll(HTF.constant(true, new String[]{"installPackagesLI", "preparePackageLI"}, new String[]{"reconcilePackages"}), "android.content.pm.SigningDetails", "checkCapability");
            }

            if (SDK_INT < 33) {
                HookTransformer compare = HTF.constant(SIGNATURE_MATCH, null, new String[]{"scanPackageLI"});
                if (SDK_INT <= 27) {
                    // 26 - 27
                    hooks.addExact(compare, "com.android.server.pm.PackageManagerService", "compareSignatures", "int", "android.content.pm.Signature[]", "android.content.pm.Signature[]");
                } else {
                    // 28 - >>
                    hooks.addExact(compare, "com.android.server.pm.PackageManagerServiceUtils", "compareSignatures", "int", "android.content.pm.Signature[]", "android.content.pm.Signature[]");
                }
            }

            if (SDK_INT <= 27) {
                // 26 - 27
                hooks.addAll(HTF.NOP, "com.android.server.pm.PackageManagerService", "verifySignaturesLP");
            } else {
                // 28 - >>
                hooks.addAll(HTF.FALSE, "com.android.server.pm.PackageManagerServiceUtils", "verifySignatures");
            }

            if (SDK_INT == 31 || SDK_INT == 32) {
                // 31 - 32
                hooks.addAll(HTF.TRUE, "com.android.server.pm.PackageManagerService", "doesSignatureMatchForPermissions");
            } else if (SDK_INT >= 33) {
                // 33 - >>
                hooks.addAll(HTF.TRUE, "com.android.server.pm.InstallPackageHelper", "doesSignatureMatchForPermissions");
            }

            if (SDK_INT <= 32) {
                // 26 - 32
                hooks.addAll(HTF.NOP, "com.android.server.pm.PackageManagerService", "checkDowngrade");
                // 26 - 32
                //hooks.addAll(HTF.NOP, "com.android.server.pm.PackageManagerService", "assertPackageIsValid");
            } else {
                // 33 - >>
                hooks.addAll(HTF.NOP, "com.android.server.pm.PackageManagerServiceUtils", "checkDowngrade");
                // 33 - >>
                //hooks.addAll(HTF.NOP, "com.android.server.pm.InstallPackageHelper", "assertPackageIsValid");
            }

            if (SDK_INT >= 33) {
                // 33 - >>
                hooks.addAll(HTF.NOP, "com.android.server.pm.ScanPackageUtils", "assertMinSignatureSchemeIsValid");
            }
            if (SDK_INT >= 30) {
                // 30 - >>
                hooks.addAll(HTF.FALSE, "android.util.apk.ApkSignatureVerifier", "getMinimumSignatureSchemeVersionForTargetSdk");
                // 30 - >>
                hooks.addAll(HTF.FALSE, "com.android.apksig.ApkVerifier", "getMinimumSignatureSchemeVersionForTargetSdk");
            }

            switch (SDK_INT) {
                case 26, 27, 28, 29, 30 -> // 26 - 30
                        hooks.addAll(HTF.TRUE, "com.android.server.pm.permission.PermissionManagerService", "hasPrivappWhitelistEntry");
                case 31, 32 -> // 31 - 32
                        hooks.addAll(HTF.TRUE, "com.android.server.pm.permission.PermissionManagerService", "isInSystemConfigPrivAppDenyPermissions");
                case 33 -> // 33
                        hooks.addAll(HTF.TRUE, "com.android.server.pm.permission.PermissionManagerServiceImpl", "isInSystemConfigPrivAppDenyPermissions");
                default -> // 34 - >>
                        hooks.addAll(HTF.TRUE, "com.android.server.pm.permission.PermissionManagerServiceImpl", "getPrivilegedPermissionAllowlistState");
            }
        }
    }

    public static void initApplication(String package_name, BulkHooker hooks) {
    }
}
