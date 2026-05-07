package ca.dnamobile.javalauncher.auth;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.webkit.CookieManager;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewDatabase;
import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.browser.trusted.sharing.ShareTarget;
import ca.dnamobile.javalauncher.data.AccountStore;
import ca.dnamobile.javalauncher.skin.AccountSkinCache;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class MicrosoftAuthManagerPersonal {
    private static final ExecutorService AUTH_EXECUTOR = Executors.newSingleThreadExecutor();
    private final AccountStore accountStore;
    private final ComponentActivity activity;
    private final ActivityResultLauncher<Intent> authLauncher;
    private Listener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface Listener {
        void onError(String str);

        void onSignedIn(AccountStore.Account account);
    }

    public MicrosoftAuthManagerPersonal(Activity activity, AccountStore accountStore) {
        if (!(activity instanceof ComponentActivity)) {
            throw new IllegalArgumentException("MicrosoftAuthManager requires an androidx.activity.ComponentActivity.");
        }
        ComponentActivity componentActivity = (ComponentActivity) activity;
        this.activity = componentActivity;
        this.accountStore = accountStore;
        this.authLauncher = componentActivity.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback() { // from class: ca.dnamobile.javalauncher.auth.MicrosoftAuthManagerPersonal$$ExternalSyntheticLambda5
            @Override // androidx.activity.result.ActivityResultCallback
            public final void onActivityResult(Object obj) {
                this.f$0.lambda$new$0((ActivityResult) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$new$0(ActivityResult activityResult) {
        Intent data = activityResult.getData();
        if (activityResult.getResultCode() != -1 || data == null) {
            MicrosoftAuthSessionStore.clear(this.activity);
            String stringExtra = data != null ? data.getStringExtra(MicrosoftAuthActivity.EXTRA_ERROR) : null;
            if (stringExtra == null || stringExtra.length() <= 0) {
                stringExtra = "Authorization canceled";
            }
            notifyError(stringExtra);
            return;
        }
        String stringExtra2 = data.getStringExtra(MicrosoftAuthActivity.EXTRA_AUTH_CODE);
        if (stringExtra2 == null || stringExtra2.length() == 0) {
            MicrosoftAuthSessionStore.clear(this.activity);
            notifyError("Authorization finished without a code.");
            return;
        }
        String stringExtra3 = data.getStringExtra(MicrosoftAuthActivity.EXTRA_CODE_VERIFIER);
        if (isBlank(stringExtra3)) {
            MicrosoftAuthSessionStore.clear(this.activity);
            notifyError("Authorization finished without a PKCE verifier. Please start Microsoft sign-in again.");
        } else {
            exchangeCodeForMinecraftAccount(stringExtra2, stringExtra3);
        }
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void signIn() {
        if (!MicrosoftAuthConfigPersonal.isConfigured()) {
            notifyError("Microsoft client ID is not configured.");
            return;
        }
        Intent intent = new Intent(this.activity, (Class<?>) MicrosoftAuthActivity.class);
        String strCreateCodeVerifier = MicrosoftAuthConfigPersonal.createCodeVerifier();
        String string = MicrosoftAuthConfigPersonal.buildAuthorizationUriWithPkce(strCreateCodeVerifier).toString();
        MicrosoftAuthSessionStore.save(this.activity, strCreateCodeVerifier, string);
        intent.putExtra(MicrosoftAuthActivity.EXTRA_AUTH_URL, string);
        intent.putExtra(MicrosoftAuthActivity.EXTRA_CODE_VERIFIER, strCreateCodeVerifier);
        this.authLauncher.launch(intent);
    }

    public void signOut() {
        MicrosoftAuthSessionStore.clear(this.activity);
        MicrosoftAuthConfigPersonal.clearCodeVerifier();
        clearStoredMicrosoftAccount();
        clearWebAuthenticationState();
    }

    private void clearStoredMicrosoftAccount() {
        if (invokeNoArgAccountStoreMethod("signOutMicrosoftAccount") || invokeNoArgAccountStoreMethod("clearMicrosoftAccount") || invokeNoArgAccountStoreMethod("clearStoredMicrosoftAccount") || invokeNoArgAccountStoreMethod("deleteStoredMicrosoftAccount")) {
            return;
        }
        try {
            this.accountStore.clear();
        } catch (Throwable unused) {
        }
    }

    private boolean invokeNoArgAccountStoreMethod(String str) {
        try {
            this.accountStore.getClass().getMethod(str, new Class[0]).invoke(this.accountStore, new Object[0]);
            return true;
        } catch (Throwable unused) {
            return false;
        }
    }

    private void clearWebAuthenticationState() {
        this.mainHandler.post(new Runnable() { // from class: ca.dnamobile.javalauncher.auth.MicrosoftAuthManagerPersonal$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$clearWebAuthenticationState$1();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$clearWebAuthenticationState$1() {
        try {
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.removeAllCookies(null);
            cookieManager.removeSessionCookies(null);
            cookieManager.flush();
        } catch (Throwable unused) {
        }
        try {
            WebStorage.getInstance().deleteAllData();
        } catch (Throwable unused2) {
        }
        try {
            WebViewDatabase webViewDatabase = WebViewDatabase.getInstance(this.activity);
            webViewDatabase.clearFormData();
            webViewDatabase.clearHttpAuthUsernamePassword();
        } catch (Throwable unused3) {
        }
        try {
            WebView.clearClientCertPreferences(null);
        } catch (Throwable unused4) {
        }
    }

    public boolean hasLoggedIntoMicrosoftAtLeastOnce() {
        return this.accountStore.hasMicrosoftLoginCompletedOnce();
    }

    public boolean canUseOfflineMode() {
        return this.accountStore.canUseOfflineMode();
    }

    public void dispose() {
        this.listener = null;
    }

    public void refreshMicrosoftAccount() {
        AccountStore.Account accountLoad = this.accountStore.load();
        if (accountLoad == null || !accountLoad.isMicrosoftAccount() || isBlank(accountLoad.refreshToken)) {
            accountLoad = this.accountStore.loadLastMicrosoftAccount();
        }
        if (accountLoad == null || isBlank(accountLoad.refreshToken)) {
            signIn();
        } else {
            final String str = accountLoad.refreshToken;
            AUTH_EXECUTOR.execute(new Runnable() { // from class: ca.dnamobile.javalauncher.auth.MicrosoftAuthManagerPersonal$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.lambda$refreshMicrosoftAccount$3(str);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$refreshMicrosoftAccount$3(String str) {
        try {
            final AccountStore.Account accountLoginWithMicrosoftRefreshToken = loginWithMicrosoftRefreshToken(str);
            this.accountStore.saveMicrosoftAccount(accountLoginWithMicrosoftRefreshToken);
            this.accountStore.markMicrosoftLoginCompletedOnce();
            AccountSkinCache.cacheMicrosoftSkinAsync(this.activity, accountLoginWithMicrosoftRefreshToken);
            this.mainHandler.post(new Runnable() { // from class: ca.dnamobile.javalauncher.auth.MicrosoftAuthManagerPersonal$$ExternalSyntheticLambda6
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.lambda$refreshMicrosoftAccount$2(accountLoginWithMicrosoftRefreshToken);
                }
            });
        } catch (Throwable th) {
            notifyError(th.getMessage() != null ? th.getMessage() : th.toString());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$refreshMicrosoftAccount$2(AccountStore.Account account) {
        Listener listener = this.listener;
        if (listener != null) {
            listener.onSignedIn(account);
        }
    }

    private void exchangeCodeForMinecraftAccount(final String str, final String str2) {
        AUTH_EXECUTOR.execute(new Runnable() { // from class: ca.dnamobile.javalauncher.auth.MicrosoftAuthManagerPersonal$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$exchangeCodeForMinecraftAccount$5(str, str2);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$exchangeCodeForMinecraftAccount$5(String str, String str2) {
        try {
            final AccountStore.Account accountLoginWithMicrosoftCode = loginWithMicrosoftCode(str, str2);
            this.accountStore.saveMicrosoftAccount(accountLoginWithMicrosoftCode);
            this.accountStore.markMicrosoftLoginCompletedOnce();
            MicrosoftAuthSessionStore.clear(this.activity);
            MicrosoftAuthConfigPersonal.clearCodeVerifier();
            AccountSkinCache.cacheMicrosoftSkinAsync(this.activity, accountLoginWithMicrosoftCode);
            this.mainHandler.post(new Runnable() { // from class: ca.dnamobile.javalauncher.auth.MicrosoftAuthManagerPersonal$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.lambda$exchangeCodeForMinecraftAccount$4(accountLoginWithMicrosoftCode);
                }
            });
        } catch (Throwable th) {
            MicrosoftAuthSessionStore.clear(this.activity);
            MicrosoftAuthConfigPersonal.clearCodeVerifier();
            notifyError(th.getMessage() != null ? th.getMessage() : th.toString());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$exchangeCodeForMinecraftAccount$4(AccountStore.Account account) {
        Listener listener = this.listener;
        if (listener != null) {
            listener.onSignedIn(account);
        }
    }

    private AccountStore.Account loginWithMicrosoftCode(String str, String str2) throws JSONException, IOException {
        return loginWithLiveTokens(acquireLiveAccessToken(false, str, str2), "");
    }

    private AccountStore.Account loginWithMicrosoftRefreshToken(String str) throws JSONException, IOException {
        return loginWithLiveTokens(acquireLiveAccessToken(true, str, null), str);
    }

    private AccountStore.Account loginWithLiveTokens(LiveTokens liveTokens, String str) throws JSONException, IOException {
        if (liveTokens.refreshToken.length() > 0) {
            str = liveTokens.refreshToken;
        }
        String str2 = str;
        XstsToken xstsTokenAcquireXstsToken = acquireXstsToken(acquireXblToken(liveTokens.accessToken));
        String strAcquireMinecraftToken = acquireMinecraftToken(xstsTokenAcquireXstsToken.userHash, xstsTokenAcquireXstsToken.token);
        fetchOwnedItemsQuietly(strAcquireMinecraftToken);
        MinecraftProfile minecraftProfileFetchMinecraftProfile = fetchMinecraftProfile(strAcquireMinecraftToken);
        String str3 = minecraftProfileFetchMinecraftProfile.name.length() > 0 ? minecraftProfileFetchMinecraftProfile.name : "Microsoft Player";
        return new AccountStore.Account("", str3, "", strAcquireMinecraftToken, str2, strAcquireMinecraftToken, str3, minecraftProfileFetchMinecraftProfile.uuidWithDashes, xstsTokenAcquireXstsToken.userHash, minecraftProfileFetchMinecraftProfile.skinUrl, minecraftProfileFetchMinecraftProfile.skinVariant);
    }

    private LiveTokens acquireLiveAccessToken(boolean z, String str, String str2) throws JSONException, IOException {
        try {
            JSONObject jSONObjectPostForm = postForm(MicrosoftAuthConfigPersonal.TOKEN_URL, createTokenRequestFormData(z, str, str2));
            return new LiveTokens(jSONObjectPostForm.getString("access_token"), jSONObjectPostForm.optString("refresh_token", ""));
        } finally {
            if (!z) {
                MicrosoftAuthConfigPersonal.clearCodeVerifier();
            }
        }
    }

    private String createTokenRequestFormData(boolean z, String str, String str2) throws IOException {
        if (z) {
            return toFormData("client_id", MicrosoftAuthConfigPersonal.CLIENT_ID, "grant_type", "refresh_token", "refresh_token", str, "scope", "XboxLive.signin offline_access");
        }
        return toFormData("client_id", MicrosoftAuthConfigPersonal.CLIENT_ID, "grant_type", "authorization_code", "code", str, "redirect_uri", "droidbridge://auth", "scope", "XboxLive.signin offline_access", "code_verifier", !isBlank(str2) ? str2 : MicrosoftAuthConfigPersonal.requireCodeVerifier());
    }

    private String acquireXblToken(String str) throws JSONException, IOException {
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("AuthMethod", "RPS");
        jSONObject.put("SiteName", "user.auth.xboxlive.com");
        jSONObject.put("RpsTicket", MicrosoftAuthConfigPersonal.formatRpsTicket(str));
        JSONObject jSONObject2 = new JSONObject();
        jSONObject2.put("Properties", jSONObject);
        jSONObject2.put("RelyingParty", "http://auth.xboxlive.com");
        jSONObject2.put("TokenType", "JWT");
        return postJson(MicrosoftAuthConfigPersonal.XBL_AUTH_URL, jSONObject2).getString("Token");
    }

    private XstsToken acquireXstsToken(String str) throws JSONException, IOException {
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("SandboxId", "RETAIL");
        jSONObject.put("UserTokens", new JSONArray((Collection) Collections.singleton(str)));
        JSONObject jSONObject2 = new JSONObject();
        jSONObject2.put("Properties", jSONObject);
        jSONObject2.put("RelyingParty", "rp://api.minecraftservices.com/");
        jSONObject2.put("TokenType", "JWT");
        JSONObject jSONObjectPostJson = postJson(MicrosoftAuthConfigPersonal.XSTS_AUTH_URL, jSONObject2);
        return new XstsToken(jSONObjectPostJson.getJSONObject("DisplayClaims").getJSONArray("xui").getJSONObject(0).getString("uhs"), jSONObjectPostJson.getString("Token"));
    }

    private String acquireMinecraftToken(String str, String str2) throws JSONException, IOException {
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("identityToken", "XBL3.0 x=" + str + ";" + str2);
        return postJson(MicrosoftAuthConfigPersonal.MC_LOGIN_URL, jSONObject).getString("access_token");
    }

    private void fetchOwnedItemsQuietly(String str) {
        HttpURLConnection httpURLConnectionOpenConnection = null;
        try {
            httpURLConnectionOpenConnection = openConnection(MicrosoftAuthConfigPersonal.MC_STORE_URL);
            httpURLConnectionOpenConnection.setRequestProperty("Authorization", "Bearer " + str);
            int responseCode = httpURLConnectionOpenConnection.getResponseCode();
            readFully((responseCode < 200 || responseCode >= 300) ? httpURLConnectionOpenConnection.getErrorStream() : httpURLConnectionOpenConnection.getInputStream());
            if (httpURLConnectionOpenConnection == null) {
                return;
            }
        } catch (Throwable unused) {
            if (httpURLConnectionOpenConnection == null) {
                return;
            }
        }
        httpURLConnectionOpenConnection.disconnect();
    }

    private MinecraftProfile fetchMinecraftProfile(String str) throws JSONException, IOException {
        InputStream errorStream;
        HttpURLConnection httpURLConnectionOpenConnection = openConnection(MicrosoftAuthConfigPersonal.MC_PROFILE_URL);
        httpURLConnectionOpenConnection.setRequestProperty("Authorization", "Bearer " + str);
        int responseCode = httpURLConnectionOpenConnection.getResponseCode();
        if (responseCode >= 200 && responseCode < 300) {
            errorStream = httpURLConnectionOpenConnection.getInputStream();
        } else {
            errorStream = httpURLConnectionOpenConnection.getErrorStream();
        }
        String fully = readFully(errorStream);
        httpURLConnectionOpenConnection.disconnect();
        if (responseCode < 200 || responseCode >= 300) {
            if (responseCode == 404) {
                throw new IOException("This Microsoft account does not own Minecraft: Java Edition.");
            }
            throw new IOException("Minecraft profile request failed: HTTP " + responseCode + " " + fully);
        }
        JSONObject jSONObject = new JSONObject(fully);
        JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("skins");
        String str2 = "classic";
        String str3 = "";
        if (jSONArrayOptJSONArray != null) {
            String str4 = "classic";
            String strNormalizeSkinUrl = "";
            for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
                JSONObject jSONObjectOptJSONObject = jSONArrayOptJSONArray.optJSONObject(i);
                if (jSONObjectOptJSONObject != null) {
                    String strOptString = jSONObjectOptJSONObject.optString("state", "ACTIVE");
                    String strOptString2 = jSONObjectOptJSONObject.optString("url", "");
                    if (strOptString2.length() != 0 && (strNormalizeSkinUrl.length() == 0 || "ACTIVE".equalsIgnoreCase(strOptString))) {
                        strNormalizeSkinUrl = AccountSkinCache.normalizeSkinUrl(strOptString2);
                        str4 = "SLIM".equalsIgnoreCase(jSONObjectOptJSONObject.optString("variant", "CLASSIC")) ? "slim" : "classic";
                        if ("ACTIVE".equalsIgnoreCase(strOptString)) {
                            break;
                        }
                    }
                }
            }
            str3 = strNormalizeSkinUrl;
            str2 = str4;
        }
        return new MinecraftProfile(jSONObject.optString("name", "Microsoft Player"), addDashesToUuid(jSONObject.getString("id")), str3, str2);
    }

    private static JSONObject postForm(String str, String str2) throws JSONException, IOException {
        HttpURLConnection httpURLConnectionOpenConnection = openConnection(str);
        byte[] bytes = str2.getBytes(StandardCharsets.UTF_8);
        httpURLConnectionOpenConnection.setRequestMethod(ShareTarget.METHOD_POST);
        httpURLConnectionOpenConnection.setRequestProperty("Content-Type", ShareTarget.ENCODING_TYPE_URL_ENCODED);
        httpURLConnectionOpenConnection.setRequestProperty("charset", "utf-8");
        httpURLConnectionOpenConnection.setRequestProperty("Content-Length", Integer.toString(bytes.length));
        httpURLConnectionOpenConnection.setUseCaches(false);
        httpURLConnectionOpenConnection.setDoInput(true);
        httpURLConnectionOpenConnection.setDoOutput(true);
        OutputStream outputStream = httpURLConnectionOpenConnection.getOutputStream();
        try {
            outputStream.write(bytes);
            if (outputStream != null) {
                outputStream.close();
            }
            return readJsonResponse(httpURLConnectionOpenConnection);
        } catch (Throwable th) {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    private static JSONObject postJson(String str, JSONObject jSONObject) throws JSONException, IOException {
        HttpURLConnection httpURLConnectionOpenConnection = openConnection(str);
        byte[] bytes = jSONObject.toString().getBytes(StandardCharsets.UTF_8);
        httpURLConnectionOpenConnection.setRequestMethod(ShareTarget.METHOD_POST);
        httpURLConnectionOpenConnection.setRequestProperty("Content-Type", "application/json");
        httpURLConnectionOpenConnection.setRequestProperty("Accept", "application/json");
        httpURLConnectionOpenConnection.setRequestProperty("charset", "utf-8");
        httpURLConnectionOpenConnection.setRequestProperty("Content-Length", Integer.toString(bytes.length));
        httpURLConnectionOpenConnection.setUseCaches(false);
        httpURLConnectionOpenConnection.setDoInput(true);
        httpURLConnectionOpenConnection.setDoOutput(true);
        OutputStream outputStream = httpURLConnectionOpenConnection.getOutputStream();
        try {
            outputStream.write(bytes);
            if (outputStream != null) {
                outputStream.close();
            }
            return readJsonResponse(httpURLConnectionOpenConnection);
        } catch (Throwable th) {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    private static JSONObject readJsonResponse(HttpURLConnection httpURLConnection) throws JSONException, IOException {
        InputStream errorStream;
        int responseCode = httpURLConnection.getResponseCode();
        if (responseCode >= 200 && responseCode < 300) {
            errorStream = httpURLConnection.getInputStream();
        } else {
            errorStream = httpURLConnection.getErrorStream();
        }
        String fully = readFully(errorStream);
        httpURLConnection.disconnect();
        if (responseCode < 200 || responseCode >= 300) {
            throw new IOException("HTTP " + responseCode + ": " + fully);
        }
        return new JSONObject(fully);
    }

    private static HttpURLConnection openConnection(String str) throws IOException {
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(str).openConnection();
        httpURLConnection.setConnectTimeout(30000);
        httpURLConnection.setReadTimeout(30000);
        return httpURLConnection;
    }

    private static String readFully(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        while (true) {
            try {
                String line = bufferedReader.readLine();
                if (line != null) {
                    sb.append(line);
                } else {
                    bufferedReader.close();
                    return sb.toString();
                }
            } catch (Throwable th) {
                try {
                    bufferedReader.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        }
    }

    private static String toFormData(String... strArr) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < strArr.length; i += 2) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(URLEncoder.encode(strArr[i], "UTF-8")).append('=').append(URLEncoder.encode(strArr[i + 1], "UTF-8"));
        }
        return sb.toString();
    }

    private static String addDashesToUuid(String str) {
        return (str.contains("-") || str.length() != 32) ? str : str.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5");
    }

    private void notifyError(final String str) {
        this.mainHandler.post(new Runnable() { // from class: ca.dnamobile.javalauncher.auth.MicrosoftAuthManagerPersonal$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$notifyError$6(str);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$notifyError$6(String str) {
        Listener listener = this.listener;
        if (listener != null) {
            listener.onError(str);
        }
    }

    private static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    private static final class LiveTokens {
        final String accessToken;
        final String refreshToken;

        LiveTokens(String str, String str2) {
            this.accessToken = str;
            this.refreshToken = str2;
        }
    }

    private static final class XstsToken {
        final String token;
        final String userHash;

        XstsToken(String str, String str2) {
            this.userHash = str;
            this.token = str2;
        }
    }

    private static final class MinecraftProfile {
        final String name;
        final String skinUrl;
        final String skinVariant;
        final String uuidWithDashes;

        MinecraftProfile(String str, String str2, String str3, String str4) {
            this.name = str;
            this.uuidWithDashes = str2;
            this.skinUrl = str3;
            this.skinVariant = str4;
        }
    }
}
