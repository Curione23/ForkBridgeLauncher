package ca.dnamobile.javalauncher.ui.version;

import android.content.Context;
import androidx.browser.trusted.sharing.ShareTarget;
import ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticBackport0;
import ca.dnamobile.javalauncher.data.model.MinecraftVersion;
import ca.dnamobile.javalauncher.feature.log.Logging;
import ca.dnamobile.javalauncher.ui.version.MinecraftVersionInstaller;
import ca.dnamobile.javalauncher.ui.version.ParallelDownloadExecutor;
import ca.dnamobile.javalauncher.utils.path.PathManager;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class MinecraftVersionInstaller {
    private static final int DOWNLOAD_BUFFER_SIZE = 65536;
    private static final int MAX_PARALLEL_DOWNLOADS = ParallelDownloadExecutor.defaultNetworkThreads();
    private static final String RESOURCE_BASE_URL = "https://resources.download.minecraft.net/";
    private static final String TAG = "VanillaInstaller";

    public interface InstallProgressListener {
        void onProgress(int i, String str);
    }

    private MinecraftVersionInstaller() {
    }

    public static void installVanillaVersion(Context context, MinecraftVersion minecraftVersion, final InstallProgressListener installProgressListener) throws Exception {
        PathManager.initContextConstants(context);
        notifyProgress(installProgressListener, 0, "Preparing " + minecraftVersion.getId() + "...");
        File versionDirectory = getVersionDirectory(minecraftVersion.getId());
        File file = new File(versionDirectory, minecraftVersion.getId() + ".json");
        File file2 = new File(versionDirectory, minecraftVersion.getId() + ".jar");
        ensureDirectory(versionDirectory);
        String strDownloadText = downloadText(minecraftVersion.getMetadataUrl());
        writeString(file, strDownloadText);
        JSONObject jSONObject = new JSONObject(strDownloadText);
        String strInstallAssetIndexAndBuildAssetDownloads = installAssetIndexAndBuildAssetDownloads(installProgressListener, jSONObject, 5, 12);
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        collectClientJar(arrayList, jSONObject, file2);
        collectLibraries(arrayList, arrayList2, jSONObject);
        final int size = arrayList.size();
        collectAssets(arrayList, getAssetIndexFile(strInstallAssetIndexAndBuildAssetDownloads));
        ArrayList<DownloadEntry> arrayListDeduplicateDownloadEntries = deduplicateDownloadEntries(arrayList);
        final int iMax = Math.max(1, arrayListDeduplicateDownloadEntries.size());
        notifyProgress(installProgressListener, 12, "Downloading " + arrayListDeduplicateDownloadEntries.size() + " Minecraft files...");
        ParallelDownloadExecutor.run(arrayListDeduplicateDownloadEntries, MAX_PARALLEL_DOWNLOADS, new ParallelDownloadExecutor.Worker() { // from class: ca.dnamobile.javalauncher.ui.version.MinecraftVersionInstaller$$ExternalSyntheticLambda0
            @Override // ca.dnamobile.javalauncher.ui.version.ParallelDownloadExecutor.Worker
            public final void run(Object obj) throws Exception {
                MinecraftVersionInstaller.downloadFileIfNeededWithRetry((MinecraftVersionInstaller.DownloadEntry) obj);
            }
        }, new ParallelDownloadExecutor.Progress() { // from class: ca.dnamobile.javalauncher.ui.version.MinecraftVersionInstaller$$ExternalSyntheticLambda1
            @Override // ca.dnamobile.javalauncher.ui.version.ParallelDownloadExecutor.Progress
            public final void onItemComplete(int i, int i2, Object obj) {
                int i3 = iMax;
                MinecraftVersionInstaller.notifyProgress(installProgressListener, ((int) ((((long) i) * 83) / ((long) i3))) + 12, (i <= size ? "Downloading game files" : "Downloading assets") + " (" + i + "/" + i3 + "): " + ((MinecraftVersionInstaller.DownloadEntry) obj).displayName);
            }
        });
        extractNativeAars(arrayList2, minecraftVersion.getId(), installProgressListener);
        writeInstallMarker(minecraftVersion, jSONObject, strInstallAssetIndexAndBuildAssetDownloads, arrayListDeduplicateDownloadEntries.size(), file2);
        writeLaunchPlan(minecraftVersion, jSONObject, strInstallAssetIndexAndBuildAssetDownloads, file2);
        notifyProgress(installProgressListener, 100, "Vanilla " + minecraftVersion.getId() + " installed.");
    }

    public static void installVersionMetadata(Context context, MinecraftVersion minecraftVersion) throws Exception {
        installVanillaVersion(context, minecraftVersion, null);
    }

    public static File getVersionsDirectory() {
        return getVersionsDirectory(new File(PathManager.DIR_MINECRAFT_HOME));
    }

    public static File getVersionsDirectory(File file) {
        return new File(file, "versions");
    }

    public static List<MinecraftVersion> findInstalledVersions() {
        if (PathManager.DIR_MINECRAFT_HOME == null || MainActivity$$ExternalSyntheticBackport0.m(PathManager.DIR_MINECRAFT_HOME)) {
            return new ArrayList();
        }
        return findInstalledVersions(new File(PathManager.DIR_MINECRAFT_HOME));
    }

    public static List<MinecraftVersion> findInstalledVersions(File file) {
        String strOptString;
        JSONObject jSONObject;
        ArrayList arrayList = new ArrayList();
        File[] fileArrListFiles = getVersionsDirectory(file).listFiles();
        if (fileArrListFiles == null) {
            return arrayList;
        }
        for (File file2 : fileArrListFiles) {
            if (file2.isDirectory()) {
                File file3 = new File(file2, file2.getName() + ".json");
                if (file3.isFile()) {
                    String name = file2.getName();
                    String strOptString2 = "installed";
                    try {
                        jSONObject = new JSONObject(readString(file3));
                    } catch (Throwable th) {
                        Logging.i(TAG, "Unable to read installed version metadata for " + name + ": " + th.getMessage());
                        strOptString = "";
                    }
                    if (hasLaunchableClientJar(name, jSONObject)) {
                        strOptString2 = jSONObject.optString("type", "installed");
                        strOptString = jSONObject.optString("releaseTime", jSONObject.optString("time", ""));
                        arrayList.add(new MinecraftVersion(name, strOptString2, strOptString, ""));
                    }
                }
            }
        }
        return arrayList;
    }

    private static boolean hasLaunchableClientJar(String str, JSONObject jSONObject) {
        if (new File(getVersionDirectory(str), str + ".jar").isFile()) {
            return true;
        }
        String strOptString = jSONObject.optString("inheritsFrom", "");
        if (strOptString.isEmpty()) {
            return false;
        }
        return new File(getVersionDirectory(strOptString), strOptString + ".jar").isFile();
    }

    public static Set<String> findInstalledVersionIds() {
        HashSet hashSet = new HashSet();
        Iterator<MinecraftVersion> it = findInstalledVersions().iterator();
        while (it.hasNext()) {
            hashSet.add(it.next().getId());
        }
        return hashSet;
    }

    public static File getVersionDirectory(String str) {
        return new File(getVersionsDirectory(), str);
    }

    public static File getVersionDirectory(File file, String str) {
        return new File(getVersionsDirectory(file), str);
    }

    public static File getLibrariesDirectory() {
        return new File(PathManager.DIR_MINECRAFT_HOME, "libraries");
    }

    public static File getLibrariesDirectory(File file) {
        return new File(file, "libraries");
    }

    public static File getAssetsDirectory() {
        return new File(PathManager.DIR_MINECRAFT_HOME, "assets");
    }

    private static String installAssetIndexAndBuildAssetDownloads(InstallProgressListener installProgressListener, JSONObject jSONObject, int i, int i2) throws Exception {
        JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject("assetIndex");
        String strOptString = jSONObject.optString("assets", "legacy");
        if (jSONObjectOptJSONObject == null) {
            notifyProgress(installProgressListener, i2, "No asset index listed for this version.");
            return strOptString;
        }
        String strOptString2 = jSONObjectOptJSONObject.optString("id", strOptString);
        String strOptString3 = jSONObjectOptJSONObject.optString("url", "");
        String strOptString4 = jSONObjectOptJSONObject.optString("sha1", null);
        if (strOptString3.isEmpty()) {
            notifyProgress(installProgressListener, i2, "No asset index URL listed for this version.");
            return strOptString2;
        }
        File assetIndexFile = getAssetIndexFile(strOptString2);
        notifyProgress(installProgressListener, i, "Downloading asset index " + strOptString2 + "...");
        downloadFileIfNeeded(new DownloadEntry(assetIndexFile, strOptString3, emptyToNull(strOptString4), jSONObjectOptJSONObject.optLong("size", 0L), "asset index " + strOptString2, false));
        notifyProgress(installProgressListener, i2, "Asset index " + strOptString2 + " is ready.");
        return strOptString2;
    }

    private static File getAssetIndexFile(String str) {
        return new File(getAssetsDirectory(), "indexes" + File.separator + str + ".json");
    }

    private static void collectClientJar(List<DownloadEntry> list, JSONObject jSONObject, File file) throws Exception {
        JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject("downloads");
        if (jSONObjectOptJSONObject == null) {
            throw new IllegalStateException("Version JSON has no downloads block.");
        }
        JSONObject jSONObjectOptJSONObject2 = jSONObjectOptJSONObject.optJSONObject("client");
        if (jSONObjectOptJSONObject2 == null) {
            throw new IllegalStateException("Version JSON has no client download block.");
        }
        String strOptString = jSONObjectOptJSONObject2.optString("url", "");
        if (strOptString.isEmpty()) {
            throw new IllegalStateException("Version JSON has no client jar URL.");
        }
        list.add(new DownloadEntry(file, strOptString, emptyToNull(jSONObjectOptJSONObject2.optString("sha1", null)), jSONObjectOptJSONObject2.optLong("size", 0L), file.getName(), false));
    }

    /* JADX WARN: Removed duplicated region for block: B:10:0x0023  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private static void collectLibraries(java.util.List<ca.dnamobile.javalauncher.ui.version.MinecraftVersionInstaller.DownloadEntry> r20, java.util.List<java.io.File> r21, org.json.JSONObject r22) throws java.lang.Exception {
        /*
            Method dump skipped, instruction units count: 238
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: ca.dnamobile.javalauncher.ui.version.MinecraftVersionInstaller.collectLibraries(java.util.List, java.util.List, org.json.JSONObject):void");
    }

    public static void ensureJnaNativesForLaunch(String str, JSONObject jSONObject) throws Exception {
        File file = new File(getNativeExtractionDirectory(str), "libjnidispatch.so");
        if (file.isFile()) {
            Logging.i(TAG, "JNA dispatch already extracted: " + file.getAbsolutePath());
            return;
        }
        JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("libraries");
        if (jSONArrayOptJSONArray == null) {
            return;
        }
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
            JSONObject jSONObjectOptJSONObject = jSONArrayOptJSONArray.optJSONObject(i);
            if (jSONObjectOptJSONObject != null) {
                String strOptString = jSONObjectOptJSONObject.optString("name", "");
                if (strOptString.startsWith("net.java.dev.jna:jna:") && isAllowedByRules(jSONObjectOptJSONObject.optJSONArray("rules"))) {
                    scheduleNativeAarDownload(arrayList, arrayList2, strOptString);
                }
            }
        }
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            downloadFileIfNeeded((DownloadEntry) it.next());
        }
        extractNativeAars(arrayList2, str, null);
    }

    private static void scheduleNativeAarDownload(List<DownloadEntry> list, List<File> list2, String str) throws Exception {
        String strArtifactPathFromNameWithExtension = artifactPathFromNameWithExtension(str, ".aar");
        if (strArtifactPathFromNameWithExtension == null || strArtifactPathFromNameWithExtension.isEmpty()) {
            return;
        }
        File file = new File(getLibrariesDirectory(), strArtifactPathFromNameWithExtension);
        if (!list2.contains(file)) {
            list2.add(file);
        }
        list.add(new DownloadEntry(file, "https://repo1.maven.org/maven2/" + strArtifactPathFromNameWithExtension, null, 0L, file.getName(), true));
    }

    private static void extractNativeAars(List<File> list, String str, InstallProgressListener installProgressListener) throws Exception {
        if (list.isEmpty()) {
            return;
        }
        File nativeExtractionDirectory = getNativeExtractionDirectory(str);
        ensureDirectory(nativeExtractionDirectory);
        NativesExtractor nativesExtractor = new NativesExtractor(nativeExtractionDirectory);
        int i = 0;
        for (File file : list) {
            i++;
            notifyProgress(installProgressListener, 95, "Extracting native libraries (" + i + "/" + list.size() + "): " + file.getName());
            if (!file.isFile()) {
                Logging.i(TAG, "Skipping missing native AAR: " + file.getAbsolutePath());
            } else {
                nativesExtractor.extractFromAar(file);
                Logging.i(TAG, "Extracted native AAR: " + file.getAbsolutePath() + " -> " + nativeExtractionDirectory.getAbsolutePath());
            }
        }
    }

    public static File getNativeExtractionDirectory(String str) {
        return new File(PathManager.DIR_CACHE, "natives" + File.separator + str);
    }

    /* JADX WARN: Removed duplicated region for block: B:16:0x0048  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private static void collectAssets(java.util.List<ca.dnamobile.javalauncher.ui.version.MinecraftVersionInstaller.DownloadEntry> r21, java.io.File r22) throws java.lang.Exception {
        /*
            Method dump skipped, instruction units count: 291
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: ca.dnamobile.javalauncher.ui.version.MinecraftVersionInstaller.collectAssets(java.util.List, java.io.File):void");
    }

    /* JADX WARN: Removed duplicated region for block: B:19:0x0037  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private static boolean isAllowedByRules(org.json.JSONArray r6) {
        /*
            if (r6 == 0) goto L47
            int r0 = r6.length()
            if (r0 != 0) goto L9
            goto L47
        L9:
            r0 = 0
            r1 = r0
        Lb:
            int r2 = r6.length()
            if (r0 >= r2) goto L46
            org.json.JSONObject r2 = r6.optJSONObject(r0)
            if (r2 != 0) goto L18
            goto L43
        L18:
            java.lang.String r3 = "os"
            org.json.JSONObject r3 = r2.optJSONObject(r3)
            if (r3 == 0) goto L37
            java.lang.String r4 = "name"
            java.lang.String r5 = ""
            java.lang.String r3 = r3.optString(r4, r5)
            boolean r4 = r3.isEmpty()
            if (r4 != 0) goto L37
            java.lang.String r4 = "linux"
            boolean r3 = r4.equals(r3)
            if (r3 != 0) goto L37
            goto L43
        L37:
            java.lang.String r1 = "action"
            java.lang.String r3 = "allow"
            java.lang.String r1 = r2.optString(r1, r3)
            boolean r1 = r3.equals(r1)
        L43:
            int r0 = r0 + 1
            goto Lb
        L46:
            return r1
        L47:
            r6 = 1
            return r6
        */
        throw new UnsupportedOperationException("Method not decompiled: ca.dnamobile.javalauncher.ui.version.MinecraftVersionInstaller.isAllowedByRules(org.json.JSONArray):boolean");
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

    private static String artifactPathFromNameWithExtension(String str, String str2) {
        String[] strArrSplit = str.split(":");
        if (strArrSplit.length < 3) {
            return null;
        }
        String strReplace = strArrSplit[0].replace('.', '/');
        String str3 = strArrSplit[1];
        String str4 = strArrSplit[2];
        return strReplace + "/" + str3 + "/" + str4 + "/" + str3 + "-" + str4 + (strArrSplit.length >= 4 ? "-" + strArrSplit[3] : "") + str2;
    }

    private static String buildLibraryUrl(String str, String str2) {
        if (str == null || str.isEmpty()) {
            str = "https://libraries.minecraft.net/";
        }
        if (str.startsWith("http://")) {
            str = "https://" + str.substring("http://".length());
        }
        if (!str.endsWith("/")) {
            str = str + "/";
        }
        return str + str2;
    }

    private static void writeInstallMarker(MinecraftVersion minecraftVersion, JSONObject jSONObject, String str, int i, File file) throws Exception {
        JSONObject jSONObject2 = new JSONObject();
        jSONObject2.put("id", minecraftVersion.getId());
        jSONObject2.put("type", minecraftVersion.getType());
        jSONObject2.put("releaseTime", minecraftVersion.getReleaseTime());
        jSONObject2.put("metadataUrl", minecraftVersion.getMetadataUrl());
        jSONObject2.put("installStage", "vanilla_full");
        jSONObject2.put("downloadCount", i);
        jSONObject2.put("versionDir", getVersionDirectory(minecraftVersion.getId()).getAbsolutePath());
        jSONObject2.put("clientJar", file.getAbsolutePath());
        jSONObject2.put("librariesDir", getLibrariesDirectory().getAbsolutePath());
        jSONObject2.put("assetsDir", getAssetsDirectory().getAbsolutePath());
        jSONObject2.put("assetIndex", str);
        jSONObject2.put("mainClass", jSONObject.optString("mainClass", ""));
        jSONObject2.put("note", "Vanilla files are installed. Next step is wiring launch arguments/JVM startup.");
        writeString(new File(getVersionDirectory(minecraftVersion.getId()), "java_launcher_install_marker.json"), jSONObject2.toString(2));
    }

    private static void writeLaunchPlan(MinecraftVersion minecraftVersion, JSONObject jSONObject, String str, File file) throws Exception {
        JSONObject jSONObject2 = new JSONObject();
        jSONObject2.put("id", minecraftVersion.getId());
        jSONObject2.put("type", minecraftVersion.getType());
        jSONObject2.put("mainClass", jSONObject.optString("mainClass", ""));
        jSONObject2.put("versionJson", new File(getVersionDirectory(minecraftVersion.getId()), minecraftVersion.getId() + ".json").getAbsolutePath());
        jSONObject2.put("clientJar", file.getAbsolutePath());
        jSONObject2.put("versionDir", getVersionDirectory(minecraftVersion.getId()).getAbsolutePath());
        jSONObject2.put("librariesDir", getLibrariesDirectory().getAbsolutePath());
        jSONObject2.put("assetsDir", getAssetsDirectory().getAbsolutePath());
        jSONObject2.put("assetIndex", str);
        jSONObject2.put("gameDirectory", PathManager.DIR_MINECRAFT_HOME);
        jSONObject2.put("javaLauncherStage", "vanilla_files_ready");
        writeString(new File(getVersionDirectory(minecraftVersion.getId()), "java_launcher_launch_plan.json"), jSONObject2.toString(2));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void downloadFileIfNeededWithRetry(DownloadEntry downloadEntry) throws Exception {
        int i = downloadEntry.optional ? 2 : 4;
        Exception e = null;
        for (int i2 = 1; i2 <= i; i2++) {
            try {
                downloadFileIfNeeded(downloadEntry);
                return;
            } catch (Exception e2) {
                e = e2;
                if (i2 >= i) {
                    break;
                }
                long j = ((long) i2) * 500;
                Logging.i(TAG, "Download failed for " + downloadEntry.displayName + " attempt " + i2 + "/" + i + ": " + e.getMessage() + ". Retrying in " + j + "ms");
                try {
                    Thread.sleep(j);
                } catch (InterruptedException e3) {
                    Thread.currentThread().interrupt();
                    throw e3;
                }
            }
        }
        if (e != null) {
            throw e;
        }
    }

    private static void downloadFileIfNeeded(DownloadEntry downloadEntry) throws Exception {
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
            if (downloadEntry.optional) {
                Logging.i(TAG, "Skipping optional download: " + downloadEntry.url + " because " + e.getMessage());
                return;
            }
            throw e;
        }
    }

    private static ArrayList<DownloadEntry> deduplicateDownloadEntries(ArrayList<DownloadEntry> arrayList) {
        String absolutePath;
        ArrayList<DownloadEntry> arrayList2 = new ArrayList<>(arrayList.size());
        HashSet hashSet = new HashSet();
        for (DownloadEntry downloadEntry : arrayList) {
            try {
                absolutePath = downloadEntry.targetFile.getCanonicalPath();
            } catch (Throwable unused) {
                absolutePath = downloadEntry.targetFile.getAbsolutePath();
            }
            if (hashSet.add(absolutePath)) {
                arrayList2.add(downloadEntry);
            }
        }
        return arrayList2;
    }

    public static String downloadText(String str) throws Exception {
        HttpURLConnection httpURLConnectionOpenConnection = openConnection(str);
        int responseCode = httpURLConnectionOpenConnection.getResponseCode();
        String stream = readStream((responseCode < 200 || responseCode >= 300) ? httpURLConnectionOpenConnection.getErrorStream() : httpURLConnectionOpenConnection.getInputStream());
        httpURLConnectionOpenConnection.disconnect();
        if (responseCode < 200 || responseCode >= 300) {
            throw new IllegalStateException("HTTP " + responseCode + " while downloading " + str);
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
        throw new UnsupportedOperationException("Method not decompiled: ca.dnamobile.javalauncher.ui.version.MinecraftVersionInstaller.readStream(java.io.InputStream):java.lang.String");
    }

    private static String readString(File file) throws Exception {
        ByteArrayOutputStream byteArrayOutputStream;
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            byteArrayOutputStream = new ByteArrayOutputStream();
        } catch (Throwable th) {
            try {
                fileInputStream.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
        try {
            byte[] bArr = new byte[65536];
            while (true) {
                int i = fileInputStream.read(bArr);
                if (i != -1) {
                    byteArrayOutputStream.write(bArr, 0, i);
                } else {
                    String string = byteArrayOutputStream.toString(StandardCharsets.UTF_8.name());
                    byteArrayOutputStream.close();
                    fileInputStream.close();
                    return string;
                }
                fileInputStream.close();
                throw th;
            }
        } finally {
        }
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

    private static String stripJsonExtension(String str) {
        return str.endsWith(".json") ? str.substring(0, str.length() - 5) : str;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void notifyProgress(InstallProgressListener installProgressListener, int i, String str) {
        if (installProgressListener != null) {
            installProgressListener.onProgress(Math.max(0, Math.min(100, i)), str);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class DownloadEntry {
        final String displayName;
        final boolean optional;
        final String sha1;
        final long size;
        final File targetFile;
        final String url;

        private DownloadEntry(File file, String str, String str2, long j, String str3, boolean z) {
            this.targetFile = file;
            this.url = str;
            this.sha1 = str2;
            this.size = Math.max(0L, j);
            this.displayName = str3;
            this.optional = z;
        }
    }
}
