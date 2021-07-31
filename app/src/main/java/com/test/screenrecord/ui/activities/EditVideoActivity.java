package com.test.screenrecord.ui.activities;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.test.screenrecord.R;
import com.test.screenrecord.common.Const;
import com.test.screenrecord.videoTrimmer.K4LVideoTrimmer;
import com.test.screenrecord.videoTrimmer.interfaces.OnTrimVideoListener;

import java.io.File;

public class EditVideoActivity extends AppCompatActivity implements OnTrimVideoListener {
    private ProgressDialog saveprogress;
    private Uri videoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_video);
        saveprogress = new ProgressDialog(EditVideoActivity.this);
        try {
            if (!getIntent().hasExtra(Const.VIDEO_EDIT_URI_KEY)) {
                Toast.makeText(this, getResources().getString(R.string.video_not_found), Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            File directory = new File(prefs.getString(getString(R.string.savelocation_key),
                    Environment.getExternalStorageDirectory()
                            + File.separator + Const.APPDIR) + File.separator );
//            if (!directory.exists()) {
//                Utils.createDirEdited();
//                Log.d(Const.TAG, "Directory missing! Creating dir");
//            }

            videoUri = Uri.parse(getIntent().getStringExtra(Const.VIDEO_EDIT_URI_KEY));

            if (!new File(videoUri.getPath()).exists()) {
                Toast.makeText(this, getResources().getString(R.string.video_not_found), Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            K4LVideoTrimmer videoTrimmer = findViewById(R.id.videoTimeLine);

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            //use one of overloaded setDataSource() functions to set your data source
            retriever.setDataSource(this, videoUri);
            String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            int timeInMins = (((int) Long.parseLong(time)) / 1000) + 1000;
            Log.d(Const.TAG, timeInMins + "");

            File video = new File(videoUri.getPath());

            videoTrimmer.setOnTrimVideoListener(this);
            videoTrimmer.setVideoURI(videoUri);
            videoTrimmer.setMaxDuration(timeInMins);
            Log.d(Const.TAG, "Edited file save name: " + video.getAbsolutePath());
            videoTrimmer.setDestinationPath(prefs.getString(getString(R.string.savelocation_key),
                    Environment.getExternalStorageDirectory()
                            + File.separator + Const.APPDIR)  );
        } catch (Exception e) {
        }
    }

    @Override
    public void onTrimStarted() {

    }

    @Override
    public void getResult(Uri uri) {
        try {

            Log.d(Const.TAG, "Test link ne: "+uri.getPath());
            indexFile(uri.getPath());

            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    saveprogress.setMessage("Please wait while the video is being saved");
                    saveprogress.setTitle("Please wait");
                    saveprogress.setIndeterminate(true);
                    saveprogress.show();
                }
            });
        } catch (Exception e) {
        }
    }

    private void showActionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.dialog_title_delete_old_file));

        String positiveText = getString(android.R.string.yes);
        builder.setPositiveButton(positiveText,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        File file = new File(videoUri.getPath());
                        if (file.exists()) {
                            file.delete();
                        }
                        dialog.dismiss();
                    }
                });

        String negativeText = getString(android.R.string.no);
        builder.setNegativeButton(negativeText,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                finish();
            }
        });
        dialog.show();
    }

    @Override
    public void cancelAction() {
        finish();
    }

    @Override
    public void onError(String message) {

    }

    private void indexFile(final String SAVEPATH) {
        File mFile = new File(SAVEPATH);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            final Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            final Uri contentUri = Uri.fromFile(mFile);
            scanIntent.setData(contentUri);
            sendBroadcast(scanIntent);
        } else {
            final Intent intent = new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory()));
            sendBroadcast(intent);
        }
        saveprogress.cancel();
        setResult(Const.VIDEO_EDIT_RESULT_CODE);
        finish();



//        try {
//
//            //Create a new ArrayList and add the newly created video file path to it
//            ArrayList<String> toBeScanned = new ArrayList<>();
//            toBeScanned.add(SAVEPATH);
//            String[] toBeScannedStr = new String[toBeScanned.size()];
//            toBeScannedStr = toBeScanned.toArray(toBeScannedStr);
//
//            //Request MediaScannerConnection to scan the new file and index it
//            MediaScannerConnection.scanFile(this, toBeScannedStr, null, new MediaScannerConnection.OnScanCompletedListener() {
//
//                @Override
//                public void onScanCompleted(String path, Uri uri) {
//                    EditVideoActivity.this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.fromFile(new File(SAVEPATH))));
//                    Log.i(Const.TAG, "SCAN COMPLETED: " + path);
//                    saveprogress.cancel();
//                    setResult(Const.VIDEO_EDIT_RESULT_CODE);
//                    finish();
//                }
//            });
//        } catch (Exception e) {
//        }
    }

}
