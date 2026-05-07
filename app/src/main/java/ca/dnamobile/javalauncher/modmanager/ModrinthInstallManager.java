package ca.dnamobile.javalauncher.modmanager;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class ModrinthInstallManager {
    private static final ModManagerSource SOURCE = ModManagerSource.MODRINTH;

    public interface Listener {
        void onComplete(String str);

        void onError(Throwable th);

        void onStatus(String str);
    }

    private ModrinthInstallManager() {
    }

    public static void installLatestCompatible(File file, String str, String str2, ModManagerContentType modManagerContentType, ModrinthProject modrinthProject, Listener listener) {
        try {
            installProject(new ModrinthApiClient(), file, str, str2, modManagerContentType, modrinthProject, false, new HashSet(), new HashSet(), listener);
            listener.onComplete("Installed " + modrinthProject.title + ".");
        } catch (Throwable th) {
            listener.onError(th);
        }
    }

    public static void installSpecificVersion(File file, String str, String str2, ModManagerContentType modManagerContentType, ModrinthProject modrinthProject, ModrinthVersion modrinthVersion, Listener listener) {
        try {
            installVersion(new ModrinthApiClient(), file, str, str2, modManagerContentType, modrinthProject, modrinthVersion, false, new HashSet(), new HashSet(), listener);
            listener.onComplete("Installed " + modrinthProject.title + " " + modrinthVersion.versionNumber + ".");
        } catch (Throwable th) {
            listener.onError(th);
        }
    }

    private static void installProject(ModrinthApiClient modrinthApiClient, File file, String str, String str2, ModManagerContentType modManagerContentType, ModrinthProject modrinthProject, boolean z, HashSet<String> hashSet, HashSet<String> hashSet2, Listener listener) throws Exception {
        if (hashSet.add(modrinthProject.projectId)) {
            if (z && isProjectAlreadyInstalled(file, modManagerContentType, modrinthProject.projectId)) {
                listener.onStatus("Dependency already installed: " + modrinthProject.title);
                return;
            }
            listener.onStatus((z ? "Installing dependency " : "Finding version for ") + modrinthProject.title + "...");
            ArrayList<ModrinthVersion> projectVersionsWithFallback = modrinthApiClient.getProjectVersionsWithFallback(modrinthProject, modManagerContentType, str, str2, false);
            if (projectVersionsWithFallback.isEmpty()) {
                throw new IllegalStateException("No compatible Modrinth version found for " + modrinthProject.title + " (Minecraft " + str + ", " + safeLoader(str2) + ").");
            }
            installVersion(modrinthApiClient, file, str, str2, modManagerContentType, modrinthProject, projectVersionsWithFallback.get(0), z, hashSet, hashSet2, listener);
        }
    }

    private static void installVersion(ModrinthApiClient modrinthApiClient, File file, String str, String str2, ModManagerContentType modManagerContentType, ModrinthProject modrinthProject, ModrinthVersion modrinthVersion, boolean z, HashSet<String> hashSet, HashSet<String> hashSet2, Listener listener) throws Exception {
        if (hashSet2.add(modrinthVersion.id)) {
            if (z && isProjectAlreadyInstalled(file, modManagerContentType, modrinthProject.projectId)) {
                listener.onStatus("Dependency already installed: " + modrinthProject.title);
                return;
            }
            if (modManagerContentType.supportsDependencies()) {
                for (ModrinthDependency modrinthDependency : modrinthVersion.dependencies) {
                    if (modrinthDependency.isRequired()) {
                        installDependency(modrinthApiClient, file, str, str2, modManagerContentType, modrinthDependency, hashSet, hashSet2, listener);
                    }
                }
            }
            ModrinthFile primaryFile = modrinthVersion.getPrimaryFile();
            if (primaryFile == null || primaryFile.url.trim().isEmpty()) {
                throw new IllegalStateException("No downloadable file found for " + modrinthProject.title + " " + modrinthVersion.versionNumber + ".");
            }
            File targetDirectory = modManagerContentType.getTargetDirectory(file);
            if (!targetDirectory.exists() && !targetDirectory.mkdirs()) {
                throw new IllegalStateException("Unable to create folder: " + targetDirectory.getAbsolutePath());
            }
            ModManagerSource modManagerSource = SOURCE;
            ModManagerManifest.removeKnownFilesForProject(file, modManagerContentType, modManagerSource, modrinthProject.projectId);
            File fileUniqueTargetFile = uniqueTargetFile(targetDirectory, sanitizeFileName(primaryFile.filename));
            listener.onStatus("Downloading " + modrinthProject.title + " " + modrinthVersion.versionNumber + "...");
            modrinthApiClient.downloadToFile(primaryFile.url, fileUniqueTargetFile);
            ModManagerManifest.recordInstalled(file, modManagerContentType, modManagerSource, modrinthProject, modrinthVersion, primaryFile, fileUniqueTargetFile, z, str, str2, modrinthProject.iconUrl, cacheProjectIcon(modrinthApiClient, file, modrinthProject));
        }
    }

    private static void installDependency(ModrinthApiClient modrinthApiClient, File file, String str, String str2, ModManagerContentType modManagerContentType, ModrinthDependency modrinthDependency, HashSet<String> hashSet, HashSet<String> hashSet2, Listener listener) throws Exception {
        if (modrinthDependency.projectId != null && !modrinthDependency.projectId.trim().isEmpty()) {
            String strTrim = modrinthDependency.projectId.trim();
            if (isProjectAlreadyInstalled(file, modManagerContentType, strTrim)) {
                listener.onStatus("Dependency already installed: " + strTrim);
                return;
            } else {
                installProject(modrinthApiClient, file, str, str2, modManagerContentType, modrinthApiClient.getProject(strTrim), true, hashSet, hashSet2, listener);
                return;
            }
        }
        if (modrinthDependency.versionId == null || modrinthDependency.versionId.trim().isEmpty()) {
            return;
        }
        ModrinthVersion version = modrinthApiClient.getVersion(modrinthDependency.versionId.trim());
        installVersion(modrinthApiClient, file, str, str2, modManagerContentType, modrinthApiClient.getProject(version.projectId), version, true, hashSet, hashSet2, listener);
    }

    private static boolean isProjectAlreadyInstalled(File file, ModManagerContentType modManagerContentType, String str) {
        return (str == null || str.trim().isEmpty() || !ModManagerManifest.isProjectInstalled(file, modManagerContentType, SOURCE.getId(), str.trim())) ? false : true;
    }

    private static File cacheProjectIcon(ModrinthApiClient modrinthApiClient, File file, ModrinthProject modrinthProject) {
        if (modrinthProject.iconUrl == null || modrinthProject.iconUrl.trim().isEmpty() || modrinthProject.projectId.trim().isEmpty()) {
            return null;
        }
        try {
            File file2 = new File(new File(file, ".javalauncher"), "modmanager_icons");
            if (!file2.exists() && !file2.mkdirs()) {
                return null;
            }
            File file3 = new File(file2, sanitizeFileName(modrinthProject.projectId) + ".img");
            modrinthApiClient.downloadToFile(modrinthProject.iconUrl, file3);
            if (file3.isFile()) {
                return file3;
            }
            return null;
        } catch (Throwable unused) {
            return null;
        }
    }

    private static File uniqueTargetFile(File file, String str) {
        String strSubstring;
        File file2 = new File(file, str);
        if (!file2.exists()) {
            return file2;
        }
        int iLastIndexOf = str.lastIndexOf(46);
        if (iLastIndexOf <= 0) {
            strSubstring = "";
        } else {
            String strSubstring2 = str.substring(0, iLastIndexOf);
            strSubstring = str.substring(iLastIndexOf);
            str = strSubstring2;
        }
        for (int i = 2; i < 1000; i++) {
            File file3 = new File(file, str + "-" + i + strSubstring);
            if (!file3.exists()) {
                return file3;
            }
        }
        return new File(file, str + "-" + System.currentTimeMillis() + strSubstring);
    }

    private static String sanitizeFileName(String str) {
        String strReplaceAll = str.trim().replace('\n', ' ').replace('\r', ' ').replaceAll("[\\\\/:*?\"<>|]", "_");
        return strReplaceAll.isEmpty() ? "download.jar" : strReplaceAll;
    }

    private static String safeLoader(String str) {
        return (str == null || str.trim().isEmpty()) ? "unknown loader" : str.trim().toLowerCase(Locale.US);
    }
}
