package ca.dnamobile.javalauncher.auth;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import androidx.core.view.ViewCompat;
import ca.dnamobile.javalauncher.auth.MicrosoftAuthSessionStore;
import ca.dnamobile.javalauncher.feature.log.Logging;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class MicrosoftAuthActivity extends Activity {
    public static final String EXTRA_AUTH_CODE = "ca.dnamobile.javalauncher.auth.AUTH_CODE";
    public static final String EXTRA_AUTH_URL = "ca.dnamobile.javalauncher.auth.AUTH_URL";
    public static final String EXTRA_CODE_VERIFIER = "ca.dnamobile.javalauncher.auth.CODE_VERIFIER";
    public static final String EXTRA_ERROR = "ca.dnamobile.javalauncher.auth.ERROR";
    private static final String STATE_AUTH_URL = "ca.dnamobile.javalauncher.auth.STATE_AUTH_URL";
    private static final String STATE_CODE_VERIFIER = "ca.dnamobile.javalauncher.auth.STATE_CODE_VERIFIER";
    private static final String TAG = "MicrosoftAuth";
    private String authUrl;
    private boolean finished;
    private String pkceCodeVerifier;
    private ProgressBar progressBar;
    private boolean restoredFromPendingSession;
    private WebView webView;

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        MicrosoftAuthSessionStore.Pending pendingLoad;
        super.onCreate(bundle);
        if (bundle != null) {
            this.authUrl = bundle.getString(STATE_AUTH_URL);
            this.pkceCodeVerifier = bundle.getString(STATE_CODE_VERIFIER);
        }
        if (isBlank(this.authUrl)) {
            this.authUrl = getIntent().getStringExtra(EXTRA_AUTH_URL);
        }
        if (isBlank(this.pkceCodeVerifier)) {
            this.pkceCodeVerifier = getIntent().getStringExtra(EXTRA_CODE_VERIFIER);
        }
        if ((isBlank(this.authUrl) || isBlank(this.pkceCodeVerifier)) && (pendingLoad = MicrosoftAuthSessionStore.load(this)) != null) {
            this.authUrl = pendingLoad.authUrl;
            this.pkceCodeVerifier = pendingLoad.codeVerifier;
            this.restoredFromPendingSession = true;
        }
        createContentView();
        setupWebView();
        if (bundle == null) {
            startSession(!this.restoredFromPendingSession);
        } else {
            this.webView.restoreState(bundle);
        }
    }

    private void createContentView() {
        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.setBackgroundColor(ViewCompat.MEASURED_STATE_MASK);
        WebView webView = new WebView(this);
        this.webView = webView;
        frameLayout.addView(webView, new FrameLayout.LayoutParams(-1, -1));
        ProgressBar progressBar = new ProgressBar(this);
        this.progressBar = progressBar;
        frameLayout.addView(progressBar, new FrameLayout.LayoutParams(-2, -2, 17));
        setContentView(frameLayout);
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void setupWebView() {
        WebSettings settings = this.webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(true);
        settings.setSaveFormData(false);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(this.webView, true);
        this.webView.setWebChromeClient(new LoginWebChromeClient());
        this.webView.setWebViewClient(new LoginWebViewClient());
    }

    private void startSession(final boolean z) {
        this.progressBar.setVisibility(0);
        final Runnable runnable = new Runnable() { // from class: ca.dnamobile.javalauncher.auth.MicrosoftAuthActivity$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                MicrosoftAuthActivity.this.lambda$startSession$0(z);
            }
        };
        if (!z) {
            runnable.run();
        } else {
            CookieManager.getInstance().removeAllCookies(new ValueCallback() { // from class: ca.dnamobile.javalauncher.auth.MicrosoftAuthActivity$$ExternalSyntheticLambda1
                @Override // android.webkit.ValueCallback
                public final void onReceiveValue(Object obj) {
                    MicrosoftAuthActivity.lambda$startSession$1(runnable, (Boolean) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$startSession$0(boolean z) {
        WebView webView = this.webView;
        if (webView == null) {
            return;
        }
        if (z) {
            webView.clearHistory();
            this.webView.clearCache(true);
            this.webView.clearFormData();
        }
        ensureAuthUrl();
        if (isBlank(this.authUrl) || isBlank(this.pkceCodeVerifier)) {
            cancelLogin("Unable to create Microsoft authorization URL.");
            return;
        }
        MicrosoftAuthSessionStore.save(this, this.pkceCodeVerifier, this.authUrl);
        Logging.i(TAG, "Opening Microsoft login: " + sanitizeUrlForLog(this.authUrl));
        this.webView.loadUrl(this.authUrl);
    }

    static /* synthetic */ void lambda$startSession$1(Runnable runnable, Boolean bool) {
        try {
            CookieManager.getInstance().flush();
        } catch (Throwable unused) {
        }
        runnable.run();
    }

    private void ensureAuthUrl() {
        String str = this.authUrl;
        if (str == null || str.length() <= 0) {
            String str2 = this.pkceCodeVerifier;
            if (str2 == null || str2.length() == 0) {
                this.pkceCodeVerifier = MicrosoftAuthConfigPersonal.createCodeVerifier();
            }
            this.authUrl = MicrosoftAuthConfigPersonal.buildAuthorizationUriWithPkce(this.pkceCodeVerifier).toString();
        }
    }

    @Override // android.app.Activity
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        String str = this.authUrl;
        if (str != null && str.length() > 0) {
            bundle.putString(STATE_AUTH_URL, this.authUrl);
        }
        String str2 = this.pkceCodeVerifier;
        if (str2 != null && str2.length() > 0) {
            bundle.putString(STATE_CODE_VERIFIER, this.pkceCodeVerifier);
        }
        WebView webView = this.webView;
        if (webView != null) {
            webView.saveState(bundle);
        }
    }

    @Override // android.app.Activity
    public void onBackPressed() {
        WebView webView = this.webView;
        if (webView != null && webView.canGoBack()) {
            this.webView.goBack();
        } else {
            cancelLogin("Authorization canceled");
        }
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        WebView webView = this.webView;
        if (webView != null) {
            webView.stopLoading();
            this.webView.setWebChromeClient(null);
            this.webView.setWebViewClient(null);
            this.webView.destroy();
            this.webView = null;
        }
        super.onDestroy();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean handleUrl(String str) {
        if (this.finished || str == null) {
            return false;
        }
        if (str.contains("res=cancel")) {
            cancelLogin("Authorization canceled");
            return true;
        }
        if (!MicrosoftAuthConfigPersonal.isRedirect(str)) {
            return false;
        }
        Uri uri = Uri.parse(str);
        String strExtractCode = MicrosoftAuthConfigPersonal.extractCode(uri);
        String strExtractError = MicrosoftAuthConfigPersonal.extractError(uri);
        Intent intent = new Intent();
        if (strExtractCode == null || strExtractCode.length() == 0) {
            if (strExtractError == null || strExtractError.length() <= 0) {
                strExtractError = "Microsoft login finished without an authorization code.";
            }
            Logging.i(TAG, strExtractError + " url=" + sanitizeUrlForLog(str));
            MicrosoftAuthSessionStore.clear(this);
            intent.putExtra(EXTRA_ERROR, strExtractError);
            this.finished = true;
            setResult(0, intent);
        } else {
            Logging.i(TAG, "Microsoft authorization code received.");
            intent.putExtra(EXTRA_AUTH_CODE, strExtractCode);
            String str2 = this.pkceCodeVerifier;
            if (str2 == null || str2.length() == 0) {
                this.pkceCodeVerifier = MicrosoftAuthConfigPersonal.getPendingCodeVerifier();
            }
            String str3 = this.pkceCodeVerifier;
            if (str3 == null || str3.length() == 0) {
                Logging.i(TAG, "Authorization finished without a PKCE verifier. Please start Microsoft sign-in again.");
                MicrosoftAuthSessionStore.clear(this);
                intent.putExtra(EXTRA_ERROR, "Authorization finished without a PKCE verifier. Please start Microsoft sign-in again.");
                this.finished = true;
                setResult(0, intent);
                finish();
                return true;
            }
            intent.putExtra(EXTRA_CODE_VERIFIER, this.pkceCodeVerifier);
            this.finished = true;
            setResult(-1, intent);
        }
        finish();
        return true;
    }

    private void cancelLogin(String str) {
        if (this.finished) {
            return;
        }
        this.finished = true;
        MicrosoftAuthSessionStore.clear(this);
        Intent intent = new Intent();
        intent.putExtra(EXTRA_ERROR, str);
        setResult(0, intent);
        finish();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String sanitizeUrlForLog(String str) {
        int iIndexOf = str.indexOf("code=");
        if (iIndexOf >= 0) {
            return str.substring(0, iIndexOf) + "code=<hidden>";
        }
        int iIndexOf2 = str.indexOf("code_challenge=");
        if (iIndexOf2 < 0) {
            return str;
        }
        int iIndexOf3 = str.indexOf(38, iIndexOf2);
        return iIndexOf3 >= 0 ? str.substring(0, iIndexOf2) + "code_challenge=<hidden>" + str.substring(iIndexOf3) : str.substring(0, iIndexOf2) + "code_challenge=<hidden>";
    }

    private static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    private final class LoginWebChromeClient extends WebChromeClient {
        private LoginWebChromeClient() {
        }

        @Override // android.webkit.WebChromeClient
        public boolean onCreateWindow(WebView webView, boolean z, boolean z2, Message message) {
            if (MicrosoftAuthActivity.this.webView == null || message == null) {
                return false;
            }
            ((WebView.WebViewTransport) message.obj).setWebView(MicrosoftAuthActivity.this.webView);
            message.sendToTarget();
            return true;
        }
    }

    private final class LoginWebViewClient extends WebViewClient {
        private LoginWebViewClient() {
        }

        @Override // android.webkit.WebViewClient
        public boolean shouldOverrideUrlLoading(WebView webView, String str) {
            return MicrosoftAuthActivity.this.handleUrl(str) || super.shouldOverrideUrlLoading(webView, str);
        }

        @Override // android.webkit.WebViewClient
        public boolean shouldOverrideUrlLoading(WebView webView, WebResourceRequest webResourceRequest) {
            return MicrosoftAuthActivity.this.handleUrl((webResourceRequest == null || webResourceRequest.getUrl() == null) ? null : webResourceRequest.getUrl().toString()) || super.shouldOverrideUrlLoading(webView, webResourceRequest);
        }

        @Override // android.webkit.WebViewClient
        public void onPageStarted(WebView webView, String str, Bitmap bitmap) {
            super.onPageStarted(webView, str, bitmap);
            if (MicrosoftAuthActivity.this.progressBar != null) {
                MicrosoftAuthActivity.this.progressBar.setVisibility(0);
            }
            MicrosoftAuthActivity.this.handleUrl(str);
        }

        @Override // android.webkit.WebViewClient
        public void doUpdateVisitedHistory(WebView webView, String str, boolean z) {
            super.doUpdateVisitedHistory(webView, str, z);
            MicrosoftAuthActivity.this.handleUrl(str);
        }

        @Override // android.webkit.WebViewClient
        public void onPageFinished(WebView webView, String str) {
            super.onPageFinished(webView, str);
            MicrosoftAuthActivity.this.handleUrl(str);
            if (MicrosoftAuthActivity.this.progressBar != null) {
                MicrosoftAuthActivity.this.progressBar.setVisibility(8);
            }
        }

        @Override // android.webkit.WebViewClient
        public void onReceivedError(WebView webView, WebResourceRequest webResourceRequest, WebResourceError webResourceError) {
            super.onReceivedError(webView, webResourceRequest, webResourceError);
            if (webResourceRequest == null || !webResourceRequest.isForMainFrame()) {
                return;
            }
            String string = webResourceRequest.getUrl() != null ? webResourceRequest.getUrl().toString() : "";
            if (MicrosoftAuthConfigPersonal.isRedirect(string)) {
                MicrosoftAuthActivity.this.handleUrl(string);
            } else {
                Logging.i(MicrosoftAuthActivity.TAG, "Microsoft login WebView main-frame error: " + (webResourceError != null ? String.valueOf(webResourceError.getDescription()) : "Unknown WebView error") + " url=" + MicrosoftAuthActivity.this.sanitizeUrlForLog(string));
            }
        }
    }
}
