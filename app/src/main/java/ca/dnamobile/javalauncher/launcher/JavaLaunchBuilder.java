package ca.dnamobile.javalauncher.launcher;

import android.content.Context;
import android.os.Build;
import ca.dnamobile.javalauncher.BuildConfig;
import ca.dnamobile.javalauncher.data.AccountStore;
import ca.dnamobile.javalauncher.feature.log.Logging;
import ca.dnamobile.javalauncher.modcompat.ControllerModCompat;
import ca.dnamobile.javalauncher.renderer.DriverPluginManager;
import ca.dnamobile.javalauncher.renderer.RendererInterface;
import ca.dnamobile.javalauncher.settings.MemoryAllocationUtils;
import ca.dnamobile.javalauncher.skin.CustomSkinStore;
import ca.dnamobile.javalauncher.skin.OfflineSkinProfile;
import ca.dnamobile.javalauncher.skin.OfflineYggdrasilServer;
import ca.dnamobile.javalauncher.skin.SkinModelType;
import ca.dnamobile.javalauncher.ui.version.InheritedVersionFlattener;
import ca.dnamobile.javalauncher.ui.version.MinecraftVersionInstaller;
import ca.dnamobile.javalauncher.utils.path.LibPath;
import ca.dnamobile.javalauncher.utils.path.PathManager;
import com.google.android.material.internal.ViewUtils;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import net.kdt.pojavlaunch.Architecture;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class JavaLaunchBuilder {
    private static final String DEFAULT_MAIN_CLASS = "net.minecraft.client.main.Main";
    private static final int DEFAULT_MEMORY_MB = 2048;
    private static final String TAG = "JavaLaunchBuilder";
    private static OfflineYggdrasilServer activeOfflineSkinServer;
    private final AccountStore.Account account;
    private final Context context;
    private File gameDirectoryOverride;
    private final int height;
    private RendererInterface rendererOverride;
    private File runtimeDirectoryOverride;
    private final String versionId;
    private final int width;

    public JavaLaunchBuilder(Context context, String str, AccountStore.Account account, int i, int i2) {
        Context applicationContext = context.getApplicationContext();
        this.context = applicationContext;
        this.versionId = str;
        this.account = account == null ? loadActiveAccountFallback(applicationContext) : account;
        this.width = Math.max(1, i);
        this.height = Math.max(1, i2);
    }

    private static AccountStore.Account loadActiveAccountFallback(Context context) {
        try {
            return new AccountStore(context).load();
        } catch (Throwable th) {
            Logging.e(TAG, "Unable to load active account fallback", th);
            return null;
        }
    }

    public JavaLaunchBuilder setRuntimeDirectory(File file) {
        this.runtimeDirectoryOverride = file;
        return this;
    }

    public JavaLaunchBuilder setGameDirectory(File file) {
        this.gameDirectoryOverride = file;
        return this;
    }

    public JavaLaunchBuilder setRenderer(RendererInterface rendererInterface) {
        this.rendererOverride = rendererInterface;
        return this;
    }

    public LaunchPlan build() throws Exception {
        String strRuntimeNameForJava;
        File file = this.gameDirectoryOverride;
        if (file != null) {
            PathManager.initContextConstants(this.context, PathManager.inferLauncherHomeFromGameDirectory(file));
        } else {
            PathManager.initContextConstants(this.context);
        }
        File versionDirectory = MinecraftVersionInstaller.getVersionDirectory(this.versionId);
        File file2 = new File(versionDirectory, this.versionId + ".json");
        File file3 = this.gameDirectoryOverride;
        if (file3 == null) {
            file3 = new File(PathManager.DIR_MINECRAFT_HOME);
        }
        File file4 = file3;
        ensureDirectory(file4);
        if (!file2.isFile()) {
            throw new IllegalStateException("Missing version JSON: " + file2.getAbsolutePath());
        }
        JSONObject jSONObject = new JSONObject(readFile(file2));
        JSONObject jSONObjectResolveInheritedVersionJson = resolveInheritedVersionJson(jSONObject);
        String strResolveEffectiveMinecraftVersionId = resolveEffectiveMinecraftVersionId(jSONObject, jSONObjectResolveInheritedVersionJson);
        JSONObject jSONObjectEnsureCriticalMinecraftLibraries = ensureCriticalMinecraftLibraries(jSONObjectResolveInheritedVersionJson, strResolveEffectiveMinecraftVersionId);
        File fileResolveClientJarFile = resolveClientJarFile(this.versionId, jSONObject);
        String strOptString = jSONObjectEnsureCriticalMinecraftLibraries.optString("mainClass", DEFAULT_MAIN_CLASS);
        String strResolveAssetIndexName = resolveAssetIndexName(jSONObjectEnsureCriticalMinecraftLibraries);
        File fileResolveGameAssetsDirectory = resolveGameAssetsDirectory(jSONObjectEnsureCriticalMinecraftLibraries, strResolveAssetIndexName);
        int iResolveJavaMajor = resolveJavaMajor(jSONObjectEnsureCriticalMinecraftLibraries);
        File file5 = this.runtimeDirectoryOverride;
        if (file5 != null) {
            strRuntimeNameForJava = file5.getName();
        } else {
            strRuntimeNameForJava = runtimeNameForJava(iResolveJavaMajor);
        }
        File fileResolveRuntime = this.runtimeDirectoryOverride;
        if (fileResolveRuntime == null) {
            fileResolveRuntime = resolveRuntime(iResolveJavaMajor);
        }
        File file6 = fileResolveRuntime;
        if (!RuntimeCompat.isRuntimeInstalledForJava(strRuntimeNameForJava, file6, iResolveJavaMajor)) {
            throw new IllegalStateException("Selected runtime is not usable for Java " + iResolveJavaMajor + ": " + RuntimeCompat.describeRuntimeState(strRuntimeNameForJava, file6));
        }
        File fileFindJavaBinary = RuntimeCompat.findJavaBinary(file6);
        if (fileFindJavaBinary == null) {
            fileFindJavaBinary = new File(file6, "bin/java");
        }
        File file7 = fileFindJavaBinary;
        File fileResolveLwjglComponent = resolveLwjglComponent(strResolveEffectiveMinecraftVersionId);
        File fileResolveLwjglNativeDir = resolveLwjglNativeDir(fileResolveLwjglComponent);
        MinecraftVersionInstaller.ensureJnaNativesForLaunch(this.versionId, jSONObjectEnsureCriticalMinecraftLibraries);
        String strBuildClassPath = buildClassPath(jSONObjectEnsureCriticalMinecraftLibraries, fileResolveClientJarFile, fileResolveLwjglComponent, isForgeOrBootstrapVersion(jSONObjectEnsureCriticalMinecraftLibraries), strResolveEffectiveMinecraftVersionId);
        Map<String, String> mapBuildReplacements = buildReplacements(jSONObjectEnsureCriticalMinecraftLibraries, strResolveAssetIndexName, fileResolveGameAssetsDirectory, strBuildClassPath, fileResolveLwjglNativeDir, file4);
        ArrayList<String> arrayListBuildJvmArgs = buildJvmArgs(file6, fileResolveLwjglNativeDir, strBuildClassPath, mapBuildReplacements, jSONObjectEnsureCriticalMinecraftLibraries, file4, this.rendererOverride);
        ArrayList<String> arrayListBuildGameArgs = buildGameArgs(jSONObjectEnsureCriticalMinecraftLibraries, mapBuildReplacements);
        writeDebugLaunchFile(new File(versionDirectory, "java_launcher_last_launch_args.txt"), file6, strOptString, strBuildClassPath, arrayListBuildJvmArgs, arrayListBuildGameArgs);
        return new LaunchPlan(this.versionId, strOptString, file4, file6, file7, fileResolveLwjglNativeDir, strBuildClassPath, arrayListBuildJvmArgs, arrayListBuildGameArgs);
    }

    private static void ensureDirectory(File file) {
        if (file.exists()) {
            if (!file.isDirectory()) {
                throw new IllegalStateException("Path exists but is not a directory: " + file.getAbsolutePath());
            }
        } else if (!file.mkdirs() && !file.isDirectory()) {
            throw new IllegalStateException("Unable to create directory: " + file.getAbsolutePath());
        }
    }

    private JSONObject resolveInheritedVersionJson(JSONObject jSONObject) throws Exception {
        String strOptString = jSONObject.optString("inheritsFrom", "");
        if (strOptString.isEmpty()) {
            return jSONObject;
        }
        JSONObject jSONObjectResolveInheritedVersionJson = resolveInheritedVersionJson(readVersionJson(strOptString));
        JSONObject jSONObject2 = new JSONObject(jSONObjectResolveInheritedVersionJson.toString());
        JSONArray jSONArrayNames = jSONObject.names();
        if (jSONArrayNames != null) {
            for (int i = 0; i < jSONArrayNames.length(); i++) {
                String string = jSONArrayNames.getString(i);
                if (!"libraries".equals(string) && !"arguments".equals(string)) {
                    jSONObject2.put(string, jSONObject.get(string));
                }
            }
        }
        jSONObject2.put("libraries", mergeLibraries(jSONObjectResolveInheritedVersionJson.optJSONArray("libraries"), jSONObject.optJSONArray("libraries")));
        jSONObject2.put("arguments", mergeArguments(jSONObjectResolveInheritedVersionJson.optJSONObject("arguments"), jSONObject.optJSONObject("arguments")));
        if (!jSONObject.has("minecraftArguments") && jSONObjectResolveInheritedVersionJson.has("minecraftArguments")) {
            jSONObject2.put("minecraftArguments", jSONObjectResolveInheritedVersionJson.optString("minecraftArguments", ""));
        }
        return jSONObject2;
    }

    private JSONObject readVersionJson(String str) throws Exception {
        File file = new File(MinecraftVersionInstaller.getVersionDirectory(str), str + ".json");
        if (!file.isFile()) {
            throw new IllegalStateException("Missing inherited version JSON: " + file.getAbsolutePath());
        }
        return new JSONObject(readFile(file));
    }

    private File resolveClientJarFile(String str, JSONObject jSONObject) {
        File file = new File(MinecraftVersionInstaller.getVersionDirectory(str), str + ".jar");
        if (file.isFile()) {
            return file;
        }
        String strOptString = jSONObject.optString("inheritsFrom", "");
        if (!strOptString.isEmpty()) {
            File file2 = new File(MinecraftVersionInstaller.getVersionDirectory(strOptString), strOptString + ".jar");
            if (file2.isFile()) {
                return file2;
            }
            throw new IllegalStateException("Missing inherited client jar: " + file2.getAbsolutePath());
        }
        throw new IllegalStateException("Missing client jar: " + file.getAbsolutePath());
    }

    private String resolveEffectiveMinecraftVersionId(JSONObject jSONObject, JSONObject jSONObject2) {
        int iLastIndexOf;
        int i;
        String flattenedParentId;
        String strOptString = jSONObject.optString("inheritsFrom", jSONObject2.optString("inheritsFrom", ""));
        if (!strOptString.isEmpty()) {
            return strOptString;
        }
        String strTrim = jSONObject2.optString("javaLauncherFlattenedParent", "").trim();
        if (strTrim.isEmpty() && (flattenedParentId = InheritedVersionFlattener.readFlattenedParentId(this.context, this.versionId)) != null) {
            strTrim = flattenedParentId.trim();
        }
        if (!strTrim.isEmpty()) {
            return strTrim;
        }
        String strOptString2 = jSONObject2.optString("id", this.versionId);
        return (!strOptString2.startsWith("fabric-loader-") || (iLastIndexOf = strOptString2.lastIndexOf(45)) <= 0 || (i = iLastIndexOf + 1) >= strOptString2.length()) ? strOptString2 : strOptString2.substring(i);
    }

    private JSONObject ensureCriticalMinecraftLibraries(JSONObject jSONObject, String str) throws Exception {
        JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("libraries");
        if (jSONArrayOptJSONArray == null) {
            jSONArrayOptJSONArray = new JSONArray();
            jSONObject.put("libraries", jSONArrayOptJSONArray);
        }
        if (requiresJtracyLibrary(str) && !hasMavenLibrary(jSONArrayOptJSONArray, "com.mojang", "jtracy")) {
            JSONObject jSONObjectCreateMavenLibrary = createMavenLibrary("com.mojang:jtracy:1.0.29");
            jSONArrayOptJSONArray.put(jSONObjectCreateMavenLibrary);
            Logging.i(TAG, "Added missing critical Mojang library fallback: " + jSONObjectCreateMavenLibrary.optString("name") + " for Minecraft " + str);
        }
        return jSONObject;
    }

    private boolean requiresJtracyLibrary(String str) {
        String lowerCase = str.trim().toLowerCase(Locale.ROOT);
        if (lowerCase.matches("^\\d{2}w\\d{2}[a-z].*$") || lowerCase.matches("^\\d{2}\\..*$")) {
            return true;
        }
        int[] releaseVersion = parseReleaseVersion(lowerCase);
        if (releaseVersion == null) {
            return false;
        }
        int i = releaseVersion[0];
        int i2 = releaseVersion[1];
        int i3 = releaseVersion[2];
        if (i > 1) {
            return true;
        }
        if (i < 1) {
            return false;
        }
        if (i2 > 21) {
            return true;
        }
        return i2 >= 21 && i3 >= 3;
    }

    private int[] parseReleaseVersion(String str) {
        String[] strArrSplit = str.split("[^0-9]+");
        ArrayList arrayList = new ArrayList();
        for (String str2 : strArrSplit) {
            if (str2 != null && !str2.isEmpty()) {
                try {
                    arrayList.add(Integer.valueOf(Integer.parseInt(str2)));
                } catch (NumberFormatException unused) {
                }
                if (arrayList.size() >= 3) {
                    break;
                }
            }
        }
        if (arrayList.size() < 2) {
            return null;
        }
        return new int[]{((Integer) arrayList.get(0)).intValue(), ((Integer) arrayList.get(1)).intValue(), arrayList.size() >= 3 ? ((Integer) arrayList.get(2)).intValue() : 0};
    }

    private boolean hasMavenLibrary(JSONArray jSONArray, String str, String str2) {
        for (int i = 0; i < jSONArray.length(); i++) {
            JSONObject jSONObjectOptJSONObject = jSONArray.optJSONObject(i);
            if (jSONObjectOptJSONObject != null) {
                String[] strArrSplit = jSONObjectOptJSONObject.optString("name", "").split(":");
                if (strArrSplit.length >= 2 && strArrSplit.length == 3 && str.equals(strArrSplit[0]) && str2.equals(strArrSplit[1])) {
                    return true;
                }
            }
        }
        return false;
    }

    private JSONObject createMavenLibrary(String str) throws Exception {
        String strArtifactToPath = artifactToPath(str);
        if (strArtifactToPath == null || strArtifactToPath.isEmpty()) {
            throw new IllegalArgumentException("Invalid Maven library name: " + str);
        }
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("path", strArtifactToPath);
        jSONObject.put("url", "https://libraries.minecraft.net/" + strArtifactToPath);
        JSONObject jSONObject2 = new JSONObject();
        jSONObject2.put("artifact", jSONObject);
        JSONObject jSONObject3 = new JSONObject();
        jSONObject3.put("name", str);
        jSONObject3.put("downloads", jSONObject2);
        return jSONObject3;
    }

    private JSONArray mergeArrays(JSONArray jSONArray, JSONArray jSONArray2) throws Exception {
        JSONArray jSONArray3 = new JSONArray();
        if (jSONArray != null) {
            for (int i = 0; i < jSONArray.length(); i++) {
                jSONArray3.put(jSONArray.get(i));
            }
        }
        if (jSONArray2 != null) {
            for (int i2 = 0; i2 < jSONArray2.length(); i2++) {
                jSONArray3.put(jSONArray2.get(i2));
            }
        }
        return jSONArray3;
    }

    private JSONArray mergeLibraries(JSONArray jSONArray, JSONArray jSONArray2) throws Exception {
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        if (jSONArray != null) {
            for (int i = 0; i < jSONArray.length(); i++) {
                JSONObject jSONObjectOptJSONObject = jSONArray.optJSONObject(i);
                if (jSONObjectOptJSONObject != null) {
                    String libraryMergeKey = getLibraryMergeKey(jSONObjectOptJSONObject);
                    if (libraryMergeKey.isEmpty()) {
                        libraryMergeKey = "parent:" + i + ":" + jSONObjectOptJSONObject.toString();
                    }
                    linkedHashMap.put(libraryMergeKey, jSONObjectOptJSONObject);
                }
            }
        }
        if (jSONArray2 != null) {
            for (int i2 = 0; i2 < jSONArray2.length(); i2++) {
                JSONObject jSONObjectOptJSONObject2 = jSONArray2.optJSONObject(i2);
                if (jSONObjectOptJSONObject2 != null) {
                    String libraryMergeKey2 = getLibraryMergeKey(jSONObjectOptJSONObject2);
                    if (libraryMergeKey2.isEmpty()) {
                        libraryMergeKey2 = "child:" + i2 + ":" + jSONObjectOptJSONObject2.toString();
                    }
                    JSONObject jSONObject = (JSONObject) linkedHashMap.put(libraryMergeKey2, jSONObjectOptJSONObject2);
                    if (jSONObject != null && !jSONObject.toString().equals(jSONObjectOptJSONObject2.toString())) {
                        Logging.i(TAG, "Replacing inherited Maven library: " + libraryMergeKey2 + " old=" + jSONObject.optString("name", "<unknown>") + " new=" + jSONObjectOptJSONObject2.optString("name", "<unknown>"));
                    }
                }
            }
        }
        JSONArray jSONArray3 = new JSONArray();
        Iterator it = linkedHashMap.values().iterator();
        while (it.hasNext()) {
            jSONArray3.put((JSONObject) it.next());
        }
        return jSONArray3;
    }

    private String getLibraryMergeKey(JSONObject jSONObject) {
        String strTrim = jSONObject.optString("name", "").trim();
        if (strTrim.isEmpty()) {
            return "";
        }
        String[] strArrSplit = strTrim.split(":");
        if (strArrSplit.length < 2) {
            return strTrim;
        }
        String strTrim2 = strArrSplit[0].trim();
        String strTrim3 = strArrSplit[1].trim();
        return (strTrim2.isEmpty() || strTrim3.isEmpty()) ? strTrim : (strArrSplit.length < 4 || strArrSplit[3].trim().isEmpty()) ? strTrim2 + ":" + strTrim3 : strTrim2 + ":" + strTrim3 + ":" + strArrSplit[3].trim();
    }

    private JSONObject mergeArguments(JSONObject jSONObject, JSONObject jSONObject2) throws Exception {
        if (jSONObject == null && jSONObject2 == null) {
            return new JSONObject();
        }
        if (jSONObject == null) {
            return new JSONObject(jSONObject2.toString());
        }
        if (jSONObject2 == null) {
            return new JSONObject(jSONObject.toString());
        }
        JSONObject jSONObject3 = new JSONObject(jSONObject.toString());
        JSONArray jSONArrayNames = jSONObject2.names();
        if (jSONArrayNames != null) {
            for (int i = 0; i < jSONArrayNames.length(); i++) {
                String string = jSONArrayNames.getString(i);
                if ("game".equals(string) || "jvm".equals(string)) {
                    jSONObject3.put(string, mergeArrays(jSONObject.optJSONArray(string), jSONObject2.optJSONArray(string)));
                } else {
                    jSONObject3.put(string, jSONObject2.get(string));
                }
            }
        }
        return jSONObject3;
    }

    private int resolveJavaMajor(JSONObject jSONObject) {
        JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject("javaVersion");
        if (jSONObjectOptJSONObject == null) {
            return 8;
        }
        int iOptInt = jSONObjectOptJSONObject.optInt("majorVersion", 0);
        if (iOptInt <= 0) {
            iOptInt = jSONObjectOptJSONObject.optInt("version", 8);
        }
        return Math.max(8, iOptInt);
    }

    private File resolveRuntime(int i) {
        return RuntimeCompat.resolveRuntimeForJava(i);
    }

    private static String runtimeNameForJava(int i) {
        if (i >= 25) {
            return "Internal-25";
        }
        if (i >= 21) {
            return "Internal-21";
        }
        if (i >= 17) {
            return "Internal-17";
        }
        return "Internal-8";
    }

    private String resolveAssetIndexName(JSONObject jSONObject) {
        JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject("assetIndex");
        if (jSONObjectOptJSONObject != null) {
            String strOptString = jSONObjectOptJSONObject.optString("id", "");
            if (!strOptString.isEmpty()) {
                return strOptString;
            }
        }
        return jSONObject.optString("assets", "legacy");
    }

    private File resolveGameAssetsDirectory(JSONObject jSONObject, String str) throws Exception {
        File assetsDirectory = MinecraftVersionInstaller.getAssetsDirectory();
        if (!jSONObject.optString("minecraftArguments", "").contains("${game_assets}")) {
            return assetsDirectory;
        }
        File file = new File(assetsDirectory, "virtual/" + (str.isEmpty() ? "legacy" : str));
        ensureVirtualAssetsIfNeeded(str, file);
        return file;
    }

    private void ensureVirtualAssetsIfNeeded(String str, File file) throws Exception {
        if (!file.exists() && !file.mkdirs()) {
            throw new IllegalStateException("Unable to create virtual assets directory: " + file.getAbsolutePath());
        }
        File file2 = new File(file, ".java_launcher_complete");
        if (file2.isFile()) {
            return;
        }
        File assetsDirectory = MinecraftVersionInstaller.getAssetsDirectory();
        File file3 = new File(assetsDirectory, "indexes/" + str + ".json");
        if (!file3.isFile()) {
            Logging.i(TAG, "Legacy virtual asset index missing: " + file3.getAbsolutePath());
            return;
        }
        JSONObject jSONObjectOptJSONObject = new JSONObject(readFile(file3)).optJSONObject("objects");
        if (jSONObjectOptJSONObject == null) {
            return;
        }
        Iterator<String> itKeys = jSONObjectOptJSONObject.keys();
        int i = 0;
        while (itKeys.hasNext()) {
            String next = itKeys.next();
            JSONObject jSONObjectOptJSONObject2 = jSONObjectOptJSONObject.optJSONObject(next);
            if (jSONObjectOptJSONObject2 != null) {
                String strOptString = jSONObjectOptJSONObject2.optString("hash", "");
                if (strOptString.length() >= 2) {
                    File file4 = new File(assetsDirectory, "objects/" + strOptString.substring(0, 2) + "/" + strOptString);
                    File file5 = new File(file, next);
                    if (file4.isFile() && !file5.isFile()) {
                        copyFile(file4, file5);
                        i++;
                    }
                }
            }
        }
        FileOutputStream fileOutputStream = new FileOutputStream(file2);
        try {
            fileOutputStream.write(("copied=" + i + "\n").getBytes(StandardCharsets.UTF_8));
            fileOutputStream.close();
            Logging.i(TAG, "Prepared legacy virtual assets: " + file.getAbsolutePath() + " copied=" + i);
        } catch (Throwable th) {
            try {
                fileOutputStream.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    private File resolveLwjglComponent(String str) {
        File file = new File(PathManager.DIR_FILE, shouldUseModernLwjgl(str) ? "lwjgl3.4.1" : "lwjgl3.3.3");
        if (file.isDirectory()) {
            return file;
        }
        File file2 = new File(PathManager.DIR_FILE, "lwjgl3.3.3");
        if (file2.isDirectory()) {
            return file2;
        }
        File file3 = new File(PathManager.DIR_FILE, "lwjgl3.4.1");
        if (file3.isDirectory()) {
            return file3;
        }
        throw new IllegalStateException("No LWJGL component found in " + PathManager.DIR_FILE.getAbsolutePath());
    }

    private boolean shouldUseModernLwjgl(String str) {
        String lowerCase = str.trim().toLowerCase(Locale.ROOT);
        if (lowerCase.matches("^\\d{2}w\\d{2}[a-z].*$")) {
            return Integer.parseInt(lowerCase.substring(0, 2)) >= 26;
        }
        if (lowerCase.matches("^\\d{2}\\..*$")) {
            try {
                return Integer.parseInt(lowerCase.substring(0, 2)) >= 26;
            } catch (NumberFormatException unused) {
            }
        }
        return false;
    }

    private File resolveLwjglNativeDir(File file) {
        String strAndroidAbiAsString = Architecture.androidAbiAsString(Architecture.getDeviceArchitecture());
        File file2 = new File(file, "natives/" + strAndroidAbiAsString);
        if (file2.isDirectory()) {
            return file2;
        }
        File file3 = new File(file, "natives/arm64-v8a");
        if (file3.isDirectory()) {
            return file3;
        }
        throw new IllegalStateException("Missing LWJGL natives for " + strAndroidAbiAsString + " in " + file.getAbsolutePath());
    }

    private String buildClassPath(JSONObject jSONObject, File file, File file2, boolean z, String str) throws Exception {
        String strResolveLibraryArtifactPath;
        String strPut;
        LinkedHashMap<String, File> linkedHashMap = new LinkedHashMap<>();
        File[] fileArrListFiles = file2.listFiles(new FilenameFilter() { // from class: ca.dnamobile.javalauncher.launcher.JavaLaunchBuilder$$ExternalSyntheticLambda5
            @Override // java.io.FilenameFilter
            public final boolean accept(File file3, String str2) {
                return str2.endsWith(".jar");
            }
        });
        if (fileArrListFiles != null) {
            Arrays.sort(fileArrListFiles, new Comparator() { // from class: ca.dnamobile.javalauncher.launcher.JavaLaunchBuilder$$ExternalSyntheticLambda6
                @Override // java.util.Comparator
                public final int compare(Object obj, Object obj2) {
                    return ((File) obj).getName().compareToIgnoreCase(((File) obj2).getName());
                }
            });
            for (File file3 : fileArrListFiles) {
                linkedHashMap.put(file3.getAbsolutePath(), file3);
            }
        }
        LinkedHashMap<String, String> linkedHashMap2 = new LinkedHashMap<>();
        JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("libraries");
        if (jSONArrayOptJSONArray != null) {
            for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
                JSONObject jSONObjectOptJSONObject = jSONArrayOptJSONArray.optJSONObject(i);
                if (jSONObjectOptJSONObject != null && isLibraryAllowed(jSONObjectOptJSONObject)) {
                    String strOptString = jSONObjectOptJSONObject.optString("name", "");
                    if (!isSkippedDesktopNativeLibrary(strOptString) && (strResolveLibraryArtifactPath = resolveLibraryArtifactPath(jSONObjectOptJSONObject)) != null && !strResolveLibraryArtifactPath.isEmpty()) {
                        if (strResolveLibraryArtifactPath.endsWith(".aar")) {
                            Logging.i(TAG, "Skipping native AAR on classpath: " + strResolveLibraryArtifactPath);
                        } else if (z && isForgeMinecraftClientArtifact(strOptString, strResolveLibraryArtifactPath)) {
                            Logging.i(TAG, "Skipping Forge Minecraft client artifact on classpath: " + strResolveLibraryArtifactPath);
                        } else {
                            File file4 = new File(MinecraftVersionInstaller.getLibrariesDirectory(), strResolveLibraryArtifactPath);
                            if (!ensureLibraryJarForLaunch(jSONObjectOptJSONObject, strResolveLibraryArtifactPath, file4)) {
                                Logging.i(TAG, "Skipping missing library on classpath: " + file4.getAbsolutePath());
                            } else {
                                String libraryMergeKey = getLibraryMergeKey(jSONObjectOptJSONObject);
                                if (!libraryMergeKey.isEmpty() && (strPut = linkedHashMap2.put(libraryMergeKey, file4.getAbsolutePath())) != null && !strPut.equals(file4.getAbsolutePath())) {
                                    linkedHashMap.remove(strPut);
                                    Logging.i(TAG, "Replacing duplicate Maven library on classpath: " + libraryMergeKey + " old=" + strPut + " new=" + file4.getAbsolutePath());
                                }
                                linkedHashMap.put(file4.getAbsolutePath(), file4);
                            }
                        }
                    }
                }
            }
        }
        addCriticalClasspathLibrariesIfNeeded(linkedHashMap, linkedHashMap2, str);
        if (z) {
            Logging.i(TAG, "Skipping vanilla/inherited Minecraft client jar on Forge classpath: " + file.getAbsolutePath());
        } else {
            linkedHashMap.put(file.getAbsolutePath(), file);
        }
        StringBuilder sb = new StringBuilder();
        for (File file5 : linkedHashMap.values()) {
            if (sb.length() > 0) {
                sb.append(':');
            }
            sb.append(file5.getAbsolutePath());
        }
        return sb.toString();
    }

    private boolean isForgeMinecraftClientArtifact(String str, String str2) {
        return str.toLowerCase(Locale.ROOT).startsWith("net.minecraft:client:") || str2.replace('\\', '/').toLowerCase(Locale.ROOT).contains("/net/minecraft/client/");
    }

    private boolean isSkippedDesktopNativeLibrary(String str) {
        return str.contains("org.lwjgl") || str.contains("jinput-platform") || str.contains("lwjgl-platform") || str.contains("twitch-platform");
    }

    private boolean isLibraryAllowed(JSONObject jSONObject) {
        JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("rules");
        if (jSONArrayOptJSONArray == null || jSONArrayOptJSONArray.length() == 0) {
            return true;
        }
        boolean zEquals = false;
        for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
            JSONObject jSONObjectOptJSONObject = jSONArrayOptJSONArray.optJSONObject(i);
            if (jSONObjectOptJSONObject != null) {
                String strOptString = jSONObjectOptJSONObject.optString("action", "allow");
                JSONObject jSONObjectOptJSONObject2 = jSONObjectOptJSONObject.optJSONObject("os");
                if (jSONObjectOptJSONObject2 == null || "linux".equals(jSONObjectOptJSONObject2.optString("name", "linux"))) {
                    zEquals = "allow".equals(strOptString);
                }
            }
        }
        return zEquals;
    }

    private String resolveLibraryArtifactPath(JSONObject jSONObject) {
        JSONObject jSONObjectOptJSONObject;
        JSONObject jSONObjectOptJSONObject2 = jSONObject.optJSONObject("downloads");
        if (jSONObjectOptJSONObject2 != null && (jSONObjectOptJSONObject = jSONObjectOptJSONObject2.optJSONObject("artifact")) != null) {
            String strOptString = jSONObjectOptJSONObject.optString("path", "");
            if (!strOptString.isEmpty()) {
                return strOptString;
            }
        }
        return artifactToPath(jSONObject.optString("name", ""));
    }

    private void addCriticalClasspathLibrariesIfNeeded(LinkedHashMap<String, File> linkedHashMap, LinkedHashMap<String, String> linkedHashMap2, String str) throws Exception {
        if (requiresJtracyLibrary(str)) {
            addMavenLibraryToClasspath(linkedHashMap, linkedHashMap2, createMavenLibrary("com.mojang:jtracy:1.0.29"), "critical Minecraft library fallback");
        }
    }

    private void addMavenLibraryToClasspath(LinkedHashMap<String, File> linkedHashMap, LinkedHashMap<String, String> linkedHashMap2, JSONObject jSONObject, String str) {
        String strPut;
        String strResolveLibraryArtifactPath = resolveLibraryArtifactPath(jSONObject);
        if (strResolveLibraryArtifactPath == null || strResolveLibraryArtifactPath.isEmpty()) {
            return;
        }
        File file = new File(MinecraftVersionInstaller.getLibrariesDirectory(), strResolveLibraryArtifactPath);
        if (!ensureLibraryJarForLaunch(jSONObject, strResolveLibraryArtifactPath, file)) {
            Logging.i(TAG, "Unable to add " + str + ": " + jSONObject.optString("name") + " path=" + file.getAbsolutePath());
            return;
        }
        String libraryMergeKey = getLibraryMergeKey(jSONObject);
        if (!libraryMergeKey.isEmpty() && (strPut = linkedHashMap2.put(libraryMergeKey, file.getAbsolutePath())) != null && !strPut.equals(file.getAbsolutePath())) {
            linkedHashMap.remove(strPut);
            Logging.i(TAG, "Replacing duplicate Maven library on classpath: " + libraryMergeKey + " old=" + strPut + " new=" + file.getAbsolutePath());
        }
        linkedHashMap.put(file.getAbsolutePath(), file);
        Logging.i(TAG, "Added " + str + " to classpath: " + jSONObject.optString("name") + " -> " + file.getAbsolutePath());
    }

    private boolean ensureLibraryJarForLaunch(JSONObject jSONObject, String str, File file) {
        String lowerCase;
        JSONObject jSONObjectOptJSONObject;
        if (file.isFile()) {
            return true;
        }
        String str2 = "";
        String strOptString = jSONObject.optString("name", "");
        JSONObject jSONObjectOptJSONObject2 = jSONObject.optJSONObject("downloads");
        if (jSONObjectOptJSONObject2 == null || (jSONObjectOptJSONObject = jSONObjectOptJSONObject2.optJSONObject("artifact")) == null) {
            lowerCase = "";
        } else {
            String strTrim = jSONObjectOptJSONObject.optString("url", "").trim();
            lowerCase = jSONObjectOptJSONObject.optString("sha1", "").trim().toLowerCase(Locale.ROOT);
            str2 = strTrim;
        }
        if (str2.isEmpty()) {
            str2 = "https://libraries.minecraft.net/" + str;
        }
        Logging.i(TAG, "Library missing before launch, attempting download: " + strOptString + " path=" + file.getAbsolutePath());
        try {
            downloadLibraryJar(str2, file);
            if (!lowerCase.isEmpty()) {
                String strSha1 = sha1(file);
                if (!lowerCase.equals(strSha1)) {
                    file.delete();
                    Logging.i(TAG, "Downloaded library SHA-1 mismatch for " + strOptString + " expected=" + lowerCase + " actual=" + strSha1);
                    return false;
                }
            }
            Logging.i(TAG, "Downloaded missing launch library: " + file.getAbsolutePath());
            return file.isFile();
        } catch (Throwable th) {
            Logging.e(TAG, "Unable to download missing launch library: " + strOptString + " from " + str2, th);
            return false;
        }
    }

    private void downloadLibraryJar(String str, File file) throws Exception {
        File parentFile = file.getParentFile();
        if (parentFile != null && !parentFile.exists() && !parentFile.mkdirs()) {
            throw new IllegalStateException("Unable to create library directory: " + parentFile.getAbsolutePath());
        }
        File file2 = new File(file.getAbsolutePath() + ".download");
        if (file2.isFile() && !file2.delete()) {
            throw new IllegalStateException("Unable to remove stale temp download: " + file2.getAbsolutePath());
        }
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(str).openConnection();
        httpURLConnection.setConnectTimeout(15000);
        httpURLConnection.setReadTimeout(30000);
        httpURLConnection.setInstanceFollowRedirects(true);
        httpURLConnection.setRequestProperty("User-Agent", "JavaLauncher/1.0");
        int responseCode = httpURLConnection.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            httpURLConnection.disconnect();
            throw new IllegalStateException("HTTP " + responseCode + " while downloading " + str);
        }
        try {
            BufferedInputStream bufferedInputStream = new BufferedInputStream(httpURLConnection.getInputStream());
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(file2);
                try {
                    byte[] bArr = new byte[8192];
                    while (true) {
                        int i = bufferedInputStream.read(bArr);
                        if (i < 0) {
                            break;
                        } else {
                            fileOutputStream.write(bArr, 0, i);
                        }
                    }
                    fileOutputStream.close();
                    bufferedInputStream.close();
                    httpURLConnection.disconnect();
                    if (file2.renameTo(file)) {
                        return;
                    }
                    copyFile(file2, file);
                    file2.delete();
                } finally {
                }
            } finally {
            }
        } catch (Throwable th) {
            httpURLConnection.disconnect();
            throw th;
        }
    }

    private String sha1(File file) throws Exception {
        int i;
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            byte[] bArr = new byte[8192];
            while (true) {
                int i2 = fileInputStream.read(bArr);
                if (i2 < 0) {
                    break;
                }
                messageDigest.update(bArr, 0, i2);
            }
            fileInputStream.close();
            byte[] bArrDigest = messageDigest.digest();
            StringBuilder sb = new StringBuilder(bArrDigest.length * 2);
            for (byte b : bArrDigest) {
                sb.append(String.format(Locale.ROOT, "%02x", Integer.valueOf(b & 255)));
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

    private String artifactToPath(String str) {
        String[] strArrSplit = str.split(":");
        if (strArrSplit.length < 3) {
            return null;
        }
        String strReplace = strArrSplit[0].replace('.', '/');
        String str2 = strArrSplit[1];
        String str3 = strArrSplit[2];
        return strReplace + "/" + str2 + "/" + str3 + "/" + str2 + "-" + str3 + (strArrSplit.length > 3 ? "-" + strArrSplit[3] : "") + ".jar";
    }

    private Map<String, String> buildReplacements(JSONObject jSONObject, String str, File file, String str2, File file2, File file3) {
        String strCreateOfflineUuid;
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        String strResolvePlayerName = resolvePlayerName();
        boolean zIsCustomSkinEnabledForLaunch = isCustomSkinEnabledForLaunch(strResolvePlayerName);
        boolean z = !zIsCustomSkinEnabledForLaunch && hasValidMinecraftSession();
        OfflineSkinProfile offlineSkinProfileResolveOfflineSkinProfile = zIsCustomSkinEnabledForLaunch ? resolveOfflineSkinProfile(strResolvePlayerName) : null;
        if (offlineSkinProfileResolveOfflineSkinProfile != null && offlineSkinProfileResolveOfflineSkinProfile.enabled) {
            strCreateOfflineUuid = stripUuidDashes(offlineSkinProfileResolveOfflineSkinProfile.uniqueUuid);
        } else if (z) {
            strCreateOfflineUuid = stripUuidDashes(this.account.minecraftUuid);
        } else {
            strCreateOfflineUuid = createOfflineUuid(strResolvePlayerName);
        }
        String str3 = z ? this.account.minecraftAccessToken : "0";
        String str4 = (!z || isNullOrBlank(this.account.xuid)) ? "" : this.account.xuid;
        String str5 = z ? "msa" : "legacy";
        linkedHashMap.put("${auth_player_name}", strResolvePlayerName);
        linkedHashMap.put("${version_name}", this.versionId);
        linkedHashMap.put("${game_directory}", file3.getAbsolutePath());
        linkedHashMap.put("${assets_root}", MinecraftVersionInstaller.getAssetsDirectory().getAbsolutePath());
        linkedHashMap.put("${game_assets}", file.getAbsolutePath());
        linkedHashMap.put("${assets_index_name}", str);
        linkedHashMap.put("${auth_uuid}", strCreateOfflineUuid);
        linkedHashMap.put("${auth_access_token}", str3);
        linkedHashMap.put("${auth_session}", str3);
        linkedHashMap.put("${clientid}", "0");
        linkedHashMap.put("${client_id}", "0");
        linkedHashMap.put("${auth_xuid}", str4);
        linkedHashMap.put("${user_type}", str5);
        linkedHashMap.put("${user_properties}", "{}");
        linkedHashMap.put("${user_property_map}", "{}");
        linkedHashMap.put("${profile_properties}", "{}");
        linkedHashMap.put("${quickPlayPath}", "");
        linkedHashMap.put("${quickPlaySingleplayer}", "");
        linkedHashMap.put("${quickPlayMultiplayer}", "");
        linkedHashMap.put("${quickPlayRealms}", "");
        linkedHashMap.put("${version_type}", jSONObject.optString("type", BuildConfig.BUILD_TYPE));
        linkedHashMap.put("${resolution_width}", String.valueOf(this.width));
        linkedHashMap.put("${resolution_height}", String.valueOf(this.height));
        linkedHashMap.put("${natives_directory}", file2.getAbsolutePath());
        linkedHashMap.put("${launcher_name}", "JavaLauncher");
        linkedHashMap.put("${launcher_version}", "1.0");
        linkedHashMap.put("${library_directory}", MinecraftVersionInstaller.getLibrariesDirectory().getAbsolutePath());
        linkedHashMap.put("${classpath_separator}", ":");
        linkedHashMap.put("${classpath}", str2);
        Logging.i(TAG, "Launch account mode=" + (zIsCustomSkinEnabledForLaunch ? "custom_skin" : z ? AccountStore.Account.TYPE_MICROSOFT : AccountStore.Account.TYPE_OFFLINE) + " player=" + strResolvePlayerName + " uuid=" + strCreateOfflineUuid + " userType=" + str5 + " accountLoaded=" + (this.account != null));
        return linkedHashMap;
    }

    private String resolvePlayerName() {
        AccountStore.Account account = this.account;
        if (account != null) {
            if (!isNullOrBlank(account.minecraftName)) {
                return sanitizePlayerName(this.account.minecraftName);
            }
            if (!isNullOrBlank(this.account.displayName)) {
                return sanitizePlayerName(this.account.displayName);
            }
            if (!isNullOrBlank(this.account.email)) {
                return sanitizePlayerName(this.account.email.split("@")[0]);
            }
            return "Player";
        }
        return "Player";
    }

    private boolean hasValidMinecraftSession() {
        AccountStore.Account account = this.account;
        return (account == null || !account.isMicrosoftAccount() || isNullOrBlank(this.account.minecraftAccessToken) || isNullOrBlank(this.account.minecraftName) || isNullOrBlank(this.account.minecraftUuid)) ? false : true;
    }

    private static boolean isNullOrBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    private static String stripUuidDashes(String str) {
        return str.replace("-", "").trim();
    }

    private String sanitizePlayerName(String str) {
        String strReplaceAll = str.replaceAll("[^A-Za-z0-9_]", "");
        return strReplaceAll.isEmpty() ? "Player" : strReplaceAll.length() > 16 ? strReplaceAll.substring(0, 16) : strReplaceAll;
    }

    private String createOfflineUuid(String str) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + str).getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");
    }

    public static void stopActiveOfflineSkinServer() {
        OfflineYggdrasilServer offlineYggdrasilServer = activeOfflineSkinServer;
        activeOfflineSkinServer = null;
        if (offlineYggdrasilServer != null) {
            try {
                offlineYggdrasilServer.stop();
            } catch (Throwable unused) {
            }
        }
    }

    private OfflineSkinProfile resolveOfflineSkinProfile(String str) {
        String offlineUuidWithSkinModel;
        AccountStore.Account account = this.account;
        if (account != null && account.isOfflineAccount() && !isNullOrBlank(this.account.offlineSkinPath)) {
            File file = new File(this.account.offlineSkinPath);
            if (file.isFile()) {
                SkinModelType skinModelTypeFromId = SkinModelType.fromId(this.account.offlineSkinModel);
                if (!isNullOrBlank(this.account.minecraftUuid)) {
                    offlineUuidWithSkinModel = this.account.minecraftUuid;
                } else {
                    offlineUuidWithSkinModel = CustomSkinStore.getOfflineUuidWithSkinModel(str, skinModelTypeFromId);
                }
                return new OfflineSkinProfile(offlineUuidWithSkinModel, file, skinModelTypeFromId, true);
            }
        }
        AccountStore.Account account2 = this.account;
        if (account2 == null || !account2.isOfflineAccount()) {
            return null;
        }
        try {
            OfflineSkinProfile offlineSkinProfileBuildOfflineProfile = new CustomSkinStore(this.context).buildOfflineProfile(str);
            if (!offlineSkinProfileBuildOfflineProfile.enabled || offlineSkinProfileBuildOfflineProfile.skinFile == null) {
                return null;
            }
            if (offlineSkinProfileBuildOfflineProfile.skinFile.isFile()) {
                return offlineSkinProfileBuildOfflineProfile;
            }
            return null;
        } catch (Throwable th) {
            Logging.e(TAG, "Unable to check legacy custom skin profile", th);
            return null;
        }
    }

    private boolean isCustomSkinEnabledForLaunch(String str) {
        OfflineSkinProfile offlineSkinProfileResolveOfflineSkinProfile = resolveOfflineSkinProfile(str);
        return offlineSkinProfileResolveOfflineSkinProfile != null && offlineSkinProfileResolveOfflineSkinProfile.enabled && offlineSkinProfileResolveOfflineSkinProfile.skinFile != null && offlineSkinProfileResolveOfflineSkinProfile.skinFile.isFile();
    }

    private void addCustomSkinAuthlibInjectorIfNeeded(ArrayList<String> arrayList) {
        String strResolvePlayerName = resolvePlayerName();
        OfflineSkinProfile offlineSkinProfileResolveOfflineSkinProfile = resolveOfflineSkinProfile(strResolvePlayerName);
        if (offlineSkinProfileResolveOfflineSkinProfile == null || !offlineSkinProfileResolveOfflineSkinProfile.enabled || offlineSkinProfileResolveOfflineSkinProfile.skinFile == null || !offlineSkinProfileResolveOfflineSkinProfile.skinFile.isFile()) {
            stopActiveOfflineSkinServer();
            return;
        }
        File fileResolveAuthlibInjectorJar = resolveAuthlibInjectorJar();
        if (fileResolveAuthlibInjectorJar == null || !fileResolveAuthlibInjectorJar.isFile()) {
            Logging.i(TAG, "Offline skin selected, but authlib-injector.jar was not found. Skin will only show in launcher UI.");
            safeWriteSkinLaunchNote("Offline skin selected, but authlib-injector.jar was not found.\n");
            stopActiveOfflineSkinServer();
            return;
        }
        try {
            stopActiveOfflineSkinServer();
            OfflineYggdrasilServer offlineYggdrasilServer = new OfflineYggdrasilServer("JavaLauncher_Offline", "JavaLauncher", "1.0");
            offlineYggdrasilServer.start();
            int port = offlineYggdrasilServer.getPort();
            if (port <= 0) {
                offlineYggdrasilServer.stop();
                Logging.i(TAG, "Offline skin server did not expose a valid port.");
                return;
            }
            offlineYggdrasilServer.addCharacter(stripUuidDashes(offlineSkinProfileResolveOfflineSkinProfile.uniqueUuid), strResolvePlayerName, offlineSkinProfileResolveOfflineSkinProfile.skinFile, offlineSkinProfileResolveOfflineSkinProfile.model);
            activeOfflineSkinServer = offlineYggdrasilServer;
            arrayList.add("-javaagent:" + fileResolveAuthlibInjectorJar.getAbsolutePath() + "=http://127.0.0.1:" + port + "/");
            arrayList.add("-Dauthlibinjector.side=client");
            Logging.i(TAG, "Using OfflineYggdrasilServer on port " + port + " for offline skin user " + strResolvePlayerName);
        } catch (Throwable th) {
            stopActiveOfflineSkinServer();
            Logging.e(TAG, "Failed to prepare custom skin authlib injector", th);
        }
    }

    private File resolveAuthlibInjectorJar() {
        File libPathFileField = readLibPathFileField("AUTHLIB_INJECTOR");
        if (libPathFileField != null && libPathFileField.isFile()) {
            return libPathFileField;
        }
        File[] fileArr = {new File(PathManager.DIR_FILE, "authlib-injector.jar"), new File(PathManager.DIR_FILE, "authlib-injector/authlib-injector.jar"), new File(PathManager.DIR_FILE, "components/authlib-injector.jar"), new File(PathManager.DIR_FILE, "components/authlib-injector/authlib-injector.jar"), new File(PathManager.DIR_FILE, "authlib_injector.jar"), new File(PathManager.DIR_DATA, "authlib-injector.jar")};
        for (int i = 0; i < 6; i++) {
            File file = fileArr[i];
            if (file.isFile()) {
                return file;
            }
        }
        return null;
    }

    private File readLibPathFileField(String str) {
        Object obj;
        try {
            Field declaredField = LibPath.class.getDeclaredField(str);
            declaredField.setAccessible(true);
            obj = declaredField.get(null);
        } catch (Throwable unused) {
        }
        if (obj instanceof File) {
            return (File) obj;
        }
        if ((obj instanceof String) && !((String) obj).trim().isEmpty()) {
            return new File((String) obj);
        }
        return null;
    }

    private void safeWriteSkinLaunchNote(String str) {
        try {
            File file = new File(PathManager.DIR_LAUNCHER_LOG, "latestlog.txt");
            File parentFile = file.getParentFile();
            if (parentFile != null && !parentFile.exists()) {
                parentFile.mkdirs();
            }
            FileOutputStream fileOutputStream = new FileOutputStream(file, true);
            try {
                fileOutputStream.write(str.getBytes(StandardCharsets.UTF_8));
                fileOutputStream.close();
            } finally {
            }
        } catch (Throwable unused) {
        }
    }

    private boolean shouldEnableLegacyAwt(JSONObject jSONObject) {
        String strOptString = jSONObject.optString("id", this.versionId);
        if (strOptString == null || strOptString.trim().isEmpty()) {
            strOptString = this.versionId;
        }
        String lowerCase = strOptString.trim().toLowerCase(Locale.ROOT);
        if (lowerCase.startsWith("rd-") || lowerCase.startsWith("classic") || lowerCase.startsWith("infdev") || lowerCase.startsWith("indev") || lowerCase.startsWith("a") || lowerCase.startsWith("b")) {
            return true;
        }
        if (!lowerCase.startsWith("1.")) {
            return false;
        }
        int i = -1;
        int i2 = 0;
        int i3 = -1;
        for (String str : lowerCase.split("[^0-9]+")) {
            if (str != null && !str.isEmpty()) {
                if (i2 == 0) {
                    try {
                        i3 = Integer.parseInt(str);
                    } catch (NumberFormatException unused) {
                        continue;
                    }
                }
                if (i2 == 1) {
                    i = Integer.parseInt(str);
                    break;
                }
                i2++;
            }
        }
        return i3 == 1 && i >= 0 && i < 6;
    }

    private ArrayList<String> buildJvmArgs(File file, File file2, String str, Map<String, String> map, JSONObject jSONObject, File file3, RendererInterface rendererInterface) throws Exception {
        ArrayList<String> arrayList = new ArrayList<>();
        addCustomSkinAuthlibInjectorIfNeeded(arrayList);
        boolean zShouldEnableLegacyAwt = shouldEnableLegacyAwt(jSONObject);
        if (zShouldEnableLegacyAwt) {
            arrayList.addAll(buildCacioJvmArgs(file));
        }
        addMojangJvmArguments(arrayList, jSONObject, map);
        addLegacyBetaJvmArgs(arrayList, jSONObject);
        removeClasspathArgs(arrayList);
        purgeZalithManagedArgs(arrayList);
        addForgeJava17ModuleOpens(arrayList, jSONObject);
        addForgeNashornAsmModulePath(arrayList, jSONObject);
        String strBuildJnaNativePath = buildJnaNativePath();
        String strBuildJnaLibraryPath = ControllerModCompat.buildJnaLibraryPath(this.context, file3, strBuildJnaNativePath);
        boolean zHasControllable = ControllerModCompat.hasControllable(file3);
        String strBuildNativeLibraryPath = buildNativeLibraryPath(file, file2, strBuildJnaLibraryPath, rendererInterface);
        String strBuildBootLibraryPath = buildBootLibraryPath(file);
        String absolutePath = new File(PathManager.DIR_DATA, "resolv.conf").getAbsolutePath();
        arrayList.add("-Djava.home=" + file.getAbsolutePath());
        arrayList.add("-Djava.io.tmpdir=" + PathManager.DIR_CACHE.getAbsolutePath());
        arrayList.add("-Duser.home=" + file3.getAbsolutePath());
        arrayList.add("-Duser.language=" + Locale.getDefault().getLanguage());
        arrayList.add("-Dos.name=Linux");
        arrayList.add("-Dos.version=Android-" + Build.VERSION.RELEASE);
        arrayList.add("-Dpojav.path.minecraft=" + file3.getAbsolutePath());
        arrayList.add("-Dpojav.path.private.account=" + PathManager.DIR_ACCOUNT_NEW);
        arrayList.add("-Djava.library.path=" + strBuildNativeLibraryPath);
        if (zShouldEnableLegacyAwt) {
            arrayList.add("-Dsun.boot.library.path=" + strBuildBootLibraryPath);
        }
        arrayList.add("-Dorg.lwjgl.librarypath=" + file2.getAbsolutePath());
        arrayList.add("-Dorg.lwjgl.system.SharedLibraryExtractPath=" + PathManager.DIR_CACHE.getAbsolutePath());
        arrayList.add("-Djna.tmpdir=" + PathManager.DIR_CACHE.getAbsolutePath());
        addJnaAndroidWorkarounds(arrayList, strBuildJnaNativePath, strBuildJnaLibraryPath, zHasControllable);
        arrayList.add("-Dio.netty.native.workdir=" + PathManager.DIR_CACHE.getAbsolutePath());
        String strResolveRendererOpenGlLibrary = resolveRendererOpenGlLibrary(rendererInterface);
        if (!strResolveRendererOpenGlLibrary.isEmpty()) {
            arrayList.add("-Dorg.lwjgl.opengl.libname=" + strResolveRendererOpenGlLibrary);
        }
        arrayList.add("-Dorg.lwjgl.freetype.libname=" + new File(PathManager.DIR_NATIVE_LIB, "libfreetype.so").getAbsolutePath());
        arrayList.add("-Dglfwstub.windowWidth=" + this.width);
        arrayList.add("-Dglfwstub.windowHeight=" + this.height);
        arrayList.add("-Dglfwstub.initEgl=" + shouldGlfwStubInitEgl(rendererInterface));
        arrayList.add("-Dext.net.resolvPath=" + absolutePath);
        arrayList.add("-Dlog4j2.formatMsgNoLookups=true");
        arrayList.add("-Dnet.minecraft.clientmodname=" + getClientModNameForRenderer(rendererInterface));
        arrayList.add("-Dfml.earlyprogresswindow=false");
        arrayList.add("-Dloader.disable_forked_guis=true");
        arrayList.add("-Djdk.lang.Process.launchMechanism=FORK");
        arrayList.add("-Dsodium.checks.issue2561=false");
        addMioLibPatcherForRenderer(arrayList, rendererInterface);
        int iResolveAllocatedMemoryMb = MemoryAllocationUtils.resolveAllocatedMemoryMb(this.context);
        int iResolveStartHeapMb = resolveStartHeapMb(iResolveAllocatedMemoryMb);
        Logging.i(TAG, "Using allocated memory: Xms=" + iResolveStartHeapMb + " MB, Xmx=" + iResolveAllocatedMemoryMb + " MB");
        arrayList.add("-XX:ActiveProcessorCount=" + Runtime.getRuntime().availableProcessors());
        arrayList.add("-Xms" + iResolveStartHeapMb + "M");
        arrayList.add("-Xmx" + iResolveAllocatedMemoryMb + "M");
        arrayList.add("-cp");
        arrayList.add(str);
        return arrayList;
    }

    private static int resolveStartHeapMb(int i) {
        if (i <= 0) {
            return 512;
        }
        return Math.min(ViewUtils.EDGE_TO_EDGE_FLAGS, Math.max(512, i / 4));
    }

    private void addLegacyBetaJvmArgs(ArrayList<String> arrayList, JSONObject jSONObject) {
        String strOptString = jSONObject.optString("id", this.versionId);
        if (strOptString == null || strOptString.trim().isEmpty()) {
            strOptString = this.versionId;
        }
        if (isBetacraftProxyVersion(strOptString.trim().toLowerCase(Locale.ROOT))) {
            addJvmArgIfMissing(arrayList, "-Dhttp.proxyHost=betacraft.uk");
            addJvmArgIfMissing(arrayList, "-Dhttp.proxyPort=11705");
            addJvmArgIfMissing(arrayList, "-Djava.util.Arrays.useLegacyMergeSort=true");
            addJvmArgIfMissing(arrayList, "-Djava.net.preferIPv4Stack=true");
            Logging.i(TAG, "Applied Betacraft proxy JVM args for legacy version " + strOptString);
        }
    }

    private boolean isBetacraftProxyVersion(String str) {
        if (str.startsWith("rd-") || str.startsWith("classic") || str.startsWith("infdev") || str.startsWith("indev") || str.matches("^a\\d.*") || str.matches("^b\\d.*")) {
            return true;
        }
        if (!str.startsWith("1.")) {
            return false;
        }
        int i = -1;
        int i2 = 0;
        int i3 = -1;
        for (String str2 : str.split("[^0-9]+")) {
            if (str2 != null && !str2.isEmpty()) {
                if (i2 == 0) {
                    try {
                        i3 = Integer.parseInt(str2);
                    } catch (NumberFormatException unused) {
                    }
                } else if (i2 == 1) {
                    i = Integer.parseInt(str2);
                    break;
                }
                i2++;
            }
        }
        return i3 == 1 && i >= 0 && i < 6;
    }

    private String resolveRendererOpenGlLibrary(RendererInterface rendererInterface) {
        if (rendererInterface == null) {
            return "libng_gl4es.so";
        }
        String rendererLibrary = rendererInterface.getRendererLibrary();
        if (rendererLibrary == null || rendererLibrary.trim().isEmpty()) {
            Logging.i(TAG, "Selected renderer returned an empty OpenGL library, falling back to libng_gl4es.so");
            return "libng_gl4es.so";
        }
        if (isLtwRenderer(rendererInterface)) {
            File file = new File(rendererLibrary.trim());
            if (file.isAbsolute() && file.isFile()) {
                return file.getAbsolutePath();
            }
            Iterator<File> it = rendererInterface.getLibrarySearchPaths().iterator();
            while (it.hasNext()) {
                File file2 = new File(it.next(), "libltw.so");
                if (file2.isFile()) {
                    return file2.getAbsolutePath();
                }
            }
            return "libltw.so";
        }
        return new File(rendererLibrary.trim()).getName();
    }

    private String getClientModNameForRenderer(RendererInterface rendererInterface) {
        if (isMobileGluesRenderer(rendererInterface)) {
            return "ZalithLauncher";
        }
        return "JavaLauncher";
    }

    private boolean isMobileGluesRenderer(RendererInterface rendererInterface) {
        if (rendererInterface == null) {
            return false;
        }
        String lowerCase = (rendererInterface.getUniqueIdentifier() + " " + rendererInterface.getRendererName() + " " + rendererInterface.getRendererId() + " " + rendererInterface.getRendererLibrary()).toLowerCase(Locale.ROOT);
        return lowerCase.contains("mobileglues") || lowerCase.contains("mobile glues");
    }

    private boolean shouldGlfwStubInitEgl(RendererInterface rendererInterface) {
        return isGl4esRenderer(rendererInterface);
    }

    private boolean isGl4esRenderer(RendererInterface rendererInterface) {
        if (rendererInterface == null) {
            return false;
        }
        String lowerCase = (rendererInterface.getUniqueIdentifier() + " " + rendererInterface.getRendererName() + " " + rendererInterface.getRendererId() + " " + rendererInterface.getRendererLibrary()).toLowerCase(Locale.ROOT);
        return lowerCase.contains("gl4es") || lowerCase.contains("opengles2");
    }

    private void addJnaAndroidWorkarounds(ArrayList<String> arrayList, String str, String str2, boolean z) {
        File fileFindFirstLibraryInPath = findFirstLibraryInPath(str, "libjnidispatch.so");
        File fileFindFirstLibraryInPath2 = findFirstLibraryInPath(str2, "libSDL2.so");
        if (!str.trim().isEmpty()) {
            arrayList.add("-Djna.boot.library.path=" + str);
        }
        if (!str2.trim().isEmpty()) {
            arrayList.add("-Djna.library.path=" + str2);
        }
        arrayList.add("-Djna.nounpack=false");
        if (z && fileFindFirstLibraryInPath2 != null && fileFindFirstLibraryInPath2.isFile()) {
            Logging.i(TAG, "Controllable SDL2 native prepared: " + fileFindFirstLibraryInPath2.getAbsolutePath());
        }
        arrayList.add("-Djna.nosys=false");
        if (fileFindFirstLibraryInPath == null || !fileFindFirstLibraryInPath.isFile()) {
            Logging.i(TAG, "JNA Android dispatch missing. Checked path: " + str);
        } else {
            Logging.i(TAG, "JNA Android dispatch found: " + fileFindFirstLibraryInPath.getAbsolutePath());
        }
        arrayList.add("-Djna.debug_load=false");
        arrayList.add("-Dcom.sun.jna.useProtected=true");
        Logging.i(TAG, "Applied Android JNA boot path: " + str);
        Logging.i(TAG, "Applied Android JNA library path: " + str2);
    }

    private String buildJnaNativePath() {
        StringBuilder sb = new StringBuilder();
        File file = new File(PathManager.DIR_CACHE, "natives");
        addPathIfContains(sb, new File(file, this.versionId), "libjnidispatch.so");
        String strExtractMinecraftIdFromLoaderVersion = extractMinecraftIdFromLoaderVersion(this.versionId);
        if (!strExtractMinecraftIdFromLoaderVersion.equals(this.versionId)) {
            addPathIfContains(sb, new File(file, strExtractMinecraftIdFromLoaderVersion), "libjnidispatch.so");
        }
        File[] fileArrListFiles = file.listFiles(new FileFilter() { // from class: ca.dnamobile.javalauncher.launcher.JavaLaunchBuilder$$ExternalSyntheticLambda0
            @Override // java.io.FileFilter
            public final boolean accept(File file2) {
                return file2.isDirectory();
            }
        });
        if (fileArrListFiles != null) {
            Arrays.sort(fileArrListFiles, new Comparator() { // from class: ca.dnamobile.javalauncher.launcher.JavaLaunchBuilder$$ExternalSyntheticLambda1
                @Override // java.util.Comparator
                public final int compare(Object obj, Object obj2) {
                    return ((File) obj).getName().compareToIgnoreCase(((File) obj2).getName());
                }
            });
            for (File file2 : fileArrListFiles) {
                addPathIfContains(sb, file2, "libjnidispatch.so");
            }
        }
        return sb.toString();
    }

    private String extractMinecraftIdFromLoaderVersion(String str) {
        int iLastIndexOf;
        int i;
        String lowerCase = str.toLowerCase(Locale.ROOT);
        return ((lowerCase.startsWith("fabric-loader-") || lowerCase.startsWith("quilt-loader-")) && (iLastIndexOf = str.lastIndexOf(45)) > 0 && (i = iLastIndexOf + 1) < str.length()) ? str.substring(i) : str;
    }

    private void addPathIfContains(StringBuilder sb, File file, String str) {
        if (file.isDirectory() && new File(file, str).isFile()) {
            addPath(sb, file);
        }
    }

    private File findFirstLibraryInPath(String str, String str2) {
        if (str.trim().isEmpty()) {
            return null;
        }
        for (String str3 : str.split(":")) {
            if (str3 != null && !str3.trim().isEmpty()) {
                File file = new File(str3.trim(), str2);
                if (file.isFile()) {
                    return file;
                }
            }
        }
        return null;
    }

    private void addPathList(StringBuilder sb, String str) {
        if (str.trim().isEmpty()) {
            return;
        }
        for (String str2 : str.split(":")) {
            if (str2 != null && !str2.trim().isEmpty()) {
                addPath(sb, new File(str2.trim()));
            }
        }
    }

    private void addMioLibPatcherForRenderer(ArrayList<String> arrayList, RendererInterface rendererInterface) {
        if (isVulkanZinkRenderer(rendererInterface) || isLtwRenderer(rendererInterface)) {
            LibPath.refresh();
            File file = LibPath.MIO_LIB_PATCHER;
            String rendererName = rendererInterface != null ? rendererInterface.getRendererName() : "selected renderer";
            if (file != null && file.isFile()) {
                String str = "-javaagent:" + file.getAbsolutePath();
                if (arrayList.contains(str)) {
                    return;
                }
                arrayList.add(str);
                Logging.i(TAG, "Applied MioLibPatcher for " + rendererName + ": " + file.getAbsolutePath());
                return;
            }
            Logging.i(TAG, rendererName + " selected but MioLibPatcher.jar was not found at " + (file != null ? file.getAbsolutePath() : "<null>"));
        }
    }

    private boolean isVulkanZinkRenderer(RendererInterface rendererInterface) {
        if (rendererInterface == null) {
            return false;
        }
        String strRendererIdentity = rendererIdentity(rendererInterface);
        return strRendererIdentity.contains("vulkan_zink") || strRendererIdentity.contains("vulkan zink") || strRendererIdentity.contains("zink") || strRendererIdentity.contains("osmesa");
    }

    private boolean isLtwRenderer(RendererInterface rendererInterface) {
        if (rendererInterface == null) {
            return false;
        }
        String strRendererIdentity = rendererIdentity(rendererInterface);
        return strRendererIdentity.contains("ltw") || strRendererIdentity.contains("libltw.so");
    }

    private String rendererIdentity(RendererInterface rendererInterface) {
        return (rendererInterface.getUniqueIdentifier() + " " + rendererInterface.getRendererName() + " " + rendererInterface.getRendererId() + " " + rendererInterface.getRendererLibrary()).toLowerCase(Locale.ROOT);
    }

    private void addForgeJava17ModuleOpens(ArrayList<String> arrayList, JSONObject jSONObject) {
        if (isForgeOrBootstrapVersion(jSONObject)) {
            addJvmArgIfMissing(arrayList, "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED");
            addJvmArgIfMissing(arrayList, "--add-opens=java.base/java.lang=ALL-UNNAMED");
            addJvmArgIfMissing(arrayList, "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED");
            addJvmArgIfMissing(arrayList, "--add-opens=java.base/java.util=ALL-UNNAMED");
            addJvmArgIfMissing(arrayList, "--add-opens=java.base/java.util.jar=ALL-UNNAMED");
            addJvmArgIfMissing(arrayList, "--add-opens=java.base/sun.nio.fs=ALL-UNNAMED");
            Logging.i(TAG, "Applied Forge Java 17 module opens for " + jSONObject.optString("id", this.versionId));
        }
    }

    private boolean isForgeOrBootstrapVersion(JSONObject jSONObject) {
        String lowerCase = jSONObject.optString("id", this.versionId).toLowerCase(Locale.ROOT);
        String lowerCase2 = jSONObject.optString("mainClass", "").toLowerCase(Locale.ROOT);
        if (lowerCase.contains("forge") || lowerCase2.contains("cpw.mods.bootstraplauncher") || lowerCase2.contains("net.minecraftforge")) {
            return true;
        }
        JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("libraries");
        if (jSONArrayOptJSONArray == null) {
            return false;
        }
        for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
            JSONObject jSONObjectOptJSONObject = jSONArrayOptJSONArray.optJSONObject(i);
            if (jSONObjectOptJSONObject != null) {
                String lowerCase3 = jSONObjectOptJSONObject.optString("name", "").toLowerCase(Locale.ROOT);
                if (lowerCase3.contains("net.minecraftforge:forge") || lowerCase3.contains("cpw.mods:securejarhandler") || lowerCase3.contains("cpw.mods:bootstraplauncher") || lowerCase3.contains("cpw.mods:modlauncher")) {
                    return true;
                }
            }
        }
        return false;
    }

    private void addJvmArgIfMissing(ArrayList<String> arrayList, String str) {
        if (arrayList.contains(str)) {
            return;
        }
        arrayList.add(str);
    }

    private void addForgeNashornAsmModulePath(ArrayList<String> arrayList, JSONObject jSONObject) {
        if (isForgeOrBootstrapVersion(jSONObject)) {
            ArrayList<String> arrayListCollectForgeAsmModuleJars = collectForgeAsmModuleJars(jSONObject);
            if (arrayListCollectForgeAsmModuleJars.isEmpty()) {
                Logging.i(TAG, "Forge ASM module-path fix skipped; no ASM module jars found.");
                return;
            }
            String strJoinPathList = joinPathList(arrayListCollectForgeAsmModuleJars);
            mergeModulePath(arrayList, strJoinPathList);
            Logging.i(TAG, "Applied Forge ASM module path: " + strJoinPathList);
        }
    }

    private ArrayList<String> collectForgeAsmModuleJars(JSONObject jSONObject) {
        String strResolveLibraryArtifactPath;
        ArrayList<String> arrayList = new ArrayList<>();
        JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("libraries");
        if (jSONArrayOptJSONArray == null) {
            return arrayList;
        }
        for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
            JSONObject jSONObjectOptJSONObject = jSONArrayOptJSONArray.optJSONObject(i);
            if (jSONObjectOptJSONObject != null && isLibraryAllowed(jSONObjectOptJSONObject)) {
                String lowerCase = jSONObjectOptJSONObject.optString("name", "").toLowerCase(Locale.ROOT);
                if ((lowerCase.startsWith("org.ow2.asm:asm:") || lowerCase.startsWith("org.ow2.asm:asm-analysis:") || lowerCase.startsWith("org.ow2.asm:asm-commons:") || lowerCase.startsWith("org.ow2.asm:asm-tree:") || lowerCase.startsWith("org.ow2.asm:asm-util:")) && (strResolveLibraryArtifactPath = resolveLibraryArtifactPath(jSONObjectOptJSONObject)) != null && !strResolveLibraryArtifactPath.isEmpty()) {
                    File file = new File(MinecraftVersionInstaller.getLibrariesDirectory(), strResolveLibraryArtifactPath);
                    if (file.isFile()) {
                        addPathIfMissing(arrayList, file.getAbsolutePath());
                    } else {
                        Logging.i(TAG, "Forge ASM module-path dependency is missing: " + file.getAbsolutePath());
                    }
                }
            }
        }
        return arrayList;
    }

    private void mergeModulePath(ArrayList<String> arrayList, String str) {
        if (str.isEmpty()) {
            return;
        }
        for (int i = 0; i < arrayList.size(); i++) {
            String str2 = arrayList.get(i);
            if ("--module-path".equals(str2) || "-p".equals(str2)) {
                int i2 = i + 1;
                if (i2 < arrayList.size()) {
                    arrayList.set(i2, appendUniquePathEntries(arrayList.get(i2), str));
                    return;
                } else {
                    arrayList.add(str);
                    return;
                }
            }
            if (str2.startsWith("--module-path=")) {
                arrayList.set(i, "--module-path=" + appendUniquePathEntries(str2.substring("--module-path=".length()), str));
                return;
            }
        }
        arrayList.add("--module-path");
        arrayList.add(str);
    }

    private String appendUniquePathEntries(String str, String str2) {
        ArrayList<String> arrayList = new ArrayList<>();
        if (str != null && !str.trim().isEmpty()) {
            for (String str3 : str.split(":")) {
                if (str3 != null && !str3.trim().isEmpty()) {
                    addPathIfMissing(arrayList, str3.trim());
                }
            }
        }
        for (String str4 : str2.split(":")) {
            if (str4 != null && !str4.trim().isEmpty()) {
                addPathIfMissing(arrayList, str4.trim());
            }
        }
        return joinPathList(arrayList);
    }

    private void addPathIfMissing(ArrayList<String> arrayList, String str) {
        if (arrayList.contains(str)) {
            return;
        }
        arrayList.add(str);
    }

    private String joinPathList(ArrayList<String> arrayList) {
        StringBuilder sb = new StringBuilder();
        for (String str : arrayList) {
            if (sb.length() > 0) {
                sb.append(':');
            }
            sb.append(str);
        }
        return sb.toString();
    }

    private void addMojangJvmArguments(ArrayList<String> arrayList, JSONObject jSONObject, Map<String, String> map) throws Exception {
        JSONArray jSONArrayOptJSONArray;
        JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject("arguments");
        if (jSONObjectOptJSONObject == null || (jSONArrayOptJSONArray = jSONObjectOptJSONObject.optJSONArray("jvm")) == null) {
            return;
        }
        addArgumentArray(arrayList, jSONArrayOptJSONArray, map);
    }

    private List<String> buildCacioJvmArgs(File file) {
        ArrayList arrayList = new ArrayList();
        boolean zContains = file.getName().contains("8");
        File file2 = zContains ? LibPath.CACIO_8 : LibPath.CACIO_17;
        String strBuildJarClassPath = buildJarClassPath(file2);
        if (strBuildJarClassPath.isEmpty()) {
            Logging.i(TAG, "Cacio AWT jars are missing, skipping AWT backend: " + file2.getAbsolutePath());
            return arrayList;
        }
        arrayList.add("-Djava.awt.headless=false");
        arrayList.add("-Dcacio.managed.screensize=" + this.width + "x" + this.height);
        arrayList.add("-Dcacio.font.fontmanager=sun.awt.X11FontManager");
        arrayList.add("-Dcacio.font.fontscaler=sun.font.FreetypeFontScaler");
        arrayList.add("-Dswing.defaultlaf=javax.swing.plaf.nimbus.NimbusLookAndFeel");
        if (zContains) {
            arrayList.add("-Dawt.toolkit=net.java.openjdk.cacio.ctc.CTCToolkit");
            arrayList.add("-Djava.awt.graphicsenv=net.java.openjdk.cacio.ctc.CTCGraphicsEnvironment");
            arrayList.add("-Xbootclasspath/p:" + strBuildJarClassPath);
        } else {
            arrayList.add("-Dawt.toolkit=com.github.caciocavallosilano.cacio.ctc.CTCToolkit");
            arrayList.add("-Djava.awt.graphicsenv=com.github.caciocavallosilano.cacio.ctc.CTCGraphicsEnvironment");
            if (LibPath.CACIO_17_AGENT != null && LibPath.CACIO_17_AGENT.isFile()) {
                arrayList.add("-javaagent:" + LibPath.CACIO_17_AGENT.getAbsolutePath());
            }
            arrayList.add("--add-exports=java.desktop/java.awt=ALL-UNNAMED");
            arrayList.add("--add-exports=java.desktop/java.awt.peer=ALL-UNNAMED");
            arrayList.add("--add-exports=java.desktop/sun.awt.image=ALL-UNNAMED");
            arrayList.add("--add-exports=java.desktop/sun.java2d=ALL-UNNAMED");
            arrayList.add("--add-exports=java.desktop/java.awt.dnd.peer=ALL-UNNAMED");
            arrayList.add("--add-exports=java.desktop/sun.awt=ALL-UNNAMED");
            arrayList.add("--add-exports=java.desktop/sun.awt.event=ALL-UNNAMED");
            arrayList.add("--add-exports=java.desktop/sun.awt.datatransfer=ALL-UNNAMED");
            arrayList.add("--add-exports=java.desktop/sun.font=ALL-UNNAMED");
            arrayList.add("--add-exports=java.base/sun.security.action=ALL-UNNAMED");
            arrayList.add("--add-opens=java.base/java.util=ALL-UNNAMED");
            arrayList.add("--add-opens=java.desktop/java.awt=ALL-UNNAMED");
            arrayList.add("--add-opens=java.desktop/sun.font=ALL-UNNAMED");
            arrayList.add("--add-opens=java.desktop/sun.java2d=ALL-UNNAMED");
            arrayList.add("--add-opens=java.base/java.lang.reflect=ALL-UNNAMED");
            arrayList.add("--add-opens=java.base/java.net=ALL-UNNAMED");
            arrayList.add("-Xbootclasspath/a:" + strBuildJarClassPath);
        }
        Logging.i(TAG, "Enabled Cacio AWT backend from " + file2.getAbsolutePath());
        return arrayList;
    }

    private String buildJarClassPath(File file) {
        File[] fileArrListFiles;
        if (file == null || !file.isDirectory() || (fileArrListFiles = file.listFiles(new FilenameFilter() { // from class: ca.dnamobile.javalauncher.launcher.JavaLaunchBuilder$$ExternalSyntheticLambda3
            @Override // java.io.FilenameFilter
            public final boolean accept(File file2, String str) {
                return str.endsWith(".jar");
            }
        })) == null || fileArrListFiles.length == 0) {
            return "";
        }
        Arrays.sort(fileArrListFiles, new Comparator() { // from class: ca.dnamobile.javalauncher.launcher.JavaLaunchBuilder$$ExternalSyntheticLambda4
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                return ((File) obj).getName().compareToIgnoreCase(((File) obj2).getName());
            }
        });
        StringBuilder sb = new StringBuilder();
        for (File file2 : fileArrListFiles) {
            if (sb.length() > 0) {
                sb.append(':');
            }
            sb.append(file2.getAbsolutePath());
        }
        return sb.toString();
    }

    private String buildNativeLibraryPath(File file, File file2, String str, RendererInterface rendererInterface) {
        File fileResolveRuntimeLibDir = resolveRuntimeLibDir(file);
        File file3 = new File(fileResolveRuntimeLibDir, "server");
        File file4 = new File(fileResolveRuntimeLibDir, "client");
        if (!new File(file3, "libjvm.so").isFile()) {
            file3 = file4;
        }
        String str2 = (Build.SUPPORTED_64_BIT_ABIS == null || Build.SUPPORTED_64_BIT_ABIS.length <= 0) ? "lib" : "lib64";
        StringBuilder sb = new StringBuilder();
        addPath(sb, file3);
        addPath(sb, new File(fileResolveRuntimeLibDir, "jli"));
        addPath(sb, fileResolveRuntimeLibDir);
        addPath(sb, file2);
        addPathList(sb, str);
        if (rendererInterface != null) {
            Iterator<File> it = rendererInterface.getLibrarySearchPaths().iterator();
            while (it.hasNext()) {
                addPath(sb, it.next());
            }
            Iterator<File> it2 = DriverPluginManager.getSelectedDriverLibrarySearchPaths(this.context, rendererInterface).iterator();
            while (it2.hasNext()) {
                addPath(sb, it2.next());
            }
        }
        addPath(sb, new File("/system/".concat(str2)));
        addPath(sb, new File("/vendor/".concat(str2)));
        addPath(sb, new File("/vendor/" + str2 + "/hw"));
        if (PathManager.DIR_RUNTIME_MOD != null) {
            addPath(sb, PathManager.DIR_RUNTIME_MOD);
        }
        addPath(sb, new File(PathManager.DIR_NATIVE_LIB));
        return sb.toString();
    }

    private String buildBootLibraryPath(File file) {
        File fileResolveRuntimeLibDir = resolveRuntimeLibDir(file);
        StringBuilder sb = new StringBuilder();
        addPath(sb, fileResolveRuntimeLibDir);
        addPath(sb, new File(fileResolveRuntimeLibDir, "server"));
        addPath(sb, new File(fileResolveRuntimeLibDir, "client"));
        addPath(sb, new File(fileResolveRuntimeLibDir, "jli"));
        addPath(sb, new File(PathManager.DIR_NATIVE_LIB));
        return sb.toString();
    }

    private File resolveRuntimeLibDir(File file) {
        Iterator<String> it = getRuntimeArchCandidates().iterator();
        while (it.hasNext()) {
            File file2 = new File(file, "lib/" + it.next());
            if (file2.isDirectory()) {
                return file2;
            }
        }
        return new File(file, "lib");
    }

    private List<String> getRuntimeArchCandidates() {
        ArrayList arrayList = new ArrayList();
        String strArchAsString = Architecture.archAsString(Architecture.getDeviceArchitecture());
        addUnique(arrayList, strArchAsString);
        if (Architecture.getDeviceArchitecture() == 1 || strArchAsString.contains("arm64") || strArchAsString.contains("aarch64")) {
            addUnique(arrayList, "aarch64");
            addUnique(arrayList, "arm64");
            addUnique(arrayList, "arm64-v8a");
        } else if (Architecture.getDeviceArchitecture() == 0 || strArchAsString.contains("arm")) {
            addUnique(arrayList, "arm");
            addUnique(arrayList, "armeabi-v7a");
        } else if (Architecture.getDeviceArchitecture() == 2) {
            addUnique(arrayList, "i386");
            addUnique(arrayList, "i486");
            addUnique(arrayList, "i586");
            addUnique(arrayList, "x86");
        } else if (strArchAsString.contains("x86_64") || strArchAsString.contains("amd64")) {
            addUnique(arrayList, "amd64");
            addUnique(arrayList, "x86_64");
        }
        return arrayList;
    }

    private void addUnique(List<String> list, String str) {
        if (str == null || str.trim().isEmpty()) {
            return;
        }
        for (String str2 : str.split("/")) {
            if (!str2.trim().isEmpty() && !list.contains(str2)) {
                list.add(str2);
            }
        }
    }

    private void addPath(StringBuilder sb, File file) {
        if (file.isDirectory()) {
            String absolutePath = file.getAbsolutePath();
            if (containsPath(sb.toString(), absolutePath)) {
                return;
            }
            if (sb.length() > 0) {
                sb.append(':');
            }
            sb.append(absolutePath);
        }
    }

    private boolean containsPath(String str, String str2) {
        if (str.isEmpty()) {
            return false;
        }
        for (String str3 : str.split(":")) {
            if (str2.equals(str3)) {
                return true;
            }
        }
        return false;
    }

    private ArrayList<String> buildGameArgs(JSONObject jSONObject, Map<String, String> map) throws Exception {
        JSONArray jSONArrayOptJSONArray;
        ArrayList<String> arrayList = new ArrayList<>();
        JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject("arguments");
        if (jSONObjectOptJSONObject != null && (jSONArrayOptJSONArray = jSONObjectOptJSONObject.optJSONArray("game")) != null) {
            addArgumentArray(arrayList, jSONArrayOptJSONArray, map);
            sanitizeAndRepairGameArgs(arrayList, map);
            return arrayList;
        }
        String strOptString = jSONObject.optString("minecraftArguments", "");
        if (!strOptString.isEmpty()) {
            for (String str : splitLegacyArguments(strOptString)) {
                if (!str.trim().isEmpty()) {
                    arrayList.add(replaceTokens(str.trim(), map));
                }
            }
        }
        sanitizeAndRepairGameArgs(arrayList, map);
        return arrayList;
    }

    private List<String> splitLegacyArguments(String str) {
        ArrayList arrayList = new ArrayList();
        StringBuilder sb = new StringBuilder();
        boolean z = false;
        for (int i = 0; i < str.length(); i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt == '\"') {
                z = !z;
            } else if (Character.isWhitespace(cCharAt) && !z) {
                if (sb.length() > 0) {
                    arrayList.add(sb.toString());
                    sb.setLength(0);
                }
            } else {
                sb.append(cCharAt);
            }
        }
        if (sb.length() > 0) {
            arrayList.add(sb.toString());
        }
        return arrayList;
    }

    private void sanitizeAndRepairGameArgs(ArrayList<String> arrayList, Map<String, String> map) {
        int i = 0;
        while (i < arrayList.size()) {
            String str = arrayList.get(i);
            if (str == null || str.trim().isEmpty()) {
                arrayList.remove(i);
            } else {
                if (str.contains("${")) {
                    Logging.i(TAG, "Removing unresolved launch argument: " + str);
                    arrayList.remove(i);
                    if (i > 0) {
                        int i2 = i - 1;
                        if (arrayList.get(i2).startsWith("--")) {
                            Logging.i(TAG, "Removing option for unresolved value: " + arrayList.get(i2));
                            arrayList.remove(i2);
                            i -= 2;
                        }
                    }
                }
                i++;
            }
            i--;
            i++;
        }
        ensureOptionHasValue(arrayList, "--gameDir", map.get("${game_directory}"));
        ensureOptionHasValue(arrayList, "--assetsDir", map.get("${game_assets}"));
        ensureOptionHasValue(arrayList, "--assetIndex", map.get("${assets_index_name}"));
    }

    private void ensureOptionHasValue(ArrayList<String> arrayList, String str, String str2) {
        if (str2 == null || str2.trim().isEmpty()) {
            return;
        }
        for (int i = 0; i < arrayList.size(); i++) {
            if (str.equals(arrayList.get(i))) {
                int i2 = i + 1;
                if (i2 >= arrayList.size() || arrayList.get(i2).startsWith("--")) {
                    Logging.i(TAG, "Repairing missing launch argument value for " + str + " -> " + str2);
                    arrayList.add(i2, str2);
                    return;
                }
                return;
            }
        }
    }

    private void addArgumentArray(ArrayList<String> arrayList, JSONArray jSONArray, Map<String, String> map) throws Exception {
        for (int i = 0; i < jSONArray.length(); i++) {
            Object obj = jSONArray.get(i);
            if (obj instanceof String) {
                arrayList.add(replaceTokens((String) obj, map));
            } else if (obj instanceof JSONObject) {
                JSONObject jSONObject = (JSONObject) obj;
                if (isRulesAllowed(jSONObject.optJSONArray("rules"))) {
                    Object objOpt = jSONObject.opt("value");
                    if (objOpt instanceof String) {
                        arrayList.add(replaceTokens((String) objOpt, map));
                    } else if (objOpt instanceof JSONArray) {
                        JSONArray jSONArray2 = (JSONArray) objOpt;
                        for (int i2 = 0; i2 < jSONArray2.length(); i2++) {
                            arrayList.add(replaceTokens(jSONArray2.getString(i2), map));
                        }
                    }
                }
            }
        }
    }

    private boolean isRulesAllowed(JSONArray jSONArray) {
        if (jSONArray == null || jSONArray.length() == 0) {
            return true;
        }
        boolean zEquals = false;
        for (int i = 0; i < jSONArray.length(); i++) {
            JSONObject jSONObjectOptJSONObject = jSONArray.optJSONObject(i);
            if (jSONObjectOptJSONObject != null && doesRuleApply(jSONObjectOptJSONObject)) {
                zEquals = "allow".equals(jSONObjectOptJSONObject.optString("action", "allow"));
            }
        }
        return zEquals;
    }

    private boolean doesRuleApply(JSONObject jSONObject) {
        JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject("os");
        if (jSONObjectOptJSONObject != null && !"linux".equals(jSONObjectOptJSONObject.optString("name", "linux"))) {
            return false;
        }
        JSONObject jSONObjectOptJSONObject2 = jSONObject.optJSONObject("features");
        if (jSONObjectOptJSONObject2 == null) {
            return true;
        }
        Iterator<String> itKeys = jSONObjectOptJSONObject2.keys();
        while (itKeys.hasNext()) {
            String next = itKeys.next();
            if (getFeatureFlag(next) != jSONObjectOptJSONObject2.optBoolean(next, false)) {
                return false;
            }
        }
        return true;
    }

    private boolean getFeatureFlag(String str) {
        if ("has_custom_resolution".equals(str)) {
            return true;
        }
        if ("is_demo_user".equals(str) || "is_quick_play_singleplayer".equals(str) || "is_quick_play_multiplayer".equals(str)) {
            return false;
        }
        "is_quick_play_realms".equals(str);
        return false;
    }

    private String replaceTokens(String str, Map<String, String> map) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            str = str.replace(entry.getKey(), entry.getValue());
        }
        return str;
    }

    private void removeClasspathArgs(ArrayList<String> arrayList) {
        int i = 0;
        while (i < arrayList.size()) {
            String str = arrayList.get(i);
            if ("-cp".equals(str) || "-classpath".equals(str) || "--class-path".equals(str)) {
                arrayList.remove(i);
                if (i < arrayList.size()) {
                    arrayList.remove(i);
                }
                i--;
            }
            i++;
        }
    }

    private void purgeZalithManagedArgs(ArrayList<String> arrayList) {
        purgeArg(arrayList, "-Xms");
        purgeArg(arrayList, "-Xmx");
        purgeArg(arrayList, "-d32");
        purgeArg(arrayList, "-d64");
        purgeArg(arrayList, "-Xint");
        purgeArg(arrayList, "-XX:+UseTransparentHugePages");
        purgeArg(arrayList, "-XX:+UseLargePagesInMetaspace");
        purgeArg(arrayList, "-XX:+UseLargePages");
        purgeArg(arrayList, "-Djava.library.path=");
        purgeArg(arrayList, "-Djna.boot.library.path=");
        purgeArg(arrayList, "-Djna.library.path=");
        purgeArg(arrayList, "-Djna.nounpack=");
        purgeArg(arrayList, "-Djna.nosys=");
        purgeArg(arrayList, "-Djna.debug_load=");
        purgeArg(arrayList, "-Dcom.sun.jna.useProtected=");
        purgeArg(arrayList, "-Djna.tmpdir=");
        purgeArg(arrayList, "-Dorg.lwjgl.librarypath=");
        purgeArg(arrayList, "-Dorg.lwjgl.opengl.libname=");
        purgeArg(arrayList, "-Dorg.lwjgl.freetype.libname=");
        purgeArg(arrayList, "-Dorg.lwjgl.system.SharedLibraryExtractPath=");
        purgeArg(arrayList, "-Dio.netty.native.workdir=");
        purgeArg(arrayList, "-XX:ActiveProcessorCount=");
    }

    private void purgeArg(ArrayList<String> arrayList, final String str) {
        arrayList.removeIf(new Predicate() { // from class: ca.dnamobile.javalauncher.launcher.JavaLaunchBuilder$$ExternalSyntheticLambda2
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return ((String) obj).startsWith(str);
            }
        });
    }

    private void copyFile(File file, File file2) throws Exception {
        File parentFile = file2.getParentFile();
        if (parentFile != null && !parentFile.exists() && !parentFile.mkdirs()) {
            throw new IllegalStateException("Unable to create directory: " + parentFile.getAbsolutePath());
        }
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file2);
            try {
                byte[] bArr = new byte[8192];
                while (true) {
                    int i = fileInputStream.read(bArr);
                    if (i >= 0) {
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

    private String readFile(File file) throws Exception {
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

    private void writeDebugLaunchFile(File file, File file2, String str, String str2, List<String> list, List<String> list2) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("version=").append(this.versionId).append('\n');
            sb.append("accountLoaded=").append(this.account != null).append('\n');
            StringBuilder sbAppend = sb.append("accountType=");
            AccountStore.Account account = this.account;
            sbAppend.append(account != null ? account.accountType : "none").append('\n');
            sb.append("hasMinecraftSession=").append(hasValidMinecraftSession()).append('\n');
            sb.append("customSkinEnabled=").append(isCustomSkinEnabledForLaunch(resolvePlayerName())).append('\n');
            sb.append("runtime=").append(file2.getAbsolutePath()).append('\n');
            sb.append("mainClass=").append(str).append('\n');
            sb.append("classpath=").append(str2).append('\n');
            sb.append("\nJVM ARGS\n");
            Iterator<String> it = list.iterator();
            while (it.hasNext()) {
                sb.append(it.next()).append('\n');
            }
            sb.append("\nGAME ARGS\n");
            Iterator<String> it2 = list2.iterator();
            while (it2.hasNext()) {
                sb.append(it2.next()).append('\n');
            }
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            try {
                fileOutputStream.write(sb.toString().getBytes(StandardCharsets.UTF_8));
                fileOutputStream.close();
            } finally {
            }
        } catch (Throwable th) {
            Logging.e(TAG, "Failed to write debug launch plan", th);
        }
    }
}
