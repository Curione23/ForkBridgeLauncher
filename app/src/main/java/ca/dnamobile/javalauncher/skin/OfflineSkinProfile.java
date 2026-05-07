package ca.dnamobile.javalauncher.skin;

import java.io.File;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class OfflineSkinProfile {
    public final boolean enabled;
    public final SkinModelType model;
    public final File skinFile;
    public final String uniqueUuid;

    public OfflineSkinProfile(String str, File file, SkinModelType skinModelType, boolean z) {
        this.uniqueUuid = str;
        this.skinFile = file;
        this.model = skinModelType;
        this.enabled = z;
    }
}
