package ca.dnamobile.javalauncher.launcher;

import android.content.Context;
import ca.dnamobile.javalauncher.feature.log.Logging;
import ca.dnamobile.javalauncher.settings.LauncherPreferences;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import org.json.JSONObject;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class GraphicsBackendHelper {
    private static final String LEGACY_BACKEND_KEY = "graphicsBackend";
    private static final String PREFERRED_BACKEND_KEY = "preferredGraphicsBackend";
    private static final String TAG = "GraphicsBackendHelper";

    private GraphicsBackendHelper() {
    }

    public static void applyBeforeLaunch(Context context, String str, JSONObject jSONObject, File file) {
        if (!LauncherPreferences.isUseOpenGlForMinecraft26Plus(context)) {
            Logging.i(TAG, "Minecraft 26+ OpenGL override disabled.");
            return;
        }
        String strResolveEffectiveMinecraftVersion = resolveEffectiveMinecraftVersion(str, jSONObject);
        if (!isMinecraft26OrNewer(strResolveEffectiveMinecraftVersion)) {
            Logging.i(TAG, "Skipping OpenGL backend override for non-26+ version: " + strResolveEffectiveMinecraftVersion);
            return;
        }
        try {
            writeBackendValue(new File(file, "options.txt"), "opengl");
            Logging.i(TAG, "Forced preferredGraphicsBackend=opengl for " + strResolveEffectiveMinecraftVersion + " options=" + new File(file, "options.txt").getAbsolutePath());
        } catch (Throwable th) {
            Logging.e(TAG, "Failed to force OpenGL backend for Minecraft " + strResolveEffectiveMinecraftVersion, th);
        }
    }

    private static String resolveEffectiveMinecraftVersion(String str, JSONObject jSONObject) {
        String strTrim = jSONObject.optString("inheritsFrom", "").trim();
        if (!strTrim.isEmpty()) {
            return strTrim;
        }
        String strTrim2 = jSONObject.optString("javaLauncherFlattenedParent", "").trim();
        if (!strTrim2.isEmpty()) {
            return strTrim2;
        }
        String strTrim3 = jSONObject.optString("id", str).trim();
        return strTrim3.isEmpty() ? str : strTrim3;
    }

    private static boolean isMinecraft26OrNewer(String str) {
        String lowerCase = str.trim().toLowerCase(Locale.ROOT);
        return lowerCase.matches("^\\d{2}w\\d{2}[a-z].*") ? parseLeadingInt(lowerCase, 2) >= 26 : (lowerCase.matches("^\\d{2}(?:\\.|-).*") || lowerCase.matches("^\\d{2}$")) && parseLeadingInt(lowerCase, 2) >= 26;
    }

    private static int parseLeadingInt(String str, int i) {
        try {
            return Integer.parseInt(str.substring(0, Math.min(i, str.length())));
        } catch (Throwable unused) {
            return -1;
        }
    }

    private static void writeBackendValue(File file, String str) throws Exception {
        File parentFile = file.getParentFile();
        if (parentFile != null && !parentFile.exists() && !parentFile.mkdirs()) {
            throw new IllegalStateException("Unable to create options directory: " + parentFile.getAbsolutePath());
        }
        ArrayList<String> arrayList = new ArrayList();
        if (file.isFile()) {
            for (String str2 : readFile(file).split("\\r?\\n", -1)) {
                if (!str2.isEmpty()) {
                    arrayList.add(str2);
                }
            }
        }
        boolean z = false;
        boolean z2 = false;
        for (int i = 0; i < arrayList.size(); i++) {
            String str3 = (String) arrayList.get(i);
            if (str3.startsWith("preferredGraphicsBackend:")) {
                arrayList.set(i, "preferredGraphicsBackend:" + str);
                z = true;
            } else if (str3.startsWith("graphicsBackend:")) {
                arrayList.set(i, "graphicsBackend:" + str);
                z2 = true;
            }
        }
        if (!z && !z2) {
            arrayList.add("preferredGraphicsBackend:" + str);
        }
        StringBuilder sb = new StringBuilder();
        for (String str4 : arrayList) {
            if (str4 != null && !str4.trim().isEmpty()) {
                sb.append(str4).append('\n');
            }
        }
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        try {
            fileOutputStream.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            fileOutputStream.close();
        } catch (Throwable th) {
            try {
                fileOutputStream.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    private static String readFile(File file) throws Exception {
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            int length = (int) file.length();
            byte[] bArr = new byte[length];
            int i = 0;
            while (i < length) {
                int i2 = fileInputStream.read(bArr, i, length - i);
                if (i2 < 0) {
                    break;
                }
                i += i2;
            }
            String str = new String(bArr, 0, i, StandardCharsets.UTF_8);
            fileInputStream.close();
            return str;
        } catch (Throwable th) {
            try {
                fileInputStream.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }
}
