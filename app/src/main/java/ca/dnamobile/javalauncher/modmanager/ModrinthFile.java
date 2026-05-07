package ca.dnamobile.javalauncher.modmanager;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class ModrinthFile {
    public final String filename;
    public final boolean primary;
    public final String sha1;
    public final long size;
    public final String url;

    public ModrinthFile(String str, String str2, String str3, boolean z, long j) {
        this.url = str;
        this.filename = str2;
        this.sha1 = (str3 == null || str3.trim().isEmpty()) ? null : str3.trim();
        this.primary = z;
        this.size = j;
    }
}
