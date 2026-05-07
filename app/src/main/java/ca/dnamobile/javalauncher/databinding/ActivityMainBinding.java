package ca.dnamobile.javalauncher.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import ca.dnamobile.javalauncher.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.tabs.TabLayout;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class ActivityMainBinding implements ViewBinding {
    public final LinearLayout bottomMainActions;
    public final MaterialButton buttonBrowseContentMain;
    public final MaterialButton buttonImportModpackMain;
    public final MaterialButton buttonLaunchVersion;
    public final MaterialButton buttonOpenFolder;
    public final MaterialButton buttonOpenSettings;
    public final MaterialButton buttonRefreshVersions;
    public final MaterialButton buttonShareLatestLog;
    public final MaterialButton buttonSignIn;
    public final MaterialButton buttonSignOut;
    public final CheckBox checkKeepLogs;
    public final FloatingActionButton fabCreateInstance;
    public final LinearLayout installinstanceslayout;
    public final FrameLayout mainContent;
    public final MaterialCardView mainviewcardview;
    public final ProgressBar progressVersions;
    public final RecyclerView recyclerVersions;
    private final FrameLayout rootView;
    public final SwitchMaterial switchShowSharedInstalls;
    public final SwitchMaterial switchUseNativeSurface;
    public final TabLayout tabVersionTypes;
    public final TextView textAccountStatus;
    public final TextView textAppTitle;
    public final TextView textFolder;
    public final TextView textRenderSurfaceSummary;
    public final TextView textRenderSurfaceTitle;
    public final TextView textSelectedVersion;
    public final TextView textSharedInstallsSummary;
    public final TextView textStatus;
    public final TextView textVersionCount;

    private ActivityMainBinding(FrameLayout frameLayout, LinearLayout linearLayout, MaterialButton materialButton, MaterialButton materialButton2, MaterialButton materialButton3, MaterialButton materialButton4, MaterialButton materialButton5, MaterialButton materialButton6, MaterialButton materialButton7, MaterialButton materialButton8, MaterialButton materialButton9, CheckBox checkBox, FloatingActionButton floatingActionButton, LinearLayout linearLayout2, FrameLayout frameLayout2, MaterialCardView materialCardView, ProgressBar progressBar, RecyclerView recyclerView, SwitchMaterial switchMaterial, SwitchMaterial switchMaterial2, TabLayout tabLayout, TextView textView, TextView textView2, TextView textView3, TextView textView4, TextView textView5, TextView textView6, TextView textView7, TextView textView8, TextView textView9) {
        this.rootView = frameLayout;
        this.bottomMainActions = linearLayout;
        this.buttonBrowseContentMain = materialButton;
        this.buttonImportModpackMain = materialButton2;
        this.buttonLaunchVersion = materialButton3;
        this.buttonOpenFolder = materialButton4;
        this.buttonOpenSettings = materialButton5;
        this.buttonRefreshVersions = materialButton6;
        this.buttonShareLatestLog = materialButton7;
        this.buttonSignIn = materialButton8;
        this.buttonSignOut = materialButton9;
        this.checkKeepLogs = checkBox;
        this.fabCreateInstance = floatingActionButton;
        this.installinstanceslayout = linearLayout2;
        this.mainContent = frameLayout2;
        this.mainviewcardview = materialCardView;
        this.progressVersions = progressBar;
        this.recyclerVersions = recyclerView;
        this.switchShowSharedInstalls = switchMaterial;
        this.switchUseNativeSurface = switchMaterial2;
        this.tabVersionTypes = tabLayout;
        this.textAccountStatus = textView;
        this.textAppTitle = textView2;
        this.textFolder = textView3;
        this.textRenderSurfaceSummary = textView4;
        this.textRenderSurfaceTitle = textView5;
        this.textSelectedVersion = textView6;
        this.textSharedInstallsSummary = textView7;
        this.textStatus = textView8;
        this.textVersionCount = textView9;
    }

    @Override // androidx.viewbinding.ViewBinding
    public FrameLayout getRoot() {
        return this.rootView;
    }

    public static ActivityMainBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static ActivityMainBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View viewInflate = layoutInflater.inflate(R.layout.activity_main, viewGroup, false);
        if (z) {
            viewGroup.addView(viewInflate);
        }
        return bind(viewInflate);
    }

    public static ActivityMainBinding bind(View view) {
        int i = R.id.bottomMainActions;
        LinearLayout linearLayout = (LinearLayout) ViewBindings.findChildViewById(view, i);
        if (linearLayout != null) {
            MaterialButton materialButton = (MaterialButton) ViewBindings.findChildViewById(view, R.id.buttonBrowseContentMain);
            MaterialButton materialButton2 = (MaterialButton) ViewBindings.findChildViewById(view, R.id.buttonImportModpackMain);
            i = R.id.buttonLaunchVersion;
            MaterialButton materialButton3 = (MaterialButton) ViewBindings.findChildViewById(view, i);
            if (materialButton3 != null) {
                i = R.id.buttonOpenFolder;
                MaterialButton materialButton4 = (MaterialButton) ViewBindings.findChildViewById(view, i);
                if (materialButton4 != null) {
                    i = R.id.buttonOpenSettings;
                    MaterialButton materialButton5 = (MaterialButton) ViewBindings.findChildViewById(view, i);
                    if (materialButton5 != null) {
                        i = R.id.buttonRefreshVersions;
                        MaterialButton materialButton6 = (MaterialButton) ViewBindings.findChildViewById(view, i);
                        if (materialButton6 != null) {
                            i = R.id.buttonShareLatestLog;
                            MaterialButton materialButton7 = (MaterialButton) ViewBindings.findChildViewById(view, i);
                            if (materialButton7 != null) {
                                i = R.id.buttonSignIn;
                                MaterialButton materialButton8 = (MaterialButton) ViewBindings.findChildViewById(view, i);
                                if (materialButton8 != null) {
                                    i = R.id.buttonSignOut;
                                    MaterialButton materialButton9 = (MaterialButton) ViewBindings.findChildViewById(view, i);
                                    if (materialButton9 != null) {
                                        i = R.id.checkKeepLogs;
                                        CheckBox checkBox = (CheckBox) ViewBindings.findChildViewById(view, i);
                                        if (checkBox != null) {
                                            i = R.id.fabCreateInstance;
                                            FloatingActionButton floatingActionButton = (FloatingActionButton) ViewBindings.findChildViewById(view, i);
                                            if (floatingActionButton != null) {
                                                LinearLayout linearLayout2 = (LinearLayout) ViewBindings.findChildViewById(view, R.id.installinstanceslayout);
                                                FrameLayout frameLayout = (FrameLayout) view;
                                                MaterialCardView materialCardView = (MaterialCardView) ViewBindings.findChildViewById(view, R.id.mainviewcardview);
                                                i = R.id.progressVersions;
                                                ProgressBar progressBar = (ProgressBar) ViewBindings.findChildViewById(view, i);
                                                if (progressBar != null) {
                                                    i = R.id.recyclerVersions;
                                                    RecyclerView recyclerView = (RecyclerView) ViewBindings.findChildViewById(view, i);
                                                    if (recyclerView != null) {
                                                        i = R.id.switchShowSharedInstalls;
                                                        SwitchMaterial switchMaterial = (SwitchMaterial) ViewBindings.findChildViewById(view, i);
                                                        if (switchMaterial != null) {
                                                            i = R.id.switchUseNativeSurface;
                                                            SwitchMaterial switchMaterial2 = (SwitchMaterial) ViewBindings.findChildViewById(view, i);
                                                            if (switchMaterial2 != null) {
                                                                i = R.id.tabVersionTypes;
                                                                TabLayout tabLayout = (TabLayout) ViewBindings.findChildViewById(view, i);
                                                                if (tabLayout != null) {
                                                                    i = R.id.textAccountStatus;
                                                                    TextView textView = (TextView) ViewBindings.findChildViewById(view, i);
                                                                    if (textView != null) {
                                                                        i = R.id.textAppTitle;
                                                                        TextView textView2 = (TextView) ViewBindings.findChildViewById(view, i);
                                                                        if (textView2 != null) {
                                                                            i = R.id.textFolder;
                                                                            TextView textView3 = (TextView) ViewBindings.findChildViewById(view, i);
                                                                            if (textView3 != null) {
                                                                                i = R.id.textRenderSurfaceSummary;
                                                                                TextView textView4 = (TextView) ViewBindings.findChildViewById(view, i);
                                                                                if (textView4 != null) {
                                                                                    i = R.id.textRenderSurfaceTitle;
                                                                                    TextView textView5 = (TextView) ViewBindings.findChildViewById(view, i);
                                                                                    if (textView5 != null) {
                                                                                        i = R.id.textSelectedVersion;
                                                                                        TextView textView6 = (TextView) ViewBindings.findChildViewById(view, i);
                                                                                        if (textView6 != null) {
                                                                                            i = R.id.textSharedInstallsSummary;
                                                                                            TextView textView7 = (TextView) ViewBindings.findChildViewById(view, i);
                                                                                            if (textView7 != null) {
                                                                                                i = R.id.textStatus;
                                                                                                TextView textView8 = (TextView) ViewBindings.findChildViewById(view, i);
                                                                                                if (textView8 != null) {
                                                                                                    i = R.id.textVersionCount;
                                                                                                    TextView textView9 = (TextView) ViewBindings.findChildViewById(view, i);
                                                                                                    if (textView9 != null) {
                                                                                                        return new ActivityMainBinding(frameLayout, linearLayout, materialButton, materialButton2, materialButton3, materialButton4, materialButton5, materialButton6, materialButton7, materialButton8, materialButton9, checkBox, floatingActionButton, linearLayout2, frameLayout, materialCardView, progressBar, recyclerView, switchMaterial, switchMaterial2, tabLayout, textView, textView2, textView3, textView4, textView5, textView6, textView7, textView8, textView9);
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
