package com.test.screenrecord.adapter.decoration;

import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

public class SpacesItemDecoration extends RecyclerView.ItemDecoration {
    private int space;

    public SpacesItemDecoration(int space) {
        this.space = space;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        outRect.bottom = 0;
        outRect.top = space;

        if (parent.getChildLayoutPosition(view) % 2 == 0) {
            outRect.right = space;
            outRect.left = space / 2;
        } else {
            outRect.right = space / 2;
            outRect.left = space;
        }
    }
}
