package ca.dnamobile.javalauncher.modmanager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class ModManagerManifest {
    private static final String MANIFEST_DIR = ".javalauncher";
    private static final String MANIFEST_FILE = "modmanager_installed.json";
    private static final int SCHEMA_VERSION = 2;

    private ModManagerManifest() {
    }

    public static void removeKnownFilesForProject(File file, ModManagerContentType modManagerContentType, String str, String str2) {
        removeKnownFilesForProject(file, modManagerContentType, ModManagerSource.fromId(str), str2);
    }

    public static void removeKnownFilesForProject(File file, ModManagerContentType modManagerContentType, ModManagerSource modManagerSource, String str) {
        File manifestFile = getManifestFile(file);
        JSONArray array = readArray(manifestFile);
        JSONArray jSONArray = new JSONArray();
        for (int i = 0; i < array.length(); i++) {
            JSONObject jSONObjectOptJSONObject = array.optJSONObject(i);
            if (jSONObjectOptJSONObject != null) {
                if (str.equals(jSONObjectOptJSONObject.optString("projectId", "")) && sourceMatches(jSONObjectOptJSONObject, modManagerSource) && modManagerContentType.name().equals(jSONObjectOptJSONObject.optString("contentType", ""))) {
                    deleteIfFile(resolveEntryFile(file, modManagerContentType, jSONObjectOptJSONObject));
                    deleteIfFile(resolveDisabledEntryFile(file, modManagerContentType, jSONObjectOptJSONObject));
                    deleteIfFile(resolveEnabledEntryFile(file, modManagerContentType, jSONObjectOptJSONObject));
                } else {
                    jSONArray.put(jSONObjectOptJSONObject);
                }
            }
        }
        writeArray(manifestFile, jSONArray);
    }

    public static void recordInstalled(File file, ModManagerContentType modManagerContentType, String str, ModrinthProject modrinthProject, ModrinthVersion modrinthVersion, ModrinthFile modrinthFile, File file2, boolean z) {
        recordInstalled(file, modManagerContentType, ModManagerSource.fromId(str), modrinthProject, modrinthVersion, modrinthFile, file2, z, "", "");
    }

    public static void recordInstalled(File file, ModManagerContentType modManagerContentType, ModManagerSource modManagerSource, ModrinthProject modrinthProject, ModrinthVersion modrinthVersion, ModrinthFile modrinthFile, File file2, boolean z, String str, String str2) {
        recordInstalled(file, modManagerContentType, modManagerSource, modrinthProject, modrinthVersion, modrinthFile, file2, z, str, str2, modrinthProject.iconUrl, null);
    }

    public static void recordInstalled(File file, ModManagerContentType modManagerContentType, ModManagerSource modManagerSource, ModrinthProject modrinthProject, ModrinthVersion modrinthVersion, ModrinthFile modrinthFile, File file2, boolean z, String str, String str2, String str3, File file3) {
        String str4;
        File manifestFile = getManifestFile(file);
        JSONArray array = readArray(manifestFile);
        JSONArray jSONArray = new JSONArray();
        int i = 0;
        while (true) {
            str4 = "";
            if (i >= array.length()) {
                break;
            }
            JSONObject jSONObjectOptJSONObject = array.optJSONObject(i);
            if (jSONObjectOptJSONObject != null && (!modrinthProject.projectId.equals(jSONObjectOptJSONObject.optString("projectId", "")) || !sourceMatches(jSONObjectOptJSONObject, modManagerSource) || !modManagerContentType.name().equals(jSONObjectOptJSONObject.optString("contentType", "")))) {
                jSONArray.put(jSONObjectOptJSONObject);
            }
            i++;
        }
        String strNow = now();
        JSONObject jSONObject = new JSONObject();
        try {
            jSONObject.put("schema", 2);
            jSONObject.put("source", modManagerSource.getId());
            jSONObject.put("platform", modManagerSource.getId());
            jSONObject.put("platformName", modManagerSource.getDisplayName());
            jSONObject.put("contentType", modManagerContentType.name());
            jSONObject.put("projectType", modManagerContentType.getModrinthProjectType());
            jSONObject.put("minecraftVersion", str == null ? "" : str);
            jSONObject.put("loader", str2 == null ? "" : str2);
            jSONObject.put("projectId", modrinthProject.projectId);
            jSONObject.put("platformProjectId", modrinthProject.projectId);
            jSONObject.put("slug", modrinthProject.slug);
            jSONObject.put("title", modrinthProject.title);
            jSONObject.put("versionId", modrinthVersion.id);
            jSONObject.put("platformVersionId", modrinthVersion.id);
            jSONObject.put("versionNumber", modrinthVersion.versionNumber);
            jSONObject.put("fileName", file2.getName());
            jSONObject.put("targetPath", file2.getAbsolutePath());
            jSONObject.put("downloadUrl", modrinthFile.url);
            jSONObject.put("iconUrl", str3 == null ? "" : str3);
            jSONObject.put("cachedIconPath", (file3 == null || !file3.isFile()) ? "" : file3.getAbsolutePath());
            if (modrinthFile.sha1 != null) {
                str4 = modrinthFile.sha1;
            }
            jSONObject.put("sha1", str4);
            jSONObject.put("dependency", z);
            jSONObject.put("installedAt", strNow);
            jSONObject.put("updatedAt", strNow);
        } catch (Throwable unused) {
        }
        jSONArray.put(jSONObject);
        writeArray(manifestFile, jSONArray);
    }

    public static ArrayList<JSONObject> getInstalledEntries(File file, ModManagerContentType modManagerContentType) {
        pruneMissingFiles(file, modManagerContentType);
        JSONArray array = readArray(getManifestFile(file));
        ArrayList<JSONObject> arrayList = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject jSONObjectOptJSONObject = array.optJSONObject(i);
            if (jSONObjectOptJSONObject != null && (modManagerContentType == null || modManagerContentType.name().equals(jSONObjectOptJSONObject.optString("contentType", "")))) {
                arrayList.add(jSONObjectOptJSONObject);
            }
        }
        return arrayList;
    }

    public static int pruneMissingFiles(File file) {
        return pruneMissingFiles(file, null);
    }

    /* JADX WARN: Removed duplicated region for block: B:10:0x0022  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public static int pruneMissingFiles(java.io.File r7, ca.dnamobile.javalauncher.modmanager.ModManagerContentType r8) {
        /*
            java.io.File r0 = getManifestFile(r7)
            org.json.JSONArray r1 = readArray(r0)
            int r2 = r1.length()
            r3 = 0
            if (r2 != 0) goto L10
            return r3
        L10:
            org.json.JSONArray r2 = new org.json.JSONArray
            r2.<init>()
            r4 = r3
        L16:
            int r5 = r1.length()
            if (r3 >= r5) goto L3f
            org.json.JSONObject r5 = r1.optJSONObject(r3)
            if (r5 != 0) goto L25
        L22:
            int r4 = r4 + 1
            goto L3c
        L25:
            ca.dnamobile.javalauncher.modmanager.ModManagerContentType r6 = contentTypeFromEntry(r5)
            if (r8 == 0) goto L31
            if (r6 == r8) goto L31
            r2.put(r5)
            goto L3c
        L31:
            if (r6 == 0) goto L39
            boolean r6 = entryFileExists(r7, r6, r5)
            if (r6 == 0) goto L22
        L39:
            r2.put(r5)
        L3c:
            int r3 = r3 + 1
            goto L16
        L3f:
            if (r4 <= 0) goto L44
            writeArray(r0, r2)
        L44:
            return r4
        */
        throw new UnsupportedOperationException("Method not decompiled: ca.dnamobile.javalauncher.modmanager.ModManagerManifest.pruneMissingFiles(java.io.File, ca.dnamobile.javalauncher.modmanager.ModManagerContentType):int");
    }

    public static JSONObject getInstalledEntryForProject(File file, ModManagerContentType modManagerContentType, String str, String str2) {
        return getInstalledEntryForProject(file, modManagerContentType, ModManagerSource.fromId(str), str2);
    }

    public static JSONObject getInstalledEntryForProject(File file, ModManagerContentType modManagerContentType, ModManagerSource modManagerSource, String str) {
        if (str.trim().isEmpty()) {
            return null;
        }
        pruneMissingFiles(file, modManagerContentType);
        JSONArray array = readArray(getManifestFile(file));
        for (int i = 0; i < array.length(); i++) {
            JSONObject jSONObjectOptJSONObject = array.optJSONObject(i);
            if (jSONObjectOptJSONObject != null && str.equals(jSONObjectOptJSONObject.optString("projectId", "")) && sourceMatches(jSONObjectOptJSONObject, modManagerSource) && modManagerContentType.name().equals(jSONObjectOptJSONObject.optString("contentType", "")) && entryFileExists(file, modManagerContentType, jSONObjectOptJSONObject)) {
                return jSONObjectOptJSONObject;
            }
        }
        return null;
    }

    public static boolean isProjectInstalled(File file, ModManagerContentType modManagerContentType, String str, String str2) {
        return getInstalledEntryForProject(file, modManagerContentType, str, str2) != null;
    }

    public static JSONObject getInstalledEntryForFile(File file, ModManagerContentType modManagerContentType, File file2) {
        pruneMissingFiles(file, modManagerContentType);
        JSONArray array = readArray(getManifestFile(file));
        String strSafeCanonicalPath = safeCanonicalPath(file2);
        String name = file2.getName();
        String strStripDisabledSuffix = stripDisabledSuffix(name);
        String str = strStripDisabledSuffix + ".disabled";
        for (int i = 0; i < array.length(); i++) {
            JSONObject jSONObjectOptJSONObject = array.optJSONObject(i);
            if (jSONObjectOptJSONObject != null && modManagerContentType.name().equals(jSONObjectOptJSONObject.optString("contentType", ""))) {
                File fileResolveEntryFile = resolveEntryFile(file, modManagerContentType, jSONObjectOptJSONObject);
                if (fileResolveEntryFile != null) {
                    String strSafeCanonicalPath2 = safeCanonicalPath(fileResolveEntryFile);
                    String strSafeCanonicalPath3 = safeCanonicalPath(new File(fileResolveEntryFile.getAbsolutePath() + ".disabled"));
                    String strSafeCanonicalPath4 = safeCanonicalPath(new File(fileResolveEntryFile.getParentFile() == null ? modManagerContentType.getTargetDirectory(file) : fileResolveEntryFile.getParentFile(), stripDisabledSuffix(fileResolveEntryFile.getName())));
                    if (strSafeCanonicalPath.equals(strSafeCanonicalPath2) || strSafeCanonicalPath.equals(strSafeCanonicalPath3) || strSafeCanonicalPath.equals(strSafeCanonicalPath4)) {
                        return jSONObjectOptJSONObject;
                    }
                }
                File fileResolveDisabledEntryFile = resolveDisabledEntryFile(file, modManagerContentType, jSONObjectOptJSONObject);
                if (fileResolveDisabledEntryFile != null && strSafeCanonicalPath.equals(safeCanonicalPath(fileResolveDisabledEntryFile))) {
                    return jSONObjectOptJSONObject;
                }
                String strOptString = jSONObjectOptJSONObject.optString("fileName", "");
                if (strOptString.trim().isEmpty()) {
                    continue;
                } else {
                    String strStripDisabledSuffix2 = stripDisabledSuffix(strOptString);
                    String str2 = strStripDisabledSuffix2 + ".disabled";
                    if (name.equals(strOptString) || name.equals(strStripDisabledSuffix2) || name.equals(str2) || strStripDisabledSuffix.equals(strStripDisabledSuffix2) || str.equals(str2)) {
                        return jSONObjectOptJSONObject;
                    }
                }
            }
        }
        return null;
    }

    public static File getInstalledIconFileForFile(File file, ModManagerContentType modManagerContentType, File file2) {
        JSONObject installedEntryForFile = getInstalledEntryForFile(file, modManagerContentType, file2);
        if (installedEntryForFile == null) {
            return null;
        }
        String strOptString = installedEntryForFile.optString("cachedIconPath", "");
        if (!strOptString.trim().isEmpty()) {
            File file3 = new File(strOptString.trim());
            if (file3.isFile()) {
                return file3;
            }
        }
        return null;
    }

    public static ModManagerSource getInstalledSourceForFile(File file, ModManagerContentType modManagerContentType, File file2) {
        JSONObject installedEntryForFile = getInstalledEntryForFile(file, modManagerContentType, file2);
        return installedEntryForFile == null ? ModManagerSource.UNKNOWN : getSource(installedEntryForFile);
    }

    /* JADX WARN: Removed duplicated region for block: B:10:0x0030  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public static void removeEntryForFile(java.io.File r16, ca.dnamobile.javalauncher.modmanager.ModManagerContentType r17, java.io.File r18) {
        /*
            java.io.File r0 = getManifestFile(r16)
            org.json.JSONArray r1 = readArray(r0)
            int r2 = r1.length()
            if (r2 != 0) goto Lf
            return
        Lf:
            org.json.JSONArray r2 = new org.json.JSONArray
            r2.<init>()
            java.lang.String r9 = safeCanonicalPath(r18)
            java.lang.String r10 = r18.getName()
            java.lang.String r11 = stripDisabledSuffix(r10)
            r3 = 0
            r12 = r3
            r13 = r12
        L23:
            int r3 = r1.length()
            if (r12 >= r3) goto L5d
            org.json.JSONObject r14 = r1.optJSONObject(r12)
            r15 = 1
            if (r14 != 0) goto L32
        L30:
            r13 = r15
            goto L5a
        L32:
            java.lang.String r3 = r17.name()
            java.lang.String r4 = "contentType"
            java.lang.String r5 = ""
            java.lang.String r4 = r14.optString(r4, r5)
            boolean r3 = r3.equals(r4)
            if (r3 != 0) goto L48
            r2.put(r14)
            goto L5a
        L48:
            r3 = r16
            r4 = r17
            r5 = r14
            r6 = r9
            r7 = r10
            r8 = r11
            boolean r3 = entryMatchesFile(r3, r4, r5, r6, r7, r8)
            if (r3 == 0) goto L57
            goto L30
        L57:
            r2.put(r14)
        L5a:
            int r12 = r12 + 1
            goto L23
        L5d:
            if (r13 == 0) goto L62
            writeArray(r0, r2)
        L62:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: ca.dnamobile.javalauncher.modmanager.ModManagerManifest.removeEntryForFile(java.io.File, ca.dnamobile.javalauncher.modmanager.ModManagerContentType, java.io.File):void");
    }

    public static void updateEntryFileTarget(File file, ModManagerContentType modManagerContentType, File file2, File file3) {
        File manifestFile = getManifestFile(file);
        JSONArray array = readArray(manifestFile);
        if (array.length() == 0) {
            return;
        }
        JSONArray jSONArray = new JSONArray();
        String strSafeCanonicalPath = safeCanonicalPath(file2);
        String name = file2.getName();
        String strStripDisabledSuffix = stripDisabledSuffix(name);
        String strNow = now();
        boolean z = false;
        for (int i = 0; i < array.length(); i++) {
            JSONObject jSONObjectOptJSONObject = array.optJSONObject(i);
            if (jSONObjectOptJSONObject != null) {
                if (modManagerContentType.name().equals(jSONObjectOptJSONObject.optString("contentType", "")) && entryMatchesFile(file, modManagerContentType, jSONObjectOptJSONObject, strSafeCanonicalPath, name, strStripDisabledSuffix)) {
                    try {
                        jSONObjectOptJSONObject.put("fileName", file3.getName());
                        jSONObjectOptJSONObject.put("targetPath", file3.getAbsolutePath());
                        jSONObjectOptJSONObject.put("updatedAt", strNow);
                    } catch (Throwable unused) {
                    }
                    z = true;
                }
                jSONArray.put(jSONObjectOptJSONObject);
            }
        }
        if (z) {
            writeArray(manifestFile, jSONArray);
        }
    }

    public static ModManagerSource getSource(JSONObject jSONObject) {
        String strOptString = jSONObject.optString("platform", "");
        return !strOptString.trim().isEmpty() ? ModManagerSource.fromId(strOptString) : ModManagerSource.fromId(jSONObject.optString("source", ""));
    }

    private static ModManagerContentType contentTypeFromEntry(JSONObject jSONObject) {
        String strOptString = jSONObject.optString("contentType", "");
        if (strOptString.trim().isEmpty()) {
            strOptString = jSONObject.optString("projectType", "");
        }
        if (strOptString.trim().isEmpty()) {
            return null;
        }
        return ModManagerContentType.fromValue(strOptString);
    }

    private static boolean entryFileExists(File file, ModManagerContentType modManagerContentType, JSONObject jSONObject) {
        File fileResolveEntryFile = resolveEntryFile(file, modManagerContentType, jSONObject);
        if (fileResolveEntryFile != null && fileResolveEntryFile.exists()) {
            return true;
        }
        File fileResolveDisabledEntryFile = resolveDisabledEntryFile(file, modManagerContentType, jSONObject);
        if (fileResolveDisabledEntryFile != null && fileResolveDisabledEntryFile.exists()) {
            return true;
        }
        File fileResolveEnabledEntryFile = resolveEnabledEntryFile(file, modManagerContentType, jSONObject);
        return fileResolveEnabledEntryFile != null && fileResolveEnabledEntryFile.exists();
    }

    private static boolean entryMatchesFile(File file, ModManagerContentType modManagerContentType, JSONObject jSONObject, String str, String str2, String str3) {
        File fileResolveEntryFile = resolveEntryFile(file, modManagerContentType, jSONObject);
        if (fileResolveEntryFile != null) {
            String strSafeCanonicalPath = safeCanonicalPath(fileResolveEntryFile);
            String name = fileResolveEntryFile.getName();
            Object objStripDisabledSuffix = stripDisabledSuffix(name);
            File fileResolveDisabledEntryFile = resolveDisabledEntryFile(file, modManagerContentType, jSONObject);
            File fileResolveEnabledEntryFile = resolveEnabledEntryFile(file, modManagerContentType, jSONObject);
            if (str.equals(strSafeCanonicalPath)) {
                return true;
            }
            if (fileResolveDisabledEntryFile != null && str.equals(safeCanonicalPath(fileResolveDisabledEntryFile))) {
                return true;
            }
            if ((fileResolveEnabledEntryFile != null && str.equals(safeCanonicalPath(fileResolveEnabledEntryFile))) || str2.equals(name) || str3.equals(objStripDisabledSuffix)) {
                return true;
            }
        }
        String strOptString = jSONObject.optString("fileName", "");
        if (strOptString.trim().isEmpty()) {
            return false;
        }
        String strStripDisabledSuffix = stripDisabledSuffix(strOptString);
        return str2.equals(strOptString) || str2.equals(strStripDisabledSuffix) || str2.equals(new StringBuilder().append(strStripDisabledSuffix).append(".disabled").toString()) || str3.equals(strStripDisabledSuffix);
    }

    private static File resolveEntryFile(File file, ModManagerContentType modManagerContentType, JSONObject jSONObject) {
        String strOptString = jSONObject.optString("targetPath", "");
        if (!strOptString.trim().isEmpty()) {
            return new File(strOptString);
        }
        String strOptString2 = jSONObject.optString("fileName", "");
        if (strOptString2.trim().isEmpty()) {
            return null;
        }
        return new File(modManagerContentType.getTargetDirectory(file), strOptString2);
    }

    private static File resolveDisabledEntryFile(File file, ModManagerContentType modManagerContentType, JSONObject jSONObject) {
        File fileResolveEntryFile = resolveEntryFile(file, modManagerContentType, jSONObject);
        if (fileResolveEntryFile == null) {
            return null;
        }
        String name = fileResolveEntryFile.getName();
        if (name.toLowerCase(Locale.US).endsWith(".disabled")) {
            return fileResolveEntryFile;
        }
        return new File(fileResolveEntryFile.getParentFile() == null ? modManagerContentType.getTargetDirectory(file) : fileResolveEntryFile.getParentFile(), name + ".disabled");
    }

    private static File resolveEnabledEntryFile(File file, ModManagerContentType modManagerContentType, JSONObject jSONObject) {
        File fileResolveEntryFile = resolveEntryFile(file, modManagerContentType, jSONObject);
        if (fileResolveEntryFile == null) {
            return null;
        }
        return new File(fileResolveEntryFile.getParentFile() == null ? modManagerContentType.getTargetDirectory(file) : fileResolveEntryFile.getParentFile(), stripDisabledSuffix(fileResolveEntryFile.getName()));
    }

    private static boolean sourceMatches(JSONObject jSONObject, ModManagerSource modManagerSource) {
        return getSource(jSONObject) == modManagerSource || modManagerSource.getId().equalsIgnoreCase(jSONObject.optString("source", ""));
    }

    private static File getManifestFile(File file) {
        return new File(new File(file, MANIFEST_DIR), MANIFEST_FILE);
    }

    private static JSONArray readArray(File file) {
        if (!file.isFile()) {
            return new JSONArray();
        }
        try {
            String string = readString(file);
            if (string == null || string.trim().isEmpty()) {
                string = "[]";
            }
            return new JSONArray(string);
        } catch (Throwable unused) {
            return new JSONArray();
        }
    }

    private static void writeArray(File file, JSONArray jSONArray) {
        try {
            File parentFile = file.getParentFile();
            if (parentFile != null && !parentFile.exists()) {
                parentFile.mkdirs();
            }
            FileOutputStream fileOutputStream = new FileOutputStream(file, false);
            try {
                fileOutputStream.write(jSONArray.toString(2).getBytes(StandardCharsets.UTF_8));
                fileOutputStream.close();
            } finally {
            }
        } catch (Throwable unused) {
        }
    }

    private static void deleteIfFile(File file) {
        if (file == null || !file.isFile()) {
            return;
        }
        file.delete();
    }

    private static String stripDisabledSuffix(String str) {
        return str.toLowerCase(Locale.US).endsWith(".disabled") ? str.substring(0, str.length() - ".disabled".length()) : str;
    }

    private static String safeCanonicalPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (Throwable unused) {
            return file.getAbsolutePath();
        }
    }

    private static String now() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US).format(new Date());
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
            byte[] bArr = new byte[16384];
            while (true) {
                int i = fileInputStream.read(bArr);
                if (i == -1) {
                    String string = byteArrayOutputStream.toString(StandardCharsets.UTF_8.name());
                    byteArrayOutputStream.close();
                    fileInputStream.close();
                    return string;
                }
                byteArrayOutputStream.write(bArr, 0, i);
                fileInputStream.close();
                throw th;
            }
        } finally {
        }
    }
}
