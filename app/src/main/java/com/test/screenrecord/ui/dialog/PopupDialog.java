package com.test.screenrecord.ui.dialog;

import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.test.screenrecord.R;
import com.test.screenrecord.common.Const;
import com.test.screenrecord.common.Utils;
import com.test.screenrecord.ui.activities.EditVideoActivity;

import java.io.File;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

public class PopupDialog extends Dialog implements View.OnClickListener {

    private ImageView imgThumbnail;
    private String FILEPATH;
    private Uri fileUri;
    private boolean isVideo = true;

     static Context mContext=null;
    public PopupDialog(@NonNull Context context) {
        super(context);
    }

    public static PopupDialog newInstance(Context context, String filepath) {
        mContext = context;
        PopupDialog dialog = new PopupDialog(context);
        dialog.setFILEPATH(filepath);
        return dialog;
    }

    public void setFILEPATH(String filepath) {
        this.FILEPATH = filepath;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            getWindow().setType(WindowManager.LayoutParams.TYPE_PHONE);
        } else {
            getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        }
        setContentView(R.layout.dialog_popup);
        setCanceledOnTouchOutside(true);
        initViews();
        initEvents();
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.dialog_popup,null);


    }

    private void initEvents() {
        findViewById(R.id.btn_cancel).setOnClickListener(this);
        findViewById(R.id.play).setOnClickListener(this);
        findViewById(R.id.share).setOnClickListener(this);
        findViewById(R.id.edit).setOnClickListener(this);
        findViewById(R.id.delete).setOnClickListener(this);
        findViewById(R.id.thumbnail).setOnClickListener(this);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        cancel();
    }

    private void initViews() {

        try {
            if (FILEPATH.endsWith(".mp4")) {
                isVideo = true;
                mContext.sendBroadcast(new Intent(Const.UPDATE_UI));
            } else {
                mContext.sendBroadcast(new Intent(Const.UPDATE_UI_IMAGE));
                isVideo = false;
                findViewById(R.id.play).setVisibility(View.GONE);
                findViewById(R.id.edit).setVisibility(View.GONE);
                ((ImageView) findViewById(R.id.thumbnail)).setScaleType(ImageView.ScaleType.CENTER_CROP);
                ((TextView) findViewById(R.id.txt_title)).setText(getContext().getString(R.string.screenshot_finished));
            }
            fileUri = FileProvider.getUriForFile(
                    getContext(), this.getContext().getPackageName() + ".provider",
                    new File(FILEPATH));

            Bitmap thumb = isVideo ?
                    Utils.getBitmapVideo(getContext(), new File(FILEPATH)) :
                    BitmapFactory.decodeFile(FILEPATH);
            imgThumbnail = findViewById(R.id.thumbnail);
            imgThumbnail.setImageBitmap(thumb);
        } catch (Exception e) {
        }
    }

    @Override
    public void onClick(View v) {
        try {
            switch (v.getId()) {
                case R.id.btn_cancel:
                    cancel();
                    break;
                case R.id.thumbnail:
                case R.id.play:
                    closeNotify();
                    Intent openVideoIntent = new Intent();
                    openVideoIntent.setAction(Intent.ACTION_VIEW)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK)
                            .setDataAndType(
                                    fileUri,
                                    getContext().getContentResolver().getType(fileUri));
                    getContext().startActivity(openVideoIntent);
                    break;
                case R.id.share:
                    Intent shareIntent = new Intent()
                            .setAction(Intent.ACTION_SEND)
                            .putExtra(Intent.EXTRA_STREAM, fileUri)
                            .setType(isVideo ? "video/mp4" : "image");
                    getContext().startActivity(shareIntent);
                    break;
                case R.id.edit:
                    closeNotify();
                    Intent editIntent = new Intent(getContext(), EditVideoActivity.class);
                    editIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    editIntent.putExtra(Const.VIDEO_EDIT_URI_KEY, FILEPATH);
                    getContext().startActivity(editIntent);
                    break;
                case R.id.delete:
                    try {
                        new File(FILEPATH).delete();
                        closeNotify();
                    } catch (Exception e) {
                    }
                    break;
            }
            cancel();
        } catch (Exception e) {
        }
    }

    private void closeNotify() {
        try {
            NotificationManager nMgr = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
            nMgr.cancel(Const.SCREEN_RECORDER_SHARE_NOTIFICATION_ID);
        } catch (Exception e) {
        }
    }
}
