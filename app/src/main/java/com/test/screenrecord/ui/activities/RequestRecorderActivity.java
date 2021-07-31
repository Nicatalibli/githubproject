package com.test.screenrecord.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.test.screenrecord.R;
import com.test.screenrecord.common.Const;
import com.test.screenrecord.common.Utils;
import com.test.screenrecord.services.FloatingControlService;
import com.test.screenrecord.services.RecorderService;

public class RequestRecorderActivity extends AppCompatActivity {
    private MediaProjectionManager mProjectionManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        if (!Utils.isServiceRunning(RecorderService.class, this)) {
            mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            startActivityForResult(mProjectionManager.createScreenCaptureIntent(), Const.SCREEN_RECORD_REQUEST_CODE);
//        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            if (resultCode == RESULT_CANCELED && requestCode == Const.SCREEN_RECORD_REQUEST_CODE) {
                Toast.makeText(this, getString(R.string.screen_recording_permission_denied), Toast.LENGTH_SHORT).show();
                this.finish();
                return;
            }

            FloatingControlService.isRecording = true;
            Intent recorderService = new Intent(this, RecorderService.class);
            recorderService.setAction(Const.SCREEN_RECORDING_START);
            recorderService.putExtra(Const.RECORDER_INTENT_DATA, data);
            recorderService.putExtra(Const.RECORDER_INTENT_RESULT, resultCode);
            startService(recorderService);
            finish();
        } catch (Exception e) {
            finish();
        }
    }
}
