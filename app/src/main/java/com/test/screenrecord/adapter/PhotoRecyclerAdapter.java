package com.test.screenrecord.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.test.screenrecord.R;
import com.test.screenrecord.common.Const;
import com.test.screenrecord.common.Utils;
import com.test.screenrecord.model.Photo;
import com.test.screenrecord.ui.fragments.ScreenshotsListFragment;

import java.io.File;
import java.util.ArrayList;

public class PhotoRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_SECTION = 0;
    private static final int VIEW_ITEM = 1;
    private ScreenshotsListFragment photosListFragment;
    private ArrayList<Photo> photos;
    private Context context;
    private boolean isMultiSelect = false;
    private int count = 0;
    private ActionMode mActionMode;
    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            photosListFragment.setEnableSwipe(false);
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.video_list_action_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false if nothing is done
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            try {
                switch (item.getItemId()) {
                    case R.id.delete:
                        ArrayList<Photo> deleteFiles = new ArrayList<>();
                        for (Photo photo : photos) {
                            if (photo.isSelected()) {
                                deleteFiles.add(photo);
                            }
                        }
                        if (!deleteFiles.isEmpty())
                            confirmDelete(deleteFiles);
                        mActionMode.finish();
                        break;
                    case R.id.share:
                        ArrayList<Integer> positions = new ArrayList<>();
                        for (Photo photo : photos) {
                            if (photo.isSelected()) {
                                positions.add(photos.indexOf(photo));
                            }
                        }
                        if (!positions.isEmpty())
                            sharePhotos(positions);
                        mActionMode.finish();
                        break;
                    case R.id.select_all:
                        for (Photo photo : photos)
                            photo.setSelected(true);
                        mActionMode.setTitle("" + photos.size());
                        notifyDataSetChanged();
                        break;
                }
            } catch (Exception e) {
            }
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            try {
                // remove all selected photos and reload recyclerview
                for (Photo photo : photos) {
                    photo.setSelected(false);
                }
                isMultiSelect = false;
                photosListFragment.setEnableSwipe(true);
                notifyDataSetChanged();
            } catch (Exception e) {
            }
        }
    };

    public PhotoRecyclerAdapter(Context context, ArrayList<Photo> android, ScreenshotsListFragment photosListFragment) {
        this.photos = android;
        this.context = context;
        this.photosListFragment = photosListFragment;
    }

    public ArrayList<Photo> getPhotos() {
        return photos;
    }

    @Override
    public int getItemViewType(int position) {
        return isSection(position) ? VIEW_SECTION : VIEW_ITEM;
    }

    public boolean isSection(int position) {
        return photos.get(position).isSection();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view;
        switch (viewType) {
            case VIEW_SECTION:
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.content_video_section, viewGroup, false);
                return new PhotoRecyclerAdapter.SectionViewHolder(view);
            case VIEW_ITEM:
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.content_photo, viewGroup, false);
                return new PhotoRecyclerAdapter.ItemViewHolder(view);
            default:
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.content_photo, viewGroup, false);
                return new PhotoRecyclerAdapter.ItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        final Photo photo = photos.get(position);
        switch (holder.getItemViewType()) {
            case VIEW_ITEM:
                final PhotoRecyclerAdapter.ItemViewHolder itemViewHolder = (PhotoRecyclerAdapter.ItemViewHolder) holder;
                if (photos.get(position).getThumbnail() != null) {
                    itemViewHolder.iv_thumbnail.setImageBitmap(photo.getThumbnail());
                } else {
                    itemViewHolder.iv_thumbnail.setImageResource(0);
                }

                if (photo.isSelected()) {
                    itemViewHolder.selectableFrame.setForeground(new ColorDrawable(ContextCompat.getColor(context, R.color.multiSelectColor)));
                } else {
                    itemViewHolder.selectableFrame.setForeground(new ColorDrawable(ContextCompat.getColor(context, android.R.color.transparent)));
                }

                itemViewHolder.photoCard.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (isMultiSelect) {
                            if (photo.isSelected())
                                count--;
                            else
                                count++;

                            // Enable disable selection based on previous choice
                            photo.setSelected(!photo.isSelected());
                            notifyDataSetChanged();
                            mActionMode.setTitle("" + count);

                            // If the count is 0, disable muliselect
                            if (count == 0)
                                setMultiSelect(false);
                            return;
                        }

                        try {
                            File photoFile = photo.getFile();
                            Uri fileUri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", photoFile);
                            Log.d(Const.TAG, fileUri.toString());
                            Intent openPhotoIntent = new Intent();
                            openPhotoIntent.setAction(Intent.ACTION_VIEW)
                                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    .setDataAndType(fileUri, context.getContentResolver().getType(fileUri));
                            context.startActivity(openPhotoIntent);
                        } catch (Exception e) {
                        }
                    }
                });

                itemViewHolder.photoCard.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        if (!isMultiSelect) {
                            setMultiSelect(true);
                            photo.setSelected(true);
                            count++;
                            mActionMode.setTitle("" + count);
                            notifyDataSetChanged();
                        }
                        return true;
                    }
                });
                break;
            case VIEW_SECTION:
                PhotoRecyclerAdapter.SectionViewHolder sectionViewHolder = (PhotoRecyclerAdapter.SectionViewHolder) holder;
                sectionViewHolder.section.setText(Utils.generateSectionTitle(photo.getLastModified()));
                break;
        }
    }

    private void setMultiSelect(boolean isMultiSelect) {
        if (isMultiSelect) {
            this.isMultiSelect = true;
            count = 0;
            mActionMode = ((AppCompatActivity) photosListFragment.getActivity()).startSupportActionMode(mActionModeCallback);
        } else {
            this.isMultiSelect = false;
            mActionMode.finish();
        }
    }

    private void sharePhotos(ArrayList<Integer> positions) {
        try {
            ArrayList<Uri> photoList = new ArrayList<>();
            for (int position : positions) {
                photoList.add(FileProvider.getUriForFile(
                        context, context.getPackageName() +
                                ".provider",
                        photos.get(position).getFile()
                ));
            }
            Intent Shareintent = new Intent()
                    .setAction(Intent.ACTION_SEND_MULTIPLE)
                    .setType("photo/*")
                    .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .putParcelableArrayListExtra(Intent.EXTRA_STREAM, photoList);
            context.startActivity(Intent.createChooser(Shareintent,
                    context.getString(R.string.share_intent_notification_title)));
        } catch (Exception e) {
        }
    }

    private void deletePhotos(ArrayList<Photo> deletePhotos) {
        try {
            for (Photo photo : deletePhotos) {
                if (!photo.isSection() && photo.getFile().delete()) {
                    notifyItemRemoved(photos.indexOf(photo));
                    photos.remove(photo);
                }
            }
            notifyDataSetChanged();
        } catch (Exception e) {
        }
    }

    private void confirmDelete(final ArrayList<Photo> deletePhotos) {
        try {
            int count = deletePhotos.size();
            new AlertDialog.Builder(context)
                    .setTitle(context.getResources().getQuantityString(R.plurals.delete_photo_alert_title, count))
                    .setMessage(context.getResources().getQuantityString(R.plurals.delete_photo_alert_message,
                            count, count))
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            deletePhotos(deletePhotos);
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    })
                    .show();
        } catch (Exception e) {
        }
    }

    @Override
    public int getItemCount() {
        return photos.size();
    }

    private final class ItemViewHolder extends RecyclerView.ViewHolder {
        private ImageView iv_thumbnail;
        private RelativeLayout photoCard;
        private FrameLayout selectableFrame;

        ItemViewHolder(View view) {
            super(view);
            iv_thumbnail = view.findViewById(R.id.thumbnail);
            iv_thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
            photoCard = view.findViewById(R.id.videoCard);
            selectableFrame = view.findViewById(R.id.selectableFrame);
        }
    }

    private final class SectionViewHolder extends RecyclerView.ViewHolder {
        private TextView section;

        SectionViewHolder(View view) {
            super(view);
            section = view.findViewById(R.id.sectionID);
        }
    }
}
