package com.test.screenrecord.common;

import java.util.HashMap;
import java.util.Map;

public class Const {

    public static final String FOLDER_EDITED = "edited";
    public static final String UPDATE_UI = "update_ui";
    public static final String UPDATE_UI_IMAGE = "update_ui_image";
    public enum ASPECT_RATIO {
        AR16_9(1.7777778f), AR18_9(2f);

        private static Map<Float, ASPECT_RATIO> map = new HashMap<Float, ASPECT_RATIO>();

        static {
            for (ASPECT_RATIO aspectRatio : ASPECT_RATIO.values()) {
                map.put(aspectRatio.numVal, aspectRatio);
            }
        }

        private float numVal;

        ASPECT_RATIO(float numVal) {
            this.numVal = numVal;
        }

        public static ASPECT_RATIO valueOf(float val) {
            return map.get(val) == null ? AR16_9 : map.get(val);
        }
    }

    public static final int VIDEO_EDIT_REQUEST_CODE = 1114;
    public static final int VIDEO_EDIT_RESULT_CODE = 1115;
    public static final String TAG = "SCREENRECORDER_LOG";
    public static final String APPDIR = "Screenrecorder";
    public static final String ALERT_EXTR_STORAGE_CB_KEY = "ext_dir_warn_donot_show_again";
    public static final String VIDEO_EDIT_URI_KEY = "edit_video";
    public static final int EXTDIR_REQUEST_CODE = 1110;
    public static final int AUDIO_REQUEST_CODE = 1111;
    public static final int FLOATING_CONTROLS_SYSTEM_WINDOWS_CODE = 1112;
    public static final int SCREEN_RECORD_REQUEST_CODE = 1113;
    public static final int CAMERA_REQUEST_CODE = 1116;
    public static final int CAMERA_SYSTEM_WINDOWS_CODE = 1117;
    public static final int INTERNAL_AUDIO_REQUEST_CODE = 1118;
    public static final int INTERNAL_R_SUBMIX_AUDIO_REQUEST_CODE = 1119;
    public static final String SCREEN_RECORDING_START = "action.startrecording";
    public static final String SCREEN_RECORDING_START_FROM_NOTIFY = "action.startrecording_from_notify";
    public static final String SCREEN_RECORDING_PAUSE = "action.pauserecording";
    public static final String SCREEN_RECORDING_RESUME = "action.resumerecording";
    public static final String SCREEN_RECORDING_STOP = "action.stoprecording";
    public static final String SCREEN_RECORDING_DESTROY = "action.destroy";
    public static final String SCREEN_RECORDING_DESTORY_SHAKE_GESTURE = "action.destoryshakegesture";
    public static final String SCREEN_RECORDER_VIDEOS_LIST_FRAGMENT_INTENT = "com.test.screenrecord.SHOWVIDEOSLIST";
    public static final int SCREEN_RECORDER_NOTIFICATION_ID = 5111;
    public static final int SCREEN_RECORDER_SHARE_NOTIFICATION_ID = 5112;
    public static final int SCREEN_RECORDER_WAITING_FOR_SHAKE_NOTIFICATION_ID = 5113;
    public static final String RECORDER_INTENT_DATA = "recorder_intent_data";
    public static final String RECORDER_INTENT_RESULT = "recorder_intent_result";
    public static final String RECORDING_NOTIFICATION_CHANNEL_ID = "recording_notification_channel_id1003";
    public static final String SHARE_NOTIFICATION_CHANNEL_ID = "share_notification_channel_id1003";
    public static final String RECORDING_NOTIFICATION_CHANNEL_NAME = "Persistent notification shown when recording screen or when waiting for shake gesture";
    public static final String SHARE_NOTIFICATION_CHANNEL_NAME = "Notification shown to share or edit the recorded video";

    public static final String PREFS_CAMERA_OVERLAY_POS = "camera_overlay_pos";
    public static final String PREFS_INTERNAL_AUDIO_DIALOG_KEY = "int_audio_diag";

    public enum RecordingState {
        NONE, RECORDING, PAUSED, STOPPED
    }
}
