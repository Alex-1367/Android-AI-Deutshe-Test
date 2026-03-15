package com.german.learner;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.german.learner.adapters.TabsPagerAdapter;
import com.german.learner.utils.StateManager;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private TabsPagerAdapter pagerAdapter;
    private StateManager stateManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        stateManager = ((GermanApplication) getApplication()).getStateManager();

        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        checkPermissions();
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                requestManageStoragePermission();
            } else {
                setupTabs();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
            } else {
                setupTabs();
            }
        }
    }

    private void requestManageStoragePermission() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Storage Permission Required")
                .setMessage("This app needs storage permission to access your German learning audio files.")
                .setPositiveButton("OK", (dialog, which) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(android.net.Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, PERMISSION_REQUEST_CODE);
                })
                .setNegativeButton("Cancel", (dialog, which) -> finish())
                .show();
    }

    private void setupTabs() {
        pagerAdapter = new TabsPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Start with first tab (Play tab)
        viewPager.setCurrentItem(0, false);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            //tab.setText("Play");
                            tab.setIcon(android.R.drawable.ic_media_play);
                            break;
                        case 1:
                            //tab.setText("Test");
                            tab.setIcon(android.R.drawable.ic_menu_edit);
                            break;
                        case 2:
                            //tab.setText("Settings");
                            tab.setIcon(android.R.drawable.ic_menu_preferences);
                            break;
                    }
                }).attach();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupTabs();
            } else {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Permission Denied")
                        .setMessage("Storage permission is required to use this app.")
                        .setPositiveButton("Exit", (dialog, which) -> finish())
                        .show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    setupTabs();
                } else {
                    finish();
                }
            }
        }
    }

    public StateManager getStateManager() {
        return stateManager;
    }
}