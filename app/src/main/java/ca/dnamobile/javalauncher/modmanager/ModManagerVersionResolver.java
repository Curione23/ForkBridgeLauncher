package ca.dnamobile.javalauncher.modmanager;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class ModManagerVersionResolver {
    private static final Pattern RELEASE_VERSION_PATTERN = Pattern.compile("(?<![0-9A-Za-z])([0-9]+(?:\\.[0-9]+){1,3})(?![0-9A-Za-z])");
    private static final Pattern SNAPSHOT_VERSION_PATTERN = Pattern.compile("(?<![0-9A-Za-z])([0-9]{2}w[0-9]{2}[a-z])(?![0-9A-Za-z])", 2);
    private static final Pattern NAMED_SNAPSHOT_PATTERN = Pattern.compile("(?<![0-9A-Za-z])([0-9]+(?:\\.[0-9]+){1,3}-(?:pre|rc)[0-9]+)(?![0-9A-Za-z])", 2);

    private ModManagerVersionResolver() {
    }

    public static String resolveGameVersionForContent(String str) {
        if (str == null) {
            return "";
        }
        String strTrim = str.trim();
        if (strTrim.isEmpty()) {
            return "";
        }
        String strFindNamedSnapshotVersion = findNamedSnapshotVersion(strTrim);
        if (!strFindNamedSnapshotVersion.isEmpty()) {
            return strFindNamedSnapshotVersion;
        }
        String strFindWeeklySnapshotVersion = findWeeklySnapshotVersion(strTrim);
        if (!strFindWeeklySnapshotVersion.isEmpty()) {
            return strFindWeeklySnapshotVersion;
        }
        String strResolveKnownProfileFormat = resolveKnownProfileFormat(strTrim);
        if (!strResolveKnownProfileFormat.isEmpty() && isMinecraftReleaseVersion(strResolveKnownProfileFormat)) {
            return strResolveKnownProfileFormat;
        }
        String strFindFirstMinecraftReleaseVersion = findFirstMinecraftReleaseVersion(strTrim);
        return !strFindFirstMinecraftReleaseVersion.isEmpty() ? strFindFirstMinecraftReleaseVersion : strTrim;
    }

    private static String resolveKnownProfileFormat(String str) {
        String lowerCase = str.toLowerCase(Locale.US);
        if (lowerCase.startsWith("fabric-loader-") || lowerCase.startsWith("quilt-loader-")) {
            String strFindLastMinecraftReleaseVersion = findLastMinecraftReleaseVersion(str);
            if (!strFindLastMinecraftReleaseVersion.isEmpty()) {
                return strFindLastMinecraftReleaseVersion;
            }
        }
        int iIndexOf = lowerCase.indexOf("-forge-");
        if (iIndexOf > 0) {
            String strFindFirstMinecraftReleaseVersion = findFirstMinecraftReleaseVersion(str.substring(0, iIndexOf).trim());
            if (!strFindFirstMinecraftReleaseVersion.isEmpty()) {
                return strFindFirstMinecraftReleaseVersion;
            }
        }
        int iIndexOf2 = lowerCase.indexOf("-neoforge-");
        if (iIndexOf2 > 0) {
            String strFindFirstMinecraftReleaseVersion2 = findFirstMinecraftReleaseVersion(str.substring(0, iIndexOf2).trim());
            return !strFindFirstMinecraftReleaseVersion2.isEmpty() ? strFindFirstMinecraftReleaseVersion2 : "";
        }
        return "";
    }

    private static String findNamedSnapshotVersion(String str) {
        Matcher matcher = NAMED_SNAPSHOT_PATTERN.matcher(str);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static String findWeeklySnapshotVersion(String str) {
        Matcher matcher = SNAPSHOT_VERSION_PATTERN.matcher(str);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static String findFirstMinecraftReleaseVersion(String str) {
        Matcher matcher = RELEASE_VERSION_PATTERN.matcher(str);
        while (matcher.find()) {
            String strGroup = matcher.group(1);
            if (isMinecraftReleaseVersion(strGroup)) {
                return strGroup;
            }
        }
        return "";
    }

    private static String findLastMinecraftReleaseVersion(String str) {
        Matcher matcher = RELEASE_VERSION_PATTERN.matcher(str);
        String str2 = "";
        while (matcher.find()) {
            String strGroup = matcher.group(1);
            if (isMinecraftReleaseVersion(strGroup)) {
                str2 = strGroup;
            }
        }
        return str2;
    }

    private static boolean isMinecraftReleaseVersion(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        String[] strArrSplit = str.trim().split("\\.");
        if (strArrSplit.length < 2) {
            return false;
        }
        try {
            int i = Integer.parseInt(strArrSplit[0]);
            if (i == 1) {
                return true;
            }
            return i >= 20 && i <= 39;
        } catch (NumberFormatException unused) {
            return false;
        }
    }
}
