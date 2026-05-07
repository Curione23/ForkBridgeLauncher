package ca.dnamobile.javalauncher.data.model;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public class MinecraftVersion {
    private final String id;
    private final String metadataUrl;
    private final String releaseTime;
    private final String type;

    public MinecraftVersion(String str, String str2, String str3, String str4) {
        this.id = str;
        this.type = str2;
        this.releaseTime = str3;
        this.metadataUrl = str4;
    }

    public String getId() {
        return this.id;
    }

    public String getType() {
        return this.type;
    }

    public String getReleaseTime() {
        return this.releaseTime;
    }

    public String getMetadataUrl() {
        return this.metadataUrl;
    }
}
