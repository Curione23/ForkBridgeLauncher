package ca.dnamobile.javalauncher.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import ca.dnamobile.javalauncher.R;
import com.google.android.material.button.MaterialButton;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class ActivityContentProjectDetailsBinding implements ViewBinding {
    public final MaterialButton buttonOpenProjectWebsite;
    public final MaterialButton buttonProjectDetailsBack;
    public final ImageView imageProjectDetailsIcon;
    public final RecyclerView recyclerProjectVersions;
    private final NestedScrollView rootView;
    public final TextView textProjectDetailsDescription;
    public final TextView textProjectDetailsGallery;
    public final TextView textProjectDetailsMeta;
    public final TextView textProjectDetailsStatus;
    public final TextView textProjectDetailsTitle;

    private ActivityContentProjectDetailsBinding(NestedScrollView nestedScrollView, MaterialButton materialButton, MaterialButton materialButton2, ImageView imageView, RecyclerView recyclerView, TextView textView, TextView textView2, TextView textView3, TextView textView4, TextView textView5) {
        this.rootView = nestedScrollView;
        this.buttonOpenProjectWebsite = materialButton;
        this.buttonProjectDetailsBack = materialButton2;
        this.imageProjectDetailsIcon = imageView;
        this.recyclerProjectVersions = recyclerView;
        this.textProjectDetailsDescription = textView;
        this.textProjectDetailsGallery = textView2;
        this.textProjectDetailsMeta = textView3;
        this.textProjectDetailsStatus = textView4;
        this.textProjectDetailsTitle = textView5;
    }

    @Override // androidx.viewbinding.ViewBinding
    public NestedScrollView getRoot() {
        return this.rootView;
    }

    public static ActivityContentProjectDetailsBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static ActivityContentProjectDetailsBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View viewInflate = layoutInflater.inflate(R.layout.activity_content_project_details, viewGroup, false);
        if (z) {
            viewGroup.addView(viewInflate);
        }
        return bind(viewInflate);
    }

    public static ActivityContentProjectDetailsBinding bind(View view) {
        int i = R.id.buttonOpenProjectWebsite;
        MaterialButton materialButton = (MaterialButton) ViewBindings.findChildViewById(view, i);
        if (materialButton != null) {
            i = R.id.buttonProjectDetailsBack;
            MaterialButton materialButton2 = (MaterialButton) ViewBindings.findChildViewById(view, i);
            if (materialButton2 != null) {
                i = R.id.imageProjectDetailsIcon;
                ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, i);
                if (imageView != null) {
                    i = R.id.recyclerProjectVersions;
                    RecyclerView recyclerView = (RecyclerView) ViewBindings.findChildViewById(view, i);
                    if (recyclerView != null) {
                        i = R.id.textProjectDetailsDescription;
                        TextView textView = (TextView) ViewBindings.findChildViewById(view, i);
                        if (textView != null) {
                            i = R.id.textProjectDetailsGallery;
                            TextView textView2 = (TextView) ViewBindings.findChildViewById(view, i);
                            if (textView2 != null) {
                                i = R.id.textProjectDetailsMeta;
                                TextView textView3 = (TextView) ViewBindings.findChildViewById(view, i);
                                if (textView3 != null) {
                                    i = R.id.textProjectDetailsStatus;
                                    TextView textView4 = (TextView) ViewBindings.findChildViewById(view, i);
                                    if (textView4 != null) {
                                        i = R.id.textProjectDetailsTitle;
                                        TextView textView5 = (TextView) ViewBindings.findChildViewById(view, i);
                                        if (textView5 != null) {
                                            return new ActivityContentProjectDetailsBinding((NestedScrollView) view, materialButton, materialButton2, imageView, recyclerView, textView, textView2, textView3, textView4, textView5);
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
