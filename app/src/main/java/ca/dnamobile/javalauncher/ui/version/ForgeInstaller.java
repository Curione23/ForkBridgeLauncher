package ca.dnamobile.javalauncher.ui.version;

import android.content.Context;
import androidx.browser.trusted.sharing.ShareTarget;
import androidx.constraintlayout.core.motion.utils.TypedValues;
import ca.dnamobile.javalauncher.data.model.MinecraftVersion;
import ca.dnamobile.javalauncher.feature.log.Logging;
import ca.dnamobile.javalauncher.launcher.JavaGameLauncher;
import ca.dnamobile.javalauncher.settings.LauncherPreferences;
import ca.dnamobile.javalauncher.storage.StorageLocationStore;
import ca.dnamobile.javalauncher.ui.version.InheritedVersionFlattener;
import ca.dnamobile.javalauncher.ui.version.MinecraftVersionInstaller;
import ca.dnamobile.javalauncher.utils.path.LibPath;
import ca.dnamobile.javalauncher.utils.path.PathManager;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import net.kdt.pojavlaunch.multirt.MultiRTUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class ForgeInstaller {
    private static final int BUFFER_SIZE = 65536;
    private static final String FORGE_INSTALLER_URL = "https://maven.minecraftforge.net/net/minecraftforge/forge/%1$s/forge-%1$s-installer.jar";
    private static final String FORGE_METADATA_URL = "https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml";
    private static final int MAX_PARALLEL_DOWNLOADS = 4;
    private static final String TAG = "ForgeInstaller";

    private ForgeInstaller() {
    }

    public static final class InstallResult {
        private final String forgeVersionId;
        private final String fullForgeVersion;
        private final String loaderVersion;
        private final String minecraftVersionId;

        private InstallResult(String str, String str2, String str3, String str4) {
            this.minecraftVersionId = str;
            this.loaderVersion = str2;
            this.fullForgeVersion = str3;
            this.forgeVersionId = str4;
        }

        public String getMinecraftVersionId() {
            return this.minecraftVersionId;
        }

        public String getLoaderVersion() {
            return this.loaderVersion;
        }

        public String getFullForgeVersion() {
            return this.fullForgeVersion;
        }

        public String getForgeVersionId() {
            return this.forgeVersionId;
        }
    }

    public static InstallResult installForgeVersion(Context context, MinecraftVersion minecraftVersion, String str, String str2, final MinecraftVersionInstaller.InstallProgressListener installProgressListener) throws Exception {
        ensureActivePathManager(context);
        String id = minecraftVersion.getId();
        ensureVanillaBaseIsValid(context, minecraftVersion, installProgressListener);
        notify(installProgressListener, 68, "Resolving Forge for " + id + "...");
        String strResolveFullForgeVersion = resolveFullForgeVersion(id, str2);
        String strSubstring = strResolveFullForgeVersion.substring((id + "-").length());
        String strCreateUniqueForgeVersionId = createUniqueForgeVersionId(str, id);
        notify(installProgressListener, 72, "Downloading Forge installer " + strSubstring + "...");
        File file = new File(PathManager.DIR_CACHE, "forge-" + strResolveFullForgeVersion + "-installer.jar");
        downloadFileIfNeeded(file, getInstallerUrl(strResolveFullForgeVersion), null);
        notify(installProgressListener, 76, "Preparing Forge installer profile...");
        patchForgeInstallerProfile(file, strCreateUniqueForgeVersionId);
        generateLauncherProfiles();
        cleanupForgeGeneratedOutputs(id, strCreateUniqueForgeVersionId, strResolveFullForgeVersion);
        if (LibPath.FORGE_INSTALLER == null || !LibPath.FORGE_INSTALLER.isFile()) {
            throw new IllegalStateException("Missing Forge installer support component: " + (LibPath.FORGE_INSTALLER == null ? "null" : LibPath.FORGE_INSTALLER.getAbsolutePath()));
        }
        notify(installProgressListener, 80, "Running Forge installer...");
        ArrayList arrayList = new ArrayList();
        arrayList.add("-Djava.awt.headless=true");
        arrayList.add("-Duser.home=" + PathManager.DIR_MINECRAFT_HOME);
        arrayList.add("-Djava.io.tmpdir=" + PathManager.DIR_CACHE.getAbsolutePath());
        arrayList.add("-javaagent:" + LibPath.FORGE_INSTALLER.getAbsolutePath() + "=" + strSubstring);
        arrayList.add("-jar");
        arrayList.add(file.getAbsolutePath());
        arrayList.add("--installClient");
        arrayList.add(PathManager.DIR_MINECRAFT_HOME);
        File fileResolveInstallerRuntime = resolveInstallerRuntime(id, strSubstring);
        int iLaunchRawJavaArgsWithProgress = JavaGameLauncher.launchRawJavaArgsWithProgress(context, "forge-installer-" + strSubstring, fileResolveInstallerRuntime, new File(PathManager.DIR_MINECRAFT_HOME), arrayList, 81, 88, new JavaGameLauncher.RawJavaProgressListener() { // from class: ca.dnamobile.javalauncher.ui.version.ForgeInstaller$$ExternalSyntheticLambda2
            @Override // ca.dnamobile.javalauncher.launcher.JavaGameLauncher.RawJavaProgressListener
            public final void onProgress(int i, String str3) {
                ForgeInstaller.notify(installProgressListener, i, str3);
            }
        });
        if (iLaunchRawJavaArgsWithProgress != 0) {
            notify(installProgressListener, 86, "Forge installer failed once. Cleaning generated outputs and retrying...");
            cleanupForgeGeneratedOutputs(id, strCreateUniqueForgeVersionId, strResolveFullForgeVersion);
            iLaunchRawJavaArgsWithProgress = JavaGameLauncher.launchRawJavaArgsWithProgress(context, "forge-installer-retry-" + strSubstring, fileResolveInstallerRuntime, new File(PathManager.DIR_MINECRAFT_HOME), arrayList, 86, 89, new JavaGameLauncher.RawJavaProgressListener() { // from class: ca.dnamobile.javalauncher.ui.version.ForgeInstaller$$ExternalSyntheticLambda3
                @Override // ca.dnamobile.javalauncher.launcher.JavaGameLauncher.RawJavaProgressListener
                public final void onProgress(int i, String str3) {
                    ForgeInstaller.notify(installProgressListener, i, str3);
                }
            });
        }
        if (iLaunchRawJavaArgsWithProgress != 0) {
            throw new IllegalStateException("Forge installer exited with code " + iLaunchRawJavaArgsWithProgress + ". Check latestlog.txt for the Forge installer command-line output.");
        }
        String strResolveInstalledForgeVersionId = resolveInstalledForgeVersionId(strCreateUniqueForgeVersionId, id, strSubstring, strResolveFullForgeVersion);
        ensureForgeVersionLibrariesDownloaded(strResolveInstalledForgeVersionId, installProgressListener);
        flattenInheritedProfileIfEnabled(context, strResolveInstalledForgeVersionId, installProgressListener);
        notify(installProgressListener, 100, "Forge " + strSubstring + " installed.");
        return new InstallResult(id, strSubstring, strResolveFullForgeVersion, strResolveInstalledForgeVersionId);
    }

    /* JADX WARN: Removed duplicated region for block: B:29:0x00a6  */
    /* JADX WARN: Removed duplicated region for block: B:30:0x00bf  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private static void ensureForgeVersionLibrariesDownloaded(java.lang.String r13, final ca.dnamobile.javalauncher.ui.version.MinecraftVersionInstaller.InstallProgressListener r14) throws java.lang.Exception {
        /*
            Method dump skipped, instruction units count: 311
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: ca.dnamobile.javalauncher.ui.version.ForgeInstaller.ensureForgeVersionLibrariesDownloaded(java.lang.String, ca.dnamobile.javalauncher.ui.version.MinecraftVersionInstaller$InstallProgressListener):void");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void downloadLibraryWithFallbacks(LibraryDownload libraryDownload) throws Exception {
        Exception exc = null;
        for (String str : libraryDownload.urls) {
            try {
                downloadFileIfNeeded(libraryDownload.targetFile, str, libraryDownload.sha1);
                return;
            } catch (Exception e) {
                Logging.i(TAG, "Forge library download failed from " + str + ": " + e.getMessage());
                exc = e;
            }
        }
        throw new IllegalStateException("Unable to download Forge library " + libraryDownload.libraryName, exc);
    }

    private static String canonicalPathKey(File file) {
        try {
            return file.getCanonicalPath();
        } catch (Throwable unused) {
            return file.getAbsolutePath();
        }
    }

    private static String resolveLibraryArtifactPath(JSONObject jSONObject) {
        JSONObject jSONObjectOptJSONObject;
        JSONObject jSONObjectOptJSONObject2 = jSONObject.optJSONObject("downloads");
        if (jSONObjectOptJSONObject2 != null && (jSONObjectOptJSONObject = jSONObjectOptJSONObject2.optJSONObject("artifact")) != null) {
            String strOptString = jSONObjectOptJSONObject.optString("path", "");
            if (!strOptString.isEmpty()) {
                return strOptString;
            }
        }
        return artifactPathFromName(jSONObject.optString("name", ""));
    }

    private static String resolveLibrarySha1(JSONObject jSONObject) {
        JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject("downloads");
        JSONObject jSONObjectOptJSONObject2 = jSONObjectOptJSONObject != null ? jSONObjectOptJSONObject.optJSONObject("artifact") : null;
        String strOptString = jSONObjectOptJSONObject2 != null ? jSONObjectOptJSONObject2.optString("sha1", "") : "";
        if (strOptString == null || strOptString.trim().isEmpty()) {
            return null;
        }
        return strOptString.trim();
    }

    private static ArrayList<String> resolveLibraryDownloadUrls(JSONObject jSONObject, String str) {
        String strOptString;
        ArrayList<String> arrayList = new ArrayList<>();
        JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject("downloads");
        JSONObject jSONObjectOptJSONObject2 = jSONObjectOptJSONObject != null ? jSONObjectOptJSONObject.optJSONObject("artifact") : null;
        if (jSONObjectOptJSONObject2 != null && (strOptString = jSONObjectOptJSONObject2.optString("url", "")) != null && !strOptString.trim().isEmpty()) {
            addUrlIfMissing(arrayList, strOptString.trim());
        }
        String strOptString2 = jSONObject.optString("url", "");
        if (strOptString2 != null && !strOptString2.trim().isEmpty()) {
            addUrlIfMissing(arrayList, joinRepoAndPath(strOptString2.trim(), str));
        }
        addUrlIfMissing(arrayList, joinRepoAndPath("https://maven.minecraftforge.net/", str));
        addUrlIfMissing(arrayList, joinRepoAndPath("https://repo1.maven.org/maven2/", str));
        addUrlIfMissing(arrayList, joinRepoAndPath("https://libraries.minecraft.net/", str));
        return arrayList;
    }

    private static void addUrlIfMissing(ArrayList<String> arrayList, String str) {
        if (arrayList.contains(str)) {
            return;
        }
        arrayList.add(str);
    }

    private static String joinRepoAndPath(String str, String str2) {
        if (str.startsWith("http://")) {
            str = "https://" + str.substring("http://".length());
        }
        if (!str.endsWith("/")) {
            str = str + "/";
        }
        return str + str2;
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

    private static void ensureVanillaBaseIsValid(Context context, MinecraftVersion minecraftVersion, MinecraftVersionInstaller.InstallProgressListener installProgressListener) throws Exception {
        String id = minecraftVersion.getId();
        File versionDirectory = MinecraftVersionInstaller.getVersionDirectory(id);
        File file = new File(versionDirectory, id + ".json");
        File file2 = new File(versionDirectory, id + ".jar");
        if (file.isFile() && file2.isFile() && isVanillaClientJarValid(file, file2)) {
            return;
        }
        if (versionDirectory.exists()) {
            notify(installProgressListener, 10, "Reinstalling clean vanilla " + id + " for Forge...");
            deleteRecursively(versionDirectory);
        }
        MinecraftVersionInstaller.installVanillaVersion(context, minecraftVersion, installProgressListener);
        File file3 = new File(versionDirectory, id + ".json");
        File file4 = new File(versionDirectory, id + ".jar");
        if (!file3.isFile() || !file4.isFile()) {
            throw new IllegalStateException("Vanilla " + id + " did not install correctly.");
        }
        if (!isVanillaClientJarValid(file3, file4)) {
            throw new IllegalStateException("Vanilla " + id + " client jar hash is still invalid after reinstall: " + file4.getAbsolutePath());
        }
    }

    private static boolean isVanillaClientJarValid(File file, File file2) {
        try {
            JSONObject jSONObjectOptJSONObject = new JSONObject(readString(file)).optJSONObject("downloads");
            JSONObject jSONObjectOptJSONObject2 = jSONObjectOptJSONObject != null ? jSONObjectOptJSONObject.optJSONObject("client") : null;
            String strOptString = jSONObjectOptJSONObject2 != null ? jSONObjectOptJSONObject2.optString("sha1", "") : "";
            if (strOptString != null && !strOptString.trim().isEmpty()) {
                String strSha1 = sha1(file2);
                boolean zEqualsIgnoreCase = strOptString.equalsIgnoreCase(strSha1);
                if (!zEqualsIgnoreCase) {
                    Logging.i(TAG, "Invalid vanilla client jar: " + file2.getAbsolutePath() + " expected=" + strOptString + " actual=" + strSha1);
                }
                return zEqualsIgnoreCase;
            }
            return true;
        } catch (Throwable th) {
            Logging.e(TAG, "Unable to validate vanilla client jar for Forge", th);
            return false;
        }
    }

    private static void cleanupForgeGeneratedOutputs(String str, String str2, String str3) {
        File[] fileArrListFiles;
        File[] fileArrListFiles2 = new File(MinecraftVersionInstaller.getLibrariesDirectory(), "net/minecraft/client").listFiles();
        if (fileArrListFiles2 != null) {
            for (File file : fileArrListFiles2) {
                if (file.getName().startsWith(str + "-")) {
                    Logging.i(TAG, "Deleting stale Forge generated client output: " + file.getAbsolutePath());
                    deleteRecursively(file);
                }
            }
        }
        File versionDirectory = MinecraftVersionInstaller.getVersionDirectory(str2);
        if (versionDirectory.exists()) {
            Logging.i(TAG, "Deleting stale Forge version dir: " + versionDirectory.getAbsolutePath());
            deleteRecursively(versionDirectory);
        }
        File file2 = new File(MinecraftVersionInstaller.getLibrariesDirectory(), "net/minecraftforge/forge/" + str3);
        if (file2.isDirectory() && (fileArrListFiles = file2.listFiles()) != null && fileArrListFiles.length == 0) {
            deleteRecursively(file2);
        }
    }

    private static void deleteRecursively(File file) {
        File[] fileArrListFiles;
        if (file.exists()) {
            if (file.isDirectory() && (fileArrListFiles = file.listFiles()) != null) {
                for (File file2 : fileArrListFiles) {
                    deleteRecursively(file2);
                }
            }
            if (file.delete() || !file.exists()) {
                return;
            }
            Logging.i(TAG, "Unable to delete: " + file.getAbsolutePath());
        }
    }

    private static void flattenInheritedProfileIfEnabled(Context context, String str, MinecraftVersionInstaller.InstallProgressListener installProgressListener) throws Exception {
        if (LauncherPreferences.isRemoveInheritedVanillaAfterLoaderInstall(context)) {
            notify(installProgressListener, 96, "Flattening loader profile...");
            if (InheritedVersionFlattener.flattenInstalledVersionProfile(context, str).flattened) {
                InheritedVersionFlattener.ParentDeleteResult parentDeleteResultDeleteFlattenedParentVersionIfSafe = InheritedVersionFlattener.deleteFlattenedParentVersionIfSafe(context, str);
                Logging.i(TAG, parentDeleteResultDeleteFlattenedParentVersionIfSafe.message);
                if (!parentDeleteResultDeleteFlattenedParentVersionIfSafe.deleted || parentDeleteResultDeleteFlattenedParentVersionIfSafe.parentVersionId == null) {
                    return;
                }
                notify(installProgressListener, 98, "Removed inherited vanilla files: " + parentDeleteResultDeleteFlattenedParentVersionIfSafe.parentVersionId);
            }
        }
    }

    public static String inferLoaderNameFromVersionId(String str) {
        return str.toLowerCase(Locale.ROOT).contains("forge") ? "Forge" : "Vanilla";
    }

    private static String resolveFullForgeVersion(String str, String str2) throws Exception {
        ArrayList<String> arrayListDownloadForgeVersions = downloadForgeVersions();
        if (str2 != null && !str2.trim().isEmpty()) {
            String strNormalizeRequestedForgeVersion = normalizeRequestedForgeVersion(str, str2);
            if (!strNormalizeRequestedForgeVersion.startsWith(str + "-")) {
                strNormalizeRequestedForgeVersion = str + "-" + strNormalizeRequestedForgeVersion;
            }
            for (String str3 : arrayListDownloadForgeVersions) {
                if (str3.equals(strNormalizeRequestedForgeVersion)) {
                    return str3;
                }
            }
            String str4 = strNormalizeRequestedForgeVersion + ".";
            ArrayList arrayList = new ArrayList();
            for (String str5 : arrayListDownloadForgeVersions) {
                if (str5.startsWith(str4)) {
                    arrayList.add(str5);
                }
            }
            if (!arrayList.isEmpty()) {
                final String str6 = str + "-";
                arrayList.sort(new Comparator() { // from class: ca.dnamobile.javalauncher.ui.version.ForgeInstaller$$ExternalSyntheticLambda4
                    @Override // java.util.Comparator
                    public final int compare(Object obj, Object obj2) {
                        String str7 = str6;
                        return ForgeInstaller.compareForgeBuilds(((String) obj2).substring(str7.length()), ((String) obj).substring(str7.length()));
                    }
                });
                return (String) arrayList.get(0);
            }
            throw new IllegalStateException("Forge " + str2 + " was not found for " + str);
        }
        ArrayList arrayList2 = new ArrayList();
        final String str7 = str + "-";
        for (String str8 : arrayListDownloadForgeVersions) {
            if (str8.startsWith(str7)) {
                arrayList2.add(str8);
            }
        }
        if (arrayList2.isEmpty()) {
            throw new IllegalStateException("No Forge versions found for " + str);
        }
        arrayList2.sort(new Comparator() { // from class: ca.dnamobile.javalauncher.ui.version.ForgeInstaller$$ExternalSyntheticLambda5
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                String str9 = str7;
                return ForgeInstaller.compareForgeBuilds(((String) obj2).substring(str9.length()), ((String) obj).substring(str9.length()));
            }
        });
        return (String) arrayList2.get(0);
    }

    private static String normalizeRequestedForgeVersion(String str, String str2) {
        int i;
        String strTrim = str2.trim();
        int iLastIndexOf = strTrim.lastIndexOf(58);
        if (iLastIndexOf >= 0 && (i = iLastIndexOf + 1) < strTrim.length()) {
            strTrim = strTrim.substring(i).trim();
        }
        if (strTrim.toLowerCase(Locale.ROOT).startsWith("forge-")) {
            strTrim = strTrim.substring("forge-".length()).trim();
        }
        if (strTrim.startsWith(str + "-")) {
            return strTrim;
        }
        Matcher matcher = Pattern.compile("^1\\.\\d+(?:\\.\\d+)?-(.+)$").matcher(strTrim);
        return matcher.matches() ? matcher.group(1).trim() : strTrim;
    }

    private static ArrayList<String> downloadForgeVersions() throws Exception {
        String strDownloadText = downloadText(FORGE_METADATA_URL);
        ArrayList<String> arrayList = new ArrayList<>();
        Matcher matcher = Pattern.compile("<version>([^<]+)</version>").matcher(strDownloadText);
        while (matcher.find()) {
            String strGroup = matcher.group(1);
            if (strGroup != null && !strGroup.trim().isEmpty()) {
                arrayList.add(strGroup.trim());
            }
        }
        return arrayList;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int compareForgeBuilds(String str, String str2) {
        int[] buildParts = parseBuildParts(str);
        int[] buildParts2 = parseBuildParts(str2);
        int iMax = Math.max(buildParts.length, buildParts2.length);
        int i = 0;
        while (i < iMax) {
            int i2 = i < buildParts.length ? buildParts[i] : 0;
            int i3 = i < buildParts2.length ? buildParts2[i] : 0;
            if (i2 != i3) {
                return Integer.compare(i2, i3);
            }
            i++;
        }
        return str.compareToIgnoreCase(str2);
    }

    private static int[] parseBuildParts(String str) {
        String[] strArrSplit = str.trim().split("[.-]");
        ArrayList arrayList = new ArrayList();
        for (String str2 : strArrSplit) {
            try {
                arrayList.add(Integer.valueOf(Integer.parseInt(str2)));
            } catch (NumberFormatException unused) {
            }
        }
        int[] iArr = new int[arrayList.size()];
        for (int i = 0; i < arrayList.size(); i++) {
            iArr[i] = ((Integer) arrayList.get(i)).intValue();
        }
        return iArr;
    }

    private static String getInstallerUrl(String str) {
        return String.format(Locale.ROOT, FORGE_INSTALLER_URL, str);
    }

    private static String createUniqueForgeVersionId(String str, String str2) {
        String strSanitizeVersionId = sanitizeVersionId(str);
        if (strSanitizeVersionId.isEmpty()) {
            strSanitizeVersionId = sanitizeVersionId(str2 + " Forge");
        }
        if (strSanitizeVersionId.isEmpty()) {
            strSanitizeVersionId = "forge-" + UUID.randomUUID();
        }
        File versionsDirectory = MinecraftVersionInstaller.getVersionsDirectory();
        if (!new File(versionsDirectory, strSanitizeVersionId).exists()) {
            return strSanitizeVersionId;
        }
        for (int i = 2; i < 1000; i++) {
            String str3 = strSanitizeVersionId + "-" + i;
            if (!new File(versionsDirectory, str3).exists()) {
                return str3;
            }
        }
        return strSanitizeVersionId + "-" + UUID.randomUUID();
    }

    private static String sanitizeVersionId(String str) {
        return str.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._ -]", "").replace(' ', '-').replaceAll("-+", "-").replaceAll("^-|-$", "");
    }

    private static void patchForgeInstallerProfile(File file, String str) throws Exception {
        File file2 = new File(file.getParentFile(), file.getName() + ".tmp");
        File file3 = new File(file.getParentFile(), "install_profile.json");
        if (file2.exists()) {
            file2.delete();
        }
        ZipFile zipFile = new ZipFile(file);
        try {
            ZipEntry entry = zipFile.getEntry("install_profile.json");
            if (entry == null) {
                throw new IllegalStateException("install_profile.json not found in " + file.getName());
            }
            InputStream inputStream = zipFile.getInputStream(entry);
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(file3);
                try {
                    copy(inputStream, fileOutputStream);
                    fileOutputStream.close();
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    zipFile.close();
                    JSONObject jSONObject = new JSONObject(readString(file3));
                    if (jSONObject.has("spec")) {
                        if (!jSONObject.has("version")) {
                            throw new IllegalStateException("Unable to find Forge install_profile version key.");
                        }
                        jSONObject.put("version", str);
                    } else {
                        JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject("install");
                        if (jSONObjectOptJSONObject == null) {
                            throw new IllegalStateException("Unable to find Forge install_profile install block.");
                        }
                        jSONObjectOptJSONObject.put(TypedValues.AttributesType.S_TARGET, str);
                        jSONObject.put("install", jSONObjectOptJSONObject);
                    }
                    relaxGeneratedClientOutputHashes(jSONObject);
                    writeString(file3, jSONObject.toString());
                    zipFile = new ZipFile(file);
                    try {
                        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(file2));
                        try {
                            Enumeration<? extends ZipEntry> enumerationEntries = zipFile.entries();
                            while (enumerationEntries.hasMoreElements()) {
                                ZipEntry zipEntryNextElement = enumerationEntries.nextElement();
                                if (!shouldSkipSignatureFile(zipEntryNextElement.getName())) {
                                    zipOutputStream.putNextEntry(new ZipEntry(zipEntryNextElement.getName()));
                                    if (!zipEntryNextElement.isDirectory()) {
                                        if ("install_profile.json".equals(zipEntryNextElement.getName())) {
                                            FileInputStream fileInputStream = new FileInputStream(file3);
                                            try {
                                                copy(fileInputStream, zipOutputStream);
                                                fileInputStream.close();
                                            } finally {
                                            }
                                        } else {
                                            InputStream inputStream2 = zipFile.getInputStream(zipEntryNextElement);
                                            try {
                                                copy(inputStream2, zipOutputStream);
                                                if (inputStream2 != null) {
                                                    inputStream2.close();
                                                }
                                            } finally {
                                            }
                                        }
                                    }
                                    zipOutputStream.closeEntry();
                                }
                            }
                            zipOutputStream.close();
                            zipFile.close();
                            if (!file.delete()) {
                                throw new IllegalStateException("Unable to replace Forge installer jar.");
                            }
                            if (!file2.renameTo(file)) {
                                throw new IllegalStateException("Unable to move patched Forge installer jar.");
                            }
                            file3.delete();
                        } finally {
                        }
                    } finally {
                    }
                } finally {
                }
            } finally {
            }
        } finally {
        }
    }

    private static void relaxGeneratedClientOutputHashes(JSONObject jSONObject) {
        JSONObject jSONObjectOptJSONObject;
        JSONArray jSONArrayNames;
        JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("processors");
        if (jSONArrayOptJSONArray == null) {
            return;
        }
        for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
            JSONObject jSONObjectOptJSONObject2 = jSONArrayOptJSONArray.optJSONObject(i);
            if (jSONObjectOptJSONObject2 != null && (jSONObjectOptJSONObject = jSONObjectOptJSONObject2.optJSONObject("outputs")) != null && (jSONArrayNames = jSONObjectOptJSONObject.names()) != null) {
                for (int length = jSONArrayNames.length() - 1; length >= 0; length--) {
                    String strOptString = jSONArrayNames.optString(length, "");
                    String lowerCase = strOptString.toLowerCase(Locale.ROOT);
                    if (lowerCase.contains("mc_slim") || lowerCase.contains("mc_extra") || lowerCase.contains("-slim.jar") || lowerCase.contains("-extra.jar") || lowerCase.contains(":slim") || lowerCase.contains(":extra")) {
                        Logging.i(TAG, "Relaxing Forge generated output hash check for " + strOptString);
                        jSONObjectOptJSONObject.remove(strOptString);
                    }
                }
                if (jSONObjectOptJSONObject.length() == 0) {
                    jSONObjectOptJSONObject2.remove("outputs");
                }
            }
        }
    }

    private static boolean shouldSkipSignatureFile(String str) {
        String upperCase = str.toUpperCase(Locale.ROOT);
        return upperCase.startsWith("META-INF/") && (upperCase.endsWith(".SF") || upperCase.endsWith(".RSA") || upperCase.endsWith(".DSA") || upperCase.endsWith(".EC"));
    }

    private static void generateLauncherProfiles() throws Exception {
        File file = new File(PathManager.DIR_MINECRAFT_HOME, "launcher_profiles.json");
        if (file.isFile()) {
            return;
        }
        JSONObject jSONObject = new JSONObject();
        JSONObject jSONObject2 = new JSONObject();
        JSONObject jSONObject3 = new JSONObject();
        jSONObject3.put("lastVersionId", "latest-release");
        jSONObject2.put(StorageLocationStore.DEFAULT_LOCATION_ID, jSONObject3);
        jSONObject.put("profiles", jSONObject2);
        jSONObject.put("selectedProfile", StorageLocationStore.DEFAULT_LOCATION_ID);
        writeString(file, jSONObject.toString());
    }

    private static String resolveInstalledForgeVersionId(String str, String str2, String str3, String str4) {
        if (isInstalledVersion(str)) {
            return str;
        }
        String[] strArr = {str2 + "-forge-" + str3, str2 + "-forge" + str4, str4};
        for (int i = 0; i < 3; i++) {
            String str5 = strArr[i];
            if (isInstalledVersion(str5)) {
                return str5;
            }
        }
        throw new IllegalStateException("Forge installer finished, but no Forge version JSON was found for " + str);
    }

    private static boolean isInstalledVersion(String str) {
        return new File(MinecraftVersionInstaller.getVersionDirectory(str), str + ".json").isFile();
    }

    private static File resolveInstallerRuntime(String str, String str2) {
        String[] strArr;
        if (isLegacyJava8ForgeVersion(str, str2)) {
            strArr = new String[]{"Internal-8", "Internal-17", "Internal-21", "Internal-25"};
        } else if (isModernJavaMinecraftVersion(str)) {
            strArr = new String[]{"Internal-21", "Internal-25", "Internal-17", "Internal-8"};
        } else {
            strArr = new String[]{"Internal-17", "Internal-21", "Internal-25", "Internal-8"};
        }
        for (String str3 : strArr) {
            File runtimeDir = MultiRTUtils.getRuntimeDir(str3);
            if (runtimeDir.isDirectory() && new File(runtimeDir, "bin/java").isFile()) {
                Logging.i(TAG, "Using " + str3 + " for Forge installer " + str + " / " + str2);
                return runtimeDir;
            }
        }
        throw new IllegalStateException("No internal Java runtime is installed for Forge installer.");
    }

    private static boolean isLegacyJava8ForgeVersion(String str, String str2) {
        String[] strArrSplit = str.split("\\.");
        try {
            if (strArrSplit.length >= 2 && "1".equals(strArrSplit[0])) {
                if (Integer.parseInt(strArrSplit[1]) <= 16) {
                    return true;
                }
            }
        } catch (NumberFormatException unused) {
        }
        String strTrim = str2.trim();
        return strTrim.startsWith("36.") || strTrim.startsWith("35.") || strTrim.startsWith("34.") || strTrim.startsWith("33.") || strTrim.startsWith("32.") || strTrim.startsWith("31.") || strTrim.startsWith("30.") || strTrim.startsWith("14.");
    }

    private static boolean isModernJavaMinecraftVersion(String str) {
        String[] strArrSplit = str.split("\\.");
        try {
            if (strArrSplit.length == 0) {
                return false;
            }
            int i = Integer.parseInt(strArrSplit[0]);
            if (i >= 26) {
                return true;
            }
            if (i == 1 && strArrSplit.length >= 2) {
                int i2 = Integer.parseInt(strArrSplit[1]);
                return i2 > 20 || (i2 == 20 && (strArrSplit.length > 2 ? Integer.parseInt(strArrSplit[2]) : 0) >= 5);
            }
            return false;
        } catch (NumberFormatException unused) {
            return false;
        }
    }

    private static void downloadFileIfNeeded(File file, String str, String str2) throws Exception {
        if (file.isFile()) {
            if (str2 == null || sha1(file).equalsIgnoreCase(str2)) {
                return;
            } else {
                Logging.i(TAG, "Existing Forge file hash mismatch, redownloading: " + file.getAbsolutePath());
            }
        }
        File parentFile = file.getParentFile();
        if (parentFile != null) {
            ensureDirectory(parentFile);
        }
        File file2 = new File(file.getAbsolutePath() + ".part");
        try {
            downloadToFile(str, file2);
            if (str2 != null && !sha1(file2).equalsIgnoreCase(str2)) {
                throw new IllegalStateException("SHA-1 mismatch for " + file.getName());
            }
            if (file.exists() && !file.delete()) {
                throw new IllegalStateException("Unable to replace " + file.getAbsolutePath());
            }
            if (file2.renameTo(file)) {
                return;
            }
            FileInputStream fileInputStream = new FileInputStream(file2);
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                try {
                    copy(fileInputStream, fileOutputStream);
                    fileOutputStream.close();
                    fileInputStream.close();
                    file2.delete();
                } finally {
                }
            } finally {
            }
        } catch (Exception e) {
            file2.delete();
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
                    copy(inputStream, fileOutputStream);
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

    private static String readStream(InputStream inputStream) throws Exception {
        if (inputStream == null) {
            return "";
        }
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try {
                copy(inputStream, byteArrayOutputStream);
                String string = byteArrayOutputStream.toString(StandardCharsets.UTF_8.name());
                byteArrayOutputStream.close();
                if (inputStream != null) {
                    inputStream.close();
                }
                return string;
            } finally {
            }
        } catch (Throwable th) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    private static String readString(File file) throws Exception {
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try {
                copy(fileInputStream, byteArrayOutputStream);
                String string = byteArrayOutputStream.toString(StandardCharsets.UTF_8.name());
                byteArrayOutputStream.close();
                fileInputStream.close();
                return string;
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

    private static void copy(InputStream inputStream, OutputStream outputStream) throws Exception {
        byte[] bArr = new byte[65536];
        while (true) {
            int i = inputStream.read(bArr);
            if (i == -1) {
                return;
            } else {
                outputStream.write(bArr, 0, i);
            }
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

    /* JADX INFO: Access modifiers changed from: private */
    static final class LibraryDownload {
        final String displayName;
        final String libraryName;
        final String sha1;
        final File targetFile;
        final ArrayList<String> urls;

        private LibraryDownload(File file, String str, ArrayList<String> arrayList, String str2, String str3) {
            this.targetFile = file;
            this.sha1 = str;
            this.urls = arrayList;
            this.displayName = str2;
            this.libraryName = str3;
        }
    }

    private static void ensureActivePathManager(Context context) {
        if (PathManager.DIR_MINECRAFT_HOME == null || PathManager.DIR_MINECRAFT_HOME.trim().isEmpty()) {
            PathManager.initContextConstants(context);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void notify(MinecraftVersionInstaller.InstallProgressListener installProgressListener, int i, String str) {
        if (installProgressListener != null) {
            installProgressListener.onProgress(Math.max(0, Math.min(100, i)), str);
        }
    }
}
