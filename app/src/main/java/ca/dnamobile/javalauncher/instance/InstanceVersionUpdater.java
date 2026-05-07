package ca.dnamobile.javalauncher.instance;

import android.content.Context;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import ca.dnamobile.javalauncher.BuildConfig;
import ca.dnamobile.javalauncher.feature.log.Logging;
import ca.dnamobile.javalauncher.instance.InstanceVersionUpdater;
import ca.dnamobile.javalauncher.ui.version.MinecraftVersionInstaller;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class InstanceVersionUpdater {
    private static final String FABRIC_META = "https://meta.fabricmc.net/v2";
    private static final String FORGE_MAVEN = "https://maven.minecraftforge.net";
    private static final String MOJANG_VERSION_MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    private static final String NEOFORGE_MAVEN = "https://maven.neoforged.net/releases";
    private static final String TAG = "InstanceVersionUpdater";

    public interface Listener {
        void onProgress(int i, int i2);

        void onStatus(String str);
    }

    private InstanceVersionUpdater() {
    }

    public enum LoaderKind {
        VANILLA("Vanilla"),
        FABRIC("Fabric"),
        FORGE("Forge"),
        NEOFORGE("NeoForge");

        public final String displayName;

        LoaderKind(String str) {
            this.displayName = str;
        }
    }

    public static final class MinecraftRelease {
        public final String id;
        public final String url;

        MinecraftRelease(String str, String str2) {
            this.id = str;
            this.url = str2;
        }
    }

    public static final class LoaderVersion {
        public final String displayVersion;
        public final String installVersion;
        public final LoaderKind kind;
        public final String minecraftVersion;
        public final boolean stable;

        LoaderVersion(LoaderKind loaderKind, String str, String str2, String str3, boolean z) {
            this.kind = loaderKind;
            this.displayVersion = str;
            this.installVersion = str2;
            this.minecraftVersion = str3;
            this.stable = z;
        }

        public String getDisplayLabel() {
            return this.displayVersion + (shouldShowBetaLabel() ? " (beta)" : "");
        }

        public String getDisplayLabel(String str) {
            String displayLabel = getDisplayLabel();
            return (InstanceVersionUpdater.isSameLoaderVersion(this.displayVersion, str) || InstanceVersionUpdater.isSameLoaderVersion(this.installVersion, str)) ? displayLabel + " (current)" : displayLabel;
        }

        private boolean shouldShowBetaLabel() {
            if (this.stable) {
                return false;
            }
            if (this.kind == LoaderKind.FABRIC) {
                return InstanceVersionUpdater.isPreReleaseLoaderVersion(this.displayVersion) || InstanceVersionUpdater.isPreReleaseLoaderVersion(this.installVersion);
            }
            return true;
        }
    }

    public static final class UpdateResult {
        public final String baseVersionId;
        public final String loader;
        public final String loaderVersion;
        public final int metadataFilesUpdated;
        public final String minecraftVersionId;
        public final String versionType;

        UpdateResult(String str, String str2, String str3, String str4, String str5, int i) {
            this.loader = str;
            this.baseVersionId = str2;
            this.minecraftVersionId = str3;
            this.versionType = str4;
            this.loaderVersion = str5;
            this.metadataFilesUpdated = i;
        }
    }

    public static ArrayList<MinecraftRelease> fetchMinecraftReleases() throws Exception {
        JSONArray jSONArray = new JSONObject(httpGetText(MOJANG_VERSION_MANIFEST)).getJSONArray("versions");
        ArrayList<MinecraftRelease> arrayList = new ArrayList<>();
        for (int i = 0; i < jSONArray.length(); i++) {
            JSONObject jSONObject = jSONArray.getJSONObject(i);
            if (BuildConfig.BUILD_TYPE.equalsIgnoreCase(jSONObject.optString("type", ""))) {
                String strTrim = jSONObject.optString("id", "").trim();
                String strTrim2 = jSONObject.optString("url", "").trim();
                if (!strTrim.isEmpty() && !strTrim2.isEmpty()) {
                    arrayList.add(new MinecraftRelease(strTrim, strTrim2));
                }
            }
        }
        return arrayList;
    }

    public static ArrayList<LoaderVersion> fetchLoaderVersions(LoaderKind loaderKind, String str) throws Exception {
        int iOrdinal = loaderKind.ordinal();
        if (iOrdinal == 1) {
            return fetchFabricLoaderVersions(str);
        }
        if (iOrdinal == 2) {
            return fetchForgeLoaderVersions(str);
        }
        if (iOrdinal == 3) {
            return fetchNeoForgeLoaderVersions(str);
        }
        return new ArrayList<>();
    }

    public static LoaderVersion findLatestLoaderVersion(LoaderKind loaderKind, String str) throws Exception {
        ArrayList<LoaderVersion> arrayListFetchLoaderVersions = fetchLoaderVersions(loaderKind, str);
        if (arrayListFetchLoaderVersions.isEmpty()) {
            return null;
        }
        return arrayListFetchLoaderVersions.get(0);
    }

    public static String resolveCurrentLoaderVersion(LoaderKind loaderKind, String str, String str2) {
        String strExtractLoaderVersionFromVersionId = extractLoaderVersionFromVersionId(loaderKind, str, str2);
        if (!StringUtils.isBlank(strExtractLoaderVersionFromVersionId)) {
            return strExtractLoaderVersionFromVersionId;
        }
        String strExtractLoaderVersionFromVersionJson = extractLoaderVersionFromVersionJson(loaderKind, str, str2);
        if (StringUtils.isBlank(strExtractLoaderVersionFromVersionJson)) {
            return null;
        }
        return strExtractLoaderVersionFromVersionJson;
    }

    public static boolean isSameLoaderVersion(String str, String str2) {
        if (StringUtils.isBlank(str) || StringUtils.isBlank(str2)) {
            return false;
        }
        return normalizeLoaderVersionForCompare(str).equals(normalizeLoaderVersionForCompare(str2));
    }

    public static UpdateResult updateInstanceVersion(Context context, File file, File file2, String str, String str2, String str3, Listener listener) throws Exception {
        String str4;
        String strInstallLoaderProfile;
        LoaderKind loaderKindResolveLoaderKind = resolveLoaderKind(str2);
        if (loaderKindResolveLoaderKind == null) {
            loaderKindResolveLoaderKind = LoaderKind.VANILLA;
        }
        notify(listener, "Preparing Minecraft " + str3 + "...");
        notifyProgress(listener, 0, 4);
        ensureVanillaVersionInstalled(str3, listener);
        notifyProgress(listener, 1, 4);
        String str5 = loaderKindResolveLoaderKind.displayName;
        if (loaderKindResolveLoaderKind == LoaderKind.VANILLA) {
            str4 = null;
            strInstallLoaderProfile = str3;
        } else {
            notify(listener, "Finding latest " + loaderKindResolveLoaderKind.displayName + " loader for " + str3 + "...");
            LoaderVersion loaderVersionFindLatestLoaderVersion = findLatestLoaderVersion(loaderKindResolveLoaderKind, str3);
            if (loaderVersionFindLatestLoaderVersion == null) {
                throw new IllegalStateException("No " + loaderKindResolveLoaderKind.displayName + " loader found for Minecraft " + str3);
            }
            str4 = loaderVersionFindLatestLoaderVersion.displayVersion;
            notifyProgress(listener, 2, 4);
            strInstallLoaderProfile = installLoaderProfile(loaderVersionFindLatestLoaderVersion, listener);
        }
        notifyProgress(listener, 3, 4);
        int iPersistInstanceMetadataBestEffort = persistInstanceMetadataBestEffort(file, file2, str, str5, strInstallLoaderProfile, str3, BuildConfig.BUILD_TYPE);
        notifyProgress(listener, 4, 4);
        return new UpdateResult(str5, strInstallLoaderProfile, str3, BuildConfig.BUILD_TYPE, str4, iPersistInstanceMetadataBestEffort);
    }

    public static UpdateResult updateInstanceLoader(Context context, File file, File file2, String str, LoaderVersion loaderVersion, Listener listener) throws Exception {
        notify(listener, "Preparing Minecraft " + loaderVersion.minecraftVersion + "...");
        notifyProgress(listener, 0, 4);
        ensureVanillaVersionInstalled(loaderVersion.minecraftVersion, listener);
        notifyProgress(listener, 1, 4);
        notify(listener, "Installing " + loaderVersion.kind.displayName + " " + loaderVersion.displayVersion + "...");
        String strInstallLoaderProfile = installLoaderProfile(loaderVersion, listener);
        notifyProgress(listener, 3, 4);
        int iPersistInstanceMetadataBestEffort = persistInstanceMetadataBestEffort(file, file2, str, loaderVersion.kind.displayName, strInstallLoaderProfile, loaderVersion.minecraftVersion, BuildConfig.BUILD_TYPE);
        notifyProgress(listener, 4, 4);
        return new UpdateResult(loaderVersion.kind.displayName, strInstallLoaderProfile, loaderVersion.minecraftVersion, BuildConfig.BUILD_TYPE, loaderVersion.displayVersion, iPersistInstanceMetadataBestEffort);
    }

    public static LoaderKind resolveLoaderKind(String str) {
        if (str == null) {
            return null;
        }
        String strReplace = str.trim().toLowerCase(Locale.US).replace(" ", "").replace("_", "").replace("-", "");
        if (strReplace.isEmpty()) {
            return null;
        }
        if (strReplace.equals("vanilla") || strReplace.contains("vanilla")) {
            return LoaderKind.VANILLA;
        }
        if (strReplace.equals("neoforge") || strReplace.contains("neoforge")) {
            return LoaderKind.NEOFORGE;
        }
        if (strReplace.equals("fabric") || strReplace.contains("fabric")) {
            return LoaderKind.FABRIC;
        }
        if (strReplace.equals("forge") || strReplace.contains("forge")) {
            return LoaderKind.FORGE;
        }
        return null;
    }

    private static ArrayList<LoaderVersion> fetchFabricLoaderVersions(String str) throws Exception {
        JSONArray jSONArray = new JSONArray(httpGetText("https://meta.fabricmc.net/v2/versions/loader/" + urlEncodePath(str)));
        ArrayList<LoaderVersion> arrayList = new ArrayList<>();
        for (int i = 0; i < jSONArray.length(); i++) {
            JSONObject jSONObject = jSONArray.getJSONObject(i).getJSONObject("loader");
            String strTrim = jSONObject.optString("version", "").trim();
            if (!strTrim.isEmpty()) {
                arrayList.add(new LoaderVersion(LoaderKind.FABRIC, strTrim, strTrim, str, jSONObject.optBoolean("stable", true)));
            }
        }
        Collections.sort(arrayList, new Comparator() { // from class: ca.dnamobile.javalauncher.instance.InstanceVersionUpdater$$ExternalSyntheticLambda1
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                return InstanceVersionUpdater.lambda$fetchFabricLoaderVersions$0((InstanceVersionUpdater.LoaderVersion) obj, (InstanceVersionUpdater.LoaderVersion) obj2);
            }
        });
        return arrayList;
    }

    static /* synthetic */ int lambda$fetchFabricLoaderVersions$0(LoaderVersion loaderVersion, LoaderVersion loaderVersion2) {
        if (loaderVersion.stable != loaderVersion2.stable) {
            return loaderVersion.stable ? -1 : 1;
        }
        return -compareVersionStrings(loaderVersion.displayVersion, loaderVersion2.displayVersion);
    }

    private static ArrayList<LoaderVersion> fetchForgeLoaderVersions(String str) throws Exception {
        ArrayList<String> mavenVersions = parseMavenVersions(httpGetText("https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml"));
        ArrayList<LoaderVersion> arrayList = new ArrayList<>();
        String str2 = str + "-";
        for (String str3 : mavenVersions) {
            if (str3.startsWith(str2)) {
                arrayList.add(new LoaderVersion(LoaderKind.FORGE, str3.substring(str2.length()), str3, str, !isPreReleaseLoaderVersion(r6)));
            }
        }
        Collections.sort(arrayList, new Comparator() { // from class: ca.dnamobile.javalauncher.instance.InstanceVersionUpdater$$ExternalSyntheticLambda2
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                return InstanceVersionUpdater.lambda$fetchForgeLoaderVersions$1((InstanceVersionUpdater.LoaderVersion) obj, (InstanceVersionUpdater.LoaderVersion) obj2);
            }
        });
        return arrayList;
    }

    static /* synthetic */ int lambda$fetchForgeLoaderVersions$1(LoaderVersion loaderVersion, LoaderVersion loaderVersion2) {
        if (loaderVersion.stable != loaderVersion2.stable) {
            return loaderVersion.stable ? -1 : 1;
        }
        return -compareVersionStrings(loaderVersion.displayVersion, loaderVersion2.displayVersion);
    }

    private static ArrayList<LoaderVersion> fetchNeoForgeLoaderVersions(String str) throws Exception {
        ArrayList<LoaderVersion> arrayList = new ArrayList<>();
        if ("1.20.1".equals(str)) {
            String str2 = str + "-";
            for (String str3 : parseMavenVersions(httpGetText("https://maven.neoforged.net/releases/net/neoforged/forge/maven-metadata.xml"))) {
                if (str3.startsWith(str2)) {
                    arrayList.add(new LoaderVersion(LoaderKind.NEOFORGE, str3.substring(str2.length()), str3, str, !isPreReleaseLoaderVersion(r6)));
                }
            }
        }
        for (String str4 : parseMavenVersions(httpGetText("https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml"))) {
            if (str.equals(formatNeoForgeGameVersion(str4))) {
                arrayList.add(new LoaderVersion(LoaderKind.NEOFORGE, str4, str4, str, !isPreReleaseLoaderVersion(str4)));
            }
        }
        Collections.sort(arrayList, new Comparator() { // from class: ca.dnamobile.javalauncher.instance.InstanceVersionUpdater$$ExternalSyntheticLambda0
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                return InstanceVersionUpdater.lambda$fetchNeoForgeLoaderVersions$2((InstanceVersionUpdater.LoaderVersion) obj, (InstanceVersionUpdater.LoaderVersion) obj2);
            }
        });
        return arrayList;
    }

    static /* synthetic */ int lambda$fetchNeoForgeLoaderVersions$2(LoaderVersion loaderVersion, LoaderVersion loaderVersion2) {
        if (loaderVersion.stable != loaderVersion2.stable) {
            return loaderVersion.stable ? -1 : 1;
        }
        return -compareVersionStrings(loaderVersion.displayVersion, loaderVersion2.displayVersion);
    }

    private static String extractLoaderVersionFromVersionId(LoaderKind loaderKind, String str, String str2) {
        String strSubstring;
        String strSubstring2;
        if (StringUtils.isBlank(str)) {
            return null;
        }
        String strTrim = str.trim();
        String lowerCase = strTrim.toLowerCase(Locale.US);
        String str3 = "-" + str2;
        String str4 = str2 + "-";
        int iOrdinal = loaderKind.ordinal();
        if (iOrdinal == 1) {
            if (lowerCase.startsWith("fabric-loader-") && strTrim.endsWith(str3)) {
                return strTrim.substring("fabric-loader-".length(), strTrim.length() - str3.length());
            }
            Matcher matcher = Pattern.compile("fabric(?:[-_ ]?loader)?[-_ ]?([0-9][A-Za-z0-9._+\\-]*)[-_ ]?" + Pattern.quote(str2), 2).matcher(strTrim);
            if (matcher.find()) {
                return matcher.group(1);
            }
            return null;
        }
        if (iOrdinal == 2) {
            if (lowerCase.startsWith("forge-")) {
                strSubstring = strTrim.substring("forge-".length());
            } else {
                Matcher matcher2 = Pattern.compile("forge[-_ ]?" + Pattern.quote(str2) + "[-_ ]([0-9][A-Za-z0-9._+\\-]*)", 2).matcher(strTrim);
                if (matcher2.find()) {
                    return matcher2.group(1);
                }
                Matcher matcher3 = Pattern.compile(Pattern.quote(str2) + "[-_ ]forge[-_ ]([0-9][A-Za-z0-9._+\\-]*)", 2).matcher(strTrim);
                if (matcher3.find()) {
                    return matcher3.group(1);
                }
                strSubstring = null;
            }
            if (StringUtils.isBlank(strSubstring) || !strSubstring.startsWith(str4)) {
                return null;
            }
            return strSubstring.substring(str4.length());
        }
        if (iOrdinal != 3) {
            return null;
        }
        if (lowerCase.startsWith("neoforge-")) {
            strSubstring2 = strTrim.substring("neoforge-".length());
        } else {
            Matcher matcher4 = Pattern.compile("neoforge[-_ ]?" + Pattern.quote(str2) + "[-_ ]([0-9][A-Za-z0-9._+\\-]*)", 2).matcher(strTrim);
            if (matcher4.find()) {
                return matcher4.group(1);
            }
            Matcher matcher5 = Pattern.compile(Pattern.quote(str2) + "[-_ ]neoforge[-_ ]([0-9][A-Za-z0-9._+\\-]*)", 2).matcher(strTrim);
            if (matcher5.find()) {
                return matcher5.group(1);
            }
            strSubstring2 = null;
        }
        if (!StringUtils.isBlank(strSubstring2)) {
            if (strSubstring2.startsWith(str4)) {
                return strSubstring2.substring(str4.length());
            }
            if (strSubstring2.startsWith(str2 + "-")) {
                return strSubstring2.substring((str2 + "-").length());
            }
            if (strSubstring2.matches("^[0-9].*")) {
                return strSubstring2;
            }
        }
        return null;
    }

    private static String extractLoaderVersionFromVersionJson(LoaderKind loaderKind, String str, String str2) {
        JSONArray jSONArrayOptJSONArray;
        if (StringUtils.isBlank(str)) {
            return null;
        }
        try {
            File file = new File(getVersionDirectory(str.trim()), str.trim() + ".json");
            if (!file.isFile() || (jSONArrayOptJSONArray = new JSONObject(readText(file)).optJSONArray("libraries")) == null) {
                return null;
            }
            for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
                JSONObject jSONObjectOptJSONObject = jSONArrayOptJSONArray.optJSONObject(i);
                if (jSONObjectOptJSONObject != null) {
                    String strExtractLoaderVersionFromLibraryName = extractLoaderVersionFromLibraryName(loaderKind, jSONObjectOptJSONObject.optString("name", "").trim(), str2);
                    if (!StringUtils.isBlank(strExtractLoaderVersionFromLibraryName)) {
                        return strExtractLoaderVersionFromLibraryName;
                    }
                }
            }
        } catch (Throwable th) {
            Logging.e(TAG, "Unable to read current loader version from version JSON", th);
        }
        return null;
    }

    private static String extractLoaderVersionFromLibraryName(LoaderKind loaderKind, String str, String str2) {
        if (StringUtils.isBlank(str)) {
            return null;
        }
        String[] strArrSplit = str.split(":");
        if (strArrSplit.length < 3) {
            return null;
        }
        String lowerCase = strArrSplit[0].toLowerCase(Locale.US);
        String lowerCase2 = strArrSplit[1].toLowerCase(Locale.US);
        String str3 = strArrSplit[2];
        int iOrdinal = loaderKind.ordinal();
        if (iOrdinal == 1) {
            if ("net.fabricmc".equals(lowerCase) && "fabric-loader".equals(lowerCase2)) {
                return str3;
            }
            return null;
        }
        if (iOrdinal == 2) {
            if (!"net.minecraftforge".equals(lowerCase) || !"forge".equals(lowerCase2)) {
                return null;
            }
            String str4 = str2 + "-";
            return str3.startsWith(str4) ? str3.substring(str4.length()) : str3;
        }
        if (iOrdinal != 3) {
            return null;
        }
        if ("net.neoforged".equals(lowerCase) && "neoforge".equals(lowerCase2)) {
            return str3;
        }
        if (!"net.neoforged".equals(lowerCase) || !"forge".equals(lowerCase2)) {
            return null;
        }
        String str5 = str2 + "-";
        return str3.startsWith(str5) ? str3.substring(str5.length()) : str3;
    }

    private static String normalizeLoaderVersionForCompare(String str) {
        String strReplace = str.trim().toLowerCase(Locale.US).replace("_", "-").replace("+", "-");
        while (strReplace.contains("--")) {
            strReplace = strReplace.replace("--", "-");
        }
        return strReplace;
    }

    private static String installLoaderProfile(LoaderVersion loaderVersion, Listener listener) throws Exception {
        int iOrdinal = loaderVersion.kind.ordinal();
        if (iOrdinal == 1) {
            return installFabricProfile(loaderVersion, listener);
        }
        if (iOrdinal == 2) {
            return installForgeLikeProfile(loaderVersion, listener);
        }
        if (iOrdinal == 3) {
            return installNeoForgeProfile(loaderVersion, listener);
        }
        return loaderVersion.minecraftVersion;
    }

    private static String installFabricProfile(LoaderVersion loaderVersion, Listener listener) throws Exception {
        String str = "fabric-loader-" + loaderVersion.displayVersion + "-" + loaderVersion.minecraftVersion;
        notify(listener, "Downloading Fabric loader profile " + loaderVersion.displayVersion + "...");
        JSONObject jSONObject = new JSONObject(httpGetText("https://meta.fabricmc.net/v2/versions/loader/" + urlEncodePath(loaderVersion.minecraftVersion) + "/" + urlEncodePath(loaderVersion.installVersion) + "/profile/json"));
        jSONObject.put("id", str);
        jSONObject.put("type", BuildConfig.BUILD_TYPE);
        jSONObject.put("inheritsFrom", loaderVersion.minecraftVersion);
        writeVersionJson(str, jSONObject);
        notifyProgress(listener, 2, 4);
        return str;
    }

    private static String installForgeLikeProfile(LoaderVersion loaderVersion, Listener listener) throws Exception {
        String str = loaderVersion.installVersion;
        String str2 = "forge-" + str;
        File fileDownloadInstaller = downloadInstaller("https://maven.minecraftforge.net/net/minecraftforge/forge/" + urlEncodePath(str) + "/forge-" + urlEncodePath(str) + "-installer.jar", "forge-" + sanitizeFilePart(str) + "-installer.jar", listener);
        JSONObject forgeLikeVersionJsonFromInstaller = readForgeLikeVersionJsonFromInstaller(fileDownloadInstaller);
        normalizeLoaderVersionJson(forgeLikeVersionJsonFromInstaller, str2, loaderVersion.minecraftVersion);
        writeVersionJson(str2, forgeLikeVersionJsonFromInstaller);
        extractInstallerLibraries(fileDownloadInstaller, listener);
        notifyProgress(listener, 2, 4);
        return str2;
    }

    private static String installNeoForgeProfile(LoaderVersion loaderVersion, Listener listener) throws Exception {
        String str;
        String str2;
        String str3;
        boolean zStartsWith = loaderVersion.installVersion.startsWith(loaderVersion.minecraftVersion + "-");
        if (zStartsWith) {
            str = "neoforge-" + loaderVersion.installVersion;
        } else {
            str = "neoforge-" + loaderVersion.minecraftVersion + "-" + loaderVersion.installVersion;
        }
        if (zStartsWith) {
            str2 = "https://maven.neoforged.net/releases/net/neoforged/forge/" + urlEncodePath(loaderVersion.installVersion) + "/forge-" + urlEncodePath(loaderVersion.installVersion) + "-installer.jar";
            str3 = "neoforge-legacy-" + sanitizeFilePart(loaderVersion.installVersion) + "-installer.jar";
        } else {
            str2 = "https://maven.neoforged.net/releases/net/neoforged/neoforge/" + urlEncodePath(loaderVersion.installVersion) + "/neoforge-" + urlEncodePath(loaderVersion.installVersion) + "-installer.jar";
            str3 = "neoforge-" + sanitizeFilePart(loaderVersion.installVersion) + "-installer.jar";
        }
        File fileDownloadInstaller = downloadInstaller(str2, str3, listener);
        JSONObject forgeLikeVersionJsonFromInstaller = readForgeLikeVersionJsonFromInstaller(fileDownloadInstaller);
        normalizeLoaderVersionJson(forgeLikeVersionJsonFromInstaller, str, loaderVersion.minecraftVersion);
        writeVersionJson(str, forgeLikeVersionJsonFromInstaller);
        extractInstallerLibraries(fileDownloadInstaller, listener);
        notifyProgress(listener, 2, 4);
        return str;
    }

    private static void ensureVanillaVersionInstalled(String str, Listener listener) throws Exception {
        File versionDirectory = getVersionDirectory(str);
        File file = new File(versionDirectory, str + ".json");
        File file2 = new File(versionDirectory, str + ".jar");
        if (file.isFile() && file2.isFile()) {
            return;
        }
        notify(listener, "Finding Minecraft " + str + " metadata...");
        MinecraftRelease minecraftReleaseFindMinecraftRelease = findMinecraftRelease(str);
        if (minecraftReleaseFindMinecraftRelease == null) {
            throw new IllegalStateException("Minecraft release not found: " + str);
        }
        JSONObject jSONObject = new JSONObject(httpGetText(minecraftReleaseFindMinecraftRelease.url));
        if (!versionDirectory.exists() && !versionDirectory.mkdirs()) {
            throw new IOException("Unable to create version folder: " + versionDirectory.getAbsolutePath());
        }
        writeText(file, jSONObject.toString(2));
        if (file2.isFile()) {
            return;
        }
        JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject("downloads");
        JSONObject jSONObjectOptJSONObject2 = jSONObjectOptJSONObject == null ? null : jSONObjectOptJSONObject.optJSONObject("client");
        String strTrim = jSONObjectOptJSONObject2 != null ? jSONObjectOptJSONObject2.optString("url", "").trim() : "";
        if (strTrim.isEmpty()) {
            throw new IllegalStateException("Missing client jar URL for " + str);
        }
        notify(listener, "Downloading Minecraft " + str + " client...");
        downloadToFile(strTrim, file2, listener, 1, 4);
    }

    private static MinecraftRelease findMinecraftRelease(String str) throws Exception {
        for (MinecraftRelease minecraftRelease : fetchMinecraftReleases()) {
            if (str.equals(minecraftRelease.id)) {
                return minecraftRelease;
            }
        }
        return null;
    }

    private static JSONObject readForgeLikeVersionJsonFromInstaller(File file) throws Exception {
        JSONObject jSONObjectOptJSONObject;
        ZipFile zipFile = new ZipFile(file);
        try {
            ZipEntry entry = zipFile.getEntry("version.json");
            if (entry != null && !entry.isDirectory()) {
                JSONObject jSONObject = new JSONObject(readZipEntryText(zipFile, entry));
                zipFile.close();
                return jSONObject;
            }
            ZipEntry entry2 = zipFile.getEntry("install_profile.json");
            if (entry2 != null && !entry2.isDirectory() && (jSONObjectOptJSONObject = new JSONObject(readZipEntryText(zipFile, entry2)).optJSONObject("versionInfo")) != null) {
                zipFile.close();
                return jSONObjectOptJSONObject;
            }
            zipFile.close();
            throw new IllegalStateException("Installer does not contain version.json or install_profile.json versionInfo: " + file.getName());
        } catch (Throwable th) {
            try {
                zipFile.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    private static void normalizeLoaderVersionJson(JSONObject jSONObject, String str, String str2) throws Exception {
        jSONObject.put("id", str);
        jSONObject.put("type", BuildConfig.BUILD_TYPE);
        if (jSONObject.optString("inheritsFrom", "").trim().isEmpty()) {
            jSONObject.put("inheritsFrom", str2);
        }
    }

    private static void writeVersionJson(String str, JSONObject jSONObject) throws Exception {
        File versionDirectory = getVersionDirectory(str);
        if (!versionDirectory.exists() && !versionDirectory.mkdirs()) {
            throw new IOException("Unable to create version folder: " + versionDirectory.getAbsolutePath());
        }
        writeText(new File(versionDirectory, str + ".json"), jSONObject.toString(2));
    }

    private static void extractInstallerLibraries(File file, Listener listener) {
        String strSubstring;
        File librariesDirectory = getLibrariesDirectory();
        if (librariesDirectory.exists() || librariesDirectory.mkdirs()) {
            try {
                ZipFile zipFile = new ZipFile(file);
                try {
                    Enumeration<? extends ZipEntry> enumerationEntries = zipFile.entries();
                    int i = 0;
                    while (enumerationEntries.hasMoreElements()) {
                        ZipEntry zipEntryNextElement = enumerationEntries.nextElement();
                        if (!zipEntryNextElement.isDirectory()) {
                            String strReplace = zipEntryNextElement.getName().replace('\\', '/');
                            if (strReplace.startsWith("maven/")) {
                                strSubstring = strReplace.substring("maven/".length());
                            } else {
                                strSubstring = strReplace.startsWith("libraries/") ? strReplace.substring("libraries/".length()) : null;
                            }
                            if (strSubstring != null && !strSubstring.trim().isEmpty() && (strSubstring.endsWith(".jar") || strSubstring.endsWith(".pom") || strSubstring.endsWith(".json"))) {
                                File file2 = new File(librariesDirectory, strSubstring);
                                if (file2.getCanonicalFile().getPath().startsWith(librariesDirectory.getCanonicalFile().getPath() + File.separator) && (!file2.isFile() || file2.length() != zipEntryNextElement.getSize())) {
                                    File parentFile = file2.getParentFile();
                                    if (parentFile == null || parentFile.exists() || parentFile.mkdirs()) {
                                        InputStream inputStream = zipFile.getInputStream(zipEntryNextElement);
                                        try {
                                            FileOutputStream fileOutputStream = new FileOutputStream(file2);
                                            try {
                                                copy(inputStream, fileOutputStream);
                                                i++;
                                                fileOutputStream.close();
                                                if (inputStream != null) {
                                                    inputStream.close();
                                                }
                                            } catch (Throwable th) {
                                                try {
                                                    fileOutputStream.close();
                                                } catch (Throwable th2) {
                                                    th.addSuppressed(th2);
                                                }
                                                throw th;
                                            }
                                        } finally {
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (i > 0) {
                        notify(listener, "Copied " + i + " embedded loader libraries...");
                    }
                    zipFile.close();
                } finally {
                }
            } catch (Throwable th3) {
                Logging.e(TAG, "Unable to extract embedded loader libraries from " + file.getName(), th3);
            }
        }
    }

    private static File downloadInstaller(String str, String str2, Listener listener) throws Exception {
        File file = new File(getMinecraftHomeDirectory(), "launcher_cache/loaders");
        if (!file.exists() && !file.mkdirs()) {
            throw new IOException("Unable to create loader cache: " + file.getAbsolutePath());
        }
        File file2 = new File(file, str2);
        if (file2.isFile() && file2.length() > 0) {
            return file2;
        }
        notify(listener, "Downloading loader installer...");
        downloadToFile(str, file2, listener, 1, 4);
        return file2;
    }

    private static int persistInstanceMetadataBestEffort(File file, File file2, String str, String str2, String str3, String str4, String str5) {
        HashSet hashSet = new HashSet();
        ArrayList<File> arrayList = new ArrayList();
        collectJsonCandidates(file, arrayList, hashSet, 2);
        collectJsonCandidates(file2, arrayList, hashSet, 1);
        int i = 0;
        for (File file3 : arrayList) {
            try {
                JSONObject jSONObject = new JSONObject(readText(file3));
                if (looksLikeInstanceMetadata(jSONObject, str)) {
                    updateMetadataObject(jSONObject, str, str2, str3, str4, str5, file, file2);
                    writeText(file3, jSONObject.toString(2));
                    i++;
                }
            } catch (Throwable unused) {
            }
        }
        if (i == 0) {
            try {
                File file4 = new File(file, "instance.json");
                JSONObject jSONObject2 = new JSONObject();
                updateMetadataObject(jSONObject2, str, str2, str3, str4, str5, file, file2);
                File parentFile = file4.getParentFile();
                if (parentFile != null && !parentFile.exists()) {
                    parentFile.mkdirs();
                }
                writeText(file4, jSONObject2.toString(2));
                return 1;
            } catch (Throwable th) {
                Logging.e(TAG, "Unable to write fallback instance metadata", th);
            }
        }
        return i;
    }

    private static boolean looksLikeInstanceMetadata(JSONObject jSONObject, String str) {
        if (!jSONObject.has("baseVersionId") && !jSONObject.has("minecraftVersionId") && !jSONObject.has("rootDirectory") && !jSONObject.has("gameDirectory") && !jSONObject.has("isIsolated") && !jSONObject.has("isolated")) {
            return false;
        }
        String strTrim = jSONObject.optString("name", jSONObject.optString("instanceName", "")).trim();
        return strTrim.isEmpty() || strTrim.equals(str);
    }

    private static void updateMetadataObject(JSONObject jSONObject, String str, String str2, String str3, String str4, String str5, File file, File file2) throws Exception {
        if (!jSONObject.has("id")) {
            jSONObject.put("id", str);
        }
        if (!jSONObject.has("name")) {
            jSONObject.put("name", str);
        }
        if (jSONObject.has("instanceName")) {
            jSONObject.put("instanceName", str);
        }
        jSONObject.put("loader", str2);
        jSONObject.put("baseVersionId", str3);
        jSONObject.put("minecraftVersionId", str4);
        jSONObject.put("versionType", str5);
        if (jSONObject.has("baseVersion")) {
            jSONObject.put("baseVersion", str3);
        }
        if (jSONObject.has("versionId")) {
            jSONObject.put("versionId", str3);
        }
        if (jSONObject.has("minecraftVersion")) {
            jSONObject.put("minecraftVersion", str4);
        }
        if (jSONObject.has("rootDirectory")) {
            jSONObject.put("rootDirectory", file.getAbsolutePath());
        }
        if (jSONObject.has("gameDirectory")) {
            jSONObject.put("gameDirectory", file2.getAbsolutePath());
        }
    }

    private static void collectJsonCandidates(File file, ArrayList<File> arrayList, Set<String> set, int i) {
        File[] fileArrListFiles;
        if (file == null || i < 0 || !file.isDirectory() || (fileArrListFiles = file.listFiles()) == null) {
            return;
        }
        for (File file2 : fileArrListFiles) {
            if (set.add(file2.getCanonicalPath())) {
                if (file2.isDirectory()) {
                    collectJsonCandidates(file2, arrayList, set, i - 1);
                } else if (file2.isFile() && file2.getName().toLowerCase(Locale.US).endsWith(".json")) {
                    arrayList.add(file2);
                }
            }
        }
    }

    private static ArrayList<String> parseMavenVersions(String str) {
        ArrayList<String> arrayList = new ArrayList<>();
        Matcher matcher = Pattern.compile("<version>([^<]+)</version>").matcher(str);
        while (matcher.find()) {
            String strTrim = matcher.group(1).trim();
            if (!strTrim.isEmpty()) {
                arrayList.add(strTrim);
            }
        }
        return arrayList;
    }

    private static String formatNeoForgeGameVersion(String str) {
        int i;
        String[] strArrSplit = str.split("-", 2)[0].split("\\.");
        return (strArrSplit.length >= 2 && (i = parseInt(strArrSplit[0], -1)) >= 0) ? i >= 26 ? strArrSplit.length >= 3 ? strArrSplit[0] + "." + strArrSplit[1] + "." + strArrSplit[2] : strArrSplit[0] + "." + strArrSplit[1] : i >= 20 ? "1." + strArrSplit[0] + "." + strArrSplit[1] : "" : "";
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isPreReleaseLoaderVersion(String str) {
        String lowerCase = str.toLowerCase(Locale.US);
        return lowerCase.contains("snapshot") || lowerCase.contains("beta") || lowerCase.contains("alpha") || lowerCase.contains("pre") || lowerCase.contains("rc");
    }

    private static int compareVersionStrings(String str, String str2) {
        int iCompareToIgnoreCase;
        String[] strArrSplit = str.split("[\\.\\-\\+_]");
        String[] strArrSplit2 = str2.split("[\\.\\-\\+_]");
        int iMax = Math.max(strArrSplit.length, strArrSplit2.length);
        int i = 0;
        while (i < iMax) {
            String str3 = i < strArrSplit.length ? strArrSplit[i] : "0";
            String str4 = i < strArrSplit2.length ? strArrSplit2[i] : "0";
            int i2 = parseInt(str3, Integer.MIN_VALUE);
            int i3 = parseInt(str4, Integer.MIN_VALUE);
            if (i2 != Integer.MIN_VALUE && i3 != Integer.MIN_VALUE) {
                iCompareToIgnoreCase = Integer.compare(i2, i3);
            } else {
                iCompareToIgnoreCase = str3.compareToIgnoreCase(str4);
            }
            if (iCompareToIgnoreCase != 0) {
                return iCompareToIgnoreCase;
            }
            i++;
        }
        return 0;
    }

    private static int parseInt(String str, int i) {
        if (str == null) {
            return i;
        }
        try {
            Matcher matcher = Pattern.compile("\\d+").matcher(str);
            return !matcher.find() ? i : Integer.parseInt(matcher.group());
        } catch (Throwable unused) {
            return i;
        }
    }

    private static File getVersionDirectory(String str) {
        return new File(MinecraftVersionInstaller.getVersionsDirectory(), str);
    }

    private static File getMinecraftHomeDirectory() {
        File versionsDirectory = MinecraftVersionInstaller.getVersionsDirectory();
        File parentFile = versionsDirectory.getParentFile();
        return parentFile == null ? versionsDirectory : parentFile;
    }

    private static File getLibrariesDirectory() {
        return new File(getMinecraftHomeDirectory(), "libraries");
    }

    private static String httpGetText(String str) throws Exception {
        HttpURLConnection httpURLConnectionOpenConnection = openConnection(str);
        try {
            InputStream inputStream = httpURLConnectionOpenConnection.getInputStream();
            try {
                String str2 = new String(readAllBytes(inputStream), StandardCharsets.UTF_8);
                if (inputStream != null) {
                    inputStream.close();
                }
                return str2;
            } finally {
            }
        } finally {
            httpURLConnectionOpenConnection.disconnect();
        }
    }

    private static void downloadToFile(String str, File file, Listener listener, int i, int i2) throws Exception {
        File parentFile = file.getParentFile();
        if (parentFile != null && !parentFile.exists() && !parentFile.mkdirs()) {
            throw new IOException("Unable to create folder: " + parentFile.getAbsolutePath());
        }
        File file2 = new File(file.getParentFile(), file.getName() + ".download");
        HttpURLConnection httpURLConnectionOpenConnection = openConnection(str);
        int contentLength = httpURLConnectionOpenConnection.getContentLength();
        try {
            InputStream inputStream = httpURLConnectionOpenConnection.getInputStream();
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(file2);
                try {
                    byte[] bArr = new byte[65536];
                    long j = 0;
                    while (true) {
                        int i3 = inputStream.read(bArr);
                        if (i3 == -1) {
                            break;
                        }
                        fileOutputStream.write(bArr, 0, i3);
                        j += (long) i3;
                        if (contentLength > 0 && listener != null) {
                            listener.onProgress(((int) Math.min(1L, j / Math.max(1L, contentLength))) + i, i2);
                        }
                    }
                    fileOutputStream.close();
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    httpURLConnectionOpenConnection.disconnect();
                    if (file.exists() && !file.delete()) {
                        throw new IOException("Unable to replace " + file.getAbsolutePath());
                    }
                    if (!file2.renameTo(file)) {
                        throw new IOException("Unable to move download into place: " + file.getAbsolutePath());
                    }
                } finally {
                }
            } finally {
            }
        } catch (Throwable th) {
            httpURLConnectionOpenConnection.disconnect();
            throw th;
        }
    }

    private static HttpURLConnection openConnection(String str) throws Exception {
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(str).openConnection();
        httpURLConnection.setConnectTimeout(AccessibilityNodeInfoCompat.EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_MAX_LENGTH);
        httpURLConnection.setReadTimeout(45000);
        httpURLConnection.setRequestProperty("User-Agent", "JavaLauncher Instance Updater");
        int responseCode = httpURLConnection.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            throw new IOException("HTTP " + responseCode + " for " + str);
        }
        return httpURLConnection;
    }

    private static String readZipEntryText(ZipFile zipFile, ZipEntry zipEntry) throws IOException {
        InputStream inputStream = zipFile.getInputStream(zipEntry);
        try {
            String str = new String(readAllBytes(inputStream), StandardCharsets.UTF_8);
            if (inputStream != null) {
                inputStream.close();
            }
            return str;
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

    private static String readText(File file) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            String str = new String(readAllBytes(fileInputStream), StandardCharsets.UTF_8);
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

    private static void writeText(File file, String str) throws IOException {
        File parentFile = file.getParentFile();
        if (parentFile != null && !parentFile.exists() && !parentFile.mkdirs()) {
            throw new IOException("Unable to create folder: " + parentFile.getAbsolutePath());
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

    private static byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        copy(inputStream, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    private static void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
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

    private static String urlEncodePath(String str) {
        return str.replace(" ", "%20").replace("+", "%2B");
    }

    private static String sanitizeFilePart(String str) {
        return str.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static void notify(Listener listener, String str) {
        if (listener != null) {
            listener.onStatus(str);
        }
    }

    private static void notifyProgress(Listener listener, int i, int i2) {
        if (listener != null) {
            listener.onProgress(i, i2);
        }
    }
}
