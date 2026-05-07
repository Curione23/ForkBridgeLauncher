package ca.dnamobile.javalauncher.launcher;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.os.EnvironmentCompat;
import ca.dnamobile.javalauncher.BuildConfig;
import ca.dnamobile.javalauncher.data.AccountStore;
import ca.dnamobile.javalauncher.feature.log.Logging;
import ca.dnamobile.javalauncher.instance.LauncherInstance;
import ca.dnamobile.javalauncher.instance.LauncherInstanceManager;
import ca.dnamobile.javalauncher.launcher.InstanceLaunchSettings;
import ca.dnamobile.javalauncher.launcher.JavaGameLauncher;
import ca.dnamobile.javalauncher.logs.LauncherLogManager;
import ca.dnamobile.javalauncher.modcompat.ControlifySDL;
import ca.dnamobile.javalauncher.modcompat.ControllerModCompat;
import ca.dnamobile.javalauncher.modcompat.MethodInjectorAgentInstaller;
import ca.dnamobile.javalauncher.modcompat.VulkanModConfigMitigation;
import ca.dnamobile.javalauncher.modcompat.VulkanModLwjglMitigation;
import ca.dnamobile.javalauncher.renderer.RendererInterface;
import ca.dnamobile.javalauncher.renderer.Renderers;
import ca.dnamobile.javalauncher.settings.LauncherPreferences;
import ca.dnamobile.javalauncher.ui.version.MinecraftVersionInstaller;
import ca.dnamobile.javalauncher.utils.path.PathManager;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.kdt.pojavlaunch.Architecture;
import net.kdt.pojavlaunch.Logger;
import net.kdt.pojavlaunch.Tools;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class LaunchGame {
    private static final AtomicBoolean IS_LAUNCHING = new AtomicBoolean(false);
    private static final String TAG = "LaunchGame";

    public interface StatusListener {
        void onStatus(String str);
    }

    private LaunchGame() {
    }

    public static void resetLaunchState() {
        IS_LAUNCHING.set(false);
    }

    public static int runGame(Context context, String str, AccountStore.Account account, int i, int i2, StatusListener statusListener) throws Exception {
        return runGame(context, str, account, i, i2, null, statusListener);
    }

    public static int runGame(Context context, String str, AccountStore.Account account, int i, int i2, String str2, final StatusListener statusListener) throws Exception {
        JavaGameLauncher.StatusListener statusListener2;
        if (!IS_LAUNCHING.compareAndSet(false, true)) {
            safeAppendLog("LaunchGame: launch request ignored because launch is already in progress");
            return NotificationManagerCompat.IMPORTANCE_UNSPECIFIED;
        }
        try {
            PathManager.initContextConstants(context);
            notify(statusListener, "Preparing launch for " + str + "...");
            LauncherInstance launcherInstanceFindByNameOrId = LauncherInstanceManager.findByNameOrId(context, str);
            String baseVersionId = launcherInstanceFindByNameOrId != null ? launcherInstanceFindByNameOrId.getBaseVersionId() : str;
            File gameDirectory = launcherInstanceFindByNameOrId != null ? launcherInstanceFindByNameOrId.getGameDirectory() : new File(PathManager.DIR_MINECRAFT_HOME);
            if (launcherInstanceFindByNameOrId != null) {
                PathManager.initContextConstants(context, PathManager.inferLauncherHomeFromGameDirectory(gameDirectory));
                gameDirectory = launcherInstanceFindByNameOrId.getGameDirectory();
            }
            LauncherLogManager.beginLatestLog(context, str);
            appendLaunchHeader(context, str, baseVersionId, launcherInstanceFindByNameOrId, account);
            InstanceLaunchSettings.Settings settingsLoad = InstanceLaunchSettings.load(context, InstanceLaunchSettings.resolveInstanceKey(launcherInstanceFindByNameOrId != null ? launcherInstanceFindByNameOrId.getId() : str, launcherInstanceFindByNameOrId != null ? launcherInstanceFindByNameOrId.getName() : str));
            if (settingsLoad.hasAnyOverride()) {
                safeAppendLog("Info: Per-instance launch settings: enabled");
            }
            ensureInstalled(baseVersionId);
            Renderers.reload(context);
            RendererInterface rendererInterfaceResolveRendererForLaunch = resolveRendererForLaunch(context, settingsLoad);
            safeAppendLog("Info: Renderer: " + rendererInterfaceResolveRendererForLaunch.getRendererName() + " (" + rendererInterfaceResolveRendererForLaunch.getRendererId() + ")");
            if (rendererInterfaceResolveRendererForLaunch.isExternalPlugin()) {
                safeAppendLog("Info: Renderer plugin: " + rendererInterfaceResolveRendererForLaunch.getUniqueIdentifier());
            }
            safeAppendLog("Info: Graphics: systemVulkan=" + LauncherPreferences.isUseSystemVulkanDriver(context) + ", forceOpenGL26Plus=" + LauncherPreferences.isUseOpenGlForMinecraft26Plus(context));
            JSONObject versionJson = readVersionJson(baseVersionId);
            int iResolveTargetJava = resolveTargetJava(baseVersionId, versionJson);
            File fileResolveRuntimeDirectory = InstanceLaunchSettings.resolveRuntimeDirectory(settingsLoad, resolveRuntimeDirectory(iResolveTargetJava));
            safeAppendLog("Info: Java: " + iResolveTargetJava + " using " + fileResolveRuntimeDirectory.getName());
            if (settingsLoad.hasRuntimeOverride()) {
                safeAppendLog("Info: Per-instance Java runtime: " + settingsLoad.runtimeName);
            }
            GraphicsBackendHelper.applyBeforeLaunch(context, baseVersionId, versionJson, gameDirectory);
            runPreLaunchModMitigations(gameDirectory);
            notify(statusListener, "Building launch arguments...");
            LaunchPlan launchPlanApplyJvmOverrides = InstanceLaunchSettings.applyJvmOverrides(context, appendMethodInjectorAgentIfNeeded(context, appendQuickPlayArgs(new JavaLaunchBuilder(context, baseVersionId, account, i, i2).setRuntimeDirectory(fileResolveRuntimeDirectory).setGameDirectory(gameDirectory).setRenderer(rendererInterfaceResolveRendererForLaunch).build(), str2, baseVersionId, versionJson, gameDirectory, statusListener), iResolveTargetJava, gameDirectory), settingsLoad);
            notify(statusListener, "Checking controller compatibility...");
            ControlifySDL.initializeIfNeeded(context, gameDirectory);
            ControllerModCompat.prepare(context, gameDirectory);
            if (statusListener == null) {
                statusListener2 = null;
            } else {
                Objects.requireNonNull(statusListener);
                statusListener2 = new JavaGameLauncher.StatusListener() { // from class: ca.dnamobile.javalauncher.launcher.LaunchGame$$ExternalSyntheticLambda3
                    @Override // ca.dnamobile.javalauncher.launcher.JavaGameLauncher.StatusListener
                    public final void onStatus(String str3) {
                        statusListener.onStatus(str3);
                    }
                };
            }
            return JavaGameLauncher.launchPreparedPlan(context, launchPlanApplyJvmOverrides, rendererInterfaceResolveRendererForLaunch, statusListener2);
        } catch (Throwable th) {
            try {
                IS_LAUNCHING.set(false);
                safeAppendLog("LaunchGame failed: " + th);
                if (th instanceof Exception) {
                    throw th;
                }
                throw new RuntimeException(th);
            } finally {
                LauncherLogManager.preserveLatestLogIfEnabled(context, str);
            }
        }
    }

    private static LaunchPlan appendMethodInjectorAgentIfNeeded(Context context, LaunchPlan launchPlan, int i, File file) {
        if (i < 17) {
            return stripMethodInjectorAgent(launchPlan);
        }
        if (!hasVeilOrImguiMod(file)) {
            return stripMethodInjectorAgent(launchPlan);
        }
        File fileInstall = MethodInjectorAgentInstaller.install(context);
        if (fileInstall == null) {
            safeAppendLog("MethodInjectorAgent: missing agent jar; Veil ImGui compatibility patch disabled");
            return stripMethodInjectorAgent(launchPlan);
        }
        ArrayList<String> arrayListBuildVeilImguiCompatibilityJvmArgs = buildVeilImguiCompatibilityJvmArgs(context, fileInstall);
        try {
            LaunchPlan launchPlanPrependJvmArgsToLaunchPlan = prependJvmArgsToLaunchPlan(launchPlan, arrayListBuildVeilImguiCompatibilityJvmArgs);
            safeAppendLog("MethodInjectorAgent: enabled Veil ImGui compatibility JVM args: " + arrayListBuildVeilImguiCompatibilityJvmArgs);
            return launchPlanPrependJvmArgsToLaunchPlan;
        } catch (Throwable th) {
            safeAppendLog("MethodInjectorAgent: unable to add compatibility JVM args to LaunchPlan: " + th);
            safeAppendLog("MethodInjectorAgent: Veil ImGui compatibility patch disabled for this launch");
            return stripMethodInjectorAgent(launchPlan);
        }
    }

    private static LaunchPlan stripMethodInjectorAgent(LaunchPlan launchPlan) {
        try {
            Object objFindJvmArgsObject = findJvmArgsObject(launchPlan);
            if (!(objFindJvmArgsObject instanceof List)) {
                return launchPlan;
            }
            ArrayList arrayList = new ArrayList();
            boolean z = false;
            for (Object obj : (List) objFindJvmArgsObject) {
                if ((obj instanceof String) && isVeilImguiCompatibilityArg((String) obj)) {
                    z = true;
                } else if (obj instanceof String) {
                    arrayList.add((String) obj);
                }
            }
            if (!z) {
                return launchPlan;
            }
            LaunchPlan launchPlanCopyLaunchPlanWithJvmArgs = copyLaunchPlanWithJvmArgs(launchPlan, arrayList);
            if (launchPlanCopyLaunchPlanWithJvmArgs != null) {
                return launchPlanCopyLaunchPlanWithJvmArgs;
            }
            if (replaceJvmArgsField(launchPlan, arrayList)) {
                return launchPlan;
            }
            removeExistingVeilImguiCompatibilityArgs((List) objFindJvmArgsObject);
        } catch (Throwable th) {
            safeAppendLog("MethodInjectorAgent: unable to strip compatibility JVM args: " + th);
        }
        return launchPlan;
    }

    private static boolean hasVeilOrImguiMod(File file) {
        ArrayList arrayList = new ArrayList();
        addModDirectory(arrayList, new File(file, "mods"));
        File parentFile = file.getParentFile();
        if (parentFile != null) {
            addModDirectory(arrayList, new File(parentFile, "mods"));
        }
        if (PathManager.DIR_MINECRAFT_HOME != null && !PathManager.DIR_MINECRAFT_HOME.trim().isEmpty()) {
            addModDirectory(arrayList, new File(PathManager.DIR_MINECRAFT_HOME, "mods"));
        }
        Iterator it = arrayList.iterator();
        while (true) {
            if (!it.hasNext()) {
                return false;
            }
            File[] fileArrListFiles = ((File) it.next()).listFiles(new FilenameFilter() { // from class: ca.dnamobile.javalauncher.launcher.LaunchGame$$ExternalSyntheticLambda0
                @Override // java.io.FilenameFilter
                public final boolean accept(File file2, String str) {
                    return (str == null ? "" : str.toLowerCase(Locale.ROOT)).endsWith(".jar");
                }
            });
            if (fileArrListFiles != null) {
                Arrays.sort(fileArrListFiles, new Comparator() { // from class: ca.dnamobile.javalauncher.launcher.LaunchGame$$ExternalSyntheticLambda1
                    @Override // java.util.Comparator
                    public final int compare(Object obj, Object obj2) {
                        return ((File) obj).getName().compareToIgnoreCase(((File) obj2).getName());
                    }
                });
                for (File file2 : fileArrListFiles) {
                    if (isVeilOrImguiJar(file2)) {
                        safeAppendLog("Info: Veil/ImGui compatibility mod detected");
                        return true;
                    }
                }
            }
        }
    }

    private static boolean isVeilOrImguiJar(File file) {
        String lowerCase;
        String lowerCase2 = file.getName().toLowerCase(Locale.ROOT);
        if (lowerCase2.contains("veil") || lowerCase2.contains("imgui")) {
            return true;
        }
        try {
            ZipFile zipFile = new ZipFile(file);
            try {
                Enumeration<? extends ZipEntry> enumerationEntries = zipFile.entries();
                int i = 0;
                while (enumerationEntries.hasMoreElements()) {
                    int i2 = i + 1;
                    if (i >= 4096) {
                        break;
                    }
                    ZipEntry zipEntryNextElement = enumerationEntries.nextElement();
                    if (zipEntryNextElement.getName() == null) {
                        lowerCase = "";
                    } else {
                        lowerCase = zipEntryNextElement.getName().toLowerCase(Locale.ROOT);
                    }
                    if (!lowerCase.contains("veil") && !lowerCase.contains("imgui") && !lowerCase.contains("foundry/veil")) {
                        if (!zipEntryNextElement.isDirectory() && (lowerCase.endsWith("fabric.mod.json") || lowerCase.endsWith("quilt.mod.json") || lowerCase.endsWith("mods.toml") || lowerCase.endsWith("neoforge.mods.toml"))) {
                            String lowerCase3 = readZipEntryText(zipFile, zipEntryNextElement, 262144).toLowerCase(Locale.ROOT);
                            if (lowerCase3.contains("foundry.veil") || lowerCase3.contains("\"veil\"") || lowerCase3.contains("imgui")) {
                                zipFile.close();
                                return true;
                            }
                        }
                        i = i2;
                    }
                    zipFile.close();
                    return true;
                }
                zipFile.close();
            } finally {
            }
        } catch (Throwable th) {
            safeAppendLog("MethodInjectorAgent: unable to inspect mod jar " + file.getName() + ": " + th);
        }
        return false;
    }

    private static String readZipEntryText(ZipFile zipFile, ZipEntry zipEntry, int i) throws Exception {
        InputStream inputStream = zipFile.getInputStream(zipEntry);
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try {
                byte[] bArr = new byte[8192];
                int i2 = 0;
                do {
                    int i3 = inputStream.read(bArr);
                    if (i3 == -1) {
                        break;
                    }
                    int iMin = Math.min(i3, i - i2);
                    if (iMin > 0) {
                        byteArrayOutputStream.write(bArr, 0, iMin);
                        i2 += iMin;
                    }
                } while (i2 < i);
                String string = byteArrayOutputStream.toString(StandardCharsets.UTF_8.name());
                byteArrayOutputStream.close();
                if (inputStream != null) {
                    inputStream.close();
                }
                return string;
            } finally {
            }
        } catch (Throwable th) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    private static void addModDirectory(ArrayList<File> arrayList, File file) {
        if (!file.isDirectory() || arrayList.contains(file)) {
            return;
        }
        arrayList.add(file);
    }

    private static ArrayList<String> buildVeilImguiCompatibilityJvmArgs(Context context, File file) {
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add("-javaagent:" + file.getAbsolutePath());
        arrayList.add("-Dimgui.library.name=imgui-java");
        return arrayList;
    }

    private static LaunchPlan prependJvmArgsToLaunchPlan(LaunchPlan launchPlan, ArrayList<String> arrayList) throws Exception {
        Object objFindJvmArgsObject = findJvmArgsObject(launchPlan);
        if (!(objFindJvmArgsObject instanceof List)) {
            throw new IllegalStateException("LaunchPlan does not expose a JVM args List");
        }
        ArrayList arrayList2 = new ArrayList();
        List list = (List) objFindJvmArgsObject;
        for (Object obj : list) {
            if (obj instanceof String) {
                String str = (String) obj;
                if (!isVeilImguiCompatibilityArg(str)) {
                    arrayList2.add(str);
                }
            }
        }
        arrayList2.addAll(0, arrayList);
        LaunchPlan launchPlanCopyLaunchPlanWithJvmArgs = copyLaunchPlanWithJvmArgs(launchPlan, arrayList2);
        if (launchPlanCopyLaunchPlanWithJvmArgs != null) {
            return launchPlanCopyLaunchPlanWithJvmArgs;
        }
        if (replaceJvmArgsField(launchPlan, arrayList2)) {
            return launchPlan;
        }
        removeExistingVeilImguiCompatibilityArgs(list);
        list.addAll(0, arrayList);
        return launchPlan;
    }

    private static boolean isMethodInjectorAgentArg(String str) {
        return str != null && str.startsWith("-javaagent:") && str.contains("methods_injector_agent.jar");
    }

    private static boolean isVeilImguiCompatibilityArg(String str) {
        return isMethodInjectorAgentArg(str) || (str != null && str.startsWith("-Dimgui.library.name=")) || (str != null && str.startsWith("-Dimgui.library.path="));
    }

    private static void removeExistingVeilImguiCompatibilityArgs(List<String> list) {
        for (int size = list.size() - 1; size >= 0; size--) {
            if (isVeilImguiCompatibilityArg(list.get(size))) {
                list.remove(size);
            }
        }
    }

    private static Object findJvmArgsObject(LaunchPlan launchPlan) throws Exception {
        Object objInvoke;
        String[] strArr = {"getJvmArgs", "getJavaArgs", "getVmArgs", "getJvmArguments", "getJavaArguments", "jvmArgs", "javaArgs", "vmArgs"};
        for (int i = 0; i < 8; i++) {
            try {
                Method method = launchPlan.getClass().getMethod(strArr[i], new Class[0]);
                method.setAccessible(true);
                objInvoke = method.invoke(launchPlan, new Object[0]);
            } catch (NoSuchMethodException unused) {
            }
            if (objInvoke instanceof List) {
                return objInvoke;
            }
        }
        String[] strArr2 = {"jvmArgs", "javaArgs", "vmArgs", "jvmArguments", "javaArguments"};
        for (int i2 = 0; i2 < 5; i2++) {
            Field fieldFindField = findField(launchPlan.getClass(), strArr2[i2]);
            if (fieldFindField != null) {
                fieldFindField.setAccessible(true);
                Object obj = fieldFindField.get(launchPlan);
                if (obj instanceof List) {
                    return obj;
                }
            }
        }
        return null;
    }

    private static LaunchPlan copyLaunchPlanWithJvmArgs(LaunchPlan launchPlan, ArrayList<String> arrayList) throws Exception {
        String[] strArr = {"copyWithJvmArgs", "copyWithJavaArgs", "copyWithVmArgs", "copyWithJvmArguments", "copyWithJavaArguments", "withJvmArgs", "withJavaArgs", "withVmArgs"};
        for (int i = 0; i < 8; i++) {
            try {
                Method method = launchPlan.getClass().getMethod(strArr[i], List.class);
                method.setAccessible(true);
                Object objInvoke = method.invoke(launchPlan, arrayList);
                if (objInvoke instanceof LaunchPlan) {
                    return (LaunchPlan) objInvoke;
                }
                continue;
            } catch (NoSuchMethodException unused) {
            }
        }
        return null;
    }

    private static boolean replaceJvmArgsField(LaunchPlan launchPlan, ArrayList<String> arrayList) {
        String[] strArr = {"jvmArgs", "javaArgs", "vmArgs", "jvmArguments", "javaArguments"};
        for (int i = 0; i < 5; i++) {
            try {
                Field fieldFindField = findField(launchPlan.getClass(), strArr[i]);
                if (fieldFindField != null) {
                    fieldFindField.setAccessible(true);
                    if (fieldFindField.get(launchPlan) instanceof List) {
                        fieldFindField.set(launchPlan, arrayList);
                        return true;
                    }
                }
            } catch (Throwable unused) {
            }
        }
        return false;
    }

    private static Field findField(Class<?> cls, String str) {
        while (cls != null) {
            try {
                return cls.getDeclaredField(str);
            } catch (NoSuchFieldException unused) {
                cls = cls.getSuperclass();
            }
        }
        return null;
    }

    private static RendererInterface resolveRendererForLaunch(Context context, InstanceLaunchSettings.Settings settings) {
        if (settings.hasRendererOverride()) {
            RendererInterface rendererInterfaceFindRenderer = Renderers.findRenderer(context, settings.rendererIdentifier);
            if (rendererInterfaceFindRenderer != null) {
                safeAppendLog("Info: Per-instance renderer override: " + rendererInterfaceFindRenderer.getRendererName() + " (" + rendererInterfaceFindRenderer.getUniqueIdentifier() + ")");
                return rendererInterfaceFindRenderer;
            }
            safeAppendLog("Info: Per-instance renderer override missing, using global renderer: " + settings.rendererIdentifier);
        }
        return Renderers.getSelectedRenderer(context);
    }

    private static LaunchPlan appendQuickPlayArgs(LaunchPlan launchPlan, String str, String str2, JSONObject jSONObject, File file, StatusListener statusListener) {
        String strTrim = str == null ? "" : str.trim();
        if (strTrim.isEmpty()) {
            return launchPlan;
        }
        QuickPlaySupport quickPlaySupport = getQuickPlaySupport(str2, jSONObject, file);
        if (!quickPlaySupport.supported) {
            notify(statusListener, "Direct world launch is not supported for Minecraft " + str2 + ". Launching Minecraft normally.");
            safeAppendLog("Quick Play skipped for world '" + strTrim + "': " + quickPlaySupport.reason);
            return launchPlan;
        }
        ArrayList arrayList = new ArrayList(launchPlan.getGameArgs());
        removeExistingQuickPlayArgs(arrayList);
        arrayList.add("--quickPlaySingleplayer");
        arrayList.add(strTrim);
        arrayList.add("--quickPlayPath");
        arrayList.add("quickPlay/log.json");
        safeAppendLog("Quick Play enabled for world folder: " + strTrim + " (" + quickPlaySupport.reason + ")");
        return launchPlan.copyWithGameArgs(arrayList);
    }

    private static QuickPlaySupport getQuickPlaySupport(String str, JSONObject jSONObject, File file) {
        if (versionTreeContainsQuickPlayArguments(str, jSONObject, new HashSet())) {
            return QuickPlaySupport.supported("version JSON contains Quick Play arguments");
        }
        if (supportsQuickPlayVersionId(str)) {
            return QuickPlaySupport.supported("version id is Quick Play capable");
        }
        if (supportsQuickPlayVersionId(jSONObject.optString("id", ""))) {
            return QuickPlaySupport.supported("version JSON id is Quick Play capable");
        }
        if (supportsQuickPlayVersionId(jSONObject.optString("inheritsFrom", ""))) {
            return QuickPlaySupport.supported("inherited Minecraft version is Quick Play capable");
        }
        if (hasQuickPlayCompatibilityMod(file)) {
            return QuickPlaySupport.supported("Quick Play compatibility mod detected");
        }
        return QuickPlaySupport.unsupported("vanilla Quick Play starts at Java Edition 1.20; older versions need a compatibility mod");
    }

    private static boolean versionTreeContainsQuickPlayArguments(String str, JSONObject jSONObject, Set<String> set) {
        if (!set.add(str)) {
            return false;
        }
        if (containsQuickPlayToken(jSONObject)) {
            return true;
        }
        String strTrim = jSONObject.optString("inheritsFrom", "").trim();
        if (strTrim.isEmpty()) {
            return false;
        }
        try {
            return versionTreeContainsQuickPlayArguments(strTrim, readVersionJson(strTrim), set);
        } catch (Throwable th) {
            safeAppendLog("Unable to inspect inherited Quick Play args from " + strTrim + ": " + th);
            return false;
        }
    }

    private static boolean containsQuickPlayToken(Object obj) {
        if (obj != null && obj != JSONObject.NULL) {
            if (obj instanceof String) {
                return ((String) obj).toLowerCase(Locale.ROOT).contains("quickplay");
            }
            if (obj instanceof JSONArray) {
                JSONArray jSONArray = (JSONArray) obj;
                for (int i = 0; i < jSONArray.length(); i++) {
                    if (containsQuickPlayToken(jSONArray.opt(i))) {
                        return true;
                    }
                }
                return false;
            }
            if (obj instanceof JSONObject) {
                JSONObject jSONObject = (JSONObject) obj;
                Iterator<String> itKeys = jSONObject.keys();
                while (itKeys.hasNext()) {
                    String next = itKeys.next();
                    if ((next != null && next.toLowerCase(Locale.ROOT).contains("quickplay")) || containsQuickPlayToken(jSONObject.opt(next))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean supportsQuickPlayVersionId(String str) {
        if (str == null) {
            return false;
        }
        String lowerCase = str.trim().toLowerCase(Locale.ROOT);
        if (lowerCase.isEmpty()) {
            return false;
        }
        int[] versionNumbers = parseVersionNumbers(lowerCase);
        if (versionNumbers.length > 0 && versionNumbers[0] >= 26) {
            return true;
        }
        if (lowerCase.matches("^\\d{2}w\\d{2}[a-z].*$")) {
            try {
                int i = Integer.parseInt(lowerCase.substring(0, 2));
                return i > 23 || (i == 23 && Integer.parseInt(lowerCase.substring(3, 5)) >= 14);
            } catch (Throwable unused) {
            }
        }
        int i2 = 0;
        while (true) {
            int i3 = i2 + 1;
            if (i3 >= versionNumbers.length) {
                return false;
            }
            if (versionNumbers[i2] == 1 && versionNumbers[i3] >= 20) {
                return true;
            }
            i2 = i3;
        }
    }

    private static boolean hasQuickPlayCompatibilityMod(File file) {
        File[] fileArrListFiles = new File(file, "mods").listFiles(new FilenameFilter() { // from class: ca.dnamobile.javalauncher.launcher.LaunchGame$$ExternalSyntheticLambda2
            @Override // java.io.FilenameFilter
            public final boolean accept(File file2, String str) {
                return LaunchGame.lambda$hasQuickPlayCompatibilityMod$2(file2, str);
            }
        });
        return fileArrListFiles != null && fileArrListFiles.length > 0;
    }

    static /* synthetic */ boolean lambda$hasQuickPlayCompatibilityMod$2(File file, String str) {
        String lowerCase = str == null ? "" : str.toLowerCase(Locale.ROOT);
        return lowerCase.endsWith(".jar") && (lowerCase.contains("quickplay") || lowerCase.contains("quick-play"));
    }

    private static void removeExistingQuickPlayArgs(ArrayList<String> arrayList) {
        removeOptionAndValue(arrayList, "--quickPlaySingleplayer");
        removeOptionAndValue(arrayList, "--quickPlayMultiplayer");
        removeOptionAndValue(arrayList, "--quickPlayRealms");
        removeOptionAndValue(arrayList, "--quickPlayPath");
    }

    private static void removeOptionAndValue(ArrayList<String> arrayList, String str) {
        int i = 0;
        while (i < arrayList.size()) {
            if (str.equals(arrayList.get(i))) {
                arrayList.remove(i);
                if (i < arrayList.size() && !arrayList.get(i).startsWith("--")) {
                    arrayList.remove(i);
                }
                i--;
            }
            i++;
        }
    }

    private static int[] parseVersionNumbers(String str) {
        String[] strArrSplit = str.replaceAll("[^0-9]+", ".").split("\\.+");
        ArrayList arrayList = new ArrayList();
        for (String str2 : strArrSplit) {
            if (str2 != null && !str2.trim().isEmpty()) {
                try {
                    arrayList.add(Integer.valueOf(Integer.parseInt(str2.trim())));
                } catch (Throwable unused) {
                }
            }
        }
        int[] iArr = new int[arrayList.size()];
        for (int i = 0; i < arrayList.size(); i++) {
            iArr[i] = ((Integer) arrayList.get(i)).intValue();
        }
        return iArr;
    }

    private static final class QuickPlaySupport {
        final String reason;
        final boolean supported;

        private QuickPlaySupport(boolean z, String str) {
            this.supported = z;
            this.reason = str;
        }

        static QuickPlaySupport supported(String str) {
            return new QuickPlaySupport(true, str);
        }

        static QuickPlaySupport unsupported(String str) {
            return new QuickPlaySupport(false, str);
        }
    }

    public static void onJvmExited(Context context, String str, int i) {
        safeAppendLog("Info: Java exit code: " + i);
        LauncherLogManager.preserveLatestLogIfEnabled(context, str);
        ControllerModCompat.reset();
        ControlifySDL.reset();
        resetLaunchState();
    }

    private static void runPreLaunchModMitigations(File file) {
        VulkanModLwjglMitigation.prepare(file);
        VulkanModConfigMitigation.prepare(file);
    }

    private static void ensureInstalled(String str) {
        File file = new File(MinecraftVersionInstaller.getVersionsDirectory(), str);
        File file2 = new File(file, str + ".json");
        File file3 = new File(file, str + ".jar");
        if (!file2.isFile()) {
            throw new IllegalStateException("Missing version json: " + file2.getAbsolutePath());
        }
        if (file3.isFile()) {
            return;
        }
        try {
            String strOptString = readVersionJson(str).optString("inheritsFrom", "");
            if (!strOptString.isEmpty()) {
                File file4 = new File(new File(MinecraftVersionInstaller.getVersionsDirectory(), strOptString), strOptString + ".jar");
                if (file4.isFile()) {
                    return;
                } else {
                    throw new IllegalStateException("Missing inherited client jar: " + file4.getAbsolutePath());
                }
            }
            throw new IllegalStateException("Missing client jar: " + file3.getAbsolutePath());
        } catch (Exception e) {
            if (!(e instanceof IllegalStateException)) {
                throw new IllegalStateException("Unable to check inherited client jar for " + str, e);
            }
            throw ((IllegalStateException) e);
        }
    }

    private static JSONObject readVersionJson(String str) throws Exception {
        FileInputStream fileInputStream = new FileInputStream(new File(new File(MinecraftVersionInstaller.getVersionsDirectory(), str), str + ".json"));
        try {
            JSONObject jSONObject = new JSONObject(Tools.read(fileInputStream));
            fileInputStream.close();
            return jSONObject;
        } catch (Throwable th) {
            try {
                fileInputStream.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    private static int resolveTargetJava(String str, JSONObject jSONObject) {
        JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject("javaVersion");
        if (jSONObjectOptJSONObject != null) {
            int iOptInt = jSONObjectOptJSONObject.optInt("majorVersion", 0);
            if (iOptInt == 0) {
                iOptInt = jSONObjectOptJSONObject.optInt("version", 8);
            }
            return Math.max(8, iOptInt);
        }
        String strOptString = jSONObject.optString("inheritsFrom", "");
        if (!strOptString.isEmpty() && !strOptString.equals(str)) {
            try {
                return resolveTargetJava(strOptString, readVersionJson(strOptString));
            } catch (Throwable th) {
                safeAppendLog("Unable to resolve inherited Java version from " + strOptString + ": " + th);
            }
        }
        return 8;
    }

    private static File resolveRuntimeDirectory(int i) {
        File fileResolveRuntimeForJava = RuntimeCompat.resolveRuntimeForJava(i);
        safeAppendLog("Info: Runtime patch: JRE8_ANDROID_14_16_V31");
        return fileResolveRuntimeForJava;
    }

    private static void notify(StatusListener statusListener, String str) {
        if (statusListener != null) {
            statusListener.onStatus(str);
        }
        Logging.i(TAG, str);
    }

    private static void appendLaunchHeader(Context context, String str, String str2, LauncherInstance launcherInstance, AccountStore.Account account) {
        safeAppendLog("--------- Start launching DroidBridge Launcher");
        safeAppendLog("Info: Launcher: " + getLauncherVersionLabel(context));
        safeAppendLog("Info: Package: " + context.getPackageName());
        safeAppendLog("Info: Device: " + Build.MANUFACTURER + " " + Build.MODEL + " / API " + Build.VERSION.SDK_INT + " / " + Architecture.androidAbiAsString(Architecture.getDeviceArchitecture()));
        if (str.equals(str2)) {
            safeAppendLog("Info: Minecraft: " + str);
        } else {
            safeAppendLog("Info: Minecraft: " + str + " / base " + str2);
        }
        if (launcherInstance != null) {
            safeAppendLog("Info: Instance: " + launcherInstance.getName());
        }
        safeAppendLog("Info: Account: " + getAccountLogLabel(account));
    }

    /* JADX WARN: Removed duplicated region for block: B:20:0x0036  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private static java.lang.String getAccountLogLabel(ca.dnamobile.javalauncher.data.AccountStore.Account r3) {
        /*
            java.lang.String r0 = ")"
            if (r3 != 0) goto L7
            java.lang.String r3 = "None"
            return r3
        L7:
            java.lang.String r1 = r3.getBestDisplayName()     // Catch: java.lang.Throwable -> Lc
            goto Le
        Lc:
            java.lang.String r1 = ""
        Le:
            if (r1 == 0) goto L20
            java.lang.String r2 = r1.trim()
            boolean r2 = r2.isEmpty()
            if (r2 == 0) goto L1b
            goto L20
        L1b:
            java.lang.String r1 = r1.trim()
            goto L22
        L20:
            java.lang.String r1 = "unknown"
        L22:
            boolean r2 = r3.isMicrosoftAccount()     // Catch: java.lang.Throwable -> L36
            if (r2 == 0) goto L36
            boolean r3 = r3.hasMinecraftSession()     // Catch: java.lang.Throwable -> L36
            if (r3 == 0) goto L36
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            java.lang.String r2 = "Microsoft ("
            r3.<init>(r2)
            goto L3d
        L36:
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            java.lang.String r2 = "Offline ("
            r3.<init>(r2)
        L3d:
            java.lang.StringBuilder r3 = r3.append(r1)
            java.lang.StringBuilder r3 = r3.append(r0)
            java.lang.String r3 = r3.toString()
            return r3
        */
        throw new UnsupportedOperationException("Method not decompiled: ca.dnamobile.javalauncher.launcher.LaunchGame.getAccountLogLabel(ca.dnamobile.javalauncher.data.AccountStore$Account):java.lang.String");
    }

    private static String getLauncherVersionLabel(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return "DroidBridge Launcher " + ((packageInfo.versionName == null || packageInfo.versionName.trim().isEmpty()) ? EnvironmentCompat.MEDIA_UNKNOWN : packageInfo.versionName.trim()) + " (" + (Build.VERSION.SDK_INT >= 28 ? packageInfo.getLongVersionCode() : packageInfo.versionCode) + ", " + getBuildChannel(context) + ")";
        } catch (Throwable unused) {
            return "DroidBridge Launcher unknown (" + getBuildChannel(context) + ")";
        }
    }

    private static String getBuildChannel(Context context) {
        String packageName = context.getPackageName();
        return (packageName == null || !packageName.endsWith(".debug")) ? BuildConfig.BUILD_TYPE : "debug";
    }

    private static void safeAppendLog(String str) {
        try {
            Logger.appendToLog(stripTrailingLineBreaks(str));
        } catch (Throwable unused) {
            Logging.i(TAG, str);
        }
    }

    private static String stripTrailingLineBreaks(String str) {
        int length = str.length();
        while (length > 0) {
            char cCharAt = str.charAt(length - 1);
            if (cCharAt != '\n' && cCharAt != '\r') {
                break;
            }
            length--;
        }
        return length == str.length() ? str : str.substring(0, length);
    }
}
