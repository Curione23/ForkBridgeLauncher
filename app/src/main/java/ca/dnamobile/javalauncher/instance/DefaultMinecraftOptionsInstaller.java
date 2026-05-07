package ca.dnamobile.javalauncher.instance;

import android.content.Context;
import java.io.File;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class DefaultMinecraftOptionsInstaller {
    private static final String ASSET_BETA_LEGACY = "minecraft_defaults/options-beta-legacy-optional.txt";
    private static final String ASSET_MODERN_1_17_PLUS = "minecraft_defaults/options-modern-1.17-plus.txt";
    private static final String ASSET_RELEASE_1_8_TO_1_16 = "minecraft_defaults/options-release-1.8-to-1.16.txt";
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+)");
    private static final String TAG = "DefaultOptions";

    private DefaultMinecraftOptionsInstaller() {
    }

    public static void installIfMissingForNewInstance(Context context, File file) {
        installIfMissingForNewInstance(context, file, null);
    }

    /* JADX WARN: Removed duplicated region for block: B:52:0x00da A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public static void installIfMissingForNewInstance(android.content.Context r7, java.io.File r8, java.lang.String r9) {
        /*
            Method dump skipped, instruction units count: 263
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: ca.dnamobile.javalauncher.instance.DefaultMinecraftOptionsInstaller.installIfMissingForNewInstance(android.content.Context, java.io.File, java.lang.String):void");
    }

    private static OptionsPreset choosePreset(String str) {
        if (str == null) {
            return OptionsPreset.SKIP_UNKNOWN;
        }
        String lowerCase = str.trim().toLowerCase(Locale.US);
        if (lowerCase.length() == 0) {
            return OptionsPreset.SKIP_UNKNOWN;
        }
        if (lowerCase.startsWith("b") || lowerCase.startsWith("a") || lowerCase.startsWith("rd") || lowerCase.startsWith("c0") || lowerCase.contains("beta") || lowerCase.contains("alpha") || lowerCase.contains("classic") || lowerCase.contains("infdev") || lowerCase.contains("indev")) {
            return OptionsPreset.BETA_LEGACY;
        }
        if (lowerCase.matches("^\\d{2}w\\d{2}[a-z].*")) {
            return OptionsPreset.MODERN_1_17_PLUS;
        }
        int[] iArrExtractFirstThreeNumbers = extractFirstThreeNumbers(lowerCase);
        int i = iArrExtractFirstThreeNumbers[0];
        int i2 = iArrExtractFirstThreeNumbers[1];
        if (i < 0) {
            return OptionsPreset.SKIP_UNKNOWN;
        }
        if (i >= 26) {
            return OptionsPreset.MODERN_1_17_PLUS;
        }
        if (i != 1) {
            return OptionsPreset.SKIP_UNKNOWN;
        }
        if (i2 >= 17) {
            return OptionsPreset.MODERN_1_17_PLUS;
        }
        if (i2 >= 8) {
            return OptionsPreset.RELEASE_1_8_TO_1_16;
        }
        return OptionsPreset.BETA_LEGACY;
    }

    private static int[] extractFirstThreeNumbers(String str) {
        int[] iArr = new int[3];
        iArr[0] = -1;
        iArr[1] = -1;
        iArr[2] = -1;
        Matcher matcher = NUMBER_PATTERN.matcher(str);
        for (int i = 0; matcher.find() && i < 3; i++) {
            try {
                iArr[i] = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException unused) {
                iArr[i] = -1;
            }
        }
        return iArr;
    }

    private enum OptionsPreset {
        SKIP_UNKNOWN("skip", ""),
        BETA_LEGACY("beta-legacy", DefaultMinecraftOptionsInstaller.ASSET_BETA_LEGACY),
        RELEASE_1_8_TO_1_16("release-1.8-to-1.16", DefaultMinecraftOptionsInstaller.ASSET_RELEASE_1_8_TO_1_16),
        MODERN_1_17_PLUS("modern-1.17-plus", DefaultMinecraftOptionsInstaller.ASSET_MODERN_1_17_PLUS);

        final String assetPath;
        final String logName;

        OptionsPreset(String str, String str2) {
            this.logName = str;
            this.assetPath = str2;
        }
    }
}
