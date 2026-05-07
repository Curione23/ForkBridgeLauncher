package ca.dnamobile.javalauncher.launcher;

import android.content.Context;
import android.system.Os;
import ca.dnamobile.javalauncher.data.AccountStore;
import ca.dnamobile.javalauncher.feature.log.Logging;
import ca.dnamobile.javalauncher.launcher.LaunchGame;
import ca.dnamobile.javalauncher.modcompat.SableRapierSupport;
import ca.dnamobile.javalauncher.renderer.RendererInterface;
import ca.dnamobile.javalauncher.utils.path.PathManager;
import com.oracle.dalvik.VMLauncher;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import net.kdt.pojavlaunch.Architecture;
import net.kdt.pojavlaunch.utils.JREUtils;
import org.json.JSONObject;
import org.lwjgl.glfw.CallbackBridge;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class JavaGameLauncher {
    private static final boolean ENABLE_NATIVE_EXIT_HOOK = true;
    private static final long INSTALLER_HEARTBEAT_SECONDS = 5;
    private static final long INSTALLER_MAIN_TIMEOUT_MINUTES = 15;
    private static final String TAG = "JavaGameLauncher";

    public interface RawJavaProgressListener {
        void onProgress(int i, String str);
    }

    public interface StatusListener {
        void onStatus(String str);
    }

    private JavaGameLauncher() {
    }

    public static int launch(Context context, String str, AccountStore.Account account, int i, int i2, final StatusListener statusListener) throws Exception {
        LaunchGame.StatusListener statusListener2;
        if (statusListener == null) {
            statusListener2 = null;
        } else {
            Objects.requireNonNull(statusListener);
            statusListener2 = new LaunchGame.StatusListener() { // from class: ca.dnamobile.javalauncher.launcher.JavaGameLauncher$$ExternalSyntheticLambda1
                @Override // ca.dnamobile.javalauncher.launcher.LaunchGame.StatusListener
                public final void onStatus(String str2) {
                    statusListener.onStatus(str2);
                }
            };
        }
        return LaunchGame.runGame(context, str, account, i, i2, statusListener2);
    }

    public static int launchPreparedPlan(Context context, LaunchPlan launchPlan, RendererInterface rendererInterface, StatusListener statusListener) throws Exception {
        PathManager.initContextConstants(context);
        notify(statusListener, "Preparing " + rendererInterface.getRendererName() + " runtime and native bridge...");
        JavaRuntimeBootstrap.prepare(context, launchPlan, rendererInterface);
        applyRendererSpecificGameOptions(launchPlan, rendererInterface);
        writeOptions(launchPlan);
        setupExitHookIfAvailable(context);
        notify(statusListener, "Starting Minecraft JVM...");
        int iLaunchWithVmLauncher = launchWithVmLauncher(context, launchPlan);
        LaunchGame.onJvmExited(context, launchPlan.getVersionId(), iLaunchWithVmLauncher);
        notify(statusListener, "Minecraft JVM exited with code " + iLaunchWithVmLauncher + ".");
        return iLaunchWithVmLauncher;
    }

    public static int launchRawJavaArgs(Context context, String str, File file, File file2, List<String> list, final StatusListener statusListener) throws Exception {
        return launchRawJavaArgsWithProgress(context, str, file, file2, list, 81, 88, statusListener == null ? null : new RawJavaProgressListener() { // from class: ca.dnamobile.javalauncher.launcher.JavaGameLauncher$$ExternalSyntheticLambda3
            @Override // ca.dnamobile.javalauncher.launcher.JavaGameLauncher.RawJavaProgressListener
            public final void onProgress(int i, String str2) {
                statusListener.onStatus(str2);
            }
        });
    }

    public static int launchRawJavaArgsWithProgress(Context context, String str, File file, File file2, List<String> list, int i, int i2, RawJavaProgressListener rawJavaProgressListener) throws Exception {
        ensureActivePathManager(context);
        File file3 = new File(file, "bin/java");
        if (!file3.isFile()) {
            throw new IllegalStateException("Missing Java binary: " + file3.getAbsolutePath());
        }
        if (!file3.canExecute()) {
            file3.setExecutable(true, false);
        }
        if (!file2.exists() && !file2.mkdirs()) {
            throw new IllegalStateException("Unable to create working directory: " + file2.getAbsolutePath());
        }
        resetInstallerLatestLogs(str, file, file2, list);
        notifyRaw(rawJavaProgressListener, i, "Preparing installer runtime...");
        prepareRawInstallerEnvironment(context, file);
        ArrayList arrayList = new ArrayList();
        arrayList.add("java");
        addInstallerJvmArgIfMissing(arrayList, "-Djava.home=" + file.getAbsolutePath());
        addInstallerJvmArgIfMissing(arrayList, "-Djava.library.path=" + buildInstallerLdLibraryPath(file));
        addInstallerJvmArgIfMissing(arrayList, "-Dsun.boot.library.path=" + buildInstallerLdLibraryPath(file));
        addInstallerJvmArgIfMissing(arrayList, "-Duser.dir=" + file2.getAbsolutePath());
        arrayList.addAll(list);
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            logJvmArg((String) it.next());
        }
        try {
            System.setProperty("user.dir", file2.getAbsolutePath());
            safeAppendLog("Installer user.dir=" + file2.getAbsolutePath());
            safeAppendLog("Installer native chdir skipped; external Java process owns its cwd.");
        } catch (Throwable th) {
            Logging.e(TAG, "installer working directory setup failed; launch will continue", th);
            safeAppendLog("installer working directory setup failed: " + th);
        }
        notifyRaw(rawJavaProgressListener, Math.min(100, i + 1), "Starting installer JVM...");
        int i3 = i + 2;
        int iLaunchInstallerProcessWithFallback = launchInstallerProcessWithFallback(context, str, file3, file2, file, arrayList, Math.min(100, i3), Math.max(i3, Math.min(100, i2 - 1)), rawJavaProgressListener);
        notifyRaw(rawJavaProgressListener, i2, "Installer JVM exited with code " + iLaunchInstallerProcessWithFallback + ".");
        return iLaunchInstallerProcessWithFallback;
    }

    private static int launchInstallerProcessWithFallback(Context context, String str, File file, File file2, File file3, List<String> list, int i, int i2, RawJavaProgressListener rawJavaProgressListener) throws Exception {
        try {
            int iLaunchInstallerProcess = launchInstallerProcess(context, str, file, file2, file3, list, false, i, i2, rawJavaProgressListener);
            if (iLaunchInstallerProcess != 126 && iLaunchInstallerProcess != 127) {
                return iLaunchInstallerProcess;
            }
            safeAppendLog("Installer direct Java process exited with " + iLaunchInstallerProcess + "; retrying through Android system linker.");
        } catch (IOException e) {
            safeAppendLog("Installer direct Java exec failed: " + e);
        }
        File fileResolveSystemLinker = resolveSystemLinker();
        if (fileResolveSystemLinker == null || !fileResolveSystemLinker.isFile()) {
            throw new IOException("Direct Java start was blocked and no Android system linker was found.");
        }
        notifyRaw(rawJavaProgressListener, i, "Direct Java start was blocked. Retrying through Android system linker...");
        safeAppendLog("Retrying installer through system linker: " + fileResolveSystemLinker.getAbsolutePath());
        return launchInstallerProcess(context, str, file, file2, file3, list, true, i, i2, rawJavaProgressListener);
    }

    private static int launchInstallerProcess(Context context, final String str, File file, File file2, File file3, List<String> list, boolean z, int i, int i2, RawJavaProgressListener rawJavaProgressListener) throws Exception {
        ArrayList arrayList = new ArrayList();
        if (z) {
            File fileResolveSystemLinker = resolveSystemLinker();
            if (fileResolveSystemLinker == null || !fileResolveSystemLinker.isFile()) {
                throw new IOException("No Android system linker found for installer fallback.");
            }
            arrayList.add(fileResolveSystemLinker.getAbsolutePath());
            arrayList.add(file.getAbsolutePath());
        } else {
            arrayList.add(file.getAbsolutePath());
        }
        for (int i3 = 1; i3 < list.size(); i3++) {
            arrayList.add(list.get(i3));
        }
        ProcessBuilder processBuilder = new ProcessBuilder(arrayList);
        processBuilder.directory(file2);
        processBuilder.redirectErrorStream(true);
        Map<String, String> mapEnvironment = processBuilder.environment();
        sanitizeInstallerChildEnvironment(mapEnvironment);
        mapEnvironment.put("JAVA_HOME", file3.getAbsolutePath());
        mapEnvironment.put("HOME", PathManager.DIR_MINECRAFT_HOME);
        mapEnvironment.put("TMPDIR", PathManager.DIR_CACHE.getAbsolutePath());
        mapEnvironment.put("LD_LIBRARY_PATH", buildInstallerLdLibraryPath(file3));
        File fileResolveHeapTaggingPreloadLibrary = resolveHeapTaggingPreloadLibrary(context);
        if (fileResolveHeapTaggingPreloadLibrary.isFile()) {
            mapEnvironment.put("LD_PRELOAD", fileResolveHeapTaggingPreloadLibrary.getAbsolutePath());
            safeAppendLog("Installer child LD_PRELOAD=" + fileResolveHeapTaggingPreloadLibrary.getAbsolutePath());
        } else {
            mapEnvironment.remove("LD_PRELOAD");
            safeAppendLog("Installer child LD_PRELOAD disabled; missing " + fileResolveHeapTaggingPreloadLibrary.getAbsolutePath());
        }
        String str2 = mapEnvironment.get("PATH");
        String absolutePath = new File(file3, "bin").getAbsolutePath();
        if (str2 != null && !str2.isEmpty()) {
            absolutePath = absolutePath + ":" + str2;
        }
        mapEnvironment.put("PATH", absolutePath);
        String str3 = z ? "system-linker" : "direct-exec";
        Logging.i(TAG, "Installer process launchMode=" + str3 + " command=" + arrayList);
        Logging.i(TAG, "Installer process cwd=" + file2.getAbsolutePath());
        Logging.i(TAG, "Installer process LD_LIBRARY_PATH=" + mapEnvironment.get("LD_LIBRARY_PATH"));
        safeAppendLog("Installer process launchMode=".concat(str3));
        safeAppendLog("Installer process command=" + arrayList);
        safeAppendLog("Installer process cwd=" + file2.getAbsolutePath());
        safeAppendLog("Installer process LD_LIBRARY_PATH=" + mapEnvironment.get("LD_LIBRARY_PATH"));
        final Process processStart = processBuilder.start();
        safeAppendLog("Installer process started: " + str + " mode=" + str3);
        Thread thread = new Thread(new Runnable() { // from class: ca.dnamobile.javalauncher.launcher.JavaGameLauncher$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                JavaGameLauncher.lambda$launchInstallerProcess$1(processStart, str);
            }
        }, "InstallerOutput-" + str);
        thread.setDaemon(true);
        thread.start();
        long jCurrentTimeMillis = System.currentTimeMillis();
        long millis = TimeUnit.MINUTES.toMillis(INSTALLER_MAIN_TIMEOUT_MINUTES);
        String strFriendlyInstallerName = friendlyInstallerName(str);
        String[] strArr = {"Finalizing " + strFriendlyInstallerName + " install...", "Finalizing " + strFriendlyInstallerName + " install... still working", "Finalizing " + strFriendlyInstallerName + " install... running processors", "Finalizing " + strFriendlyInstallerName + " install... writing version profile", "Finalizing " + strFriendlyInstallerName + " install... checking generated files"};
        int iMax = Math.max(0, Math.min(100, i));
        int iMax2 = Math.max(iMax, Math.min(100, i2));
        notifyRaw(rawJavaProgressListener, iMax, strArr[0] + " (0s)");
        while (!processStart.waitFor(INSTALLER_HEARTBEAT_SECONDS, TimeUnit.SECONDS)) {
            long jCurrentTimeMillis2 = System.currentTimeMillis() - jCurrentTimeMillis;
            long j = jCurrentTimeMillis;
            long seconds = TimeUnit.MILLISECONDS.toSeconds(jCurrentTimeMillis2);
            long j2 = millis;
            long j3 = seconds / INSTALLER_HEARTBEAT_SECONDS;
            String[] strArr2 = strArr;
            notifyRaw(rawJavaProgressListener, ((int) Math.min(Math.max(0, iMax2 - iMax), j3)) + iMax, strArr2[(int) (j3 % ((long) 5))] + " (" + seconds + "s)");
            safeAppendLog("Installer process still running: " + str + " mode=" + str3 + " elapsed=" + seconds + "s");
            if (jCurrentTimeMillis2 >= j2) {
                safeAppendLog("Installer process timed out: " + str + ". Killing child process.");
                processStart.destroy();
                if (!processStart.waitFor(INSTALLER_HEARTBEAT_SECONDS, TimeUnit.SECONDS)) {
                    processStart.destroyForcibly();
                }
                thread.join(5000L);
                throw new IllegalStateException("Installer process timed out after 15 minutes. Check latestlog.txt for command/env details.");
            }
            jCurrentTimeMillis = j;
            strArr = strArr2;
            millis = j2;
        }
        thread.join(5000L);
        int iExitValue = processStart.exitValue();
        safeAppendLog("Installer process finished: " + str + " mode=" + str3 + " exitCode=" + iExitValue);
        return iExitValue;
    }

    static /* synthetic */ void lambda$launchInstallerProcess$1(Process process, String str) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while (true) {
                try {
                    String line = bufferedReader.readLine();
                    if (line != null) {
                        Logging.i(TAG, "[installer:" + str + "] " + line);
                        safeAppendLog("[installer:" + str + "] " + line);
                    } else {
                        bufferedReader.close();
                        return;
                    }
                } finally {
                }
            }
        } catch (Throwable th) {
            Logging.e(TAG, "Installer output reader failed", th);
            safeAppendLog("Installer output reader failed (" + str + "): " + th);
        }
    }

    private static File resolveSystemLinker() {
        File file = new File("/system/bin/linker64");
        if (file.isFile()) {
            return file;
        }
        File file2 = new File("/system/bin/linker");
        if (file2.isFile()) {
            return file2;
        }
        return null;
    }

    private static void ensureActivePathManager(Context context) {
        if (PathManager.DIR_MINECRAFT_HOME == null || PathManager.DIR_MINECRAFT_HOME.trim().isEmpty()) {
            PathManager.initContextConstants(context);
        }
    }

    private static void addInstallerJvmArgIfMissing(ArrayList<String> arrayList, String str) {
        int iIndexOf = str.indexOf(61);
        if (iIndexOf > 0) {
            String strSubstring = str.substring(0, iIndexOf + 1);
            for (String str2 : arrayList) {
                if (str2 != null && str2.startsWith(strSubstring)) {
                    return;
                }
            }
        } else if (arrayList.contains(str)) {
            return;
        }
        arrayList.add(str);
    }

    private static Thread startInstallerHeartbeat(final String str, final AtomicBoolean atomicBoolean, final int i, final int i2, final RawJavaProgressListener rawJavaProgressListener) {
        Thread thread = new Thread(new Runnable() { // from class: ca.dnamobile.javalauncher.launcher.JavaGameLauncher$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                JavaGameLauncher.lambda$startInstallerHeartbeat$2(str, i, i2, atomicBoolean, rawJavaProgressListener);
            }
        }, "InstallerHeartbeat-" + str);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    static /* synthetic */ void lambda$startInstallerHeartbeat$2(String str, int i, int i2, AtomicBoolean atomicBoolean, RawJavaProgressListener rawJavaProgressListener) {
        long jCurrentTimeMillis = System.currentTimeMillis();
        String strFriendlyInstallerName = friendlyInstallerName(str);
        String[] strArr = {"Finalizing " + strFriendlyInstallerName + " install...", "Finalizing " + strFriendlyInstallerName + " install... still working", "Finalizing " + strFriendlyInstallerName + " install... running processors", "Finalizing " + strFriendlyInstallerName + " install... writing version profile", "Finalizing " + strFriendlyInstallerName + " install... checking generated files"};
        int iMax = Math.max(0, Math.min(100, i));
        int iMax2 = Math.max(iMax, Math.min(100, i2));
        while (atomicBoolean.get()) {
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(INSTALLER_HEARTBEAT_SECONDS));
                if (!atomicBoolean.get()) {
                    return;
                }
                long seconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - jCurrentTimeMillis);
                long j = seconds / INSTALLER_HEARTBEAT_SECONDS;
                notifyRaw(rawJavaProgressListener, ((int) Math.min(Math.max(0, iMax2 - iMax), j)) + iMax, strArr[(int) (j % ((long) 5))] + " (" + seconds + "s)");
                safeAppendLog("Installer VMLauncher still running: " + str + " elapsed=" + seconds + "s");
            } catch (InterruptedException unused) {
                return;
            }
        }
    }

    private static String friendlyInstallerName(String str) {
        String lowerCase = str.toLowerCase(Locale.ROOT);
        return lowerCase.contains("neoforge") ? "NeoForge" : lowerCase.contains("forge") ? "Forge" : "loader";
    }

    private static void notifyRaw(RawJavaProgressListener rawJavaProgressListener, int i, String str) {
        if (rawJavaProgressListener != null) {
            rawJavaProgressListener.onProgress(Math.max(0, Math.min(100, i)), str);
        }
        Logging.i(TAG, str);
        safeAppendLog(str);
    }

    private static File resolveHeapTaggingPreloadLibrary(Context context) {
        String str = context.getApplicationInfo() != null ? context.getApplicationInfo().nativeLibraryDir : null;
        if (str != null && !str.isEmpty()) {
            return new File(str, "libdisable_heap_tagging.so");
        }
        String str2 = PathManager.DIR_NATIVE_LIB;
        if (str2 != null && !str2.isEmpty()) {
            return new File(str2, "libdisable_heap_tagging.so");
        }
        return new File("libdisable_heap_tagging.so");
    }

    private static String buildInstallerLdLibraryPath(File file) {
        File fileResolveRuntimeLibDir = resolveRuntimeLibDir(file);
        File fileResolveJvmLibraryDir = resolveJvmLibraryDir(fileResolveRuntimeLibDir);
        StringBuilder sb = new StringBuilder();
        appendPath(sb, fileResolveJvmLibraryDir);
        appendPath(sb, new File(fileResolveRuntimeLibDir, "jli"));
        appendPath(sb, fileResolveRuntimeLibDir);
        String str = PathManager.DIR_NATIVE_LIB;
        if (str != null && !str.isEmpty()) {
            appendPath(sb, new File(str));
        }
        return sb.toString();
    }

    private static String buildInstallerNativeLinkerPath(File file) {
        File fileResolveJvmLibraryDir = resolveJvmLibraryDir(resolveRuntimeLibDir(file));
        String strBuildInstallerLdLibraryPath = buildInstallerLdLibraryPath(file);
        return fileResolveJvmLibraryDir.getAbsolutePath() + (strBuildInstallerLdLibraryPath.isEmpty() ? "" : ":" + strBuildInstallerLdLibraryPath);
    }

    private static File resolveRuntimeLibDir(File file) {
        File file2 = new File(file, "lib");
        Iterator<String> it = getRuntimeArchCandidates().iterator();
        while (it.hasNext()) {
            File file3 = new File(file2, it.next());
            if (file3.isDirectory()) {
                return file3;
            }
        }
        return file2;
    }

    private static File resolveJvmLibraryDir(File file) {
        File file2 = new File(file, "server");
        if (new File(file2, "libjvm.so").isFile()) {
            return file2;
        }
        File file3 = new File(file, "client");
        return new File(file3, "libjvm.so").isFile() ? file3 : file2;
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

    private static void addArchCandidate(ArrayList<String> arrayList, String str) {
        if (str == null || str.trim().isEmpty()) {
            return;
        }
        for (String str2 : str.split("/")) {
            String strTrim = str2.trim();
            if (!strTrim.isEmpty() && !arrayList.contains(strTrim)) {
                arrayList.add(strTrim);
            }
        }
    }

    private static void prepareRawInstallerEnvironment(Context context, File file) {
        sanitizeInstallerProcessEnvironment();
        String strBuildInstallerLdLibraryPath = buildInstallerLdLibraryPath(file);
        setInstallerEnv("JAVA_HOME", file.getAbsolutePath());
        setInstallerEnv("HOME", PathManager.DIR_MINECRAFT_HOME);
        setInstallerEnv("TMPDIR", PathManager.DIR_CACHE.getAbsolutePath());
        setInstallerEnv("LD_LIBRARY_PATH", strBuildInstallerLdLibraryPath);
        File fileResolveHeapTaggingPreloadLibrary = resolveHeapTaggingPreloadLibrary(context);
        unsetInstallerEnv("LD_PRELOAD");
        if (fileResolveHeapTaggingPreloadLibrary.isFile()) {
            Logging.i(TAG, "Installer global LD_PRELOAD disabled; child will use: " + fileResolveHeapTaggingPreloadLibrary.getAbsolutePath());
            safeAppendLog("Installer global LD_PRELOAD disabled; child will use: " + fileResolveHeapTaggingPreloadLibrary.getAbsolutePath());
        } else {
            Logging.i(TAG, "Installer LD_PRELOAD unavailable: " + fileResolveHeapTaggingPreloadLibrary.getAbsolutePath());
            safeAppendLog("Installer LD_PRELOAD unavailable: " + fileResolveHeapTaggingPreloadLibrary.getAbsolutePath());
        }
        String str = System.getenv("PATH");
        String absolutePath = new File(file, "bin").getAbsolutePath();
        if (str != null && !str.isEmpty()) {
            absolutePath = absolutePath + ":" + str;
        }
        setInstallerEnv("PATH", absolutePath);
        Logging.i(TAG, "Installer native linker bridge skipped for Android 16 compatibility.");
        safeAppendLog("Installer native linker bridge skipped for Android 16 compatibility.");
        Logging.i(TAG, "Installer runtime preloading skipped; child Java process will load runtime libs.");
        safeAppendLog("Installer runtime preloading skipped; child Java process will load runtime libs.");
        Logging.i(TAG, "Installer runtimeHome=" + file.getAbsolutePath());
        Logging.i(TAG, "Installer LD_LIBRARY_PATH=" + strBuildInstallerLdLibraryPath);
        safeAppendLog("Installer runtimeHome=" + file.getAbsolutePath());
        safeAppendLog("Installer LD_LIBRARY_PATH=" + strBuildInstallerLdLibraryPath);
    }

    private static void sanitizeInstallerChildEnvironment(Map<String, String> map) {
        String[] strArr = {"CLASSPATH", "JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS", "POJAV_RENDERER", "POJAVEXEC_EGL", "POJAV_EGL_LIBRARY", "POJAVEXEC_EGL_LIBRARY", "POJAV_RENDERER_LIBRARY", "POJAVEXEC_RENDERER", "OSMESA_LIB", "LIB_MESA_NAME", "MESA_LOADER_DRIVER_OVERRIDE", "GALLIUM_DRIVER", "VK_ICD_FILENAMES", "VK_DRIVER_FILES", "DRIVER_PATH", "LIBGL_DRIVERS_PATH", "EGL_DRIVERS_PATH", "LTW_NEVER_FLUSH_BUFFERS", "LTW_COHERENT_DYNAMIC_STORAGE"};
        for (int i = 0; i < 21; i++) {
            map.remove(strArr[i]);
        }
    }

    private static void sanitizeInstallerProcessEnvironment() {
        String[] strArr = {"CLASSPATH", "JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS", "POJAV_RENDERER", "POJAVEXEC_EGL", "POJAV_EGL_LIBRARY", "POJAVEXEC_EGL_LIBRARY", "POJAV_RENDERER_LIBRARY", "POJAVEXEC_RENDERER", "OSMESA_LIB", "LIB_MESA_NAME", "MESA_LOADER_DRIVER_OVERRIDE", "GALLIUM_DRIVER", "VK_ICD_FILENAMES", "VK_DRIVER_FILES", "DRIVER_PATH", "LIBGL_DRIVERS_PATH", "EGL_DRIVERS_PATH", "LTW_NEVER_FLUSH_BUFFERS", "LTW_COHERENT_DYNAMIC_STORAGE"};
        for (int i = 0; i < 21; i++) {
            unsetInstallerEnv(strArr[i]);
        }
    }

    private static void preloadRawInstallerRuntime(File file) {
        File fileResolveRuntimeLibDir = resolveRuntimeLibDir(file);
        File fileResolveJvmLibraryDir = resolveJvmLibraryDir(fileResolveRuntimeLibDir);
        dlopenOptional(new File(fileResolveRuntimeLibDir, "jli/libjli.so"));
        dlopenOptional(new File(fileResolveRuntimeLibDir, "libjli.so"));
        dlopenOptional(new File(fileResolveJvmLibraryDir, "libjvm.so"));
        dlopenOptional(new File(fileResolveRuntimeLibDir, "libverify.so"));
        dlopenOptional(new File(fileResolveRuntimeLibDir, "libjava.so"));
        dlopenOptional(new File(fileResolveRuntimeLibDir, "libzip.so"));
        dlopenOptional(new File(fileResolveRuntimeLibDir, "libnet.so"));
        dlopenOptional(new File(fileResolveRuntimeLibDir, "libnio.so"));
        dlopenOptional(new File(fileResolveRuntimeLibDir, "libawt.so"));
        dlopenOptional(new File(fileResolveRuntimeLibDir, "libawt_headless.so"));
        dlopenOptional(new File(fileResolveRuntimeLibDir, "libfreetype.so"));
        dlopenOptional(new File(fileResolveRuntimeLibDir, "libfontmanager.so"));
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
                Logging.i(TAG, "installer dlopen " + str + " = " + zDlopen);
                safeAppendLog("installer dlopen " + str + " = " + zDlopen);
                return zDlopen;
            } catch (Throwable th) {
                Logging.e(TAG, "installer dlopen failed for " + str, th);
                safeAppendLog("installer dlopen failed for " + str + ": " + th);
            }
        }
        return false;
    }

    private static void setInstallerEnv(String str, String str2) {
        try {
            Os.setenv(str, str2, true);
        } catch (Throwable th) {
            Logging.e(TAG, "Unable to set installer env " + str, th);
            safeAppendLog("Unable to set installer env " + str + ": " + th);
        }
    }

    private static void unsetInstallerEnv(String str) {
        try {
            Os.unsetenv(str);
        } catch (Throwable th) {
            Logging.e(TAG, "Unable to unset installer env " + str, th);
            safeAppendLog("Unable to unset installer env " + str + ": " + th);
        }
    }

    private static void appendPath(StringBuilder sb, File file) {
        if (file.isDirectory()) {
            String absolutePath = file.getAbsolutePath();
            if (containsPath(sb.toString(), absolutePath)) {
                return;
            }
            if (sb.length() > 0) {
                sb.append(':');
            }
            sb.append(absolutePath);
        }
    }

    private static boolean containsPath(String str, String str2) {
        if (str.isEmpty()) {
            return false;
        }
        for (String str3 : str.split(":")) {
            if (str2.equals(str3)) {
                return true;
            }
        }
        return false;
    }

    private static File resolveAnyLwjglNativeDir() {
        String strAndroidAbiAsString = Architecture.androidAbiAsString(Architecture.getDeviceArchitecture());
        File[] fileArr = {new File(PathManager.DIR_FILE, "lwjgl3.4.1/natives/" + strAndroidAbiAsString), new File(PathManager.DIR_FILE, "lwjgl3.3.3/natives/" + strAndroidAbiAsString), new File(PathManager.DIR_FILE, "lwjgl3.4.1/natives/arm64-v8a"), new File(PathManager.DIR_FILE, "lwjgl3.3.3/natives/arm64-v8a")};
        for (int i = 0; i < 4; i++) {
            File file = fileArr[i];
            if (file.isDirectory()) {
                return file;
            }
        }
        File file2 = new File(PathManager.DIR_CACHE, "installer-empty-natives");
        file2.mkdirs();
        return file2;
    }

    private static int launchWithVmLauncher(Context context, LaunchPlan launchPlan) {
        int i;
        ArrayList arrayList = new ArrayList();
        arrayList.add("java");
        SableRapierSupport.addJvmArgsIfNeeded(context, launchPlan, arrayList);
        ArrayList<String> arrayListNormalizeJvmArgsForVmLauncher = normalizeJvmArgsForVmLauncher(launchPlan.getJvmArgs());
        String strResolveMainClassForVmLauncher = resolveMainClassForVmLauncher(launchPlan, arrayListNormalizeJvmArgsForVmLauncher);
        arrayList.addAll(arrayListNormalizeJvmArgsForVmLauncher);
        arrayList.add(strResolveMainClassForVmLauncher);
        arrayList.addAll(launchPlan.getGameArgs());
        int i2 = 0;
        while (i2 < arrayList.size()) {
            String str = (String) arrayList.get(i2);
            if ("--accessToken".equals(str) && (i = i2 + 1) < arrayList.size()) {
                logJvmArg(str);
                logJvmArg("<hidden>");
                i2 = i;
            } else {
                logJvmArg(str);
            }
            i2++;
        }
        try {
            JREUtils.chdir(launchPlan.getGameDirectory().getAbsolutePath());
        } catch (Throwable th) {
            Logging.e(TAG, "chdir failed; launch will continue", th);
            safeAppendLog("chdir failed: " + th);
        }
        return VMLauncher.launchJVM((String[]) arrayList.toArray(new String[0]));
    }

    private static ArrayList<String> normalizeJvmArgsForVmLauncher(List<String> list) {
        ArrayList<String> arrayList = new ArrayList<>(list.size() + 2);
        for (String str : list) {
            if (str != null && !str.trim().isEmpty()) {
                if (looksLikeStandaloneClasspath(str) && !hasClasspathFlagImmediatelyBefore(arrayList)) {
                    arrayList.add("-cp");
                    safeAppendLog("Recovered missing -cp before launch classpath JVM argument.");
                }
                arrayList.add(str);
            }
        }
        return arrayList;
    }

    private static String resolveMainClassForVmLauncher(LaunchPlan launchPlan, ArrayList<String> arrayList) {
        String mainClass = launchPlan.getMainClass();
        if (mainClass == null) {
            mainClass = "";
        }
        String strTrim = mainClass.trim();
        if (!looksLikeStandaloneClasspath(strTrim)) {
            return strTrim;
        }
        if (!classpathAlreadyPresent(arrayList, strTrim)) {
            arrayList.add("-cp");
            arrayList.add(strTrim);
            safeAppendLog("Moved classpath-looking LaunchPlan mainClass back into JVM classpath args.");
        }
        String installedVersionMainClass = readInstalledVersionMainClass(launchPlan.getVersionId());
        if (!installedVersionMainClass.isEmpty() && !looksLikeStandaloneClasspath(installedVersionMainClass)) {
            safeAppendLog("Recovered LaunchPlan mainClass from installed version JSON: " + installedVersionMainClass);
            return installedVersionMainClass;
        }
        throw new IllegalStateException("Launch plan mainClass is a classpath, not a Java class. The version argument builder is dropping or misplacing -cp for " + launchPlan.getVersionId() + ". Send LaunchGame/LaunchPlan builder classes if this still happens.");
    }

    private static boolean classpathAlreadyPresent(ArrayList<String> arrayList, String str) {
        for (int i = 1; i < arrayList.size(); i++) {
            if (isClasspathFlag(arrayList.get(i - 1)) && str.equals(arrayList.get(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasClasspathFlagImmediatelyBefore(ArrayList<String> arrayList) {
        if (arrayList.isEmpty()) {
            return false;
        }
        return isClasspathFlag(arrayList.get(arrayList.size() - 1));
    }

    private static boolean isClasspathFlag(String str) {
        return "-cp".equals(str) || "-classpath".equals(str) || "--class-path".equals(str);
    }

    private static boolean looksLikeStandaloneClasspath(String str) {
        if (str == null) {
            return false;
        }
        String strTrim = str.trim();
        if (strTrim.isEmpty() || strTrim.startsWith("-")) {
            return false;
        }
        boolean zContains = strTrim.contains(".jar");
        boolean z = strTrim.contains("/") || strTrim.contains("\\");
        boolean zContains2 = strTrim.contains(File.pathSeparator);
        boolean z2 = strTrim.contains("/libraries/") || strTrim.contains("\\libraries\\");
        if (zContains && z) {
            return zContains2 || z2;
        }
        return false;
    }

    private static String readInstalledVersionMainClass(String str) {
        try {
            return readMainClassFromVersionJson(new File(new File(new File(PathManager.DIR_MINECRAFT_HOME, "versions"), str), str + ".json"), 0);
        } catch (Throwable th) {
            Logging.e(TAG, "Unable to recover mainClass from installed version JSON", th);
            safeAppendLog("Unable to recover mainClass from installed version JSON: " + th);
            return "";
        }
    }

    private static String readMainClassFromVersionJson(File file, int i) throws Exception {
        if (i > 8 || !file.isFile()) {
            return "";
        }
        JSONObject jSONObject = new JSONObject(readFileString(file));
        String strTrim = jSONObject.optString("mainClass", "").trim();
        if (!strTrim.isEmpty()) {
            return strTrim;
        }
        String strTrim2 = jSONObject.optString("inheritsFrom", "").trim();
        if (strTrim2.isEmpty()) {
            return "";
        }
        return readMainClassFromVersionJson(new File(new File(new File(PathManager.DIR_MINECRAFT_HOME, "versions"), strTrim2), strTrim2 + ".json"), i + 1);
    }

    private static String readFileString(File file) throws Exception {
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            int length = (int) file.length();
            byte[] bArr = new byte[length];
            int i = 0;
            while (i < length) {
                int i2 = fileInputStream.read(bArr, i, length - i);
                if (i2 < 0) {
                    break;
                }
                i += i2;
            }
            String str = new String(bArr, 0, i, StandardCharsets.UTF_8);
            fileInputStream.close();
            return str;
        } catch (Throwable th) {
            try {
                fileInputStream.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    private static void setupExitHookIfAvailable(Context context) {
        try {
            JREUtils.setupExitMethod(context.getApplicationContext());
            safeAppendLog("Native exit method registered.");
            try {
                JREUtils.initializeGameExitHook();
                safeAppendLog("Native exit hook initialized.");
            } catch (Throwable th) {
                Logging.e(TAG, "initializeGameExitHook failed; launch will continue", th);
                safeAppendLog("initializeGameExitHook failed: " + th);
            }
        } catch (Throwable th2) {
            Logging.e(TAG, "setupExitMethod failed; launch will continue", th2);
            safeAppendLog("setupExitMethod failed: " + th2);
        }
    }

    private static void applyRendererSpecificGameOptions(LaunchPlan launchPlan, RendererInterface rendererInterface) {
        if (isLtwRenderer(rendererInterface)) {
            File file = new File(launchPlan.getGameDirectory(), "options.txt");
            try {
                LinkedHashMap<String, String> optionsFile = readOptionsFile(file);
                if (capIntegerOption(optionsFile, "renderDistance", 4) | capIntegerOption(optionsFile, "simulationDistance", 5) | capIntegerOption(optionsFile, "mipmapLevels", 0) | setOption(optionsFile, "graphicsMode", "fast") | setOption(optionsFile, "clouds", "false") | capFloatOption(optionsFile, "entityDistanceScaling", 0.75f) | capIntegerOption(optionsFile, "biomeBlendRadius", 0)) {
                    writeOptionsFile(file, optionsFile);
                    safeAppendLog("LTW safe options applied: renderDistance<=4, simulationDistance<=5, mipmapLevels=0, graphicsMode=fast");
                } else {
                    safeAppendLog("LTW safe options already present.");
                }
            } catch (Throwable th) {
                Logging.e(TAG, "Failed to apply LTW safe options", th);
                safeAppendLog("Failed to apply LTW safe options: " + th);
            }
        }
    }

    private static boolean isLtwRenderer(RendererInterface rendererInterface) {
        if (rendererInterface == null) {
            return false;
        }
        String lowerCase = (String.valueOf(rendererInterface.getUniqueIdentifier()) + " " + String.valueOf(rendererInterface.getRendererName()) + " " + String.valueOf(rendererInterface.getRendererId()) + " " + String.valueOf(rendererInterface.getRendererLibrary()) + " " + String.valueOf(rendererInterface.getRendererEGL())).toLowerCase(Locale.ROOT);
        return lowerCase.contains("ltw") || lowerCase.contains("libltw.so");
    }

    private static LinkedHashMap<String, String> readOptionsFile(File file) throws Exception {
        int iIndexOf;
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<>();
        if (!file.isFile()) {
            return linkedHashMap;
        }
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            int length = (int) file.length();
            byte[] bArr = new byte[length];
            int i = 0;
            while (i < length) {
                int i2 = fileInputStream.read(bArr, i, length - i);
                if (i2 < 0) {
                    break;
                }
                i += i2;
            }
            fileInputStream.close();
            String[] strArrSplit = new String(bArr, StandardCharsets.UTF_8).split("\\r?\\n");
            for (String str : strArrSplit) {
                if (str != null) {
                    String strTrim = str.trim();
                    if (!strTrim.isEmpty() && (iIndexOf = strTrim.indexOf(58)) > 0) {
                        String strTrim2 = strTrim.substring(0, iIndexOf).trim();
                        String strTrim3 = strTrim.substring(iIndexOf + 1).trim();
                        if (!strTrim2.isEmpty()) {
                            linkedHashMap.put(strTrim2, strTrim3);
                        }
                    }
                }
            }
            return linkedHashMap;
        } catch (Throwable th) {
            try {
                fileInputStream.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    private static void writeOptionsFile(File file, LinkedHashMap<String, String> linkedHashMap) throws Exception {
        File parentFile = file.getParentFile();
        if (parentFile != null && !parentFile.exists()) {
            parentFile.mkdirs();
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : linkedHashMap.entrySet()) {
            sb.append(entry.getKey()).append(':').append(entry.getValue()).append('\n');
        }
        FileOutputStream fileOutputStream = new FileOutputStream(file, false);
        try {
            fileOutputStream.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            fileOutputStream.close();
        } catch (Throwable th) {
            try {
                fileOutputStream.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    private static boolean setOption(LinkedHashMap<String, String> linkedHashMap, String str, String str2) {
        String str3 = linkedHashMap.get(str);
        if (str3 != null && str2.equalsIgnoreCase(str3.trim())) {
            return false;
        }
        linkedHashMap.put(str, str2);
        return true;
    }

    private static boolean capIntegerOption(LinkedHashMap<String, String> linkedHashMap, String str, int i) {
        String str2 = linkedHashMap.get(str);
        int i2 = Integer.MAX_VALUE;
        if (str2 != null) {
            try {
                i2 = Integer.parseInt(str2.trim());
            } catch (Throwable unused) {
            }
        }
        if (i2 <= i) {
            return false;
        }
        linkedHashMap.put(str, String.valueOf(i));
        return true;
    }

    private static boolean capFloatOption(LinkedHashMap<String, String> linkedHashMap, String str, float f) {
        String str2 = linkedHashMap.get(str);
        float f2 = Float.MAX_VALUE;
        if (str2 != null) {
            try {
                f2 = Float.parseFloat(str2.trim());
            } catch (Throwable unused) {
            }
        }
        if (f2 <= f) {
            return false;
        }
        linkedHashMap.put(str, trimFloat(f));
        return true;
    }

    private static String trimFloat(float f) {
        long j = (long) f;
        if (f == j) {
            return String.valueOf(j);
        }
        return String.valueOf(f);
    }

    private static void writeOptions(LaunchPlan launchPlan) {
        File file = new File(launchPlan.getGameDirectory(), "options.txt");
        if (file.exists()) {
            return;
        }
        String str = "fullscreen:false\noverrideWidth:" + Math.max(1, CallbackBridge.windowWidth) + "\noverrideHeight:" + Math.max(1, CallbackBridge.windowHeight) + "\nfboEnable:true\n";
        try {
            File parentFile = file.getParentFile();
            if (parentFile != null && !parentFile.exists()) {
                parentFile.mkdirs();
            }
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            try {
                fileOutputStream.write(str.getBytes(StandardCharsets.UTF_8));
                fileOutputStream.close();
            } finally {
            }
        } catch (Throwable th) {
            Logging.e(TAG, "Failed to write default options.txt", th);
            safeAppendLog("Failed to write options.txt: " + th);
        }
    }

    private static void logJvmArg(String str) {
        Logging.i(TAG, "JVMArg: " + str);
        safeAppendLog("JVMArg: " + str);
    }

    private static void notify(StatusListener statusListener, String str) {
        if (statusListener != null) {
            statusListener.onStatus(str);
        }
        Logging.i(TAG, str);
        safeAppendLog(str);
    }

    private static void resetInstallerLatestLogs(String str, File file, File file2, List<String> list) {
        String str2 = "==== JavaLauncher installer log ====\ntask=" + str + "\nruntime=" + file.getAbsolutePath() + "\nworkingDirectory=" + file2.getAbsolutePath() + "\nargs=" + list + "\n";
        writeLogFile(getLauncherLatestLogFile(), str2, false);
        writeLogFile(getMinecraftLatestLogTxtFile(), str2, false);
        writeLogFile(getMinecraftLatestDotLogFile(), str2, false);
        Logging.i(TAG, "Installer latest logs reset for " + str);
    }

    private static void safeAppendLog(String str) {
        String str2 = str.endsWith("\n") ? str : str + "\n";
        if (!writeLogFile(getMinecraftLatestDotLogFile(), str2, true) && !(writeLogFile(getLauncherLatestLogFile(), str2, true) | writeLogFile(getMinecraftLatestLogTxtFile(), str2, true))) {
            Logging.i(TAG, str);
        }
    }

    private static File getLauncherLatestLogFile() {
        return new File(PathManager.DIR_LAUNCHER_LOG, "latestlog.txt");
    }

    private static File getMinecraftLatestLogTxtFile() {
        return new File(new File(PathManager.DIR_MINECRAFT_HOME, "logs"), "latestlog.txt");
    }

    private static File getMinecraftLatestDotLogFile() {
        return new File(new File(PathManager.DIR_MINECRAFT_HOME, "logs"), "latest.log");
    }

    private static boolean writeLogFile(File file, String str, boolean z) {
        try {
            File parentFile = file.getParentFile();
            if (parentFile != null && !parentFile.exists() && !parentFile.mkdirs()) {
                return false;
            }
            FileOutputStream fileOutputStream = new FileOutputStream(file, z);
            try {
                fileOutputStream.write(str.getBytes(StandardCharsets.UTF_8));
                fileOutputStream.close();
                return true;
            } finally {
            }
        } catch (Throwable unused) {
            return false;
        }
    }
}
