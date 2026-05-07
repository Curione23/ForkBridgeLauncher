package ca.dnamobile.javalauncher.launcher;

import ca.dnamobile.javalauncher.feature.log.Logging;
import ca.dnamobile.javalauncher.utils.path.PathManager;
import java.io.File;
import java.util.ArrayList;
import net.kdt.pojavlaunch.multirt.MultiRTUtils;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class RuntimeCompat {
    public static final String PATCH_ID = "JRE8_ANDROID_14_16_V31";
    private static final String TAG = "RuntimeCompat";

    private RuntimeCompat() {
    }

    public static File getRuntimeDirectory(String str) {
        try {
            File runtimeDir = MultiRTUtils.getRuntimeDir(str);
            if (runtimeDir != null) {
                return runtimeDir;
            }
        } catch (Throwable unused) {
        }
        return new File(PathManager.DIR_MULTIRT_HOME, str);
    }

    public static File resolveRuntimeForJava(int i) {
        for (String str : preferredRuntimes(i)) {
            File runtimeDirectory = getRuntimeDirectory(str);
            if (isRuntimeInstalledForJava(str, runtimeDirectory, i)) {
                Logging.i(TAG, "Runtime patch active: JRE8_ANDROID_14_16_V31 selected " + str + " -> " + runtimeDirectory.getAbsolutePath());
                return runtimeDirectory;
            }
            Logging.i(TAG, "Runtime " + str + " is not usable for Java " + i + ": " + describeRuntimeState(str, runtimeDirectory));
        }
        throw new IllegalStateException("No launchable internal Java runtime is installed for Java " + i);
    }

    private static String[] preferredRuntimes(int i) {
        if (i >= 25) {
            return new String[]{"Internal-25", "Internal-21", "Internal-17", "Internal-8"};
        }
        if (i >= 21) {
            return new String[]{"Internal-21", "Internal-25", "Internal-17", "Internal-8"};
        }
        if (i >= 17) {
            return new String[]{"Internal-17", "Internal-21", "Internal-25", "Internal-8"};
        }
        return new String[]{"Internal-8", "Internal-17", "Internal-21", "Internal-25"};
    }

    public static File normalizeRuntimeHome(String str, File file, int i) {
        if (isJava8Runtime(str) || i <= 8) {
            File fileFindJava8Home = findJava8Home(file);
            return fileFindJava8Home != null ? fileFindJava8Home : file;
        }
        File fileFindModernJavaHome = findModernJavaHome(file);
        return fileFindModernJavaHome != null ? fileFindModernJavaHome : file;
    }

    public static boolean isRuntimeInstalledForDisplay(String str) {
        return isRuntimeInstalledForJava(str, getRuntimeDirectory(str), javaMajorForRuntimeName(str));
    }

    public static boolean isRuntimeInstalledForJava(String str, File file, int i) {
        if (!file.isDirectory()) {
            return false;
        }
        if (isJava8Runtime(str) || i <= 8) {
            String strSafeRuntimeVersion = safeRuntimeVersion(str);
            File fileFindJava8Home = findJava8Home(file);
            return (isBlank(strSafeRuntimeVersion) || fileFindJava8Home == null || findLibJvm(fileFindJava8Home) == null || findFileNamed(fileFindJava8Home, "rt.jar", 8) == null) ? false : true;
        }
        File fileFindModernJavaHome = findModernJavaHome(file);
        return (fileFindModernJavaHome == null || findJavaBinary(fileFindModernJavaHome) == null || findLibJvm(fileFindModernJavaHome) == null || findFileNamed(fileFindModernJavaHome, "modules", 8) == null) ? false : true;
    }

    public static String describeRuntimeState(String str, File file) {
        String absolutePath;
        String absolutePath2;
        String absolutePath3;
        if (!file.exists()) {
            return "missing folder " + file.getAbsolutePath();
        }
        if (!file.isDirectory()) {
            return "not a directory " + file.getAbsolutePath();
        }
        String strSafeRuntimeVersion = safeRuntimeVersion(str);
        File fileFindJava8Home = findJava8Home(file);
        File fileFindModernJavaHome = findModernJavaHome(file);
        if (fileFindJava8Home == null) {
            fileFindJava8Home = fileFindModernJavaHome;
        }
        File fileFindJavaBinary = fileFindJava8Home != null ? findJavaBinary(fileFindJava8Home) : null;
        File fileFindLibJvm = fileFindJava8Home != null ? findLibJvm(fileFindJava8Home) : null;
        File fileFindFileNamed = fileFindJava8Home != null ? findFileNamed(fileFindJava8Home, "rt.jar", 8) : null;
        File fileFindFileNamed2 = fileFindJava8Home != null ? findFileNamed(fileFindJava8Home, "modules", 8) : null;
        StringBuilder sbAppend = new StringBuilder("path=").append(file.getAbsolutePath()).append(", marker=").append(strSafeRuntimeVersion).append(", javaHome=");
        String absolutePath4 = "<missing>";
        StringBuilder sbAppend2 = sbAppend.append(fileFindJava8Home != null ? fileFindJava8Home.getAbsolutePath() : "<missing>").append(", binJava=");
        if (fileFindJavaBinary == null) {
            absolutePath = "<missing>";
        } else {
            absolutePath = fileFindJavaBinary.getAbsolutePath();
        }
        StringBuilder sbAppend3 = sbAppend2.append(absolutePath).append(", libjvm=");
        if (fileFindLibJvm == null) {
            absolutePath2 = "<missing>";
        } else {
            absolutePath2 = fileFindLibJvm.getAbsolutePath();
        }
        StringBuilder sbAppend4 = sbAppend3.append(absolutePath2).append(", rt.jar=");
        if (fileFindFileNamed == null) {
            absolutePath3 = "<missing>";
        } else {
            absolutePath3 = fileFindFileNamed.getAbsolutePath();
        }
        StringBuilder sbAppend5 = sbAppend4.append(absolutePath3).append(", modules=");
        if (fileFindFileNamed2 != null) {
            absolutePath4 = fileFindFileNamed2.getAbsolutePath();
        }
        return sbAppend5.append(absolutePath4).toString();
    }

    public static File findJavaBinary(File file) {
        File file2 = new File(file, "bin/java");
        return file2.isFile() ? file2 : findFileNamed(file, "java", 4);
    }

    public static File findJava8Home(File file) {
        File parentFile;
        File parentFile2;
        if (new File(file, "lib/rt.jar").isFile()) {
            return file;
        }
        if (new File(file, "jre/lib/rt.jar").isFile()) {
            return new File(file, "jre");
        }
        File fileFindFileNamed = findFileNamed(file, "rt.jar", 10);
        if (fileFindFileNamed == null || (parentFile = fileFindFileNamed.getParentFile()) == null || !"lib".equals(parentFile.getName()) || (parentFile2 = parentFile.getParentFile()) == null || !parentFile2.isDirectory()) {
            return null;
        }
        return parentFile2;
    }

    public static File findModernJavaHome(File file) {
        File parentFile;
        File parentFile2;
        if (new File(file, "lib/modules").isFile()) {
            return file;
        }
        if (new File(file, "jre/lib/modules").isFile()) {
            return new File(file, "jre");
        }
        File fileFindFileNamed = findFileNamed(file, "modules", 10);
        if (fileFindFileNamed == null || (parentFile = fileFindFileNamed.getParentFile()) == null || !"lib".equals(parentFile.getName()) || (parentFile2 = parentFile.getParentFile()) == null || !parentFile2.isDirectory()) {
            return null;
        }
        return parentFile2;
    }

    public static File findLibJvm(File file) {
        ArrayList<File> arrayList = new ArrayList();
        arrayList.add(new File(file, "lib/server/libjvm.so"));
        arrayList.add(new File(file, "lib/client/libjvm.so"));
        arrayList.add(new File(file, "lib/aarch64/server/libjvm.so"));
        arrayList.add(new File(file, "lib/aarch64/client/libjvm.so"));
        arrayList.add(new File(file, "lib/arm64/server/libjvm.so"));
        arrayList.add(new File(file, "lib/arm64/client/libjvm.so"));
        arrayList.add(new File(file, "lib/arm64-v8a/server/libjvm.so"));
        arrayList.add(new File(file, "lib/arm64-v8a/client/libjvm.so"));
        arrayList.add(new File(file, "lib/arm/server/libjvm.so"));
        arrayList.add(new File(file, "lib/arm/client/libjvm.so"));
        arrayList.add(new File(file, "lib/x86_64/server/libjvm.so"));
        arrayList.add(new File(file, "lib/x86_64/client/libjvm.so"));
        arrayList.add(new File(file, "lib/i386/server/libjvm.so"));
        arrayList.add(new File(file, "lib/i386/client/libjvm.so"));
        for (File file2 : arrayList) {
            if (file2.isFile()) {
                return file2;
            }
        }
        return findFileNamed(file, "libjvm.so", 10);
    }

    public static File findFileNamed(File file, String str, int i) {
        File[] fileArrListFiles;
        File fileFindFileNamed;
        if (i < 0 || !file.isDirectory() || (fileArrListFiles = file.listFiles()) == null) {
            return null;
        }
        for (File file2 : fileArrListFiles) {
            if (file2.isFile() && str.equals(file2.getName())) {
                return file2;
            }
            if (file2.isDirectory() && (fileFindFileNamed = findFileNamed(file2, str, i - 1)) != null) {
                return fileFindFileNamed;
            }
        }
        return null;
    }

    public static int javaMajorForRuntimeName(String str) {
        if (str.contains("25")) {
            return 25;
        }
        if (str.contains("21")) {
            return 21;
        }
        if (str.contains("17")) {
            return 17;
        }
        str.contains("8");
        return 8;
    }

    private static boolean isJava8Runtime(String str) {
        return str.contains("8");
    }

    private static String safeRuntimeVersion(String str) {
        try {
            return emptyToNull(MultiRTUtils.readInternalRuntimeVersion(str));
        } catch (Throwable unused) {
            return null;
        }
    }

    private static String emptyToNull(String str) {
        if (str == null || str.trim().isEmpty()) {
            return null;
        }
        return str.trim();
    }

    private static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
}
