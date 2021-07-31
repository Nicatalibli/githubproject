package com.test.screenrecord.ui.activities;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.ads.control.AdmobHelp;
import com.google.android.material.navigation.NavigationView;
import com.test.screenrecord.R;
import com.test.screenrecord.common.Const;
import com.test.screenrecord.common.PrefUtils;
import com.test.screenrecord.common.Utils;
import com.test.screenrecord.interfaces.PermissionResultListener;
import com.test.screenrecord.services.FloatingControlService;
import com.test.screenrecord.ui.fragments.BaseFragment;
import com.test.screenrecord.ui.fragments.ScreenshotsListFragment;
import com.test.screenrecord.ui.fragments.SettingsFragment;
import com.test.screenrecord.ui.fragments.VideosEditedListFragment;
import com.test.screenrecord.ui.fragments.VideosListFragment;


public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private FragmentManager fragmentManager;
    private FragmentTransaction mTransaction;
    private VideosListFragment mVideosFragment;
    private VideosEditedListFragment mVideosEditedFragment;
    private ScreenshotsListFragment mScreenshotFragment;
    private SettingsFragment mSettingsFragment;

    //Common
    private PermissionResultListener mPermissionResultListener;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        try {

            AdmobHelp.getInstance().loadBanner(this);
            // initAdmob();
            initViews();
            initEvents();
            addVideoFragment();
            mSettingsFragment = SettingsFragment.newInstance();
        } catch (Exception e) {
        }
    }

//    private void initAdmob() {
//        final AdView adView = findViewById(R.id.adView);
//        final AdRequest adRequest = new AdRequest.Builder().build();
//        adView.loadAd(adRequest);
//        adView.setAdListener(new AdListener() {
//            @Override
//            public void onAdFailedToLoad(int i) {
//                super.onAdFailedToLoad(i);
//                adView.setVisibility(View.GONE);
//            }
//        });
//    }

    private void initViews() {
        try {
            fragmentManager = getFragmentManager();

            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            DrawerLayout drawer = findViewById(R.id.drawer_layout);
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this,
                    drawer,
                    toolbar,
                    R.string.navigation_drawer_open,
                    R.string.navigation_drawer_close);
            drawer.addDrawerListener(toggle);
            toggle.syncState();
            NavigationView navigationView = findViewById(R.id.nav_view);
            navigationView.setItemIconTintList(null);
            navigationView.setNavigationItemSelectedListener(this);
        } catch (Exception e) {
        }
    }

    private void initEvents() {

    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
    };

    public void setPermissionResultListener(PermissionResultListener mPermissionResultListener) {
        this.mPermissionResultListener = mPermissionResultListener;
    }

    public boolean requestPermissionStorage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.storage_permission_request_title))
                    .setMessage(getString(R.string.storage_permission_request_summary))
                    .setNeutralButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
//                            ActivityCompat.requestPermissions(HomeActivity.this,
//                                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
//                                    Const.EXTDIR_REQUEST_CODE);
                            if (!hasPermissions(HomeActivity.this, PERMISSIONS)) {
                                ActivityCompat.requestPermissions(HomeActivity.this, PERMISSIONS, Const.EXTDIR_REQUEST_CODE);
                            }
                        }
                    })
                    .setPositiveButton("EXIT", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setCancelable(false);

            alert.create().show();
            return false;
        }
        return true;
    }

    private void addFragment(final Fragment fragment) {
        try {
            mTransaction = fragmentManager.beginTransaction();
            mTransaction.replace(R.id.fragment_content, fragment);
            mTransaction.commit();
            if (fragment instanceof BaseFragment) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((BaseFragment) fragment).onVisibleFragment();
                            }
                        });
                    }
                }, 100);
            }
        } catch (Exception e) {
        }
    }

    private void backFragment() {

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        try {
            int id = item.getItemId();

            switch (id) {
                case R.id.nav_video:
                    addVideoFragment();

                    break;
//                case R.id.nav_video_edited:
//                    InterstitialUtils.getInstance().showInterstitialAd(new InterstitialUtils.AdCloseListener() {
//                        @Override
//                        public void onAdClosed() {
//                            addVideoEditedFragment();
//                        }
//                    });
//                    break;
                case R.id.nav_gallery:
                    addScreenshotFragment();

                    break;
                case R.id.nav_settings:
                    addSettingsFragment();

                    break;
//                case R.id.nav_upgrade:
//                    Toast.makeText(this, "Update soon...", Toast.LENGTH_LONG).show();
//                    //   Utils.openURL(this, this.getResources().getString(R.string.link_upgrade));
//                    break;
//                case R.id.nav_rating:
//                    Utils.openURL(this, Utils.getAppUrl(this));
//                    break;
                case R.id.nav_policy:
                    //    Utils.openURL(this, this.getResources().getString(R.string.link_policy));
                    break;
                case R.id.nav_share:
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, Utils.getAppUrl(this));
                    sendIntent.setType("text/plain");
                    startActivity(sendIntent);
                    break;
                case R.id.nav_more:
//                   AdsCompat.getInstance(this).showStoreAds(new AdsCompat.AdCloseListener() {
//                       @Override
//                       public void onAdClosed() {
//
//                       }
//                   });
                    break;
            }

            DrawerLayout drawer = findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
        } catch (Exception e) {
        }
        return true;
    }

    private void addVideoEditedFragment() {
        if (mVideosEditedFragment == null) {
            mVideosEditedFragment = VideosEditedListFragment.newInstance();
        }
        addFragment(mVideosEditedFragment);
    }

    private void addVideoFragment() {
        if (mVideosFragment == null) {
            mVideosFragment = VideosListFragment.newInstance();
        }
        addFragment(mVideosFragment);
    }

    private void addScreenshotFragment() {
        if (mScreenshotFragment == null) {
            mScreenshotFragment = ScreenshotsListFragment.newInstance();
        }
        addFragment(mScreenshotFragment);
    }

    private void addSettingsFragment() {
        if (mSettingsFragment == null) {
            mSettingsFragment = SettingsFragment.newInstance();
        }
        addFragment(mSettingsFragment);
    }

    @TargetApi(23)
    public void requestSystemWindowsPermission(int code) {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, code);
        }
    }

    public void onDirectoryChanged() {
//        ViewPagerAdapter adapter = (ViewPagerAdapter) viewPager.getAdapter();
////        ((VideosListFragment) adapter.getItem(1)).removePhotosList();
//        Log.d(Const.TAG, "reached main act");
    }

    @TargetApi(23)
    private void setSystemWindowsPermissionResult(int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                mPermissionResultListener.onPermissionResult(requestCode,
                        new String[]{"System Windows Permission"},
                        new int[]{PackageManager.PERMISSION_GRANTED});
            } else {
                mPermissionResultListener.onPermissionResult(requestCode,
                        new String[]{"System Windows Permission"},
                        new int[]{PackageManager.PERMISSION_DENIED});
            }
        } else {
            mPermissionResultListener.onPermissionResult(requestCode,
                    new String[]{"System Windows Permission"},
                    new int[]{PackageManager.PERMISSION_GRANTED});
        }
    }

    public void requestPermissionCamera() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    Const.CAMERA_REQUEST_CODE);
        }
    }

    public void requestPermissionAudio(int requestCode) {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    requestCode);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case Const.EXTDIR_REQUEST_CODE:
                if ((grantResults.length > 0) &&
                        (grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
                    Log.d(Const.TAG, "write storage Permission Denied");
                    /* Disable floating action Button in case write storage permission is denied.
                     * There is no use in recording screen when the video is unable to be saved */
                } else {
                    /* Since we have write storage permission now, lets create the app directory
                     * in external storage*/
                    startService();
                    Log.d(Const.TAG, "write storage Permission granted");
                    Utils.createDir();
                }
        }

        // Let's also pass the result data to SettingsPreferenceFragment using the callback interface
        if (mPermissionResultListener != null) {
            mPermissionResultListener.onPermissionResult(requestCode, permissions, grantResults);
        }
    }

    public void startService() {
        if (PrefUtils.readBooleanValue(this, getString(R.string.preference_floating_control_key), PrefUtils.VALUE_USE_FLOAT)) {
            Intent intent = new Intent(this, FloatingControlService.class);
            startService(intent);
        }
    }

}
