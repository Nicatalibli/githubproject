package com.test.screenrecord.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.test.screenrecord.common.Const;
import com.test.screenrecord.services.FloatingControlService;
import com.test.screenrecord.services.RecorderService;

public class RecorderActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            if (FloatingControlService.isCountdown) {
                finish();
                return;
            }
            switch (getIntent().getAction()) {
                case Const.SCREEN_RECORDING_DESTROY:
                    Intent desIntent = new Intent(this, FloatingControlService.class);
                    desIntent.setAction(Const.SCREEN_RECORDING_DESTROY);
                    startService(desIntent);
                    break;
                case Const.SCREEN_RECORDING_START_FROM_NOTIFY:
                    Intent startIntent = new Intent(this, FloatingControlService.class);
                    startIntent.setAction(Const.SCREEN_RECORDING_START_FROM_NOTIFY);
                    startService(startIntent);
                    break;
                case Const.SCREEN_RECORDING_STOP:
                    Intent stopIntent = new Intent(this, RecorderService.class);
                    stopIntent.setAction(Const.SCREEN_RECORDING_STOP);
                    startService(stopIntent);
                    break;
                case Const.SCREEN_RECORDING_PAUSE:
                    Intent pauseIntent = new Intent(this, RecorderService.class);
                    pauseIntent.setAction(Const.SCREEN_RECORDING_PAUSE);
                    startService(pauseIntent);
                    break;
                case Const.SCREEN_RECORDING_RESUME:
                    Log.e("status", "resume click");
                    Intent resumeIntent = new Intent(this, RecorderService.class);
                    resumeIntent.setAction(Const.SCREEN_RECORDING_RESUME);
                    startService(resumeIntent);
                    break;
            }
        } catch (Exception e) {
        }
        finish();
    }
}
