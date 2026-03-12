package com.german.learner.fragments;

import com.german.learner.R;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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

    private Button detectcardbutton;
    private StateManager stateManager;
    private boolean isSelectingA1 = true;

    // Modern Activity Result API
// Modern Activity Result API
// Simple directory picker that returns a file path
    private final ActivityResultLauncher<Intent> directoryPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                            Uri uri = result.getData().getData();
                            if (uri != null) {
                                // Take persistable permission
                                getActivity().getContentResolver().takePersistableUriPermission(uri,
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION);

                                // Get the path from URI - simplified version
                                String path = uri.getPath();
                                if (path != null) {
                                    // Extract the actual path
                                    // Format: /tree/primary:A1
                                    if (path.contains(":")) {
                                        String[] parts = path.split(":");
                                        if (parts.length >= 2) {
                                            String folderName = parts[1];
                                            path = "/storage/emulated/0/" + folderName;

                                            if (isSelectingA1) {
                                                a1PathEditText.setText(path);
                                            } else {
                                                a2PathEditText.setText(path);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    });

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
        detectcardbutton = view.findViewById(R.id.detect_sdcard);

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
                isSelectingA1 = true;
                openDirectoryPicker();
            }
        });

        browseA2Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isSelectingA1 = false;
                openDirectoryPicker();
            }
        });

        savePathsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String a1Path = a1PathEditText.getText().toString().trim();
                String a2Path = a2PathEditText.getText().toString().trim();

                Log.d("SETTINGS", "Saving A1 path: " + a1Path);
                Log.d("SETTINGS", "Saving A2 path: " + a2Path);

                if (!a1Path.isEmpty()) {
                    File a1Dir = new File(a1Path);
                    if (a1Dir.exists()) {
                        stateManager.setPrimaryRootPath(a1Path);
                        Log.d("SETTINGS", "A1 path saved to StateManager");
                    } else {
                        Log.d("SETTINGS", "A1 directory does NOT exist: " + a1Path);
                        Toast.makeText(getContext(), "A1 directory does not exist", Toast.LENGTH_LONG).show();
                    }
                }

                if (!a2Path.isEmpty()) {
                    File a2Dir = new File(a2Path);
                    if (a2Dir.exists()) {
                        stateManager.setSecondaryRootPath(a2Path);
                        Log.d("SETTINGS", "A2 path saved to StateManager");
                    } else {
                        Log.d("SETTINGS", "A2 directory does NOT exist: " + a2Path);
                        Toast.makeText(getContext(), "A2 directory does not exist", Toast.LENGTH_LONG).show();
                    }
                }

                // Verify they were saved
                Log.d("SETTINGS", "After save - Primary: " + stateManager.getPrimaryRootPath());
                Log.d("SETTINGS", "After save - Secondary: " + stateManager.getSecondaryRootPath());

                stateManager.setAutoResumePlayback(autoResumeSwitch.isChecked());
                Toast.makeText(getContext(), "Settings saved", Toast.LENGTH_SHORT).show();
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

        // Add SD card detection button listener
        detectcardbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                detectAndShowSdCardPaths();
            }
        });
    }

    private void openDirectoryPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        directoryPickerLauncher.launch(intent);
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

    /**
     * Detect SD card and show paths to user
     */
    private void detectAndShowSdCardPaths() {
        try {
            // Get all external storage directories
            File[] externalDirs = requireContext().getExternalFilesDirs(null);

            StringBuilder message = new StringBuilder("Detected Storage Locations:\n\n");

            for (int i = 0; i < externalDirs.length; i++) {
                if (externalDirs[i] != null) {
                    String fullPath = externalDirs[i].getAbsolutePath();
                    message.append("Storage ").append(i).append(": ").append(fullPath).append("\n");

                    // Try to get root path (remove /Android/data/...)
                    if (fullPath.contains("/Android")) {
                        String rootPath = fullPath.substring(0, fullPath.indexOf("/Android"));
                        message.append("   Root: ").append(rootPath).append("\n");

                        // If this is SD card (usually index 1), offer to use it
                        if (i == 1) {
                            message.append("\n✅ SD Card detected!\n");
                            message.append("Suggested A1 path: ").append(rootPath).append("/A1\n");
                            message.append("Suggested A2 path: ").append(rootPath).append("/A2\n");

                            // Ask user if they want to use these paths
                            suggestSdCardPaths(rootPath);
                        }
                    }
                    message.append("\n");
                }
            }

            // Show all detected storage
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Storage Detection")
                    .setMessage(message.toString())
                    .setPositiveButton("OK", null)
                    .show();

        } catch (Exception e) {
            Log.e("SettingsFragment", "Error detecting SD card", e);
            Toast.makeText(requireContext(), "Error detecting storage: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Suggest using SD card paths
     */
    private void suggestSdCardPaths(String sdCardRoot) {
        String suggestedA1 = sdCardRoot + "/A1";
        String suggestedA2 = sdCardRoot + "/A2";

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Use SD Card?")
                .setMessage("Set A1 path to:\n" + suggestedA1 + "\n\nSet A2 path to:\n" + suggestedA2)
                .setPositiveButton("Yes", (dialog, which) -> {
                    a1PathEditText.setText(suggestedA1);
                    a2PathEditText.setText(suggestedA2);
                    Toast.makeText(requireContext(), "Paths updated. Click Save to store.",
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();
    }
}
