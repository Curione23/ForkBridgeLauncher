package ca.dnamobile.javalauncher.launcher;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class LaunchPlan {
    private final String classPath;
    private final ArrayList<String> gameArgs;
    private final File gameDirectory;
    private final File javaBinary;
    private final ArrayList<String> jvmArgs;
    private final File lwjglNativeDirectory;
    private final String mainClass;
    private final File runtimeDirectory;
    private final String versionId;

    LaunchPlan(String str, String str2, File file, File file2, File file3, File file4, String str3, List<String> list, List<String> list2) {
        this.versionId = str;
        this.mainClass = str2;
        this.gameDirectory = file;
        this.runtimeDirectory = file2;
        this.javaBinary = file3;
        this.lwjglNativeDirectory = file4;
        this.classPath = str3;
        this.jvmArgs = new ArrayList<>(list);
        this.gameArgs = new ArrayList<>(list2);
    }

    public String getVersionId() {
        return this.versionId;
    }

    public String getMainClass() {
        return this.mainClass;
    }

    public File getGameDirectory() {
        return this.gameDirectory;
    }

    public File getRuntimeDirectory() {
        return this.runtimeDirectory;
    }

    public File getJavaBinary() {
        return this.javaBinary;
    }

    public File getLwjglNativeDirectory() {
        return this.lwjglNativeDirectory;
    }

    public String getClassPath() {
        return this.classPath;
    }

    public List<String> getJvmArgs() {
        return Collections.unmodifiableList(this.jvmArgs);
    }

    public List<String> getGameArgs() {
        return Collections.unmodifiableList(this.gameArgs);
    }

    LaunchPlan copyWithJvmArgs(List<String> list) {
        return new LaunchPlan(this.versionId, this.mainClass, this.gameDirectory, this.runtimeDirectory, this.javaBinary, this.lwjglNativeDirectory, this.classPath, list, this.gameArgs);
    }

    LaunchPlan copyWithGameArgs(List<String> list) {
        return new LaunchPlan(this.versionId, this.mainClass, this.gameDirectory, this.runtimeDirectory, this.javaBinary, this.lwjglNativeDirectory, this.classPath, this.jvmArgs, list);
    }
}
