package ca.dnamobile.javalauncher;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import ca.dnamobile.javalauncher.ContentProjectDetailsActivity;
import ca.dnamobile.javalauncher.instance.LauncherInstance;
import ca.dnamobile.javalauncher.modmanager.CurseForgeApiClient;
import ca.dnamobile.javalauncher.modmanager.CurseForgeInstallManager;
import ca.dnamobile.javalauncher.modmanager.ModManagerContentType;
import ca.dnamobile.javalauncher.modmanager.ModManagerSource;
import ca.dnamobile.javalauncher.modmanager.ModManagerVersionResolver;
import ca.dnamobile.javalauncher.modmanager.ModpackInstallManager;
import ca.dnamobile.javalauncher.modmanager.ModrinthApiClient;
import ca.dnamobile.javalauncher.modmanager.ModrinthInstallManager;
import ca.dnamobile.javalauncher.modmanager.ModrinthProject;
import ca.dnamobile.javalauncher.modmanager.ModrinthVersion;
import ca.dnamobile.javalauncher.modmanager.NetworkImageLoader;
import ca.dnamobile.javalauncher.utils.FullscreenUtils;
import ca.dnamobile.javalauncher.utils.path.PathManager;
import com.google.android.material.button.MaterialButton;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class ContentProjectDetailsActivity extends AppCompatActivity {
    private MaterialButton buttonBack;
    private MaterialButton buttonOpenWebsite;
    private ImageView imageProjectIcon;
    private ModrinthProject project;
    private RecyclerView recyclerVersions;
    private TextView textDescription;
    private TextView textGallery;
    private TextView textMeta;
    private TextView textStatus;
    private TextView textTitle;
    private final VersionAdapter adapter = new VersionAdapter();
    private String instanceId = "";
    private String instanceName = "";
    private String loader = "";
    private String baseVersionId = "";
    private String gameVersionId = "";
    private String gameDirectoryPath = "";
    private String projectId = "";
    private String projectSlug = "";
    private ModManagerContentType contentType = ModManagerContentType.MODS;
    private ModManagerSource source = ModManagerSource.MODRINTH;

    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        PathManager.initContextConstants(this);
        setContentView(R.layout.activity_content_project_details);
        FullscreenUtils.enableImmersive(this);
        readExtras();
        bindViews();
        setupViews();
        loadProject();
    }

    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    protected void onResume() {
        super.onResume();
        FullscreenUtils.enableImmersive(this);
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public void onWindowFocusChanged(boolean z) {
        super.onWindowFocusChanged(z);
        if (z) {
            FullscreenUtils.enableImmersive(this);
        }
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
        this.gameDirectoryPath = safeExtra(InstanceDetailsActivity.EXTRA_GAME_DIRECTORY, "");
        this.projectId = safeExtra(ContentBrowserActivity.EXTRA_PROJECT_ID, "");
        this.projectSlug = safeExtra(ContentBrowserActivity.EXTRA_PROJECT_SLUG, "");
        String strSafeExtra2 = safeExtra(InstanceDetailsActivity.EXTRA_CONTENT_CATEGORY, "");
        if (strSafeExtra2.isEmpty()) {
            strSafeExtra2 = safeExtra(ContentBrowserActivity.EXTRA_PROJECT_TYPE, "mods");
        }
        this.contentType = ModManagerContentType.fromValue(strSafeExtra2);
        this.source = ModManagerSource.fromId(safeExtra(ContentBrowserActivity.EXTRA_PROJECT_SOURCE, ModManagerSource.MODRINTH.getId()));
    }

    private String safeExtra(String str, String str2) {
        String stringExtra = getIntent().getStringExtra(str);
        return (stringExtra == null || stringExtra.trim().isEmpty()) ? str2 : stringExtra.trim();
    }

    private void bindViews() {
        this.imageProjectIcon = (ImageView) findViewById(R.id.imageProjectDetailsIcon);
        this.textTitle = (TextView) findViewById(R.id.textProjectDetailsTitle);
        this.textMeta = (TextView) findViewById(R.id.textProjectDetailsMeta);
        this.textDescription = (TextView) findViewById(R.id.textProjectDetailsDescription);
        this.textGallery = (TextView) findViewById(R.id.textProjectDetailsGallery);
        this.textStatus = (TextView) findViewById(R.id.textProjectDetailsStatus);
        this.recyclerVersions = (RecyclerView) findViewById(R.id.recyclerProjectVersions);
        this.buttonOpenWebsite = (MaterialButton) findViewById(R.id.buttonOpenProjectWebsite);
        this.buttonBack = (MaterialButton) findViewById(R.id.buttonProjectDetailsBack);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupViews$0(View view) {
        finish();
    }

    private void setupViews() {
        this.buttonBack.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.ContentProjectDetailsActivity$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ContentProjectDetailsActivity.this.lambda$setupViews$0(view);
            }
        });
        this.buttonOpenWebsite.setEnabled(false);
        this.recyclerVersions.setLayoutManager(new LinearLayoutManager(this));
        this.recyclerVersions.setNestedScrollingEnabled(false);
        this.recyclerVersions.setAdapter(this.adapter);
        this.textTitle.setText(safeExtra(ContentBrowserActivity.EXTRA_PROJECT_TITLE, getString(R.string.content_project_details_loading)));
        this.textMeta.setText(buildInitialMetaLine());
        this.textGallery.setText("");
        this.textStatus.setText(R.string.content_project_details_loading);
        NetworkImageLoader.load(this.imageProjectIcon, safeExtra(ContentBrowserActivity.EXTRA_PROJECT_ICON_URL, ""), getFallbackIcon());
    }

    private String buildInitialMetaLine() {
        if (this.contentType == ModManagerContentType.MODPACKS) {
            if (this.gameDirectoryPath.trim().isEmpty()) {
                return "Modpack · " + this.source.getDisplayName() + " · Installs to the selected launcher storage location";
            }
            return "Modpack · " + this.source.getDisplayName() + " · Current instance: " + this.instanceName;
        }
        return getString(R.string.content_project_details_instance_meta, new Object[]{this.instanceName, displayLoader(this.loader), this.gameVersionId.isEmpty() ? getString(R.string.content_browser_unknown_version) : this.gameVersionId});
    }

    private void loadProject() {
        if (this.projectId.trim().isEmpty() && this.projectSlug.trim().isEmpty()) {
            this.textStatus.setText(R.string.content_project_details_missing_project);
        } else {
            this.textStatus.setText(R.string.content_project_details_loading);
            new Thread(new Runnable() { // from class: ca.dnamobile.javalauncher.ContentProjectDetailsActivity$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    ContentProjectDetailsActivity.this.lambda$loadProject$3();
                }
            }, this.contentType == ModManagerContentType.MODPACKS ? "ModpackProjectDetails" : "ContentProjectDetails").start();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$loadProject$3() {
        ArrayList<VersionRow> arrayListLoadNormalVersionRows;
        final ModrinthProject project;
        final ArrayList<VersionRow> arrayListLoadNormalVersionRows2;
        try {
            if (this.source == ModManagerSource.CURSEFORGE) {
                CurseForgeApiClient curseForgeApiClient = new CurseForgeApiClient(this);
                project = curseForgeApiClient.getProject(this.projectId);
                if (this.contentType == ModManagerContentType.MODPACKS) {
                    arrayListLoadNormalVersionRows2 = loadModpackVersionRows(project);
                } else {
                    arrayListLoadNormalVersionRows2 = loadNormalVersionRows(curseForgeApiClient.getProjectVersions(this.projectId, this.contentType, this.gameVersionId, this.loader));
                }
            } else {
                ModrinthApiClient modrinthApiClient = new ModrinthApiClient();
                ModrinthProject projectWithFallback = modrinthApiClient.getProjectWithFallback(this.projectId, this.projectSlug);
                if (this.contentType == ModManagerContentType.MODPACKS) {
                    arrayListLoadNormalVersionRows = loadModpackVersionRows(projectWithFallback);
                } else {
                    arrayListLoadNormalVersionRows = loadNormalVersionRows(modrinthApiClient.getProjectVersionsWithFallback(projectWithFallback, this.contentType, this.gameVersionId, this.loader, true));
                }
                ArrayList<VersionRow> arrayList = arrayListLoadNormalVersionRows;
                project = projectWithFallback;
                arrayListLoadNormalVersionRows2 = arrayList;
            }
            runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.ContentProjectDetailsActivity$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    ContentProjectDetailsActivity.this.lambda$loadProject$1(project, arrayListLoadNormalVersionRows2);
                }
            });
        } catch (Throwable th) {
            runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.ContentProjectDetailsActivity$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    ContentProjectDetailsActivity.this.lambda$loadProject$2(th);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$loadProject$2(Throwable th) {
        this.textStatus.setText(getString(R.string.content_project_details_load_failed, new Object[]{th.getMessage() != null ? th.getMessage() : th.getClass().getSimpleName()}));
    }

    private ArrayList<VersionRow> loadNormalVersionRows(ArrayList<ModrinthVersion> arrayList) {
        ArrayList<VersionRow> arrayList2 = new ArrayList<>();
        for (ModrinthVersion modrinthVersion : arrayList) {
            if (modrinthVersion != null) {
                arrayList2.add(VersionRow.normal(modrinthVersion));
            }
        }
        sortRowsNewestFirst(arrayList2);
        return arrayList2;
    }

    private ArrayList<VersionRow> loadModpackVersionRows(ModrinthProject modrinthProject) throws Exception {
        ArrayList<ModpackInstallManager.ModpackVersionChoice> arrayListListProjectVersions = ModpackInstallManager.listProjectVersions(this, this.source, safeProjectId(modrinthProject), safeProjectSlug(modrinthProject));
        sortModpackVersionsNewestFirst(arrayListListProjectVersions);
        ArrayList<VersionRow> arrayList = new ArrayList<>();
        for (ModpackInstallManager.ModpackVersionChoice modpackVersionChoice : arrayListListProjectVersions) {
            if (modpackVersionChoice != null) {
                arrayList.add(VersionRow.modpack(modpackVersionChoice));
            }
        }
        return arrayList;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: renamed from: bindLoadedProject, reason: merged with bridge method [inline-methods] */
    public void lambda$loadProject$1(final ModrinthProject modrinthProject, ArrayList<VersionRow> arrayList) {
        this.project = modrinthProject;
        this.textTitle.setText(modrinthProject.title);
        this.textDescription.setText(buildDescriptionText(modrinthProject));
        this.textMeta.setText(buildProjectMetaText(modrinthProject));
        this.textGallery.setText(buildProjectDetailsLine(modrinthProject));
        this.textStatus.setText(buildVersionStatusText(arrayList.size()));
        NetworkImageLoader.load(this.imageProjectIcon, firstUsableImageUrl(modrinthProject), getFallbackIcon());
        this.buttonOpenWebsite.setEnabled(true);
        this.buttonOpenWebsite.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.ContentProjectDetailsActivity$$ExternalSyntheticLambda8
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ContentProjectDetailsActivity.this.lambda$bindLoadedProject$4(modrinthProject, view);
            }
        });
        this.adapter.submit(arrayList);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$bindLoadedProject$4(ModrinthProject modrinthProject, View view) {
        openWebsite(modrinthProject);
    }

    private String buildDescriptionText(ModrinthProject modrinthProject) {
        String strTrim = modrinthProject.body == null ? "" : modrinthProject.body.trim();
        if (!strTrim.isEmpty()) {
            return strTrim;
        }
        String strTrim2 = modrinthProject.description != null ? modrinthProject.description.trim() : "";
        return strTrim2.isEmpty() ? "No description was provided for this project." : strTrim2;
    }

    private String buildProjectMetaText(ModrinthProject modrinthProject) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(getContentTypeTitle(this.contentType));
        arrayList.add(resolveSource(modrinthProject).getDisplayName());
        String strTrim = modrinthProject.author == null ? "" : modrinthProject.author.trim();
        if (!strTrim.isEmpty()) {
            arrayList.add("By " + strTrim);
        }
        arrayList.add(formatNumber(modrinthProject.downloads) + " downloads");
        if (modrinthProject.followers > 0) {
            arrayList.add(formatNumber(modrinthProject.followers) + " followers");
        }
        String strTrimDate = trimDate(modrinthProject.dateModified);
        if (!strTrimDate.isEmpty()) {
            arrayList.add("Updated " + strTrimDate);
        }
        return joinParts(arrayList, " · ");
    }

    private String buildProjectDetailsLine(ModrinthProject modrinthProject) {
        ArrayList arrayList = new ArrayList();
        String tags = formatTags(modrinthProject.categories);
        if (!tags.isEmpty()) {
            arrayList.add("Categories: " + tags);
        }
        arrayList.add(getString(R.string.content_project_details_gallery_value, new Object[]{Integer.valueOf(modrinthProject.galleryUrls == null ? 0 : modrinthProject.galleryUrls.size())}));
        if (this.contentType == ModManagerContentType.MODPACKS) {
            arrayList.add("Install target: selected launcher storage location");
        } else if (!this.gameDirectoryPath.trim().isEmpty()) {
            arrayList.add("Install target: " + this.gameDirectoryPath);
        }
        return joinParts(arrayList, "\n");
    }

    private String buildVersionStatusText(int i) {
        if (this.contentType != ModManagerContentType.MODPACKS) {
            return getString(R.string.content_project_details_versions_value, new Object[]{Integer.valueOf(i)});
        }
        if (i == 1) {
            return "1 installable modpack version";
        }
        return i + " installable modpack versions";
    }

    private String firstUsableImageUrl(ModrinthProject modrinthProject) {
        String strNormalizeImageUrl = normalizeImageUrl(modrinthProject.iconUrl);
        if (strNormalizeImageUrl != null) {
            return strNormalizeImageUrl;
        }
        if (modrinthProject.galleryUrls == null) {
            return null;
        }
        Iterator<String> it = modrinthProject.galleryUrls.iterator();
        while (it.hasNext()) {
            String strNormalizeImageUrl2 = normalizeImageUrl(it.next());
            if (strNormalizeImageUrl2 != null) {
                return strNormalizeImageUrl2;
            }
        }
        return null;
    }

    private void openWebsite(ModrinthProject modrinthProject) {
        try {
            startActivity(new Intent("android.intent.action.VIEW", Uri.parse(modrinthProject.getWebsiteUrl())));
        } catch (ActivityNotFoundException unused) {
            Toast.makeText(this, modrinthProject.getWebsiteUrl(), 1).show();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void confirmInstallVersion(VersionRow versionRow) {
        String string;
        final ModrinthProject modrinthProject = this.project;
        if (modrinthProject == null) {
            return;
        }
        if (versionRow.modpackVersion != null) {
            confirmInstallModpackVersion(modrinthProject, versionRow.modpackVersion);
            return;
        }
        if (versionRow.normalVersion == null) {
            return;
        }
        final ModrinthVersion modrinthVersion = versionRow.normalVersion;
        if (this.gameVersionId.isEmpty()) {
            string = getString(R.string.content_browser_unknown_version);
        } else {
            string = this.gameVersionId;
        }
        new AlertDialog.Builder(this).setTitle(getString(R.string.content_project_details_install_version_title, new Object[]{modrinthProject.title, modrinthVersion.versionNumber})).setMessage(getString(R.string.content_project_details_install_version_message, new Object[]{string, displayLoader(this.loader)})).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.content_browser_install, new DialogInterface.OnClickListener() { // from class: ca.dnamobile.javalauncher.ContentProjectDetailsActivity$$ExternalSyntheticLambda1
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                ContentProjectDetailsActivity.this.lambda$confirmInstallVersion$5(modrinthProject, modrinthVersion, dialogInterface, i);
            }
        }).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$confirmInstallVersion$5(ModrinthProject modrinthProject, ModrinthVersion modrinthVersion, DialogInterface dialogInterface, int i) {
        installNormalVersion(modrinthProject, modrinthVersion);
    }

    private void confirmInstallModpackVersion(final ModrinthProject modrinthProject, final ModpackInstallManager.ModpackVersionChoice modpackVersionChoice) {
        StringBuilder sb = new StringBuilder("Install ");
        sb.append(modrinthProject.title).append(" as a new launcher instance?\n\nSelected version:\n");
        sb.append(modpackVersionChoice.getDisplayTitle());
        sb.append("\n").append(modpackVersionChoice.getDisplaySubtitle());
        sb.append("\n\nThis uses the launcher storage location currently selected in settings, including scoped storage if the user selected one.");
        if (!this.gameVersionId.trim().isEmpty() && !modpackVersionChoice.isCompatibleWith(this.gameVersionId, this.loader)) {
            sb.append("\n\nThis modpack version does not match the current instance filter. It will still install using the pack's own Minecraft version and loader metadata.");
        }
        new AlertDialog.Builder(this).setTitle("Install Modpack").setMessage(sb.toString()).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.content_browser_install, new DialogInterface.OnClickListener() { // from class: ca.dnamobile.javalauncher.ContentProjectDetailsActivity$$ExternalSyntheticLambda6
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                ContentProjectDetailsActivity.this.lambda$confirmInstallModpackVersion$6(modrinthProject, modpackVersionChoice, dialogInterface, i);
            }
        }).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$confirmInstallModpackVersion$6(ModrinthProject modrinthProject, ModpackInstallManager.ModpackVersionChoice modpackVersionChoice, DialogInterface dialogInterface, int i) {
        installModpackVersion(modrinthProject, modpackVersionChoice);
    }

    private void installNormalVersion(final ModrinthProject modrinthProject, final ModrinthVersion modrinthVersion) {
        if (this.gameDirectoryPath.trim().isEmpty()) {
            Toast.makeText(this, R.string.content_browser_missing_game_dir, 1).show();
            return;
        }
        final File file = new File(this.gameDirectoryPath);
        this.textStatus.setText(getString(R.string.content_browser_install_started, new Object[]{modrinthProject.title}));
        final AnonymousClass1 anonymousClass1 = new AnonymousClass1();
        new Thread(new Runnable() { // from class: ca.dnamobile.javalauncher.ContentProjectDetailsActivity$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                ContentProjectDetailsActivity.this.lambda$installNormalVersion$7(modrinthProject, file, modrinthVersion, anonymousClass1);
            }
        }, resolveSource(modrinthProject) == ModManagerSource.CURSEFORGE ? "CurseForgeInstallVersion" : "ModrinthInstallVersion").start();
    }

    /* JADX INFO: renamed from: ca.dnamobile.javalauncher.ContentProjectDetailsActivity$1, reason: invalid class name */
    class AnonymousClass1 implements ModrinthInstallManager.Listener {
        AnonymousClass1() {
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onStatus$0(String str) {
            ContentProjectDetailsActivity.this.textStatus.setText(str);
        }

        @Override // ca.dnamobile.javalauncher.modmanager.ModrinthInstallManager.Listener
        public void onStatus(final String str) {
            ContentProjectDetailsActivity.this.runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.ContentProjectDetailsActivity$1$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    ContentProjectDetailsActivity.this.lambda$onStatus$0(str);
                }
            });
        }

        @Override // ca.dnamobile.javalauncher.modmanager.ModrinthInstallManager.Listener
        public void onComplete(final String str) {
            ContentProjectDetailsActivity.this.runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.ContentProjectDetailsActivity$1$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    ContentProjectDetailsActivity.this.lambda$onComplete$1(str);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onComplete$1(String str) {
            ContentProjectDetailsActivity.this.textStatus.setText(str);
            Toast.makeText(ContentProjectDetailsActivity.this, str, 1).show();
            ContentProjectDetailsActivity.this.setResult(-1);
        }

        @Override // ca.dnamobile.javalauncher.modmanager.ModrinthInstallManager.Listener
        public void onError(final Throwable th) {
            ContentProjectDetailsActivity.this.runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.ContentProjectDetailsActivity$1$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    ContentProjectDetailsActivity.this.lambda$onError$2(th);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onError$2(Throwable th) {
            String message = th.getMessage() != null ? th.getMessage() : th.getClass().getSimpleName();
            ContentProjectDetailsActivity.this.textStatus.setText(ContentProjectDetailsActivity.this.getString(R.string.content_browser_install_failed, new Object[]{message}));
            ContentProjectDetailsActivity contentProjectDetailsActivity = ContentProjectDetailsActivity.this;
            Toast.makeText(contentProjectDetailsActivity, contentProjectDetailsActivity.getString(R.string.content_browser_install_failed, new Object[]{message}), 1).show();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$installNormalVersion$7(ModrinthProject modrinthProject, File file, ModrinthVersion modrinthVersion, ModrinthInstallManager.Listener listener) {
        if (resolveSource(modrinthProject) == ModManagerSource.CURSEFORGE) {
            CurseForgeInstallManager.installSpecificVersion(new CurseForgeApiClient(this), file, this.gameVersionId, this.loader, this.contentType, modrinthProject, modrinthVersion, listener);
        } else {
            ModrinthInstallManager.installSpecificVersion(file, this.gameVersionId, this.loader, this.contentType, modrinthProject, modrinthVersion, listener);
        }
    }

    private void installModpackVersion(final ModrinthProject modrinthProject, final ModpackInstallManager.ModpackVersionChoice modpackVersionChoice) {
        getWindow().addFlags(128);
        this.textStatus.setText(getString(R.string.content_browser_install_started, new Object[]{modrinthProject.title}));
        final AnonymousClass2 anonymousClass2 = new AnonymousClass2();
        new Thread(new Runnable() { // from class: ca.dnamobile.javalauncher.ContentProjectDetailsActivity$$ExternalSyntheticLambda10
            @Override // java.lang.Runnable
            public final void run() {
                ContentProjectDetailsActivity.this.lambda$installModpackVersion$8(modrinthProject, modpackVersionChoice, anonymousClass2);
            }
        }, resolveSource(modrinthProject) == ModManagerSource.CURSEFORGE ? "CurseForgeModpackInstallVersion" : "ModrinthModpackInstallVersion").start();
    }

    /* JADX INFO: renamed from: ca.dnamobile.javalauncher.ContentProjectDetailsActivity$2, reason: invalid class name */
    class AnonymousClass2 implements ModpackInstallManager.Listener {
        AnonymousClass2() {
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onStatus$0(String str) {
            ContentProjectDetailsActivity.this.textStatus.setText(str);
        }

        @Override // ca.dnamobile.javalauncher.modmanager.ModpackInstallManager.Listener
        public void onStatus(final String str) {
            ContentProjectDetailsActivity.this.runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.ContentProjectDetailsActivity$2$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    ContentProjectDetailsActivity.this.lambda$onStatus$0(str);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onProgress$1(int i, int i2) {
            ContentProjectDetailsActivity.this.textStatus.setText("Installing " + Math.max(0, i) + " / " + i2);
        }

        @Override // ca.dnamobile.javalauncher.modmanager.ModpackInstallManager.Listener
        public void onProgress(final int i, final int i2) {
            if (i2 > 0) {
                ContentProjectDetailsActivity.this.runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.ContentProjectDetailsActivity$2$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        ContentProjectDetailsActivity.this.lambda$onProgress$1(i, i2);
                    }
                });
            }
        }

        @Override // ca.dnamobile.javalauncher.modmanager.ModpackInstallManager.Listener
        public void onComplete(String str) {
            onComplete(str, null);
        }

        @Override // ca.dnamobile.javalauncher.modmanager.ModpackInstallManager.Listener
        public void onComplete(final String str, final LauncherInstance launcherInstance) {
            ContentProjectDetailsActivity.this.runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.ContentProjectDetailsActivity$2$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    ContentProjectDetailsActivity.this.lambda$onComplete$2(str, launcherInstance);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onComplete$2(String str, LauncherInstance launcherInstance) {
            ContentProjectDetailsActivity.this.getWindow().clearFlags(128);
            ContentProjectDetailsActivity.this.textStatus.setText(str);
            Toast.makeText(ContentProjectDetailsActivity.this, str, 1).show();
            ContentProjectDetailsActivity.this.setResult(-1);
            if (launcherInstance != null) {
                ContentProjectDetailsActivity.this.startActivity(InstanceDetailsActivity.createIntent(ContentProjectDetailsActivity.this, launcherInstance));
                ContentProjectDetailsActivity.this.finish();
            }
        }

        @Override // ca.dnamobile.javalauncher.modmanager.ModpackInstallManager.Listener
        public void onError(final Throwable th) {
            ContentProjectDetailsActivity.this.runOnUiThread(new Runnable() { // from class: ca.dnamobile.javalauncher.ContentProjectDetailsActivity$2$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    ContentProjectDetailsActivity.this.lambda$onError$3(th);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onError$3(Throwable th) {
            ContentProjectDetailsActivity.this.getWindow().clearFlags(128);
            String message = th.getMessage() != null ? th.getMessage() : th.getClass().getSimpleName();
            ContentProjectDetailsActivity.this.textStatus.setText(ContentProjectDetailsActivity.this.getString(R.string.content_browser_install_failed, new Object[]{message}));
            ContentProjectDetailsActivity contentProjectDetailsActivity = ContentProjectDetailsActivity.this;
            Toast.makeText(contentProjectDetailsActivity, contentProjectDetailsActivity.getString(R.string.content_browser_install_failed, new Object[]{message}), 1).show();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$installModpackVersion$8(ModrinthProject modrinthProject, ModpackInstallManager.ModpackVersionChoice modpackVersionChoice, ModpackInstallManager.Listener listener) {
        ModpackInstallManager.installFromProjectVersion(this, resolveSource(modrinthProject), safeProjectId(modrinthProject), safeProjectSlug(modrinthProject), modrinthProject.title, modrinthProject.iconUrl, modpackVersionChoice, listener);
    }

    private String safeProjectId(ModrinthProject modrinthProject) {
        String strTrim = modrinthProject.projectId == null ? "" : modrinthProject.projectId.trim();
        return strTrim.isEmpty() ? this.projectId : strTrim;
    }

    private String safeProjectSlug(ModrinthProject modrinthProject) {
        String strTrim = modrinthProject.slug == null ? "" : modrinthProject.slug.trim();
        return strTrim.isEmpty() ? this.projectSlug : strTrim;
    }

    private ModManagerSource resolveSource(ModrinthProject modrinthProject) {
        return (modrinthProject.source == null || modrinthProject.source == ModManagerSource.UNKNOWN) ? this.source : modrinthProject.source;
    }

    /* JADX INFO: renamed from: ca.dnamobile.javalauncher.ContentProjectDetailsActivity$3, reason: invalid class name */
    static /* synthetic */ class AnonymousClass3 {
        static final /* synthetic */ int[] $SwitchMap$ca$dnamobile$javalauncher$modmanager$ModManagerContentType;

        static {
            int[] iArr = new int[ModManagerContentType.values().length];
            $SwitchMap$ca$dnamobile$javalauncher$modmanager$ModManagerContentType = iArr;
            try {
                iArr[ModManagerContentType.MODPACKS.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
            try {
                $SwitchMap$ca$dnamobile$javalauncher$modmanager$ModManagerContentType[ModManagerContentType.RESOURCEPACKS.ordinal()] = 2;
            } catch (NoSuchFieldError unused2) {
            }
            try {
                $SwitchMap$ca$dnamobile$javalauncher$modmanager$ModManagerContentType[ModManagerContentType.SHADERPACKS.ordinal()] = 3;
            } catch (NoSuchFieldError unused3) {
            }
            try {
                $SwitchMap$ca$dnamobile$javalauncher$modmanager$ModManagerContentType[ModManagerContentType.MODS.ordinal()] = 4;
            } catch (NoSuchFieldError unused4) {
            }
        }
    }

    private int getFallbackIcon() {
        int i = AnonymousClass3.$SwitchMap$ca$dnamobile$javalauncher$modmanager$ModManagerContentType[this.contentType.ordinal()];
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

    private String displayLoader(String str) {
        if (str == null || str.trim().isEmpty()) {
            return "Vanilla";
        }
        String strTrim = str.trim();
        return strTrim.substring(0, 1).toUpperCase(Locale.US) + strTrim.substring(1);
    }

    private String getContentTypeTitle(ModManagerContentType modManagerContentType) {
        int i = AnonymousClass3.$SwitchMap$ca$dnamobile$javalauncher$modmanager$ModManagerContentType[modManagerContentType.ordinal()];
        if (i == 1) {
            return "Modpack";
        }
        if (i == 2) {
            return "Resource Pack";
        }
        if (i == 3) {
            return "Shader Pack";
        }
        return "Mod";
    }

    private String formatNumber(long j) {
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

    private String formatTags(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "Unknown";
        }
        StringBuilder sb = new StringBuilder();
        int iMin = Math.min(4, list.size());
        for (int i = 0; i < iMin; i++) {
            String str = list.get(i);
            if (str != null && !str.trim().isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(formatTag(str));
            }
        }
        if (list.size() > iMin) {
            sb.append(" +").append(list.size() - iMin);
        }
        return sb.length() == 0 ? "Unknown" : sb.toString();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String formatTag(String str) {
        String strTrim = str.replace('-', ' ').replace('_', ' ').trim();
        return strTrim.isEmpty() ? str : strTrim.substring(0, 1).toUpperCase(Locale.US) + strTrim.substring(1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String joinParts(List<String> list, String str) {
        StringBuilder sb = new StringBuilder();
        for (String str2 : list) {
            if (str2 != null && !str2.trim().isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(str);
                }
                sb.append(str2.trim());
            }
        }
        return sb.toString();
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

    /* JADX INFO: Access modifiers changed from: private */
    public String trimDate(String str) {
        if (str == null) {
            return "";
        }
        String strTrim = str.trim();
        return strTrim.substring(0, Math.min(10, strTrim.length()));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ int lambda$sortRowsNewestFirst$9(VersionRow versionRow, VersionRow versionRow2) {
        return compareNullableIsoDatesDescending(versionRow.datePublished(), versionRow2.datePublished());
    }

    private void sortRowsNewestFirst(ArrayList<VersionRow> arrayList) {
        Collections.sort(arrayList, new Comparator() { // from class: ca.dnamobile.javalauncher.ContentProjectDetailsActivity$$ExternalSyntheticLambda9
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                return ContentProjectDetailsActivity.this.lambda$sortRowsNewestFirst$9((ContentProjectDetailsActivity.VersionRow) obj, (ContentProjectDetailsActivity.VersionRow) obj2);
            }
        });
    }

    private void sortModpackVersionsNewestFirst(ArrayList<ModpackInstallManager.ModpackVersionChoice> arrayList) {
        Collections.sort(arrayList, new Comparator() { // from class: ca.dnamobile.javalauncher.ContentProjectDetailsActivity$$ExternalSyntheticLambda5
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                return ContentProjectDetailsActivity.this.lambda$sortModpackVersionsNewestFirst$10((ModpackInstallManager.ModpackVersionChoice) obj, (ModpackInstallManager.ModpackVersionChoice) obj2);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ int lambda$sortModpackVersionsNewestFirst$10(ModpackInstallManager.ModpackVersionChoice modpackVersionChoice, ModpackInstallManager.ModpackVersionChoice modpackVersionChoice2) {
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

    /* JADX INFO: Access modifiers changed from: private */
    public String reflectedString(Object obj, String str) {
        try {
            Object obj2 = obj.getClass().getField(str).get(obj);
            if (obj2 == null) {
                return "";
            }
            return String.valueOf(obj2).trim();
        } catch (Throwable unused) {
            return "";
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public ArrayList<String> reflectedStringList(Object obj, String str) {
        ArrayList<String> arrayList = new ArrayList<>();
        try {
            Object obj2 = obj.getClass().getField(str).get(obj);
            if (obj2 instanceof Iterable) {
                for (Object obj3 : (Iterable) obj2) {
                    if (obj3 != null) {
                        String strTrim = String.valueOf(obj3).trim();
                        if (!strTrim.isEmpty()) {
                            arrayList.add(strTrim);
                        }
                    }
                }
            }
        } catch (Throwable unused) {
        }
        return arrayList;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String joinShortList(ArrayList<String> arrayList, int i) {
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
    static final class VersionRow {
        final ModpackInstallManager.ModpackVersionChoice modpackVersion;
        final ModrinthVersion normalVersion;

        private VersionRow(ModrinthVersion modrinthVersion, ModpackInstallManager.ModpackVersionChoice modpackVersionChoice) {
            this.normalVersion = modrinthVersion;
            this.modpackVersion = modpackVersionChoice;
        }

        static VersionRow normal(ModrinthVersion modrinthVersion) {
            return new VersionRow(modrinthVersion, null);
        }

        static VersionRow modpack(ModpackInstallManager.ModpackVersionChoice modpackVersionChoice) {
            return new VersionRow(null, modpackVersionChoice);
        }

        String datePublished() {
            ModpackInstallManager.ModpackVersionChoice modpackVersionChoice = this.modpackVersion;
            if (modpackVersionChoice != null) {
                return modpackVersionChoice.datePublished;
            }
            ModrinthVersion modrinthVersion = this.normalVersion;
            return (modrinthVersion == null || modrinthVersion.datePublished == null) ? "" : this.normalVersion.datePublished;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    final class VersionAdapter extends RecyclerView.Adapter<ViewHolder> {
        private final ArrayList<VersionRow> versions;

        private VersionAdapter() {
            this.versions = new ArrayList<>();
        }

        void submit(List<VersionRow> list) {
            this.versions.clear();
            this.versions.addAll(list);
            notifyDataSetChanged();
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            return new ViewHolder(ContentProjectDetailsActivity.this.getLayoutInflater().inflate(R.layout.item_content_version, viewGroup, false));
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public void onBindViewHolder(ViewHolder viewHolder, int i) {
            final VersionRow versionRow = this.versions.get(i);
            if (versionRow.modpackVersion != null) {
                bindModpackVersion(viewHolder, versionRow.modpackVersion);
            } else if (versionRow.normalVersion != null) {
                bindNormalVersion(viewHolder, versionRow.normalVersion);
            }
            viewHolder.install.setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.ContentProjectDetailsActivity$VersionAdapter$$ExternalSyntheticLambda0
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    ContentProjectDetailsActivity.this.lambda$onBindViewHolder$0(versionRow, view);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onBindViewHolder$0(VersionRow versionRow, View view) {
            ContentProjectDetailsActivity.this.confirmInstallVersion(versionRow);
        }

        private void bindNormalVersion(ViewHolder viewHolder, ModrinthVersion modrinthVersion) {
            String str;
            if (modrinthVersion.name == null || modrinthVersion.name.trim().isEmpty()) {
                str = modrinthVersion.versionNumber;
            } else {
                str = modrinthVersion.name;
            }
            viewHolder.name.setText(str);
            ArrayList arrayList = new ArrayList();
            if (modrinthVersion.versionNumber != null && !modrinthVersion.versionNumber.trim().isEmpty()) {
                arrayList.add(modrinthVersion.versionNumber.trim());
            }
            if (modrinthVersion.versionType != null && !modrinthVersion.versionType.trim().isEmpty()) {
                arrayList.add(ContentProjectDetailsActivity.this.formatTag(modrinthVersion.versionType));
            }
            ArrayList arrayListReflectedStringList = ContentProjectDetailsActivity.this.reflectedStringList(modrinthVersion, "gameVersions");
            if (!arrayListReflectedStringList.isEmpty()) {
                arrayList.add("Minecraft " + ContentProjectDetailsActivity.this.joinShortList(arrayListReflectedStringList, 3));
            }
            ArrayList arrayListReflectedStringList2 = ContentProjectDetailsActivity.this.reflectedStringList(modrinthVersion, "loaders");
            if (!arrayListReflectedStringList2.isEmpty()) {
                arrayList.add("Loader " + ContentProjectDetailsActivity.this.joinShortList(arrayListReflectedStringList2, 2));
            }
            String strTrimDate = ContentProjectDetailsActivity.this.trimDate(modrinthVersion.datePublished);
            if (!strTrimDate.isEmpty()) {
                arrayList.add(strTrimDate);
            }
            String strReflectedString = ContentProjectDetailsActivity.this.reflectedString(modrinthVersion, "fileName");
            if (!strReflectedString.isEmpty()) {
                arrayList.add(strReflectedString);
            }
            viewHolder.meta.setText(ContentProjectDetailsActivity.this.joinParts(arrayList, " · "));
        }

        private void bindModpackVersion(ViewHolder viewHolder, ModpackInstallManager.ModpackVersionChoice modpackVersionChoice) {
            viewHolder.name.setText(modpackVersionChoice.getDisplayTitle());
            ArrayList arrayList = new ArrayList();
            String displaySubtitle = modpackVersionChoice.getDisplaySubtitle();
            if (displaySubtitle != null && !displaySubtitle.trim().isEmpty()) {
                arrayList.add(displaySubtitle.trim());
            }
            String strTrim = modpackVersionChoice.fileName == null ? "" : modpackVersionChoice.fileName.trim();
            if (!strTrim.isEmpty()) {
                arrayList.add(strTrim);
            }
            if (!ContentProjectDetailsActivity.this.gameVersionId.trim().isEmpty() && !modpackVersionChoice.isCompatibleWith(ContentProjectDetailsActivity.this.gameVersionId, ContentProjectDetailsActivity.this.loader)) {
                arrayList.add("Not filtered out: installs using this pack version's own Minecraft version and loader");
            }
            viewHolder.meta.setText(ContentProjectDetailsActivity.this.joinParts(arrayList, "\n"));
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public int getItemCount() {
            return this.versions.size();
        }

        final class ViewHolder extends RecyclerView.ViewHolder {
            final MaterialButton install;
            final TextView meta;
            final TextView name;

            ViewHolder(View view) {
                super(view);
                this.name = (TextView) view.findViewById(R.id.textVersionName);
                this.meta = (TextView) view.findViewById(R.id.textVersionMeta);
                MaterialButton materialButton = (MaterialButton) view.findViewById(R.id.buttonInstallVersion);
                this.install = materialButton;
                materialButton.setFocusable(false);
                materialButton.setFocusableInTouchMode(false);
            }
        }
    }
}
