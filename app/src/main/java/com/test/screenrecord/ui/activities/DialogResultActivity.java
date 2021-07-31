package com.test.screenrecord.ui.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.ads.control.AdmobHelp;
import com.test.screenrecord.R;
import com.test.screenrecord.common.Const;
import com.test.screenrecord.common.Utils;

import java.io.File;



public class DialogResultActivity extends AppCompatActivity implements View.OnClickListener {
    @Override
    public void onClick(View view) {
        try {
            switch (view.getId()) {
                case R.id.btn_cancel:
                    finish();
                    break;
                case R.id.thumbnail:
                case R.id.play:
                    closeNotify();
                    Intent openVideoIntent = new Intent();
                    openVideoIntent.setAction(Intent.ACTION_VIEW)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK)
                            .setDataAndType(
                                    fileUri,
                                    DialogResultActivity.this.getContentResolver().getType(fileUri));
                    DialogResultActivity.this.startActivity(openVideoIntent);
                    break;
                case R.id.share:
                    Intent shareIntent = new Intent()
                            .setAction(Intent.ACTION_SEND)
                            .putExtra(Intent.EXTRA_STREAM, fileUri)
                            .setType(isVideo ? "video/mp4" : "image/*");
                    shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    DialogResultActivity.this.startActivity(shareIntent);
                    break;
                case R.id.edit:
                    closeNotify();
                    Intent editIntent = new Intent(DialogResultActivity.this, EditVideoActivity.class);
                    editIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    editIntent.putExtra(Const.VIDEO_EDIT_URI_KEY, FILEPATH);
                    DialogResultActivity.this.startActivity(editIntent);
                    break;
                case R.id.delete:
                    try {
                        new File(FILEPATH).delete();
                        closeNotify();
                    } catch (Exception e) {
                    }
                    break;
            }
            finish();
        } catch (Exception e) {
        }
    }

    private ImageView imgThumbnail;
    private String FILEPATH;
    private Uri fileUri;
    private boolean isVideo = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_popup);
        intData();
        intView();
        intEvent();

        AdmobHelp.getInstance().loadNative(this);
    }
    public void intView(){
        try {
            if (FILEPATH.endsWith(".mp4")) {
                isVideo = true;
                sendBroadcast(new Intent(Const.UPDATE_UI));
            } else {
                sendBroadcast(new Intent(Const.UPDATE_UI_IMAGE));
                isVideo = false;
                findViewById(R.id.play).setVisibility(View.GONE);
                findViewById(R.id.edit).setVisibility(View.GONE);
                ((ImageView) findViewById(R.id.thumbnail)).setScaleType(ImageView.ScaleType.CENTER_CROP);
                ((TextView) findViewById(R.id.txt_title)).setText(getString(R.string.screenshot_finished));
            }
            fileUri = FileProvider.getUriForFile(
                    this, this.getPackageName() + ".provider",
                    new File(FILEPATH));

            Bitmap thumb = isVideo ?
                    Utils.getBitmapVideo(this, new File(FILEPATH)) :
                    BitmapFactory.decodeFile(FILEPATH);
            imgThumbnail = findViewById(R.id.thumbnail);
            imgThumbnail.setImageBitmap(thumb);
        } catch (Exception e) {
        }
    }
    public void intData(){
        Intent intent = getIntent();
        FILEPATH = intent.getStringExtra("path");
    }
    public void intEvent(){
        findViewById(R.id.btn_cancel).setOnClickListener(this);
        findViewById(R.id.play).setOnClickListener(this);
        findViewById(R.id.share).setOnClickListener(this);
        findViewById(R.id.edit).setOnClickListener(this);
        findViewById(R.id.delete).setOnClickListener(this);
        findViewById(R.id.thumbnail).setOnClickListener(this);
    }
    private void closeNotify() {
        try {
            NotificationManager nMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nMgr.cancel(Const.SCREEN_RECORDER_SHARE_NOTIFICATION_ID);
        } catch (Exception e) {
        }
    }
}
