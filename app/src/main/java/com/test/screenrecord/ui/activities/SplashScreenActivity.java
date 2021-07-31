package com.test.screenrecord.ui.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ads.control.AdmobHelp;
import com.test.screenrecord.R;
import com.test.screenrecord.common.PrefUtils;

import java.util.Locale;


public class SplashScreenActivity extends AppCompatActivity {

    Handler mHandler;
    Runnable r;
    public static boolean isFirstOpen = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_splash_screen);
        AdmobHelp.getInstance().init(this);
        setLocale();
        isFirstOpen = true;

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (!Settings.System.canWrite(getApplicationContext())) {
//                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName()));
//                startActivityForResult(intent, 200);
//            }
//        }
        mHandler = new Handler();
        r = new Runnable() {
            @Override
            public void run() {
                checkPermission();

            }
        };

        mHandler.postDelayed(r, 7000);

    }

//    private void showPermissionDialog() {
//        try {
//            if (Utils.isAndroid23()) {
//                if (!Settings.canDrawOverlays(this)) {
//                    showPermissionRationale();
//                } else {
//                    onPermissionGranted();
//                }
//            } else {
//                onPermissionGranted();
//            }
//        } catch (Exception e) {
//        }
//    }

//    private void startService() {
//        if (PrefUtils.readBooleanValue(this, getString(R.string.preference_floating_control_key), PrefUtils.VALUE_USE_FLOAT)) {
//            Intent intent = new Intent(this, FloatingControlService.class);
//            startService(intent);
//        }
//    }

//    private void showPermissionRationale() {
//        try {
//            final AlertDialog builder = new AlertDialog.Builder(this).create();
//            View view = View.inflate(this, R.layout.dialog_one_button, null);
//            builder.setIcon(R.drawable.ic_folder);
//            builder.setView(view);
//            if (builder.getWindow() != null) {
//                builder.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
//            }
//            TextView title, message;
//            title = view.findViewById(R.id.title);
//            message = view.findViewById(R.id.message);
//            title.setText(getString(R.string.app_name));
//            message.setText(getString(R.string.perm_rationale));
//
//            Button positiveButton = view.findViewById(R.id.dlg_one_button_btn_ok);
//            positiveButton.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    builder.dismiss();
//                    checkPermission();
//                }
//            });
//            Button negativeButton = view.findViewById(R.id.dlg_one_button_btn_exit);
//            negativeButton.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    builder.dismiss();
//                    finishAffinity();
//                }
//            });
//            View policy = view.findViewById(R.id.tvPolicy);
//            policy.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    Utils.openURL(SplashScreenActivity.this, SplashScreenActivity.this.getString(R.string.link_policy));
//                }
//            });
//            builder.setCanceledOnTouchOutside(false);
//            try {
//                builder.show();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        } catch (Exception e) {
//        }
//    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 12) {
            if (isSystemAlertPermissionGranted(this)) {
                onPermissionGranted();
            } else {
                Toast.makeText(this, R.string.str_permission_remind, Toast.LENGTH_LONG).show();
                finishAffinity();
            }
        }
    }

    @SuppressLint({"NewApi"})
    public static boolean isSystemAlertPermissionGranted(Context context) {
        if (Build.VERSION.SDK_INT < 23) {
            return true;
        }
        return Settings.canDrawOverlays(context);
    }

    @SuppressLint({"NewApi"})
    public void checkPermission() {
        if (isSystemAlertPermissionGranted(this)) {
            onPermissionGranted();
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("package:");
            stringBuilder.append(getPackageName());
            startActivityForResult(new Intent("android.settings.action.MANAGE_OVERLAY_PERMISSION", Uri.parse(stringBuilder.toString())), 12);
            startActivity(new Intent(this, GuideActivity.class));
        }

    }

    private void onPermissionGranted() {
        try {
            AdmobHelp.getInstance().showInterstitialAd(new AdmobHelp.AdCloseListener() {
                @Override
                public void onAdClosed() {
                    Intent intent = new Intent(SplashScreenActivity.this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                }
            });


        } catch (Exception e) {
        }
    }

    public void setLocale() {
        String language = PrefUtils.readStringValue(this, getString(R.string.language_key), PrefUtils.VALUE_LANGUAGE);

        if (PrefUtils.firstOpen(this)) {
            language = Locale.getDefault().getLanguage();
            if (language.equalsIgnoreCase("vi")) {
                PrefUtils.saveStringValue(this, getString(R.string.language_key), language);
            }
        }

        Locale myLocale = new Locale(language);
        Resources res = getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.locale = myLocale;
        res.updateConfiguration(conf, dm);
    }

    @Override
    protected void onDestroy() {
        if (mHandler != null && r != null)
            mHandler.removeCallbacks(r);
        super.onDestroy();
    }
}
