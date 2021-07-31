package com.test.screenrecord.ui.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;
import android.util.DisplayMetrics;
import android.view.Display;
import android.widget.Toast;

import com.test.screenrecord.BuildConfig;
import com.test.screenrecord.R;
import com.test.screenrecord.common.Const;
import com.test.screenrecord.common.Utils;
import com.test.screenrecord.services.FloatingControlService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.Buffer;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ScreenShotActivity extends Activity {

    private static final int NOTIFICATION_ID = 161;
    private int IMAGES_PRODUCED = 0;
    private String STORE_DIRECTORY;
    private int VIRTUAL_DISPLAY_FLAGS = 9;
    private int mDensity;
    private Display mDisplay;
    private int mHeight;
    private ImageReader mImageReader;
    private MediaProjection mMediaProjection;
    private MediaProjectionManager mMediaProjectionManager;
    private NotificationManager mNotificationManager;
    private int mResultCode = 0;
    private Intent mResultData = null;
    private VirtualDisplay mVirtualDisplay;
    private int mWidth;
    private DisplayMetrics metrics;

    private class ImageAvailableListener implements ImageReader.OnImageAvailableListener {
        private ImageAvailableListener() {
        }

        public void onImageAvailable(ImageReader imageReader) {
            Bitmap createBitmap = null;
            Image acquireLatestImage;
            OutputStream outputStream = null;
            try {
                acquireLatestImage = mImageReader.acquireLatestImage();
                if (acquireLatestImage != null) {
                    try {
                        Image.Plane[] planes = acquireLatestImage.getPlanes();
                        Buffer buffer = planes[0].getBuffer();
                        int pixelStride = planes[0].getPixelStride();
                        createBitmap = Bitmap.createBitmap(mWidth + ((planes[0].getRowStride() - (mWidth * pixelStride)) / pixelStride), mHeight, Bitmap.Config.ARGB_8888);
                        try {
                            createBitmap.copyPixelsFromBuffer(buffer);
                            if (acquireLatestImage != null) {
                                acquireLatestImage.close();
                            }
                            if (IMAGES_PRODUCED == 0) {
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append(STORE_DIRECTORY);
                                stringBuilder.append("/Screenshot_");
                                stringBuilder.append(getDateTime());
                                stringBuilder.append(".png");
                                String uri = stringBuilder.toString();
                                OutputStream fileOutputStream = new FileOutputStream(uri);
                                try {
                                    Bitmap.createBitmap(createBitmap, 0, 0, mWidth, mHeight).compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                                    acquireLatestImage.close();
                                    IMAGES_PRODUCED = IMAGES_PRODUCED + 1;
                                    showNotificationScreenshot(uri);
                                    stopScreenCapture();
                                    tearDownMediaProjection();
                                    StringBuilder stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append("file://");
                                    stringBuilder3.append(uri);
                                    sendBroadcast(new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE", Uri.parse(stringBuilder3.toString())));
                                    IMAGES_PRODUCED = 0;
                                    outputStream = fileOutputStream;
                                } catch (Exception e) {
                                    outputStream = fileOutputStream;
                                    try {
                                        e.printStackTrace();
                                        if (imageReader != null) {
                                            try {
                                                imageReader.close();
                                            } catch (Exception ex) {
                                                ex.printStackTrace();
                                            }
                                        }
                                        if (createBitmap != null) {
                                            createBitmap.recycle();
                                        }
                                        if (acquireLatestImage == null) {
                                            return;
                                        }
                                        acquireLatestImage.close();
                                    } catch (Throwable th2) {
                                        if (outputStream != null) {
                                            try {
                                                outputStream.close();
                                            } catch (Exception e1) {
                                                e1.printStackTrace();
                                            }
                                        }
                                        if (createBitmap != null) {
                                            createBitmap.recycle();
                                        }
                                        if (acquireLatestImage != null) {
                                            acquireLatestImage.close();
                                        }
                                    }
                                } catch (Throwable th3) {
                                    outputStream = fileOutputStream;
                                    if (outputStream != null) {
                                        outputStream.close();
                                    }
                                    if (createBitmap != null) {
                                        createBitmap.recycle();
                                    }
                                    if (acquireLatestImage != null) {
                                        acquireLatestImage.close();
                                    }
                                }
                            }
                            IMAGES_PRODUCED = IMAGES_PRODUCED + 1;
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            if (outputStream != null) {
                                outputStream.close();
                            }
                            if (createBitmap != null) {
                                createBitmap.recycle();
                            }
                            if (acquireLatestImage == null) {
                                return;
                            }
                            acquireLatestImage.close();
                        }
                    } catch (Exception ex) {
                        createBitmap = null;
                        ex.printStackTrace();
                        if (outputStream != null) {
                            outputStream.close();
                        }
                        if (createBitmap != null) {
                            createBitmap.recycle();
                        }
                        if (acquireLatestImage == null) {
                            return;
                        }
                        acquireLatestImage.close();
                    } catch (Throwable th4) {
                        createBitmap = null;
                        if (outputStream != null) {
                            outputStream.close();
                        }
                        if (createBitmap != null) {
                            createBitmap.recycle();
                        }
                        if (acquireLatestImage != null) {
                            acquireLatestImage.close();
                        }
                    }
                }
                createBitmap = null;
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
                if (createBitmap != null) {
                    createBitmap.recycle();
                }
                if (acquireLatestImage != null) {
                    acquireLatestImage.close();
                }
            } catch (Exception ex) {
                acquireLatestImage = null;
//                createBitmap = acquireLatestImage;
                ex.printStackTrace();
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
                if (createBitmap != null) {
                    createBitmap.recycle();
                }
                if (acquireLatestImage == null) {
                    return;
                }
                acquireLatestImage.close();
            }
        }
    }

    @SuppressLint("WrongConstant")
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (FloatingControlService.isCountdown) {
            finish();
            return;
        }
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 131);
                return;
            }
        }
        metrics = getResources().getDisplayMetrics();
        mDensity = metrics.densityDpi;
        mDisplay = getWindowManager().getDefaultDisplay();
        mNotificationManager = (NotificationManager) getSystemService("notification");
        mMediaProjectionManager = (MediaProjectionManager) getSystemService("media_projection");
        activeScreenCapture();
    }

    private void setUpMediaProjection() {
        if (mMediaProjection == null) {
            mMediaProjection = mMediaProjectionManager.getMediaProjection(mResultCode, mResultData);
        }
    }

    private void activeScreenCapture() {
        startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(), 100);
    }

    public void setUpVirtualDisplay() {
        Point point = new Point();
        mDisplay.getSize(point);
        mWidth = point.x;
        mHeight = point.y;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Size: ");
        stringBuilder.append(mWidth);
        stringBuilder.append(" ");
        stringBuilder.append(mHeight);
        mImageReader = ImageReader.newInstance(mWidth, mHeight, 1, 2);
        mVirtualDisplay = mMediaProjection.createVirtualDisplay(Const.APPDIR, mWidth, mHeight, mDensity, VIRTUAL_DISPLAY_FLAGS, mImageReader.getSurface(), null, null);
        IMAGES_PRODUCED = 0;
        mImageReader.setOnImageAvailableListener(new ImageAvailableListener(), null);
    }

    public String getDateTime() {
        return new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(Calendar.getInstance().getTime());
    }

    private void showNotificationScreenshot(String str) {
//        PopupDialog popupDialog = PopupDialog.newInstance(this.getApplicationContext(), str);
////        popupDialog.show();
        Utils.showDialogResult(this.getApplicationContext(),str);

        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.logo);
        Bitmap decodeFile = BitmapFactory.decodeFile(str);
        Intent intent = new Intent("android.intent.action.VIEW");
        Uri uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", new File(str));
        intent.setDataAndType(uri, "image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        PendingIntent activity = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "");
        builder.setContentIntent(activity).
                setContentTitle(getString(R.string.share_intent_notification_title_photo))
                .setContentText(getString(R.string.share_intent_notification_content_photo))
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setAutoCancel(true);
        mNotificationManager.cancel(NOTIFICATION_ID);
        if (Utils.isAndroid26()) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel notificationChannel = new NotificationChannel("my_channel_id", "NOTIFICATION_CHANNEL_NAME", importance);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.enableVibration(true);
            notificationChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            assert mNotificationManager != null;
            builder.setChannelId("my_channel_id");
            mNotificationManager.createNotificationChannel(notificationChannel);
        }
        mNotificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        try {
            finish();
        } catch (Exception e) {
        }
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        if (i == 100) {
            if (i2 != -1) {
                Toast.makeText(this, getString(R.string.permission_deny), Toast.LENGTH_SHORT).show();
                stopScreenCapture();
                tearDownMediaProjection();
                finish();
                return;
            }
            mResultData = intent;
            mResultCode = i2;
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    startCaptureScreen();
                }
            }, 250);
            finish();
        }
    }

    public void startCaptureScreen() {
        if (!(mResultCode == 0 || mResultData == null)) {
            if (mMediaProjection != null) {
                tearDownMediaProjection();
            }
            setUpMediaProjection();
            if (mMediaProjection != null) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                File file = new File(prefs.getString(getString(R.string.savelocation_key),
                        Environment.getExternalStorageDirectory()
                                + File.separator + Const.APPDIR));

                STORE_DIRECTORY = prefs.getString(getString(R.string.savelocation_key),
                        Environment.getExternalStorageDirectory()
                                + File.separator + Const.APPDIR);

                if (file.exists() || file.mkdirs()) {
                    setUpVirtualDisplay();
                } else {
                    stopScreenCapture();
                    tearDownMediaProjection();
                }
            }
        }
    }

    private void stopScreenCapture() {
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
            if (mImageReader != null) {
                mImageReader.setOnImageAvailableListener(null, null);
            }
        }
    }

    private void tearDownMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }

    public boolean isScreenshotActived() {
        return mResultCode == -1 && mResultData != null;
    }
}
