package ca.dnamobile.javalauncher.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import ca.dnamobile.javalauncher.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class ItemContentProjectBinding implements ViewBinding {
    public final MaterialButton buttonInstallProject;
    public final MaterialButton buttonProjectMenu;
    public final ShapeableImageView imageProjectIcon;
    public final ImageView imageProjectInstalledSource;
    private final MaterialCardView rootView;
    public final TextView textProjectAuthor;
    public final TextView textProjectDescription;
    public final TextView textProjectDownloads;
    public final TextView textProjectLikes;
    public final TextView textProjectName;
    public final TextView textProjectTags;
    public final TextView textProjectUpdated;

    private ItemContentProjectBinding(MaterialCardView materialCardView, MaterialButton materialButton, MaterialButton materialButton2, ShapeableImageView shapeableImageView, ImageView imageView, TextView textView, TextView textView2, TextView textView3, TextView textView4, TextView textView5, TextView textView6, TextView textView7) {
        this.rootView = materialCardView;
        this.buttonInstallProject = materialButton;
        this.buttonProjectMenu = materialButton2;
        this.imageProjectIcon = shapeableImageView;
        this.imageProjectInstalledSource = imageView;
        this.textProjectAuthor = textView;
        this.textProjectDescription = textView2;
        this.textProjectDownloads = textView3;
        this.textProjectLikes = textView4;
        this.textProjectName = textView5;
        this.textProjectTags = textView6;
        this.textProjectUpdated = textView7;
    }

    @Override // androidx.viewbinding.ViewBinding
    public MaterialCardView getRoot() {
        return this.rootView;
    }

    public static ItemContentProjectBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static ItemContentProjectBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View viewInflate = layoutInflater.inflate(R.layout.item_content_project, viewGroup, false);
        if (z) {
            viewGroup.addView(viewInflate);
        }
        return bind(viewInflate);
    }

    public static ItemContentProjectBinding bind(View view) {
        int i = R.id.buttonInstallProject;
        MaterialButton materialButton = (MaterialButton) ViewBindings.findChildViewById(view, i);
        if (materialButton != null) {
            i = R.id.buttonProjectMenu;
            MaterialButton materialButton2 = (MaterialButton) ViewBindings.findChildViewById(view, i);
            if (materialButton2 != null) {
                i = R.id.imageProjectIcon;
                ShapeableImageView shapeableImageView = (ShapeableImageView) ViewBindings.findChildViewById(view, i);
                if (shapeableImageView != null) {
                    i = R.id.imageProjectInstalledSource;
                    ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, i);
                    if (imageView != null) {
                        i = R.id.textProjectAuthor;
                        TextView textView = (TextView) ViewBindings.findChildViewById(view, i);
                        if (textView != null) {
                            i = R.id.textProjectDescription;
                            TextView textView2 = (TextView) ViewBindings.findChildViewById(view, i);
                            if (textView2 != null) {
                                i = R.id.textProjectDownloads;
                                TextView textView3 = (TextView) ViewBindings.findChildViewById(view, i);
                                if (textView3 != null) {
                                    i = R.id.textProjectLikes;
                                    TextView textView4 = (TextView) ViewBindings.findChildViewById(view, i);
                                    if (textView4 != null) {
                                        i = R.id.textProjectName;
                                        TextView textView5 = (TextView) ViewBindings.findChildViewById(view, i);
                                        if (textView5 != null) {
                                            i = R.id.textProjectTags;
                                            TextView textView6 = (TextView) ViewBindings.findChildViewById(view, i);
                                            if (textView6 != null) {
                                                i = R.id.textProjectUpdated;
                                                TextView textView7 = (TextView) ViewBindings.findChildViewById(view, i);
                                                if (textView7 != null) {
                                                    return new ItemContentProjectBinding((MaterialCardView) view, materialButton, materialButton2, shapeableImageView, imageView, textView, textView2, textView3, textView4, textView5, textView6, textView7);
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
