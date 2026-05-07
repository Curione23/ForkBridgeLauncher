package ca.dnamobile.javalauncher.modmanager;

import ca.dnamobile.javalauncher.modmanager.ModrinthInstallManager;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class CurseForgeInstallManager {
    private static final ModManagerSource SOURCE = ModManagerSource.CURSEFORGE;

    private CurseForgeInstallManager() {
    }

    public static void installLatestCompatible(CurseForgeApiClient curseForgeApiClient, File file, String str, String str2, ModManagerContentType modManagerContentType, ModrinthProject modrinthProject, ModrinthInstallManager.Listener listener) {
        try {
            installProject(curseForgeApiClient, file, str, str2, modManagerContentType, modrinthProject, false, new HashSet(), new HashSet(), listener);
            listener.onComplete("Installed " + modrinthProject.title + ".");
        } catch (Throwable th) {
            listener.onError(th);
        }
    }

    public static void installSpecificVersion(CurseForgeApiClient curseForgeApiClient, File file, String str, String str2, ModManagerContentType modManagerContentType, ModrinthProject modrinthProject, ModrinthVersion modrinthVersion, ModrinthInstallManager.Listener listener) {
        try {
            installVersion(curseForgeApiClient, file, str, str2, modManagerContentType, modrinthProject, modrinthVersion, false, new HashSet(), new HashSet(), listener);
            listener.onComplete("Installed " + modrinthProject.title + " " + modrinthVersion.versionNumber + ".");
        } catch (Throwable th) {
            listener.onError(th);
        }
    }

    private static void installProject(CurseForgeApiClient curseForgeApiClient, File file, String str, String str2, ModManagerContentType modManagerContentType, ModrinthProject modrinthProject, boolean z, HashSet<String> hashSet, HashSet<String> hashSet2, ModrinthInstallManager.Listener listener) throws Exception {
        if (hashSet.add(modrinthProject.projectId)) {
            if (z && isProjectAlreadyInstalled(file, modManagerContentType, modrinthProject.projectId)) {
                listener.onStatus("Dependency already installed: " + modrinthProject.title);
                return;
            }
            listener.onStatus((z ? "Installing CurseForge dependency " : "Finding CurseForge version for ") + modrinthProject.title + "...");
            ArrayList<ModrinthVersion> projectVersions = curseForgeApiClient.getProjectVersions(modrinthProject.projectId, modManagerContentType, str, str2);
            if (projectVersions.isEmpty()) {
                throw new IllegalStateException("No compatible CurseForge file found for " + modrinthProject.title + " (Minecraft " + str + ", " + safeLoader(str2) + ").");
            }
            installVersion(curseForgeApiClient, file, str, str2, modManagerContentType, modrinthProject, projectVersions.get(0), z, hashSet, hashSet2, listener);
        }
    }

    private static void installVersion(CurseForgeApiClient curseForgeApiClient, File file, String str, String str2, ModManagerContentType modManagerContentType, ModrinthProject modrinthProject, ModrinthVersion modrinthVersion, boolean z, HashSet<String> hashSet, HashSet<String> hashSet2, ModrinthInstallManager.Listener listener) throws Exception {
        if (hashSet2.add(modrinthProject.projectId + ":" + modrinthVersion.id)) {
            if (z && isProjectAlreadyInstalled(file, modManagerContentType, modrinthProject.projectId)) {
                listener.onStatus("Dependency already installed: " + modrinthProject.title);
                return;
            }
            if (modManagerContentType.supportsDependencies()) {
                for (ModrinthDependency modrinthDependency : modrinthVersion.dependencies) {
                    if (modrinthDependency.isRequired()) {
                        installDependency(curseForgeApiClient, file, str, str2, modManagerContentType, modrinthDependency, hashSet, hashSet2, listener);
                    }
                }
            }
            ModrinthFile primaryFile = modrinthVersion.getPrimaryFile();
            if (primaryFile == null || primaryFile.url.trim().isEmpty()) {
                throw new IllegalStateException("No downloadable CurseForge file found for " + modrinthProject.title + " " + modrinthVersion.versionNumber + ".");
            }
            File targetDirectory = modManagerContentType.getTargetDirectory(file);
            if (!targetDirectory.exists() && !targetDirectory.mkdirs()) {
                throw new IllegalStateException("Unable to create folder: " + targetDirectory.getAbsolutePath());
            }
            ModManagerSource modManagerSource = SOURCE;
            ModManagerManifest.removeKnownFilesForProject(file, modManagerContentType, modManagerSource, modrinthProject.projectId);
            File fileUniqueTargetFile = uniqueTargetFile(targetDirectory, sanitizeFileName(primaryFile.filename));
            listener.onStatus("Downloading " + modrinthProject.title + " " + modrinthVersion.versionNumber + " from CurseForge...");
            curseForgeApiClient.downloadToFile(primaryFile.url, fileUniqueTargetFile);
            ModManagerManifest.recordInstalled(file, modManagerContentType, modManagerSource, modrinthProject, modrinthVersion, primaryFile, fileUniqueTargetFile, z, str, str2, modrinthProject.iconUrl, cacheProjectIcon(curseForgeApiClient, file, modrinthProject));
        }
    }

    private static void installDependency(CurseForgeApiClient curseForgeApiClient, File file, String str, String str2, ModManagerContentType modManagerContentType, ModrinthDependency modrinthDependency, HashSet<String> hashSet, HashSet<String> hashSet2, ModrinthInstallManager.Listener listener) throws Exception {
        if (modrinthDependency.projectId == null || modrinthDependency.projectId.trim().isEmpty()) {
            return;
        }
        String strTrim = modrinthDependency.projectId.trim();
        if (isProjectAlreadyInstalled(file, modManagerContentType, strTrim)) {
            listener.onStatus("Dependency already installed: " + strTrim);
        } else {
            installProject(curseForgeApiClient, file, str, str2, modManagerContentType, curseForgeApiClient.getProject(strTrim), true, hashSet, hashSet2, listener);
        }
    }

    private static boolean isProjectAlreadyInstalled(File file, ModManagerContentType modManagerContentType, String str) {
        return (str == null || str.trim().isEmpty() || !ModManagerManifest.isProjectInstalled(file, modManagerContentType, SOURCE.getId(), str.trim())) ? false : true;
    }

    private static File cacheProjectIcon(CurseForgeApiClient curseForgeApiClient, File file, ModrinthProject modrinthProject) {
        if (modrinthProject.iconUrl == null || modrinthProject.iconUrl.trim().isEmpty() || modrinthProject.projectId.trim().isEmpty()) {
            return null;
        }
        try {
            File file2 = new File(new File(file, ".javalauncher"), "modmanager_icons");
            if (!file2.exists() && !file2.mkdirs()) {
                return null;
            }
            File file3 = new File(file2, "curseforge-" + sanitizeFileName(modrinthProject.projectId) + ".img");
            curseForgeApiClient.downloadToFile(modrinthProject.iconUrl, file3);
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
