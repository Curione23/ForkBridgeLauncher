package ca.dnamobile.javalauncher;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import ca.dnamobile.javalauncher.auth.MicrosoftAuthConfigPersonal;
import ca.dnamobile.javalauncher.auth.MicrosoftAuthManagerPersonal;
import ca.dnamobile.javalauncher.auth.OfflineAccessBlocker;
import ca.dnamobile.javalauncher.controls.ControlsMain;
import ca.dnamobile.javalauncher.data.AccountStore;
import ca.dnamobile.javalauncher.data.model.MinecraftVersion;
import ca.dnamobile.javalauncher.databinding.ActivityMainBinding;
import ca.dnamobile.javalauncher.feature.log.Logging;
import ca.dnamobile.javalauncher.installation.InstallationForegroundService;
import ca.dnamobile.javalauncher.instance.LauncherInstance;
import ca.dnamobile.javalauncher.instance.LauncherInstanceManager;
import ca.dnamobile.javalauncher.legal.LegalConsentStore;
import ca.dnamobile.javalauncher.legal.LegalLinks;
import ca.dnamobile.javalauncher.logs.LauncherLogManager;
import ca.dnamobile.javalauncher.modcompat.SimpleVoiceChatCompat;
import ca.dnamobile.javalauncher.modmanager.ModpackInstallManager;
import ca.dnamobile.javalauncher.notifications.LauncherNotificationPermissionHelper;
import ca.dnamobile.javalauncher.settings.LauncherPreferences;
import ca.dnamobile.javalauncher.storage.SafMinecraftMirror;
import ca.dnamobile.javalauncher.storage.StorageLocation;
import ca.dnamobile.javalauncher.storage.StorageLocationDialog;
import ca.dnamobile.javalauncher.storage.StorageLocationStore;
import ca.dnamobile.javalauncher.ui.instance.CreateInstanceDialog;
import ca.dnamobile.javalauncher.ui.instance.LauncherInstanceAdapter;
import ca.dnamobile.javalauncher.ui.version.FabricInstaller;
import ca.dnamobile.javalauncher.ui.version.ForgeInstaller;
import ca.dnamobile.javalauncher.ui.version.MinecraftVersionInstaller;
import ca.dnamobile.javalauncher.ui.version.MinecraftVersionManifestClient;
import ca.dnamobile.javalauncher.ui.version.NeoForgeInstaller;
import ca.dnamobile.javalauncher.update.LauncherUpdateDialogs;
import ca.dnamobile.javalauncher.utils.FullscreenUtils;
import ca.dnamobile.javalauncher.utils.path.PathManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.tabs.TabLayout;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.kdt.pojavlaunch.Tools;
import org.json.JSONObject;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public class MainActivity extends AppCompatActivity {
    private static final String FILTER_ALL = "all";
    private static final String FILTER_MODIFIED = "modified";
    private static final String FILTER_RECENT = "recent";
    private static final String FILTER_SHARED = "shared";
    private static final String FILTER_SNAPSHOT = "snapshot";
    private static final String FILTER_VANILLA = "vanilla";
    private static final int INSTALL_NOTIFICATION_PROGRESS_STEP = 2;
    private static final long INSTALL_NOTIFICATION_UPDATE_INTERVAL_MS = 1000;
    private static final long INSTALL_UI_MESSAGE_UPDATE_INTERVAL_MS = 850;
    private static final long INSTALL_UI_UPDATE_INTERVAL_MS = 350;
    private static final int REQUEST_ADD_STORAGE_LOCATION = 8032;
    private static final int REQUEST_IMPORT_MODPACK = 8033;
    private static final int REQUEST_PICK_INSTANCE_ICON = 8021;
    private static final String TAG_BROWSE_CONTENT_MAIN_BUTTON = "browse_content_main_button_dynamic";
    private static final String TAG_IMPORT_MODPACK_MAIN_BUTTON = "import_modpack_main_button_dynamic";
    private static final String TYPE_INSTALLED = "installed";
    private AccountStore accountStore;
    private int activeInstallProgress;
    private boolean appIntegrityBlocked;
    private MicrosoftAuthManagerPersonal authManager;
    private ActivityMainBinding binding;
    private CreateInstanceDialog createInstanceDialog;
    private AlertDialog installDialog;
    private CheckBox installDialogForegroundCheck;
    private TextView installDialogMessage;
    private ProgressBar installDialogProgress;
    private boolean installPermissionPromptShownThisSession;
    private boolean installSessionActive;
    private LauncherInstanceAdapter instanceAdapter;
    private long lastInstallNotificationUpdateMs;
    private long lastInstallUiDispatchMs;
    private AlertDialog launchPrepareDialog;
    private TextView launchPrepareMessage;
    private TextView launchPreparePercent;
    private ProgressBar launchPrepareProgress;
    private ActivityResultLauncher<String> notificationPermissionLauncher;
    private Runnable pendingAfterMicrosoftSignIn;
    private LauncherInstance selectedInstance;
    private static final int COLOR_DIALOG_BG = Color.rgb(30, 34, 42);
    private static final int COLOR_CARD_BG = Color.rgb(38, 43, 53);
    private static final int COLOR_CARD_STROKE = Color.rgb(54, 61, 74);
    private static final int COLOR_TEXT_PRIMARY = Color.rgb(238, 241, 248);
    private static final int COLOR_TEXT_SECONDARY = Color.rgb(198, 204, 216);
    private static final int COLOR_TEXT_MUTED = Color.rgb(150, 159, 176);
    private static final int COLOR_ACCENT = Color.rgb(37, 211, 128);
    private String selectedFilter = FILTER_ALL;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private String activeInstallTitle = "Installing Minecraft";
    private String activeInstallMessage = "Preparing installation...";
    private int lastInstallUiDispatchProgress = -1;
    private String lastInstallUiDispatchMessage = "";
    private int lastInstallNotificationProgress = -1;
    private final ArrayList<MinecraftVersion> allVersions = new ArrayList<>();
    private final ArrayList<LauncherInstance> installedInstances = new ArrayList<>();
    private final Set<String> installedBaseVersionIds = new HashSet();

    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        boolean zBlockIfInvalidSignature = ControlsMain.blockIfInvalidSignature(this);
        this.appIntegrityBlocked = zBlockIfInvalidSignature;
        if (zBlockIfInvalidSignature) {
            return;
        }
        PathManager.initContextConstants(this);
        this.selectedFilter = sanitizeSavedFilter(LauncherPreferences.getSelectedInstanceFilter(this, FILTER_ALL));
        ActivityMainBinding activityMainBindingInflate = ActivityMainBinding.inflate(getLayoutInflater());
        this.binding = activityMainBindingInflate;
        setContentView(activityMainBindingInflate.getRoot());
        FullscreenUtils.enableImmersive(this);
        registerNotificationPermissionLauncher();
        setupAccountUi();
        setupInstanceUi();
        refreshInstancesAndRebind(false);
        loadVersions(false);
        maybeShowRequiredLegalAcceptanceDialog();
    }

    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    protected void onResume() {
        super.onResume();
        if (this.appIntegrityBlocked || this.binding == null) {
            return;
        }
        PathManager.initContextConstants(this);
        FullscreenUtils.enableImmersive(this);
        refreshAccountUiFromStore();
        if (this.installSessionActive) {
            return;
        }
        refreshInstancesAndRebind(true);
    }

    @Override // androidx.appcompat.app.AppCompatActivity, androidx.activity.ComponentActivity, android.app.Activity, android.content.ComponentCallbacks
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        ActivityMainBinding activityMainBinding = this.binding;
        if (activityMainBinding == null || !(activityMainBinding.recyclerVersions.getLayoutManager() instanceof GridLayoutManager)) {
            return;
        }
        ((GridLayoutManager) this.binding.recyclerVersions.getLayoutManager()).setSpanCount(getInstanceGridSpanCount());
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public void onWindowFocusChanged(boolean z) {
        super.onWindowFocusChanged(z);
        if (this.appIntegrityBlocked || this.binding == null || !z) {
            return;
        }
        FullscreenUtils.enableImmersive(this);
    }

    @Override // androidx.appcompat.app.AppCompatActivity, androidx.fragment.app.FragmentActivity, android.app.Activity
    protected void onDestroy() {
        if (this.installSessionActive) {
            getWindow().clearFlags(128);
        }
        dismissLaunchPrepareDialog();
        if (this.authManager != null && !isChangingConfigurations()) {
            this.authManager.dispose();
        }
        this.mainHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void maybeShowRequiredLegalAcceptanceDialog() {
        if (LegalConsentStore.hasAcceptedCurrentTerms(this)) {
            maybeShowNotificationPermissionLaunchPrompt();
            LauncherUpdateDialogs.checkOnStartup(this);
            return;
        }
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        int iDp = dp(22.0f);
        linearLayout.setPadding(iDp, 0, iDp, 0);
        TextView textView = new TextView(this);
        textView.setText("Before using DroidBridge Launcher, you must accept that your use of Minecraft is subject to the Minecraft End User License Agreement (EULA) and Minecraft Usage Guidelines.\n\nYou do not have to read the EULA here before continuing, but the link is provided below for review. Press Accept to start using the launcher.");
        textView.setTextAppearance(android.R.style.TextAppearance.Material.Body1);
        linearLayout.addView(textView, new LinearLayout.LayoutParams(-1, -2));
        TextView textView2 = new TextView(this);
        textView2.setText("Open Minecraft EULA");
        textView2.setTextAppearance(android.R.style.TextAppearance.Material.Medium);
        textView2.setTextColor(-14776091);
        textView2.setPadding(0, dp(14.0f), 0, 0);
        textView2.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda41
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.lambda$maybeShowRequiredLegalAcceptanceDialog$0(view);
            }
        });
        linearLayout.addView(textView2, new LinearLayout.LayoutParams(-1, -2));
        final AlertDialog alertDialogCreate = new AlertDialog.Builder(this).setTitle("Minecraft EULA").setView(linearLayout).setCancelable(false).setPositiveButton("Accept", (DialogInterface.OnClickListener) null).create();
        alertDialogCreate.setOnShowListener(new DialogInterface.OnShowListener() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda42
            @Override // android.content.DialogInterface.OnShowListener
            public final void onShow(DialogInterface dialogInterface) {
                MainActivity.this.lambda$maybeShowRequiredLegalAcceptanceDialog$2(alertDialogCreate, dialogInterface);
            }
        });
        alertDialogCreate.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$maybeShowRequiredLegalAcceptanceDialog$0(View view) {
        LegalLinks.open(this, LegalLinks.MINECRAFT_EULA_URL);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$maybeShowRequiredLegalAcceptanceDialog$2(final AlertDialog alertDialog, DialogInterface dialogInterface) {
        alertDialog.getButton(-1).setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda22
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.lambda$maybeShowRequiredLegalAcceptanceDialog$1(alertDialog, view);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$maybeShowRequiredLegalAcceptanceDialog$1(AlertDialog alertDialog, View view) {
        LegalConsentStore.markCurrentTermsAccepted(this);
        alertDialog.dismiss();
        maybeShowNotificationPermissionLaunchPrompt();
        LauncherUpdateDialogs.checkOnStartup(this);
    }

    private void setupAccountUi() {
        try {
            this.accountStore = new AccountStore(this);
            MicrosoftAuthManagerPersonal microsoftAuthManagerPersonal = new MicrosoftAuthManagerPersonal(this, this.accountStore);
            this.authManager = microsoftAuthManagerPersonal;
            microsoftAuthManagerPersonal.setListener(new MicrosoftAuthManagerPersonal.Listener() { // from class: ca.dnamobile.javalauncher.MainActivity.1
                @Override // ca.dnamobile.javalauncher.auth.MicrosoftAuthManagerPersonal.Listener
                public void onSignedIn(AccountStore.Account account) {
                    MainActivity.this.updateAccountStatus(account);
                    MainActivity mainActivity = MainActivity.this;
                    mainActivity.setStatus(mainActivity.getString(R.string.msg_sign_in_success));
                    Runnable runnable = MainActivity.this.pendingAfterMicrosoftSignIn;
                    MainActivity.this.pendingAfterMicrosoftSignIn = null;
                    if (runnable != null) {
                        runnable.run();
                    }
                }

                @Override // ca.dnamobile.javalauncher.auth.MicrosoftAuthManagerPersonal.Listener
                public void onError(String str) {
                    MainActivity.this.pendingAfterMicrosoftSignIn = null;
                    MainActivity.this.setStatus(str);
                    Toast.makeText(MainActivity.this, str, 1).show();
                }
            });
            updateAccountStatus(this.accountStore.load());
        } catch (Throwable th) {
            Logging.e("MainActivity", "Microsoft account UI initialization failed", th);
            this.binding.textAccountStatus.setText(R.string.status_signed_out);
            this.binding.buttonSignIn.setEnabled(false);
            this.binding.buttonSignOut.setEnabled(false);
        }
        this.binding.buttonSignIn.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda29
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.lambda$setupAccountUi$3(view);
            }
        });
        this.binding.buttonSignOut.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda30
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.lambda$setupAccountUi$4(view);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupAccountUi$3(View view) {
        lambda$requireMicrosoftLoginHistoryBeforeLaunch$7(null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupAccountUi$4(View view) {
        showSignOutConfirmationDialog();
    }

    private void refreshAccountUiFromStore() {
        AccountStore accountStore = this.accountStore;
        if (accountStore == null || this.binding == null) {
            return;
        }
        try {
            updateAccountStatus(accountStore.load());
        } catch (Throwable th) {
            Logging.e("MainActivity", "Unable to refresh account UI", th);
        }
    }

    private void showSignOutConfirmationDialog() {
        new AlertDialog.Builder(this).setTitle(R.string.sign_out_confirm_title).setMessage(R.string.sign_out_confirm_message).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.button_sign_out, new DialogInterface.OnClickListener() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda44
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.this.lambda$showSignOutConfirmationDialog$5(dialogInterface, i);
            }
        }).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showSignOutConfirmationDialog$5(DialogInterface dialogInterface, int i) {
        performMicrosoftSignOut();
    }

    private void performMicrosoftSignOut() {
        MicrosoftAuthManagerPersonal microsoftAuthManagerPersonal = this.authManager;
        if (microsoftAuthManagerPersonal == null || this.accountStore == null) {
            return;
        }
        this.pendingAfterMicrosoftSignIn = null;
        microsoftAuthManagerPersonal.signOut();
        updateAccountStatus(this.accountStore.load());
        setStatus(getString(R.string.msg_sign_out_success));
        Toast.makeText(this, R.string.msg_sign_out_success, 0).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: renamed from: signInWithMicrosoftThen, reason: merged with bridge method [inline-methods] and merged with bridge method [inline-methods] */
    public void lambda$requireMicrosoftLoginHistoryBeforeLaunch$7(Runnable runnable) {
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

    private boolean hasActiveMicrosoftAccount() {
        return OfflineAccessBlocker.hasActiveMicrosoftAccount(this.accountStore);
    }

    private boolean hasCompletedMicrosoftLoginOnce() {
        return OfflineAccessBlocker.hasCompletedMicrosoftLoginOnce(this.accountStore);
    }

    private boolean requireActiveMicrosoftAccountBeforeCreateInstance(final Runnable runnable) {
        return OfflineAccessBlocker.requireActiveMicrosoftAccountBeforeInstall(this, this.accountStore, new Runnable() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda23
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.lambda$requireActiveMicrosoftAccountBeforeCreateInstance$6(runnable);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean requireMicrosoftLoginHistoryBeforeLaunch(final Runnable runnable) {
        return OfflineAccessBlocker.requireMicrosoftLoginHistoryBeforeLaunch(this, this.accountStore, new Runnable() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda21
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.lambda$requireMicrosoftLoginHistoryBeforeLaunch$7(runnable);
            }
        });
    }

    private void setupInstanceUi() {
        this.binding.textFolder.setText(getString(R.string.launcher_folder_value, new Object[]{PathManager.DIR_MINECRAFT_HOME}));
        setupRenderSurfaceUi();
        setupSharedInstallsUi();
        this.instanceAdapter = new LauncherInstanceAdapter(this, new AnonymousClass2());
        this.binding.recyclerVersions.setLayoutManager(new GridLayoutManager(this, getInstanceGridSpanCount()));
        this.binding.recyclerVersions.setAdapter(this.instanceAdapter);
        addInstanceTabs();
        this.binding.buttonRefreshVersions.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda10
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.lambda$setupInstanceUi$8(view);
            }
        });
        setupStorageLocationsButton();
        setupMainContentButtons();
        this.binding.fabCreateInstance.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda12
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.lambda$setupInstanceUi$9(view);
            }
        });
        this.binding.buttonLaunchVersion.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda13
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.lambda$setupInstanceUi$10(view);
            }
        });
        this.binding.buttonOpenFolder.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda14
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.lambda$setupInstanceUi$11(view);
            }
        });
        if (this.binding.buttonOpenSettings != null) {
            this.binding.buttonOpenSettings.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda15
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    MainActivity.this.lambda$setupInstanceUi$12(view);
                }
            });
        }
        this.binding.checkKeepLogs.setChecked(LauncherLogManager.isKeepLogHistoryEnabled(this));
        this.binding.checkKeepLogs.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda16
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                MainActivity.this.lambda$setupInstanceUi$13(compoundButton, z);
            }
        });
        this.binding.buttonShareLatestLog.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda17
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.lambda$setupInstanceUi$14(view);
            }
        });
        this.binding.buttonLaunchVersion.setEnabled(false);
        this.binding.buttonOpenFolder.setEnabled(false);
        updateSelectedInstanceCard();
    }

    /* JADX INFO: renamed from: ca.dnamobile.javalauncher.MainActivity$2, reason: invalid class name */
    class AnonymousClass2 implements LauncherInstanceAdapter.Listener {
        AnonymousClass2() {
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onInstanceSelected$0(LauncherInstance launcherInstance) {
            MainActivity.this.selectAndOpenInstance(launcherInstance);
        }

        @Override // ca.dnamobile.javalauncher.ui.instance.LauncherInstanceAdapter.Listener
        public void onInstanceSelected(final LauncherInstance launcherInstance) {
            if (MainActivity.this.requireMicrosoftLoginHistoryBeforeLaunch(new Runnable() { // from class: ca.dnamobile.javalauncher.MainActivity$2$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.lambda$onInstanceSelected$0(launcherInstance);
                }
            })) {
                MainActivity.this.selectedInstance = null;
                MainActivity.this.instanceAdapter.clearSelectedInstance();
                MainActivity.this.updateSelectedInstanceCard();
                return;
            }
            MainActivity.this.selectAndOpenInstance(launcherInstance);
        }

        @Override // ca.dnamobile.javalauncher.ui.instance.LauncherInstanceAdapter.Listener
        public void onInstanceQuickPlayRequested(LauncherInstance launcherInstance) {
            MainActivity.this.lambda$quickLaunchInstance$44(launcherInstance);
        }

        @Override // ca.dnamobile.javalauncher.ui.instance.LauncherInstanceAdapter.Listener
        public void onInstanceDeleteRequested(LauncherInstance launcherInstance) {
            MainActivity.this.showDeleteInstanceDialog(launcherInstance);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupInstanceUi$8(View view) {
        refreshInstancesAndRebind(true);
        loadVersions(false);
        setStatus(getString(R.string.instance_status_refreshed, new Object[]{Integer.valueOf(this.installedInstances.size())}));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupInstanceUi$9(View view) {
        if (requireActiveMicrosoftAccountBeforeCreateInstance(new MainActivity$$ExternalSyntheticLambda2(this))) {
            return;
        }
        showCreateInstanceDialog();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupInstanceUi$10(View view) {
        launchSelectedInstance();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupInstanceUi$11(View view) {
        showSelectedInstanceFolder();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupInstanceUi$12(View view) {
        startActivity(new Intent(this, (Class<?>) LauncherSettingsActivity.class));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupInstanceUi$13(CompoundButton compoundButton, boolean z) {
        LauncherLogManager.setKeepLogHistoryEnabled(this, z);
        setStatus(getString(z ? R.string.log_history_enabled : R.string.log_history_disabled));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupInstanceUi$14(View view) {
        LauncherLogManager.shareLatestLog(this);
    }

    private void setupRenderSurfaceUi() {
        boolean zIsUseNativeSurfaceView = LauncherPreferences.isUseNativeSurfaceView(this);
        this.binding.switchUseNativeSurface.setChecked(zIsUseNativeSurfaceView);
        updateRenderSurfaceSwitchText(zIsUseNativeSurfaceView);
        this.binding.switchUseNativeSurface.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda3
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                MainActivity.this.lambda$setupRenderSurfaceUi$15(compoundButton, z);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupRenderSurfaceUi$15(CompoundButton compoundButton, boolean z) {
        LauncherPreferences.setUseNativeSurfaceView(this, z);
        updateRenderSurfaceSwitchText(z);
        setStatus(getString(R.string.render_surface_mode_status, new Object[]{getString(z ? R.string.render_surface_surface_view : R.string.render_surface_texture_view)}));
    }

    private void updateRenderSurfaceSwitchText(boolean z) {
        int i;
        SwitchMaterial switchMaterial = this.binding.switchUseNativeSurface;
        if (z) {
            i = R.string.render_surface_surface_view;
        } else {
            i = R.string.render_surface_texture_view;
        }
        switchMaterial.setText(i);
    }

    private void setupSharedInstallsUi() {
        boolean zIsShowSharedInstalls = LauncherPreferences.isShowSharedInstalls(this);
        this.binding.switchShowSharedInstalls.setChecked(zIsShowSharedInstalls);
        updateSharedInstallsSwitchText(zIsShowSharedInstalls);
        this.binding.switchShowSharedInstalls.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda1
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                MainActivity.this.lambda$setupSharedInstallsUi$16(compoundButton, z);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupSharedInstallsUi$16(CompoundButton compoundButton, boolean z) {
        LauncherPreferences.setShowSharedInstalls(this, z);
        updateSharedInstallsSwitchText(z);
        refreshInstancesAndRebind(true);
        setStatus(getString(z ? R.string.shared_installs_shown : R.string.shared_installs_hidden));
    }

    private void updateSharedInstallsSwitchText(boolean z) {
        int i;
        SwitchMaterial switchMaterial = this.binding.switchShowSharedInstalls;
        if (z) {
            i = R.string.shared_installs_show;
        } else {
            i = R.string.shared_installs_hide;
        }
        switchMaterial.setText(i);
    }

    private void addInstanceTabs() {
        addInstanceTab(getString(R.string.instance_tab_all), FILTER_ALL, FILTER_ALL.equals(this.selectedFilter));
        addInstanceTab(getString(R.string.instance_tab_recent), FILTER_RECENT, FILTER_RECENT.equals(this.selectedFilter));
        addInstanceTab(getString(R.string.instance_tab_vanilla), FILTER_VANILLA, FILTER_VANILLA.equals(this.selectedFilter));
        addInstanceTab(getString(R.string.instance_tab_modified), FILTER_MODIFIED, FILTER_MODIFIED.equals(this.selectedFilter));
        addInstanceTab(getString(R.string.instance_tab_snapshot), FILTER_SNAPSHOT, FILTER_SNAPSHOT.equals(this.selectedFilter));
        addInstanceTab(getString(R.string.instance_tab_shared), FILTER_SHARED, FILTER_SHARED.equals(this.selectedFilter));
        this.binding.tabVersionTypes.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() { // from class: ca.dnamobile.javalauncher.MainActivity.3
            @Override // com.google.android.material.tabs.TabLayout.BaseOnTabSelectedListener
            public void onTabReselected(TabLayout.Tab tab) {
            }

            @Override // com.google.android.material.tabs.TabLayout.BaseOnTabSelectedListener
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override // com.google.android.material.tabs.TabLayout.BaseOnTabSelectedListener
            public void onTabSelected(TabLayout.Tab tab) {
                Object tag = tab.getTag();
                MainActivity mainActivity = MainActivity.this;
                mainActivity.selectedFilter = mainActivity.sanitizeSavedFilter(tag instanceof String ? (String) tag : MainActivity.FILTER_ALL);
                MainActivity mainActivity2 = MainActivity.this;
                LauncherPreferences.setSelectedInstanceFilter(mainActivity2, mainActivity2.selectedFilter);
                MainActivity.this.selectedInstance = null;
                MainActivity.this.instanceAdapter.clearSelectedInstance();
                MainActivity.this.applyInstanceFilter();
                MainActivity.this.updateSelectedInstanceCard();
            }
        });
    }

    private void addInstanceTab(String str, String str2, boolean z) {
        TabLayout.Tab tabNewTab = this.binding.tabVersionTypes.newTab();
        tabNewTab.setText(str);
        tabNewTab.setTag(str2);
        this.binding.tabVersionTypes.addTab(tabNewTab, z);
    }

    private int getInstanceGridSpanCount() {
        return getResources().getConfiguration().orientation == 2 ? 2 : 1;
    }

    private void loadVersions(final boolean z) {
        if (z) {
            setLoading(true);
            setStatus(getString(R.string.msg_fetching_versions));
        }
        new Thread(new Runnable() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda20
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.lambda$loadVersions$19(z);
            }
        }, "Minecraft Version Manifest").start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$loadVersions$19(final boolean z) {
        try {
            final List<MinecraftVersion> listLoadVersions = MinecraftVersionManifestClient.loadVersions(this);
            runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda51
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.lambda$loadVersions$17(listLoadVersions, z);
                }
            });
        } catch (Throwable th) {
            Logging.e("VersionManifest", "Unable to load Minecraft version manifest", th);
            runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda52
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.lambda$loadVersions$18(z, th);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$loadVersions$17(List list, boolean z) {
        this.allVersions.clear();
        this.allVersions.addAll(list);
        if (z) {
            setLoading(false);
        }
        setStatus(getString(R.string.msg_versions_loaded, new Object[]{Integer.valueOf(list.size())}));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$loadVersions$18(boolean z, Throwable th) {
        if (z) {
            setLoading(false);
        }
        setStatus(getString(R.string.msg_versions_failed, new Object[]{th.getMessage() != null ? th.getMessage() : th.getClass().getSimpleName()}));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void refreshInstancesAndRebind(boolean z) {
        LauncherInstance launcherInstance;
        String selectionKey = (!z || (launcherInstance = this.selectedInstance) == null) ? null : LauncherInstanceAdapter.getSelectionKey(launcherInstance);
        refreshInstalledBaseVersions();
        this.installedInstances.clear();
        ArrayList<LauncherInstance> arrayListFindInstances = LauncherInstanceManager.findInstances(this);
        this.installedInstances.addAll(arrayListFindInstances);
        addSharedInstalledVersions(arrayListFindInstances);
        if (selectionKey != null) {
            this.selectedInstance = findInstanceBySelectionKey(selectionKey);
        } else {
            this.selectedInstance = null;
        }
        LauncherInstanceAdapter launcherInstanceAdapter = this.instanceAdapter;
        if (launcherInstanceAdapter != null) {
            launcherInstanceAdapter.setSelectedInstance(this.selectedInstance);
            applyInstanceFilter();
        }
        updateSelectedInstanceCard();
    }

    private void refreshInstalledBaseVersions() {
        this.installedBaseVersionIds.clear();
        Iterator<MinecraftVersion> it = MinecraftVersionInstaller.findInstalledVersions().iterator();
        while (it.hasNext()) {
            this.installedBaseVersionIds.add(it.next().getId());
        }
    }

    private void addSharedInstalledVersions(List<LauncherInstance> list) {
        if (LauncherPreferences.isShowSharedInstalls(this)) {
            HashSet<String> hashSetCollectVersionsRequiredByIsolatedInstances = collectVersionsRequiredByIsolatedInstances(list);
            HashSet hashSet = new HashSet();
            for (File file : StorageLocationStore.getVisibleMinecraftHomes(this)) {
                for (MinecraftVersion minecraftVersion : MinecraftVersionInstaller.findInstalledVersions(file)) {
                    if (!hashSetCollectVersionsRequiredByIsolatedInstances.contains(minecraftVersion.getId()) && hashSet.add(LauncherInstance.sharedInstanceId(minecraftVersion.getId(), file))) {
                        this.installedInstances.add(LauncherInstance.sharedInstalledVersion(minecraftVersion.getId(), normalizeInstalledVersionType(minecraftVersion.getType()), file, minecraftVersion.getReleaseTime(), inferLoaderNameFromVersionId(minecraftVersion.getId())));
                    }
                }
            }
        }
    }

    private LauncherInstance findInstanceBySelectionKey(String str) {
        for (LauncherInstance launcherInstance : this.installedInstances) {
            if (str.equals(LauncherInstanceAdapter.getSelectionKey(launcherInstance))) {
                return launcherInstance;
            }
        }
        return null;
    }

    private HashSet<String> collectVersionsRequiredByIsolatedInstances(List<LauncherInstance> list) {
        HashSet<String> hashSet = new HashSet<>();
        for (LauncherInstance launcherInstance : list) {
            String baseVersionId = launcherInstance.getBaseVersionId();
            if (baseVersionId != null && !baseVersionId.trim().isEmpty()) {
                hashSet.add(baseVersionId);
                collectInheritedVersionIds(baseVersionId, hashSet, new HashSet<>());
            }
            String minecraftVersionId = launcherInstance.getMinecraftVersionId();
            if (minecraftVersionId != null && !minecraftVersionId.trim().isEmpty()) {
                hashSet.add(minecraftVersionId);
                collectInheritedVersionIds(minecraftVersionId, hashSet, new HashSet<>());
            }
        }
        return hashSet;
    }

    private void collectInheritedVersionIds(String str, HashSet<String> hashSet, HashSet<String> hashSet2) {
        if (hashSet2.add(str)) {
            File file = new File(MinecraftVersionInstaller.getVersionDirectory(str), str + ".json");
            if (file.isFile()) {
                try {
                    String strOptString = new JSONObject(readFile(file)).optString("inheritsFrom", "");
                    if (strOptString != null && !strOptString.trim().isEmpty()) {
                        hashSet.add(strOptString);
                        collectInheritedVersionIds(strOptString, hashSet, hashSet2);
                    }
                } catch (Throwable th) {
                    Logging.i("MainActivity", "Unable to inspect inherited version for " + str + ": " + th.getMessage());
                }
            }
        }
    }

    private static String readFile(File file) throws Exception {
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            String str = Tools.read(fileInputStream);
            fileInputStream.close();
            return str;
        } catch (Throwable th) {
            try {
                fileInputStream.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    private String inferLoaderNameFromVersionId(String str) {
        String strInferLoaderNameFromVersionId = NeoForgeInstaller.inferLoaderNameFromVersionId(str);
        if (!"Vanilla".equalsIgnoreCase(strInferLoaderNameFromVersionId)) {
            return strInferLoaderNameFromVersionId;
        }
        String strInferLoaderNameFromVersionId2 = ForgeInstaller.inferLoaderNameFromVersionId(str);
        return !"Vanilla".equalsIgnoreCase(strInferLoaderNameFromVersionId2) ? strInferLoaderNameFromVersionId2 : FabricInstaller.inferLoaderNameFromVersionId(str);
    }

    private String normalizeInstalledVersionType(String str) {
        return (str == null || MainActivity$$ExternalSyntheticBackport0.m(str) || TYPE_INSTALLED.equals(str)) ? BuildConfig.BUILD_TYPE : str;
    }

    private LauncherInstance findInstanceById(String str) {
        for (LauncherInstance launcherInstance : this.installedInstances) {
            if (str.equals(launcherInstance.getId())) {
                return launcherInstance;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void applyInstanceFilter() {
        ArrayList arrayList = new ArrayList();
        for (LauncherInstance launcherInstance : this.installedInstances) {
            if (matchesFilter(launcherInstance)) {
                arrayList.add(launcherInstance);
            }
        }
        if (FILTER_RECENT.equals(this.selectedFilter)) {
            arrayList.sort(new Comparator() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda25
                @Override // java.util.Comparator
                public final int compare(Object obj, Object obj2) {
                    return MainActivity.this.lambda$applyInstanceFilter$20((LauncherInstance) obj, (LauncherInstance) obj2);
                }
            });
        }
        this.instanceAdapter.submitList(arrayList);
        this.binding.textVersionCount.setText(getString(R.string.instance_count_value, new Object[]{Integer.valueOf(arrayList.size())}));
        if (arrayList.isEmpty()) {
            return;
        }
        this.binding.recyclerVersions.scrollToPosition(0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ int lambda$applyInstanceFilter$20(LauncherInstance launcherInstance, LauncherInstance launcherInstance2) {
        return Long.compare(LauncherPreferences.getInstanceLastPlayed(this, launcherInstance2.getId()), LauncherPreferences.getInstanceLastPlayed(this, launcherInstance.getId()));
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Failed to restore switch over string. Please report as a decompilation issue */
    /* JADX WARN: Removed duplicated region for block: B:23:0x004b  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private boolean matchesFilter(ca.dnamobile.javalauncher.instance.LauncherInstance r10) {
        /*
            r9 = this;
            java.lang.String r0 = r9.selectedFilter
            int r1 = r0.hashCode()
            java.lang.String r2 = "snapshot"
            r3 = 4
            r4 = 3
            r5 = 2
            r6 = 0
            java.lang.String r7 = "vanilla"
            r8 = 1
            switch(r1) {
                case -934918565: goto L41;
                case -903566235: goto L37;
                case -615513399: goto L2d;
                case 96673: goto L23;
                case 233102203: goto L1b;
                case 284874180: goto L13;
                default: goto L12;
            }
        L12:
            goto L4b
        L13:
            boolean r0 = r0.equals(r2)
            if (r0 == 0) goto L4b
            r0 = r4
            goto L4c
        L1b:
            boolean r0 = r0.equals(r7)
            if (r0 == 0) goto L4b
            r0 = r8
            goto L4c
        L23:
            java.lang.String r1 = "all"
            boolean r0 = r0.equals(r1)
            if (r0 == 0) goto L4b
            r0 = 5
            goto L4c
        L2d:
            java.lang.String r1 = "modified"
            boolean r0 = r0.equals(r1)
            if (r0 == 0) goto L4b
            r0 = r5
            goto L4c
        L37:
            java.lang.String r1 = "shared"
            boolean r0 = r0.equals(r1)
            if (r0 == 0) goto L4b
            r0 = r3
            goto L4c
        L41:
            java.lang.String r1 = "recent"
            boolean r0 = r0.equals(r1)
            if (r0 == 0) goto L4b
            r0 = r6
            goto L4c
        L4b:
            r0 = -1
        L4c:
            if (r0 == 0) goto L81
            if (r0 == r8) goto L78
            if (r0 == r5) goto L6e
            if (r0 == r4) goto L65
            if (r0 == r3) goto L57
            return r8
        L57:
            boolean r0 = ca.dnamobile.javalauncher.settings.LauncherPreferences.isShowSharedInstalls(r9)
            if (r0 == 0) goto L64
            boolean r10 = r10.isIsolated()
            if (r10 != 0) goto L64
            r6 = r8
        L64:
            return r6
        L65:
            java.lang.String r10 = r10.getVersionType()
            boolean r10 = r2.equalsIgnoreCase(r10)
            return r10
        L6e:
            java.lang.String r10 = r10.getLoader()
            boolean r10 = r7.equalsIgnoreCase(r10)
            r10 = r10 ^ r8
            return r10
        L78:
            java.lang.String r10 = r10.getLoader()
            boolean r10 = r7.equalsIgnoreCase(r10)
            return r10
        L81:
            java.lang.String r10 = r10.getId()
            long r0 = ca.dnamobile.javalauncher.settings.LauncherPreferences.getInstanceLastPlayed(r9, r10)
            r2 = 0
            int r10 = (r0 > r2 ? 1 : (r0 == r2 ? 0 : -1))
            if (r10 <= 0) goto L90
            r6 = r8
        L90:
            return r6
        */
        throw new UnsupportedOperationException("Method not decompiled: ca.dnamobile.javalauncher.MainActivity.matchesFilter(ca.dnamobile.javalauncher.instance.LauncherInstance):boolean");
    }

    private void setupMainContentButtons() {
        View.OnClickListener onClickListener = new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda4
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.lambda$setupMainContentButtons$21(view);
            }
        };
        View.OnClickListener onClickListener2 = new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda5
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.lambda$setupMainContentButtons$22(view);
            }
        };
        View viewFindOrCreateMainActionButton = findOrCreateMainActionButton("buttonBrowseContentMain", TAG_BROWSE_CONTENT_MAIN_BUTTON, "Browse Modpacks", onClickListener, true);
        if (viewFindOrCreateMainActionButton != null) {
            viewFindOrCreateMainActionButton.setOnClickListener(onClickListener);
            if (viewFindOrCreateMainActionButton instanceof TextView) {
                ((TextView) viewFindOrCreateMainActionButton).setText("Browse Modpacks");
            }
        }
        View viewFindOrCreateMainActionButton2 = findOrCreateMainActionButton("buttonImportModpackMain", TAG_IMPORT_MODPACK_MAIN_BUTTON, "Import Modpack", onClickListener2, true);
        if (viewFindOrCreateMainActionButton2 != null) {
            viewFindOrCreateMainActionButton2.setOnClickListener(onClickListener2);
        }
        updateMainContentButtonVisibility(hasActiveMicrosoftAccount());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupMainContentButtons$21(View view) {
        if (!hasActiveMicrosoftAccount()) {
            lambda$requireMicrosoftLoginHistoryBeforeLaunch$7(new Runnable() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda18
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.openGlobalContentBrowser();
                }
            });
        } else {
            openGlobalContentBrowser();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupMainContentButtons$22(View view) {
        if (!hasActiveMicrosoftAccount()) {
            lambda$requireMicrosoftLoginHistoryBeforeLaunch$7(new Runnable() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda19
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.openModpackImportPicker();
                }
            });
        } else {
            openModpackImportPicker();
        }
    }

    private View findOrCreateMainActionButton(String str, String str2, String str3, View.OnClickListener onClickListener, boolean z) {
        View viewFindMainActionButton = findMainActionButton(str, str2);
        if (viewFindMainActionButton != null) {
            viewFindMainActionButton.setOnClickListener(onClickListener);
            return viewFindMainActionButton;
        }
        if (!(this.binding.buttonRefreshVersions.getParent() instanceof ViewGroup)) {
            Logging.i("MainActivity", "Unable to add main action button because refresh parent is unavailable: " + str3);
            return null;
        }
        ViewGroup viewGroup = (ViewGroup) this.binding.buttonRefreshVersions.getParent();
        MaterialButton materialButton = new MaterialButton(this);
        materialButton.setTag(str2);
        materialButton.setText(str3);
        materialButton.setSingleLine(true);
        materialButton.setAllCaps(false);
        materialButton.setMinHeight(dp(40.0f));
        materialButton.setMinWidth(dp(96.0f));
        materialButton.setOnClickListener(onClickListener);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, -2);
        layoutParams.leftMargin = dp(8.0f);
        int iIndexOfChild = viewGroup.indexOfChild(this.binding.buttonRefreshVersions);
        if (!z || iIndexOfChild < 0) {
            iIndexOfChild = iIndexOfChild >= 0 ? iIndexOfChild + 1 : viewGroup.getChildCount();
        }
        viewGroup.addView(materialButton, Math.max(0, Math.min(iIndexOfChild, viewGroup.getChildCount())), layoutParams);
        return materialButton;
    }

    private View findMainActionButton(String str, String str2) {
        View viewFindViewById;
        ActivityMainBinding activityMainBinding = this.binding;
        if (activityMainBinding == null || activityMainBinding.getRoot() == null) {
            return null;
        }
        int identifier = getResources().getIdentifier(str, "id", getPackageName());
        if (identifier != 0 && (viewFindViewById = this.binding.getRoot().findViewById(identifier)) != null) {
            return viewFindViewById;
        }
        if (this.binding.buttonRefreshVersions.getParent() instanceof ViewGroup) {
            return ((ViewGroup) this.binding.buttonRefreshVersions.getParent()).findViewWithTag(str2);
        }
        return this.binding.getRoot().findViewWithTag(str2);
    }

    private void updateMainContentButtonVisibility(boolean z) {
        if (this.binding == null) {
            return;
        }
        View viewFindMainActionButton = findMainActionButton("buttonBrowseContentMain", TAG_BROWSE_CONTENT_MAIN_BUTTON);
        if (viewFindMainActionButton != null) {
            viewFindMainActionButton.setVisibility(z ? 0 : 8);
            viewFindMainActionButton.setEnabled(z);
        }
        View viewFindMainActionButton2 = findMainActionButton("buttonImportModpackMain", TAG_IMPORT_MODPACK_MAIN_BUTTON);
        if (viewFindMainActionButton2 != null) {
            viewFindMainActionButton2.setVisibility(z ? 0 : 8);
            viewFindMainActionButton2.setEnabled(z);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void openGlobalContentBrowser() {
        Intent intent = new Intent(this, (Class<?>) ContentBrowserActivity.class);
        intent.putExtra(InstanceDetailsActivity.EXTRA_CONTENT_CATEGORY, "modpacks");
        startActivity(intent);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void openModpackImportPicker() {
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
        if (ControlsMain.toastAndBlockIfInvalidSignature(this)) {
            return;
        }
        setLoading(true);
        showInstallDialog("Modpack");
        beginInstallSession("Modpack");
        final AnonymousClass4 anonymousClass4 = new AnonymousClass4();
        new Thread(new Runnable() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda11
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.lambda$importModpackFromUri$23(uri, anonymousClass4);
            }
        }, "Import Modpack").start();
    }

    /* JADX INFO: renamed from: ca.dnamobile.javalauncher.MainActivity$4, reason: invalid class name */
    class AnonymousClass4 implements ModpackInstallManager.Listener {
        AnonymousClass4() {
        }

        @Override // ca.dnamobile.javalauncher.modmanager.ModpackInstallManager.Listener
        public void onStatus(String str) {
            MainActivity mainActivity = MainActivity.this;
            mainActivity.dispatchInstallProgress(mainActivity.activeInstallProgress, str);
        }

        @Override // ca.dnamobile.javalauncher.modmanager.ModpackInstallManager.Listener
        public void onProgress(int i, int i2) {
            int iMax = i2 > 0 ? Math.max(0, Math.min(100, (int) ((((long) i) * 100) / ((long) i2)))) : 0;
            MainActivity mainActivity = MainActivity.this;
            mainActivity.dispatchInstallProgress(iMax, mainActivity.activeInstallMessage);
        }

        @Override // ca.dnamobile.javalauncher.modmanager.ModpackInstallManager.Listener
        public void onComplete(String str) {
            onComplete(str, null);
        }

        @Override // ca.dnamobile.javalauncher.modmanager.ModpackInstallManager.Listener
        public void onComplete(final String str, final LauncherInstance launcherInstance) {
            MainActivity.this.runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.MainActivity$4$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.lambda$onComplete$0(str, launcherInstance);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onComplete$0(String str, LauncherInstance launcherInstance) {
            MainActivity.this.setLoading(false);
            MainActivity.this.finishInstallSession();
            MainActivity.this.dismissInstallDialog();
            MainActivity.this.refreshInstancesAndRebind(false);
            MainActivity.this.selectedFilter = MainActivity.FILTER_ALL;
            MainActivity.this.selectTabByFilter(MainActivity.FILTER_ALL);
            MainActivity.this.setStatus(str);
            Toast.makeText(MainActivity.this, str, 1).show();
            if (launcherInstance != null) {
                MainActivity mainActivity = MainActivity.this;
                mainActivity.startActivity(InstanceDetailsActivity.createIntent(mainActivity, launcherInstance));
            }
        }

        @Override // ca.dnamobile.javalauncher.modmanager.ModpackInstallManager.Listener
        public void onError(final Throwable th) {
            MainActivity.this.runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.MainActivity$4$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.lambda$onError$1(th);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onError$1(Throwable th) {
            MainActivity.this.setLoading(false);
            MainActivity.this.finishInstallSession();
            MainActivity.this.dismissInstallDialog();
            String message = th.getMessage() != null ? th.getMessage() : th.getClass().getSimpleName();
            MainActivity.this.setStatus("Modpack import failed: " + message);
            Toast.makeText(MainActivity.this, "Modpack import failed: " + message, 1).show();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$importModpackFromUri$23(Uri uri, ModpackInstallManager.Listener listener) {
        ModpackInstallManager.importFromUri(this, uri, listener);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupStorageLocationsButton$24(View view) {
        showStorageLocationsDialog();
    }

    private void setupStorageLocationsButton() {
        View viewFindViewById;
        View.OnClickListener onClickListener = new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda37
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.lambda$setupStorageLocationsButton$24(view);
            }
        };
        int identifier = getResources().getIdentifier("buttonStorageLocations", "id", getPackageName());
        if (identifier != 0 && (viewFindViewById = this.binding.getRoot().findViewById(identifier)) != null) {
            viewFindViewById.setOnClickListener(onClickListener);
            return;
        }
        if (!(this.binding.buttonRefreshVersions.getParent() instanceof ViewGroup)) {
            Logging.i("MainActivity", "Storage location button missing and refresh parent is unavailable");
            return;
        }
        ViewGroup viewGroup = (ViewGroup) this.binding.buttonRefreshVersions.getParent();
        View viewFindViewWithTag = viewGroup.findViewWithTag("storage_locations_button_dynamic");
        if (viewFindViewWithTag != null) {
            viewFindViewWithTag.setOnClickListener(onClickListener);
            return;
        }
        MaterialButton materialButton = new MaterialButton(this);
        materialButton.setTag("storage_locations_button_dynamic");
        materialButton.setText("");
        materialButton.setIconResource(R.drawable.ic_folder_24);
        materialButton.setIconPadding(0);
        materialButton.setContentDescription(getString(R.string.storage_locations_title));
        materialButton.setMinWidth(dp(48.0f));
        materialButton.setMinHeight(dp(40.0f));
        materialButton.setOnClickListener(onClickListener);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, -2);
        layoutParams.leftMargin = dp(8.0f);
        int iIndexOfChild = viewGroup.indexOfChild(this.binding.buttonRefreshVersions);
        viewGroup.addView(materialButton, Math.min(iIndexOfChild >= 0 ? iIndexOfChild + 1 : viewGroup.getChildCount(), viewGroup.getChildCount()), layoutParams);
    }

    private int dp(float f) {
        return (int) ((f * getResources().getDisplayMetrics().density) + 0.5f);
    }

    private void showStorageLocationsDialog() {
        StorageLocationDialog.show(this, new StorageLocationDialog.Listener() { // from class: ca.dnamobile.javalauncher.MainActivity.5
            @Override // ca.dnamobile.javalauncher.storage.StorageLocationDialog.Listener
            public void onLocationSelected(StorageLocation storageLocation) {
                StorageLocationStore.setSelectedLocationId(MainActivity.this, storageLocation.getId());
                MainActivity mainActivity = MainActivity.this;
                mainActivity.refreshAfterStorageLocationSelection(mainActivity.getString(R.string.storage_location_selected, new Object[]{storageLocation.getDisplayName()}), false);
            }

            @Override // ca.dnamobile.javalauncher.storage.StorageLocationDialog.Listener
            public void onAddLocationRequested() {
                MainActivity.this.openStorageLocationPicker();
            }

            @Override // ca.dnamobile.javalauncher.storage.StorageLocationDialog.Listener
            public void onDeleteLocationRequested(StorageLocation storageLocation) {
                MainActivity.this.showDeleteStorageLocationDialog(storageLocation);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showDeleteStorageLocationDialog(final StorageLocation storageLocation) {
        if (storageLocation.isDefaultLocation()) {
            Toast.makeText(this, R.string.storage_location_default_cannot_delete, 0).show();
            showStorageLocationsDialog();
        } else {
            new AlertDialog.Builder(this).setTitle(getString(R.string.storage_location_delete_title, new Object[]{storageLocation.getDisplayName()})).setMessage(getString(R.string.storage_location_delete_message, new Object[]{storageLocation.getDisplayName()})).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda48
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    MainActivity.this.lambda$showDeleteStorageLocationDialog$25(dialogInterface, i);
                }
            }).setPositiveButton(R.string.storage_location_delete_confirm, new DialogInterface.OnClickListener() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda49
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    MainActivity.this.lambda$showDeleteStorageLocationDialog$26(storageLocation, dialogInterface, i);
                }
            }).show();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showDeleteStorageLocationDialog$25(DialogInterface dialogInterface, int i) {
        showStorageLocationsDialog();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showDeleteStorageLocationDialog$26(StorageLocation storageLocation, DialogInterface dialogInterface, int i) {
        if (!StorageLocationStore.removeLocation(this, storageLocation.getId())) {
            Toast.makeText(this, R.string.storage_location_delete_failed, 1).show();
            showStorageLocationsDialog();
        } else {
            refreshAfterStorageLocationChange();
            setStatus(getString(R.string.storage_location_deleted, new Object[]{storageLocation.getDisplayName()}));
            Toast.makeText(this, getString(R.string.storage_location_deleted, new Object[]{storageLocation.getDisplayName()}), 0).show();
            showStorageLocationsDialog();
        }
    }

    private void refreshAfterStorageLocationChange() {
        refreshAfterStorageLocationChange(null, false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void refreshAfterStorageLocationSelection(String str, boolean z) {
        refreshStorageLocationForAdapter(str, z);
    }

    private void refreshAfterStorageLocationChange(String str, boolean z) {
        refreshStorageLocationForAdapter(str, z);
    }

    private void refreshStorageLocationForAdapter(final String str, final boolean z) {
        this.selectedInstance = null;
        LauncherInstanceAdapter launcherInstanceAdapter = this.instanceAdapter;
        if (launcherInstanceAdapter != null) {
            launcherInstanceAdapter.clearSelectedInstance();
        }
        if (!StorageLocationStore.isSelectedScopedStorage(this)) {
            PathManager.initContextConstants(this);
            this.binding.textFolder.setText(getString(R.string.launcher_folder_value, new Object[]{PathManager.DIR_MINECRAFT_HOME}));
            refreshInstancesAndRebind(false);
            if (str != null) {
                setStatus(str);
            }
            if (z) {
                showStorageLocationsDialog();
                return;
            }
            return;
        }
        setLoading(true);
        setStatus("Reading launcher metadata...");
        new Thread(new Runnable() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda31
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.lambda$refreshStorageLocationForAdapter$28(str, z);
            }
        }, "ScopedStorageMetadataRefresh").start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$refreshStorageLocationForAdapter$28(final String str, final boolean z) {
        final Throwable th = null;
        try {
            StorageLocationStore.syncSelectedTreeMetadataToMirror(this, null);
        } catch (Throwable th2) {
            th = th2;
            Logging.i("ScopedStorage", "Unable to read scoped-storage metadata: " + th.getMessage());
        }
        runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda58
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.lambda$refreshStorageLocationForAdapter$27(th, str, z);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$refreshStorageLocationForAdapter$27(Throwable th, String str, boolean z) {
        String simpleName;
        PathManager.initContextConstants(this);
        this.binding.textFolder.setText(getString(R.string.launcher_folder_value, new Object[]{PathManager.DIR_MINECRAFT_HOME}));
        refreshInstancesAndRebind(false);
        setLoading(false);
        if (th != null) {
            if (th.getMessage() != null) {
                simpleName = th.getMessage();
            } else {
                simpleName = th.getClass().getSimpleName();
            }
            setStatus("Storage selected, but metadata read failed: " + simpleName);
        } else if (str != null) {
            setStatus(str);
        }
        if (z) {
            showStorageLocationsDialog();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void openStorageLocationPicker() {
        Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT_TREE");
        intent.addFlags(195);
        try {
            startActivityForResult(intent, REQUEST_ADD_STORAGE_LOCATION);
        } catch (ActivityNotFoundException unused) {
            setStatus(getString(R.string.storage_picker_unavailable));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showCreateInstanceDialog() {
        if (requireActiveMicrosoftAccountBeforeCreateInstance(new MainActivity$$ExternalSyntheticLambda2(this))) {
            return;
        }
        if (this.allVersions.isEmpty()) {
            Toast.makeText(this, R.string.msg_fetching_versions, 0).show();
            loadVersions(true);
        } else {
            CreateInstanceDialog createInstanceDialog = new CreateInstanceDialog(this, this.allVersions, new CreateInstanceDialog.Listener() { // from class: ca.dnamobile.javalauncher.MainActivity.6
                @Override // ca.dnamobile.javalauncher.ui.instance.CreateInstanceDialog.Listener
                public void onPickIcon(CreateInstanceDialog createInstanceDialog2) {
                    MainActivity.this.pickInstanceIcon();
                }

                @Override // ca.dnamobile.javalauncher.ui.instance.CreateInstanceDialog.Listener
                public void onCreateInstance(CreateInstanceDialog.Request request) {
                    MainActivity.this.lambda$createInstanceFromRequest$29(request);
                }
            });
            this.createInstanceDialog = createInstanceDialog;
            createInstanceDialog.setExistingInstanceNames(collectExistingInstanceNamesForDialog());
            this.createInstanceDialog.show();
        }
    }

    private ArrayList<String> collectExistingInstanceNamesForDialog() {
        ArrayList<String> arrayList = new ArrayList<>();
        HashSet<String> hashSet = new HashSet<>();
        Iterator<LauncherInstance> it = this.installedInstances.iterator();
        while (it.hasNext()) {
            addInstanceNameForDuplicateCheck(arrayList, hashSet, it.next().getName());
        }
        return arrayList;
    }

    private void addInstanceNameForDuplicateCheck(ArrayList<String> arrayList, HashSet<String> hashSet, String str) {
        String strNormalizeInstanceNameKey = normalizeInstanceNameKey(str);
        if (strNormalizeInstanceNameKey.isEmpty() || !hashSet.add(strNormalizeInstanceNameKey)) {
            return;
        }
        arrayList.add(str.trim());
    }

    private static String normalizeInstanceNameKey(String str) {
        if (str == null) {
            return "";
        }
        return str.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isolatedInstanceNameExists(String str) {
        String strNormalizeInstanceNameKey = normalizeInstanceNameKey(str);
        if (strNormalizeInstanceNameKey.isEmpty()) {
            return false;
        }
        for (LauncherInstance launcherInstance : this.installedInstances) {
            if (launcherInstance.isIsolated() && strNormalizeInstanceNameKey.equals(normalizeInstanceNameKey(launcherInstance.getName()))) {
                return true;
            }
        }
        return false;
    }

    private void showDuplicateInstanceNameMessage(String str) {
        String string = getString(R.string.create_instance_name_already_exists, new Object[]{str});
        setStatus(string);
        Toast.makeText(this, string, 1).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void pickInstanceIcon() {
        Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT");
        intent.setType("image/*");
        intent.addCategory("android.intent.category.OPENABLE");
        intent.addFlags(65);
        try {
            startActivityForResult(intent, REQUEST_PICK_INSTANCE_ICON);
        } catch (ActivityNotFoundException unused) {
            Toast.makeText(this, R.string.create_instance_icon_picker_missing, 0).show();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: renamed from: createInstanceFromRequest, reason: merged with bridge method [inline-methods] */
    public void lambda$createInstanceFromRequest$29(final CreateInstanceDialog.Request request) {
        if (ControlsMain.toastAndBlockIfInvalidSignature(this) || requireActiveMicrosoftAccountBeforeCreateInstance(new Runnable() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda54
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.lambda$createInstanceFromRequest$29(request);
            }
        })) {
            return;
        }
        if (request.isolatedInstance && isolatedInstanceNameExists(request.name)) {
            showDuplicateInstanceNameMessage(request.name);
            refreshInstancesAndRebind(true);
            return;
        }
        final MinecraftVersion minecraftVersionFindManifestVersionById = findManifestVersionById(request.minecraftVersionId);
        if (minecraftVersionFindManifestVersionById == null) {
            Toast.makeText(this, R.string.create_instance_version_missing, 1).show();
            return;
        }
        setLoading(true);
        this.binding.buttonLaunchVersion.setEnabled(false);
        String str = request.isolatedInstance ? request.name : request.minecraftVersionId;
        showInstallDialog(str);
        beginInstallSession(str);
        setStatus(getString(request.isolatedInstance ? R.string.create_instance_installing : R.string.create_instance_installing_shared, new Object[]{request.isolatedInstance ? request.name : request.minecraftVersionId}));
        final MinecraftVersionInstaller.InstallProgressListener installProgressListener = new MinecraftVersionInstaller.InstallProgressListener() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda55
            @Override // ca.dnamobile.javalauncher.ui.version.MinecraftVersionInstaller.InstallProgressListener
            public final void onProgress(int i, String str2) {
                MainActivity.this.dispatchInstallProgress(i, str2);
            }
        };
        new Thread(new Runnable() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda56
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.lambda$createInstanceFromRequest$33(installProgressListener, minecraftVersionFindManifestVersionById, request);
            }
        }, "Create Launcher Instance").start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$createInstanceFromRequest$33(MinecraftVersionInstaller.InstallProgressListener installProgressListener, MinecraftVersion minecraftVersion, CreateInstanceDialog.Request request) {
        try {
            PathManager.initContextConstants(this);
            if (StorageLocationStore.isSelectedScopedStorage(this)) {
                installProgressListener.onProgress(1, "Using scoped-storage compatibility mirror...");
            }
            MinecraftVersionInstaller.installVanillaVersion(this, minecraftVersion, installProgressListener);
            String id = minecraftVersion.getId();
            String str = request.loader;
            if ("Fabric".equalsIgnoreCase(str)) {
                id = FabricInstaller.installFabricVersion(this, minecraftVersion, request.loaderVersion, installProgressListener).getFabricVersionId();
            } else if ("Forge".equalsIgnoreCase(str)) {
                id = ForgeInstaller.installForgeVersion(this, minecraftVersion, request.name, request.loaderVersion, installProgressListener).getForgeVersionId();
            } else if ("NeoForge".equalsIgnoreCase(str)) {
                id = NeoForgeInstaller.installNeoForgeVersion(this, minecraftVersion, request.name, request.loaderVersion, installProgressListener).getNeoForgeVersionId();
            }
            final String str2 = id;
            if (!request.isolatedInstance) {
                ensureModsDirectoryForLoader(request.loader, null);
                syncSelectedStorageMirrorToTree(installProgressListener);
                LauncherPreferences.setShowSharedInstalls(this, true);
                runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda45
                    @Override // java.lang.Runnable
                    public final void run() {
                        MainActivity.this.lambda$createInstanceFromRequest$30(str2);
                    }
                });
                return;
            }
            final LauncherInstance launcherInstanceCreateInstance = LauncherInstanceManager.createInstance(this, request.name, request.loader, str2, request.minecraftVersionId, request.versionType, request.iconUri);
            ensureModsDirectoryForLoader(request.loader, launcherInstanceCreateInstance);
            syncSelectedStorageMirrorToTree(installProgressListener);
            runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda46
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.lambda$createInstanceFromRequest$31(launcherInstanceCreateInstance);
                }
            });
        } catch (Throwable th) {
            Logging.e("CreateInstance", "Unable to create launcher instance", th);
            runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda47
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.lambda$createInstanceFromRequest$32(th);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$createInstanceFromRequest$30(String str) {
        if (this.binding.switchShowSharedInstalls != null) {
            this.binding.switchShowSharedInstalls.setChecked(true);
        }
        refreshInstancesAndRebind(false);
        this.selectedFilter = FILTER_SHARED;
        selectTabByFilter(FILTER_SHARED);
        LauncherInstance launcherInstanceFindInstanceById = findInstanceById(LauncherInstance.sharedInstanceId(str, new File(PathManager.DIR_MINECRAFT_HOME)));
        this.selectedInstance = launcherInstanceFindInstanceById;
        this.instanceAdapter.setSelectedInstance(launcherInstanceFindInstanceById);
        applyInstanceFilter();
        updateSelectedInstanceCard();
        setLoading(false);
        finishInstallSession();
        dismissInstallDialog();
        setStatus(getString(R.string.create_instance_shared_complete, new Object[]{str}));
        Toast.makeText(this, getString(R.string.create_instance_shared_complete, new Object[]{str}), 0).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$createInstanceFromRequest$31(LauncherInstance launcherInstance) {
        refreshInstancesAndRebind(false);
        this.selectedFilter = FILTER_ALL;
        selectTabByFilter(FILTER_ALL);
        this.selectedInstance = launcherInstance;
        this.instanceAdapter.setSelectedInstance(launcherInstance);
        applyInstanceFilter();
        updateSelectedInstanceCard();
        setLoading(false);
        finishInstallSession();
        dismissInstallDialog();
        setStatus(getString(R.string.create_instance_complete, new Object[]{launcherInstance.getName()}));
        Toast.makeText(this, getString(R.string.create_instance_complete, new Object[]{launcherInstance.getName()}), 0).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$createInstanceFromRequest$32(Throwable th) {
        setLoading(false);
        finishInstallSession();
        dismissInstallDialog();
        updateSelectedInstanceCard();
        setStatus(getString(R.string.msg_version_install_failed, new Object[]{th.getMessage() != null ? th.getMessage() : th.getClass().getSimpleName()}));
    }

    private void syncSelectedStorageMirrorToTree(final MinecraftVersionInstaller.InstallProgressListener installProgressListener) throws Exception {
        if (StorageLocationStore.isSelectedScopedStorage(this)) {
            installProgressListener.onProgress(98, "Saving files to selected folder...");
            StorageLocationStore.syncSelectedMirrorToTree(this, new SafMinecraftMirror.Progress() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda33
                @Override // ca.dnamobile.javalauncher.storage.SafMinecraftMirror.Progress
                public final void onProgress(int i, String str) {
                    installProgressListener.onProgress(Math.max(98, Math.min(99, i)), str);
                }
            });
        }
    }

    private void ensureModsDirectoryForLoader(String str, LauncherInstance launcherInstance) {
        File file;
        if ("Fabric".equalsIgnoreCase(str) || "Forge".equalsIgnoreCase(str) || "NeoForge".equalsIgnoreCase(str)) {
            if (launcherInstance != null) {
                file = new File(launcherInstance.getGameDirectory(), "mods");
            } else {
                file = new File(PathManager.DIR_MINECRAFT_HOME, "mods");
            }
            if (file.exists() || file.mkdirs()) {
                return;
            }
            Logging.i("CreateInstance", "Unable to create mods folder: " + file.getAbsolutePath());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void selectTabByFilter(String str) {
        String strSanitizeSavedFilter = sanitizeSavedFilter(str);
        this.selectedFilter = strSanitizeSavedFilter;
        LauncherPreferences.setSelectedInstanceFilter(this, strSanitizeSavedFilter);
        for (int i = 0; i < this.binding.tabVersionTypes.getTabCount(); i++) {
            TabLayout.Tab tabAt = this.binding.tabVersionTypes.getTabAt(i);
            if (tabAt != null && strSanitizeSavedFilter.equals(tabAt.getTag())) {
                tabAt.select();
                return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String sanitizeSavedFilter(String str) {
        if (str == null) {
            return FILTER_ALL;
        }
        str.hashCode();
        switch (str) {
        }
        return FILTER_ALL;
    }

    private MinecraftVersion findManifestVersionById(String str) {
        for (MinecraftVersion minecraftVersion : this.allVersions) {
            if (str.equals(minecraftVersion.getId())) {
                return minecraftVersion;
            }
        }
        return null;
    }

    private void registerNotificationPermissionLauncher() {
        this.notificationPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda36
            @Override // androidx.activity.result.ActivityResultCallback
            public final void onActivityResult(Object obj) {
                MainActivity.this.lambda$registerNotificationPermissionLauncher$35((Boolean) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$registerNotificationPermissionLauncher$35(Boolean bool) {
        CheckBox checkBox;
        CheckBox checkBox2;
        if (bool.booleanValue()) {
            LauncherNotificationPermissionHelper.setBackgroundInstallNotificationsEnabled(this, true);
            if (this.installSessionActive && (checkBox2 = this.installDialogForegroundCheck) != null && checkBox2.isChecked()) {
                startOrUpdateInstallForegroundService(this.activeInstallProgress <= 0);
            }
            Toast.makeText(this, R.string.notification_permission_enabled_toast, 0).show();
            return;
        }
        LauncherNotificationPermissionHelper.setBackgroundInstallNotificationsEnabled(this, false);
        if (this.installSessionActive && (checkBox = this.installDialogForegroundCheck) != null) {
            checkBox.setChecked(false);
        }
        Toast.makeText(this, R.string.notification_permission_denied_toast, 1).show();
    }

    private void maybeShowNotificationPermissionLaunchPrompt() {
        if (LauncherNotificationPermissionHelper.shouldShowLaunchPrompt(this)) {
            LauncherNotificationPermissionHelper.markLaunchPromptShown(this);
            new AlertDialog.Builder(this).setTitle(R.string.notification_permission_launch_title).setMessage(R.string.notification_permission_launch_message).setNegativeButton(R.string.notification_permission_not_now, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.notification_permission_allow, new DialogInterface.OnClickListener() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda6
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    MainActivity.this.lambda$maybeShowNotificationPermissionLaunchPrompt$36(dialogInterface, i);
                }
            }).show();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$maybeShowNotificationPermissionLaunchPrompt$36(DialogInterface dialogInterface, int i) {
        requestNotificationPermissionIfPossible();
    }

    private void requestNotificationPermissionIfPossible() {
        ActivityResultLauncher<String> activityResultLauncher = this.notificationPermissionLauncher;
        if (activityResultLauncher == null) {
            return;
        }
        LauncherNotificationPermissionHelper.requestPostNotificationsPermission(activityResultLauncher);
    }

    private void beginInstallSession(String str) {
        this.installSessionActive = true;
        this.installPermissionPromptShownThisSession = false;
        this.activeInstallTitle = "Installing " + str;
        this.activeInstallMessage = "Preparing installation...";
        this.activeInstallProgress = 0;
        resetInstallProgressThrottles();
        getWindow().addFlags(128);
        startOrUpdateInstallForegroundService(true);
    }

    private void resetInstallProgressThrottles() {
        this.lastInstallUiDispatchMs = 0L;
        this.lastInstallUiDispatchProgress = -1;
        this.lastInstallUiDispatchMessage = "";
        this.lastInstallNotificationUpdateMs = 0L;
        this.lastInstallNotificationProgress = -1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Removed duplicated region for block: B:15:0x0044  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void dispatchInstallProgress(int r13, final java.lang.String r14) {
        /*
            r12 = this;
            r0 = 100
            int r13 = java.lang.Math.min(r0, r13)
            r1 = 0
            int r13 = java.lang.Math.max(r1, r13)
            java.lang.String r2 = r14.trim()
            boolean r2 = r2.isEmpty()
            if (r2 == 0) goto L17
            java.lang.String r14 = r12.activeInstallMessage
        L17:
            long r2 = android.os.SystemClock.uptimeMillis()
            java.lang.String r4 = r12.lastInstallUiDispatchMessage
            boolean r4 = r14.equals(r4)
            r5 = 1
            if (r13 <= 0) goto L44
            if (r13 >= r0) goto L44
            int r0 = r12.lastInstallUiDispatchProgress
            int r0 = r13 - r0
            int r0 = java.lang.Math.abs(r0)
            if (r0 >= r5) goto L44
            long r6 = r12.lastInstallUiDispatchMs
            long r8 = r2 - r6
            r10 = 350(0x15e, double:1.73E-321)
            int r0 = (r8 > r10 ? 1 : (r8 == r10 ? 0 : -1))
            if (r0 >= 0) goto L44
            if (r4 != 0) goto L45
            long r6 = r2 - r6
            r8 = 850(0x352, double:4.2E-321)
            int r0 = (r6 > r8 ? 1 : (r6 == r8 ? 0 : -1))
            if (r0 < 0) goto L45
        L44:
            r1 = r5
        L45:
            r12.activeInstallProgress = r13
            r12.activeInstallMessage = r14
            if (r1 != 0) goto L4c
            return
        L4c:
            r12.lastInstallUiDispatchMs = r2
            r12.lastInstallUiDispatchProgress = r13
            r12.lastInstallUiDispatchMessage = r14
            android.os.Handler r0 = r12.mainHandler
            ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda57 r1 = new ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda57
            r1.<init>()
            r0.post(r1)
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: ca.dnamobile.javalauncher.MainActivity.dispatchInstallProgress(int, java.lang.String):void");
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: renamed from: updateInstallProgress, reason: merged with bridge method [inline-methods] */
    public void lambda$dispatchInstallProgress$37(int i, String str) {
        if (this.binding == null) {
            return;
        }
        int iMax = Math.max(0, Math.min(100, i));
        this.activeInstallProgress = iMax;
        this.activeInstallMessage = str;
        this.binding.progressVersions.setIndeterminate(false);
        this.binding.progressVersions.setMax(100);
        this.binding.progressVersions.setProgress(iMax);
        updateInstallDialog(iMax, str);
        setStatus(str);
        if (this.installSessionActive) {
            startOrUpdateInstallForegroundServiceThrottled(false);
        }
    }

    private void startOrUpdateInstallForegroundServiceThrottled(boolean z) {
        int i;
        long jUptimeMillis = SystemClock.uptimeMillis();
        if (z || (i = this.activeInstallProgress) <= 0 || i >= 100 || Math.abs(i - this.lastInstallNotificationProgress) >= 2 || jUptimeMillis - this.lastInstallNotificationUpdateMs >= INSTALL_NOTIFICATION_UPDATE_INTERVAL_MS) {
            this.lastInstallNotificationUpdateMs = jUptimeMillis;
            this.lastInstallNotificationProgress = this.activeInstallProgress;
            startOrUpdateInstallForegroundService(z);
        }
    }

    private void startOrUpdateInstallForegroundService(boolean z) {
        if (this.installSessionActive) {
            CheckBox checkBox = this.installDialogForegroundCheck;
            if ((checkBox == null || checkBox.isChecked()) && LauncherNotificationPermissionHelper.isBackgroundInstallNotificationsEnabled(this)) {
                if (!LauncherNotificationPermissionHelper.hasPostNotificationsPermission(this)) {
                    if (this.installPermissionPromptShownThisSession) {
                        return;
                    }
                    this.installPermissionPromptShownThisSession = true;
                    requestNotificationPermissionIfPossible();
                    return;
                }
                InstallationForegroundService.update(this, this.activeInstallTitle, this.activeInstallMessage, this.activeInstallProgress, z);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void finishInstallSession() {
        this.installSessionActive = false;
        this.installPermissionPromptShownThisSession = false;
        getWindow().clearFlags(128);
        InstallationForegroundService.stop(this);
        resetInstallProgressThrottles();
    }

    private void showInstallDialog(String str) {
        LinearLayout linearLayout = new LinearLayout(this);
        int i = (int) (getResources().getDisplayMetrics().density * 24.0f);
        linearLayout.setOrientation(1);
        linearLayout.setPadding(i, i / 2, i, 0);
        TextView textView = new TextView(this);
        this.installDialogMessage = textView;
        textView.setText(getString(R.string.create_instance_installing, new Object[]{str}));
        ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        this.installDialogProgress = progressBar;
        progressBar.setMax(100);
        this.installDialogProgress.setProgress(0);
        this.installDialogProgress.setIndeterminate(true);
        CheckBox checkBox = new CheckBox(this);
        this.installDialogForegroundCheck = checkBox;
        checkBox.setText(R.string.install_dialog_background_notifications);
        this.installDialogForegroundCheck.setChecked(LauncherNotificationPermissionHelper.isBackgroundInstallNotificationsEnabled(this));
        this.installDialogForegroundCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda26
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                MainActivity.this.lambda$showInstallDialog$38(compoundButton, z);
            }
        });
        linearLayout.addView(this.installDialogMessage);
        linearLayout.addView(this.installDialogProgress);
        linearLayout.addView(this.installDialogForegroundCheck);
        AlertDialog alertDialogCreate = new AlertDialog.Builder(this).setTitle(R.string.create_instance_install_dialog_title).setView(linearLayout).setCancelable(false).create();
        this.installDialog = alertDialogCreate;
        alertDialogCreate.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showInstallDialog$38(CompoundButton compoundButton, boolean z) {
        LauncherNotificationPermissionHelper.setBackgroundInstallNotificationsEnabled(this, z);
        if (this.installSessionActive) {
            if (z) {
                startOrUpdateInstallForegroundService(this.activeInstallProgress <= 0);
            } else {
                InstallationForegroundService.stop(this);
            }
        }
    }

    private void updateInstallDialog(int i, String str) {
        TextView textView = this.installDialogMessage;
        if (textView != null) {
            textView.setText(str);
        }
        ProgressBar progressBar = this.installDialogProgress;
        if (progressBar != null) {
            progressBar.setIndeterminate(false);
            this.installDialogProgress.setProgress(Math.max(0, Math.min(100, i)));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dismissInstallDialog() {
        AlertDialog alertDialog = this.installDialog;
        if (alertDialog != null) {
            alertDialog.dismiss();
            this.installDialog = null;
        }
        this.installDialogProgress = null;
        this.installDialogMessage = null;
        this.installDialogForegroundCheck = null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showDeleteInstanceDialog(final LauncherInstance launcherInstance) {
        String absolutePath;
        int i;
        if (!launcherInstance.isIsolated()) {
            ArrayList<String> arrayListFindSharedVersionDependents = LauncherInstanceManager.findSharedVersionDependents(this, launcherInstance.getBaseVersionId());
            if (!arrayListFindSharedVersionDependents.isEmpty()) {
                new AlertDialog.Builder(this).setTitle(getString(R.string.delete_shared_instance_blocked_title, new Object[]{launcherInstance.getName()})).setMessage(getString(R.string.delete_shared_instance_blocked_message, new Object[]{launcherInstance.getName(), LauncherInstanceManager.formatDependentVersionList(arrayListFindSharedVersionDependents)})).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).show();
                return;
            }
        }
        try {
            absolutePath = LauncherInstanceManager.getDeleteTargetDirectory(launcherInstance).getAbsolutePath();
        } catch (Throwable unused) {
            absolutePath = launcherInstance.getRootDirectory().getAbsolutePath();
        }
        if (launcherInstance.isIsolated()) {
            i = R.string.delete_instance_message;
        } else {
            i = R.string.delete_shared_instance_message;
        }
        new AlertDialog.Builder(this).setTitle(getString(R.string.delete_instance_title, new Object[]{launcherInstance.getName()})).setMessage(getString(i, new Object[]{launcherInstance.getName(), absolutePath})).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.button_delete_forever, new DialogInterface.OnClickListener() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda24
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i2) {
                MainActivity.this.lambda$showDeleteInstanceDialog$39(launcherInstance, dialogInterface, i2);
            }
        }).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showDeleteInstanceDialog$39(LauncherInstance launcherInstance, DialogInterface dialogInterface, int i) {
        deleteInstance(launcherInstance);
    }

    private void deleteInstance(final LauncherInstance launcherInstance) {
        setLoading(true);
        setStatus(getString(R.string.delete_instance_deleting, new Object[]{launcherInstance.getName()}));
        new Thread(new Runnable() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda50
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.lambda$deleteInstance$42(launcherInstance);
            }
        }, "Delete Launcher Instance").start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$deleteInstance$42(final LauncherInstance launcherInstance) {
        try {
            LauncherInstanceManager.deleteInstance(this, launcherInstance);
            LauncherPreferences.clearInstancePlayed(this, launcherInstance.getId());
            runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda27
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.lambda$deleteInstance$40(launcherInstance);
                }
            });
        } catch (Throwable th) {
            Logging.e("DeleteInstance", "Unable to delete instance " + launcherInstance.getName(), th);
            runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda28
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.lambda$deleteInstance$41(th, launcherInstance);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$deleteInstance$40(LauncherInstance launcherInstance) {
        LauncherInstance launcherInstance2 = this.selectedInstance;
        if (launcherInstance2 != null && launcherInstance2.getId().equals(launcherInstance.getId())) {
            this.selectedInstance = null;
        }
        refreshInstancesAndRebind(false);
        setLoading(false);
        setStatus(getString(R.string.delete_instance_deleted, new Object[]{launcherInstance.getName()}));
        Toast.makeText(this, getString(R.string.delete_instance_deleted, new Object[]{launcherInstance.getName()}), 0).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$deleteInstance$41(Throwable th, LauncherInstance launcherInstance) {
        setLoading(false);
        String message = th.getMessage() != null ? th.getMessage() : th.getClass().getSimpleName();
        setStatus(getString(R.string.delete_instance_failed, new Object[]{launcherInstance.getName(), message}));
        Toast.makeText(this, getString(R.string.delete_instance_failed, new Object[]{launcherInstance.getName(), message}), 1).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void selectAndOpenInstance(final LauncherInstance launcherInstance) {
        this.selectedInstance = launcherInstance;
        LauncherInstanceAdapter launcherInstanceAdapter = this.instanceAdapter;
        if (launcherInstanceAdapter != null) {
            launcherInstanceAdapter.setSelectedInstance(launcherInstance);
        }
        updateSelectedInstanceCard();
        restoreScopedStorageForInstanceDetails(launcherInstance, new Runnable() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda38
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.lambda$selectAndOpenInstance$43(launcherInstance);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: renamed from: openInstanceDetails, reason: merged with bridge method [inline-methods] */
    public void lambda$selectAndOpenInstance$43(LauncherInstance launcherInstance) {
        startActivity(InstanceDetailsActivity.createIntent(this, launcherInstance));
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: renamed from: quickLaunchInstance, reason: merged with bridge method [inline-methods] */
    public void lambda$quickLaunchInstance$44(final LauncherInstance launcherInstance) {
        if (ControlsMain.toastAndBlockIfInvalidSignature(this) || requireMicrosoftLoginHistoryBeforeLaunch(new Runnable() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda39
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.lambda$quickLaunchInstance$44(launcherInstance);
            }
        })) {
            return;
        }
        this.selectedInstance = launcherInstance;
        LauncherInstanceAdapter launcherInstanceAdapter = this.instanceAdapter;
        if (launcherInstanceAdapter != null) {
            launcherInstanceAdapter.setSelectedInstance(launcherInstance);
        }
        updateSelectedInstanceCard();
        if (SimpleVoiceChatCompat.ensureMicrophoneReadyBeforeLaunch(this, launcherInstance.getGameDirectory())) {
            restoreScopedStorageForLaunchIfNeeded(launcherInstance, new Runnable() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda40
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.lambda$quickLaunchInstance$45(launcherInstance);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void launchSelectedInstance() {
        if (ControlsMain.toastAndBlockIfInvalidSignature(this)) {
            return;
        }
        if (this.selectedInstance == null) {
            Toast.makeText(this, R.string.hint_select_instance, 0).show();
        } else if (!requireMicrosoftLoginHistoryBeforeLaunch(new Runnable() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.launchSelectedInstance();
            }
        }) && SimpleVoiceChatCompat.ensureMicrophoneReadyBeforeLaunch(this, this.selectedInstance.getGameDirectory())) {
            final LauncherInstance launcherInstance = this.selectedInstance;
            restoreScopedStorageForLaunchIfNeeded(launcherInstance, new Runnable() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda9
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.lambda$launchSelectedInstance$46(launcherInstance);
                }
            });
        }
    }

    private void restoreScopedStorageForInstanceDetails(final LauncherInstance launcherInstance, final Runnable runnable) {
        if (!StorageLocationStore.isSelectedScopedStorage(this)) {
            runnable.run();
            return;
        }
        setLoading(true);
        setStatus("Reading instance files...");
        new Thread(new Runnable() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda43
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.lambda$restoreScopedStorageForInstanceDetails$48(launcherInstance, runnable);
            }
        }, "ScopedStorageInstanceDetails").start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$restoreScopedStorageForInstanceDetails$48(LauncherInstance launcherInstance, final Runnable runnable) {
        final Throwable th;
        try {
            th = null;
            StorageLocationStore.syncSelectedLocalPathFromTree(this, launcherInstance.getRootDirectory(), null);
        } catch (Throwable th2) {
            th = th2;
            Logging.i("ScopedStorage", "Unable to restore instance folder before details: " + th.getMessage());
        }
        runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.lambda$restoreScopedStorageForInstanceDetails$47(th, runnable);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$restoreScopedStorageForInstanceDetails$47(Throwable th, Runnable runnable) {
        String simpleName;
        PathManager.initContextConstants(this);
        this.binding.textFolder.setText(getString(R.string.launcher_folder_value, new Object[]{PathManager.DIR_MINECRAFT_HOME}));
        setLoading(false);
        if (th != null) {
            if (th.getMessage() != null) {
                simpleName = th.getMessage();
            } else {
                simpleName = th.getClass().getSimpleName();
            }
            setStatus("Instance files could not be fully read: " + simpleName);
            Toast.makeText(this, "Instance files could not be fully read: " + simpleName, 1).show();
        }
        runnable.run();
    }

    private void restoreScopedStorageForLaunchIfNeeded(LauncherInstance launcherInstance, final Runnable runnable) {
        if (!StorageLocationStore.needsSelectedTreeRestoreForLaunch(this, launcherInstance.getRootDirectory(), launcherInstance.getBaseVersionId())) {
            runnable.run();
            return;
        }
        setLoading(true);
        this.binding.buttonLaunchVersion.setEnabled(false);
        setStatus("Preparing game files...");
        showLaunchPrepareDialog(launcherInstance);
        new Thread(new Runnable() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda35
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.lambda$restoreScopedStorageForLaunchIfNeeded$52(runnable);
            }
        }, "ScopedStorageLaunchRestore").start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$restoreScopedStorageForLaunchIfNeeded$52(final Runnable runnable) {
        try {
            StorageLocationStore.syncSelectedTreeToMirror(this, new SafMinecraftMirror.Progress() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda32
                @Override // ca.dnamobile.javalauncher.storage.SafMinecraftMirror.Progress
                public final void onProgress(int i, String str) {
                    MainActivity.this.lambda$restoreScopedStorageForLaunchIfNeeded$50(i, str);
                }
            });
            th = null;
        } catch (Throwable th) {
            th = th;
            Logging.i("ScopedStorage", "Unable to restore scoped storage before launch: " + th.getMessage());
        }
        runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda34
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.lambda$restoreScopedStorageForLaunchIfNeeded$51(th, runnable);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$restoreScopedStorageForLaunchIfNeeded$50(final int i, final String str) {
        this.mainHandler.post(new Runnable() { // from class: ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticLambda53
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.lambda$restoreScopedStorageForLaunchIfNeeded$49(str, i);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$restoreScopedStorageForLaunchIfNeeded$49(String str, int i) {
        String strTrim;
        if (str == null || str.trim().isEmpty()) {
            strTrim = "Preparing game files...";
        } else {
            strTrim = str.trim();
        }
        updateLaunchPrepareProgress(i, strTrim);
        setStatus("Launching game: " + strTrim);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$restoreScopedStorageForLaunchIfNeeded$51(Throwable th, Runnable runnable) {
        String simpleName;
        PathManager.initContextConstants(this);
        this.binding.textFolder.setText(getString(R.string.launcher_folder_value, new Object[]{PathManager.DIR_MINECRAFT_HOME}));
        refreshInstancesAndRebind(true);
        setLoading(false);
        if (th != null) {
            dismissLaunchPrepareDialog();
            if (th.getMessage() != null) {
                simpleName = th.getMessage();
            } else {
                simpleName = th.getClass().getSimpleName();
            }
            setStatus("Unable to prepare scoped storage files: " + simpleName);
            Toast.makeText(this, "Unable to prepare scoped storage files: " + simpleName, 1).show();
            return;
        }
        updateLaunchPrepareProgress(100, "Starting Minecraft...");
        dismissLaunchPrepareDialog();
        runnable.run();
    }

    private void showLaunchPrepareDialog(LauncherInstance launcherInstance) {
        dismissLaunchPrepareDialog();
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setBackgroundColor(COLOR_DIALOG_BG);
        int iDp = dp(18.0f);
        linearLayout.setPadding(iDp, iDp, iDp, dp(10.0f));
        TextView textView = new TextView(this);
        textView.setText("Launching game");
        textView.setTextSize(24.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        int i = COLOR_TEXT_PRIMARY;
        textView.setTextColor(i);
        textView.setPadding(dp(2.0f), 0, dp(2.0f), dp(6.0f));
        linearLayout.addView(textView, new LinearLayout.LayoutParams(-1, -2));
        TextView textView2 = new TextView(this);
        textView2.setText("Preparing local game files for " + launcherInstance.getName() + ". This usually only happens after reinstalling the app or choosing a scoped storage folder.");
        textView2.setTextSize(14.0f);
        int i2 = COLOR_TEXT_SECONDARY;
        textView2.setTextColor(i2);
        textView2.setPadding(dp(2.0f), 0, dp(2.0f), dp(12.0f));
        linearLayout.addView(textView2, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(1);
        int iDp2 = dp(14.0f);
        linearLayout2.setPadding(iDp2, iDp2, iDp2, iDp2);
        linearLayout2.setBackground(roundedDrawable(COLOR_CARD_BG, COLOR_CARD_STROKE, 18));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.setMargins(0, 0, 0, dp(12.0f));
        linearLayout.addView(linearLayout2, layoutParams);
        TextView textView3 = new TextView(this);
        textView3.setText("Restoring scoped storage");
        textView3.setTextSize(18.0f);
        textView3.setTypeface(Typeface.DEFAULT_BOLD);
        textView3.setTextColor(i);
        textView3.setPadding(0, 0, 0, dp(8.0f));
        linearLayout2.addView(textView3, new LinearLayout.LayoutParams(-1, -2));
        TextView textView4 = new TextView(this);
        this.launchPrepareMessage = textView4;
        textView4.setText("Checking game files...");
        this.launchPrepareMessage.setTextSize(13.0f);
        this.launchPrepareMessage.setTextColor(i2);
        this.launchPrepareMessage.setPadding(0, 0, 0, dp(10.0f));
        linearLayout2.addView(this.launchPrepareMessage, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout linearLayout3 = new LinearLayout(this);
        linearLayout3.setOrientation(0);
        linearLayout3.setGravity(16);
        linearLayout3.setPadding(0, 0, 0, dp(4.0f));
        TextView textView5 = new TextView(this);
        textView5.setText("Progress");
        textView5.setTextSize(13.0f);
        int i3 = COLOR_TEXT_MUTED;
        textView5.setTextColor(i3);
        linearLayout3.addView(textView5, new LinearLayout.LayoutParams(0, -2, 1.0f));
        TextView textView6 = new TextView(this);
        this.launchPreparePercent = textView6;
        textView6.setText("Loading...");
        this.launchPreparePercent.setTextSize(13.0f);
        this.launchPreparePercent.setTypeface(Typeface.DEFAULT_BOLD);
        this.launchPreparePercent.setTextColor(COLOR_ACCENT);
        this.launchPreparePercent.setGravity(GravityCompat.END);
        linearLayout3.addView(this.launchPreparePercent, new LinearLayout.LayoutParams(-2, -2));
        linearLayout2.addView(linearLayout3, new LinearLayout.LayoutParams(-1, -2));
        ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        this.launchPrepareProgress = progressBar;
        progressBar.setIndeterminate(true);
        this.launchPrepareProgress.setMax(100);
        this.launchPrepareProgress.setProgress(0);
        tintProgressBar(this.launchPrepareProgress);
        linearLayout2.addView(this.launchPrepareProgress, new LinearLayout.LayoutParams(-1, -2));
        TextView textView7 = new TextView(this);
        textView7.setText("Please keep this screen open and do not press Play again. The next launch should be much faster.");
        textView7.setTextSize(12.0f);
        textView7.setTextColor(i3);
        textView7.setPadding(0, dp(10.0f), 0, 0);
        linearLayout2.addView(textView7, new LinearLayout.LayoutParams(-1, -2));
        AlertDialog alertDialogCreate = new AlertDialog.Builder(this).setView(linearLayout).setCancelable(false).create();
        this.launchPrepareDialog = alertDialogCreate;
        alertDialogCreate.show();
        styleLaunchPrepareDialogChrome(this.launchPrepareDialog);
        updateLaunchPrepareProgress(1, "Checking game files...");
    }

    private void updateLaunchPrepareProgress(int i, String str) {
        String strTrim = str.trim().isEmpty() ? "Preparing game files..." : str.trim();
        TextView textView = this.launchPrepareMessage;
        if (textView != null) {
            textView.setText(strTrim);
        }
        TextView textView2 = this.launchPreparePercent;
        if (textView2 != null) {
            if (i >= 100) {
                textView2.setText("Done");
            } else {
                textView2.setText("Loading...");
            }
        }
        ProgressBar progressBar = this.launchPrepareProgress;
        if (progressBar != null) {
            if (i >= 100) {
                progressBar.setIndeterminate(false);
                this.launchPrepareProgress.setProgress(100);
            } else {
                progressBar.setIndeterminate(true);
            }
        }
    }

    private void dismissLaunchPrepareDialog() {
        AlertDialog alertDialog = this.launchPrepareDialog;
        if (alertDialog != null) {
            alertDialog.dismiss();
            this.launchPrepareDialog = null;
        }
        this.launchPrepareMessage = null;
        this.launchPreparePercent = null;
        this.launchPrepareProgress = null;
    }

    private void styleLaunchPrepareDialogChrome(AlertDialog alertDialog) {
        Window window = alertDialog.getWindow();
        if (window == null) {
            return;
        }
        int i = COLOR_DIALOG_BG;
        window.setBackgroundDrawable(roundedDrawable(i, i, 22));
        window.setDimAmount(0.58f);
    }

    private GradientDrawable roundedDrawable(int i, int i2, int i3) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(i);
        gradientDrawable.setCornerRadius(dp(i3));
        gradientDrawable.setStroke(dp(1.0f), i2);
        return gradientDrawable;
    }

    private void tintProgressBar(ProgressBar progressBar) {
        int i = COLOR_ACCENT;
        progressBar.setProgressTintList(ColorStateList.valueOf(i));
        progressBar.setProgressBackgroundTintList(ColorStateList.valueOf(COLOR_CARD_STROKE));
        progressBar.setIndeterminateTintList(ColorStateList.valueOf(i));
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: renamed from: startGameActivity, reason: merged with bridge method [inline-methods] and merged with bridge method [inline-methods] */
    public void lambda$quickLaunchInstance$45(LauncherInstance launcherInstance) {
        LauncherPreferences.recordInstancePlayed(this, launcherInstance.getId());
        Intent intent = new Intent(this, (Class<?>) GameActivity.class);
        intent.putExtra(GameActivity.EXTRA_VERSION_ID, launcherInstance.isIsolated() ? launcherInstance.getName() : launcherInstance.getBaseVersionId());
        startActivity(intent);
    }

    private void showSelectedInstanceFolder() {
        LauncherInstance launcherInstance = this.selectedInstance;
        if (launcherInstance == null) {
            Toast.makeText(this, R.string.hint_select_instance, 0).show();
            return;
        }
        String absolutePath = launcherInstance.getGameDirectory().getAbsolutePath();
        setStatus(getString(R.string.msg_instance_folder_location, new Object[]{absolutePath}));
        Toast.makeText(this, absolutePath, 1).show();
    }

    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, android.app.Activity
    protected void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        if (this.appIntegrityBlocked || this.binding == null) {
            return;
        }
        if (i == REQUEST_IMPORT_MODPACK) {
            if (i2 != -1 || intent == null || intent.getData() == null) {
                return;
            }
            importModpackFromUri(intent.getData());
            return;
        }
        if (i == REQUEST_ADD_STORAGE_LOCATION) {
            if (i2 != -1 || intent == null || intent.getData() == null) {
                return;
            }
            Uri data = intent.getData();
            try {
                getContentResolver().takePersistableUriPermission(data, intent.getFlags() & 3);
            } catch (Throwable unused) {
            }
            StorageLocation storageLocationAddTreeUri = StorageLocationStore.addTreeUri(this, data);
            StorageLocationStore.setSelectedLocationId(this, storageLocationAddTreeUri.getId());
            refreshAfterStorageLocationChange(getString(R.string.storage_location_added, new Object[]{storageLocationAddTreeUri.getDisplayName()}), false);
            return;
        }
        if (i != REQUEST_PICK_INSTANCE_ICON || i2 != -1 || intent == null || intent.getData() == null) {
            return;
        }
        Uri data2 = intent.getData();
        try {
            getContentResolver().takePersistableUriPermission(data2, 1);
        } catch (Throwable unused2) {
        }
        CreateInstanceDialog createInstanceDialog = this.createInstanceDialog;
        if (createInstanceDialog == null || !createInstanceDialog.isShowing()) {
            return;
        }
        this.createInstanceDialog.setIconUri(data2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateSelectedInstanceCard() {
        if (this.selectedInstance == null) {
            this.binding.textSelectedVersion.setText(R.string.selected_instance_empty);
            this.binding.buttonLaunchVersion.setEnabled(false);
            this.binding.buttonOpenFolder.setEnabled(false);
        } else {
            this.binding.textSelectedVersion.setText(getString(R.string.selected_instance_value, new Object[]{this.selectedInstance.getName(), this.selectedInstance.getLoader(), this.selectedInstance.getMinecraftVersionId()}));
            this.binding.buttonLaunchVersion.setEnabled(true);
            this.binding.buttonOpenFolder.setEnabled(true);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateAccountStatus(AccountStore.Account account) {
        boolean z = account != null;
        updateMainContentButtonVisibility(z);
        this.binding.buttonSignIn.setVisibility(z ? 8 : 0);
        this.binding.buttonSignIn.setEnabled(!z);
        this.binding.buttonSignOut.setVisibility(z ? 0 : 8);
        this.binding.buttonSignOut.setEnabled(z);
        if (account == null) {
            if (hasCompletedMicrosoftLoginOnce()) {
                this.binding.textAccountStatus.setText(R.string.status_signed_out_offline_unlocked);
                return;
            } else {
                this.binding.textAccountStatus.setText(R.string.status_signed_out);
                return;
            }
        }
        String str = account.displayName;
        if (isNullOrBlank(str)) {
            str = account.minecraftName;
        }
        if (isNullOrBlank(str)) {
            str = account.email;
        }
        if (isNullOrBlank(str)) {
            str = "Microsoft Player";
        }
        this.binding.textAccountStatus.setText(getString(R.string.status_signed_in, new Object[]{str}));
    }

    private static boolean isNullOrBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setLoading(boolean z) {
        this.binding.progressVersions.setIndeterminate(z);
        this.binding.progressVersions.setVisibility(z ? 0 : 8);
        this.binding.buttonRefreshVersions.setEnabled(!z);
        this.binding.fabCreateInstance.setEnabled(!z);
        if (z) {
            return;
        }
        updateSelectedInstanceCard();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setStatus(String str) {
        ActivityMainBinding activityMainBinding = this.binding;
        if (activityMainBinding == null || activityMainBinding.textStatus == null) {
            return;
        }
        CharSequence text = this.binding.textStatus.getText();
        if (text == null || !str.contentEquals(text)) {
            this.binding.textStatus.setText(str);
        }
    }
}
