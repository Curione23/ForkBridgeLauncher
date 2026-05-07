package ca.dnamobile.javalauncher;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import ca.dnamobile.javalauncher.auth.MicrosoftAuthConfigPersonal;
import ca.dnamobile.javalauncher.auth.MicrosoftAuthManagerPersonal;
import ca.dnamobile.javalauncher.controls.ControlsActivity;
import ca.dnamobile.javalauncher.controls.ControlsPreferences;
import ca.dnamobile.javalauncher.data.AccountStore;
import ca.dnamobile.javalauncher.databinding.ActivityLauncherSettingsBinding;
import ca.dnamobile.javalauncher.feature.log.Logging;
import ca.dnamobile.javalauncher.input.GamepadMappingDialog;
import ca.dnamobile.javalauncher.input.GamepadMappingStore;
import ca.dnamobile.javalauncher.legal.LegalLinks;
import ca.dnamobile.javalauncher.logs.LauncherLogManager;
import ca.dnamobile.javalauncher.modcompat.AndroidMicrophonePermission;
import ca.dnamobile.javalauncher.notifications.LauncherNotificationPermissionHelper;
import ca.dnamobile.javalauncher.renderer.Driver;
import ca.dnamobile.javalauncher.renderer.DriverPluginManager;
import ca.dnamobile.javalauncher.renderer.MobileGluesConfigHelper;
import ca.dnamobile.javalauncher.renderer.RendererInterface;
import ca.dnamobile.javalauncher.renderer.RendererPluginManager;
import ca.dnamobile.javalauncher.renderer.Renderers;
import ca.dnamobile.javalauncher.settings.GameOverlayPreferences;
import ca.dnamobile.javalauncher.settings.LauncherPreferences;
import ca.dnamobile.javalauncher.settings.MemoryAllocationUtils;
import ca.dnamobile.javalauncher.skin.CustomSkinStore;
import ca.dnamobile.javalauncher.skin.MicrosoftSkinUploader;
import ca.dnamobile.javalauncher.skin.PlayerHeadLoader;
import ca.dnamobile.javalauncher.skin.SkinModelType;
import ca.dnamobile.javalauncher.update.LauncherUpdateDialogs;
import ca.dnamobile.javalauncher.update.LauncherUpdatePreferences;
import ca.dnamobile.javalauncher.utils.FullscreenUtils;
import ca.dnamobile.javalauncher.utils.path.PathManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.tabs.TabLayout;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class LauncherSettingsActivity extends AppCompatActivity {
    private static final String SETTINGS_DEFAULTS_APPLIED_KEY = "settings_defaults_applied_2026_04_instances";
    private static final String SETTINGS_DEFAULTS_PREFS = "launcher_settings_defaults";
    private AccountStore accountStore;
    private MicrosoftAuthManagerPersonal authManager;
    private ActivityLauncherSettingsBinding binding;
    private ActivityResultLauncher<Intent> customSkinPickerLauncher;
    private CustomSkinStore customSkinStore;
    private boolean driverSpinnerReady;
    private ActivityResultLauncher<String> microphonePermissionLauncher;
    private ActivityResultLauncher<Intent> microsoftSkinPickerLauncher;
    private ActivityResultLauncher<Intent> mobileGluesFolderPickerLauncher;
    private ActivityResultLauncher<String> notificationPermissionLauncher;
    private AlertDialog offlineAccountsDialog;
    private ActivityResultLauncher<Intent> offlineSkinPickerLauncher;
    private TextView pendingOfflineSkinLabel;
    private ImageView pendingOfflineSkinPreview;
    private Uri pendingOfflineSkinUri;
    private boolean rendererSpinnerReady;
    private SeekBar sliderHardwareMouseDpiScale;
    private TextView textHardwareMouseDpiScale;
    private final List<RendererInterface> availableRenderers = new ArrayList();
    private final List<Driver> availableDrivers = new ArrayList();

    static /* synthetic */ void lambda$registerSkinPickerLauncher$9(ActivityResult activityResult) {
    }

    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        PathManager.initContextConstants(this);
        ActivityLauncherSettingsBinding activityLauncherSettingsBindingInflate = ActivityLauncherSettingsBinding.inflate(getLayoutInflater());
        this.binding = activityLauncherSettingsBindingInflate;
        setContentView(activityLauncherSettingsBindingInflate.getRoot());
        FullscreenUtils.enableImmersive(this);
        this.binding.buttonSettingsBack.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda63
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                LauncherSettingsActivity.this.lambda$onCreate$0(view);
            }
        });
        applySettingsDefaultsOnce();
        setupSettingsSectionTabs();
        registerSkinPickerLauncher();
        registerMicrosoftSkinPickerLauncher();
        registerOfflineSkinPickerLauncher();
        registerNotificationPermissionLauncher();
        registerMicrophonePermissionLauncher();
        registerMobileGluesFolderPickerLauncher();
        setupAccountUi();
        setupInstanceSettings();
        setupRendererSettings();
        setupRenderSurfaceSettings();
        setupControllerSettings();
        setupLauncherSettings();
        setupPrivacyPolicySettings();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$0(View view) {
        finish();
    }

    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    protected void onResume() {
        super.onResume();
        FullscreenUtils.enableImmersive(this);
        if (this.binding != null) {
            RendererInterface selectedRendererFromSpinner = getSelectedRendererFromSpinner();
            updateMobileGluesConfigSummary(selectedRendererFromSpinner);
            if (DriverPluginManager.isVulkanZinkRenderer(selectedRendererFromSpinner)) {
                DriverPluginManager.reload(this);
                updateVulkanDriverSettings(selectedRendererFromSpinner);
            }
            refreshControllerSettingsValues();
            updateInstallNotificationSettingsUi();
            updateSimpleVoiceChatPermissionUi();
            refreshAccountUiFromStore();
        }
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public void onWindowFocusChanged(boolean z) {
        super.onWindowFocusChanged(z);
        if (z) {
            FullscreenUtils.enableImmersive(this);
        }
    }

    @Override // androidx.appcompat.app.AppCompatActivity, androidx.fragment.app.FragmentActivity, android.app.Activity
    protected void onDestroy() {
        if (this.authManager != null && !isChangingConfigurations()) {
            this.authManager.dispose();
        }
        super.onDestroy();
    }

    private void refreshAccountUiFromStore() {
        AccountStore accountStore = this.accountStore;
        if (accountStore == null || this.binding == null) {
            return;
        }
        try {
            AccountStore.Account accountLoad = accountStore.load();
            updateAccountStatus(accountLoad);
            updateSkinUi(accountLoad);
            updateChangeMicrosoftSkinButtonState(accountLoad);
        } catch (Throwable th) {
            Logging.e("LauncherSettings", "Unable to refresh account UI", th);
        }
    }

    private void applySettingsDefaultsOnce() {
        SharedPreferences sharedPreferences = getSharedPreferences(SETTINGS_DEFAULTS_PREFS, 0);
        if (sharedPreferences.getBoolean(SETTINGS_DEFAULTS_APPLIED_KEY, false)) {
            return;
        }
        LauncherPreferences.setShowSharedInstalls(this, false);
        LauncherPreferences.setRemoveInheritedVanillaAfterLoaderInstall(this, false);
        sharedPreferences.edit().putBoolean(SETTINGS_DEFAULTS_APPLIED_KEY, true).apply();
    }

    private void setupSettingsSectionTabs() {
        this.binding.settingsSectionTabs.removeAllTabs();
        addSettingsSectionTab(R.string.settings_account_title);
        addSettingsSectionTab(R.string.renderer_settings_title);
        addSettingsSectionTab(R.string.controller_settings_title);
        addSettingsSectionTab(R.string.settings_launcher_title);
        addSettingsSectionTab(R.string.settings_instance_title);
        addSettingsSectionTab("Privacy Policy");
        this.binding.settingsSectionTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity.1
            @Override // com.google.android.material.tabs.TabLayout.BaseOnTabSelectedListener
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override // com.google.android.material.tabs.TabLayout.BaseOnTabSelectedListener
            public void onTabSelected(TabLayout.Tab tab) {
                LauncherSettingsActivity.this.scrollToSettingsSection(tab.getPosition());
            }

            @Override // com.google.android.material.tabs.TabLayout.BaseOnTabSelectedListener
            public void onTabReselected(TabLayout.Tab tab) {
                LauncherSettingsActivity.this.scrollToSettingsSection(tab.getPosition());
            }
        });
    }

    private void addSettingsSectionTab(int i) {
        TabLayout.Tab tabNewTab = this.binding.settingsSectionTabs.newTab();
        tabNewTab.setText(i);
        this.binding.settingsSectionTabs.addTab(tabNewTab);
    }

    private void addSettingsSectionTab(String str) {
        TabLayout.Tab tabNewTab = this.binding.settingsSectionTabs.newTab();
        tabNewTab.setText(str);
        this.binding.settingsSectionTabs.addTab(tabNewTab);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void scrollToSettingsSection(int i) {
        final MaterialCardView materialCardView;
        if (i == 0) {
            materialCardView = this.binding.cardAccountSettings;
        } else if (i == 1) {
            materialCardView = this.binding.cardRendererSettings;
        } else if (i == 2) {
            materialCardView = this.binding.cardControllerSettings;
        } else if (i == 3) {
            materialCardView = this.binding.cardLauncherSettings;
        } else if (i == 4) {
            materialCardView = this.binding.cardInstanceSettings;
        } else if (i != 5) {
            return;
        } else {
            materialCardView = this.binding.cardPrivacyPolicySettings;
        }
        this.binding.settingsScrollView.post(new Runnable() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                LauncherSettingsActivity.this.lambda$scrollToSettingsSection$1(materialCardView);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$scrollToSettingsSection$1(View view) {
        this.binding.settingsScrollView.smoothScrollTo(0, Math.max(0, view.getTop() - dp(8.0f)));
    }

    private void setupAccountUi() {
        try {
            this.accountStore = new AccountStore(this);
            this.customSkinStore = new CustomSkinStore(this);
            MicrosoftAuthManagerPersonal microsoftAuthManagerPersonal = new MicrosoftAuthManagerPersonal(this, this.accountStore);
            this.authManager = microsoftAuthManagerPersonal;
            microsoftAuthManagerPersonal.setListener(new MicrosoftAuthManagerPersonal.Listener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity.2
                @Override // ca.dnamobile.javalauncher.auth.MicrosoftAuthManagerPersonal.Listener
                public void onSignedIn(AccountStore.Account account) {
                    LauncherSettingsActivity.this.updateAccountStatus(account);
                    LauncherSettingsActivity.this.updateSkinUi(account);
                    LauncherSettingsActivity.this.updateChangeMicrosoftSkinButtonState(account);
                    LauncherSettingsActivity.this.binding.buttonRefreshMicrosoftSkin.setEnabled(true);
                }

                @Override // ca.dnamobile.javalauncher.auth.MicrosoftAuthManagerPersonal.Listener
                public void onError(String str) {
                    LauncherSettingsActivity.this.binding.textAccountStatus.setText(str);
                    LauncherSettingsActivity.this.binding.buttonRefreshMicrosoftSkin.setEnabled(true);
                    Toast.makeText(LauncherSettingsActivity.this, str, 1).show();
                }
            });
            AccountStore.Account accountLoad = this.accountStore.load();
            updateAccountStatus(accountLoad);
            updateSkinUi(accountLoad);
        } catch (Throwable th) {
            Logging.e("LauncherSettings", "Microsoft account UI initialization failed", th);
            this.binding.textAccountStatus.setText(R.string.status_signed_out);
            this.binding.buttonSignIn.setEnabled(false);
            this.binding.buttonSignOut.setEnabled(false);
            this.binding.buttonManageOfflineAccounts.setEnabled(false);
            this.binding.buttonUseMicrosoftAccount.setEnabled(false);
            this.binding.buttonRefreshMicrosoftSkin.setEnabled(false);
        }
        setupChangeMicrosoftSkinButton();
        this.binding.buttonSignIn.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda66
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                LauncherSettingsActivity.this.lambda$setupAccountUi$2(view);
            }
        });
        this.binding.buttonSignOut.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda68
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                LauncherSettingsActivity.this.lambda$setupAccountUi$3(view);
            }
        });
        this.binding.buttonUseMicrosoftAccount.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda69
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                LauncherSettingsActivity.this.lambda$setupAccountUi$4(view);
            }
        });
        this.binding.buttonManageOfflineAccounts.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda70
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                LauncherSettingsActivity.this.lambda$setupAccountUi$5(view);
            }
        });
        this.binding.buttonRefreshMicrosoftSkin.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                LauncherSettingsActivity.this.lambda$setupAccountUi$6(view);
            }
        });
        AccountStore accountStore = this.accountStore;
        updateChangeMicrosoftSkinButtonState(accountStore != null ? accountStore.load() : null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupAccountUi$2(View view) {
        if (this.authManager == null) {
            return;
        }
        if (!MicrosoftAuthConfigPersonal.isConfigured()) {
            this.binding.textAccountStatus.setText(R.string.msg_configure_client_id);
        } else {
            this.authManager.signIn();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupAccountUi$3(View view) {
        showSignOutConfirmationDialog();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupAccountUi$4(View view) {
        useRememberedMicrosoftAccount();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupAccountUi$5(View view) {
        showOfflineAccountsDialog();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupAccountUi$6(View view) {
        refreshMicrosoftAccountAndSkin(true);
    }

    private void setupChangeMicrosoftSkinButton() {
        ActivityLauncherSettingsBinding activityLauncherSettingsBinding = this.binding;
        if (activityLauncherSettingsBinding == null) {
            return;
        }
        activityLauncherSettingsBinding.buttonChangeMicrosoftSkin.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda52
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                LauncherSettingsActivity.this.lambda$setupChangeMicrosoftSkinButton$7(view);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupChangeMicrosoftSkinButton$7(View view) {
        showChangeMicrosoftSkinDialog();
    }

    private void showSignOutConfirmationDialog() {
        new AlertDialog.Builder(this).setTitle(R.string.sign_out_confirm_title).setMessage(R.string.sign_out_confirm_message).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.button_sign_out, new DialogInterface.OnClickListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda54
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                LauncherSettingsActivity.this.lambda$showSignOutConfirmationDialog$8(dialogInterface, i);
            }
        }).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showSignOutConfirmationDialog$8(DialogInterface dialogInterface, int i) {
        performMicrosoftSignOut();
    }

    private void performMicrosoftSignOut() {
        MicrosoftAuthManagerPersonal microsoftAuthManagerPersonal = this.authManager;
        if (microsoftAuthManagerPersonal == null || this.accountStore == null) {
            return;
        }
        microsoftAuthManagerPersonal.signOut();
        AccountStore.Account accountLoad = this.accountStore.load();
        updateAccountStatus(accountLoad);
        updateSkinUi(accountLoad);
        if (this.binding.buttonRefreshMicrosoftSkin != null) {
            this.binding.buttonRefreshMicrosoftSkin.setEnabled(false);
        }
        updateChangeMicrosoftSkinButtonState(accountLoad);
        Toast.makeText(this, R.string.msg_sign_out_success, 0).show();
    }

    private void registerSkinPickerLauncher() {
        this.customSkinPickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda64
            @Override // androidx.activity.result.ActivityResultCallback
            public final void onActivityResult(Object obj) {
                LauncherSettingsActivity.lambda$registerSkinPickerLauncher$9((ActivityResult) obj);
            }
        });
    }

    private void registerMicrosoftSkinPickerLauncher() {
        this.microsoftSkinPickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda61
            @Override // androidx.activity.result.ActivityResultCallback
            public final void onActivityResult(Object obj) {
                LauncherSettingsActivity.this.lambda$registerMicrosoftSkinPickerLauncher$10((ActivityResult) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$registerMicrosoftSkinPickerLauncher$10(ActivityResult activityResult) {
        Uri data;
        if (activityResult.getResultCode() != -1 || activityResult.getData() == null || (data = activityResult.getData().getData()) == null) {
            return;
        }
        prepareMicrosoftSkinUpload(data);
    }

    private void registerOfflineSkinPickerLauncher() {
        this.offlineSkinPickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda3
            @Override // androidx.activity.result.ActivityResultCallback
            public final void onActivityResult(Object obj) {
                LauncherSettingsActivity.this.lambda$registerOfflineSkinPickerLauncher$11((ActivityResult) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$registerOfflineSkinPickerLauncher$11(ActivityResult activityResult) {
        Uri data;
        if (activityResult.getResultCode() != -1 || activityResult.getData() == null || (data = activityResult.getData().getData()) == null) {
            return;
        }
        this.pendingOfflineSkinUri = data;
        if (this.pendingOfflineSkinPreview != null) {
            updatePendingOfflineSkinPreview(data);
        }
        TextView textView = this.pendingOfflineSkinLabel;
        if (textView != null) {
            textView.setText(R.string.offline_account_skin_selected);
        }
    }

    private void openCustomSkinPicker() {
        showOfflineAccountsDialog();
    }

    private void handleCustomSkinResult(Uri uri) {
        this.pendingOfflineSkinUri = uri;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateSkinUi(AccountStore.Account account) {
        AccountStore accountStore = this.accountStore;
        boolean z = accountStore != null && accountStore.hasMicrosoftLoginCompletedOnce();
        boolean z2 = account != null && account.isOfflineAccount() && account.hasOfflineSkin();
        boolean z3 = (account == null || !account.isMicrosoftAccount() || isNullOrBlank(account.skinUrl)) ? false : true;
        AccountStore accountStore2 = this.accountStore;
        boolean z4 = accountStore2 != null && accountStore2.hasStoredMicrosoftAccount();
        if (z2) {
            this.binding.textSkinStatus.setText(getString(R.string.offline_account_skin_active, new Object[]{account.getBestDisplayName()}));
        } else if (z3) {
            this.binding.textSkinStatus.setText(R.string.custom_skin_status_microsoft);
        } else if (z4) {
            this.binding.textSkinStatus.setText(R.string.microsoft_skin_needs_refresh);
        } else if (!z) {
            this.binding.textSkinStatus.setText(R.string.custom_skin_status_locked);
        } else {
            this.binding.textSkinStatus.setText(R.string.custom_skin_status_none);
        }
        PlayerHeadLoader.loadInto(this, this.binding.imagePlayerHead, account, null);
        updateChangeMicrosoftSkinButtonState(account);
    }

    private void setupInstanceSettings() {
        this.binding.textFolder.setText(getString(R.string.launcher_folder_value, new Object[]{PathManager.DIR_MINECRAFT_HOME}));
        boolean zIsShowSharedInstalls = LauncherPreferences.isShowSharedInstalls(this);
        this.binding.switchShowSharedInstalls.setChecked(zIsShowSharedInstalls);
        updateSharedInstallsSwitchText(zIsShowSharedInstalls);
        this.binding.switchShowSharedInstalls.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda13
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                LauncherSettingsActivity.this.lambda$setupInstanceSettings$12(compoundButton, z);
            }
        });
        boolean zIsRemoveInheritedVanillaAfterLoaderInstall = LauncherPreferences.isRemoveInheritedVanillaAfterLoaderInstall(this);
        this.binding.switchRemoveInheritedVanilla.setChecked(zIsRemoveInheritedVanillaAfterLoaderInstall);
        updateRemoveInheritedVanillaSwitchText(zIsRemoveInheritedVanillaAfterLoaderInstall);
        this.binding.switchRemoveInheritedVanilla.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda14
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                LauncherSettingsActivity.this.lambda$setupInstanceSettings$13(compoundButton, z);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupInstanceSettings$12(CompoundButton compoundButton, boolean z) {
        LauncherPreferences.setShowSharedInstalls(this, z);
        updateSharedInstallsSwitchText(z);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupInstanceSettings$13(CompoundButton compoundButton, boolean z) {
        LauncherPreferences.setRemoveInheritedVanillaAfterLoaderInstall(this, z);
        updateRemoveInheritedVanillaSwitchText(z);
    }

    private void setupRendererSettings() {
        this.binding.spinnerRenderer.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity.3
            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onNothingSelected(AdapterView<?> adapterView) {
            }

            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long j) {
                if (!LauncherSettingsActivity.this.rendererSpinnerReady || i < 0 || i >= LauncherSettingsActivity.this.availableRenderers.size()) {
                    return;
                }
                RendererInterface rendererInterface = (RendererInterface) LauncherSettingsActivity.this.availableRenderers.get(i);
                LauncherPreferences.setSelectedRendererIdentifier(LauncherSettingsActivity.this, rendererInterface.getUniqueIdentifier());
                Renderers.setCurrentRenderer(LauncherSettingsActivity.this, rendererInterface.getUniqueIdentifier(), true);
                LauncherSettingsActivity.this.updateRendererDescription(rendererInterface);
                LauncherSettingsActivity.this.updateRendererPluginButtons(rendererInterface);
                LauncherSettingsActivity.this.updateVulkanDriverSettings(rendererInterface);
            }
        });
        this.binding.spinnerVulkanDriver.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity.4
            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onNothingSelected(AdapterView<?> adapterView) {
            }

            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long j) {
                if (!LauncherSettingsActivity.this.driverSpinnerReady || i < 0 || i >= LauncherSettingsActivity.this.availableDrivers.size()) {
                    return;
                }
                Driver driver = (Driver) LauncherSettingsActivity.this.availableDrivers.get(i);
                LauncherPreferences.setSelectedVulkanDriverName(LauncherSettingsActivity.this, driver.getName());
                LauncherSettingsActivity.this.updateVulkanDriverDescription(driver);
            }
        });
        this.binding.buttonImportRendererPlugin.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda19
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                LauncherSettingsActivity.this.lambda$setupRendererSettings$14(view);
            }
        });
        this.binding.buttonGrantRendererStorageAccess.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda20
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                LauncherSettingsActivity.this.lambda$setupRendererSettings$15(view);
            }
        });
        this.binding.buttonClearRendererPluginCache.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda21
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                LauncherSettingsActivity.this.lambda$setupRendererSettings$16(view);
            }
        });
        this.binding.buttonRefreshRenderers.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda23
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                LauncherSettingsActivity.this.lambda$setupRendererSettings$17(view);
            }
        });
        boolean zIsUseSystemVulkanDriver = LauncherPreferences.isUseSystemVulkanDriver(this);
        this.binding.switchUseSystemVulkanDriver.setChecked(zIsUseSystemVulkanDriver);
        updateSystemVulkanDriverSwitchText(zIsUseSystemVulkanDriver);
        this.binding.switchUseSystemVulkanDriver.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda24
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                LauncherSettingsActivity.this.lambda$setupRendererSettings$18(compoundButton, z);
            }
        });
        boolean zIsUseOpenGlForMinecraft26Plus = LauncherPreferences.isUseOpenGlForMinecraft26Plus(this);
        this.binding.switchUseOpenGlFor26Plus.setChecked(zIsUseOpenGlForMinecraft26Plus);
        updateOpenGl26PlusSwitchText(zIsUseOpenGlForMinecraft26Plus);
        this.binding.switchUseOpenGlFor26Plus.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda25
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                LauncherSettingsActivity.this.lambda$setupRendererSettings$19(compoundButton, z);
            }
        });
        Renderers.reload(this);
        refreshRendererList();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupRendererSettings$14(View view) {
        openSelectedRendererPluginSettings();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupRendererSettings$15(View view) {
        openJavaLauncherStorageAccessSettings();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupRendererSettings$16(View view) {
        clearRendererPluginCache();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupRendererSettings$17(View view) {
        Renderers.reload(this);
        DriverPluginManager.reload(this);
        refreshRendererList();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupRendererSettings$18(CompoundButton compoundButton, boolean z) {
        LauncherPreferences.setUseSystemVulkanDriver(this, z);
        updateSystemVulkanDriverSwitchText(z);
        updateVulkanDriverSettings(getSelectedRendererFromSpinner());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupRendererSettings$19(CompoundButton compoundButton, boolean z) {
        LauncherPreferences.setUseOpenGlForMinecraft26Plus(this, z);
        updateOpenGl26PlusSwitchText(z);
    }

    private void refreshRendererList() {
        this.rendererSpinnerReady = false;
        this.availableRenderers.clear();
        this.availableRenderers.addAll(Renderers.getCompatibleRenderers(this));
        ArrayList arrayList = new ArrayList();
        for (RendererInterface rendererInterface : this.availableRenderers) {
            arrayList.add(rendererInterface.getRendererName() + (rendererInterface.isExternalPlugin() ? "  •  Plugin" : ""));
        }
        ArrayAdapter arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, arrayList);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.binding.spinnerRenderer.setAdapter((SpinnerAdapter) arrayAdapter);
        if (this.availableRenderers.isEmpty()) {
            this.binding.textRendererDescription.setText(R.string.renderer_none_found);
            updateRendererPluginButtons(null);
            updateMobileGluesConfigSummary(null);
            updateVulkanDriverSettings(null);
            return;
        }
        int iIndexOfRenderer = Renderers.indexOfRenderer(this.availableRenderers, LauncherPreferences.getSelectedRendererIdentifier(this));
        this.binding.spinnerRenderer.setSelection(iIndexOfRenderer, false);
        updateRendererDescription(this.availableRenderers.get(iIndexOfRenderer));
        updateRendererPluginButtons(this.availableRenderers.get(iIndexOfRenderer));
        updateVulkanDriverSettings(this.availableRenderers.get(iIndexOfRenderer));
        this.rendererSpinnerReady = true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateVulkanDriverSettings(RendererInterface rendererInterface) {
        boolean z = DriverPluginManager.isVulkanZinkRenderer(rendererInterface) && !LauncherPreferences.isUseSystemVulkanDriver(this);
        this.binding.layoutVulkanDriverSettings.setVisibility(z ? 0 : 8);
        if (!z) {
            this.driverSpinnerReady = false;
            this.availableDrivers.clear();
            this.binding.spinnerVulkanDriver.setAdapter((SpinnerAdapter) null);
            this.binding.textVulkanDriverDescription.setText("");
            return;
        }
        refreshVulkanDriverList();
    }

    private void refreshVulkanDriverList() {
        this.driverSpinnerReady = false;
        this.availableDrivers.clear();
        this.availableDrivers.addAll(DriverPluginManager.getDrivers(this));
        ArrayList arrayList = new ArrayList();
        Iterator<Driver> it = this.availableDrivers.iterator();
        while (it.hasNext()) {
            arrayList.add(it.next().getName());
        }
        ArrayAdapter arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, arrayList);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.binding.spinnerVulkanDriver.setAdapter((SpinnerAdapter) arrayAdapter);
        if (this.availableDrivers.isEmpty()) {
            this.binding.textVulkanDriverDescription.setText("");
            return;
        }
        int iIndexOfDriver = DriverPluginManager.indexOfDriver(this, LauncherPreferences.getSelectedVulkanDriverName(this));
        this.binding.spinnerVulkanDriver.setSelection(iIndexOfDriver, false);
        updateVulkanDriverDescription(this.availableDrivers.get(iIndexOfDriver));
        this.driverSpinnerReady = true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateVulkanDriverDescription(Driver driver) {
        String description = driver.getDescription();
        if (description == null || description.trim().isEmpty()) {
            description = "Uses the selected Vulkan driver for Vulkan/Zink rendering.";
        }
        this.binding.textVulkanDriverDescription.setText(getString(R.string.vulkan_driver_description_value, new Object[]{driver.getName(), description}));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateRendererDescription(RendererInterface rendererInterface) {
        this.binding.textRendererDescription.setText(buildFriendlyRendererDescription(rendererInterface));
        updateMobileGluesConfigSummary(rendererInterface);
    }

    private String buildFriendlyRendererDescription(RendererInterface rendererInterface) {
        String rendererName = rendererInterface.getRendererName();
        String lowerCase = (rendererInterface.getRendererName() + " " + rendererInterface.getRendererId() + " " + rendererInterface.getUniqueIdentifier() + " " + rendererInterface.getRendererLibrary()).toLowerCase();
        if (lowerCase.contains("mobileglues") || lowerCase.contains("mobile glues")) {
            return rendererName + "\nRecommended for most Android devices. Good balance of compatibility and performance for modern Minecraft versions.";
        }
        if (lowerCase.contains("vulkan") || lowerCase.contains("zink")) {
            return rendererName + "\nUses Vulkan/Zink rendering. Best for devices with strong Vulkan support, and useful for newer Minecraft versions or Vulkan-focused testing.";
        }
        if (lowerCase.contains("gl4es") || lowerCase.contains("opengles")) {
            return rendererName + "\nClassic OpenGL ES compatibility renderer. Useful for older Minecraft versions or devices that do not work well with Vulkan.";
        }
        if (lowerCase.contains("virgl")) {
            return rendererName + "\nCompatibility renderer for specific devices and setups. Try this if the recommended renderer does not work correctly.";
        }
        String rendererDescription = rendererInterface.getRendererDescription();
        if (rendererDescription != null && !rendererDescription.trim().isEmpty()) {
            return rendererName + "\n" + rendererDescription.trim();
        }
        return rendererName + "\nRuns Minecraft using this renderer.";
    }

    private void updateMobileGluesConfigSummary(RendererInterface rendererInterface) {
        String str;
        if (!MobileGluesConfigHelper.isMobileGluesRenderer(rendererInterface)) {
            this.binding.textRendererPluginConfig.setText("");
            this.binding.textRendererPluginConfig.setVisibility(8);
            this.binding.buttonGrantRendererStorageAccess.setVisibility(8);
            return;
        }
        this.binding.textRendererPluginConfig.setText(MobileGluesConfigHelper.buildSettingsSummary(this, rendererInterface));
        this.binding.textRendererPluginConfig.setVisibility(0);
        boolean zHasStorageAccess = MobileGluesConfigHelper.hasStorageAccess(this);
        this.binding.buttonGrantRendererStorageAccess.setVisibility(0);
        this.binding.buttonGrantRendererStorageAccess.setEnabled(true);
        MaterialButton materialButton = this.binding.buttonGrantRendererStorageAccess;
        if (zHasStorageAccess) {
            str = "Choose MobileGlues folder again";
        } else {
            str = "Choose MobileGlues folder";
        }
        materialButton.setText(str);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateRendererPluginButtons(RendererInterface rendererInterface) {
        this.binding.buttonImportRendererPlugin.setEnabled(rendererInterface != null && rendererInterface.isExternalPlugin());
        this.binding.buttonClearRendererPluginCache.setEnabled(RendererPluginManager.hasImportedOrCachedRendererPlugins(this));
    }

    private void openSelectedRendererPluginSettings() {
        RendererInterface selectedRendererFromSpinner = getSelectedRendererFromSpinner();
        if (selectedRendererFromSpinner == null || !selectedRendererFromSpinner.isExternalPlugin()) {
            return;
        }
        RendererPluginManager.openPluginApp(this, selectedRendererFromSpinner);
    }

    private void openJavaLauncherStorageAccessSettings() {
        if (this.mobileGluesFolderPickerLauncher == null) {
            return;
        }
        Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT_TREE");
        intent.addFlags(1);
        intent.addFlags(2);
        intent.addFlags(64);
        intent.addFlags(128);
        this.mobileGluesFolderPickerLauncher.launch(intent);
    }

    private RendererInterface getSelectedRendererFromSpinner() {
        int selectedItemPosition = this.binding.spinnerRenderer.getSelectedItemPosition();
        if (selectedItemPosition < 0 || selectedItemPosition >= this.availableRenderers.size()) {
            return null;
        }
        return this.availableRenderers.get(selectedItemPosition);
    }

    private void clearRendererPluginCache() {
        RendererPluginManager.clearImportedAndCachedRendererPlugins(this);
        Renderers.reload(this);
        refreshRendererList();
    }

    private void setupRenderSurfaceSettings() {
        boolean zIsUseNativeSurfaceView = LauncherPreferences.isUseNativeSurfaceView(this);
        this.binding.switchUseNativeSurface.setChecked(zIsUseNativeSurfaceView);
        updateRenderSurfaceSwitchText(zIsUseNativeSurfaceView);
        this.binding.switchUseNativeSurface.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda10
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                LauncherSettingsActivity.this.lambda$setupRenderSurfaceSettings$20(compoundButton, z);
            }
        });
        setupGameDisplaySettings();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupRenderSurfaceSettings$20(CompoundButton compoundButton, boolean z) {
        LauncherPreferences.setUseNativeSurfaceView(this, z);
        updateRenderSurfaceSwitchText(z);
    }

    private void setupGameDisplaySettings() {
        int gameResolutionScalePercent = LauncherPreferences.getGameResolutionScalePercent(this);
        this.binding.sliderGameResolutionScale.setMax(175);
        updateResolutionScaleUi(gameResolutionScalePercent);
        this.binding.sliderGameResolutionScale.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity.5
            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
                if (z) {
                    int i2 = i + 25;
                    LauncherPreferences.setGameResolutionScalePercent(LauncherSettingsActivity.this, i2);
                    LauncherSettingsActivity.this.updateResolutionScaleText(i2);
                }
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStopTrackingTouch(SeekBar seekBar) {
                int iClampGameResolutionScalePercent = LauncherPreferences.clampGameResolutionScalePercent(seekBar.getProgress() + 25);
                LauncherPreferences.setGameResolutionScalePercent(LauncherSettingsActivity.this, iClampGameResolutionScalePercent);
                LauncherSettingsActivity.this.updateResolutionScaleUi(iClampGameResolutionScalePercent);
            }
        });
        this.binding.textGameResolutionScale.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda41
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                LauncherSettingsActivity.this.lambda$setupGameDisplaySettings$21(view);
            }
        });
        boolean zIsForceFullscreenMode = LauncherPreferences.isForceFullscreenMode(this);
        this.binding.switchForceFullscreenMode.setChecked(zIsForceFullscreenMode);
        updateForceFullscreenSwitchText(zIsForceFullscreenMode);
        this.binding.switchForceFullscreenMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda42
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                LauncherSettingsActivity.this.lambda$setupGameDisplaySettings$22(compoundButton, z);
            }
        });
        boolean zIsAvoidRoundedDisplayCorners = LauncherPreferences.isAvoidRoundedDisplayCorners(this);
        this.binding.switchAvoidRoundedCorners.setChecked(zIsAvoidRoundedDisplayCorners);
        updateAvoidRoundedCornersSwitchText(zIsAvoidRoundedDisplayCorners);
        this.binding.switchAvoidRoundedCorners.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda43
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                LauncherSettingsActivity.this.lambda$setupGameDisplaySettings$23(compoundButton, z);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupGameDisplaySettings$21(View view) {
        openResolutionScaleInputDialog();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupGameDisplaySettings$22(CompoundButton compoundButton, boolean z) {
        LauncherPreferences.setForceFullscreenMode(this, z);
        updateForceFullscreenSwitchText(z);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupGameDisplaySettings$23(CompoundButton compoundButton, boolean z) {
        LauncherPreferences.setAvoidRoundedDisplayCorners(this, z);
        updateAvoidRoundedCornersSwitchText(z);
    }

    private void openResolutionScaleInputDialog() {
        int gameResolutionScalePercent = LauncherPreferences.getGameResolutionScalePercent(this);
        final EditText editText = new EditText(this);
        editText.setInputType(2);
        editText.setSingleLine(true);
        editText.setSelectAllOnFocus(true);
        editText.setText(String.valueOf(gameResolutionScalePercent));
        final AlertDialog alertDialogCreate = new AlertDialog.Builder(this).setTitle(R.string.settings_renderer_resolution_scale_title).setMessage(getString(R.string.settings_renderer_resolution_scale_dialog_message, new Object[]{25, 200})).setView(editText).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).create();
        alertDialogCreate.setOnShowListener(new DialogInterface.OnShowListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda28
            @Override // android.content.DialogInterface.OnShowListener
            public final void onShow(DialogInterface dialogInterface) {
                LauncherSettingsActivity.this.lambda$openResolutionScaleInputDialog$25(alertDialogCreate, editText, dialogInterface);
            }
        });
        alertDialogCreate.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$openResolutionScaleInputDialog$25(final AlertDialog alertDialog, final EditText editText, DialogInterface dialogInterface) {
        alertDialog.getButton(-1).setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda9
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                LauncherSettingsActivity.this.lambda$openResolutionScaleInputDialog$24(editText, alertDialog, view);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$openResolutionScaleInputDialog$24(EditText editText, AlertDialog alertDialog, View view) {
        int iClampGameResolutionScalePercent = LauncherPreferences.clampGameResolutionScalePercent(parseResolutionScaleInput(editText.getText() == null ? "" : editText.getText().toString()));
        LauncherPreferences.setGameResolutionScalePercent(this, iClampGameResolutionScalePercent);
        updateResolutionScaleUi(iClampGameResolutionScalePercent);
        alertDialog.dismiss();
    }

    private int parseResolutionScaleInput(String str) {
        try {
            return Integer.parseInt(str.trim());
        } catch (Throwable unused) {
            return LauncherPreferences.getGameResolutionScalePercent(this);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateResolutionScaleUi(int i) {
        int iClampGameResolutionScalePercent = LauncherPreferences.clampGameResolutionScalePercent(i);
        this.binding.sliderGameResolutionScale.setProgress(iClampGameResolutionScalePercent - 25);
        updateResolutionScaleText(iClampGameResolutionScalePercent);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateResolutionScaleText(int i) {
        this.binding.textGameResolutionScale.setText(getString(R.string.settings_renderer_resolution_scale_value, new Object[]{Integer.valueOf(LauncherPreferences.clampGameResolutionScalePercent(i))}));
    }

    private void setupControllerSettings() {
        this.binding.buttonEditBuiltInController.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda30
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                LauncherSettingsActivity.this.lambda$setupControllerSettings$28(view);
            }
        });
        this.binding.buttonManageTouchControls.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda31
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                LauncherSettingsActivity.this.lambda$setupControllerSettings$29(view);
            }
        });
        setupHardwareMouseDpiScaleSettings();
        refreshControllerSettingsValues();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupControllerSettings$26() {
        FullscreenUtils.enableImmersive(this);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupControllerSettings$27() {
        runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda36
            @Override // java.lang.Runnable
            public final void run() {
                LauncherSettingsActivity.this.lambda$setupControllerSettings$26();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupControllerSettings$28(View view) {
        GamepadMappingDialog.show(this, new GamepadMappingDialog.OnSettingsSavedListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda55
            @Override // ca.dnamobile.javalauncher.input.GamepadMappingDialog.OnSettingsSavedListener
            public final void onSettingsSaved() {
                LauncherSettingsActivity.this.lambda$setupControllerSettings$27();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupControllerSettings$29(View view) {
        startActivity(new Intent(this, (Class<?>) ControlsActivity.class));
    }

    private void setupHardwareMouseDpiScaleSettings() {
        ActivityLauncherSettingsBinding activityLauncherSettingsBinding = this.binding;
        if (activityLauncherSettingsBinding == null || activityLauncherSettingsBinding.layoutControllerSettings == null || this.binding.layoutControllerSettings.findViewWithTag("hardware_mouse_dpi_scale") != null) {
            return;
        }
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setTag("hardware_mouse_dpi_scale");
        linearLayout.setOrientation(1);
        linearLayout.setPadding(0, dp(14.0f), 0, 0);
        View view = new View(this);
        view.setBackgroundColor(855638016);
        linearLayout.addView(view, new LinearLayout.LayoutParams(-1, Math.max(1, dp(1.0f))));
        TextView textView = new TextView(this);
        textView.setText("Hardware mouse DPI scale");
        textView.setTextSize(16.0f);
        textView.setTypeface(textView.getTypeface(), 1);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = dp(12.0f);
        linearLayout.addView(textView, layoutParams);
        TextView textView2 = new TextView(this);
        textView2.setText("Adjusts real mouse / captured-pointer speed using a Zalith-style relative pointer multiplier. Touch camera movement, hotbar taps, and absolute menu taps are not scaled.");
        textView2.setTextSize(13.0f);
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-1, -2);
        layoutParams2.topMargin = dp(2.0f);
        linearLayout.addView(textView2, layoutParams2);
        TextView textView3 = new TextView(this);
        this.textHardwareMouseDpiScale = textView3;
        textView3.setTextSize(14.0f);
        TextView textView4 = this.textHardwareMouseDpiScale;
        textView4.setTypeface(textView4.getTypeface(), 1);
        LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(-1, -2);
        layoutParams3.topMargin = dp(8.0f);
        linearLayout.addView(this.textHardwareMouseDpiScale, layoutParams3);
        SeekBar seekBar = new SeekBar(this);
        this.sliderHardwareMouseDpiScale = seekBar;
        seekBar.setMax(275);
        this.sliderHardwareMouseDpiScale.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity.6
            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStartTrackingTouch(SeekBar seekBar2) {
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onProgressChanged(SeekBar seekBar2, int i, boolean z) {
                int i2 = i + 25;
                LauncherSettingsActivity.this.updateHardwareMouseDpiScaleText(i2);
                if (z) {
                    GamepadMappingStore.get(LauncherSettingsActivity.this).setHardwareMouseDpiScale(i2);
                }
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStopTrackingTouch(SeekBar seekBar2) {
                int progress = seekBar2.getProgress() + 25;
                GamepadMappingStore.get(LauncherSettingsActivity.this).setHardwareMouseDpiScale(progress);
                LauncherSettingsActivity.this.updateHardwareMouseDpiScaleUi(progress);
            }
        });
        linearLayout.addView(this.sliderHardwareMouseDpiScale, new LinearLayout.LayoutParams(-1, -2));
        this.binding.layoutControllerSettings.addView(linearLayout);
    }

    private void refreshControllerSettingsValues() {
        if (this.binding == null) {
            return;
        }
        boolean zIsTouchControlsEnabled = ControlsPreferences.isTouchControlsEnabled(this);
        this.binding.switchTouchControlsEnabled.setOnCheckedChangeListener(null);
        this.binding.switchTouchControlsEnabled.setChecked(zIsTouchControlsEnabled);
        updateTouchControlsSwitchText(zIsTouchControlsEnabled);
        this.binding.switchTouchControlsEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda34
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                LauncherSettingsActivity.this.lambda$refreshControllerSettingsValues$30(compoundButton, z);
            }
        });
        updateMinecraftTouchGestureSettingsUi();
        updateHardwareMouseDpiScaleUi(GamepadMappingStore.get(this).getHardwareMouseDpiScale());
        boolean zIsForceSdlControllerBridge = LauncherPreferences.isForceSdlControllerBridge(this);
        this.binding.switchForceSdlControllerBridge.setOnCheckedChangeListener(null);
        this.binding.switchForceSdlControllerBridge.setChecked(zIsForceSdlControllerBridge);
        updateForceSdlControllerBridgeSwitchText(zIsForceSdlControllerBridge);
        this.binding.switchForceSdlControllerBridge.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda35
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                LauncherSettingsActivity.this.lambda$refreshControllerSettingsValues$31(compoundButton, z);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$refreshControllerSettingsValues$30(CompoundButton compoundButton, boolean z) {
        ControlsPreferences.setTouchControlsEnabled(this, z);
        updateTouchControlsSwitchText(z);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$refreshControllerSettingsValues$31(CompoundButton compoundButton, boolean z) {
        LauncherPreferences.setForceSdlControllerBridge(this, z);
        updateForceSdlControllerBridgeSwitchText(z);
    }

    private void updateMinecraftTouchGestureSettingsUi() {
        boolean zIsMinecraftTouchGesturesEnabled = ControlsPreferences.isMinecraftTouchGesturesEnabled(this);
        this.binding.switchMinecraftTouchGestures.setOnCheckedChangeListener(null);
        this.binding.switchMinecraftTouchGestures.setChecked(zIsMinecraftTouchGesturesEnabled);
        updateMinecraftTouchGesturesSwitchText(zIsMinecraftTouchGesturesEnabled);
        this.binding.switchMinecraftTouchGestures.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda38
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                LauncherSettingsActivity.this.lambda$updateMinecraftTouchGestureSettingsUi$32(compoundButton, z);
            }
        });
        boolean zIsDoubleTapToDropEnabled = ControlsPreferences.isDoubleTapToDropEnabled(this);
        this.binding.switchDoubleTapToDrop.setOnCheckedChangeListener(null);
        this.binding.switchDoubleTapToDrop.setChecked(zIsDoubleTapToDropEnabled);
        updateDoubleTapToDropSwitchText(zIsDoubleTapToDropEnabled);
        updateDoubleTapToDropEnabledState(zIsMinecraftTouchGesturesEnabled);
        this.binding.switchDoubleTapToDrop.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda39
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                LauncherSettingsActivity.this.lambda$updateMinecraftTouchGestureSettingsUi$33(compoundButton, z);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$updateMinecraftTouchGestureSettingsUi$32(CompoundButton compoundButton, boolean z) {
        ControlsPreferences.setMinecraftTouchGesturesEnabled(this, z);
        updateMinecraftTouchGesturesSwitchText(z);
        updateDoubleTapToDropEnabledState(z);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$updateMinecraftTouchGestureSettingsUi$33(CompoundButton compoundButton, boolean z) {
        ControlsPreferences.setDoubleTapToDropEnabled(this, z);
        updateDoubleTapToDropSwitchText(z);
    }

    private void updateMinecraftTouchGesturesSwitchText(boolean z) {
        int i;
        SwitchMaterial switchMaterial = this.binding.switchMinecraftTouchGestures;
        if (z) {
            i = R.string.controller_minecraft_touch_gestures_on;
        } else {
            i = R.string.controller_minecraft_touch_gestures_off;
        }
        switchMaterial.setText(i);
    }

    private void updateDoubleTapToDropSwitchText(boolean z) {
        int i;
        SwitchMaterial switchMaterial = this.binding.switchDoubleTapToDrop;
        if (z) {
            i = R.string.controller_double_tap_to_drop_on;
        } else {
            i = R.string.controller_double_tap_to_drop_off;
        }
        switchMaterial.setText(i);
    }

    private void updateDoubleTapToDropEnabledState(boolean z) {
        this.binding.switchDoubleTapToDrop.setEnabled(z);
        this.binding.textDoubleTapToDropSummary.setAlpha(z ? 1.0f : 0.55f);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateHardwareMouseDpiScaleUi(int i) {
        int iMax = Math.max(25, Math.min(300, i));
        SeekBar seekBar = this.sliderHardwareMouseDpiScale;
        if (seekBar != null) {
            seekBar.setProgress(iMax - 25);
        }
        updateHardwareMouseDpiScaleText(iMax);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateHardwareMouseDpiScaleText(int i) {
        TextView textView = this.textHardwareMouseDpiScale;
        if (textView != null) {
            textView.setText("Mouse DPI scale: " + i + "%");
        }
    }

    private void updateTouchControlsSwitchText(boolean z) {
        int i;
        SwitchMaterial switchMaterial = this.binding.switchTouchControlsEnabled;
        if (z) {
            i = R.string.controller_touch_controls_enabled_on;
        } else {
            i = R.string.controller_touch_controls_enabled_off;
        }
        switchMaterial.setText(i);
    }

    private void updateForceSdlControllerBridgeSwitchText(boolean z) {
        int i;
        SwitchMaterial switchMaterial = this.binding.switchForceSdlControllerBridge;
        if (z) {
            i = R.string.controller_force_sdl_on;
        } else {
            i = R.string.controller_force_sdl_off;
        }
        switchMaterial.setText(i);
    }

    private void registerNotificationPermissionLauncher() {
        this.notificationPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda15
            @Override // androidx.activity.result.ActivityResultCallback
            public final void onActivityResult(Object obj) {
                LauncherSettingsActivity.this.lambda$registerNotificationPermissionLauncher$34((Boolean) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$registerNotificationPermissionLauncher$34(Boolean bool) {
        int i;
        LauncherNotificationPermissionHelper.setBackgroundInstallNotificationsEnabled(this, bool.booleanValue());
        updateInstallNotificationSettingsUi();
        if (bool.booleanValue()) {
            i = R.string.notification_permission_enabled_toast;
        } else {
            i = R.string.notification_permission_denied_toast;
        }
        Toast.makeText(this, i, !bool.booleanValue() ? 1 : 0).show();
        if (bool.booleanValue() || !LauncherNotificationPermissionHelper.requiresRuntimePermission()) {
            return;
        }
        showNotificationDeniedSettingsDialog();
    }

    private void setupInstallNotificationSettings() {
        updateInstallNotificationSettingsUi();
    }

    private void updateInstallNotificationSettingsUi() {
        int i;
        ActivityLauncherSettingsBinding activityLauncherSettingsBinding = this.binding;
        if (activityLauncherSettingsBinding == null || activityLauncherSettingsBinding.switchInstallNotifications == null) {
            return;
        }
        boolean zHasPostNotificationsPermission = LauncherNotificationPermissionHelper.hasPostNotificationsPermission(this);
        boolean z = LauncherNotificationPermissionHelper.isBackgroundInstallNotificationsEnabled(this) && zHasPostNotificationsPermission;
        this.binding.switchInstallNotifications.setOnCheckedChangeListener(null);
        this.binding.switchInstallNotifications.setChecked(z);
        SwitchMaterial switchMaterial = this.binding.switchInstallNotifications;
        if (z) {
            i = R.string.install_notifications_on;
        } else {
            i = R.string.install_notifications_off;
        }
        switchMaterial.setText(i);
        if (!LauncherNotificationPermissionHelper.requiresRuntimePermission()) {
            this.binding.textInstallNotificationsSummary.setText(R.string.install_notifications_summary_old_android);
        } else if (zHasPostNotificationsPermission) {
            this.binding.textInstallNotificationsSummary.setText(R.string.install_notifications_summary_enabled);
        } else {
            this.binding.textInstallNotificationsSummary.setText(R.string.install_notifications_summary_permission_needed);
        }
        this.binding.switchInstallNotifications.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda51
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z2) {
                LauncherSettingsActivity.this.lambda$updateInstallNotificationSettingsUi$35(compoundButton, z2);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$updateInstallNotificationSettingsUi$35(CompoundButton compoundButton, boolean z) {
        if (!z) {
            LauncherNotificationPermissionHelper.setBackgroundInstallNotificationsEnabled(this, false);
            updateInstallNotificationSettingsUi();
        } else {
            if (LauncherNotificationPermissionHelper.hasPostNotificationsPermission(this)) {
                LauncherNotificationPermissionHelper.setBackgroundInstallNotificationsEnabled(this, true);
                updateInstallNotificationSettingsUi();
                return;
            }
            LauncherNotificationPermissionHelper.setBackgroundInstallNotificationsEnabled(this, true);
            ActivityResultLauncher<String> activityResultLauncher = this.notificationPermissionLauncher;
            if (activityResultLauncher != null) {
                LauncherNotificationPermissionHelper.requestPostNotificationsPermission(activityResultLauncher);
            }
        }
    }

    private void showNotificationDeniedSettingsDialog() {
        new AlertDialog.Builder(this).setTitle(R.string.notification_permission_denied_title).setMessage(R.string.notification_permission_denied_message).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.notification_permission_open_settings, new DialogInterface.OnClickListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda29
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                LauncherSettingsActivity.this.lambda$showNotificationDeniedSettingsDialog$36(dialogInterface, i);
            }
        }).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showNotificationDeniedSettingsDialog$36(DialogInterface dialogInterface, int i) {
        LauncherNotificationPermissionHelper.openAppNotificationSettings(this);
    }

    private void registerMicrophonePermissionLauncher() {
        this.microphonePermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda50
            @Override // androidx.activity.result.ActivityResultCallback
            public final void onActivityResult(Object obj) {
                LauncherSettingsActivity.this.lambda$registerMicrophonePermissionLauncher$37((Boolean) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$registerMicrophonePermissionLauncher$37(Boolean bool) {
        int i;
        updateSimpleVoiceChatPermissionUi();
        if (bool.booleanValue()) {
            i = R.string.simple_voice_chat_permission_granted_toast;
        } else {
            i = R.string.simple_voice_chat_permission_denied_toast;
        }
        Toast.makeText(this, i, !bool.booleanValue() ? 1 : 0).show();
        if (bool.booleanValue()) {
            return;
        }
        showSimpleVoiceChatPermissionDeniedDialog();
    }

    private void registerMobileGluesFolderPickerLauncher() {
        this.mobileGluesFolderPickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda44
            @Override // androidx.activity.result.ActivityResultCallback
            public final void onActivityResult(Object obj) {
                LauncherSettingsActivity.this.lambda$registerMobileGluesFolderPickerLauncher$38((ActivityResult) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$registerMobileGluesFolderPickerLauncher$38(ActivityResult activityResult) {
        Uri data;
        if (activityResult.getResultCode() != -1 || activityResult.getData() == null || (data = activityResult.getData().getData()) == null) {
            return;
        }
        try {
            getContentResolver().takePersistableUriPermission(data, activityResult.getData().getFlags() & 3);
        } catch (Throwable unused) {
        }
        MobileGluesConfigHelper.setSelectedConfigTreeUri(this, data);
        updateMobileGluesConfigSummary(getSelectedRendererFromSpinner());
        Toast.makeText(this, "MobileGlues folder saved.", 0).show();
    }

    private void setupSimpleVoiceChatSettings() {
        updateSimpleVoiceChatPermissionUi();
        this.binding.buttonSimpleVoiceChatMicrophonePermission.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda62
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                LauncherSettingsActivity.this.lambda$setupSimpleVoiceChatSettings$39(view);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupSimpleVoiceChatSettings$39(View view) {
        if (AndroidMicrophonePermission.isGranted(this)) {
            showSimpleVoiceChatPermissionGrantedDialog();
            return;
        }
        ActivityResultLauncher<String> activityResultLauncher = this.microphonePermissionLauncher;
        if (activityResultLauncher != null) {
            activityResultLauncher.launch("android.permission.RECORD_AUDIO");
        } else {
            AndroidMicrophonePermission.showRequestDialog(this);
        }
    }

    private void updateSimpleVoiceChatPermissionUi() {
        int i;
        int i2;
        ActivityLauncherSettingsBinding activityLauncherSettingsBinding = this.binding;
        if (activityLauncherSettingsBinding == null || activityLauncherSettingsBinding.buttonSimpleVoiceChatMicrophonePermission == null || this.binding.textSimpleVoiceChatMicrophoneStatus == null) {
            return;
        }
        boolean zIsGranted = AndroidMicrophonePermission.isGranted(this);
        TextView textView = this.binding.textSimpleVoiceChatMicrophoneStatus;
        if (zIsGranted) {
            i = R.string.simple_voice_chat_microphone_status_granted;
        } else {
            i = R.string.simple_voice_chat_microphone_status_missing;
        }
        textView.setText(i);
        MaterialButton materialButton = this.binding.buttonSimpleVoiceChatMicrophonePermission;
        if (zIsGranted) {
            i2 = R.string.simple_voice_chat_microphone_button_enabled;
        } else {
            i2 = R.string.simple_voice_chat_microphone_button_enable;
        }
        materialButton.setText(i2);
    }

    private void showSimpleVoiceChatPermissionGrantedDialog() {
        new AlertDialog.Builder(this).setTitle(R.string.simple_voice_chat_microphone_title).setMessage(R.string.simple_voice_chat_microphone_already_granted).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).show();
    }

    private void showSimpleVoiceChatPermissionDeniedDialog() {
        new AlertDialog.Builder(this).setTitle(R.string.simple_voice_chat_microphone_title).setMessage(R.string.simple_voice_chat_microphone_denied_message).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.simple_voice_chat_open_app_settings, new DialogInterface.OnClickListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda40
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                LauncherSettingsActivity.this.lambda$showSimpleVoiceChatPermissionDeniedDialog$40(dialogInterface, i);
            }
        }).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showSimpleVoiceChatPermissionDeniedDialog$40(DialogInterface dialogInterface, int i) {
        AndroidMicrophonePermission.openAppSettings(this);
    }

    private void setupLauncherSettings() {
        setupMemorySettings();
        setupInstallNotificationSettings();
        setupSimpleVoiceChatSettings();
        this.binding.checkKeepLogs.setChecked(LauncherLogManager.isKeepLogHistoryEnabled(this));
        this.binding.checkKeepLogs.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda57
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                LauncherSettingsActivity.this.lambda$setupLauncherSettings$41(compoundButton, z);
            }
        });
        boolean zIsShowInGameSettingsButton = LauncherPreferences.isShowInGameSettingsButton(this);
        this.binding.switchShowInGameSettingsButton.setChecked(zIsShowInGameSettingsButton);
        updateInGameSettingsButtonSwitchText(zIsShowInGameSettingsButton);
        this.binding.switchShowInGameSettingsButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda58
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                LauncherSettingsActivity.this.lambda$setupLauncherSettings$42(compoundButton, z);
            }
        });
        boolean zIsShowGameLogOverlay = LauncherPreferences.isShowGameLogOverlay(this);
        this.binding.switchShowGameLogOverlay.setChecked(zIsShowGameLogOverlay);
        updateGameLogOverlaySwitchText(zIsShowGameLogOverlay);
        this.binding.switchShowGameLogOverlay.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda59
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                LauncherSettingsActivity.this.lambda$setupLauncherSettings$43(compoundButton, z);
            }
        });
        setupFloatingGameOverlaySettings();
        this.binding.buttonShareLatestLog.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda60
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                LauncherSettingsActivity.this.lambda$setupLauncherSettings$44(view);
            }
        });
        setupUpdateCheckerSettings();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupLauncherSettings$41(CompoundButton compoundButton, boolean z) {
        LauncherLogManager.setKeepLogHistoryEnabled(this, z);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupLauncherSettings$42(CompoundButton compoundButton, boolean z) {
        LauncherPreferences.setShowInGameSettingsButton(this, z);
        updateInGameSettingsButtonSwitchText(z);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupLauncherSettings$43(CompoundButton compoundButton, boolean z) {
        LauncherPreferences.setShowGameLogOverlay(this, z);
        updateGameLogOverlaySwitchText(z);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupLauncherSettings$44(View view) {
        LauncherLogManager.shareLatestLog(this);
    }

    private void setupFloatingGameOverlaySettings() {
        ActivityLauncherSettingsBinding activityLauncherSettingsBinding = this.binding;
        if (activityLauncherSettingsBinding == null || activityLauncherSettingsBinding.switchShowInGameSettingsButton == null || !(this.binding.switchShowInGameSettingsButton.getParent() instanceof ViewGroup)) {
            return;
        }
        ViewGroup viewGroup = (ViewGroup) this.binding.switchShowInGameSettingsButton.getParent();
        if (viewGroup.findViewWithTag("floating_game_overlay_settings") != null) {
            return;
        }
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setTag("floating_game_overlay_settings");
        linearLayout.setOrientation(1);
        linearLayout.setPadding(0, dp(12.0f), 0, 0);
        viewGroup.addView(linearLayout, new ViewGroup.LayoutParams(-1, -2));
        View view = new View(this);
        view.setBackgroundColor(855638016);
        linearLayout.addView(view, new LinearLayout.LayoutParams(-1, Math.max(1, dp(1.0f))));
        TextView textView = new TextView(this);
        textView.setText("In-game overlay");
        textView.setTextSize(16.0f);
        textView.setTypeface(textView.getTypeface(), 1);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = dp(12.0f);
        linearLayout.addView(textView, layoutParams);
        final SwitchMaterial switchMaterial = new SwitchMaterial(this);
        switchMaterial.setChecked(GameOverlayPreferences.isShowGameFpsCounter(this));
        updateFloatingFpsSwitchText(switchMaterial, switchMaterial.isChecked());
        switchMaterial.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda17
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                LauncherSettingsActivity.this.lambda$setupFloatingGameOverlaySettings$45(switchMaterial, compoundButton, z);
            }
        });
        linearLayout.addView(switchMaterial, new LinearLayout.LayoutParams(-1, -2));
        TextView textView2 = new TextView(this);
        textView2.setText("Shows real-time FPS on the floating settings button while Minecraft is running.");
        textView2.setTextSize(13.0f);
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-1, -2);
        layoutParams2.topMargin = dp(2.0f);
        linearLayout.addView(textView2, layoutParams2);
        TextView textView3 = new TextView(this);
        textView3.setText("Floating settings button position");
        textView3.setTextSize(15.0f);
        textView3.setTypeface(textView3.getTypeface(), 1);
        LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(-1, -2);
        layoutParams3.topMargin = dp(12.0f);
        linearLayout.addView(textView3, layoutParams3);
        Spinner spinner = new Spinner(this);
        ArrayAdapter arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, GameOverlayPreferences.getPlacementLabels());
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter((SpinnerAdapter) arrayAdapter);
        spinner.setSelection(GameOverlayPreferences.indexOfPlacement(GameOverlayPreferences.getGameSettingsButtonPlacement(this)), false);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity.7
            private boolean ready;

            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onNothingSelected(AdapterView<?> adapterView) {
            }

            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onItemSelected(AdapterView<?> adapterView, View view2, int i, long j) {
                if (!this.ready) {
                    this.ready = true;
                } else {
                    GameOverlayPreferences.setGameSettingsButtonPlacement(LauncherSettingsActivity.this, GameOverlayPreferences.placementValueForIndex(i));
                }
            }
        });
        LinearLayout.LayoutParams layoutParams4 = new LinearLayout.LayoutParams(-1, -2);
        layoutParams4.topMargin = dp(6.0f);
        linearLayout.addView(spinner, layoutParams4);
        TextView textView4 = new TextView(this);
        textView4.setText("This controls the default corner. Dragging the button in game saves a custom position until you reset it or pick a new corner.");
        textView4.setTextSize(13.0f);
        LinearLayout.LayoutParams layoutParams5 = new LinearLayout.LayoutParams(-1, -2);
        layoutParams5.topMargin = dp(2.0f);
        linearLayout.addView(textView4, layoutParams5);
        MaterialButton materialButton = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        materialButton.setText("Reset floating button position");
        materialButton.setAllCaps(false);
        materialButton.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda18
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                LauncherSettingsActivity.this.lambda$setupFloatingGameOverlaySettings$46(view2);
            }
        });
        LinearLayout.LayoutParams layoutParams6 = new LinearLayout.LayoutParams(-2, -2);
        layoutParams6.topMargin = dp(8.0f);
        linearLayout.addView(materialButton, layoutParams6);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupFloatingGameOverlaySettings$45(SwitchMaterial switchMaterial, CompoundButton compoundButton, boolean z) {
        GameOverlayPreferences.setShowGameFpsCounter(this, z);
        updateFloatingFpsSwitchText(switchMaterial, z);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupFloatingGameOverlaySettings$46(View view) {
        GameOverlayPreferences.resetGameSettingsButtonCustomPosition(this);
    }

    private void updateFloatingFpsSwitchText(CompoundButton compoundButton, boolean z) {
        compoundButton.setText(z ? "Show FPS counter: On" : "Show FPS counter: Off");
    }

    private void setupUpdateCheckerSettings() {
        ActivityLauncherSettingsBinding activityLauncherSettingsBinding = this.binding;
        if (activityLauncherSettingsBinding == null || activityLauncherSettingsBinding.buttonShareLatestLog == null || !(this.binding.buttonShareLatestLog.getParent() instanceof ViewGroup)) {
            return;
        }
        ViewGroup viewGroup = (ViewGroup) this.binding.buttonShareLatestLog.getParent();
        if (viewGroup.findViewWithTag("update_checker_settings") != null) {
            return;
        }
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setTag("update_checker_settings");
        linearLayout.setOrientation(1);
        linearLayout.setPadding(0, dp(12.0f), 0, 0);
        viewGroup.addView(linearLayout, new ViewGroup.LayoutParams(-1, -2));
        TextView textView = new TextView(this);
        textView.setText("Launcher updates");
        textView.setTextSize(16.0f);
        textView.setGravity(GravityCompat.START);
        linearLayout.addView(textView, new LinearLayout.LayoutParams(-1, -2));
        TextView textView2 = new TextView(this);
        textView2.setText("Checks GitHub releases for newer DroidBridge builds.");
        textView2.setTextSize(13.0f);
        textView2.setPadding(0, dp(2.0f), 0, dp(6.0f));
        linearLayout.addView(textView2, new LinearLayout.LayoutParams(-1, -2));
        CheckBox checkBox = new CheckBox(this);
        checkBox.setText("Check for updates on startup");
        checkBox.setChecked(LauncherUpdatePreferences.isAutoCheckEnabled(this));
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda26
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                LauncherSettingsActivity.this.lambda$setupUpdateCheckerSettings$47(compoundButton, z);
            }
        });
        linearLayout.addView(checkBox, new LinearLayout.LayoutParams(-1, -2));
        MaterialButton materialButton = new MaterialButton(this);
        materialButton.setText("Check for updates");
        materialButton.setAllCaps(false);
        materialButton.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda27
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                LauncherSettingsActivity.this.lambda$setupUpdateCheckerSettings$48(view);
            }
        });
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = dp(6.0f);
        linearLayout.addView(materialButton, layoutParams);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupUpdateCheckerSettings$47(CompoundButton compoundButton, boolean z) {
        LauncherUpdatePreferences.setAutoCheckEnabled(this, z);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupUpdateCheckerSettings$48(View view) {
        LauncherUpdateDialogs.checkManually(this);
    }

    private void setupPrivacyPolicySettings() {
        setupLegalLinkButton(this.binding.buttonOpenMinecraftEula, LegalLinks.MINECRAFT_EULA_URL, "Minecraft EULA link is not configured.");
        setupLegalLinkButton(this.binding.buttonOpenPrivacyPolicy, LegalLinks.DROIDBRIDGE_PRIVACY_POLICY_URL, "Privacy Policy link is not available yet.");
        setupLegalLinkButton(this.binding.buttonOpenDroidBridgeTerms, LegalLinks.DROIDBRIDGE_TERMS_URL, "DroidBridge Terms of Service link is not available yet.");
        setupLegalLinkButton(this.binding.buttonOpenDroidBridgeLicense, LegalLinks.DROIDBRIDGE_LICENSING, "DroidBridge Terms of Service link is not available yet.");
    }

    private void setupLegalLinkButton(MaterialButton materialButton, final String str, final String str2) {
        final boolean z = !isNullOrBlank(str);
        materialButton.setEnabled(z);
        materialButton.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda37
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                LauncherSettingsActivity.this.lambda$setupLegalLinkButton$49(z, str, str2, view);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupLegalLinkButton$49(boolean z, String str, String str2, View view) {
        if (z && LegalLinks.open(this, str)) {
            return;
        }
        Toast.makeText(this, str2, 1).show();
    }

    private void setupMemorySettings() {
        int maxAllocatableMemoryMb = MemoryAllocationUtils.getMaxAllocatableMemoryMb(this);
        int iResolveAllocatedMemoryMb = MemoryAllocationUtils.resolveAllocatedMemoryMb(this);
        updateMemorySeekBarBounds(iResolveAllocatedMemoryMb);
        updateMemoryText(iResolveAllocatedMemoryMb);
        updateAvailableMemorySummary(maxAllocatableMemoryMb);
        this.binding.sliderAllocatedRam.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity.8
            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
                if (z) {
                    int iMemoryFromSeekBarProgress = LauncherSettingsActivity.this.memoryFromSeekBarProgress(i);
                    LauncherPreferences.setAllocatedMemoryMb(LauncherSettingsActivity.this, iMemoryFromSeekBarProgress);
                    LauncherSettingsActivity.this.updateMemoryText(iMemoryFromSeekBarProgress);
                }
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStopTrackingTouch(SeekBar seekBar) {
                int iClampToAllowedRam = MemoryAllocationUtils.clampToAllowedRam(LauncherSettingsActivity.this, LauncherSettingsActivity.this.memoryFromSeekBarProgress(seekBar.getProgress()));
                LauncherPreferences.setAllocatedMemoryMb(LauncherSettingsActivity.this, iClampToAllowedRam);
                LauncherSettingsActivity.this.updateMemorySlider(iClampToAllowedRam);
            }
        });
        this.binding.textAllocatedRam.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda2
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                LauncherSettingsActivity.this.lambda$setupMemorySettings$50(view);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupMemorySettings$50(View view) {
        openMemoryInputDialog();
    }

    private void openMemoryInputDialog() {
        int maxAllocatableMemoryMb = MemoryAllocationUtils.getMaxAllocatableMemoryMb(this);
        int iResolveAllocatedMemoryMb = MemoryAllocationUtils.resolveAllocatedMemoryMb(this);
        final EditText editText = new EditText(this);
        editText.setInputType(2);
        editText.setSingleLine(true);
        editText.setSelectAllOnFocus(true);
        editText.setText(String.valueOf(iResolveAllocatedMemoryMb));
        final AlertDialog alertDialogCreate = new AlertDialog.Builder(this).setTitle(R.string.memory_dialog_title).setMessage(getString(R.string.memory_dialog_message, new Object[]{Integer.valueOf(maxAllocatableMemoryMb)})).setView(editText).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).create();
        alertDialogCreate.setOnShowListener(new DialogInterface.OnShowListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda53
            @Override // android.content.DialogInterface.OnShowListener
            public final void onShow(DialogInterface dialogInterface) {
                LauncherSettingsActivity.this.lambda$openMemoryInputDialog$52(alertDialogCreate, editText, dialogInterface);
            }
        });
        alertDialogCreate.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$openMemoryInputDialog$52(final AlertDialog alertDialog, final EditText editText, DialogInterface dialogInterface) {
        alertDialog.getButton(-1).setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda56
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                LauncherSettingsActivity.this.lambda$openMemoryInputDialog$51(editText, alertDialog, view);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$openMemoryInputDialog$51(EditText editText, AlertDialog alertDialog, View view) {
        int iClampToAllowedRam = MemoryAllocationUtils.clampToAllowedRam(this, parseMemoryInput(editText.getText() == null ? "" : editText.getText().toString()));
        LauncherPreferences.setAllocatedMemoryMb(this, iClampToAllowedRam);
        updateMemorySlider(iClampToAllowedRam);
        alertDialog.dismiss();
    }

    private int parseMemoryInput(String str) {
        try {
            return Integer.parseInt(str.trim());
        } catch (Throwable unused) {
            return MemoryAllocationUtils.resolveAllocatedMemoryMb(this);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateMemorySlider(int i) {
        int maxAllocatableMemoryMb = MemoryAllocationUtils.getMaxAllocatableMemoryMb(this);
        int iClampToAllowedRam = MemoryAllocationUtils.clampToAllowedRam(this, i);
        updateMemorySeekBarBounds(iClampToAllowedRam);
        updateMemoryText(iClampToAllowedRam);
        updateAvailableMemorySummary(maxAllocatableMemoryMb);
    }

    private void updateMemorySeekBarBounds(int i) {
        int maxAllocatableMemoryMb = MemoryAllocationUtils.getMaxAllocatableMemoryMb(this);
        int minimumMemoryMb = MemoryAllocationUtils.getMinimumMemoryMb(maxAllocatableMemoryMb);
        int iClampToAllowedRam = MemoryAllocationUtils.clampToAllowedRam(this, i);
        this.binding.sliderAllocatedRam.setMax(Math.max(1, (maxAllocatableMemoryMb - minimumMemoryMb) / 256));
        this.binding.sliderAllocatedRam.setProgress(progressFromMemory(iClampToAllowedRam));
    }

    private int progressFromMemory(int i) {
        return Math.max(0, (MemoryAllocationUtils.clampToAllowedRam(this, i) - MemoryAllocationUtils.getMinimumMemoryMb(MemoryAllocationUtils.getMaxAllocatableMemoryMb(this))) / 256);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int memoryFromSeekBarProgress(int i) {
        return MemoryAllocationUtils.clampToAllowedRam(this, MemoryAllocationUtils.getMinimumMemoryMb(MemoryAllocationUtils.getMaxAllocatableMemoryMb(this)) + (Math.max(0, i) * 256));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateMemoryText(int i) {
        this.binding.textAllocatedRam.setText(getString(R.string.memory_allocated_value, new Object[]{Integer.valueOf(i), Float.valueOf(i / 1024.0f)}));
    }

    private void updateAvailableMemorySummary(int i) {
        int totalMemoryMb = MemoryAllocationUtils.getTotalMemoryMb(this);
        this.binding.textAvailableRamSummary.setText(getString(R.string.memory_available_summary, new Object[]{Integer.valueOf(i), Float.valueOf(i / 1024.0f), Integer.valueOf(totalMemoryMb), Float.valueOf(totalMemoryMb / 1024.0f)}));
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

    private void updateRemoveInheritedVanillaSwitchText(boolean z) {
        int i;
        SwitchMaterial switchMaterial = this.binding.switchRemoveInheritedVanilla;
        if (z) {
            i = R.string.inherited_vanilla_remove_on;
        } else {
            i = R.string.inherited_vanilla_remove_off;
        }
        switchMaterial.setText(i);
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

    private void updateForceFullscreenSwitchText(boolean z) {
        int i;
        SwitchMaterial switchMaterial = this.binding.switchForceFullscreenMode;
        if (z) {
            i = R.string.settings_renderer_full_screen_on;
        } else {
            i = R.string.settings_renderer_full_screen_off;
        }
        switchMaterial.setText(i);
    }

    private void updateAvoidRoundedCornersSwitchText(boolean z) {
        int i;
        SwitchMaterial switchMaterial = this.binding.switchAvoidRoundedCorners;
        if (z) {
            i = R.string.settings_renderer_avoid_rounded_corners_on;
        } else {
            i = R.string.settings_renderer_avoid_rounded_corners_off;
        }
        switchMaterial.setText(i);
    }

    private void updateSystemVulkanDriverSwitchText(boolean z) {
        int i;
        SwitchMaterial switchMaterial = this.binding.switchUseSystemVulkanDriver;
        if (z) {
            i = R.string.use_system_vulkan_driver_on;
        } else {
            i = R.string.use_system_vulkan_driver_off;
        }
        switchMaterial.setText(i);
    }

    private void updateOpenGl26PlusSwitchText(boolean z) {
        int i;
        SwitchMaterial switchMaterial = this.binding.switchUseOpenGlFor26Plus;
        if (z) {
            i = R.string.use_opengl_26_plus_on;
        } else {
            i = R.string.use_opengl_26_plus_off;
        }
        switchMaterial.setText(i);
    }

    private void updateInGameSettingsButtonSwitchText(boolean z) {
        int i;
        SwitchMaterial switchMaterial = this.binding.switchShowInGameSettingsButton;
        if (z) {
            i = R.string.game_settings_button_on;
        } else {
            i = R.string.game_settings_button_off;
        }
        switchMaterial.setText(i);
    }

    private void updateGameLogOverlaySwitchText(boolean z) {
        int i;
        SwitchMaterial switchMaterial = this.binding.switchShowGameLogOverlay;
        if (z) {
            i = R.string.game_log_overlay_on;
        } else {
            i = R.string.game_log_overlay_off;
        }
        switchMaterial.setText(i);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateAccountStatus(AccountStore.Account account) {
        AccountStore accountStore = this.accountStore;
        boolean z = accountStore != null && accountStore.hasStoredMicrosoftAccount();
        AccountStore accountStore2 = this.accountStore;
        boolean z2 = accountStore2 != null && accountStore2.hasMicrosoftLoginCompletedOnce();
        boolean z3 = account != null && account.isOfflineAccount();
        boolean z4 = account != null && account.isMicrosoftAccount();
        this.binding.buttonSignIn.setVisibility(z ? 8 : 0);
        this.binding.buttonSignOut.setVisibility((z || account != null) ? 0 : 8);
        this.binding.buttonManageOfflineAccounts.setVisibility(z2 ? 0 : 8);
        this.binding.buttonManageOfflineAccounts.setEnabled(z2);
        this.binding.buttonUseMicrosoftAccount.setVisibility((z && z3) ? 0 : 8);
        this.binding.buttonRefreshMicrosoftSkin.setVisibility(z ? 0 : 8);
        this.binding.buttonRefreshMicrosoftSkin.setEnabled(z);
        if (z3) {
            AccountStore accountStore3 = this.accountStore;
            AccountStore.Account accountLoadLastMicrosoftAccount = accountStore3 != null ? accountStore3.loadLastMicrosoftAccount() : null;
            this.binding.textAccountStatus.setText(getString(R.string.status_offline_account_with_microsoft, new Object[]{account.getBestDisplayName(), accountLoadLastMicrosoftAccount != null ? accountLoadLastMicrosoftAccount.getBestDisplayName() : "Microsoft account"}));
        } else {
            if (z4) {
                this.binding.textAccountStatus.setText(getString(R.string.status_signed_in, new Object[]{account.getBestDisplayName()}));
                return;
            }
            if (z) {
                AccountStore.Account accountLoadLastMicrosoftAccount2 = this.accountStore.loadLastMicrosoftAccount();
                this.binding.textAccountStatus.setText(getString(R.string.status_microsoft_remembered, new Object[]{accountLoadLastMicrosoftAccount2 != null ? accountLoadLastMicrosoftAccount2.getBestDisplayName() : "Microsoft Player"}));
            } else if (z2) {
                this.binding.textAccountStatus.setText(R.string.status_signed_out_offline_unlocked);
            } else {
                this.binding.textAccountStatus.setText(R.string.status_signed_out);
            }
        }
    }

    private void useRememberedMicrosoftAccount() {
        AccountStore accountStore = this.accountStore;
        if (accountStore == null) {
            return;
        }
        try {
            accountStore.useLastMicrosoftAccount();
            AccountStore.Account accountLoad = this.accountStore.load();
            updateAccountStatus(accountLoad);
            updateSkinUi(accountLoad);
            Toast.makeText(this, R.string.microsoft_account_restored, 0).show();
        } catch (Throwable th) {
            Toast.makeText(this, th.getMessage() != null ? th.getMessage() : th.toString(), 1).show();
        }
    }

    private void showOfflineAccountsDialog() {
        AccountStore accountStore = this.accountStore;
        if (accountStore == null || !accountStore.hasMicrosoftLoginCompletedOnce()) {
            new AlertDialog.Builder(this).setTitle(R.string.offline_locked_title).setMessage(R.string.offline_locked_message).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).show();
            return;
        }
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(false);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        int iDp = dp(24.0f);
        linearLayout.setPadding(iDp, dp(18.0f), iDp, dp(4.0f));
        scrollView.addView(linearLayout, new FrameLayout.LayoutParams(-1, -2));
        linearLayout.addView(buildDialogHeader(R.drawable.ic_player_head_placeholder, R.string.offline_accounts_title, R.string.offline_accounts_dialog_summary));
        TextView textView = new TextView(this);
        textView.setText(R.string.offline_accounts_section_title);
        textView.setTextAppearance(android.R.style.TextAppearance.Material.Medium);
        textView.setTypeface(textView.getTypeface(), 1);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = dp(18.0f);
        linearLayout.addView(textView, layoutParams);
        ArrayList<AccountStore.Account> arrayListListOfflineAccounts = this.accountStore.listOfflineAccounts();
        if (arrayListListOfflineAccounts.isEmpty()) {
            linearLayout.addView(buildEmptyOfflineAccountCard());
        } else {
            AccountStore.Account accountLoad = this.accountStore.load();
            Iterator<AccountStore.Account> it = arrayListListOfflineAccounts.iterator();
            while (it.hasNext()) {
                linearLayout.addView(buildOfflineAccountRow(it.next(), accountLoad));
            }
        }
        final AlertDialog alertDialogCreate = new AlertDialog.Builder(this).setView(scrollView).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.offline_account_add, (DialogInterface.OnClickListener) null).create();
        alertDialogCreate.setOnShowListener(new DialogInterface.OnShowListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda0
            @Override // android.content.DialogInterface.OnShowListener
            public final void onShow(DialogInterface dialogInterface) {
                LauncherSettingsActivity.this.lambda$showOfflineAccountsDialog$54(alertDialogCreate, dialogInterface);
            }
        });
        this.offlineAccountsDialog = alertDialogCreate;
        alertDialogCreate.setOnDismissListener(new DialogInterface.OnDismissListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda11
            @Override // android.content.DialogInterface.OnDismissListener
            public final void onDismiss(DialogInterface dialogInterface) {
                LauncherSettingsActivity.this.lambda$showOfflineAccountsDialog$55(alertDialogCreate, dialogInterface);
            }
        });
        alertDialogCreate.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showOfflineAccountsDialog$54(final AlertDialog alertDialog, DialogInterface dialogInterface) {
        alertDialog.getButton(-1).setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda12
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                LauncherSettingsActivity.this.lambda$showOfflineAccountsDialog$53(alertDialog, view);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showOfflineAccountsDialog$53(AlertDialog alertDialog, View view) {
        alertDialog.dismiss();
        showEditOfflineAccountDialog(null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showOfflineAccountsDialog$55(AlertDialog alertDialog, DialogInterface dialogInterface) {
        if (this.offlineAccountsDialog == alertDialog) {
            this.offlineAccountsDialog = null;
        }
    }

    private View buildOfflineAccountRow(final AccountStore.Account account, AccountStore.Account account2) {
        String string;
        boolean z = account2 != null && account2.isOfflineAccount() && account.accountId.equals(account2.accountId);
        MaterialCardView materialCardView = new MaterialCardView(this);
        materialCardView.setRadius(dp(18.0f));
        materialCardView.setCardElevation(dp(1.0f));
        materialCardView.setStrokeWidth(dp(1.0f));
        materialCardView.setStrokeColor(z ? -9909867 : 570425344);
        materialCardView.setCardBackgroundColor(z ? -1443601 : -1);
        materialCardView.setUseCompatPadding(true);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(16);
        linearLayout.setPadding(dp(14.0f), dp(12.0f), dp(14.0f), dp(12.0f));
        materialCardView.addView(linearLayout, new FrameLayout.LayoutParams(-1, -2));
        ImageView imageView = new ImageView(this);
        imageView.setAdjustViewBounds(true);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setBackgroundResource(R.drawable.bg_player_head_preview);
        imageView.setPadding(dp(4.0f), dp(4.0f), dp(4.0f), dp(4.0f));
        imageView.setImageResource(R.drawable.ic_player_head_placeholder);
        PlayerHeadLoader.loadInto(this, imageView, account, null);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(dp(58.0f), dp(58.0f));
        layoutParams.rightMargin = dp(14.0f);
        linearLayout.addView(imageView, layoutParams);
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(1);
        linearLayout2.setGravity(16);
        linearLayout.addView(linearLayout2, new LinearLayout.LayoutParams(0, -2, 1.0f));
        TextView textView = new TextView(this);
        textView.setText(account.getBestDisplayName());
        textView.setTextAppearance(android.R.style.TextAppearance.Material.Medium);
        textView.setTypeface(textView.getTypeface(), 1);
        linearLayout2.addView(textView, new LinearLayout.LayoutParams(-1, -2));
        TextView textView2 = new TextView(this);
        if (account.hasOfflineSkin()) {
            string = getString(R.string.offline_account_row_skin, new Object[]{account.offlineSkinModel});
        } else {
            string = getString(R.string.offline_account_row_no_skin);
        }
        textView2.setText(string);
        textView2.setTextAppearance(android.R.style.TextAppearance.Material.Small);
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-1, -2);
        layoutParams2.topMargin = dp(2.0f);
        linearLayout2.addView(textView2, layoutParams2);
        if (z) {
            TextView textView3 = new TextView(this);
            textView3.setText(R.string.offline_account_active_badge);
            textView3.setTextAppearance(android.R.style.TextAppearance.Material.Small);
            textView3.setTextColor(-15300023);
            textView3.setTypeface(textView3.getTypeface(), 1);
            LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(-1, -2);
            layoutParams3.topMargin = dp(4.0f);
            linearLayout2.addView(textView3, layoutParams3);
        }
        LinearLayout linearLayout3 = new LinearLayout(this);
        linearLayout3.setOrientation(1);
        linearLayout3.setGravity(16);
        LinearLayout.LayoutParams layoutParams4 = new LinearLayout.LayoutParams(-2, -2);
        layoutParams4.leftMargin = dp(12.0f);
        linearLayout.addView(linearLayout3, layoutParams4);
        MaterialButton materialButtonBuildCompactDialogButton = buildCompactDialogButton(z ? R.string.offline_account_active_button : R.string.offline_account_use);
        materialButtonBuildCompactDialogButton.setEnabled(!z);
        materialButtonBuildCompactDialogButton.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda45
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                LauncherSettingsActivity.this.lambda$buildOfflineAccountRow$56(account, view);
            }
        });
        addButtonWithTopMargin(linearLayout3, materialButtonBuildCompactDialogButton, 0);
        MaterialButton materialButtonBuildCompactDialogButton2 = buildCompactDialogButton(R.string.offline_account_edit_button);
        materialButtonBuildCompactDialogButton2.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda46
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                LauncherSettingsActivity.this.lambda$buildOfflineAccountRow$57(account, view);
            }
        });
        addButtonWithTopMargin(linearLayout3, materialButtonBuildCompactDialogButton2, dp(6.0f));
        MaterialButton materialButtonBuildCompactDialogButton3 = buildCompactDialogButton(R.string.offline_account_delete_button);
        materialButtonBuildCompactDialogButton3.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda47
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                LauncherSettingsActivity.this.lambda$buildOfflineAccountRow$58(account, view);
            }
        });
        addButtonWithTopMargin(linearLayout3, materialButtonBuildCompactDialogButton3, dp(6.0f));
        LinearLayout.LayoutParams layoutParams5 = new LinearLayout.LayoutParams(-1, -2);
        layoutParams5.topMargin = dp(10.0f);
        materialCardView.setLayoutParams(layoutParams5);
        return materialCardView;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildOfflineAccountRow$56(AccountStore.Account account, View view) {
        AlertDialog alertDialog = this.offlineAccountsDialog;
        if (alertDialog != null) {
            alertDialog.dismiss();
        }
        this.accountStore.activateOfflineAccount(account.accountId);
        AccountStore.Account accountLoad = this.accountStore.load();
        updateAccountStatus(accountLoad);
        updateSkinUi(accountLoad);
        Toast.makeText(this, getString(R.string.offline_account_enabled, new Object[]{account.getBestDisplayName()}), 0).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildOfflineAccountRow$57(AccountStore.Account account, View view) {
        AlertDialog alertDialog = this.offlineAccountsDialog;
        if (alertDialog != null) {
            alertDialog.dismiss();
        }
        showEditOfflineAccountDialog(account);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildOfflineAccountRow$58(AccountStore.Account account, View view) {
        AlertDialog alertDialog = this.offlineAccountsDialog;
        if (alertDialog != null) {
            alertDialog.dismiss();
        }
        confirmDeleteOfflineAccount(account);
    }

    private void showEditOfflineAccountDialog(final AccountStore.Account account) {
        String string;
        if (this.accountStore == null) {
            return;
        }
        this.pendingOfflineSkinUri = null;
        this.pendingOfflineSkinPreview = null;
        this.pendingOfflineSkinLabel = null;
        ScrollView scrollView = new ScrollView(this);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setPadding(dp(24.0f), dp(18.0f), dp(24.0f), dp(4.0f));
        scrollView.addView(linearLayout, new FrameLayout.LayoutParams(-1, -2));
        linearLayout.addView(buildDialogHeader(R.drawable.ic_player_head_placeholder, account == null ? R.string.offline_account_create_title : R.string.offline_account_edit_title, R.string.offline_account_edit_summary));
        MaterialCardView materialCardView = new MaterialCardView(this);
        materialCardView.setRadius(dp(18.0f));
        materialCardView.setCardElevation(dp(1.0f));
        materialCardView.setStrokeWidth(dp(1.0f));
        materialCardView.setStrokeColor(570425344);
        materialCardView.setCardBackgroundColor(-1);
        materialCardView.setUseCompatPadding(true);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = dp(16.0f);
        linearLayout.addView(materialCardView, layoutParams);
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        linearLayout2.setPadding(dp(14.0f), dp(14.0f), dp(14.0f), dp(14.0f));
        materialCardView.addView(linearLayout2, new FrameLayout.LayoutParams(-1, -2));
        final ImageView imageView = new ImageView(this);
        imageView.setAdjustViewBounds(true);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setBackgroundResource(R.drawable.bg_player_head_preview);
        imageView.setPadding(dp(6.0f), dp(6.0f), dp(6.0f), dp(6.0f));
        imageView.setImageResource(R.drawable.ic_player_head_placeholder);
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(dp(88.0f), dp(88.0f));
        layoutParams2.rightMargin = dp(14.0f);
        linearLayout2.addView(imageView, layoutParams2);
        this.pendingOfflineSkinPreview = imageView;
        if (account != null && account.hasOfflineSkin()) {
            PlayerHeadLoader.loadInto(this, imageView, account, null);
        }
        LinearLayout linearLayout3 = new LinearLayout(this);
        linearLayout3.setOrientation(1);
        linearLayout2.addView(linearLayout3, new LinearLayout.LayoutParams(0, -2, 1.0f));
        final EditText editText = new EditText(this);
        editText.setInputType(1);
        editText.setSingleLine(true);
        editText.setSelectAllOnFocus(true);
        editText.setHint(R.string.offline_account_name_hint);
        editText.setText(account != null ? account.getBestDisplayName() : "Player");
        linearLayout3.addView(editText, new LinearLayout.LayoutParams(-1, -2));
        final TextView textView = new TextView(this);
        if (account != null && account.hasOfflineSkin()) {
            string = getString(R.string.offline_account_skin_current, new Object[]{account.offlineSkinModel});
        } else {
            string = getString(R.string.offline_account_skin_none);
        }
        textView.setText(string);
        textView.setTextAppearance(android.R.style.TextAppearance.Material.Small);
        LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(-1, -2);
        layoutParams3.topMargin = dp(6.0f);
        linearLayout3.addView(textView, layoutParams3);
        this.pendingOfflineSkinLabel = textView;
        LinearLayout linearLayout4 = new LinearLayout(this);
        linearLayout4.setOrientation(0);
        LinearLayout.LayoutParams layoutParams4 = new LinearLayout.LayoutParams(-1, -2);
        layoutParams4.topMargin = dp(8.0f);
        linearLayout3.addView(linearLayout4, layoutParams4);
        MaterialButton materialButtonBuildCompactDialogButton = buildCompactDialogButton(R.string.offline_account_choose_skin);
        materialButtonBuildCompactDialogButton.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda4
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                LauncherSettingsActivity.this.lambda$showEditOfflineAccountDialog$59(view);
            }
        });
        linearLayout4.addView(materialButtonBuildCompactDialogButton, new LinearLayout.LayoutParams(0, -2, 1.0f));
        final boolean[] zArr = {false};
        MaterialButton materialButtonBuildCompactDialogButton2 = buildCompactDialogButton(R.string.offline_account_clear_skin);
        materialButtonBuildCompactDialogButton2.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda5
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                LauncherSettingsActivity.this.lambda$showEditOfflineAccountDialog$60(zArr, imageView, textView, view);
            }
        });
        LinearLayout.LayoutParams layoutParams5 = new LinearLayout.LayoutParams(0, -2, 1.0f);
        layoutParams5.leftMargin = dp(8.0f);
        linearLayout4.addView(materialButtonBuildCompactDialogButton2, layoutParams5);
        final AlertDialog alertDialogCreate = new AlertDialog.Builder(this).setView(scrollView).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(account == null ? R.string.offline_account_create : R.string.offline_account_save, (DialogInterface.OnClickListener) null).create();
        alertDialogCreate.setOnShowListener(new DialogInterface.OnShowListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda6
            @Override // android.content.DialogInterface.OnShowListener
            public final void onShow(DialogInterface dialogInterface) {
                LauncherSettingsActivity.this.lambda$showEditOfflineAccountDialog$62(alertDialogCreate, editText, account, zArr, dialogInterface);
            }
        });
        alertDialogCreate.setOnDismissListener(new DialogInterface.OnDismissListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda7
            @Override // android.content.DialogInterface.OnDismissListener
            public final void onDismiss(DialogInterface dialogInterface) {
                LauncherSettingsActivity.this.lambda$showEditOfflineAccountDialog$63(dialogInterface);
            }
        });
        alertDialogCreate.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showEditOfflineAccountDialog$59(View view) {
        openOfflineSkinPicker();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showEditOfflineAccountDialog$60(boolean[] zArr, ImageView imageView, TextView textView, View view) {
        this.pendingOfflineSkinUri = null;
        zArr[0] = true;
        imageView.setImageResource(R.drawable.ic_player_head_placeholder);
        textView.setText(R.string.offline_account_skin_none);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showEditOfflineAccountDialog$62(final AlertDialog alertDialog, final EditText editText, final AccountStore.Account account, final boolean[] zArr, DialogInterface dialogInterface) {
        alertDialog.getButton(-1).setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda48
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                LauncherSettingsActivity.this.lambda$showEditOfflineAccountDialog$61(editText, account, zArr, alertDialog, view);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showEditOfflineAccountDialog$61(EditText editText, AccountStore.Account account, boolean[] zArr, AlertDialog alertDialog, View view) {
        String strSanitizeOfflineName = sanitizeOfflineName(editText.getText() == null ? "" : editText.getText().toString());
        if (!isValidOfflineName(strSanitizeOfflineName)) {
            editText.setError(getString(R.string.offline_account_invalid));
            return;
        }
        try {
            AccountStore.Account accountSaveOrUpdateOfflineAccount = this.accountStore.saveOrUpdateOfflineAccount(account != null ? account.accountId : null, strSanitizeOfflineName, this.pendingOfflineSkinUri, zArr[0]);
            updateAccountStatus(accountSaveOrUpdateOfflineAccount);
            updateSkinUi(accountSaveOrUpdateOfflineAccount);
            Toast.makeText(this, getString(R.string.offline_account_enabled, new Object[]{accountSaveOrUpdateOfflineAccount.getBestDisplayName()}), 0).show();
            alertDialog.dismiss();
        } catch (Throwable th) {
            editText.setError(th.getMessage() != null ? th.getMessage() : th.toString());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showEditOfflineAccountDialog$63(DialogInterface dialogInterface) {
        this.pendingOfflineSkinUri = null;
        this.pendingOfflineSkinPreview = null;
        this.pendingOfflineSkinLabel = null;
    }

    private View buildDialogHeader(int i, int i2, int i3) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(16);
        ImageView imageView = new ImageView(this);
        imageView.setImageResource(i);
        imageView.setBackgroundResource(R.drawable.bg_player_head_preview);
        imageView.setPadding(dp(10.0f), dp(10.0f), dp(10.0f), dp(10.0f));
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(dp(72.0f), dp(72.0f));
        layoutParams.rightMargin = dp(16.0f);
        linearLayout.addView(imageView, layoutParams);
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(1);
        linearLayout.addView(linearLayout2, new LinearLayout.LayoutParams(0, -2, 1.0f));
        TextView textView = new TextView(this);
        textView.setText(i2);
        textView.setTextAppearance(android.R.style.TextAppearance.Material.Large);
        textView.setTypeface(textView.getTypeface(), 1);
        linearLayout2.addView(textView, new LinearLayout.LayoutParams(-1, -2));
        TextView textView2 = new TextView(this);
        textView2.setText(i3);
        textView2.setTextAppearance(android.R.style.TextAppearance.Material.Small);
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-1, -2);
        layoutParams2.topMargin = dp(4.0f);
        linearLayout2.addView(textView2, layoutParams2);
        return linearLayout;
    }

    private View buildEmptyOfflineAccountCard() {
        MaterialCardView materialCardView = new MaterialCardView(this);
        materialCardView.setRadius(dp(18.0f));
        materialCardView.setCardElevation(dp(1.0f));
        materialCardView.setStrokeWidth(dp(1.0f));
        materialCardView.setStrokeColor(570425344);
        materialCardView.setCardBackgroundColor(-1);
        materialCardView.setUseCompatPadding(true);
        TextView textView = new TextView(this);
        textView.setText(R.string.offline_accounts_empty);
        textView.setGravity(17);
        textView.setPadding(dp(18.0f), dp(22.0f), dp(18.0f), dp(22.0f));
        textView.setTextAppearance(android.R.style.TextAppearance.Material.Small);
        materialCardView.addView(textView, new FrameLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = dp(10.0f);
        materialCardView.setLayoutParams(layoutParams);
        return materialCardView;
    }

    private MaterialButton buildCompactDialogButton(int i) {
        MaterialButton materialButton = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        materialButton.setText(i);
        materialButton.setAllCaps(false);
        materialButton.setMinHeight(0);
        materialButton.setMinWidth(0);
        materialButton.setMinimumHeight(0);
        materialButton.setMinimumWidth(0);
        materialButton.setInsetTop(0);
        materialButton.setInsetBottom(0);
        materialButton.setPadding(dp(12.0f), 0, dp(12.0f), 0);
        return materialButton;
    }

    private void addButtonWithTopMargin(LinearLayout linearLayout, View view, int i) {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = i;
        linearLayout.addView(view, layoutParams);
    }

    private void updatePendingOfflineSkinPreview(Uri uri) {
        if (this.pendingOfflineSkinPreview == null) {
            return;
        }
        File file = new File(getCacheDir(), "pending_offline_skin_preview.png");
        try {
            InputStream inputStreamOpenInputStream = getContentResolver().openInputStream(uri);
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                try {
                    if (inputStreamOpenInputStream == null) {
                        throw new IllegalStateException("Unable to open selected skin.");
                    }
                    byte[] bArr = new byte[8192];
                    while (true) {
                        int i = inputStreamOpenInputStream.read(bArr);
                        if (i == -1) {
                            break;
                        } else {
                            fileOutputStream.write(bArr, 0, i);
                        }
                    }
                    Bitmap bitmapLoadHeadFromSkinFile = PlayerHeadLoader.loadHeadFromSkinFile(file);
                    if (bitmapLoadHeadFromSkinFile != null) {
                        this.pendingOfflineSkinPreview.setImageBitmap(bitmapLoadHeadFromSkinFile);
                    } else {
                        this.pendingOfflineSkinPreview.setImageResource(R.drawable.ic_player_head_placeholder);
                    }
                    fileOutputStream.close();
                    if (inputStreamOpenInputStream != null) {
                        inputStreamOpenInputStream.close();
                    }
                } finally {
                }
            } finally {
            }
        } catch (Throwable unused) {
            this.pendingOfflineSkinPreview.setImageResource(R.drawable.ic_player_head_placeholder);
        }
    }

    private void openOfflineSkinPicker() {
        if (this.offlineSkinPickerLauncher == null) {
            return;
        }
        Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT");
        intent.addCategory("android.intent.category.OPENABLE");
        intent.setType("image/png");
        this.offlineSkinPickerLauncher.launch(intent);
    }

    private void confirmDeleteOfflineAccount(final AccountStore.Account account) {
        new AlertDialog.Builder(this).setTitle(getString(R.string.offline_account_delete_title, new Object[]{account.getBestDisplayName()})).setMessage(R.string.offline_account_delete_message).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.offline_account_delete_button, new DialogInterface.OnClickListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda49
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                LauncherSettingsActivity.this.lambda$confirmDeleteOfflineAccount$64(account, dialogInterface, i);
            }
        }).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$confirmDeleteOfflineAccount$64(AccountStore.Account account, DialogInterface dialogInterface, int i) {
        this.accountStore.deleteOfflineAccount(account.accountId);
        AccountStore.Account accountLoad = this.accountStore.load();
        updateAccountStatus(accountLoad);
        updateSkinUi(accountLoad);
        showOfflineAccountsDialog();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateChangeMicrosoftSkinButtonState(AccountStore.Account account) {
        if (this.binding == null) {
            return;
        }
        AccountStore.Account microsoftSkinTargetAccount = getMicrosoftSkinTargetAccount(account);
        boolean z = microsoftSkinTargetAccount != null && microsoftSkinTargetAccount.isMicrosoftAccount() && microsoftSkinTargetAccount.hasMinecraftSession();
        this.binding.buttonChangeMicrosoftSkin.setVisibility(z ? 0 : 8);
        this.binding.buttonChangeMicrosoftSkin.setEnabled(z);
    }

    private AccountStore.Account getMicrosoftSkinTargetAccount(AccountStore.Account account) {
        AccountStore.Account accountLoadLastMicrosoftAccount;
        if (account != null && account.isMicrosoftAccount() && account.hasMinecraftSession()) {
            return account;
        }
        AccountStore accountStore = this.accountStore;
        if (accountStore != null && (accountLoadLastMicrosoftAccount = accountStore.loadLastMicrosoftAccount()) != null && accountLoadLastMicrosoftAccount.isMicrosoftAccount() && accountLoadLastMicrosoftAccount.hasMinecraftSession()) {
            return accountLoadLastMicrosoftAccount;
        }
        return null;
    }

    private void showChangeMicrosoftSkinDialog() {
        AccountStore accountStore = this.accountStore;
        AccountStore.Account microsoftSkinTargetAccount = getMicrosoftSkinTargetAccount(accountStore != null ? accountStore.load() : null);
        if (microsoftSkinTargetAccount == null) {
            Toast.makeText(this, R.string.microsoft_skin_requires_account, 1).show();
        } else {
            new AlertDialog.Builder(this).setTitle(R.string.microsoft_skin_change_title).setMessage(getString(R.string.microsoft_skin_change_message, new Object[]{microsoftSkinTargetAccount.getBestDisplayName()})).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.microsoft_skin_pick, new DialogInterface.OnClickListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda16
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    LauncherSettingsActivity.this.lambda$showChangeMicrosoftSkinDialog$65(dialogInterface, i);
                }
            }).show();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showChangeMicrosoftSkinDialog$65(DialogInterface dialogInterface, int i) {
        openMicrosoftSkinPicker();
    }

    private void openMicrosoftSkinPicker() {
        if (this.microsoftSkinPickerLauncher == null) {
            return;
        }
        Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT");
        intent.addCategory("android.intent.category.OPENABLE");
        intent.setType("image/png");
        this.microsoftSkinPickerLauncher.launch(intent);
    }

    private void prepareMicrosoftSkinUpload(Uri uri) {
        File file = new File(getCacheDir(), "pending_microsoft_account_skin.png");
        try {
            copyUriToFile(uri, file);
            if (!CustomSkinStore.isSkinValid(file)) {
                file.delete();
                Toast.makeText(this, R.string.microsoft_skin_invalid, 1).show();
            } else {
                showConfirmMicrosoftSkinUploadDialog(file, CustomSkinStore.getSkinModel(file));
            }
        } catch (Throwable th) {
            Toast.makeText(this, th.getMessage() != null ? th.getMessage() : th.toString(), 1).show();
        }
    }

    private void showConfirmMicrosoftSkinUploadDialog(final File file, SkinModelType skinModelType) {
        final SkinModelType[] skinModelTypeArr = new SkinModelType[1];
        skinModelTypeArr[0] = skinModelType == SkinModelType.SLIM ? SkinModelType.SLIM : SkinModelType.CLASSIC;
        new AlertDialog.Builder(this).setTitle(R.string.microsoft_skin_upload_title).setMessage(R.string.microsoft_skin_upload_message).setSingleChoiceItems(new String[]{getString(R.string.microsoft_skin_variant_classic), getString(R.string.microsoft_skin_variant_slim)}, skinModelTypeArr[0] != SkinModelType.SLIM ? 0 : 1, new DialogInterface.OnClickListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda65
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                LauncherSettingsActivity.lambda$showConfirmMicrosoftSkinUploadDialog$66(skinModelTypeArr, dialogInterface, i);
            }
        }).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.microsoft_skin_upload, new DialogInterface.OnClickListener() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda67
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                LauncherSettingsActivity.this.lambda$showConfirmMicrosoftSkinUploadDialog$67(file, skinModelTypeArr, dialogInterface, i);
            }
        }).show();
    }

    static /* synthetic */ void lambda$showConfirmMicrosoftSkinUploadDialog$66(SkinModelType[] skinModelTypeArr, DialogInterface dialogInterface, int i) {
        skinModelTypeArr[0] = i == 1 ? SkinModelType.SLIM : SkinModelType.CLASSIC;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showConfirmMicrosoftSkinUploadDialog$67(File file, SkinModelType[] skinModelTypeArr, DialogInterface dialogInterface, int i) {
        uploadMicrosoftAccountSkin(file, skinModelTypeArr[0]);
    }

    private void uploadMicrosoftAccountSkin(final File file, final SkinModelType skinModelType) {
        AccountStore accountStore = this.accountStore;
        final AccountStore.Account microsoftSkinTargetAccount = getMicrosoftSkinTargetAccount(accountStore != null ? accountStore.load() : null);
        if (microsoftSkinTargetAccount == null) {
            Toast.makeText(this, R.string.microsoft_skin_requires_account, 1).show();
            return;
        }
        this.binding.buttonChangeMicrosoftSkin.setEnabled(false);
        this.binding.buttonRefreshMicrosoftSkin.setEnabled(false);
        this.binding.textSkinStatus.setText(R.string.microsoft_skin_uploading);
        new Thread(new Runnable() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda32
            @Override // java.lang.Runnable
            public final void run() {
                LauncherSettingsActivity.this.lambda$uploadMicrosoftAccountSkin$70(microsoftSkinTargetAccount, file, skinModelType);
            }
        }, "Microsoft Skin Upload").start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$uploadMicrosoftAccountSkin$70(AccountStore.Account account, File file, SkinModelType skinModelType) {
        try {
            MicrosoftSkinUploader.uploadSkin(account.minecraftAccessToken, file, skinModelType);
            runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda22
                @Override // java.lang.Runnable
                public final void run() {
                    LauncherSettingsActivity.this.lambda$uploadMicrosoftAccountSkin$68();
                }
            });
        } catch (Throwable th) {
            runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.LauncherSettingsActivity$$ExternalSyntheticLambda33
                @Override // java.lang.Runnable
                public final void run() {
                    LauncherSettingsActivity.this.lambda$uploadMicrosoftAccountSkin$69(th);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$uploadMicrosoftAccountSkin$68() {
        Toast.makeText(this, R.string.microsoft_skin_upload_success, 1).show();
        refreshMicrosoftAccountAndSkin(false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$uploadMicrosoftAccountSkin$69(Throwable th) {
        this.binding.buttonChangeMicrosoftSkin.setEnabled(true);
        this.binding.buttonRefreshMicrosoftSkin.setEnabled(true);
        String message = th.getMessage() != null ? th.getMessage() : th.toString();
        this.binding.textSkinStatus.setText(getString(R.string.microsoft_skin_upload_failed, new Object[]{message}));
        Toast.makeText(this, getString(R.string.microsoft_skin_upload_failed, new Object[]{message}), 1).show();
    }

    private void copyUriToFile(Uri uri, File file) throws Exception {
        InputStream inputStreamOpenInputStream = getContentResolver().openInputStream(uri);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            try {
                if (inputStreamOpenInputStream == null) {
                    throw new IllegalStateException("Could not open selected skin.");
                }
                byte[] bArr = new byte[8192];
                while (true) {
                    int i = inputStreamOpenInputStream.read(bArr);
                    if (i == -1) {
                        break;
                    } else {
                        fileOutputStream.write(bArr, 0, i);
                    }
                }
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

    private void refreshMicrosoftAccountAndSkin(boolean z) {
        if (this.authManager == null) {
            return;
        }
        this.binding.buttonRefreshMicrosoftSkin.setEnabled(false);
        this.binding.textSkinStatus.setText(R.string.microsoft_skin_refreshing);
        if (z) {
            Toast.makeText(this, R.string.microsoft_skin_refreshing, 0).show();
        }
        this.authManager.refreshMicrosoftAccount();
    }

    private static String sanitizeOfflineName(String str) {
        if (str == null) {
            return "Player";
        }
        String strReplaceAll = str.trim().replaceAll("[^A-Za-z0-9_]", "");
        if (strReplaceAll.length() > 16) {
            strReplaceAll = strReplaceAll.substring(0, 16);
        }
        return strReplaceAll.length() == 0 ? "Player" : strReplaceAll;
    }

    private static boolean isValidOfflineName(String str) {
        return str != null && str.matches("[A-Za-z0-9_]{3,16}");
    }

    private int dp(float f) {
        return (int) ((f * getResources().getDisplayMetrics().density) + 0.5f);
    }

    private static boolean isNullOrBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
}
