package ca.dnamobile.javalauncher.auth;

import android.net.Uri;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Locale;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class MicrosoftAuthConfigPersonal {
    public static final String AUTH_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize";
    private static final String AZURE_AUTHORITY = "https://login.microsoftonline.com/consumers";
    public static final String CALLBACK_SCHEME = "droidbridge";
    public static final String MC_LOGIN_URL = "https://api.minecraftservices.com/authentication/login_with_xbox";
    public static final String MC_PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile";
    public static final String MC_STORE_URL = "https://api.minecraftservices.com/entitlements/mcstore";
    public static final String REDIRECT_URI = "droidbridge://auth";
    public static final String REDIRECT_URL = "droidbridge://auth";
    public static final String SCOPE = "XboxLive.signin offline_access";
    public static final String TOKEN_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
    public static final boolean USE_AZURE_APP_REGISTRATION = true;
    public static final String XBL_AUTH_URL = "https://user.auth.xboxlive.com/user/authenticate";
    public static final String XSTS_AUTH_URL = "https://xsts.auth.xboxlive.com/xsts/authorize";
    private static String pendingCodeVerifier;
    public static final String CLIENT_ID = resolveClientId();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private MicrosoftAuthConfigPersonal() {
    }

    public static boolean isConfigured() {
        String str = CLIENT_ID;
        return (isBlank(str) || "CHANGE_ME".equals(str)) ? false : true;
    }

    public static synchronized Uri getAuthorizationUri() {
        String strCreateCodeVerifier;
        strCreateCodeVerifier = createCodeVerifier();
        pendingCodeVerifier = strCreateCodeVerifier;
        return buildAuthorizationUriWithPkce(strCreateCodeVerifier);
    }

    public static synchronized Uri buildAuthorizationUriWithPkce(String str) {
        pendingCodeVerifier = str;
        return Uri.parse(AUTH_URL).buildUpon().appendQueryParameter("client_id", CLIENT_ID).appendQueryParameter("response_type", "code").appendQueryParameter("scope", "XboxLive.signin offline_access").appendQueryParameter("redirect_uri", "droidbridge://auth").appendQueryParameter("response_mode", "query").appendQueryParameter("prompt", "select_account").appendQueryParameter("code_challenge", createCodeChallenge(str)).appendQueryParameter("code_challenge_method", "S256").build();
    }

    public static String getRedirectParameterName() {
        return "redirect_uri";
    }

    public static synchronized String requireCodeVerifier() {
        String str = pendingCodeVerifier;
        if (str == null || str.length() == 0) {
            throw new IllegalStateException("Missing PKCE code verifier. Start Microsoft sign-in again.");
        }
        return pendingCodeVerifier;
    }

    public static synchronized void clearCodeVerifier() {
        pendingCodeVerifier = null;
    }

    public static synchronized String getPendingCodeVerifier() {
        return pendingCodeVerifier;
    }

    public static String createCodeVerifier() {
        byte[] bArr = new byte[64];
        SECURE_RANDOM.nextBytes(bArr);
        return base64UrlNoPadding(bArr);
    }

    public static String createCodeChallenge(String str) {
        try {
            return base64UrlNoPadding(MessageDigest.getInstance("SHA-256").digest(str.getBytes(StandardCharsets.US_ASCII)));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create PKCE code challenge", e);
        }
    }

    public static String formatRpsTicket(String str) {
        return str.startsWith("d=") ? str : "d=" + str;
    }

    public static boolean isRedirect(String str) {
        if (str == null) {
            return false;
        }
        String lowerCase = str.toLowerCase(Locale.ROOT);
        return lowerCase.startsWith("droidbridge://auth".toLowerCase(Locale.ROOT)) || lowerCase.startsWith(new StringBuilder().append(CALLBACK_SCHEME.toLowerCase(Locale.ROOT)).append(":").toString());
    }

    public static String extractCode(Uri uri) {
        String queryParameter;
        if (uri == null) {
            return null;
        }
        String queryParameter2 = uri.getQueryParameter("code");
        if (queryParameter2 != null && queryParameter2.length() > 0) {
            return queryParameter2;
        }
        Uri fragment = parseFragment(uri);
        if (fragment == null || (queryParameter = fragment.getQueryParameter("code")) == null || queryParameter.length() <= 0) {
            return null;
        }
        return queryParameter;
    }

    public static String extractError(Uri uri) {
        if (uri == null) {
            return null;
        }
        String queryParameter = uri.getQueryParameter("error_description");
        if (queryParameter == null || queryParameter.length() == 0) {
            queryParameter = uri.getQueryParameter("error");
        }
        if (queryParameter != null && queryParameter.length() > 0) {
            return queryParameter;
        }
        Uri fragment = parseFragment(uri);
        if (fragment != null) {
            String queryParameter2 = fragment.getQueryParameter("error_description");
            if (queryParameter2 == null || queryParameter2.length() == 0) {
                queryParameter2 = fragment.getQueryParameter("error");
            }
            if (queryParameter2 != null && queryParameter2.length() > 0) {
                return queryParameter2;
            }
        }
        return null;
    }

    private static Uri parseFragment(Uri uri) {
        String fragment = uri.getFragment();
        if (fragment == null || fragment.length() == 0) {
            return null;
        }
        return Uri.parse("https://localhost/?" + fragment);
    }

    private static String base64UrlNoPadding(byte[] bArr) {
        return Base64.encodeToString(bArr, 11);
    }

    private static String resolveClientId() {
        String buildConfigString = readBuildConfigString("MICROSOFT_CLIENT_ID");
        if (!isBlank(buildConfigString)) {
            return buildConfigString.trim();
        }
        String buildConfigString2 = readBuildConfigString("AZURE_CLIENT_ID");
        if (!isBlank(buildConfigString2)) {
            return buildConfigString2.trim();
        }
        String buildConfigString3 = readBuildConfigString("MICROSOFT_AUTH_CLIENT_ID");
        return !isBlank(buildConfigString3) ? buildConfigString3.trim() : "";
    }

    private static String readBuildConfigString(String str) {
        try {
            Object obj = Class.forName("ca.dnamobile.javalauncher.BuildConfig").getField(str).get(null);
            if (obj == null) {
                return null;
            }
            String strTrim = String.valueOf(obj).trim();
            if (strTrim.length() != 0) {
                if (!"null".equalsIgnoreCase(strTrim)) {
                    return strTrim;
                }
            }
        } catch (Throwable unused) {
        }
        return null;
    }

    private static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
}
