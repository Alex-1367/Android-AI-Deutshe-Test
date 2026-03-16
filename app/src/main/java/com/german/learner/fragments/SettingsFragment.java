package com.german.learner.fragments;

import com.german.learner.R;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class SettingsFragment extends Fragment {

    private EditText rootPathEditText;  // Changed from a1PathEditText
    private Button browseButton;         // Changed from browseA1Button
    private Button savePathsButton;
    private Button clearDataButton;
    private Button resetStateButton;
    private Button viewStateButton;
    private SwitchMaterial autoResumeSwitch;
    private TextView storageInfoTextView;

    private Button detectcardbutton;
    private StateManager stateManager;


    // Modern Activity Result API
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
                                    if (path.contains(":")) {
                                        String[] parts = path.split(":");
                                        if (parts.length >= 2) {
                                            String folderName = parts[1];
                                            path = "/storage/emulated/0/" + folderName;
                                            rootPathEditText.setText(path);
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

        rootPathEditText = view.findViewById(R.id.root_path);
        browseButton = view.findViewById(R.id.browse_button);
        savePathsButton = view.findViewById(R.id.save_paths);
        clearDataButton = view.findViewById(R.id.clear_data);
        autoResumeSwitch = view.findViewById(R.id.auto_resume);
        storageInfoTextView = view.findViewById(R.id.storage_info);
        detectcardbutton = view.findViewById(R.id.detect_sdcard);
        viewStateButton = view.findViewById(R.id.view_state);
        resetStateButton = view.findViewById(R.id.reset_state);

        loadSettings();
        setupListeners();
        updateStorageInfo();

        return view;
    }

    private void loadSettings() {
        rootPathEditText.setText(stateManager.getRootPath());
        autoResumeSwitch.setChecked(stateManager.isAutoResumePlayback());
    }

    private void setupListeners() {
        browseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDirectoryPicker();
            }
        });

        savePathsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String rootPath = rootPathEditText.getText().toString().trim();

                Log.d("SETTINGS", "Saving root path: " + rootPath);

                if (!rootPath.isEmpty()) {
                    File rootDir = new File(rootPath);
                    if (rootDir.exists()) {
                        stateManager.setRootPath(rootPath);
                        Log.d("SETTINGS", "Root path saved to StateManager");
                    } else {
                        Log.d("SETTINGS", "Root directory does NOT exist: " + rootPath);
                        Toast.makeText(getContext(), "Root directory does not exist", Toast.LENGTH_LONG).show();
                    }
                }

                // Verify it was saved
                Log.d("SETTINGS", "After save - Root: " + stateManager.getRootPath());

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

        viewStateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showStateFile();
            }
        });

        resetStateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Reset to First Run")
                        .setMessage("This will delete your saved state and restart the app to the Settings tab on next launch. Continue?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            // Delete state file
                            File stateFile = new File(requireContext().getFilesDir(), "deutsch_lerner_state.json");
                            if (stateFile.exists()) {
                                stateFile.delete();
                            }
                            // Also clear in-memory state
                            stateManager.clearAllData();

                            Toast.makeText(requireContext(), "State reset. Restart app to test first-run experience.", Toast.LENGTH_LONG).show();
                        })
                        .setNegativeButton("No", null)
                        .show();
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
                            message.append("Suggested root path: ").append(rootPath).append("/DeutscheCource\n");

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
        String suggestedRoot = sdCardRoot + "/DeutscheCourse";

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Use SD Card?")
                .setMessage("Set root path to:\n" + suggestedRoot)
                .setPositiveButton("Yes", (dialog, which) -> {
                    rootPathEditText.setText(suggestedRoot);
                    Toast.makeText(requireContext(), "Path updated. Click Save to store.",
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();
    }


    private void showStateFile() {
        try {
            // Get the state file
            File stateFile = new File(requireContext().getFilesDir(), "deutsch_lerner_state.json");

            if (!stateFile.exists()) {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("State File")
                        .setMessage("No state file found. App hasn't saved any data yet.")
                        .setPositiveButton("OK", null)
                        .show();
                return;
            }

            // Read the file
            StringBuilder content = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(stateFile));
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();

            // Parse to make it readable
            String jsonContent = content.toString();

            // Show in dialog with copy option
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
            builder.setTitle("Current State File");

            // Create a ScrollView for long content
            ScrollView scrollView = new ScrollView(requireContext());
            TextView textView = new TextView(requireContext());
            textView.setText(jsonContent);
            textView.setTextSize(10);
            textView.setTypeface(Typeface.MONOSPACE);
            textView.setPadding(16, 16, 16, 16);
            scrollView.addView(textView);

            builder.setView(scrollView);
            builder.setPositiveButton("OK", null);
            builder.setNeutralButton("Copy", (dialog, which) -> {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("State JSON", jsonContent);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(requireContext(), "State copied to clipboard", Toast.LENGTH_SHORT).show();
            });
            builder.show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Error reading state: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}