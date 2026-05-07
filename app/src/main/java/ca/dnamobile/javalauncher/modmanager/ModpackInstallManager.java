package ca.dnamobile.javalauncher.modmanager;

import android.content.Context;
import android.net.Uri;
import androidx.browser.trusted.sharing.ShareTarget;
import ca.dnamobile.javalauncher.data.model.MinecraftVersion;
import ca.dnamobile.javalauncher.feature.log.Logging;
import ca.dnamobile.javalauncher.instance.LauncherInstance;
import ca.dnamobile.javalauncher.instance.LauncherInstanceManager;
import ca.dnamobile.javalauncher.ui.version.MinecraftVersionInstaller;
import ca.dnamobile.javalauncher.ui.version.MinecraftVersionManifestClient;
import ca.dnamobile.javalauncher.utils.path.PathManager;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class ModpackInstallManager {
    private static final int BUFFER_SIZE = 65536;
    private static final int CURSEFORGE_MINECRAFT_GAME_ID = 432;
    private static final String JAVALAUNCHER_METADATA_DIRECTORY = ".javalauncher";
    private static final String JAVALAUNCHER_PACK_ICON_ENTRY = "javalauncher-pack-icon.png";
    private static final String MODPACK_FILES_MANIFEST_FILE = "modpack_files_manifest.json";
    private static final String MODPACK_INSTALL_WARNINGS_FILE = "modpack_install_warnings.txt";
    private static final String MODPACK_MANIFEST_FILE = "modpack_manifest.json";
    private static final String TAG = "ModpackInstall";

    private ModpackInstallManager() {
    }

    public interface Listener {
        void onComplete(String str);

        void onError(Throwable th);

        void onProgress(int i, int i2);

        void onStatus(String str);

        default void onComplete(String str, LauncherInstance launcherInstance) {
            onComplete(str);
        }
    }

    public static void installFromProject(Context context, ModManagerSource modManagerSource, String str, String str2, String str3, String str4, String str5, String str6, Listener listener) {
        File fileDownloadModrinthModpack;
        try {
            PathManager.initContextConstants(context);
            listener.onStatus("Finding latest compatible modpack version...");
            if (modManagerSource == ModManagerSource.CURSEFORGE) {
                fileDownloadModrinthModpack = downloadCurseForgeModpack(context, str, str3, str5, str6, listener);
            } else {
                fileDownloadModrinthModpack = downloadModrinthModpack(context, str, str2, str3, str5, str6, listener);
            }
            installFromLocalFile(context, fileDownloadModrinthModpack, str4, listener);
        } catch (Throwable th) {
            Logging.e(TAG, "Unable to install modpack project " + str, th);
            listener.onError(th);
        }
    }

    public static ArrayList<ModpackVersionChoice> listProjectVersions(Context context, ModManagerSource modManagerSource, String str, String str2) throws Exception {
        PathManager.initContextConstants(context);
        if (modManagerSource == ModManagerSource.CURSEFORGE) {
            return listCurseForgeModpackVersions(str);
        }
        return listModrinthModpackVersions(str, str2);
    }

    public static void installFromProjectVersion(Context context, ModManagerSource modManagerSource, String str, String str2, String str3, String str4, ModpackVersionChoice modpackVersionChoice, Listener listener) {
        File fileDownloadModrinthModpackVersion;
        try {
            PathManager.initContextConstants(context);
            listener.onStatus("Preparing " + modpackVersionChoice.getDisplayTitle() + "...");
            if (modManagerSource == ModManagerSource.CURSEFORGE) {
                fileDownloadModrinthModpackVersion = downloadCurseForgeModpackVersion(context, str, str3, modpackVersionChoice, listener);
            } else {
                fileDownloadModrinthModpackVersion = downloadModrinthModpackVersion(context, str, str2, str3, modpackVersionChoice, listener);
            }
            installFromLocalFile(context, fileDownloadModrinthModpackVersion, str4, listener);
        } catch (Throwable th) {
            Logging.e(TAG, "Unable to install selected modpack version for " + str, th);
            listener.onError(th);
        }
    }

    public static void importFromUri(Context context, Uri uri, Listener listener) {
        InputStream inputStreamOpenInputStream;
        File fileCreateTempFile = null;
        try {
            PathManager.initContextConstants(context);
            listener.onStatus("Preparing selected modpack...");
            fileCreateTempFile = File.createTempFile("javalauncher-modpack-import-", ".zip", context.getCacheDir());
            inputStreamOpenInputStream = context.getContentResolver().openInputStream(uri);
        } catch (Throwable th) {
            try {
                Logging.e(TAG, "Unable to import modpack", th);
                listener.onError(th);
                if (fileCreateTempFile == null) {
                    return;
                }
            } finally {
                if (fileCreateTempFile != null) {
                    fileCreateTempFile.delete();
                }
            }
        }
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(fileCreateTempFile);
            try {
                if (inputStreamOpenInputStream == null) {
                    throw new IOException("Unable to open selected modpack file.");
                }
                copyStream(inputStreamOpenInputStream, fileOutputStream);
                fileOutputStream.close();
                if (inputStreamOpenInputStream != null) {
                    inputStreamOpenInputStream.close();
                }
                installFromLocalFile(context, fileCreateTempFile, listener);
                if (fileCreateTempFile == null) {
                }
            } finally {
            }
        } finally {
        }
    }

    public static void installFromLocalFile(Context context, File file, Listener listener) throws Exception {
        installFromLocalFile(context, file, null, listener);
    }

    private static void installFromLocalFile(Context context, File file, String str, Listener listener) throws Exception {
        if (!file.isFile()) {
            throw new IOException("Modpack file not found: " + file.getAbsolutePath());
        }
        ZipFile zipFile = new ZipFile(file);
        try {
            if (zipFile.getEntry("modrinth.index.json") != null) {
                installMrpack(context, file, str, listener);
                zipFile.close();
            } else {
                if (zipFile.getEntry("manifest.json") != null) {
                    installCurseForgePack(context, file, str, listener);
                    zipFile.close();
                    return;
                }
                String strFindMultiMcRootPrefix = findMultiMcRootPrefix(zipFile);
                if (strFindMultiMcRootPrefix != null) {
                    installMultiMcPack(context, file, strFindMultiMcRootPrefix, listener);
                    zipFile.close();
                } else {
                    zipFile.close();
                    throw new IllegalArgumentException("Unsupported modpack. Select a Modrinth .mrpack, CurseForge exported .zip, or MultiMC/Prism exported .zip.");
                }
            }
        } catch (Throwable th) {
            try {
                zipFile.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    private static ArrayList<ModpackVersionChoice> listModrinthModpackVersions(String str, String str2) throws Exception {
        JSONObject jSONObjectFindModrinthMrpackFile;
        String strTrim = !isBlank(str) ? str.trim() : safe(str2);
        if (isBlank(strTrim)) {
            throw new IllegalArgumentException("Missing Modrinth project id.");
        }
        JSONArray jSONArray = new JSONArray(httpGetString("https://api.modrinth.com/v2/project/" + urlEncode(strTrim) + "/version", null));
        ArrayList<ModpackVersionChoice> arrayList = new ArrayList<>();
        for (int i = 0; i < jSONArray.length(); i++) {
            JSONObject jSONObjectOptJSONObject = jSONArray.optJSONObject(i);
            if (jSONObjectOptJSONObject != null && (jSONObjectFindModrinthMrpackFile = findModrinthMrpackFile(jSONObjectOptJSONObject)) != null) {
                ArrayList<String> arrayListJsonArrayToStringList = jsonArrayToStringList(jSONObjectOptJSONObject.optJSONArray("game_versions"));
                ArrayList<String> arrayListJsonArrayToStringList2 = jsonArrayToStringList(jSONObjectOptJSONObject.optJSONArray("loaders"));
                String strTrim2 = jSONObjectOptJSONObject.optString("id", "").trim();
                String strFirstNonBlank = firstNonBlank(jSONObjectOptJSONObject.optString("name", ""), jSONObjectOptJSONObject.optString("version_number", ""));
                String strTrim3 = jSONObjectOptJSONObject.optString("version_number", "").trim();
                String strTrim4 = jSONObjectOptJSONObject.optString("date_published", "").trim();
                String strSanitizeFileName = sanitizeFileName(jSONObjectFindModrinthMrpackFile.optString("filename", ""), "modpack.mrpack");
                String strCleanDownloadUrl = cleanDownloadUrl(jSONObjectFindModrinthMrpackFile.optString("url", ""));
                if (!isBlank(strTrim2) && isHttpUrl(strCleanDownloadUrl)) {
                    arrayList.add(new ModpackVersionChoice(ModManagerSource.MODRINTH, strTrim2, 0, strFirstNonBlank, strTrim3, strSanitizeFileName, strCleanDownloadUrl, arrayListJsonArrayToStringList, arrayListJsonArrayToStringList2, strTrim4));
                }
            }
        }
        return arrayList;
    }

    private static JSONObject findModrinthMrpackFile(JSONObject jSONObject) {
        JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("files");
        JSONObject jSONObject2 = null;
        if (jSONArrayOptJSONArray == null) {
            return null;
        }
        for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
            JSONObject jSONObjectOptJSONObject = jSONArrayOptJSONArray.optJSONObject(i);
            if (jSONObjectOptJSONObject != null && jSONObjectOptJSONObject.optString("filename", "").toLowerCase(Locale.US).endsWith(".mrpack")) {
                if (jSONObject2 == null) {
                    jSONObject2 = jSONObjectOptJSONObject;
                }
                if (jSONObjectOptJSONObject.optBoolean("primary", false)) {
                    return jSONObjectOptJSONObject;
                }
            }
        }
        return jSONObject2;
    }

    private static File downloadModrinthModpackVersion(Context context, String str, String str2, String str3, ModpackVersionChoice modpackVersionChoice, Listener listener) throws Exception {
        String strCleanDownloadUrl = cleanDownloadUrl(modpackVersionChoice.downloadUrl);
        if (!isHttpUrl(strCleanDownloadUrl)) {
            Iterator<ModpackVersionChoice> it = listModrinthModpackVersions(str, str2).iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                ModpackVersionChoice next = it.next();
                if (next.versionId.equals(modpackVersionChoice.versionId)) {
                    strCleanDownloadUrl = cleanDownloadUrl(next.downloadUrl);
                    if (isHttpUrl(strCleanDownloadUrl)) {
                        modpackVersionChoice = next;
                        break;
                    }
                }
            }
        }
        if (!isHttpUrl(strCleanDownloadUrl)) {
            throw new IOException("The selected Modrinth modpack version does not have a downloadable .mrpack file.");
        }
        String strSanitizeFileName = sanitizeFileName(modpackVersionChoice.fileName, sanitizeFileName(safe(str3) + "-" + modpackVersionChoice.getDisplayTitle() + ".mrpack", "modpack.mrpack"));
        File file = new File(context.getCacheDir(), strSanitizeFileName);
        listener.onStatus("Downloading " + strSanitizeFileName + "...");
        downloadFile(strCleanDownloadUrl, file, listener);
        return file;
    }

    private static ArrayList<ModpackVersionChoice> listCurseForgeModpackVersions(String str) throws Exception {
        int iOptInt;
        int positiveInt = parsePositiveInt(str, "CurseForge project id");
        String strResolve = CurseForgeApiKeyProvider.resolve();
        if (isBlank(strResolve)) {
            throw new IOException("Missing CurseForge API key.");
        }
        ArrayList<ModpackVersionChoice> arrayList = new ArrayList<>();
        int iOptInt2 = Integer.MAX_VALUE;
        int length = 0;
        while (length < iOptInt2) {
            JSONObject jSONObject = new JSONObject(httpGetString("https://api.curseforge.com/v1/mods/" + positiveInt + "/files?pageSize=50&index=" + length + "&gameId=432", strResolve));
            JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject("pagination");
            if (jSONObjectOptJSONObject != null) {
                iOptInt2 = jSONObjectOptJSONObject.optInt("totalCount", iOptInt2);
            }
            JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("data");
            if (jSONArrayOptJSONArray == null || jSONArrayOptJSONArray.length() == 0) {
                break;
            }
            for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
                JSONObject jSONObjectOptJSONObject2 = jSONArrayOptJSONArray.optJSONObject(i);
                if (jSONObjectOptJSONObject2 != null) {
                    String strSanitizeFileName = sanitizeFileName(optJsonString(jSONObjectOptJSONObject2, "fileName"), "");
                    if (!isBlank(strSanitizeFileName) && strSanitizeFileName.toLowerCase(Locale.US).endsWith(".zip") && (iOptInt = jSONObjectOptJSONObject2.optInt("id", 0)) > 0) {
                        ArrayList<String> arrayListJsonArrayToStringList = jsonArrayToStringList(jSONObjectOptJSONObject2.optJSONArray("gameVersions"));
                        ArrayList<String> arrayListExtractCurseForgeLoaders = extractCurseForgeLoaders(arrayListJsonArrayToStringList);
                        String strFirstNonBlank = firstNonBlank(optJsonString(jSONObjectOptJSONObject2, "displayName"), stripArchiveExtension(strSanitizeFileName));
                        arrayList.add(new ModpackVersionChoice(ModManagerSource.CURSEFORGE, String.valueOf(iOptInt), iOptInt, strFirstNonBlank, strFirstNonBlank, strSanitizeFileName, cleanDownloadUrl(optJsonString(jSONObjectOptJSONObject2, "downloadUrl")), arrayListJsonArrayToStringList, arrayListExtractCurseForgeLoaders, firstNonBlank(optJsonString(jSONObjectOptJSONObject2, "fileDate"), optJsonString(jSONObjectOptJSONObject2, "dateCreated"))));
                    }
                }
            }
            length += jSONArrayOptJSONArray.length();
            if (jSONObjectOptJSONObject == null || jSONArrayOptJSONArray.length() < 50) {
                break;
            }
        }
        return arrayList;
    }

    private static File downloadCurseForgeModpackVersion(Context context, String str, String str2, ModpackVersionChoice modpackVersionChoice, Listener listener) throws Exception {
        int positiveInt = parsePositiveInt(str, "CurseForge project id");
        int positiveInt2 = modpackVersionChoice.fileId > 0 ? modpackVersionChoice.fileId : parsePositiveInt(modpackVersionChoice.versionId, "CurseForge file id");
        String strResolve = CurseForgeApiKeyProvider.resolve();
        if (isBlank(strResolve)) {
            throw new IOException("Missing CurseForge API key.");
        }
        JSONObject jSONObjectOptJSONObject = new JSONObject(httpGetString("https://api.curseforge.com/v1/mods/" + positiveInt + "/files/" + positiveInt2, strResolve)).optJSONObject("data");
        if (jSONObjectOptJSONObject == null) {
            throw new IOException("The selected CurseForge modpack file could not be loaded.");
        }
        String strSanitizeFileName = sanitizeFileName(optJsonString(jSONObjectOptJSONObject, "fileName"), sanitizeFileName(modpackVersionChoice.fileName, sanitizeFileName(safe(str2) + "-" + positiveInt2 + ".zip", "modpack.zip")));
        String strResolveCurseForgeDownloadUrl = resolveCurseForgeDownloadUrl(strResolve, positiveInt, positiveInt2, strSanitizeFileName, jSONObjectOptJSONObject);
        if (isBlank(strResolveCurseForgeDownloadUrl)) {
            throw new IOException("CurseForge did not provide a downloadable URL for this modpack file: " + strSanitizeFileName);
        }
        File file = new File(context.getCacheDir(), strSanitizeFileName);
        listener.onStatus("Downloading " + strSanitizeFileName + "...");
        downloadFile(strResolveCurseForgeDownloadUrl, file, listener);
        return file;
    }

    private static File downloadModrinthModpack(Context context, String str, String str2, String str3, String str4, String str5, Listener listener) throws Exception {
        String strTrim = !isBlank(str) ? str.trim() : safe(str2);
        if (isBlank(strTrim)) {
            throw new IllegalArgumentException("Missing Modrinth project id.");
        }
        JSONObject jSONObject = null;
        JSONObject jSONObjectSelectModrinthVersion = selectModrinthVersion(new JSONArray(httpGetString("https://api.modrinth.com/v2/project/" + urlEncode(strTrim) + "/version", null)), str4, str5);
        if (jSONObjectSelectModrinthVersion == null) {
            throw new IOException("No compatible Modrinth modpack version was found.");
        }
        JSONArray jSONArrayOptJSONArray = jSONObjectSelectModrinthVersion.optJSONArray("files");
        if (jSONArrayOptJSONArray != null) {
            int i = 0;
            while (true) {
                if (i >= jSONArrayOptJSONArray.length()) {
                    break;
                }
                JSONObject jSONObjectOptJSONObject = jSONArrayOptJSONArray.optJSONObject(i);
                if (jSONObjectOptJSONObject != null) {
                    String strOptString = jSONObjectOptJSONObject.optString("filename", "");
                    if (jSONObjectOptJSONObject.optBoolean("primary", false) || strOptString.toLowerCase(Locale.US).endsWith(".mrpack")) {
                        if (jSONObjectOptJSONObject.optBoolean("primary", false)) {
                            jSONObject = jSONObjectOptJSONObject;
                            break;
                        }
                        jSONObject = jSONObjectOptJSONObject;
                    }
                }
                i++;
            }
        }
        if (jSONObject == null) {
            throw new IOException("The selected Modrinth version does not contain an .mrpack file.");
        }
        String strOptString2 = jSONObject.optString("url", "");
        if (!isBlank(strOptString2)) {
            String strSanitizeFileName = sanitizeFileName(jSONObject.optString("filename", safe(str3) + ".mrpack"), "modpack.mrpack");
            File file = new File(context.getCacheDir(), strSanitizeFileName);
            listener.onStatus("Downloading " + strSanitizeFileName + "...");
            downloadFile(strOptString2, file, listener);
            return file;
        }
        throw new IOException("The Modrinth .mrpack download URL is missing.");
    }

    private static JSONObject selectModrinthVersion(JSONArray jSONArray, String str, String str2) {
        String strTrim = safe(str).trim();
        String strNormalizeLoader = normalizeLoader(str2);
        JSONObject jSONObject = null;
        for (int i = 0; i < jSONArray.length(); i++) {
            JSONObject jSONObjectOptJSONObject = jSONArray.optJSONObject(i);
            if (jSONObjectOptJSONObject != null) {
                if (jSONObject == null) {
                    jSONObject = jSONObjectOptJSONObject;
                }
                if ((isBlank(strTrim) || jsonArrayContains(jSONObjectOptJSONObject.optJSONArray("game_versions"), strTrim)) && (isBlank(strNormalizeLoader) || jsonArrayContains(jSONObjectOptJSONObject.optJSONArray("loaders"), strNormalizeLoader))) {
                    return jSONObjectOptJSONObject;
                }
            }
        }
        return jSONObject;
    }

    private static File downloadCurseForgeModpack(Context context, String str, String str2, String str3, String str4, Listener listener) throws Exception {
        JSONObject jSONObjectOptJSONObject;
        int positiveInt = parsePositiveInt(str, "CurseForge project id");
        String strResolve = CurseForgeApiKeyProvider.resolve();
        if (isBlank(strResolve)) {
            throw new IOException("Missing CurseForge API key.");
        }
        StringBuilder sbAppend = new StringBuilder("https://api.curseforge.com/v1/mods/").append(positiveInt).append("/files?pageSize=50&index=0&gameId=432");
        if (!isBlank(str3)) {
            sbAppend.append("&gameVersion=").append(urlEncode(str3.trim()));
        }
        int iCurseForgeLoaderType = curseForgeLoaderType(str4);
        if (iCurseForgeLoaderType > 0) {
            sbAppend.append("&modLoaderType=").append(iCurseForgeLoaderType);
        }
        JSONArray jSONArrayOptJSONArray = new JSONObject(httpGetString(sbAppend.toString(), strResolve)).optJSONArray("data");
        if (jSONArrayOptJSONArray != null) {
            for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
                jSONObjectOptJSONObject = jSONArrayOptJSONArray.optJSONObject(i);
                if (jSONObjectOptJSONObject != null && jSONObjectOptJSONObject.optString("fileName", "").toLowerCase(Locale.US).endsWith(".zip")) {
                    break;
                }
            }
            jSONObjectOptJSONObject = null;
        } else {
            jSONObjectOptJSONObject = null;
        }
        if (jSONObjectOptJSONObject == null) {
            throw new IOException("No compatible CurseForge modpack file was found.");
        }
        int iOptInt = jSONObjectOptJSONObject.optInt("id", 0);
        String strSanitizeFileName = sanitizeFileName(optJsonString(jSONObjectOptJSONObject, "fileName"), sanitizeFileName(safe(str2) + ".zip", "modpack.zip"));
        String strResolveCurseForgeDownloadUrl = resolveCurseForgeDownloadUrl(strResolve, positiveInt, iOptInt, strSanitizeFileName, jSONObjectOptJSONObject);
        if (isBlank(strResolveCurseForgeDownloadUrl)) {
            throw new IOException("CurseForge did not provide a downloadable URL for this modpack file: " + strSanitizeFileName);
        }
        File file = new File(context.getCacheDir(), strSanitizeFileName);
        listener.onStatus("Downloading " + strSanitizeFileName + "...");
        downloadFile(strResolveCurseForgeDownloadUrl, file, listener);
        return file;
    }

    private static void installMrpack(Context context, File file, String str, Listener listener) throws Exception {
        JSONArray jSONArray;
        LoaderSpec loaderSpec;
        String str2;
        LauncherInstance launcherInstance;
        String str3;
        listener.onStatus("Reading Modrinth modpack...");
        ZipFile zipFile = new ZipFile(file);
        try {
            JSONObject jSONObject = new JSONObject(readZipEntryText(zipFile, "modrinth.index.json"));
            zipFile.close();
            JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject("dependencies");
            if (jSONObjectOptJSONObject == null) {
                throw new IOException("modrinth.index.json is missing dependencies.");
            }
            String str4 = "";
            String strTrim = jSONObjectOptJSONObject.optString("minecraft", "").trim();
            if (isBlank(strTrim)) {
                throw new IOException("Modpack is missing the Minecraft dependency.");
            }
            LoaderSpec loaderSpecFromModrinthDependencies = LoaderSpec.fromModrinthDependencies(jSONObjectOptJSONObject);
            String str5 = "name";
            String strUniqueInstanceName = uniqueInstanceName(context, jSONObject.optString("name", "Modrinth Modpack"));
            File fileExtractPackIconToTempFile = extractPackIconToTempFile(context, file, "");
            try {
                LauncherInstance launcherInstanceCreateBaseInstance = createBaseInstance(context, strUniqueInstanceName, strTrim, loaderSpecFromModrinthDependencies, str, fileExtractPackIconToTempFile, listener);
                deleteTempFile(fileExtractPackIconToTempFile);
                File gameDirectory = launcherInstanceCreateBaseInstance.getGameDirectory();
                JSONArray jSONArray2 = new JSONArray();
                listener.onStatus("Installing Modrinth files...");
                JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("files");
                int length = jSONArrayOptJSONArray == null ? 0 : jSONArrayOptJSONArray.length();
                ArrayList<PendingModrinthInstalledFile> arrayList = new ArrayList();
                int i = 0;
                while (jSONArrayOptJSONArray != null && i < jSONArrayOptJSONArray.length()) {
                    JSONObject jSONObjectOptJSONObject2 = jSONArrayOptJSONArray.optJSONObject(i);
                    if (jSONObjectOptJSONObject2 == null) {
                        launcherInstance = launcherInstanceCreateBaseInstance;
                        jSONArray = jSONArrayOptJSONArray;
                        str3 = str4;
                        loaderSpec = loaderSpecFromModrinthDependencies;
                        str2 = strTrim;
                    } else {
                        jSONArray = jSONArrayOptJSONArray;
                        String strOptString = jSONObjectOptJSONObject2.optString("path", str4);
                        if (!isSafeRelativePath(strOptString)) {
                            throw new SecurityException("Blocked unsafe modpack path: " + strOptString);
                        }
                        loaderSpec = loaderSpecFromModrinthDependencies;
                        str2 = strTrim;
                        launcherInstance = launcherInstanceCreateBaseInstance;
                        File file2 = new File(gameDirectory, strOptString.replace('/', File.separatorChar));
                        ensureParent(file2);
                        JSONArray jSONArrayOptJSONArray2 = jSONObjectOptJSONObject2.optJSONArray("downloads");
                        if (jSONArrayOptJSONArray2 == null || jSONArrayOptJSONArray2.length() == 0) {
                            throw new IOException("Missing download URL for " + strOptString);
                        }
                        String strOptString2 = jSONArrayOptJSONArray2.optString(0, str4);
                        str3 = str4;
                        listener.onStatus("Downloading " + strOptString + "...");
                        downloadFile(strOptString2, file2, null);
                        verifyHashesIfPresent(file2, jSONObjectOptJSONObject2.optJSONObject("hashes"));
                        arrayList.add(new PendingModrinthInstalledFile(file2, strOptString, jSONObjectOptJSONObject2, strOptString2));
                        listener.onProgress(i + 1, Math.max(1, length));
                    }
                    i++;
                    jSONArrayOptJSONArray = jSONArray;
                    loaderSpecFromModrinthDependencies = loaderSpec;
                    strTrim = str2;
                    str4 = str3;
                    launcherInstanceCreateBaseInstance = launcherInstance;
                }
                LauncherInstance launcherInstance2 = launcherInstanceCreateBaseInstance;
                LoaderSpec loaderSpec2 = loaderSpecFromModrinthDependencies;
                String str6 = strTrim;
                listener.onStatus("Resolving Modrinth update metadata...");
                Map<String, JSONObject> mapResolveModrinthVersionMetadataBySha1 = resolveModrinthVersionMetadataBySha1(arrayList);
                for (PendingModrinthInstalledFile pendingModrinthInstalledFile : arrayList) {
                    String strResolveModrinthSha1 = resolveModrinthSha1(pendingModrinthInstalledFile.fileMetadata, pendingModrinthInstalledFile.target);
                    File file3 = pendingModrinthInstalledFile.target;
                    String str7 = pendingModrinthInstalledFile.relativePath;
                    JSONObject jSONObject2 = pendingModrinthInstalledFile.fileMetadata;
                    String str8 = pendingModrinthInstalledFile.downloadUrl;
                    String strOptString3 = jSONObject.optString(str5, strUniqueInstanceName);
                    JSONObject jSONObject3 = mapResolveModrinthVersionMetadataBySha1.get(strResolveModrinthSha1);
                    String str9 = strUniqueInstanceName;
                    Map<String, JSONObject> map = mapResolveModrinthVersionMetadataBySha1;
                    String str10 = str5;
                    LoaderSpec loaderSpec3 = loaderSpec2;
                    String str11 = str6;
                    JSONObject jSONObjectBuildModrinthInstalledContentEntry = buildModrinthInstalledContentEntry(gameDirectory, file3, str7, jSONObject2, str8, strOptString3, str6, loaderSpec3, strResolveModrinthSha1, jSONObject3);
                    if (jSONObjectBuildModrinthInstalledContentEntry != null) {
                        jSONArray2.put(jSONObjectBuildModrinthInstalledContentEntry);
                    }
                    str6 = str11;
                    str5 = str10;
                    loaderSpec2 = loaderSpec3;
                    strUniqueInstanceName = str9;
                    mapResolveModrinthVersionMetadataBySha1 = map;
                }
                String str12 = strUniqueInstanceName;
                String str13 = str5;
                LoaderSpec loaderSpec4 = loaderSpec2;
                String str14 = str6;
                listener.onStatus("Copying Modrinth overrides...");
                zipFile = new ZipFile(file);
                try {
                    copyZipPrefixToDirectory(zipFile, "overrides/", gameDirectory);
                    zipFile.close();
                    listener.onStatus("Resolving override metadata...");
                    appendMissingModrinthMetadataForLocalContent(gameDirectory, jSONArray2, jSONObject.optString(str13, str12), str14, loaderSpec4);
                    writeInstalledContentMetadata(gameDirectory, "modrinth", jSONObject.optString(str13, str12), str14, loaderSpec4, jSONArray2);
                    registerInstalledContentWithModManagerManifest(gameDirectory, jSONArray2);
                    writeInstalledPackManifest(gameDirectory, "modrinth", jSONObject.optString(str13, str12), str14, loaderSpec4, null, null);
                    listener.onComplete("Installed modpack: " + str12, launcherInstance2);
                } finally {
                }
            } catch (Throwable th) {
                deleteTempFile(fileExtractPackIconToTempFile);
                throw th;
            }
        } finally {
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:61:0x01e6  */
    /* JADX WARN: Removed duplicated region for block: B:62:0x01ef  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private static void installCurseForgePack(android.content.Context r29, java.io.File r30, java.lang.String r31, ca.dnamobile.javalauncher.modmanager.ModpackInstallManager.Listener r32) throws java.lang.Exception {
        /*
            Method dump skipped, instruction units count: 887
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: ca.dnamobile.javalauncher.modmanager.ModpackInstallManager.installCurseForgePack(android.content.Context, java.io.File, java.lang.String, ca.dnamobile.javalauncher.modmanager.ModpackInstallManager$Listener):void");
    }

    private static void installMultiMcPack(Context context, File file, String str, Listener listener) throws Exception {
        String toString;
        listener.onStatus("Reading MultiMC/Prism modpack...");
        ZipFile zipFile = new ZipFile(file);
        try {
            JSONObject jSONObject = new JSONObject(readZipEntryText(zipFile, str + "mmc-pack.json"));
            ZipEntry entry = zipFile.getEntry(str + "instance.cfg");
            if (entry != null && !entry.isDirectory()) {
                InputStream inputStream = zipFile.getInputStream(entry);
                try {
                    toString = readToString(inputStream);
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } finally {
                }
            } else {
                toString = "";
            }
            zipFile.close();
            String strFindMultiMcMinecraftVersion = findMultiMcMinecraftVersion(jSONObject);
            if (isBlank(strFindMultiMcMinecraftVersion)) {
                throw new IOException("mmc-pack.json is missing the Minecraft component.");
            }
            LoaderSpec loaderSpecFromMultiMcPackJson = LoaderSpec.fromMultiMcPackJson(jSONObject);
            String multiMcCfgValue = readMultiMcCfgValue(toString, "name");
            if (isBlank(multiMcCfgValue)) {
                multiMcCfgValue = "MultiMC Modpack";
            }
            String strUniqueInstanceName = uniqueInstanceName(context, multiMcCfgValue);
            File fileExtractPackIconToTempFile = extractPackIconToTempFile(context, file, str);
            try {
                LauncherInstance launcherInstanceCreateBaseInstance = createBaseInstance(context, strUniqueInstanceName, strFindMultiMcMinecraftVersion, loaderSpecFromMultiMcPackJson, null, fileExtractPackIconToTempFile, listener);
                deleteTempFile(fileExtractPackIconToTempFile);
                File gameDirectory = launcherInstanceCreateBaseInstance.getGameDirectory();
                listener.onStatus("Copying MultiMC/Prism .minecraft files...");
                zipFile = new ZipFile(file);
                try {
                    String strFindFirstExistingPrefix = findFirstExistingPrefix(zipFile, str + ".minecraft/", str + "minecraft/");
                    if (strFindFirstExistingPrefix == null) {
                        throw new IOException("MultiMC/Prism pack is missing the .minecraft folder.");
                    }
                    copyZipPrefixToDirectory(zipFile, strFindFirstExistingPrefix, gameDirectory);
                    zipFile.close();
                    JSONObject jSONObject2 = new JSONObject();
                    jSONObject2.put("rootPrefix", str);
                    jSONObject2.put("mmcPack", jSONObject);
                    writeInstalledPackManifest(gameDirectory, "multimc", strUniqueInstanceName, strFindMultiMcMinecraftVersion, loaderSpecFromMultiMcPackJson, jSONObject, jSONObject2);
                    listener.onComplete("Installed modpack: " + strUniqueInstanceName, launcherInstanceCreateBaseInstance);
                } finally {
                }
            } catch (Throwable th) {
                deleteTempFile(fileExtractPackIconToTempFile);
                throw th;
            }
        } finally {
        }
    }

    private static String findMultiMcRootPrefix(ZipFile zipFile) {
        if (zipFile.getEntry("mmc-pack.json") != null) {
            return "";
        }
        Enumeration<? extends ZipEntry> enumerationEntries = zipFile.entries();
        while (enumerationEntries.hasMoreElements()) {
            ZipEntry zipEntryNextElement = enumerationEntries.nextElement();
            if (!zipEntryNextElement.isDirectory()) {
                String strNormalizeZipPath = normalizeZipPath(zipEntryNextElement.getName());
                if (strNormalizeZipPath.endsWith("/mmc-pack.json")) {
                    return strNormalizeZipPath.substring(0, strNormalizeZipPath.length() - "mmc-pack.json".length());
                }
            }
        }
        return null;
    }

    private static String findFirstExistingPrefix(ZipFile zipFile, String... strArr) {
        for (String str : strArr) {
            Enumeration<? extends ZipEntry> enumerationEntries = zipFile.entries();
            while (enumerationEntries.hasMoreElements()) {
                if (normalizeZipPath(enumerationEntries.nextElement().getName()).startsWith(str)) {
                    return str;
                }
            }
        }
        return null;
    }

    private static String findMultiMcMinecraftVersion(JSONObject jSONObject) {
        JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("components");
        if (jSONArrayOptJSONArray == null) {
            return "";
        }
        for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
            JSONObject jSONObjectOptJSONObject = jSONArrayOptJSONArray.optJSONObject(i);
            if (jSONObjectOptJSONObject != null && "net.minecraft".equalsIgnoreCase(jSONObjectOptJSONObject.optString("uid", ""))) {
                return jSONObjectOptJSONObject.optString("version", "").trim();
            }
        }
        return "";
    }

    private static String readMultiMcCfgValue(String str, String str2) {
        if (str == null) {
            return "";
        }
        String str3 = str2 + "=";
        for (String str4 : str.replace("\r", "").split("\n")) {
            if (str4.startsWith(str3)) {
                return str4.substring(str3.length()).trim();
            }
        }
        return "";
    }

    /* JADX WARN: Removed duplicated region for block: B:25:0x00e9  */
    /* JADX WARN: Removed duplicated region for block: B:30:0x00fc  */
    /* JADX WARN: Removed duplicated region for block: B:32:0x00ff  */
    /* JADX WARN: Removed duplicated region for block: B:36:0x0111 A[DONT_GENERATE] */
    /* JADX WARN: Removed duplicated region for block: B:41:0x012b  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private static ca.dnamobile.javalauncher.instance.LauncherInstance createBaseInstance(android.content.Context r10, java.lang.String r11, java.lang.String r12, ca.dnamobile.javalauncher.modmanager.ModpackInstallManager.LoaderSpec r13, java.lang.String r14, java.io.File r15, final ca.dnamobile.javalauncher.modmanager.ModpackInstallManager.Listener r16) throws java.lang.Exception {
        /*
            Method dump skipped, instruction units count: 351
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: ca.dnamobile.javalauncher.modmanager.ModpackInstallManager.createBaseInstance(android.content.Context, java.lang.String, java.lang.String, ca.dnamobile.javalauncher.modmanager.ModpackInstallManager$LoaderSpec, java.lang.String, java.io.File, ca.dnamobile.javalauncher.modmanager.ModpackInstallManager$Listener):ca.dnamobile.javalauncher.instance.LauncherInstance");
    }

    static /* synthetic */ void lambda$createBaseInstance$0(Listener listener, int i, String str) {
        listener.onStatus(str);
        listener.onProgress(i, 100);
    }

    private static File extractPackIconToTempFile(Context context, File file, String str) {
        ZipFile zipFile;
        if (!file.isFile()) {
            return null;
        }
        String strNormalizeZipPath = str == null ? "" : normalizeZipPath(str);
        try {
            zipFile = new ZipFile(file);
        } catch (Throwable th) {
            Logging.i(TAG, "Unable to extract modpack icon: " + th.getMessage());
            return null;
        }
        try {
            ZipEntry zipEntryFindPackIconEntry = findPackIconEntry(zipFile, strNormalizeZipPath);
            if (zipEntryFindPackIconEntry != null && !zipEntryFindPackIconEntry.isDirectory()) {
                if (zipEntryFindPackIconEntry.getSize() > 5242880) {
                    Logging.i(TAG, "Skipping oversized modpack icon: " + zipEntryFindPackIconEntry.getName());
                    zipFile.close();
                    return null;
                }
                File fileCreateTempFile = File.createTempFile("javalauncher-imported-modpack-icon-", resolveIconExtension(zipEntryFindPackIconEntry.getName()), context.getCacheDir());
                InputStream inputStream = zipFile.getInputStream(zipEntryFindPackIconEntry);
                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(fileCreateTempFile);
                    try {
                        copyStream(inputStream, fileOutputStream);
                        fileOutputStream.close();
                        if (inputStream != null) {
                            inputStream.close();
                        }
                        zipFile.close();
                        return fileCreateTempFile;
                    } finally {
                    }
                } finally {
                }
                Logging.i(TAG, "Unable to extract modpack icon: " + th.getMessage());
                return null;
            }
            zipFile.close();
            return null;
        } finally {
        }
    }

    private static ZipEntry findPackIconEntry(ZipFile zipFile, String str) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(JAVALAUNCHER_PACK_ICON_ENTRY);
        arrayList.add("icon.png");
        arrayList.add("pack.png");
        arrayList.add("modpack-icon.png");
        arrayList.add("logo.png");
        arrayList.add("overrides/pack.png");
        arrayList.add("overrides/icon.png");
        arrayList.add("overrides/modpack-icon.png");
        arrayList.add("overrides/logo.png");
        if (!isBlank(str)) {
            arrayList.add(str + JAVALAUNCHER_PACK_ICON_ENTRY);
            arrayList.add(str + "icon.png");
            arrayList.add(str + "pack.png");
            arrayList.add(str + "modpack-icon.png");
            arrayList.add(str + "logo.png");
            arrayList.add(str + ".minecraft/pack.png");
            arrayList.add(str + ".minecraft/icon.png");
        }
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            ZipEntry entry = zipFile.getEntry((String) it.next());
            if (entry != null && !entry.isDirectory()) {
                return entry;
            }
        }
        Enumeration<? extends ZipEntry> enumerationEntries = zipFile.entries();
        while (enumerationEntries.hasMoreElements()) {
            ZipEntry zipEntryNextElement = enumerationEntries.nextElement();
            if (!zipEntryNextElement.isDirectory()) {
                String lowerCase = normalizeZipPath(zipEntryNextElement.getName()).toLowerCase(Locale.US);
                if (lowerCase.equals(JAVALAUNCHER_PACK_ICON_ENTRY) || lowerCase.endsWith("/javalauncher-pack-icon.png") || lowerCase.endsWith("/instance-icon.png")) {
                    return zipEntryNextElement;
                }
            }
        }
        return null;
    }

    private static String resolveIconExtension(String str) {
        String lowerCase = str.toLowerCase(Locale.US);
        if (lowerCase.endsWith(".jpg") || lowerCase.endsWith(".jpeg")) {
            return ".jpg";
        }
        if (lowerCase.endsWith(".webp")) {
            return ".webp";
        }
        return ".png";
    }

    private static void deleteTempFile(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        file.delete();
    }

    private static File downloadInstanceIcon(Context context, String str) {
        File fileCreateTempFile;
        String strCleanDownloadUrl = cleanDownloadUrl(str);
        if (!isHttpUrl(strCleanDownloadUrl)) {
            return null;
        }
        try {
            fileCreateTempFile = File.createTempFile("javalauncher-modpack-icon-", ".png", context.getCacheDir());
            try {
                downloadFile(strCleanDownloadUrl, fileCreateTempFile, null);
                return fileCreateTempFile;
            } catch (Throwable th) {
                th = th;
                if (fileCreateTempFile != null && fileCreateTempFile.exists()) {
                    fileCreateTempFile.delete();
                }
                Logging.i(TAG, "Unable to download modpack icon: " + th.getMessage());
                return null;
            }
        } catch (Throwable th2) {
            th = th2;
            fileCreateTempFile = null;
        }
    }

    private static void writeInstallWarnings(File file, ArrayList<String> arrayList) {
        if (arrayList.isEmpty()) {
            return;
        }
        try {
            File file2 = new File(getJavaLauncherMetadataDirectory(file), MODPACK_INSTALL_WARNINGS_FILE);
            StringBuilder sb = new StringBuilder();
            sb.append("Some CurseForge files could not be downloaded.\n");
            sb.append("The pack may still launch if those files were optional or unavailable on CurseForge.\n\n");
            Iterator<String> it = arrayList.iterator();
            while (it.hasNext()) {
                sb.append("- ").append(it.next()).append('\n');
            }
            FileOutputStream fileOutputStream = new FileOutputStream(file2);
            try {
                fileOutputStream.write(sb.toString().getBytes("UTF-8"));
                fileOutputStream.close();
            } finally {
            }
        } catch (Throwable th) {
            Logging.e(TAG, "Unable to write modpack install warnings", th);
        }
    }

    private static File getJavaLauncherMetadataDirectory(File file) {
        File file2 = new File(file, JAVALAUNCHER_METADATA_DIRECTORY);
        if (!file2.exists()) {
            file2.mkdirs();
        }
        return file2;
    }

    private static MinecraftVersion findMinecraftVersion(Context context, String str) throws Exception {
        for (MinecraftVersion minecraftVersion : MinecraftVersionManifestClient.loadVersions(context)) {
            if (str.equals(minecraftVersion.getId())) {
                return minecraftVersion;
            }
        }
        return null;
    }

    private static boolean isVanillaInstalled(String str) {
        Iterator<MinecraftVersion> it = MinecraftVersionInstaller.findInstalledVersions().iterator();
        while (it.hasNext()) {
            if (str.equals(it.next().getId())) {
                return true;
            }
        }
        return false;
    }

    private static String uniqueInstanceName(Context context, String str) {
        String strSanitizeInstanceName = sanitizeInstanceName(isBlank(str) ? "Imported Modpack" : str.trim());
        String str2 = strSanitizeInstanceName.isEmpty() ? "Imported Modpack" : strSanitizeInstanceName;
        HashSet hashSet = new HashSet();
        Iterator<LauncherInstance> it = LauncherInstanceManager.findInstances(context).iterator();
        while (it.hasNext()) {
            hashSet.add(it.next().getName().trim().toLowerCase(Locale.US));
        }
        if (!hashSet.contains(str2.toLowerCase(Locale.US))) {
            return str2;
        }
        for (int i = 2; i < 1000; i++) {
            String str3 = str2 + " " + i;
            if (!hashSet.contains(str3.toLowerCase(Locale.US))) {
                return str3;
            }
        }
        return str2 + " " + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String sanitizeInstanceName(String str) {
        String strReplaceAll = str.trim().replace('\n', ' ').replace('\r', ' ').replaceAll("[\\\\/:*?\"<>|]", "_");
        while (strReplaceAll.contains("  ")) {
            strReplaceAll = strReplaceAll.replace("  ", " ");
        }
        return (".".equals(strReplaceAll) || "..".equals(strReplaceAll)) ? "" : strReplaceAll;
    }

    private static void copyZipPrefixToDirectory(ZipFile zipFile, String str, File file) throws Exception {
        String canonicalPath = file.getCanonicalPath();
        Enumeration<? extends ZipEntry> enumerationEntries = zipFile.entries();
        while (enumerationEntries.hasMoreElements()) {
            ZipEntry zipEntryNextElement = enumerationEntries.nextElement();
            if (!zipEntryNextElement.isDirectory()) {
                String strNormalizeZipPath = normalizeZipPath(zipEntryNextElement.getName());
                if (strNormalizeZipPath.startsWith(str)) {
                    String strSubstring = strNormalizeZipPath.substring(str.length());
                    if (!isBlank(strSubstring) && isSafeRelativePath(strSubstring)) {
                        File file2 = new File(file, strSubstring.replace('/', File.separatorChar));
                        String canonicalPath2 = file2.getCanonicalPath();
                        if (!canonicalPath2.equals(canonicalPath) && !canonicalPath2.startsWith(canonicalPath + File.separator)) {
                            throw new SecurityException("Blocked unsafe override path: " + zipEntryNextElement.getName());
                        }
                        ensureParent(file2);
                        InputStream inputStream = zipFile.getInputStream(zipEntryNextElement);
                        try {
                            FileOutputStream fileOutputStream = new FileOutputStream(file2);
                            try {
                                copyStream(inputStream, fileOutputStream);
                                fileOutputStream.close();
                                if (inputStream != null) {
                                    inputStream.close();
                                }
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
                } else {
                    continue;
                }
            }
        }
    }

    private static void appendMissingModrinthMetadataForLocalContent(File file, JSONArray jSONArray, String str, String str2, LoaderSpec loaderSpec) {
        try {
            HashSet hashSet = new HashSet();
            for (int i = 0; i < jSONArray.length(); i++) {
                JSONObject jSONObjectOptJSONObject = jSONArray.optJSONObject(i);
                if (jSONObjectOptJSONObject != null) {
                    String lowerCase = normalizeZipPath(firstNonBlank(jSONObjectOptJSONObject.optString("relativePath", ""), firstNonBlank(jSONObjectOptJSONObject.optString("filePath", ""), jSONObjectOptJSONObject.optString("path", "")))).toLowerCase(Locale.US);
                    if (!isBlank(lowerCase)) {
                        hashSet.add(lowerCase);
                    }
                }
            }
            ArrayList<PendingModrinthInstalledFile> arrayList = new ArrayList();
            collectMissingLocalModrinthCandidates(file, "mods", hashSet, arrayList);
            collectMissingLocalModrinthCandidates(file, "resourcepacks", hashSet, arrayList);
            collectMissingLocalModrinthCandidates(file, "shaderpacks", hashSet, arrayList);
            if (arrayList.isEmpty()) {
                return;
            }
            Map<String, JSONObject> mapResolveModrinthVersionMetadataBySha1 = resolveModrinthVersionMetadataBySha1(arrayList);
            for (PendingModrinthInstalledFile pendingModrinthInstalledFile : arrayList) {
                String strResolveModrinthSha1 = resolveModrinthSha1(pendingModrinthInstalledFile.fileMetadata, pendingModrinthInstalledFile.target);
                JSONObject jSONObject = mapResolveModrinthVersionMetadataBySha1.get(strResolveModrinthSha1);
                if (jSONObject != null) {
                    String strSelectModrinthDownloadUrl = selectModrinthDownloadUrl(jSONObject, strResolveModrinthSha1);
                    if (!isBlank(strSelectModrinthDownloadUrl)) {
                        JSONArray jSONArray2 = new JSONArray();
                        jSONArray2.put(strSelectModrinthDownloadUrl);
                        pendingModrinthInstalledFile.fileMetadata.put("downloads", jSONArray2);
                    }
                    JSONObject jSONObjectBuildModrinthInstalledContentEntry = buildModrinthInstalledContentEntry(file, pendingModrinthInstalledFile.target, pendingModrinthInstalledFile.relativePath, pendingModrinthInstalledFile.fileMetadata, strSelectModrinthDownloadUrl, str, str2, loaderSpec, strResolveModrinthSha1, jSONObject);
                    if (jSONObjectBuildModrinthInstalledContentEntry != null) {
                        jSONArray.put(jSONObjectBuildModrinthInstalledContentEntry);
                    }
                }
            }
        } catch (Throwable th) {
            Logging.i(TAG, "Unable to resolve Modrinth metadata for override files: " + th.getMessage());
        }
    }

    private static void collectMissingLocalModrinthCandidates(File file, String str, HashSet<String> hashSet, ArrayList<PendingModrinthInstalledFile> arrayList) {
        File[] fileArrListFiles = new File(file, str).listFiles();
        if (fileArrListFiles == null) {
            return;
        }
        for (File file2 : fileArrListFiles) {
            if (!file2.isHidden() && file2.isFile()) {
                String lowerCase = file2.getName().toLowerCase(Locale.US);
                if (lowerCase.endsWith(".jar") || lowerCase.endsWith(".zip")) {
                    String str2 = str + "/" + file2.getName();
                    if (!hashSet.contains(normalizeZipPath(str2).toLowerCase(Locale.US))) {
                        try {
                            arrayList.add(new PendingModrinthInstalledFile(file2, str2, buildLocalModrinthFileMetadata(file2, str2), ""));
                        } catch (Throwable th) {
                            Logging.i(TAG, "Unable to prepare local Modrinth metadata candidate for " + file2.getName() + ": " + th.getMessage());
                        }
                    }
                }
            }
        }
    }

    private static JSONObject buildLocalModrinthFileMetadata(File file, String str) throws Exception {
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("path", str);
        jSONObject.put("fileSize", file.length());
        JSONObject jSONObject2 = new JSONObject();
        jSONObject2.put("sha1", hashFile(file, "SHA-1"));
        jSONObject2.put("sha512", hashFile(file, "SHA-512"));
        jSONObject.put("hashes", jSONObject2);
        jSONObject.put("downloads", new JSONArray());
        JSONObject jSONObject3 = new JSONObject();
        jSONObject3.put("client", "required");
        jSONObject3.put("server", "optional");
        jSONObject.put("env", jSONObject3);
        return jSONObject;
    }

    private static String selectModrinthDownloadUrl(JSONObject jSONObject, String str) {
        JSONArray jSONArrayOptJSONArray;
        if (jSONObject != null && (jSONArrayOptJSONArray = jSONObject.optJSONArray("files")) != null && jSONArrayOptJSONArray.length() != 0) {
            String str2 = "";
            String str3 = str2;
            for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
                JSONObject jSONObjectOptJSONObject = jSONArrayOptJSONArray.optJSONObject(i);
                if (jSONObjectOptJSONObject != null) {
                    String strTrim = jSONObjectOptJSONObject.optString("url", "").trim();
                    if (isBlank(strTrim)) {
                        continue;
                    } else {
                        if (isBlank(str3)) {
                            str3 = strTrim;
                        }
                        if (jSONObjectOptJSONObject.optBoolean("primary", false)) {
                            str2 = strTrim;
                        }
                        JSONObject jSONObjectOptJSONObject2 = jSONObjectOptJSONObject.optJSONObject("hashes");
                        String strTrim2 = jSONObjectOptJSONObject2 == null ? "" : jSONObjectOptJSONObject2.optString("sha1", "").trim();
                        if (!isBlank(str) && str.equalsIgnoreCase(strTrim2)) {
                            return strTrim;
                        }
                    }
                }
            }
            return firstNonBlank(str2, str3);
        }
        return "";
    }

    private static Map<String, JSONObject> resolveModrinthVersionMetadataBySha1(ArrayList<PendingModrinthInstalledFile> arrayList) {
        HashMap map = new HashMap();
        LinkedHashSet<String> linkedHashSet = new LinkedHashSet();
        for (PendingModrinthInstalledFile pendingModrinthInstalledFile : arrayList) {
            String strResolveModrinthSha1 = resolveModrinthSha1(pendingModrinthInstalledFile.fileMetadata, pendingModrinthInstalledFile.target);
            if (!isBlank(strResolveModrinthSha1)) {
                linkedHashSet.add(strResolveModrinthSha1);
            }
        }
        if (linkedHashSet.isEmpty()) {
            return map;
        }
        try {
            JSONObject jSONObject = new JSONObject();
            JSONArray jSONArray = new JSONArray();
            Iterator it = linkedHashSet.iterator();
            while (it.hasNext()) {
                jSONArray.put((String) it.next());
            }
            jSONObject.put("hashes", jSONArray);
            jSONObject.put("algorithm", "sha1");
            JSONObject jSONObject2 = new JSONObject(httpPostString("https://api.modrinth.com/v2/version_files", jSONObject, null));
            for (String str : linkedHashSet) {
                JSONObject jSONObjectOptJSONObject = jSONObject2.optJSONObject(str);
                if (jSONObjectOptJSONObject != null) {
                    map.put(str, jSONObjectOptJSONObject);
                }
            }
        } catch (Throwable th) {
            Logging.i(TAG, "Unable to resolve Modrinth update metadata from hashes: " + th.getMessage());
        }
        return map;
    }

    private static String resolveModrinthSha1(JSONObject jSONObject, File file) {
        JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject("hashes");
        String strTrim = jSONObjectOptJSONObject == null ? "" : jSONObjectOptJSONObject.optString("sha1", "").trim();
        if (!isBlank(strTrim)) {
            return strTrim;
        }
        try {
            return hashFile(file, "SHA-1");
        } catch (Throwable th) {
            Logging.i(TAG, "Unable to hash Modrinth file " + file.getName() + ": " + th.getMessage());
            return "";
        }
    }

    private static JSONObject buildModrinthInstalledContentEntry(File file, File file2, String str, JSONObject jSONObject, String str2, String str3, String str4, LoaderSpec loaderSpec, String str5, JSONObject jSONObject2) throws Exception {
        String strOptString;
        String str6;
        ModManagerContentType modManagerContentTypeResolveContentTypeForRelativePath = resolveContentTypeForRelativePath(str);
        if (modManagerContentTypeResolveContentTypeForRelativePath == null) {
            return null;
        }
        ParsedModrinthDownloadIds modrinthDownloadIds = parseModrinthDownloadIds(str2);
        String strFirstNonBlank = firstNonBlank(jSONObject2 == null ? "" : jSONObject2.optString("project_id", ""), modrinthDownloadIds.projectId);
        String strFirstNonBlank2 = firstNonBlank(jSONObject2 == null ? "" : jSONObject2.optString("id", ""), modrinthDownloadIds.versionId);
        String strFirstNonBlank3 = firstNonBlank(jSONObject2 == null ? "" : jSONObject2.optString("version_number", ""), strFirstNonBlank2);
        JSONObject jSONObjectBuildInstalledContentBaseEntry = buildInstalledContentBaseEntry(file, file2, str, modManagerContentTypeResolveContentTypeForRelativePath, ModManagerSource.MODRINTH, str3, str4, loaderSpec);
        JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject("hashes");
        jSONObjectBuildInstalledContentBaseEntry.put("platform", "modrinth");
        jSONObjectBuildInstalledContentBaseEntry.put("modpackPlatform", "modrinth");
        jSONObjectBuildInstalledContentBaseEntry.put("installedFromPlatformModpack", true);
        jSONObjectBuildInstalledContentBaseEntry.put("updateReady", !isBlank(strFirstNonBlank));
        jSONObjectBuildInstalledContentBaseEntry.put("platformProjectId", strFirstNonBlank);
        jSONObjectBuildInstalledContentBaseEntry.put("projectId", strFirstNonBlank);
        jSONObjectBuildInstalledContentBaseEntry.put("modrinthProjectId", strFirstNonBlank);
        jSONObjectBuildInstalledContentBaseEntry.put("platformVersionId", strFirstNonBlank2);
        jSONObjectBuildInstalledContentBaseEntry.put("versionId", strFirstNonBlank2);
        jSONObjectBuildInstalledContentBaseEntry.put("modrinthVersionId", strFirstNonBlank2);
        jSONObjectBuildInstalledContentBaseEntry.put("versionNumber", strFirstNonBlank3);
        jSONObjectBuildInstalledContentBaseEntry.put("downloadUrl", str2);
        jSONObjectBuildInstalledContentBaseEntry.put("downloads", jSONObject.optJSONArray("downloads") == null ? new JSONArray().put(str2) : jSONObject.optJSONArray("downloads"));
        jSONObjectBuildInstalledContentBaseEntry.put("fileSize", file2.length());
        jSONObjectBuildInstalledContentBaseEntry.put("declaredFileSize", jSONObject.optLong("fileSize", file2.length()));
        jSONObjectBuildInstalledContentBaseEntry.put("hashes", jSONObjectOptJSONObject == null ? new JSONObject() : jSONObjectOptJSONObject);
        if (jSONObjectOptJSONObject == null) {
            str6 = str5;
            strOptString = "";
        } else {
            strOptString = jSONObjectOptJSONObject.optString("sha1", "");
            str6 = str5;
        }
        jSONObjectBuildInstalledContentBaseEntry.put("sha1", firstNonBlank(str6, strOptString));
        jSONObjectBuildInstalledContentBaseEntry.put("sha512", jSONObjectOptJSONObject != null ? jSONObjectOptJSONObject.optString("sha512", "") : "");
        jSONObjectBuildInstalledContentBaseEntry.put("modrinthPackFile", jSONObject);
        if (jSONObject2 != null) {
            jSONObjectBuildInstalledContentBaseEntry.put("modrinthVersion", jSONObject2);
            jSONObjectBuildInstalledContentBaseEntry.put("gameVersions", jSONObject2.optJSONArray("game_versions") == null ? new JSONArray() : jSONObject2.optJSONArray("game_versions"));
            jSONObjectBuildInstalledContentBaseEntry.put("loaders", jSONObject2.optJSONArray("loaders") == null ? new JSONArray() : jSONObject2.optJSONArray("loaders"));
        }
        return jSONObjectBuildInstalledContentBaseEntry;
    }

    private static JSONObject buildCurseForgeInstalledContentEntry(File file, File file2, JSONObject jSONObject, int i, int i2, String str, String str2, String str3, LoaderSpec loaderSpec) throws Exception {
        JSONObject jSONObjectBuildInstalledContentBaseEntry = buildInstalledContentBaseEntry(file, file2, "mods/" + file2.getName(), ModManagerContentType.MODS, ModManagerSource.CURSEFORGE, str2, str3, loaderSpec);
        jSONObjectBuildInstalledContentBaseEntry.put("platform", "curseforge");
        jSONObjectBuildInstalledContentBaseEntry.put("modpackPlatform", "curseforge");
        jSONObjectBuildInstalledContentBaseEntry.put("installedFromPlatformModpack", true);
        jSONObjectBuildInstalledContentBaseEntry.put("updateReady", true);
        jSONObjectBuildInstalledContentBaseEntry.put("platformProjectId", String.valueOf(i));
        jSONObjectBuildInstalledContentBaseEntry.put("projectId", String.valueOf(i));
        jSONObjectBuildInstalledContentBaseEntry.put("curseForgeProjectId", i);
        jSONObjectBuildInstalledContentBaseEntry.put("platformFileId", String.valueOf(i2));
        jSONObjectBuildInstalledContentBaseEntry.put("fileId", String.valueOf(i2));
        jSONObjectBuildInstalledContentBaseEntry.put("curseForgeFileId", i2);
        jSONObjectBuildInstalledContentBaseEntry.put("versionNumber", optJsonString(jSONObject, "displayName"));
        jSONObjectBuildInstalledContentBaseEntry.put("downloadUrl", str);
        jSONObjectBuildInstalledContentBaseEntry.put("fileDate", optJsonString(jSONObject, "fileDate"));
        jSONObjectBuildInstalledContentBaseEntry.put("releaseType", jSONObject.optInt("releaseType", 0));
        jSONObjectBuildInstalledContentBaseEntry.put("gameVersions", jSONObject.optJSONArray("gameVersions") == null ? new JSONArray() : jSONObject.optJSONArray("gameVersions"));
        jSONObjectBuildInstalledContentBaseEntry.put("hashes", jSONObject.optJSONArray("hashes") == null ? new JSONArray() : jSONObject.optJSONArray("hashes"));
        jSONObjectBuildInstalledContentBaseEntry.put("sha1", resolveCurseForgeSha1(jSONObject.optJSONArray("hashes")));
        jSONObjectBuildInstalledContentBaseEntry.put("fileSize", file2.length());
        jSONObjectBuildInstalledContentBaseEntry.put("declaredFileSize", jSONObject.optLong("fileLength", file2.length()));
        jSONObjectBuildInstalledContentBaseEntry.put("curseForgeFile", jSONObject);
        return jSONObjectBuildInstalledContentBaseEntry;
    }

    private static JSONObject buildInstalledContentBaseEntry(File file, File file2, String str, ModManagerContentType modManagerContentType, ModManagerSource modManagerSource, String str2, String str3, LoaderSpec loaderSpec) throws Exception {
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("source", modManagerSource.getId());
        jSONObject.put("platform", modManagerSource.getId());
        jSONObject.put("contentType", modManagerContentType.getIntentValue());
        jSONObject.put("type", modManagerContentType.getIntentValue());
        jSONObject.put("fileName", file2.getName());
        jSONObject.put("name", stripExtension(file2.getName()));
        jSONObject.put("displayName", stripExtension(file2.getName()));
        jSONObject.put("filePath", str);
        jSONObject.put("relativePath", str);
        jSONObject.put("path", str);
        jSONObject.put("absolutePath", file2.getAbsolutePath());
        jSONObject.put("canonicalPath", safeCanonicalPath(file2));
        jSONObject.put("enabled", true);
        jSONObject.put("installedAt", System.currentTimeMillis());
        jSONObject.put("installedBy", "modpack");
        jSONObject.put("modpackName", str2);
        jSONObject.put("minecraftVersion", str3);
        jSONObject.put("loader", loaderSpec.loaderName);
        jSONObject.put("loaderVersion", safe(loaderSpec.loaderVersion));
        jSONObject.put("gameDirectory", file.getAbsolutePath());
        return jSONObject;
    }

    private static int countUpdateReadyFiles(JSONArray jSONArray) {
        int i = 0;
        for (int i2 = 0; i2 < jSONArray.length(); i2++) {
            JSONObject jSONObjectOptJSONObject = jSONArray.optJSONObject(i2);
            if (jSONObjectOptJSONObject != null && jSONObjectOptJSONObject.optBoolean("updateReady", false)) {
                i++;
            }
        }
        return i;
    }

    private static void writeInstalledContentMetadata(File file, String str, String str2, String str3, LoaderSpec loaderSpec, JSONArray jSONArray) {
        try {
            File javaLauncherMetadataDirectory = getJavaLauncherMetadataDirectory(file);
            JSONObject jSONObject = new JSONObject();
            jSONObject.put("schemaVersion", 1);
            jSONObject.put("type", "modpack-installed-content");
            jSONObject.put("platform", str);
            jSONObject.put("packName", str2);
            jSONObject.put("minecraftVersion", str3);
            jSONObject.put("loader", loaderSpec.loaderName);
            jSONObject.put("loaderVersion", safe(loaderSpec.loaderVersion));
            jSONObject.put("installedAt", System.currentTimeMillis());
            jSONObject.put("fileCount", jSONArray.length());
            jSONObject.put("updateReadyFileCount", countUpdateReadyFiles(jSONArray));
            jSONObject.put("files", jSONArray);
            FileOutputStream fileOutputStream = new FileOutputStream(new File(javaLauncherMetadataDirectory, MODPACK_FILES_MANIFEST_FILE));
            try {
                fileOutputStream.write(jSONObject.toString(2).getBytes("UTF-8"));
                fileOutputStream.close();
            } finally {
            }
        } catch (Throwable th) {
            Logging.e(TAG, "Unable to write modpack installed-content metadata", th);
        }
    }

    private static void registerInstalledContentWithModManagerManifest(File file, JSONArray jSONArray) {
        ModManagerContentType modManagerContentTypeContentTypeFromIntentValue;
        for (int i = 0; i < jSONArray.length(); i++) {
            JSONObject jSONObjectOptJSONObject = jSONArray.optJSONObject(i);
            if (jSONObjectOptJSONObject != null && (modManagerContentTypeContentTypeFromIntentValue = contentTypeFromIntentValue(jSONObjectOptJSONObject.optString("contentType", ""))) != null) {
                tryRegisterInstalledEntry(file, modManagerContentTypeContentTypeFromIntentValue, jSONObjectOptJSONObject);
            }
        }
    }

    private static void tryRegisterInstalledEntry(File file, ModManagerContentType modManagerContentType, JSONObject jSONObject) {
        try {
            for (Method method : ModManagerManifest.class.getDeclaredMethods()) {
                if (Modifier.isStatic(method.getModifiers())) {
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    String lowerCase = method.getName().toLowerCase(Locale.US);
                    if (lowerCase.contains("add") || lowerCase.contains("put") || lowerCase.contains("save") || lowerCase.contains("record") || lowerCase.contains("register") || lowerCase.contains("entry")) {
                        if (parameterTypes.length == 3 && File.class.equals(parameterTypes[0]) && ModManagerContentType.class.equals(parameterTypes[1]) && JSONObject.class.equals(parameterTypes[2])) {
                            method.setAccessible(true);
                            method.invoke(null, file, modManagerContentType, jSONObject);
                            return;
                        } else if (parameterTypes.length == 4 && File.class.equals(parameterTypes[0]) && ModManagerContentType.class.equals(parameterTypes[1]) && File.class.equals(parameterTypes[2]) && JSONObject.class.equals(parameterTypes[3])) {
                            method.setAccessible(true);
                            File fileResolveInstalledContentFile = resolveInstalledContentFile(file, jSONObject);
                            if (fileResolveInstalledContentFile != null) {
                                method.invoke(null, file, modManagerContentType, fileResolveInstalledContentFile, jSONObject);
                                return;
                            }
                            return;
                        }
                    }
                }
            }
        } catch (Throwable th) {
            Logging.i(TAG, "Unable to mirror modpack file into ModManagerManifest: " + th.getMessage());
        }
    }

    private static File resolveInstalledContentFile(File file, JSONObject jSONObject) {
        String strOptString = jSONObject.optString("relativePath", jSONObject.optString("filePath", ""));
        if (isSafeRelativePath(strOptString)) {
            return new File(file, strOptString.replace('/', File.separatorChar));
        }
        return null;
    }

    private static ModManagerContentType resolveContentTypeForRelativePath(String str) {
        String lowerCase = normalizeZipPath(str).toLowerCase(Locale.US);
        if (lowerCase.startsWith("mods/") && (lowerCase.endsWith(".jar") || lowerCase.endsWith(".jar.disabled"))) {
            return ModManagerContentType.MODS;
        }
        if (lowerCase.startsWith("resourcepacks/") && (lowerCase.endsWith(".zip") || lowerCase.endsWith(".zip.disabled"))) {
            return ModManagerContentType.RESOURCEPACKS;
        }
        if (!lowerCase.startsWith("shaderpacks/")) {
            return null;
        }
        if (lowerCase.endsWith(".zip") || lowerCase.endsWith(".zip.disabled")) {
            return ModManagerContentType.SHADERPACKS;
        }
        return null;
    }

    private static ModManagerContentType contentTypeFromIntentValue(String str) {
        if (str == null) {
            return null;
        }
        if ("mods".equalsIgnoreCase(str)) {
            return ModManagerContentType.MODS;
        }
        if ("resourcepacks".equalsIgnoreCase(str)) {
            return ModManagerContentType.RESOURCEPACKS;
        }
        if ("shaderpacks".equalsIgnoreCase(str) || "shaders".equalsIgnoreCase(str)) {
            return ModManagerContentType.SHADERPACKS;
        }
        return null;
    }

    private static String resolveCurseForgeSha1(JSONArray jSONArray) {
        if (jSONArray == null) {
            return "";
        }
        for (int i = 0; i < jSONArray.length(); i++) {
            JSONObject jSONObjectOptJSONObject = jSONArray.optJSONObject(i);
            if (jSONObjectOptJSONObject != null) {
                String strTrim = jSONObjectOptJSONObject.optString("value", "").trim();
                if (jSONObjectOptJSONObject.optInt("algo", 0) == 1 && !isBlank(strTrim)) {
                    return strTrim;
                }
            }
        }
        return "";
    }

    private static ParsedModrinthDownloadIds parseModrinthDownloadIds(String str) {
        String strCleanDownloadUrl = cleanDownloadUrl(str);
        int iIndexOf = strCleanDownloadUrl.indexOf("/data/");
        String strTrim = "";
        if (iIndexOf < 0) {
            return new ParsedModrinthDownloadIds("", "");
        }
        String[] strArrSplit = strCleanDownloadUrl.substring(iIndexOf + "/data/".length()).split("/");
        String strTrim2 = strArrSplit.length > 0 ? strArrSplit[0].trim() : "";
        if (strArrSplit.length > 2 && "versions".equals(strArrSplit[1])) {
            strTrim = strArrSplit[2].trim();
        }
        return new ParsedModrinthDownloadIds(strTrim2, strTrim);
    }

    private static String safeCanonicalPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (Throwable unused) {
            return file.getAbsolutePath();
        }
    }

    private static String stripExtension(String str) {
        if (str.toLowerCase(Locale.US).endsWith(".disabled")) {
            str = str.substring(0, str.length() - ".disabled".length());
        }
        int iLastIndexOf = str.lastIndexOf(46);
        return iLastIndexOf > 0 ? str.substring(0, iLastIndexOf) : str;
    }

    private static void writeInstalledPackManifest(File file, String str, String str2, String str3, LoaderSpec loaderSpec, JSONObject jSONObject, JSONObject jSONObject2) {
        try {
            File javaLauncherMetadataDirectory = getJavaLauncherMetadataDirectory(file);
            JSONObject jSONObject3 = new JSONObject();
            jSONObject3.put("type", "modpack");
            jSONObject3.put("platform", str);
            jSONObject3.put("name", str2);
            jSONObject3.put("minecraftVersion", str3);
            jSONObject3.put("loader", loaderSpec.loaderName);
            jSONObject3.put("loaderVersion", loaderSpec.loaderVersion);
            jSONObject3.put("installedAt", System.currentTimeMillis());
            jSONObject3.put("contentManifest", MODPACK_FILES_MANIFEST_FILE);
            if (jSONObject != null) {
                jSONObject3.put("sourceManifest", jSONObject);
            }
            if (jSONObject2 != null) {
                jSONObject3.put("extra", jSONObject2);
            }
            FileOutputStream fileOutputStream = new FileOutputStream(new File(javaLauncherMetadataDirectory, MODPACK_MANIFEST_FILE));
            try {
                fileOutputStream.write(jSONObject3.toString(2).getBytes("UTF-8"));
                fileOutputStream.close();
            } finally {
            }
        } catch (Throwable th) {
            Logging.e(TAG, "Unable to write modpack manifest", th);
        }
    }

    private static String resolveCurseForgeDownloadUrl(String str, int i, int i2, String str2, JSONObject jSONObject) {
        String strCleanDownloadUrl = jSONObject == null ? "" : cleanDownloadUrl(optJsonString(jSONObject, "downloadUrl"));
        if (isHttpUrl(strCleanDownloadUrl)) {
            return strCleanDownloadUrl;
        }
        if (i > 0 && i2 > 0) {
            try {
                String curseForgeDownloadUrlResponse = parseCurseForgeDownloadUrlResponse(httpGetString("https://api.curseforge.com/v1/mods/" + i + "/files/" + i2 + "/download-url", str));
                if (isHttpUrl(curseForgeDownloadUrlResponse)) {
                    return curseForgeDownloadUrlResponse;
                }
            } catch (Throwable th) {
                Logging.i(TAG, "CurseForge download-url endpoint failed for " + i + ":" + i2 + " - " + th.getMessage());
            }
        }
        if (i2 <= 0 || isBlank(str2)) {
            return "";
        }
        return buildCurseForgeCdnUrl(i2, str2);
    }

    private static String parseCurseForgeDownloadUrlResponse(String str) {
        String strCleanDownloadUrl = cleanDownloadUrl(str);
        if (isBlank(strCleanDownloadUrl)) {
            return "";
        }
        if (strCleanDownloadUrl.startsWith("{") && strCleanDownloadUrl.endsWith("}")) {
            try {
                JSONObject jSONObject = new JSONObject(strCleanDownloadUrl);
                String strCleanDownloadUrl2 = cleanDownloadUrl(optJsonString(jSONObject, "data"));
                if (isHttpUrl(strCleanDownloadUrl2)) {
                    return strCleanDownloadUrl2;
                }
                String strCleanDownloadUrl3 = cleanDownloadUrl(optJsonString(jSONObject, "downloadUrl"));
                if (isHttpUrl(strCleanDownloadUrl3)) {
                    return strCleanDownloadUrl3;
                }
            } catch (Throwable unused) {
            }
        }
        return strCleanDownloadUrl;
    }

    private static String buildCurseForgeCdnUrl(int i, String str) {
        return "https://edge.forgecdn.net/files/" + (i / 1000) + "/" + String.format(Locale.US, "%03d", Integer.valueOf(i % 1000)) + "/" + urlEncodePathSegment(str);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String cleanDownloadUrl(String str) {
        if (str == null) {
            return "";
        }
        String strTrim = str.trim();
        if (strTrim.isEmpty() || "null".equalsIgnoreCase(strTrim) || "<null>".equalsIgnoreCase(strTrim) || "\"null\"".equalsIgnoreCase(strTrim) || "'null'".equalsIgnoreCase(strTrim)) {
            return "";
        }
        if ((strTrim.startsWith("\"") && strTrim.endsWith("\"")) || (strTrim.startsWith("'") && strTrim.endsWith("'"))) {
            strTrim = strTrim.substring(1, strTrim.length() - 1).trim();
        }
        return "null".equalsIgnoreCase(strTrim) ? "" : strTrim;
    }

    private static boolean isHttpUrl(String str) {
        String strCleanDownloadUrl = cleanDownloadUrl(str);
        return strCleanDownloadUrl.startsWith("http://") || strCleanDownloadUrl.startsWith("https://");
    }

    private static String optJsonString(JSONObject jSONObject, String str) {
        Object objOpt;
        return (jSONObject == null || !jSONObject.has(str) || jSONObject.isNull(str) || (objOpt = jSONObject.opt(str)) == null || objOpt == JSONObject.NULL) ? "" : String.valueOf(objOpt).trim();
    }

    private static String urlEncodePathSegment(String str) {
        try {
            return URLEncoder.encode(str, "UTF-8").replace("+", "%20").replace("%2F", "/");
        } catch (Throwable unused) {
            return str.replace(" ", "%20");
        }
    }

    private static void verifyHashesIfPresent(File file, JSONObject jSONObject) throws Exception {
        if (jSONObject == null) {
            return;
        }
        String strTrim = jSONObject.optString("sha1", "").trim();
        if (!isBlank(strTrim) && !strTrim.equalsIgnoreCase(hashFile(file, "SHA-1"))) {
            throw new IOException("SHA-1 mismatch for " + file.getName());
        }
    }

    private static String hashFile(File file, String str) throws Exception {
        int i;
        MessageDigest messageDigest = MessageDigest.getInstance(str);
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
                sb.append(String.format(Locale.US, "%02x", Integer.valueOf(b & 255)));
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

    private static void downloadFile(String str, File file, Listener listener) throws Exception {
        String strCleanDownloadUrl = cleanDownloadUrl(str);
        if (!isHttpUrl(strCleanDownloadUrl)) {
            throw new IOException("Missing or invalid download URL for " + file.getName());
        }
        ensureParent(file);
        HttpURLConnection httpURLConnectionOpenConnection = openConnection(strCleanDownloadUrl, null);
        int responseCode = httpURLConnectionOpenConnection.getResponseCode();
        if (responseCode / 100 != 2) {
            throw new IOException("HTTP " + responseCode + " while downloading " + strCleanDownloadUrl);
        }
        int contentLength = httpURLConnectionOpenConnection.getContentLength();
        try {
            InputStream inputStream = httpURLConnectionOpenConnection.getInputStream();
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                try {
                    byte[] bArr = new byte[65536];
                    int i = 0;
                    while (true) {
                        int i2 = inputStream.read(bArr);
                        if (i2 == -1) {
                            break;
                        }
                        fileOutputStream.write(bArr, 0, i2);
                        i += i2;
                        if (listener != null && contentLength > 0) {
                            listener.onProgress(i, contentLength);
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

    private static String httpGetString(String str, String str2) throws Exception {
        HttpURLConnection httpURLConnectionOpenConnection = openConnection(str, str2);
        int responseCode = httpURLConnectionOpenConnection.getResponseCode();
        int i = responseCode / 100;
        InputStream inputStream = i == 2 ? httpURLConnectionOpenConnection.getInputStream() : httpURLConnectionOpenConnection.getErrorStream();
        String toString = inputStream == null ? "" : readToString(inputStream);
        httpURLConnectionOpenConnection.disconnect();
        if (i == 2) {
            return toString;
        }
        throw new IOException("HTTP " + responseCode + ": " + toString);
    }

    private static String httpPostString(String str, JSONObject jSONObject, String str2) throws Exception {
        HttpURLConnection httpURLConnectionOpenConnection = openConnection(str, str2);
        httpURLConnectionOpenConnection.setRequestMethod(ShareTarget.METHOD_POST);
        httpURLConnectionOpenConnection.setDoOutput(true);
        httpURLConnectionOpenConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        byte[] bytes = jSONObject.toString().getBytes("UTF-8");
        httpURLConnectionOpenConnection.setFixedLengthStreamingMode(bytes.length);
        OutputStream outputStream = httpURLConnectionOpenConnection.getOutputStream();
        try {
            outputStream.write(bytes);
            if (outputStream != null) {
                outputStream.close();
            }
            int responseCode = httpURLConnectionOpenConnection.getResponseCode();
            int i = responseCode / 100;
            InputStream inputStream = i == 2 ? httpURLConnectionOpenConnection.getInputStream() : httpURLConnectionOpenConnection.getErrorStream();
            String toString = inputStream == null ? "" : readToString(inputStream);
            httpURLConnectionOpenConnection.disconnect();
            if (i == 2) {
                return toString;
            }
            throw new IOException("HTTP " + responseCode + ": " + toString);
        } catch (Throwable th) {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    private static HttpURLConnection openConnection(String str, String str2) throws Exception {
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(str).openConnection();
        httpURLConnection.setConnectTimeout(30000);
        httpURLConnection.setReadTimeout(60000);
        httpURLConnection.setRequestProperty("User-Agent", "JavaLauncher/Modpacks");
        if (!isBlank(str2)) {
            httpURLConnection.setRequestProperty("x-api-key", str2.trim());
        }
        return httpURLConnection;
    }

    private static String readZipEntryText(ZipFile zipFile, String str) throws Exception {
        ZipEntry entry = zipFile.getEntry(str);
        if (entry == null || entry.isDirectory()) {
            throw new IOException("Missing " + str);
        }
        InputStream inputStream = zipFile.getInputStream(entry);
        try {
            String toString = readToString(inputStream);
            if (inputStream != null) {
                inputStream.close();
            }
            return toString;
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

    private static String readToString(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        copyStream(inputStream, byteArrayOutputStream);
        return byteArrayOutputStream.toString("UTF-8");
    }

    private static void copyStream(InputStream inputStream, OutputStream outputStream) throws IOException {
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

    private static void ensureParent(File file) throws IOException {
        File parentFile = file.getParentFile();
        if (parentFile != null && !parentFile.exists() && !parentFile.mkdirs()) {
            throw new IOException("Unable to create folder: " + parentFile.getAbsolutePath());
        }
    }

    private static boolean isSafeRelativePath(String str) {
        if (str == null) {
            return false;
        }
        String strTrim = normalizeZipPath(str).trim();
        if (strTrim.isEmpty() || strTrim.startsWith("/") || strTrim.startsWith("\\") || strTrim.matches("^[A-Za-z]:[/\\\\].*")) {
            return false;
        }
        for (String str2 : strTrim.split("/")) {
            if (str2.equals("..") || str2.equals(".")) {
                return false;
            }
        }
        return true;
    }

    private static String normalizeZipPath(String str) {
        return str.replace('\\', '/');
    }

    private static ArrayList<String> jsonArrayToStringList(JSONArray jSONArray) {
        ArrayList<String> arrayList = new ArrayList<>();
        if (jSONArray == null) {
            return arrayList;
        }
        for (int i = 0; i < jSONArray.length(); i++) {
            String strTrim = jSONArray.optString(i, "").trim();
            if (!isBlank(strTrim)) {
                arrayList.add(strTrim);
            }
        }
        return arrayList;
    }

    /* JADX WARN: Removed duplicated region for block: B:28:0x0068 A[PHI: r3
      0x0068: PHI (r3v2 java.lang.String) = 
      (r3v1 java.lang.String)
      (r3v1 java.lang.String)
      (r3v3 java.lang.String)
      (r3v3 java.lang.String)
      (r3v4 java.lang.String)
      (r3v4 java.lang.String)
      (r3v5 java.lang.String)
      (r3v5 java.lang.String)
     binds: [B:10:0x002b, B:12:0x0033, B:15:0x003c, B:17:0x0044, B:20:0x004d, B:22:0x0055, B:25:0x005e, B:27:0x0066] A[DONT_GENERATE, DONT_INLINE]] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private static java.util.ArrayList<java.lang.String> extractCurseForgeLoaders(java.util.ArrayList<java.lang.String> r5) {
        /*
            java.util.ArrayList r0 = new java.util.ArrayList
            r0.<init>()
            java.util.Iterator r5 = r5.iterator()
        L9:
            boolean r1 = r5.hasNext()
            if (r1 == 0) goto L79
            java.lang.Object r1 = r5.next()
            java.lang.String r1 = (java.lang.String) r1
            java.lang.String r2 = ""
            if (r1 != 0) goto L1b
            r1 = r2
            goto L25
        L1b:
            java.lang.String r1 = r1.trim()
            java.util.Locale r3 = java.util.Locale.US
            java.lang.String r1 = r1.toLowerCase(r3)
        L25:
            java.lang.String r3 = "forge"
            boolean r4 = r3.equals(r1)
            if (r4 != 0) goto L68
            java.lang.String r4 = "forge "
            boolean r4 = r1.startsWith(r4)
            if (r4 == 0) goto L36
            goto L68
        L36:
            java.lang.String r3 = "fabric"
            boolean r4 = r3.equals(r1)
            if (r4 != 0) goto L68
            java.lang.String r4 = "fabric "
            boolean r4 = r1.startsWith(r4)
            if (r4 == 0) goto L47
            goto L68
        L47:
            java.lang.String r3 = "neoforge"
            boolean r4 = r3.equals(r1)
            if (r4 != 0) goto L68
            java.lang.String r4 = "neoforge "
            boolean r4 = r1.startsWith(r4)
            if (r4 == 0) goto L58
            goto L68
        L58:
            java.lang.String r3 = "quilt"
            boolean r4 = r3.equals(r1)
            if (r4 != 0) goto L68
            java.lang.String r4 = "quilt "
            boolean r1 = r1.startsWith(r4)
            if (r1 == 0) goto L69
        L68:
            r2 = r3
        L69:
            boolean r1 = isBlank(r2)
            if (r1 != 0) goto L9
            boolean r1 = containsIgnoreCase(r0, r2)
            if (r1 != 0) goto L9
            r0.add(r2)
            goto L9
        L79:
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: ca.dnamobile.javalauncher.modmanager.ModpackInstallManager.extractCurseForgeLoaders(java.util.ArrayList):java.util.ArrayList");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean containsIgnoreCase(ArrayList<String> arrayList, String str) {
        Iterator<String> it = arrayList.iterator();
        while (it.hasNext()) {
            if (str.equalsIgnoreCase(it.next())) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String stripArchiveExtension(String str) {
        String strTrim = str.trim();
        String lowerCase = strTrim.toLowerCase(Locale.US);
        if (lowerCase.endsWith(".mrpack")) {
            return strTrim.substring(0, strTrim.length() - ".mrpack".length());
        }
        return lowerCase.endsWith(".zip") ? strTrim.substring(0, strTrim.length() - ".zip".length()) : strTrim;
    }

    private static boolean jsonArrayContains(JSONArray jSONArray, String str) {
        if (jSONArray == null) {
            return false;
        }
        for (int i = 0; i < jSONArray.length(); i++) {
            if (str.equalsIgnoreCase(jSONArray.optString(i, ""))) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String normalizeLoader(String str) {
        String lowerCase = safe(str).trim().toLowerCase(Locale.US);
        if (lowerCase.equals("vanilla") || lowerCase.equals("minecraft")) {
            return "";
        }
        return lowerCase.equals("fabric-loader") ? "fabric" : lowerCase.equals("neoforge") ? "neoforge" : lowerCase;
    }

    private static int curseForgeLoaderType(String str) {
        String strNormalizeLoader = normalizeLoader(str);
        if ("forge".equals(strNormalizeLoader)) {
            return 1;
        }
        if ("fabric".equals(strNormalizeLoader)) {
            return 4;
        }
        if ("quilt".equals(strNormalizeLoader)) {
            return 5;
        }
        return "neoforge".equals(strNormalizeLoader) ? 6 : 0;
    }

    private static int parsePositiveInt(String str, String str2) {
        try {
            int i = Integer.parseInt(str.trim());
            if (i > 0) {
                return i;
            }
        } catch (Throwable unused) {
        }
        throw new IllegalArgumentException("Invalid " + str2 + ": " + str);
    }

    private static String sanitizeFileName(String str, String str2) {
        String strReplaceAll = (isBlank(str) ? str2 : str.trim()).replaceAll("[\\\\/:*?\"<>|]", "_");
        return (strReplaceAll.isEmpty() || ".".equals(strReplaceAll) || "..".equals(strReplaceAll)) ? str2 : strReplaceAll;
    }

    private static String urlEncode(String str) throws Exception {
        return URLEncoder.encode(str, "UTF-8").replace("+", "%20");
    }

    private static String firstNonBlank(String str, String str2) {
        if (isBlank(str)) {
            str = safe(str2);
        }
        return str.trim();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String safe(String str) {
        return str == null ? "" : str;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isBlank(String str) {
        if (str == null) {
            return true;
        }
        String strTrim = str.trim();
        return strTrim.isEmpty() || "null".equalsIgnoreCase(strTrim) || "<null>".equalsIgnoreCase(strTrim) || "\"null\"".equalsIgnoreCase(strTrim) || "'null'".equalsIgnoreCase(strTrim);
    }

    public static final class ModpackVersionChoice {
        public final String datePublished;
        public final String downloadUrl;
        public final int fileId;
        public final String fileName;
        public final ArrayList<String> gameVersions;
        public final ArrayList<String> loaders;
        public final ModManagerSource source;
        public final String versionId;
        public final String versionName;
        public final String versionNumber;

        ModpackVersionChoice(ModManagerSource modManagerSource, String str, int i, String str2, String str3, String str4, String str5, ArrayList<String> arrayList, ArrayList<String> arrayList2, String str6) {
            this.source = modManagerSource;
            this.versionId = ModpackInstallManager.safe(str).trim();
            this.fileId = i;
            this.versionName = ModpackInstallManager.safe(str2).trim();
            this.versionNumber = ModpackInstallManager.safe(str3).trim();
            this.fileName = ModpackInstallManager.safe(str4).trim();
            this.downloadUrl = ModpackInstallManager.cleanDownloadUrl(str5);
            this.gameVersions = arrayList;
            this.loaders = arrayList2;
            this.datePublished = ModpackInstallManager.safe(str6).trim();
        }

        public String getDisplayTitle() {
            return !ModpackInstallManager.isBlank(this.versionName) ? this.versionName : !ModpackInstallManager.isBlank(this.versionNumber) ? this.versionNumber : !ModpackInstallManager.isBlank(this.fileName) ? ModpackInstallManager.stripArchiveExtension(this.fileName) : this.source == ModManagerSource.CURSEFORGE ? "CurseForge file " + this.fileId : this.versionId;
        }

        public String getDisplaySubtitle() {
            StringBuilder sb = new StringBuilder();
            if (!this.gameVersions.isEmpty()) {
                sb.append("Minecraft ").append(joinShortList(this.gameVersions, 4));
            }
            if (!this.loaders.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(" · ");
                }
                sb.append("Loader ").append(joinShortList(this.loaders, 3));
            }
            if (!ModpackInstallManager.isBlank(this.datePublished)) {
                if (sb.length() > 0) {
                    sb.append(" · ");
                }
                String str = this.datePublished;
                sb.append(str.substring(0, Math.min(10, str.length())));
            }
            if (!ModpackInstallManager.isBlank(this.fileName)) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(this.fileName);
            }
            return sb.length() == 0 ? "No version metadata available" : sb.toString();
        }

        public boolean isCompatibleWith(String str, String str2) {
            String strTrim = ModpackInstallManager.safe(str).trim();
            String strNormalizeLoader = ModpackInstallManager.normalizeLoader(str2);
            return (ModpackInstallManager.isBlank(strTrim) || this.gameVersions.isEmpty() || ModpackInstallManager.containsIgnoreCase(this.gameVersions, strTrim)) && (ModpackInstallManager.isBlank(strNormalizeLoader) || this.loaders.isEmpty() || ModpackInstallManager.containsIgnoreCase(this.loaders, strNormalizeLoader));
        }

        private static String joinShortList(ArrayList<String> arrayList, int i) {
            StringBuilder sb = new StringBuilder();
            int iMin = Math.min(arrayList.size(), Math.max(1, i));
            for (int i2 = 0; i2 < iMin; i2++) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(arrayList.get(i2));
            }
            if (arrayList.size() > iMin) {
                sb.append(" +").append(arrayList.size() - iMin);
            }
            return sb.toString();
        }
    }

    private static String normalizeLoaderVersionForInstaller(String str, String str2, String str3) {
        String strTrim = safe(str2).trim();
        if (isBlank(strTrim)) {
            return strTrim;
        }
        if ("Forge".equalsIgnoreCase(str)) {
            return normalizeForgeLoaderVersion(strTrim, str3);
        }
        if (!"NeoForge".equalsIgnoreCase(str)) {
            return strTrim;
        }
        String strStripNeoForgePrefix = stripNeoForgePrefix(strTrim);
        String str4 = str3.trim() + "-";
        if (!isBlank(str3) && strStripNeoForgePrefix.startsWith(str4)) {
            strStripNeoForgePrefix = strStripNeoForgePrefix.substring(str4.length()).trim();
        }
        String str5 = strStripNeoForgePrefix;
        Matcher matcher = Pattern.compile("^1\\.\\d+(?:\\.\\d+)?-(.+)$").matcher(str5);
        return matcher.matches() ? matcher.group(1).trim() : str5;
    }

    private static String normalizeForgeLoaderVersion(String str, String str2) {
        int i;
        String strTrim = str.trim();
        int iLastIndexOf = strTrim.lastIndexOf(58);
        if (iLastIndexOf >= 0 && (i = iLastIndexOf + 1) < strTrim.length()) {
            strTrim = strTrim.substring(i).trim();
        }
        if (strTrim.toLowerCase(Locale.US).startsWith("forge-")) {
            strTrim = strTrim.substring("forge-".length()).trim();
        }
        String str3 = str2.trim() + "-";
        if (!isBlank(str2) && strTrim.startsWith(str3)) {
            strTrim = strTrim.substring(str3.length()).trim();
        }
        Matcher matcher = Pattern.compile("^1\\.\\d+(?:\\.\\d+)?-(.+)$").matcher(strTrim);
        return matcher.matches() ? matcher.group(1).trim() : strTrim;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String stripNeoForgePrefix(String str) {
        String strTrim = str.trim();
        return strTrim.toLowerCase(Locale.US).startsWith("neoforge-") ? strTrim.substring("neoforge-".length()).trim() : strTrim;
    }

    private static final class PendingModrinthInstalledFile {
        final String downloadUrl;
        final JSONObject fileMetadata;
        final String relativePath;
        final File target;

        PendingModrinthInstalledFile(File file, String str, JSONObject jSONObject, String str2) {
            this.target = file;
            this.relativePath = str;
            this.fileMetadata = jSONObject;
            this.downloadUrl = str2;
        }
    }

    private static final class ParsedModrinthDownloadIds {
        final String projectId;
        final String versionId;

        ParsedModrinthDownloadIds(String str, String str2) {
            this.projectId = str;
            this.versionId = str2;
        }
    }

    private static final class LoaderSpec {
        final String loaderName;
        final String loaderVersion;

        LoaderSpec(String str, String str2) {
            this.loaderName = str;
            this.loaderVersion = str2;
        }

        static LoaderSpec fromModrinthDependencies(JSONObject jSONObject) {
            if (!ModpackInstallManager.isBlank(jSONObject.optString("fabric-loader", ""))) {
                return new LoaderSpec("Fabric", jSONObject.optString("fabric-loader", ""));
            }
            if (!ModpackInstallManager.isBlank(jSONObject.optString("forge", ""))) {
                return new LoaderSpec("Forge", jSONObject.optString("forge", ""));
            }
            if (!ModpackInstallManager.isBlank(jSONObject.optString("neoforge", ""))) {
                return new LoaderSpec("NeoForge", ModpackInstallManager.stripNeoForgePrefix(jSONObject.optString("neoforge", "")));
            }
            if (!ModpackInstallManager.isBlank(jSONObject.optString("quilt-loader", ""))) {
                return new LoaderSpec("Quilt", jSONObject.optString("quilt-loader", ""));
            }
            return new LoaderSpec("Vanilla", null);
        }

        static LoaderSpec fromMultiMcPackJson(JSONObject jSONObject) {
            JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("components");
            if (jSONArrayOptJSONArray == null) {
                return new LoaderSpec("Vanilla", null);
            }
            for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
                JSONObject jSONObjectOptJSONObject = jSONArrayOptJSONArray.optJSONObject(i);
                if (jSONObjectOptJSONObject != null) {
                    String lowerCase = jSONObjectOptJSONObject.optString("uid", "").trim().toLowerCase(Locale.US);
                    String strTrim = jSONObjectOptJSONObject.optString("version", "").trim();
                    if ("net.fabricmc.fabric-loader".equals(lowerCase)) {
                        return new LoaderSpec("Fabric", strTrim);
                    }
                    if ("net.minecraftforge".equals(lowerCase)) {
                        return new LoaderSpec("Forge", strTrim);
                    }
                    if ("net.neoforged".equals(lowerCase) || "net.neoforged.neoforge".equals(lowerCase)) {
                        return new LoaderSpec("NeoForge", ModpackInstallManager.stripNeoForgePrefix(strTrim));
                    }
                    if ("org.quiltmc.quilt-loader".equals(lowerCase)) {
                        return new LoaderSpec("Quilt", strTrim);
                    }
                }
            }
            return new LoaderSpec("Vanilla", null);
        }

        static LoaderSpec fromCurseForgeManifest(JSONArray jSONArray) {
            if (jSONArray == null) {
                return new LoaderSpec("Vanilla", null);
            }
            JSONObject jSONObject = null;
            int i = 0;
            while (true) {
                if (i >= jSONArray.length()) {
                    break;
                }
                JSONObject jSONObjectOptJSONObject = jSONArray.optJSONObject(i);
                if (jSONObjectOptJSONObject != null) {
                    if (jSONObjectOptJSONObject.optBoolean("primary", false)) {
                        jSONObject = jSONObjectOptJSONObject;
                        break;
                    }
                    if (jSONObject == null) {
                        jSONObject = jSONObjectOptJSONObject;
                    }
                }
                i++;
            }
            if (jSONObject == null) {
                return new LoaderSpec("Vanilla", null);
            }
            String strTrim = jSONObject.optString("id", "").trim();
            return strTrim.toLowerCase(Locale.US).startsWith("fabric-") ? new LoaderSpec("Fabric", strTrim.substring("fabric-".length())) : strTrim.toLowerCase(Locale.US).startsWith("forge-") ? new LoaderSpec("Forge", strTrim.substring("forge-".length())) : strTrim.toLowerCase(Locale.US).startsWith("neoforge-") ? new LoaderSpec("NeoForge", ModpackInstallManager.stripNeoForgePrefix(strTrim)) : strTrim.toLowerCase(Locale.US).startsWith("quilt-") ? new LoaderSpec("Quilt", strTrim.substring("quilt-".length())) : new LoaderSpec("Vanilla", null);
        }
    }
}
