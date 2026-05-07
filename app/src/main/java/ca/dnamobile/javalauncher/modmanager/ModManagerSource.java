package ca.dnamobile.javalauncher.modmanager;

import androidx.core.os.EnvironmentCompat;
import ca.dnamobile.javalauncher.R;
import java.util.Locale;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public enum ModManagerSource {
    MODRINTH("modrinth", "Modrinth", R.drawable.ic_source_modrinth_24),
    CURSEFORGE("curseforge", "CurseForge", R.drawable.ic_source_curseforge_24),
    MANUAL("manual", "Manual", 0),
    UNKNOWN(EnvironmentCompat.MEDIA_UNKNOWN, "Unknown", 0);

    private final String displayName;
    private final int iconRes;
    private final String id;

    ModManagerSource(String str, String str2, int i) {
        this.id = str;
        this.displayName = str2;
        this.iconRes = i;
    }

    public String getId() {
        return this.id;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public int getIconRes() {
        return this.iconRes;
    }

    public boolean hasIcon() {
        return this.iconRes != 0;
    }

    public static ModManagerSource fromId(String str) {
        if (str == null) {
            return UNKNOWN;
        }
        String lowerCase = str.trim().toLowerCase(Locale.US);
        if (lowerCase.isEmpty()) {
            return UNKNOWN;
        }
        for (ModManagerSource modManagerSource : values()) {
            if (modManagerSource.id.equals(lowerCase) || modManagerSource.name().toLowerCase(Locale.US).equals(lowerCase)) {
                return modManagerSource;
            }
        }
        if (lowerCase.contains("modrinth")) {
            return MODRINTH;
        }
        if (lowerCase.contains("curseforge") || lowerCase.contains("curse_forge")) {
            return CURSEFORGE;
        }
        return lowerCase.contains("manual") ? MANUAL : UNKNOWN;
    }
}
