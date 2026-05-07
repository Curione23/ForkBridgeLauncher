package ca.dnamobile.javalauncher.modmanager;

import ca.dnamobile.javalauncher.BuildConfig;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class CurseForgeApiKeyProvider {
    private CurseForgeApiKeyProvider() {
    }

    public static String resolve() {
        return isRealKey(BuildConfig.CURSEFORGE_API_KEY) ? BuildConfig.CURSEFORGE_API_KEY.trim() : "";
    }

    private static boolean isRealKey(String str) {
        if (str == null) {
            return false;
        }
        String strTrim = str.trim();
        return (strTrim.isEmpty() || "YOUR_CURSEFORGE_API_KEY".equalsIgnoreCase(strTrim) || "PUT_YOUR_CURSEFORGE_API_KEY_HERE".equalsIgnoreCase(strTrim) || strTrim.startsWith("REPLACE_")) ? false : true;
    }
}
