package com.rbricks.appsharing.architecture.MVVM.withDataBinding;

import android.databinding.BindingAdapter;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

/**
 * Created by gopi on 19/10/17.
 */

public class BindConvertorUtil {

    public static String modifyString(String input) {
        return input + " converted";
    }

    @BindingAdapter("android:src" )
    public static void setImageUrl(ImageView view, String url) {  // For ImageView object & attribute src & param is url.
        Glide.with(view.getContext()).load(url).into(view);
    }
}
