package ca.dnamobile.javalauncher.skin;

import android.content.Context;
import ca.dnamobile.javalauncher.data.AccountStore;
import ca.dnamobile.javalauncher.feature.log.Logging;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class AccountSkinCache {
    private static final String DIR_NAME = "account_skins";
    private static final String TAG = "AccountSkinCache";

    private AccountSkinCache() {
    }

    public static File getSkinFile(Context context, String str) {
        File file = new File(context.getApplicationContext().getFilesDir(), DIR_NAME);
        if (!file.exists()) {
            file.mkdirs();
        }
        return new File(file, str.replace("-", "").toLowerCase() + ".png");
    }

    public static File getCachedSkinFileIfPresent(Context context, AccountStore.Account account) {
        if (account == null || isBlank(account.minecraftUuid)) {
            return null;
        }
        File skinFile = getSkinFile(context, account.minecraftUuid);
        if (skinFile.isFile()) {
            return skinFile;
        }
        return null;
    }

    public static void cacheMicrosoftSkinAsync(Context context, final AccountStore.Account account) {
        if (account == null || isBlank(account.minecraftUuid) || isBlank(account.skinUrl)) {
            return;
        }
        final Context applicationContext = context.getApplicationContext();
        new Thread(new Runnable() { // from class: ca.dnamobile.javalauncher.skin.AccountSkinCache$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                AccountSkinCache.cacheMicrosoftSkin(applicationContext, account);
            }
        }, "JavaLauncherMicrosoftSkinCache").start();
    }

    public static boolean cacheMicrosoftSkin(Context context, AccountStore.Account account) {
        if (account == null || isBlank(account.minecraftUuid) || isBlank(account.skinUrl)) {
            return false;
        }
        File skinFile = getSkinFile(context, account.minecraftUuid);
        File file = new File(skinFile.getParentFile(), skinFile.getName() + ".tmp");
        HttpURLConnection httpURLConnection = null;
        try {
            HttpURLConnection httpURLConnection2 = (HttpURLConnection) new URL(normalizeSkinUrl(account.skinUrl)).openConnection();
            try {
                httpURLConnection2.setConnectTimeout(15000);
                httpURLConnection2.setReadTimeout(15000);
                httpURLConnection2.setUseCaches(true);
                httpURLConnection2.setRequestProperty("User-Agent", "JavaLauncher");
                int responseCode = httpURLConnection2.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) {
                    InputStream inputStream = httpURLConnection2.getInputStream();
                    try {
                        FileOutputStream fileOutputStream = new FileOutputStream(file);
                        try {
                            byte[] bArr = new byte[8192];
                            while (true) {
                                int i = inputStream.read(bArr);
                                if (i == -1) {
                                    break;
                                }
                                fileOutputStream.write(bArr, 0, i);
                            }
                            fileOutputStream.close();
                            if (inputStream != null) {
                                inputStream.close();
                            }
                            if (!CustomSkinStore.isSkinValid(file)) {
                                file.delete();
                                Logging.i(TAG, "Downloaded Microsoft skin was not a valid 64x64/64x32 skin.");
                                if (httpURLConnection2 != null) {
                                    httpURLConnection2.disconnect();
                                }
                                return false;
                            }
                            if (skinFile.exists()) {
                                skinFile.delete();
                            }
                            if (file.renameTo(skinFile)) {
                                Logging.i(TAG, "Cached Microsoft skin for " + account.minecraftName + " at " + skinFile.getAbsolutePath());
                                if (httpURLConnection2 != null) {
                                    httpURLConnection2.disconnect();
                                }
                                return true;
                            }
                            file.delete();
                            if (httpURLConnection2 != null) {
                                httpURLConnection2.disconnect();
                            }
                            return false;
                        } finally {
                        }
                    } finally {
                    }
                }
                Logging.i(TAG, "Skin download failed with HTTP " + responseCode + " for " + account.minecraftName);
                if (httpURLConnection2 != null) {
                    httpURLConnection2.disconnect();
                }
                return false;
            } catch (Throwable th) {
                th = th;
                httpURLConnection = httpURLConnection2;
                try {
                    Logging.e(TAG, "Unable to cache Microsoft skin", th);
                    file.delete();
                    return false;
                } finally {
                    if (httpURLConnection != null) {
                        httpURLConnection.disconnect();
                    }
                }
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    public static String normalizeSkinUrl(String str) {
        if (str == null) {
            return "";
        }
        String strTrim = str.trim();
        return strTrim.startsWith("http://textures.minecraft.net/") ? "https://textures.minecraft.net/" + strTrim.substring("http://textures.minecraft.net/".length()) : strTrim;
    }

    private static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
}
