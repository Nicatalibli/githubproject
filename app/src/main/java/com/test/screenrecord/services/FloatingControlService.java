package com.test.screenrecord.services;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.internal.view.SupportMenu;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.test.screenrecord.R;
import com.test.screenrecord.common.Const;
import com.test.screenrecord.common.Const.RecordingState;
import com.test.screenrecord.common.PrefUtils;
import com.test.screenrecord.common.Utils;
import com.test.screenrecord.listener.ObserverInterface;
import com.test.screenrecord.listener.ObserverUtils;
import com.test.screenrecord.model.listener.EvbStageRecord;
import com.test.screenrecord.model.listener.EvbStopService;
import com.test.screenrecord.ui.activities.HomeActivity;
import com.test.screenrecord.ui.activities.RecorderActivity;
import com.test.screenrecord.ui.activities.RequestRecorderActivity;
import com.test.screenrecord.ui.activities.ScreenShotActivity;
import com.test.screenrecord.ui.activities.SplashScreenActivity;


public class FloatingControlService extends Service implements View.OnClickListener, ObserverInterface {

    private final int NOTIFICATION_ID = 212;
    private final int TIME_DELAY = 2000;
    private WindowManager windowManager;
    private View mRemoveView;
    private LinearLayout floatingControls;
    private int[] overlayViewLocation = {0, 0};
    private boolean isOverRemoveView;
    private int[] removeViewLocation = {0, 0};
    private int width, height;
    private WindowManager.LayoutParams params;
    private WindowManager.LayoutParams paramsTimer;
    private GestureDetector gestureDetector;
    private View controlsRecorder, controlsMain, controlsMainLeft, controlsRecorderLeft;
    private ImageView img;
    private ImageButton recorderIB, screenshotIB, panelIB, rewardIB, recorderLeftIB, screenshotLeftIB, panelLeftIB;
    private LinearLayout layoutTimer;
    private TextView txtTimer;
    private ImageButton pauseIB, pauseLeftIB;
    private ImageButton resumeIB, resumeLeftIB;
    private ImageButton stopIB, stopLeftIB;
    private IBinder binder = new ServiceBinder();
    public static boolean isRecording = false;
    private static boolean isRightSide = true;
    public static boolean isCountdown = false;
    public static boolean isExpand = false;
    public static boolean isPause = false;
    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {

        @Override
        public void run() {
            setAlphaAssistiveIcon();
        }
    };

    private void setAlphaAssistiveIcon() {
        try {
            if (floatingControls != null && (controlsRecorder.getVisibility() != View.VISIBLE || controlsMain.getVisibility() != View.VISIBLE
                    || controlsRecorderLeft.getVisibility() != View.VISIBLE || controlsMainLeft.getVisibility() != View.VISIBLE)) {
                ViewGroup.LayoutParams layoutParams = img.getLayoutParams();
                layoutParams.height = width / 16;
                layoutParams.width = width / 16;
                floatingControls.setAlpha(0.3f);
                img.setLayoutParams(layoutParams);

                if (params.x < width - params.x) {
                    params.x = 0;
                } else {
                    params.x = width;
                }
                windowManager.updateViewLayout(floatingControls, params);
            }
        } catch (Exception e) {
            Log.e(Const.TAG, "Error FloatingControlService: " + e.getMessage());
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            ObserverUtils.getInstance().registerObserver(this);
            windowManager = (WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE);
            LayoutInflater li = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);
            floatingControls = (LinearLayout) li.inflate(R.layout.layout_floating_controls, null);
            mRemoveView = onGetRemoveView();
            setupRemoveView(mRemoveView);
            layoutTimer = (LinearLayout) li.inflate(R.layout.layout_timer, null);
            txtTimer = layoutTimer.findViewById(R.id.txt_timer);
            controlsRecorder = floatingControls.findViewById(R.id.controls_recorder);
            controlsRecorderLeft = floatingControls.findViewById(R.id.controls_recorder_left);
            controlsMain = floatingControls.findViewById(R.id.controls_main);
            controlsMainLeft = floatingControls.findViewById(R.id.controls_main_left);
            img = floatingControls.findViewById(R.id.imgIcon);

            controlsMain.setVisibility(View.GONE);
            controlsMainLeft.setVisibility(View.GONE);
            controlsRecorder.setVisibility(View.GONE);
            controlsRecorderLeft.setVisibility(View.GONE);

            //Initialize imageButtons
            stopIB = controlsRecorder.findViewById(R.id.stop);
            pauseIB = controlsRecorder.findViewById(R.id.pause);
            resumeIB = controlsRecorder.findViewById(R.id.resume);
            stopLeftIB = controlsRecorderLeft.findViewById(R.id.stop_left);
            pauseLeftIB = controlsRecorderLeft.findViewById(R.id.pause_left);
            resumeLeftIB = controlsRecorderLeft.findViewById(R.id.resume_left);
            recorderIB = controlsMain.findViewById(R.id.recorder);
            screenshotIB = controlsMain.findViewById(R.id.screenshot);
            panelIB = controlsMain.findViewById(R.id.panel);
            recorderLeftIB = controlsMainLeft.findViewById(R.id.recorder_left);
            screenshotLeftIB = controlsMainLeft.findViewById(R.id.screenshot_left);
            panelLeftIB = controlsMainLeft.findViewById(R.id.panel_left);

            stopIB.setOnClickListener(this);
            stopLeftIB.setOnClickListener(this);
            recorderIB.setOnClickListener(this);
            recorderLeftIB.setOnClickListener(this);
            screenshotIB.setOnClickListener(this);
            screenshotLeftIB.setOnClickListener(this);
            panelIB.setOnClickListener(this);
            panelLeftIB.setOnClickListener(this);

//            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
//                pauseIB.setVisibility(View.GONE);
//                pauseLeftIB.setVisibility(View.GONE);
//                resumeIB.setVisibility(View.GONE);
//                resumeLeftIB.setVisibility(View.GONE);
//            } else {
            pauseIB.setOnClickListener(this);
            pauseLeftIB.setOnClickListener(this);
            resumeIB.setOnClickListener(this);
            resumeLeftIB.setOnClickListener(this);
//            }

            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
                params.type = WindowManager.LayoutParams.TYPE_TOAST;

            DisplayMetrics displaymetrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(displaymetrics);
            height = displaymetrics.heightPixels;
            width = displaymetrics.widthPixels;

            initSizeLayout();

            params.gravity = Gravity.TOP | Gravity.START;
            params.x = 0;
            params.y = height / 4;

            paramsTimer = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
                paramsTimer.type = WindowManager.LayoutParams.TYPE_TOAST;

            paramsTimer.gravity = Gravity.CENTER | Gravity.CENTER;

            try {
                gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onSingleTapUp(MotionEvent e) {
                        return true;
                    }

                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        return true;
                    }
                });
                floatingControls.setOnTouchListener(new View.OnTouchListener() {
                    private WindowManager.LayoutParams paramsF = params;
                    private int initialX;
                    private int initialY;
                    private float initialTouchX;
                    private float initialTouchY;
                    private boolean flag = false;

                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (gestureDetector.onTouchEvent(event)) {
                            mRemoveView.setVisibility(View.GONE);
                            if (controlsRecorder.getVisibility() == View.VISIBLE || controlsMain.getVisibility() == View.VISIBLE
                                    || controlsRecorderLeft.getVisibility() == View.VISIBLE || controlsMainLeft.getVisibility() == View.VISIBLE) {
                                collapseFloatingControls();
                            } else {
                                expandFloatingControls();
                            }
                        } else {
                            if (!isExpand) {
                                switch (event.getAction()) {
                                    case MotionEvent.ACTION_DOWN:
                                        handler.removeCallbacks(runnable);
                                        ViewGroup.LayoutParams layoutParams = img.getLayoutParams();
                                        layoutParams.height = width / 8;
                                        layoutParams.width = width / 8;
                                        img.setLayoutParams(layoutParams);
                                        floatingControls.setAlpha(1f);
                                        initialX = paramsF.x;
                                        initialY = paramsF.y;
                                        initialTouchX = event.getRawX();
                                        initialTouchY = event.getRawY();
                                        flag = true;
                                        break;
                                    case MotionEvent.ACTION_UP:
                                        if (params.x < width - params.x) {
                                            params.x = 0;
                                            isRightSide = true;
                                        } else {
                                            params.x = width - floatingControls.getWidth();
                                            isRightSide = false;
                                        }
                                        flag = false;
                                        if (isOverRemoveView) {
                                            stopSelf();
                                        } else {
                                            windowManager.updateViewLayout(floatingControls, params);
                                            handler.postDelayed(runnable, TIME_DELAY);
                                        }
                                        mRemoveView.setVisibility(View.GONE);
                                        break;
                                    case MotionEvent.ACTION_MOVE:
                                        int xDiff = (int) (event.getRawX() - initialTouchX);
                                        int yDiff = (int) (event.getRawY() - initialTouchY);
                                        paramsF.x = initialX + xDiff;
                                        paramsF.y = initialY + yDiff;
                                        if (flag) {
                                            mRemoveView.setVisibility(View.VISIBLE);
                                        }
                                        windowManager.updateViewLayout(floatingControls, paramsF);
                                        floatingControls.getLocationOnScreen(overlayViewLocation);
                                        mRemoveView.getLocationOnScreen(removeViewLocation);
                                        isOverRemoveView = isPointInArea(overlayViewLocation[0], overlayViewLocation[1],
                                                removeViewLocation[0], removeViewLocation[1], mRemoveView.getWidth());
                                        if (isOverRemoveView) {
                                        }
                                        break;
                                    case MotionEvent.ACTION_CANCEL:
                                        mRemoveView.setVisibility(View.GONE);
                                        break;
                                }
                            }
                        }
                        return false;
                    }
                });
            } catch (Exception e) {
                Log.e(Const.TAG, "Error FloatingControlService: " + e.getMessage());
            }

            addBubbleView();
            handler.postDelayed(runnable, TIME_DELAY);
        } catch (Exception e) {
            Log.e(Const.TAG, "Error FloatingControlService: " + e.getMessage());
        }
    }

    private boolean isPointInArea(int x1, int y1, int x2, int y2, int radius) {
        return x1 >= x2 - radius && x1 <= x2 + radius && y1 >= y2 - radius && y1 <= y2 + radius;
    }

    private void setupRemoveView(View removeView) {
        removeView.setVisibility(View.GONE);
        windowManager.addView(removeView, newWindowManagerLayoutParamsForRemoveView());
    }

    private static WindowManager.LayoutParams newWindowManagerLayoutParamsForRemoveView() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT < Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_PHONE : WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
        params.y = 56;
        return params;
    }

    @SuppressLint("InflateParams")
    protected View onGetRemoveView() {
        return LayoutInflater.from(this).inflate(R.layout.overlay_remove_view, null);
    }

    private void initSizeLayout() {
        try {
            int size = width / 8;
            int icon = size;
            int pading = size / 6;
            ViewGroup.LayoutParams layout = img.getLayoutParams();
            layout.height = size;
            layout.width = size;
            img.setLayoutParams(layout);

            ViewGroup.LayoutParams record = recorderIB.getLayoutParams();
            record.width = icon;
            record.height = icon;
            recorderIB.setLayoutParams(record);
            recorderIB.setPadding(pading, pading, pading, pading);

            ViewGroup.LayoutParams screenshot = screenshotIB.getLayoutParams();
            screenshot.width = icon;
            screenshot.height = icon;
            screenshotIB.setLayoutParams(screenshot);
            screenshotIB.setPadding(pading, pading, pading, pading);

            ViewGroup.LayoutParams panel = panelIB.getLayoutParams();
            panel.width = icon;
            panel.height = icon;
            panelIB.setLayoutParams(panel);
            panelIB.setPadding(pading, pading, pading, pading);

            //Left
            ViewGroup.LayoutParams recordL = recorderLeftIB.getLayoutParams();
            recordL.width = icon;
            recordL.height = icon;
            recorderLeftIB.setLayoutParams(recordL);
            recorderLeftIB.setPadding(pading, pading, pading, pading);

            ViewGroup.LayoutParams screenshotL = screenshotLeftIB.getLayoutParams();
            screenshotL.width = icon;
            screenshotL.height = icon;
            screenshotLeftIB.setLayoutParams(screenshotL);
            screenshotLeftIB.setPadding(pading, pading, pading, pading);

            ViewGroup.LayoutParams panelL = panelLeftIB.getLayoutParams();
            panelL.width = icon;
            panelL.height = icon;
            panelLeftIB.setLayoutParams(panelL);
            panelLeftIB.setPadding(pading, pading, pading, pading);

            ViewGroup.LayoutParams resume = resumeIB.getLayoutParams();
            resume.width = icon;
            resume.height = icon;
            resumeIB.setLayoutParams(resume);
            resumeIB.setPadding(pading, pading, pading, pading);

            ViewGroup.LayoutParams pause = pauseIB.getLayoutParams();
            pause.width = icon;
            pause.height = icon;
            pauseIB.setLayoutParams(pause);
            pauseIB.setPadding(pading, pading, pading, pading);

            ViewGroup.LayoutParams stop = stopIB.getLayoutParams();
            stop.width = icon;
            stop.height = icon;
            stopIB.setLayoutParams(stop);
            stopIB.setPadding(pading, pading, pading, pading);

            //Left
            ViewGroup.LayoutParams resumeL = resumeLeftIB.getLayoutParams();
            resumeL.width = icon;
            resumeL.height = icon;
            resumeLeftIB.setLayoutParams(resumeL);
            resumeLeftIB.setPadding(pading, pading, pading, pading);

            ViewGroup.LayoutParams pauseL = pauseLeftIB.getLayoutParams();
            pauseL.width = icon;
            pauseL.height = icon;
            pauseLeftIB.setLayoutParams(pauseL);
            pauseLeftIB.setPadding(pading, pading, pading, pading);

            ViewGroup.LayoutParams stopL = stopLeftIB.getLayoutParams();
            stopL.width = icon;
            stopL.height = icon;
            stopLeftIB.setLayoutParams(stopL);
            stopLeftIB.setPadding(pading, pading, pading, pading);
        } catch (Exception e) {
        }
    }

    @SuppressLint("WrongConstant")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        showNotification();
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case Const.SCREEN_RECORDING_START_FROM_NOTIFY:
                    handlerTimer();
                    break;
                case Const.SCREEN_RECORDING_DESTROY:
                    stopForeground(true);
                    onDestroy();
                    stopSelf();
                    break;
                case Const.SCREEN_RECORDING_START:
                case Const.SCREEN_RECORDING_STOP:
                    collapseFloatingControls();
                    break;
            }
        }
        if (SplashScreenActivity.isFirstOpen) {
            if (!isRecording && !isPause) {
                if (isRightSide) {
                    controlsMain.setVisibility(View.VISIBLE);
                } else {
                    controlsMainLeft.setVisibility(View.VISIBLE);
                }
                expandFloatingControls();
                ViewGroup.LayoutParams layoutParams = img.getLayoutParams();
                layoutParams.height = width / 8;
                layoutParams.width = width / 8;
                img.setLayoutParams(layoutParams);
                floatingControls.setAlpha(1f);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!isRecording && !isPause) {
                            if (isRightSide) {
                                controlsMain.setVisibility(View.GONE);
                            } else {
                                controlsMainLeft.setVisibility(View.GONE);
                            }
                            collapseFloatingControls();
                            setAlphaAssistiveIcon();
                        }
                    }
                }, 2000);
                SplashScreenActivity.isFirstOpen = false;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public void addBubbleView() {
        try {
            if (windowManager != null && floatingControls != null) {
                windowManager.addView(floatingControls, params);
            }
        } catch (Exception e) {
            Log.e(Const.TAG, "Error FloatingControlService: " + e.getMessage());
        }
    }

    public void addTimerView() {
        try {
            if (windowManager != null && layoutTimer != null) {
                windowManager.addView(layoutTimer, paramsTimer);
            }
        } catch (Exception e) {
            Log.e(Const.TAG, "Error FloatingControlService: " + e.getMessage());
        }
    }

    public void removeTimerView() {
        try {
            if (windowManager != null && layoutTimer != null) {
                windowManager.removeView(layoutTimer);
            }
        } catch (Exception e) {
        }
    }

    public void removeBubbleView() {
        try {
            if (windowManager != null && floatingControls != null) {
                windowManager.removeView(floatingControls);
            }
        } catch (Exception e) {
        }
    }

    public void handlerTimer() {
        try {
            collapseFloatingControls();
            int x = Integer.valueOf(PrefUtils.readStringValue(this, getString(R.string.timer_key), PrefUtils.VALUE_TIMER));
            if (x == 0) {
                requestRecorder();
                return;
            }
            isCountdown = true;
            addTimerView();
            txtTimer.setText(x + 1 + "");
            new CountDownTimer((x + 1) * 1000, 1000) {

                @Override
                public void onFinish() {
                    try {
                        txtTimer.setText("");
                        removeTimerView();
                        requestRecorder();
                        isCountdown = false;
                    } catch (Exception e) {
                    }
                }

                @Override
                public void onTick(long millisUntilFinished) {
                    try {
                        txtTimer.setText(millisUntilFinished / 1000 + "");
                    } catch (Exception e) {
                    }
                }
            }.start();
        } catch (Exception e) {
        }
    }

    public void removeNotification() {
        try {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(NOTIFICATION_ID);
        } catch (Exception e) {
        }
    }

    private void showNotification() {
        try {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent activity = PendingIntent.getActivity(this, 0, intent, 0);

            Intent setting = new Intent(this, HomeActivity.class);
            setting.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent mSeting = PendingIntent.getActivity(this, 0, setting, 0);

            Intent destroy = new Intent(this, RecorderActivity.class);
            destroy.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            destroy.setAction(Const.SCREEN_RECORDING_DESTROY);
            PendingIntent mDestroy = PendingIntent.getActivity(this, 0, destroy, 0);

            Intent resume = new Intent(this, RecorderActivity.class);
            resume.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            resume.setAction(Const.SCREEN_RECORDING_RESUME);
            PendingIntent mResume = PendingIntent.getActivity(this, 0, resume, 0);

            Intent pause = new Intent(this, RecorderActivity.class);
            pause.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            pause.setAction(Const.SCREEN_RECORDING_PAUSE);
            PendingIntent mPause = PendingIntent.getActivity(this, 0, pause, 0);

            Intent stop = new Intent(this, RecorderActivity.class);
            stop.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            stop.setAction(Const.SCREEN_RECORDING_STOP);
            PendingIntent mStop = PendingIntent.getActivity(this, 0, stop, 0);

            Intent recorder = new Intent(this, RecorderActivity.class);
            recorder.setAction(Const.SCREEN_RECORDING_START_FROM_NOTIFY);
            PendingIntent mRecorder = PendingIntent.getActivity(this, 0, recorder, 0);

            Intent screenshot = new Intent(this, ScreenShotActivity.class);
            screenshot.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent mScreenshot = PendingIntent.getActivity(this, 0, screenshot, 0);

            NotificationCompat.Builder ongoing = new NotificationCompat.Builder(this);
            ongoing.setSmallIcon(R.drawable.ic_notification);
            ongoing.setPriority(-2);
            ongoing.setLargeIcon(Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.logo), 52, 52, false));
            ongoing.setOngoing(true);
            NotificationCompat.Builder content = ongoing.setContentTitle(getString(R.string.app_name) + " is running");
            content.setTicker("Notification keeps app always run properly");
            RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.custom_notification);
            remoteViews.setOnClickPendingIntent(R.id.recorder, mRecorder);
            remoteViews.setOnClickPendingIntent(R.id.screenshot, mScreenshot);
            remoteViews.setOnClickPendingIntent(R.id.panel, mSeting);
            remoteViews.setOnClickPendingIntent(R.id.stop, mStop);
            remoteViews.setOnClickPendingIntent(R.id.close, mDestroy);

//            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
//                remoteViews.setViewVisibility(R.id.pause, View.GONE);
//                remoteViews.setViewVisibility(R.id.resume, View.GONE);
//            } else {
            remoteViews.setOnClickPendingIntent(R.id.resume, mResume);
            remoteViews.setOnClickPendingIntent(R.id.pause, mPause);
//            }

            if (isPause) {
                remoteViews.setViewVisibility(R.id.pause, View.GONE);
                remoteViews.setViewVisibility(R.id.resume, View.VISIBLE);
            } else {
                remoteViews.setViewVisibility(R.id.pause, View.VISIBLE);
                remoteViews.setViewVisibility(R.id.resume, View.GONE);
            }

            if (isRecording) {
                remoteViews.setViewVisibility(R.id.controls_main, View.GONE);
                remoteViews.setViewVisibility(R.id.controls_recorder, View.VISIBLE);
            } else {
                remoteViews.setViewVisibility(R.id.controls_main, View.VISIBLE);
                remoteViews.setViewVisibility(R.id.controls_recorder, View.GONE);
            }

            remoteViews.setOnClickPendingIntent(R.id.notification_layout_main_container, activity);
            remoteViews.setTextViewText(R.id.notification_layout_tv_first, getResources().getString(R.string.app_name));
            remoteViews.setTextViewText(R.id.notification_layout_tv_second, "Touch to open");
            if (Utils.isAndroid26()) {
                createChanelID();
                ongoing.setChannelId("my_channel_screenrecorder");
            }
            ongoing.setCustomContentView(remoteViews);
            Notification notification = ongoing.build();
            startForeground(NOTIFICATION_ID, notification);
        } catch (Exception e) {
        }
    }

    @SuppressLint("WrongConstant")
    @TargetApi(26)
    private void createChanelID() {
        try {
            NotificationManager notificationManager = (NotificationManager) getSystemService("notification");
            String str = "my_channel_screenrecorder";
            CharSequence string = getString(R.string.app_name);
            String string2 = getString(R.string.app_name);
            NotificationChannel notificationChannel = new NotificationChannel(str, string, NotificationManager.IMPORTANCE_LOW);
            notificationChannel.setDescription(string2);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(SupportMenu.CATEGORY_MASK);
            notificationChannel.enableVibration(true);
            notificationChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            notificationManager.createNotificationChannel(notificationChannel);
        } catch (Exception e) {
        }
    }

    private void expandFloatingControls() {
        isExpand = true;
        if (isRightSide) {
            if (isRecording) {
                controlsMain.setVisibility(View.GONE);
                controlsRecorder.setVisibility(View.VISIBLE);
            } else {
                controlsRecorder.setVisibility(View.GONE);
                controlsMain.setVisibility(View.VISIBLE);
            }
            if (isPause) {
                pauseIB.setVisibility(View.GONE);
                resumeIB.setVisibility(View.VISIBLE);
            } else {
                pauseIB.setVisibility(View.VISIBLE);
                resumeIB.setVisibility(View.GONE);
            }
        } else {
            removeBubbleView();
            if (isRecording) {
                controlsMainLeft.setVisibility(View.GONE);
                controlsRecorderLeft.setVisibility(View.VISIBLE);
            } else {
                controlsRecorderLeft.setVisibility(View.GONE);
                controlsMainLeft.setVisibility(View.VISIBLE);
            }
            if (isPause) {
                pauseLeftIB.setVisibility(View.GONE);
                resumeLeftIB.setVisibility(View.VISIBLE);
            } else {
                pauseLeftIB.setVisibility(View.VISIBLE);
                resumeLeftIB.setVisibility(View.GONE);
            }
            addBubbleView();
        }
    }

    private void collapseFloatingControls() {
        isExpand = false;
        try {
            controlsMain.setVisibility(View.GONE);
            controlsRecorder.setVisibility(View.GONE);
            controlsMainLeft.setVisibility(View.GONE);
            controlsRecorderLeft.setVisibility(View.GONE);
            removeBubbleView();
            addBubbleView();
            handler.postDelayed(runnable, TIME_DELAY);
        } catch (Exception e) {
        }
    }

    @Override
    public void onClick(View view) {
        if (isCountdown) return;
        switch (view.getId()) {
            case R.id.stop:
            case R.id.stop_left:
                stopScreenSharing();
                break;
            case R.id.pause:
            case R.id.pause_left:
                pauseScreenRecording();
                break;
            case R.id.resume:
            case R.id.resume_left:
                resumeScreenRecording();
                break;
            case R.id.screenshot:
            case R.id.screenshot_left:
                screenshot();
                break;
            case R.id.recorder:
            case R.id.recorder_left:
                handlerTimer();
                break;
            case R.id.panel:
            case R.id.panel_left:
                openPanel();
                break;
        }

        if (PrefUtils.readBooleanValue(this, getString(R.string.preference_vibrate_key), true)) {
            Vibrator vibrate = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);
            vibrate.vibrate(100);
        }
    }

    private void openPanel() {
        try {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            collapseFloatingControls();
        } catch (Exception e) {
        }
    }

    private void requestRecorder() {
        try {
            Intent intent = new Intent(this, RequestRecorderActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
        }
    }

    private void screenshot() {
        try {
            collapseFloatingControls();
            Intent intent = new Intent(this, ScreenShotActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
        }
    }

    private void resumeScreenRecording() {
        try {
            isRecording = true;
            isPause = false;
            collapseFloatingControls();
            Intent resumeIntent = new Intent(this, RecorderService.class);
            resumeIntent.setAction(Const.SCREEN_RECORDING_RESUME);
            startService(resumeIntent);
        } catch (Exception e) {
        }
    }

    private void pauseScreenRecording() {
        try {
            isRecording = true;
            isPause = true;
            collapseFloatingControls();
            Intent resumeIntent = new Intent(this, RecorderService.class);
            resumeIntent.setAction(Const.SCREEN_RECORDING_PAUSE);
            startService(resumeIntent);
        } catch (Exception e) {
        }
    }

    private void stopScreenSharing() {
        try {
            isRecording = false;
            isPause = false;
            collapseFloatingControls();
            Intent stopIntent = new Intent(this, RecorderService.class);
            stopIntent.setAction(Const.SCREEN_RECORDING_STOP);
            startService(stopIntent);
        } catch (Exception e) {
        }
    }

    public void setRecordingState(RecordingState state) {
        try {
            switch (state) {
                case PAUSED:
//                    pauseIB.setEnabled(false);
//                    resumeIB.setEnabled(true);
                    isPause = true;
                    break;
                case RECORDING:
//                    pauseIB.setEnabled(true);
//                    resumeIB.setEnabled(false);
                    isPause = false;
                    break;
                case STOPPED:
                    isRecording = false;
                    isPause = false;
                    collapseFloatingControls();
                    break;
            }
        } catch (Exception e) {
        }
    }

    @Override
    public void onDestroy() {
        ObserverUtils.getInstance().notifyObservers(new EvbStopService());
        removeBubbleView();
        removeNotification();
        ObserverUtils.getInstance().removeObserver(this);
        try {
            if (windowManager != null && mRemoveView != null) {
                windowManager.removeView(mRemoveView);
            }
        } catch (Exception e) {
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(Const.TAG, "Binding successful!");
        return binder;
    }

    @Override
    public void notifyAction(Object action) {
        if (action instanceof EvbStageRecord) {
            if (((EvbStageRecord) action).isStart) {
                handlerTimer();
            } else {
                stopScreenSharing();
            }
        }
    }

    /**
     * Binder class for binding to recording service
     */
    public class ServiceBinder extends Binder {
        FloatingControlService getService() {
            return FloatingControlService.this;
        }
    }
}
