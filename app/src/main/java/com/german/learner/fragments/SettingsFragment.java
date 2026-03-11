package com.german.learner.fragments;

import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.german.learner.MainActivity;
import com.german.learner.R;
import com.german.learner.utils.StateManager;

import java.io.File;

public class SettingsFragment extends Fragment {

    private EditText a1PathEditText;
    private EditText a2PathEditText;
    private Button browseA1Button;
    private Button browseA2Button;
    private Button savePathsButton;
    private Button clearDataButton;
    private SwitchMaterial autoResumeSwitch;
    private TextView storageInfoTextView;

    private StateManager stateManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        stateManager = ((MainActivity) requireActivity()).getStateManager();

        a1PathEditText = view.findViewById(R.id.a1_path);
        a2PathEditText = view.findViewById(R.id.a2_path);
        browseA1Button = view.findViewById(R.id.browse_a1);
        browseA2Button = view.findViewById(R.id.browse_a2);
        savePathsButton = view.findViewById(R.id.save_paths);
        clearDataButton = view.findViewById(R.id.clear_data);
        autoResumeSwitch = view.findViewById(R.id.auto_resume);
        storageInfoTextView = view.findViewById(R.id.storage_info);

        loadSettings();
        setupListeners();
        updateStorageInfo();

        return view;
    }

    private void loadSettings() {
        a1PathEditText.setText(stateManager.getPrimaryRootPath());
        a2PathEditText.setText(stateManager.getSecondaryRootPath());
        autoResumeSwitch.setChecked(stateManager.isAutoResumePlayback());
    }

    private void setupListeners() {
        browseA1Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // In a real app, implement directory picker
                // For now, just set to default A1 path
                String defaultPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/A1";
                a1PathEditText.setText(defaultPath);
            }
        });

        browseA2Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String defaultPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/A2";
                a2PathEditText.setText(defaultPath);
            }
        });

        savePathsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String a1Path = a1PathEditText.getText().toString().trim();
                String a2Path = a2PathEditText.getText().toString().trim();

                boolean pathsValid = true;

                if (!a1Path.isEmpty()) {
                    File a1Dir = new File(a1Path);
                    if (!a1Dir.exists()) {
                        a1PathEditText.setError("Directory does not exist");
                        pathsValid = false;
                    } else {
                        stateManager.setPrimaryRootPath(a1Path);
                    }
                }

                if (!a2Path.isEmpty()) {
                    File a2Dir = new File(a2Path);
                    if (!a2Dir.exists()) {
                        a2PathEditText.setError("Directory does not exist");
                        pathsValid = false;
                    } else {
                        stateManager.setSecondaryRootPath(a2Path);
                    }
                }

                stateManager.setAutoResumePlayback(autoResumeSwitch.isChecked());

                if (pathsValid) {
                    Toast.makeText(getContext(), "Settings saved", Toast.LENGTH_SHORT).show();
                }
            }
        });

        clearDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new androidx.appcompat.app.AlertDialog.Builder(getContext())
                        .setTitle("Clear All Data")
                        .setMessage("Are you sure you want to clear all app data? This cannot be undone.")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            stateManager.clearAllData();
                            loadSettings();
                            Toast.makeText(getContext(), "All data cleared", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });
    }

    private void updateStorageInfo() {
        File root = Environment.getExternalStorageDirectory();
        long totalSpace = root.getTotalSpace();
        long freeSpace = root.getFreeSpace();

        String info = String.format("Storage: %s free of %s total",
                formatSize(freeSpace),
                formatSize(totalSpace));

        storageInfoTextView.setText(info);
    }

    private String formatSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format(java.util.Locale.getDefault(), "%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format(java.util.Locale.getDefault(), "%.1f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format(java.util.Locale.getDefault(), "%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }
}