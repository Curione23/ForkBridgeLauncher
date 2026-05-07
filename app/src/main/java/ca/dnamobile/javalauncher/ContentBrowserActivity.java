package ca.dnamobile.javalauncher;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import ca.dnamobile.javalauncher.ContentBrowserActivity;
import ca.dnamobile.javalauncher.instance.LauncherInstance;
import ca.dnamobile.javalauncher.modmanager.CurseForgeApiClient;
import ca.dnamobile.javalauncher.modmanager.CurseForgeApiKeyProvider;
import ca.dnamobile.javalauncher.modmanager.CurseForgeInstallManager;
import ca.dnamobile.javalauncher.modmanager.ModManagerContentType;
import ca.dnamobile.javalauncher.modmanager.ModManagerManifest;
import ca.dnamobile.javalauncher.modmanager.ModManagerSource;
import ca.dnamobile.javalauncher.modmanager.ModManagerVersionResolver;
import ca.dnamobile.javalauncher.modmanager.ModpackInstallManager;
import ca.dnamobile.javalauncher.modmanager.ModpackSearchApiClient;
import ca.dnamobile.javalauncher.modmanager.ModrinthApiClient;
import ca.dnamobile.javalauncher.modmanager.ModrinthInstallManager;
import ca.dnamobile.javalauncher.modmanager.ModrinthProject;
import ca.dnamobile.javalauncher.modmanager.NetworkImageLoader;
import ca.dnamobile.javalauncher.utils.FullscreenUtils;
import ca.dnamobile.javalauncher.utils.path.PathManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class ContentBrowserActivity extends AppCompatActivity {
    public static final String EXTRA_PROJECT_ICON_URL = "ca.dnamobile.javalauncher.extra.PROJECT_ICON_URL";
    public static final String EXTRA_PROJECT_ID = "ca.dnamobile.javalauncher.extra.PROJECT_ID";
    public static final String EXTRA_PROJECT_SLUG = "ca.dnamobile.javalauncher.extra.PROJECT_SLUG";
    public static final String EXTRA_PROJECT_SOURCE = "ca.dnamobile.javalauncher.extra.PROJECT_SOURCE";
    public static final String EXTRA_PROJECT_TITLE = "ca.dnamobile.javalauncher.extra.PROJECT_TITLE";
    public static final String EXTRA_PROJECT_TYPE = "ca.dnamobile.javalauncher.extra.PROJECT_TYPE";
    private static final int PAGE_SIZE = 20;
    private static final long SEARCH_DEBOUNCE_DELAY_MS = 350;
    private MaterialButton buttonPageNext;
    private MaterialButton buttonPagePrevious;
    private AlertDialog currentModpackVersionDialog;
    private TextInputEditText editSearch;
    private ImageView imageInstanceIcon;
    private AlertDialog modpackInstallDialog;
    private TextView modpackInstallMessage;
    private ProgressBar modpackInstallProgress;
    private AlertDialog modpackVersionLoadingDialog;
    private Runnable pendingSearchRunnable;
    private RecyclerView recyclerContentProjects;
    private NestedScrollView scrollRoot;
    private MaterialButtonToggleGroup sourceToggleGroup;
    private TabLayout tabContentTypes;
    private TextView textContentTitle;
    private TextView textInstanceMeta;
    private TextView textInstanceName;
    private TextView textLoaderChip;
    private TextView textPageIndicator;
    private TextView textResultSummary;
    private TextView textVersionChip;
    private final ContentProjectAdapter adapter = new ContentProjectAdapter();
    private final AtomicInteger requestGeneration = new AtomicInteger(0);
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private int currentPage = 0;
    private int totalHits = 0;
    private ContentSource selectedSource = ContentSource.MODRINTH;
    private ModManagerContentType selectedType = ModManagerContentType.MODS;
    private final ArrayList<ModManagerContentType> visibleTabTypes = new ArrayList<>();
    private String instanceId = "";
    private String instanceName = "";
    private String loader = "";
    private String baseVersionId = "";
    private String gameVersionId = "";
    private String iconPath = "";
    private String gameDirectoryPath = "";
    private final Map<String, String> resolvedProjectIconUrls = new ConcurrentHashMap();
    private final Set<
            String> resolvingProjectIconUrls = Collections.newSetFromMap(new ConcurrentHashMap());

    private enum ContentSource {
        MODRINTH,
        CURSEFORGE
    }

    /* JADX INFO: Access modifiers changed from: private */
    interface ModpackMinecraftVersionClickListener {
        void onMinecraftVersionClicked(ModpackMinecraftVersionGroup modpackMinecraftVersionGroup);
    }

    /* JADX INFO: Access modifiers changed from: private */
    interface ModpackVersionClickListener {
        void onVersionClicked(ModpackInstallManager.ModpackVersionChoice modpackVersionChoice);
    }

    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity,
              // androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        PathManager.initContextConstants(this);
        setContentView(R.layout.activity_content_browser);
        FullscreenUtils.enableImmersive(this);
        readExtras();
        bindViews();
        setupHeader();
        setupSourceToggle();
        setupTabs();
        setupSearch();
        setupRecycler();
        setupPagination();
        prepareTopFocus();
        loadContent(true);
        forceScrollTop();
    }

    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    protected void onResume() {
        super.onResume();
        FullscreenUtils.enableImmersive(this);
        pruneInstalledManifestForCurrentTab();
        this.adapter.notifyDataSetChanged();
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public void onWindowFocusChanged(boolean z) {
        super.onWindowFocusChanged(z);
        if (z) {
            FullscreenUtils.enableImmersive(this);
        }
    }

    @Override // androidx.appcompat.app.AppCompatActivity, androidx.fragment.app.FragmentActivity,
              // android.app.Activity
    protected void onDestroy() {
        clearPendingSearch();
        dismissModpackVersionLoadingDialog();
        AlertDialog alertDialog = this.currentModpackVersionDialog;
        if (alertDialog != null) {
            alertDialog.dismiss();
            this.currentModpackVersionDialog = null;
        }
        dismissModpackInstallDialog();
        super.onDestroy();
    }

    private void readExtras() {
        this.instanceId = safeExtra(InstanceDetailsActivity.EXTRA_INSTANCE_ID, "");
        this.instanceName = safeExtra(InstanceDetailsActivity.EXTRA_INSTANCE_NAME, "Unknown instance");
        this.loader = safeExtra(InstanceDetailsActivity.EXTRA_INSTANCE_LOADER, "Vanilla");
        this.baseVersionId = safeExtra(InstanceDetailsActivity.EXTRA_BASE_VERSION_ID, "");
        String strSafeExtra = safeExtra(InstanceDetailsActivity.EXTRA_MINECRAFT_VERSION_ID, "");
        this.gameVersionId = strSafeExtra;
        if (strSafeExtra.isEmpty()) {
            this.gameVersionId = ModManagerVersionResolver.resolveGameVersionForContent(this.baseVersionId);
        }
        this.iconPath = safeExtra(InstanceDetailsActivity.EXTRA_ICON_FILE, "");
        this.gameDirectoryPath = safeExtra(InstanceDetailsActivity.EXTRA_GAME_DIRECTORY, "");
        this.selectedType = ModManagerContentType.fromValue(safeExtra(InstanceDetailsActivity.EXTRA_CONTENT_CATEGORY, "mods"));
        if (this.gameDirectoryPath.isEmpty() && this.instanceId.isEmpty()) {
            this.selectedType = ModManagerContentType.MODPACKS;
        }
    }

    private String safeExtra(String str, String str2) {
        String stringExtra = getIntent().getStringExtra(str);
        return (stringExtra == null || stringExtra.trim().isEmpty()) ? str2 : stringExtra.trim();
    }

    private void bindViews() {
        this.scrollRoot = (NestedScrollView) findViewById(R.id.scrollContentBrowserRoot);
        this.imageInstanceIcon = (ImageView) findViewById(R.id.imageContentBrowserInstanceIcon);
        this.textInstanceName = (TextView) findViewById(R.id.textContentBrowserInstanceName);
        this.textInstanceMeta = (TextView) findViewById(R.id.textContentBrowserInstanceMeta);
        this.textContentTitle = (TextView) findViewById(R.id.textContentBrowserTitle);
        this.textVersionChip = (TextView) findViewById(R.id.textContentBrowserVersionChip);
        this.textLoaderChip = (TextView) findViewById(R.id.textContentBrowserLoaderChip);
        this.textResultSummary = (TextView) findViewById(R.id.textContentBrowserResultSummary);
        this.editSearch = (TextInputEditText) findViewById(R.id.editContentSearch);
        this.sourceToggleGroup = (MaterialButtonToggleGroup) findViewById(R.id.toggleContentSource);
        this.tabContentTypes = (TabLayout) findViewById(R.id.tabContentTypes);
        this.recyclerContentProjects = (RecyclerView) findViewById(R.id.recyclerContentProjects);
        this.buttonPagePrevious = (MaterialButton) findViewById(R.id.buttonPagePrevious);
        this.buttonPageNext = (MaterialButton) findViewById(R.id.buttonPageNext);
        this.textPageIndicator = (TextView) findViewById(R.id.textPageIndicator);
        ((MaterialButton) findViewById(R.id.buttonBackToInstance)).setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.ContentBrowserActivity$$ExternalSyntheticLambda9
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view) {
                        ContentBrowserActivity.this.lambda$bindViews$0(view);
                    }
                });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$bindViews$0(View view) {
        finish();
    }

    private void setupHeader() {
        if (isGlobalBrowserMode()) {
            this.textInstanceName.setText("Browse Modpacks");
            this.textInstanceMeta.setText("Install modpacks as new launcher instances");
        } else {
            this.textInstanceName.setText(this.instanceName);
            this.textInstanceMeta.setText(getString(R.string.content_browser_instance_meta, new Object
                    []{displayLoader(this.loader), this.gameVersionId.isEmpty() ? getString(R.string.content_browser_unknown_version) : this.gameVersionId}));
        }
        if (!this.iconPath.isEmpty()) {
            File file = new File(this.iconPath);
            if (file.isFile()) {
                this.imageInstanceIcon.setImageURI(Uri.fromFile(file));
            } else {
                this.imageInstanceIcon.setImageResource(R.mipmap.ic_launcher);
            }
        } else {
            this.imageInstanceIcon.setImageResource(R.mipmap.ic_launcher);
        }
        this.textContentTitle.setText(this.selectedType != ModManagerContentType.MODPACKS ? getString(R.string.content_browser_install_title) : "Browse Modpacks");
        updateFilterChips();
    }

    private void setupSourceToggle() {
        this.sourceToggleGroup.check(R.id.buttonSourceModrinth);
        this.sourceToggleGroup.addOnButtonCheckedListener(new MaterialButtonToggleGroup.OnButtonCheckedListener() { // from class: ca.dnamobile.javalauncher.ContentBrowserActivity$$ExternalSyntheticLambda10
            @Override // com.google.android.material.button.MaterialButtonToggleGroup.OnButtonCheckedListener
            public final void onButtonChecked(MaterialButtonToggleGroup materialButtonToggleGroup, int i, boolean z) {
                ContentBrowserActivity.this.lambda$setupSourceToggle$1(materialButtonToggleGroup, i, z);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupSourceToggle$1(MaterialButtonToggleGroup materialButtonToggleGroup, int i, boolean z) {
        ContentSource contentSource;
        if (z) {
            if (i == R.id.buttonSourceCurseForge) {
                contentSource = ContentSource.CURSEFORGE;
            } else {
                contentSource = ContentSource.MODRINTH;
            }
            this.selectedSource = contentSource;
            clearPendingSearch();
            loadContent(true);
        }
    }

    private void setupTabs() {
        this.tabContentTypes.removeAllTabs();
        this.visibleTabTypes.clear();
        if (isBrowseModpacksOnlyMode()) {
            this.selectedType = ModManagerContentType.MODPACKS;
            this.visibleTabTypes.add(ModManagerContentType.MODPACKS);
        } else {
            Collections.addAll(this.visibleTabTypes, ModManagerContentType.values());
        }
        int i = 0;
        for (int i2 = 0; i2 < this.visibleTabTypes.size(); i2++) {
            ModManagerContentType modManagerContentType = this.visibleTabTypes.get(i2);
            TabLayout tabLayout = this.tabContentTypes;
            tabLayout.addTab(tabLayout.newTab().setText(getTabTitle(modManagerContentType)));
            if (modManagerContentType == this.selectedType) {
                i = i2;
            }
        }
        TabLayout.Tab tabAt = this.tabContentTypes.getTabAt(i);
        if (tabAt != null) {
            tabAt.select();
        }
        this.tabContentTypes.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() { // from class: ca.dnamobile.javalauncher.ContentBrowserActivity.1
            @Override // com.google.android.material.tabs.TabLayout.BaseOnTabSelectedListener
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override // com.google.android.material.tabs.TabLayout.BaseOnTabSelectedListener
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                if (position < 0 || position >= ContentBrowserActivity.this.visibleTabTypes.size()) {
                    return;
                }
                ContentBrowserActivity contentBrowserActivity = ContentBrowserActivity.this;
                contentBrowserActivity.selectedType = (ModManagerContentType) contentBrowserActivity.visibleTabTypes.get(position);
                ContentBrowserActivity.this.clearPendingSearch();
                ContentBrowserActivity.this.loadContent(true);
            }

            @Override // com.google.android.material.tabs.TabLayout.BaseOnTabSelectedListener
            public void onTabReselected(TabLayout.Tab tab) {
                ContentBrowserActivity.this.clearPendingSearch();
                ContentBrowserActivity.this.loadContent(true);
            }
        });
    }

    private void setupSearch() {
        this.editSearch.setHint(getSearchHint(this.selectedType));
        this.editSearch.setImeOptions(3);
        this.editSearch.setSingleLine(true);
        this.editSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() { // from
                                                                                          // class:
                                                                                          // ca.dnamobile.javalauncher.ContentBrowserActivity$$ExternalSyntheticLambda16
            @Override // android.widget.TextView.OnEditorActionListener
            public final boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                return ContentBrowserActivity.this.lambda$setupSearch$2(textView, i, keyEvent);
            }
        });
        this.editSearch.addTextChangedListener(new TextWatcher() { // from class:
                                                                   // ca.dnamobile.javalauncher.ContentBrowserActivity.2
            @Override // android.text.TextWatcher
            public void afterTextChanged(Editable editable) {}

            @Override // android.text.TextWatcher
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {}

            @Override // android.text.TextWatcher
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                ContentBrowserActivity.this.scheduleSearchFromTyping();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean lambda$setupSearch$2(TextView textView, int i, KeyEvent keyEvent) {
        boolean z = i == 3;
        boolean z2 = keyEvent != null && keyEvent.getKeyCode() == 66 && keyEvent.getAction() == 1;
        if (!z && !z2) {
            return false;
        }
        runSearchNow();
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void scheduleSearchFromTyping() {
        clearPendingSearch();
        Runnable runnable = new Runnable() { // from class:
                                             // ca.dnamobile.javalauncher.ContentBrowserActivity$$ExternalSyntheticLambda23
            @Override // java.lang.Runnable
            public final void run() {
                ContentBrowserActivity.this.lambda$scheduleSearchFromTyping$3();
            }
        };
        this.pendingSearchRunnable = runnable;
        this.searchHandler.postDelayed(runnable, SEARCH_DEBOUNCE_DELAY_MS);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$scheduleSearchFromTyping$3() {
        this.pendingSearchRunnable = null;
        loadContent(true, false);
    }

    private void runSearchNow() {
        clearPendingSearch();
        hideSearchKeyboardAndClearFocus();
        loadContent(true, true);
    }

    private void hideSearchKeyboardAndClearFocus() {
        if (this.editSearch == null) {
            return;
        }
        try {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService("input_method");
            if (inputMethodManager != null) {
                inputMethodManager.hideSoftInputFromWindow(this.editSearch.getWindowToken(), 0);
            }
        } catch (Throwable unused) {
        }
        this.editSearch.clearFocus();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void clearPendingSearch() {
        Runnable runnable = this.pendingSearchRunnable;
        if (runnable != null) {
            this.searchHandler.removeCallbacks(runnable);
            this.pendingSearchRunnable = null;
        }
    }

    private void setupRecycler() {
        this.recyclerContentProjects.setLayoutManager(new LinearLayoutManager(this));
        this.recyclerContentProjects.setNestedScrollingEnabled(false);
        this.recyclerContentProjects.setFocusable(false);
        this.recyclerContentProjects.setHasFixedSize(false);
        this.recyclerContentProjects.setAdapter(this.adapter);
    }

    private void setupPagination() {
        this.buttonPagePrevious.setFocusable(false);
        this.buttonPageNext.setFocusable(false);
        this.buttonPagePrevious.setOnClickListener(new View.OnClickListener() { // from class:
                                                                                // ca.dnamobile.javalauncher.ContentBrowserActivity$$ExternalSyntheticLambda24
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ContentBrowserActivity.this.lambda$setupPagination$4(view);
            }
        });
        this.buttonPageNext.setOnClickListener(new View.OnClickListener() { // from class:
                                                                            // ca.dnamobile.javalauncher.ContentBrowserActivity$$ExternalSyntheticLambda25
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ContentBrowserActivity.this.lambda$setupPagination$5(view);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupPagination$4(View view) {
        if (this.currentPage <= 0) {
            return;
        }
        clearPendingSearch();
        this.currentPage--;
        loadContent(false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupPagination$5(View view) {
        if (this.currentPage + 1 >= getTotalPages(this.totalHits)) {
            return;
        }
        clearPendingSearch();
        this.currentPage++;
        loadContent(false);
    }

    private void prepareTopFocus() {
        NestedScrollView nestedScrollView = this.scrollRoot;
        if (nestedScrollView == null) {
            return;
        }
        nestedScrollView.setFocusableInTouchMode(true);
        this.scrollRoot.setDescendantFocusability(131072);
        this.scrollRoot.requestFocus();
        TextInputEditText textInputEditText = this.editSearch;
        if (textInputEditText != null) {
            textInputEditText.clearFocus();
        }
    }

    private void forceScrollTop() {
        NestedScrollView nestedScrollView = this.scrollRoot;
        if (nestedScrollView == null) {
            return;
        }
        nestedScrollView.post(new Runnable() { // from class:
                                               // ca.dnamobile.javalauncher.ContentBrowserActivity$$ExternalSyntheticLambda28
            @Override // java.lang.Runnable
            public final void run() {
                ContentBrowserActivity.this.lambda$forceScrollTop$6();
            }
        });
        this.scrollRoot.postDelayed(new Runnable() { // from class:
                                                     // ca.dnamobile.javalauncher.ContentBrowserActivity$$ExternalSyntheticLambda29
            @Override // java.lang.Runnable
            public final void run() {
                ContentBrowserActivity.this.lambda$forceScrollTop$7();
            }
        }, 120L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$forceScrollTop$6() {
        this.scrollRoot.fullScroll(33);
        this.scrollRoot.scrollTo(0, 0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$forceScrollTop$7() {
        this.scrollRoot.fullScroll(33);
        this.scrollRoot.scrollTo(0, 0);
    }

    private void pruneInstalledManifestForCurrentTab() {
        String str;
        if (this.selectedType == ModManagerContentType.MODPACKS || (str = this.gameDirectoryPath) == null || str.trim().isEmpty()) {
            return;
        }
        ModManagerManifest.pruneMissingFiles(new File(this.gameDirectoryPath), this.selectedType);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void loadContent(boolean z) {
        loadContent(z, true);
    }

    private void loadContent(boolean z, final boolean z2) {
        this.editSearch.setHint(getSearchHint(this.selectedType));
        this.textContentTitle.setText(this.selectedType == ModManagerContentType.MODPACKS ? "Browse Modpacks" : getString(R.string.content_browser_install_title));
        updateFilterChips();
        pruneInstalledManifestForCurrentTab();
        if (z) {
            this.currentPage = 0;
        }
        final int iIncrementAndGet = this.requestGeneration.incrementAndGet();
        final String strTrim = this.editSearch.getText() == null ? "" : this.editSearch.getText().toString().trim();
        final int i = this.currentPage * 20;
        this.textResultSummary.setText(getString(R.string.content_browser_loading_source, new Object
                []{getSelectedSourceLabel()}));
        updatePaginationControls();
        new Thread(new Runnable() { // from class:
                                    // ca.dnamobile.javalauncher.ContentBrowserActivity$$ExternalSyntheticLambda11
            @Override // java.lang.Runnable
            public final void run() {
                ContentBrowserActivity.this.lambda$loadContent$10(strTrim, i, iIncrementAndGet, z2);
            }
        }, this.selectedSource == ContentSource.CURSEFORGE ? "CurseForgeSearch" : "ModrinthSearch").start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$loadContent$10(String str, int i, final int i2, final boolean z) {
        ArrayList<ModrinthProject> arrayList;
        int i3;
        try {
            if (this.selectedType == ModManagerContentType.MODPACKS) {
                ModpackSearchApiClient.SearchResult searchResultSearch = ModpackSearchApiClient.search(this, this.selectedSource == ContentSource.CURSEFORGE ? ModManagerSource.CURSEFORGE : ModManagerSource.MODRINTH, str, this.gameVersionId, this.loader, 20, i);
                arrayList = searchResultSearch.hits;
                i3 = searchResultSearch.totalHits;
            } else {
                String str2 = "downloads";
                if (this.selectedSource == ContentSource.CURSEFORGE) {
                    CurseForgeApiClient curseForgeApiClient = new CurseForgeApiClient(this);
                    ModManagerContentType modManagerContentType = this.selectedType;
                    String str3 = this.gameVersionId;
                    String str4 = this.loader;
                    if (!str.isEmpty()) {
                        str2 = "popularity";
                    }
                    CurseForgeApiClient.SearchResult searchResultSearchProjects = curseForgeApiClient.searchProjects(str, modManagerContentType, str3, str4, 20, i, str2);
                    arrayList = searchResultSearchProjects.hits;
                    i3 = searchResultSearchProjects.totalHits;
                } else {
                    ModrinthApiClient modrinthApiClient = new ModrinthApiClient();
                    ModManagerContentType modManagerContentType2 = this.selectedType;
                    String str5 = this.gameVersionId;
                    String str6 = this.loader;
                    if (!str.isEmpty()) {
                        str2 = "relevance";
                    }
                    ModrinthApiClient.SearchResult searchResultSearchProjects2 = modrinthApiClient.searchProjects(str, modManagerContentType2, str5, str6, 20, i, str2);
                    arrayList = searchResultSearchProjects2.hits;
                    i3 = searchResultSearchProjects2.totalHits;
                }
            }
            final int i4 = i3;
            final ArrayList<ModrinthProject> arrayList2 = arrayList;
            runOnUiThread(new Runnable() { // from class:
                                           // ca.dnamobile.javalauncher.ContentBrowserActivity$$ExternalSyntheticLambda20
                @Override // java.lang.Runnable
                public final void run() {
                    ContentBrowserActivity.this.lambda$loadContent$8(i2, i4, arrayList2, z);
                }
            });
        } catch (Throwable th) {
            runOnUiThread(new Runnable() { // from class:
                                           // ca.dnamobile.javalauncher.ContentBrowserActivity$$ExternalSyntheticLambda21
                @Override // java.lang.Runnable
                public final void run() {
                    ContentBrowserActivity.this.lambda$loadContent$9(i2, th);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$loadContent$8(int i, int i2, ArrayList arrayList, boolean z) {
        if (i != this.requestGeneration.get()) {
            return;
        }
        this.totalHits = i2;
        this.adapter.submit(arrayList);
        updatePaginationControls();
        updateResultSummary();
        if (z) {
            TextInputEditText textInputEditText = this.editSearch;
            if (textInputEditText == null || !textInputEditText.hasFocus()) {
                forceScrollTop();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$loadContent$9(int i, Throwable th) {
        if (i != this.requestGeneration.get()) {
            return;
        }
        this.adapter.submit(new ArrayList());
        this.totalHits = 0;
        updatePaginationControls();
        this.textResultSummary.setText(getString(R.string.content_browser_load_failed, new Object
                []{th.getMessage() != null ? th.getMessage() : th.getClass().getSimpleName()}));
    }

    private void updateResultSummary() {
        this.textResultSummary.setText(getString(R.string.content_browser_result_summary, new Object
                []{Integer.valueOf(this.totalHits), getPluralLabel(this.selectedType), getSelectedSourceLabel()}));
    }

    private void updatePaginationControls() {
        int totalPages = getTotalPages(this.totalHits);
        this.buttonPagePrevious.setEnabled(this.currentPage > 0);
        this.buttonPageNext.setEnabled(this.currentPage + 1 < totalPages);
        this.textPageIndicator.setText(getString(R.string.content_browser_page_indicator, new Object
                []{Integer.valueOf(this.currentPage + 1), Integer.valueOf(totalPages)}));
    }

    private int getTotalPages(int i) {
        if (i <= 0) {
            return 1;
        }
        return (int) Math.ceil(((double) i) / 20.0d);
    }

    private void updateFilterChips() {
        if (this.selectedType == ModManagerContentType.MODPACKS && isGlobalBrowserMode()) {
            this.textVersionChip.setVisibility(8);
            this.textLoaderChip.setText("Modpacks");
            return;
        }
        if (this.gameVersionId.isEmpty()) {
            this.textVersionChip.setVisibility(8);
        } else {
            this.textVersionChip.setVisibility(0);
            this.textVersionChip.setText(this.gameVersionId);
        }
        this.textLoaderChip.setText(this.selectedType != ModManagerContentType.MODPACKS ? displayLoader(this.loader) : "Modpacks");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showProjectMenu(View view, final ModrinthProject modrinthProject) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenu().add(0, 1, 0, R.string.content_browser_install);
        popupMenu.getMenu().add(0, 2, 1, R.string.content_browser_view_versions);
        popupMenu.getMenu().add(0, 3, 2, R.string.content_browser_open_website);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() { // from
                                                                                       // class:
                                                                                       // ca.dnamobile.javalauncher.ContentBrowserActivity$$ExternalSyntheticLambda15
            @Override // androidx.appcompat.widget.PopupMenu.OnMenuItemClickListener
            public final boolean onMenuItemClick(MenuItem menuItem) {
                return ContentBrowserActivity.this.lambda$showProjectMenu$11(modrinthProject, menuItem);
            }
        });
        popupMenu.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean lambda$showProjectMenu$11(ModrinthProject modrinthProject, MenuItem menuItem) {
        if (menuItem.getItemId() == 1) {
            confirmInstall(modrinthProject);
            return true;
        }
        if (menuItem.getItemId() == 2) {
            openProjectDetails(modrinthProject);
            return true;
        }
        if (menuItem.getItemId() != 3) {
            return false;
        }
        openProjectWebsite(modrinthProject);
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void confirmInstall(final ModrinthProject modrinthProject) {
        if (this.selectedType == ModManagerContentType.MODPACKS) {
            showModpackVersionPicker(modrinthProject);
        } else if (this.gameDirectoryPath.trim().isEmpty()) {
            Toast.makeText(this, R.string.content_browser_missing_game_dir, 1).show();
        } else {
            new AlertDialog.Builder(this).setTitle(getString(R.string.content_browser_install_title_value, new Object
                            []{modrinthProject.title})).setMessage(getString(R.string.content_browser_install_message, new Object
                            []{modrinthProject.title, getPluralLabel(this.selectedType), this.gameVersionId.isEmpty() ? getString(R.string.content_browser_unknown_version) : this.gameVersionId, displayLoader(this.loader)})).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.content_browser_install, new DialogInterface.OnClickListener() { // from class: ca.dnamobile.javalauncher.ContentBrowserActivity$$ExternalSyntheticLambda5
                        @Override // android.content.DialogInterface.OnClickListener
                        public final void onClick(DialogInterface dialogInterface, int i) {
                            ContentBrowserActivity.this.lambda$confirmInstall$12(modrinthProject, dialogInterface, i);
                        }
                    }).show();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$confirmInstall$12(ModrinthProject modrinthProject, DialogInterface dialogInterface, int i) {
        installProject(modrinthProject);
    }

    private void installProject(final ModrinthProject modrinthProject) {
        final File file = new File(this.gameDirectoryPath);
        Toast.makeText(this, getString(R.string.content_browser_install_started, new Object
                []{modrinthProject.title}), 0).show();
        if (this.selectedType == ModManagerContentType.MODPACKS) {
            showModpackVersionPicker(modrinthProject);
        } else {
            final AnonymousClass3 anonymousClass3 = new AnonymousClass3();
            new Thread(new Runnable() { // from class:
                                        // ca.dnamobile.javalauncher.ContentBrowserActivity$$ExternalSyntheticLambda7
                @Override // java.lang.Runnable
                public final void run() {
                    ContentBrowserActivity.this.lambda$installProject$13(modrinthProject, file, anonymousClass3);
                }
            }, modrinthProject.source == ModManagerSource.CURSEFORGE ? "CurseForgeInstall" : "ModrinthInstall").start();
        }
    }

    /* JADX INFO: renamed from: ca.dnamobile.javalauncher.ContentBrowserActivity$3, reason: invalid class name */
    class AnonymousClass3 implements ModrinthInstallManager.Listener {
        AnonymousClass3() {}

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onStatus$0(String str) {
            ContentBrowserActivity.this.textResultSummary.setText(str);
        }

        @Override // ca.dnamobile.javalauncher.modmanager.ModrinthInstallManager.Listener
        public void onStatus(final String str) {
            ContentBrowserActivity.this.runOnUiThread(new Runnable() { // from class:
                                                                       // ca.dnamobile.javalauncher.ContentBrowserActivity$3$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    ContentBrowserActivity.this.lambda$onStatus$0(str);
                }
            });
        }

        @Override // ca.dnamobile.javalauncher.modmanager.ModrinthInstallManager.Listener
        public void onComplete(final String str) {
            ContentBrowserActivity.this.runOnUiThread(new Runnable() { // from class:
                                                                       // ca.dnamobile.javalauncher.ContentBrowserActivity$3$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    ContentBrowserActivity.this.lambda$onComplete$1(str);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onComplete$1(String str) {
            ContentBrowserActivity.this.textResultSummary.setText(str);
            Toast.makeText(ContentBrowserActivity.this, str, 1).show();
            ContentBrowserActivity.this.setResult(-1);
            ContentBrowserActivity.this.adapter.notifyDataSetChanged();
        }

        @Override // ca.dnamobile.javalauncher.modmanager.ModrinthInstallManager.Listener
        public void onError(final Throwable th) {
            ContentBrowserActivity.this.runOnUiThread(new Runnable() { // from class:
                                                                       // ca.dnamobile.javalauncher.ContentBrowserActivity$3$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    ContentBrowserActivity.this.lambda$onError$2(th);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onError$2(Throwable th) {
            String message = th.getMessage() != null ? th.getMessage() : th.getClass().getSimpleName();
            ContentBrowserActivity.this.textResultSummary.setText(ContentBrowserActivity.this.getString(R.string.content_browser_install_failed, new Object
                    []{message}));
            ContentBrowserActivity contentBrowserActivity = ContentBrowserActivity.this;
            Toast.makeText(contentBrowserActivity, contentBrowserActivity.getString(R.string.content_browser_install_failed, new Object
                    []{message}), 1).show();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$installProject$13(ModrinthProject modrinthProject, File file, ModrinthInstallManager.Listener listener) {
        if (modrinthProject.source == ModManagerSource.CURSEFORGE) {
            CurseForgeInstallManager.installLatestCompatible(new CurseForgeApiClient(this), file, this.gameVersionId, this.loader, this.selectedType, modrinthProject, listener);
        } else {
            ModrinthInstallManager.installLatestCompatible(file, this.gameVersionId, this.loader, this.selectedType, modrinthProject, listener);
        }
    }

    private void installModpackProject(final ModrinthProject modrinthProject, final ModpackInstallManager.ModpackVersionChoice modpackVersionChoice) {
        showModpackInstallDialog(modrinthProject.title);
        final AnonymousClass4 anonymousClass4 = new AnonymousClass4();
        new Thread(new Runnable() { // from class:
                                    // ca.dnamobile.javalauncher.ContentBrowserActivity$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                ContentBrowserActivity.this.lambda$installModpackProject$14(modpackVersionChoice, modrinthProject, anonymousClass4);
            }
        }, modrinthProject.source == ModManagerSource.CURSEFORGE ? "CurseForgeModpackInstall" : "ModrinthModpackInstall").start();
    }

    /* JADX INFO: renamed from: ca.dnamobile.javalauncher.ContentBrowserActivity$4, reason: invalid class name */
    class AnonymousClass4 implements ModpackInstallManager.Listener {
        AnonymousClass4() {}

        @Override // ca.dnamobile.javalauncher.modmanager.ModpackInstallManager.Listener
        public void onStatus(final String str) {
            ContentBrowserActivity.this.runOnUiThread(new Runnable() { // from class:
                                                                       // ca.dnamobile.javalauncher.ContentBrowserActivity$4$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    ContentBrowserActivity.this.lambda$onStatus$0(str);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onStatus$0(String str) {
            ContentBrowserActivity.this.textResultSummary.setText(str);
            ContentBrowserActivity.this.updateModpackInstallDialog(str, -1, -1);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onProgress$1(int i, int i2) {
            ContentBrowserActivity.this.updateModpackInstallDialog(null, i, i2);
        }

        @Override // ca.dnamobile.javalauncher.modmanager.ModpackInstallManager.Listener
        public void onProgress(final int i, final int i2) {
            ContentBrowserActivity.this.runOnUiThread(new Runnable() { // from class:
                                                                       // ca.dnamobile.javalauncher.ContentBrowserActivity$4$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    ContentBrowserActivity.this.lambda$onProgress$1(i, i2);
                }
            });
        }

        @Override // ca.dnamobile.javalauncher.modmanager.ModpackInstallManager.Listener
        public void onComplete(String str) {
            onComplete(str, null);
        }

        @Override // ca.dnamobile.javalauncher.modmanager.ModpackInstallManager.Listener
        public void onComplete(final String str, final LauncherInstance launcherInstance) {
            ContentBrowserActivity.this.runOnUiThread(new Runnable() { // from class:
                                                                       // ca.dnamobile.javalauncher.ContentBrowserActivity$4$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    ContentBrowserActivity.this.lambda$onComplete$2(str, launcherInstance);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onComplete$2(String str, LauncherInstance launcherInstance) {
            ContentBrowserActivity.this.textResultSummary.setText(str);
            ContentBrowserActivity.this.dismissModpackInstallDialog();
            Toast.makeText(ContentBrowserActivity.this, str, 1).show();
            ContentBrowserActivity.this.setResult(-1);
            ContentBrowserActivity.this.openInstalledModpackInstance(launcherInstance);
        }

        @Override // ca.dnamobile.javalauncher.modmanager.ModpackInstallManager.Listener
        public void onError(final Throwable th) {
            ContentBrowserActivity.this.runOnUiThread(new Runnable() { // from class:
                                                                       // ca.dnamobile.javalauncher.ContentBrowserActivity$4$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    ContentBrowserActivity.this.lambda$onError$3(th);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onError$3(Throwable th) {
            ContentBrowserActivity.this.dismissModpackInstallDialog();
            String message = th.getMessage() != null ? th.getMessage() : th.getClass().getSimpleName();
            ContentBrowserActivity.this.textResultSummary.setText(ContentBrowserActivity.this.getString(R.string.content_browser_install_failed, new Object
                    []{message}));
            ContentBrowserActivity contentBrowserActivity = ContentBrowserActivity.this;
            Toast.makeText(contentBrowserActivity, contentBrowserActivity.getString(R.string.content_browser_install_failed, new Object
                    []{message}), 1).show();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$installModpackProject$14(ModpackInstallManager.ModpackVersionChoice modpackVersionChoice, ModrinthProject modrinthProject, ModpackInstallManager.Listener listener) {
        if (modpackVersionChoice != null) {
            ModpackInstallManager.installFromProjectVersion(this, modrinthProject.source, modrinthProject.projectId, modrinthProject.slug, modrinthProject.title, modrinthProject.iconUrl, modpackVersionChoice, listener);
        } else {
            ModpackInstallManager.installFromProject(this, modrinthProject.source, modrinthProject.projectId, modrinthProject.slug, modrinthProject.title, modrinthProject.iconUrl, this.gameVersionId, this.loader, listener);
        }
    }

    private void showModpackVersionPicker(final ModrinthProject modrinthProject) {
        showModpackVersionLoadingDialog(modrinthProject.title);
        new Thread(new Runnable() { // from class:
                                    // ca.dnamobile.javalauncher.ContentBrowserActivity$$ExternalSyntheticLambda22
            @Override // java.lang.Runnable
            public final void run() {
                ContentBrowserActivity.this.lambda$showModpackVersionPicker$17(modrinthProject);
            }
        }, modrinthProject.source == ModManagerSource.CURSEFORGE ? "CurseForgeModpackVersions" : "ModrinthModpackVersions").start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showModpackVersionPicker$17(final ModrinthProject modrinthProject) {
        try {
            final ArrayList<
                    ModpackInstallManager.ModpackVersionChoice> arrayListListProjectVersions = ModpackInstallManager.listProjectVersions(this, modrinthProject.source, modrinthProject.projectId, modrinthProject.slug);
            runOnUiThread(new Runnable() { // from class:
                                           // ca.dnamobile.javalauncher.ContentBrowserActivity$$ExternalSyntheticLambda13
                @Override // java.lang.Runnable
                public final void run() {
                    ContentBrowserActivity.this.lambda$showModpackVersionPicker$15(arrayListListProjectVersions, modrinthProject);
                }
            });
        } catch (Throwable th) {
            runOnUiThread(new Runnable() { // from class:
                                           // ca.dnamobile.javalauncher.ContentBrowserActivity$$ExternalSyntheticLambda14
                @Override // java.lang.Runnable
                public final void run() {
                    ContentBrowserActivity.this.lambda$showModpackVersionPicker$16(th);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showModpackVersionPicker$15(ArrayList arrayList, ModrinthProject modrinthProject) {
        dismissModpackVersionLoadingDialog();
        if (arrayList.isEmpty()) {
            Toast.makeText(this, "No installable modpack versions were found for " + modrinthProject.title + ".", 1).show();
        } else {
            showModpackVersionSelectionDialog(modrinthProject, arrayList);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showModpackVersionPicker$16(Throwable th) {
        dismissModpackVersionLoadingDialog();
        Toast.makeText(this, "Unable to load modpack versions: " + (th.getMessage() != null ? th.getMessage() : th.getClass().getSimpleName()), 1).show();
    }

    private void showModpackVersionLoadingDialog(String str) {
        dismissModpackVersionLoadingDialog();
        AlertDialog alertDialogCreate = new AlertDialog.Builder(this).setTitle("Loading Versions").setMessage("Fetching available versions for " + str + "...").setCancelable(true).create();
        this.modpackVersionLoadingDialog = alertDialogCreate;
        alertDialogCreate.show();
        FullscreenUtils.enableImmersive(this);
    }

    private void dismissModpackVersionLoadingDialog() {
        AlertDialog alertDialog = this.modpackVersionLoadingDialog;
        if (alertDialog != null) {
            alertDialog.dismiss();
            this.modpackVersionLoadingDialog = null;
        }
    }

    private void showModpackVersionSelectionDialog(ModrinthProject modrinthProject, ArrayList<
                    ModpackInstallManager.ModpackVersionChoice> arrayList) {
        LinkedHashMap<
                String,
                ArrayList<
                        ModpackInstallManager.ModpackVersionChoice>> linkedHashMapGroupModpackVersionsByMinecraftVersion = groupModpackVersionsByMinecraftVersion(arrayList);
        if (linkedHashMapGroupModpackVersionsByMinecraftVersion.isEmpty()) {
            Toast.makeText(this, "No installable modpack versions were found for " + modrinthProject.title + ".", 1).show();
        } else {
            showMinecraftVersionPickerDialog(modrinthProject, linkedHashMapGroupModpackVersionsByMinecraftVersion);
        }
    }

    private void showMinecraftVersionPickerDialog(final ModrinthProject modrinthProject, final LinkedHashMap<
                    String, ArrayList<ModpackInstallManager.ModpackVersionChoice>> linkedHashMap) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        int iDp = dp(20);
        linearLayout.setPadding(iDp, dp(2), iDp, 0);
        TextView textView = new TextView(this);
        textView.setText("Choose the Minecraft version first. The next screen will show every pack version available for that Minecraft version.");
        textView.setTextSize(2, 14.0f);
        textView.setTextColor(resolveThemeColor(android.R.attr.textColorSecondary, -3355444));
        textView.setLineSpacing(dp(1), 1.0f);
        linearLayout.addView(textView, new LinearLayout.LayoutParams(-1, -2));
        ArrayList arrayList = new ArrayList();
        for (Map.Entry<
                String,
                ArrayList<
                        ModpackInstallManager.ModpackVersionChoice>> entry : linkedHashMap.entrySet()) {
            arrayList.add(new ModpackMinecraftVersionGroup(entry.getKey(), entry.getValue()));
        }
        RecyclerView recyclerView = new RecyclerView(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setClipToPadding(false);
        recyclerView.setPadding(0, dp(10), 0, dp(14));
        recyclerView.setAdapter(new ModpackMinecraftVersionDialogAdapter(arrayList, new ModpackMinecraftVersionClickListener() { // from class: ca.dnamobile.javalauncher.ContentBrowserActivity$$ExternalSyntheticLambda17
            @Override // ca.dnamobile.javalauncher.ContentBrowserActivity.ModpackMinecraftVersionClickListener
            public final void onMinecraftVersionClicked(ContentBrowserActivity.ModpackMinecraftVersionGroup modpackMinecraftVersionGroup) {
                ContentBrowserActivity.this.lambda$showMinecraftVersionPickerDialog$18(modrinthProject, linkedHashMap, modpackMinecraftVersionGroup);
            }
        }));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, Math.min(Math.max(dp(240), getResources().getDisplayMetrics().heightPixels - dp(230)), dp(460)));
        layoutParams.topMargin = dp(8);
        linearLayout.addView(recyclerView, layoutParams);
        AlertDialog alertDialogCreate = new AlertDialog.Builder(this).setTitle("Pick Minecraft Version").setView(linearLayout).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).create();
        this.currentModpackVersionDialog = alertDialogCreate;
        alertDialogCreate.setOnDismissListener(new DialogInterface.OnDismissListener() { // from
                                                                                         // class:
                                                                                         // ca.dnamobile.javalauncher.ContentBrowserActivity$$ExternalSyntheticLambda18
            @Override // android.content.DialogInterface.OnDismissListener
            public final void onDismiss(DialogInterface dialogInterface) {
                ContentBrowserActivity.this.lambda$showMinecraftVersionPickerDialog$19(dialogInterface);
            }
        });
        this.currentModpackVersionDialog.setOnShowListener(new DialogInterface.OnShowListener() { // from class: ca.dnamobile.javalauncher.ContentBrowserActivity$$ExternalSyntheticLambda19
            @Override // android.content.DialogInterface.OnShowListener
            public final void onShow(DialogInterface dialogInterface) {
                ContentBrowserActivity.this.lambda$showMinecraftVersionPickerDialog$20(dialogInterface);
            }
        });
        this.currentModpackVersionDialog.show();
        FullscreenUtils.enableImmersive(this);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showMinecraftVersionPickerDialog$18(ModrinthProject modrinthProject, LinkedHashMap linkedHashMap, ModpackMinecraftVersionGroup modpackMinecraftVersionGroup) {
        AlertDialog alertDialog = this.currentModpackVersionDialog;
        if (alertDialog != null) {
            alertDialog.dismiss();
        }
        showModpackVersionsForMinecraftDialog(modrinthProject, linkedHashMap, modpackMinecraftVersionGroup.minecraftVersion, modpackMinecraftVersionGroup.versions);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showMinecraftVersionPickerDialog$19(DialogInterface dialogInterface) {
        this.currentModpackVersionDialog = null;
        FullscreenUtils.enableImmersive(this);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showMinecraftVersionPickerDialog$20(DialogInterface dialogInterface) {
        FullscreenUtils.enableImmersive(this);
    }

    private void showModpackVersionsForMinecraftDialog(final ModrinthProject modrinthProject, final LinkedHashMap<
                    String,
                    ArrayList<
                            ModpackInstallManager.ModpackVersionChoice>> linkedHashMap, String str, ArrayList<
                    ModpackInstallManager.ModpackVersionChoice> arrayList) {
        ArrayList<
                ModpackInstallManager.ModpackVersionChoice> arrayList2 = new ArrayList<>(arrayList);
        sortModpackVersionsNewestFirst(arrayList2);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        int iDp = dp(20);
        linearLayout.setPadding(iDp, dp(2), iDp, 0);
        TextView textView = new TextView(this);
        textView.setText("Choose which " + modrinthProject.title + " version to install for " + formatMinecraftVersionTitle(str) + ". Newest pack versions are listed first.");
        textView.setTextSize(2, 14.0f);
        textView.setTextColor(resolveThemeColor(android.R.attr.textColorSecondary, -3355444));
        textView.setLineSpacing(dp(1), 1.0f);
        linearLayout.addView(textView, new LinearLayout.LayoutParams(-1, -2));
        RecyclerView recyclerView = new RecyclerView(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setClipToPadding(false);
        recyclerView.setPadding(0, dp(10), 0, dp(14));
        ArrayList arrayList3 = new ArrayList();
        Iterator<ModpackInstallManager.ModpackVersionChoice> it = arrayList2.iterator();
        while (it.hasNext()) {
            arrayList3.add(ModpackVersionDialogRow.version(it.next()));
        }
        recyclerView.setAdapter(new ModpackVersionDialogAdapter(arrayList3, new ModpackVersionClickListener() { // from class: ca.dnamobile.javalauncher.ContentBrowserActivity$$ExternalSyntheticLambda30
            @Override // ca.dnamobile.javalauncher.ContentBrowserActivity.ModpackVersionClickListener
            public final void onVersionClicked(ModpackInstallManager.ModpackVersionChoice modpackVersionChoice) {
                ContentBrowserActivity.this.lambda$showModpackVersionsForMinecraftDialog$21(modrinthProject, modpackVersionChoice);
            }
        }));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, Math.min(Math.max(dp(260), getResources().getDisplayMetrics().heightPixels - dp(230)), dp(540)));
        layoutParams.topMargin = dp(8);
        linearLayout.addView(recyclerView, layoutParams);
        AlertDialog alertDialogCreate = new AlertDialog.Builder(this).setTitle(formatMinecraftVersionTitle(str)).setView(linearLayout).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setNeutralButton("Minecraft Versions", (DialogInterface.OnClickListener) null).create();
        this.currentModpackVersionDialog = alertDialogCreate;
        alertDialogCreate.setOnDismissListener(new DialogInterface.OnDismissListener() { // from
                                                                                         // class:
                                                                                         // ca.dnamobile.javalauncher.ContentBrowserActivity$$ExternalSyntheticLambda1
            @Override // android.content.DialogInterface.OnDismissListener
            public final void onDismiss(DialogInterface dialogInterface) {
                ContentBrowserActivity.this.lambda$showModpackVersionsForMinecraftDialog$22(dialogInterface);
            }
        });
        this.currentModpackVersionDialog.setOnShowListener(new DialogInterface.OnShowListener() { // from class: ca.dnamobile.javalauncher.ContentBrowserActivity$$ExternalSyntheticLambda2
            @Override // android.content.DialogInterface.OnShowListener
            public final void onShow(DialogInterface dialogInterface) {
                ContentBrowserActivity.this.lambda$showModpackVersionsForMinecraftDialog$23(dialogInterface);
            }
        });
        this.currentModpackVersionDialog.show();
        this.currentModpackVersionDialog.getButton(-3).setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.ContentBrowserActivity$$ExternalSyntheticLambda3
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ContentBrowserActivity.this.lambda$showModpackVersionsForMinecraftDialog$24(modrinthProject, linkedHashMap, view);
            }
        });
        FullscreenUtils.enableImmersive(this);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showModpackVersionsForMinecraftDialog$21(ModrinthProject modrinthProject, ModpackInstallManager.ModpackVersionChoice modpackVersionChoice) {
        AlertDialog alertDialog = this.currentModpackVersionDialog;
        if (alertDialog != null) {
            alertDialog.dismiss();
        }
        confirmInstallModpackVersion(modrinthProject, modpackVersionChoice);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showModpackVersionsForMinecraftDialog$22(DialogInterface dialogInterface) {
        this.currentModpackVersionDialog = null;
        FullscreenUtils.enableImmersive(this);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showModpackVersionsForMinecraftDialog$23(DialogInterface dialogInterface) {
        FullscreenUtils.enableImmersive(this);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showModpackVersionsForMinecraftDialog$24(ModrinthProject modrinthProject, LinkedHashMap linkedHashMap, View view) {
        AlertDialog alertDialog = this.currentModpackVersionDialog;
        if (alertDialog != null) {
            alertDialog.dismiss();
        }
        showMinecraftVersionPickerDialog(modrinthProject, linkedHashMap);
    }

    private void confirmInstallModpackVersion(final ModrinthProject modrinthProject, final ModpackInstallManager.ModpackVersionChoice modpackVersionChoice) {
        StringBuilder sb = new StringBuilder("Install ");
        sb.append(modrinthProject.title).append(" as a new launcher instance?\n\nSelected version:\n");
        sb.append(modpackVersionChoice.getDisplayTitle());
        sb.append("\n").append(modpackVersionChoice.getDisplaySubtitle());
        if (!isGlobalBrowserMode() && !modpackVersionChoice.isCompatibleWith(this.gameVersionId, this.loader)) {
            sb.append("\n\nWarning: this version does not match the current instance filter. The installed modpack will still use the pack's own Minecraft version and loader.");
        }
        new AlertDialog.Builder(this).setTitle("Install Modpack").setMessage(sb.toString()).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.content_browser_install, new DialogInterface.OnClickListener() { // from class: ca.dnamobile.javalauncher.ContentBrowserActivity$$ExternalSyntheticLambda27
                    @Override // android.content.DialogInterface.OnClickListener
                    public final void onClick(DialogInterface dialogInterface, int i) {
                        ContentBrowserActivity.this.lambda$confirmInstallModpackVersion$25(modrinthProject, modpackVersionChoice, dialogInterface, i);
                    }
                }).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$confirmInstallModpackVersion$25(ModrinthProject modrinthProject, ModpackInstallManager.ModpackVersionChoice modpackVersionChoice, DialogInterface dialogInterface, int i) {
        installModpackProject(modrinthProject, modpackVersionChoice);
    }

    private void showModpackInstallDialog(String str) {
        dismissModpackInstallDialog();
        getWindow().addFlags(128);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        int iDp = dp(22);
        linearLayout.setPadding(iDp, dp(8), iDp, 0);
        TextView textView = new TextView(this);
        this.modpackInstallMessage = textView;
        textView.setText("Preparing modpack install...");
        linearLayout.addView(this.modpackInstallMessage, new LinearLayout.LayoutParams(-1, -2));
        ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        this.modpackInstallProgress = progressBar;
        progressBar.setIndeterminate(true);
        this.modpackInstallProgress.setMax(100);
        this.modpackInstallProgress.setProgress(0);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = dp(12);
        linearLayout.addView(this.modpackInstallProgress, layoutParams);
        AlertDialog alertDialogCreate = new AlertDialog.Builder(this).setTitle("Installing " + str).setMessage("Please do not close JavaLauncher while this modpack is being installed.").setView(linearLayout).setCancelable(false).create();
        this.modpackInstallDialog = alertDialogCreate;
        alertDialogCreate.setCanceledOnTouchOutside(false);
        this.modpackInstallDialog.setOnDismissListener(new DialogInterface.OnDismissListener() { // from class: ca.dnamobile.javalauncher.ContentBrowserActivity$$ExternalSyntheticLambda26
            @Override // android.content.DialogInterface.OnDismissListener
            public final void onDismiss(DialogInterface dialogInterface) {
                ContentBrowserActivity.this.lambda$showModpackInstallDialog$26(dialogInterface);
            }
        });
        this.modpackInstallDialog.show();
        FullscreenUtils.enableImmersive(this);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showModpackInstallDialog$26(DialogInterface dialogInterface) {
        getWindow().clearFlags(128);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateModpackInstallDialog(String str, int i, int i2) {
        TextView textView;
        if (str != null && (textView = this.modpackInstallMessage) != null) {
            textView.setText(str);
        }
        ProgressBar progressBar = this.modpackInstallProgress;
        if (progressBar == null) {
            return;
        }
        if (i2 > 0) {
            progressBar.setIndeterminate(false);
            this.modpackInstallProgress.setMax(i2);
            this.modpackInstallProgress.setProgress(Math.max(0, Math.min(i, i2)));
        } else if (i < 0) {
            progressBar.setIndeterminate(true);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dismissModpackInstallDialog() {
        AlertDialog alertDialog = this.modpackInstallDialog;
        if (alertDialog != null) {
            alertDialog.dismiss();
            this.modpackInstallDialog = null;
        }
        this.modpackInstallMessage = null;
        this.modpackInstallProgress = null;
        getWindow().clearFlags(128);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void openInstalledModpackInstance(LauncherInstance launcherInstance) {
        if (launcherInstance == null) {
            finish();
        } else {
            startActivity(InstanceDetailsActivity.createIntent(this, launcherInstance));
            finish();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void openProjectDetails(ModrinthProject modrinthProject) {
        Intent intent = new Intent(this, (Class<?>) ContentProjectDetailsActivity.class);
        intent.putExtra(InstanceDetailsActivity.EXTRA_INSTANCE_ID, this.instanceId);
        intent.putExtra(InstanceDetailsActivity.EXTRA_INSTANCE_NAME, this.instanceName);
        intent.putExtra(InstanceDetailsActivity.EXTRA_INSTANCE_LOADER, this.loader);
        intent.putExtra(InstanceDetailsActivity.EXTRA_BASE_VERSION_ID, this.baseVersionId);
        intent.putExtra(InstanceDetailsActivity.EXTRA_MINECRAFT_VERSION_ID, this.gameVersionId);
        intent.putExtra(InstanceDetailsActivity.EXTRA_GAME_DIRECTORY, this.gameDirectoryPath);
        intent.putExtra(InstanceDetailsActivity.EXTRA_CONTENT_CATEGORY, this.selectedType.getIntentValue());
        intent.putExtra(EXTRA_PROJECT_ID, modrinthProject.projectId);
        intent.putExtra(EXTRA_PROJECT_SLUG, modrinthProject.slug);
        intent.putExtra(EXTRA_PROJECT_TITLE, modrinthProject.title);
        intent.putExtra(EXTRA_PROJECT_TYPE, this.selectedType.getIntentValue());
        intent.putExtra(EXTRA_PROJECT_ICON_URL, modrinthProject.iconUrl == null ? "" : modrinthProject.iconUrl);
        intent.putExtra(EXTRA_PROJECT_SOURCE, modrinthProject.source.getId());
        startActivity(intent);
    }

    private void openProjectWebsite(ModrinthProject modrinthProject) {
        try {
            startActivity(new Intent("android.intent.action.VIEW", Uri.parse(modrinthProject.getWebsiteUrl())));
        } catch (ActivityNotFoundException unused) {
            Toast.makeText(this, modrinthProject.getWebsiteUrl(), 1).show();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int dp(int i) {
        return Math.round(i * getResources().getDisplayMetrics().density);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isGlobalBrowserMode() {
        String str = this.gameDirectoryPath;
        return str == null || str.trim().isEmpty();
    }

    private boolean isBrowseModpacksOnlyMode() {
        return isGlobalBrowserMode() && this.selectedType == ModManagerContentType.MODPACKS;
    }

    private String displayLoader(String str) {
        if (str == null || str.trim().isEmpty()) {
            return "Vanilla";
        }
        String strTrim = str.trim();
        return strTrim.substring(0, 1).toUpperCase(Locale.US) + strTrim.substring(1);
    }

    /* JADX INFO: renamed from: ca.dnamobile.javalauncher.ContentBrowserActivity$5, reason: invalid class name */
    static /* synthetic */ class AnonymousClass5 {
        static final /* synthetic */ int
                [] $SwitchMap$ca$dnamobile$javalauncher$modmanager$ModManagerContentType;

        static {
            int[] iArr = new int[ModManagerContentType.values().length];
            $SwitchMap$ca$dnamobile$javalauncher$modmanager$ModManagerContentType = iArr;
            try {
                iArr[ModManagerContentType.MODPACKS.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
            try {
                $SwitchMap$ca$dnamobile$javalauncher$modmanager$ModManagerContentType[
                ModManagerContentType.RESOURCEPACKS.ordinal()] = 2;
            } catch (NoSuchFieldError unused2) {
            }
            try {
                $SwitchMap$ca$dnamobile$javalauncher$modmanager$ModManagerContentType[
                ModManagerContentType.SHADERPACKS.ordinal()] = 3;
            } catch (NoSuchFieldError unused3) {
            }
            try {
                $SwitchMap$ca$dnamobile$javalauncher$modmanager$ModManagerContentType[
                ModManagerContentType.MODS.ordinal()] = 4;
            } catch (NoSuchFieldError unused4) {
            }
        }
    }

    private String getTabTitle(ModManagerContentType modManagerContentType) {
        int i = AnonymousClass5.$SwitchMap$ca$dnamobile$javalauncher$modmanager$ModManagerContentType[
        modManagerContentType.ordinal()];
        if (i == 1) {
            return "Modpacks";
        }
        if (i == 2) {
            return getString(R.string.content_browser_tab_resourcepacks);
        }
        if (i == 3) {
            return getString(R.string.content_browser_tab_shaders);
        }
        return getString(R.string.content_browser_tab_mods);
    }

    private String getSearchHint(ModManagerContentType modManagerContentType) {
        int i = AnonymousClass5.$SwitchMap$ca$dnamobile$javalauncher$modmanager$ModManagerContentType[
        modManagerContentType.ordinal()];
        if (i == 1) {
            return "Search modpacks";
        }
        if (i == 2) {
            return getString(R.string.content_browser_search_resourcepacks);
        }
        if (i == 3) {
            return getString(R.string.content_browser_search_shaders);
        }
        return getString(R.string.content_browser_search_mods);
    }

    private String getPluralLabel(ModManagerContentType modManagerContentType) {
        int i = AnonymousClass5.$SwitchMap$ca$dnamobile$javalauncher$modmanager$ModManagerContentType[
        modManagerContentType.ordinal()];
        if (i == 1) {
            return "modpacks";
        }
        if (i == 2) {
            return getString(R.string.content_browser_resourcepacks_plural);
        }
        if (i == 3) {
            return getString(R.string.content_browser_shaders_plural);
        }
        return getString(R.string.content_browser_mods_plural);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public ModManagerContentType resolveProjectDisplayType(ModrinthProject modrinthProject, ModManagerContentType modManagerContentType) {
        String lowerCase = modrinthProject.projectType == null ? "" : modrinthProject.projectType.trim().toLowerCase(Locale.US);
        if ("modpack".equals(lowerCase) || categoryContains(modrinthProject, "modpack") || categoryContains(modrinthProject, "modpacks")) {
            return ModManagerContentType.MODPACKS;
        }
        if ("resourcepack".equals(lowerCase) || "resourcepacks".equals(lowerCase) || categoryContains(modrinthProject, "resourcepack") || categoryContains(modrinthProject, "resourcepacks")) {
            return ModManagerContentType.RESOURCEPACKS;
        }
        return ("shader".equals(lowerCase) || "shaderpack".equals(lowerCase) || "shaderpacks".equals(lowerCase) || categoryContains(modrinthProject, "shader") || categoryContains(modrinthProject, "shaders") || categoryContains(modrinthProject, "shaderpack") || categoryContains(modrinthProject, "shaderpacks")) ? ModManagerContentType.SHADERPACKS : modManagerContentType;
    }

    private boolean categoryContains(ModrinthProject modrinthProject, String str) {
        for (String str2 : modrinthProject.categories) {
            if (str2 != null && str.equalsIgnoreCase(str2.trim())) {
                return true;
            }
        }
        return false;
    }

    private String normalizeImageUrl(String str) {
        if (str == null) {
            return null;
        }
        String strTrim = str.trim();
        if ((strTrim.startsWith("\"") && strTrim.endsWith("\"")) || (strTrim.startsWith("'") && strTrim.endsWith("'"))) {
            strTrim = strTrim.substring(1, strTrim.length() - 1).trim();
        }
        if (strTrim.isEmpty() || "null".equalsIgnoreCase(strTrim)) {
            return null;
        }
        if (strTrim.startsWith("//")) {
            return "https:" + strTrim;
        }
        if (strTrim.startsWith("http://") || strTrim.startsWith("https://")) {
            return strTrim;
        }
        return null;
    }

    private String getImmediateProjectImageUrl(ModrinthProject modrinthProject) {
        String strNormalizeImageUrl = normalizeImageUrl(this.resolvedProjectIconUrls.get(buildProjectIconCacheKey(modrinthProject)));
        if (strNormalizeImageUrl != null) {
            return strNormalizeImageUrl;
        }
        String strNormalizeImageUrl2 = normalizeImageUrl(modrinthProject.iconUrl);
        if (strNormalizeImageUrl2 != null) {
            return strNormalizeImageUrl2;
        }
        Iterator<String> it = modrinthProject.galleryUrls.iterator();
        while (it.hasNext()) {
            String strNormalizeImageUrl3 = normalizeImageUrl(it.next());
            if (strNormalizeImageUrl3 != null) {
                return strNormalizeImageUrl3;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void bindProjectIcon(ImageView imageView, ModrinthProject modrinthProject, ModManagerContentType modManagerContentType) {
        String strBuildProjectIconCacheKey = buildProjectIconCacheKey(modrinthProject);
        int fallbackIcon = getFallbackIcon(modManagerContentType);
        imageView.setTag(strBuildProjectIconCacheKey);
        String immediateProjectImageUrl = getImmediateProjectImageUrl(modrinthProject);
        NetworkImageLoader.load(imageView, immediateProjectImageUrl, fallbackIcon);
        if (modManagerContentType == ModManagerContentType.RESOURCEPACKS || modManagerContentType == ModManagerContentType.SHADERPACKS || immediateProjectImageUrl == null) {
            resolveProjectIconUrlAsync(imageView, modrinthProject, strBuildProjectIconCacheKey, fallbackIcon);
        }
    }

    private String buildProjectIconCacheKey(ModrinthProject modrinthProject) {
        String strTrim = modrinthProject.projectId == null ? "" : modrinthProject.projectId.trim();
        if (strTrim.isEmpty()) {
            strTrim = modrinthProject.slug == null ? "" : modrinthProject.slug.trim();
        }
        if (strTrim.isEmpty()) {
            strTrim = modrinthProject.title != null ? modrinthProject.title.trim() : "";
        }
        return modrinthProject.source.getId() + ":" + strTrim;
    }

    private void resolveProjectIconUrlAsync(final ImageView imageView, final ModrinthProject modrinthProject, final String str, final int i) {
        String strNormalizeImageUrl = normalizeImageUrl(this.resolvedProjectIconUrls.get(str));
        if (strNormalizeImageUrl != null) {
            if (str.equals(imageView.getTag())) {
                NetworkImageLoader.load(imageView, strNormalizeImageUrl, i);
            }
        } else if (this.resolvingProjectIconUrls.add(str)) {
            new Thread(new Runnable() { // from class:
                                        // ca.dnamobile.javalauncher.ContentBrowserActivity$$ExternalSyntheticLambda12
                @Override // java.lang.Runnable
                public final void run() {
                    ContentBrowserActivity.this.lambda$resolveProjectIconUrlAsync$28(modrinthProject, str, imageView, i);
                }
            }, "ResolveContentIcon").start();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$resolveProjectIconUrlAsync$28(ModrinthProject modrinthProject, final String str, final ImageView imageView, final int i) {
        final String strNormalizeImageUrl = null;
        try {
            strNormalizeImageUrl = normalizeImageUrl(fetchProjectIconUrl(modrinthProject));
            if (strNormalizeImageUrl != null) {
                this.resolvedProjectIconUrls.put(str, strNormalizeImageUrl);
            }
        } catch (Throwable unused) {
        }
        this.resolvingProjectIconUrls.remove(str);
        if (strNormalizeImageUrl == null) {
            return;
        }
        runOnUiThread(new Runnable() { // from class:
                                       // ca.dnamobile.javalauncher.ContentBrowserActivity$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                ContentBrowserActivity.lambda$resolveProjectIconUrlAsync$27(imageView, str, strNormalizeImageUrl, i);
            }
        });
    }

    static /* synthetic */ void lambda$resolveProjectIconUrlAsync$27(ImageView imageView, String str, String str2, int i) {
        if (str.equals(imageView.getTag())) {
            NetworkImageLoader.load(imageView, str2, i);
        }
    }

    private String fetchProjectIconUrl(ModrinthProject modrinthProject) throws Exception {
        if (modrinthProject.source == ModManagerSource.CURSEFORGE) {
            return fetchCurseForgeProjectIconUrl(modrinthProject);
        }
        return fetchModrinthProjectIconUrl(modrinthProject);
    }

    private String fetchModrinthProjectIconUrl(ModrinthProject modrinthProject) throws Exception {
        String strNormalizeImageUrl;
        String strTrim = modrinthProject.projectId == null ? "" : modrinthProject.projectId.trim();
        if (strTrim.isEmpty()) {
            strTrim = modrinthProject.slug == null ? "" : modrinthProject.slug.trim();
        }
        if (strTrim.isEmpty()) {
            return null;
        }
        JSONObject jSONObject = new JSONObject(httpGet("https://api.modrinth.com/v2/project/" + strTrim, null));
        String strNormalizeImageUrl2 = normalizeImageUrl(jSONObject.optString("icon_url", ""));
        if (strNormalizeImageUrl2 != null) {
            return strNormalizeImageUrl2;
        }
        JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("gallery");
        if (jSONArrayOptJSONArray != null) {
            for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
                JSONObject jSONObjectOptJSONObject = jSONArrayOptJSONArray.optJSONObject(i);
                if (jSONObjectOptJSONObject != null && (strNormalizeImageUrl = normalizeImageUrl(jSONObjectOptJSONObject.optString("raw_url", jSONObjectOptJSONObject.optString("url", "")))) != null) {
                    return strNormalizeImageUrl;
                }
            }
        }
        return null;
    }

    private String fetchCurseForgeProjectIconUrl(ModrinthProject modrinthProject) throws Exception {
        String strResolve;
        JSONObject jSONObjectOptJSONObject;
        JSONObject jSONObjectOptJSONObject2;
        String strTrim = modrinthProject.projectId == null ? "" : modrinthProject.projectId.trim();
        if (!strTrim.isEmpty() && (strResolve = CurseForgeApiKeyProvider.resolve()) != null && !strResolve.trim().isEmpty() && (jSONObjectOptJSONObject = new JSONObject(httpGet("https://api.curseforge.com/v1/mods/" + strTrim, strResolve.trim())).optJSONObject("data")) != null && (jSONObjectOptJSONObject2 = jSONObjectOptJSONObject.optJSONObject("logo")) != null) {
            String strNormalizeImageUrl = normalizeImageUrl(jSONObjectOptJSONObject2.optString("thumbnailUrl", ""));
            if (strNormalizeImageUrl != null) {
                return strNormalizeImageUrl;
            }
            String strNormalizeImageUrl2 = normalizeImageUrl(jSONObjectOptJSONObject2.optString("url", ""));
            if (strNormalizeImageUrl2 != null) {
                return strNormalizeImageUrl2;
            }
        }
        return null;
    }

    /* JADX WARN: Removed duplicated region for block: B:54:0x00aa A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private java.lang.String httpGet(java.lang.String r9, java.lang.String r10)
            throws java.lang.Exception {
        /*
            Method dump skipped, instruction units count: 203
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: ca.dnamobile.javalauncher.ContentBrowserActivity.httpGet(java.lang.String, java.lang.String):java.lang.String");
    }

    private int getFallbackIcon(ModManagerContentType modManagerContentType) {
        int i = AnonymousClass5.$SwitchMap$ca$dnamobile$javalauncher$modmanager$ModManagerContentType[
        modManagerContentType.ordinal()];
        if (i == 1) {
            return R.drawable.ic_content_mod_24;
        }
        if (i == 2) {
            return R.drawable.ic_content_resourcepack_24;
        }
        if (i == 3) {
            return R.drawable.ic_content_shaderpack_24;
        }
        return R.drawable.ic_content_mod_24;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String formatNumber(long j) {
        if (j >= 1000000000) {
            return String.format(Locale.US, "%.1fB", Double.valueOf(j / 1.0E9d));
        }
        if (j >= 1000000) {
            return String.format(Locale.US, "%.1fM", Double.valueOf(j / 1000000.0d));
        }
        if (j >= 1000) {
            return String.format(Locale.US, "%.1fK", Double.valueOf(j / 1000.0d));
        }
        return String.valueOf(j);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String formatTags(List<String> list) {
        if (list.isEmpty()) {
            return getString(R.string.content_browser_tag_unknown);
        }
        StringBuilder sb = new StringBuilder();
        int iMin = Math.min(2, list.size());
        for (int i = 0; i < iMin; i++) {
            if (sb.length() > 0) {
                sb.append("  ");
            }
            sb.append(formatTag(list.get(i)));
        }
        if (list.size() > iMin) {
            sb.append("  +").append(list.size() - iMin);
        }
        return sb.toString();
    }

    private String formatTag(String str) {
        String strTrim = str.replace('-', ' ').replace('_', ' ').trim();
        return strTrim.isEmpty() ? str : strTrim.substring(0, 1).toUpperCase(Locale.US) + strTrim.substring(1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isProjectInstalled(ModrinthProject modrinthProject) {
        if (this.selectedType == ModManagerContentType.MODPACKS || this.gameDirectoryPath.trim().isEmpty() || modrinthProject.projectId.trim().isEmpty()) {
            return false;
        }
        return ModManagerManifest.isProjectInstalled(new File(this.gameDirectoryPath), this.selectedType, modrinthProject.source.getId(), modrinthProject.projectId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public ModManagerSource getInstalledSource(ModrinthProject modrinthProject) {
        if (this.gameDirectoryPath.trim().isEmpty() || modrinthProject.projectId.trim().isEmpty()) {
            return ModManagerSource.UNKNOWN;
        }
        JSONObject installedEntryForProject = ModManagerManifest.getInstalledEntryForProject(new File(this.gameDirectoryPath), this.selectedType, modrinthProject.source, modrinthProject.projectId);
        return installedEntryForProject == null ? ModManagerSource.UNKNOWN : ModManagerManifest.getSource(installedEntryForProject);
    }

    private String getSelectedSourceLabel() {
        int i;
        if (this.selectedSource == ContentSource.CURSEFORGE) {
            i = R.string.content_browser_source_curseforge;
        } else {
            i = R.string.content_browser_source_modrinth;
        }
        return getString(i);
    }

    private LinkedHashMap<
                    String,
                    ArrayList<
                            ModpackInstallManager.ModpackVersionChoice>> groupModpackVersionsByMinecraftVersion(ArrayList<
                    ModpackInstallManager.ModpackVersionChoice> arrayList) {
        LinkedHashMap<
                String,
                ArrayList<
                        ModpackInstallManager.ModpackVersionChoice>> linkedHashMap = new LinkedHashMap<>();
        for (ModpackInstallManager.ModpackVersionChoice modpackVersionChoice : arrayList) {
            ArrayList<String> arrayList2 = modpackVersionChoice.gameVersions;
            if (arrayList2.isEmpty()) {
                addModpackVersionToGroup(linkedHashMap, "Unknown Minecraft version", modpackVersionChoice);
            } else {
                Iterator<String> it = arrayList2.iterator();
                while (it.hasNext()) {
                    String strNormalizeMinecraftVersionKey = normalizeMinecraftVersionKey(it.next());
                    if (strNormalizeMinecraftVersionKey.isEmpty()) {
                        strNormalizeMinecraftVersionKey = "Unknown Minecraft version";
                    }
                    addModpackVersionToGroup(linkedHashMap, strNormalizeMinecraftVersionKey, modpackVersionChoice);
                }
            }
        }
        ArrayList<String> arrayList3 = new ArrayList(linkedHashMap.keySet());
        Collections.sort(arrayList3, new Comparator() { // from class:
                                                        // ca.dnamobile.javalauncher.ContentBrowserActivity$$ExternalSyntheticLambda6
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                return ContentBrowserActivity.this.compareMinecraftVersionKeysDescending((String) obj, (String) obj2);
            }
        });
        LinkedHashMap<
                String,
                ArrayList<
                        ModpackInstallManager.ModpackVersionChoice>> linkedHashMap2 = new LinkedHashMap<>();
        for (String str : arrayList3) {
            ArrayList<
                    ModpackInstallManager.ModpackVersionChoice> arrayList4 = linkedHashMap.get(str);
            if (arrayList4 != null && !arrayList4.isEmpty()) {
                sortModpackVersionsNewestFirst(arrayList4);
                linkedHashMap2.put(str, arrayList4);
            }
        }
        return linkedHashMap2;
    }

    private void addModpackVersionToGroup(LinkedHashMap<
                    String,
                    ArrayList<
                            ModpackInstallManager.ModpackVersionChoice>> linkedHashMap, String str, ModpackInstallManager.ModpackVersionChoice modpackVersionChoice) {
        ArrayList<ModpackInstallManager.ModpackVersionChoice> arrayList = linkedHashMap.get(str);
        if (arrayList == null) {
            arrayList = new ArrayList<>();
            linkedHashMap.put(str, arrayList);
        }
        String strBuildModpackVersionIdentity = buildModpackVersionIdentity(modpackVersionChoice);
        Iterator<ModpackInstallManager.ModpackVersionChoice> it = arrayList.iterator();
        while (it.hasNext()) {
            if (strBuildModpackVersionIdentity.equals(buildModpackVersionIdentity(it.next()))) {
                return;
            }
        }
        arrayList.add(modpackVersionChoice);
    }

    private String buildModpackVersionIdentity(ModpackInstallManager.ModpackVersionChoice modpackVersionChoice) {
        return !modpackVersionChoice.versionId.trim().isEmpty() ? modpackVersionChoice.source.getId() + ":" + modpackVersionChoice.versionId : modpackVersionChoice.fileId > 0 ? modpackVersionChoice.source.getId() + ":file:" + modpackVersionChoice.fileId : modpackVersionChoice.source.getId() + ":" + modpackVersionChoice.fileName + ":" + modpackVersionChoice.downloadUrl;
    }

    private void sortModpackVersionsNewestFirst(ArrayList<
                    ModpackInstallManager.ModpackVersionChoice> arrayList) {
        Collections.sort(arrayList, new Comparator() { // from class:
                                                       // ca.dnamobile.javalauncher.ContentBrowserActivity$$ExternalSyntheticLambda4
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                return ContentBrowserActivity.this.lambda$sortModpackVersionsNewestFirst$29((ModpackInstallManager.ModpackVersionChoice) obj, (ModpackInstallManager.ModpackVersionChoice) obj2);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ int lambda$sortModpackVersionsNewestFirst$29(ModpackInstallManager.ModpackVersionChoice modpackVersionChoice, ModpackInstallManager.ModpackVersionChoice modpackVersionChoice2) {
        int iCompareNullableIsoDatesDescending = compareNullableIsoDatesDescending(modpackVersionChoice.datePublished, modpackVersionChoice2.datePublished);
        if (iCompareNullableIsoDatesDescending != 0) {
            return iCompareNullableIsoDatesDescending;
        }
        int iCompareVersionLabelsDescending = compareVersionLabelsDescending(modpackVersionChoice.versionNumber, modpackVersionChoice2.versionNumber);
        if (iCompareVersionLabelsDescending != 0) {
            return iCompareVersionLabelsDescending;
        }
        int iCompareVersionLabelsDescending2 = compareVersionLabelsDescending(modpackVersionChoice.versionName, modpackVersionChoice2.versionName);
        return iCompareVersionLabelsDescending2 != 0 ? iCompareVersionLabelsDescending2 : modpackVersionChoice2.getDisplayTitle().compareToIgnoreCase(modpackVersionChoice.getDisplayTitle());
    }

    private int compareNullableIsoDatesDescending(String str, String str2) {
        String strTrim = "";
        String strTrim2 = str == null ? "" : str.trim();
        if (str2 != null) {
            strTrim = str2.trim();
        }
        boolean zIsEmpty = strTrim2.isEmpty();
        boolean zIsEmpty2 = strTrim.isEmpty();
        if (zIsEmpty && zIsEmpty2) {
            return 0;
        }
        if (zIsEmpty) {
            return 1;
        }
        if (zIsEmpty2) {
            return -1;
        }
        return strTrim.compareToIgnoreCase(strTrim2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int compareMinecraftVersionKeysDescending(String str, String str2) {
        boolean zEqualsIgnoreCase = str.equalsIgnoreCase("Unknown Minecraft version");
        boolean zEqualsIgnoreCase2 = str2.equalsIgnoreCase("Unknown Minecraft version");
        if (zEqualsIgnoreCase && zEqualsIgnoreCase2) {
            return 0;
        }
        if (zEqualsIgnoreCase) {
            return 1;
        }
        if (zEqualsIgnoreCase2) {
            return -1;
        }
        int iCompareVersionLabelsDescending = compareVersionLabelsDescending(str, str2);
        return iCompareVersionLabelsDescending != 0 ? iCompareVersionLabelsDescending : str2.compareToIgnoreCase(str);
    }

    private int compareVersionLabelsDescending(String str, String str2) {
        String strTrim = "";
        String strTrim2 = str == null ? "" : str.trim();
        if (str2 != null) {
            strTrim = str2.trim();
        }
        boolean zIsEmpty = strTrim2.isEmpty();
        boolean zIsEmpty2 = strTrim.isEmpty();
        if (zIsEmpty && zIsEmpty2) {
            return 0;
        }
        if (zIsEmpty) {
            return 1;
        }
        if (zIsEmpty2) {
            return -1;
        }
        ArrayList<Integer> arrayListExtractVersionNumberParts = extractVersionNumberParts(strTrim2);
        ArrayList<Integer> arrayListExtractVersionNumberParts2 = extractVersionNumberParts(strTrim);
        int iMax = Math.max(arrayListExtractVersionNumberParts.size(), arrayListExtractVersionNumberParts2.size());
        int i = 0;
        while (i < iMax) {
            int iIntValue = i < arrayListExtractVersionNumberParts.size() ? arrayListExtractVersionNumberParts.get(i).intValue() : 0;
            int iIntValue2 = i < arrayListExtractVersionNumberParts2.size() ? arrayListExtractVersionNumberParts2.get(i).intValue() : 0;
            if (iIntValue != iIntValue2) {
                return Integer.compare(iIntValue2, iIntValue);
            }
            i++;
        }
        int iComparePrereleaseWeightDescending = comparePrereleaseWeightDescending(strTrim2, strTrim);
        return iComparePrereleaseWeightDescending != 0 ? iComparePrereleaseWeightDescending : strTrim.compareToIgnoreCase(strTrim2);
    }

    private int comparePrereleaseWeightDescending(String str, String str2) {
        return Integer.compare(getPrereleaseWeight(str2), getPrereleaseWeight(str));
    }

    private int getPrereleaseWeight(String str) {
        String lowerCase = str.toLowerCase(Locale.US);
        if (lowerCase.contains("snapshot")) {
            return 0;
        }
        if (lowerCase.contains("alpha")) {
            return 1;
        }
        if (lowerCase.contains("beta")) {
            return 2;
        }
        return (lowerCase.contains("rc") || lowerCase.contains("release-candidate")) ? 3 : 4;
    }

    private ArrayList<Integer> extractVersionNumberParts(String str) {
        ArrayList<Integer> arrayList = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char cCharAt = str.charAt(i);
            if (Character.isDigit(cCharAt)) {
                sb.append(cCharAt);
            } else if (sb.length() > 0) {
                addParsedVersionNumberPart(arrayList, sb.toString());
                sb.setLength(0);
            }
        }
        if (sb.length() > 0) {
            addParsedVersionNumberPart(arrayList, sb.toString());
        }
        return arrayList;
    }

    private void addParsedVersionNumberPart(ArrayList<Integer> arrayList, String str) {
        try {
            arrayList.add(Integer.valueOf(Integer.parseInt(str)));
        } catch (Throwable unused) {
            arrayList.add(0);
        }
    }

    private String normalizeMinecraftVersionKey(String str) {
        if (str == null) {
            return "";
        }
        String strTrim = str.trim();
        return strTrim.toLowerCase(Locale.US).startsWith("minecraft ") ? strTrim.substring("minecraft ".length()).trim() : strTrim;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String formatMinecraftVersionTitle(String str) {
        return str.equalsIgnoreCase("Unknown Minecraft version") ? str : "Minecraft " + str;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String buildMinecraftVersionGroupSubtitle(ModpackMinecraftVersionGroup modpackMinecraftVersionGroup) {
        StringBuilder sb = new StringBuilder();
        sb.append(modpackMinecraftVersionGroup.versions.size()).append(modpackMinecraftVersionGroup.versions.size() == 1 ? " pack version" : " pack versions");
        String strBuildLoaderSummary = buildLoaderSummary(modpackMinecraftVersionGroup.versions);
        if (!strBuildLoaderSummary.isEmpty()) {
            sb.append(" · ").append(strBuildLoaderSummary);
        }
        String newestPublishedDate = getNewestPublishedDate(modpackMinecraftVersionGroup.versions);
        if (!newestPublishedDate.isEmpty()) {
            sb.append(" · Newest ").append(newestPublishedDate);
        }
        return sb.toString();
    }

    private String buildLoaderSummary(ArrayList<
                    ModpackInstallManager.ModpackVersionChoice> arrayList) {
        ArrayList<String> arrayList2 = new ArrayList<>();
        Iterator<ModpackInstallManager.ModpackVersionChoice> it = arrayList.iterator();
        while (it.hasNext()) {
            Iterator<String> it2 = it.next().loaders.iterator();
            while (it2.hasNext()) {
                String next = it2.next();
                String strTrim = next == null ? "" : next.trim();
                if (!strTrim.isEmpty() && !containsIgnoreCase(arrayList2, strTrim)) {
                    arrayList2.add(strTrim);
                }
            }
        }
        return arrayList2.isEmpty() ? "" : "Loader " + joinShortList(arrayList2, 3);
    }

    private String getNewestPublishedDate(ArrayList<
                    ModpackInstallManager.ModpackVersionChoice> arrayList) {
        String str = "";
        for (ModpackInstallManager.ModpackVersionChoice modpackVersionChoice : arrayList) {
            String strTrim = modpackVersionChoice.datePublished == null ? "" : modpackVersionChoice.datePublished.trim();
            if (!strTrim.isEmpty() && (str.isEmpty() || strTrim.compareToIgnoreCase(str) > 0)) {
                str = strTrim;
            }
        }
        return str.isEmpty() ? "" : str.substring(0, Math.min(10, str.length()));
    }

    private boolean containsIgnoreCase(ArrayList<String> arrayList, String str) {
        for (String str2 : arrayList) {
            if (str2 != null && str2.equalsIgnoreCase(str)) {
                return true;
            }
        }
        return false;
    }

    private ArrayList<ModpackVersionDialogRow> buildModpackVersionDialogRows(ArrayList<
                    ModpackInstallManager.ModpackVersionChoice> arrayList) {
        String str;
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        for (ModpackInstallManager.ModpackVersionChoice modpackVersionChoice : arrayList) {
            String primaryGameVersion = getPrimaryGameVersion(modpackVersionChoice);
            ArrayList arrayList2 = (ArrayList) linkedHashMap.get(primaryGameVersion);
            if (arrayList2 == null) {
                arrayList2 = new ArrayList();
                linkedHashMap.put(primaryGameVersion, arrayList2);
            }
            arrayList2.add(modpackVersionChoice);
        }
        ArrayList<ModpackVersionDialogRow> arrayList3 = new ArrayList<>();
        for (Map.Entry entry : linkedHashMap.entrySet()) {
            if ("Unknown Minecraft version".equals(entry.getKey())) {
                str = (String) entry.getKey();
            } else {
                str = "Minecraft " + ((String) entry.getKey());
            }
            arrayList3.add(ModpackVersionDialogRow.header(str, ((ArrayList) entry.getValue()).size()));
            Iterator it = ((ArrayList) entry.getValue()).iterator();
            while (it.hasNext()) {
                arrayList3.add(ModpackVersionDialogRow.version((ModpackInstallManager.ModpackVersionChoice) it.next()));
            }
        }
        return arrayList3;
    }

    private String getPrimaryGameVersion(ModpackInstallManager.ModpackVersionChoice modpackVersionChoice) {
        return !modpackVersionChoice.gameVersions.isEmpty() ? modpackVersionChoice.gameVersions.get(0) : "Unknown Minecraft version";
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String buildVersionMetaLine(ModpackInstallManager.ModpackVersionChoice modpackVersionChoice) {
        StringBuilder sb = new StringBuilder();
        if (!modpackVersionChoice.gameVersions.isEmpty()) {
            sb.append("Minecraft ").append(joinShortList(modpackVersionChoice.gameVersions, 3));
        }
        if (!modpackVersionChoice.loaders.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append("Loader ").append(joinShortList(modpackVersionChoice.loaders, 2));
        }
        if (!modpackVersionChoice.datePublished.trim().isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(modpackVersionChoice.datePublished.substring(0, Math.min(10, modpackVersionChoice.datePublished.length())));
        }
        return sb.length() == 0 ? "No version metadata available" : sb.toString();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String buildVersionFileLine(ModpackInstallManager.ModpackVersionChoice modpackVersionChoice) {
        String strTrim = modpackVersionChoice.fileName == null ? "" : modpackVersionChoice.fileName.trim();
        if (!strTrim.isEmpty()) {
            return strTrim;
        }
        if (modpackVersionChoice.source == ModManagerSource.CURSEFORGE && modpackVersionChoice.fileId > 0) {
            return "CurseForge file " + modpackVersionChoice.fileId;
        }
        return modpackVersionChoice.versionId;
    }

    private String joinShortList(ArrayList<String> arrayList, int i) {
        StringBuilder sb = new StringBuilder();
        int iMin = Math.min(arrayList.size(), Math.max(1, i));
        for (int i2 = 0; i2 < iMin; i2++) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(arrayList.get(i2));
        }
        if (arrayList.size() > iMin) {
            sb.append(" +").append(arrayList.size() - iMin);
        }
        return sb.toString();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int resolveThemeColor(int i, int i2) {
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

    /* JADX INFO: Access modifiers changed from: private */
    static final class ModpackMinecraftVersionGroup {
        final String minecraftVersion;
        final ArrayList<ModpackInstallManager.ModpackVersionChoice> versions;

        ModpackMinecraftVersionGroup(String str, ArrayList<
                        ModpackInstallManager.ModpackVersionChoice> arrayList) {
            this.minecraftVersion = str;
            this.versions = new ArrayList<>(arrayList);
        }
    }

    private final class ModpackMinecraftVersionDialogAdapter
            extends RecyclerView.Adapter<ModpackMinecraftVersionDialogAdapter.ViewHolder> {
        private final ArrayList<ModpackMinecraftVersionGroup> groups;
        private final ModpackMinecraftVersionClickListener listener;

        ModpackMinecraftVersionDialogAdapter(ArrayList<
                        ModpackMinecraftVersionGroup> arrayList, ModpackMinecraftVersionClickListener modpackMinecraftVersionClickListener) {
            this.groups = arrayList;
            this.listener = modpackMinecraftVersionClickListener;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            LinearLayout linearLayout = new LinearLayout(viewGroup.getContext());
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            linearLayout.setGravity(Gravity.CENTER_VERTICAL);

            int p16 = ContentBrowserActivity.this.dp(16);
            int p14 = ContentBrowserActivity.this.dp(14);
            linearLayout.setPadding(p16, p14, p16, p14);
            linearLayout.setMinimumHeight(ContentBrowserActivity.this.dp(78));

            GradientDrawable gradientDrawable = new GradientDrawable();
            gradientDrawable.setColor(ContentBrowserActivity.this.resolveThemeColor(android.R.attr.colorBackground, 0));
            gradientDrawable.setCornerRadius(ContentBrowserActivity.this.dp(16));
            gradientDrawable.setStroke(ContentBrowserActivity.this.dp(1), ContentBrowserActivity.this.resolveThemeColor(android.R.attr.textColorSecondary, -7829368));
            linearLayout.setBackground(gradientDrawable);

            TextView textView = new TextView(viewGroup.getContext());
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17.0f);
            textView.setTextColor(ContentBrowserActivity.this.resolveThemeColor(android.R.attr.textColorPrimary, -1));
            textView.setTypeface(null, Typeface.BOLD);
            textView.setSingleLine(true);
            textView.setEllipsize(TextUtils.TruncateAt.END);
            linearLayout.addView(textView, new LinearLayout.LayoutParams(-1, -2));

            TextView textView2 = new TextView(viewGroup.getContext());
            textView2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.0f);
            textView2.setTextColor(ContentBrowserActivity.this.resolveThemeColor(android.R.attr.textColorSecondary, -3355444));
            textView2.setSingleLine(false);
            textView2.setMaxLines(2);

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
            layoutParams.topMargin = ContentBrowserActivity.this.dp(4);
            linearLayout.addView(textView2, layoutParams);

            RecyclerView.LayoutParams layoutParams2 = new RecyclerView.LayoutParams(-1, -2);
            layoutParams2.setMargins(0, 0, 0, ContentBrowserActivity.this.dp(8));
            linearLayout.setLayoutParams(layoutParams2);

            return new ViewHolder(linearLayout, textView, textView2);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, int i) {
            final ModpackMinecraftVersionGroup group = this.groups.get(i);
            viewHolder.title.setText(ContentBrowserActivity.this.formatMinecraftVersionTitle(group.minecraftVersion));
            viewHolder.subtitle.setText(ContentBrowserActivity.this.buildMinecraftVersionGroupSubtitle(group));

            viewHolder.itemView.setOnClickListener(v -> listener.onMinecraftVersionClicked(group));
        }

        @Override
        public int getItemCount() {
            return this.groups != null ? this.groups.size() : 0;
        }

        final class ViewHolder extends RecyclerView.ViewHolder {
            final TextView subtitle;
            final TextView title;

            ViewHolder(View view, TextView textView, TextView textView2) {
                super(view);
                this.title = textView;
                this.subtitle = textView2;
            }
        }
    }

    private static final class ModpackVersionDialogRow {
        static final int TYPE_HEADER = 0;
        static final int TYPE_VERSION = 1;
        final int headerCount;
        final String headerTitle;
        final int type;
        final ModpackInstallManager.ModpackVersionChoice version;

        private ModpackVersionDialogRow(int i, String str, int i2, ModpackInstallManager.ModpackVersionChoice modpackVersionChoice) {
            this.type = i;
            this.headerTitle = str;
            this.headerCount = i2;
            this.version = modpackVersionChoice;
        }

        static ModpackVersionDialogRow header(String str, int i) {
            return new ModpackVersionDialogRow(0, str, i, null);
        }

        static ModpackVersionDialogRow version(ModpackInstallManager.ModpackVersionChoice modpackVersionChoice) {
            return new ModpackVersionDialogRow(1, "", 0, modpackVersionChoice);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    final class ModpackVersionDialogAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final ModpackVersionClickListener listener;
        private final ArrayList<ModpackVersionDialogRow> rows;

        ModpackVersionDialogAdapter(ArrayList<
                        ModpackVersionDialogRow> arrayList, ModpackVersionClickListener modpackVersionClickListener) {
            this.rows = arrayList;
            this.listener = modpackVersionClickListener;
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public int getItemViewType(int i) {
            return this.rows.get(i).type;
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            if (i == 0) {
                TextView textView = new TextView(viewGroup.getContext());
                textView.setGravity(16);
                textView.setTextSize(2, 13.0f);
                textView.setTextColor(ContentBrowserActivity.this.resolveThemeColor(android.R.attr.textColorSecondary, -3355444));
                textView.setTypeface(textView.getTypeface(), 1);
                textView.setPadding(ContentBrowserActivity.this.dp(2), ContentBrowserActivity.this.dp(14), ContentBrowserActivity.this.dp(2), ContentBrowserActivity.this.dp(6));
                return new HeaderViewHolder(textView);
            }
            LinearLayout linearLayout = new LinearLayout(viewGroup.getContext());
            linearLayout.setOrientation(1);
            linearLayout.setGravity(16);
            linearLayout.setPadding(ContentBrowserActivity.this.dp(16), ContentBrowserActivity.this.dp(12), ContentBrowserActivity.this.dp(16), ContentBrowserActivity.this.dp(12));
            linearLayout.setMinimumHeight(ContentBrowserActivity.this.dp(92));
            GradientDrawable gradientDrawable = new GradientDrawable();
            gradientDrawable.setColor(ContentBrowserActivity.this.resolveThemeColor(android.R.attr.colorBackground, 0));
            gradientDrawable.setCornerRadius(ContentBrowserActivity.this.dp(16));
            gradientDrawable.setStroke(ContentBrowserActivity.this.dp(1), ContentBrowserActivity.this.resolveThemeColor(android.R.attr.textColorSecondary, -7829368));
            linearLayout.setBackground(gradientDrawable);
            TextView textView2 = new TextView(viewGroup.getContext());
            textView2.setTextSize(2, 16.0f);
            textView2.setTextColor(ContentBrowserActivity.this.resolveThemeColor(android.R.attr.textColorPrimary, -1));
            textView2.setTypeface(textView2.getTypeface(), 1);
            textView2.setSingleLine(false);
            textView2.setMaxLines(2);
            linearLayout.addView(textView2, new LinearLayout.LayoutParams(-1, -2));
            TextView textView3 = new TextView(viewGroup.getContext());
            textView3.setTextSize(2, 13.0f);
            textView3.setTextColor(ContentBrowserActivity.this.resolveThemeColor(android.R.attr.textColorSecondary, -3355444));
            textView3.setSingleLine(false);
            textView3.setMaxLines(2);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
            layoutParams.topMargin = ContentBrowserActivity.this.dp(4);
            linearLayout.addView(textView3, layoutParams);
            TextView textView4 = new TextView(viewGroup.getContext());
            textView4.setTextSize(2, 12.0f);
            textView4.setTextColor(ContentBrowserActivity.this.resolveThemeColor(android.R.attr.textColorSecondary, -7829368));
            textView4.setSingleLine(true);
            textView4.setEllipsize(TextUtils.TruncateAt.MIDDLE);
            LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-1, -2);
            layoutParams2.topMargin = ContentBrowserActivity.this.dp(4);
            linearLayout.addView(textView4, layoutParams2);
            TextView textView5 = new TextView(viewGroup.getContext());
            textView5.setTextSize(2, 12.0f);
            textView5.setTextColor(ContentBrowserActivity.this.resolveThemeColor(android.R.attr.textColorSecondary, -3355444));
            textView5.setSingleLine(false);
            LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(-1, -2);
            layoutParams3.topMargin = ContentBrowserActivity.this.dp(6);
            linearLayout.addView(textView5, layoutParams3);
            RecyclerView.LayoutParams layoutParams4 = new RecyclerView.LayoutParams(-1, -2);
            layoutParams4.setMargins(0, 0, 0, ContentBrowserActivity.this.dp(8));
            linearLayout.setLayoutParams(layoutParams4);
            return new VersionViewHolder(linearLayout, textView2, textView3, textView4, textView5);
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
            ModpackVersionDialogRow modpackVersionDialogRow = this.rows.get(i);
            if (viewHolder instanceof HeaderViewHolder) {
                ((HeaderViewHolder) viewHolder).header.setText(modpackVersionDialogRow.headerTitle + "  ·  " + modpackVersionDialogRow.headerCount + " " + (modpackVersionDialogRow.headerCount == 1 ? "version" : "versions"));
                return;
            }
            VersionViewHolder versionViewHolder = (VersionViewHolder) viewHolder;
            final ModpackInstallManager.ModpackVersionChoice modpackVersionChoice = modpackVersionDialogRow.version;
            if (modpackVersionChoice == null) {
                return;
            }
            versionViewHolder.title.setText(modpackVersionChoice.getDisplayTitle());
            versionViewHolder.meta.setText(ContentBrowserActivity.this.buildVersionMetaLine(modpackVersionChoice));
            versionViewHolder.file.setText(ContentBrowserActivity.this.buildVersionFileLine(modpackVersionChoice));
            boolean z = (ContentBrowserActivity.this.isGlobalBrowserMode() || modpackVersionChoice.isCompatibleWith(ContentBrowserActivity.this.gameVersionId, ContentBrowserActivity.this.loader)) ? false : true;
            versionViewHolder.warning.setVisibility(z ? 0 : 8);
            if (z) {
                versionViewHolder.warning.setText("Does not match the current instance filter. It will still install using this pack version's Minecraft version and loader.");
            } else {
                versionViewHolder.warning.setText("");
            }
            versionViewHolder.itemView.setOnClickListener(new View.OnClickListener() { // from
                                                                                       // class:
                                                                                       // ca.dnamobile.javalauncher.ContentBrowserActivity$ModpackVersionDialogAdapter$$ExternalSyntheticLambda0
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    ContentBrowserActivity.this.lambda$onBindViewHolder$0(modpackVersionChoice, view);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onBindViewHolder$0(ModpackInstallManager.ModpackVersionChoice modpackVersionChoice, View view) {
            this.listener.onVersionClicked(modpackVersionChoice);
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public int getItemCount() {
            return this.rows.size();
        }

        final class HeaderViewHolder extends RecyclerView.ViewHolder {
            final TextView header;

            HeaderViewHolder(TextView textView) {
                super(textView);
                this.header = textView;
            }
        }

        final class VersionViewHolder extends RecyclerView.ViewHolder {
            final TextView file;
            final TextView meta;
            final TextView title;
            final TextView warning;

            VersionViewHolder(View view, TextView textView, TextView textView2, TextView textView3, TextView textView4) {
                super(view);
                this.title = textView;
                this.meta = textView2;
                this.file = textView3;
                this.warning = textView4;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    final class ContentProjectAdapter extends RecyclerView.Adapter<ViewHolder> {
        private ModManagerContentType boundType;
        private final ArrayList<ModrinthProject> items;

        private ContentProjectAdapter() {
            this.items = new ArrayList<>();
            this.boundType = ModManagerContentType.MODS;
        }

        void submit(List<ModrinthProject> list) {
            this.boundType = ContentBrowserActivity.this.selectedType;
            this.items.clear();
            this.items.addAll(list);
            notifyDataSetChanged();
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View viewInflate = ContentBrowserActivity.this.getLayoutInflater().inflate(R.layout.item_content_project, viewGroup, false);
            viewInflate.setFocusable(false);
            viewInflate.setFocusableInTouchMode(false);
            return new ViewHolder(viewInflate);
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public void onBindViewHolder(ViewHolder viewHolder, int i) {
            String string;
            final ModrinthProject modrinthProject = this.items.get(i);
            ContentBrowserActivity.this.bindProjectIcon(viewHolder.icon, modrinthProject, ContentBrowserActivity.this.resolveProjectDisplayType(modrinthProject, this.boundType));
            viewHolder.name.setText(modrinthProject.title);
            viewHolder.author.setText(ContentBrowserActivity.this.getString(R.string.content_browser_project_author, new Object
                    []{(modrinthProject.author == null || modrinthProject.author.trim().isEmpty()) ? modrinthProject.source.getDisplayName() : modrinthProject.author}));
            viewHolder.description.setText(modrinthProject.description);
            viewHolder.tags.setText(ContentBrowserActivity.this.formatTags(modrinthProject.categories));
            viewHolder.downloads.setText(ContentBrowserActivity.this.formatNumber(modrinthProject.downloads));
            viewHolder.likes.setText(ContentBrowserActivity.this.formatNumber(modrinthProject.followers));
            TextView textView = viewHolder.updated;
            if (modrinthProject.dateModified == null || modrinthProject.dateModified.trim().isEmpty()) {
                string = ContentBrowserActivity.this.getString(R.string.content_browser_updated_unknown);
            } else {
                string = modrinthProject.dateModified.substring(0, Math.min(10, modrinthProject.dateModified.length()));
            }
            textView.setText(string);
            boolean zIsProjectInstalled = ContentBrowserActivity.this.isProjectInstalled(modrinthProject);
            ModManagerSource installedSource = zIsProjectInstalled ? ContentBrowserActivity.this.getInstalledSource(modrinthProject) : ModManagerSource.UNKNOWN;
            viewHolder.sourceIcon.setVisibility(installedSource.hasIcon() ? 0 : 8);
            if (installedSource.hasIcon()) {
                viewHolder.sourceIcon.setImageResource(installedSource.getIconRes());
                viewHolder.sourceIcon.setContentDescription(ContentBrowserActivity.this.getString(R.string.modmanager_installed_from, new Object
                        []{installedSource.getDisplayName()}));
            }
            viewHolder.install.setEnabled(!zIsProjectInstalled);
            viewHolder.install.setText(zIsProjectInstalled ? R.string.content_browser_installed : R.string.content_browser_install);
            if (zIsProjectInstalled) {
                viewHolder.install.setIcon(null);
            } else {
                viewHolder.install.setIconResource(R.drawable.ic_add_24);
            }
            viewHolder.install.setOnClickListener(zIsProjectInstalled ? null : new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.ContentBrowserActivity$ContentProjectAdapter$$ExternalSyntheticLambda0
                        @Override // android.view.View.OnClickListener
                        public final void onClick(View view) {
                            ContentBrowserActivity.this.lambda$onBindViewHolder$0(modrinthProject, view);
                        }
                    });
            viewHolder.menu.setOnClickListener(new View.OnClickListener() { // from class:
                                                                            // ca.dnamobile.javalauncher.ContentBrowserActivity$ContentProjectAdapter$$ExternalSyntheticLambda1
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    ContentBrowserActivity.this.lambda$onBindViewHolder$1(modrinthProject, view);
                }
            });
            viewHolder.itemView.setOnClickListener(new View.OnClickListener() { // from class:
                                                                                // ca.dnamobile.javalauncher.ContentBrowserActivity$ContentProjectAdapter$$ExternalSyntheticLambda2
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    ContentBrowserActivity.this.lambda$onBindViewHolder$2(modrinthProject, view);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onBindViewHolder$0(ModrinthProject modrinthProject, View view) {
            ContentBrowserActivity.this.confirmInstall(modrinthProject);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onBindViewHolder$1(ModrinthProject modrinthProject, View view) {
            ContentBrowserActivity.this.showProjectMenu(view, modrinthProject);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onBindViewHolder$2(ModrinthProject modrinthProject, View view) {
            ContentBrowserActivity.this.openProjectDetails(modrinthProject);
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public int getItemCount() {
            return this.items.size();
        }

        final class ViewHolder extends RecyclerView.ViewHolder {
            final TextView author;
            final TextView description;
            final TextView downloads;
            final ImageView icon;
            final MaterialButton install;
            final TextView likes;
            final MaterialButton menu;
            final TextView name;
            final ImageView sourceIcon;
            final TextView tags;
            final TextView updated;

            ViewHolder(View view) {
                super(view);
                this.icon = (ImageView) view.findViewById(R.id.imageProjectIcon);
                this.name = (TextView) view.findViewById(R.id.textProjectName);
                this.author = (TextView) view.findViewById(R.id.textProjectAuthor);
                this.description = (TextView) view.findViewById(R.id.textProjectDescription);
                this.tags = (TextView) view.findViewById(R.id.textProjectTags);
                this.downloads = (TextView) view.findViewById(R.id.textProjectDownloads);
                this.likes = (TextView) view.findViewById(R.id.textProjectLikes);
                this.updated = (TextView) view.findViewById(R.id.textProjectUpdated);
                this.sourceIcon = (ImageView) view.findViewById(R.id.imageProjectInstalledSource);
                MaterialButton materialButton = (MaterialButton) view.findViewById(R.id.buttonInstallProject);
                this.install = materialButton;
                MaterialButton materialButton2 = (MaterialButton) view.findViewById(R.id.buttonProjectMenu);
                this.menu = materialButton2;
                materialButton.setFocusable(false);
                materialButton.setFocusableInTouchMode(false);
                materialButton2.setFocusable(false);
                materialButton2.setFocusableInTouchMode(false);
            }
        }
    }
}
