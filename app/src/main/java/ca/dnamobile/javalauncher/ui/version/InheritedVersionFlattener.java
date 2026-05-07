package ca.dnamobile.javalauncher.ui.version;

import android.content.Context;
import ca.dnamobile.javalauncher.feature.log.Logging;
import ca.dnamobile.javalauncher.utils.path.PathManager;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class InheritedVersionFlattener {
    private static final String JSON_FLATTENED_FIELD = "javaLauncherFlattened";
    private static final String JSON_PARENT_FIELD = "javaLauncherFlattenedParent";
    private static final String METADATA_DIR_NAME = "JavaLauncher";
    private static final String PARENT_MARKER_FILE_NAME = "flattened_parent.txt";
    private static final String TAG = "InheritedFlattener";

    private InheritedVersionFlattener() {
    }

    public static final class FlattenResult {
        public final boolean copiedClientJar;
        public final boolean flattened;
        public final String parentVersionId;

        private FlattenResult(boolean z, String str, boolean z2) {
            this.flattened = z;
            this.parentVersionId = str;
            this.copiedClientJar = z2;
        }
    }

    public static final class ParentDeleteResult {
        public final boolean deleted;
        public final String message;
        public final String parentVersionId;

        private ParentDeleteResult(boolean z, String str, String str2) {
            this.deleted = z;
            this.parentVersionId = str;
            this.message = str2;
        }
    }

    public static FlattenResult flattenInstalledVersionProfile(Context context, String str) throws Exception {
        ensureActivePathManager(context);
        File versionsRoot = getVersionsRoot();
        File canonicalFile = new File(versionsRoot, str).getCanonicalFile();
        File file = new File(canonicalFile, str + ".json");
        if (!file.isFile()) {
            throw new IllegalStateException("Child version JSON not found: " + file.getAbsolutePath());
        }
        JSONObject jSONObject = new JSONObject(readString(file));
        String strTrim = jSONObject.optString("inheritsFrom", "").trim();
        boolean z = false;
        if (strTrim.isEmpty()) {
            return new FlattenResult(z, readFlattenedParentId(context, str), z);
        }
        if (str.equals(strTrim)) {
            throw new IllegalStateException("Refusing to flatten a version that inherits from itself: " + str);
        }
        File canonicalFile2 = new File(versionsRoot, strTrim).getCanonicalFile();
        File file2 = new File(canonicalFile2, strTrim + ".json");
        File file3 = new File(canonicalFile2, strTrim + ".jar");
        assertChildOf(versionsRoot.getCanonicalFile(), canonicalFile, "child version");
        assertChildOf(versionsRoot.getCanonicalFile(), canonicalFile2, "parent version");
        if (!file2.isFile()) {
            throw new IllegalStateException("Parent version JSON not found: " + file2.getAbsolutePath());
        }
        if (!file3.isFile()) {
            throw new IllegalStateException("Parent client jar not found: " + file3.getAbsolutePath());
        }
        JSONObject jSONObjectMergeVersionJson = mergeVersionJson(new JSONObject(readString(file2)), jSONObject, str, strTrim);
        File file4 = new File(canonicalFile, str + ".jar");
        boolean z2 = true;
        if (!file4.isFile()) {
            copyFile(file3, file4);
            z = true;
        }
        writeString(file, jSONObjectMergeVersionJson.toString(2));
        writeParentMarker(canonicalFile, strTrim);
        Logging.i(TAG, "Flattened " + str + " inherited from " + strTrim + ", copiedClientJar=" + z);
        return new FlattenResult(z2, strTrim, z);
    }

    /* JADX WARN: Multi-variable type inference failed */
    public static ParentDeleteResult deleteFlattenedParentVersionIfSafe(Context context, String str) throws Exception {
        ensureActivePathManager(context);
        String flattenedParentId = readFlattenedParentId(context, str);
        boolean z = false;
        Object[] objArr = null;
        Object[] objArr2 = null;
        Object[] objArr3 = null;
        Object[] objArr4 = null;
        Object[] objArr5 = null;
        Object[] objArr6 = null;
        Object[] objArr7 = null;
        Object[] objArr8 = null;
        if (flattenedParentId == null || flattenedParentId.trim().isEmpty()) {
            return new ParentDeleteResult(z, (objArr2 != null) ? 1 : 0, "No flattened parent marker found.");
        }
        String strTrim = flattenedParentId.trim();
        File canonicalFile = getVersionsRoot().getCanonicalFile();
        File canonicalFile2 = new File(canonicalFile, str).getCanonicalFile();
        File file = new File(canonicalFile2, str + ".json");
        if (!file.isFile()) {
            return new ParentDeleteResult(z, strTrim, "Flattened child JSON is missing.");
        }
        if (!new JSONObject(readString(file)).optString("inheritsFrom", "").trim().isEmpty()) {
            return new ParentDeleteResult(z, strTrim, "Child still inherits from parent.");
        }
        File canonicalFile3 = new File(canonicalFile, strTrim).getCanonicalFile();
        if (str.equals(strTrim) || canonicalFile2.equals(canonicalFile3)) {
            return new ParentDeleteResult(z, strTrim, "Refusing to delete the active child version.");
        }
        if (!canonicalFile3.isDirectory()) {
            deleteParentMarker(canonicalFile2);
            return new ParentDeleteResult(z, strTrim, "Parent version is already gone.");
        }
        assertChildOf(canonicalFile, canonicalFile3, "parent version");
        File file2 = new File(canonicalFile3, strTrim + ".json");
        if (file2.isFile() && !new JSONObject(readString(file2)).optString("inheritsFrom", "").trim().isEmpty()) {
            return new ParentDeleteResult(z, strTrim, "Parent is not a standalone vanilla profile.");
        }
        HashSet<String> hashSetFindDirectInheritors = findDirectInheritors(strTrim, str);
        if (!hashSetFindDirectInheritors.isEmpty()) {
            return new ParentDeleteResult(z, strTrim, "Parent kept because other installed versions still inherit from it: " + hashSetFindDirectInheritors);
        }
        deleteDirectory(canonicalFile3);
        deleteParentMarker(canonicalFile2);
        Logging.i(TAG, "Deleted flattened parent version " + strTrim + " after flattening " + str);
        return new ParentDeleteResult(true, strTrim, "Deleted flattened parent " + strTrim + ".");
    }

    public static String readFlattenedParentId(Context context, String str) {
        try {
            ensureActivePathManager(context);
            File file = new File(getVersionsRoot(), str);
            File file2 = new File(new File(file, METADATA_DIR_NAME), PARENT_MARKER_FILE_NAME);
            if (file2.isFile()) {
                String strTrim = readString(file2).trim();
                if (!strTrim.isEmpty()) {
                    return strTrim;
                }
            }
            File file3 = new File(file, str + ".json");
            if (!file3.isFile()) {
                return null;
            }
            String strTrim2 = new JSONObject(readString(file3)).optString(JSON_PARENT_FIELD, "").trim();
            if (strTrim2.isEmpty()) {
                return null;
            }
            return strTrim2;
        } catch (Throwable th) {
            Logging.i(TAG, "Unable to read flattened parent marker for " + str + ": " + th.getMessage());
            return null;
        }
    }

    private static void ensureActivePathManager(Context context) {
        if (PathManager.DIR_MINECRAFT_HOME == null || PathManager.DIR_MINECRAFT_HOME.trim().isEmpty()) {
            PathManager.initContextConstants(context);
        }
    }

    private static JSONObject mergeVersionJson(JSONObject jSONObject, JSONObject jSONObject2, String str, String str2) throws Exception {
        JSONObject jSONObject3 = new JSONObject(jSONObject.toString());
        JSONArray jSONArrayNames = jSONObject2.names();
        if (jSONArrayNames != null) {
            for (int i = 0; i < jSONArrayNames.length(); i++) {
                String string = jSONArrayNames.getString(i);
                if (!"libraries".equals(string) && !"arguments".equals(string) && !"inheritsFrom".equals(string)) {
                    jSONObject3.put(string, jSONObject2.get(string));
                }
            }
        }
        jSONObject3.put("id", str);
        jSONObject3.remove("inheritsFrom");
        jSONObject3.put(JSON_FLATTENED_FIELD, true);
        jSONObject3.put(JSON_PARENT_FIELD, str2);
        jSONObject3.put("libraries", mergeLibraries(jSONObject.optJSONArray("libraries"), jSONObject2.optJSONArray("libraries")));
        jSONObject3.put("arguments", mergeArguments(jSONObject.optJSONObject("arguments"), jSONObject2.optJSONObject("arguments")));
        if (jSONObject2.has("minecraftArguments")) {
            jSONObject3.put("minecraftArguments", jSONObject2.optString("minecraftArguments", ""));
        } else if (jSONObject.has("minecraftArguments")) {
            jSONObject3.put("minecraftArguments", jSONObject.optString("minecraftArguments", ""));
        }
        return jSONObject3;
    }

    private static JSONArray mergeLibraries(JSONArray jSONArray, JSONArray jSONArray2) throws Exception {
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        if (jSONArray != null) {
            for (int i = 0; i < jSONArray.length(); i++) {
                JSONObject jSONObjectOptJSONObject = jSONArray.optJSONObject(i);
                if (jSONObjectOptJSONObject != null) {
                    linkedHashMap.put(libraryMergeKey(jSONObjectOptJSONObject), new JSONObject(jSONObjectOptJSONObject.toString()));
                }
            }
        }
        if (jSONArray2 != null) {
            for (int i2 = 0; i2 < jSONArray2.length(); i2++) {
                JSONObject jSONObjectOptJSONObject2 = jSONArray2.optJSONObject(i2);
                if (jSONObjectOptJSONObject2 != null) {
                    linkedHashMap.put(libraryMergeKey(jSONObjectOptJSONObject2), new JSONObject(jSONObjectOptJSONObject2.toString()));
                }
            }
        }
        JSONArray jSONArray3 = new JSONArray();
        Iterator it = linkedHashMap.entrySet().iterator();
        while (it.hasNext()) {
            jSONArray3.put(((Map.Entry) it.next()).getValue());
        }
        return jSONArray3;
    }

    private static String libraryMergeKey(JSONObject jSONObject) {
        String strOptString = jSONObject.optString("name", "");
        if (strOptString.isEmpty()) {
            return "missing-name-" + jSONObject.toString().hashCode();
        }
        String[] strArrSplit = strOptString.split(":");
        if (strArrSplit.length < 3) {
            return strOptString;
        }
        return strArrSplit[0] + ":" + strArrSplit[1] + ":" + (strArrSplit.length > 3 ? strArrSplit[3] : "");
    }

    private static JSONObject mergeArguments(JSONObject jSONObject, JSONObject jSONObject2) throws Exception {
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

    private static JSONArray mergeArrays(JSONArray jSONArray, JSONArray jSONArray2) throws Exception {
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

    private static HashSet<String> findDirectInheritors(String str, String str2) {
        HashSet<String> hashSet = new HashSet<>();
        File[] fileArrListFiles = getVersionsRoot().listFiles();
        if (fileArrListFiles == null) {
            return hashSet;
        }
        for (File file : fileArrListFiles) {
            if (file.isDirectory()) {
                String name = file.getName();
                if (!str.equals(name) && !str2.equals(name)) {
                    File file2 = new File(file, name + ".json");
                    if (file2.isFile()) {
                        try {
                            JSONObject jSONObject = new JSONObject(readString(file2));
                            if (str.equals(jSONObject.optString("inheritsFrom", "").trim())) {
                                hashSet.add(jSONObject.optString("id", name));
                            }
                        } catch (Throwable th) {
                            Logging.i(TAG, "Unable to inspect version inherit link for " + name + ": " + th.getMessage());
                        }
                    }
                }
            }
        }
        return hashSet;
    }

    private static void writeParentMarker(File file, String str) throws Exception {
        File file2 = new File(file, METADATA_DIR_NAME);
        ensureDirectory(file2);
        writeString(new File(file2, PARENT_MARKER_FILE_NAME), str + "\n");
    }

    private static void deleteParentMarker(File file) {
        try {
            File file2 = new File(new File(file, METADATA_DIR_NAME), PARENT_MARKER_FILE_NAME);
            if (!file2.exists() || file2.delete()) {
                return;
            }
            Logging.i(TAG, "Unable to delete parent marker: " + file2.getAbsolutePath());
        } catch (Throwable unused) {
        }
    }

    private static File getVersionsRoot() {
        return new File(PathManager.DIR_MINECRAFT_HOME, "versions");
    }

    private static void ensureDirectory(File file) throws Exception {
        if (file.exists()) {
            if (!file.isDirectory()) {
                throw new IllegalStateException("Path exists but is not a directory: " + file.getAbsolutePath());
            }
        } else if (!file.mkdirs() && !file.isDirectory()) {
            throw new IllegalStateException("Unable to create directory: " + file.getAbsolutePath());
        }
    }

    private static void assertChildOf(File file, File file2, String str) throws Exception {
        File canonicalFile = file.getCanonicalFile();
        File canonicalFile2 = file2.getCanonicalFile();
        if (!canonicalFile2.getAbsolutePath().startsWith(canonicalFile.getAbsolutePath() + File.separator)) {
            throw new IllegalStateException(String.format(Locale.ROOT, "Refusing unsafe %s path outside versions root: %s", str, canonicalFile2.getAbsolutePath()));
        }
    }

    private static void deleteDirectory(File file) throws Exception {
        File[] fileArrListFiles = file.listFiles();
        if (fileArrListFiles != null) {
            for (File file2 : fileArrListFiles) {
                if (file2.isDirectory()) {
                    deleteDirectory(file2);
                } else if (file2.exists() && !file2.delete()) {
                    throw new IllegalStateException("Unable to delete file: " + file2.getAbsolutePath());
                }
            }
        }
        if (file.exists() && !file.delete()) {
            throw new IllegalStateException("Unable to delete directory: " + file.getAbsolutePath());
        }
    }

    private static void copyFile(File file, File file2) throws Exception {
        File parentFile = file2.getParentFile();
        if (parentFile != null) {
            ensureDirectory(parentFile);
        }
        File file3 = new File(file2.getAbsolutePath() + ".part");
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file3);
            try {
                byte[] bArr = new byte[65536];
                while (true) {
                    int i = fileInputStream.read(bArr);
                    if (i == -1) {
                        break;
                    } else {
                        fileOutputStream.write(bArr, 0, i);
                    }
                }
                fileOutputStream.close();
                fileInputStream.close();
                if (file2.exists() && !file2.delete()) {
                    throw new IllegalStateException("Unable to replace file: " + file2.getAbsolutePath());
                }
                if (file3.renameTo(file2)) {
                    return;
                }
                fileInputStream = new FileInputStream(file3);
                try {
                    fileOutputStream = new FileOutputStream(file2);
                    try {
                        byte[] bArr2 = new byte[65536];
                        while (true) {
                            int i2 = fileInputStream.read(bArr2);
                            if (i2 != -1) {
                                fileOutputStream.write(bArr2, 0, i2);
                            } else {
                                fileOutputStream.close();
                                fileInputStream.close();
                                file3.delete();
                                return;
                            }
                        }
                    } finally {
                    }
                } finally {
                }
            } finally {
                try {
                    fileOutputStream.close();
                } catch (Throwable th) {
                    th.addSuppressed(th);
                }
            }
        } finally {
        }
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
}
