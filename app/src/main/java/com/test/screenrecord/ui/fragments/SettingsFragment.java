package com.test.screenrecord.ui.fragments;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;

import com.test.screenrecord.R;
import com.test.screenrecord.common.Const;
import com.test.screenrecord.common.PrefUtils;
import com.test.screenrecord.common.Utils;
import com.test.screenrecord.folderpicker.FolderChooserDialog;
import com.test.screenrecord.folderpicker.OnDirectorySelectedListerner;
import com.test.screenrecord.interfaces.PermissionResultListener;
import com.test.screenrecord.services.FloatingControlService;
import com.test.screenrecord.ui.activities.HomeActivity;
import com.test.screenrecord.ui.activities.ShowTouchTutsActivity;
import com.test.screenrecord.ui.dialog.AppPickerDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;

public class SettingsFragment extends Fragment
        implements PermissionResultListener, OnDirectorySelectedListerner, View.OnClickListener {

    private View mRootView;
    private SharedPreferences prefs;
    private FolderChooserDialog folderChooserDialog;
    private AppPickerDialog appPickerDialog;
    private HomeActivity activity;

    private SwitchCompat cbFloatControls, cbTouches, cbCamera, cbTargetApp, cbSavingGif, cbShark, cbVibrate;

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    String [] resEntries;
    String [] resEntryValues;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mRootView = inflater.inflate(R.layout.fragment_settings, container, false);
        mRootView.setBackgroundColor(getResources().getColor(R.color.globalWhite));
        initViews();
        initEvents();

        return mRootView;
    }


//        private void checkNativeRes() {
//
//            String [] resEntries2 = getResources().getStringArray(R.array.resolutionsArray);
//            String [] resEntryValues2 = getResources().getStringArray(R.array.resolutionValues);
//
//            String nativeRes = getNativeRes();
//            int j=0;
//            for (int i = 0; i <resEntryValues2.length ; i++) {
//                if (Integer.parseInt(nativeRes)>=Integer.parseInt(resEntries2[i])) {
//                    resEntries[j]=resEntries2[i];
//                    resEntryValues[j]=resEntryValues2[j];
//                    j++;
//                }
//
//            }
//            if(resEntries.length==0){
//                resEntries[j]=nativeRes+"P";
//                resEntryValues[j]=nativeRes;
//            }
//
//
//
//
//
//        int b= 9;
//
//
//    }

    private void initEvents() {
        mRootView.findViewById(R.id.layout_vibrate).setOnClickListener(this);
        mRootView.findViewById(R.id.layout_language).setOnClickListener(this);
        mRootView.findViewById(R.id.layout_timer).setOnClickListener(this);
        mRootView.findViewById(R.id.layout_resolution).setOnClickListener(this);
        mRootView.findViewById(R.id.layout_frams).setOnClickListener(this);
        mRootView.findViewById(R.id.layout_bit_rate).setOnClickListener(this);
        mRootView.findViewById(R.id.layout_orientation).setOnClickListener(this);
        mRootView.findViewById(R.id.layout_audio).setOnClickListener(this);
        mRootView.findViewById(R.id.layout_location).setOnClickListener(this);
        mRootView.findViewById(R.id.layout_name_format).setOnClickListener(this);
        mRootView.findViewById(R.id.layout_name_prefix).setOnClickListener(this);
        mRootView.findViewById(R.id.layout_use_float_controls).setOnClickListener(this);
        mRootView.findViewById(R.id.layout_show_touches).setOnClickListener(this);
        mRootView.findViewById(R.id.layout_camera_overlay).setOnClickListener(this);
        mRootView.findViewById(R.id.layout_enable_target_app).setOnClickListener(this);
        mRootView.findViewById(R.id.layout_choose_app).setOnClickListener(this);
        mRootView.findViewById(R.id.layout_enable_saving_in_gif).setOnClickListener(this);
        mRootView.findViewById(R.id.layout_shark).setOnClickListener(this);
        cbFloatControls.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    if (isChecked) {
                        requestSystemWindowsPermission(Const.FLOATING_CONTROLS_SYSTEM_WINDOWS_CODE);
                        Intent intent = new Intent(getActivity(), FloatingControlService.class);
                        getActivity().startService(intent);
                    } else {
                        Intent intent = new Intent(getActivity(), FloatingControlService.class);
                        getActivity().stopService(intent);
                    }
                    PrefUtils.saveBooleanValue(getActivity(), getString(R.string.preference_floating_control_key), isChecked);
                } catch (Exception e) {
                }
            }
        });
        cbTouches.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PrefUtils.saveBooleanValue(getActivity(), getString(R.string.preference_show_touch_key), isChecked);
            }
        });
        cbCamera.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    requestCameraPermission();
                    requestSystemWindowsPermission(Const.CAMERA_SYSTEM_WINDOWS_CODE);
                }
                PrefUtils.saveBooleanValue(getActivity(), getString(R.string.preference_camera_overlay_key), isChecked);
            }
        });
        cbTargetApp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PrefUtils.saveBooleanValue(getActivity(), getString(R.string.preference_enable_target_app_key), isChecked);
                mRootView.findViewById(R.id.layout_choose_app).setEnabled(isChecked);
            }
        });
        cbSavingGif.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PrefUtils.saveBooleanValue(getActivity(), getString(R.string.preference_save_gif_key), isChecked);
            }
        });
        cbShark.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PrefUtils.saveBooleanValue(getActivity(), getString(R.string.preference_shake_gesture_key), isChecked);
            }
        });
        cbVibrate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PrefUtils.saveBooleanValue(getActivity(), getString(R.string.preference_vibrate_key), isChecked);
            }
        });
    }

    private void initViews() {
        //init permission listener callback
        setPermissionListener();

        //Get Default save location from shared preference
        String defaultSaveLoc = PrefUtils.readStringValue(getActivity(), getString(R.string.savelocation_key), Environment.getExternalStorageDirectory()
                + File.separator + Const.APPDIR);

        //Get instances of all preferences
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        folderChooserDialog = new FolderChooserDialog(getActivity());
        folderChooserDialog.setOnDirectoryClickedListerner(this);
        folderChooserDialog.setCurrentDir(defaultSaveLoc);
        folderChooserDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                folderChooserDialog.onDirectorySelectedListerner.onDirectorySelected();
            }
        });

        //Init Checkbox
        cbFloatControls = mRootView.findViewById(R.id.cb_use_float_controls);
        cbTouches = mRootView.findViewById(R.id.cb_show_touches);
        cbCamera = mRootView.findViewById(R.id.cb_camera_overlay);
        cbTargetApp = mRootView.findViewById(R.id.cb_enable_target_app);
        cbSavingGif = mRootView.findViewById(R.id.cb_saving_in_gif);
        cbShark = mRootView.findViewById(R.id.cb_shark);
        cbVibrate = mRootView.findViewById(R.id.cb_vibrate);
        cbFloatControls.setChecked(PrefUtils.readBooleanValue(getActivity(), getString(R.string.preference_floating_control_key), PrefUtils.VALUE_USE_FLOAT));
        cbTouches.setChecked(PrefUtils.readBooleanValue(getActivity(), getString(R.string.preference_show_touch_key), PrefUtils.VALUE_TOUCHES));
        cbCamera.setChecked(PrefUtils.readBooleanValue(getActivity(), getString(R.string.preference_camera_overlay_key), PrefUtils.VALUE_CAMERA));
        cbTargetApp.setChecked(PrefUtils.readBooleanValue(getActivity(), getString(R.string.preference_enable_target_app_key), PrefUtils.VALUE_ENABLE_TARGET_APP));
        cbSavingGif.setChecked(PrefUtils.readBooleanValue(getActivity(), getString(R.string.preference_save_gif_key), PrefUtils.VALUE_SAVING_GIF));
        cbShark.setChecked(PrefUtils.readBooleanValue(getActivity(), getString(R.string.preference_shake_gesture_key), PrefUtils.VALUE_SHAKE));
        cbVibrate.setChecked(PrefUtils.readBooleanValue(getActivity(), getString(R.string.preference_vibrate_key), PrefUtils.VALUE_VIBRATE));
        mRootView.findViewById(R.id.layout_choose_app).setEnabled(PrefUtils.readBooleanValue(getActivity(), getString(R.string.preference_enable_target_app_key), PrefUtils.VALUE_ENABLE_TARGET_APP));

        checkAudioRecPermission();

        if (cbFloatControls.isChecked()) {
            requestSystemWindowsPermission(Const.FLOATING_CONTROLS_SYSTEM_WINDOWS_CODE);
        }

        if (cbCamera.isChecked()) {
            requestCameraPermission();
            requestSystemWindowsPermission(Const.CAMERA_SYSTEM_WINDOWS_CODE);
        }

        //Resolution
        updateResolution();

        //FPS
        updateFPS();

        //BitRate
        updateBitRate();

        //Orientation
        updateOrientation();

        //Audio
        updateAudio();

        //FileName
        updateFileName();

        //FilePrefix
        updateNamePrefix();

        //Saving location
        updateLocation();

        //Update Countdown
        updateTimer();

        //Update Language
        updateLanguage();

    }

    private void updateLocation() {
        ((TextView) mRootView.findViewById(R.id.value_location)).setText(
                PrefUtils.readStringValue(getActivity(), getString(R.string.savelocation_key), Environment.getExternalStorageDirectory()
                        + File.separator + Const.APPDIR)
        );
    }

    private void updateNamePrefix() {
        ((TextView) mRootView.findViewById(R.id.value_name_prefix)).setText(
                PrefUtils.readStringValue(getActivity(), getString(R.string.fileprefix_key), PrefUtils.VALUE_NAME_PREFIX)
        );
    }

    private void updateFileName() {
        ((TextView) mRootView.findViewById(R.id.value_name_format)).setText(
                PrefUtils.readStringValue(getActivity(), getString(R.string.fileprefix_key), PrefUtils.VALUE_NAME_PREFIX) + "_" +
                        PrefUtils.readStringValue(getActivity(), getString(R.string.filename_key), PrefUtils.VALUE_NAME_FORMAT)
        );
    }

    private void updateAudio() {
        ((TextView) mRootView.findViewById(R.id.value_audio)).setText(
                Utils.getValue(
                        getResources().getStringArray(R.array.audioSettingsEntries),
                        getResources().getStringArray(R.array.audioSettingsValues),
                        PrefUtils.readStringValue(getActivity(), getString(R.string.audiorec_key), PrefUtils.VALUE_AUDIO)
                )
        );
    }

    private void updateOrientation() {
        ((TextView) mRootView.findViewById(R.id.value_orientation)).setText(
                Utils.getValue(
                        getResources().getStringArray(R.array.orientationEntries),
                        getResources().getStringArray(R.array.orientationValues),
                        PrefUtils.readStringValue(getActivity(), getString(R.string.orientation_key), PrefUtils.VALUE_ORIENTATION)
                )
        );
    }

    private void updateBitRate() {
        ((TextView) mRootView.findViewById(R.id.value_bit_rate)).setText(
                Utils.getValue(
                        getResources().getStringArray(R.array.bitrateArray),
                        getResources().getStringArray(R.array.bitratesValue),
                        PrefUtils.readStringValue(getActivity(), getString(R.string.bitrate_key), PrefUtils.VALUE_BITRATE)
                )
        );
    }

    private void updateFPS() {
        ((TextView) mRootView.findViewById(R.id.value_frams)).setText(
                Utils.getValue(
                        getResources().getStringArray(R.array.fpsArray),
                        getResources().getStringArray(R.array.fpsArray),
                        PrefUtils.readStringValue(getActivity(), getString(R.string.fps_key), PrefUtils.VALUE_FRAMES)
                )
        );
    }

    private void checkAudioRecPermission() {
        String value = Utils.getValue(
                getResources().getStringArray(R.array.audioSettingsEntries),
                getResources().getStringArray(R.array.audioSettingsValues),
                PrefUtils.readStringValue(getActivity(), getString(R.string.audiorec_key), PrefUtils.VALUE_AUDIO)
        );
        switch (value) {
            case "1":
                requestAudioPermission(Const.AUDIO_REQUEST_CODE);
                break;
            case "2":
                requestAudioPermission(Const.INTERNAL_AUDIO_REQUEST_CODE);
                break;
        }
        updateAudio();
    }

    private void updateResolution() {
        ((TextView) mRootView.findViewById(R.id.value_resolution)).setText(
                Utils.getValue(
                        getResources().getStringArray(R.array.resolutionsArray),
                        getResources().getStringArray(R.array.resolutionValues),
                        PrefUtils.readStringValue(getActivity(), getString(R.string.res_key), PrefUtils.VALUE_RESOLUTION)
                )
        );
    }

    private ArrayList<String> buildEntries(int resID) {
        DisplayMetrics metrics = getRealDisplayMetrics();
        int deviceWidth = getScreenWidth(metrics);
        ArrayList<String> entries = new ArrayList<>(Arrays.asList(getResources().getStringArray(resID)));
        Iterator<String> entriesIterator = entries.iterator();
        while (entriesIterator.hasNext()) {
            String width = entriesIterator.next();
            if (deviceWidth < Integer.parseInt(width)) {
                entriesIterator.remove();
            }
        }
        if (!entries.contains("" + deviceWidth))
            entries.add("" + deviceWidth);
        return entries;
    }


    /**
     * Returns object of DisplayMetrics
     *
     * @return DisplayMetrics
     */
    private DisplayMetrics getRealDisplayMetrics() {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager window = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
        window.getDefaultDisplay().getRealMetrics(metrics);
        return metrics;
    }


    /**
     * Get width of screen in pixels
     *
     * @return screen width
     */
    private int getScreenWidth(DisplayMetrics metrics) {
        return metrics.widthPixels;
    }

    /**
     * Get height of screen in pixels
     *
     * @return Screen height
     */
    private int getScreenHeight(DisplayMetrics metrics) {
        return metrics.heightPixels;
    }


    /**
     * Get aspect ratio of the screen
     */
    @Deprecated
    private Const.ASPECT_RATIO getAspectRatio() {
        float screen_width = getScreenWidth(getRealDisplayMetrics());
        float screen_height = getScreenHeight(getRealDisplayMetrics());
        float aspectRatio;
        if (screen_width > screen_height) {
            aspectRatio = screen_width / screen_height;
        } else {
            aspectRatio = screen_height / screen_width;
        }
        return Const.ASPECT_RATIO.valueOf(aspectRatio);
    }

    private void setPermissionListener() {
        if (getActivity() != null && getActivity() instanceof HomeActivity) {
            activity = (HomeActivity) getActivity();
            activity.setPermissionResultListener(this);
        }
    }

    /**
     * Method to convert bits per second to MB/s
     *
     * @param bps float bitsPerSecond
     * @return float
     */
    private float bitsToMb(float bps) {
        return bps / (1024 * 1024);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void showInternalAudioWarning(boolean isR_submix) {
        int message;
        final int requestCode;
        if (isR_submix) {
            message = R.string.alert_dialog_r_submix_audio_warning_message;
            requestCode = Const.INTERNAL_R_SUBMIX_AUDIO_REQUEST_CODE;
        } else {
            message = R.string.alert_dialog_internal_audio_warning_message;
            requestCode = Const.INTERNAL_AUDIO_REQUEST_CODE;
        }
        new AlertDialog.Builder(activity)
                .setTitle(R.string.alert_dialog_internal_audio_warning_title)
                .setMessage(message)
                .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        requestAudioPermission(requestCode);

                    }
                })
                .setNegativeButton(R.string.alert_dialog_internal_audio_warning_negative_btn_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        prefs.edit().putBoolean(Const.PREFS_INTERNAL_AUDIO_DIALOG_KEY, true)
                                .apply();
                        requestAudioPermission(Const.INTERNAL_AUDIO_REQUEST_CODE);
                    }
                })
                .create()
                .show();
    }


    /**
     * Method to request android permission to record audio
     */
    public void requestAudioPermission(int requestCode) {
        if (activity != null) {
            activity.requestPermissionAudio(requestCode);
        }
    }

    /**
     * Method to request Camera permission
     */
    public void requestCameraPermission() {
        if (activity != null)
            activity.requestPermissionCamera();
    }

    private void requestSystemWindowsPermission(int code) {
        if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.requestSystemWindowsPermission(code);
        } else {
            Intent intent = new Intent(getActivity(), FloatingControlService.class);
            getActivity().startService(intent);
        }
    }

    /**
     * Show snackbar with permission Intent when the user rejects write storage permission
     */

    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.alert_permission_denied_title)
                .setMessage(R.string.alert_permission_denied_message)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (activity != null) {
                            activity.requestPermissionStorage();
                        }
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                })
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setCancelable(false)
                .create().show();
    }

    @Override
    public void onPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case Const.EXTDIR_REQUEST_CODE:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_DENIED)) {
                    Log.d(Const.TAG, "Storage permission denied. Requesting again");
                    mRootView.findViewById(R.id.layout_location).setEnabled(false);
                    showPermissionDeniedDialog();
                } else if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    mRootView.findViewById(R.id.layout_location).setEnabled(true);
                }
                return;
            case Const.AUDIO_REQUEST_CODE:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Log.d(Const.TAG, "Record audio permission granted.");
                    PrefUtils.saveStringValue(getActivity(), getString(R.string.audiorec_key), "1");
                } else {
                    Log.d(Const.TAG, "Record audio permission denied");
                    PrefUtils.saveStringValue(getActivity(), getString(R.string.audiorec_key), "0");
                }
                updateAudio();
                return;
            case Const.INTERNAL_AUDIO_REQUEST_CODE:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Log.d(Const.TAG, "Record audio permission granted.");
                    PrefUtils.saveStringValue(getActivity(), getString(R.string.audiorec_key), "2");
                } else {
                    Log.d(Const.TAG, "Record audio permission denied");
                    PrefUtils.saveStringValue(getActivity(), getString(R.string.audiorec_key), "0");
                }
                updateAudio();
                return;
            case Const.INTERNAL_R_SUBMIX_AUDIO_REQUEST_CODE:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Log.d(Const.TAG, "Record audio permission granted.");
                    PrefUtils.saveStringValue(getActivity(), getString(R.string.audiorec_key), "3");
                } else {
                    Log.d(Const.TAG, "Record audio permission denied");
                    PrefUtils.saveStringValue(getActivity(), getString(R.string.audiorec_key), "0");
                }
                updateAudio();
                return;
            case Const.FLOATING_CONTROLS_SYSTEM_WINDOWS_CODE:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Log.d(Const.TAG, "System Windows permission granted");
                    cbFloatControls.setChecked(true);
                    Intent intent = new Intent(getActivity(), FloatingControlService.class);
                    getActivity().startService(intent);
                } else {
                    Log.d(Const.TAG, "System Windows permission denied");
                    cbFloatControls.setChecked(false);
                }
                return;
            case Const.CAMERA_SYSTEM_WINDOWS_CODE:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Log.d(Const.TAG, "System Windows permission granted");
                    cbCamera.setChecked(true);

                } else {
                    Log.d(Const.TAG, "System Windows permission denied");
                    cbCamera.setChecked(false);
                }
                return;
            case Const.CAMERA_REQUEST_CODE:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Log.d(Const.TAG, "System Windows permission granted");
                    requestSystemWindowsPermission(Const.CAMERA_SYSTEM_WINDOWS_CODE);
                } else {
                    Log.d(Const.TAG, "System Windows permission denied");
                    cbCamera.setChecked(false);
                }
            default:
                Log.d(Const.TAG, "Unknown permission request with request code: " + requestCode);
        }
    }

    @Override
    public void onDirectorySelected() {
        Log.d(Const.TAG, "In settings fragment");
        if (getActivity() != null && getActivity() instanceof HomeActivity) {
            ((HomeActivity) getActivity()).onDirectoryChanged();
        }
        updateLocation();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.layout_vibrate:
                cbVibrate.setChecked(!cbVibrate.isChecked());
                break;
            case R.id.layout_language:
                openLanguage();
                break;
            case R.id.layout_timer:
                openTimer();
                break;
            case R.id.layout_resolution:
                openResolutionDialog();
                break;
            case R.id.layout_frams:
                openFramesDialog();
                break;
            case R.id.layout_bit_rate:
                openBitRate();
                break;
            case R.id.layout_orientation:
                openOrientationDialog();
                break;
            case R.id.layout_audio:
                openAudioDialog();
                break;
            case R.id.layout_location:
                folderChooserDialog.show();
                break;
            case R.id.layout_name_format:
                openNameFormat();
                break;
            case R.id.layout_name_prefix:
                openNamePrefix();
                break;
            case R.id.layout_use_float_controls:
                cbFloatControls.setChecked(!cbFloatControls.isChecked());
                break;
            case R.id.layout_show_touches:
//                cbTouches.setChecked(!cbTouches.isChecked());
                Intent intent = new Intent(getActivity(), ShowTouchTutsActivity.class);
                startActivity(intent);
                break;

            case R.id.layout_camera_overlay:
                cbCamera.setChecked(!cbCamera.isChecked());
                break;
            case R.id.layout_enable_target_app:
                cbTargetApp.setChecked(!cbTargetApp.isChecked());
                break;
            case R.id.layout_choose_app:
                appPickerDialog = new AppPickerDialog(getActivity());
                appPickerDialog.show();
                break;
            case R.id.layout_enable_saving_in_gif:
                cbSavingGif.setChecked(!cbSavingGif.isChecked());
                break;
            case R.id.layout_shark:
                cbShark.setChecked(!cbShark.isChecked());
                break;
        }
    }

    private void openTimer() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.preference_timer_title);
        String[] items = getResources().getStringArray(R.array.timerArray);
        int pos = Utils.getPosition(getResources().getStringArray(R.array.timer), PrefUtils.readStringValue(getActivity(), getString(R.string.timer_key), PrefUtils.VALUE_TIMER));
        builder.setSingleChoiceItems(items, pos,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PrefUtils.saveStringValue(getActivity(), getString(R.string.timer_key), getResources().getStringArray(R.array.timer)[which]);
                        dialog.dismiss();
                    }
                });

        String positiveText = getString(android.R.string.ok);
        builder.setPositiveButton(positiveText,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                updateTimer();
            }
        });
        dialog.show();
    }

    private void updateTimer() {
        ((TextView) mRootView.findViewById(R.id.value_timer)).setText(
                Utils.getValue(
                        getResources().getStringArray(R.array.timerArray),
                        getResources().getStringArray(R.array.timer),
                        PrefUtils.readStringValue(getActivity(), getString(R.string.timer_key), PrefUtils.VALUE_TIMER)
                )
        );
    }

    private void openLanguage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.preference_language_title);
        String[] items = getResources().getStringArray(R.array.language);
        int pos = Utils.getPosition(getResources().getStringArray(R.array.languageValue), PrefUtils.readStringValue(getActivity(), getString(R.string.language_key), PrefUtils.VALUE_LANGUAGE));
        builder.setSingleChoiceItems(items, pos,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PrefUtils.saveStringValue(getActivity(), getString(R.string.language_key), getResources().getStringArray(R.array.languageValue)[which]);
                        setLocale(getResources().getStringArray(R.array.languageValue)[which]);
                        dialog.dismiss();
                    }
                });

        String positiveText = getString(android.R.string.ok);
        builder.setPositiveButton(positiveText,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                updateLanguage();
            }
        });
        dialog.show();
    }

    public void setLocale(String lang) {
        Locale myLocale = new Locale(lang);
        Resources res = getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.locale = myLocale;
        res.updateConfiguration(conf, dm);
        Intent refresh = new Intent(getActivity(), HomeActivity.class);
        startActivity(refresh);
        getActivity().finish();
    }

    private void updateLanguage() {
        ((TextView) mRootView.findViewById(R.id.value_language)).setText(
                Utils.getValue(
                        getResources().getStringArray(R.array.language),
                        getResources().getStringArray(R.array.languageValue),
                        PrefUtils.readStringValue(getActivity(), getString(R.string.language_key), PrefUtils.VALUE_LANGUAGE)
                )
        );
    }

    private void openNamePrefix() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.preference_filename_prefix_title));
        final EditText input = new EditText(getActivity());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        int margin = Utils.convertDpToPixel(20, getActivity());
        lp.setMargins(margin, margin, margin, margin);
        input.setLayoutParams(lp);
        input.setText(PrefUtils.readStringValue(getActivity(), getString(R.string.fileprefix_key), PrefUtils.VALUE_NAME_PREFIX));
        input.setSelection(input.getText().toString().length());
        LinearLayout layout = new LinearLayout(getActivity());
        layout.addView(input);
        builder.setView(layout);
        String positiveText = getString(android.R.string.ok);
        builder.setPositiveButton(positiveText,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PrefUtils.saveStringValue(getActivity(), getString(R.string.fileprefix_key), input.getText().toString());
                        dialog.dismiss();
                    }
                });

        String negativeText = getString(android.R.string.cancel);
        builder.setNegativeButton(negativeText,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                updateNamePrefix();
            }
        });
        dialog.show();
    }

    private void openNameFormat() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.preference_filename_format_title);
        String[] items = getResources().getStringArray(R.array.filename);
        int pos = Utils.getPosition(getResources().getStringArray(R.array.filename), PrefUtils.readStringValue(getActivity(), getString(R.string.filename_key), PrefUtils.VALUE_NAME_FORMAT));
        builder.setSingleChoiceItems(items, pos,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PrefUtils.saveStringValue(getActivity(), getString(R.string.filename_key), getResources().getStringArray(R.array.filename)[which]);
                        dialog.dismiss();
                    }
                });

        String negativeText = getString(android.R.string.cancel);
        builder.setNegativeButton(negativeText,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                updateFileName();
            }
        });
        dialog.show();
    }

    private void openAudioDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.preference_audio_record_title);
        String[] items = getResources().getStringArray(R.array.audioSettingsEntries);
        int pos = Utils.getPosition(getResources().getStringArray(R.array.audioSettingsValues), PrefUtils.readStringValue(getActivity(), getString(R.string.audiorec_key), PrefUtils.VALUE_AUDIO));
        builder.setSingleChoiceItems(items, pos,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PrefUtils.saveStringValue(getActivity(), getString(R.string.audiorec_key), getResources().getStringArray(R.array.audioSettingsValues)[which]);
                        checkAudioRecPermission();
                        dialog.dismiss();
                    }
                });

        String negativeText = getString(android.R.string.cancel);
        builder.setNegativeButton(negativeText,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                updateAudio();
            }
        });
        dialog.show();
    }

    private void openOrientationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.preference_orientation_title);
        String[] items = getResources().getStringArray(R.array.orientationEntries);
        int pos = Utils.getPosition(getResources().getStringArray(R.array.orientationValues), PrefUtils.readStringValue(getActivity(), getString(R.string.orientation_key), PrefUtils.VALUE_ORIENTATION));
        builder.setSingleChoiceItems(items, pos,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PrefUtils.saveStringValue(getActivity(), getString(R.string.orientation_key), getResources().getStringArray(R.array.orientationValues)[which]);
                        dialog.dismiss();
                    }
                });

        String negativeText = getString(android.R.string.cancel);
        builder.setNegativeButton(negativeText,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                updateOrientation();
            }
        });
        dialog.show();
    }

    private void openBitRate() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.preference_bit_title);
        String[] items = getResources().getStringArray(R.array.bitrateArray);
        int pos = Utils.getPosition(getResources().getStringArray(R.array.bitratesValue), PrefUtils.readStringValue(getActivity(), getString(R.string.bitrate_key), PrefUtils.VALUE_BITRATE));
        builder.setSingleChoiceItems(items, pos,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PrefUtils.saveStringValue(getActivity(), getString(R.string.bitrate_key), getResources().getStringArray(R.array.bitratesValue)[which]);
                        dialog.dismiss();
                    }
                });

        String negativeText = getString(android.R.string.cancel);
        builder.setNegativeButton(negativeText,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                updateBitRate();
            }
        });
        dialog.show();
    }

    private void openFramesDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.preference_fps_title);
        String[] items = getResources().getStringArray(R.array.fpsArray);
        int pos = Utils.getPosition(getResources().getStringArray(R.array.fpsArray), PrefUtils.readStringValue(getActivity(), getString(R.string.fps_key), PrefUtils.VALUE_FRAMES));
        builder.setSingleChoiceItems(items, pos,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PrefUtils.saveStringValue(getActivity(), getString(R.string.fps_key), getResources().getStringArray(R.array.fpsArray)[which]);
                        dialog.dismiss();
                    }
                });

        String negativeText = getString(android.R.string.cancel);
        builder.setNegativeButton(negativeText,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                updateFPS();
            }
        });
        dialog.show();
    }

    private void openResolutionDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.preference_resolution_title);
        final String[] items = getResources().getStringArray(R.array.resolutionsArray);
        final int pos = Utils.getPosition(getResources().getStringArray(R.array.resolutionValues), PrefUtils.readStringValue(getActivity(), getString(R.string.res_key), PrefUtils.VALUE_RESOLUTION));
        builder.setSingleChoiceItems(items, pos,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (Integer.parseInt(getNativeRes())<Integer.parseInt(getResources().getStringArray(R.array.resolutionValues)[which])){
                            Toast.makeText(getActivity(),getString(R.string.notsupport)+items[which],Toast.LENGTH_LONG).show();
                        }else{
                            PrefUtils.saveStringValue(getActivity(), getString(R.string.res_key), getResources().getStringArray(R.array.resolutionValues)[which]);
                            dialog.dismiss();
                        }

                    }
                });

        String negativeText = getString(android.R.string.cancel);
        builder.setNegativeButton(negativeText,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                updateResolution();
            }
        });
        dialog.show();
    }

//    private CharSequence[] getResolutionEntriesValues() {
//
//        ArrayList<String> entrieValues = buildEntries(R.array.resolutionValues);
//
//        String[] entriesArray = new String[entrieValues.size()];
//        return entrieValues.toArray(entriesArray);
//    }
////    private void updateScreenAspectRatio() {
////        CharSequence[] entriesValues = getResolutionEntriesValues();
////        res.setEntries(getResolutionEntries(entriesValues));
////        //res.setEntries(entriesValues);
////        res.setEntryValues(entriesValues);
////    }
//    private CharSequence[] getResolutionEntries(CharSequence[] entriesValues) {
//        ArrayList<String> entries = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.resolutionsArray)));
//        ArrayList<String> newEntries = new ArrayList<>();
//        for (CharSequence values : entriesValues) {
//            Log.d(Const.TAG, "res entries:" + values.toString());
//            for (String entry : entries) {
//                if (entry.contains(values))
//                    newEntries.add(entry);
//            }
//            Log.d(Const.TAG, "res entries: split " + values.toString().split("P")[0] + " val: ");
//        }
//        Log.d(Const.TAG, "res entries" + newEntries.toString());
//        String[] entriesArray = new String[newEntries.size()];
//        return newEntries.toArray(entriesArray);
//    }
    private String getNativeRes() {
        DisplayMetrics metrics = getRealDisplayMetrics();
        return String.valueOf(getScreenWidth(metrics));
    }




}
