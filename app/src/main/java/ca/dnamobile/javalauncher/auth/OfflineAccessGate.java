package ca.dnamobile.javalauncher.auth;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.Toast;
import ca.dnamobile.javalauncher.R;
import ca.dnamobile.javalauncher.data.AccountStore;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class OfflineAccessGate {
    private static final boolean BYPASS_LOGIN_GATE_FOR_TESTING = false;

    private static boolean isTestingBypassEnabled() {
        return false;
    }

    private OfflineAccessGate() {
    }

    public static boolean hasActiveMicrosoftAccount(AccountStore accountStore) {
        if (isTestingBypassEnabled()) {
            return true;
        }
        return isMicrosoftAccount(loadActiveAccount(accountStore));
    }

    public static boolean hasCompletedMicrosoftLoginOnce(AccountStore accountStore) {
        if (isTestingBypassEnabled()) {
            return true;
        }
        if (accountStore == null) {
            return false;
        }
        try {
            return accountStore.hasMicrosoftLoginCompletedOnce();
        } catch (Throwable unused) {
            return false;
        }
    }

    public static boolean canInstallGame(AccountStore accountStore) {
        if (isTestingBypassEnabled()) {
            return true;
        }
        return hasActiveMicrosoftAccount(accountStore);
    }

    public static boolean canUseOfflineMode(AccountStore accountStore) {
        return isTestingBypassEnabled() || hasActiveMicrosoftAccount(accountStore) || hasCompletedMicrosoftLoginOnce(accountStore);
    }

    public static boolean requireActiveMicrosoftAccountBeforeInstall(Activity activity, AccountStore accountStore, final Runnable runnable) {
        if (isTestingBypassEnabled() || canInstallGame(accountStore)) {
            return false;
        }
        new AlertDialog.Builder(activity).setTitle(R.string.account_required_title).setMessage(R.string.account_required_before_install_message).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.button_sign_in, new DialogInterface.OnClickListener() { // from class: ca.dnamobile.javalauncher.auth.OfflineAccessGate$$ExternalSyntheticLambda0
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                runnable.run();
            }
        }).show();
        return true;
    }

    public static boolean requireMicrosoftLoginHistoryBeforeLaunch(Activity activity, AccountStore accountStore, final Runnable runnable) {
        if (isTestingBypassEnabled() || canUseOfflineMode(accountStore)) {
            return false;
        }
        new AlertDialog.Builder(activity).setTitle(R.string.offline_locked_title).setMessage(R.string.offline_locked_message).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.button_sign_in, new DialogInterface.OnClickListener() { // from class: ca.dnamobile.javalauncher.auth.OfflineAccessGate$$ExternalSyntheticLambda1
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                runnable.run();
            }
        }).show();
        return true;
    }

    public static boolean blockInstallIfNeeded(Activity activity, AccountStore accountStore) {
        if (isTestingBypassEnabled() || canInstallGame(accountStore)) {
            return false;
        }
        Toast.makeText(activity, R.string.microsoft_login_required_install, 1).show();
        return true;
    }

    public static boolean blockOfflineIfNeeded(Activity activity, AccountStore accountStore) {
        if (isTestingBypassEnabled() || canUseOfflineMode(accountStore)) {
            return false;
        }
        Toast.makeText(activity, R.string.microsoft_login_required_offline, 1).show();
        return true;
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
