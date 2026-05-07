package ca.dnamobile.javalauncher.modmanager;

import android.content.Context;
import ca.dnamobile.javalauncher.modmanager.ModrinthInstallManager;
import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONObject;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class ModManagerUpdateManager {

    public interface Listener {
        void onProgress(int i, int i2);

        void onStatus(String str);
    }

    private ModManagerUpdateManager() {
    }

    public static final class UpdateCandidate {
        public final ModManagerContentType contentType;
        public final String currentVersionId;
        public final String currentVersionNumber;
        public final JSONObject entry;
        public final ModrinthVersion latestVersion;
        public final ModrinthProject project;
        public final ModManagerSource source;

        UpdateCandidate(JSONObject jSONObject, ModManagerContentType modManagerContentType, ModManagerSource modManagerSource, ModrinthProject modrinthProject, ModrinthVersion modrinthVersion, String str, String str2) {
            this.entry = jSONObject;
            this.contentType = modManagerContentType;
            this.source = modManagerSource;
            this.project = modrinthProject;
            this.latestVersion = modrinthVersion;
            this.currentVersionId = str;
            this.currentVersionNumber = str2;
        }

        public String getDisplayName() {
            String strTrim = this.entry.optString("title", "").trim();
            return strTrim.isEmpty() ? this.project.title : strTrim;
        }

        public String getProjectId() {
            String strTrim = this.entry.optString("platformProjectId", "").trim();
            if (!strTrim.isEmpty()) {
                return strTrim;
            }
            String strTrim2 = this.entry.optString("projectId", "").trim();
            return strTrim2.isEmpty() ? this.project.projectId : strTrim2;
        }
    }

    public static ArrayList<UpdateCandidate> checkUpdates(Context context, File file, ModManagerContentType modManagerContentType, String str, String str2, Listener listener) throws Exception {
        ArrayList<JSONObject> installedEntries = ModManagerManifest.getInstalledEntries(file, modManagerContentType);
        ArrayList<UpdateCandidate> arrayList = new ArrayList<>();
        int size = installedEntries.size();
        for (int i = 0; i < installedEntries.size(); i++) {
            JSONObject jSONObject = installedEntries.get(i);
            if (listener != null) {
                listener.onProgress(i + 1, Math.max(1, size));
            }
            String strOptString = jSONObject.optString("title", jSONObject.optString("fileName", "content"));
            if (listener != null) {
                listener.onStatus("Checking " + strOptString + "...");
            }
            UpdateCandidate updateCandidateCheckUpdateForEntry = checkUpdateForEntry(context, file, modManagerContentType, jSONObject, str, str2);
            if (updateCandidateCheckUpdateForEntry != null) {
                arrayList.add(updateCandidateCheckUpdateForEntry);
            }
        }
        if (listener != null) {
            listener.onStatus(arrayList.isEmpty() ? "No updates found." : arrayList.size() + " update(s) available.");
        }
        return arrayList;
    }

    public static UpdateCandidate checkUpdateForEntry(Context context, File file, ModManagerContentType modManagerContentType, JSONObject jSONObject, String str, String str2) throws Exception {
        ModrinthProject projectWithFallback;
        ArrayList<ModrinthVersion> projectVersionsWithFallback;
        ModManagerSource source = ModManagerManifest.getSource(jSONObject);
        if (source != ModManagerSource.MODRINTH && source != ModManagerSource.CURSEFORGE) {
            return null;
        }
        String lowerCase = "";
        String strTrim = jSONObject.optString("platformProjectId", "").trim();
        if (strTrim.isEmpty()) {
            strTrim = jSONObject.optString("projectId", "").trim();
        }
        if (strTrim.isEmpty()) {
            return null;
        }
        String strTrim2 = jSONObject.optString("platformVersionId", "").trim();
        if (strTrim2.isEmpty()) {
            strTrim2 = jSONObject.optString("versionId", "").trim();
        }
        String str3 = strTrim2;
        String strTrim3 = jSONObject.optString("versionNumber", "").trim();
        String lowerCase2 = jSONObject.optString("sha1", "").trim().toLowerCase(Locale.US);
        if (source == ModManagerSource.CURSEFORGE) {
            CurseForgeApiClient curseForgeApiClient = new CurseForgeApiClient(context);
            projectWithFallback = curseForgeApiClient.getProject(strTrim);
            projectVersionsWithFallback = curseForgeApiClient.getProjectVersions(projectWithFallback.projectId, modManagerContentType, str, str2);
        } else {
            ModrinthApiClient modrinthApiClient = new ModrinthApiClient();
            projectWithFallback = modrinthApiClient.getProjectWithFallback(strTrim, jSONObject.optString("slug", ""));
            projectVersionsWithFallback = modrinthApiClient.getProjectVersionsWithFallback(projectWithFallback, modManagerContentType, str, str2, false);
        }
        ArrayList<ModrinthVersion> arrayList = projectVersionsWithFallback;
        ModrinthProject modrinthProject = projectWithFallback;
        if (arrayList.isEmpty()) {
            return null;
        }
        boolean z = false;
        ModrinthVersion modrinthVersion = arrayList.get(0);
        ModrinthFile primaryFile = modrinthVersion.getPrimaryFile();
        if (primaryFile != null && primaryFile.sha1 != null) {
            lowerCase = primaryFile.sha1.trim().toLowerCase(Locale.US);
        }
        boolean z2 = !str3.isEmpty() && str3.equals(modrinthVersion.id);
        if (!lowerCase2.isEmpty() && !lowerCase.isEmpty() && lowerCase2.equals(lowerCase)) {
            z = true;
        }
        if (z2 || z) {
            return null;
        }
        return new UpdateCandidate(jSONObject, modManagerContentType, source, modrinthProject, modrinthVersion, str3, strTrim3);
    }

    public static void updateCandidate(Context context, File file, String str, String str2, UpdateCandidate updateCandidate, ModrinthInstallManager.Listener listener) {
        if (updateCandidate.source == ModManagerSource.CURSEFORGE) {
            CurseForgeInstallManager.installSpecificVersion(new CurseForgeApiClient(context), file, str, str2, updateCandidate.contentType, updateCandidate.project, updateCandidate.latestVersion, listener);
        } else {
            ModrinthInstallManager.installSpecificVersion(file, str, str2, updateCandidate.contentType, updateCandidate.project, updateCandidate.latestVersion, listener);
        }
    }

    public static void updateAll(Context context, File file, String str, String str2, ArrayList<UpdateCandidate> arrayList, final Listener listener) throws Exception {
        for (int i = 0; i < arrayList.size(); i++) {
            UpdateCandidate updateCandidate = arrayList.get(i);
            if (listener != null) {
                listener.onProgress(i + 1, Math.max(1, arrayList.size()));
                listener.onStatus("Updating " + updateCandidate.getDisplayName() + "...");
            }
            final AtomicReference atomicReference = new AtomicReference();
            updateCandidate(context, file, str, str2, updateCandidate, new ModrinthInstallManager.Listener() { // from class: ca.dnamobile.javalauncher.modmanager.ModManagerUpdateManager.1
                @Override // ca.dnamobile.javalauncher.modmanager.ModrinthInstallManager.Listener
                public void onStatus(String str3) {
                    Listener listener2 = listener;
                    if (listener2 != null) {
                        listener2.onStatus(str3);
                    }
                }

                @Override // ca.dnamobile.javalauncher.modmanager.ModrinthInstallManager.Listener
                public void onComplete(String str3) {
                    Listener listener2 = listener;
                    if (listener2 != null) {
                        listener2.onStatus(str3);
                    }
                }

                @Override // ca.dnamobile.javalauncher.modmanager.ModrinthInstallManager.Listener
                public void onError(Throwable th) {
                    atomicReference.set(th);
                }
            });
            if (atomicReference.get() != null) {
                throw new IllegalStateException("Failed to update " + updateCandidate.getDisplayName() + ": " + (((Throwable) atomicReference.get()).getMessage() != null ? ((Throwable) atomicReference.get()).getMessage() : ((Throwable) atomicReference.get()).getClass().getSimpleName()), (Throwable) atomicReference.get());
            }
        }
    }
}
