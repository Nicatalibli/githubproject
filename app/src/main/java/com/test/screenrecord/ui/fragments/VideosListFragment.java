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
import android.widget.ImageView;
import android.widget.TextView;

import com.test.screenrecord.R;
import com.test.screenrecord.adapter.VideoRecyclerAdapter;
import com.test.screenrecord.adapter.decoration.SpacesItemDecoration;
import com.test.screenrecord.common.Cache;
import com.test.screenrecord.common.Const;
import com.test.screenrecord.common.Utils;
import com.test.screenrecord.interfaces.PermissionResultListener;
import com.test.screenrecord.listener.ObserverInterface;
import com.test.screenrecord.listener.ObserverUtils;
import com.test.screenrecord.model.Video;
import com.test.screenrecord.model.listener.EvbRecordTime;
import com.test.screenrecord.model.listener.EvbStageRecord;
import com.test.screenrecord.model.listener.EvbStopService;
import com.test.screenrecord.services.FloatingControlService;
import com.test.screenrecord.services.RecorderService;
import com.test.screenrecord.ui.activities.HomeActivity;

import java.io.File;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class VideosListFragment extends BaseFragment implements PermissionResultListener, SwipeRefreshLayout.OnRefreshListener, ObserverInterface {
    private RecyclerView videoRV;
    private TextView message;
    private SharedPreferences prefs;
    private SwipeRefreshLayout swipeRefreshLayout;
    private VideoRecyclerAdapter mAdapter;
    private ArrayList<Video> videosList = new ArrayList<>();
    private View btnFloatButton, loRecord;
    private TextView tvTimeRecord;
    private ImageView imRecord;

    public static VideosListFragment newInstance() {
        VideosListFragment fragment = new VideosListFragment();
        return fragment;
    }

    private static boolean isVideoFile(String path) {
        String mimeType = URLConnection.guessContentTypeFromName(path);
        return mimeType != null && mimeType.startsWith("video");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_videos, container, false);
        ObserverUtils.getInstance().registerObserver(this);
        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Const.UPDATE_UI);
            getActivity().registerReceiver(mReceiverUpdate, filter);
            message = view.findViewById(R.id.message_tv);
            videoRV = view.findViewById(R.id.videos_rv);
            btnFloatButton = view.findViewById(R.id.btn_floatbutton);
            loRecord = view.findViewById(R.id.lo_record);
            tvTimeRecord = view.findViewById(R.id.tv_time_record);
            imRecord = view.findViewById(R.id.im_record);
            videoRV.addItemDecoration(new SpacesItemDecoration(Utils.convertDpToPixel(10, getActivity())));

            swipeRefreshLayout = view.findViewById(R.id.swipeRefresh);
            swipeRefreshLayout.setOnRefreshListener(this);
            swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary,
                    android.R.color.holo_green_dark,
                    android.R.color.holo_orange_dark,
                    android.R.color.holo_blue_dark);

            prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        } catch (Exception e) {
        }
        initControl();
        return view;
    }

    private void initControl() {
        btnFloatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (FloatingControlService.isCountdown)
                    return;
                showFloatbtnRecord(RecorderService.isRecording);
                ObserverUtils.getInstance().notifyObservers(new EvbStageRecord(!FloatingControlService.isRecording));
            }
        });
    }

    public void showFloatbtnRecord(boolean isRecord) {
        if (!isRecord) {
            loRecord.setVisibility(View.INVISIBLE);
            imRecord.setVisibility(View.VISIBLE);
        } else {
            loRecord.setVisibility(View.VISIBLE);
            imRecord.setVisibility(View.INVISIBLE);
        }
    }

    public ArrayList<Video> getVideosList() {
        return videosList;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onVisibleFragment() {
        super.onVisibleFragment();
        setRecyclerView(Cache.getInstance().getArrVideos());
        if (getActivity() != null) {
            videosList.clear();
            checkPermission();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (mAdapter != null) {
            Cache.getInstance().setArrVideos(mAdapter.getVideos());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        getActivity().unregisterReceiver(mReceiverUpdate);
        super.onDestroy();
    }

    @Override
    public void onStop() {
        super.onStop();
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
                videosList.clear();
                checkPermission();
                Log.d(Const.TAG, "Refreshing");
                return false;
            }
        });
    }

    private void checkPermission() {
        try {
            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                if (getActivity() instanceof HomeActivity) {
                    ((HomeActivity) getActivity()).setPermissionResultListener(this);
                    ((HomeActivity) getActivity()).requestPermissionStorage();
                    if (getActivity() instanceof HomeActivity) {
//                    ((HomeActivity) getActivity()).startService();
                    }
                }
            } else {
                if (getActivity() instanceof HomeActivity) {
                    ((HomeActivity) getActivity()).startService();
                    btnFloatButton.setVisibility(View.VISIBLE);
                }
                if (videosList.isEmpty()) {
                    File directory = new File(prefs.getString(getString(R.string.savelocation_key),
                            Environment.getExternalStorageDirectory()
                                    + File.separator + Const.APPDIR));
                    if (!directory.exists()) {
                        Utils.createDir();
                        Log.d(Const.TAG, "Directory missing! Creating dir");
                    }
                    ArrayList<File> filesList = new ArrayList<File>();
                    if (directory.isDirectory() && directory.exists()) {
                        filesList.addAll(Arrays.asList(getVideos(directory.listFiles())));
                    }

                    new GetVideosAsync().execute(filesList.toArray(new File[filesList.size()]));
                }
            }
        } catch (Exception e) {
        }
    }

    private File[] getVideos(File[] files) {
        try {
            List<File> newFiles = new ArrayList<>();
            for (File file : files) {
                if (!file.isDirectory() && isVideoFile(file.getPath()))
                    newFiles.add(file);
            }
            return newFiles.toArray(new File[newFiles.size()]);
        } catch (Exception e) {
        }
        return null;
    }

    private void setRecyclerView(ArrayList<Video> videos) {
        try {
            if (!videos.isEmpty() && message.getVisibility() != View.GONE) {
                message.setVisibility(View.GONE);
            }
            videoRV.setHasFixedSize(true);
            final GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), 2);
            videoRV.setLayoutManager(layoutManager);
            mAdapter = new VideoRecyclerAdapter(getActivity(), videos, this);
            videoRV.setAdapter(mAdapter);
            layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    try {
                        return mAdapter.isSection(position) ? layoutManager.getSpanCount() : 1;
                    } catch (Exception e) {
                    }
                    return 1;
                }
            });
        } catch (Exception e) {
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAdapter != null) {
            Cache.getInstance().setArrVideos(mAdapter.getVideos());
        }
    }

    @Override
    public void onPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        try {
            switch (requestCode) {
                case Const.EXTDIR_REQUEST_CODE:
                    if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                        Log.d(Const.TAG, "Storage permission granted.");
                        checkPermission();
                    } else {
                        Log.d(Const.TAG, "Storage permission denied.");
                        videoRV.setVisibility(View.GONE);
                        message.setText(R.string.video_list_permission_denied_message);
                    }
                    break;
            }
        } catch (Exception e) {
        }
    }

    public void removeVideosList() {
        videosList.clear();
        Log.d(Const.TAG, "Reached video fragment");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        removeVideosList();
        checkPermission();
    }

    BroadcastReceiver mReceiverUpdate = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Const.UPDATE_UI.equals(intent.getAction())) {
                // update fragment here
                onRefresh();
            }

        }
    };

    @Override
    public void onRefresh() {
        videosList.clear();
        checkPermission();
    }

    public void setEnableSwipe(boolean isEnable) {
        swipeRefreshLayout.setEnabled(isEnable);
    }

    @Override
    public void notifyAction(Object action) {
        if (action instanceof EvbRecordTime) {
            showFloatbtnRecord(true);
            String time = ((EvbRecordTime) action).time;
            tvTimeRecord.setText(time);
        } else if (action instanceof EvbStageRecord) {
            tvTimeRecord.setText("00:00");
            showFloatbtnRecord(((EvbStageRecord) action).isStart);
        }else if (action instanceof EvbStopService){
            btnFloatButton.setVisibility(View.GONE);
        }
    }

    class GetVideosAsync extends AsyncTask<File[], Integer, ArrayList<Video>> {
        File[] files;
        ContentResolver resolver;

        GetVideosAsync() {
            resolver = getActivity().getApplicationContext().getContentResolver();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            swipeRefreshLayout.setRefreshing(true);
        }

        @Override
        protected void onPostExecute(ArrayList<Video> videos) {
            try {
                if (videos.isEmpty()) {
                    videoRV.setVisibility(View.GONE);
                    message.setVisibility(View.VISIBLE);
                } else {
                    Collections.sort(videos, Collections.<Video>reverseOrder());
                    setRecyclerView(addSections(videos));
                    videoRV.setVisibility(View.VISIBLE);
                    message.setVisibility(View.GONE);
                }
                swipeRefreshLayout.setRefreshing(false);
            } catch (Exception e) {
            }
        }

        private ArrayList<Video> addSections(ArrayList<Video> videos) {
            try {
                ArrayList<Video> videosWithSections = new ArrayList<>();
                Date currentSection = new Date();
                Log.d(Const.TAG, "Original Length: " + videos.size());
                for (int i = 0; i < videos.size(); i++) {
                    Video video = videos.get(i);
                    if (i == 0) {
                        videosWithSections.add(new Video(true, video.getLastModified()));
                        videosWithSections.add(video);
                        currentSection = video.getLastModified();
                        continue;
                    }
                    if (addNewSection(currentSection, video.getLastModified())) {
                        videosWithSections.add(new Video(true, video.getLastModified()));
                        currentSection = video.getLastModified();
                    }
                    videosWithSections.add(video);
                }
                Log.d(Const.TAG, "Length with sections: " + videosWithSections.size());
                return videosWithSections;
            } catch (Exception e) {
            }
            return new ArrayList<>();
        }

        private boolean addNewSection(Date current, Date next) {
            Calendar currentSectionDate = toCalendar(current.getTime());
            Calendar nextVideoDate = toCalendar(next.getTime());

            long milis1 = currentSectionDate.getTimeInMillis();
            long milis2 = nextVideoDate.getTimeInMillis();

            int dayDiff = (int) Math.abs((milis2 - milis1) / (24 * 60 * 60 * 1000));
            Log.d(Const.TAG, "Date diff is: " + (dayDiff));
            return dayDiff > 0;
        }

        private Calendar toCalendar(long timestamp) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(timestamp);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            return calendar;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            Log.d(Const.TAG, "Progress is :" + values[0]);
        }

        @Override
        protected ArrayList<Video> doInBackground(File[]... arg) {
            try {
                files = arg[0];
                for (int i = 0; i < files.length; i++) {
                    File file = files[i];
                    if (!file.isDirectory() && isVideoFile(file.getPath()) && !file.getName().endsWith("_.mp4")) {
                        videosList.add(new Video(file.getName(),
                                file,
                                getBitmap(file),
                                new Date(file.lastModified())));
                        //Update progress dialog
                        publishProgress(i);
                    }
                }
                return videosList;
            } catch (Exception e) {
            }
            return new ArrayList<>();
        }

        Bitmap getBitmap(File file) {
            try {
                String[] projection = {MediaStore.Images.Media._ID, MediaStore.Images.Media.BUCKET_ID,
                        MediaStore.Images.Media.BUCKET_DISPLAY_NAME, MediaStore.Images.Media.DATA};
                Cursor cursor = resolver.query(MediaStore.Video.Media.getContentUri("external"),
                        projection,
                        MediaStore.Images.Media.DATA + "=? ",
                        new String[]{file.getPath()}, null);

                if (cursor != null && cursor.moveToNext()) {
                    int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                    int id = cursor.getInt(idColumn);
                    Bitmap thumbNail = MediaStore.Video.Thumbnails.getThumbnail(resolver, id,
                            MediaStore.Video.Thumbnails.MINI_KIND, null);
                    cursor.close();
                    return thumbNail;
                }
            } catch (Exception e) {
            }
            return null;
        }
    }
}
