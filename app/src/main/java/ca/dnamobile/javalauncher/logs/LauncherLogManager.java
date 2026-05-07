package ca.dnamobile.javalauncher.logs;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import androidx.core.os.EnvironmentCompat;
import ca.dnamobile.javalauncher.BuildConfig;
import ca.dnamobile.javalauncher.R;
import ca.dnamobile.javalauncher.feature.log.Logging;
import ca.dnamobile.javalauncher.utils.path.PathManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import net.kdt.pojavlaunch.Logger;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class LauncherLogManager {
    private static final String KEY_KEEP_LOG_HISTORY = "keep_log_history";
    private static final String KEY_LAST_LATEST_LOG_PATH = "last_latest_log_path";
    private static final String PREFS = "launcher_logs";
    private static final String TAG = "LauncherLogManager";
    private static File activeLatestLogFile = null;
    private static boolean nativeLogStarted = false;

    private LauncherLogManager() {
    }

    public static boolean isKeepLogHistoryEnabled(Context context) {
        return prefs(context).getBoolean(KEY_KEEP_LOG_HISTORY, true);
    }

    public static void setKeepLogHistoryEnabled(Context context, boolean z) {
        prefs(context).edit().putBoolean(KEY_KEEP_LOG_HISTORY, z).apply();
    }

    public static File getLatestLogFile(Context context) {
        PathManager.initContextConstants(context);
        return new File(PathManager.DIR_MINECRAFT_HOME, "latestlog.txt");
    }

    public static File resolveLatestLogFile(Context context) {
        File currentLogFile = Logger.getCurrentLogFile();
        if (isUsableLog(currentLogFile)) {
            return currentLogFile;
        }
        if (isUsableLog(activeLatestLogFile)) {
            return activeLatestLogFile;
        }
        String string = prefs(context).getString(KEY_LAST_LATEST_LOG_PATH, "");
        if (string != null && !string.trim().isEmpty()) {
            File file = new File(string.trim());
            if (isUsableLog(file)) {
                return file;
            }
        }
        File file2 = null;
        for (File file3 : buildLatestLogCandidates(context)) {
            if (isUsableLog(file3) && (file2 == null || file3.lastModified() > file2.lastModified())) {
                file2 = file3;
            }
        }
        return file2 != null ? file2 : getLatestLogFile(context);
    }

    public static File getLogHistoryDirectory(Context context) {
        PathManager.initContextConstants(context);
        File file = new File(PathManager.DIR_MINECRAFT_HOME, "launcher_log");
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }

    private static File getLogHistoryDirectoryForLatest(Context context, File file) {
        File parentFile = file.getParentFile();
        File file2 = parentFile != null ? new File(parentFile, "launcher_log") : getLogHistoryDirectory(context);
        if (!file2.exists()) {
            file2.mkdirs();
        }
        return file2;
    }

    public static synchronized void beginLatestLog(Context context, String str) {
        File latestLogFileForActivePath = getLatestLogFileForActivePath(context);
        rememberLatestLogPath(context, latestLogFileForActivePath);
        File parentFile = latestLogFileForActivePath.getParentFile();
        if (parentFile != null && !parentFile.exists()) {
            parentFile.mkdirs();
        }
        boolean z = nativeLogStarted;
        File file = activeLatestLogFile;
        boolean z2 = file == null || !file.equals(latestLogFileForActivePath);
        if (!z || z2 || latestLogFileForActivePath.length() == 0) {
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(latestLogFileForActivePath, false);
                try {
                    fileOutputStream.write(buildFileHeader(context, str).getBytes(StandardCharsets.UTF_8));
                    fileOutputStream.close();
                } finally {
                }
            } catch (Throwable th) {
                Logging.e(TAG, "Failed to initialize latestlog.txt", th);
            }
        }
        if (!nativeLogStarted || z2) {
            Logger.beginLog(latestLogFileForActivePath);
            nativeLogStarted = true;
            activeLatestLogFile = latestLogFileForActivePath;
        } else {
            append("----------------------------------------");
            append("New launch started for " + str + " at " + new Date());
        }
    }

    public static void append(String str) {
        String strCleanLauncherLine = LatestLogTextFilter.cleanLauncherLine(str);
        if (strCleanLauncherLine == null) {
            return;
        }
        try {
            Logger.appendToLog(strCleanLauncherLine);
        } catch (Throwable th) {
            Logging.e(TAG, "appendToLog failed", th);
            appendFallback(strCleanLauncherLine);
        }
    }

    public static synchronized void cleanLatestLogInPlace(Context context) {
        String textFile;
        String strCleanWholeLog;
        File fileResolveLatestLogFile = resolveLatestLogFile(context);
        if (!fileResolveLatestLogFile.isFile() || fileResolveLatestLogFile.length() <= 0) {
            return;
        }
        try {
            textFile = readTextFile(fileResolveLatestLogFile);
            strCleanWholeLog = LatestLogTextFilter.cleanWholeLog(textFile);
        } catch (Throwable th) {
            Logging.e(TAG, "Failed to clean latestlog.txt", th);
        }
        if (strCleanWholeLog.equals(textFile)) {
            return;
        }
        FileOutputStream fileOutputStream = new FileOutputStream(fileResolveLatestLogFile, false);
        try {
            fileOutputStream.write(strCleanWholeLog.getBytes(StandardCharsets.UTF_8));
            fileOutputStream.close();
        } finally {
        }
    }

    private static void appendFallback(String str) {
        File file = activeLatestLogFile;
        if (file == null) {
            return;
        }
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file, true);
            try {
                fileOutputStream.write((LatestLogTextFilter.normalizeLauncherLine(str) + "\n").getBytes(StandardCharsets.UTF_8));
                fileOutputStream.close();
            } finally {
            }
        } catch (Throwable unused) {
        }
    }

    public static void preserveLatestLogIfEnabled(Context context, String str) {
        if (isKeepLogHistoryEnabled(context)) {
            cleanLatestLogInPlace(context);
            File fileResolveLatestLogFile = resolveLatestLogFile(context);
            if (!fileResolveLatestLogFile.isFile() || fileResolveLatestLogFile.length() <= 0) {
                return;
            }
            File file = new File(getLogHistoryDirectoryForLatest(context, fileResolveLatestLogFile), "latestlog-" + str.replaceAll("[^A-Za-z0-9._-]", "_") + "-" + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date()) + ".txt");
            try {
                copyFile(fileResolveLatestLogFile, file);
                Logging.i(TAG, "Saved launch log history: " + file.getAbsolutePath());
            } catch (Throwable th) {
                Logging.e(TAG, "Failed to save launch log history", th);
            }
        }
    }

    public static void shareLatestLog(Activity activity) {
        cleanLatestLogInPlace(activity);
        File fileResolveLatestLogFile = resolveLatestLogFile(activity);
        if (!fileResolveLatestLogFile.isFile() || fileResolveLatestLogFile.length() <= 0) {
            Toast.makeText(activity, R.string.log_latest_missing, 1).show();
            return;
        }
        rememberLatestLogPath(activity, fileResolveLatestLogFile);
        File file = new File(activity.getCacheDir(), "shared_logs");
        File file2 = new File(file, "latestlog.txt");
        File file3 = new File(file, "latestlog.html");
        try {
            copyFile(fileResolveLatestLogFile, file2);
            writeChromeHtmlPreview(file2, file3);
            file2.setReadable(true, false);
            file3.setReadable(true, false);
            openOrShareTextFile(activity, file2, file3);
        } catch (Throwable th) {
            Logging.e(TAG, "Failed to share cached latestlog.txt", th);
            try {
                openOrShareTextFile(activity, fileResolveLatestLogFile, null);
            } catch (Throwable th2) {
                Logging.e(TAG, "Failed to share latestlog.txt", th2);
                Toast.makeText(activity, th2.getMessage(), 1).show();
            }
        }
    }

    private static void openOrShareTextFile(Activity activity, File file, File file2) {
        Intent intentBuildViewIntent;
        Uri uriForFile = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", file);
        Intent intentBuildSendIntent = buildSendIntent(activity, uriForFile);
        if (file2 != null && file2.isFile()) {
            intentBuildViewIntent = buildViewIntent(activity, FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", file2), "text/html", "latestlog.html");
        } else {
            intentBuildViewIntent = buildViewIntent(activity, uriForFile, "text/plain", "latestlog.txt");
        }
        Intent intentCreateChooser = Intent.createChooser(intentBuildViewIntent, activity.getString(R.string.button_share_latest_log));
        intentCreateChooser.putExtra("android.intent.extra.INITIAL_INTENTS", new Intent[]{intentBuildSendIntent});
        intentCreateChooser.addFlags(1);
        try {
            activity.startActivity(intentCreateChooser);
        } catch (ActivityNotFoundException unused) {
            activity.startActivity(Intent.createChooser(intentBuildSendIntent, activity.getString(R.string.button_share_latest_log)));
        }
    }

    private static Intent buildSendIntent(Activity activity, Uri uri) {
        Intent intent = new Intent("android.intent.action.SEND");
        intent.setType("text/plain");
        intent.putExtra("android.intent.extra.STREAM", uri);
        intent.putExtra("android.intent.extra.SUBJECT", "DroidBridge latestlog.txt");
        intent.putExtra("android.intent.extra.TEXT", "DroidBridge latestlog.txt");
        intent.setClipData(ClipData.newUri(activity.getContentResolver(), "latestlog.txt", uri));
        intent.addFlags(1);
        return intent;
    }

    private static Intent buildViewIntent(Activity activity, Uri uri, String str, String str2) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setDataAndType(uri, str);
        intent.setClipData(ClipData.newUri(activity.getContentResolver(), str2, uri));
        intent.addFlags(1);
        return intent;
    }

    private static void writeChromeHtmlPreview(File file, File file2) throws Exception {
        File parentFile = file2.getParentFile();
        if (parentFile != null && !parentFile.exists()) {
            parentFile.mkdirs();
        }
        String str = "<!doctype html>\n<html><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"><title>DroidBridge latestlog.txt</title><style>body{margin:0;padding:16px;background:#111;color:#eee;font-family:monospace;font-size:13px;line-height:1.35;}pre{white-space:pre-wrap;word-wrap:break-word;margin:0;}</style></head><body><pre>" + escapeHtml(readTextFile(file)) + "</pre></body></html>\n";
        FileOutputStream fileOutputStream = new FileOutputStream(file2, false);
        try {
            fileOutputStream.write(str.getBytes(StandardCharsets.UTF_8));
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

    private static String escapeHtml(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt == '\"') {
                sb.append("&quot;");
            } else if (cCharAt == '<') {
                sb.append("&lt;");
            } else if (cCharAt == '>') {
                sb.append("&gt;");
            } else if (cCharAt == '&') {
                sb.append("&amp;");
            } else if (cCharAt == '\'') {
                sb.append("&#39;");
            } else {
                sb.append(cCharAt);
            }
        }
        return sb.toString();
    }

    private static String buildFileHeader(Context context, String str) {
        StringBuilder sb = new StringBuilder("DroidBridge Launcher latestlog.txt\nLauncher: ");
        sb.append(getInstalledLauncherVersion(context)).append("\nBuild package: ");
        sb.append(context.getPackageName()).append("\nMinecraft: ");
        sb.append(str).append("\nStarted: ");
        sb.append(new Date()).append("\n----------------------------------------\n");
        return sb.toString();
    }

    public static String getInstalledLauncherVersion(Context context) {
        String strTrim;
        long longVersionCode;
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            if (packageInfo.versionName == null || packageInfo.versionName.trim().isEmpty()) {
                strTrim = EnvironmentCompat.MEDIA_UNKNOWN;
            } else {
                strTrim = packageInfo.versionName.trim();
            }
            if (Build.VERSION.SDK_INT >= 28) {
                longVersionCode = packageInfo.getLongVersionCode();
            } else {
                longVersionCode = packageInfo.versionCode;
            }
            return "DroidBridge Launcher " + strTrim + " (" + longVersionCode + ")";
        } catch (Throwable unused) {
            return "DroidBridge Launcher unknown";
        }
    }

    private static File getLatestLogFileForActivePath(Context context) {
        if (PathManager.DIR_MINECRAFT_HOME == null || PathManager.DIR_MINECRAFT_HOME.trim().isEmpty()) {
            PathManager.initContextConstants(context);
        }
        return new File(PathManager.DIR_MINECRAFT_HOME, "latestlog.txt");
    }

    private static ArrayList<File> buildLatestLogCandidates(Context context) {
        ArrayList<File> arrayList = new ArrayList<>();
        addCandidate(arrayList, activeLatestLogFile);
        String string = prefs(context).getString(KEY_LAST_LATEST_LOG_PATH, "");
        if (string != null && !string.trim().isEmpty()) {
            addCandidate(arrayList, new File(string.trim()));
        }
        try {
            PathManager.initContextConstants(context);
            addCandidate(arrayList, new File(PathManager.DIR_MINECRAFT_HOME, "latestlog.txt"));
        } catch (Throwable unused) {
        }
        try {
            addCandidate(arrayList, new File(new File(PathManager.getDefaultLauncherHome(context), BuildConfig.GAME_DIRECTORY_NAME), "latestlog.txt"));
        } catch (Throwable unused2) {
        }
        return arrayList;
    }

    private static void addCandidate(ArrayList<File> arrayList, File file) {
        if (file == null) {
            return;
        }
        String absolutePath = file.getAbsolutePath();
        Iterator<File> it = arrayList.iterator();
        while (it.hasNext()) {
            if (it.next().getAbsolutePath().equals(absolutePath)) {
                return;
            }
        }
        arrayList.add(file);
    }

    private static boolean isUsableLog(File file) {
        return file != null && file.isFile() && file.length() > 0;
    }

    private static void rememberLatestLogPath(Context context, File file) {
        prefs(context).edit().putString(KEY_LAST_LATEST_LOG_PATH, file.getAbsolutePath()).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, 0);
    }

    private static String readTextFile(File file) throws Exception {
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            int iMin = (int) Math.min(file.length(), 2147483647L);
            byte[] bArr = new byte[iMin];
            int i = 0;
            while (i < iMin) {
                int i2 = fileInputStream.read(bArr, i, iMin - i);
                if (i2 == -1) {
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

    private static void copyFile(File file, File file2) throws Exception {
        File parentFile = file2.getParentFile();
        if (parentFile != null && !parentFile.exists()) {
            parentFile.mkdirs();
        }
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file2, false);
            try {
                byte[] bArr = new byte[65536];
                while (true) {
                    int i = fileInputStream.read(bArr);
                    if (i != -1) {
                        fileOutputStream.write(bArr, 0, i);
                    } else {
                        fileOutputStream.close();
                        fileInputStream.close();
                        return;
                    }
                }
            } finally {
            }
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
