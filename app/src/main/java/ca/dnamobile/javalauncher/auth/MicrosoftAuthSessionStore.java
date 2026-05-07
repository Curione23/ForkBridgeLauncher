package ca.dnamobile.javalauncher.auth;

import android.content.Context;
import android.content.SharedPreferences;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
final class MicrosoftAuthSessionStore {
    private static final String KEY_AUTH_URL = "auth_url";
    private static final String KEY_CODE_VERIFIER = "code_verifier";
    private static final String KEY_STARTED_AT = "started_at";
    private static final long MAX_SESSION_AGE_MS = 1800000;
    private static final String PREFS = "microsoft_auth_pending_session";

    private MicrosoftAuthSessionStore() {
    }

    static void save(Context context, String str, String str2) {
        context.getApplicationContext().getSharedPreferences(PREFS, 0).edit().putString(KEY_CODE_VERIFIER, str).putString(KEY_AUTH_URL, str2).putLong(KEY_STARTED_AT, System.currentTimeMillis()).apply();
    }

    static Pending load(Context context) {
        SharedPreferences sharedPreferences = context.getApplicationContext().getSharedPreferences(PREFS, 0);
        String string = sharedPreferences.getString(KEY_CODE_VERIFIER, "");
        String string2 = sharedPreferences.getString(KEY_AUTH_URL, "");
        long j = sharedPreferences.getLong(KEY_STARTED_AT, 0L);
        if (!isBlank(string) && !isBlank(string2) && j > 0) {
            long jCurrentTimeMillis = System.currentTimeMillis() - j;
            if (jCurrentTimeMillis < 0 || jCurrentTimeMillis > MAX_SESSION_AGE_MS) {
                clear(context);
            } else {
                return new Pending(string, string2, j);
            }
        }
        return null;
    }

    static void clear(Context context) {
        context.getApplicationContext().getSharedPreferences(PREFS, 0).edit().clear().apply();
    }

    private static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    static final class Pending {
        final String authUrl;
        final String codeVerifier;
        final long startedAt;

        Pending(String str, String str2, long j) {
            this.codeVerifier = str;
            this.authUrl = str2;
            this.startedAt = j;
        }
    }
}
