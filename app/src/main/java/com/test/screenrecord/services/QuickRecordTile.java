package com.test.screenrecord.services;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import com.test.screenrecord.common.Const;
import com.test.screenrecord.R;
import com.test.screenrecord.ui.activities.HomeActivity;

@TargetApi(24)
public class QuickRecordTile extends TileService {
    private boolean isTileActive;

    @Override
    public void onStartListening() {
        super.onStartListening();
        isTileActive = isServiceRunning(RecorderService.class);
        changeTileState();
    }

    @Override
    public void onClick() {
        Tile tile = getQsTile();
        isTileActive = !(tile.getState() == Tile.STATE_ACTIVE);
        changeTileState();
        if (isTileActive) {
            startActivity(
                    new Intent(this, HomeActivity.class)
                            .setAction(getString(R.string.app_shortcut_action))
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            );
        } else {
            startService(new Intent(this, RecorderService.class).setAction(Const.SCREEN_RECORDING_STOP));
        }
        isTileActive = !isTileActive;
    }

    private void changeTileState() {
        Tile tile = super.getQsTile();
        int activeState = isTileActive ?
                Tile.STATE_ACTIVE : Tile.STATE_INACTIVE;

        if (!isTileActive)
            tile.setLabel(getString(R.string.quick_settings_tile_start_title));
        else
            tile.setLabel(getString(R.string.quick_settings_tile_stop_title));

        tile.setState(activeState);
        tile.updateTile();

    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
