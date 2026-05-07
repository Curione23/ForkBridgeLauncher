package ca.dnamobile.javalauncher.ui.version;

import android.content.Context;
import androidx.browser.trusted.sharing.ShareTarget;
import ca.dnamobile.javalauncher.data.model.MinecraftVersion;
import ca.dnamobile.javalauncher.feature.log.Logging;
import ca.dnamobile.javalauncher.settings.LauncherPreferences;
import ca.dnamobile.javalauncher.ui.version.FabricInstaller;
import ca.dnamobile.javalauncher.ui.version.InheritedVersionFlattener;
import ca.dnamobile.javalauncher.ui.version.MinecraftVersionInstaller;
import ca.dnamobile.javalauncher.ui.version.ParallelDownloadExecutor;
import ca.dnamobile.javalauncher.utils.path.PathManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class FabricInstaller {
    private static final int BUFFER_SIZE = 65536;
    private static final String FABRIC_META = "https://meta.fabricmc.net/v2";
    private static final int MAX_PARALLEL_DOWNLOADS = 4;
    private static final String TAG = "FabricInstaller";

    private FabricInstaller() {
    }

    public static final class InstallResult {
        private final String fabricVersionId;
        private final String loaderVersion;
        private final String minecraftVersionId;

        private InstallResult(String str, String str2, String str3) {
            this.minecraftVersionId = str;
            this.loaderVersion = str2;
            this.fabricVersionId = str3;
        }

        public String getMinecraftVersionId() {
            return this.minecraftVersionId;
        }

        public String getLoaderVersion() {
            return this.loaderVersion;
        }

        public String getFabricVersionId() {
            return this.fabricVersionId;
        }
    }

    public static InstallResult installFabricVersion(Context context, MinecraftVersion minecraftVersion, String str, MinecraftVersionInstaller.InstallProgressListener installProgressListener) throws Exception {
        PathManager.initContextConstants(context);
        String id = minecraftVersion.getId();
        if (!MinecraftVersionInstaller.findInstalledVersionIds().contains(id)) {
            MinecraftVersionInstaller.installVanillaVersion(context, minecraftVersion, installProgressListener);
        }
        notify(installProgressListener, 72, "Resolving Fabric Loader for " + id + "...");
        if (str == null || str.trim().isEmpty()) {
            str = fetchLatestStableLoaderVersion(id);
        }
        notify(installProgressListener, 76, "Downloading Fabric profile " + str + "...");
        JSONObject jSONObject = new JSONObject(downloadText(profileJsonUrl(id, str)));
        String strOptString = jSONObject.optString("id", "fabric-loader-" + str + "-" + id);
        File versionDirectory = MinecraftVersionInstaller.getVersionDirectory(strOptString);
        ensureDirectory(versionDirectory);
        writeString(new File(versionDirectory, strOptString + ".json"), jSONObject.toString(2));
        notify(installProgressListener, 80, "Downloading Fabric libraries...");
        downloadLibraries(jSONObject, installProgressListener);
        writeInstallMarker(strOptString, id, str, jSONObject);
        flattenInheritedProfileIfEnabled(context, strOptString, installProgressListener);
        notify(installProgressListener, 96, "Fabric " + str + " is ready.");
        return new InstallResult(id, str, strOptString);
    }

    private static void flattenInheritedProfileIfEnabled(Context context, String str, MinecraftVersionInstaller.InstallProgressListener installProgressListener) throws Exception {
        if (LauncherPreferences.isRemoveInheritedVanillaAfterLoaderInstall(context)) {
            notify(installProgressListener, 94, "Flattening loader profile...");
            if (InheritedVersionFlattener.flattenInstalledVersionProfile(context, str).flattened) {
                InheritedVersionFlattener.ParentDeleteResult parentDeleteResultDeleteFlattenedParentVersionIfSafe = InheritedVersionFlattener.deleteFlattenedParentVersionIfSafe(context, str);
                Logging.i(TAG, parentDeleteResultDeleteFlattenedParentVersionIfSafe.message);
                if (!parentDeleteResultDeleteFlattenedParentVersionIfSafe.deleted || parentDeleteResultDeleteFlattenedParentVersionIfSafe.parentVersionId == null) {
                    return;
                }
                notify(installProgressListener, 95, "Removed inherited vanilla files: " + parentDeleteResultDeleteFlattenedParentVersionIfSafe.parentVersionId);
            }
        }
    }

    public static String inferLoaderNameFromVersionId(String str) {
        String lowerCase = str.toLowerCase(Locale.ROOT);
        if (lowerCase.contains("fabric-loader") || lowerCase.startsWith("fabric-")) {
            return "Fabric";
        }
        return "Vanilla";
    }

    private static String fetchLatestStableLoaderVersion(String str) throws Exception {
        JSONArray jSONArray = new JSONArray(downloadText("https://meta.fabricmc.net/v2/versions/loader/" + encode(str)));
        String str2 = "";
        for (int i = 0; i < jSONArray.length(); i++) {
            JSONObject jSONObject = jSONArray.getJSONObject(i);
            JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject("loader");
            if (jSONObjectOptJSONObject != null) {
                jSONObject = jSONObjectOptJSONObject;
            }
            String strOptString = jSONObject.optString("version", "");
            if (!strOptString.isEmpty()) {
                if (str2.isEmpty()) {
                    str2 = strOptString;
                }
                if (jSONObject.optBoolean("stable", false)) {
                    return strOptString;
                }
            }
        }
        if (str2.isEmpty()) {
            throw new IllegalStateException("No Fabric Loader versions are available for " + str);
        }
        return str2;
    }

    private static String profileJsonUrl(String str, String str2) throws Exception {
        return "https://meta.fabricmc.net/v2/versions/loader/" + encode(str) + "/" + encode(str2) + "/profile/json";
    }

    private static String encode(String str) throws Exception {
        return URLEncoder.encode(str, "UTF-8");
    }

    private static void downloadLibraries(JSONObject jSONObject, final MinecraftVersionInstaller.InstallProgressListener installProgressListener) throws Exception {
        DownloadEntry downloadEntryCreateLibraryDownloadEntry;
        JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("libraries");
        if (jSONArrayOptJSONArray == null || jSONArrayOptJSONArray.length() == 0) {
            return;
        }
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
            JSONObject jSONObjectOptJSONObject = jSONArrayOptJSONArray.optJSONObject(i);
            if (jSONObjectOptJSONObject != null && (downloadEntryCreateLibraryDownloadEntry = createLibraryDownloadEntry(jSONObjectOptJSONObject)) != null) {
                arrayList.add(downloadEntryCreateLibraryDownloadEntry);
            }
        }
        if (arrayList.isEmpty()) {
            return;
        }
        final int iMax = Math.max(1, arrayList.size());
        ParallelDownloadExecutor.run(arrayList, 4, new ParallelDownloadExecutor.Worker() { // from class: ca.dnamobile.javalauncher.ui.version.FabricInstaller$$ExternalSyntheticLambda0
            @Override // ca.dnamobile.javalauncher.ui.version.ParallelDownloadExecutor.Worker
            public final void run(Object obj) throws Exception {
                FabricInstaller.downloadFileIfNeeded((FabricInstaller.DownloadEntry) obj);
            }
        }, new ParallelDownloadExecutor.Progress() { // from class: ca.dnamobile.javalauncher.ui.version.FabricInstaller$$ExternalSyntheticLambda1
            @Override // ca.dnamobile.javalauncher.ui.version.ParallelDownloadExecutor.Progress
            public final void onItemComplete(int i2, int i3, Object obj) {
                int i4 = iMax;
                FabricInstaller.notify(installProgressListener, ((int) ((((long) i2) * 12) / ((long) i4))) + 80, "Downloading Fabric libraries (" + i2 + "/" + i4 + "): " + ((FabricInstaller.DownloadEntry) obj).displayName);
            }
        });
    }

    private static DownloadEntry createLibraryDownloadEntry(JSONObject jSONObject) {
        String strArtifactPathFromName;
        long j;
        String strBuildLibraryUrl;
        String str;
        String strOptString = jSONObject.optString("name", "");
        if (strOptString.startsWith("org.lwjgl:")) {
            return null;
        }
        JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject("downloads");
        JSONObject jSONObjectOptJSONObject2 = jSONObjectOptJSONObject != null ? jSONObjectOptJSONObject.optJSONObject("artifact") : null;
        if (jSONObjectOptJSONObject2 != null) {
            strArtifactPathFromName = jSONObjectOptJSONObject2.optString("path", "");
            String strOptString2 = jSONObjectOptJSONObject2.optString("url", "");
            String strEmptyToNull = emptyToNull(jSONObjectOptJSONObject2.optString("sha1", null));
            long jOptLong = jSONObjectOptJSONObject2.optLong("size", 0L);
            if ((strOptString2 == null || strOptString2.isEmpty()) && strArtifactPathFromName != null && !strArtifactPathFromName.isEmpty()) {
                strOptString2 = buildLibraryUrl(jSONObject.optString("url", ""), strArtifactPathFromName);
            }
            strBuildLibraryUrl = strOptString2;
            str = strEmptyToNull;
            j = jOptLong;
        } else {
            strArtifactPathFromName = artifactPathFromName(strOptString);
            j = 0;
            strBuildLibraryUrl = strArtifactPathFromName == null ? null : buildLibraryUrl(jSONObject.optString("url", ""), strArtifactPathFromName);
            str = null;
        }
        if (strArtifactPathFromName == null || strArtifactPathFromName.isEmpty() || strBuildLibraryUrl == null || strBuildLibraryUrl.isEmpty()) {
            return null;
        }
        File file = new File(MinecraftVersionInstaller.getLibrariesDirectory(), strArtifactPathFromName);
        return new DownloadEntry(file, strBuildLibraryUrl, str, j, file.getName());
    }

    private static String artifactPathFromName(String str) {
        String[] strArrSplit = str.split(":");
        if (strArrSplit.length < 3) {
            return null;
        }
        String strReplace = strArrSplit[0].replace('.', '/');
        String str2 = strArrSplit[1];
        String str3 = strArrSplit[2];
        return strReplace + "/" + str2 + "/" + str3 + "/" + str2 + "-" + str3 + (strArrSplit.length >= 4 ? "-" + strArrSplit[3] : "") + ".jar";
    }

    private static String buildLibraryUrl(String str, String str2) {
        if (str == null || str.isEmpty()) {
            str = "https://maven.fabricmc.net/";
        }
        if (str.startsWith("http://")) {
            str = "https://" + str.substring("http://".length());
        }
        if (!str.endsWith("/")) {
            str = str + "/";
        }
        return str + str2;
    }

    private static void writeInstallMarker(String str, String str2, String str3, JSONObject jSONObject) throws Exception {
        File versionDirectory = MinecraftVersionInstaller.getVersionDirectory(str);
        JSONObject jSONObject2 = new JSONObject();
        jSONObject2.put("id", str);
        jSONObject2.put("loader", "Fabric");
        jSONObject2.put("minecraftVersion", str2);
        jSONObject2.put("loaderVersion", str3);
        jSONObject2.put("inheritsFrom", jSONObject.optString("inheritsFrom", str2));
        jSONObject2.put("versionDir", versionDirectory.getAbsolutePath());
        jSONObject2.put("librariesDir", MinecraftVersionInstaller.getLibrariesDirectory().getAbsolutePath());
        jSONObject2.put("installStage", "fabric_profile_ready");
        writeString(new File(versionDirectory, "java_launcher_fabric_install_marker.json"), jSONObject2.toString(2));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void downloadFileIfNeeded(DownloadEntry downloadEntry) throws Exception {
        if (downloadEntry.targetFile.isFile()) {
            if (downloadEntry.sha1 == null || sha1(downloadEntry.targetFile).equalsIgnoreCase(downloadEntry.sha1)) {
                return;
            } else {
                Logging.i(TAG, "Existing file hash mismatch, redownloading: " + downloadEntry.targetFile.getAbsolutePath());
            }
        }
        File parentFile = downloadEntry.targetFile.getParentFile();
        if (parentFile != null) {
            ensureDirectory(parentFile);
        }
        File file = new File(downloadEntry.targetFile.getAbsolutePath() + ".part");
        try {
            downloadToFile(downloadEntry.url, file);
            if (downloadEntry.sha1 != null && !sha1(file).equalsIgnoreCase(downloadEntry.sha1)) {
                throw new IllegalStateException("SHA-1 mismatch for " + downloadEntry.displayName);
            }
            if (downloadEntry.targetFile.exists() && !downloadEntry.targetFile.delete()) {
                throw new IllegalStateException("Unable to replace " + downloadEntry.targetFile.getAbsolutePath());
            }
            if (file.renameTo(downloadEntry.targetFile)) {
                return;
            }
            copyFile(file, downloadEntry.targetFile);
            file.delete();
        } catch (Exception e) {
            file.delete();
            throw e;
        }
    }

    private static String downloadText(String str) throws Exception {
        HttpURLConnection httpURLConnectionOpenConnection = openConnection(str);
        int responseCode = httpURLConnectionOpenConnection.getResponseCode();
        String stream = readStream((responseCode < 200 || responseCode >= 300) ? httpURLConnectionOpenConnection.getErrorStream() : httpURLConnectionOpenConnection.getInputStream());
        httpURLConnectionOpenConnection.disconnect();
        if (responseCode < 200 || responseCode >= 300) {
            throw new IllegalStateException("HTTP " + responseCode + " while downloading " + str + " " + stream);
        }
        return stream;
    }

    private static void downloadToFile(String str, File file) throws Exception {
        HttpURLConnection httpURLConnectionOpenConnection = openConnection(str);
        int responseCode = httpURLConnectionOpenConnection.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            String stream = readStream(httpURLConnectionOpenConnection.getErrorStream());
            httpURLConnectionOpenConnection.disconnect();
            throw new IllegalStateException("HTTP " + responseCode + " while downloading " + str + " " + stream);
        }
        try {
            InputStream inputStream = httpURLConnectionOpenConnection.getInputStream();
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                try {
                    byte[] bArr = new byte[65536];
                    while (true) {
                        int i = inputStream.read(bArr);
                        if (i == -1) {
                            break;
                        } else {
                            fileOutputStream.write(bArr, 0, i);
                        }
                    }
                    fileOutputStream.close();
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } finally {
                }
            } finally {
            }
        } finally {
            httpURLConnectionOpenConnection.disconnect();
        }
    }

    private static HttpURLConnection openConnection(String str) throws Exception {
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(str).openConnection();
        httpURLConnection.setConnectTimeout(15000);
        httpURLConnection.setReadTimeout(45000);
        httpURLConnection.setRequestProperty("User-Agent", "JavaLauncher/1.0");
        httpURLConnection.setRequestMethod(ShareTarget.METHOD_GET);
        return httpURLConnection;
    }

    /* JADX WARN: Removed duplicated region for block: B:34:0x003a A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private static java.lang.String readStream(java.io.InputStream r4) throws java.lang.Exception {
        /*
            if (r4 != 0) goto L5
            java.lang.String r4 = ""
            return r4
        L5:
            java.io.ByteArrayOutputStream r0 = new java.io.ByteArrayOutputStream     // Catch: java.lang.Throwable -> L37
            r0.<init>()     // Catch: java.lang.Throwable -> L37
            r1 = 65536(0x10000, float:9.1835E-41)
            byte[] r1 = new byte[r1]     // Catch: java.lang.Throwable -> L2d
        Le:
            int r2 = r4.read(r1)     // Catch: java.lang.Throwable -> L2d
            r3 = -1
            if (r2 == r3) goto L1a
            r3 = 0
            r0.write(r1, r3, r2)     // Catch: java.lang.Throwable -> L2d
            goto Le
        L1a:
            java.nio.charset.Charset r1 = java.nio.charset.StandardCharsets.UTF_8     // Catch: java.lang.Throwable -> L2d
            java.lang.String r1 = r1.name()     // Catch: java.lang.Throwable -> L2d
            java.lang.String r1 = r0.toString(r1)     // Catch: java.lang.Throwable -> L2d
            r0.close()     // Catch: java.lang.Throwable -> L37
            if (r4 == 0) goto L2c
            r4.close()
        L2c:
            return r1
        L2d:
            r1 = move-exception
            r0.close()     // Catch: java.lang.Throwable -> L32
            goto L36
        L32:
            r0 = move-exception
            r1.addSuppressed(r0)     // Catch: java.lang.Throwable -> L37
        L36:
            throw r1     // Catch: java.lang.Throwable -> L37
        L37:
            r0 = move-exception
            if (r4 == 0) goto L42
            r4.close()     // Catch: java.lang.Throwable -> L3e
            goto L42
        L3e:
            r4 = move-exception
            r0.addSuppressed(r4)
        L42:
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: ca.dnamobile.javalauncher.ui.version.FabricInstaller.readStream(java.io.InputStream):java.lang.String");
    }

    private static void writeString(File file, String str) throws Exception {
        File parentFile = file.getParentFile();
        if (parentFile != null) {
            ensureDirectory(parentFile);
        }
        FileOutputStream fileOutputStream = new FileOutputStream(file);
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

    private static void copyFile(File file, File file2) throws Exception {
        File parentFile = file2.getParentFile();
        if (parentFile != null) {
            ensureDirectory(parentFile);
        }
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file2);
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

    private static void ensureDirectory(File file) {
        if (!file.exists() && !file.mkdirs()) {
            throw new IllegalStateException("Unable to create directory: " + file.getAbsolutePath());
        }
    }

    private static String sha1(File file) throws Exception {
        int i;
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            byte[] bArr = new byte[65536];
            while (true) {
                int i2 = fileInputStream.read(bArr);
                if (i2 == -1) {
                    break;
                }
                messageDigest.update(bArr, 0, i2);
            }
            fileInputStream.close();
            byte[] bArrDigest = messageDigest.digest();
            StringBuilder sb = new StringBuilder(bArrDigest.length * 2);
            for (byte b : bArrDigest) {
                sb.append(String.format(Locale.ROOT, "%02x", Byte.valueOf(b)));
            }
            return sb.toString();
        } catch (Throwable th) {
            try {
                fileInputStream.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    private static String emptyToNull(String str) {
        if (str == null || str.trim().isEmpty()) {
            return null;
        }
        return str.trim();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void notify(MinecraftVersionInstaller.InstallProgressListener installProgressListener, int i, String str) {
        if (installProgressListener != null) {
            installProgressListener.onProgress(Math.max(0, Math.min(100, i)), str);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class DownloadEntry {
        final String displayName;
        final String sha1;
        final long size;
        final File targetFile;
        final String url;

        private DownloadEntry(File file, String str, String str2, long j, String str3) {
            this.targetFile = file;
            this.url = str;
            this.sha1 = str2;
            this.size = Math.max(0L, j);
            this.displayName = str3;
        }
    }
}
