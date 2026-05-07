package ca.dnamobile.javalauncher.modmanager;

import android.content.Context;
import android.net.Uri;
import ca.dnamobile.javalauncher.feature.log.Logging;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class ModpackExportManager {
    private static final int BUFFER_SIZE = 65536;
    private static final String JAVALAUNCHER_METADATA_DIRECTORY = ".javalauncher";
    private static final String JAVALAUNCHER_PACK_ICON_ENTRY = "javalauncher-pack-icon.png";
    private static final String MODPACK_FILES_MANIFEST_FILE = "modpack_files_manifest.json";
    private static final String MODPACK_MANIFEST_FILE = "modpack_manifest.json";
    private static final String TAG = "ModpackExport";

    public interface Listener {
        void onComplete(String str);

        void onError(Throwable th);

        void onProgress(int i, int i2);

        void onStatus(String str);
    }

    public enum Platform {
        MODRINTH,
        CURSEFORGE,
        MULTIMC
    }

    private ModpackExportManager() {
    }

    public static void exportToUri(Context context, File file, String str, String str2, String str3, String str4, Platform platform, Uri uri, Listener listener) {
        exportToUri(context, file, str, str2, str3, str4, null, platform, uri, listener);
    }

    /* JADX WARN: Removed duplicated region for block: B:52:0x00dd A[DONT_GENERATE] */
    /* JADX WARN: Removed duplicated region for block: B:73:? A[RETURN, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public static void exportToUri(android.content.Context r11, java.io.File r12, java.lang.String r13, java.lang.String r14, java.lang.String r15, java.lang.String r16, java.io.File r17, ca.dnamobile.javalauncher.modmanager.ModpackExportManager.Platform r18, android.net.Uri r19, ca.dnamobile.javalauncher.modmanager.ModpackExportManager.Listener r20) {
        /*
            Method dump skipped, instruction units count: 232
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: ca.dnamobile.javalauncher.modmanager.ModpackExportManager.exportToUri(android.content.Context, java.io.File, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.io.File, ca.dnamobile.javalauncher.modmanager.ModpackExportManager$Platform, android.net.Uri, ca.dnamobile.javalauncher.modmanager.ModpackExportManager$Listener):void");
    }

    private static void exportModrinth(File file, File file2, String str, String str2, String str3, String str4, File file3, Listener listener) throws Exception {
        ArrayList<FileRecord> arrayListCollectContentRecords = collectContentRecords(file2);
        ArrayList arrayList = new ArrayList();
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("formatVersion", 1);
        jSONObject.put("game", "minecraft");
        jSONObject.put("versionId", "1.0.0");
        jSONObject.put("name", str);
        jSONObject.put("summary", "Exported from JavaLauncher");
        JSONObject jSONObject2 = new JSONObject();
        jSONObject2.put("minecraft", str2);
        addModrinthLoaderDependency(jSONObject2, file2, str3, str4);
        jSONObject.put("dependencies", jSONObject2);
        JSONArray jSONArray = new JSONArray();
        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(file));
        try {
            addInstanceIconToZip(zipOutputStream, file3);
            int i = 0;
            for (FileRecord fileRecord : arrayListCollectContentRecords) {
                i++;
                listener.onStatus("Adding " + fileRecord.relativePath + "...");
                listener.onProgress(i, Math.max(1, arrayListCollectContentRecords.size()));
                if (fileRecord.source == ModManagerSource.MODRINTH && !isBlank(fileRecord.downloadUrl)) {
                    JSONObject jSONObject3 = new JSONObject();
                    jSONObject3.put("path", fileRecord.relativePath);
                    JSONObject jSONObject4 = new JSONObject();
                    jSONObject4.put("sha1", sha1(fileRecord.file));
                    jSONObject4.put("sha512", sha512(fileRecord.file));
                    jSONObject3.put("hashes", jSONObject4);
                    JSONArray jSONArray2 = new JSONArray();
                    jSONArray2.put(fileRecord.downloadUrl);
                    jSONObject3.put("downloads", jSONArray2);
                    jSONObject3.put("fileSize", fileRecord.file.length());
                    JSONObject jSONObject5 = new JSONObject();
                    jSONObject5.put("client", "required");
                    jSONObject5.put("server", "optional");
                    jSONObject3.put("env", jSONObject5);
                    jSONArray.put(jSONObject3);
                } else {
                    addFileToZip(zipOutputStream, fileRecord.file, "overrides/" + fileRecord.relativePath);
                    arrayList.add(fileRecord.relativePath + " was exported as an override because it is not tracked as a Modrinth file with a download URL.");
                }
            }
            addCommonOverrides(zipOutputStream, file2, arrayList);
            jSONObject.put("files", jSONArray);
            addTextEntry(zipOutputStream, "modrinth.index.json", jSONObject.toString(2));
            addWarnings(zipOutputStream, arrayList, Platform.MODRINTH);
            zipOutputStream.close();
        } catch (Throwable th) {
            try {
                zipOutputStream.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    private static void exportCurseForge(File file, File file2, String str, String str2, String str3, String str4, File file3, Listener listener) throws Exception {
        ArrayList<FileRecord> arrayListCollectContentRecords = collectContentRecords(file2);
        ArrayList arrayList = new ArrayList();
        HashSet hashSet = new HashSet();
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("minecraft", buildCurseForgeMinecraftBlock(file2, str2, str3, str4));
        jSONObject.put("manifestType", "minecraftModpack");
        jSONObject.put("manifestVersion", 1);
        jSONObject.put("name", str);
        jSONObject.put("version", "1.0.0");
        jSONObject.put("author", "JavaLauncher");
        jSONObject.put("overrides", "overrides");
        JSONArray jSONArray = new JSONArray();
        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(file));
        try {
            addInstanceIconToZip(zipOutputStream, file3);
            int i = 0;
            for (FileRecord fileRecord : arrayListCollectContentRecords) {
                i++;
                listener.onStatus("Adding " + fileRecord.relativePath + "...");
                listener.onProgress(i, Math.max(1, arrayListCollectContentRecords.size()));
                if (fileRecord.source == ModManagerSource.CURSEFORGE && fileRecord.projectId > 0 && fileRecord.fileId > 0) {
                    if (hashSet.add(String.valueOf(fileRecord.projectId))) {
                        JSONObject jSONObject2 = new JSONObject();
                        jSONObject2.put("projectID", fileRecord.projectId);
                        jSONObject2.put("fileID", fileRecord.fileId);
                        jSONObject2.put("required", true);
                        jSONArray.put(jSONObject2);
                    } else {
                        arrayList.add(fileRecord.relativePath + " was skipped from CurseForge manifest because another file from the same project was already listed.");
                    }
                } else {
                    addFileToZip(zipOutputStream, fileRecord.file, "overrides/" + fileRecord.relativePath);
                    arrayList.add(fileRecord.relativePath + " was exported as an override because it is not tracked as a CurseForge project/file id.");
                }
            }
            addCommonOverrides(zipOutputStream, file2, arrayList);
            jSONObject.put("files", jSONArray);
            addTextEntry(zipOutputStream, "manifest.json", jSONObject.toString(2));
            addTextEntry(zipOutputStream, "modlist.html", buildModListHtml(arrayListCollectContentRecords));
            addWarnings(zipOutputStream, arrayList, Platform.CURSEFORGE);
            zipOutputStream.close();
        } catch (Throwable th) {
            try {
                zipOutputStream.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    private static void exportMultiMc(File file, File file2, String str, String str2, String str3, String str4, File file3, Listener listener) throws Exception {
        ArrayList arrayList = new ArrayList();
        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(file));
        try {
            addInstanceIconToZip(zipOutputStream, file3);
            listener.onStatus("Writing MultiMC metadata...");
            addTextEntry(zipOutputStream, "instance.cfg", buildMultiMcInstanceCfg(str));
            addTextEntry(zipOutputStream, "mmc-pack.json", buildMultiMcPackJson(file2, str2, str3, str4).toString(2));
            ArrayList<File> arrayListCollectMultiMcExportRoots = collectMultiMcExportRoots(file2);
            int i = 0;
            for (File file4 : arrayListCollectMultiMcExportRoots) {
                i++;
                listener.onStatus("Adding .minecraft/" + file4.getName() + "...");
                listener.onProgress(i, Math.max(1, arrayListCollectMultiMcExportRoots.size()));
                addFileOrDirectoryToZip(zipOutputStream, file4, ".minecraft/" + file4.getName());
            }
            File file5 = new File(file2, "options.txt");
            if (file5.isFile()) {
                addFileToZip(zipOutputStream, file5, ".minecraft/options.txt");
                arrayList.add("options.txt was included for private sharing. Remove it before sharing if it contains personal settings you do not want to ship.");
            }
            arrayList.add("MultiMC/Prism exports bundle files directly. Before sharing, make sure every bundled mod/resourcepack/shader/config is allowed to be redistributed.");
            addTextEntry(zipOutputStream, "README_JAVALAUNCHER_MULTIMC.txt", buildMultiMcReadme(str));
            addWarnings(zipOutputStream, arrayList, Platform.MULTIMC);
            zipOutputStream.close();
        } catch (Throwable th) {
            try {
                zipOutputStream.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    private static void addInstanceIconToZip(ZipOutputStream zipOutputStream, File file) throws Exception {
        if (file == null || !file.isFile() || file.length() <= 0) {
            return;
        }
        addFileToZip(zipOutputStream, file, JAVALAUNCHER_PACK_ICON_ENTRY);
    }

    private static ArrayList<FileRecord> collectContentRecords(File file) {
        ArrayList<FileRecord> arrayList = new ArrayList<>();
        collectFolderRecords(file, ModManagerContentType.MODS, "mods", arrayList);
        collectFolderRecords(file, ModManagerContentType.RESOURCEPACKS, "resourcepacks", arrayList);
        collectFolderRecords(file, ModManagerContentType.SHADERPACKS, "shaderpacks", arrayList);
        return arrayList;
    }

    private static void collectFolderRecords(File file, ModManagerContentType modManagerContentType, String str, ArrayList<FileRecord> arrayList) {
        JSONObject jSONObjectFindJavaLauncherInstalledContentEntry;
        File[] fileArrListFiles = new File(file, str).listFiles();
        if (fileArrListFiles == null) {
            return;
        }
        for (File file2 : fileArrListFiles) {
            if (!file2.isHidden() && file2.isFile()) {
                String lowerCase = file2.getName().toLowerCase(Locale.US);
                if (lowerCase.endsWith(".jar") || lowerCase.endsWith(".zip")) {
                    String str2 = str + "/" + file2.getName();
                    try {
                        jSONObjectFindJavaLauncherInstalledContentEntry = ModManagerManifest.getInstalledEntryForFile(file, modManagerContentType, file2);
                    } catch (Throwable unused) {
                        jSONObjectFindJavaLauncherInstalledContentEntry = null;
                    }
                    if (jSONObjectFindJavaLauncherInstalledContentEntry == null) {
                        jSONObjectFindJavaLauncherInstalledContentEntry = findJavaLauncherInstalledContentEntry(file, file2, str2);
                    }
                    arrayList.add(FileRecord.fromEntry(file2, str2, jSONObjectFindJavaLauncherInstalledContentEntry));
                }
            }
        }
    }

    private static JSONObject findJavaLauncherInstalledContentEntry(File file, File file2, String str) {
        File file3 = new File(new File(file, JAVALAUNCHER_METADATA_DIRECTORY), MODPACK_FILES_MANIFEST_FILE);
        if (!file3.isFile()) {
            return null;
        }
        String strSafeCanonicalPath = safeCanonicalPath(file2);
        String absolutePath = file2.getAbsolutePath();
        char c = '\\';
        String strReplace = str.replace('\\', '/');
        String name = file2.getName();
        try {
            JSONArray jSONArrayOptJSONArray = new JSONObject(readTextFile(file3)).optJSONArray("files");
            if (jSONArrayOptJSONArray == null) {
                return null;
            }
            JSONObject jSONObject = null;
            int i = 0;
            while (i < jSONArrayOptJSONArray.length()) {
                JSONObject jSONObjectOptJSONObject = jSONArrayOptJSONArray.optJSONObject(i);
                if (jSONObjectOptJSONObject != null) {
                    String strTrim = jSONObjectOptJSONObject.optString("canonicalPath", "").trim();
                    String strTrim2 = jSONObjectOptJSONObject.optString("absolutePath", "").trim();
                    String strReplace2 = optStringAny(jSONObjectOptJSONObject, "relativePath", "filePath", "path").replace(c, '/');
                    String strTrim3 = jSONObjectOptJSONObject.optString("fileName", "").trim();
                    if (!isBlank(strTrim) && strSafeCanonicalPath.equals(strTrim)) {
                        return jSONObjectOptJSONObject;
                    }
                    if (!isBlank(strTrim2) && absolutePath.equals(strTrim2)) {
                        return jSONObjectOptJSONObject;
                    }
                    if (!isBlank(strReplace2) && strReplace.equalsIgnoreCase(strReplace2)) {
                        return jSONObjectOptJSONObject;
                    }
                    if (!isBlank(strTrim3) && name.equalsIgnoreCase(strTrim3)) {
                        jSONObject = jSONObject == null ? jSONObjectOptJSONObject : null;
                    }
                }
                i++;
                c = '\\';
            }
            return jSONObject;
        } catch (Throwable th) {
            Logging.i(TAG, "Unable to read JavaLauncher modpack file metadata for export: " + th.getMessage());
            return null;
        }
    }

    private static String safeCanonicalPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (Throwable unused) {
            return file.getAbsolutePath();
        }
    }

    private static void addCommonOverrides(ZipOutputStream zipOutputStream, File file, ArrayList<String> arrayList) throws Exception {
        String[] strArr = {"config", "defaultconfigs", "kubejs", "scripts"};
        for (int i = 0; i < 4; i++) {
            String str = strArr[i];
            File file2 = new File(file, str);
            if (file2.exists()) {
                addFileOrDirectoryToZip(zipOutputStream, file2, "overrides/" + str);
            }
        }
        File file3 = new File(file, "options.txt");
        if (file3.isFile()) {
            addFileToZip(zipOutputStream, file3, "overrides/options.txt");
            arrayList.add("options.txt was included for private sharing. Remove it before publishing if it contains personal settings you do not want to ship.");
        }
    }

    private static void addModrinthLoaderDependency(JSONObject jSONObject, File file, String str, String str2) throws Exception {
        String strResolveLoaderVersion = resolveLoaderVersion(file, str, str2);
        if (isBlank(strResolveLoaderVersion)) {
            if (!"Vanilla".equalsIgnoreCase(str)) {
                throw new IllegalStateException("Unable to resolve " + str + " loader version for export. Open this instance once after installing the modpack, or reinstall the pack so JavaLauncher can write .javalauncher/modpack_manifest.json.");
            }
            return;
        }
        if ("Fabric".equalsIgnoreCase(str)) {
            jSONObject.put("fabric-loader", strResolveLoaderVersion);
            return;
        }
        if ("Forge".equalsIgnoreCase(str)) {
            jSONObject.put("forge", strResolveLoaderVersion);
        } else if ("NeoForge".equalsIgnoreCase(str)) {
            jSONObject.put("neoforge", strResolveLoaderVersion);
        } else if ("Quilt".equalsIgnoreCase(str)) {
            jSONObject.put("quilt-loader", strResolveLoaderVersion);
        }
    }

    private static String buildMultiMcInstanceCfg(String str) {
        return "InstanceType=OneSix\nname=" + sanitizeMultiMcCfgValue(str) + "\niconKey=default\nnotes=Exported from JavaLauncher.\n";
    }

    private static JSONObject buildMultiMcPackJson(File file, String str, String str2, String str3) throws Exception {
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("formatVersion", 1);
        JSONArray jSONArray = new JSONArray();
        JSONObject jSONObject2 = new JSONObject();
        jSONObject2.put("cachedName", "Minecraft");
        jSONObject2.put("uid", "net.minecraft");
        jSONObject2.put("version", str);
        jSONObject2.put("important", true);
        jSONArray.put(jSONObject2);
        String strResolveLoaderVersion = resolveLoaderVersion(file, str2, str3);
        String strBuildMultiMcLoaderUid = buildMultiMcLoaderUid(str2);
        if (!isBlank(strBuildMultiMcLoaderUid) && isBlank(strResolveLoaderVersion)) {
            throw new IllegalStateException("Unable to resolve " + str2 + " loader version for MultiMC/Prism export. Reinstall the pack or repair its JavaLauncher metadata first.");
        }
        if (!isBlank(strBuildMultiMcLoaderUid) && !isBlank(strResolveLoaderVersion)) {
            JSONObject jSONObject3 = new JSONObject();
            jSONObject3.put("uid", strBuildMultiMcLoaderUid);
            jSONObject3.put("version", strResolveLoaderVersion);
            jSONObject3.put("important", true);
            jSONArray.put(jSONObject3);
        }
        jSONObject.put("components", jSONArray);
        return jSONObject;
    }

    private static String buildMultiMcLoaderUid(String str) {
        return isBlank(str) ? "" : "Fabric".equalsIgnoreCase(str) ? "net.fabricmc.fabric-loader" : "Forge".equalsIgnoreCase(str) ? "net.minecraftforge" : "NeoForge".equalsIgnoreCase(str) ? "net.neoforged" : "Quilt".equalsIgnoreCase(str) ? "org.quiltmc.quilt-loader" : "";
    }

    private static String sanitizeMultiMcCfgValue(String str) {
        return str.replace('\n', ' ').replace('\r', ' ').trim();
    }

    private static ArrayList<File> collectMultiMcExportRoots(File file) {
        ArrayList<File> arrayList = new ArrayList<>();
        String[] strArr = {"mods", "resourcepacks", "shaderpacks", "config", "defaultconfigs", "kubejs", "scripts"};
        for (int i = 0; i < 7; i++) {
            File file2 = new File(file, strArr[i]);
            if (file2.exists()) {
                arrayList.add(file2);
            }
        }
        return arrayList;
    }

    private static String buildMultiMcReadme(String str) {
        return "JavaLauncher MultiMC/Prism export\n\nInstance: " + str + "\n\nThis ZIP contains instance.cfg, mmc-pack.json, and .minecraft content. It is intended for MultiMC/Prism-style import from zip and private sharing. Before public sharing, verify redistribution permissions for every included mod, resource pack, shader pack, and config file.\n";
    }

    private static JSONObject buildCurseForgeMinecraftBlock(File file, String str, String str2, String str3) throws Exception {
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("version", str);
        JSONArray jSONArray = new JSONArray();
        String strResolveLoaderVersion = resolveLoaderVersion(file, str2, str3);
        if (!"Vanilla".equalsIgnoreCase(str2) && isBlank(strResolveLoaderVersion)) {
            throw new IllegalStateException("Unable to resolve " + str2 + " loader version for CurseForge export. Reinstall the pack or repair its JavaLauncher metadata first.");
        }
        String strBuildCurseForgeLoaderId = buildCurseForgeLoaderId(str2, strResolveLoaderVersion);
        if (!isBlank(strBuildCurseForgeLoaderId)) {
            JSONObject jSONObject2 = new JSONObject();
            jSONObject2.put("id", strBuildCurseForgeLoaderId);
            jSONObject2.put("primary", true);
            jSONArray.put(jSONObject2);
        }
        jSONObject.put("modLoaders", jSONArray);
        return jSONObject;
    }

    private static String buildCurseForgeLoaderId(String str, String str2) {
        return isBlank(str2) ? "" : "Fabric".equalsIgnoreCase(str) ? "fabric-" + str2 : "Forge".equalsIgnoreCase(str) ? "forge-" + str2 : "NeoForge".equalsIgnoreCase(str) ? "neoforge-" + str2 : "Quilt".equalsIgnoreCase(str) ? "quilt-" + str2 : "";
    }

    private static String resolveLoaderVersion(File file, String str, String str2) {
        String strSubstring;
        int i;
        int i2;
        if (isBlank(str) || "Vanilla".equalsIgnoreCase(str)) {
            return "";
        }
        String strNormalizeResolvedLoaderVersion = normalizeResolvedLoaderVersion(str, readStoredLoaderVersion(file, str));
        if (looksLikeLoaderVersion(str, strNormalizeResolvedLoaderVersion)) {
            return strNormalizeResolvedLoaderVersion;
        }
        if (isBlank(str2)) {
            return "";
        }
        String strTrim = str2.trim();
        String lowerCase = strTrim.toLowerCase(Locale.US);
        if (lowerCase.contains("fabric-loader-")) {
            strSubstring = strTrim.substring(lowerCase.indexOf("fabric-loader-") + "fabric-loader-".length());
            int iIndexOf = strSubstring.indexOf(45);
            if (iIndexOf >= 0) {
                strSubstring = strSubstring.substring(0, iIndexOf);
            }
        } else if (lowerCase.startsWith("forge-")) {
            strSubstring = strTrim.substring("forge-".length());
            int iIndexOf2 = strSubstring.indexOf(45);
            if (iIndexOf2 >= 0 && (i2 = iIndexOf2 + 1) < strSubstring.length()) {
                strSubstring = strSubstring.substring(i2);
            }
        } else if ("NeoForge".equalsIgnoreCase(str)) {
            strSubstring = resolveNeoForgeLoaderVersionFromVersionId(strTrim);
        } else {
            int iLastIndexOf = strTrim.lastIndexOf(45);
            strSubstring = (iLastIndexOf < 0 || (i = iLastIndexOf + 1) >= strTrim.length()) ? "" : strTrim.substring(i);
        }
        String strNormalizeResolvedLoaderVersion2 = normalizeResolvedLoaderVersion(str, strSubstring);
        return looksLikeLoaderVersion(str, strNormalizeResolvedLoaderVersion2) ? strNormalizeResolvedLoaderVersion2 : "";
    }

    private static String normalizeResolvedLoaderVersion(String str, String str2) {
        if (isBlank(str2)) {
            return "";
        }
        String strTrim = str2.trim();
        if ("NeoForge".equalsIgnoreCase(str)) {
            strTrim = stripNeoForgePrefix(strTrim);
        }
        return strTrim.trim();
    }

    private static boolean looksLikeLoaderVersion(String str, String str2) {
        if (isBlank(str2)) {
            return false;
        }
        String strTrim = str2.trim();
        if (strTrim.equalsIgnoreCase("recommended") || strTrim.equalsIgnoreCase("latest")) {
            return true;
        }
        if ("NeoForge".equalsIgnoreCase(str)) {
            return strTrim.matches(".*\\d+\\.\\d+.*");
        }
        return strTrim.matches(".*\\d+\\.\\d+.*") || strTrim.matches(".*\\d+.*");
    }

    private static String readStoredLoaderVersion(File file, String str) {
        String storedLoaderVersionFromFile = readStoredLoaderVersionFromFile(new File(new File(file, JAVALAUNCHER_METADATA_DIRECTORY), MODPACK_MANIFEST_FILE), str);
        return !isBlank(storedLoaderVersionFromFile) ? storedLoaderVersionFromFile : readStoredLoaderVersionFromFile(new File(new File(file, JAVALAUNCHER_METADATA_DIRECTORY), MODPACK_FILES_MANIFEST_FILE), str);
    }

    private static String readStoredLoaderVersionFromFile(File file, String str) {
        if (!file.isFile()) {
            return "";
        }
        try {
            JSONObject jSONObject = new JSONObject(readTextFile(file));
            String strTrim = jSONObject.optString("loader", "").trim();
            return (isBlank(strTrim) || isBlank(str) || strTrim.equalsIgnoreCase(str) || strTrim.toLowerCase(Locale.US).replace("-loader", "").equals(str.toLowerCase(Locale.US).replace("-loader", ""))) ? jSONObject.optString("loaderVersion", "").trim() : "";
        } catch (Throwable th) {
            Logging.i(TAG, "Unable to read stored modpack loader metadata from " + file.getAbsolutePath() + ": " + th.getMessage());
            return "";
        }
    }

    private static String readTextFile(File file) throws Exception {
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try {
                copyStream(fileInputStream, byteArrayOutputStream);
                String string = byteArrayOutputStream.toString("UTF-8");
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

    private static String stripNeoForgePrefix(String str) {
        String strTrim = str.trim();
        return strTrim.toLowerCase(Locale.US).startsWith("neoforge-") ? strTrim.substring("neoforge-".length()).trim() : strTrim;
    }

    private static String resolveNeoForgeLoaderVersionFromVersionId(String str) {
        String strTrim = str.trim();
        int iIndexOf = strTrim.toLowerCase(Locale.US).indexOf("neoforge-");
        if (iIndexOf >= 0) {
            strTrim = strTrim.substring(iIndexOf + "neoforge-".length());
        }
        String strTrim2 = strTrim.trim();
        while (strTrim2.startsWith("-")) {
            strTrim2 = strTrim2.substring(1).trim();
        }
        Matcher matcher = Pattern.compile("^1\\.\\d+(?:\\.\\d+)?-(.+)$").matcher(strTrim2);
        return matcher.matches() ? matcher.group(1).trim() : strTrim2;
    }

    private static void addWarnings(ZipOutputStream zipOutputStream, ArrayList<String> arrayList, Platform platform) throws Exception {
        if (arrayList.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder("JavaLauncher modpack export warnings\n\n");
        if (platform == Platform.MODRINTH) {
            sb.append("Modrinth publishing note: .mrpack uploads should reference allowed remote downloads where possible. Files in overrides may be rejected if they are redistributed mods/resource packs without permission.\n\n");
        } else if (platform == Platform.CURSEFORGE) {
            sb.append("CurseForge publishing note: CurseForge project uploads normally expect CurseForge-hosted files in manifest.json. Files in overrides/mods may be rejected unless they are approved third-party resources.\n\n");
        } else {
            sb.append("MultiMC/Prism sharing note: this export bundles .minecraft files directly. Make sure every included resource is allowed to be redistributed before sharing.\n\n");
        }
        Iterator<String> it = arrayList.iterator();
        while (it.hasNext()) {
            sb.append("- ").append(it.next()).append('\n');
        }
        addTextEntry(zipOutputStream, "EXPORT_WARNINGS.txt", sb.toString());
    }

    private static String buildModListHtml(ArrayList<FileRecord> arrayList) {
        StringBuilder sb = new StringBuilder("<ul>\n");
        Iterator<FileRecord> it = arrayList.iterator();
        while (it.hasNext()) {
            sb.append("  <li>").append(escapeHtml(it.next().file.getName())).append("</li>\n");
        }
        sb.append("</ul>\n");
        return sb.toString();
    }

    private static String escapeHtml(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static void addFileOrDirectoryToZip(ZipOutputStream zipOutputStream, File file, String str) throws Exception {
        if (file.isDirectory()) {
            File[] fileArrListFiles = file.listFiles();
            if (fileArrListFiles == null) {
                return;
            }
            for (File file2 : fileArrListFiles) {
                addFileOrDirectoryToZip(zipOutputStream, file2, str + "/" + file2.getName());
            }
            return;
        }
        if (file.isFile()) {
            addFileToZip(zipOutputStream, file, str);
        }
    }

    private static void addFileToZip(ZipOutputStream zipOutputStream, File file, String str) throws Exception {
        zipOutputStream.putNextEntry(new ZipEntry(str.replace('\\', '/')));
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            copyStream(fileInputStream, zipOutputStream);
            fileInputStream.close();
            zipOutputStream.closeEntry();
        } catch (Throwable th) {
            try {
                fileInputStream.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    private static void addTextEntry(ZipOutputStream zipOutputStream, String str, String str2) throws Exception {
        zipOutputStream.putNextEntry(new ZipEntry(str));
        zipOutputStream.write(str2.getBytes("UTF-8"));
        zipOutputStream.closeEntry();
    }

    private static String sha1(File file) throws Exception {
        return hashFile(file, "SHA-1");
    }

    private static String sha512(File file) throws Exception {
        return hashFile(file, "SHA-512");
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

    /* JADX INFO: Access modifiers changed from: private */
    public static String optStringAny(JSONObject jSONObject, String... strArr) {
        if (jSONObject == null) {
            return "";
        }
        for (String str : strArr) {
            String strTrim = jSONObject.optString(str, "").trim();
            if (!strTrim.isEmpty()) {
                return strTrim;
            }
        }
        return "";
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int optIntAny(JSONObject jSONObject, String... strArr) {
        if (jSONObject == null) {
            return 0;
        }
        for (String str : strArr) {
            int iOptInt = jSONObject.optInt(str, 0);
            if (iOptInt > 0) {
                return iOptInt;
            }
            String strTrim = jSONObject.optString(str, "").trim();
            if (!strTrim.isEmpty()) {
                try {
                    int i = Integer.parseInt(strTrim);
                    if (i > 0) {
                        return i;
                    }
                } catch (Throwable unused) {
                    continue;
                }
            }
        }
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    private static final class FileRecord {
        final String downloadUrl;
        final File file;
        final int fileId;
        final int projectId;
        final String relativePath;
        final ModManagerSource source;

        FileRecord(File file, String str, ModManagerSource modManagerSource, String str2, int i, int i2) {
            this.file = file;
            this.relativePath = str.replace('\\', '/');
            this.source = modManagerSource;
            this.downloadUrl = str2;
            this.projectId = i;
            this.fileId = i2;
        }

        static FileRecord fromEntry(File file, String str, JSONObject jSONObject) {
            JSONArray jSONArrayOptJSONArray;
            ModManagerSource source = ModManagerSource.UNKNOWN;
            if (jSONObject != null) {
                try {
                    source = ModManagerManifest.getSource(jSONObject);
                } catch (Throwable unused) {
                }
            }
            if (source == ModManagerSource.UNKNOWN && jSONObject != null) {
                String lowerCase = ModpackExportManager.optStringAny(jSONObject, "source", "platform", "modpackPlatform").toLowerCase(Locale.US);
                if ("modrinth".equals(lowerCase)) {
                    source = ModManagerSource.MODRINTH;
                } else if ("curseforge".equals(lowerCase) || "curse_forge".equals(lowerCase)) {
                    source = ModManagerSource.CURSEFORGE;
                }
            }
            ModManagerSource modManagerSource = source;
            String strOptStringAny = ModpackExportManager.optStringAny(jSONObject, "downloadUrl", "url", "fileUrl", "primaryDownloadUrl");
            if (ModpackExportManager.isBlank(strOptStringAny) && jSONObject != null && (jSONArrayOptJSONArray = jSONObject.optJSONArray("downloads")) != null && jSONArrayOptJSONArray.length() > 0) {
                strOptStringAny = jSONArrayOptJSONArray.optString(0, "").trim();
            }
            return new FileRecord(file, str, modManagerSource, strOptStringAny, ModpackExportManager.optIntAny(jSONObject, "curseForgeProjectId", "platformProjectId", "projectId", "modId"), ModpackExportManager.optIntAny(jSONObject, "curseForgeFileId", "platformFileId", "fileId"));
        }
    }
}
