package ca.dnamobile.javalauncher.auth;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.Toast;
import ca.dnamobile.javalauncher.R;
import ca.dnamobile.javalauncher.data.AccountStore;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class OfflineAccessBlocker {
    private OfflineAccessBlocker() {
    }

    public static boolean hasActiveMicrosoftAccount(AccountStore accountStore) {
        return isMicrosoftAccount(loadActiveAccount(accountStore));
    }

    public static boolean hasCompletedMicrosoftLoginOnce(AccountStore accountStore) {
        return true;
    }

    public static boolean canInstallGame(AccountStore accountStore) {
        return true;
    }

    public static boolean canUseOfflineMode(AccountStore accountStore) {
        return true;
    }

    public static boolean requireActiveMicrosoftAccountBeforeInstall(Activity activity, AccountStore accountStore, final Runnable runnable) {
        return false;
    }

    public static boolean requireMicrosoftLoginHistoryBeforeLaunch(Activity activity, AccountStore accountStore, final Runnable runnable) {
        return false;
    }

    public static boolean blockInstallIfNeeded(Activity activity, AccountStore accountStore) {
        return false;
    }

    public static boolean blockOfflineIfNeeded(Activity activity, AccountStore accountStore) {
        return false;
    }

    private static AccountStore.Account loadActiveAccount(AccountStore accountStore) {
        if (accountStore == null) {
            return null;
        }
        try {
            return accountStore.load();
        } catch (Throwable unused) {
            return null;
        }
    }

    private static boolean isMicrosoftAccount(AccountStore.Account account) {
        if (account == null) {
            return false;
        }
        try {
            if (account.isMicrosoftAccount()) {
                return true;
            }
        } catch (Throwable unused) {
        }
        return (isBlank(account.minecraftAccessToken) || "0".equals(account.minecraftAccessToken.trim()) || isBlank(account.minecraftUuid) || isBlank(account.minecraftName)) ? false : true;
    }

    private static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
}
