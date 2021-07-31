package com.test.screenrecord.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.viewpager.widget.PagerAdapter;

import com.test.screenrecord.R;
import com.test.screenrecord.common.Utils;

import java.util.List;

public class FullScreenImageGalleryAdapter extends PagerAdapter {

    private final List<String> images;
    private FullScreenImageLoader fullScreenImageLoader;

    public interface FullScreenImageLoader {
        void loadFullScreenImage(ImageView iv, String imageUrl, int width, LinearLayout bglinearLayout);
    }

    public FullScreenImageGalleryAdapter(List<String> images) {
        this.images = images;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        LayoutInflater inflater = (LayoutInflater) container.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.fullscreen_image, null);

        ImageView imageView = view.findViewById(R.id.iv);
        final LinearLayout linearLayout = view.findViewById(R.id.ll);

        String image = images.get(position);
        Context context = imageView.getContext();
        int width = Utils.getScreenWidth(context);
        fullScreenImageLoader.loadFullScreenImage(imageView, image, width, linearLayout);

        container.addView(view, 0);
        return view;
    }

    @Override
    public int getCount() {
        return images.size();
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    public void setFullScreenImageLoader(FullScreenImageLoader loader) {
        this.fullScreenImageLoader = loader;
    }
}
