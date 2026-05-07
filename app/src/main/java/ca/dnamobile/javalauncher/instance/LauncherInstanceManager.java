package ca.dnamobile.javalauncher.instance;

import android.content.Context;
import android.net.Uri;
import ca.dnamobile.javalauncher.BuildConfig;
import ca.dnamobile.javalauncher.feature.log.Logging;
import ca.dnamobile.javalauncher.modmanager.ModManagerVersionResolver;
import ca.dnamobile.javalauncher.storage.StorageLocationStore;
import ca.dnamobile.javalauncher.utils.path.PathManager;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.json.JSONObject;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class LauncherInstanceManager {
    private static final String METADATA_FILE = "instance.json";
    private static final String TAG = "InstanceManager";

    private LauncherInstanceManager() {
    }

    public static File getInstancesRoot() {
        return getInstancesRoot(new File(PathManager.DIR_MINECRAFT_HOME));
    }

    public static File getInstancesRoot(File file) {
        return new File(file, "instances");
    }

    public static LauncherInstance createInstance(Context context, String str, String str2, String str3, String str4, Uri uri) throws Exception {
        return createInstance(context, str, str2, str3, str3, str4, uri);
    }

    public static LauncherInstance createInstance(Context context, String str, String str2, String str3, String str4, String str5, Uri uri) throws Exception {
        String strTrim;
        File file;
        PathManager.initContextConstants(context);
        if (str4.trim().isEmpty()) {
            strTrim = ModManagerVersionResolver.resolveGameVersionForContent(str3);
        } else {
            strTrim = str4.trim();
        }
        String str6 = strTrim.isEmpty() ? str3 : strTrim;
        String strCleanDisplayName = cleanDisplayName(str, str2, str3);
        String strUniqueIdForName = uniqueIdForName(strCleanDisplayName);
        File fileCreateUniqueInstanceRoot = createUniqueInstanceRoot(strUniqueIdForName);
        File file2 = new File(fileCreateUniqueInstanceRoot, "game");
        ensureDirectory(file2);
        ensureDirectory(new File(file2, "saves"));
        ensureDirectory(new File(file2, "resourcepacks"));
        ensureDirectory(new File(file2, "shaderpacks"));
        ensureDirectory(new File(file2, "mods"));
        ensureDirectory(new File(file2, "config"));
        ensureDirectory(new File(file2, "logs"));
        ensureDirectory(new File(fileCreateUniqueInstanceRoot, "metadata"));
        DefaultMinecraftOptionsInstaller.installIfMissingForNewInstance(context, file2, str6);
        if (uri != null) {
            File file3 = new File(fileCreateUniqueInstanceRoot, "icon");
            copyUri(context, uri, file3);
            file = file3;
        } else {
            file = null;
        }
        String str7 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US).format(new Date());
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("schema", 1);
        jSONObject.put("id", strUniqueIdForName);
        jSONObject.put("name", strCleanDisplayName);
        jSONObject.put("loader", str2);
        jSONObject.put("baseVersionId", str3);
        jSONObject.put("minecraftVersionId", str6);
        jSONObject.put("versionType", str5);
        jSONObject.put("rootDirectory", fileCreateUniqueInstanceRoot.getAbsolutePath());
        jSONObject.put("gameDirectory", file2.getAbsolutePath());
        jSONObject.put("iconFile", file != null ? file.getAbsolutePath() : "");
        jSONObject.put("createdAt", str7);
        jSONObject.put("storageMode", "storage_location");
        jSONObject.put("launcherHome", PathManager.DIR_GAME_HOME);
        jSONObject.put("minecraftHome", PathManager.DIR_MINECRAFT_HOME);
        jSONObject.put("note", "Shared game files live under this storage location's .minecraft/versions/libraries/assets. This directory isolates saves/options/mods/resourcepacks for this launcher instance.");
        writeString(new File(fileCreateUniqueInstanceRoot, METADATA_FILE), jSONObject.toString(2));
        return new LauncherInstance(strUniqueIdForName, strCleanDisplayName, str2, str3, str6, str5, fileCreateUniqueInstanceRoot, file2, file, str7);
    }

    public static ArrayList<LauncherInstance> findInstances(Context context) {
        ArrayList<LauncherInstance> arrayList = new ArrayList<>();
        HashSet hashSet = new HashSet();
        Iterator<File> it = StorageLocationStore.getVisibleMinecraftHomes(context).iterator();
        while (it.hasNext()) {
            File[] fileArrListFiles = getInstancesRoot(it.next()).listFiles();
            if (fileArrListFiles != null) {
                for (File file : fileArrListFiles) {
                    if (file.isDirectory()) {
                        File file2 = new File(file, METADATA_FILE);
                        if (file2.isFile()) {
                            try {
                                LauncherInstance instance = readInstance(file2);
                                if (hashSet.add(instance.getId() + "@" + instance.getRootDirectory().getAbsolutePath())) {
                                    arrayList.add(instance);
                                }
                            } catch (Throwable th) {
                                Logging.i(TAG, "Skipping broken instance " + file.getAbsolutePath() + ": " + th.getMessage());
                            }
                        }
                    }
                }
            }
        }
        arrayList.sort(new Comparator() { // from class: ca.dnamobile.javalauncher.instance.LauncherInstanceManager$$ExternalSyntheticLambda1
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                return ((LauncherInstance) obj).getName().compareToIgnoreCase(((LauncherInstance) obj2).getName());
            }
        });
        return arrayList;
    }

    public static LauncherInstance findByNameOrId(Context context, String str) {
        for (LauncherInstance launcherInstance : findInstances(context)) {
            if (launcherInstance.getId().equals(str) || launcherInstance.getName().equals(str)) {
                return launcherInstance;
            }
        }
        return null;
    }

    public static LauncherInstance renameInstance(Context context, File file, String str) throws Exception {
        PathManager.initContextConstants(context);
        File fileResolveMetadataFile = resolveMetadataFile(file);
        JSONObject jSONObject = new JSONObject(readString(fileResolveMetadataFile));
        File fileRequireParent = requireParent(fileResolveMetadataFile);
        String strOptString = jSONObject.optString("id", fileRequireParent.getName());
        String strOptString2 = jSONObject.optString("loader", "Vanilla");
        String strOptString3 = jSONObject.optString("baseVersionId", "");
        String strResolveStoredMinecraftVersionId = resolveStoredMinecraftVersionId(jSONObject, strOptString3, fileRequireParent);
        String strCleanDisplayName = cleanDisplayName(str, strOptString2, strOptString3);
        if (strCleanDisplayName.trim().isEmpty()) {
            throw new IllegalArgumentException("Instance name is empty.");
        }
        for (LauncherInstance launcherInstance : findInstances(context)) {
            if (!launcherInstance.getId().equals(strOptString) && !safeCanonicalPath(launcherInstance.getRootDirectory()).equals(safeCanonicalPath(fileRequireParent)) && launcherInstance.getName().equalsIgnoreCase(strCleanDisplayName)) {
                throw new IllegalStateException("An instance named " + strCleanDisplayName + " already exists.");
            }
        }
        File fileResolveStoredGameDirectory = resolveStoredGameDirectory(jSONObject, fileRequireParent);
        File fileResolveStoredIconFile = resolveStoredIconFile(jSONObject, fileRequireParent);
        jSONObject.put("id", strOptString);
        jSONObject.put("name", strCleanDisplayName);
        jSONObject.put("minecraftVersionId", strResolveStoredMinecraftVersionId);
        jSONObject.put("rootDirectory", fileRequireParent.getAbsolutePath());
        jSONObject.put("gameDirectory", fileResolveStoredGameDirectory.getAbsolutePath());
        jSONObject.put("iconFile", fileResolveStoredIconFile != null ? fileResolveStoredIconFile.getAbsolutePath() : "");
        writeString(fileResolveMetadataFile, jSONObject.toString(2));
        return readInstance(fileResolveMetadataFile);
    }

    public static LauncherInstance updateInstanceIcon(Context context, File file, Uri uri) throws Exception {
        PathManager.initContextConstants(context);
        File fileResolveMetadataFile = resolveMetadataFile(file);
        JSONObject jSONObject = new JSONObject(readString(fileResolveMetadataFile));
        File fileRequireParent = requireParent(fileResolveMetadataFile);
        File fileResolveStoredGameDirectory = resolveStoredGameDirectory(jSONObject, fileRequireParent);
        File fileResolveStoredIconFile = resolveStoredIconFile(jSONObject, fileRequireParent);
        File file2 = new File(fileRequireParent, "icon" + resolveImageExtension(context, uri));
        copyUri(context, uri, file2);
        if (fileResolveStoredIconFile != null && !safeCanonicalPath(fileResolveStoredIconFile).equals(safeCanonicalPath(file2)) && isChildOf(fileRequireParent, fileResolveStoredIconFile) && fileResolveStoredIconFile.isFile()) {
            fileResolveStoredIconFile.delete();
        }
        jSONObject.put("rootDirectory", fileRequireParent.getAbsolutePath());
        jSONObject.put("gameDirectory", fileResolveStoredGameDirectory.getAbsolutePath());
        jSONObject.put("iconFile", file2.getAbsolutePath());
        writeString(fileResolveMetadataFile, jSONObject.toString(2));
        return readInstance(fileResolveMetadataFile);
    }

    public static ArrayList<String> findSharedVersionDependents(Context context, String str) {
        PathManager.initContextConstants(context);
        ArrayList<String> arrayList = new ArrayList<>();
        if (str.trim().isEmpty()) {
            return arrayList;
        }
        for (File file : StorageLocationStore.getVisibleMinecraftHomes(context)) {
            File[] fileArrListFiles = new File(file, "versions").listFiles();
            if (fileArrListFiles != null) {
                for (File file2 : fileArrListFiles) {
                    if (file2.isDirectory()) {
                        String name = file2.getName();
                        if (!str.equals(name)) {
                            File file3 = new File(file2, name + ".json");
                            if (file3.isFile()) {
                                try {
                                    JSONObject jSONObject = new JSONObject(readString(file3));
                                    if (str.equals(jSONObject.optString("inheritsFrom", ""))) {
                                        String strOptString = jSONObject.optString("id", name);
                                        StringBuilder sb = new StringBuilder();
                                        if (strOptString.isEmpty()) {
                                            strOptString = name;
                                        }
                                        arrayList.add(sb.append(strOptString).append(" (").append(file.getAbsolutePath()).append(")").toString());
                                    }
                                } catch (Throwable th) {
                                    Logging.i(TAG, "Unable to inspect shared version dependency for " + name + ": " + th.getMessage());
                                }
                            }
                        }
                    }
                }
            }
        }
        arrayList.sort(new LauncherInstanceManager$$ExternalSyntheticLambda0());
        return arrayList;
    }

    public static ArrayList<String> findIsolatedInstanceDependents(Context context, String str) {
        PathManager.initContextConstants(context);
        ArrayList<String> arrayList = new ArrayList<>();
        if (str.trim().isEmpty()) {
            return arrayList;
        }
        for (LauncherInstance launcherInstance : findInstances(context)) {
            String baseVersionId = launcherInstance.getBaseVersionId();
            File minecraftHomeForInstance = getMinecraftHomeForInstance(launcherInstance);
            if (str.equals(baseVersionId) || versionInheritsFrom(minecraftHomeForInstance, baseVersionId, str, new HashSet())) {
                arrayList.add(launcherInstance.getName() + " (instance)");
            }
        }
        arrayList.sort(new LauncherInstanceManager$$ExternalSyntheticLambda0());
        return arrayList;
    }

    public static boolean isSharedVersionRequiredByIsolatedInstances(Context context, String str) {
        return !findIsolatedInstanceDependents(context, str).isEmpty();
    }

    private static boolean versionInheritsFrom(File file, String str, String str2, HashSet<String> hashSet) {
        if (!str.trim().isEmpty() && hashSet.add(str)) {
            File file2 = new File(new File(new File(file, "versions"), str), str + ".json");
            if (!file2.isFile()) {
                return false;
            }
            try {
                String strOptString = new JSONObject(readString(file2)).optString("inheritsFrom", "");
                if (strOptString.trim().isEmpty()) {
                    return false;
                }
                if (str2.equals(strOptString)) {
                    return true;
                }
                return versionInheritsFrom(file, strOptString, str2, hashSet);
            } catch (Throwable th) {
                Logging.i(TAG, "Unable to inspect inherited version chain for " + str + ": " + th.getMessage());
            }
        }
        return false;
    }

    public static String formatDependentVersionList(List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (String str : list) {
            if (str != null && !str.trim().isEmpty()) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append("• ").append(str);
            }
        }
        return sb.length() > 0 ? sb.toString() : "• Unknown loader version";
    }

    private static void ensureSharedVersionIsNotRequired(Context context, String str) {
        ArrayList<String> arrayListFindSharedVersionDependents = findSharedVersionDependents(context, str);
        arrayListFindSharedVersionDependents.addAll(findIsolatedInstanceDependents(context, str));
        if (!arrayListFindSharedVersionDependents.isEmpty()) {
            throw new IllegalStateException("Cannot delete shared version " + str + " because it is required by:\n" + formatDependentVersionList(arrayListFindSharedVersionDependents) + "\nDelete those instances/loader versions first, or keep this shared version installed.");
        }
    }

    public static void deleteInstance(Context context, LauncherInstance launcherInstance) throws Exception {
        deleteInstance(context, launcherInstance.getId(), launcherInstance.getBaseVersionId(), launcherInstance.getRootDirectory(), launcherInstance.isIsolated());
    }

    public static void deleteInstance(Context context, String str, String str2, File file, boolean z) throws Exception {
        PathManager.initContextConstants(context);
        File canonicalFile = getDeleteTargetDirectory(str2, file, z).getCanonicalFile();
        if (z) {
            File canonicalFile2 = file.getParentFile() != null ? file.getParentFile().getCanonicalFile() : getInstancesRoot().getCanonicalFile();
            if (canonicalFile.equals(canonicalFile2) || !isChildOf(canonicalFile2, canonicalFile)) {
                throw new IllegalStateException("Refusing to delete unsafe instance path: " + canonicalFile.getAbsolutePath());
            }
        } else {
            File canonicalFile3 = new File(file, "versions").getCanonicalFile();
            File canonicalFile4 = canonicalFile.getParentFile() != null ? canonicalFile.getParentFile().getCanonicalFile() : null;
            if (canonicalFile4 == null || !canonicalFile3.equals(canonicalFile4)) {
                throw new IllegalStateException("Refusing to delete unsafe shared version path: " + canonicalFile.getAbsolutePath());
            }
            ensureSharedVersionIsNotRequired(context, str2);
        }
        deleteRecursively(canonicalFile);
        try {
            if (StorageLocationStore.deleteFromScopedStorageIfNeeded(context, canonicalFile)) {
                Logging.i(TAG, "Deleted scoped-storage copy for instance " + str + " at " + canonicalFile.getAbsolutePath());
            }
            Logging.i(TAG, "Deleted instance " + str + " at " + canonicalFile.getAbsolutePath());
        } catch (Throwable th) {
            Logging.e(TAG, "Unable to delete scoped-storage copy for instance " + str, th);
            throw new IllegalStateException("Local instance was deleted, but the scoped-storage copy could not be deleted: " + (th.getMessage() != null ? th.getMessage() : th.getClass().getSimpleName()), th);
        }
    }

    public static File getDeleteTargetDirectory(LauncherInstance launcherInstance) {
        return getDeleteTargetDirectory(launcherInstance.getBaseVersionId(), launcherInstance.getRootDirectory(), launcherInstance.isIsolated());
    }

    public static File getDeleteTargetDirectory(String str, File file, boolean z) {
        if (z) {
            return file;
        }
        if (str.trim().isEmpty()) {
            throw new IllegalArgumentException("Shared version id is empty.");
        }
        return new File(new File(file, "versions"), str);
    }

    private static File getMinecraftHomeForInstance(LauncherInstance launcherInstance) {
        if (!launcherInstance.isIsolated()) {
            return launcherInstance.getRootDirectory();
        }
        File parentFile = launcherInstance.getRootDirectory().getParentFile();
        File parentFile2 = parentFile != null ? parentFile.getParentFile() : null;
        return parentFile2 != null ? parentFile2 : new File(PathManager.DIR_MINECRAFT_HOME);
    }

    private static boolean isChildOf(File file, File file2) throws Exception {
        return file2.getCanonicalPath().startsWith(file.getCanonicalPath() + File.separator);
    }

    private static void deleteRecursively(File file) throws Exception {
        if (file.exists()) {
            File[] fileArrListFiles = file.listFiles();
            if (fileArrListFiles != null) {
                for (File file2 : fileArrListFiles) {
                    deleteRecursively(file2);
                }
            }
            if (!file.delete() && file.exists()) {
                throw new IllegalStateException("Unable to delete: " + file.getAbsolutePath());
            }
        }
    }

    private static LauncherInstance readInstance(File file) throws Exception {
        JSONObject jSONObject = new JSONObject(readString(file));
        File fileRequireParent = requireParent(file);
        String strOptString = jSONObject.optString("id", fileRequireParent.getName());
        String strOptString2 = jSONObject.optString("name", strOptString);
        String strOptString3 = jSONObject.optString("loader", "Vanilla");
        String strOptString4 = jSONObject.optString("baseVersionId", "");
        String strResolveStoredMinecraftVersionId = resolveStoredMinecraftVersionId(jSONObject, strOptString4, fileRequireParent);
        if (jSONObject.optString("minecraftVersionId", "").trim().isEmpty() && !strResolveStoredMinecraftVersionId.trim().isEmpty()) {
            jSONObject.put("minecraftVersionId", strResolveStoredMinecraftVersionId);
            writeString(file, jSONObject.toString(2));
        }
        String strOptString5 = jSONObject.optString("versionType", BuildConfig.BUILD_TYPE);
        File fileResolveStoredRoot = resolveStoredRoot(jSONObject, fileRequireParent);
        return new LauncherInstance(strOptString, strOptString2, strOptString3, strOptString4, strResolveStoredMinecraftVersionId, strOptString5, fileResolveStoredRoot, resolveStoredGameDirectory(jSONObject, fileResolveStoredRoot), resolveStoredIconFile(jSONObject, fileResolveStoredRoot), jSONObject.optString("createdAt", ""));
    }

    private static String resolveStoredMinecraftVersionId(JSONObject jSONObject, String str, File file) {
        String strTrim = jSONObject.optString("minecraftVersionId", "").trim();
        if (!strTrim.isEmpty()) {
            return strTrim;
        }
        String minecraftVersionFromLaunchProfile = readMinecraftVersionFromLaunchProfile(str, file);
        if (!minecraftVersionFromLaunchProfile.isEmpty()) {
            return minecraftVersionFromLaunchProfile;
        }
        String strResolveGameVersionForContent = ModManagerVersionResolver.resolveGameVersionForContent(str);
        return strResolveGameVersionForContent.trim().isEmpty() ? str : strResolveGameVersionForContent;
    }

    private static String readMinecraftVersionFromLaunchProfile(String str, File file) {
        if (str.trim().isEmpty()) {
            return "";
        }
        File file2 = new File(new File(new File(inferMinecraftHome(file), "versions"), str), str + ".json");
        if (!file2.isFile()) {
            return "";
        }
        try {
            JSONObject jSONObject = new JSONObject(readString(file2));
            String strTrim = jSONObject.optString("minecraftVersionId", "").trim();
            if (!strTrim.isEmpty()) {
                return strTrim;
            }
            String strTrim2 = jSONObject.optString("inheritsFrom", "").trim();
            if (!strTrim2.isEmpty()) {
                return strTrim2;
            }
            String strTrim3 = jSONObject.optString("jar", "").trim();
            if (!strTrim3.isEmpty() && !strTrim3.equals(str)) {
                return ModManagerVersionResolver.resolveGameVersionForContent(strTrim3);
            }
        } catch (Throwable th) {
            Logging.i(TAG, "Unable to resolve Minecraft version for " + str + ": " + th.getMessage());
        }
        return "";
    }

    private static File inferMinecraftHome(File file) {
        File parentFile = file.getParentFile();
        File parentFile2 = (parentFile == null || !"instances".equalsIgnoreCase(parentFile.getName())) ? null : parentFile.getParentFile();
        return parentFile2 != null ? parentFile2 : new File(PathManager.DIR_MINECRAFT_HOME);
    }

    private static File resolveMetadataFile(File file) throws Exception {
        File file2 = new File(file, METADATA_FILE);
        if (file2.isFile()) {
            return file2;
        }
        File parentFile = file.getParentFile();
        if (parentFile != null) {
            File file3 = new File(parentFile, METADATA_FILE);
            if (file3.isFile()) {
                return file3;
            }
        }
        throw new IllegalStateException("Instance metadata was not found: " + file.getAbsolutePath());
    }

    private static File requireParent(File file) {
        File parentFile = file.getParentFile();
        if (parentFile != null) {
            return parentFile;
        }
        throw new IllegalStateException("Missing parent folder for: " + file.getAbsolutePath());
    }

    private static File resolveStoredRoot(JSONObject jSONObject, File file) {
        String strOptString = jSONObject.optString("rootDirectory", "");
        if (strOptString.trim().isEmpty()) {
            return file;
        }
        File file2 = new File(strOptString);
        File file3 = new File(file2, METADATA_FILE);
        if (!file3.isFile()) {
            return file;
        }
        try {
            return !file3.getCanonicalPath().equals(new File(file, METADATA_FILE).getCanonicalPath()) ? file : file2;
        } catch (Throwable unused) {
            return file;
        }
    }

    private static File resolveStoredGameDirectory(JSONObject jSONObject, File file) {
        String strOptString = jSONObject.optString("gameDirectory", "");
        if (strOptString.trim().isEmpty()) {
            return new File(file, "game");
        }
        File file2 = new File(strOptString);
        String strSafeCanonicalPath = safeCanonicalPath(file);
        String strSafeCanonicalPath2 = safeCanonicalPath(file2);
        return (strSafeCanonicalPath2.equals(strSafeCanonicalPath) || strSafeCanonicalPath2.startsWith(new StringBuilder().append(strSafeCanonicalPath).append(File.separator).toString())) ? file2 : new File(file, "game");
    }

    private static File resolveStoredIconFile(JSONObject jSONObject, File file) {
        String strOptString = jSONObject.optString("iconFile", "");
        if (strOptString.trim().isEmpty()) {
            return null;
        }
        File file2 = new File(strOptString);
        String strSafeCanonicalPath = safeCanonicalPath(file);
        String strSafeCanonicalPath2 = safeCanonicalPath(file2);
        if (strSafeCanonicalPath2.equals(strSafeCanonicalPath) || strSafeCanonicalPath2.startsWith(strSafeCanonicalPath + File.separator)) {
            return file2;
        }
        File file3 = new File(file, file2.getName());
        return file3.isFile() ? file3 : file2;
    }

    private static String resolveImageExtension(Context context, Uri uri) {
        String type;
        try {
            type = context.getContentResolver().getType(uri);
        } catch (Throwable unused) {
            type = null;
        }
        if (!"image/jpeg".equalsIgnoreCase(type) && !"image/jpg".equalsIgnoreCase(type)) {
            if ("image/webp".equalsIgnoreCase(type)) {
                return ".webp";
            }
            if ("image/gif".equalsIgnoreCase(type)) {
                return ".gif";
            }
            String lowerCase = uri.toString().toLowerCase(Locale.US);
            if (!lowerCase.endsWith(".jpg") && !lowerCase.endsWith(".jpeg")) {
                return lowerCase.endsWith(".webp") ? ".webp" : lowerCase.endsWith(".gif") ? ".gif" : ".png";
            }
        }
        return ".jpg";
    }

    private static String safeCanonicalPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (Throwable unused) {
            return file.getAbsolutePath();
        }
    }

    private static File createUniqueInstanceRoot(String str) {
        File instancesRoot = getInstancesRoot();
        ensureDirectory(instancesRoot);
        File file = new File(instancesRoot, str);
        if (!file.exists()) {
            return file;
        }
        for (int i = 2; i < 1000; i++) {
            File file2 = new File(instancesRoot, str + "-" + i);
            if (!file2.exists()) {
                return file2;
            }
        }
        return new File(instancesRoot, str + "-" + UUID.randomUUID());
    }

    private static String cleanDisplayName(String str, String str2, String str3) {
        String strTrim = str.trim();
        if (strTrim.isEmpty()) {
            strTrim = str3 + " (" + str2 + ")";
        }
        String strTrim2 = strTrim.replace('\n', ' ').replace('\r', ' ').trim();
        return strTrim2.isEmpty() ? str3 + " (" + str2 + ")" : strTrim2;
    }

    private static String uniqueIdForName(String str) {
        String strReplaceAll = str.toLowerCase(Locale.US).replaceAll("[^a-z0-9._ -]", "").replace(' ', '-').replaceAll("-+", "-").replaceAll("^-|-$", "");
        return strReplaceAll.isEmpty() ? "instance" : strReplaceAll;
    }

    private static void copyUri(Context context, Uri uri, File file) throws Exception {
        File parentFile = file.getParentFile();
        if (parentFile != null) {
            ensureDirectory(parentFile);
        }
        InputStream inputStreamOpenInputStream = context.getContentResolver().openInputStream(uri);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            try {
                if (inputStreamOpenInputStream == null) {
                    throw new IllegalStateException("Unable to open selected icon.");
                }
                byte[] bArr = new byte[16384];
                while (true) {
                    int i = inputStreamOpenInputStream.read(bArr);
                    if (i == -1) {
                        break;
                    } else {
                        fileOutputStream.write(bArr, 0, i);
                    }
                }
                fileOutputStream.close();
                if (inputStreamOpenInputStream != null) {
                    inputStreamOpenInputStream.close();
                }
            } finally {
            }
        } catch (Throwable th) {
            if (inputStreamOpenInputStream != null) {
                try {
                    inputStreamOpenInputStream.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    private static void ensureDirectory(File file) {
        if (!file.exists() && !file.mkdirs()) {
            throw new IllegalStateException("Unable to create directory: " + file.getAbsolutePath());
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
