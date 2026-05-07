package ca.dnamobile.javalauncher.auth;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import ca.dnamobile.javalauncher.R;
import ca.dnamobile.javalauncher.data.AccountStore;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class LauncherAccountGate {
    private LauncherAccountGate() {
    }

    public static boolean requireMicrosoftLoginBeforeInstall(Activity activity, AccountStore accountStore, final Runnable runnable) {
        if (accountStore.hasMicrosoftLoginCompletedOnce()) {
            return false;
        }
        new AlertDialog.Builder(activity).setTitle(R.string.account_required_title).setMessage(R.string.account_required_before_install_message).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.button_sign_in, new DialogInterface.OnClickListener() { // from class: ca.dnamobile.javalauncher.auth.LauncherAccountGate$$ExternalSyntheticLambda1
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                runnable.run();
            }
        }).show();
        return true;
    }

    public static boolean blockOfflineModeIfNeeded(Activity activity, AccountStore accountStore, final Runnable runnable) {
        if (accountStore.canUseOfflineMode()) {
            return false;
        }
        new AlertDialog.Builder(activity).setTitle(R.string.offline_locked_title).setMessage(R.string.offline_locked_message).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.button_sign_in, new DialogInterface.OnClickListener() { // from class: ca.dnamobile.javalauncher.auth.LauncherAccountGate$$ExternalSyntheticLambda0
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                runnable.run();
            }
        }).show();
        return true;
    }
}
