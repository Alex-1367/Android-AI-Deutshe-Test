package com.german.learner.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.german.learner.MainActivity;
import com.german.learner.R;
import com.german.learner.adapters.FileListAdapter;
import com.german.learner.models.PlaybackState;
import com.german.learner.models.TrackInfo;
import com.german.learner.utils.StateManager;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PlayFragment extends Fragment {

    private static final String TAG = "PlayFragment";

    private TextView currentPathTextView;
    private ListView fileListView;
    private Button navigateUpButton;
    private ImageButton playPauseButton;
    private TextView nowPlayingTextView;
    private LinearLayout tagSelectorLayout;

    private File currentDirectory;
    private List<File> fileList = new ArrayList<>();
    private FileListAdapter adapter;

    private StateManager stateManager;
    private MediaPlayer mediaPlayer;
    private File currentlyPlayingFile;
    private boolean isPlaying = false;
    private Handler handler = new Handler();
    private Runnable updatePositionRunnable;

    // Currently selected file for tagging
    private File selectedFileForTagging;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_play, container, false);

        currentPathTextView = view.findViewById(R.id.current_path);
        fileListView = view.findViewById(R.id.file_list);
        navigateUpButton = view.findViewById(R.id.navigate_up);
        playPauseButton = view.findViewById(R.id.play_pause);
        nowPlayingTextView = view.findViewById(R.id.now_playing);
        tagSelectorLayout = view.findViewById(R.id.tag_selector);

        stateManager = ((MainActivity) requireActivity()).getStateManager();

        setupViews();
        setupTagSelector();

        // Start with A1 folder or last saved folder
        String startPath = stateManager.getPrimaryRootPath();
        File startDir = new File(startPath);
        if (!startDir.exists()) {
            startPath = stateManager.getSecondaryRootPath();
        }
        navigateToPath(startPath);
        Log.d("PLAY_DEBUG", "Navigated to: " + startPath);

        // Restore playing state
        restorePlayingState();

        debugStorageAccess(startPath);

        return view;
    }

    private void setupViews() {
        adapter = new FileListAdapter(requireContext(), fileList, stateManager, this);
        fileListView.setAdapter(adapter);

        fileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                File file = fileList.get(position);
                if (file.isDirectory()) {
                    navigateToPath(file.getAbsolutePath());
                } else if (isAudioFile(file)) {
                    playAudio(file);
                }
            }
        });

        fileListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                File file = fileList.get(position);
                if (isAudioFile(file)) {
                    showTagSelector(file);
                    return true;
                }
                return false;
            }
        });

        navigateUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentDirectory != null) {
                    File parent = currentDirectory.getParentFile();
                    if (parent != null && !parent.getAbsolutePath().equals("/")) {
                        navigateToPath(parent.getAbsolutePath());
                    }
                }
            }
        });

        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlayer != null) {
                    if (mediaPlayer.isPlaying()) {
                        pauseAudio();
                    } else {
                        resumeAudio();
                    }
                } else if (currentlyPlayingFile != null) {
                    playAudio(currentlyPlayingFile);
                }
            }
        });

        // Setup position update runnable
        updatePositionRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying() && currentlyPlayingFile != null) {
                    stateManager.updatePlaybackState(
                            currentlyPlayingFile.getAbsolutePath(),
                            mediaPlayer.getCurrentPosition(),
                            true
                    );
                    stateManager.updateTrackPosition(
                            currentlyPlayingFile.getAbsolutePath(),
                            mediaPlayer.getCurrentPosition()
                    );
                    handler.postDelayed(this, 5000);
                }
            }
        };
    }

    private void setupTagSelector() {
        // Create tag buttons
        String[] tags = {"Digits", "Words", "Dialog", "Dictionary", "Grammar"};
        int[] colors = {
                android.R.color.holo_red_dark,
                android.R.color.holo_blue_dark,
                android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark,
                android.R.color.holo_purple
        };

        tagSelectorLayout.removeAllViews();

        for (int i = 0; i < tags.length; i++) {
            final String tag = tags[i];
            final int color = colors[i];

            Button button = new Button(requireContext());
            button.setText(tag);
            button.setBackgroundColor(getResources().getColor(color));
            button.setPadding(16, 8, 16, 8);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(4, 4, 4, 4);
            button.setLayoutParams(params);

            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (selectedFileForTagging != null) {
                        toggleTag(selectedFileForTagging, tag);
                        tagSelectorLayout.setVisibility(View.GONE);
                    }
                }
            });

            tagSelectorLayout.addView(button);
        }

        // Add importance level selector
        LinearLayout importanceLayout = new LinearLayout(requireContext());
        importanceLayout.setOrientation(LinearLayout.HORIZONTAL);

        TextView importanceLabel = new TextView(requireContext());
        importanceLabel.setText("Importance: ");
        importanceLayout.addView(importanceLabel);

        for (int i = 1; i <= 5; i++) {
            final int level = i;
            Button levelButton = new Button(requireContext());
            levelButton.setText(String.valueOf(i));
            levelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (selectedFileForTagging != null) {
                        stateManager.setTrackImportance(
                                selectedFileForTagging.getAbsolutePath(),
                                level
                        );
                        adapter.notifyDataSetChanged();
                        tagSelectorLayout.setVisibility(View.GONE);
                        Toast.makeText(requireContext(),
                                "Importance level set to " + level,
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
            importanceLayout.addView(levelButton);
        }

        tagSelectorLayout.addView(importanceLayout);
        tagSelectorLayout.setVisibility(View.GONE);
    }

    public void showTagSelector(File file) {
        selectedFileForTagging = file;
        tagSelectorLayout.setVisibility(View.VISIBLE);

        // Highlight existing tags
        TrackInfo info = stateManager.getTrackInfo(file.getAbsolutePath());
        if (info != null) {
            // Could highlight buttons based on existing tags
        }
    }

    private void toggleTag(File file, String tag) {
        TrackInfo info = stateManager.getTrackInfo(file.getAbsolutePath());
        if (info.hasTag(tag)) {
            info.removeTag(tag);
            Toast.makeText(requireContext(), "Removed tag: " + tag, Toast.LENGTH_SHORT).show();
        } else {
            info.addTag(tag);
            Toast.makeText(requireContext(), "Added tag: " + tag, Toast.LENGTH_SHORT).show();
        }
        stateManager.updateTrackInfo(info);
        adapter.notifyDataSetChanged();
    }

    private void navigateToPath(String path) {
        File file = new File(path);
        if (file.exists() && file.isDirectory()) {
            // Save current folder state before leaving
            if (currentDirectory != null) {
                stateManager.saveFolderState(currentDirectory.getAbsolutePath(), fileListView);
            }

            currentDirectory = file;
            currentPathTextView.setText(file.getAbsolutePath());
            loadFiles(file);

            // Restore scroll position for this folder
            stateManager.restoreFolderState(file.getAbsolutePath(), fileListView);
        }
    }

    private void loadFiles(File directory) {
        fileList.clear();

        Log.d("PLAY_DEBUG", "========== LOADING FOLDER ==========");
        Log.d("PLAY_DEBUG", "Loading directory: " + directory.getAbsolutePath());
        Log.d("PLAY_DEBUG", "Directory exists: " + directory.exists());
        Log.d("PLAY_DEBUG", "Is directory: " + directory.isDirectory());
        Log.d("PLAY_DEBUG", "Can read: " + directory.canRead());

        if (!directory.exists()) {
            Log.e("PLAY_DEBUG", "Directory does not exist!");
            Toast.makeText(getContext(), "Directory does not exist: " + directory.getAbsolutePath(), Toast.LENGTH_LONG).show();
            adapter.notifyDataSetChanged();
            return;
        }

        if (!directory.isDirectory()) {
            Log.e("PLAY_DEBUG", "Path is not a directory!");
            Toast.makeText(getContext(), "Not a directory: " + directory.getAbsolutePath(), Toast.LENGTH_LONG).show();
            adapter.notifyDataSetChanged();
            return;
        }

        File[] files = directory.listFiles();

        if (files == null) {
            Log.e("PLAY_DEBUG", "listFiles() returned NULL! This usually means permission denied.");

            // Check permission specifically
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                boolean hasManageStorage = Environment.isExternalStorageManager();
                Log.d("PLAY_DEBUG", "Has MANAGE_EXTERNAL_STORAGE permission: " + hasManageStorage);
                if (!hasManageStorage) {
                    Toast.makeText(getContext(), "Need MANAGE_EXTERNAL_STORAGE permission", Toast.LENGTH_LONG).show();
                }
            } else {
                int permission = ContextCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.READ_EXTERNAL_STORAGE);
                Log.d("PLAY_DEBUG", "READ_EXTERNAL_STORAGE permission: " +
                        (permission == PackageManager.PERMISSION_GRANTED ? "GRANTED" : "DENIED"));
            }

            adapter.notifyDataSetChanged();
            return;
        }

        Log.d("PLAY_DEBUG", "Total items in directory: " + files.length);

        // Filter and sort
        List<File> filteredList = new ArrayList<>();
        for (File file : files) {
            if (file.isDirectory()) {
                filteredList.add(file);
                Log.d("PLAY_DEBUG", "Found directory: " + file.getName());
            } else if (file.isFile()) {
                String name = file.getName().toLowerCase();
                if (name.endsWith(".mp3")) {
                    filteredList.add(file);
                    Log.d("PLAY_DEBUG", "Found MP3: " + file.getName());
                } else {
                    Log.d("PLAY_DEBUG", "Ignoring non-MP3 file: " + file.getName());
                }
            }
        }

        // Sort: directories first, then files alphabetically
        Collections.sort(filteredList, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                if (f1.isDirectory() && !f2.isDirectory()) return -1;
                if (!f1.isDirectory() && f2.isDirectory()) return 1;
                return f1.getName().compareToIgnoreCase(f2.getName());
            }
        });

        fileList.addAll(filteredList);
        Log.d("PLAY_DEBUG", "Final list size: " + fileList.size());
        Log.d("PLAY_DEBUG", "=====================================");

        adapter.notifyDataSetChanged();
        navigateUpButton.setEnabled(!directory.getAbsolutePath().equals("/"));

        if (fileList.isEmpty()) {
            Toast.makeText(getContext(), "No MP3 files or folders found in this directory", Toast.LENGTH_LONG).show();
        }
    }

    private boolean isAudioFile(File file) {
        return file.isFile() && file.getName().toLowerCase().endsWith(".mp3");
    }

    public void playAudio(File file) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }

            mediaPlayer = new MediaPlayer();

            // This works for BOTH internal storage AND SD card!
            mediaPlayer.setDataSource(file.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.start();

            // ... rest of your code
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Error playing: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    private void pauseAudio() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPlaying = false;
            playPauseButton.setImageResource(android.R.drawable.ic_media_play);

            if (currentlyPlayingFile != null) {
                stateManager.updatePlaybackState(
                        currentlyPlayingFile.getAbsolutePath(),
                        mediaPlayer.getCurrentPosition(),
                        false
                );
                stateManager.updateTrackPosition(
                        currentlyPlayingFile.getAbsolutePath(),
                        mediaPlayer.getCurrentPosition()
                );
            }
            handler.removeCallbacks(updatePositionRunnable);
        }
    }

    private void resumeAudio() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            isPlaying = true;
            playPauseButton.setImageResource(android.R.drawable.ic_media_pause);

            if (currentlyPlayingFile != null) {
                stateManager.updatePlaybackState(
                        currentlyPlayingFile.getAbsolutePath(),
                        mediaPlayer.getCurrentPosition(),
                        true
                );
            }
            handler.post(updatePositionRunnable);
        }
    }

    private void restorePlayingState() {
        PlaybackState playbackState = stateManager.getPlaybackState();
        if (playbackState != null && playbackState.getCurrentFilePath() != null &&
                !playbackState.getCurrentFilePath().isEmpty()) {

            File lastPlayed = new File(playbackState.getCurrentFilePath());
            if (lastPlayed.exists()) {
                currentlyPlayingFile = lastPlayed;
                String status = playbackState.isPlaying() ? "Paused at" : "Last played";
                nowPlayingTextView.setText(String.format("%s: %s (%d sec)",
                        status,
                        lastPlayed.getName(),
                        playbackState.getCurrentPosition() / 1000));

                playPauseButton.setImageResource(android.R.drawable.ic_media_play);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Save current folder state
        if (currentDirectory != null) {
            stateManager.saveFolderState(currentDirectory.getAbsolutePath(), fileListView);
        }

        // Save playback state
        if (mediaPlayer != null && currentlyPlayingFile != null) {
            stateManager.updatePlaybackState(
                    currentlyPlayingFile.getAbsolutePath(),
                    mediaPlayer.getCurrentPosition(),
                    mediaPlayer.isPlaying()
            );
            stateManager.updateTrackPosition(
                    currentlyPlayingFile.getAbsolutePath(),
                    mediaPlayer.getCurrentPosition()
            );

            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
            }
        }

        handler.removeCallbacks(updatePositionRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Restore scroll position
        if (currentDirectory != null) {
            stateManager.restoreFolderState(currentDirectory.getAbsolutePath(), fileListView);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        handler.removeCallbacksAndMessages(null);
    }

    public void refreshFileList() {
        if (currentDirectory != null) {
            loadFiles(currentDirectory);
        }

    }

    private void debugStorageAccess(String path) {
        Log.d("PLAY_DEBUG", "========== STORAGE ACCESS DEBUG ==========");

        // 1. Check if we have permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            boolean hasManageStorage = Environment.isExternalStorageManager();
            Log.d("PLAY_DEBUG", "Has MANAGE_EXTERNAL_STORAGE: " + hasManageStorage);
        } else {
            int permission = ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE);
            Log.d("PLAY_DEBUG", "READ_EXTERNAL_STORAGE: " +
                    (permission == PackageManager.PERMISSION_GRANTED ? "GRANTED" : "DENIED"));
        }

        // 2. Check all storage locations
        File[] externalDirs = requireContext().getExternalFilesDirs(null);
        Log.d("PLAY_DEBUG", "Number of storage locations: " + externalDirs.length);

        for (int i = 0; i < externalDirs.length; i++) {
            if (externalDirs[i] != null) {
                String fullPath = externalDirs[i].getAbsolutePath();
                Log.d("PLAY_DEBUG", "Storage " + i + ": " + fullPath);

                // Try to get root
                if (fullPath.contains("/Android")) {
                    String root = fullPath.substring(0, fullPath.indexOf("/Android"));
                    Log.d("PLAY_DEBUG", "  Root " + i + ": " + root);

                    // Check if root is accessible
                    File rootFile = new File(root);
                    Log.d("PLAY_DEBUG", "  Root exists: " + rootFile.exists());
                    Log.d("PLAY_DEBUG", "  Root can read: " + rootFile.canRead());
                    Log.d("PLAY_DEBUG", "  Root can list: " + (rootFile.list() != null ? "YES" : "NO"));
                }
            }
        }

        // 3. Check the specific path we're trying to access
        File targetDir = new File(path);
        Log.d("PLAY_DEBUG", "Target path: " + path);
        Log.d("PLAY_DEBUG", "Target exists: " + targetDir.exists());
        Log.d("PLAY_DEBUG", "Target is directory: " + targetDir.isDirectory());
        Log.d("PLAY_DEBUG", "Target can read: " + targetDir.canRead());

        if (targetDir.exists() && targetDir.isDirectory()) {
            String[] contents = targetDir.list();
            Log.d("PLAY_DEBUG", "Target contents count: " + (contents != null ? contents.length : "null"));
            if (contents != null) {
                for (int i = 0; i < Math.min(contents.length, 5); i++) {
                    Log.d("PLAY_DEBUG", "  Item " + i + ": " + contents[i]);
                }
            }
        }

        // 4. Check if we can create a test file
        try {
            File testFile = new File(targetDir, "test_write_permission.txt");
            if (testFile.createNewFile()) {
                Log.d("PLAY_DEBUG", "Can write to target directory: YES");
                testFile.delete();
            } else {
                Log.d("PLAY_DEBUG", "Can write to target directory: NO");
            }
        } catch (Exception e) {
            Log.d("PLAY_DEBUG", "Can write to target directory: EXCEPTION - " + e.getMessage());
        }

        Log.d("PLAY_DEBUG", "========== END DEBUG ==========");
    }
}