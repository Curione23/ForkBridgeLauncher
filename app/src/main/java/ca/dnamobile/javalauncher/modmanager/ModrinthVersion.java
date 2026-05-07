package ca.dnamobile.javalauncher.modmanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class ModrinthVersion {
    public final String changelog;
    public final String datePublished;
    public final List<ModrinthDependency> dependencies;
    public final long downloads;
    public final List<ModrinthFile> files;
    public final List<String> gameVersions;
    public final String id;
    public final List<String> loaders;
    public final String name;
    public final String projectId;
    public final String versionNumber;
    public final String versionType;

    public ModrinthVersion(String str, String str2, String str3, String str4, String str5, String str6, String str7, long j, List<String> list, List<String> list2, List<ModrinthDependency> list3, List<ModrinthFile> list4) {
        this.id = str;
        this.projectId = str2;
        this.name = str3;
        this.versionNumber = str4;
        this.versionType = str5;
        this.datePublished = str6;
        this.changelog = str7;
        this.downloads = j;
        this.gameVersions = Collections.unmodifiableList(new ArrayList(list));
        this.loaders = Collections.unmodifiableList(new ArrayList(list2));
        this.dependencies = Collections.unmodifiableList(new ArrayList(list3));
        this.files = Collections.unmodifiableList(new ArrayList(list4));
    }

    public ModrinthFile getPrimaryFile() {
        for (ModrinthFile modrinthFile : this.files) {
            if (modrinthFile.primary) {
                return modrinthFile;
            }
        }
        if (this.files.isEmpty()) {
            return null;
        }
        return this.files.get(0);
    }
}
