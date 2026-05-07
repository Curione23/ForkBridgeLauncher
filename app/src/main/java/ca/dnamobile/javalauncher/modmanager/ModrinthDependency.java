package ca.dnamobile.javalauncher.modmanager;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class ModrinthDependency {
    public final String dependencyType;
    public final String fileName;
    public final String projectId;
    public final String versionId;

    public ModrinthDependency(String str, String str2, String str3, String str4) {
        this.versionId = emptyToNull(str);
        this.projectId = emptyToNull(str2);
        this.fileName = emptyToNull(str3);
        this.dependencyType = str4;
    }

    public boolean isRequired() {
        return "required".equalsIgnoreCase(this.dependencyType);
    }

    private static String emptyToNull(String str) {
        if (str == null) {
            return null;
        }
        String strTrim = str.trim();
        if (strTrim.isEmpty() || "null".equalsIgnoreCase(strTrim)) {
            return null;
        }
        return strTrim;
    }
}
