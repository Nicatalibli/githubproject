package com.test.screenrecord.common;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;

import com.test.screenrecord.ui.activities.DialogResultActivity;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Utils {
    public static boolean isAndroid26() {
        return Build.VERSION.SDK_INT >= 26;
    }

    public static int convertDpToPixel(float dp, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        int px = (int) (dp * ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }

    public static int convertPixelsToDp(float px, Context context) {
        return (int) (px / ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public static void openURL(Activity context, String url) {
        try {
            context.getApplicationContext().startActivity(new Intent("android.intent.action.VIEW", Uri.parse(url)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showDialogResult(Context c, String path){
        Intent intent = new Intent(c, DialogResultActivity.class);
        intent.putExtra("path", path);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        c.startActivity(intent);
    }
    public static Calendar toCalendar(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    public static int getScreenWidth(@NonNull Context context) {
        Point size = new Point();
        ((Activity) context).getWindowManager().getDefaultDisplay().getSize(size);
        return size.x;
    }

    public static int getScreenHeight(@NonNull Context context) {
        Point size = new Point();
        ((Activity) context).getWindowManager().getDefaultDisplay().getSize(size);
        return size.y;
    }

    public static boolean isInLandscapeMode(@NonNull Context context) {
        boolean isLandscape = false;
        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            isLandscape = true;
        }
        return isLandscape;
    }

    public static void createDir() {
        File appDir = new File(Environment.getExternalStorageDirectory() + File.separator + Const.APPDIR);
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) && !appDir.isDirectory()) {
            appDir.mkdirs();
        }
    }

    public static void createDirEdited() {
        File appDir = new File(Environment.getExternalStorageDirectory() + File.separator + Const.APPDIR + File.separator + Const.FOLDER_EDITED);
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) && !appDir.isDirectory()) {
            appDir.mkdirs();
        }
    }

    public static boolean isServiceRunning(Class<?> serviceClass, Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static String generateSectionTitle(Date date) {
        Calendar sDate = toCalendar(new Date().getTime());
        Calendar eDate = toCalendar(date.getTime());

        long milis1 = sDate.getTimeInMillis();
        long milis2 = eDate.getTimeInMillis();

        int dayDiff = (int) Math.abs((milis2 - milis1) / (24 * 60 * 60 * 1000));

        int yearDiff = sDate.get(Calendar.YEAR) - eDate.get(Calendar.YEAR);

        if (yearDiff == 0) {
            switch (dayDiff) {
                case 0:
                    return "Today";
                case 1:
                    return "Yesterday";
                default:
                    SimpleDateFormat format = new SimpleDateFormat("EEEE, dd MMM", Locale.getDefault());
                    return format.format(date);
            }
        } else {
            SimpleDateFormat format = new SimpleDateFormat("EEEE, dd MMM YYYY", Locale.getDefault());
            return format.format(date);
        }
    }

    public static String getValue(String[] values, String[] keys, String key) {
        for (int i = 0; i < keys.length; i++) {
            if (keys[i].equalsIgnoreCase(key)) {
                return values[i];
            }
        }
        return values[0];
    }

    public static int getPosition(String[] keys, String key) {
        for (int i = 0; i < keys.length; i++) {
            if (keys[i].equalsIgnoreCase(key)) {
                return i;
            }
        }
        return 0;
    }

    public static boolean isAndroid23() {
        return Build.VERSION.SDK_INT >= 23;
    }

    public static Bitmap getBitmapVideo(Context context, File file) {
        ContentResolver resolver = context.getApplicationContext().getContentResolver();
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
        return null;
    }

    public static void setEnableTouch(Context context, int isEnable) {
//        Settings.System.putInt(context.getApplicationContext().getContentResolver(), "show_touches", isEnable);
    }

    public static String getAppUrl(Context context) {
        return "https://play.google.com/store/apps/details?id=" + context.getPackageName();
    }
}
