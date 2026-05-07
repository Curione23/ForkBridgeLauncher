package ca.dnamobile.javalauncher.instance;

import java.io.File;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class LauncherInstance {
    private final String baseVersionId;
    private final String createdAt;
    private final File gameDirectory;
    private final File iconFile;
    private final String id;
    private final boolean isolated;
    private final String loader;
    private final String minecraftVersionId;
    private final String name;
    private final File rootDirectory;
    private final String versionType;

    LauncherInstance(String str, String str2, String str3, String str4, String str5, File file, File file2, File file3, String str6) {
        this(str, str2, str3, str4, str4, str5, file, file2, file3, str6, true);
    }

    LauncherInstance(String str, String str2, String str3, String str4, String str5, String str6, File file, File file2, File file3, String str7) {
        this(str, str2, str3, str4, str5, str6, file, file2, file3, str7, true);
    }

    LauncherInstance(String str, String str2, String str3, String str4, String str5, File file, File file2, File file3, String str6, boolean z) {
        this(str, str2, str3, str4, str4, str5, file, file2, file3, str6, z);
    }

    LauncherInstance(String str, String str2, String str3, String str4, String str5, String str6, File file, File file2, File file3, String str7, boolean z) {
        this.id = str;
        this.name = str2;
        this.loader = str3;
        this.baseVersionId = str4;
        this.minecraftVersionId = str5.trim().isEmpty() ? str4 : str5;
        this.versionType = str6;
        this.rootDirectory = file;
        this.gameDirectory = file2;
        this.iconFile = file3;
        this.createdAt = str7;
        this.isolated = z;
    }

    public static LauncherInstance sharedInstalledVersion(String str, String str2, File file, String str3) {
        return sharedInstalledVersion(str, str2, file, str3, "Vanilla");
    }

    public static LauncherInstance sharedInstalledVersion(String str, String str2, File file, String str3, String str4) {
        return new LauncherInstance(sharedInstanceId(str, file), str, str4, str, str2, file, file, (File) null, str3, false);
    }

    public static String sharedInstanceId(String str, File file) {
        return "shared-" + Integer.toHexString(file.getAbsolutePath().hashCode()) + "-" + str;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getLoader() {
        return this.loader;
    }

    public String getBaseVersionId() {
        return this.baseVersionId;
    }

    public String getMinecraftVersionId() {
        return this.minecraftVersionId;
    }

    public String getVersionType() {
        return this.versionType;
    }

    public File getRootDirectory() {
        return this.rootDirectory;
    }

    public File getGameDirectory() {
        return this.gameDirectory;
    }

    public File getIconFile() {
        return this.iconFile;
    }

    public String getCreatedAt() {
        return this.createdAt;
    }

    public boolean isIsolated() {
        return this.isolated;
    }
}
