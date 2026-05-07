package ca.dnamobile.javalauncher.launcher;

import android.content.Context;
import android.os.Build;
import android.system.Os;
import ca.dnamobile.javalauncher.feature.log.Logging;
import ca.dnamobile.javalauncher.modcompat.DiscordRpcCompatPatch;
import ca.dnamobile.javalauncher.renderer.DriverPluginManager;
import ca.dnamobile.javalauncher.renderer.RendererInterface;
import ca.dnamobile.javalauncher.utils.path.PathManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.kdt.pojavlaunch.Architecture;
import net.kdt.pojavlaunch.utils.JREUtils;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class JavaRuntimeBootstrap {
    private static final String TAG = "JavaRuntimeBootstrap";

    private JavaRuntimeBootstrap() {
    }

    public static RuntimePaths prepare(Context context, LaunchPlan launchPlan, RendererInterface rendererInterface) {
        PathManager.initContextConstants(context);
        RuntimePaths runtimePathsResolve = RuntimePaths.resolve(context, launchPlan, rendererInterface);
        DiscordRpcCompatPatch.apply(context, launchPlan);
        applyEnvironment(context, launchPlan, rendererInterface, runtimePathsResolve);
        pushLdLibraryPath(runtimePathsResolve);
        preloadRuntimeAndGraphics(launchPlan, rendererInterface, runtimePathsResolve);
        return runtimePathsResolve;
    }

    private static void applyEnvironment(Context context, LaunchPlan launchPlan, RendererInterface rendererInterface, RuntimePaths runtimePaths) {
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        linkedHashMap.put("JAVA_HOME", launchPlan.getRuntimeDirectory().getAbsolutePath());
        linkedHashMap.put("HOME", launchPlan.getGameDirectory().getAbsolutePath());
        linkedHashMap.put("TMPDIR", PathManager.DIR_CACHE.getAbsolutePath());
        linkedHashMap.put("POJAV_NATIVEDIR", PathManager.DIR_NATIVE_LIB);
        linkedHashMap.put("LD_LIBRARY_PATH", runtimePaths.ldLibraryPath);
        linkedHashMap.put("PATH", new File(launchPlan.getRuntimeDirectory(), "bin").getAbsolutePath() + ":" + safeEnv("PATH"));
        linkedHashMap.put("AWTSTUB_WIDTH", resolveJvmArgValue(launchPlan, "-Dglfwstub.windowWidth=", "1"));
        linkedHashMap.put("AWTSTUB_HEIGHT", resolveJvmArgValue(launchPlan, "-Dglfwstub.windowHeight=", "1"));
        linkedHashMap.put("MOD_ANDROID_RUNTIME", PathManager.DIR_RUNTIME_MOD != null ? PathManager.DIR_RUNTIME_MOD.getAbsolutePath() : "");
        linkedHashMap.put("POJAV_RENDERER", isLtwRenderer(rendererInterface) ? "opengles3_ltw" : rendererInterface.getRendererId());
        linkedHashMap.putAll(rendererInterface.getRendererEnv());
        linkedHashMap.putAll(DriverPluginManager.buildEnvironment(context, rendererInterface));
        if (isLtwRenderer(rendererInterface)) {
            linkedHashMap.put("POJAV_RENDERER", "opengles3_ltw");
            linkedHashMap.put("POJAVEXEC_EGL", "libltw.so");
            linkedHashMap.put("POJAV_EGL_LIBRARY", "libltw.so");
            linkedHashMap.put("POJAVEXEC_EGL_LIBRARY", "libltw.so");
            linkedHashMap.put("POJAV_RENDERER_LIBRARY", "libltw.so");
            linkedHashMap.put("POJAVEXEC_RENDERER", "libltw.so");
            linkedHashMap.put("LIBGL_ES", "3");
            linkedHashMap.put("POJAV_USE_SYSTEM_VULKAN", "1");
            linkedHashMap.put("DRIVER_PATH", "");
            linkedHashMap.put("VK_ICD_FILENAMES", "");
            linkedHashMap.put("VK_DRIVER_FILES", "");
            linkedHashMap.put("LIBGL_DRIVERS_PATH", "");
            linkedHashMap.put("EGL_DRIVERS_PATH", "");
            linkedHashMap.put("MESA_LOADER_DRIVER_OVERRIDE", "");
            linkedHashMap.put("GALLIUM_DRIVER", "");
            linkedHashMap.put("OSMESA_LIB", "");
            linkedHashMap.put("LTW_NEVER_FLUSH_BUFFERS", "0");
            linkedHashMap.put("LTW_COHERENT_DYNAMIC_STORAGE", "0");
        }
        String strSanitizeLibraryName = sanitizeLibraryName(rendererInterface.getRendererEGL());
        if (strSanitizeLibraryName.isEmpty()) {
            strSanitizeLibraryName = inferPojavExecEgl(rendererInterface);
        }
        if (!strSanitizeLibraryName.isEmpty()) {
            linkedHashMap.put("POJAVEXEC_EGL", strSanitizeLibraryName);
        }
        applyRendererBridgeAliases(linkedHashMap, rendererInterface, strSanitizeLibraryName);
        String strFindMesaDriverPath = isLtwRenderer(rendererInterface) ? null : findMesaDriverPath(runtimePaths);
        if (strFindMesaDriverPath != null) {
            linkedHashMap.put("LIBGL_DRIVERS_PATH", strFindMesaDriverPath);
            linkedHashMap.put("EGL_DRIVERS_PATH", strFindMesaDriverPath);
        }
        String strFindJsphLibrary = findJsphLibrary(runtimePaths);
        if (strFindJsphLibrary != null) {
            linkedHashMap.put("JSP", strFindJsphLibrary);
        }
        for (Map.Entry entry : linkedHashMap.entrySet()) {
            setEnv((String) entry.getKey(), (String) entry.getValue());
        }
    }

    private static void applyRendererBridgeAliases(LinkedHashMap<String, String> linkedHashMap, RendererInterface rendererInterface, String str) {
        String strSanitizeLibraryName = sanitizeLibraryName(rendererInterface.getRendererLibrary());
        String lowerCase = (rendererInterface.getRendererId() + " " + rendererInterface.getRendererName() + " " + strSanitizeLibraryName).toLowerCase(Locale.ROOT);
        if (isLtwRenderer(rendererInterface)) {
            linkedHashMap.put("POJAV_RENDERER", "opengles3_ltw");
            linkedHashMap.put("POJAVEXEC_EGL", "libltw.so");
            linkedHashMap.put("POJAV_EGL_LIBRARY", "libltw.so");
            linkedHashMap.put("POJAVEXEC_EGL_LIBRARY", "libltw.so");
            linkedHashMap.put("POJAV_RENDERER_LIBRARY", "libltw.so");
            linkedHashMap.put("POJAVEXEC_RENDERER", "libltw.so");
            linkedHashMap.put("LIBGL_ES", "3");
            linkedHashMap.put("POJAV_USE_SYSTEM_VULKAN", "1");
            linkedHashMap.put("DRIVER_PATH", "");
            linkedHashMap.put("VK_ICD_FILENAMES", "");
            linkedHashMap.put("VK_DRIVER_FILES", "");
            linkedHashMap.put("LIBGL_DRIVERS_PATH", "");
            linkedHashMap.put("EGL_DRIVERS_PATH", "");
            linkedHashMap.put("OSMESA_LIB", "");
            linkedHashMap.put("GALLIUM_DRIVER", "");
            linkedHashMap.put("MESA_LOADER_DRIVER_OVERRIDE", "");
            linkedHashMap.put("LTW_NEVER_FLUSH_BUFFERS", "0");
            linkedHashMap.put("LTW_COHERENT_DYNAMIC_STORAGE", "0");
            return;
        }
        if (!strSanitizeLibraryName.isEmpty()) {
            linkedHashMap.put("POJAV_RENDERER_LIBRARY", strSanitizeLibraryName);
            linkedHashMap.put("POJAVEXEC_RENDERER", strSanitizeLibraryName);
            linkedHashMap.put("OSMESA_LIB", strSanitizeLibraryName);
        }
        if (!str.isEmpty()) {
            linkedHashMap.put("POJAV_EGL_LIBRARY", str);
            linkedHashMap.put("POJAVEXEC_EGL_LIBRARY", str);
        }
        if (lowerCase.contains("zink") || lowerCase.contains("osmesa")) {
            linkedHashMap.put("POJAV_RENDERER", "vulkan_zink");
            linkedHashMap.put("POJAVEXEC_EGL", strSanitizeLibraryName.isEmpty() ? "libOSMesa_8.so" : strSanitizeLibraryName);
            linkedHashMap.put("LIBGL_ES", "3");
            if (strSanitizeLibraryName.isEmpty()) {
                strSanitizeLibraryName = "libOSMesa_8.so";
            }
            linkedHashMap.put("LIB_MESA_NAME", strSanitizeLibraryName);
            linkedHashMap.put("MESA_LOADER_DRIVER_OVERRIDE", "zink");
            linkedHashMap.put("GALLIUM_DRIVER", "zink");
        }
    }

    private static String resolveJvmArgValue(LaunchPlan launchPlan, String str, String str2) {
        for (String str3 : launchPlan.getJvmArgs()) {
            if (str3 != null && str3.startsWith(str)) {
                String strTrim = str3.substring(str.length()).trim();
                if (!strTrim.isEmpty()) {
                    return strTrim;
                }
            }
        }
        return str2;
    }

    private static void setEnv(String str, String str2) {
        if (str2 == null) {
            return;
        }
        try {
            if (str2.isEmpty()) {
                Os.unsetenv(str);
                Logging.i(TAG, "env unset " + str);
            } else {
                Os.setenv(str, str2, true);
                Logging.i(TAG, "env " + str + "=" + str2);
            }
        } catch (Throwable th) {
            Logging.e(TAG, "Failed to update env " + str, th);
        }
    }

    private static String safeEnv(String str) {
        String str2 = Os.getenv(str);
        return str2 != null ? str2 : "";
    }

    private static String inferPojavExecEgl(RendererInterface rendererInterface) {
        String str = rendererInterface.getRendererId().toLowerCase(Locale.ROOT) + " " + rendererInterface.getRendererName().toLowerCase(Locale.ROOT) + " " + sanitizeLibraryName(rendererInterface.getRendererLibrary()).toLowerCase(Locale.ROOT);
        if (isLtwRenderer(rendererInterface)) {
            return "libltw.so";
        }
        if (str.contains("gl4es") || str.contains("opengles") || str.contains("krypton") || str.contains("ng_gl4es")) {
            return "libEGL.so";
        }
        if (str.contains("mobileglues") || str.contains("mobile glues")) {
            return "libmobileglues.so";
        }
        if (str.contains("osmesa") || str.contains("zink") || str.contains("mesa") || str.contains("virgl") || str.contains("freedreno") || str.contains("panfrost")) {
            return sanitizeLibraryName(rendererInterface.getRendererLibrary());
        }
        return "";
    }

    private static boolean isLtwRenderer(RendererInterface rendererInterface) {
        if (rendererInterface == null) {
            return false;
        }
        String lowerCase = (rendererInterface.getUniqueIdentifier() + " " + rendererInterface.getRendererName() + " " + rendererInterface.getRendererId() + " " + rendererInterface.getRendererLibrary() + " " + sanitizeLibraryName(rendererInterface.getRendererEGL())).toLowerCase(Locale.ROOT);
        return lowerCase.contains("ltw") || lowerCase.contains("libltw.so");
    }

    private static void pushLdLibraryPath(RuntimePaths runtimePaths) {
        try {
            JREUtils.setLdLibraryPath(runtimePaths.nativeLinkerPath);
            Logging.i(TAG, "setLdLibraryPath=" + runtimePaths.nativeLinkerPath);
        } catch (Throwable th) {
            Logging.e(TAG, "Failed to push LD_LIBRARY_PATH into native linker", th);
        }
    }

    private static void preloadRuntimeAndGraphics(LaunchPlan launchPlan, RendererInterface rendererInterface, RuntimePaths runtimePaths) {
        dlopenOptional(new File(PathManager.DIR_NATIVE_LIB, "libSDL3.so"));
        dlopenOptional(new File(PathManager.DIR_NATIVE_LIB, "libSDL2.so"));
        dlopenOptional(new File(PathManager.DIR_NATIVE_LIB, "libspirv-cross.so"));
        dlopenOptional(new File(PathManager.DIR_NATIVE_LIB, "libshaderc.so"));
        dlopenOptional(new File(PathManager.DIR_NATIVE_LIB, "libshaderc_shared.so"));
        dlopenOptional(new File(PathManager.DIR_NATIVE_LIB, "liblwjgl_vma.so"));
        dlopenOptional("libzstd-jni_dh-1.5.7-6.so");
        boolean zShouldPrepareAwtNative = shouldPrepareAwtNative(launchPlan);
        if (zShouldPrepareAwtNative) {
            prepareAwtDummyNative(runtimePaths);
        }
        dlopenOptional(new File(runtimePaths.runtimeLibDir, "jli/libjli.so"));
        dlopenOptional(new File(runtimePaths.runtimeLibDir, "libjli.so"));
        dlopenOptional(new File(runtimePaths.jvmLibraryDir, "libjvm.so"));
        dlopenOptional(new File(runtimePaths.runtimeLibDir, "libverify.so"));
        dlopenOptional(new File(runtimePaths.runtimeLibDir, "libjava.so"));
        dlopenOptional(new File(runtimePaths.runtimeLibDir, "libnet.so"));
        dlopenOptional(new File(runtimePaths.runtimeLibDir, "libnio.so"));
        dlopenOptional(new File(runtimePaths.runtimeLibDir, "libawt.so"));
        dlopenOptional(new File(runtimePaths.runtimeLibDir, "libawt_headless.so"));
        if (zShouldPrepareAwtNative) {
            dlopenOptional(new File(runtimePaths.runtimeLibDir, "libawt_xawt.so"));
            dlopenOptional(new File(PathManager.DIR_NATIVE_LIB, "libawt_xawt.so"));
        }
        dlopenOptional(new File(runtimePaths.runtimeLibDir, "libfreetype.so"));
        dlopenOptional(new File(runtimePaths.runtimeLibDir, "libfontmanager.so"));
        Iterator<File> it = listSharedLibraries(runtimePaths.runtimeLibDir).iterator();
        while (it.hasNext()) {
            dlopenOptional(it.next());
        }
        dlopenOptional(new File(PathManager.DIR_NATIVE_LIB, "libopenal.so"));
        dlopenOptional(new File(launchPlan.getLwjglNativeDirectory(), "libopenal.so"));
        if (isOpenGlesWrapperRenderer(rendererInterface)) {
            dlopenOptional("libEGL.so");
            dlopenOptional("libGLESv2.so");
            dlopenOptional("libGLESv3.so");
        }
        Iterator<String> it2 = rendererInterface.getDlopenLibrary().iterator();
        while (it2.hasNext()) {
            dlopenOptional(it2.next());
        }
        String strSanitizeLibraryName = sanitizeLibraryName(rendererInterface.getRendererLibrary());
        if (!strSanitizeLibraryName.isEmpty()) {
            Iterator<File> it3 = rendererInterface.getLibrarySearchPaths().iterator();
            while (it3.hasNext()) {
                dlopenOptional(new File(it3.next(), strSanitizeLibraryName));
            }
            dlopenOptional(new File(PathManager.DIR_NATIVE_LIB, strSanitizeLibraryName));
            dlopenOptional(strSanitizeLibraryName);
        } else {
            Logging.i(TAG, "Skipping renderer preload because renderer library is empty: " + rendererInterface.getRendererName());
        }
        Iterator<File> it4 = listSharedLibraries(launchPlan.getLwjglNativeDirectory()).iterator();
        while (it4.hasNext()) {
            dlopenOptional(it4.next());
        }
    }

    private static boolean isOpenGlesWrapperRenderer(RendererInterface rendererInterface) {
        String str = rendererInterface.getRendererId().toLowerCase(Locale.ROOT) + " " + rendererInterface.getRendererName().toLowerCase(Locale.ROOT) + " " + sanitizeLibraryName(rendererInterface.getRendererLibrary()).toLowerCase(Locale.ROOT);
        return str.contains("opengles") || str.contains("gl4es") || str.contains("ng_gl4es") || str.contains("krypton");
    }

    private static boolean shouldPrepareAwtNative(LaunchPlan launchPlan) {
        for (String str : launchPlan.getJvmArgs()) {
            if (str != null) {
                String lowerCase = str.toLowerCase(Locale.ROOT);
                if (lowerCase.contains("cacio") || lowerCase.startsWith("-dawt.toolkit=") || lowerCase.startsWith("-djava.awt.graphicsenv=")) {
                    return true;
                }
            }
        }
        return isPre16Version(launchPlan.getVersionId());
    }

    private static boolean isPre16Version(String str) {
        String lowerCase = str.trim().toLowerCase(Locale.ROOT);
        if (lowerCase.startsWith("rd-") || lowerCase.startsWith("classic") || lowerCase.startsWith("infdev") || lowerCase.startsWith("indev") || lowerCase.startsWith("a") || lowerCase.startsWith("b")) {
            return true;
        }
        if (!lowerCase.startsWith("1.")) {
            return false;
        }
        int i = -1;
        int i2 = 0;
        int i3 = -1;
        for (String str2 : lowerCase.split("[^0-9]+")) {
            if (str2 != null && !str2.isEmpty()) {
                if (i2 == 0) {
                    try {
                        i3 = Integer.parseInt(str2);
                    } catch (NumberFormatException unused) {
                        continue;
                    }
                }
                if (i2 == 1) {
                    i = Integer.parseInt(str2);
                    break;
                }
                i2++;
            }
        }
        return i3 == 1 && i >= 0 && i < 6;
    }

    private static void prepareAwtDummyNative(RuntimePaths runtimePaths) {
        copyRuntimeNativeIfNeeded(new File(PathManager.DIR_NATIVE_LIB, "libawt_xawt.so"), new File(runtimePaths.runtimeLibDir, "libawt_xawt.so"));
    }

    private static void copyRuntimeNativeIfNeeded(File file, File file2) {
        FileOutputStream fileOutputStream;
        if (!file.isFile()) {
            Logging.i(TAG, "Missing optional native source: " + file.getAbsolutePath());
            return;
        }
        if (file2.isFile() && file2.length() == file.length()) {
            return;
        }
        File parentFile = file2.getParentFile();
        if (parentFile != null && !parentFile.exists() && !parentFile.mkdirs()) {
            Logging.i(TAG, "Unable to create native target folder: " + parentFile.getAbsolutePath());
            return;
        }
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            try {
                fileOutputStream = new FileOutputStream(file2);
            } finally {
            }
            try {
                byte[] bArr = new byte[16384];
                while (true) {
                    int i = fileInputStream.read(bArr);
                    if (i != -1) {
                        fileOutputStream.write(bArr, 0, i);
                    } else {
                        file2.setReadable(true, false);
                        file2.setExecutable(true, false);
                        Logging.i(TAG, "Prepared native " + file.getAbsolutePath() + " -> " + file2.getAbsolutePath());
                        fileOutputStream.close();
                        fileInputStream.close();
                        return;
                    }
                }
            } finally {
            }
        } catch (Throwable th) {
            Logging.e(TAG, "Failed to prepare native " + file2.getAbsolutePath(), th);
        }
    }

    private static List<File> listSharedLibraries(File file) {
        if (!file.isDirectory()) {
            return Collections.emptyList();
        }
        ArrayList arrayList = new ArrayList();
        collectSharedLibraries(file, arrayList);
        arrayList.sort(new Comparator() { // from class: ca.dnamobile.javalauncher.launcher.JavaRuntimeBootstrap$$ExternalSyntheticLambda0
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                return ((File) obj).getAbsolutePath().compareToIgnoreCase(((File) obj2).getAbsolutePath());
            }
        });
        return arrayList;
    }

    private static void collectSharedLibraries(File file, List<File> list) {
        File[] fileArrListFiles = file.listFiles();
        if (fileArrListFiles == null) {
            return;
        }
        for (File file2 : fileArrListFiles) {
            if (file2.isDirectory()) {
                collectSharedLibraries(file2, list);
            } else if (file2.isFile() && file2.getName().endsWith(".so")) {
                list.add(file2);
            }
        }
    }

    private static boolean dlopenOptional(File file) {
        if (file.isFile()) {
            return dlopenOptional(file.getAbsolutePath());
        }
        return false;
    }

    private static boolean dlopenOptional(String str) {
        if (str != null && !str.trim().isEmpty()) {
            try {
                boolean zDlopen = JREUtils.dlopen(str);
                Logging.i(TAG, "dlopen " + str + " = " + zDlopen);
                return zDlopen;
            } catch (Throwable th) {
                Logging.e(TAG, "dlopen failed for " + str, th);
            }
        }
        return false;
    }

    private static String sanitizeLibraryName(String str) {
        if (str == null) {
            return "";
        }
        String strTrim = str.trim();
        if (strTrim.isEmpty() || "null".equalsIgnoreCase(strTrim) || "(null)".equalsIgnoreCase(strTrim)) {
            return "";
        }
        return new File(strTrim).getName();
    }

    private static String findMesaDriverPath(RuntimePaths runtimePaths) {
        ArrayList<File> arrayList = new ArrayList();
        arrayList.add(new File(PathManager.DIR_NATIVE_LIB, "dri"));
        arrayList.add(new File(PathManager.DIR_NATIVE_LIB, "gallium"));
        arrayList.add(new File(PathManager.DIR_NATIVE_LIB));
        for (String str : runtimePaths.ldLibraryPath.split(":")) {
            if (str != null && !str.trim().isEmpty()) {
                File file = new File(str.trim());
                arrayList.add(new File(file, "dri"));
                arrayList.add(new File(file, "gallium"));
            }
        }
        for (File file2 : arrayList) {
            if (file2.isDirectory()) {
                return file2.getAbsolutePath();
            }
        }
        return null;
    }

    private static String findJsphLibrary(RuntimePaths runtimePaths) {
        int runtimeJavaVersion = parseRuntimeJavaVersion(runtimePaths.runtimeHome.getName());
        if (runtimeJavaVersion <= 11) {
            return null;
        }
        final String str = runtimeJavaVersion == 17 ? "libjsph17" : "libjsph21";
        File[] fileArrListFiles = new File(PathManager.DIR_NATIVE_LIB).listFiles(new FilenameFilter() { // from class: ca.dnamobile.javalauncher.launcher.JavaRuntimeBootstrap$$ExternalSyntheticLambda1
            @Override // java.io.FilenameFilter
            public final boolean accept(File file, String str2) {
                return JavaRuntimeBootstrap.lambda$findJsphLibrary$1(str, file, str2);
            }
        });
        if (fileArrListFiles == null || fileArrListFiles.length == 0) {
            return null;
        }
        return fileArrListFiles[0].getAbsolutePath();
    }

    static /* synthetic */ boolean lambda$findJsphLibrary$1(String str, File file, String str2) {
        return str2.startsWith(str) && str2.endsWith(".so");
    }

    private static int parseRuntimeJavaVersion(String str) {
        if (str.contains("25")) {
            return 25;
        }
        if (str.contains("21")) {
            return 21;
        }
        if (str.contains("17")) {
            return 17;
        }
        return str.contains("8") ? 8 : 0;
    }

    public static final class RuntimePaths {
        public final File jvmLibraryDir;
        public final String ldLibraryPath;
        public final String nativeLinkerPath;
        public final File runtimeHome;
        public final File runtimeLibDir;

        private RuntimePaths(File file, File file2, File file3, String str, String str2) {
            this.runtimeHome = file;
            this.runtimeLibDir = file2;
            this.jvmLibraryDir = file3;
            this.ldLibraryPath = str;
            this.nativeLinkerPath = str2;
        }

        static RuntimePaths resolve(Context context, LaunchPlan launchPlan, RendererInterface rendererInterface) {
            File file;
            File runtimeDirectory = launchPlan.getRuntimeDirectory();
            File fileResolveRuntimeLibDir = resolveRuntimeLibDir(runtimeDirectory);
            if (new File(fileResolveRuntimeLibDir, "server/libjvm.so").isFile()) {
                file = new File(fileResolveRuntimeLibDir, "server");
            } else {
                file = new File(fileResolveRuntimeLibDir, "client");
            }
            File file2 = file;
            ArrayList arrayList = new ArrayList();
            addPath(arrayList, file2);
            addPath(arrayList, new File(fileResolveRuntimeLibDir, "jli"));
            addPath(arrayList, fileResolveRuntimeLibDir);
            addPath(arrayList, launchPlan.getLwjglNativeDirectory());
            Iterator<File> it = rendererInterface.getLibrarySearchPaths().iterator();
            while (it.hasNext()) {
                addPath(arrayList, it.next());
            }
            Iterator<File> it2 = DriverPluginManager.getSelectedDriverLibrarySearchPaths(context, rendererInterface).iterator();
            while (it2.hasNext()) {
                addPath(arrayList, it2.next());
            }
            addSystemVendorPaths(arrayList);
            if (PathManager.DIR_RUNTIME_MOD != null) {
                addPath(arrayList, PathManager.DIR_RUNTIME_MOD);
            }
            addPath(arrayList, new File(PathManager.DIR_NATIVE_LIB));
            String strJoinPathList = joinPathList(arrayList);
            String str = file2.getAbsolutePath() + ":" + strJoinPathList;
            Logging.i(JavaRuntimeBootstrap.TAG, "runtimeHome=" + runtimeDirectory.getAbsolutePath());
            Logging.i(JavaRuntimeBootstrap.TAG, "runtimeLibDir=" + fileResolveRuntimeLibDir.getAbsolutePath());
            Logging.i(JavaRuntimeBootstrap.TAG, "jvmLibraryDir=" + file2.getAbsolutePath());
            Logging.i(JavaRuntimeBootstrap.TAG, "LD_LIBRARY_PATH=" + strJoinPathList);
            return new RuntimePaths(runtimeDirectory, fileResolveRuntimeLibDir, file2, strJoinPathList, str);
        }

        private static File resolveRuntimeLibDir(File file) {
            Iterator<String> it = getRuntimeArchCandidates().iterator();
            while (it.hasNext()) {
                File file2 = new File(file, "lib/" + it.next());
                if (file2.isDirectory()) {
                    return file2;
                }
            }
            return new File(file, "lib");
        }

        private static List<String> getRuntimeArchCandidates() {
            ArrayList arrayList = new ArrayList();
            String strArchAsString = Architecture.archAsString(Architecture.getDeviceArchitecture());
            addArchCandidate(arrayList, strArchAsString);
            if (Architecture.getDeviceArchitecture() == 1 || strArchAsString.contains("arm64") || strArchAsString.contains("aarch64")) {
                addArchCandidate(arrayList, "aarch64");
                addArchCandidate(arrayList, "arm64");
                addArchCandidate(arrayList, "arm64-v8a");
            } else if (Architecture.getDeviceArchitecture() == 0 || strArchAsString.contains("arm")) {
                addArchCandidate(arrayList, "arm");
                addArchCandidate(arrayList, "armeabi-v7a");
            } else if (Architecture.getDeviceArchitecture() == 2) {
                addArchCandidate(arrayList, "i386");
                addArchCandidate(arrayList, "i486");
                addArchCandidate(arrayList, "i586");
                addArchCandidate(arrayList, "x86");
            } else if (Architecture.getDeviceArchitecture() == 3 || strArchAsString.contains("x86_64") || strArchAsString.contains("amd64")) {
                addArchCandidate(arrayList, "amd64");
                addArchCandidate(arrayList, "x86_64");
            }
            return arrayList;
        }

        private static void addArchCandidate(List<String> list, String str) {
            if (str == null || str.trim().isEmpty()) {
                return;
            }
            for (String str2 : str.split("/")) {
                if (!str2.trim().isEmpty() && !list.contains(str2)) {
                    list.add(str2);
                }
            }
        }

        private static void addSystemVendorPaths(List<String> list) {
            String str = is64BitDevice() ? "lib64" : "lib";
            list.add("/system/".concat(str));
            list.add("/vendor/".concat(str));
            list.add("/vendor/" + str + "/hw");
        }

        private static boolean is64BitDevice() {
            return Build.SUPPORTED_64_BIT_ABIS != null && Build.SUPPORTED_64_BIT_ABIS.length > 0;
        }

        private static void addPath(List<String> list, File file) {
            if (file.exists()) {
                String absolutePath = file.getAbsolutePath();
                if (list.contains(absolutePath)) {
                    return;
                }
                list.add(absolutePath);
            }
        }

        private static String joinPathList(List<String> list) {
            StringBuilder sb = new StringBuilder();
            for (String str : list) {
                if (str != null && !str.trim().isEmpty()) {
                    if (sb.length() > 0) {
                        sb.append(':');
                    }
                    sb.append(str);
                }
            }
            return sb.toString();
        }
    }
}
