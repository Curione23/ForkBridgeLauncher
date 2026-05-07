package ca.dnamobile.javalauncher.modmanager;

import java.io.File;
import java.util.Locale;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public enum ModManagerContentType {
    MODS("mods", "mod", "mods", 6),
    MODPACKS("modpacks", "modpack", "modpacks", 4471),
    RESOURCEPACKS("resourcepacks", "resourcepack", "resourcepacks", 12),
    SHADERPACKS("shaderpacks", "shader", "shaderpacks", 6552);

    private final int curseForgeClassId;
    private final String intentValue;
    private final String modrinthProjectType;
    private final String targetFolderName;

    ModManagerContentType(String str, String str2, String str3, int i) {
        this.intentValue = str;
        this.modrinthProjectType = str2;
        this.targetFolderName = str3;
        this.curseForgeClassId = i;
    }

    public String getIntentValue() {
        return this.intentValue;
    }

    public String getModrinthProjectType() {
        return this.modrinthProjectType;
    }

    public String getTargetFolderName() {
        return this.targetFolderName;
    }

    public int getCurseForgeClassId() {
        return this.curseForgeClassId;
    }

    public File getTargetDirectory(File file) {
        return this == MODPACKS ? file : new File(file, this.targetFolderName);
    }

    public boolean supportsDependencies() {
        return this == MODS;
    }

    public boolean isLoaderSpecific() {
        return this == MODS || this == MODPACKS;
    }

    public boolean isInstallableIntoExistingInstance() {
        return this != MODPACKS;
    }

    public static ModManagerContentType fromValue(String str) {
        if (str == null) {
            return MODS;
        }
        String strReplace = str.trim().toLowerCase(Locale.US).replace('-', '_');
        if (strReplace.isEmpty()) {
            return MODS;
        }
        for (ModManagerContentType modManagerContentType : values()) {
            if (modManagerContentType.intentValue.equalsIgnoreCase(strReplace) || modManagerContentType.name().equalsIgnoreCase(strReplace) || modManagerContentType.modrinthProjectType.equalsIgnoreCase(strReplace) || modManagerContentType.targetFolderName.equalsIgnoreCase(strReplace)) {
                return modManagerContentType;
            }
        }
        if ("resource_packs".equals(strReplace) || "resource-pack".equals(strReplace)) {
            return RESOURCEPACKS;
        }
        if ("shader".equals(strReplace) || "shaders".equals(strReplace)) {
            return SHADERPACKS;
        }
        if ("pack".equals(strReplace) || "packs".equals(strReplace) || "modpack".equals(strReplace)) {
            return MODPACKS;
        }
        return MODS;
    }
}
