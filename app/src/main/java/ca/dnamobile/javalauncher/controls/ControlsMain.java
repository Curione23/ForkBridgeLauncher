package ca.dnamobile.javalauncher.controls;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import ca.dnamobile.javalauncher.BuildConfig;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class ControlsMain {
    private static final String BLOCK_MESSAGE = "This copy of DroidBridge Launcher was not signed by DNA Mobile Applications. For your safety, this build is blocked.";
    private static final String BLOCK_TITLE = "App integrity check failed";
    private static final String CLOSE_BUTTON = "Close";
    private static final boolean ENFORCE_IN_DEBUG_BUILDS = false;

    public static boolean shouldEnforceSignatureCheck() {
        return false;
    }

    private ControlsMain() {
    }

    public static boolean isExpectedSignature(Context context) {
        if (!shouldEnforceSignatureCheck()) {
            return true;
        }
        List<String> expectedHashes = getExpectedHashes();
        if (expectedHashes.isEmpty()) {
            return true;
        }
        try {
            Signature[] installedSignatures = getInstalledSignatures(context);
            if (installedSignatures != null && installedSignatures.length != 0) {
                for (Signature signature : installedSignatures) {
                    if (expectedHashes.contains(sha256(signature.toByteArray()))) {
                        return true;
                    }
                }
            }
        } catch (Throwable unused) {
        }
        return false;
    }

    public static boolean blockIfInvalidSignature(Activity activity) {
        if (isExpectedSignature(activity)) {
            return false;
        }
        showBlockingDialog(activity);
        return true;
    }

    public static boolean shouldBlockSensitiveAction(Context context) {
        return !isExpectedSignature(context);
    }

    public static void throwIfInvalidSignature(Context context) {
        if (shouldBlockSensitiveAction(context)) {
            throw new SecurityException("Blocked modified or re-signed DroidBridge Launcher build.");
        }
    }

    public static boolean toastAndBlockIfInvalidSignature(Context context) {
        if (!shouldBlockSensitiveAction(context)) {
            return false;
        }
        Toast.makeText(context, BLOCK_TITLE, 1).show();
        return true;
    }

    private static void showBlockingDialog(final Activity activity) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        new Handler(Looper.getMainLooper()).post(new Runnable() { // from class: ca.dnamobile.javalauncher.controls.ControlsMain$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                ControlsMain.lambda$showBlockingDialog$1(activity);
            }
        });
    }

    static /* synthetic */ void lambda$showBlockingDialog$1(final Activity activity) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        try {
            new AlertDialog.Builder(activity).setTitle(BLOCK_TITLE).setMessage(BLOCK_MESSAGE).setCancelable(false).setPositiveButton(CLOSE_BUTTON, new DialogInterface.OnClickListener() { // from class: ca.dnamobile.javalauncher.controls.ControlsMain$$ExternalSyntheticLambda0
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    ControlsMain.lambda$showBlockingDialog$0(activity, dialogInterface, i);
                }
            }).show();
        } catch (Throwable unused) {
            closeApp(activity);
        }
    }

    static /* synthetic */ void lambda$showBlockingDialog$0(Activity activity, DialogInterface dialogInterface, int i) {
        dialogInterface.dismiss();
        closeApp(activity);
    }

    private static List<String> getExpectedHashes() {
        ArrayList arrayList = new ArrayList();
        String strSafeString = safeString(BuildConfig.EXPECTED_RELEASE_CERT_SHA256);
        if (!strSafeString.isEmpty() && !strSafeString.contains("PUT_YOUR") && !strSafeString.contains("CHANGE_ME")) {
            for (String str : strSafeString.split("[,;\\n\\r]+")) {
                String strNormalizeSha256 = normalizeSha256(str);
                if (strNormalizeSha256.length() == 64 && !arrayList.contains(strNormalizeSha256)) {
                    arrayList.add(strNormalizeSha256);
                }
            }
        }
        return arrayList;
    }

    private static Signature[] getInstalledSignatures(Context context) throws Exception {
        PackageManager packageManager = context.getPackageManager();
        String packageName = context.getPackageName();
        if (Build.VERSION.SDK_INT >= 28) {
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 134217728);
            if (packageInfo.signingInfo == null) {
                return null;
            }
            if (packageInfo.signingInfo.hasMultipleSigners()) {
                return packageInfo.signingInfo.getApkContentsSigners();
            }
            return packageInfo.signingInfo.getSigningCertificateHistory();
        }
        return packageManager.getPackageInfo(packageName, 64).signatures;
    }

    private static String sha256(byte[] bArr) throws Exception {
        byte[] bArrDigest = MessageDigest.getInstance("SHA-256").digest(bArr);
        StringBuilder sb = new StringBuilder(bArrDigest.length * 2);
        for (byte b : bArrDigest) {
            sb.append(String.format(Locale.ROOT, "%02x", Integer.valueOf(b & 255)));
        }
        return sb.toString();
    }

    private static String normalizeSha256(String str) {
        if (str == null) {
            return "";
        }
        return str.replace(":", "").replace(" ", "").replace("-", "").trim().toLowerCase(Locale.ROOT);
    }

    private static String safeString(String str) {
        return str == null ? "" : str.trim();
    }

    private static void closeApp(Activity activity) {
        try {
            try {
                activity.finishAffinity();
            } catch (Throwable unused) {
                activity.finish();
            }
        } catch (Throwable unused2) {
        }
    }
}
