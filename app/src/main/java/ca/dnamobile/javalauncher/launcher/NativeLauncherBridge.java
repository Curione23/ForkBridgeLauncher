package ca.dnamobile.javalauncher.launcher;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class NativeLauncherBridge {
    private static native int nativeLaunchJvm(String str, String str2, String str3, String[] strArr, String[] strArr2);

    static {
        System.loadLibrary("javalauncher_native");
    }

    private NativeLauncherBridge() {
    }

    public static int launchJvm(LaunchPlan launchPlan) {
        return nativeLaunchJvm(launchPlan.getJavaBinary().getAbsolutePath(), launchPlan.getMainClass(), launchPlan.getGameDirectory().getAbsolutePath(), (String[]) launchPlan.getJvmArgs().toArray(new String[0]), (String[]) launchPlan.getGameArgs().toArray(new String[0]));
    }
}
