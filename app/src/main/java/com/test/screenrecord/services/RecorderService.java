package com.test.screenrecord.services;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.StatFs;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.ads.control.funtion.UtilsApp;
import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.test.screenrecord.R;
import com.test.screenrecord.common.Const;
import com.test.screenrecord.common.PrefUtils;
import com.test.screenrecord.common.Utils;
import com.test.screenrecord.gesture.ShakeEventManager;
import com.test.screenrecord.listener.ObserverUtils;
import com.test.screenrecord.model.listener.EvbRecordTime;
import com.test.screenrecord.model.listener.EvbStageRecord;
import com.test.screenrecord.ui.activities.EditVideoActivity;
import com.test.screenrecord.ui.activities.HomeActivity;
import com.test.screenrecord.videoTrimmer.utils.Toolbox;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;


public class RecorderService extends Service implements ShakeEventManager.ShakeListener {
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static int WIDTH, HEIGHT, FPS, DENSITY_DPI;
    private static int BITRATE;
    private static String audioRecSource;
    private static String SAVEPATH;
    private static int part = 0;
    private static ArrayList<String> arrPart;
    private int time = 0;
    private boolean isStart = false;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    private int screenOrientation;
    public static boolean isRecording;
    private boolean showCameraOverlay;
    //    private boolean showTouches;
    private boolean isShakeGestureActive;
    private FloatingControlService floatingControlService;
    private boolean isBound = false;
    private NotificationManager mNotificationManager;
    Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message message) {
            Toast.makeText(RecorderService.this, R.string.screen_recording_stopped_toast, Toast.LENGTH_SHORT).show();
            showShareNotification();
            Utils.showDialogResult(RecorderService.this.getApplicationContext(), SAVEPATH);
        }
    };
    private ShakeEventManager mShakeDetector;
    private Intent data;
    private int result;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            FloatingControlService.ServiceBinder binder = (FloatingControlService.ServiceBinder) service;
            floatingControlService = binder.getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            floatingControlService = null;
            isBound = false;
        }
    };

    private ServiceConnection floatingCameraConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            FloatingCameraViewService.ServiceBinder binder = (FloatingCameraViewService.ServiceBinder) service;
            FloatingCameraViewService floatingCameraViewService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            floatingControlService = null;
        }
    };
    private long startTime, elapsedTime = 0;
    private SharedPreferences prefs;
    private WindowManager window;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaProjectionCallback mMediaProjectionCallback;
    private MediaRecorder mMediaRecorder;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                createNotificationChannels();

            Intent floatinControlsIntent = new Intent(this, FloatingControlService.class);
            floatinControlsIntent.setAction(intent.getAction());
            startService(floatinControlsIntent);
            bindService(floatinControlsIntent, serviceConnection, BIND_AUTO_CREATE);

            prefs = PreferenceManager.getDefaultSharedPreferences(this);

            switch (intent.getAction()) {
                case Const.SCREEN_RECORDING_START:
                    if (!isRecording) {
                        screenOrientation = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
                        data = intent.getParcelableExtra(Const.RECORDER_INTENT_DATA);
                        result = intent.getIntExtra(Const.RECORDER_INTENT_RESULT, Activity.RESULT_OK);

                        getValues();
                        if (prefs.getBoolean(getString(R.string.preference_enable_target_app_key), false))
                            startAppBeforeRecording(prefs.getString(getString(R.string.preference_app_chooser_key), "none"));

                        if (isShakeGestureActive) {
                            mShakeDetector = new ShakeEventManager(this);
                            mShakeDetector.init(this);

                            Bitmap icon = BitmapFactory.decodeResource(getResources(),
                                    R.drawable.logo);

                            Intent destroyMediaRecorderIntent = new Intent(this, RecorderService.class);
                            destroyMediaRecorderIntent.setAction(Const.SCREEN_RECORDING_DESTORY_SHAKE_GESTURE);
                            PendingIntent pdestroyMediaRecorderIntent = PendingIntent.getService(this, 0, destroyMediaRecorderIntent, 0);

                            NotificationCompat.Builder shakeGestureWaitNotification =
                                    new NotificationCompat.Builder(this, Const.RECORDING_NOTIFICATION_CHANNEL_ID)
                                            .setContentTitle("Waiting for device shake")
                                            .setContentText("Shake your device to start recording or press this notification to cancel")
                                            .setOngoing(true)
                                            .setSmallIcon(R.drawable.ic_notification)
                                            .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                                            .setContentIntent(pdestroyMediaRecorderIntent);

                            startNotificationForeGround(shakeGestureWaitNotification.build(), Const.SCREEN_RECORDER_SHARE_NOTIFICATION_ID);

                            Toast.makeText(this, R.string.screenrecording_waiting_for_gesture_toast,
                                    Toast.LENGTH_LONG).show();
                        } else {
                            startRecording();
                        }

                    } else {
                        Toast.makeText(this, R.string.screenrecording_already_active_toast, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Const.SCREEN_RECORDING_PAUSE:
                    pauseScreenRecording();
                    break;
                case Const.SCREEN_RECORDING_RESUME:
                    resumeScreenRecording();
                    break;
                case Const.SCREEN_RECORDING_STOP:
                    stopRecording();
                    break;
                case Const.SCREEN_RECORDING_DESTORY_SHAKE_GESTURE:
                    mShakeDetector.stop();
                    stopSelf();
                    break;
            }
        } catch (Exception e) {
        }
        return START_STICKY;
    }

    private void stopRecording() {
        try {
            if (isBound) {
                if (isBound)
                    floatingControlService.setRecordingState(Const.RecordingState.STOPPED);
                unbindService(serviceConnection);
                if (showCameraOverlay) {
                    unbindService(floatingCameraConnection);
                }
                Log.d(Const.TAG, "Unbinding connection service");
            }

            stopScreenSharing();
            isRecording = false;
            isStart = false;
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void startAppBeforeRecording(String packagename) {
        try {
            if (packagename.equals("none"))
                return;

            Intent startAppIntent = getPackageManager().getLaunchIntentForPackage(packagename);
            startActivity(startAppIntent);
        } catch (Exception e) {
        }
    }

    @TargetApi(24)
    private void pauseScreenRecording() {
        try {
            if (!isRecording) return;
            if (Build.VERSION.SDK_INT < 24) {
                destroyMediaProjection();
            } else {
                mMediaRecorder.pause();
                isRecording = false;
            }
            elapsedTime += (System.currentTimeMillis() - startTime);

            Intent recordResumeIntent = new Intent(this, RecorderService.class);
            recordResumeIntent.setAction(Const.SCREEN_RECORDING_RESUME);
            updateNotification(createRecordingNotification().setUsesChronometer(false).build(), Const.SCREEN_RECORDER_NOTIFICATION_ID);
            Toast.makeText(this, R.string.screen_recording_paused_toast, Toast.LENGTH_SHORT).show();

            if (isBound)
                floatingControlService.setRecordingState(Const.RecordingState.PAUSED);


        } catch (Exception e) {
        }
    }

    @TargetApi(24)
    private void resumeScreenRecording() {
        if (isRecording) return;
        try {
            if (Build.VERSION.SDK_INT < 24) {
                startRecording();
            } else {
                mMediaRecorder.resume();
            }

            isRecording = true;

            //Reset startTime to current time again
            startTime = System.currentTimeMillis();

            //set Pause action to Notification and update current Notification
            Intent recordPauseIntent = new Intent(this, RecorderService.class);
            recordPauseIntent.setAction(Const.SCREEN_RECORDING_PAUSE);
            Toast.makeText(this, R.string.screen_recording_resumed_toast, Toast.LENGTH_SHORT).show();

            if (isBound)
                floatingControlService.setRecordingState(Const.RecordingState.RECORDING);


        } catch (Exception e) {
        }
    }

    private void startRecording() {
        try {
            FloatingControlService.isRecording = true;
            mMediaRecorder = new MediaRecorder();
            initRecorder();

            mMediaProjectionCallback = new MediaProjectionCallback();
            MediaProjectionManager mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

            mMediaProjection = mProjectionManager.getMediaProjection(result, data);
            mMediaProjection.registerCallback(mMediaProjectionCallback, null);

            mVirtualDisplay = createVirtualDisplay();
            try {
                mMediaRecorder.start();

                if (showCameraOverlay) {
                    if (UtilsApp.isMyServiceRunning(FloatingCameraViewService.class, RecorderService.this)) {

                    } else {
                        Intent floatingCameraIntent = new Intent(this, FloatingCameraViewService.class);
                        startService(floatingCameraIntent);
                        bindService(floatingCameraIntent, floatingCameraConnection, BIND_AUTO_CREATE);
                    }

                }

                if (isBound)
                    floatingControlService.setRecordingState(Const.RecordingState.RECORDING);
                isRecording = true;
                if (part == 0) {
                    Toast.makeText(this, R.string.screen_recording_started_toast, Toast.LENGTH_SHORT).show();
                    isStart = true;
                    TimeCount mTimeCount = new TimeCount();
                    mTimeCount.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            } catch (Exception e) {
                Log.e(Const.TAG, "Mediarecorder reached Illegal state exception. Did you start the recording twice?");
                Toast.makeText(this, R.string.recording_failed_toast, Toast.LENGTH_SHORT).show();
                isRecording = false;
                mMediaProjection.stop();
                stopSelf();
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                startTime = System.currentTimeMillis();
                Intent recordPauseIntent = new Intent(this, RecorderService.class);
                recordPauseIntent.setAction(Const.SCREEN_RECORDING_PAUSE);
                PendingIntent precordPauseIntent = PendingIntent.getService(this, 0, recordPauseIntent, 0);
                NotificationCompat.Action action = new NotificationCompat.Action(android.R.drawable.ic_media_pause,
                        getString(R.string.screen_recording_notification_action_pause), precordPauseIntent);

                startNotificationForeGround(createRecordingNotification().build(), Const.SCREEN_RECORDER_NOTIFICATION_ID);
            } else
                startNotificationForeGround(createRecordingNotification().build(), Const.SCREEN_RECORDER_NOTIFICATION_ID);
        } catch (Exception e) {
            Log.e(Const.TAG, "Loi ne " + e.getMessage());

        }
    }

    private class TimeCount extends AsyncTask<Void, Integer, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            while (isStart) {
                if (isRecording)
                    time++;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Log.e("XXX",time+"");
                publishProgress(time);
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            ObserverUtils.getInstance().notifyObservers(new EvbRecordTime(Toolbox.converTime(values[0] + "")));
        }

        @Override
        protected void onPostExecute(Void voids) {
            super.onPostExecute(voids);
            ObserverUtils.getInstance().notifyObservers(new EvbStageRecord(false));
            time = 0;
        }
    }

    private VirtualDisplay createVirtualDisplay() {
        return mMediaProjection.createVirtualDisplay("MainActivity",
                WIDTH, HEIGHT, DENSITY_DPI,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mMediaRecorder.getSurface(), null /*Callbacks*/, null);
    }

    private void initRecorder() {
        boolean mustRecAudio = false;
        try {
            switch (audioRecSource) {
                case "1":
                    mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    mustRecAudio = true;
                    break;
//                case "2":
//                    mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
//                    mMediaRecorder.setAudioEncodingBitRate(320 * 1000);
//                    mMediaRecorder.setAudioSamplingRate(48000);
//                    mustRecAudio = true;
//                    break;
//                case "3":
//                    mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.REMOTE_SUBMIX);
//                    mMediaRecorder.setAudioEncodingBitRate(320 * 1000);
//                    mMediaRecorder.setAudioSamplingRate(48000);
//                    mustRecAudio = true;
//                    break;
            }
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            String tempPath;
            if (Build.VERSION.SDK_INT < 24) {
                String strPart = SAVEPATH.replaceAll("[.]mp4", "." + part + "_.mp4");
                arrPart.add(strPart);
                tempPath = strPart;
                mMediaRecorder.setOutputFile(strPart);
            } else {
                tempPath = SAVEPATH;
                mMediaRecorder.setOutputFile(SAVEPATH);
            }
            mMediaRecorder.setVideoSize(WIDTH, HEIGHT);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            //   mMediaRecorder.setMaxFileSize(getFreeSpaceInBytes(tempPath));

            if (mustRecAudio)
                mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mMediaRecorder.setVideoEncodingBitRate(BITRATE);
            mMediaRecorder.setVideoFrameRate(FPS);
            mMediaRecorder.prepare();
        } catch (IOException e) {
            Log.e(Const.TAG, "Loi ne initRecorder " + e.getMessage());
            int b = 6;
        }

    }

    private int getBestVideoEncoder() {
        int VideoCodec = MediaRecorder.VideoEncoder.DEFAULT;
        if (getMediaCodecFor(MediaFormat.MIMETYPE_VIDEO_HEVC)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                VideoCodec = MediaRecorder.VideoEncoder.HEVC;
            }
        } else if (getMediaCodecFor(MediaFormat.MIMETYPE_VIDEO_AVC))
            VideoCodec = MediaRecorder.VideoEncoder.H264;
        return VideoCodec;
    }

    private boolean getMediaCodecFor(String format) {
        MediaCodecList list = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(
                format,
                WIDTH,
                HEIGHT
        );
        String encoder = list.findEncoderForFormat(mediaFormat);
        if (encoder == null) {
            Log.d("Null Encoder: ", format);
            return false;
        }
        Log.d("Encoder", encoder);
        return !encoder.startsWith("OMX.google");
    }

    private long getFreeSpaceInBytes(String path) {
        StatFs FSStats = new StatFs(path);
        long bytesAvailable = FSStats.getAvailableBytes();// * FSStats.getBlockCountLong();
        Log.d(Const.TAG, "Free space in GB: " + bytesAvailable / (1000 * 1000 * 1000));
        return bytesAvailable;
    }

    @TargetApi(26)
    private void createNotificationChannels() {
        try {
            List<NotificationChannel> notificationChannels = new ArrayList<>();
            NotificationChannel recordingNotificationChannel = new NotificationChannel(
                    Const.RECORDING_NOTIFICATION_CHANNEL_ID,
                    Const.RECORDING_NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            recordingNotificationChannel.enableLights(true);
            recordingNotificationChannel.setLightColor(Color.RED);
            recordingNotificationChannel.setShowBadge(true);
            recordingNotificationChannel.enableVibration(true);
            recordingNotificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationChannels.add(recordingNotificationChannel);

            NotificationChannel shareNotificationChannel = new NotificationChannel(
                    Const.SHARE_NOTIFICATION_CHANNEL_ID,
                    Const.SHARE_NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            shareNotificationChannel.enableLights(true);
            shareNotificationChannel.setLightColor(Color.RED);
            shareNotificationChannel.setShowBadge(true);
            shareNotificationChannel.enableVibration(true);
            shareNotificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationChannels.add(shareNotificationChannel);
            getManager().createNotificationChannels(notificationChannels);
        } catch (Exception e) {
        }
    }

    private NotificationCompat.Builder createRecordingNotification() {
        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.logo);
        Intent recordStopIntent = new Intent(this, RecorderService.class);
        recordStopIntent.setAction(Const.SCREEN_RECORDING_STOP);
        Intent UIIntent = new Intent(this, HomeActivity.class);
        PendingIntent notificationContentIntent = PendingIntent.getActivity(this, 0, UIIntent, 0);

        NotificationCompat.Builder notification = new NotificationCompat.Builder(this, Const.RECORDING_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(getResources().getString(R.string.screen_recording_notification_title))
                .setTicker(getResources().getString(R.string.screen_recording_notification_title))
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setUsesChronometer(true)
                .setOngoing(true)
                .setContentIntent(notificationContentIntent)
                .setPriority(NotificationManager.IMPORTANCE_DEFAULT);
        return notification;
    }

    private void showShareNotification() {
        try {
            Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.logo);
            Intent editIntent = new Intent(this, EditVideoActivity.class);
            editIntent.putExtra(Const.VIDEO_EDIT_URI_KEY, SAVEPATH);
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                    new Intent(this, HomeActivity.class).setAction(Const.SCREEN_RECORDER_VIDEOS_LIST_FRAGMENT_INTENT), PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationCompat.Builder shareNotification = new NotificationCompat.Builder(this, Const.SHARE_NOTIFICATION_CHANNEL_ID)
                    .setContentTitle(getString(R.string.share_intent_notification_title))
                    .setContentText(getString(R.string.share_intent_notification_content))
                    .setSmallIcon(R.drawable.ic_notification)
                    .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                    .setAutoCancel(true)
                    .setContentIntent(contentIntent);
            updateNotification(shareNotification.build(), Const.SCREEN_RECORDER_SHARE_NOTIFICATION_ID);
        } catch (Exception e) {
        }
    }

    private void startNotificationForeGround(Notification notification, int ID) {
        startForeground(ID, notification);
    }

    private void updateNotification(Notification notification, int ID) {
        getManager().notify(ID, notification);
    }

    private NotificationManager getManager() {
        if (mNotificationManager == null) {
            mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return mNotificationManager;
    }

    @Override
    public void onDestroy() {
        Log.d(Const.TAG, "Recorder service destroyed");
        isStart = false;
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void getValues() {
        try {
            String res = getResolution();
            setWidthHeight(res);
            if (arrPart == null) {
                arrPart = new ArrayList<>();
            } else {
                arrPart.clear();
            }
            FPS = Integer.parseInt(prefs.getString(getString(R.string.fps_key), "30"));
            BITRATE = Integer.parseInt(prefs.getString(getString(R.string.bitrate_key), "7130317"));
            audioRecSource = prefs.getString(getString(R.string.audiorec_key), PrefUtils.VALUE_AUDIO);
            String saveLocation = prefs.getString(getString(R.string.savelocation_key),
                    Environment.getExternalStorageDirectory() + File.separator + Const.APPDIR);
            File saveDir = new File(saveLocation);
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) && !saveDir.isDirectory()) {
                saveDir.mkdirs();
            }
            showCameraOverlay = prefs.getBoolean(getString(R.string.preference_camera_overlay_key), false);
            String saveFileName = getFileSaveName();
            SAVEPATH = saveLocation + File.separator + saveFileName + ".mp4";
            isShakeGestureActive = prefs.getBoolean(getString(R.string.preference_shake_gesture_key), false);
        } catch (Exception e) {
        }
    }

    private void setWidthHeight(String res) {
        String[] widthHeight = res.split("x");
        String orientationPrefs = prefs.getString(getString(R.string.orientation_key), "auto");
        switch (orientationPrefs) {
            case "auto":
                if (screenOrientation == 0 || screenOrientation == 2) {
                    WIDTH = Integer.parseInt(widthHeight[0]);
                    HEIGHT = Integer.parseInt(widthHeight[1]);
                } else {
                    HEIGHT = Integer.parseInt(widthHeight[0]);
                    WIDTH = Integer.parseInt(widthHeight[1]);
                }
                break;
            case "portrait":
                WIDTH = Integer.parseInt(widthHeight[0]);
                HEIGHT = Integer.parseInt(widthHeight[1]);
                break;
            case "landscape":
                HEIGHT = Integer.parseInt(widthHeight[0]);
                WIDTH = Integer.parseInt(widthHeight[1]);
                break;
        }
        Log.d(Const.TAG, "Width: " + WIDTH + ",Height:" + HEIGHT);
    }


    //Get the device resolution in pixels
    private String getResolution() {
        DisplayMetrics metrics = new DisplayMetrics();
        window = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        window.getDefaultDisplay().getRealMetrics(metrics);
        DENSITY_DPI = metrics.densityDpi;
        int width = metrics.widthPixels;
        width = Integer.parseInt(prefs.getString(getString(R.string.res_key), Integer.toString(width)));
        float aspectRatio = getAspectRatio(metrics);
        int height = calculateClosestHeight(width, aspectRatio);
        //String res = width + "x" + (int) (width * getAspectRatio(metrics));
        String res = width + "x" + height;
        Log.d(Const.TAG, "resolution service: " + "[Width: "
                + width + ", Height: " + width * aspectRatio + ", aspect ratio: " + aspectRatio + "]");
        return res;
    }

    private int calculateClosestHeight(int width, float aspectRatio) {
        int calculatedHeight = (int) (width * aspectRatio);
        Log.d(Const.TAG, "Calculated width=" + calculatedHeight);
        Log.d(Const.TAG, "Aspect ratio: " + aspectRatio);
        if (calculatedHeight / 16 != 0) {
            int quotient = calculatedHeight / 16;
            Log.d(Const.TAG, calculatedHeight + " not divisible by 16");

            calculatedHeight = 16 * quotient;

            Log.d(Const.TAG, "Maximum possible height is " + calculatedHeight);
        }
        return calculatedHeight;
    }


    private float getAspectRatio(DisplayMetrics metrics) {
        float screen_width = metrics.widthPixels;
        float screen_height = metrics.heightPixels;
        float aspectRatio;
        if (screen_width > screen_height) {
            aspectRatio = screen_width / screen_height;
        } else {
            aspectRatio = screen_height / screen_width;
        }
        return aspectRatio;
    }

    private String getFileSaveName() {
        String filename = prefs.getString(getString(R.string.filename_key), "yyyyMMdd_hhmmss");
        String prefix = prefs.getString(getString(R.string.fileprefix_key), "recording");
        Date today = Calendar.getInstance().getTime();
        SimpleDateFormat formatter = new SimpleDateFormat(filename);
        return prefix + "_" + formatter.format(today);
    }

    private void destroyMediaProjection() {
        try {
            mMediaRecorder.stop();
            if (Build.VERSION.SDK_INT >= 24) {
                indexFile();
            } else {
                part++;
            }
        } catch (RuntimeException e) {
            if (Build.VERSION.SDK_INT < 24 && arrPart.size() > 0) {
                if (new File(arrPart.get(arrPart.size() - 1)).delete()) ;
            }
            if (new File(SAVEPATH).delete())
                Log.d(Const.TAG, "Corrupted file delete successful");
            Toast.makeText(this, getString(R.string.fatal_exception_message), Toast.LENGTH_SHORT).show();
        } finally {
            mMediaRecorder.reset();
            mVirtualDisplay.release();
            mMediaRecorder.release();
            if (mMediaProjection != null) {
                mMediaProjection.unregisterCallback(mMediaProjectionCallback);
                mMediaProjection.stop();
                mMediaProjection = null;
            }
        }
        isRecording = false;
    }

    private void indexFile() {
        try {
            ArrayList<String> toBeScanned = new ArrayList<>();
            toBeScanned.add(SAVEPATH);
            String[] toBeScannedStr = new String[toBeScanned.size()];
            toBeScannedStr = toBeScanned.toArray(toBeScannedStr);

            MediaScannerConnection.scanFile(this, toBeScannedStr, null, new MediaScannerConnection.OnScanCompletedListener() {

                @Override
                public void onScanCompleted(String path, Uri uri) {
                    Message message = mHandler.obtainMessage();
                    message.sendToTarget();
                    stopSelf();
                }
            });
        } catch (Exception e) {
        }
    }

    private void stopScreenSharing() {
        if (mVirtualDisplay == null) {
            Log.d(Const.TAG, "Virtual display is null. Screen sharing already stopped");
            return;
        }
        if (Build.VERSION.SDK_INT >= 24) {
            destroyMediaProjection();
        } else {
            if (isRecording) {
                destroyMediaProjection();
            }
            mergeMediaFiles(arrPart, SAVEPATH);
        }

        for (String item : arrPart) {
            Log.e(Const.TAG, item);
        }
    }

    public void mergeMediaFiles(ArrayList<String> sourceFiles, String targetFile) {
        new MyTask(sourceFiles, targetFile).execute();
    }

    @Override
    public void onShake() {
        if (!isRecording) {
            Vibrator vibrate = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

            getManager().cancel(Const.SCREEN_RECORDER_WAITING_FOR_SHAKE_NOTIFICATION_ID);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
                vibrate.vibrate(500);
            else
                VibrationEffect.createOneShot(500, 255);

            startRecording();
        } else {
            Intent recordStopIntent = new Intent(this, RecorderService.class);
            recordStopIntent.setAction(Const.SCREEN_RECORDING_STOP);
            startService(recordStopIntent);
            mShakeDetector.stop();
        }
    }

    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            Log.v(Const.TAG, "Recording Stopped");
            stopScreenSharing();
        }
    }

    public class MyTask extends AsyncTask<Void, Void, Void> {

        ArrayList<String> sourceFiles;
        String targetFile;

        public MyTask(ArrayList<String> sourceFiles, String targetFile) {
            this.sourceFiles = sourceFiles;
            this.targetFile = targetFile;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Log.e(Const.TAG, "start mergeVideos");
                Log.e(Const.TAG, "target: " + targetFile);
                List<Movie> listMovies = new ArrayList<>();
                for (String filename : sourceFiles) {
                    listMovies.add(MovieCreator.build(filename));
                    Log.e(Const.TAG, filename);
                }
                List<Track> listTracksVideo = new LinkedList<>();
                List<Track> listTracks = new LinkedList<>();
                for (Movie movie : listMovies) {
                    for (Track track : movie.getTracks()) {
                        if (track.getHandler().equals("vide")) {
                            listTracksVideo.add(track);
                        }
                        if (track.getHandler().equals("soun")) {
                            listTracks.add(track);
                        }
                    }
                }
                Movie outputMovie = new Movie();
                if (!listTracksVideo.isEmpty()) {
                    outputMovie.addTrack(new AppendTrack(listTracksVideo.toArray(new Track[listTracksVideo.size()])));
                }
                if (Build.VERSION.SDK_INT < 24) {
                    if (!listTracks.isEmpty()) {
                        outputMovie.addTrack(new AppendTrack(listTracks.toArray(new Track[listTracks.size()])));
                    }
                }
                Container container = new DefaultMp4Builder().build(outputMovie);
                FileChannel fileChannel = new RandomAccessFile(String.format(targetFile), "rw").getChannel();
                container.writeContainer(fileChannel);
                fileChannel.close();
                indexFile();
                for (String item : arrPart) {
                    File file = new File(item);
                    if (file.exists()) {
                        file.delete();
                    }
                }
                part = 0;
                arrPart.clear();
                Log.e(Const.TAG, "finish mergeVideos");
            } catch (Exception e) {
                Log.e(Const.TAG, "Error merging media files. exception: " + e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.e(Const.TAG, "onPreExcute");
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Log.e(Const.TAG, "onPostExcute");
            stopForeground(true);
        }
    }
}