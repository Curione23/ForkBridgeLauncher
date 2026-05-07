package ca.dnamobile.javalauncher.ui.version;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import ca.dnamobile.javalauncher.MainActivity$$ExternalSyntheticBackport0;
import ca.dnamobile.javalauncher.R;
import ca.dnamobile.javalauncher.data.model.MinecraftVersion;
import ca.dnamobile.javalauncher.databinding.ItemVersionBinding;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class MinecraftVersionAdapter extends RecyclerView.Adapter<VersionViewHolder> {
    private final Context context;
    private final LayoutInflater inflater;
    private final Listener listener;
    private String selectedVersionId;
    private final ArrayList<MinecraftVersion> versions = new ArrayList<>();
    private final Set<String> installedVersionIds = new HashSet();

    public interface Listener {
        void onVersionSelected(MinecraftVersion minecraftVersion);
    }

    public MinecraftVersionAdapter(Context context, Listener listener) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    public void submitList(List<MinecraftVersion> list) {
        this.versions.clear();
        this.versions.addAll(list);
        notifyDataSetChanged();
    }

    public void setInstalledVersionIds(Set<String> set) {
        this.installedVersionIds.clear();
        this.installedVersionIds.addAll(set);
        notifyDataSetChanged();
    }

    public void setSelectedVersionId(String str) {
        this.selectedVersionId = str;
        notifyDataSetChanged();
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public VersionViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new VersionViewHolder(ItemVersionBinding.inflate(this.inflater, viewGroup, false));
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public void onBindViewHolder(VersionViewHolder versionViewHolder, int i) {
        final MinecraftVersion minecraftVersion = this.versions.get(i);
        boolean zEquals = minecraftVersion.getId().equals(this.selectedVersionId);
        boolean zContains = this.installedVersionIds.contains(minecraftVersion.getId());
        versionViewHolder.binding.textVersionName.setText(minecraftVersion.getId());
        versionViewHolder.binding.textVersionMeta.setText(this.context.getString(R.string.version_meta_value, toDisplayType(minecraftVersion.getType()), cleanDate(minecraftVersion.getReleaseTime())));
        versionViewHolder.binding.textVersionState.setText(zContains ? R.string.version_state_installed : R.string.version_state_not_installed);
        versionViewHolder.binding.versionCard.setChecked(zEquals);
        versionViewHolder.binding.getRoot().setOnClickListener(new View.OnClickListener() { // from class: ca.dnamobile.javalauncher.ui.version.MinecraftVersionAdapter$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.lambda$onBindViewHolder$0(minecraftVersion, view);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onBindViewHolder$0(MinecraftVersion minecraftVersion, View view) {
        this.listener.onVersionSelected(minecraftVersion);
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public int getItemCount() {
        return this.versions.size();
    }

    public List<MinecraftVersion> getCurrentItems() {
        return Collections.unmodifiableList(this.versions);
    }

    public static String toDisplayType(String str) {
        if (str == null) {
            return "Unknown";
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

    private static String cleanDate(String str) {
        if (str == null || MainActivity$$ExternalSyntheticBackport0.m(str)) {
            return "Unknown date";
        }
        int iIndexOf = str.indexOf(84);
        return iIndexOf > 0 ? str.substring(0, iIndexOf) : str;
    }

    static final class VersionViewHolder extends RecyclerView.ViewHolder {
        final ItemVersionBinding binding;

        VersionViewHolder(ItemVersionBinding itemVersionBinding) {
            super(itemVersionBinding.getRoot());
            this.binding = itemVersionBinding;
        }
    }
}
