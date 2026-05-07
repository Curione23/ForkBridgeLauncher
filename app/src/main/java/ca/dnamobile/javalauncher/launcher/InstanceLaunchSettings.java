package ca.dnamobile.javalauncher.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import ca.dnamobile.javalauncher.feature.log.Logging;
import ca.dnamobile.javalauncher.renderer.RendererInterface;
import ca.dnamobile.javalauncher.settings.MemoryAllocationUtils;
import ca.dnamobile.javalauncher.storage.StorageLocationStore;
import com.google.android.material.internal.ViewUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class InstanceLaunchSettings {
    private static final String PREFS = "per_instance_launch_settings";
    public static final int RAM_DEFAULT = -1;
    public static final String RENDERER_DEFAULT = "";
    public static final String RUNTIME_DEFAULT = "";
    private static final String TAG = "InstanceLaunchSettings";

    private InstanceLaunchSettings() {
    }

    public static final class Settings {
        public String rendererIdentifier = "";
        public String runtimeName = "";
        public String customJvmArgs = "";
        public int ramMb = -1;

        public boolean hasRendererOverride() {
            return !InstanceLaunchSettings.isBlank(this.rendererIdentifier);
        }

        public boolean hasRuntimeOverride() {
            return !InstanceLaunchSettings.isBlank(this.runtimeName);
        }

        public boolean hasRamOverride() {
            return this.ramMb > 0;
        }

        public boolean hasCustomJvmArgs() {
            return !InstanceLaunchSettings.isBlank(this.customJvmArgs);
        }

        public boolean hasAnyOverride() {
            return hasRendererOverride() || hasRuntimeOverride() || hasRamOverride() || hasCustomJvmArgs();
        }
    }

    public static String resolveInstanceKey(String str, String str2) {
        return firstNonBlank(str, str2, StorageLocationStore.DEFAULT_LOCATION_ID).trim().replace('\n', ' ').replace('\r', ' ');
    }

    public static Settings load(Context context, String str) {
        SharedPreferences sharedPreferencesPrefs = prefs(context);
        String strPrefix = prefix(str);
        Settings settings = new Settings();
        settings.rendererIdentifier = sharedPreferencesPrefs.getString(strPrefix + "renderer", "");
        settings.runtimeName = sharedPreferencesPrefs.getString(strPrefix + "runtime", "");
        settings.customJvmArgs = sharedPreferencesPrefs.getString(strPrefix + "jvm_args", "");
        settings.ramMb = sharedPreferencesPrefs.getInt(strPrefix + "ram_mb", -1);
        if (settings.rendererIdentifier == null) {
            settings.rendererIdentifier = "";
        }
        if (settings.runtimeName == null) {
            settings.runtimeName = "";
        }
        if (settings.customJvmArgs == null) {
            settings.customJvmArgs = "";
        }
        return settings;
    }

    public static void save(Context context, String str, Settings settings) {
        String strPrefix = prefix(str);
        SharedPreferences.Editor editorEdit = prefs(context).edit();
        editorEdit.putString(strPrefix + "renderer", safe(settings.rendererIdentifier));
        editorEdit.putString(strPrefix + "runtime", safe(settings.runtimeName));
        editorEdit.putString(strPrefix + "jvm_args", safe(settings.customJvmArgs));
        editorEdit.putInt(strPrefix + "ram_mb", settings.ramMb > 0 ? settings.ramMb : -1);
        Logging.i(TAG, "Saved per-instance settings for " + str + ": renderer=" + safe(settings.rendererIdentifier) + ", runtime=" + safe(settings.runtimeName) + ", ram=" + (settings.ramMb > 0 ? settings.ramMb : -1) + ", jvmArgs=" + (!isBlank(settings.customJvmArgs)) + ", committed=" + editorEdit.commit());
    }

    public static void clear(Context context, String str) {
        String strPrefix = prefix(str);
        prefs(context).edit().remove(strPrefix + "renderer").remove(strPrefix + "runtime").remove(strPrefix + "jvm_args").remove(strPrefix + "ram_mb").commit();
        Logging.i(TAG, "Cleared per-instance settings for " + str);
    }

    public static String[] getRuntimeNames() {
        return new String[]{"Internal-8", "Internal-17", "Internal-21", "Internal-25"};
    }

    public static String[] getRuntimeDisplayLabels() {
        String str;
        String[] runtimeNames = getRuntimeNames();
        String[] strArr = new String[runtimeNames.length + 1];
        int i = 0;
        strArr[0] = "Default for Minecraft version";
        while (i < runtimeNames.length) {
            int i2 = i + 1;
            if (RuntimeCompat.isRuntimeInstalledForDisplay(runtimeNames[i])) {
                str = runtimeNames[i];
            } else {
                str = runtimeNames[i] + " (not installed)";
            }
            strArr[i2] = str;
            i = i2;
        }
        return strArr;
    }

    public static int runtimeIndexForName(String str) {
        if (isBlank(str)) {
            return 0;
        }
        String[] runtimeNames = getRuntimeNames();
        for (int i = 0; i < runtimeNames.length; i++) {
            if (runtimeNames[i].equals(str)) {
                return i + 1;
            }
        }
        return 0;
    }

    public static String runtimeNameForIndex(int i) {
        String[] runtimeNames = getRuntimeNames();
        int i2 = i - 1;
        if (i2 < 0 || i2 >= runtimeNames.length) {
            return "";
        }
        return runtimeNames[i2];
    }

    public static File resolveRuntimeDirectory(Settings settings, File file) {
        if (!settings.hasRuntimeOverride()) {
            return file;
        }
        File runtimeDirectory = RuntimeCompat.getRuntimeDirectory(settings.runtimeName);
        if (RuntimeCompat.isRuntimeInstalledForJava(settings.runtimeName, runtimeDirectory, RuntimeCompat.javaMajorForRuntimeName(settings.runtimeName))) {
            Logging.i(TAG, "Using per-instance Java runtime: " + settings.runtimeName + " -> " + runtimeDirectory.getAbsolutePath());
            return runtimeDirectory;
        }
        Logging.i(TAG, "Per-instance Java runtime is missing/broken, falling back to default: " + settings.runtimeName + " state=" + RuntimeCompat.describeRuntimeState(settings.runtimeName, runtimeDirectory));
        return file;
    }

    public static LaunchPlan applyJvmOverrides(Context context, LaunchPlan launchPlan, Settings settings) {
        if (!settings.hasRamOverride() && !settings.hasCustomJvmArgs()) {
            return launchPlan;
        }
        ArrayList arrayList = new ArrayList(launchPlan.getJvmArgs());
        if (settings.hasRamOverride()) {
            int iClampToAllowedRam = MemoryAllocationUtils.clampToAllowedRam(context, settings.ramMb);
            int iResolveStartHeapMb = resolveStartHeapMb(iClampToAllowedRam);
            purgeArg(arrayList, "-Xms");
            purgeArg(arrayList, "-Xmx");
            insertBeforeClasspath(arrayList, "-Xms" + iResolveStartHeapMb + "M");
            insertBeforeClasspath(arrayList, "-Xmx" + iClampToAllowedRam + "M");
            Logging.i(TAG, "Applied per-instance RAM: Xms=" + iResolveStartHeapMb + " MB, Xmx=" + iClampToAllowedRam + " MB");
        }
        if (settings.hasCustomJvmArgs()) {
            Iterator<String> it = sanitizeCustomJvmArgs(tokenizeJvmArgs(settings.customJvmArgs)).iterator();
            while (it.hasNext()) {
                insertBeforeClasspath(arrayList, it.next());
            }
            Logging.i(TAG, "Applied per-instance JVM args: " + settings.customJvmArgs);
        }
        return launchPlan.copyWithJvmArgs(arrayList);
    }

    public static String describeRendererChoice(RendererInterface rendererInterface) {
        if (rendererInterface == null) {
            return "Default launcher renderer";
        }
        return rendererInterface.getRendererName() + (rendererInterface.isExternalPlugin() ? "  •  Plugin" : "");
    }

    static /* synthetic */ boolean lambda$purgeArg$0(String str, String str2) {
        return str2 != null && str2.startsWith(str);
    }

    private static void purgeArg(ArrayList<String> arrayList, final String str) {
        arrayList.removeIf(new Predicate() { // from class: ca.dnamobile.javalauncher.launcher.InstanceLaunchSettings$$ExternalSyntheticLambda0
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return InstanceLaunchSettings.lambda$purgeArg$0(str, (String) obj);
            }
        });
    }

    private static int resolveStartHeapMb(int i) {
        if (i <= 0) {
            return 512;
        }
        return Math.min(ViewUtils.EDGE_TO_EDGE_FLAGS, Math.max(512, i / 4));
    }

    private static void insertBeforeClasspath(ArrayList<String> arrayList, String str) {
        if (isBlank(str)) {
            return;
        }
        int iFindClasspathIndex = findClasspathIndex(arrayList);
        if (iFindClasspathIndex < 0) {
            iFindClasspathIndex = arrayList.size();
        }
        arrayList.add(iFindClasspathIndex, str);
    }

    private static int findClasspathIndex(List<String> list) {
        for (int i = 0; i < list.size(); i++) {
            String str = list.get(i);
            if ("-cp".equals(str) || "-classpath".equals(str) || "--class-path".equals(str)) {
                return i;
            }
        }
        return -1;
    }

    private static ArrayList<String> sanitizeCustomJvmArgs(ArrayList<String> arrayList) {
        ArrayList<String> arrayList2 = new ArrayList<>();
        int i = 0;
        while (i < arrayList.size()) {
            String str = arrayList.get(i);
            if (!isBlank(str)) {
                if ("-cp".equals(str) || "-classpath".equals(str) || "--class-path".equals(str)) {
                    i++;
                } else if (!str.startsWith("-Xms") && !str.startsWith("-Xmx") && !"java".equals(str) && !str.endsWith("/java") && !str.endsWith("\\java.exe")) {
                    arrayList2.add(str);
                }
            }
            i++;
        }
        return arrayList2;
    }

    public static ArrayList<String> tokenizeJvmArgs(String str) {
        ArrayList<String> arrayList = new ArrayList<>();
        if (str != null && !str.trim().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            boolean z = false;
            boolean z2 = false;
            boolean z3 = false;
            for (int i = 0; i < str.length(); i++) {
                char cCharAt = str.charAt(i);
                if (z) {
                    sb.append(cCharAt);
                    z = false;
                } else if (cCharAt == '\\') {
                    z = true;
                } else if (cCharAt == '\'' && !z2) {
                    z3 = !z3;
                } else if (cCharAt == '\"' && !z3) {
                    z2 = !z2;
                } else if (Character.isWhitespace(cCharAt) && !z3 && !z2) {
                    if (sb.length() > 0) {
                        arrayList.add(sb.toString());
                        sb.setLength(0);
                    }
                } else {
                    sb.append(cCharAt);
                }
            }
            if (z) {
                sb.append('\\');
            }
            if (sb.length() > 0) {
                arrayList.add(sb.toString());
            }
        }
        return arrayList;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, 0);
    }

    private static String prefix(String str) {
        return "instance." + sanitizeKey(str) + ".";
    }

    private static String sanitizeKey(String str) {
        String lowerCase = str.trim().toLowerCase(Locale.ROOT);
        return lowerCase.isEmpty() ? StorageLocationStore.DEFAULT_LOCATION_ID : lowerCase.replaceAll("[^a-z0-9._-]", "_");
    }

    private static String firstNonBlank(String... strArr) {
        if (strArr != null) {
            for (String str : strArr) {
                if (!isBlank(str)) {
                    return str.trim();
                }
            }
            return "";
        }
        return "";
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    private static String safe(String str) {
        return str == null ? "" : str.trim();
    }
}
