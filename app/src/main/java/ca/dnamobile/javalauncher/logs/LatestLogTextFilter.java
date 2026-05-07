package ca.dnamobile.javalauncher.logs;

import java.util.ArrayList;
import java.util.Locale;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class LatestLogTextFilter {
    private LatestLogTextFilter() {
    }

    public static String cleanLauncherLine(String str) {
        if (str == null) {
            return null;
        }
        String strNormalizeLauncherLine = normalizeLauncherLine(str);
        if (strNormalizeLauncherLine.isEmpty()) {
            return "";
        }
        if ("Building launch arguments...".equals(strNormalizeLauncherLine) || "Checking controller compatibility...".equals(strNormalizeLauncherLine) || strNormalizeLauncherLine.startsWith("Preparing launch for ") || strNormalizeLauncherLine.startsWith("LaunchGame: launch state reset")) {
            return null;
        }
        return strNormalizeLauncherLine;
    }

    public static String cleanRealtimeLine(String str) {
        if (str == null) {
            return null;
        }
        String strNormalizeLauncherLine = normalizeLauncherLine(str);
        if (strNormalizeLauncherLine.isEmpty()) {
            return "";
        }
        if (isControlifyScanSpam(strNormalizeLauncherLine)) {
            return null;
        }
        return strNormalizeLauncherLine;
    }

    public static String normalizeLauncherLine(String str) {
        if (str == null) {
            return "";
        }
        String strReplace = str.replace("\r\n", "\n").replace('\r', '\n');
        while (strReplace.endsWith("\n")) {
            strReplace = strReplace.substring(0, strReplace.length() - 1);
        }
        return strReplace.trim();
    }

    /* JADX WARN: Removed duplicated region for block: B:18:0x005a  */
    /* JADX WARN: Removed duplicated region for block: B:23:0x0068 A[PHI: r8 r9
      0x0068: PHI (r8v2 boolean) = (r8v1 boolean), (r8v5 boolean) binds: [B:17:0x0058, B:22:0x0066] A[DONT_GENERATE, DONT_INLINE]
      0x0068: PHI (r9v2 boolean) = (r9v1 boolean), (r9v3 boolean) binds: [B:17:0x0058, B:22:0x0066] A[DONT_GENERATE, DONT_INLINE]] */
    /* JADX WARN: Removed duplicated region for block: B:25:0x006e  */
    /* JADX WARN: Removed duplicated region for block: B:27:0x007b  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public static java.lang.String cleanWholeLog(java.lang.String r19) {
        /*
            Method dump skipped, instruction units count: 352
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: ca.dnamobile.javalauncher.logs.LatestLogTextFilter.cleanWholeLog(java.lang.String):java.lang.String");
    }

    private static boolean isDuplicateVulkanModLine(String str, boolean z, boolean z2) {
        if (str.startsWith("VulkanMod mitigation: about to run on ")) {
            return z;
        }
        if ("VulkanMod mitigation: finished".equals(str)) {
            return z2;
        }
        return false;
    }

    private static void flushJreDlopenSummary(ArrayList<String> arrayList, int i) {
        if (i <= 0) {
            return;
        }
        if (i == 1) {
            addLine(arrayList, "Info: JRE native library loaded successfully.");
        } else {
            addLine(arrayList, "Info: JRE native libraries loaded successfully (" + i + " entries collapsed).");
        }
    }

    private static void addLine(ArrayList<String> arrayList, String str) {
        arrayList.add(str);
    }

    private static void addBlankLine(ArrayList<String> arrayList) {
        if (arrayList.isEmpty() || arrayList.get(arrayList.size() - 1).trim().isEmpty()) {
            return;
        }
        arrayList.add("");
    }

    private static boolean isControlifyScanSpam(String str) {
        return str.startsWith("ControlifySDL: scan ");
    }

    private static boolean isJreDlopenSuccess(String str) {
        String lowerCase = str.toLowerCase(Locale.ROOT);
        return lowerCase.contains("d/jrelog") && lowerCase.contains("dlopen") && lowerCase.endsWith("success");
    }

    private static boolean isLwjglMismatchStart(String str) {
        return str.contains("[LWJGL] [ERROR] Incompatible Java and native library versions detected.");
    }

    private static boolean isLwjglMismatchContinuation(String str) {
        if (str.isEmpty()) {
            return true;
        }
        String lowerCase = str.toLowerCase(Locale.ROOT);
        return lowerCase.startsWith("possible reasons:") || lowerCase.startsWith("possible solutions:") || lowerCase.startsWith("a) ") || lowerCase.startsWith("b) ") || lowerCase.startsWith("sure the folder") || lowerCase.startsWith("check the classpath") || lowerCase.contains("-djava.library.path") || lowerCase.contains("shared libraries of an older lwjgl version") || lowerCase.contains("jar files of an older lwjgl version") || lowerCase.contains("jar files of the same lwjgl version");
    }

    private static boolean isNarratorFliteStart(String str) {
        return str.contains("Error while loading the narrator") || str.contains("Failed to load library flite") || str.contains("Unable to load library 'flite'") || str.contains("Native library (linux-aarch64/libflite.so) not found in resource path");
    }

    private static boolean isRendererSetupStart(String str) {
        String lowerCase = str.toLowerCase(Locale.ROOT);
        return lowerCase.startsWith("initializing mobileglues") || lowerCase.startsWith("initialising mobileglues") || lowerCase.startsWith("initializing krypton wrapper") || lowerCase.startsWith("initialising krypton wrapper") || lowerCase.startsWith("initializing gl4es") || lowerCase.startsWith("initialising gl4es") || lowerCase.startsWith("initializing virgl") || lowerCase.startsWith("initialising virgl") || lowerCase.startsWith("initializing renderer") || lowerCase.startsWith("initialising renderer");
    }

    private static boolean isMinecraftOrJvmOutputStart(String str) {
        if (str.isEmpty()) {
            return false;
        }
        return str.matches("^\\[\\d{2}:\\d{2}:\\d{2}\\] \\[.+") || str.startsWith("--------- beginning of main") || str.startsWith("D/jrelog") || str.startsWith("E/jrelog") || str.startsWith("W/jrelog") || str.startsWith("I/jrelog") || str.startsWith("WARNING:") || str.matches("^\\d{4}-\\d{2}-\\d{2}T.+") || str.startsWith("Registered forkAndExec");
    }

    private static boolean isLikelyNewImportantLogLine(String str) {
        if (str.isEmpty()) {
            return false;
        }
        return str.startsWith("[") || str.startsWith("EGLBridge:") || str.startsWith("OpenGL ES Version:") || str.startsWith("Registered forkAndExec") || str.startsWith("OSMDroid:") || str.startsWith("D/jrelog") || str.startsWith("E/jrelog") || str.startsWith("W/jrelog") || str.startsWith("WARNING:") || str.startsWith("Info:") || str.startsWith("VulkanMod mitigation:") || str.startsWith("---------");
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
