package ca.dnamobile.javalauncher.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import ca.dnamobile.javalauncher.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.tabs.TabLayout;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class ActivityLauncherSettingsBinding implements ViewBinding {
    public final MaterialButton buttonChangeMicrosoftSkin;
    public final MaterialButton buttonClearRendererPluginCache;
    public final MaterialButton buttonEditBuiltInController;
    public final MaterialButton buttonGrantRendererStorageAccess;
    public final MaterialButton buttonImportRendererPlugin;
    public final MaterialButton buttonManageOfflineAccounts;
    public final MaterialButton buttonManageTouchControls;
    public final MaterialButton buttonOpenDroidBridgeLicense;
    public final MaterialButton buttonOpenDroidBridgeTerms;
    public final MaterialButton buttonOpenMinecraftEula;
    public final MaterialButton buttonOpenPrivacyPolicy;
    public final MaterialButton buttonRefreshMicrosoftSkin;
    public final MaterialButton buttonRefreshRenderers;
    public final MaterialButton buttonSettingsBack;
    public final MaterialButton buttonShareLatestLog;
    public final MaterialButton buttonSignIn;
    public final MaterialButton buttonSignOut;
    public final MaterialButton buttonSimpleVoiceChatMicrophonePermission;
    public final MaterialButton buttonUseMicrosoftAccount;
    public final MaterialCardView cardAccountSettings;
    public final MaterialCardView cardControllerSettings;
    public final MaterialCardView cardInstanceSettings;
    public final MaterialCardView cardLauncherSettings;
    public final MaterialCardView cardPrivacyPolicySettings;
    public final MaterialCardView cardRendererSettings;
    public final MaterialCardView cardSettingsTabsHeader;
    public final CheckBox checkKeepLogs;
    public final ImageView imagePlayerHead;
    public final LinearLayout layoutControllerSettings;
    public final LinearLayout layoutVulkanDriverSettings;
    private final LinearLayout rootView;
    public final FrameLayout settingsContent;
    public final ScrollView settingsScrollView;
    public final TabLayout settingsSectionTabs;
    public final LinearLayout settingsTabsHeader;
    public final SeekBar sliderAllocatedRam;
    public final SeekBar sliderGameResolutionScale;
    public final Spinner spinnerRenderer;
    public final Spinner spinnerVulkanDriver;
    public final SwitchMaterial switchAvoidRoundedCorners;
    public final SwitchMaterial switchDoubleTapToDrop;
    public final SwitchMaterial switchForceFullscreenMode;
    public final SwitchMaterial switchForceSdlControllerBridge;
    public final SwitchMaterial switchInstallNotifications;
    public final SwitchMaterial switchMinecraftTouchGestures;
    public final SwitchMaterial switchRemoveInheritedVanilla;
    public final SwitchMaterial switchShowGameLogOverlay;
    public final SwitchMaterial switchShowInGameSettingsButton;
    public final SwitchMaterial switchShowSharedInstalls;
    public final SwitchMaterial switchTouchControlsEnabled;
    public final SwitchMaterial switchUseNativeSurface;
    public final SwitchMaterial switchUseOpenGlFor26Plus;
    public final SwitchMaterial switchUseSystemVulkanDriver;
    public final TextView textAccountStatus;
    public final TextView textAllocatedRam;
    public final TextView textAvailableRamSummary;
    public final TextView textDoubleTapToDropSummary;
    public final TextView textFolder;
    public final TextView textGameResolutionScale;
    public final TextView textGameResolutionScaleSummary;
    public final TextView textInstallNotificationsSummary;
    public final TextView textMinecraftTouchGesturesSummary;
    public final TextView textPrivacyPolicySummary;
    public final TextView textRendererDescription;
    public final TextView textRendererPluginConfig;
    public final TextView textSimpleVoiceChatMicrophoneStatus;
    public final TextView textSkinStatus;
    public final TextView textVulkanDriverDescription;

    private ActivityLauncherSettingsBinding(LinearLayout linearLayout, MaterialButton materialButton, MaterialButton materialButton2, MaterialButton materialButton3, MaterialButton materialButton4, MaterialButton materialButton5, MaterialButton materialButton6, MaterialButton materialButton7, MaterialButton materialButton8, MaterialButton materialButton9, MaterialButton materialButton10, MaterialButton materialButton11, MaterialButton materialButton12, MaterialButton materialButton13, MaterialButton materialButton14, MaterialButton materialButton15, MaterialButton materialButton16, MaterialButton materialButton17, MaterialButton materialButton18, MaterialButton materialButton19, MaterialCardView materialCardView, MaterialCardView materialCardView2, MaterialCardView materialCardView3, MaterialCardView materialCardView4, MaterialCardView materialCardView5, MaterialCardView materialCardView6, MaterialCardView materialCardView7, CheckBox checkBox, ImageView imageView, LinearLayout linearLayout2, LinearLayout linearLayout3, FrameLayout frameLayout, ScrollView scrollView, TabLayout tabLayout, LinearLayout linearLayout4, SeekBar seekBar, SeekBar seekBar2, Spinner spinner, Spinner spinner2, SwitchMaterial switchMaterial, SwitchMaterial switchMaterial2, SwitchMaterial switchMaterial3, SwitchMaterial switchMaterial4, SwitchMaterial switchMaterial5, SwitchMaterial switchMaterial6, SwitchMaterial switchMaterial7, SwitchMaterial switchMaterial8, SwitchMaterial switchMaterial9, SwitchMaterial switchMaterial10, SwitchMaterial switchMaterial11, SwitchMaterial switchMaterial12, SwitchMaterial switchMaterial13, SwitchMaterial switchMaterial14, TextView textView, TextView textView2, TextView textView3, TextView textView4, TextView textView5, TextView textView6, TextView textView7, TextView textView8, TextView textView9, TextView textView10, TextView textView11, TextView textView12, TextView textView13, TextView textView14, TextView textView15) {
        this.rootView = linearLayout;
        this.buttonChangeMicrosoftSkin = materialButton;
        this.buttonClearRendererPluginCache = materialButton2;
        this.buttonEditBuiltInController = materialButton3;
        this.buttonGrantRendererStorageAccess = materialButton4;
        this.buttonImportRendererPlugin = materialButton5;
        this.buttonManageOfflineAccounts = materialButton6;
        this.buttonManageTouchControls = materialButton7;
        this.buttonOpenDroidBridgeLicense = materialButton8;
        this.buttonOpenDroidBridgeTerms = materialButton9;
        this.buttonOpenMinecraftEula = materialButton10;
        this.buttonOpenPrivacyPolicy = materialButton11;
        this.buttonRefreshMicrosoftSkin = materialButton12;
        this.buttonRefreshRenderers = materialButton13;
        this.buttonSettingsBack = materialButton14;
        this.buttonShareLatestLog = materialButton15;
        this.buttonSignIn = materialButton16;
        this.buttonSignOut = materialButton17;
        this.buttonSimpleVoiceChatMicrophonePermission = materialButton18;
        this.buttonUseMicrosoftAccount = materialButton19;
        this.cardAccountSettings = materialCardView;
        this.cardControllerSettings = materialCardView2;
        this.cardInstanceSettings = materialCardView3;
        this.cardLauncherSettings = materialCardView4;
        this.cardPrivacyPolicySettings = materialCardView5;
        this.cardRendererSettings = materialCardView6;
        this.cardSettingsTabsHeader = materialCardView7;
        this.checkKeepLogs = checkBox;
        this.imagePlayerHead = imageView;
        this.layoutControllerSettings = linearLayout2;
        this.layoutVulkanDriverSettings = linearLayout3;
        this.settingsContent = frameLayout;
        this.settingsScrollView = scrollView;
        this.settingsSectionTabs = tabLayout;
        this.settingsTabsHeader = linearLayout4;
        this.sliderAllocatedRam = seekBar;
        this.sliderGameResolutionScale = seekBar2;
        this.spinnerRenderer = spinner;
        this.spinnerVulkanDriver = spinner2;
        this.switchAvoidRoundedCorners = switchMaterial;
        this.switchDoubleTapToDrop = switchMaterial2;
        this.switchForceFullscreenMode = switchMaterial3;
        this.switchForceSdlControllerBridge = switchMaterial4;
        this.switchInstallNotifications = switchMaterial5;
        this.switchMinecraftTouchGestures = switchMaterial6;
        this.switchRemoveInheritedVanilla = switchMaterial7;
        this.switchShowGameLogOverlay = switchMaterial8;
        this.switchShowInGameSettingsButton = switchMaterial9;
        this.switchShowSharedInstalls = switchMaterial10;
        this.switchTouchControlsEnabled = switchMaterial11;
        this.switchUseNativeSurface = switchMaterial12;
        this.switchUseOpenGlFor26Plus = switchMaterial13;
        this.switchUseSystemVulkanDriver = switchMaterial14;
        this.textAccountStatus = textView;
        this.textAllocatedRam = textView2;
        this.textAvailableRamSummary = textView3;
        this.textDoubleTapToDropSummary = textView4;
        this.textFolder = textView5;
        this.textGameResolutionScale = textView6;
        this.textGameResolutionScaleSummary = textView7;
        this.textInstallNotificationsSummary = textView8;
        this.textMinecraftTouchGesturesSummary = textView9;
        this.textPrivacyPolicySummary = textView10;
        this.textRendererDescription = textView11;
        this.textRendererPluginConfig = textView12;
        this.textSimpleVoiceChatMicrophoneStatus = textView13;
        this.textSkinStatus = textView14;
        this.textVulkanDriverDescription = textView15;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static ActivityLauncherSettingsBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static ActivityLauncherSettingsBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View viewInflate = layoutInflater.inflate(R.layout.activity_launcher_settings, viewGroup, false);
        if (z) {
            viewGroup.addView(viewInflate);
        }
        return bind(viewInflate);
    }

    public static ActivityLauncherSettingsBinding bind(View view) {
        int i = R.id.buttonChangeMicrosoftSkin;
        MaterialButton materialButton = (MaterialButton) ViewBindings.findChildViewById(view, i);
        if (materialButton != null) {
            i = R.id.buttonClearRendererPluginCache;
            MaterialButton materialButton2 = (MaterialButton) ViewBindings.findChildViewById(view, i);
            if (materialButton2 != null) {
                i = R.id.buttonEditBuiltInController;
                MaterialButton materialButton3 = (MaterialButton) ViewBindings.findChildViewById(view, i);
                if (materialButton3 != null) {
                    i = R.id.buttonGrantRendererStorageAccess;
                    MaterialButton materialButton4 = (MaterialButton) ViewBindings.findChildViewById(view, i);
                    if (materialButton4 != null) {
                        i = R.id.buttonImportRendererPlugin;
                        MaterialButton materialButton5 = (MaterialButton) ViewBindings.findChildViewById(view, i);
                        if (materialButton5 != null) {
                            i = R.id.buttonManageOfflineAccounts;
                            MaterialButton materialButton6 = (MaterialButton) ViewBindings.findChildViewById(view, i);
                            if (materialButton6 != null) {
                                i = R.id.buttonManageTouchControls;
                                MaterialButton materialButton7 = (MaterialButton) ViewBindings.findChildViewById(view, i);
                                if (materialButton7 != null) {
                                    i = R.id.buttonOpenDroidBridgeLicense;
                                    MaterialButton materialButton8 = (MaterialButton) ViewBindings.findChildViewById(view, i);
                                    if (materialButton8 != null) {
                                        i = R.id.buttonOpenDroidBridgeTerms;
                                        MaterialButton materialButton9 = (MaterialButton) ViewBindings.findChildViewById(view, i);
                                        if (materialButton9 != null) {
                                            i = R.id.buttonOpenMinecraftEula;
                                            MaterialButton materialButton10 = (MaterialButton) ViewBindings.findChildViewById(view, i);
                                            if (materialButton10 != null) {
                                                i = R.id.buttonOpenPrivacyPolicy;
                                                MaterialButton materialButton11 = (MaterialButton) ViewBindings.findChildViewById(view, i);
                                                if (materialButton11 != null) {
                                                    i = R.id.buttonRefreshMicrosoftSkin;
                                                    MaterialButton materialButton12 = (MaterialButton) ViewBindings.findChildViewById(view, i);
                                                    if (materialButton12 != null) {
                                                        i = R.id.buttonRefreshRenderers;
                                                        MaterialButton materialButton13 = (MaterialButton) ViewBindings.findChildViewById(view, i);
                                                        if (materialButton13 != null) {
                                                            i = R.id.buttonSettingsBack;
                                                            MaterialButton materialButton14 = (MaterialButton) ViewBindings.findChildViewById(view, i);
                                                            if (materialButton14 != null) {
                                                                i = R.id.buttonShareLatestLog;
                                                                MaterialButton materialButton15 = (MaterialButton) ViewBindings.findChildViewById(view, i);
                                                                if (materialButton15 != null) {
                                                                    i = R.id.buttonSignIn;
                                                                    MaterialButton materialButton16 = (MaterialButton) ViewBindings.findChildViewById(view, i);
                                                                    if (materialButton16 != null) {
                                                                        i = R.id.buttonSignOut;
                                                                        MaterialButton materialButton17 = (MaterialButton) ViewBindings.findChildViewById(view, i);
                                                                        if (materialButton17 != null) {
                                                                            i = R.id.buttonSimpleVoiceChatMicrophonePermission;
                                                                            MaterialButton materialButton18 = (MaterialButton) ViewBindings.findChildViewById(view, i);
                                                                            if (materialButton18 != null) {
                                                                                i = R.id.buttonUseMicrosoftAccount;
                                                                                MaterialButton materialButton19 = (MaterialButton) ViewBindings.findChildViewById(view, i);
                                                                                if (materialButton19 != null) {
                                                                                    i = R.id.cardAccountSettings;
                                                                                    MaterialCardView materialCardView = (MaterialCardView) ViewBindings.findChildViewById(view, i);
                                                                                    if (materialCardView != null) {
                                                                                        i = R.id.cardControllerSettings;
                                                                                        MaterialCardView materialCardView2 = (MaterialCardView) ViewBindings.findChildViewById(view, i);
                                                                                        if (materialCardView2 != null) {
                                                                                            i = R.id.cardInstanceSettings;
                                                                                            MaterialCardView materialCardView3 = (MaterialCardView) ViewBindings.findChildViewById(view, i);
                                                                                            if (materialCardView3 != null) {
                                                                                                i = R.id.cardLauncherSettings;
                                                                                                MaterialCardView materialCardView4 = (MaterialCardView) ViewBindings.findChildViewById(view, i);
                                                                                                if (materialCardView4 != null) {
                                                                                                    i = R.id.cardPrivacyPolicySettings;
                                                                                                    MaterialCardView materialCardView5 = (MaterialCardView) ViewBindings.findChildViewById(view, i);
                                                                                                    if (materialCardView5 != null) {
                                                                                                        i = R.id.cardRendererSettings;
                                                                                                        MaterialCardView materialCardView6 = (MaterialCardView) ViewBindings.findChildViewById(view, i);
                                                                                                        if (materialCardView6 != null) {
                                                                                                            i = R.id.cardSettingsTabsHeader;
                                                                                                            MaterialCardView materialCardView7 = (MaterialCardView) ViewBindings.findChildViewById(view, i);
                                                                                                            if (materialCardView7 != null) {
                                                                                                                i = R.id.checkKeepLogs;
                                                                                                                CheckBox checkBox = (CheckBox) ViewBindings.findChildViewById(view, i);
                                                                                                                if (checkBox != null) {
                                                                                                                    i = R.id.imagePlayerHead;
                                                                                                                    ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, i);
                                                                                                                    if (imageView != null) {
                                                                                                                        i = R.id.layoutControllerSettings;
                                                                                                                        LinearLayout linearLayout = (LinearLayout) ViewBindings.findChildViewById(view, i);
                                                                                                                        if (linearLayout != null) {
                                                                                                                            i = R.id.layoutVulkanDriverSettings;
                                                                                                                            LinearLayout linearLayout2 = (LinearLayout) ViewBindings.findChildViewById(view, i);
                                                                                                                            if (linearLayout2 != null) {
                                                                                                                                i = R.id.settingsContent;
                                                                                                                                FrameLayout frameLayout = (FrameLayout) ViewBindings.findChildViewById(view, i);
                                                                                                                                if (frameLayout != null) {
                                                                                                                                    i = R.id.settingsScrollView;
                                                                                                                                    ScrollView scrollView = (ScrollView) ViewBindings.findChildViewById(view, i);
                                                                                                                                    if (scrollView != null) {
                                                                                                                                        i = R.id.settingsSectionTabs;
                                                                                                                                        TabLayout tabLayout = (TabLayout) ViewBindings.findChildViewById(view, i);
                                                                                                                                        if (tabLayout != null) {
                                                                                                                                            i = R.id.settingsTabsHeader;
                                                                                                                                            LinearLayout linearLayout3 = (LinearLayout) ViewBindings.findChildViewById(view, i);
                                                                                                                                            if (linearLayout3 != null) {
                                                                                                                                                i = R.id.sliderAllocatedRam;
                                                                                                                                                SeekBar seekBar = (SeekBar) ViewBindings.findChildViewById(view, i);
                                                                                                                                                if (seekBar != null) {
                                                                                                                                                    i = R.id.sliderGameResolutionScale;
                                                                                                                                                    SeekBar seekBar2 = (SeekBar) ViewBindings.findChildViewById(view, i);
                                                                                                                                                    if (seekBar2 != null) {
                                                                                                                                                        i = R.id.spinnerRenderer;
                                                                                                                                                        Spinner spinner = (Spinner) ViewBindings.findChildViewById(view, i);
                                                                                                                                                        if (spinner != null) {
                                                                                                                                                            i = R.id.spinnerVulkanDriver;
                                                                                                                                                            Spinner spinner2 = (Spinner) ViewBindings.findChildViewById(view, i);
                                                                                                                                                            if (spinner2 != null) {
                                                                                                                                                                i = R.id.switchAvoidRoundedCorners;
                                                                                                                                                                SwitchMaterial switchMaterial = (SwitchMaterial) ViewBindings.findChildViewById(view, i);
                                                                                                                                                                if (switchMaterial != null) {
                                                                                                                                                                    i = R.id.switchDoubleTapToDrop;
                                                                                                                                                                    SwitchMaterial switchMaterial2 = (SwitchMaterial) ViewBindings.findChildViewById(view, i);
                                                                                                                                                                    if (switchMaterial2 != null) {
                                                                                                                                                                        i = R.id.switchForceFullscreenMode;
                                                                                                                                                                        SwitchMaterial switchMaterial3 = (SwitchMaterial) ViewBindings.findChildViewById(view, i);
                                                                                                                                                                        if (switchMaterial3 != null) {
                                                                                                                                                                            i = R.id.switchForceSdlControllerBridge;
                                                                                                                                                                            SwitchMaterial switchMaterial4 = (SwitchMaterial) ViewBindings.findChildViewById(view, i);
                                                                                                                                                                            if (switchMaterial4 != null) {
                                                                                                                                                                                i = R.id.switchInstallNotifications;
                                                                                                                                                                                SwitchMaterial switchMaterial5 = (SwitchMaterial) ViewBindings.findChildViewById(view, i);
                                                                                                                                                                                if (switchMaterial5 != null) {
                                                                                                                                                                                    i = R.id.switchMinecraftTouchGestures;
                                                                                                                                                                                    SwitchMaterial switchMaterial6 = (SwitchMaterial) ViewBindings.findChildViewById(view, i);
                                                                                                                                                                                    if (switchMaterial6 != null) {
                                                                                                                                                                                        i = R.id.switchRemoveInheritedVanilla;
                                                                                                                                                                                        SwitchMaterial switchMaterial7 = (SwitchMaterial) ViewBindings.findChildViewById(view, i);
                                                                                                                                                                                        if (switchMaterial7 != null) {
                                                                                                                                                                                            i = R.id.switchShowGameLogOverlay;
                                                                                                                                                                                            SwitchMaterial switchMaterial8 = (SwitchMaterial) ViewBindings.findChildViewById(view, i);
                                                                                                                                                                                            if (switchMaterial8 != null) {
                                                                                                                                                                                                i = R.id.switchShowInGameSettingsButton;
                                                                                                                                                                                                SwitchMaterial switchMaterial9 = (SwitchMaterial) ViewBindings.findChildViewById(view, i);
                                                                                                                                                                                                if (switchMaterial9 != null) {
                                                                                                                                                                                                    i = R.id.switchShowSharedInstalls;
                                                                                                                                                                                                    SwitchMaterial switchMaterial10 = (SwitchMaterial) ViewBindings.findChildViewById(view, i);
                                                                                                                                                                                                    if (switchMaterial10 != null) {
                                                                                                                                                                                                        i = R.id.switchTouchControlsEnabled;
                                                                                                                                                                                                        SwitchMaterial switchMaterial11 = (SwitchMaterial) ViewBindings.findChildViewById(view, i);
                                                                                                                                                                                                        if (switchMaterial11 != null) {
                                                                                                                                                                                                            i = R.id.switchUseNativeSurface;
                                                                                                                                                                                                            SwitchMaterial switchMaterial12 = (SwitchMaterial) ViewBindings.findChildViewById(view, i);
                                                                                                                                                                                                            if (switchMaterial12 != null) {
                                                                                                                                                                                                                i = R.id.switchUseOpenGlFor26Plus;
                                                                                                                                                                                                                SwitchMaterial switchMaterial13 = (SwitchMaterial) ViewBindings.findChildViewById(view, i);
                                                                                                                                                                                                                if (switchMaterial13 != null) {
                                                                                                                                                                                                                    i = R.id.switchUseSystemVulkanDriver;
                                                                                                                                                                                                                    SwitchMaterial switchMaterial14 = (SwitchMaterial) ViewBindings.findChildViewById(view, i);
                                                                                                                                                                                                                    if (switchMaterial14 != null) {
                                                                                                                                                                                                                        i = R.id.textAccountStatus;
                                                                                                                                                                                                                        TextView textView = (TextView) ViewBindings.findChildViewById(view, i);
                                                                                                                                                                                                                        if (textView != null) {
                                                                                                                                                                                                                            i = R.id.textAllocatedRam;
                                                                                                                                                                                                                            TextView textView2 = (TextView) ViewBindings.findChildViewById(view, i);
                                                                                                                                                                                                                            if (textView2 != null) {
                                                                                                                                                                                                                                i = R.id.textAvailableRamSummary;
                                                                                                                                                                                                                                TextView textView3 = (TextView) ViewBindings.findChildViewById(view, i);
                                                                                                                                                                                                                                if (textView3 != null) {
                                                                                                                                                                                                                                    i = R.id.textDoubleTapToDropSummary;
                                                                                                                                                                                                                                    TextView textView4 = (TextView) ViewBindings.findChildViewById(view, i);
                                                                                                                                                                                                                                    if (textView4 != null) {
                                                                                                                                                                                                                                        i = R.id.textFolder;
                                                                                                                                                                                                                                        TextView textView5 = (TextView) ViewBindings.findChildViewById(view, i);
                                                                                                                                                                                                                                        if (textView5 != null) {
                                                                                                                                                                                                                                            i = R.id.textGameResolutionScale;
                                                                                                                                                                                                                                            TextView textView6 = (TextView) ViewBindings.findChildViewById(view, i);
                                                                                                                                                                                                                                            if (textView6 != null) {
                                                                                                                                                                                                                                                i = R.id.textGameResolutionScaleSummary;
                                                                                                                                                                                                                                                TextView textView7 = (TextView) ViewBindings.findChildViewById(view, i);
                                                                                                                                                                                                                                                if (textView7 != null) {
                                                                                                                                                                                                                                                    i = R.id.textInstallNotificationsSummary;
                                                                                                                                                                                                                                                    TextView textView8 = (TextView) ViewBindings.findChildViewById(view, i);
                                                                                                                                                                                                                                                    if (textView8 != null) {
                                                                                                                                                                                                                                                        i = R.id.textMinecraftTouchGesturesSummary;
                                                                                                                                                                                                                                                        TextView textView9 = (TextView) ViewBindings.findChildViewById(view, i);
                                                                                                                                                                                                                                                        if (textView9 != null) {
                                                                                                                                                                                                                                                            i = R.id.textPrivacyPolicySummary;
                                                                                                                                                                                                                                                            TextView textView10 = (TextView) ViewBindings.findChildViewById(view, i);
                                                                                                                                                                                                                                                            if (textView10 != null) {
                                                                                                                                                                                                                                                                i = R.id.textRendererDescription;
                                                                                                                                                                                                                                                                TextView textView11 = (TextView) ViewBindings.findChildViewById(view, i);
                                                                                                                                                                                                                                                                if (textView11 != null) {
                                                                                                                                                                                                                                                                    i = R.id.textRendererPluginConfig;
                                                                                                                                                                                                                                                                    TextView textView12 = (TextView) ViewBindings.findChildViewById(view, i);
                                                                                                                                                                                                                                                                    if (textView12 != null) {
                                                                                                                                                                                                                                                                        i = R.id.textSimpleVoiceChatMicrophoneStatus;
                                                                                                                                                                                                                                                                        TextView textView13 = (TextView) ViewBindings.findChildViewById(view, i);
                                                                                                                                                                                                                                                                        if (textView13 != null) {
                                                                                                                                                                                                                                                                            i = R.id.textSkinStatus;
                                                                                                                                                                                                                                                                            TextView textView14 = (TextView) ViewBindings.findChildViewById(view, i);
                                                                                                                                                                                                                                                                            if (textView14 != null) {
                                                                                                                                                                                                                                                                                i = R.id.textVulkanDriverDescription;
                                                                                                                                                                                                                                                                                TextView textView15 = (TextView) ViewBindings.findChildViewById(view, i);
                                                                                                                                                                                                                                                                                if (textView15 != null) {
                                                                                                                                                                                                                                                                                    return new ActivityLauncherSettingsBinding((LinearLayout) view, materialButton, materialButton2, materialButton3, materialButton4, materialButton5, materialButton6, materialButton7, materialButton8, materialButton9, materialButton10, materialButton11, materialButton12, materialButton13, materialButton14, materialButton15, materialButton16, materialButton17, materialButton18, materialButton19, materialCardView, materialCardView2, materialCardView3, materialCardView4, materialCardView5, materialCardView6, materialCardView7, checkBox, imageView, linearLayout, linearLayout2, frameLayout, scrollView, tabLayout, linearLayout3, seekBar, seekBar2, spinner, spinner2, switchMaterial, switchMaterial2, switchMaterial3, switchMaterial4, switchMaterial5, switchMaterial6, switchMaterial7, switchMaterial8, switchMaterial9, switchMaterial10, switchMaterial11, switchMaterial12, switchMaterial13, switchMaterial14, textView, textView2, textView3, textView4, textView5, textView6, textView7, textView8, textView9, textView10, textView11, textView12, textView13, textView14, textView15);
                                                                                                                                                                                                                                                                                }
                                                                                                                                                                                                                                                                            }
                                                                                                                                                                                                                                                                        }
                                                                                                                                                                                                                                                                    }
                                                                                                                                                                                                                                                                }
                                                                                                                                                                                                                                                            }
                                                                                                                                                                                                                                                        }
                                                                                                                                                                                                                                                    }
                                                                                                                                                                                                                                                }
                                                                                                                                                                                                                                            }
                                                                                                                                                                                                                                        }
                                                                                                                                                                                                                                    }
                                                                                                                                                                                                                                }
                                                                                                                                                                                                                            }
                                                                                                                                                                                                                        }
                                                                                                                                                                                                                    }
                                                                                                                                                                                                                }
                                                                                                                                                                                                            }
                                                                                                                                                                                                        }
                                                                                                                                                                                                    }
                                                                                                                                                                                                }
                                                                                                                                                                                            }
                                                                                                                                                                                        }
                                                                                                                                                                                    }
                                                                                                                                                                                }
                                                                                                                                                                            }
                                                                                                                                                                        }
                                                                                                                                                                    }
                                                                                                                                                                }
                                                                                                                                                            }
                                                                                                                                                        }
                                                                                                                                                    }
                                                                                                                                                }
                                                                                                                                            }
                                                                                                                                        }
                                                                                                                                    }
                                                                                                                                }
                                                                                                                            }
                                                                                                                        }
                                                                                                                    }
                                                                                                                }
                                                                                                            }
                                                                                                        }
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
