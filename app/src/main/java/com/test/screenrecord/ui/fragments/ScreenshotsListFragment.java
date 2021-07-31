package com.test.screenrecord.ui.fragments;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.test.screenrecord.R;
import com.test.screenrecord.adapter.PhotoRecyclerAdapter;
import com.test.screenrecord.adapter.decoration.SpacesItemDecoration;
import com.test.screenrecord.common.Cache;
import com.test.screenrecord.common.Const;
import com.test.screenrecord.common.Utils;
import com.test.screenrecord.interfaces.PermissionResultListener;
import com.test.screenrecord.model.Photo;
import com.test.screenrecord.ui.activities.HomeActivity;

import java.io.File;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class ScreenshotsListFragment extends BaseFragment implements PermissionResultListener, SwipeRefreshLayout.OnRefreshListener {
    private RecyclerView photoRV;
    private TextView message;
    private SharedPreferences prefs;
    private SwipeRefreshLayout swipeRefreshLayout;
    private PhotoRecyclerAdapter mAdapter;
    private ArrayList<Photo> photosList = new ArrayList<>();

    public static ScreenshotsListFragment newInstance() {
        ScreenshotsListFragment fragment = new ScreenshotsListFragment();
        return fragment;
    }

    private static boolean isPhotoFile(String path) {
        String mimeType = URLConnection.guessContentTypeFromName(path);
        return mimeType != null && mimeType.startsWith("image");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_photos, container, false);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Const.UPDATE_UI_IMAGE);
        getActivity().registerReceiver(mReceiverUpdate, filter);
        initViews();
        initEvents();
        return mRootView;
    }
    BroadcastReceiver mReceiverUpdate = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Const.UPDATE_UI_IMAGE.equals(intent.getAction())) {
                // update fragment here
                onRefresh();
            }

        }
    };
    public void setEnableSwipe(boolean isEnable) {
        swipeRefreshLayout.setEnabled(isEnable);
    }

    @Override
    public void onVisibleFragment() {
        super.onVisibleFragment();
        setRecyclerView(Cache.getInstance().getArrPhotos());
        if (getActivity() != null) {
            photosList.clear();
            checkPermission();
        }
    }

    private void initViews() {
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        message = mRootView.findViewById(R.id.message_tv);
        photoRV = mRootView.findViewById(R.id.videos_rv);
        photoRV.addItemDecoration(new SpacesItemDecoration((int) Utils.convertDpToPixel(10, getActivity())));
        swipeRefreshLayout = mRootView.findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary,
                android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark,
                android.R.color.holo_blue_dark);
    }

    private void initEvents() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MenuItem refresh = menu.add("Refresh");
        refresh.setIcon(R.drawable.ic_refresh_white_24dp);
        refresh.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        refresh.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                if (swipeRefreshLayout.isRefreshing())
                    return false;
                photosList.clear();
                checkPermission();
                return false;
            }
        });
    }

    /**
     * Check if we have permission to read the external storage and load the videos into ArrayList<Video>
     */
    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (getActivity() instanceof HomeActivity) {
                ((HomeActivity) getActivity()).setPermissionResultListener(this);
                ((HomeActivity) getActivity()).requestPermissionStorage();
            }
        } else {
            //We have required permission now and lets populate the video from the selected
            // directory if the arraylist holding videos is empty
            if (photosList.isEmpty()) {
                File directory = new File(prefs.getString(getString(R.string.savelocation_key),
                        Environment.getExternalStorageDirectory()
                                + File.separator + Const.APPDIR));
                //Remove directory pointers and other files from the list
                if (!directory.exists()) {
                    Utils.createDir();
                    Log.d(Const.TAG, "Directory missing! Creating dir");
                }
                ArrayList<File> filesList = new ArrayList<File>();
                if (directory.isDirectory() && directory.exists()) {
                    filesList.addAll(Arrays.asList(getPhotos(directory.listFiles())));
                }
                //Read the videos and extract details from it in async.
                // This is essential if the directory contains huge number of videos

                new GetPhotosAsync().execute(filesList.toArray(new File[filesList.size()]));
            }
        }
    }

    /**
     * Filter all video files from array of files
     *
     * @param files File[] containing files from a directory
     * @return File[] containing only video files
     */
    private File[] getPhotos(File[] files) {
        List<File> newFiles = new ArrayList<>();
        for (File file : files) {
            if (!file.isDirectory() && isPhotoFile(file.getPath()))
                newFiles.add(file);
        }
        return newFiles.toArray(new File[newFiles.size()]);
    }

    private void setRecyclerView(ArrayList<Photo> photos) {
        if (!photos.isEmpty() && message.getVisibility() != View.GONE) {
            message.setVisibility(View.GONE);
        }
        photoRV.setHasFixedSize(true);
        final GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), 2);
        photoRV.setLayoutManager(layoutManager);
        mAdapter = new PhotoRecyclerAdapter(getActivity(), photos, this);
        photoRV.setAdapter(mAdapter);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return mAdapter.isSection(position) ? layoutManager.getSpanCount() : 1;
            }
        });
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (mAdapter != null) {
            Cache.getInstance().setArrPhotos(mAdapter.getPhotos());
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAdapter != null) {
            Cache.getInstance().setArrPhotos(mAdapter.getPhotos());
        }
    }

    //Permission result callback method
    @Override
    public void onPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case Const.EXTDIR_REQUEST_CODE:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Log.d(Const.TAG, "Storage permission granted.");
                    //Performing storage task immediately after granting permission sometimes causes
                    //permission not taking effect.
                    checkPermission();
                } else {
                    Log.d(Const.TAG, "Storage permission denied.");
                    photoRV.setVisibility(View.GONE);
                    message.setText(R.string.video_list_permission_denied_message);
                }
                break;
        }
    }

    public void removePhotosList() {
        photosList.clear();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        removePhotosList();
        checkPermission();
    }

    @Override
    public void onRefresh() {
        photosList.clear();
        checkPermission();
    }

    class GetPhotosAsync extends AsyncTask<File[], Integer, ArrayList<Photo>> {
        File[] files;
        ContentResolver resolver;

        GetPhotosAsync() {
            resolver = getActivity().getApplicationContext().getContentResolver();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            swipeRefreshLayout.setRefreshing(true);
        }

        @Override
        protected void onPostExecute(ArrayList<Photo> photos) {
            //If the directory has no videos, remove recyclerview from rootview and show empty message.
            // Else set recyclerview and remove message textview
            if (photos.isEmpty()) {
                photoRV.setVisibility(View.GONE);
                message.setVisibility(View.VISIBLE);
            } else {
                //Sort the videos in a descending order
                Collections.sort(photos, Collections.<Photo>reverseOrder());
                setRecyclerView(addSections(photos));
                photoRV.setVisibility(View.VISIBLE);
                message.setVisibility(View.GONE);
            }
            //Finish refreshing
            swipeRefreshLayout.setRefreshing(false);
        }

        private ArrayList<Photo> addSections(ArrayList<Photo> photos) {
            ArrayList<Photo> photosWithSections = new ArrayList<>();
            Date currentSection = new Date();
            for (int i = 0; i < photos.size(); i++) {
                Photo photo = photos.get(i);
                //Add the first section arbitrarily
                if (i == 0) {
                    photosWithSections.add(new Photo(true, photo.getLastModified()));
                    photosWithSections.add(photo);
                    currentSection = photo.getLastModified();
                    continue;
                }
                if (addNewSection(currentSection, photo.getLastModified())) {
                    photosWithSections.add(new Photo(true, photo.getLastModified()));
                    currentSection = photo.getLastModified();
                }
                photosWithSections.add(photo);
            }
            return photosWithSections;
        }

        private boolean addNewSection(Date current, Date next) {
            Calendar currentSectionDate = Utils.toCalendar(current.getTime());
            Calendar nextVideoDate = Utils.toCalendar(next.getTime());

            long milis1 = currentSectionDate.getTimeInMillis();
            long milis2 = nextVideoDate.getTimeInMillis();

            int dayDiff = (int) Math.abs((milis2 - milis1) / (24 * 60 * 60 * 1000));
            return dayDiff > 0;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            Log.d(Const.TAG, "Progress is :" + values[0]);
        }

        @Override
        protected ArrayList<Photo> doInBackground(File[]... arg) {
            //Get video file name, Uri and video thumbnail from mediastore
            files = arg[0];
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                if (!file.isDirectory() && isPhotoFile(file.getPath())) {
                    photosList.add(new Photo(file.getName(),
                            file,
                            getBitmap(file),
                            new Date(file.lastModified())));
                    publishProgress(i);
                }
            }
            return photosList;
        }

        Bitmap getBitmap(File file) {
            String[] projection = {MediaStore.Images.Media._ID, MediaStore.Images.Media.BUCKET_ID,
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME, MediaStore.Images.Media.DATA};
            Cursor cursor = resolver.query(MediaStore.Images.Media.getContentUri("external"),
                    projection,
                    MediaStore.Images.Media.DATA + "=? ",
                    new String[]{file.getPath()}, null);

            if (cursor != null && cursor.moveToNext()) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                int id = cursor.getInt(idColumn);
                Bitmap thumbNail = MediaStore.Images.Thumbnails.getThumbnail(resolver, id,
                        MediaStore.Images.Thumbnails.MINI_KIND, null);
                cursor.close();
                return thumbNail;
            }
            return null;
        }
    }

    @Override
    public void onDestroy() {
        getActivity().unregisterReceiver(mReceiverUpdate);
        super.onDestroy();
    }
}
