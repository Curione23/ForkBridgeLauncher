package ca.dnamobile.javalauncher.modmanager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public class ModrinthProject {
    public String author;
    public String body;
    public ArrayList<String> categories;
    public String dateModified;
    public String description;
    public long downloads;
    public long followers;
    public ArrayList<String> galleryUrls;
    public ArrayList<String> gameVersions;
    public String iconUrl;
    public ArrayList<String> loaders;
    public String projectId;
    public String projectType;
    public String slug;
    public ModManagerSource source;
    public String title;
    public String websiteUrl;

    public ModrinthProject() {
        this.projectId = "";
        this.slug = "";
        this.title = "";
        this.description = "";
        this.author = null;
        this.iconUrl = null;
        this.body = "";
        this.projectType = "mod";
        this.categories = new ArrayList<>();
        this.galleryUrls = new ArrayList<>();
        this.gameVersions = new ArrayList<>();
        this.loaders = new ArrayList<>();
        this.downloads = 0L;
        this.followers = 0L;
        this.dateModified = null;
        this.source = ModManagerSource.MODRINTH;
        this.websiteUrl = null;
    }

    public ModrinthProject(String str, String str2, String str3, String str4, String str5, String str6, List<String> list, long j, long j2, String str7, ModManagerSource modManagerSource) {
        this.projectId = "";
        this.slug = "";
        this.title = "";
        this.description = "";
        this.author = null;
        this.iconUrl = null;
        this.body = "";
        this.projectType = "mod";
        this.categories = new ArrayList<>();
        this.galleryUrls = new ArrayList<>();
        this.gameVersions = new ArrayList<>();
        this.loaders = new ArrayList<>();
        this.downloads = 0L;
        this.followers = 0L;
        this.dateModified = null;
        this.source = ModManagerSource.MODRINTH;
        this.websiteUrl = null;
        this.projectId = safe(str);
        this.slug = safe(str2);
        this.title = safe(str3);
        this.description = safe(str4);
        this.author = str5;
        this.iconUrl = normalizeIconUrl(str6);
        if (list != null) {
            this.categories.addAll(list);
        }
        this.downloads = j;
        this.followers = j2;
        this.dateModified = str7;
        this.source = modManagerSource;
        inferProjectTypeFromCategories();
    }

    public ModrinthProject(String str, String str2, String str3, String str4, String str5, String str6, String str7, String str8, long j, long j2, String str9, ArrayList<String> arrayList, ArrayList<String> arrayList2) {
        this(str, str2, str3, str4, str5, str6, arrayList, j, j2, str9, ModManagerSource.MODRINTH);
        this.body = safe(str7);
        if (arrayList2 != null) {
            this.galleryUrls.addAll(arrayList2);
        }
        this.iconUrl = firstImageUrl(str6, str7, str8, str5, firstGalleryUrl(this.galleryUrls));
        String strNormalizeProjectType = normalizeProjectType(str8);
        if (!strNormalizeProjectType.isEmpty()) {
            this.projectType = strNormalizeProjectType;
        }
        inferProjectTypeFromCategories();
    }

    public ModrinthProject(String str, String str2, String str3, String str4, String str5, String str6, String str7, String str8, long j, long j2, String str9, ArrayList<String> arrayList, ArrayList<String> arrayList2, ModManagerSource modManagerSource, String str10) {
        this(str, str2, str3, str4, str5, str6, arrayList, j, j2, str9, modManagerSource);
        this.body = safe(str7);
        if (arrayList2 != null) {
            this.galleryUrls.addAll(arrayList2);
        }
        this.iconUrl = firstImageUrl(str6, str7, str8, str5, firstGalleryUrl(this.galleryUrls));
        String strNormalizeProjectType = normalizeProjectType(str8);
        if (!strNormalizeProjectType.isEmpty()) {
            this.projectType = strNormalizeProjectType;
        }
        this.websiteUrl = str10;
        inferProjectTypeFromCategories();
    }

    public String getWebsiteUrl() {
        String str = this.websiteUrl;
        if (str != null && !str.trim().isEmpty()) {
            return this.websiteUrl.trim();
        }
        if (this.source == ModManagerSource.CURSEFORGE) {
            if (!this.slug.trim().isEmpty() && this.slug.startsWith("http")) {
                return this.slug;
            }
            if (!this.slug.trim().isEmpty()) {
                return "https://www.curseforge.com/minecraft/" + (isModpack() ? "modpacks" : "mc-mods") + "/" + this.slug;
            }
            return "https://www.curseforge.com/minecraft/search?search=" + this.title.replace(' ', '+');
        }
        return "https://modrinth.com/" + (isModpack() ? "modpack" : "mod") + "/" + (!this.slug.trim().isEmpty() ? this.slug : this.projectId);
    }

    public boolean isModpack() {
        return "modpack".equalsIgnoreCase(this.projectType) || categoriesContain("modpack") || categoriesContain("modpacks");
    }

    private void inferProjectTypeFromCategories() {
        if (categoriesContain("modpack") || categoriesContain("modpacks")) {
            this.projectType = "modpack";
            return;
        }
        if (categoriesContain("resourcepack") || categoriesContain("resourcepacks")) {
            this.projectType = "resourcepack";
        } else if (categoriesContain("shader") || categoriesContain("shaders") || categoriesContain("shaderpack") || categoriesContain("shaderpacks")) {
            this.projectType = "shaderpack";
        }
    }

    private boolean categoriesContain(String str) {
        for (String str2 : this.categories) {
            if (str2 != null && str.equalsIgnoreCase(str2.trim())) {
                return true;
            }
        }
        return false;
    }

    public String normalizedTitleKey() {
        return this.title.trim().toLowerCase(Locale.US);
    }

    private static String firstImageUrl(String... strArr) {
        if (strArr == null) {
            return null;
        }
        for (String str : strArr) {
            String strNormalizeIconUrl = normalizeIconUrl(str);
            if (strNormalizeIconUrl != null) {
                return strNormalizeIconUrl;
            }
        }
        return null;
    }

    private static String firstGalleryUrl(ArrayList<String> arrayList) {
        if (arrayList == null) {
            return null;
        }
        Iterator<String> it = arrayList.iterator();
        while (it.hasNext()) {
            String strNormalizeIconUrl = normalizeIconUrl(it.next());
            if (strNormalizeIconUrl != null) {
                return strNormalizeIconUrl;
            }
        }
        return null;
    }

    private static String normalizeProjectType(String str) {
        if (str == null) {
            return "";
        }
        String strReplace = str.trim().toLowerCase(Locale.US).replace('_', '-');
        if (strReplace.isEmpty() || strReplace.startsWith("http://") || strReplace.startsWith("https://") || "null".equals(strReplace)) {
            return "";
        }
        return "mods".equals(strReplace) ? "mod" : "modpacks".equals(strReplace) ? "modpack" : ("resourcepacks".equals(strReplace) || "resource-pack".equals(strReplace) || "resource-packs".equals(strReplace)) ? "resourcepack" : ("shader".equals(strReplace) || "shaders".equals(strReplace) || "shaderpacks".equals(strReplace) || "shader-pack".equals(strReplace) || "shader-packs".equals(strReplace)) ? "shaderpack" : ("mod".equals(strReplace) || "modpack".equals(strReplace) || "resourcepack".equals(strReplace) || "shaderpack".equals(strReplace)) ? strReplace : "";
    }

    private static String normalizeIconUrl(String str) {
        if (str == null) {
            return null;
        }
        String strTrim = str.trim();
        if ((strTrim.startsWith("\"") && strTrim.endsWith("\"")) || (strTrim.startsWith("'") && strTrim.endsWith("'"))) {
            strTrim = strTrim.substring(1, strTrim.length() - 1).trim();
        }
        if (strTrim.isEmpty() || "null".equalsIgnoreCase(strTrim)) {
            return null;
        }
        if (strTrim.startsWith("//")) {
            return "https:" + strTrim;
        }
        if (strTrim.startsWith("http://") || strTrim.startsWith("https://")) {
            return strTrim;
        }
        return null;
    }

    private static String safe(String str) {
        return str == null ? "" : str;
    }
}
