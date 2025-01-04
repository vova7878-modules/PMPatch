package com.v7878.hooks.pmpatch;

import static android.content.pm.PackageManager.SIGNATURE_MATCH;
import static com.v7878.unsafe.Reflection.fieldOffset;
import static com.v7878.unsafe.Reflection.getDeclaredField;
import static com.v7878.unsafe.invoke.EmulatedStackFrame.RETURN_VALUE_IDX;

import com.v7878.unsafe.AndroidUnsafe;
import com.v7878.unsafe.invoke.Transformers;
import com.v7878.vmtools.Hooks.HookTransformer;
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

    public static void init(BulkHooker hooks, boolean system_server) {
        if (!system_server && BuildConfig.PATCH_1
                && booleanProperty("PATCH_1")) {
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

            hooks.add(verify_impl, "java.security.Signature", "verify", "boolean", "byte[]");
            hooks.add(verify_impl, "java.security.Signature", "verify", "boolean", "byte[]", "int", "int");

            hooks.add(HTF.TRUE, "com.android.org.conscrypt.OpenSSLSignature", "engineVerify", "boolean", "byte[]");
        }

        if (!system_server && BuildConfig.PATCH_2
                && booleanProperty("PATCH_2")) {
            hooks.add(HTF.TRUE, "java.security.MessageDigest", "isEqual", "boolean", "byte[]", "byte[]");
        }

        if (system_server && BuildConfig.PATCH_3
                && booleanProperty("PATCH_3")) {
            hooks.add(HTF.TRUE, "android.content.pm.PackageParser$SigningDetails", "checkCapability", "boolean", "android.content.pm.PackageParser$SigningDetails", "int");
            hooks.add(HTF.TRUE, "android.content.pm.PackageParser$SigningDetails", "checkCapability", "boolean", "java.lang.String", "int");
            hooks.add(HTF.TRUE, "android.content.pm.PackageParser$SigningDetails", "checkCapabilityRecover", "boolean", "android.content.pm./PackageParser$SigningDetails", "int");

            hooks.add(HTF.TRUE, "android.content.pm.SigningDetails", "checkCapability", "boolean", "android.content.pm.SigningDetails", "int");
            hooks.add(HTF.TRUE, "android.content.pm.SigningDetails", "checkCapability", "boolean", "java.lang.String", "int");
            hooks.add(HTF.TRUE, "android.content.pm.SigningDetails", "checkCapabilityRecover", "boolean", "android.content.pm.SigningDetails", "int");

            hooks.add(HTF.return_constant(SIGNATURE_MATCH), "com.android.server.pm.PackageManagerServiceUtils", "compareSignatures", "int", "android.content.pm.Signature[]", "android.content.pm.Signature[]");
            hooks.add(HTF.return_constant(SIGNATURE_MATCH), "com.android.server.pm.PackageManagerService", "compareSignatures", "int", "android.content.pm.Signature[]", "android.content.pm.Signature[]");

            hooks.add(HTF.FALSE, "com.android.server.pm.PackageManagerServiceUtils", "verifySignatures", "boolean", "com.android.server.pm.PackageSetting", "com.android.server.pm.SharedUserSetting", "com.android.server.pm.PackageSetting", "android.content.pm.SigningDetails", "boolean", "boolean", "boolean");
            hooks.add(HTF.FALSE, "com.android.server.pm.PackageManagerServiceUtils", "verifySignatures", "boolean", "com.android.server.pm.PackageSetting", "com.android.server.pm.PackageSetting", "android.content.pm.PackageParser$SigningDetails", "boolean", "boolean", "boolean");

            hooks.add(HTF.TRUE, "com.android.server.pm.PackageManagerServiceUtils", "isDowngradePermitted", "boolean", "int", "boolean");

            hooks.add(HTF.NOP, "com.android.server.pm.PackageManagerServiceUtils", "checkDowngrade", "void", "com.android.server.pm.parsing.pkg.AndroidPackage", "android.content.pm.PackageInfoLite");
            hooks.add(HTF.NOP, "com.android.server.pm.PackageManagerServiceUtils", "checkDowngrade", "void", "com.android.server.pm.pkg.AndroidPackage", "android.content.pm.PackageInfoLite");
            hooks.add(HTF.NOP, "com.android.server.pm.PackageManagerService", "checkDowngrade", "void", "android.content.pm.PackageParser$Package", "android.content.pm.PackageInfoLite");
            hooks.add(HTF.NOP, "com.android.server.pm.PackageManagerService", "checkDowngrade", "void", "com.android.server.pm.parsing.pkg.AndroidPackage", "android.content.pm.PackageInfoLite");

            hooks.add(HTF.TRUE, "com.android.server.pm.InstallPackageHelper", "doesSignatureMatchForPermissions", "boolean", "java.lang.String", "com.android.server.pm.parsing.pkg.ParsedPackage", "int");
            hooks.add(HTF.TRUE, "com.android.server.pm.InstallPackageHelper", "doesSignatureMatchForPermissions", "boolean", "java.lang.String", "com.android.internal.pm.parsing.pkg.ParsedPackage", "int");

            hooks.add(HTF.NOP, "com.android.server.pm.PackageManagerService", "assertPackageIsValid", "void", "android.content.pm.PackageParser$Package", "android.content.pm.PackageInfoLite");
            hooks.add(HTF.NOP, "com.android.server.pm.InstallPackageHelper", "assertPackageIsValid", "void", "com.android.server.pm.pkg.AndroidPackage", "int", "int");
            hooks.add(HTF.NOP, "com.android.server.pm.InstallPackageHelper", "assertPackageIsValid", "void", "com.android.server.pm.parsing.pkg.AndroidPackage", "int", "int");

            hooks.add(HTF.TRUE, "com.android.server.pm.InstallPackageHelper", "canSkipForcedPackageVerification", "boolean", "com.android.server.pm.parsing.pkg.AndroidPackage");

            hooks.add(HTF.return_constant(Boolean.TRUE), "com.android.server.pm.permission.PermissionManagerServiceImpl", "getPrivilegedPermissionAllowlistState", "java.lang.Boolean", "com.android.server.pm.pkg.PackageState", "java.lang.String", "java.lang.String");
            hooks.add(HTF.TRUE, "com.android.server.pm.permission.PermissionManagerServiceImpl", "isInSystemConfigPrivAppDenyPermissions", "boolean", "com.android.server.pm.parsing.pkg.AndroidPackage", "java.lang.String", "java.lang.String");

            // android oreo
            //hooks.add(HTF.TODO, "com.android.server.pm.PackageManagerService", "scanPackageDirtyLI", "android.content.pm.PackageParser$Package", "android.content.pm.PackageParser$Package", "int", "int", "long", "android.os.UserHandle");
        }
    }
}
