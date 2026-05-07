package ca.dnamobile.javalauncher;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.core.motion.utils.TypedValues;
import androidx.core.os.EnvironmentCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import ca.dnamobile.javalauncher.auth.MicrosoftAuthConfigPersonal;
import ca.dnamobile.javalauncher.auth.MicrosoftAuthManagerPersonal;
import ca.dnamobile.javalauncher.auth.OfflineAccessBlocker;
import ca.dnamobile.javalauncher.data.AccountStore;
import ca.dnamobile.javalauncher.data.model.MinecraftVersion;
import ca.dnamobile.javalauncher.databinding.ActivityInstanceDetailsBinding;
import ca.dnamobile.javalauncher.feature.log.Logging;
import ca.dnamobile.javalauncher.instance.InstanceVersionUpdater;
import ca.dnamobile.javalauncher.instance.LauncherInstance;
import ca.dnamobile.javalauncher.instance.LauncherInstanceManager;
import ca.dnamobile.javalauncher.launcher.InstanceLaunchSettings;
import ca.dnamobile.javalauncher.modmanager.CurseForgeApiKeyProvider;
import ca.dnamobile.javalauncher.modmanager.ModJarMetadataExtractor;
import ca.dnamobile.javalauncher.modmanager.ModManagerContentType;
import ca.dnamobile.javalauncher.modmanager.ModManagerManifest;
import ca.dnamobile.javalauncher.modmanager.ModManagerSource;
import ca.dnamobile.javalauncher.modmanager.ModManagerUpdateManager;
import ca.dnamobile.javalauncher.modmanager.ModManagerVersionResolver;
import ca.dnamobile.javalauncher.modmanager.ModpackExportManager;
import ca.dnamobile.javalauncher.modmanager.ModpackInstallManager;
import ca.dnamobile.javalauncher.modmanager.ModrinthInstallManager;
import ca.dnamobile.javalauncher.modmanager.ModrinthVersion;
import ca.dnamobile.javalauncher.renderer.MobileGluesConfigHelper;
import ca.dnamobile.javalauncher.renderer.RendererInterface;
import ca.dnamobile.javalauncher.renderer.RendererPluginManager;
import ca.dnamobile.javalauncher.renderer.Renderers;
import ca.dnamobile.javalauncher.settings.LauncherPreferences;
import ca.dnamobile.javalauncher.settings.MemoryAllocationUtils;
import ca.dnamobile.javalauncher.ui.instance.InstanceIconResolver;
import ca.dnamobile.javalauncher.ui.instance.PerInstanceSettingsDialog;
import ca.dnamobile.javalauncher.ui.version.MinecraftVersionInstaller;
import ca.dnamobile.javalauncher.ui.version.MinecraftVersionManifestClient;
import ca.dnamobile.javalauncher.utils.FullscreenUtils;
import ca.dnamobile.javalauncher.utils.path.PathManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.tabs.TabLayout;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class InstanceDetailsActivity extends AppCompatActivity {
    public static final String EXTRA_BASE_VERSION_ID = "ca.dnamobile.javalauncher.extra.BASE_VERSION_ID";
    public static final String EXTRA_CONTENT_CATEGORY = "ca.dnamobile.javalauncher.extra.CONTENT_CATEGORY";
    public static final String EXTRA_GAME_DIRECTORY = "ca.dnamobile.javalauncher.extra.GAME_DIRECTORY";
    public static final String EXTRA_ICON_FILE = "ca.dnamobile.javalauncher.extra.ICON_FILE";
    public static final String EXTRA_INSTANCE_ID = "ca.dnamobile.javalauncher.extra.INSTANCE_ID";
    public static final String EXTRA_INSTANCE_LOADER = "ca.dnamobile.javalauncher.extra.INSTANCE_LOADER";
    public static final String EXTRA_INSTANCE_NAME = "ca.dnamobile.javalauncher.extra.INSTANCE_NAME";
    public static final String EXTRA_ISOLATED = "ca.dnamobile.javalauncher.extra.ISOLATED";
    public static final String EXTRA_MINECRAFT_VERSION_ID = "ca.dnamobile.javalauncher.extra.MINECRAFT_VERSION_ID";
    public static final String EXTRA_ROOT_DIRECTORY = "ca.dnamobile.javalauncher.extra.ROOT_DIRECTORY";
    public static final String EXTRA_VERSION_TYPE = "ca.dnamobile.javalauncher.extra.VERSION_TYPE";
    private static final int MENU_DELETE_INSTANCE = 2;
    private static final int MENU_EDIT_INSTANCE_ICON = 4;
    private static final int MENU_EDIT_INSTANCE_NAME = 3;
    private static final int MENU_EXPORT_MODPACK = 5;
    private static final int MENU_IMPORT_MODPACK = 8;
    private static final int MENU_PER_INSTANCE_SETTINGS = 9;
    private static final int MENU_REPAIR_INSTANCE = 10;
    private static final int MENU_UPDATE_LOADER = 7;
    private static final int MENU_UPDATE_VERSION = 6;
    private static final int MENU_VIEW_FOLDER = 1;
    private static final int REQUEST_EXPORT_MODPACK = 9126;
    private static final int REQUEST_EXPORT_WORLD = 9128;
    private static final int REQUEST_IMPORT_MODPACK = 9127;
    private static final int REQUEST_PICK_CONTENT = 9124;
    private static final int REQUEST_PICK_INSTANCE_ICON = 9125;
    private static final String TAG = "InstanceDetails";
    private AccountStore accountStore;
    private MicrosoftAuthManagerPersonal authManager;
    private String baseVersionId;
    private ActivityInstanceDetailsBinding binding;
    private InstanceContentAdapter contentAdapter;
    private TextView contentLoadingMessage;
    private View contentLoadingOverlay;
    private TextView contentLoadingTitle;
    private boolean contentOperationRunning;
    private int contentRefreshGeneration;
    private boolean contentRefreshRunning;
    private boolean contentSearchFilterApplyQueued;
    private File gameDirectory;
    private File iconFile;
    private String instanceId;
    private String instanceName;
    private boolean isolated;
    private String loader;
    private String minecraftVersionId;
    private File modsDirectory;
    private Runnable pendingAfterMicrosoftSignIn;
    private Runnable pendingContentLoadingRunnable;
    private ModpackExportManager.Platform pendingExportPlatform;
    private Runnable pendingMetadataSearchFilterRunnable;
    private File pendingWorldExportDirectory;
    private File resourcepacksDirectory;
    private File rootDirectory;
    private File shaderpacksDirectory;
    private boolean skipNextResumeContentRefresh;
    private ProgressBar updateProgressBar;
    private AlertDialog updateProgressDialog;
    private TextView updateProgressMessage;
    private String versionType;
    private File worldsDirectory;
    private ResourceCategory selectedCategory = ResourceCategory.MODS;
    private ResourceCategory pendingImportCategory = ResourceCategory.MODS;
    private final ArrayList<InstanceContentItem> allContentItems = new ArrayList<>();
    private final ArrayList<InstanceContentItem> contentItems = new ArrayList<>();
    private final Map<String, String> contentSearchMetadata = new ConcurrentHashMap();
    private String contentSearchQuery = "";
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService iconExecutor = Executors.newFixedThreadPool(2);
    private final ExecutorService contentRefreshExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService contentOperationExecutor = Executors.newSingleThreadExecutor();
    private final Map<String, ModManagerUpdateManager.UpdateCandidate> updateCandidates = new ConcurrentHashMap();
    private final Map<String, UpdateState> updateStates = new ConcurrentHashMap();
    private final Map<String, String> updateMessages = new ConcurrentHashMap();

    private enum UpdateState {
        UNKNOWN,
        CHECKING,
        UPDATE_AVAILABLE,
        UPDATING,
        UP_TO_DATE,
        ERROR
    }

    private int getImmersiveSystemUiFlags() {
        return 5894;
    }

    public static Intent createIntent(Context context, LauncherInstance launcherInstance) {
        Intent intent = new Intent(context, (Class<?>) InstanceDetailsActivity.class);
        intent.putExtra(EXTRA_INSTANCE_ID, launcherInstance.getId());
        intent.putExtra(EXTRA_INSTANCE_NAME, launcherInstance.getName());
        intent.putExtra(EXTRA_INSTANCE_LOADER, launcherInstance.getLoader());
        intent.putExtra(EXTRA_BASE_VERSION_ID, launcherInstance.getBaseVersionId());
        intent.putExtra(EXTRA_MINECRAFT_VERSION_ID, launcherInstance.getMinecraftVersionId());
        intent.putExtra(EXTRA_VERSION_TYPE, launcherInstance.getVersionType());
        intent.putExtra(EXTRA_ROOT_DIRECTORY, launcherInstance.getRootDirectory().getAbsolutePath());
        intent.putExtra(EXTRA_GAME_DIRECTORY, launcherInstance.getGameDirectory().getAbsolutePath());
        intent.putExtra(EXTRA_ICON_FILE, launcherInstance.getIconFile() != null ? launcherInstance.getIconFile().getAbsolutePath() : "");
        intent.putExtra(EXTRA_ISOLATED, launcherInstance.isIsolated());
        return intent;
    }

    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        PathManager.initContextConstants(this);
        ActivityInstanceDetailsBinding activityInstanceDetailsBindingInflate = ActivityInstanceDetailsBinding.inflate(getLayoutInflater());
        this.binding = activityInstanceDetailsBindingInflate;
        setContentView(activityInstanceDetailsBindingInflate.getRoot());
        enableFullscreen();
        this.binding.buttonBackFromInstanceDetails.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda92
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                InstanceDetailsActivity.this.lambda$onCreate$0(view);
            }
        });
        if (!readExtras()) {
            Toast.makeText(this, R.string.hint_select_instance, 0).show();
            finish();
            return;
        }
        setupAccountGate();
        bindHeader();
        setupActions();
        setupContentSearch();
        setupContentTabs();
        refreshContentList();
        this.skipNextResumeContentRefresh = true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$0(View view) {
        finish();
    }

    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    protected void onResume() {
        super.onResume();
        enableFullscreen();
        if (this.skipNextResumeContentRefresh) {
            this.skipNextResumeContentRefresh = false;
        } else {
            if (this.binding == null || this.gameDirectory == null) {
                return;
            }
            refreshContentList();
        }
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public void onWindowFocusChanged(boolean z) {
        super.onWindowFocusChanged(z);
        if (z) {
            enableFullscreen();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enableFullscreen() {
        FullscreenUtils.enableImmersive(this);
        applyFullscreenToWindow(getWindow());
    }

    private void applyFullscreenToWindow(Window window) {
        if (window == null) {
            return;
        }
        window.getDecorView().setSystemUiVisibility(getImmersiveSystemUiFlags());
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController insetsController = window.getInsetsController();
            if (insetsController != null) {
                insetsController.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                insetsController.setSystemBarsBehavior(2);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showFullscreenSafeDialog$1(DialogInterface dialogInterface) {
        this.mainHandler.postDelayed(new InstanceDetailsActivity$$ExternalSyntheticLambda13(this), 80L);
    }

    private void showFullscreenSafeDialog(final AlertDialog alertDialog) {
        alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda87
            @Override // android.content.DialogInterface.OnDismissListener
            public final void onDismiss(DialogInterface dialogInterface) {
                InstanceDetailsActivity.this.lambda$showFullscreenSafeDialog$1(dialogInterface);
            }
        });
        alertDialog.show();
        applyFullscreenToWindow(alertDialog.getWindow());
        this.mainHandler.postDelayed(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda88
            @Override // java.lang.Runnable
            public final void run() {
                InstanceDetailsActivity.this.lambda$showFullscreenSafeDialog$2(alertDialog);
            }
        }, 120L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showFullscreenSafeDialog$2(AlertDialog alertDialog) {
        applyFullscreenToWindow(alertDialog.getWindow());
        enableFullscreen();
    }

    @Override // androidx.appcompat.app.AppCompatActivity, androidx.fragment.app.FragmentActivity, android.app.Activity
    protected void onDestroy() {
        cancelPendingContentLoadingOverlay();
        cancelPendingMetadataSearchFilter();
        this.contentRefreshExecutor.shutdownNow();
        this.contentOperationExecutor.shutdownNow();
        this.iconExecutor.shutdownNow();
        MicrosoftAuthManagerPersonal microsoftAuthManagerPersonal = this.authManager;
        if (microsoftAuthManagerPersonal != null) {
            microsoftAuthManagerPersonal.dispose();
        }
        super.onDestroy();
    }

    private void setupAccountGate() {
        try {
            this.accountStore = new AccountStore(this);
            MicrosoftAuthManagerPersonal microsoftAuthManagerPersonal = new MicrosoftAuthManagerPersonal(this, this.accountStore);
            this.authManager = microsoftAuthManagerPersonal;
            microsoftAuthManagerPersonal.setListener(new MicrosoftAuthManagerPersonal.Listener() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity.1
                @Override // ca.dnamobile.javalauncher.auth.MicrosoftAuthManagerPersonal.Listener
                public void onSignedIn(AccountStore.Account account) {
                    Runnable runnable = InstanceDetailsActivity.this.pendingAfterMicrosoftSignIn;
                    InstanceDetailsActivity.this.pendingAfterMicrosoftSignIn = null;
                    if (runnable != null) {
                        runnable.run();
                    }
                }

                @Override // ca.dnamobile.javalauncher.auth.MicrosoftAuthManagerPersonal.Listener
                public void onError(String str) {
                    InstanceDetailsActivity.this.pendingAfterMicrosoftSignIn = null;
                    Toast.makeText(InstanceDetailsActivity.this, str, 1).show();
                }
            });
        } catch (Throwable th) {
            Logging.e(TAG, "Microsoft account gate initialization failed", th);
            this.accountStore = null;
            this.authManager = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: renamed from: signInWithMicrosoftThen, reason: merged with bridge method [inline-methods] */
    public void lambda$requireMicrosoftLoginHistoryBeforeLaunch$3(Runnable runnable) {
        if (this.authManager == null) {
            return;
        }
        if (!MicrosoftAuthConfigPersonal.isConfigured()) {
            Toast.makeText(this, R.string.msg_configure_client_id, 1).show();
        } else {
            this.pendingAfterMicrosoftSignIn = runnable;
            this.authManager.signIn();
        }
    }

    private boolean requireMicrosoftLoginHistoryBeforeLaunch(final Runnable runnable) {
        return OfflineAccessBlocker.requireMicrosoftLoginHistoryBeforeLaunch(this, this.accountStore, new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda96
            @Override // java.lang.Runnable
            public final void run() {
                InstanceDetailsActivity.this.lambda$requireMicrosoftLoginHistoryBeforeLaunch$3(runnable);
            }
        });
    }

    private boolean readExtras() {
        Intent intent = getIntent();
        this.instanceId = intent.getStringExtra(EXTRA_INSTANCE_ID);
        this.instanceName = intent.getStringExtra(EXTRA_INSTANCE_NAME);
        this.loader = intent.getStringExtra(EXTRA_INSTANCE_LOADER);
        this.baseVersionId = intent.getStringExtra(EXTRA_BASE_VERSION_ID);
        this.minecraftVersionId = intent.getStringExtra(EXTRA_MINECRAFT_VERSION_ID);
        this.versionType = intent.getStringExtra(EXTRA_VERSION_TYPE);
        String stringExtra = intent.getStringExtra(EXTRA_ROOT_DIRECTORY);
        String stringExtra2 = intent.getStringExtra(EXTRA_GAME_DIRECTORY);
        String stringExtra3 = intent.getStringExtra(EXTRA_ICON_FILE);
        this.isolated = intent.getBooleanExtra(EXTRA_ISOLATED, true);
        if (isBlank(this.instanceName) || isBlank(this.baseVersionId) || isBlank(stringExtra2)) {
            return false;
        }
        if (isBlank(this.instanceId)) {
            this.instanceId = this.instanceName;
        }
        if (isBlank(this.loader)) {
            this.loader = "Vanilla";
        }
        if (isBlank(this.minecraftVersionId)) {
            this.minecraftVersionId = ModManagerVersionResolver.resolveGameVersionForContent(this.baseVersionId);
        }
        if (isBlank(this.minecraftVersionId)) {
            this.minecraftVersionId = this.baseVersionId;
        }
        if (isBlank(this.versionType)) {
            this.versionType = BuildConfig.BUILD_TYPE;
        }
        if (isBlank(stringExtra)) {
            stringExtra = stringExtra2;
        }
        this.rootDirectory = new File(stringExtra);
        this.gameDirectory = new File(stringExtra2);
        this.iconFile = isBlank(stringExtra3) ? null : new File(stringExtra3);
        resetContentDirectories();
        return true;
    }

    private void bindHeader() {
        this.binding.textInstanceName.setText(this.instanceName);
        this.binding.textInstanceMeta.setText(getString(R.string.instance_details_meta_value, new Object[]{displayLoader(this.loader), this.minecraftVersionId, displayVersionType(this.versionType)}));
        bindInstanceIcon();
    }

    private void bindInstanceIcon() {
        ActivityInstanceDetailsBinding activityInstanceDetailsBinding = this.binding;
        if (activityInstanceDetailsBinding == null || activityInstanceDetailsBinding.imageInstanceIcon == null) {
            return;
        }
        this.binding.imageInstanceIcon.setImageDrawable(null);
        File file = this.iconFile;
        if (file != null && file.isFile()) {
            try {
                this.binding.imageInstanceIcon.setImageURI(Uri.fromFile(this.iconFile));
                if (this.binding.imageInstanceIcon.getDrawable() != null) {
                    return;
                }
            } catch (Throwable th) {
                Logging.i(TAG, "Unable to load custom instance icon: " + readableError(th));
            }
        }
        this.binding.imageInstanceIcon.setImageResource(InstanceIconResolver.getDefaultIcon(this.loader, this.baseVersionId, this.minecraftVersionId, this.instanceName));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupActions$4(View view) {
        launchInstance();
    }

    private void setupActions() {
        this.binding.buttonPlay.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda45
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                InstanceDetailsActivity.this.lambda$setupActions$4(view);
            }
        });
        this.binding.buttonInstanceSettings.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda46
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                InstanceDetailsActivity.this.showInstanceSettingsMenu(view);
            }
        });
        this.binding.buttonBrowseContent.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda47
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                InstanceDetailsActivity.this.lambda$setupActions$5(view);
            }
        });
        this.binding.buttonAddMods.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda48
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                InstanceDetailsActivity.this.lambda$setupActions$6(view);
            }
        });
        this.binding.buttonCheckContentUpdates.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda49
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                InstanceDetailsActivity.this.lambda$setupActions$7(view);
            }
        });
        this.binding.buttonUpdateAllContent.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda51
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                InstanceDetailsActivity.this.lambda$setupActions$8(view);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupActions$5(View view) {
        browseSelectedContent();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupActions$6(View view) {
        pickSelectedContent();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupActions$7(View view) {
        checkUpdatesForSelectedCategory();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupActions$8(View view) {
        updateAllAvailableForSelectedCategory();
    }

    private void setupContentSearch() {
        if (this.binding.editTextContentSearch == null) {
            return;
        }
        this.binding.editTextContentSearch.addTextChangedListener(new TextWatcher() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity.2
            @Override // android.text.TextWatcher
            public void afterTextChanged(Editable editable) {
            }

            @Override // android.text.TextWatcher
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override // android.text.TextWatcher
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                InstanceDetailsActivity.this.contentSearchQuery = charSequence == null ? "" : charSequence.toString();
                InstanceDetailsActivity.this.requestContentSearchFilter(true);
            }
        });
        this.binding.editTextContentSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda52
            @Override // android.widget.TextView.OnEditorActionListener
            public final boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                return InstanceDetailsActivity.this.lambda$setupContentSearch$9(textView, i, keyEvent);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean lambda$setupContentSearch$9(TextView textView, int i, KeyEvent keyEvent) {
        boolean z = i == 6 || i == 3 || i == 2;
        boolean z2 = keyEvent != null && keyEvent.getAction() == 1 && keyEvent.getKeyCode() == 66;
        if (!z && !z2) {
            return false;
        }
        finishContentSearchInput(textView);
        return true;
    }

    private void finishContentSearchInput(View view) {
        view.clearFocus();
        hideKeyboardFromView(view);
    }

    private void hideKeyboardFromView(View view) {
        try {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService("input_method");
            if (inputMethodManager != null) {
                inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        } catch (Throwable unused) {
        }
        this.mainHandler.postDelayed(new InstanceDetailsActivity$$ExternalSyntheticLambda13(this), 80L);
    }

    private void setupContentTabs() {
        this.contentAdapter = new InstanceContentAdapter();
        this.binding.recyclerResourceItems.setLayoutManager(new LinearLayoutManager(this));
        this.binding.recyclerResourceItems.setNestedScrollingEnabled(true);
        this.binding.recyclerResourceItems.setItemViewCacheSize(8);
        this.binding.recyclerResourceItems.setAdapter(this.contentAdapter);
        this.binding.recyclerResourceItems.setOnTouchListener(new View.OnTouchListener() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda32
            @Override // android.view.View.OnTouchListener
            public final boolean onTouch(View view, MotionEvent motionEvent) {
                return InstanceDetailsActivity.this.lambda$setupContentTabs$10(view, motionEvent);
            }
        });
        constrainResourceRecyclerHeightIfNeeded();
        for (ResourceCategory resourceCategory : ResourceCategory.values()) {
            this.binding.tabResourceCategories.addTab(this.binding.tabResourceCategories.newTab().setText(resourceCategory.tabTitleRes));
        }
        this.binding.tabResourceCategories.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity.3
            @Override // com.google.android.material.tabs.TabLayout.BaseOnTabSelectedListener
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override // com.google.android.material.tabs.TabLayout.BaseOnTabSelectedListener
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                ResourceCategory[] resourceCategoryArrValues = ResourceCategory.values();
                if (position < 0 || position >= resourceCategoryArrValues.length) {
                    return;
                }
                InstanceDetailsActivity.this.selectedCategory = resourceCategoryArrValues[position];
                InstanceDetailsActivity.this.refreshContentList();
            }

            @Override // com.google.android.material.tabs.TabLayout.BaseOnTabSelectedListener
            public void onTabReselected(TabLayout.Tab tab) {
                InstanceDetailsActivity.this.refreshContentList();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean lambda$setupContentTabs$10(View view, MotionEvent motionEvent) {
        if (!this.binding.editTextContentSearch.hasFocus()) {
            return false;
        }
        finishContentSearchInput(this.binding.editTextContentSearch);
        return false;
    }

    private void constrainResourceRecyclerHeightIfNeeded() {
        ViewGroup.LayoutParams layoutParams;
        ActivityInstanceDetailsBinding activityInstanceDetailsBinding = this.binding;
        if (activityInstanceDetailsBinding == null || activityInstanceDetailsBinding.recyclerResourceItems == null || (layoutParams = this.binding.recyclerResourceItems.getLayoutParams()) == null || layoutParams.height != -2) {
            return;
        }
        layoutParams.height = Math.max(dp(180), getResources().getDisplayMetrics().heightPixels - dp(260));
        this.binding.recyclerResourceItems.setLayoutParams(layoutParams);
    }

    private void bindImportButtonForCategory(ResourceCategory resourceCategory) {
        ActivityInstanceDetailsBinding activityInstanceDetailsBinding = this.binding;
        if (activityInstanceDetailsBinding == null || activityInstanceDetailsBinding.buttonAddMods == null) {
            return;
        }
        this.binding.buttonAddMods.setText(resourceCategory.uploadButtonTextRes);
        if (resourceCategory == ResourceCategory.WORLDS) {
            this.binding.buttonAddMods.setIconResource(R.drawable.ic_arrow_downward_24);
            this.binding.buttonAddMods.setContentDescription("Import World");
        } else {
            this.binding.buttonAddMods.setIcon(null);
            this.binding.buttonAddMods.setContentDescription(null);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showInstanceSettingsMenu(View view) {
        enableFullscreen();
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setPadding(0, dp(6), 0, dp(6) + getDropdownBottomSafePadding());
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(resolveThemeColor(android.R.attr.colorBackground, -1));
        gradientDrawable.setCornerRadius(dp(18));
        linearLayout.setBackground(gradientDrawable);
        int iMin = Math.min(dp(280), Math.max(dp(220), getResources().getDisplayMetrics().widthPixels - dp(32)));
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(false);
        scrollView.setOverScrollMode(1);
        scrollView.addView(linearLayout, new FrameLayout.LayoutParams(-1, -2));
        PopupWindow popupWindow = new PopupWindow((View) scrollView, iMin, -2, false);
        popupWindow.setBackgroundDrawable(new ColorDrawable(0));
        popupWindow.setOutsideTouchable(true);
        popupWindow.setClippingEnabled(true);
        popupWindow.setInputMethodMode(2);
        popupWindow.setElevation(dp(8));
        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda12
            @Override // android.widget.PopupWindow.OnDismissListener
            public final void onDismiss() {
                InstanceDetailsActivity.this.enableFullscreen();
            }
        });
        addInstanceSettingsMenuRow(linearLayout, R.string.instance_settings_view_folder, popupWindow, 1);
        addInstanceSettingsMenuRow(linearLayout, R.string.button_delete_instance, popupWindow, 2);
        addInstanceSettingsMenuRow(linearLayout, R.string.instance_settings_edit_name, popupWindow, 3);
        addInstanceSettingsMenuRow(linearLayout, R.string.instance_settings_edit_icon, popupWindow, 4);
        addInstanceSettingsMenuRow(linearLayout, "Per Instance Settings", popupWindow, 9);
        addInstanceSettingsMenuRow(linearLayout, "Update Version", popupWindow, 6);
        addInstanceSettingsMenuRow(linearLayout, "Repair Instance", popupWindow, 10);
        if (getSupportedLoaderKind() != null) {
            addInstanceSettingsMenuRow(linearLayout, "Update Loader", popupWindow, 7);
        }
        addInstanceSettingsMenuRow(linearLayout, R.string.instance_settings_export_modpack, popupWindow, 5);
        addInstanceSettingsMenuRow(linearLayout, "Import Modpack", popupWindow, 8);
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        View decorView = getWindow().getDecorView();
        if (decorView.getWidth() > 0) {
            width = decorView.getWidth();
        }
        if (decorView.getHeight() > 0) {
            height = decorView.getHeight();
        }
        int iMin2 = Math.min((linearLayout.getChildCount() * dp(48)) + dp(12) + getDropdownBottomSafePadding(), Math.max(dp(220), height - dp(32)));
        popupWindow.setHeight(iMin2);
        int[] iArr = new int[2];
        view.getLocationOnScreen(iArr);
        int iMax = Math.max(dp(8), Math.min((iArr[0] + view.getWidth()) - iMin, (width - iMin) - dp(8)));
        int height2 = iArr[1] + view.getHeight() + dp(8);
        int iDp = height - dp(8);
        if (height2 + iMin2 > iDp) {
            height2 = Math.max(dp(8), iDp - iMin2);
        }
        popupWindow.showAtLocation(decorView, 8388659, iMax, height2);
        this.mainHandler.postDelayed(new InstanceDetailsActivity$$ExternalSyntheticLambda13(this), 80L);
    }

    private void addInstanceSettingsMenuRow(LinearLayout linearLayout, int i, PopupWindow popupWindow, int i2) {
        addInstanceSettingsMenuRow(linearLayout, getString(i), popupWindow, i2);
    }

    private void addInstanceSettingsMenuRow(LinearLayout linearLayout, String str, final PopupWindow popupWindow, final int i) {
        TextView textView = new TextView(this);
        textView.setText(str);
        textView.setGravity(16);
        textView.setMinHeight(dp(48));
        textView.setTextSize(2, 15.0f);
        textView.setTextColor(resolveThemeColor(android.R.attr.textColorPrimary, ViewCompat.MEASURED_STATE_MASK));
        textView.setPadding(dp(20), 0, dp(20), 0);
        int iResolveSelectableItemBackground = resolveSelectableItemBackground();
        if (iResolveSelectableItemBackground != 0) {
            textView.setBackgroundResource(iResolveSelectableItemBackground);
        }
        textView.setSingleLine(true);
        textView.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda34
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                InstanceDetailsActivity.this.lambda$addInstanceSettingsMenuRow$11(popupWindow, i, view);
            }
        });
        linearLayout.addView(textView, new LinearLayout.LayoutParams(-1, dp(48)));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$addInstanceSettingsMenuRow$11(PopupWindow popupWindow, int i, View view) {
        popupWindow.dismiss();
        handleInstanceSettingsMenuItem(i);
    }

    private void handleInstanceSettingsMenuItem(int i) {
        enableFullscreen();
        if (i == 1) {
            showFolderLocation();
            return;
        }
        if (i == 2) {
            showDeleteInstanceDialog();
            return;
        }
        if (i == 3) {
            showEditInstanceNameDialog();
            return;
        }
        if (i == 4) {
            pickInstanceIcon();
            return;
        }
        if (i == 6) {
            showUpdateVersionDialog();
            return;
        }
        if (i == 7) {
            showUpdateLoaderDialog();
            return;
        }
        if (i == 5) {
            showExportModpackPlatformDialog();
            return;
        }
        if (i == 8) {
            openModpackImportPicker();
        } else if (i == 10) {
            showRepairInstanceDialog();
        } else if (i == 9) {
            showPerInstanceSettingsDialog();
        }
    }

    private void showExportModpackPlatformDialog() {
        enableFullscreen();
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setPadding(dp(22), dp(4), dp(22), 0);
        TextView textView = new TextView(this);
        textView.setText("Choose the export format.");
        textView.setTextSize(2, 16.0f);
        textView.setTextColor(resolveThemeColor(android.R.attr.textColorPrimary, -1));
        linearLayout.addView(textView, new LinearLayout.LayoutParams(-1, -2));
        TextView textView2 = new TextView(this);
        textView2.setText("Sharing note: only upload a pack publicly when every included mod, resource pack, shader, config, and file is allowed on the platform you choose.");
        textView2.setTextSize(2, 13.0f);
        textView2.setTextColor(resolveThemeColor(android.R.attr.textColorSecondary, -3355444));
        textView2.setLineSpacing(0.0f, 1.05f);
        textView2.setMaxLines(4);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = dp(10);
        linearLayout.addView(textView2, layoutParams);
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(false);
        scrollView.setClipToPadding(false);
        scrollView.setPadding(0, 0, 0, dp(4));
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(1);
        scrollView.addView(linearLayout2, new FrameLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-1, Math.min(dp(300), Math.max(dp(190), getResources().getDisplayMetrics().heightPixels / 3)));
        layoutParams2.topMargin = dp(12);
        linearLayout.addView(scrollView, layoutParams2);
        final AlertDialog alertDialogCreate = new AlertDialog.Builder(this).setTitle("Export Modpack").setView(linearLayout).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).create();
        addExportPlatformRow(linearLayout2, "Modrinth", ".mrpack export · best for Modrinth publishing", new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda9
            @Override // java.lang.Runnable
            public final void run() {
                InstanceDetailsActivity.this.lambda$showExportModpackPlatformDialog$12(alertDialogCreate);
            }
        });
        addExportPlatformRow(linearLayout2, "CurseForge", ".zip export · best for CurseForge publishing", new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda10
            @Override // java.lang.Runnable
            public final void run() {
                InstanceDetailsActivity.this.lambda$showExportModpackPlatformDialog$13(alertDialogCreate);
            }
        });
        addExportPlatformRow(linearLayout2, "MultiMC / Prism", ".zip instance export · bundles .minecraft files directly", new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda11
            @Override // java.lang.Runnable
            public final void run() {
                InstanceDetailsActivity.this.lambda$showExportModpackPlatformDialog$14(alertDialogCreate);
            }
        });
        showFullscreenSafeDialog(alertDialogCreate);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showExportModpackPlatformDialog$12(AlertDialog alertDialog) {
        alertDialog.dismiss();
        startModpackExport(ModpackExportManager.Platform.MODRINTH);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showExportModpackPlatformDialog$13(AlertDialog alertDialog) {
        alertDialog.dismiss();
        startModpackExport(ModpackExportManager.Platform.CURSEFORGE);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showExportModpackPlatformDialog$14(AlertDialog alertDialog) {
        alertDialog.dismiss();
        startModpackExport(ModpackExportManager.Platform.MULTIMC);
    }

    private void addExportPlatformRow(LinearLayout linearLayout, String str, String str2, final Runnable runnable) {
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(1);
        linearLayout2.setGravity(16);
        linearLayout2.setMinimumHeight(dp(62));
        linearLayout2.setPadding(dp(16), dp(8), dp(16), dp(8));
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(resolveThemeColor(android.R.attr.colorBackground, 0));
        gradientDrawable.setStroke(dp(1), resolveThemeColor(android.R.attr.textColorHint, -12303292));
        gradientDrawable.setCornerRadius(dp(16));
        linearLayout2.setBackground(gradientDrawable);
        linearLayout2.setClickable(true);
        linearLayout2.setFocusable(true);
        linearLayout2.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda69
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                runnable.run();
            }
        });
        TextView textView = new TextView(this);
        textView.setText(str);
        textView.setTextSize(2, 16.0f);
        textView.setTextColor(resolveThemeColor(android.R.attr.textColorPrimary, -1));
        textView.setTypeface(textView.getTypeface(), 1);
        textView.setSingleLine(true);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        linearLayout2.addView(textView, new LinearLayout.LayoutParams(-1, -2));
        TextView textView2 = new TextView(this);
        textView2.setText(str2);
        textView2.setTextSize(2, 12.0f);
        textView2.setTextColor(resolveThemeColor(android.R.attr.textColorSecondary, -3355444));
        textView2.setSingleLine(true);
        textView2.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = dp(2);
        linearLayout2.addView(textView2, layoutParams);
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-1, -2);
        layoutParams2.topMargin = dp(8);
        linearLayout.addView(linearLayout2, layoutParams2);
    }

    private void startModpackExport(ModpackExportManager.Platform platform) {
        String str;
        this.pendingExportPlatform = platform;
        String str2 = platform == ModpackExportManager.Platform.MODRINTH ? ".mrpack" : ".zip";
        if (platform == ModpackExportManager.Platform.MODRINTH) {
            str = "application/x-modrinth-modpack+zip";
        } else {
            str = "application/zip";
        }
        Intent intent = new Intent("android.intent.action.CREATE_DOCUMENT");
        intent.addCategory("android.intent.category.OPENABLE");
        intent.setType(str);
        intent.putExtra("android.intent.extra.TITLE", sanitizeImportedFileName(this.instanceName, null) + str2);
        try {
            startActivityForResult(intent, REQUEST_EXPORT_MODPACK);
        } catch (ActivityNotFoundException unused) {
            Toast.makeText(this, "No file picker is available for exporting.", 1).show();
        }
    }

    private void exportModpackToUri(final Uri uri, final ModpackExportManager.Platform platform) {
        showUpdateProgressDialog("Export Modpack", "Preparing export...", false, 100, false);
        new Thread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda84
            @Override // java.lang.Runnable
            public final void run() {
                InstanceDetailsActivity.this.lambda$exportModpackToUri$16(platform, uri);
            }
        }, "Export Modpack").start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$exportModpackToUri$16(ModpackExportManager.Platform platform, Uri uri) {
        ModpackExportManager.exportToUri(this, this.gameDirectory, this.instanceName, getGameVersionIdForContent(), this.loader, this.baseVersionId, this.iconFile, platform, uri, new AnonymousClass4());
    }

    /* JADX INFO: renamed from: ca.dnamobile.javalauncher.InstanceDetailsActivity$4, reason: invalid class name */
    class AnonymousClass4 implements ModpackExportManager.Listener {
        AnonymousClass4() {
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onStatus$0(String str) {
            InstanceDetailsActivity.this.setUpdateProgressMessage(str);
        }

        @Override // ca.dnamobile.javalauncher.modmanager.ModpackExportManager.Listener
        public void onStatus(final String str) {
            InstanceDetailsActivity.this.runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$4$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$onStatus$0(str);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onProgress$1(int i, int i2) {
            InstanceDetailsActivity.this.lambda$checkUpdatesForSelectedCategory$76(i, i2);
        }

        @Override // ca.dnamobile.javalauncher.modmanager.ModpackExportManager.Listener
        public void onProgress(final int i, final int i2) {
            InstanceDetailsActivity.this.runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$4$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$onProgress$1(i, i2);
                }
            });
        }

        @Override // ca.dnamobile.javalauncher.modmanager.ModpackExportManager.Listener
        public void onComplete(final String str) {
            InstanceDetailsActivity.this.runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$4$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$onComplete$2(str);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onComplete$2(String str) {
            InstanceDetailsActivity.this.dismissUpdateProgressDialog();
            Toast.makeText(InstanceDetailsActivity.this, str, 1).show();
        }

        @Override // ca.dnamobile.javalauncher.modmanager.ModpackExportManager.Listener
        public void onError(final Throwable th) {
            InstanceDetailsActivity.this.runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$4$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$onError$3(th);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onError$3(Throwable th) {
            InstanceDetailsActivity.this.dismissUpdateProgressDialog();
            Toast.makeText(InstanceDetailsActivity.this, "Export failed: " + (th.getMessage() != null ? th.getMessage() : th.getClass().getSimpleName()), 1).show();
        }
    }

    private void openModpackImportPicker() {
        Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT");
        intent.addCategory("android.intent.category.OPENABLE");
        intent.setType("*/*");
        intent.putExtra("android.intent.extra.MIME_TYPES", new String[]{"application/zip", "application/x-zip-compressed", "application/x-modrinth-modpack+zip", "application/octet-stream"});
        intent.addFlags(1);
        try {
            startActivityForResult(Intent.createChooser(intent, "Import Modpack (.mrpack, CurseForge .zip, MultiMC/Prism .zip)"), REQUEST_IMPORT_MODPACK);
        } catch (ActivityNotFoundException unused) {
            Toast.makeText(this, "No file picker is available.", 1).show();
        }
    }

    private void importModpackFromUri(final Uri uri) {
        showUpdateProgressDialog("Import Modpack", "Preparing import...", false, 100, false);
        new Thread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                InstanceDetailsActivity.this.lambda$importModpackFromUri$17(uri);
            }
        }, "Import Modpack").start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$importModpackFromUri$17(Uri uri) {
        ModpackInstallManager.importFromUri(this, uri, new AnonymousClass5());
    }

    /* JADX INFO: renamed from: ca.dnamobile.javalauncher.InstanceDetailsActivity$5, reason: invalid class name */
    class AnonymousClass5 implements ModpackInstallManager.Listener {
        AnonymousClass5() {
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onStatus$0(String str) {
            InstanceDetailsActivity.this.setUpdateProgressMessage(str);
        }

        @Override // ca.dnamobile.javalauncher.modmanager.ModpackInstallManager.Listener
        public void onStatus(final String str) {
            InstanceDetailsActivity.this.runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$5$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$onStatus$0(str);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onProgress$1(int i, int i2) {
            InstanceDetailsActivity.this.lambda$checkUpdatesForSelectedCategory$76(i, i2);
        }

        @Override // ca.dnamobile.javalauncher.modmanager.ModpackInstallManager.Listener
        public void onProgress(final int i, final int i2) {
            InstanceDetailsActivity.this.runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$5$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$onProgress$1(i, i2);
                }
            });
        }

        @Override // ca.dnamobile.javalauncher.modmanager.ModpackInstallManager.Listener
        public void onComplete(final String str) {
            InstanceDetailsActivity.this.runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$5$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$onComplete$2(str);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onComplete$2(String str) {
            InstanceDetailsActivity.this.dismissUpdateProgressDialog();
            InstanceDetailsActivity.this.setResult(-1);
            Toast.makeText(InstanceDetailsActivity.this, str, 1).show();
        }

        @Override // ca.dnamobile.javalauncher.modmanager.ModpackInstallManager.Listener
        public void onError(final Throwable th) {
            InstanceDetailsActivity.this.runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$5$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$onError$3(th);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onError$3(Throwable th) {
            InstanceDetailsActivity.this.dismissUpdateProgressDialog();
            Toast.makeText(InstanceDetailsActivity.this, "Import failed: " + (th.getMessage() != null ? th.getMessage() : th.getClass().getSimpleName()), 1).show();
        }
    }

    private int getDropdownBottomSafePadding() {
        int identifier = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        return Math.max(dp(16), identifier > 0 ? getResources().getDimensionPixelSize(identifier) : 0);
    }

    private int resolveThemeColor(int i, int i2) {
        TypedValue typedValue = new TypedValue();
        if (!getTheme().resolveAttribute(i, typedValue, true)) {
            return i2;
        }
        if (typedValue.resourceId != 0) {
            try {
                return getResources().getColor(typedValue.resourceId);
            } catch (Throwable unused) {
                return i2;
            }
        }
        return typedValue.data;
    }

    private int resolveSelectableItemBackground() {
        TypedValue typedValue = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)) {
            return typedValue.resourceId;
        }
        return 0;
    }

    private InstanceVersionUpdater.LoaderKind getSupportedLoaderKind() {
        InstanceVersionUpdater.LoaderKind loaderKindResolveLoaderKind = InstanceVersionUpdater.resolveLoaderKind(this.loader);
        if (loaderKindResolveLoaderKind == InstanceVersionUpdater.LoaderKind.FABRIC || loaderKindResolveLoaderKind == InstanceVersionUpdater.LoaderKind.FORGE || loaderKindResolveLoaderKind == InstanceVersionUpdater.LoaderKind.NEOFORGE) {
            return loaderKindResolveLoaderKind;
        }
        return null;
    }

    private void showUpdateVersionDialog() {
        showUpdateProgressDialog("Loading Minecraft versions", "Fetching release versions...", true, 1, true);
        new Thread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda100
            @Override // java.lang.Runnable
            public final void run() {
                InstanceDetailsActivity.this.lambda$showUpdateVersionDialog$20();
            }
        }, "Load Minecraft Releases").start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showUpdateVersionDialog$20() {
        try {
            final ArrayList<InstanceVersionUpdater.MinecraftRelease> arrayListFetchMinecraftReleases = InstanceVersionUpdater.fetchMinecraftReleases();
            runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$showUpdateVersionDialog$18(arrayListFetchMinecraftReleases);
                }
            });
        } catch (Throwable th) {
            Logging.e(TAG, "Unable to load Minecraft release versions", th);
            runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$showUpdateVersionDialog$19(th);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showUpdateVersionDialog$18(ArrayList arrayList) {
        dismissUpdateProgressDialog();
        showMinecraftReleaseSelector(arrayList);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showUpdateVersionDialog$19(Throwable th) {
        dismissUpdateProgressDialog();
        Toast.makeText(this, "Unable to load versions: " + readableError(th), 1).show();
    }

    private void showMinecraftReleaseSelector(final ArrayList<InstanceVersionUpdater.MinecraftRelease> arrayList) {
        if (arrayList.isEmpty()) {
            Toast.makeText(this, "No release versions found.", 1).show();
            return;
        }
        String[] strArr = new String[arrayList.size()];
        String gameVersionIdForContent = getGameVersionIdForContent();
        for (int i = 0; i < arrayList.size(); i++) {
            String str = arrayList.get(i).id;
            if (str.equals(gameVersionIdForContent)) {
                str = str + " (current)";
            }
            strArr[i] = str;
        }
        showVersionSelectionDialog("Update Version", "Pick the Minecraft release to update this instance to. Snapshots are hidden.", strArr, new DialogInterface.OnClickListener() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda35
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i2) {
                InstanceDetailsActivity.this.lambda$showMinecraftReleaseSelector$21(arrayList, dialogInterface, i2);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showMinecraftReleaseSelector$21(ArrayList arrayList, DialogInterface dialogInterface, int i) {
        confirmUpdateVersion(((InstanceVersionUpdater.MinecraftRelease) arrayList.get(i)).id);
    }

    private void showVersionSelectionDialog(String str, String str2, String[] strArr, final DialogInterface.OnClickListener onClickListener) {
        enableFullscreen();
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        int iDp = dp(24);
        linearLayout.setPadding(iDp, dp(4), iDp, 0);
        TextView textView = new TextView(this);
        textView.setText(str2);
        textView.setTextSize(2, 15.0f);
        textView.setTextColor(resolveThemeColor(android.R.attr.textColorPrimary, -1));
        linearLayout.addView(textView, new LinearLayout.LayoutParams(-1, -2));
        ListView listView = new ListView(this);
        listView.setAdapter((ListAdapter) new ArrayAdapter(this, android.R.layout.simple_list_item_1, strArr));
        listView.setChoiceMode(0);
        listView.setDividerHeight(0);
        listView.setClipToPadding(false);
        listView.setPadding(0, dp(8), 0, getDropdownBottomSafePadding());
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, Math.min(dp(400), (Math.min(Math.max(strArr.length, 1), 8) * dp(52)) + dp(8) + getDropdownBottomSafePadding()));
        layoutParams.topMargin = dp(8);
        linearLayout.addView(listView, layoutParams);
        final AlertDialog alertDialogCreate = new AlertDialog.Builder(this).setTitle(str).setView(linearLayout).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).create();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda40
            @Override // android.widget.AdapterView.OnItemClickListener
            public final void onItemClick(AdapterView adapterView, View view, int i, long j) {
                InstanceDetailsActivity.lambda$showVersionSelectionDialog$22(alertDialogCreate, onClickListener, adapterView, view, i, j);
            }
        });
        showFullscreenSafeDialog(alertDialogCreate);
    }

    static /* synthetic */ void lambda$showVersionSelectionDialog$22(AlertDialog alertDialog, DialogInterface.OnClickListener onClickListener, AdapterView adapterView, View view, int i, long j) {
        alertDialog.dismiss();
        onClickListener.onClick(alertDialog, i);
    }

    private void confirmUpdateVersion(final String str) {
        String str2;
        InstanceVersionUpdater.LoaderKind loaderKindResolveLoaderKind = InstanceVersionUpdater.resolveLoaderKind(this.loader);
        if (loaderKindResolveLoaderKind == InstanceVersionUpdater.LoaderKind.FABRIC || loaderKindResolveLoaderKind == InstanceVersionUpdater.LoaderKind.FORGE || loaderKindResolveLoaderKind == InstanceVersionUpdater.LoaderKind.NEOFORGE) {
            str2 = "Update this instance to Minecraft " + str + "?\n\n" + loaderKindResolveLoaderKind.displayName + " will also be updated to the latest loader available for that Minecraft version.";
        } else {
            str2 = "Update this vanilla instance to Minecraft " + str + "?";
        }
        new AlertDialog.Builder(this).setTitle("Update Version").setMessage(str2).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton("Update", new DialogInterface.OnClickListener() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda0
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                InstanceDetailsActivity.this.lambda$confirmUpdateVersion$23(str, dialogInterface, i);
            }
        }).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$confirmUpdateVersion$23(String str, DialogInterface dialogInterface, int i) {
        runVersionUpdate(str);
    }

    private void runVersionUpdate(final String str) {
        if (this.rootDirectory == null || this.gameDirectory == null) {
            Toast.makeText(this, "Missing instance folder.", 1).show();
            return;
        }
        setVersionUpdateInProgress(true);
        showUpdateProgressDialog("Update Version", "Preparing update...", false, 4, false);
        new Thread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda38
            @Override // java.lang.Runnable
            public final void run() {
                InstanceDetailsActivity.this.lambda$runVersionUpdate$26(str);
            }
        }, "Update Instance Version").start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$runVersionUpdate$26(String str) {
        try {
            final InstanceVersionUpdater.UpdateResult updateResultUpdateInstanceVersion = InstanceVersionUpdater.updateInstanceVersion(this, this.rootDirectory, this.gameDirectory, this.instanceName, this.loader, str, new AnonymousClass6());
            runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda70
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$runVersionUpdate$24(updateResultUpdateInstanceVersion);
                }
            });
        } catch (Throwable th) {
            Logging.e(TAG, "Unable to update instance version", th);
            runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda71
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$runVersionUpdate$25(th);
                }
            });
        }
    }

    /* JADX INFO: renamed from: ca.dnamobile.javalauncher.InstanceDetailsActivity$6, reason: invalid class name */
    class AnonymousClass6 implements InstanceVersionUpdater.Listener {
        AnonymousClass6() {
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onStatus$0(String str) {
            InstanceDetailsActivity.this.setUpdateProgressMessage(str);
        }

        @Override // ca.dnamobile.javalauncher.instance.InstanceVersionUpdater.Listener
        public void onStatus(final String str) {
            InstanceDetailsActivity.this.runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$6$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$onStatus$0(str);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onProgress$1(int i, int i2) {
            InstanceDetailsActivity.this.lambda$checkUpdatesForSelectedCategory$76(i, i2);
        }

        @Override // ca.dnamobile.javalauncher.instance.InstanceVersionUpdater.Listener
        public void onProgress(final int i, final int i2) {
            InstanceDetailsActivity.this.runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$6$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$onProgress$1(i, i2);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$runVersionUpdate$24(InstanceVersionUpdater.UpdateResult updateResult) {
        applyVersionUpdateResult(updateResult, "Version updated");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$runVersionUpdate$25(Throwable th) {
        dismissUpdateProgressDialog();
        setVersionUpdateInProgress(false);
        Toast.makeText(this, "Version update failed: " + readableError(th), 1).show();
    }

    private void showUpdateLoaderDialog() {
        final InstanceVersionUpdater.LoaderKind supportedLoaderKind = getSupportedLoaderKind();
        if (supportedLoaderKind == null) {
            Toast.makeText(this, "Update Loader is only available for Fabric, Forge, and NeoForge instances.", 1).show();
            return;
        }
        final String gameVersionIdForContent = getGameVersionIdForContent();
        showUpdateProgressDialog("Loading " + supportedLoaderKind.displayName + " versions", "Fetching loader versions for Minecraft " + gameVersionIdForContent + "...", true, 1, true);
        new Thread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda98
            @Override // java.lang.Runnable
            public final void run() {
                InstanceDetailsActivity.this.lambda$showUpdateLoaderDialog$29(supportedLoaderKind, gameVersionIdForContent);
            }
        }, "Load Loader Versions").start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showUpdateLoaderDialog$29(final InstanceVersionUpdater.LoaderKind loaderKind, String str) {
        try {
            final ArrayList<InstanceVersionUpdater.LoaderVersion> arrayListFetchLoaderVersions = InstanceVersionUpdater.fetchLoaderVersions(loaderKind, str);
            runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda81
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$showUpdateLoaderDialog$27(loaderKind, arrayListFetchLoaderVersions);
                }
            });
        } catch (Throwable th) {
            Logging.e(TAG, "Unable to load loader versions", th);
            runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda82
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$showUpdateLoaderDialog$28(th);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showUpdateLoaderDialog$27(InstanceVersionUpdater.LoaderKind loaderKind, ArrayList arrayList) {
        dismissUpdateProgressDialog();
        showLoaderVersionSelector(loaderKind, arrayList);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showUpdateLoaderDialog$28(Throwable th) {
        dismissUpdateProgressDialog();
        Toast.makeText(this, "Unable to load loader versions: " + readableError(th), 1).show();
    }

    private void showLoaderVersionSelector(InstanceVersionUpdater.LoaderKind loaderKind, final ArrayList<InstanceVersionUpdater.LoaderVersion> arrayList) {
        String str;
        if (arrayList.isEmpty()) {
            Toast.makeText(this, "No " + loaderKind.displayName + " versions found for Minecraft " + getGameVersionIdForContent() + ".", 1).show();
            return;
        }
        final String strResolveCurrentLoaderVersion = InstanceVersionUpdater.resolveCurrentLoaderVersion(loaderKind, this.baseVersionId, getGameVersionIdForContent());
        String[] strArr = new String[arrayList.size()];
        for (int i = 0; i < arrayList.size(); i++) {
            strArr[i] = arrayList.get(i).getDisplayLabel(strResolveCurrentLoaderVersion);
        }
        if (isBlank(strResolveCurrentLoaderVersion)) {
            str = "";
        } else {
            str = "\n\nCurrent " + loaderKind.displayName + " loader: " + strResolveCurrentLoaderVersion;
        }
        showVersionSelectionDialog("Update Loader", "Pick the " + loaderKind.displayName + " loader version to install for Minecraft " + getGameVersionIdForContent() + "." + str, strArr, new DialogInterface.OnClickListener() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda99
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i2) {
                InstanceDetailsActivity.this.lambda$showLoaderVersionSelector$30(arrayList, strResolveCurrentLoaderVersion, dialogInterface, i2);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showLoaderVersionSelector$30(ArrayList arrayList, String str, DialogInterface dialogInterface, int i) {
        confirmUpdateLoader((InstanceVersionUpdater.LoaderVersion) arrayList.get(i), str);
    }

    private void confirmUpdateLoader(final InstanceVersionUpdater.LoaderVersion loaderVersion, String str) {
        boolean z = InstanceVersionUpdater.isSameLoaderVersion(loaderVersion.displayVersion, str) || InstanceVersionUpdater.isSameLoaderVersion(loaderVersion.installVersion, str);
        AlertDialog.Builder negativeButton = new AlertDialog.Builder(this).setTitle("Update Loader").setMessage(z ? "This instance is already using " + loaderVersion.kind.displayName + " " + loaderVersion.displayVersion + " for Minecraft " + loaderVersion.minecraftVersion + "." : "Update this instance to " + loaderVersion.kind.displayName + " " + loaderVersion.displayVersion + " for Minecraft " + loaderVersion.minecraftVersion + "?").setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null);
        if (z) {
            negativeButton.setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null);
        } else {
            negativeButton.setPositiveButton("Update", new DialogInterface.OnClickListener() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda102
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    InstanceDetailsActivity.this.lambda$confirmUpdateLoader$31(loaderVersion, dialogInterface, i);
                }
            });
        }
        negativeButton.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$confirmUpdateLoader$31(InstanceVersionUpdater.LoaderVersion loaderVersion, DialogInterface dialogInterface, int i) {
        runLoaderUpdate(loaderVersion);
    }

    private void runLoaderUpdate(final InstanceVersionUpdater.LoaderVersion loaderVersion) {
        if (this.rootDirectory == null || this.gameDirectory == null) {
            Toast.makeText(this, "Missing instance folder.", 1).show();
            return;
        }
        setVersionUpdateInProgress(true);
        showUpdateProgressDialog("Update Loader", "Preparing loader update...", false, 4, false);
        new Thread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda50
            @Override // java.lang.Runnable
            public final void run() {
                InstanceDetailsActivity.this.lambda$runLoaderUpdate$34(loaderVersion);
            }
        }, "Update Instance Loader").start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$runLoaderUpdate$34(InstanceVersionUpdater.LoaderVersion loaderVersion) {
        try {
            final InstanceVersionUpdater.UpdateResult updateResultUpdateInstanceLoader = InstanceVersionUpdater.updateInstanceLoader(this, this.rootDirectory, this.gameDirectory, this.instanceName, loaderVersion, new AnonymousClass7());
            runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda94
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$runLoaderUpdate$32(updateResultUpdateInstanceLoader);
                }
            });
        } catch (Throwable th) {
            Logging.e(TAG, "Unable to update instance loader", th);
            runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda105
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$runLoaderUpdate$33(th);
                }
            });
        }
    }

    /* JADX INFO: renamed from: ca.dnamobile.javalauncher.InstanceDetailsActivity$7, reason: invalid class name */
    class AnonymousClass7 implements InstanceVersionUpdater.Listener {
        AnonymousClass7() {
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onStatus$0(String str) {
            InstanceDetailsActivity.this.setUpdateProgressMessage(str);
        }

        @Override // ca.dnamobile.javalauncher.instance.InstanceVersionUpdater.Listener
        public void onStatus(final String str) {
            InstanceDetailsActivity.this.runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$7$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$onStatus$0(str);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onProgress$1(int i, int i2) {
            InstanceDetailsActivity.this.lambda$checkUpdatesForSelectedCategory$76(i, i2);
        }

        @Override // ca.dnamobile.javalauncher.instance.InstanceVersionUpdater.Listener
        public void onProgress(final int i, final int i2) {
            InstanceDetailsActivity.this.runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$7$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$onProgress$1(i, i2);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$runLoaderUpdate$32(InstanceVersionUpdater.UpdateResult updateResult) {
        applyVersionUpdateResult(updateResult, "Loader updated");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$runLoaderUpdate$33(Throwable th) {
        dismissUpdateProgressDialog();
        setVersionUpdateInProgress(false);
        Toast.makeText(this, "Loader update failed: " + readableError(th), 1).show();
    }

    private void applyVersionUpdateResult(InstanceVersionUpdater.UpdateResult updateResult, String str) {
        this.loader = updateResult.loader;
        this.baseVersionId = updateResult.baseVersionId;
        this.minecraftVersionId = updateResult.minecraftVersionId;
        this.versionType = updateResult.versionType;
        updateIntentExtras();
        bindHeader();
        refreshContentList();
        dismissUpdateProgressDialog();
        setVersionUpdateInProgress(false);
        setResult(-1);
        Toast.makeText(this, str + ": Minecraft " + updateResult.minecraftVersionId + (updateResult.loaderVersion == null ? "" : " · " + updateResult.loader + " " + updateResult.loaderVersion), 1).show();
    }

    private void setVersionUpdateInProgress(boolean z) {
        this.binding.buttonPlay.setEnabled(!z);
        this.binding.buttonInstanceSettings.setEnabled(!z);
        boolean z2 = false;
        this.binding.buttonBrowseContent.setEnabled(!z && canBrowseSelectedCategory());
        this.binding.buttonAddMods.setEnabled(!z && canUploadSelectedCategory());
        this.binding.buttonCheckContentUpdates.setEnabled(!z && canCheckUpdatesForSelectedCategory());
        MaterialButton materialButton = this.binding.buttonUpdateAllContent;
        if (!z && hasAvailableUpdatesForSelectedCategory()) {
            z2 = true;
        }
        materialButton.setEnabled(z2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String readableError(Throwable th) {
        String message = th.getMessage();
        return (message == null || message.trim().isEmpty()) ? th.getClass().getSimpleName() : message;
    }

    private void showRepairInstanceDialog() {
        String gameVersionIdForContent = getGameVersionIdForContent();
        String str = PathManager.DIR_MINECRAFT_HOME;
        StringBuilder sb = new StringBuilder("Repair this instance for Minecraft ");
        sb.append(gameVersionIdForContent).append("?\n\nThis will redownload the vanilla game files, libraries, asset index, and missing assets needed by the current launcher storage location.\n\nActive launcher home:\n");
        if (str == null) {
            str = "(unknown)";
        }
        sb.append(str).append("\n\nThis does not delete saves, mods, shaderpacks, or resource packs.");
        new AlertDialog.Builder(this).setTitle("Repair Instance").setMessage(sb.toString()).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton("Repair", new DialogInterface.OnClickListener() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda101
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                InstanceDetailsActivity.this.lambda$showRepairInstanceDialog$35(dialogInterface, i);
            }
        }).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showRepairInstanceDialog$35(DialogInterface dialogInterface, int i) {
        runRepairInstance();
    }

    private void runRepairInstance() {
        final String gameVersionIdForContent = getGameVersionIdForContent();
        if (isBlank(gameVersionIdForContent)) {
            Toast.makeText(this, "Missing Minecraft version.", 1).show();
            return;
        }
        setVersionUpdateInProgress(true);
        showUpdateProgressDialog("Repair Instance", "Preparing repair...", false, 100, false);
        new Thread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda103
            @Override // java.lang.Runnable
            public final void run() {
                InstanceDetailsActivity.this.lambda$runRepairInstance$40(gameVersionIdForContent);
            }
        }, "Repair Instance").start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$runRepairInstance$40(final String str) {
        try {
            PathManager.initContextConstants(this);
            MinecraftVersion minecraftVersionResolveRepairManifestVersion = resolveRepairManifestVersion(str);
            if (minecraftVersionResolveRepairManifestVersion.getMetadataUrl() == null || minecraftVersionResolveRepairManifestVersion.getMetadataUrl().trim().isEmpty()) {
                throw new IllegalStateException("No Mojang metadata URL found for " + str);
            }
            MinecraftVersionInstaller.installVanillaVersion(this, minecraftVersionResolveRepairManifestVersion, new MinecraftVersionInstaller.InstallProgressListener() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda42
                @Override // ca.dnamobile.javalauncher.ui.version.MinecraftVersionInstaller.InstallProgressListener
                public final void onProgress(int i, String str2) {
                    InstanceDetailsActivity.this.lambda$runRepairInstance$37(i, str2);
                }
            });
            runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda43
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$runRepairInstance$38(str);
                }
            });
        } catch (Throwable th) {
            Logging.e(TAG, "Unable to repair instance " + this.instanceName, th);
            runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda44
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$runRepairInstance$39(th);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$runRepairInstance$37(final int i, final String str) {
        runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda58
            @Override // java.lang.Runnable
            public final void run() {
                InstanceDetailsActivity.this.lambda$runRepairInstance$36(i, str);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$runRepairInstance$36(int i, String str) {
        lambda$checkUpdatesForSelectedCategory$76(i, 100);
        setUpdateProgressMessage(str);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$runRepairInstance$38(String str) {
        dismissUpdateProgressDialog();
        setVersionUpdateInProgress(false);
        setResult(-1);
        Toast.makeText(this, "Repair complete for Minecraft " + str + ".", 1).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$runRepairInstance$39(Throwable th) {
        dismissUpdateProgressDialog();
        setVersionUpdateInProgress(false);
        Toast.makeText(this, "Repair failed: " + readableError(th), 1).show();
    }

    private MinecraftVersion resolveRepairManifestVersion(String str) throws Exception {
        for (MinecraftVersion minecraftVersion : MinecraftVersionManifestClient.loadVersions(this)) {
            if (str.equals(minecraftVersion.getId())) {
                return minecraftVersion;
            }
        }
        throw new IllegalStateException("Minecraft version not found in Mojang manifest: " + str);
    }

    private void launchInstance() {
        lambda$launchInstance$41(null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: renamed from: launchInstance, reason: merged with bridge method [inline-methods] */
    public void lambda$launchInstance$41(final String str) {
        if (requireMicrosoftLoginHistoryBeforeLaunch(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda85
            @Override // java.lang.Runnable
            public final void run() {
                InstanceDetailsActivity.this.lambda$launchInstance$41(str);
            }
        })) {
            return;
        }
        Renderers.reload(this);
        RendererInterface selectedRenderer = Renderers.getSelectedRenderer(this);
        if (MobileGluesConfigHelper.isMobileGluesRenderer(selectedRenderer) && !MobileGluesConfigHelper.hasStorageAccess(this)) {
            showRendererPluginStorageDialog(selectedRenderer, str);
        } else {
            continueLaunchInstance(str);
        }
    }

    private void continueLaunchInstance(String str) {
        LauncherPreferences.recordInstancePlayed(this, this.instanceId);
        Intent intent = new Intent(this, (Class<?>) GameActivity.class);
        intent.putExtra(GameActivity.EXTRA_VERSION_ID, this.isolated ? this.instanceName : this.baseVersionId);
        if (str != null && !str.trim().isEmpty()) {
            intent.putExtra(GameActivity.EXTRA_QUICK_PLAY_WORLD, str);
        }
        startActivity(intent);
    }

    private void showRendererPluginStorageDialog(final RendererInterface rendererInterface, final String str) {
        new AlertDialog.Builder(this).setTitle(R.string.renderer_plugin_storage_title).setMessage(getString(R.string.renderer_plugin_storage_message, new Object[]{rendererInterface.getRendererName(), MobileGluesConfigHelper.getConfigFile().getAbsolutePath()})).setNegativeButton(R.string.renderer_plugin_continue_anyway, new DialogInterface.OnClickListener() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda23
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                InstanceDetailsActivity.this.lambda$showRendererPluginStorageDialog$42(str, dialogInterface, i);
            }
        }).setNeutralButton(R.string.renderer_plugin_open_settings, new DialogInterface.OnClickListener() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda24
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                InstanceDetailsActivity.this.lambda$showRendererPluginStorageDialog$43(rendererInterface, dialogInterface, i);
            }
        }).setPositiveButton(R.string.button_grant_renderer_storage_access, new DialogInterface.OnClickListener() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda25
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                InstanceDetailsActivity.this.lambda$showRendererPluginStorageDialog$44(dialogInterface, i);
            }
        }).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showRendererPluginStorageDialog$42(String str, DialogInterface dialogInterface, int i) {
        continueLaunchInstance(str);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showRendererPluginStorageDialog$43(RendererInterface rendererInterface, DialogInterface dialogInterface, int i) {
        RendererPluginManager.openPluginApp(this, rendererInterface);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showRendererPluginStorageDialog$44(DialogInterface dialogInterface, int i) {
        try {
            startActivity(MobileGluesConfigHelper.buildStorageAccessIntent(this));
        } catch (Throwable unused) {
            Toast.makeText(this, R.string.renderer_storage_access_open_failed, 1).show();
        }
    }

    private void continueLaunchInstance() {
        LauncherPreferences.recordInstancePlayed(this, this.instanceId);
        Intent intent = new Intent(this, (Class<?>) GameActivity.class);
        intent.putExtra(GameActivity.EXTRA_VERSION_ID, this.isolated ? this.instanceName : this.baseVersionId);
        startActivity(intent);
    }

    private void showFolderLocation() {
        File file = this.gameDirectory;
        if (file == null) {
            file = this.rootDirectory;
        }
        if (file == null) {
            return;
        }
        if (!file.exists() && !file.mkdirs()) {
            showFolderPathFallback(file);
        } else {
            if (tryOpenExternalStorageFolder(file)) {
                return;
            }
            showFolderPathFallback(file);
        }
    }

    private boolean tryOpenExternalStorageFolder(File file) {
        String strBuildExternalStorageDocumentId = buildExternalStorageDocumentId(file);
        if (strBuildExternalStorageDocumentId == null) {
            return false;
        }
        Uri uriBuildDocumentUri = DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", strBuildExternalStorageDocumentId);
        Uri uriBuildTreeDocumentUri = DocumentsContract.buildTreeDocumentUri("com.android.externalstorage.documents", strBuildExternalStorageDocumentId);
        Uri uriBuildRootUri = DocumentsContract.buildRootUri("com.android.externalstorage.documents", "primary");
        ArrayList<Intent> arrayList = new ArrayList<>();
        addFolderOpenAttempts(arrayList, uriBuildDocumentUri, uriBuildTreeDocumentUri, uriBuildRootUri, "com.android.documentsui");
        addFolderOpenAttempts(arrayList, uriBuildDocumentUri, uriBuildTreeDocumentUri, uriBuildRootUri, "com.google.android.documentsui");
        addFolderOpenAttempts(arrayList, uriBuildDocumentUri, uriBuildTreeDocumentUri, uriBuildRootUri, null);
        Iterator<Intent> it = arrayList.iterator();
        Throwable th = null;
        while (it.hasNext()) {
            try {
                startActivity(it.next());
                return true;
            } catch (Throwable th2) {
                th = th2;
            }
        }
        if (th != null) {
            Logging.e(TAG, "Unable to open instance folder in file manager: " + file.getAbsolutePath(), th);
        }
        return false;
    }

    private void addFolderOpenAttempts(ArrayList<Intent> arrayList, Uri uri, Uri uri2, Uri uri3, String str) {
        arrayList.add(buildViewFolderIntent(uri, str));
        arrayList.add(buildViewFolderIntent(uri2, str));
        arrayList.add(buildRootFolderIntent(uri3, uri, str));
        arrayList.add(buildRootFolderIntent(uri3, uri2, str));
    }

    private Intent buildViewFolderIntent(Uri uri, String str) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setDataAndType(uri, "vnd.android.document/directory");
        intent.addFlags(3);
        intent.putExtra("android.content.extra.SHOW_ADVANCED", true);
        if (str != null) {
            intent.setPackage(str);
        }
        return intent;
    }

    private Intent buildRootFolderIntent(Uri uri, Uri uri2, String str) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setDataAndType(uri, "vnd.android.document/root");
        intent.putExtra("android.provider.extra.INITIAL_URI", uri2);
        intent.putExtra("android.content.extra.SHOW_ADVANCED", true);
        intent.addFlags(3);
        if (str != null) {
            intent.setPackage(str);
        }
        return intent;
    }

    private String buildExternalStorageDocumentId(File file) {
        String strSafeCanonicalPath = safeCanonicalPath(file);
        if (strSafeCanonicalPath.equals("/storage/emulated/0")) {
            return "primary:";
        }
        if (!strSafeCanonicalPath.startsWith("/storage/emulated/0" + File.separator)) {
            return null;
        }
        return "primary:" + strSafeCanonicalPath.substring("/storage/emulated/0".length() + 1).replace(File.separatorChar, '/');
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String safeCanonicalPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException unused) {
            return file.getAbsolutePath();
        }
    }

    private void showFolderPathFallback(File file) {
        final String absolutePath = file.getAbsolutePath();
        new AlertDialog.Builder(this).setTitle(R.string.instance_folder_open_failed_title).setMessage(getString(R.string.instance_folder_open_failed_message, new Object[]{absolutePath})).setNegativeButton(android.R.string.ok, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.instance_folder_copy_path, new DialogInterface.OnClickListener() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda78
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                InstanceDetailsActivity.this.lambda$showFolderPathFallback$45(absolutePath, dialogInterface, i);
            }
        }).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showFolderPathFallback$45(String str, DialogInterface dialogInterface, int i) {
        copyTextToClipboard(str);
    }

    private void copyTextToClipboard(String str) {
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService("clipboard");
        if (clipboardManager != null) {
            clipboardManager.setPrimaryClip(ClipData.newPlainText(getString(R.string.instance_settings_view_folder), str));
            Toast.makeText(this, R.string.instance_folder_path_copied, 0).show();
        }
    }

    private void showEditInstanceNameDialog() {
        if (!this.isolated) {
            Toast.makeText(this, R.string.instance_rename_shared_not_supported, 1).show();
            return;
        }
        final EditText editText = new EditText(this);
        editText.setSingleLine(true);
        editText.setSelectAllOnFocus(true);
        editText.setText(this.instanceName);
        editText.setInputType(8193);
        FrameLayout frameLayout = new FrameLayout(this);
        int iDp = dp(20);
        frameLayout.setPadding(iDp, dp(8), iDp, 0);
        frameLayout.addView(editText, new FrameLayout.LayoutParams(-1, -2));
        final AlertDialog alertDialogCreate = new AlertDialog.Builder(this).setTitle(R.string.instance_settings_edit_name).setView(frameLayout).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.instance_settings_save_name, (DialogInterface.OnClickListener) null).create();
        alertDialogCreate.setOnShowListener(new DialogInterface.OnShowListener() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda86
            @Override // android.content.DialogInterface.OnShowListener
            public final void onShow(DialogInterface dialogInterface) {
                InstanceDetailsActivity.this.lambda$showEditInstanceNameDialog$47(alertDialogCreate, editText, dialogInterface);
            }
        });
        alertDialogCreate.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showEditInstanceNameDialog$47(final AlertDialog alertDialog, final EditText editText, DialogInterface dialogInterface) {
        alertDialog.getButton(-1).setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda20
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                InstanceDetailsActivity.this.lambda$showEditInstanceNameDialog$46(editText, alertDialog, view);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showEditInstanceNameDialog$46(EditText editText, AlertDialog alertDialog, View view) {
        String strSanitizeInstanceName = sanitizeInstanceName(editText.getText() == null ? "" : editText.getText().toString());
        if (isBlank(strSanitizeInstanceName)) {
            editText.setError(getString(R.string.instance_rename_empty));
        } else if (strSanitizeInstanceName.equals(this.instanceName)) {
            alertDialog.dismiss();
        } else {
            renameInstance(strSanitizeInstanceName, alertDialog);
        }
    }

    private void renameInstance(final String str, final AlertDialog alertDialog) {
        final File file = this.rootDirectory;
        if (file == null) {
            Toast.makeText(this, R.string.instance_rename_failed_missing_root, 1).show();
        } else {
            setInstanceEditInProgress(true);
            new Thread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda33
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$renameInstance$50(file, str, alertDialog);
                }
            }, "Rename Instance").start();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$renameInstance$50(File file, String str, final AlertDialog alertDialog) {
        final String str2 = this.instanceName;
        try {
            final LauncherInstance launcherInstanceRenameInstance = LauncherInstanceManager.renameInstance(this, file, str);
            runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$renameInstance$48(launcherInstanceRenameInstance, alertDialog, str2);
                }
            });
        } catch (Throwable th) {
            Logging.e(TAG, "Unable to rename instance " + str2 + " to " + str, th);
            runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$renameInstance$49(th);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$renameInstance$48(LauncherInstance launcherInstance, AlertDialog alertDialog, String str) {
        applyUpdatedInstance(launcherInstance);
        setInstanceEditInProgress(false);
        setResult(-1);
        alertDialog.dismiss();
        Toast.makeText(this, getString(R.string.instance_rename_success, new Object[]{str, launcherInstance.getName()}), 0).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$renameInstance$49(Throwable th) {
        setInstanceEditInProgress(false);
        Toast.makeText(this, getString(R.string.instance_rename_failed, new Object[]{th.getMessage() != null ? th.getMessage() : th.getClass().getSimpleName()}), 1).show();
    }

    private File remapPathAfterDirectoryRename(File file, File file2, File file3) {
        String strSafeCanonicalPath = safeCanonicalPath(file);
        String strSafeCanonicalPath2 = safeCanonicalPath(file3);
        return strSafeCanonicalPath2.equals(strSafeCanonicalPath) ? file2 : strSafeCanonicalPath2.startsWith(new StringBuilder().append(strSafeCanonicalPath).append(File.separator).toString()) ? new File(file2, strSafeCanonicalPath2.substring(strSafeCanonicalPath.length() + 1)) : file3;
    }

    private void applyUpdatedInstance(LauncherInstance launcherInstance) {
        this.instanceId = launcherInstance.getId();
        this.instanceName = launcherInstance.getName();
        this.loader = launcherInstance.getLoader();
        this.baseVersionId = launcherInstance.getBaseVersionId();
        this.minecraftVersionId = launcherInstance.getMinecraftVersionId();
        this.versionType = launcherInstance.getVersionType();
        this.rootDirectory = launcherInstance.getRootDirectory();
        this.gameDirectory = launcherInstance.getGameDirectory();
        this.iconFile = launcherInstance.getIconFile();
        this.isolated = launcherInstance.isIsolated();
        resetContentDirectories();
        updateIntentExtras();
        bindHeader();
        refreshContentList();
    }

    private void resetContentDirectories() {
        this.modsDirectory = new File(this.gameDirectory, "mods");
        this.shaderpacksDirectory = new File(this.gameDirectory, "shaderpacks");
        this.resourcepacksDirectory = new File(this.gameDirectory, "resourcepacks");
        this.worldsDirectory = new File(this.gameDirectory, "saves");
    }

    private void updateIntentExtras() {
        Intent intent = getIntent();
        intent.putExtra(EXTRA_INSTANCE_ID, this.instanceId);
        intent.putExtra(EXTRA_INSTANCE_NAME, this.instanceName);
        intent.putExtra(EXTRA_INSTANCE_LOADER, this.loader);
        intent.putExtra(EXTRA_BASE_VERSION_ID, this.baseVersionId);
        intent.putExtra(EXTRA_MINECRAFT_VERSION_ID, this.minecraftVersionId);
        intent.putExtra(EXTRA_VERSION_TYPE, this.versionType);
        File file = this.rootDirectory;
        intent.putExtra(EXTRA_ROOT_DIRECTORY, file != null ? file.getAbsolutePath() : "");
        File file2 = this.gameDirectory;
        intent.putExtra(EXTRA_GAME_DIRECTORY, file2 != null ? file2.getAbsolutePath() : "");
        File file3 = this.iconFile;
        intent.putExtra(EXTRA_ICON_FILE, file3 != null ? file3.getAbsolutePath() : "");
        intent.putExtra(EXTRA_ISOLATED, this.isolated);
    }

    private void pickInstanceIcon() {
        Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT");
        intent.addCategory("android.intent.category.OPENABLE");
        intent.setType("image/*");
        intent.addFlags(65);
        try {
            startActivityForResult(Intent.createChooser(intent, getString(R.string.instance_icon_picker_title)), REQUEST_PICK_INSTANCE_ICON);
        } catch (ActivityNotFoundException unused) {
            Toast.makeText(this, R.string.instance_icon_picker_missing, 1).show();
        }
    }

    private void savePickedInstanceIcon(final Uri uri) {
        final File file = this.rootDirectory;
        if (file == null) {
            Toast.makeText(this, R.string.instance_rename_failed_missing_root, 1).show();
        } else {
            setInstanceEditInProgress(true);
            new Thread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda6
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$savePickedInstanceIcon$53(file, uri);
                }
            }, "Update Instance Icon").start();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$savePickedInstanceIcon$53(File file, Uri uri) {
        try {
            final LauncherInstance launcherInstanceUpdateInstanceIcon = LauncherInstanceManager.updateInstanceIcon(this, file, uri);
            runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda66
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$savePickedInstanceIcon$51(launcherInstanceUpdateInstanceIcon);
                }
            });
        } catch (Throwable th) {
            Logging.e(TAG, "Unable to update instance icon", th);
            runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda67
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$savePickedInstanceIcon$52(th);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$savePickedInstanceIcon$51(LauncherInstance launcherInstance) {
        applyUpdatedInstance(launcherInstance);
        bindInstanceIcon();
        setInstanceEditInProgress(false);
        setResult(-1);
        Toast.makeText(this, R.string.instance_icon_update_success, 0).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$savePickedInstanceIcon$52(Throwable th) {
        setInstanceEditInProgress(false);
        Toast.makeText(this, getString(R.string.instance_icon_update_failed, new Object[]{th.getMessage() != null ? th.getMessage() : th.getClass().getSimpleName()}), 1).show();
    }

    private File resolveIconTargetFile(Uri uri) throws Exception {
        File file = this.iconFile;
        if (file != null && file.getParentFile() != null && this.iconFile.getParentFile().canWrite()) {
            return this.iconFile;
        }
        File file2 = this.rootDirectory;
        if (file2 == null) {
            file2 = this.gameDirectory;
        }
        if (file2 == null) {
            throw new IllegalStateException("Missing instance folder.");
        }
        if (!file2.exists() && !file2.mkdirs()) {
            throw new IllegalStateException("Unable to create icon folder: " + file2.getAbsolutePath());
        }
        return new File(file2, "icon" + resolveImageExtension(uri));
    }

    private String resolveImageExtension(Uri uri) {
        String lowerCase = resolveDisplayName(uri).toLowerCase(Locale.US);
        if (lowerCase.endsWith(".jpg") || lowerCase.endsWith(".jpeg")) {
            return ".jpg";
        }
        if (lowerCase.endsWith(".webp")) {
            return ".webp";
        }
        if (lowerCase.endsWith(".gif")) {
            return ".gif";
        }
        return ".png";
    }

    private void setInstanceEditInProgress(boolean z) {
        this.binding.buttonPlay.setEnabled(!z);
        this.binding.buttonInstanceSettings.setEnabled(!z);
        boolean z2 = false;
        this.binding.buttonBrowseContent.setEnabled(!z && canBrowseSelectedCategory());
        this.binding.buttonAddMods.setEnabled(!z && canUploadSelectedCategory());
        this.binding.buttonCheckContentUpdates.setEnabled(!z && canCheckUpdatesForSelectedCategory());
        MaterialButton materialButton = this.binding.buttonUpdateAllContent;
        if (!z && hasAvailableUpdatesForSelectedCategory()) {
            z2 = true;
        }
        materialButton.setEnabled(z2);
    }

    private void showDeleteInstanceDialog() {
        String absolutePath;
        int i;
        if (!this.isolated) {
            ArrayList<String> arrayListFindSharedVersionDependents = LauncherInstanceManager.findSharedVersionDependents(this, this.baseVersionId);
            if (!arrayListFindSharedVersionDependents.isEmpty()) {
                new AlertDialog.Builder(this).setTitle(getString(R.string.delete_shared_instance_blocked_title, new Object[]{this.instanceName})).setMessage(getString(R.string.delete_shared_instance_blocked_message, new Object[]{this.instanceName, LauncherInstanceManager.formatDependentVersionList(arrayListFindSharedVersionDependents)})).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).show();
                return;
            }
        }
        try {
            absolutePath = LauncherInstanceManager.getDeleteTargetDirectory(this.baseVersionId, this.rootDirectory, this.isolated).getAbsolutePath();
        } catch (Throwable unused) {
            absolutePath = this.rootDirectory.getAbsolutePath();
        }
        if (this.isolated) {
            i = R.string.delete_instance_message;
        } else {
            i = R.string.delete_shared_instance_message;
        }
        new AlertDialog.Builder(this).setTitle(getString(R.string.delete_instance_title, new Object[]{this.instanceName})).setMessage(getString(i, new Object[]{this.instanceName, absolutePath})).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.button_delete_forever, new DialogInterface.OnClickListener() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda104
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i2) {
                InstanceDetailsActivity.this.lambda$showDeleteInstanceDialog$54(dialogInterface, i2);
            }
        }).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showDeleteInstanceDialog$54(DialogInterface dialogInterface, int i) {
        deleteInstance();
    }

    private void deleteInstance() {
        setDeleteInProgress(true);
        new Thread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda77
            @Override // java.lang.Runnable
            public final void run() {
                InstanceDetailsActivity.this.lambda$deleteInstance$57();
            }
        }, "Delete Instance From Details").start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$deleteInstance$57() {
        try {
            LauncherInstanceManager.deleteInstance(this, this.instanceId, this.baseVersionId, this.rootDirectory, this.isolated);
            LauncherPreferences.clearInstancePlayed(this, this.instanceId);
            runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda15
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$deleteInstance$55();
                }
            });
        } catch (Throwable th) {
            Logging.e(TAG, "Unable to delete instance " + this.instanceName, th);
            runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda16
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$deleteInstance$56(th);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$deleteInstance$55() {
        Toast.makeText(this, getString(R.string.delete_instance_deleted, new Object[]{this.instanceName}), 0).show();
        setResult(-1);
        finish();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$deleteInstance$56(Throwable th) {
        setDeleteInProgress(false);
        Toast.makeText(this, getString(R.string.delete_instance_failed, new Object[]{this.instanceName, th.getMessage() != null ? th.getMessage() : th.getClass().getSimpleName()}), 1).show();
    }

    private void setDeleteInProgress(boolean z) {
        this.binding.buttonPlay.setEnabled(!z);
        this.binding.buttonInstanceSettings.setEnabled(!z);
        boolean z2 = false;
        this.binding.buttonBrowseContent.setEnabled(!z && canBrowseSelectedCategory());
        this.binding.buttonAddMods.setEnabled(!z && canUploadSelectedCategory());
        this.binding.buttonCheckContentUpdates.setEnabled(!z && canCheckUpdatesForSelectedCategory());
        MaterialButton materialButton = this.binding.buttonUpdateAllContent;
        if (!z && hasAvailableUpdatesForSelectedCategory()) {
            z2 = true;
        }
        materialButton.setEnabled(z2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void refreshContentList() {
        String str;
        String str2;
        if (this.contentAdapter == null || this.binding == null || this.gameDirectory == null) {
            return;
        }
        final ResourceCategory resourceCategory = this.selectedCategory;
        final File directoryForCategory = getDirectoryForCategory(resourceCategory);
        final ModManagerContentType modManagerContentType = toModManagerContentType(resourceCategory);
        final int i = this.contentRefreshGeneration + 1;
        this.contentRefreshGeneration = i;
        int size = this.contentItems.size();
        this.contentRefreshRunning = true;
        String string = getString(resourceCategory.pluralLabelRes);
        if (size == 0) {
            str = "Loading " + getString(resourceCategory.pluralLabelRes).toLowerCase(Locale.US) + "...";
        } else {
            str = "Refreshing " + getString(resourceCategory.pluralLabelRes).toLowerCase(Locale.US) + "...";
        }
        setContentLoadingOverlayVisible(true, string, str);
        this.binding.buttonBrowseContent.setVisibility(0);
        this.binding.buttonBrowseContent.setText(R.string.button_browse_content);
        this.binding.buttonBrowseContent.setEnabled(canBrowseSelectedCategory() && !this.contentOperationRunning);
        bindImportButtonForCategory(resourceCategory);
        this.binding.buttonAddMods.setEnabled(canUploadSelectedCategory() && !this.contentOperationRunning);
        TextView textView = this.binding.textModsHint;
        if (size == 0) {
            str2 = "Loading " + getString(resourceCategory.pluralLabelRes).toLowerCase(Locale.US) + "...";
        } else {
            str2 = "Refreshing " + getString(resourceCategory.pluralLabelRes).toLowerCase(Locale.US) + "...";
        }
        textView.setText(str2);
        updateContentUpdateButtons();
        this.contentRefreshExecutor.execute(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda79
            @Override // java.lang.Runnable
            public final void run() {
                InstanceDetailsActivity.this.lambda$refreshContentList$64(modManagerContentType, directoryForCategory, resourceCategory, i);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$refreshContentList$64(ModManagerContentType modManagerContentType, final File file, final ResourceCategory resourceCategory, final int i) {
        File[] fileArrListFiles;
        final ArrayList arrayList = new ArrayList();
        if (modManagerContentType != null) {
            try {
                ModManagerManifest.pruneMissingFiles(this.gameDirectory, modManagerContentType);
            } catch (Throwable th) {
                Logging.i(TAG, "Unable to prune missing content metadata: " + readableError(th));
            }
        }
        try {
            if ((file.exists() || file.mkdirs()) && (fileArrListFiles = file.listFiles(new FileFilter() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda26
                @Override // java.io.FileFilter
                public final boolean accept(File file2) {
                    return InstanceDetailsActivity.this.lambda$refreshContentList$58(resourceCategory, file2);
                }
            })) != null) {
                ArrayList<File> arrayList2 = new ArrayList();
                Collections.addAll(arrayList2, fileArrListFiles);
                arrayList2.sort(Comparator.comparing(new Function() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda27
                    @Override // java.util.function.Function
                    public final Object apply(Object obj) {
                        File file2 = (File) obj;
                        return Boolean.valueOf(!file2.isDirectory());
                    }
                }).thenComparing(new Function() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda29
                    @Override // java.util.function.Function
                    public final Object apply(Object obj) {
                        return InstanceDetailsActivity.this.lambda$refreshContentList$60(resourceCategory, (File) obj);
                    }
                }));
                for (File file2 : arrayList2) {
                    arrayList.add(new InstanceContentItem(file2, resourceCategory, resolveDisplayTitle(file2, resourceCategory)));
                }
            }
        } catch (Throwable th2) {
            Logging.e(TAG, "Unable to scan instance content folder: " + file.getAbsolutePath(), th2);
        }
        this.mainHandler.post(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda30
            @Override // java.lang.Runnable
            public final void run() {
                InstanceDetailsActivity.this.lambda$refreshContentList$63(i, resourceCategory, arrayList, file);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ String lambda$refreshContentList$60(ResourceCategory resourceCategory, File file) {
        return resolveDisplayTitle(file, resourceCategory).toLowerCase(Locale.US);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$refreshContentList$63(final int i, final ResourceCategory resourceCategory, ArrayList arrayList, File file) {
        if (this.binding == null || isFinishing() || isDestroyed() || i != this.contentRefreshGeneration || this.selectedCategory != resourceCategory) {
            return;
        }
        this.allContentItems.clear();
        this.allContentItems.addAll(arrayList);
        boolean z = false;
        applyContentSearchFilter(false);
        this.binding.buttonBrowseContent.setVisibility(0);
        this.binding.buttonBrowseContent.setText(R.string.button_browse_content);
        this.binding.buttonBrowseContent.setEnabled(canBrowseSelectedCategory() && !this.contentOperationRunning);
        this.binding.buttonAddMods.setText(resourceCategory.uploadButtonTextRes);
        MaterialButton materialButton = this.binding.buttonAddMods;
        if (canUploadSelectedCategory() && !this.contentOperationRunning) {
            z = true;
        }
        materialButton.setEnabled(z);
        updateContentHint(file);
        updateContentUpdateButtons();
        this.binding.recyclerResourceItems.post(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda68
            @Override // java.lang.Runnable
            public final void run() {
                InstanceDetailsActivity.this.lambda$refreshContentList$62(i, resourceCategory);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$refreshContentList$62(int i, ResourceCategory resourceCategory) {
        if (this.binding == null || isFinishing() || isDestroyed() || i != this.contentRefreshGeneration || this.selectedCategory != resourceCategory) {
            return;
        }
        InstanceContentAdapter instanceContentAdapter = this.contentAdapter;
        if (instanceContentAdapter != null) {
            instanceContentAdapter.notifyDataSetChanged();
        }
        this.binding.recyclerResourceItems.post(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                InstanceDetailsActivity.this.lambda$refreshContentList$61();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$refreshContentList$61() {
        this.contentRefreshRunning = false;
        hideContentLoadingOverlayIfIdle();
    }

    private void showContentLoadingSoon(final String str, final String str2) {
        cancelPendingContentLoadingOverlay();
        Runnable runnable = new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda18
            @Override // java.lang.Runnable
            public final void run() {
                InstanceDetailsActivity.this.lambda$showContentLoadingSoon$65(str, str2);
            }
        };
        this.pendingContentLoadingRunnable = runnable;
        this.mainHandler.postDelayed(runnable, 60L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showContentLoadingSoon$65(String str, String str2) {
        this.pendingContentLoadingRunnable = null;
        if (this.binding == null || isFinishing() || isDestroyed()) {
            return;
        }
        if (this.contentRefreshRunning || this.contentOperationRunning) {
            setContentLoadingOverlayVisible(true, str, str2);
        }
    }

    private void showContentOperationOverlay(String str, String str2) {
        cancelPendingContentLoadingOverlay();
        setContentLoadingOverlayVisible(true, str, str2);
    }

    private void hideContentLoadingOverlayIfIdle() {
        if (this.contentRefreshRunning || this.contentOperationRunning) {
            return;
        }
        cancelPendingContentLoadingOverlay();
        setContentLoadingOverlayVisible(false, "", "");
    }

    private void cancelPendingContentLoadingOverlay() {
        Runnable runnable = this.pendingContentLoadingRunnable;
        if (runnable != null) {
            this.mainHandler.removeCallbacks(runnable);
            this.pendingContentLoadingRunnable = null;
        }
    }

    private void setContentLoadingOverlayVisible(boolean z, String str, String str2) {
        if (z) {
            ensureContentLoadingOverlay();
            TextView textView = this.contentLoadingTitle;
            if (textView != null) {
                textView.setText(str);
            }
            TextView textView2 = this.contentLoadingMessage;
            if (textView2 != null) {
                textView2.setText(str2);
            }
        }
        View view = this.contentLoadingOverlay;
        if (view != null) {
            view.setVisibility(z ? 0 : 8);
        }
    }

    private void ensureContentLoadingOverlay() {
        if (this.contentLoadingOverlay != null) {
            return;
        }
        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.setVisibility(8);
        frameLayout.setClickable(true);
        frameLayout.setFocusable(true);
        frameLayout.setPadding(dp(24), dp(24), dp(24), dp(24));
        frameLayout.setBackgroundColor(-1728053248);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setGravity(1);
        linearLayout.setPadding(dp(26), dp(22), dp(26), dp(22));
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(resolveThemeColor(android.R.attr.colorBackground, -14670805));
        gradientDrawable.setCornerRadius(dp(24));
        linearLayout.setBackground(gradientDrawable);
        ProgressBar progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);
        linearLayout.addView(progressBar, new LinearLayout.LayoutParams(dp(44), dp(44)));
        TextView textView = new TextView(this);
        this.contentLoadingTitle = textView;
        textView.setTextSize(2, 18.0f);
        TextView textView2 = this.contentLoadingTitle;
        textView2.setTypeface(textView2.getTypeface(), 1);
        this.contentLoadingTitle.setTextColor(resolveThemeColor(android.R.attr.textColorPrimary, -1));
        this.contentLoadingTitle.setGravity(17);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = dp(12);
        linearLayout.addView(this.contentLoadingTitle, layoutParams);
        TextView textView3 = new TextView(this);
        this.contentLoadingMessage = textView3;
        textView3.setTextSize(2, 13.0f);
        this.contentLoadingMessage.setTextColor(resolveThemeColor(android.R.attr.textColorSecondary, -3355444));
        this.contentLoadingMessage.setGravity(17);
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-1, -2);
        layoutParams2.topMargin = dp(6);
        linearLayout.addView(this.contentLoadingMessage, layoutParams2);
        frameLayout.addView(linearLayout, new FrameLayout.LayoutParams(Math.min(dp(360), Math.max(dp(260), getResources().getDisplayMetrics().widthPixels - dp(72))), -2, 17));
        getWindow().addContentView(frameLayout, new ViewGroup.LayoutParams(-1, -1));
        this.contentLoadingOverlay = frameLayout;
    }

    private void setContentOperationInProgress(boolean z, String str, String str2) {
        this.contentOperationRunning = z;
        if (z) {
            showContentOperationOverlay(str, str2);
        } else {
            hideContentLoadingOverlayIfIdle();
        }
        ActivityInstanceDetailsBinding activityInstanceDetailsBinding = this.binding;
        if (activityInstanceDetailsBinding == null) {
            return;
        }
        activityInstanceDetailsBinding.buttonBrowseContent.setEnabled(!z && canBrowseSelectedCategory());
        this.binding.buttonAddMods.setEnabled(!z && canUploadSelectedCategory());
        this.binding.buttonCheckContentUpdates.setEnabled(!z && canCheckUpdatesForSelectedCategory());
        this.binding.buttonUpdateAllContent.setEnabled(!z && hasAvailableUpdatesForSelectedCategory());
        this.binding.recyclerResourceItems.setAlpha(z ? 0.55f : 1.0f);
    }

    private void updateContentHint(File file) {
        String string;
        int size = (this.allContentItems.isEmpty() ? this.contentItems : this.allContentItems).size();
        int size2 = this.contentItems.size();
        String string2 = getString(this.selectedCategory.pluralLabelRes);
        if (!isBlank(this.contentSearchQuery)) {
            string = size2 + " of " + size + " " + string2.toLowerCase(Locale.US) + " found";
        } else if (size2 == 0) {
            string = getString(R.string.instance_content_empty_value, new Object[]{string2});
        } else {
            string = getString(R.string.instance_content_count_value, new Object[]{Integer.valueOf(size2), string2});
        }
        if (this.selectedCategory == ResourceCategory.MODS && !supportsMods()) {
            this.binding.textModsHint.setText(getString(R.string.mods_vanilla_hint));
        } else {
            this.binding.textModsHint.setText(string);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void requestContentSearchFilter(boolean z) {
        if (!z) {
            applyContentSearchFilter(false);
        } else {
            if (this.contentSearchFilterApplyQueued) {
                return;
            }
            this.contentSearchFilterApplyQueued = true;
            this.mainHandler.post(new InstanceDetailsActivity$$ExternalSyntheticLambda75(this));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void runQueuedContentSearchFilter() {
        this.contentSearchFilterApplyQueued = false;
        if (this.binding == null || isFinishing() || isDestroyed()) {
            return;
        }
        RecyclerView recyclerView = this.binding.recyclerResourceItems;
        if (recyclerView != null && (recyclerView.isComputingLayout() || recyclerView.getScrollState() != 0)) {
            this.contentSearchFilterApplyQueued = true;
            recyclerView.postDelayed(new InstanceDetailsActivity$$ExternalSyntheticLambda75(this), 64L);
        } else {
            applyContentSearchFilter(true);
        }
    }

    private void applyContentSearchFilter(boolean z) {
        InstanceContentAdapter instanceContentAdapter;
        RecyclerView recyclerView;
        ActivityInstanceDetailsBinding activityInstanceDetailsBinding = this.binding;
        if (activityInstanceDetailsBinding == null) {
            return;
        }
        if (z && (recyclerView = activityInstanceDetailsBinding.recyclerResourceItems) != null && (recyclerView.isComputingLayout() || recyclerView.getScrollState() != 0)) {
            requestContentSearchFilter(true);
            return;
        }
        String str = this.contentSearchQuery;
        String strTrim = str == null ? "" : str.trim();
        this.contentItems.clear();
        if (strTrim.isEmpty()) {
            this.contentItems.addAll(this.allContentItems);
        } else {
            for (InstanceContentItem instanceContentItem : this.allContentItems) {
                if (matchesContentSearch(instanceContentItem, strTrim)) {
                    this.contentItems.add(instanceContentItem);
                }
            }
        }
        updateContentHint(getDirectoryForCategory(this.selectedCategory));
        updateContentUpdateButtons();
        if (!z || (instanceContentAdapter = this.contentAdapter) == null) {
            return;
        }
        instanceContentAdapter.notifyDataSetChanged();
    }

    private boolean matchesContentSearch(InstanceContentItem instanceContentItem, String str) {
        String strNormalizeSearchText = normalizeSearchText(str);
        if (strNormalizeSearchText.isEmpty()) {
            return true;
        }
        String str2 = this.contentSearchMetadata.get(safeCanonicalPath(instanceContentItem.file));
        StringBuilder sbAppend = new StringBuilder().append(instanceContentItem.title).append(" ").append(instanceContentItem.file.getName()).append(" ").append(stripExtension(instanceContentItem.file.getName())).append(" ").append(instanceContentItem.category.name()).append(" ");
        if (str2 == null) {
            str2 = "";
        }
        String strNormalizeSearchText2 = normalizeSearchText(sbAppend.append(str2).toString());
        for (String str3 : strNormalizeSearchText.split(" ")) {
            if (!str3.trim().isEmpty() && !strNormalizeSearchText2.contains(str3.trim())) {
                return false;
            }
        }
        return true;
    }

    private String normalizeSearchText(String str) {
        if (str == null) {
            return "";
        }
        String strTrim = str.toLowerCase(Locale.US).replace('_', ' ').replace('-', ' ').replace('.', ' ').replace('+', ' ').trim();
        while (strTrim.contains("  ")) {
            strTrim = strTrim.replace("  ", " ");
        }
        return strTrim;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void rememberContentSearchMetadata(InstanceContentItem instanceContentItem, String str) {
        if (isBlank(str)) {
            return;
        }
        this.contentSearchMetadata.put(safeCanonicalPath(instanceContentItem.file), str.trim());
        if (isBlank(this.contentSearchQuery) || isContentItemCurrentlyVisible(instanceContentItem)) {
            return;
        }
        requestMetadataSearchFilter();
    }

    private boolean isContentItemCurrentlyVisible(InstanceContentItem instanceContentItem) {
        return findContentItemIndexByCanonicalPath(this.contentItems, safeCanonicalPath(instanceContentItem.file)) >= 0;
    }

    private void requestMetadataSearchFilter() {
        cancelPendingMetadataSearchFilter();
        Runnable runnable = new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda17
            @Override // java.lang.Runnable
            public final void run() {
                InstanceDetailsActivity.this.lambda$requestMetadataSearchFilter$66();
            }
        };
        this.pendingMetadataSearchFilterRunnable = runnable;
        this.mainHandler.postDelayed(runnable, 250L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$requestMetadataSearchFilter$66() {
        this.pendingMetadataSearchFilterRunnable = null;
        requestContentSearchFilter(true);
    }

    private void cancelPendingMetadataSearchFilter() {
        Runnable runnable = this.pendingMetadataSearchFilterRunnable;
        if (runnable == null) {
            return;
        }
        this.mainHandler.removeCallbacks(runnable);
        this.pendingMetadataSearchFilterRunnable = null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: renamed from: shouldShowFileForCategory, reason: merged with bridge method [inline-methods] */
    public boolean lambda$refreshContentList$58(ResourceCategory resourceCategory, File file) {
        if (file.isHidden()) {
            return false;
        }
        String lowerCase = file.getName().toLowerCase(Locale.US);
        int iOrdinal = resourceCategory.ordinal();
        if (iOrdinal == 0) {
            if (file.isFile()) {
                return lowerCase.endsWith(".jar") || lowerCase.endsWith(".jar.disabled");
            }
            return false;
        }
        if (iOrdinal == 1) {
            if (!file.isDirectory()) {
                if (!file.isFile()) {
                    return false;
                }
                if (!lowerCase.endsWith(".zip") && !lowerCase.endsWith(".zip.disabled")) {
                    return false;
                }
            }
            return true;
        }
        if (iOrdinal != 2) {
            return iOrdinal == 3 && file.isDirectory() && new File(file, "level.dat").isFile();
        }
        if (!file.isDirectory()) {
            if (!file.isFile()) {
                return false;
            }
            if (!lowerCase.endsWith(".zip") && !lowerCase.endsWith(".zip.disabled")) {
                return false;
            }
        }
        return true;
    }

    private File getDirectoryForCategory(ResourceCategory resourceCategory) {
        int iOrdinal = resourceCategory.ordinal();
        if (iOrdinal == 0) {
            return this.modsDirectory;
        }
        if (iOrdinal == 1) {
            return this.shaderpacksDirectory;
        }
        if (iOrdinal == 2) {
            return this.resourcepacksDirectory;
        }
        if (iOrdinal == 3) {
            return this.worldsDirectory;
        }
        return this.gameDirectory;
    }

    private boolean canUploadSelectedCategory() {
        return this.selectedCategory != ResourceCategory.MODS || supportsMods();
    }

    private boolean canBrowseSelectedCategory() {
        return this.selectedCategory != ResourceCategory.MODS || supportsMods();
    }

    private boolean supportsMods() {
        return !"vanilla".equalsIgnoreCase(this.loader);
    }

    private void browseSelectedContent() {
        if (!canBrowseSelectedCategory()) {
            if (this.selectedCategory != ResourceCategory.MODS || supportsMods()) {
                return;
            }
            Toast.makeText(this, R.string.mods_vanilla_hint, 1).show();
            return;
        }
        Intent intent = new Intent(this, (Class<?>) ContentBrowserActivity.class);
        intent.putExtra(EXTRA_INSTANCE_ID, this.instanceId);
        intent.putExtra(EXTRA_INSTANCE_NAME, this.instanceName);
        intent.putExtra(EXTRA_INSTANCE_LOADER, this.loader);
        intent.putExtra(EXTRA_BASE_VERSION_ID, this.baseVersionId);
        intent.putExtra(EXTRA_MINECRAFT_VERSION_ID, this.minecraftVersionId);
        intent.putExtra(EXTRA_VERSION_TYPE, this.versionType);
        File file = this.rootDirectory;
        intent.putExtra(EXTRA_ROOT_DIRECTORY, file != null ? file.getAbsolutePath() : "");
        File file2 = this.gameDirectory;
        intent.putExtra(EXTRA_GAME_DIRECTORY, file2 != null ? file2.getAbsolutePath() : "");
        File file3 = this.iconFile;
        intent.putExtra(EXTRA_ICON_FILE, file3 != null ? file3.getAbsolutePath() : "");
        intent.putExtra(EXTRA_CONTENT_CATEGORY, this.selectedCategory.name().toLowerCase(Locale.US));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException unused) {
            Toast.makeText(this, getString(R.string.instance_content_browser_not_ready, new Object[]{getString(this.selectedCategory.tabTitleRes)}), 1).show();
        }
    }

    private void showDeleteContentItemDialog(InstanceContentItem instanceContentItem) {
        final InstanceContentItem instanceContentItemResolveContentItemForAction = resolveContentItemForAction(instanceContentItem);
        new AlertDialog.Builder(this).setTitle(getString(R.string.instance_content_delete_title, new Object[]{instanceContentItemResolveContentItemForAction.title})).setMessage(getString(R.string.instance_content_delete_message, new Object[]{instanceContentItemResolveContentItemForAction.file.getAbsolutePath()})).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.button_delete_forever, new DialogInterface.OnClickListener() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda97
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                InstanceDetailsActivity.this.lambda$showDeleteContentItemDialog$67(instanceContentItemResolveContentItemForAction, dialogInterface, i);
            }
        }).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showDeleteContentItemDialog$67(InstanceContentItem instanceContentItem, DialogInterface dialogInterface, int i) {
        deleteContentItem(instanceContentItem);
    }

    private void deleteContentItem(InstanceContentItem instanceContentItem) {
        final InstanceContentItem instanceContentItemResolveContentItemForAction = resolveContentItemForAction(instanceContentItem);
        new Thread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda80
            @Override // java.lang.Runnable
            public final void run() {
                InstanceDetailsActivity.this.lambda$deleteContentItem$70(instanceContentItemResolveContentItemForAction);
            }
        }, "Delete Instance Content").start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$deleteContentItem$70(final InstanceContentItem instanceContentItem) {
        try {
            deleteFileOrDirectory(instanceContentItem.file);
            ModManagerContentType modManagerContentType = toModManagerContentType(instanceContentItem.category);
            File file = this.gameDirectory;
            if (file != null && modManagerContentType != null) {
                ModManagerManifest.removeEntryForFile(file, modManagerContentType, instanceContentItem.file);
            }
            runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda36
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$deleteContentItem$68(instanceContentItem);
                }
            });
        } catch (Throwable th) {
            Logging.e(TAG, "Unable to delete content item " + instanceContentItem.file.getAbsolutePath(), th);
            runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda37
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$deleteContentItem$69(th, instanceContentItem);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$deleteContentItem$68(InstanceContentItem instanceContentItem) {
        refreshContentList();
        Toast.makeText(this, getString(R.string.instance_content_delete_success, new Object[]{instanceContentItem.title}), 0).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$deleteContentItem$69(Throwable th, InstanceContentItem instanceContentItem) {
        Toast.makeText(this, getString(R.string.instance_content_delete_failed, new Object[]{instanceContentItem.title, th.getMessage() != null ? th.getMessage() : th.getClass().getSimpleName()}), 1).show();
    }

    private void deleteFileOrDirectory(File file) throws IOException {
        File[] fileArrListFiles;
        if (file.exists()) {
            if (file.isDirectory() && (fileArrListFiles = file.listFiles()) != null) {
                for (File file2 : fileArrListFiles) {
                    deleteFileOrDirectory(file2);
                }
            }
            if (!file.delete() && file.exists()) {
                throw new IOException("Unable to delete: " + file.getAbsolutePath());
            }
        }
    }

    private void setContentItemEnabled(InstanceContentItem instanceContentItem, final boolean z) {
        final File file;
        InstanceContentItem instanceContentItemResolveContentItemForAction = resolveContentItemForAction(instanceContentItem);
        if (!instanceContentItemResolveContentItemForAction.category.supportsDisableToggle || instanceContentItemResolveContentItemForAction.file.isDirectory() || this.contentOperationRunning || isContentItemEnabled(instanceContentItemResolveContentItemForAction.file) == z) {
            return;
        }
        File parentFile = instanceContentItemResolveContentItemForAction.file.getParentFile();
        if (parentFile == null) {
            Toast.makeText(this, getString(R.string.instance_content_toggle_failed, new Object[]{instanceContentItemResolveContentItemForAction.file.getName()}), 1).show();
            notifyContentItemChangedByPath(instanceContentItemResolveContentItemForAction.file);
            return;
        }
        final File file2 = instanceContentItemResolveContentItemForAction.file;
        if (z) {
            file = new File(parentFile, removeDisabledSuffix(file2.getName()));
        } else {
            file = new File(parentFile, file2.getName() + ".disabled");
        }
        final ModManagerContentType modManagerContentType = toModManagerContentType(instanceContentItemResolveContentItemForAction.category);
        final ResourceCategory resourceCategory = instanceContentItemResolveContentItemForAction.category;
        final String strSafeCanonicalPath = safeCanonicalPath(file2);
        final String strStripDisabledSuffix = stripDisabledSuffix(file.getName());
        setContentOperationInProgress(true, z ? "Enabling content" : "Disabling content", strStripDisabledSuffix);
        this.contentOperationExecutor.execute(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda73
            @Override // java.lang.Runnable
            public final void run() {
                InstanceDetailsActivity.this.lambda$setContentItemEnabled$72(file2, file, modManagerContentType, strSafeCanonicalPath, resourceCategory, z, strStripDisabledSuffix);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setContentItemEnabled$72(final File file, final File file2, ModManagerContentType modManagerContentType, final String str, final ResourceCategory resourceCategory, final boolean z, final String str2) {
        boolean z2 = false;
        try {
            try {
            } catch (Throwable th) {
                th = th;
                Logging.e(TAG, "Unable to toggle content item " + file.getAbsolutePath(), th);
            }
        } catch (Throwable th2) {
            th = th2;
        }
        if (!file.exists()) {
            throw new IOException("File no longer exists: " + file.getName());
        }
        if (file2.exists()) {
            throw new IOException("Target already exists: " + file2.getName());
        }
        if (!file.renameTo(file2)) {
            throw new IOException("Unable to rename " + file.getName());
        }
        z2 = true;
        File file3 = this.gameDirectory;
        if (file3 != null && modManagerContentType != null) {
            ModManagerManifest.updateEntryFileTarget(file3, modManagerContentType, file, file2);
        }
        th = null;
        final boolean z3 = z2;
        final Throwable th3 = th;
        this.mainHandler.post(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                InstanceDetailsActivity.this.lambda$setContentItemEnabled$71(z3, th3, str, file2, resourceCategory, z, str2, file);
            }
        });
        Logging.e(TAG, "Unable to toggle content item " + file.getAbsolutePath(), th);
        final boolean z32 = z2;
        final Throwable th32 = th;
        this.mainHandler.post(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                InstanceDetailsActivity.this.lambda$setContentItemEnabled$71(z32, th32, str, file2, resourceCategory, z, str2, file);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setContentItemEnabled$71(boolean z, Throwable th, String str, File file, ResourceCategory resourceCategory, boolean z2, String str2, File file2) {
        if (this.binding == null || isFinishing() || isDestroyed()) {
            return;
        }
        if (z && th == null) {
            replaceContentItemAfterToggle(str, file, resourceCategory);
            Toast.makeText(this, getString(z2 ? R.string.instance_content_enabled_success : R.string.instance_content_disabled_success, new Object[]{str2}), 0).show();
        } else {
            notifyContentItemChangedByPath(file2);
            Toast.makeText(this, getString(R.string.instance_content_toggle_failed, new Object[]{str2}) + ": " + (th == null ? "Unknown error" : readableError(th)), 1).show();
        }
        setContentOperationInProgress(false, "", "");
    }

    private void replaceContentItemAfterToggle(String str, File file, ResourceCategory resourceCategory) {
        InstanceContentItem instanceContentItem = new InstanceContentItem(file, resourceCategory, resolveDisplayTitle(file, resourceCategory));
        int iFindContentItemIndexByCanonicalPath = findContentItemIndexByCanonicalPath(this.allContentItems, str);
        if (iFindContentItemIndexByCanonicalPath >= 0) {
            this.allContentItems.set(iFindContentItemIndexByCanonicalPath, instanceContentItem);
        }
        int iFindContentItemIndexByCanonicalPath2 = findContentItemIndexByCanonicalPath(this.contentItems, str);
        if (iFindContentItemIndexByCanonicalPath2 < 0 && iFindContentItemIndexByCanonicalPath < 0) {
            refreshContentList();
            return;
        }
        this.contentSearchMetadata.remove(str);
        InstanceContentAdapter instanceContentAdapter = this.contentAdapter;
        if (instanceContentAdapter != null) {
            instanceContentAdapter.clearTransientCachesForFile(str);
        }
        if (!isBlank(this.contentSearchQuery)) {
            requestContentSearchFilter(true);
            return;
        }
        if (iFindContentItemIndexByCanonicalPath2 >= 0) {
            this.contentItems.set(iFindContentItemIndexByCanonicalPath2, instanceContentItem);
            InstanceContentAdapter instanceContentAdapter2 = this.contentAdapter;
            if (instanceContentAdapter2 != null) {
                instanceContentAdapter2.notifyItemChanged(iFindContentItemIndexByCanonicalPath2);
            }
        }
        updateContentHint(getDirectoryForCategory(resourceCategory));
        updateContentUpdateButtons();
    }

    private void notifyContentItemChangedByPath(File file) {
        InstanceContentAdapter instanceContentAdapter;
        int iFindContentItemIndexByCanonicalPath = findContentItemIndexByCanonicalPath(safeCanonicalPath(file));
        if (iFindContentItemIndexByCanonicalPath < 0 || (instanceContentAdapter = this.contentAdapter) == null) {
            return;
        }
        instanceContentAdapter.notifyItemChanged(iFindContentItemIndexByCanonicalPath);
    }

    private int findContentItemIndexByCanonicalPath(String str) {
        return findContentItemIndexByCanonicalPath(this.contentItems, str);
    }

    private int findContentItemIndexByCanonicalPath(ArrayList<InstanceContentItem> arrayList, String str) {
        for (int i = 0; i < arrayList.size(); i++) {
            if (str.equals(safeCanonicalPath(arrayList.get(i).file))) {
                return i;
            }
        }
        return -1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public InstanceContentItem resolveContentItemForAction(InstanceContentItem instanceContentItem) {
        String strSafeCanonicalPath = safeCanonicalPath(instanceContentItem.file);
        int iFindContentItemIndexByCanonicalPath = findContentItemIndexByCanonicalPath(this.contentItems, strSafeCanonicalPath);
        if (iFindContentItemIndexByCanonicalPath >= 0) {
            return this.contentItems.get(iFindContentItemIndexByCanonicalPath);
        }
        int iFindContentItemIndexByCanonicalPath2 = findContentItemIndexByCanonicalPath(this.allContentItems, strSafeCanonicalPath);
        return iFindContentItemIndexByCanonicalPath2 >= 0 ? this.allContentItems.get(iFindContentItemIndexByCanonicalPath2) : instanceContentItem;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void prepareContentRowAction() {
        cancelPendingMetadataSearchFilter();
        ActivityInstanceDetailsBinding activityInstanceDetailsBinding = this.binding;
        if (activityInstanceDetailsBinding != null && activityInstanceDetailsBinding.editTextContentSearch != null && this.binding.editTextContentSearch.hasFocus()) {
            finishContentSearchInput(this.binding.editTextContentSearch);
        } else {
            enableFullscreen();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showDeleteContentItemDialogFromRow(InstanceContentItem instanceContentItem) {
        prepareContentRowAction();
        showDeleteContentItemDialog(resolveContentItemForAction(instanceContentItem));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setContentItemEnabledFromRow(InstanceContentItem instanceContentItem, boolean z) {
        prepareContentRowAction();
        setContentItemEnabled(resolveContentItemForAction(instanceContentItem), z);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkSingleContentUpdateFromRow(InstanceContentItem instanceContentItem) {
        prepareContentRowAction();
        checkSingleContentUpdate(resolveContentItemForAction(instanceContentItem));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateSingleContentItemFromRow(InstanceContentItem instanceContentItem) {
        prepareContentRowAction();
        updateSingleContentItem(resolveContentItemForAction(instanceContentItem));
    }

    private boolean canCheckUpdatesForSelectedCategory() {
        return (toModManagerContentType(this.selectedCategory) == null || this.gameDirectory == null) ? false : true;
    }

    private void updateContentUpdateButtons() {
        String string;
        boolean zCanCheckUpdatesForSelectedCategory = canCheckUpdatesForSelectedCategory();
        boolean z = false;
        this.binding.buttonCheckContentUpdates.setVisibility(zCanCheckUpdatesForSelectedCategory ? 0 : 8);
        this.binding.buttonCheckContentUpdates.setEnabled(zCanCheckUpdatesForSelectedCategory && !this.contentOperationRunning);
        boolean zHasAvailableUpdatesForSelectedCategory = hasAvailableUpdatesForSelectedCategory();
        this.binding.buttonUpdateAllContent.setVisibility(zHasAvailableUpdatesForSelectedCategory ? 0 : 8);
        MaterialButton materialButton = this.binding.buttonUpdateAllContent;
        if (zHasAvailableUpdatesForSelectedCategory && !this.contentOperationRunning) {
            z = true;
        }
        materialButton.setEnabled(z);
        MaterialButton materialButton2 = this.binding.buttonUpdateAllContent;
        if (zHasAvailableUpdatesForSelectedCategory) {
            string = getString(R.string.instance_content_update_all_count, new Object[]{Integer.valueOf(countAvailableUpdatesForSelectedCategory())});
        } else {
            string = getString(R.string.instance_content_update_all);
        }
        materialButton2.setText(string);
    }

    private boolean hasAvailableUpdatesForSelectedCategory() {
        return countAvailableUpdatesForSelectedCategory() > 0;
    }

    private int countAvailableUpdatesForSelectedCategory() {
        ModManagerContentType modManagerContentType = toModManagerContentType(this.selectedCategory);
        int i = 0;
        if (modManagerContentType == null) {
            return 0;
        }
        String str = modManagerContentType.name() + ":";
        Iterator<String> it = this.updateCandidates.keySet().iterator();
        while (it.hasNext()) {
            if (it.next().startsWith(str)) {
                i++;
            }
        }
        return i;
    }

    private void checkUpdatesForSelectedCategory() {
        final ModManagerContentType modManagerContentType = toModManagerContentType(this.selectedCategory);
        if (modManagerContentType == null || this.gameDirectory == null) {
            Toast.makeText(this, R.string.instance_content_updates_not_supported, 0).show();
        } else {
            showUpdateProgressDialog(getString(R.string.instance_content_checking_updates_title), getString(R.string.instance_content_checking_updates_message), true, 1, true);
            new Thread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda74
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$checkUpdatesForSelectedCategory$79(modManagerContentType);
                }
            }, "Check Instance Content Updates").start();
        }
    }

    /* JADX INFO: renamed from: ca.dnamobile.javalauncher.InstanceDetailsActivity$8, reason: invalid class name */
    class AnonymousClass8 implements ModManagerUpdateManager.Listener {
        AnonymousClass8() {
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onStatus$0(String str) {
            InstanceDetailsActivity.this.setUpdateProgressMessage(str);
        }

        @Override // ca.dnamobile.javalauncher.modmanager.ModManagerUpdateManager.Listener
        public void onStatus(final String str) {
            InstanceDetailsActivity.this.runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$8$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$onStatus$0(str);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onProgress$1(int i, int i2) {
            InstanceDetailsActivity.this.lambda$checkUpdatesForSelectedCategory$76(i, i2);
        }

        @Override // ca.dnamobile.javalauncher.modmanager.ModManagerUpdateManager.Listener
        public void onProgress(final int i, final int i2) {
            InstanceDetailsActivity.this.runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$8$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$onProgress$1(i, i2);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$checkUpdatesForSelectedCategory$79(ModManagerContentType modManagerContentType) {
        try {
            markTrackedEntriesUpToDate(modManagerContentType);
            final ArrayList<ModManagerUpdateManager.UpdateCandidate> arrayListCheckUpdates = ModManagerUpdateManager.checkUpdates(this, this.gameDirectory, modManagerContentType, getGameVersionIdForContent(), this.loader, new AnonymousClass8());
            ArrayList<JSONObject> modpackInstalledEntriesForType = getModpackInstalledEntriesForType(modManagerContentType);
            if (!modpackInstalledEntriesForType.isEmpty()) {
                runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda59
                    @Override // java.lang.Runnable
                    public final void run() {
                        InstanceDetailsActivity.this.lambda$checkUpdatesForSelectedCategory$73();
                    }
                });
                final int iMax = Math.max(1, modpackInstalledEntriesForType.size());
                for (int i = 0; i < modpackInstalledEntriesForType.size(); i++) {
                    JSONObject jSONObject = modpackInstalledEntriesForType.get(i);
                    File fileResolveFileForInstalledEntry = resolveFileForInstalledEntry(jSONObject);
                    if (fileResolveFileForInstalledEntry != null && ModManagerManifest.getInstalledEntryForFile(this.gameDirectory, modManagerContentType, fileResolveFileForInstalledEntry) != null) {
                        final int i2 = i + 1;
                        runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda60
                            @Override // java.lang.Runnable
                            public final void run() {
                                InstanceDetailsActivity.this.lambda$checkUpdatesForSelectedCategory$74(i2, iMax);
                            }
                        });
                    } else {
                        ModManagerSource source = ModManagerManifest.getSource(jSONObject);
                        if (source != ModManagerSource.MODRINTH && source != ModManagerSource.CURSEFORGE) {
                            final int i3 = i + 1;
                            runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda62
                                @Override // java.lang.Runnable
                                public final void run() {
                                    InstanceDetailsActivity.this.lambda$checkUpdatesForSelectedCategory$75(i3, iMax);
                                }
                            });
                        } else {
                            try {
                                ModManagerUpdateManager.UpdateCandidate updateCandidateCheckUpdateForEntry = ModManagerUpdateManager.checkUpdateForEntry(this, this.gameDirectory, modManagerContentType, jSONObject, getGameVersionIdForContent(), this.loader);
                                if (updateCandidateCheckUpdateForEntry != null && !containsUpdateCandidate(arrayListCheckUpdates, updateCandidateCheckUpdateForEntry)) {
                                    arrayListCheckUpdates.add(updateCandidateCheckUpdateForEntry);
                                }
                            } catch (Throwable th) {
                                Logging.i(TAG, "Unable to check modpack-installed file update: " + readableError(th));
                            }
                            final int i4 = i + 1;
                            runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda63
                                @Override // java.lang.Runnable
                                public final void run() {
                                    InstanceDetailsActivity.this.lambda$checkUpdatesForSelectedCategory$76(i4, iMax);
                                }
                            });
                        }
                    }
                }
            }
            removeUpdateCandidatesForType(modManagerContentType);
            for (ModManagerUpdateManager.UpdateCandidate updateCandidate : arrayListCheckUpdates) {
                String strBuildUpdateKey = buildUpdateKey(updateCandidate);
                this.updateCandidates.put(strBuildUpdateKey, updateCandidate);
                this.updateStates.put(strBuildUpdateKey, UpdateState.UPDATE_AVAILABLE);
                this.updateMessages.put(strBuildUpdateKey, getString(R.string.instance_content_update_available_value, new Object[]{updateCandidate.latestVersion.versionNumber}));
            }
            runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda64
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$checkUpdatesForSelectedCategory$77(arrayListCheckUpdates);
                }
            });
        } catch (Throwable th2) {
            Logging.e(TAG, "Unable to check content updates", th2);
            runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda65
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$checkUpdatesForSelectedCategory$78(th2);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$checkUpdatesForSelectedCategory$73() {
        setUpdateProgressMessage("Checking modpack-installed files...");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$checkUpdatesForSelectedCategory$77(ArrayList arrayList) {
        dismissUpdateProgressDialog();
        updateContentUpdateButtons();
        InstanceContentAdapter instanceContentAdapter = this.contentAdapter;
        if (instanceContentAdapter != null) {
            instanceContentAdapter.notifyDataSetChanged();
        }
        Toast.makeText(this, arrayList.isEmpty() ? getString(R.string.instance_content_no_updates_found) : getString(R.string.instance_content_updates_found, new Object[]{Integer.valueOf(arrayList.size())}), 1).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$checkUpdatesForSelectedCategory$78(Throwable th) {
        dismissUpdateProgressDialog();
        Toast.makeText(this, getString(R.string.instance_content_update_check_failed, new Object[]{th.getMessage() != null ? th.getMessage() : th.getClass().getSimpleName()}), 1).show();
    }

    private void checkSingleContentUpdate(InstanceContentItem instanceContentItem) {
        InstanceContentItem instanceContentItemResolveContentItemForAction = resolveContentItemForAction(instanceContentItem);
        final ModManagerContentType modManagerContentType = toModManagerContentType(instanceContentItemResolveContentItemForAction.category);
        if (modManagerContentType == null || this.gameDirectory == null) {
            return;
        }
        final JSONObject installedEntryForItem = getInstalledEntryForItem(instanceContentItemResolveContentItemForAction);
        if (installedEntryForItem == null) {
            Toast.makeText(this, R.string.instance_content_update_missing_metadata, 1).show();
            return;
        }
        ModManagerSource source = ModManagerManifest.getSource(installedEntryForItem);
        if (source != ModManagerSource.MODRINTH && source != ModManagerSource.CURSEFORGE) {
            Toast.makeText(this, R.string.instance_content_update_manual_not_supported, 1).show();
            return;
        }
        final String strBuildUpdateKey = buildUpdateKey(modManagerContentType, installedEntryForItem);
        this.updateStates.put(strBuildUpdateKey, UpdateState.CHECKING);
        this.updateMessages.put(strBuildUpdateKey, getString(R.string.instance_content_checking_updates_short));
        InstanceContentAdapter instanceContentAdapter = this.contentAdapter;
        if (instanceContentAdapter != null) {
            instanceContentAdapter.notifyDataSetChanged();
        }
        new Thread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda21
            @Override // java.lang.Runnable
            public final void run() {
                InstanceDetailsActivity.this.lambda$checkSingleContentUpdate$82(modManagerContentType, installedEntryForItem, strBuildUpdateKey);
            }
        }, "Check Content Item Update").start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$checkSingleContentUpdate$82(ModManagerContentType modManagerContentType, JSONObject jSONObject, final String str) {
        try {
            final ModManagerUpdateManager.UpdateCandidate updateCandidateCheckUpdateForEntry = ModManagerUpdateManager.checkUpdateForEntry(this, this.gameDirectory, modManagerContentType, jSONObject, getGameVersionIdForContent(), this.loader);
            runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda28
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$checkSingleContentUpdate$80(updateCandidateCheckUpdateForEntry, str);
                }
            });
        } catch (Throwable th) {
            Logging.e(TAG, "Unable to check single content update", th);
            runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda39
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$checkSingleContentUpdate$81(str, th);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$checkSingleContentUpdate$80(ModManagerUpdateManager.UpdateCandidate updateCandidate, String str) {
        if (updateCandidate == null) {
            this.updateCandidates.remove(str);
            this.updateStates.put(str, UpdateState.UP_TO_DATE);
            this.updateMessages.put(str, getString(R.string.instance_content_up_to_date));
            Toast.makeText(this, R.string.instance_content_up_to_date, 0).show();
        } else {
            String strBuildUpdateKey = buildUpdateKey(updateCandidate);
            this.updateCandidates.put(strBuildUpdateKey, updateCandidate);
            this.updateStates.put(strBuildUpdateKey, UpdateState.UPDATE_AVAILABLE);
            this.updateMessages.put(strBuildUpdateKey, getString(R.string.instance_content_update_available_value, new Object[]{updateCandidate.latestVersion.versionNumber}));
            Toast.makeText(this, getString(R.string.instance_content_update_available_for, new Object[]{updateCandidate.getDisplayName()}), 1).show();
        }
        updateContentUpdateButtons();
        InstanceContentAdapter instanceContentAdapter = this.contentAdapter;
        if (instanceContentAdapter != null) {
            instanceContentAdapter.notifyDataSetChanged();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$checkSingleContentUpdate$81(String str, Throwable th) {
        this.updateStates.put(str, UpdateState.ERROR);
        this.updateMessages.put(str, th.getMessage() != null ? th.getMessage() : th.getClass().getSimpleName());
        Toast.makeText(this, getString(R.string.instance_content_update_check_failed, new Object[]{this.updateMessages.get(str)}), 1).show();
        InstanceContentAdapter instanceContentAdapter = this.contentAdapter;
        if (instanceContentAdapter != null) {
            instanceContentAdapter.notifyDataSetChanged();
        }
    }

    private void updateSingleContentItem(InstanceContentItem instanceContentItem) {
        InstanceContentItem instanceContentItemResolveContentItemForAction = resolveContentItemForAction(instanceContentItem);
        ModManagerContentType modManagerContentType = toModManagerContentType(instanceContentItemResolveContentItemForAction.category);
        if (modManagerContentType == null || this.gameDirectory == null) {
            return;
        }
        JSONObject installedEntryForItem = getInstalledEntryForItem(instanceContentItemResolveContentItemForAction);
        if (installedEntryForItem == null) {
            Toast.makeText(this, R.string.instance_content_update_missing_metadata, 1).show();
            return;
        }
        ModManagerUpdateManager.UpdateCandidate updateCandidate = this.updateCandidates.get(buildUpdateKey(modManagerContentType, installedEntryForItem));
        if (updateCandidate == null) {
            checkSingleContentUpdate(instanceContentItemResolveContentItemForAction);
        } else {
            updateCandidate(updateCandidate);
        }
    }

    private void updateAllAvailableForSelectedCategory() {
        ModManagerContentType modManagerContentType = toModManagerContentType(this.selectedCategory);
        if (modManagerContentType == null || this.gameDirectory == null) {
            return;
        }
        final ArrayList arrayList = new ArrayList();
        String str = modManagerContentType.name() + ":";
        for (Map.Entry<String, ModManagerUpdateManager.UpdateCandidate> entry : this.updateCandidates.entrySet()) {
            if (entry.getKey().startsWith(str)) {
                arrayList.add(entry.getValue());
            }
        }
        if (arrayList.isEmpty()) {
            Toast.makeText(this, R.string.instance_content_no_updates_found, 0).show();
        } else {
            new AlertDialog.Builder(this).setTitle(R.string.instance_content_update_all_title).setMessage(getString(R.string.instance_content_update_all_message, new Object[]{Integer.valueOf(arrayList.size())})).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.instance_content_update_all, new DialogInterface.OnClickListener() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda91
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    InstanceDetailsActivity.this.lambda$updateAllAvailableForSelectedCategory$83(arrayList, dialogInterface, i);
                }
            }).show();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$updateAllAvailableForSelectedCategory$83(ArrayList arrayList, DialogInterface dialogInterface, int i) {
        updateAllCandidates(arrayList);
    }

    private void updateCandidate(final ModManagerUpdateManager.UpdateCandidate updateCandidate) {
        final String strBuildUpdateKey = buildUpdateKey(updateCandidate);
        final UpdateCleanupPlan updateCleanupPlanCreateUpdateCleanupPlan = createUpdateCleanupPlan(updateCandidate);
        this.updateStates.put(strBuildUpdateKey, UpdateState.UPDATING);
        this.updateMessages.put(strBuildUpdateKey, getString(R.string.instance_content_updating_value, new Object[]{updateCandidate.latestVersion.versionNumber}));
        InstanceContentAdapter instanceContentAdapter = this.contentAdapter;
        if (instanceContentAdapter != null) {
            instanceContentAdapter.notifyDataSetChanged();
        }
        showUpdateProgressDialog(getString(R.string.instance_content_updating_title), getString(R.string.instance_content_updating_one, new Object[]{updateCandidate.getDisplayName()}), true, 1, false);
        new Thread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda19
            @Override // java.lang.Runnable
            public final void run() {
                InstanceDetailsActivity.this.lambda$updateCandidate$86(updateCandidate, updateCleanupPlanCreateUpdateCleanupPlan, strBuildUpdateKey);
            }
        }, "Update Instance Content Item").start();
    }

    /* JADX INFO: renamed from: ca.dnamobile.javalauncher.InstanceDetailsActivity$9, reason: invalid class name */
    class AnonymousClass9 implements ModrinthInstallManager.Listener {
        final /* synthetic */ Throwable[] val$error;

        AnonymousClass9(Throwable[] thArr) {
            this.val$error = thArr;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onStatus$0(String str) {
            InstanceDetailsActivity.this.setUpdateProgressMessage(str);
        }

        @Override // ca.dnamobile.javalauncher.modmanager.ModrinthInstallManager.Listener
        public void onStatus(final String str) {
            InstanceDetailsActivity.this.runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$9$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$onStatus$0(str);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onComplete$1(String str) {
            InstanceDetailsActivity.this.setUpdateProgressMessage(str);
        }

        @Override // ca.dnamobile.javalauncher.modmanager.ModrinthInstallManager.Listener
        public void onComplete(final String str) {
            InstanceDetailsActivity.this.runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$9$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$onComplete$1(str);
                }
            });
        }

        @Override // ca.dnamobile.javalauncher.modmanager.ModrinthInstallManager.Listener
        public void onError(Throwable th) {
            this.val$error[0] = th;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$updateCandidate$86(final ModManagerUpdateManager.UpdateCandidate updateCandidate, UpdateCleanupPlan updateCleanupPlan, final String str) {
        try {
            Throwable[] thArr = new Throwable[1];
            ModManagerUpdateManager.updateCandidate(this, this.gameDirectory, getGameVersionIdForContent(), this.loader, updateCandidate, new AnonymousClass9(thArr));
            Throwable th = thArr[0];
            if (th != null) {
                throw th;
            }
            final int iFinalizeContentUpdateCleanup = finalizeContentUpdateCleanup(Collections.singletonList(updateCleanupPlan));
            runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda89
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$updateCandidate$84(str, updateCandidate, iFinalizeContentUpdateCleanup);
                }
            });
        } catch (Throwable th2) {
            Logging.e(TAG, "Unable to update content item", th2);
            runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda90
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$updateCandidate$85(str, th2, updateCandidate);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$updateCandidate$84(String str, ModManagerUpdateManager.UpdateCandidate updateCandidate, int i) {
        dismissUpdateProgressDialog();
        this.updateCandidates.remove(str);
        this.updateStates.remove(str);
        this.updateMessages.remove(str);
        refreshContentList();
        setResult(-1);
        Toast.makeText(this, getString(R.string.instance_content_update_success, new Object[]{updateCandidate.getDisplayName()}) + formatUpdateCleanupSuffix(i), 1).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$updateCandidate$85(String str, Throwable th, ModManagerUpdateManager.UpdateCandidate updateCandidate) {
        dismissUpdateProgressDialog();
        this.updateStates.put(str, UpdateState.ERROR);
        this.updateMessages.put(str, th.getMessage() != null ? th.getMessage() : th.getClass().getSimpleName());
        InstanceContentAdapter instanceContentAdapter = this.contentAdapter;
        if (instanceContentAdapter != null) {
            instanceContentAdapter.notifyDataSetChanged();
        }
        Toast.makeText(this, getString(R.string.instance_content_update_failed, new Object[]{updateCandidate.getDisplayName(), this.updateMessages.get(str)}), 1).show();
    }

    private void updateAllCandidates(final ArrayList<ModManagerUpdateManager.UpdateCandidate> arrayList) {
        if (arrayList.isEmpty() || this.gameDirectory == null) {
            return;
        }
        final ArrayList arrayList2 = new ArrayList();
        Iterator<ModManagerUpdateManager.UpdateCandidate> it = arrayList.iterator();
        while (it.hasNext()) {
            arrayList2.add(createUpdateCleanupPlan(it.next()));
        }
        showUpdateProgressDialog(getString(R.string.instance_content_update_all_title), getString(R.string.instance_content_update_all_wait), false, arrayList.size(), false);
        for (ModManagerUpdateManager.UpdateCandidate updateCandidate : arrayList) {
            String strBuildUpdateKey = buildUpdateKey(updateCandidate);
            this.updateStates.put(strBuildUpdateKey, UpdateState.UPDATING);
            this.updateMessages.put(strBuildUpdateKey, getString(R.string.instance_content_updating_value, new Object[]{updateCandidate.latestVersion.versionNumber}));
        }
        InstanceContentAdapter instanceContentAdapter = this.contentAdapter;
        if (instanceContentAdapter != null) {
            instanceContentAdapter.notifyDataSetChanged();
        }
        new Thread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda14
            @Override // java.lang.Runnable
            public final void run() {
                InstanceDetailsActivity.this.lambda$updateAllCandidates$89(arrayList, arrayList2);
            }
        }, "Update All Instance Content").start();
    }

    /* JADX INFO: renamed from: ca.dnamobile.javalauncher.InstanceDetailsActivity$10, reason: invalid class name */
    class AnonymousClass10 implements ModManagerUpdateManager.Listener {
        AnonymousClass10() {
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onStatus$0(String str) {
            InstanceDetailsActivity.this.setUpdateProgressMessage(str);
        }

        @Override // ca.dnamobile.javalauncher.modmanager.ModManagerUpdateManager.Listener
        public void onStatus(final String str) {
            InstanceDetailsActivity.this.runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$10$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$onStatus$0(str);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onProgress$1(int i, int i2) {
            InstanceDetailsActivity.this.lambda$checkUpdatesForSelectedCategory$76(i, i2);
        }

        @Override // ca.dnamobile.javalauncher.modmanager.ModManagerUpdateManager.Listener
        public void onProgress(final int i, final int i2) {
            InstanceDetailsActivity.this.runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$10$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$onProgress$1(i, i2);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$updateAllCandidates$89(final ArrayList arrayList, ArrayList arrayList2) {
        try {
            ModManagerUpdateManager.updateAll(this, this.gameDirectory, getGameVersionIdForContent(), this.loader, arrayList, new AnonymousClass10());
            final int iFinalizeContentUpdateCleanup = finalizeContentUpdateCleanup(arrayList2);
            runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda93
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$updateAllCandidates$87(arrayList, iFinalizeContentUpdateCleanup);
                }
            });
        } catch (Throwable th) {
            Logging.e(TAG, "Unable to update all content", th);
            runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda95
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$updateAllCandidates$88(th);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$updateAllCandidates$87(ArrayList arrayList, int i) {
        dismissUpdateProgressDialog();
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            String strBuildUpdateKey = buildUpdateKey((ModManagerUpdateManager.UpdateCandidate) it.next());
            this.updateCandidates.remove(strBuildUpdateKey);
            this.updateStates.remove(strBuildUpdateKey);
            this.updateMessages.remove(strBuildUpdateKey);
        }
        refreshContentList();
        setResult(-1);
        Toast.makeText(this, getString(R.string.instance_content_update_all_success, new Object[]{Integer.valueOf(arrayList.size())}) + formatUpdateCleanupSuffix(i), 1).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$updateAllCandidates$88(Throwable th) {
        dismissUpdateProgressDialog();
        refreshContentList();
        Toast.makeText(this, getString(R.string.instance_content_update_all_failed, new Object[]{th.getMessage() != null ? th.getMessage() : th.getClass().getSimpleName()}), 1).show();
    }

    private UpdateCleanupPlan createUpdateCleanupPlan(ModManagerUpdateManager.UpdateCandidate updateCandidate) {
        UpdateCleanupPlan updateCleanupPlan = new UpdateCleanupPlan(updateCandidate, updateCandidate.contentType, updateCandidate.source, sanitizeProjectId(updateCandidate.getProjectId()), buildUpdateKey(updateCandidate), System.currentTimeMillis());
        collectOldFilesForUpdate(updateCleanupPlan, getInstalledManifestEntriesForProject(updateCleanupPlan.contentType, updateCleanupPlan.source, updateCleanupPlan.projectId));
        ArrayList<JSONObject> modpackInstalledEntriesForProject = getModpackInstalledEntriesForProject(updateCleanupPlan.contentType, updateCleanupPlan.source, updateCleanupPlan.projectId);
        updateCleanupPlan.hadModpackMetadata = !modpackInstalledEntriesForProject.isEmpty();
        collectOldFilesForUpdate(updateCleanupPlan, modpackInstalledEntriesForProject);
        return updateCleanupPlan;
    }

    private void collectOldFilesForUpdate(UpdateCleanupPlan updateCleanupPlan, ArrayList<JSONObject> arrayList) {
        Iterator<JSONObject> it = arrayList.iterator();
        while (it.hasNext()) {
            File fileResolveFileForInstalledEntry = resolveFileForInstalledEntry(it.next(), updateCleanupPlan.contentType);
            if (fileResolveFileForInstalledEntry != null) {
                if (updateCleanupPlan.oldCanonicalPaths.add(safeCanonicalPath(fileResolveFileForInstalledEntry))) {
                    updateCleanupPlan.oldFiles.add(fileResolveFileForInstalledEntry);
                }
            }
        }
    }

    private int finalizeContentUpdateCleanup(List<UpdateCleanupPlan> list) {
        int iRemoveOldFilesForUpdate = 0;
        if (this.gameDirectory != null && !list.isEmpty()) {
            for (UpdateCleanupPlan updateCleanupPlan : list) {
                try {
                    File fileFindReplacementFileForUpdate = findReplacementFileForUpdate(updateCleanupPlan);
                    JSONObject jSONObjectFindReplacementEntryForUpdate = findReplacementEntryForUpdate(updateCleanupPlan, fileFindReplacementFileForUpdate);
                    iRemoveOldFilesForUpdate = iRemoveOldFilesForUpdate + removeOldFilesForUpdate(updateCleanupPlan, fileFindReplacementFileForUpdate) + removeDuplicateTrackedFilesForProject(updateCleanupPlan, fileFindReplacementFileForUpdate);
                    syncModpackFilesManifestAfterUpdate(updateCleanupPlan, fileFindReplacementFileForUpdate, jSONObjectFindReplacementEntryForUpdate);
                    ModManagerManifest.pruneMissingFiles(this.gameDirectory, updateCleanupPlan.contentType);
                } catch (Throwable th) {
                    Logging.i(TAG, "Unable to finish update cleanup for " + updateCleanupPlan.key + ": " + readableError(th));
                }
            }
        }
        return iRemoveOldFilesForUpdate;
    }

    private int removeOldFilesForUpdate(UpdateCleanupPlan updateCleanupPlan, File file) {
        int i = 0;
        if (this.gameDirectory != null && file != null && file.isFile()) {
            String strSafeCanonicalPath = safeCanonicalPath(file);
            for (File file2 : updateCleanupPlan.oldFiles) {
                if (!safeCanonicalPath(file2).equals(strSafeCanonicalPath)) {
                    try {
                        ModManagerManifest.removeEntryForFile(this.gameDirectory, updateCleanupPlan.contentType, file2);
                    } catch (Throwable unused) {
                    }
                    if (file2.exists()) {
                        try {
                            deleteFileOrDirectory(file2);
                            i++;
                        } catch (Throwable th) {
                            Logging.i(TAG, "Unable to delete old updated file " + file2.getAbsolutePath() + ": " + readableError(th));
                        }
                    }
                }
            }
        }
        return i;
    }

    private int removeDuplicateTrackedFilesForProject(UpdateCleanupPlan updateCleanupPlan, File file) {
        int i = 0;
        if (this.gameDirectory == null) {
            return 0;
        }
        if (file == null || !file.isFile()) {
            file = findNewestTrackedProjectFile(updateCleanupPlan);
        }
        if (file != null && file.isFile()) {
            String strSafeCanonicalPath = safeCanonicalPath(file);
            HashSet<String> hashSet = new HashSet<>();
            ArrayList<File> arrayList = new ArrayList<>();
            collectTrackedProjectFilesForCleanup(arrayList, hashSet, getInstalledManifestEntriesForProject(updateCleanupPlan.contentType, updateCleanupPlan.source, updateCleanupPlan.projectId), updateCleanupPlan.contentType);
            collectTrackedProjectFilesForCleanup(arrayList, hashSet, getModpackInstalledEntriesForProject(updateCleanupPlan.contentType, updateCleanupPlan.source, updateCleanupPlan.projectId), updateCleanupPlan.contentType);
            for (File file2 : arrayList) {
                if (file2 != null && file2.isFile() && !safeCanonicalPath(file2).equals(strSafeCanonicalPath)) {
                    try {
                        ModManagerManifest.removeEntryForFile(this.gameDirectory, updateCleanupPlan.contentType, file2);
                    } catch (Throwable unused) {
                    }
                    try {
                        deleteFileOrDirectory(file2);
                        i++;
                    } catch (Throwable th) {
                        Logging.i(TAG, "Unable to delete duplicate updated file " + file2.getAbsolutePath() + ": " + readableError(th));
                    }
                }
            }
        }
        return i;
    }

    private void collectTrackedProjectFilesForCleanup(ArrayList<File> arrayList, HashSet<String> hashSet, ArrayList<JSONObject> arrayList2, ModManagerContentType modManagerContentType) {
        Iterator<JSONObject> it = arrayList2.iterator();
        while (it.hasNext()) {
            File fileResolveFileForInstalledEntry = resolveFileForInstalledEntry(it.next(), modManagerContentType);
            if (fileResolveFileForInstalledEntry != null && fileResolveFileForInstalledEntry.isFile() && hashSet.add(safeCanonicalPath(fileResolveFileForInstalledEntry))) {
                arrayList.add(fileResolveFileForInstalledEntry);
            }
        }
    }

    private File findNewestTrackedProjectFile(UpdateCleanupPlan updateCleanupPlan) {
        ArrayList<File> arrayList = new ArrayList<>();
        HashSet<String> hashSet = new HashSet<>();
        collectTrackedProjectFilesForCleanup(arrayList, hashSet, getInstalledManifestEntriesForProject(updateCleanupPlan.contentType, updateCleanupPlan.source, updateCleanupPlan.projectId), updateCleanupPlan.contentType);
        collectTrackedProjectFilesForCleanup(arrayList, hashSet, getModpackInstalledEntriesForProject(updateCleanupPlan.contentType, updateCleanupPlan.source, updateCleanupPlan.projectId), updateCleanupPlan.contentType);
        String strResolveLatestFileName = resolveLatestFileName(updateCleanupPlan.candidate);
        File file = null;
        long j = Long.MIN_VALUE;
        for (File file2 : arrayList) {
            if (file2 != null && file2.isFile()) {
                if (!isBlank(strResolveLatestFileName) && file2.getName().equalsIgnoreCase(strResolveLatestFileName)) {
                    return file2;
                }
                long jLastModified = file2.lastModified();
                if (file == null || jLastModified > j) {
                    file = file2;
                    j = jLastModified;
                }
            }
        }
        return file;
    }

    private File findReplacementFileForUpdate(UpdateCleanupPlan updateCleanupPlan) {
        File fileFindBestReplacementFileFromEntries = findBestReplacementFileFromEntries(updateCleanupPlan, getInstalledManifestEntriesForProject(updateCleanupPlan.contentType, updateCleanupPlan.source, updateCleanupPlan.projectId));
        if (fileFindBestReplacementFileFromEntries != null) {
            return fileFindBestReplacementFileFromEntries;
        }
        File fileFindBestReplacementFileFromEntries2 = findBestReplacementFileFromEntries(updateCleanupPlan, getModpackInstalledEntriesForProject(updateCleanupPlan.contentType, updateCleanupPlan.source, updateCleanupPlan.projectId));
        if (fileFindBestReplacementFileFromEntries2 != null) {
            return fileFindBestReplacementFileFromEntries2;
        }
        String strResolveLatestFileName = resolveLatestFileName(updateCleanupPlan.candidate);
        if (!isBlank(strResolveLatestFileName) && this.gameDirectory != null) {
            File targetDirectory = updateCleanupPlan.contentType.getTargetDirectory(this.gameDirectory);
            File file = new File(targetDirectory, strResolveLatestFileName);
            if (file.isFile()) {
                return file;
            }
            File file2 = new File(targetDirectory, strResolveLatestFileName + ".disabled");
            if (file2.isFile()) {
                return file2;
            }
        }
        File fileFindNewestNewFileInTargetDirectory = findNewestNewFileInTargetDirectory(updateCleanupPlan);
        if (fileFindNewestNewFileInTargetDirectory == null || !fileFindNewestNewFileInTargetDirectory.isFile()) {
            return null;
        }
        return fileFindNewestNewFileInTargetDirectory;
    }

    private File findBestReplacementFileFromEntries(UpdateCleanupPlan updateCleanupPlan, ArrayList<JSONObject> arrayList) {
        Iterator<JSONObject> it = arrayList.iterator();
        while (it.hasNext()) {
            File fileResolveFileForInstalledEntry = resolveFileForInstalledEntry(it.next(), updateCleanupPlan.contentType);
            if (fileResolveFileForInstalledEntry != null && fileResolveFileForInstalledEntry.isFile() && !updateCleanupPlan.oldCanonicalPaths.contains(safeCanonicalPath(fileResolveFileForInstalledEntry))) {
                return fileResolveFileForInstalledEntry;
            }
        }
        return null;
    }

    private File findNewestNewFileInTargetDirectory(UpdateCleanupPlan updateCleanupPlan) {
        File[] fileArrListFiles;
        File file = null;
        if (this.gameDirectory == null || (fileArrListFiles = updateCleanupPlan.contentType.getTargetDirectory(this.gameDirectory).listFiles(new FileFilter() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda22
            @Override // java.io.FileFilter
            public final boolean accept(File file2) {
                return InstanceDetailsActivity.lambda$findNewestNewFileInTargetDirectory$90(file2);
            }
        })) == null) {
            return null;
        }
        long j = updateCleanupPlan.createdAt - 2000;
        for (File file2 : fileArrListFiles) {
            if (!updateCleanupPlan.oldCanonicalPaths.contains(safeCanonicalPath(file2))) {
                long jLastModified = file2.lastModified();
                if (jLastModified >= j) {
                    file = file2;
                    j = jLastModified;
                }
            }
        }
        return file;
    }

    static /* synthetic */ boolean lambda$findNewestNewFileInTargetDirectory$90(File file) {
        return file.isFile() && !file.isHidden();
    }

    private JSONObject findReplacementEntryForUpdate(UpdateCleanupPlan updateCleanupPlan, File file) {
        JSONObject jSONObjectFindMatchingEntryForFile = findMatchingEntryForFile(getInstalledManifestEntriesForProject(updateCleanupPlan.contentType, updateCleanupPlan.source, updateCleanupPlan.projectId), updateCleanupPlan.contentType, file);
        if (jSONObjectFindMatchingEntryForFile != null) {
            return jSONObjectFindMatchingEntryForFile;
        }
        JSONObject jSONObjectFindMatchingEntryForFile2 = findMatchingEntryForFile(getModpackInstalledEntriesForProject(updateCleanupPlan.contentType, updateCleanupPlan.source, updateCleanupPlan.projectId), updateCleanupPlan.contentType, file);
        if (jSONObjectFindMatchingEntryForFile2 != null) {
            return jSONObjectFindMatchingEntryForFile2;
        }
        if (file == null || !file.isFile()) {
            return null;
        }
        return buildFallbackUpdatedEntry(updateCleanupPlan, file);
    }

    private JSONObject findMatchingEntryForFile(ArrayList<JSONObject> arrayList, ModManagerContentType modManagerContentType, File file) {
        if (arrayList.isEmpty()) {
            return null;
        }
        if (file != null) {
            String strSafeCanonicalPath = safeCanonicalPath(file);
            for (JSONObject jSONObject : arrayList) {
                File fileResolveFileForInstalledEntry = resolveFileForInstalledEntry(jSONObject, modManagerContentType);
                if (fileResolveFileForInstalledEntry != null && strSafeCanonicalPath.equals(safeCanonicalPath(fileResolveFileForInstalledEntry))) {
                    return jSONObject;
                }
            }
        }
        return arrayList.get(0);
    }

    private void syncModpackFilesManifestAfterUpdate(UpdateCleanupPlan updateCleanupPlan, File file, JSONObject jSONObject) throws Exception {
        JSONObject jSONObject2;
        if (this.gameDirectory == null) {
            return;
        }
        File modpackFilesManifestFile = getModpackFilesManifestFile();
        boolean zIsFile = modpackFilesManifestFile.isFile();
        if (zIsFile || updateCleanupPlan.hadModpackMetadata) {
            JSONObject jSONObject3 = zIsFile ? new JSONObject(readTextFile(modpackFilesManifestFile)) : new JSONObject();
            JSONArray jSONArrayOptJSONArray = jSONObject3.optJSONArray("files");
            if (jSONArrayOptJSONArray == null) {
                jSONArrayOptJSONArray = new JSONArray();
            }
            JSONArray jSONArray = new JSONArray();
            for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
                JSONObject jSONObjectOptJSONObject = jSONArrayOptJSONArray.optJSONObject(i);
                if (jSONObjectOptJSONObject != null) {
                    ModManagerContentType modManagerContentTypeResolveContentTypeFromEntry = resolveContentTypeFromEntry(jSONObjectOptJSONObject);
                    File fileResolveFileForInstalledEntry = modManagerContentTypeResolveContentTypeFromEntry == null ? null : resolveFileForInstalledEntry(jSONObjectOptJSONObject, modManagerContentTypeResolveContentTypeFromEntry);
                    boolean z = (fileResolveFileForInstalledEntry == null || fileResolveFileForInstalledEntry.isFile()) ? false : true;
                    if (!entryMatchesProject(jSONObjectOptJSONObject, updateCleanupPlan.contentType, updateCleanupPlan.source, updateCleanupPlan.projectId) && !z) {
                        jSONArray.put(jSONObjectOptJSONObject);
                    }
                }
            }
            if (file != null && file.isFile()) {
                if (jSONObject == null) {
                    jSONObject2 = buildFallbackUpdatedEntry(updateCleanupPlan, file);
                } else {
                    jSONObject2 = new JSONObject(jSONObject.toString());
                }
                normalizeUpdatedManifestEntry(updateCleanupPlan, jSONObject2, file);
                jSONArray.put(jSONObject2);
            }
            jSONObject3.put("files", jSONArray);
            writeTextFile(modpackFilesManifestFile, jSONObject3.toString(2));
        }
    }

    private JSONObject buildFallbackUpdatedEntry(UpdateCleanupPlan updateCleanupPlan, File file) {
        JSONObject jSONObject = new JSONObject();
        try {
            jSONObject.put("source", updateCleanupPlan.source.getId());
            jSONObject.put("platform", updateCleanupPlan.source.getId());
            jSONObject.put("modpackPlatform", updateCleanupPlan.source.getId());
            jSONObject.put("contentType", updateCleanupPlan.contentType.getIntentValue());
            jSONObject.put("type", updateCleanupPlan.contentType.getIntentValue());
            jSONObject.put("platformProjectId", updateCleanupPlan.projectId);
            jSONObject.put("projectId", updateCleanupPlan.projectId);
            jSONObject.put("versionNumber", updateCleanupPlan.candidate.latestVersion.versionNumber);
            String strResolveLatestFileName = resolveLatestFileName(updateCleanupPlan.candidate);
            if (!isBlank(strResolveLatestFileName)) {
                jSONObject.put("remoteFileName", strResolveLatestFileName);
            }
            String latestVersionString = readLatestVersionString(updateCleanupPlan.candidate, "versionId", "id");
            if (!isBlank(latestVersionString)) {
                jSONObject.put("versionId", latestVersionString);
            }
            String latestVersionString2 = readLatestVersionString(updateCleanupPlan.candidate, "fileId", "fileID", "curseForgeFileId", "curseforgeFileId");
            if (!isBlank(latestVersionString2)) {
                jSONObject.put("fileId", latestVersionString2);
            }
            String latestVersionString3 = readLatestVersionString(updateCleanupPlan.candidate, "downloadUrl", "url");
            if (!isBlank(latestVersionString3)) {
                jSONObject.put("downloadUrl", latestVersionString3);
            }
        } catch (Throwable unused) {
        }
        normalizeUpdatedManifestEntry(updateCleanupPlan, jSONObject, file);
        return jSONObject;
    }

    private void normalizeUpdatedManifestEntry(UpdateCleanupPlan updateCleanupPlan, JSONObject jSONObject, File file) {
        try {
            jSONObject.put("source", updateCleanupPlan.source.getId());
            jSONObject.put("platform", updateCleanupPlan.source.getId());
            if (!jSONObject.has("modpackPlatform")) {
                jSONObject.put("modpackPlatform", updateCleanupPlan.source.getId());
            }
            jSONObject.put("contentType", updateCleanupPlan.contentType.getIntentValue());
            jSONObject.put("type", updateCleanupPlan.contentType.getIntentValue());
            jSONObject.put("platformProjectId", updateCleanupPlan.projectId);
            jSONObject.put("projectId", updateCleanupPlan.projectId);
            jSONObject.put("fileName", file.getName());
            jSONObject.put("relativePath", getRelativePathFromGameDirectory(file));
            jSONObject.put("filePath", getRelativePathFromGameDirectory(file));
            jSONObject.put("absolutePath", file.getAbsolutePath());
            jSONObject.put("canonicalPath", safeCanonicalPath(file));
            jSONObject.put("installedAt", System.currentTimeMillis());
            jSONObject.put("updatedBy", "JavaLauncher");
            if (jSONObject.has("versionNumber")) {
                return;
            }
            jSONObject.put("versionNumber", updateCleanupPlan.candidate.latestVersion.versionNumber);
        } catch (Throwable unused) {
        }
    }

    private ArrayList<JSONObject> getInstalledManifestEntriesForProject(ModManagerContentType modManagerContentType, ModManagerSource modManagerSource, String str) {
        ArrayList<JSONObject> arrayList = new ArrayList<>();
        File file = this.gameDirectory;
        if (file == null) {
            return arrayList;
        }
        for (JSONObject jSONObject : ModManagerManifest.getInstalledEntries(file, modManagerContentType)) {
            if (entryMatchesProject(jSONObject, modManagerContentType, modManagerSource, str)) {
                arrayList.add(jSONObject);
            }
        }
        return arrayList;
    }

    private ArrayList<JSONObject> getModpackInstalledEntriesForProject(ModManagerContentType modManagerContentType, ModManagerSource modManagerSource, String str) {
        ArrayList<JSONObject> arrayList = new ArrayList<>();
        for (JSONObject jSONObject : getModpackInstalledEntriesForType(modManagerContentType)) {
            if (entryMatchesProject(jSONObject, modManagerContentType, modManagerSource, str)) {
                arrayList.add(jSONObject);
            }
        }
        return arrayList;
    }

    private boolean entryMatchesProject(JSONObject jSONObject, ModManagerContentType modManagerContentType, ModManagerSource modManagerSource, String str) {
        String strOptString = jSONObject.optString("contentType", jSONObject.optString("type", ""));
        if (!isBlank(strOptString) && !modManagerContentType.getIntentValue().equalsIgnoreCase(strOptString)) {
            return false;
        }
        ModManagerSource source = ModManagerManifest.getSource(jSONObject);
        if (source == ModManagerSource.UNKNOWN || source == modManagerSource) {
            return !isBlank(str) && str.equalsIgnoreCase(getProjectIdFromEntry(jSONObject));
        }
        return false;
    }

    private String getProjectIdFromEntry(JSONObject jSONObject) {
        String strTrim = jSONObject.optString("platformProjectId", "").trim();
        if (strTrim.isEmpty()) {
            strTrim = jSONObject.optString("projectId", "").trim();
        }
        if (strTrim.isEmpty()) {
            strTrim = jSONObject.optString("modrinthProjectId", "").trim();
        }
        if (strTrim.isEmpty()) {
            strTrim = jSONObject.optString("modrinth_project_id", "").trim();
        }
        if (strTrim.isEmpty()) {
            strTrim = jSONObject.optString("curseForgeProjectId", "").trim();
        }
        if (strTrim.isEmpty()) {
            strTrim = jSONObject.optString("curseforgeProjectId", "").trim();
        }
        if (strTrim.isEmpty()) {
            strTrim = jSONObject.optString("curseForgeProjectID", "").trim();
        }
        return strTrim.isEmpty() ? jSONObject.optString("projectID", "").trim() : strTrim;
    }

    private String sanitizeProjectId(String str) {
        return str == null ? "" : str.trim();
    }

    private ModManagerContentType resolveContentTypeFromEntry(JSONObject jSONObject) {
        String strOptString = jSONObject.optString("contentType", jSONObject.optString("type", ""));
        if (strOptString.equalsIgnoreCase(ModManagerContentType.MODS.getIntentValue())) {
            return ModManagerContentType.MODS;
        }
        if (strOptString.equalsIgnoreCase(ModManagerContentType.RESOURCEPACKS.getIntentValue())) {
            return ModManagerContentType.RESOURCEPACKS;
        }
        if (strOptString.equalsIgnoreCase(ModManagerContentType.SHADERPACKS.getIntentValue())) {
            return ModManagerContentType.SHADERPACKS;
        }
        return null;
    }

    private File resolveFileForInstalledEntry(JSONObject jSONObject, ModManagerContentType modManagerContentType) {
        File fileResolveFileForInstalledEntry = resolveFileForInstalledEntry(jSONObject);
        if ((fileResolveFileForInstalledEntry != null && fileResolveFileForInstalledEntry.isFile()) || this.gameDirectory == null) {
            return fileResolveFileForInstalledEntry;
        }
        String strOptString = jSONObject.optString("fileName", "");
        if (!isBlank(strOptString)) {
            File targetDirectory = modManagerContentType.getTargetDirectory(this.gameDirectory);
            File file = new File(targetDirectory, strOptString);
            if (file.isFile()) {
                return file;
            }
            File file2 = new File(targetDirectory, strOptString + ".disabled");
            if (file2.isFile()) {
                return file2;
            }
            File file3 = new File(targetDirectory, stripDisabledSuffix(strOptString));
            if (file3.isFile()) {
                return file3;
            }
        }
        return fileResolveFileForInstalledEntry;
    }

    private File getModpackFilesManifestFile() {
        return new File(new File(this.gameDirectory, ".javalauncher"), "modpack_files_manifest.json");
    }

    private String formatUpdateCleanupSuffix(int i) {
        if (i <= 0) {
            return "";
        }
        return " · removed " + i + " old " + (i == 1 ? "file" : "files");
    }

    private String resolveLatestFileName(ModManagerUpdateManager.UpdateCandidate updateCandidate) {
        int i;
        String latestVersionString = readLatestVersionString(updateCandidate, "fileName", "filename", "primaryFileName", "name");
        if (!isBlank(latestVersionString)) {
            return latestVersionString;
        }
        String latestVersionString2 = readLatestVersionString(updateCandidate, "downloadUrl", "url");
        if (isBlank(latestVersionString2)) {
            return "";
        }
        int iLastIndexOf = latestVersionString2.lastIndexOf(47);
        if (iLastIndexOf >= 0 && (i = iLastIndexOf + 1) < latestVersionString2.length()) {
            latestVersionString2 = latestVersionString2.substring(i);
        }
        int iIndexOf = latestVersionString2.indexOf(63);
        if (iIndexOf >= 0) {
            latestVersionString2 = latestVersionString2.substring(0, iIndexOf);
        }
        return latestVersionString2.trim();
    }

    private String readLatestVersionString(ModManagerUpdateManager.UpdateCandidate updateCandidate, String... strArr) {
        ModrinthVersion modrinthVersion = updateCandidate.latestVersion;
        if (modrinthVersion == null) {
            return "";
        }
        return readObjectStringField(modrinthVersion, strArr);
    }

    private String readObjectStringField(Object obj, String... strArr) {
        for (String str : strArr) {
            try {
                Object obj2 = obj.getClass().getField(str).get(obj);
                if (obj2 != null) {
                    String strTrim = String.valueOf(obj2).trim();
                    if (!strTrim.isEmpty() && !"null".equalsIgnoreCase(strTrim)) {
                        return strTrim;
                    }
                } else {
                    continue;
                }
            } catch (Throwable unused) {
            }
        }
        return "";
    }

    private void writeTextFile(File file, String str) throws IOException {
        File parentFile = file.getParentFile();
        if (parentFile != null && !parentFile.exists() && !parentFile.mkdirs()) {
            throw new IOException("Unable to create folder: " + parentFile.getAbsolutePath());
        }
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        try {
            fileOutputStream.write(str.getBytes("UTF-8"));
            fileOutputStream.close();
        } catch (Throwable th) {
            try {
                fileOutputStream.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class UpdateCleanupPlan {
        final ModManagerUpdateManager.UpdateCandidate candidate;
        final ModManagerContentType contentType;
        final long createdAt;
        boolean hadModpackMetadata;
        final String key;
        final String projectId;
        final ModManagerSource source;
        final ArrayList<File> oldFiles = new ArrayList<>();
        final HashSet<String> oldCanonicalPaths = new HashSet<>();

        UpdateCleanupPlan(ModManagerUpdateManager.UpdateCandidate updateCandidate, ModManagerContentType modManagerContentType, ModManagerSource modManagerSource, String str, String str2, long j) {
            this.candidate = updateCandidate;
            this.contentType = modManagerContentType;
            this.source = modManagerSource;
            this.projectId = str;
            this.key = str2;
            this.createdAt = j;
        }
    }

    private void markTrackedEntriesUpToDate(ModManagerContentType modManagerContentType) {
        if (this.gameDirectory == null) {
            return;
        }
        removeUpdateCandidatesForType(modManagerContentType);
        for (JSONObject jSONObject : ModManagerManifest.getInstalledEntries(this.gameDirectory, modManagerContentType)) {
            ModManagerSource source = ModManagerManifest.getSource(jSONObject);
            if (source == ModManagerSource.MODRINTH || source == ModManagerSource.CURSEFORGE) {
                String strBuildUpdateKey = buildUpdateKey(modManagerContentType, jSONObject);
                this.updateStates.put(strBuildUpdateKey, UpdateState.UP_TO_DATE);
                this.updateMessages.put(strBuildUpdateKey, getString(R.string.instance_content_up_to_date));
            }
        }
        for (JSONObject jSONObject2 : getModpackInstalledEntriesForType(modManagerContentType)) {
            ModManagerSource source2 = ModManagerManifest.getSource(jSONObject2);
            if (source2 == ModManagerSource.MODRINTH || source2 == ModManagerSource.CURSEFORGE) {
                String strBuildUpdateKey2 = buildUpdateKey(modManagerContentType, jSONObject2);
                this.updateStates.put(strBuildUpdateKey2, UpdateState.UP_TO_DATE);
                this.updateMessages.put(strBuildUpdateKey2, getString(R.string.instance_content_up_to_date));
            }
        }
    }

    private void removeUpdateCandidatesForType(ModManagerContentType modManagerContentType) {
        String str = modManagerContentType.name() + ":";
        for (String str2 : new ArrayList(this.updateCandidates.keySet())) {
            if (str2.startsWith(str)) {
                this.updateCandidates.remove(str2);
            }
        }
        for (String str3 : new ArrayList(this.updateStates.keySet())) {
            if (str3.startsWith(str)) {
                this.updateStates.remove(str3);
            }
        }
        for (String str4 : new ArrayList(this.updateMessages.keySet())) {
            if (str4.startsWith(str)) {
                this.updateMessages.remove(str4);
            }
        }
    }

    private String getGameVersionIdForContent() {
        return isBlank(this.minecraftVersionId) ? ModManagerVersionResolver.resolveGameVersionForContent(this.baseVersionId) : this.minecraftVersionId;
    }

    private String buildUpdateKey(ModManagerUpdateManager.UpdateCandidate updateCandidate) {
        return updateCandidate.contentType.name() + ":" + updateCandidate.source.getId() + ":" + updateCandidate.getProjectId();
    }

    private String buildUpdateKey(ModManagerContentType modManagerContentType, JSONObject jSONObject) {
        ModManagerSource source = ModManagerManifest.getSource(jSONObject);
        String projectIdFromEntry = getProjectIdFromEntry(jSONObject);
        if (projectIdFromEntry.isEmpty()) {
            projectIdFromEntry = jSONObject.optString("fileName", EnvironmentCompat.MEDIA_UNKNOWN).trim();
        }
        return modManagerContentType.name() + ":" + source.getId() + ":" + projectIdFromEntry;
    }

    private String buildUpdateKey(InstanceContentItem instanceContentItem) {
        ModManagerContentType modManagerContentType = toModManagerContentType(instanceContentItem.category);
        if (modManagerContentType == null || this.gameDirectory == null) {
            return instanceContentItem.category.name() + ":file:" + safeCanonicalPath(instanceContentItem.file);
        }
        JSONObject installedEntryForItem = getInstalledEntryForItem(instanceContentItem);
        if (installedEntryForItem == null) {
            return modManagerContentType.name() + ":file:" + safeCanonicalPath(instanceContentItem.file);
        }
        return buildUpdateKey(modManagerContentType, installedEntryForItem);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public UpdateState getUpdateStateForItem(InstanceContentItem instanceContentItem) {
        UpdateState updateState = this.updateStates.get(buildUpdateKey(instanceContentItem));
        return updateState == null ? UpdateState.UNKNOWN : updateState;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getUpdateMessageForItem(InstanceContentItem instanceContentItem) {
        return this.updateMessages.get(buildUpdateKey(instanceContentItem));
    }

    private JSONObject getInstalledEntryForItem(InstanceContentItem instanceContentItem) {
        File file;
        ModManagerContentType modManagerContentType = toModManagerContentType(instanceContentItem.category);
        if (modManagerContentType == null || (file = this.gameDirectory) == null) {
            return null;
        }
        JSONObject installedEntryForFile = ModManagerManifest.getInstalledEntryForFile(file, modManagerContentType, instanceContentItem.file);
        return installedEntryForFile != null ? installedEntryForFile : getModpackInstalledEntryForFile(modManagerContentType, instanceContentItem.file);
    }

    private JSONObject getModpackInstalledEntryForFile(ModManagerContentType modManagerContentType, File file) {
        for (JSONObject jSONObject : getModpackInstalledEntriesForType(modManagerContentType)) {
            if (matchesInstalledContentEntry(jSONObject, file)) {
                return jSONObject;
            }
        }
        return null;
    }

    private ArrayList<JSONObject> getModpackInstalledEntriesForType(ModManagerContentType modManagerContentType) {
        ArrayList<JSONObject> arrayList = new ArrayList<>();
        if (this.gameDirectory == null) {
            return arrayList;
        }
        File file = new File(new File(this.gameDirectory, ".javalauncher"), "modpack_files_manifest.json");
        if (!file.isFile()) {
            return arrayList;
        }
        try {
            JSONArray jSONArrayOptJSONArray = new JSONObject(readTextFile(file)).optJSONArray("files");
            if (jSONArrayOptJSONArray == null) {
                return arrayList;
            }
            String intentValue = modManagerContentType.getIntentValue();
            for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
                JSONObject jSONObjectOptJSONObject = jSONArrayOptJSONArray.optJSONObject(i);
                if (jSONObjectOptJSONObject != null && intentValue.equalsIgnoreCase(jSONObjectOptJSONObject.optString("contentType", jSONObjectOptJSONObject.optString("type", "")))) {
                    arrayList.add(jSONObjectOptJSONObject);
                }
            }
        } catch (Throwable th) {
            Logging.i(TAG, "Unable to read modpack installed-content metadata: " + readableError(th));
        }
        return arrayList;
    }

    private boolean matchesInstalledContentEntry(JSONObject jSONObject, File file) {
        String strSafeCanonicalPath = safeCanonicalPath(file);
        if (matchesPathValue(strSafeCanonicalPath, jSONObject.optString("canonicalPath", "")) || matchesPathValue(strSafeCanonicalPath, jSONObject.optString("absolutePath", ""))) {
            return true;
        }
        String relativePathFromGameDirectory = getRelativePathFromGameDirectory(file);
        String strStripDisabledSuffix = stripDisabledSuffix(relativePathFromGameDirectory);
        if (matchesRelativePath(relativePathFromGameDirectory, jSONObject.optString("relativePath", "")) || matchesRelativePath(relativePathFromGameDirectory, jSONObject.optString("filePath", "")) || matchesRelativePath(relativePathFromGameDirectory, jSONObject.optString("path", "")) || matchesRelativePath(strStripDisabledSuffix, jSONObject.optString("relativePath", "")) || matchesRelativePath(strStripDisabledSuffix, jSONObject.optString("filePath", "")) || matchesRelativePath(strStripDisabledSuffix, jSONObject.optString("path", ""))) {
            return true;
        }
        String strOptString = jSONObject.optString("fileName", "");
        return !isBlank(strOptString) && stripDisabledSuffix(file.getName()).equalsIgnoreCase(stripDisabledSuffix(strOptString));
    }

    private boolean matchesPathValue(String str, String str2) {
        if (isBlank(str2)) {
            return false;
        }
        return str.equals(safeCanonicalPath(new File(str2)));
    }

    private boolean matchesRelativePath(String str, String str2) {
        if (isBlank(str2)) {
            return false;
        }
        return normalizeContentPath(str).equals(normalizeContentPath(str2));
    }

    private String normalizeContentPath(String str) {
        String strTrim = str.replace('\\', '/').trim();
        while (strTrim.startsWith("/")) {
            strTrim = strTrim.substring(1);
        }
        return strTrim.toLowerCase(Locale.US);
    }

    private String getRelativePathFromGameDirectory(File file) {
        File file2 = this.gameDirectory;
        if (file2 == null) {
            return file.getName();
        }
        String strSafeCanonicalPath = safeCanonicalPath(file2);
        String strSafeCanonicalPath2 = safeCanonicalPath(file);
        if (strSafeCanonicalPath2.equals(strSafeCanonicalPath)) {
            return "";
        }
        if (strSafeCanonicalPath2.startsWith(strSafeCanonicalPath + File.separator)) {
            return strSafeCanonicalPath2.substring(strSafeCanonicalPath.length() + 1).replace(File.separatorChar, '/');
        }
        return file.getName();
    }

    private File resolveFileForInstalledEntry(JSONObject jSONObject) {
        if (this.gameDirectory == null) {
            return null;
        }
        String strOptString = jSONObject.optString("relativePath", jSONObject.optString("filePath", jSONObject.optString("path", "")));
        if (!isBlank(strOptString)) {
            File file = new File(this.gameDirectory, strOptString.replace('/', File.separatorChar));
            if (file.isFile()) {
                return file;
            }
            File file2 = new File(this.gameDirectory, strOptString.replace('/', File.separatorChar) + ".disabled");
            if (file2.isFile()) {
                return file2;
            }
        }
        String strOptString2 = jSONObject.optString("absolutePath", "");
        if (!isBlank(strOptString2)) {
            File file3 = new File(strOptString2);
            if (file3.isFile()) {
                return file3;
            }
        }
        return null;
    }

    private boolean containsUpdateCandidate(ArrayList<ModManagerUpdateManager.UpdateCandidate> arrayList, ModManagerUpdateManager.UpdateCandidate updateCandidate) {
        String strBuildUpdateKey = buildUpdateKey(updateCandidate);
        Iterator<ModManagerUpdateManager.UpdateCandidate> it = arrayList.iterator();
        while (it.hasNext()) {
            if (strBuildUpdateKey.equals(buildUpdateKey(it.next()))) {
                return true;
            }
        }
        return false;
    }

    private String readTextFile(File file) throws IOException {
        InputStream fileInputStream = new FileInputStream(file);
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try {
                copyStream(fileInputStream, byteArrayOutputStream);
                String string = byteArrayOutputStream.toString("UTF-8");
                byteArrayOutputStream.close();
                fileInputStream.close();
                return string;
            } finally {
            }
        } catch (Throwable th) {
            try {
                fileInputStream.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    private void showUpdateProgressDialog(String str, String str2, boolean z, int i, boolean z2) {
        dismissUpdateProgressDialog();
        getWindow().addFlags(128);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        int iDp = dp(22);
        linearLayout.setPadding(iDp, dp(6), iDp, 0);
        TextView textView = new TextView(this);
        this.updateProgressMessage = textView;
        textView.setText(str2);
        linearLayout.addView(this.updateProgressMessage, new LinearLayout.LayoutParams(-1, -2));
        ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        this.updateProgressBar = progressBar;
        progressBar.setIndeterminate(z);
        this.updateProgressBar.setMax(Math.max(1, i));
        this.updateProgressBar.setProgress(0);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = dp(12);
        linearLayout.addView(this.updateProgressBar, layoutParams);
        AlertDialog alertDialogCreate = new AlertDialog.Builder(this).setTitle(str).setView(linearLayout).setCancelable(z2).create();
        this.updateProgressDialog = alertDialogCreate;
        alertDialogCreate.setOnDismissListener(new DialogInterface.OnDismissListener() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda41
            @Override // android.content.DialogInterface.OnDismissListener
            public final void onDismiss(DialogInterface dialogInterface) {
                InstanceDetailsActivity.this.lambda$showUpdateProgressDialog$91(dialogInterface);
            }
        });
        this.updateProgressDialog.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showUpdateProgressDialog$91(DialogInterface dialogInterface) {
        getWindow().clearFlags(128);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setUpdateProgressMessage(String str) {
        TextView textView = this.updateProgressMessage;
        if (textView != null) {
            textView.setText(str);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: renamed from: setUpdateProgress, reason: merged with bridge method [inline-methods] and merged with bridge method [inline-methods] and merged with bridge method [inline-methods] */
    public void lambda$checkUpdatesForSelectedCategory$76(int i, int i2) {
        ProgressBar progressBar = this.updateProgressBar;
        if (progressBar == null) {
            return;
        }
        progressBar.setIndeterminate(false);
        this.updateProgressBar.setMax(Math.max(1, i2));
        this.updateProgressBar.setProgress(Math.max(0, Math.min(i, Math.max(1, i2))));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dismissUpdateProgressDialog() {
        AlertDialog alertDialog = this.updateProgressDialog;
        if (alertDialog != null) {
            alertDialog.dismiss();
            this.updateProgressDialog = null;
        }
        this.updateProgressMessage = null;
        this.updateProgressBar = null;
        getWindow().clearFlags(128);
    }

    private void pickSelectedContent() {
        if (this.selectedCategory == ResourceCategory.MODS && !supportsMods()) {
            Toast.makeText(this, R.string.mods_vanilla_hint, 1).show();
            return;
        }
        this.pendingImportCategory = this.selectedCategory;
        Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT");
        intent.addCategory("android.intent.category.OPENABLE");
        intent.setType(this.pendingImportCategory.mimeType);
        intent.putExtra("android.intent.extra.ALLOW_MULTIPLE", true);
        if (this.pendingImportCategory.mimeTypes.length > 0) {
            intent.putExtra("android.intent.extra.MIME_TYPES", this.pendingImportCategory.mimeTypes);
        }
        try {
            startActivityForResult(Intent.createChooser(intent, getString(this.pendingImportCategory.pickerTitleRes)), REQUEST_PICK_CONTENT);
        } catch (ActivityNotFoundException unused) {
            Toast.makeText(this, R.string.mods_picker_missing, 1).show();
        }
    }

    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, android.app.Activity
    protected void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        if (i == REQUEST_EXPORT_WORLD) {
            File file = this.pendingWorldExportDirectory;
            this.pendingWorldExportDirectory = null;
            if (i2 != -1 || intent == null || intent.getData() == null || file == null) {
                return;
            }
            exportWorldToUri(intent.getData(), file);
            return;
        }
        if (i == REQUEST_EXPORT_MODPACK) {
            ModpackExportManager.Platform platform = this.pendingExportPlatform;
            this.pendingExportPlatform = null;
            if (i2 != -1 || intent == null || intent.getData() == null || platform == null) {
                return;
            }
            exportModpackToUri(intent.getData(), platform);
            return;
        }
        if (i == REQUEST_IMPORT_MODPACK) {
            if (i2 != -1 || intent == null || intent.getData() == null) {
                return;
            }
            importModpackFromUri(intent.getData());
            return;
        }
        if (i == REQUEST_PICK_INSTANCE_ICON) {
            if (i2 != -1 || intent == null || intent.getData() == null) {
                return;
            }
            savePickedInstanceIcon(intent.getData());
            return;
        }
        if (i == REQUEST_PICK_CONTENT && i2 == -1 && intent != null) {
            final ArrayList<Uri> arrayListCollectSelectedUris = collectSelectedUris(intent);
            if (arrayListCollectSelectedUris.isEmpty()) {
                return;
            }
            final ResourceCategory resourceCategory = this.pendingImportCategory;
            new Thread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda31
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$onActivityResult$92(arrayListCollectSelectedUris, resourceCategory);
                }
            }, "Import Instance Content").start();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: renamed from: importSelectedContent, reason: merged with bridge method [inline-methods] */
    public void lambda$onActivityResult$92(ArrayList<Uri> arrayList, final ResourceCategory resourceCategory) {
        File directoryForCategory = getDirectoryForCategory(resourceCategory);
        try {
            if (!directoryForCategory.exists() && !directoryForCategory.mkdirs()) {
                throw new IllegalStateException("Unable to create folder: " + directoryForCategory.getAbsolutePath());
            }
            final int i = 0;
            for (Uri uri : arrayList) {
                if (resourceCategory == ResourceCategory.WORLDS) {
                    importWorldArchive(uri, directoryForCategory);
                } else {
                    copyUriToFile(uri, uniqueTargetFile(directoryForCategory, sanitizeImportedFileName(resolveDisplayName(uri), resourceCategory.defaultExtension)));
                }
                i++;
            }
            runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda53
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$importSelectedContent$93(i, resourceCategory);
                }
            });
        } catch (Throwable th) {
            Logging.e(TAG, "Unable to import " + resourceCategory.name(), th);
            runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda54
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$importSelectedContent$94(resourceCategory, th);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$importSelectedContent$93(int i, ResourceCategory resourceCategory) {
        refreshContentList();
        Toast.makeText(this, getString(R.string.instance_content_imported_value, new Object[]{Integer.valueOf(i), getString(resourceCategory.pluralLabelRes)}), 0).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$importSelectedContent$94(ResourceCategory resourceCategory, Throwable th) {
        Toast.makeText(this, getString(R.string.instance_content_import_failed, new Object[]{getString(resourceCategory.pluralLabelRes), th.getMessage() != null ? th.getMessage() : th.getClass().getSimpleName()}), 1).show();
    }

    private ArrayList<Uri> collectSelectedUris(Intent intent) {
        ArrayList<Uri> arrayList = new ArrayList<>();
        ClipData clipData = intent.getClipData();
        if (clipData != null) {
            for (int i = 0; i < clipData.getItemCount(); i++) {
                Uri uri = clipData.getItemAt(i).getUri();
                if (uri != null) {
                    arrayList.add(uri);
                }
            }
        } else if (intent.getData() != null) {
            arrayList.add(intent.getData());
        }
        return arrayList;
    }

    private void importWorldArchive(Uri uri, File file) throws Exception {
        File fileCreateTempFile = File.createTempFile("world-import-", ".zip", getCacheDir());
        try {
            copyUriToFile(uri, fileCreateTempFile);
            String strFindWorldRootPrefix = findWorldRootPrefix(fileCreateTempFile);
            if (strFindWorldRootPrefix == null) {
                throw new IllegalStateException("Selected zip does not look like a Minecraft world. It must contain level.dat.");
            }
            String strStripExtension = stripExtension(sanitizeImportedFileName(resolveDisplayName(uri), ".zip"));
            String strSanitizeImportedFileName = sanitizeImportedFileName(lastFolderName(strFindWorldRootPrefix), null);
            if (!isBlank(strSanitizeImportedFileName)) {
                strStripExtension = strSanitizeImportedFileName;
            }
            if (isBlank(strStripExtension)) {
                strStripExtension = "Imported World";
            }
            extractWorldZip(fileCreateTempFile, uniqueTargetDirectory(file, strStripExtension), strFindWorldRootPrefix);
        } finally {
            fileCreateTempFile.delete();
        }
    }

    private String findWorldRootPrefix(File file) throws IOException {
        ZipFile zipFile = new ZipFile(file);
        try {
            Enumeration<? extends ZipEntry> enumerationEntries = zipFile.entries();
            while (enumerationEntries.hasMoreElements()) {
                ZipEntry zipEntryNextElement = enumerationEntries.nextElement();
                if (!zipEntryNextElement.isDirectory()) {
                    String strNormalizeZipPath = normalizeZipPath(zipEntryNextElement.getName());
                    if (strNormalizeZipPath.endsWith("level.dat")) {
                        String strSubstring = strNormalizeZipPath.substring(0, strNormalizeZipPath.length() - "level.dat".length());
                        zipFile.close();
                        return strSubstring;
                    }
                }
            }
            zipFile.close();
            return null;
        } catch (Throwable th) {
            try {
                zipFile.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    private void extractWorldZip(File file, File file2, String str) throws Exception {
        String canonicalPath = file2.getCanonicalPath();
        if (!file2.exists() && !file2.mkdirs()) {
            throw new IllegalStateException("Unable to create world folder: " + file2.getAbsolutePath());
        }
        String strNormalizeZipPath = str == null ? "" : normalizeZipPath(str);
        ZipFile zipFile = new ZipFile(file);
        try {
            Enumeration<? extends ZipEntry> enumerationEntries = zipFile.entries();
            while (enumerationEntries.hasMoreElements()) {
                ZipEntry zipEntryNextElement = enumerationEntries.nextElement();
                if (!zipEntryNextElement.isDirectory()) {
                    String strNormalizeZipPath2 = normalizeZipPath(zipEntryNextElement.getName());
                    if (!strNormalizeZipPath.isEmpty()) {
                        if (strNormalizeZipPath2.startsWith(strNormalizeZipPath)) {
                            strNormalizeZipPath2 = strNormalizeZipPath2.substring(strNormalizeZipPath.length());
                        }
                    }
                    while (strNormalizeZipPath2.startsWith("/")) {
                        strNormalizeZipPath2 = strNormalizeZipPath2.substring(1);
                    }
                    if (!isBlank(strNormalizeZipPath2)) {
                        File file3 = new File(file2, strNormalizeZipPath2);
                        String canonicalPath2 = file3.getCanonicalPath();
                        if (!canonicalPath2.equals(canonicalPath) && !canonicalPath2.startsWith(canonicalPath + File.separator)) {
                            throw new SecurityException("Blocked unsafe zip entry: " + zipEntryNextElement.getName());
                        }
                        File parentFile = file3.getParentFile();
                        if (parentFile != null && !parentFile.exists() && !parentFile.mkdirs()) {
                            throw new IllegalStateException("Unable to create folder: " + parentFile.getAbsolutePath());
                        }
                        InputStream inputStream = zipFile.getInputStream(zipEntryNextElement);
                        try {
                            FileOutputStream fileOutputStream = new FileOutputStream(file3);
                            try {
                                copyStream(inputStream, fileOutputStream);
                                fileOutputStream.close();
                                if (inputStream != null) {
                                    inputStream.close();
                                }
                            } finally {
                            }
                        } finally {
                        }
                    }
                }
            }
            zipFile.close();
        } catch (Throwable th) {
            try {
                zipFile.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startWorldExportFromRow(InstanceContentItem instanceContentItem) {
        prepareContentRowAction();
        startWorldExport(resolveContentItemForAction(instanceContentItem).file);
    }

    private void startWorldExport(File file) {
        if (!file.isDirectory() || !new File(file, "level.dat").isFile()) {
            Toast.makeText(this, "This folder does not look like a Minecraft world.", 1).show();
            return;
        }
        this.pendingWorldExportDirectory = file;
        Intent intent = new Intent("android.intent.action.CREATE_DOCUMENT");
        intent.addCategory("android.intent.category.OPENABLE");
        intent.setType("application/zip");
        intent.putExtra("android.intent.extra.TITLE", sanitizeImportedFileName(file.getName(), ".zip"));
        try {
            startActivityForResult(Intent.createChooser(intent, "Export World"), REQUEST_EXPORT_WORLD);
        } catch (ActivityNotFoundException unused) {
            this.pendingWorldExportDirectory = null;
            Toast.makeText(this, "No file picker is available for exporting.", 1).show();
        }
    }

    private void exportWorldToUri(final Uri uri, final File file) {
        showUpdateProgressDialog("Export World", "Preparing world export...", false, 100, false);
        new Thread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda57
            @Override // java.lang.Runnable
            public final void run() {
                InstanceDetailsActivity.this.lambda$exportWorldToUri$98(file, uri);
            }
        }, "Export World").start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$exportWorldToUri$98(final File file, Uri uri) {
        try {
            if (!file.isDirectory() || !new File(file, "level.dat").isFile()) {
                throw new IllegalStateException("This folder does not look like a Minecraft world.");
            }
            ArrayList<File> arrayList = new ArrayList<>();
            collectFilesForWorldExport(file, arrayList);
            if (arrayList.isEmpty()) {
                throw new IllegalStateException("World folder is empty.");
            }
            OutputStream outputStreamOpenOutputStream = getContentResolver().openOutputStream(uri);
            if (outputStreamOpenOutputStream == null) {
                throw new IOException("Unable to open export target.");
            }
            try {
                ZipOutputStream zipOutputStream = new ZipOutputStream(outputStreamOpenOutputStream);
                try {
                    String strSanitizeImportedFileName = sanitizeImportedFileName(file.getName(), null);
                    if (isBlank(strSanitizeImportedFileName)) {
                        strSanitizeImportedFileName = "World";
                    }
                    for (int i = 0; i < arrayList.size(); i++) {
                        File file2 = arrayList.get(i);
                        String relativePathFromDirectory = getRelativePathFromDirectory(file, file2);
                        if (!isBlank(relativePathFromDirectory)) {
                            ZipEntry zipEntry = new ZipEntry(strSanitizeImportedFileName + "/" + relativePathFromDirectory);
                            zipEntry.setTime(file2.lastModified());
                            zipOutputStream.putNextEntry(zipEntry);
                            InputStream fileInputStream = new FileInputStream(file2);
                            try {
                                copyStream(fileInputStream, zipOutputStream);
                                fileInputStream.close();
                                zipOutputStream.closeEntry();
                                final int i2 = i + 1;
                                final int size = arrayList.size();
                                if (i2 == size || i2 % 8 == 0) {
                                    runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda61
                                        @Override // java.lang.Runnable
                                        public final void run() {
                                            InstanceDetailsActivity.this.lambda$exportWorldToUri$95(i2, size);
                                        }
                                    });
                                }
                            } catch (Throwable th) {
                                try {
                                    fileInputStream.close();
                                } catch (Throwable th2) {
                                    th.addSuppressed(th2);
                                }
                                throw th;
                            }
                        }
                    }
                    zipOutputStream.close();
                    if (outputStreamOpenOutputStream != null) {
                        outputStreamOpenOutputStream.close();
                    }
                    runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda72
                        @Override // java.lang.Runnable
                        public final void run() {
                            InstanceDetailsActivity.this.lambda$exportWorldToUri$96(file);
                        }
                    });
                } finally {
                }
            } finally {
            }
        } catch (Throwable th3) {
            Logging.e(TAG, "Unable to export world " + file.getAbsolutePath(), th3);
            runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda83
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$exportWorldToUri$97(th3);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$exportWorldToUri$95(int i, int i2) {
        setUpdateProgressMessage("Exporting " + i + " of " + i2 + " files...");
        lambda$checkUpdatesForSelectedCategory$76(i, i2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$exportWorldToUri$96(File file) {
        dismissUpdateProgressDialog();
        Toast.makeText(this, "World exported: " + file.getName(), 1).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$exportWorldToUri$97(Throwable th) {
        dismissUpdateProgressDialog();
        Toast.makeText(this, "World export failed: " + readableError(th), 1).show();
    }

    private void collectFilesForWorldExport(File file, ArrayList<File> arrayList) {
        if (file.isHidden()) {
            return;
        }
        if (file.isFile()) {
            arrayList.add(file);
            return;
        }
        File[] fileArrListFiles = file.listFiles();
        if (fileArrListFiles == null) {
            return;
        }
        ArrayList arrayList2 = new ArrayList();
        Collections.addAll(arrayList2, fileArrListFiles);
        arrayList2.sort(Comparator.comparing(new Function() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda56
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ((File) obj).getName();
            }
        }, String.CASE_INSENSITIVE_ORDER));
        Iterator it = arrayList2.iterator();
        while (it.hasNext()) {
            collectFilesForWorldExport((File) it.next(), arrayList);
        }
    }

    private String getRelativePathFromDirectory(File file, File file2) throws IOException {
        String canonicalPath = file.getCanonicalPath();
        String canonicalPath2 = file2.getCanonicalPath();
        if (canonicalPath2.equals(canonicalPath) || canonicalPath2.startsWith(canonicalPath + File.separator)) {
            return canonicalPath2.equals(canonicalPath) ? "" : canonicalPath2.substring(canonicalPath.length() + 1).replace(File.separatorChar, '/');
        }
        throw new SecurityException("Blocked unsafe export path: " + file2.getAbsolutePath());
    }

    private String normalizeZipPath(String str) {
        return str.replace('\\', '/');
    }

    private String lastFolderName(String str) {
        String strNormalizeZipPath = normalizeZipPath(str);
        while (strNormalizeZipPath.endsWith("/")) {
            strNormalizeZipPath = strNormalizeZipPath.substring(0, strNormalizeZipPath.length() - 1);
        }
        int iLastIndexOf = strNormalizeZipPath.lastIndexOf(47);
        return iLastIndexOf >= 0 ? strNormalizeZipPath.substring(iLastIndexOf + 1) : strNormalizeZipPath;
    }

    private File uniqueTargetDirectory(File file, String str) {
        String strSanitizeImportedFileName = sanitizeImportedFileName(str, null);
        if (isBlank(strSanitizeImportedFileName)) {
            strSanitizeImportedFileName = "Imported World";
        }
        File file2 = new File(file, strSanitizeImportedFileName);
        if (!file2.exists()) {
            return file2;
        }
        for (int i = 2; i < 1000; i++) {
            File file3 = new File(file, strSanitizeImportedFileName + "-" + i);
            if (!file3.exists()) {
                return file3;
            }
        }
        return new File(file, strSanitizeImportedFileName + "-" + System.currentTimeMillis());
    }

    private String resolveDisplayName(Uri uri) {
        int columnIndex;
        String string;
        String lastPathSegment = uri.getLastPathSegment();
        if (lastPathSegment == null || lastPathSegment.trim().isEmpty()) {
            lastPathSegment = "file";
        }
        try {
            Cursor cursorQuery = getContentResolver().query(uri, new String[]{"_display_name"}, null, null, null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.moveToFirst() && (columnIndex = cursorQuery.getColumnIndex("_display_name")) >= 0 && (string = cursorQuery.getString(columnIndex)) != null) {
                        if (!string.trim().isEmpty()) {
                            if (cursorQuery != null) {
                                cursorQuery.close();
                            }
                            return string;
                        }
                    }
                } finally {
                }
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
        } catch (Throwable unused) {
        }
        return lastPathSegment;
    }

    private String sanitizeInstanceName(String str) {
        String strReplaceAll = str.trim().replace('\n', ' ').replace('\r', ' ').replaceAll("[\\\\/:*?\"<>|]", "_");
        while (strReplaceAll.contains("  ")) {
            strReplaceAll = strReplaceAll.replace("  ", " ");
        }
        return (".".equals(strReplaceAll) || "..".equals(strReplaceAll)) ? "" : strReplaceAll;
    }

    private String sanitizeImportedFileName(String str, String str2) {
        String strReplaceAll = str.trim().replace('\n', ' ').replace('\r', ' ').replaceAll("[\\\\/:*?\"<>|]", "_");
        while (strReplaceAll.contains("  ")) {
            strReplaceAll = strReplaceAll.replace("  ", " ");
        }
        if (strReplaceAll.isEmpty() || ".".equals(strReplaceAll) || "..".equals(strReplaceAll)) {
            strReplaceAll = "file";
        }
        return (isBlank(str2) || strReplaceAll.toLowerCase(Locale.US).endsWith(str2.toLowerCase(Locale.US))) ? strReplaceAll : strReplaceAll + str2;
    }

    private File uniqueTargetFile(File file, String str) {
        String strSubstring;
        File file2 = new File(file, str);
        if (!file2.exists()) {
            return file2;
        }
        int iLastIndexOf = str.lastIndexOf(46);
        if (iLastIndexOf <= 0) {
            strSubstring = "";
        } else {
            String strSubstring2 = str.substring(0, iLastIndexOf);
            strSubstring = str.substring(iLastIndexOf);
            str = strSubstring2;
        }
        for (int i = 2; i < 1000; i++) {
            File file3 = new File(file, str + "-" + i + strSubstring);
            if (!file3.exists()) {
                return file3;
            }
        }
        return new File(file, str + "-" + System.currentTimeMillis() + strSubstring);
    }

    private void copyUriToFile(Uri uri, File file) throws Exception {
        InputStream inputStreamOpenInputStream = getContentResolver().openInputStream(uri);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            try {
                if (inputStreamOpenInputStream == null) {
                    throw new IllegalStateException("Unable to open selected file.");
                }
                copyStream(inputStreamOpenInputStream, fileOutputStream);
                fileOutputStream.close();
                if (inputStreamOpenInputStream != null) {
                    inputStreamOpenInputStream.close();
                }
            } finally {
            }
        } catch (Throwable th) {
            if (inputStreamOpenInputStream != null) {
                try {
                    inputStreamOpenInputStream.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    private void copyStream(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] bArr = new byte[32768];
        while (true) {
            int i = inputStream.read(bArr);
            if (i == -1) {
                return;
            } else {
                outputStream.write(bArr, 0, i);
            }
        }
    }

    private String resolveDisplayTitle(File file, ResourceCategory resourceCategory) {
        if (resourceCategory == ResourceCategory.WORLDS) {
            return file.getName();
        }
        if (resourceCategory == ResourceCategory.MODS && file.isFile()) {
            return stripExtension(file.getName());
        }
        return stripExtension(file.getName());
    }

    private String readModName(File file) {
        String displayName = ModJarMetadataExtractor.readDisplayName(file);
        if (!isBlank(displayName)) {
            return displayName;
        }
        try {
            ZipFile zipFile = new ZipFile(file);
            try {
                String strExtractJsonString = extractJsonString(readZipEntryText(zipFile, "fabric.mod.json"), "name");
                if (!isBlank(strExtractJsonString)) {
                    zipFile.close();
                    return strExtractJsonString;
                }
                String strExtractJsonString2 = extractJsonString(readZipEntryText(zipFile, "quilt.mod.json"), "name");
                if (!isBlank(strExtractJsonString2)) {
                    zipFile.close();
                    return strExtractJsonString2;
                }
                String strExtractTomlString = extractTomlString(readZipEntryText(zipFile, "META-INF/mods.toml"), "displayName");
                if (!isBlank(strExtractTomlString)) {
                    zipFile.close();
                    return strExtractTomlString;
                }
                String strExtractJsonString3 = extractJsonString(readZipEntryText(zipFile, "mcmod.info"), "name");
                if (isBlank(strExtractJsonString3)) {
                    zipFile.close();
                    return null;
                }
                zipFile.close();
                return strExtractJsonString3;
            } finally {
            }
        } catch (Throwable unused) {
            return null;
        }
        return null;
    }

    private String readZipEntryText(ZipFile zipFile, String str) throws IOException {
        ZipEntry entry = zipFile.getEntry(str);
        if (entry == null || entry.isDirectory() || entry.getSize() > 1048576) {
            return null;
        }
        InputStream inputStream = zipFile.getInputStream(entry);
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try {
                copyStream(inputStream, byteArrayOutputStream);
                String string = byteArrayOutputStream.toString("UTF-8");
                byteArrayOutputStream.close();
                if (inputStream != null) {
                    inputStream.close();
                }
                return string;
            } finally {
            }
        } catch (Throwable th) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    private String extractJsonString(String str, String str2) {
        if (str == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(str2) + "\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(str);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String extractTomlString(String str, String str2) {
        if (str == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("(?m)^\\s*" + Pattern.quote(str2) + "\\s*=\\s*(?:\"([^\"]+)\"|'([^']+)')").matcher(str);
        if (!matcher.find()) {
            return null;
        }
        String strGroup = matcher.group(1);
        return !isBlank(strGroup) ? strGroup : matcher.group(2);
    }

    private Bitmap loadIconForItem(InstanceContentItem instanceContentItem) {
        Bitmap bitmapLoadModIcon;
        Bitmap icon;
        File file = instanceContentItem.file;
        ResourceCategory resourceCategory = instanceContentItem.category;
        if (resourceCategory == ResourceCategory.WORLDS) {
            File file2 = new File(file, "icon.png");
            if (file2.isFile()) {
                return BitmapFactory.decodeFile(file2.getAbsolutePath());
            }
            return null;
        }
        if (file.isDirectory()) {
            File file3 = new File(file, "pack.png");
            if (file3.isFile()) {
                return BitmapFactory.decodeFile(file3.getAbsolutePath());
            }
            File file4 = new File(file, "icon.png");
            return file4.isFile() ? BitmapFactory.decodeFile(file4.getAbsolutePath()) : loadManifestIconForItem(instanceContentItem);
        }
        if (resourceCategory == ResourceCategory.MODS && (icon = ModJarMetadataExtractor.readIcon(file)) != null) {
            return icon;
        }
        try {
            ZipFile zipFile = new ZipFile(file);
            try {
                if (resourceCategory == ResourceCategory.MODS && (bitmapLoadModIcon = loadModIcon(zipFile)) != null) {
                    zipFile.close();
                    return bitmapLoadModIcon;
                }
                Bitmap bitmapDecodeZipBitmap = decodeZipBitmap(zipFile, "pack.png");
                if (bitmapDecodeZipBitmap != null) {
                    zipFile.close();
                    return bitmapDecodeZipBitmap;
                }
                Bitmap bitmapDecodeFirstLikelyIcon = decodeFirstLikelyIcon(zipFile);
                if (bitmapDecodeFirstLikelyIcon != null) {
                    zipFile.close();
                    return bitmapDecodeFirstLikelyIcon;
                }
                Bitmap bitmapLoadManifestIconForItem = loadManifestIconForItem(instanceContentItem);
                zipFile.close();
                return bitmapLoadManifestIconForItem;
            } finally {
            }
        } catch (Throwable unused) {
            return loadManifestIconForItem(instanceContentItem);
        }
        return loadManifestIconForItem(instanceContentItem);
    }

    private Bitmap loadManifestIconForItem(InstanceContentItem instanceContentItem) {
        File file;
        Bitmap bitmapDecodeFile;
        ModManagerContentType modManagerContentType = toModManagerContentType(instanceContentItem.category);
        if (modManagerContentType == null || (file = this.gameDirectory) == null) {
            return null;
        }
        File installedIconFileForFile = ModManagerManifest.getInstalledIconFileForFile(file, modManagerContentType, instanceContentItem.file);
        if (installedIconFileForFile != null && installedIconFileForFile.isFile() && (bitmapDecodeFile = BitmapFactory.decodeFile(installedIconFileForFile.getAbsolutePath())) != null) {
            return bitmapDecodeFile;
        }
        JSONObject installedEntryForItem = getInstalledEntryForItem(instanceContentItem);
        if (installedEntryForItem == null) {
            return null;
        }
        Bitmap bitmapLoadIconFromInstalledMetadata = loadIconFromInstalledMetadata(instanceContentItem, modManagerContentType, installedEntryForItem);
        return bitmapLoadIconFromInstalledMetadata != null ? bitmapLoadIconFromInstalledMetadata : loadPlatformProjectIconForEntry(instanceContentItem, modManagerContentType, installedEntryForItem);
    }

    private Bitmap loadIconFromInstalledMetadata(InstanceContentItem instanceContentItem, ModManagerContentType modManagerContentType, JSONObject jSONObject) {
        Bitmap bitmapDecodeFile;
        File fileResolveLocalIconFileFromEntry = resolveLocalIconFileFromEntry(jSONObject);
        if (fileResolveLocalIconFileFromEntry != null && fileResolveLocalIconFileFromEntry.isFile() && (bitmapDecodeFile = BitmapFactory.decodeFile(fileResolveLocalIconFileFromEntry.getAbsolutePath())) != null) {
            return bitmapDecodeFile;
        }
        String strResolveIconUrlFromEntry = resolveIconUrlFromEntry(jSONObject);
        if (isBlank(strResolveIconUrlFromEntry)) {
            return null;
        }
        File installedContentIconCacheFile = getInstalledContentIconCacheFile(instanceContentItem, modManagerContentType, jSONObject);
        Bitmap bitmapDecodeFile2 = installedContentIconCacheFile.isFile() ? BitmapFactory.decodeFile(installedContentIconCacheFile.getAbsolutePath()) : null;
        return bitmapDecodeFile2 != null ? bitmapDecodeFile2 : downloadAndCacheBitmap(strResolveIconUrlFromEntry, installedContentIconCacheFile, null);
    }

    private Bitmap loadPlatformProjectIconForEntry(InstanceContentItem instanceContentItem, ModManagerContentType modManagerContentType, JSONObject jSONObject) {
        String strFetchCurseForgeProjectIconUrl;
        ModManagerSource source = ModManagerManifest.getSource(jSONObject);
        String projectIdFromEntry = getProjectIdFromEntry(jSONObject);
        if (isBlank(projectIdFromEntry)) {
            return null;
        }
        File installedContentIconCacheFile = getInstalledContentIconCacheFile(instanceContentItem, modManagerContentType, jSONObject);
        Bitmap bitmapDecodeFile = installedContentIconCacheFile.isFile() ? BitmapFactory.decodeFile(installedContentIconCacheFile.getAbsolutePath()) : null;
        if (bitmapDecodeFile != null) {
            return bitmapDecodeFile;
        }
        if (source == ModManagerSource.MODRINTH) {
            strFetchCurseForgeProjectIconUrl = fetchModrinthProjectIconUrl(projectIdFromEntry);
        } else if (source != ModManagerSource.CURSEFORGE) {
            strFetchCurseForgeProjectIconUrl = "";
        } else {
            strFetchCurseForgeProjectIconUrl = fetchCurseForgeProjectIconUrl(projectIdFromEntry);
        }
        if (isBlank(strFetchCurseForgeProjectIconUrl)) {
            return null;
        }
        return downloadAndCacheBitmap(strFetchCurseForgeProjectIconUrl, installedContentIconCacheFile, null);
    }

    private File resolveLocalIconFileFromEntry(JSONObject jSONObject) {
        String strFirstNonBlank = firstNonBlank(jSONObject.optString("cachedIconPath", ""), jSONObject.optString("iconCachePath", ""), jSONObject.optString("installedIconPath", ""), jSONObject.optString("localIconPath", ""), jSONObject.optString("iconPath", ""), jSONObject.optString("iconFile", ""));
        if (!isBlank(strFirstNonBlank) && !strFirstNonBlank.startsWith("http://") && !strFirstNonBlank.startsWith("https://") && !strFirstNonBlank.startsWith("//")) {
            File file = new File(strFirstNonBlank);
            if (file.isFile()) {
                return file;
            }
            if (this.gameDirectory != null) {
                File file2 = new File(this.gameDirectory, strFirstNonBlank.replace('/', File.separatorChar));
                if (file2.isFile()) {
                    return file2;
                }
            }
        }
        return null;
    }

    private String resolveIconUrlFromEntry(JSONObject jSONObject) {
        String strFirstNonBlank = firstNonBlank(jSONObject.optString("iconUrl", ""), jSONObject.optString("iconURL", ""), jSONObject.optString("icon_url", ""), jSONObject.optString("projectIconUrl", ""), jSONObject.optString("projectIconURL", ""), jSONObject.optString("project_icon_url", ""), jSONObject.optString("thumbnailUrl", ""), jSONObject.optString("thumbnailURL", ""), jSONObject.optString("thumbnail_url", ""), jSONObject.optString("logoUrl", ""), jSONObject.optString("logoURL", ""), jSONObject.optString("logo_url", ""), jSONObject.optString("imageUrl", ""), jSONObject.optString("imageURL", ""), jSONObject.optString("image_url", ""));
        return !isBlank(strFirstNonBlank) ? normalizeIconUrl(strFirstNonBlank) : normalizeIconUrl(firstNonBlank(resolveIconUrlFromObject(jSONObject.optJSONObject("project")), resolveIconUrlFromObject(jSONObject.optJSONObject("modrinthProject")), resolveIconUrlFromObject(jSONObject.optJSONObject("curseForgeProject")), resolveIconUrlFromObject(jSONObject.optJSONObject("curseforgeProject")), resolveIconUrlFromObject(jSONObject.optJSONObject("data"))));
    }

    private String resolveIconUrlFromObject(JSONObject jSONObject) {
        if (jSONObject == null) {
            return "";
        }
        String strFirstNonBlank = firstNonBlank(jSONObject.optString("iconUrl", ""), jSONObject.optString("iconURL", ""), jSONObject.optString("icon_url", ""), jSONObject.optString("thumbnailUrl", ""), jSONObject.optString("thumbnailURL", ""), jSONObject.optString("thumbnail_url", ""), jSONObject.optString("logoUrl", ""), jSONObject.optString("logoURL", ""), jSONObject.optString("logo_url", ""), jSONObject.optString("imageUrl", ""), jSONObject.optString("imageURL", ""), jSONObject.optString("image_url", ""));
        if (!isBlank(strFirstNonBlank)) {
            return strFirstNonBlank;
        }
        JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject("logo");
        if (jSONObjectOptJSONObject != null) {
            strFirstNonBlank = firstNonBlank(jSONObjectOptJSONObject.optString("thumbnailUrl", ""), jSONObjectOptJSONObject.optString("thumbnailURL", ""), jSONObjectOptJSONObject.optString("url", ""));
            if (!isBlank(strFirstNonBlank)) {
                return strFirstNonBlank;
            }
        }
        JSONObject jSONObjectOptJSONObject2 = jSONObject.optJSONObject("icon");
        return jSONObjectOptJSONObject2 != null ? firstNonBlank(jSONObjectOptJSONObject2.optString("thumbnailUrl", ""), jSONObjectOptJSONObject2.optString("thumbnailURL", ""), jSONObjectOptJSONObject2.optString("url", "")) : strFirstNonBlank;
    }

    private String fetchModrinthProjectIconUrl(String str) {
        try {
            return normalizeIconUrl(new JSONObject(readNetworkText("https://api.modrinth.com/v2/project/" + Uri.encode(str), null)).optString("icon_url", ""));
        } catch (Throwable th) {
            Logging.i(TAG, "Unable to resolve Modrinth project icon for " + str + ": " + readableError(th));
            return "";
        }
    }

    private String fetchCurseForgeProjectIconUrl(String str) {
        JSONObject jSONObjectOptJSONObject;
        JSONObject jSONObjectOptJSONObject2;
        try {
            String strResolve = CurseForgeApiKeyProvider.resolve();
            return (isBlank(strResolve) || (jSONObjectOptJSONObject = new JSONObject(readNetworkText(new StringBuilder("https://api.curseforge.com/v1/mods/").append(Uri.encode(str)).toString(), strResolve)).optJSONObject("data")) == null || (jSONObjectOptJSONObject2 = jSONObjectOptJSONObject.optJSONObject("logo")) == null) ? "" : normalizeIconUrl(firstNonBlank(jSONObjectOptJSONObject2.optString("thumbnailUrl", ""), jSONObjectOptJSONObject2.optString("url", "")));
        } catch (Throwable th) {
            Logging.i(TAG, "Unable to resolve CurseForge project icon for " + str + ": " + readableError(th));
            return "";
        }
    }

    private String readNetworkText(String str, String str2) throws IOException {
        InputStream errorStream;
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(str).openConnection();
        httpURLConnection.setConnectTimeout(12000);
        httpURLConnection.setReadTimeout(12000);
        httpURLConnection.setRequestProperty("User-Agent", "JavaLauncher");
        if (!isBlank(str2)) {
            httpURLConnection.setRequestProperty("x-api-key", str2.trim());
        }
        int responseCode = httpURLConnection.getResponseCode();
        if (responseCode >= 200 && responseCode < 300) {
            errorStream = httpURLConnection.getInputStream();
        } else {
            errorStream = httpURLConnection.getErrorStream();
        }
        if (errorStream == null) {
            httpURLConnection.disconnect();
            throw new IOException("HTTP " + responseCode);
        }
        try {
            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                try {
                    copyStream(errorStream, byteArrayOutputStream);
                    if (responseCode < 200 || responseCode >= 300) {
                        throw new IOException("HTTP " + responseCode + ": " + byteArrayOutputStream.toString("UTF-8"));
                    }
                    String string = byteArrayOutputStream.toString("UTF-8");
                    byteArrayOutputStream.close();
                    if (errorStream != null) {
                        errorStream.close();
                    }
                    return string;
                } finally {
                }
            } finally {
            }
        } finally {
            httpURLConnection.disconnect();
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:24:0x006d, code lost:
    
        if (r1 == null) goto L26;
     */
    /* JADX WARN: Code restructure failed: missing block: B:25:0x006f, code lost:
    
        r1.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:26:0x0072, code lost:
    
        r11 = r11.toByteArray();
        r1 = android.graphics.BitmapFactory.decodeByteArray(r11, 0, r11.length);
     */
    /* JADX WARN: Code restructure failed: missing block: B:27:0x007b, code lost:
    
        if (r1 != null) goto L31;
     */
    /* JADX WARN: Code restructure failed: missing block: B:30:0x0082, code lost:
    
        return null;
     */
    /* JADX WARN: Code restructure failed: missing block: B:31:0x0083, code lost:
    
        r2 = r10.getParentFile();
     */
    /* JADX WARN: Code restructure failed: missing block: B:32:0x0087, code lost:
    
        if (r2 == null) goto L76;
     */
    /* JADX WARN: Code restructure failed: missing block: B:34:0x008d, code lost:
    
        if (r2.exists() != false) goto L76;
     */
    /* JADX WARN: Code restructure failed: missing block: B:35:0x008f, code lost:
    
        r2.mkdirs();
     */
    /* JADX WARN: Code restructure failed: missing block: B:36:0x0092, code lost:
    
        r2 = new java.io.FileOutputStream(r10);
     */
    /* JADX WARN: Code restructure failed: missing block: B:37:0x0097, code lost:
    
        r2.write(r11);
     */
    /* JADX WARN: Code restructure failed: missing block: B:38:0x009a, code lost:
    
        r2.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:40:0x009e, code lost:
    
        r10 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:45:0x00a7, code lost:
    
        throw r10;
     */
    /* JADX WARN: Code restructure failed: missing block: B:46:0x00a8, code lost:
    
        r10 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:47:0x00a9, code lost:
    
        ca.dnamobile.javalauncher.feature.log.Logging.i(ca.dnamobile.javalauncher.InstanceDetailsActivity.TAG, "Unable to cache installed content icon: " + r10.getMessage());
     */
    /* JADX WARN: Code restructure failed: missing block: B:48:0x00c3, code lost:
    
        if (r9 != null) goto L49;
     */
    /* JADX WARN: Code restructure failed: missing block: B:49:0x00c5, code lost:
    
        r9.disconnect();
     */
    /* JADX WARN: Code restructure failed: missing block: B:50:0x00c8, code lost:
    
        return r1;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private android.graphics.Bitmap downloadAndCacheBitmap(java.lang.String r9, java.io.File r10, java.lang.String r11) {
        /*
            Method dump skipped, instruction units count: 275
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: ca.dnamobile.javalauncher.InstanceDetailsActivity.downloadAndCacheBitmap(java.lang.String, java.io.File, java.lang.String):android.graphics.Bitmap");
    }

    private File getInstalledContentIconCacheFile(InstanceContentItem instanceContentItem, ModManagerContentType modManagerContentType, JSONObject jSONObject) {
        return new File(new File(new File(this.gameDirectory, ".javalauncher"), "content_icons"), sanitizeCacheFileName(modManagerContentType.getIntentValue() + "-" + (firstNonBlank(ModManagerManifest.getSource(jSONObject).getId(), EnvironmentCompat.MEDIA_UNKNOWN) + "-" + firstNonBlank(getProjectIdFromEntry(jSONObject), stripDisabledSuffix(instanceContentItem.file.getName())))) + ".png");
    }

    private String normalizeIconUrl(String str) {
        if (str == null) {
            return "";
        }
        String strTrim = str.trim();
        if (strTrim.isEmpty() || "null".equalsIgnoreCase(strTrim)) {
            return "";
        }
        if (strTrim.startsWith("//")) {
            strTrim = "https:" + strTrim;
        }
        return (strTrim.startsWith("http://") || strTrim.startsWith("https://")) ? strTrim : "";
    }

    private String firstNonBlank(String... strArr) {
        if (strArr == null) {
            return "";
        }
        for (String str : strArr) {
            if (str != null && !str.trim().isEmpty() && !"null".equalsIgnoreCase(str.trim())) {
                return str.trim();
            }
        }
        return "";
    }

    private String sanitizeCacheFileName(String str) {
        String strReplaceAll = str.trim().replaceAll("[^A-Za-z0-9._-]", "_");
        if (strReplaceAll.isEmpty()) {
            strReplaceAll = "icon";
        }
        return strReplaceAll.length() > 120 ? strReplaceAll.substring(0, 120) : strReplaceAll;
    }

    private Bitmap loadModIcon(ZipFile zipFile) throws IOException {
        Bitmap bitmapDecodeZipBitmap = decodeZipBitmap(zipFile, extractJsonIconString(readZipEntryText(zipFile, "fabric.mod.json")));
        if (bitmapDecodeZipBitmap != null) {
            return bitmapDecodeZipBitmap;
        }
        Bitmap bitmapDecodeZipBitmap2 = decodeZipBitmap(zipFile, extractJsonIconString(readZipEntryText(zipFile, "quilt.mod.json")));
        if (bitmapDecodeZipBitmap2 != null) {
            return bitmapDecodeZipBitmap2;
        }
        Bitmap bitmapDecodeZipBitmap3 = decodeZipBitmap(zipFile, extractTomlString(readZipEntryText(zipFile, "META-INF/mods.toml"), "logoFile"));
        if (bitmapDecodeZipBitmap3 != null) {
            return bitmapDecodeZipBitmap3;
        }
        Bitmap bitmapDecodeZipBitmap4 = decodeZipBitmap(zipFile, extractTomlString(readZipEntryText(zipFile, "META-INF/neoforge.mods.toml"), "logoFile"));
        if (bitmapDecodeZipBitmap4 != null) {
            return bitmapDecodeZipBitmap4;
        }
        return null;
    }

    private String extractJsonIconString(String str) {
        String strExtractJsonString = extractJsonString(str, "icon");
        if (!isBlank(strExtractJsonString)) {
            return strExtractJsonString;
        }
        if (str == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("\\\"icon\\\"\\s*:\\s*\\{([^}]+)\\}", 32).matcher(str);
        if (!matcher.find()) {
            return null;
        }
        Matcher matcher2 = Pattern.compile("\\\"[^\\\"]+\\\"\\s*:\\s*\\\"([^\\\"]+\\.(?:png|jpg|jpeg|webp))\\\"").matcher(matcher.group(1));
        if (matcher2.find()) {
            return matcher2.group(1);
        }
        return null;
    }

    private Bitmap decodeFirstLikelyIcon(ZipFile zipFile) throws IOException {
        int i;
        Enumeration<? extends ZipEntry> enumerationEntries = zipFile.entries();
        while (enumerationEntries.hasMoreElements()) {
            ZipEntry zipEntryNextElement = enumerationEntries.nextElement();
            if (!zipEntryNextElement.isDirectory()) {
                String lowerCase = normalizeZipPath(zipEntryNextElement.getName()).toLowerCase(Locale.US);
                int iLastIndexOf = lowerCase.lastIndexOf(47);
                String strSubstring = (iLastIndexOf < 0 || (i = iLastIndexOf + 1) >= lowerCase.length()) ? lowerCase : lowerCase.substring(i);
                if (isLikelyIconPath(lowerCase) || isLikelyIconFileName(strSubstring)) {
                    InputStream inputStream = zipFile.getInputStream(zipEntryNextElement);
                    try {
                        Bitmap bitmapDecodeStream = BitmapFactory.decodeStream(inputStream);
                        if (bitmapDecodeStream != null) {
                            if (inputStream != null) {
                                inputStream.close();
                            }
                            return bitmapDecodeStream;
                        }
                        if (inputStream != null) {
                            inputStream.close();
                        }
                    } catch (Throwable th) {
                        if (inputStream != null) {
                            try {
                                inputStream.close();
                            } catch (Throwable th2) {
                                th.addSuppressed(th2);
                            }
                        }
                        throw th;
                    }
                }
            }
        }
        return null;
    }

    private boolean isLikelyIconPath(String str) {
        return str.endsWith("/icon.png") || str.endsWith("/logo.png") || str.endsWith("/icon.jpg") || str.endsWith("/icon.jpeg") || str.endsWith("/icon.webp") || str.endsWith("/logo.jpg") || str.endsWith("/logo.jpeg") || str.endsWith("/logo.webp") || str.endsWith("/mod_icon.png") || str.endsWith("/modicon.png");
    }

    private boolean isLikelyIconFileName(String str) {
        return "icon.png".equals(str) || "logo.png".equals(str) || "pack.png".equals(str) || "icon.jpg".equals(str) || "icon.jpeg".equals(str) || "icon.webp".equals(str) || "logo.jpg".equals(str) || "logo.jpeg".equals(str) || "logo.webp".equals(str) || "mod_icon.png".equals(str) || "modicon.png".equals(str);
    }

    private Bitmap decodeZipBitmap(ZipFile zipFile, String str) throws IOException {
        if (isBlank(str)) {
            return null;
        }
        String strNormalizeZipPath = normalizeZipPath(str.trim());
        while (strNormalizeZipPath.startsWith("/")) {
            strNormalizeZipPath = strNormalizeZipPath.substring(1);
        }
        ZipEntry entry = zipFile.getEntry(strNormalizeZipPath);
        if (entry == null && strNormalizeZipPath.startsWith("./")) {
            entry = zipFile.getEntry(strNormalizeZipPath.substring(2));
        }
        if (entry == null) {
            entry = findZipEntryBySuffix(zipFile, strNormalizeZipPath);
        }
        if (entry == null || entry.isDirectory()) {
            return null;
        }
        InputStream inputStream = zipFile.getInputStream(entry);
        try {
            Bitmap bitmapDecodeStream = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) {
                inputStream.close();
            }
            return bitmapDecodeStream;
        } catch (Throwable th) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    private ZipEntry findZipEntryBySuffix(ZipFile zipFile, String str) {
        String lowerCase = str.toLowerCase(Locale.US);
        Enumeration<? extends ZipEntry> enumerationEntries = zipFile.entries();
        while (enumerationEntries.hasMoreElements()) {
            ZipEntry zipEntryNextElement = enumerationEntries.nextElement();
            if (!zipEntryNextElement.isDirectory()) {
                String lowerCase2 = normalizeZipPath(zipEntryNextElement.getName()).toLowerCase(Locale.US);
                if (lowerCase2.equals(lowerCase) || lowerCase2.endsWith("/" + lowerCase)) {
                    return zipEntryNextElement;
                }
            }
        }
        return null;
    }

    private String stripExtension(String str) {
        String strStripDisabledSuffix = stripDisabledSuffix(str);
        int iLastIndexOf = strStripDisabledSuffix.lastIndexOf(46);
        return iLastIndexOf > 0 ? strStripDisabledSuffix.substring(0, iLastIndexOf) : strStripDisabledSuffix;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isContentItemEnabled(File file) {
        return !file.getName().toLowerCase(Locale.US).endsWith(".disabled");
    }

    private String stripDisabledSuffix(String str) {
        return str.toLowerCase(Locale.US).endsWith(".disabled") ? str.substring(0, str.length() - ".disabled".length()) : str;
    }

    private String removeDisabledSuffix(String str) {
        return stripDisabledSuffix(str);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String formatSubtitle(InstanceContentItem instanceContentItem) {
        String string;
        if (instanceContentItem.file.isDirectory()) {
            string = getString(R.string.instance_content_folder_subtitle, new Object[]{instanceContentItem.file.getName()});
        } else {
            string = getString(R.string.instance_content_file_subtitle, new Object[]{instanceContentItem.file.getName(), formatFileSize(instanceContentItem.file.length())});
        }
        return (!instanceContentItem.category.supportsDisableToggle || isContentItemEnabled(instanceContentItem.file)) ? string : string + " · " + getString(R.string.instance_content_disabled_label);
    }

    private String formatFileSize(long j) {
        if (j < 1024) {
            return j + " B";
        }
        double d = j / 1024.0d;
        int i = 0;
        String[] strArr = {"KB", "MB", "GB"};
        while (d >= 1024.0d && i < 2) {
            d /= 1024.0d;
            i++;
        }
        return String.format(Locale.US, "%.1f %s", Double.valueOf(d), strArr[i]);
    }

    private ModManagerContentType toModManagerContentType(ResourceCategory resourceCategory) {
        int iOrdinal = resourceCategory.ordinal();
        if (iOrdinal == 0) {
            return ModManagerContentType.MODS;
        }
        if (iOrdinal == 1) {
            return ModManagerContentType.SHADERPACKS;
        }
        if (iOrdinal != 2) {
            return null;
        }
        return ModManagerContentType.RESOURCEPACKS;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public ModManagerSource getInstalledSourceForItem(InstanceContentItem instanceContentItem) {
        File file;
        ModManagerContentType modManagerContentType = toModManagerContentType(instanceContentItem.category);
        if (modManagerContentType == null || (file = this.gameDirectory) == null) {
            return ModManagerSource.UNKNOWN;
        }
        ModManagerSource installedSourceForFile = ModManagerManifest.getInstalledSourceForFile(file, modManagerContentType, instanceContentItem.file);
        if (installedSourceForFile != ModManagerSource.UNKNOWN) {
            return installedSourceForFile;
        }
        JSONObject modpackInstalledEntryForFile = getModpackInstalledEntryForFile(modManagerContentType, instanceContentItem.file);
        return modpackInstalledEntryForFile == null ? ModManagerSource.UNKNOWN : ModManagerManifest.getSource(modpackInstalledEntryForFile);
    }

    private String displayLoader(String str) {
        if (str == null || str.trim().isEmpty()) {
            return "Vanilla";
        }
        return str.substring(0, 1).toUpperCase(Locale.US) + str.substring(1);
    }

    private String displayVersionType(String str) {
        if (str == null || str.trim().isEmpty()) {
            return "Release";
        }
        str.hashCode();
        switch (str) {
            case "old_beta":
                return "Beta";
            case "snapshot":
                return "Snapshot";
            case "release":
                return "Release";
            case "old_alpha":
                return "Alpha";
            default:
                return str.substring(0, 1).toUpperCase(Locale.US) + str.substring(1).replace('_', ' ');
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int dp(int i) {
        return Math.round(i * getResources().getDisplayMetrics().density);
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public LoadedContentMetadata loadContentMetadataForItem(InstanceContentItem instanceContentItem) {
        String str;
        Bitmap bitmapLoadModIcon = null;
        if (instanceContentItem.category == ResourceCategory.MODS && instanceContentItem.file.isFile()) {
            ModJarMetadataExtractor.Result result = ModJarMetadataExtractor.read(instanceContentItem.file);
            if (result != null) {
                String displayName = result.getDisplayName();
                bitmapLoadModIcon = result.getIcon();
                str = displayName;
            } else {
                str = null;
            }
            if (bitmapLoadModIcon == null) {
                try {
                    ZipFile zipFile = new ZipFile(instanceContentItem.file);
                    try {
                        bitmapLoadModIcon = loadModIcon(zipFile);
                        if (bitmapLoadModIcon == null) {
                            bitmapLoadModIcon = decodeZipBitmap(zipFile, "pack.png");
                        }
                        if (bitmapLoadModIcon == null) {
                            bitmapLoadModIcon = decodeFirstLikelyIcon(zipFile);
                        }
                        zipFile.close();
                    } finally {
                    }
                } catch (Throwable unused) {
                }
            }
            if (bitmapLoadModIcon == null) {
                bitmapLoadModIcon = loadManifestIconForItem(instanceContentItem);
            }
            return new LoadedContentMetadata(str, bitmapLoadModIcon);
        }
        return new LoadedContentMetadata(null, loadIconForItem(instanceContentItem));
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class LoadedContentMetadata {
        final String displayName;
        final Bitmap icon;

        LoadedContentMetadata(String str, Bitmap bitmap) {
            this.displayName = str;
            this.icon = bitmap;
        }

        boolean hasAny() {
            String str = this.displayName;
            return ((str == null || str.trim().isEmpty()) && this.icon == null) ? false : true;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    enum ResourceCategory {
        MODS(R.string.instance_tab_mods, R.string.button_upload_mods, R.string.button_browse_mods, R.string.mods_picker_title, R.string.instance_content_mods_plural, R.drawable.ic_instance_mod_24, "*/*", new String[]{"application/java-archive", "application/x-java-archive", "application/zip", "application/octet-stream"}, ".jar", true, true, true),
        SHADERPACKS(R.string.instance_tab_shaderpacks, R.string.button_upload_shaderpacks, R.string.button_browse_shaderpacks, R.string.shaderpacks_picker_title, R.string.instance_content_shaderpacks_plural, R.drawable.ic_instance_shaderpack_24, "*/*", new String[]{"application/zip", "application/octet-stream"}, ".zip", true, true, true),
        RESOURCEPACKS(R.string.instance_tab_resourcepacks, R.string.button_upload_resourcepacks, R.string.button_browse_resourcepacks, R.string.resourcepacks_picker_title, R.string.instance_content_resourcepacks_plural, R.drawable.ic_instance_resourcepack_24, "*/*", new String[]{"application/zip", "application/octet-stream"}, ".zip", true, true, true),
        WORLDS(R.string.instance_tab_worlds, R.string.button_upload_worlds, 0, R.string.worlds_picker_title, R.string.instance_content_worlds_plural, R.drawable.ic_instance_world_24, "*/*", new String[]{"application/zip", "application/octet-stream"}, ".zip", false, false, false);

        final int browseButtonTextRes;
        final String defaultExtension;
        final int defaultIconRes;
        final String mimeType;
        final String[] mimeTypes;
        final int pickerTitleRes;
        final int pluralLabelRes;
        final boolean supportsBrowse;
        final boolean supportsDisableToggle;
        final boolean supportsUpdatePlaceholder;
        final int tabTitleRes;
        final int uploadButtonTextRes;

        ResourceCategory(int i, int i2, int i3, int i4, int i5, int i6, String str, String[] strArr, String str2, boolean z, boolean z2, boolean z3) {
            this.tabTitleRes = i;
            this.uploadButtonTextRes = i2;
            this.browseButtonTextRes = i3;
            this.pickerTitleRes = i4;
            this.pluralLabelRes = i5;
            this.defaultIconRes = i6;
            this.mimeType = str;
            this.mimeTypes = strArr;
            this.defaultExtension = str2;
            this.supportsUpdatePlaceholder = z;
            this.supportsBrowse = z2;
            this.supportsDisableToggle = z3;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class InstanceContentItem {
        final ResourceCategory category;
        final File file;
        final String title;

        InstanceContentItem(File file, ResourceCategory resourceCategory, String str) {
            this.file = file;
            this.category = resourceCategory;
            this.title = str;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    final class InstanceContentAdapter extends RecyclerView.Adapter<ContentViewHolder> {
        private final Map<String, LoadedContentMetadata> metadataCache = new ConcurrentHashMap();
        private final Set<String> missingMetadataCache = Collections.newSetFromMap(new ConcurrentHashMap());
        private final Set<String> loadingMetadata = Collections.newSetFromMap(new ConcurrentHashMap());
        private final Map<String, ModManagerSource> sourceCache = new ConcurrentHashMap();
        private final Set<String> loadingSource = Collections.newSetFromMap(new ConcurrentHashMap());

        InstanceContentAdapter() {
            setHasStableIds(true);
        }

        void clearTransientCachesForFile(String str) {
            for (String str2 : new ArrayList(this.metadataCache.keySet())) {
                if (str2.startsWith(str + ":")) {
                    this.metadataCache.remove(str2);
                }
            }
            for (String str3 : new ArrayList(this.sourceCache.keySet())) {
                if (str3.startsWith(str + ":")) {
                    this.sourceCache.remove(str3);
                }
            }
            for (String str4 : new ArrayList(this.missingMetadataCache)) {
                if (str4.startsWith(str + ":")) {
                    this.missingMetadataCache.remove(str4);
                }
            }
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public ContentViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            return new ContentViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_instance_resource, viewGroup, false));
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public void onBindViewHolder(ContentViewHolder contentViewHolder, int i) {
            final InstanceContentItem instanceContentItem = (InstanceContentItem) InstanceDetailsActivity.this.contentItems.get(i);
            String strBuildMetadataKey = buildMetadataKey(instanceContentItem);
            contentViewHolder.itemView.setTag(strBuildMetadataKey);
            contentViewHolder.title.setText(instanceContentItem.title);
            contentViewHolder.subtitle.setText(InstanceDetailsActivity.this.formatSubtitle(instanceContentItem));
            applyFallbackIcon(contentViewHolder, instanceContentItem);
            LoadedContentMetadata loadedContentMetadata = this.metadataCache.get(strBuildMetadataKey);
            if (loadedContentMetadata != null) {
                applyLoadedMetadata(contentViewHolder, instanceContentItem, strBuildMetadataKey, loadedContentMetadata);
            } else if (!this.missingMetadataCache.contains(strBuildMetadataKey)) {
                loadMetadataAsync(contentViewHolder, instanceContentItem, strBuildMetadataKey);
            }
            ModManagerSource modManagerSource = this.sourceCache.get(strBuildMetadataKey);
            if (modManagerSource == null) {
                contentViewHolder.sourceIcon.setVisibility(8);
                loadSourceAsync(contentViewHolder, instanceContentItem, strBuildMetadataKey);
            } else {
                contentViewHolder.sourceIcon.setVisibility(modManagerSource.hasIcon() ? 0 : 8);
                if (modManagerSource.hasIcon()) {
                    contentViewHolder.sourceIcon.setImageResource(modManagerSource.getIconRes());
                    contentViewHolder.sourceIcon.setContentDescription(InstanceDetailsActivity.this.getString(R.string.modmanager_installed_from, new Object[]{modManagerSource.getDisplayName()}));
                }
            }
            boolean z = instanceContentItem.category == ResourceCategory.WORLDS && instanceContentItem.file.isDirectory();
            contentViewHolder.playWorldButton.setVisibility(z ? 0 : 8);
            contentViewHolder.playWorldButton.setOnClickListener(z ? new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$InstanceContentAdapter$$ExternalSyntheticLambda0
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    InstanceDetailsActivity.this.lambda$onBindViewHolder$0(instanceContentItem, view);
                }
            } : null);
            if (instanceContentItem.category == ResourceCategory.WORLDS && instanceContentItem.file.isDirectory()) {
                bindWorldExportButton(contentViewHolder, instanceContentItem);
            } else if (instanceContentItem.category.supportsUpdatePlaceholder) {
                bindUpdateButton(contentViewHolder, instanceContentItem);
            } else {
                contentViewHolder.updateButton.setVisibility(8);
                contentViewHolder.updateButton.setOnClickListener(null);
            }
            boolean z2 = instanceContentItem.category.supportsDisableToggle && instanceContentItem.file.isFile();
            contentViewHolder.enabledSwitch.setOnCheckedChangeListener(null);
            contentViewHolder.enabledSwitch.setVisibility(z2 ? 0 : 8);
            contentViewHolder.enabledSwitch.setChecked(InstanceDetailsActivity.this.isContentItemEnabled(instanceContentItem.file));
            if (z2) {
                contentViewHolder.enabledSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$InstanceContentAdapter$$ExternalSyntheticLambda3
                    @Override // android.widget.CompoundButton.OnCheckedChangeListener
                    public final void onCheckedChanged(CompoundButton compoundButton, boolean z3) {
                        InstanceDetailsActivity.this.lambda$onBindViewHolder$1(instanceContentItem, compoundButton, z3);
                    }
                });
            }
            contentViewHolder.deleteButton.setVisibility(0);
            contentViewHolder.deleteButton.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$InstanceContentAdapter$$ExternalSyntheticLambda4
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    InstanceDetailsActivity.this.lambda$onBindViewHolder$2(instanceContentItem, view);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onBindViewHolder$0(InstanceContentItem instanceContentItem, View view) {
            InstanceDetailsActivity.this.prepareContentRowAction();
            InstanceDetailsActivity instanceDetailsActivity = InstanceDetailsActivity.this;
            instanceDetailsActivity.lambda$launchInstance$41(instanceDetailsActivity.resolveContentItemForAction(instanceContentItem).file.getName());
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onBindViewHolder$1(InstanceContentItem instanceContentItem, CompoundButton compoundButton, boolean z) {
            InstanceDetailsActivity.this.setContentItemEnabledFromRow(instanceContentItem, z);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onBindViewHolder$2(InstanceContentItem instanceContentItem, View view) {
            InstanceDetailsActivity.this.showDeleteContentItemDialogFromRow(instanceContentItem);
        }

        private void applyFallbackIcon(ContentViewHolder contentViewHolder, InstanceContentItem instanceContentItem) {
            contentViewHolder.icon.setBackgroundResource(R.drawable.bg_instance_icon);
            contentViewHolder.icon.setPadding(InstanceDetailsActivity.this.dp(8), InstanceDetailsActivity.this.dp(8), InstanceDetailsActivity.this.dp(8), InstanceDetailsActivity.this.dp(8));
            contentViewHolder.icon.setScaleType(ImageView.ScaleType.FIT_XY);
            contentViewHolder.icon.setImageResource(instanceContentItem.category.defaultIconRes);
        }

        private void applyLoadedIcon(ContentViewHolder contentViewHolder, Bitmap bitmap) {
            contentViewHolder.icon.setBackgroundResource(R.drawable.bg_instance_icon);
            contentViewHolder.icon.setPadding(InstanceDetailsActivity.this.dp(4), InstanceDetailsActivity.this.dp(4), InstanceDetailsActivity.this.dp(4), InstanceDetailsActivity.this.dp(4));
            contentViewHolder.icon.setScaleType(ImageView.ScaleType.FIT_XY);
            contentViewHolder.icon.setImageBitmap(bitmap);
        }

        private void loadSourceAsync(final ContentViewHolder contentViewHolder, final InstanceContentItem instanceContentItem, final String str) {
            if (this.loadingSource.add(str)) {
                InstanceDetailsActivity.this.iconExecutor.execute(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$InstanceContentAdapter$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        InstanceDetailsActivity.this.lambda$loadSourceAsync$4(instanceContentItem, str, contentViewHolder);
                    }
                });
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$loadSourceAsync$4(InstanceContentItem instanceContentItem, final String str, final ContentViewHolder contentViewHolder) {
            final ModManagerSource installedSourceForItem;
            try {
                installedSourceForItem = InstanceDetailsActivity.this.getInstalledSourceForItem(instanceContentItem);
            } catch (Throwable th) {
                Logging.i(InstanceDetailsActivity.TAG, "Unable to resolve installed source for " + instanceContentItem.file.getName() + ": " + InstanceDetailsActivity.this.readableError(th));
                installedSourceForItem = ModManagerSource.UNKNOWN;
            }
            if (installedSourceForItem == null) {
                installedSourceForItem = ModManagerSource.UNKNOWN;
            }
            InstanceDetailsActivity.this.mainHandler.post(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$InstanceContentAdapter$$ExternalSyntheticLambda10
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$loadSourceAsync$3(str, installedSourceForItem, contentViewHolder);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$loadSourceAsync$3(String str, ModManagerSource modManagerSource, ContentViewHolder contentViewHolder) {
            this.loadingSource.remove(str);
            this.sourceCache.put(str, modManagerSource);
            if (InstanceDetailsActivity.this.binding == null || InstanceDetailsActivity.this.isFinishing() || InstanceDetailsActivity.this.isDestroyed()) {
                return;
            }
            Object tag = contentViewHolder.itemView.getTag();
            if ((tag instanceof String) && str.equals(tag)) {
                contentViewHolder.sourceIcon.setVisibility(modManagerSource.hasIcon() ? 0 : 8);
                if (modManagerSource.hasIcon()) {
                    contentViewHolder.sourceIcon.setImageResource(modManagerSource.getIconRes());
                    contentViewHolder.sourceIcon.setContentDescription(InstanceDetailsActivity.this.getString(R.string.modmanager_installed_from, new Object[]{modManagerSource.getDisplayName()}));
                }
            }
        }

        private void bindWorldExportButton(ContentViewHolder contentViewHolder, final InstanceContentItem instanceContentItem) {
            contentViewHolder.updateButton.setVisibility(0);
            contentViewHolder.updateButton.setText("");
            contentViewHolder.updateButton.setEnabled(true);
            contentViewHolder.updateButton.setIconResource(R.drawable.ic_arrow_upward_24);
            contentViewHolder.updateButton.setContentDescription("Export World");
            contentViewHolder.updateButton.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$InstanceContentAdapter$$ExternalSyntheticLambda1
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    InstanceDetailsActivity.this.lambda$bindWorldExportButton$5(instanceContentItem, view);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$bindWorldExportButton$5(InstanceContentItem instanceContentItem, View view) {
            InstanceDetailsActivity.this.startWorldExportFromRow(instanceContentItem);
        }

        private void bindUpdateButton(ContentViewHolder contentViewHolder, final InstanceContentItem instanceContentItem) {
            contentViewHolder.updateButton.setVisibility(0);
            contentViewHolder.updateButton.setText("");
            contentViewHolder.updateButton.setEnabled(true);
            UpdateState updateStateForItem = InstanceDetailsActivity.this.getUpdateStateForItem(instanceContentItem);
            String updateMessageForItem = InstanceDetailsActivity.this.getUpdateMessageForItem(instanceContentItem);
            int iOrdinal = updateStateForItem.ordinal();
            if (iOrdinal == 1) {
                contentViewHolder.updateButton.setEnabled(false);
                contentViewHolder.updateButton.setIconResource(R.drawable.ic_sync_24);
                contentViewHolder.updateButton.setContentDescription(InstanceDetailsActivity.this.getString(R.string.instance_content_checking_updates_short));
                contentViewHolder.updateButton.setOnClickListener(null);
                return;
            }
            if (iOrdinal == 2) {
                contentViewHolder.updateButton.setIconResource(R.drawable.ic_update_24);
                MaterialButton materialButton = contentViewHolder.updateButton;
                if (updateMessageForItem == null) {
                    updateMessageForItem = InstanceDetailsActivity.this.getString(R.string.instance_content_update_available);
                }
                materialButton.setContentDescription(updateMessageForItem);
                contentViewHolder.updateButton.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$InstanceContentAdapter$$ExternalSyntheticLambda5
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view) {
                        InstanceDetailsActivity.this.lambda$bindUpdateButton$6(instanceContentItem, view);
                    }
                });
                return;
            }
            if (iOrdinal == 3) {
                contentViewHolder.updateButton.setEnabled(false);
                contentViewHolder.updateButton.setIconResource(R.drawable.ic_update_24);
                MaterialButton materialButton2 = contentViewHolder.updateButton;
                if (updateMessageForItem == null) {
                    updateMessageForItem = InstanceDetailsActivity.this.getString(R.string.instance_content_updating_title);
                }
                materialButton2.setContentDescription(updateMessageForItem);
                contentViewHolder.updateButton.setOnClickListener(null);
                return;
            }
            if (iOrdinal == 4) {
                contentViewHolder.updateButton.setIconResource(R.drawable.ic_check_24);
                contentViewHolder.updateButton.setContentDescription(InstanceDetailsActivity.this.getString(R.string.instance_content_up_to_date));
                contentViewHolder.updateButton.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$InstanceContentAdapter$$ExternalSyntheticLambda6
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view) {
                        InstanceDetailsActivity.this.lambda$bindUpdateButton$7(instanceContentItem, view);
                    }
                });
            } else {
                if (iOrdinal == 5) {
                    contentViewHolder.updateButton.setIconResource(R.drawable.ic_sync_24);
                    MaterialButton materialButton3 = contentViewHolder.updateButton;
                    if (updateMessageForItem == null) {
                        updateMessageForItem = InstanceDetailsActivity.this.getString(R.string.instance_content_update_check_failed_short);
                    }
                    materialButton3.setContentDescription(updateMessageForItem);
                    contentViewHolder.updateButton.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$InstanceContentAdapter$$ExternalSyntheticLambda7
                        @Override // android.view.View.OnClickListener
                        public final void onClick(View view) {
                            InstanceDetailsActivity.this.lambda$bindUpdateButton$8(instanceContentItem, view);
                        }
                    });
                    return;
                }
                contentViewHolder.updateButton.setIconResource(R.drawable.ic_sync_24);
                contentViewHolder.updateButton.setContentDescription(InstanceDetailsActivity.this.getString(R.string.instance_content_check_update_button));
                contentViewHolder.updateButton.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$InstanceContentAdapter$$ExternalSyntheticLambda8
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view) {
                        InstanceDetailsActivity.this.lambda$bindUpdateButton$9(instanceContentItem, view);
                    }
                });
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$bindUpdateButton$6(InstanceContentItem instanceContentItem, View view) {
            InstanceDetailsActivity.this.updateSingleContentItemFromRow(instanceContentItem);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$bindUpdateButton$7(InstanceContentItem instanceContentItem, View view) {
            InstanceDetailsActivity.this.checkSingleContentUpdateFromRow(instanceContentItem);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$bindUpdateButton$8(InstanceContentItem instanceContentItem, View view) {
            InstanceDetailsActivity.this.checkSingleContentUpdateFromRow(instanceContentItem);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$bindUpdateButton$9(InstanceContentItem instanceContentItem, View view) {
            InstanceDetailsActivity.this.checkSingleContentUpdateFromRow(instanceContentItem);
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public long getItemId(int i) {
            if (i < 0 || i >= InstanceDetailsActivity.this.contentItems.size()) {
                return -1L;
            }
            InstanceContentItem instanceContentItem = (InstanceContentItem) InstanceDetailsActivity.this.contentItems.get(i);
            return (((long) InstanceDetailsActivity.this.safeCanonicalPath(instanceContentItem.file).hashCode()) * 31) + ((long) instanceContentItem.category.ordinal());
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public int getItemCount() {
            return InstanceDetailsActivity.this.contentItems.size();
        }

        private String buildMetadataKey(InstanceContentItem instanceContentItem) {
            return instanceContentItem.file.getAbsolutePath() + ":" + instanceContentItem.file.length() + ":" + instanceContentItem.file.lastModified() + ":" + instanceContentItem.category.name();
        }

        private void loadMetadataAsync(final ContentViewHolder contentViewHolder, final InstanceContentItem instanceContentItem, final String str) {
            if (this.loadingMetadata.add(str)) {
                InstanceDetailsActivity.this.iconExecutor.execute(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$InstanceContentAdapter$$ExternalSyntheticLambda9
                    @Override // java.lang.Runnable
                    public final void run() {
                        InstanceDetailsActivity.this.lambda$loadMetadataAsync$11(instanceContentItem, str, contentViewHolder);
                    }
                });
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$loadMetadataAsync$11(final InstanceContentItem instanceContentItem, final String str, final ContentViewHolder contentViewHolder) {
            LoadedContentMetadata loadedContentMetadata;
            try {
                loadedContentMetadata = InstanceDetailsActivity.this.loadContentMetadataForItem(instanceContentItem);
            } catch (Throwable th) {
                Logging.e(InstanceDetailsActivity.TAG, "Unable to resolve installed content metadata for " + instanceContentItem.file.getName(), th);
                loadedContentMetadata = new LoadedContentMetadata(null, null);
            }
            final LoadedContentMetadata loadedContentMetadata2 = loadedContentMetadata;
            InstanceDetailsActivity.this.mainHandler.post(new Runnable() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$InstanceContentAdapter$$ExternalSyntheticLambda11
                @Override // java.lang.Runnable
                public final void run() {
                    InstanceDetailsActivity.this.lambda$loadMetadataAsync$10(str, loadedContentMetadata2, contentViewHolder, instanceContentItem);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$loadMetadataAsync$10(String str, LoadedContentMetadata loadedContentMetadata, ContentViewHolder contentViewHolder, InstanceContentItem instanceContentItem) {
            this.loadingMetadata.remove(str);
            if (loadedContentMetadata.hasAny()) {
                this.metadataCache.put(str, loadedContentMetadata);
            } else {
                this.missingMetadataCache.add(str);
            }
            if (InstanceDetailsActivity.this.binding == null || InstanceDetailsActivity.this.isFinishing() || InstanceDetailsActivity.this.isDestroyed()) {
                return;
            }
            applyLoadedMetadata(contentViewHolder, instanceContentItem, str, loadedContentMetadata);
        }

        private void applyLoadedMetadata(ContentViewHolder contentViewHolder, InstanceContentItem instanceContentItem, String str, LoadedContentMetadata loadedContentMetadata) {
            InstanceDetailsActivity.this.rememberContentSearchMetadata(instanceContentItem, loadedContentMetadata.displayName);
            Object tag = contentViewHolder.itemView.getTag();
            if ((tag instanceof String) && str.equals(tag)) {
                if (instanceContentItem.category == ResourceCategory.MODS && loadedContentMetadata.displayName != null && !loadedContentMetadata.displayName.trim().isEmpty()) {
                    contentViewHolder.title.setText(loadedContentMetadata.displayName.trim());
                }
                if (loadedContentMetadata.icon != null) {
                    applyLoadedIcon(contentViewHolder, loadedContentMetadata.icon);
                }
            }
        }

        final class ContentViewHolder extends RecyclerView.ViewHolder {
            final MaterialButton deleteButton;
            final SwitchMaterial enabledSwitch;
            final ImageView icon;
            final MaterialButton playWorldButton;
            final ImageView sourceIcon;
            final TextView subtitle;
            final TextView title;
            final MaterialButton updateButton;

            ContentViewHolder(View view) {
                super(view);
                this.icon = (ImageView) view.findViewById(R.id.imageResourceIcon);
                this.title = (TextView) view.findViewById(R.id.textResourceName);
                this.subtitle = (TextView) view.findViewById(R.id.textResourceSubtitle);
                this.sourceIcon = (ImageView) view.findViewById(R.id.imageResourceInstalledSource);
                this.playWorldButton = (MaterialButton) view.findViewById(R.id.buttonPlayWorld);
                this.updateButton = (MaterialButton) view.findViewById(R.id.buttonUpdateResource);
                this.enabledSwitch = (SwitchMaterial) view.findViewById(R.id.switchResourceEnabled);
                this.deleteButton = (MaterialButton) view.findViewById(R.id.buttonDeleteResource);
            }
        }
    }

    private void showPerInstanceSettingsDialog() {
        enableFullscreen();
        String perInstanceSettingsKey = getPerInstanceSettingsKey();
        new PerInstanceSettingsDialog(this, perInstanceSettingsKey, collectPerInstanceSettingsAliasKeys(perInstanceSettingsKey), new InstanceDetailsActivity$$ExternalSyntheticLambda13(this)).show();
    }

    private void savePerInstanceSettingsAliases(String str, InstanceLaunchSettings.Settings settings) {
        Iterator<String> it = collectPerInstanceSettingsAliasKeys(str).iterator();
        while (it.hasNext()) {
            InstanceLaunchSettings.save(this, it.next(), settings);
        }
    }

    private void clearPerInstanceSettingsAliases(String str) {
        Iterator<String> it = collectPerInstanceSettingsAliasKeys(str).iterator();
        while (it.hasNext()) {
            InstanceLaunchSettings.clear(this, it.next());
        }
    }

    private ArrayList<String> collectPerInstanceSettingsAliasKeys(String str) {
        ArrayList<String> arrayList = new ArrayList<>();
        addPerInstanceSettingsAlias(arrayList, str);
        addPerInstanceSettingsAlias(arrayList, InstanceLaunchSettings.resolveInstanceKey(this.instanceId, this.instanceName));
        addPerInstanceSettingsAlias(arrayList, this.instanceId);
        addPerInstanceSettingsAlias(arrayList, this.instanceName);
        addPerInstanceSettingsAlias(arrayList, this.baseVersionId);
        if (this.isolated) {
            addPerInstanceSettingsAlias(arrayList, this.instanceName);
        } else {
            addPerInstanceSettingsAlias(arrayList, this.baseVersionId);
        }
        return arrayList;
    }

    private void addPerInstanceSettingsAlias(ArrayList<String> arrayList, String str) {
        if (isBlank(str)) {
            return;
        }
        String strResolveInstanceKey = InstanceLaunchSettings.resolveInstanceKey(str, str);
        if (arrayList.contains(strResolveInstanceKey)) {
            return;
        }
        arrayList.add(strResolveInstanceKey);
    }

    private String getPerInstanceSettingsKey() {
        return InstanceLaunchSettings.resolveInstanceKey(this.instanceId, this.instanceName);
    }

    private TextView buildPerInstanceDialogLabel(String str) {
        TextView textView = new TextView(this);
        textView.setText(str);
        textView.setTextSize(2, 14.0f);
        textView.setTypeface(textView.getTypeface(), 1);
        textView.setTextColor(resolveThemeColor(android.R.attr.textColorPrimary, -1));
        return textView;
    }

    private LinearLayout.LayoutParams topMarginParams(int i) {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = i;
        return layoutParams;
    }

    private int resolveRendererSelectionIndex(ArrayList<RendererInterface> arrayList, String str) {
        if (isBlank(str)) {
            return 0;
        }
        for (int i = 0; i < arrayList.size(); i++) {
            if (str.equals(arrayList.get(i).getUniqueIdentifier())) {
                return i + 1;
            }
        }
        return 0;
    }

    private void updatePerInstanceRamSliderState(SeekBar seekBar, boolean z) {
        seekBar.setEnabled(z);
        seekBar.setAlpha(z ? 1.0f : 0.45f);
    }

    private int calculatePerInstanceRamStepCount(int i, int i2, int i3) {
        if (i2 <= i) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil((i2 - i) / i3));
    }

    private int perInstanceRamMemoryFromProgress(int i, int i2, int i3, int i4) {
        long jMax = ((long) i2) + (((long) Math.max(0, i)) * ((long) i3));
        long j = i4;
        if (jMax > j) {
            jMax = j;
        }
        return MemoryAllocationUtils.clampToAllowedRam(this, (int) jMax);
    }

    private int perInstanceRamProgressFromMemory(int i, int i2, int i3, int i4) {
        return Math.max(0, Math.min(Math.round((MemoryAllocationUtils.clampToAllowedRam(this, i) - i2) / Math.max(1, i3)), i4));
    }

    private void updatePerInstanceRamText(TextView textView, boolean z, int i, int i2) {
        if (z) {
            textView.setText("Custom RAM: " + i + " MB (" + formatGb(i) + " GB) · Max recommended: " + i2 + " MB");
        } else {
            int iResolveAllocatedMemoryMb = MemoryAllocationUtils.resolveAllocatedMemoryMb(this);
            textView.setText("Using launcher default: " + iResolveAllocatedMemoryMb + " MB (" + formatGb(iResolveAllocatedMemoryMb) + " GB)");
        }
    }

    private void showPerInstanceRamInputDialog(final TextView textView, final SeekBar seekBar, final SwitchMaterial switchMaterial, final boolean[] zArr, final int[] iArr, final int i, final int i2, final int i3, final int i4) {
        int iResolveAllocatedMemoryMb;
        final EditText editText = new EditText(this);
        editText.setInputType(2);
        editText.setSingleLine(true);
        editText.setSelectAllOnFocus(true);
        if (zArr[0]) {
            iResolveAllocatedMemoryMb = iArr[0];
        } else {
            iResolveAllocatedMemoryMb = MemoryAllocationUtils.resolveAllocatedMemoryMb(this);
        }
        editText.setText(String.valueOf(iResolveAllocatedMemoryMb));
        final AlertDialog alertDialogCreate = new AlertDialog.Builder(this).setTitle("Custom RAM").setMessage("Enter RAM in MB. Allowed range: " + i + " - " + i4 + " MB.").setView(editText).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).create();
        alertDialogCreate.setOnShowListener(new DialogInterface.OnShowListener() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda76
            @Override // android.content.DialogInterface.OnShowListener
            public final void onShow(DialogInterface dialogInterface) {
                InstanceDetailsActivity.this.lambda$showPerInstanceRamInputDialog$100(alertDialogCreate, editText, iArr, zArr, switchMaterial, seekBar, i, i2, i3, textView, i4, dialogInterface);
            }
        });
        showFullscreenSafeDialog(alertDialogCreate);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showPerInstanceRamInputDialog$100(final AlertDialog alertDialog, final EditText editText, final int[] iArr, final boolean[] zArr, final SwitchMaterial switchMaterial, final SeekBar seekBar, final int i, final int i2, final int i3, final TextView textView, final int i4, DialogInterface dialogInterface) {
        alertDialog.getButton(-1).setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.InstanceDetailsActivity$$ExternalSyntheticLambda55
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                InstanceDetailsActivity.this.lambda$showPerInstanceRamInputDialog$99(editText, iArr, zArr, switchMaterial, seekBar, i, i2, i3, textView, i4, alertDialog, view);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showPerInstanceRamInputDialog$99(EditText editText, int[] iArr, boolean[] zArr, SwitchMaterial switchMaterial, SeekBar seekBar, int i, int i2, int i3, TextView textView, int i4, AlertDialog alertDialog, View view) {
        try {
            iArr[0] = MemoryAllocationUtils.clampToAllowedRam(this, Integer.parseInt(editText.getText() == null ? "" : editText.getText().toString().trim()));
            zArr[0] = true;
            switchMaterial.setChecked(true);
            updatePerInstanceRamSliderState(seekBar, true);
            seekBar.setProgress(perInstanceRamProgressFromMemory(iArr[0], i, i2, i3));
            updatePerInstanceRamText(textView, true, iArr[0], i4);
            alertDialog.dismiss();
        } catch (Throwable unused) {
            editText.setError("Enter a number in MB.");
        }
    }

    private String formatGb(int i) {
        return String.format(Locale.US, "%.1f", Float.valueOf(i / 1024.0f));
    }
}
