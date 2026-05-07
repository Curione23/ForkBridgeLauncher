package ca.dnamobile.javalauncher.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import ca.dnamobile.javalauncher.R;
import com.google.android.material.card.MaterialCardView;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class ItemVersionBinding implements ViewBinding {
    private final MaterialCardView rootView;
    public final TextView textVersionMeta;
    public final TextView textVersionName;
    public final TextView textVersionState;
    public final MaterialCardView versionCard;

    private ItemVersionBinding(MaterialCardView materialCardView, TextView textView, TextView textView2, TextView textView3, MaterialCardView materialCardView2) {
        this.rootView = materialCardView;
        this.textVersionMeta = textView;
        this.textVersionName = textView2;
        this.textVersionState = textView3;
        this.versionCard = materialCardView2;
    }

    @Override // androidx.viewbinding.ViewBinding
    public MaterialCardView getRoot() {
        return this.rootView;
    }

    public static ItemVersionBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static ItemVersionBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View viewInflate = layoutInflater.inflate(R.layout.item_version, viewGroup, false);
        if (z) {
            viewGroup.addView(viewInflate);
        }
        return bind(viewInflate);
    }

    public static ItemVersionBinding bind(View view) {
        int i = R.id.textVersionMeta;
        TextView textView = (TextView) ViewBindings.findChildViewById(view, i);
        if (textView != null) {
            i = R.id.textVersionName;
            TextView textView2 = (TextView) ViewBindings.findChildViewById(view, i);
            if (textView2 != null) {
                i = R.id.textVersionState;
                TextView textView3 = (TextView) ViewBindings.findChildViewById(view, i);
                if (textView3 != null) {
                    MaterialCardView materialCardView = (MaterialCardView) view;
                    return new ItemVersionBinding(materialCardView, textView, textView2, textView3, materialCardView);
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}
