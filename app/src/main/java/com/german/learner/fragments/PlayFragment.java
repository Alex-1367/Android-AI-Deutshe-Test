package com.german.learner.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
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
import android.widget.Space;
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
    private TextView positionIndicator;

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
        positionIndicator = view.findViewById(R.id.position_indicator);

        stateManager = ((MainActivity) requireActivity()).getStateManager();

        setupViews();
        setupTagSelector();

// Get saved path from state
        String startPath = stateManager.getRootPath();
        File startDir = new File(startPath);

// Check if we have a last played file
        PlaybackState playbackState = stateManager.getPlaybackState();
        String targetPath = startPath; // Default to root

        if (playbackState != null && playbackState.getCurrentFilePath() != null &&
                !playbackState.getCurrentFilePath().isEmpty()) {

            File lastPlayed = new File(playbackState.getCurrentFilePath());
            if (lastPlayed.exists()) {
                // Navigate to the folder containing the last played file, not the file itself
                File parentDir = lastPlayed.getParentFile();
                if (parentDir != null && parentDir.exists()) {
                    targetPath = parentDir.getAbsolutePath();
                    currentlyPlayingFile = lastPlayed; // Save for later highlighting
                    Log.d("PLAY_DEBUG", "Last played file found in: " + targetPath);
                }
            }
        }

// Navigate to the target path (either root or last played file's folder)
        navigateToPath(targetPath);
        Log.d("PLAY_DEBUG", "Navigated to: " + targetPath);

// Restore playing state AFTER files are loaded
        restorePlayingState();

// Scroll to last position after list is populated
        fileListView.post(new Runnable() {
            @Override
            public void run() {
                // First restore saved scroll position
                if (currentDirectory != null) {
                    stateManager.restoreFolderState(currentDirectory.getAbsolutePath(), fileListView);
                }

                // Then if we have a currently playing file, make sure it's visible
                if (currentlyPlayingFile != null) {
                    scrollToFile(currentlyPlayingFile);
                }
            }
        });

        debugStorageAccess(startPath);

        return view;
    }

    private void scrollToFile(File file) {
        if (file == null || fileList.isEmpty()) return;

        for (int i = 0; i < fileList.size(); i++) {
            File f = fileList.get(i);
            if (f.getAbsolutePath().equals(file.getAbsolutePath())) {
                final int position = i;
                fileListView.post(new Runnable() {
                    @Override
                    public void run() {
                        fileListView.setSelection(position);
                        // Optionally highlight the item
                    }
                });
                break;
            }
        }
    }

    private void setupViews() {
        adapter = new FileListAdapter(requireContext(), fileList, stateManager, this);
        fileListView.setAdapter(adapter);
        fileListView.setClickable(true);
        fileListView.setItemsCanFocus(false);
        fileListView.setFocusable(false);
        fileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d("CLICK_DEBUG", "onItemClick called for position " + position);
                File file = fileList.get(position);

                if (file.isDirectory()) {
                    // For directories, show folder info
                    File[] files = file.listFiles();
                    int itemCount = files != null ? files.length : 0;
                    int mp3Count = 0;
                    if (files != null) {
                        for (File f : files) {
                            if (f.isFile() && f.getName().toLowerCase().endsWith(".mp3")) {
                                mp3Count++;
                            }
                        }
                    }
                    Toast.makeText(getContext(),
                            "📁 " + file.getName() + "\n" +
                                    "Contains: " + itemCount + " items (" + mp3Count + " audio files)",
                            Toast.LENGTH_LONG).show();

                    Log.d("PLAY_DEBUG", "CLICKED: " + file.getName() + " isFile=" + file.isFile() + " isAudio=" + isAudioFile(file));

                    // Navigate to directory
                    navigateToPath(file.getAbsolutePath());
                } else {
                    // For MP3 files, show file size and duration
                    long fileSize = file.length();
                    String sizeStr;
                    if (fileSize < 1024) {
                        sizeStr = fileSize + " B";
                    } else if (fileSize < 1024 * 1024) {
                        sizeStr = String.format(java.util.Locale.getDefault(), "%.1f KB", fileSize / 1024.0);
                    } else {
                        sizeStr = String.format(java.util.Locale.getDefault(), "%.1f MB", fileSize / (1024.0 * 1024.0));
                    }

                    // Get duration if possible (requires MediaPlayer)
                    String durationStr = "Unknown";
                    try {
                        MediaPlayer mp = new MediaPlayer();
                        mp.setDataSource(file.getAbsolutePath());
                        mp.prepare();
                        int duration = mp.getDuration() / 1000; // in seconds
                        mp.release();

                        int minutes = duration / 60;
                        int seconds = duration % 60;
                        durationStr = String.format(java.util.Locale.getDefault(), "%d:%02d", minutes, seconds);
                    } catch (Exception e) {
                        // Ignore duration errors
                    }

                    Toast.makeText(getContext(),
                            "🎵 " + file.getName() + "\n" +
                                    "Size: " + sizeStr + "\n" +
                                    "Duration: " + durationStr,
                            Toast.LENGTH_LONG).show();

                    Log.d("PLAY_DEBUG", "CLICKED: " + file.getName() + " isFile=" + file.isFile() + " isAudio=" + isAudioFile(file));

                    if (isAudioFile(file)) {
                        Log.d("PLAY_TEST", "CALLING playAudio for: " + file.getName());
                        playAudio(file);
                    } else {
                        Log.d("PLAY_TEST", "Not an audio file: " + file.getName());
                        Toast.makeText(getContext(), "Not an MP3 file", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        fileListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                File file = fileList.get(position);
                Log.d("PLAY_TEST", "LONG CLICK: " + file.getName());
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
                    String currentPath = currentDirectory.getAbsolutePath();
                    String rootPath = stateManager.getRootPath();

                    Log.d("NAV_CHECK", "Current: " + currentPath);
                    Log.d("NAV_CHECK", "Root: " + rootPath);

                    // Only navigate up if we are NOT at the root
                    if (!currentPath.equals(rootPath)) {
                        File parent = currentDirectory.getParentFile();
                        if (parent != null) {
                            navigateToPath(parent.getAbsolutePath());
                        }
                    } else {
                        Log.d("NAV_CHECK", "Navigation blocked: Already at Root.");
                        Toast.makeText(getContext(), "At root directory", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("PLAY_TEST", "Play/Pause clicked, isPlaying=" + isPlaying);
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
                    int currentPos = mediaPlayer.getCurrentPosition() / 1000; // seconds
                    int totalDur = mediaPlayer.getDuration() / 1000; // seconds

                    // Format as minutes:seconds
                    String currentStr = String.format(java.util.Locale.getDefault(), "%d:%02d",
                            currentPos / 60, currentPos % 60);
                    String totalStr = String.format(java.util.Locale.getDefault(), "%d:%02d",
                            totalDur / 60, totalDur % 60);

                    // Update position indicator
                    positionIndicator.setText(currentStr + "/" + totalStr);
                    positionIndicator.setVisibility(View.VISIBLE);

                    stateManager.updatePlaybackState(
                            currentlyPlayingFile.getAbsolutePath(),
                            mediaPlayer.getCurrentPosition(),
                            true
                    );
                    stateManager.updateTrackPosition(
                            currentlyPlayingFile.getAbsolutePath(),
                            mediaPlayer.getCurrentPosition()
                    );
                    handler.postDelayed(this, 1000); // Update every second
                }
            }
        };
    }

    private void setupTagSelector() {
        tagSelectorLayout.removeAllViews();
        tagSelectorLayout.setOrientation(LinearLayout.VERTICAL);
        tagSelectorLayout.setPadding(4, 4, 4, 4);

        // FIRST ROW - Tags
        LinearLayout tagRow = new LinearLayout(requireContext());
        tagRow.setOrientation(LinearLayout.HORIZONTAL);
        tagRow.setHorizontalScrollBarEnabled(true);
        tagRow.setPadding(0, 0, 0, 0);

        String[] tags = {"Digits", "Words", "Dialog", "Dict"};
        int[] tagColors = {
                getResources().getColor(android.R.color.holo_red_dark),
                getResources().getColor(android.R.color.holo_blue_dark),
                getResources().getColor(android.R.color.holo_green_dark),
                getResources().getColor(android.R.color.holo_orange_dark)
        };

        for (int i = 0; i < tags.length; i++) {
            final String tag = tags[i];
            TextView tagButton = createTagButton(tag, tagColors[i], Color.WHITE, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (selectedFileForTagging != null) {
                        toggleTag(selectedFileForTagging, tag);
                        tagSelectorLayout.setVisibility(View.GONE);
                    }
                }
            });
            tagRow.addView(tagButton);
        }
        tagSelectorLayout.addView(tagRow);

        // Spacing between rows
        Space space = new Space(requireContext());
        space.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 8));
        tagSelectorLayout.addView(space);

        // SECOND ROW - Grammar + Importance + Clear Tags + Close
        LinearLayout actionRow = new LinearLayout(requireContext());
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setPadding(0, 0, 0, 0);

        // Grammar button (wide)
        TextView grammarButton = createTagButton("Grammar",
                getResources().getColor(android.R.color.holo_purple),
                Color.WHITE,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (selectedFileForTagging != null) {
                            toggleTag(selectedFileForTagging, "Grammar");
                            tagSelectorLayout.setVisibility(View.GONE);
                        }
                    }
                });
        actionRow.addView(grammarButton);

        // Importance buttons 1-5
        for (int i = 1; i <= 5; i++) {
            final int level = i;
            TextView importanceButton = createSmallButton(String.valueOf(i),
                    Color.parseColor("#FFC107"),
                    Color.BLACK,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (selectedFileForTagging != null) {
                                stateManager.setTrackImportance(selectedFileForTagging.getAbsolutePath(), level);
                                adapter.notifyDataSetChanged();
                                tagSelectorLayout.setVisibility(View.GONE);
                                Toast.makeText(requireContext(), level + "★", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
            actionRow.addView(importanceButton);
        }

        // Clear tags button
        TextView clearTagsButton = createSmallButton("🗑",
                Color.parseColor("#F44336"),
                Color.WHITE,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (selectedFileForTagging != null) {
                            TrackInfo info = stateManager.getTrackInfo(selectedFileForTagging.getAbsolutePath());
                            if (info != null && info.getTags() != null && !info.getTags().isEmpty()) {
                                info.clearTags();
                                stateManager.updateTrackInfo(info);
                                adapter.notifyDataSetChanged();
                                tagSelectorLayout.setVisibility(View.GONE);
                                Toast.makeText(requireContext(),
                                        "All tags removed from " + selectedFileForTagging.getName(),
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(requireContext(), "No tags to remove", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
        actionRow.addView(clearTagsButton);

        // Close button
        TextView closeButton = createSmallButton("✕",
                Color.parseColor("#888888"),
                Color.WHITE,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        tagSelectorLayout.setVisibility(View.GONE);
                    }
                });
        actionRow.addView(closeButton);

        tagSelectorLayout.addView(actionRow);
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
            File targetDir = new File(path);
            if (!targetDir.exists() || !targetDir.isDirectory()) {
                Log.e("PLAY_DEBUG", "Target directory does not exist: " + path);
                return;
            }

            currentDirectory = targetDir;
            String rootPath = stateManager.getRootPath();

            // Debugging output as you requested
            Log.d("NAV_DEBUG", "Navigating to: " + path);
            Log.d("NAV_DEBUG", "Locked Root: " + rootPath);

            // Update the UP button state
            if (path.equals(rootPath)) {
                // We are at the root, user cannot go higher
                Log.d("NAV_DEBUG", "At Root - Disabling UP button");
                navigateUpButton.setEnabled(false);
                navigateUpButton.setAlpha(0.5f); // Visual feedback
            } else {
                // We are in a subfolder, user can go back up to root
                Log.d("NAV_DEBUG", "In Subfolder - Enabling UP button");
                navigateUpButton.setEnabled(true);
                navigateUpButton.setAlpha(1.0f);
            }

            currentPathTextView.setText(path);

            // Load the files for the new directory
            loadFiles(targetDir);

            currentPathTextView.setText(targetDir.getAbsolutePath());
            loadFiles(targetDir);
            stateManager.setCurrentFolder(targetDir.getAbsolutePath());
            // Restore scroll position for this folder
            stateManager.restoreFolderState(targetDir.getAbsolutePath(), fileListView);
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
        Log.d("PLAY_TEST", "playAudio called for: " + file.getName());
        try {
            // Completely destroy old player
            if (mediaPlayer != null) {
                try {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.stop();
                    }
                } catch (IllegalStateException e) {
                    // Ignore
                }
                mediaPlayer.release();
                mediaPlayer = null;
                handler.removeCallbacks(updatePositionRunnable);
            }

            // Create new player
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(file.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.setLooping(true);
            mediaPlayer.start();

            // Update state
            currentlyPlayingFile = file;
            isPlaying = true;

            if (adapter != null) {
                adapter.setCurrentlyPlayingFile(file.getAbsolutePath());
            }

            // Update UI
            nowPlayingTextView.setText(file.getName());
            playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
            playPauseButton.setBackgroundColor(Color.GREEN);

            // Show position indicator
            positionIndicator.setVisibility(View.VISIBLE);
            int totalDur = mediaPlayer.getDuration() / 1000;
            String totalStr = String.format(java.util.Locale.getDefault(), "%d:%02d",
                    totalDur / 60, totalDur % 60);
            positionIndicator.setText("0:00/" + totalStr);

            // Update play count
            stateManager.incrementPlayCount(file.getAbsolutePath());
            stateManager.setLastSelectedFile(currentDirectory.getAbsolutePath(), file.getName());

            // Start position updates
            handler.post(updatePositionRunnable);

            // Simple completion listener
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    isPlaying = false;
                    nowPlayingTextView.setText("Finished: " + file.getName());
                    playPauseButton.setImageResource(android.R.drawable.ic_media_play);
                    handler.removeCallbacks(updatePositionRunnable);

                    // Reset position indicator
                    positionIndicator.setText("0:00/" + totalStr);

                    stateManager.updatePlaybackState(file.getAbsolutePath(), 0, false);
                    stateManager.updateTrackPosition(file.getAbsolutePath(), 0);
                }
            });

            Log.d("PLAY_DEBUG", "Now playing: " + file.getName());

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Error playing: " + file.getName(), Toast.LENGTH_SHORT).show();
        }
    }

    private void pauseAudio() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPlaying = false;
            // Change to PLAY when paused
            playPauseButton.setImageResource(android.R.drawable.ic_media_play);
            playPauseButton.setBackgroundColor(Color.RED);

            if (adapter != null && currentlyPlayingFile != null) {
                adapter.setCurrentlyPlayingFile(currentlyPlayingFile.getAbsolutePath());
            }

            if (currentlyPlayingFile != null) {
                // Update indicator one last time with current position
                int currentPos = mediaPlayer.getCurrentPosition() / 1000;
                int totalDur = mediaPlayer.getDuration() / 1000;
                String currentStr = String.format(java.util.Locale.getDefault(), "%d:%02d",
                        currentPos / 60, currentPos % 60);
                String totalStr = String.format(java.util.Locale.getDefault(), "%d:%02d",
                        totalDur / 60, totalDur % 60);
                positionIndicator.setText(currentStr + "/" + totalStr);

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

            Toast.makeText(requireContext(), "Paused", Toast.LENGTH_SHORT).show();
        }
    }

    private void resumeAudio() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            isPlaying = true;
            // Change to PAUSE when playing
            playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
            playPauseButton.setBackgroundColor(Color.YELLOW);

            if (adapter != null && currentlyPlayingFile != null) {
                adapter.setCurrentlyPlayingFile(currentlyPlayingFile.getAbsolutePath());
            }

            if (currentlyPlayingFile != null) {
                stateManager.updatePlaybackState(
                        currentlyPlayingFile.getAbsolutePath(),
                        mediaPlayer.getCurrentPosition(),
                        true
                );
            }
            handler.post(updatePositionRunnable);

            Toast.makeText(requireContext(), "Resumed", Toast.LENGTH_SHORT).show();
        }
    }

    private void restorePlayingState() {
        PlaybackState playbackState = stateManager.getPlaybackState();
        if (playbackState != null && playbackState.getCurrentFilePath() != null &&
                !playbackState.getCurrentFilePath().isEmpty()) {

            File lastPlayed = new File(playbackState.getCurrentFilePath());
            if (lastPlayed.exists()) {
                currentlyPlayingFile = lastPlayed;

                if (adapter != null) {
                    adapter.setCurrentlyPlayingFile(lastPlayed.getAbsolutePath());
                }

                // Check if we're in the correct directory
                if (currentDirectory != null && lastPlayed.getParentFile() != null &&
                        lastPlayed.getParentFile().getAbsolutePath().equals(currentDirectory.getAbsolutePath())) {

                    String status = playbackState.isPlaying() ? "Paused at" : "Last played";
                    long position = playbackState.getCurrentPosition();
                    nowPlayingTextView.setText(String.format("%s: %s (%d sec)",
                            status,
                            lastPlayed.getName(),
                            position / 1000));

                    playPauseButton.setImageResource(android.R.drawable.ic_media_play);

                    // Save as last selected for scrolling
                    stateManager.setLastSelectedFile(currentDirectory.getAbsolutePath(), lastPlayed.getName());
                }
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

        // Restore playback state
        PlaybackState playbackState = stateManager.getPlaybackState();
        if (playbackState != null && playbackState.getCurrentFilePath() != null &&
                !playbackState.getCurrentFilePath().isEmpty()) {

            File lastPlayed = new File(playbackState.getCurrentFilePath());

            // If we're in the same directory as the last played file
            if (currentDirectory != null && lastPlayed.getParentFile() != null &&
                    lastPlayed.getParentFile().getAbsolutePath().equals(currentDirectory.getAbsolutePath())) {

                currentlyPlayingFile = lastPlayed;

                String status = playbackState.isPlaying() ? "Paused at" : "Last played";
                long position = playbackState.getCurrentPosition();
                nowPlayingTextView.setText(String.format("%s: %s (%d sec)",
                        status,
                        lastPlayed.getName(),
                        position / 1000));

                playPauseButton.setImageResource(android.R.drawable.ic_media_play);

                // Scroll to the file
                scrollToFile(lastPlayed);
            }
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

    private void dumpStateInfo() {
        Log.d("STATE_DEBUG", "========== STATE DIAGNOSTIC ==========");

        // Check if state file exists
        File stateFile = new File(requireContext().getFilesDir(), "deutsch_lerner_state.json");
        Log.d("STATE_DEBUG", "State file exists: " + stateFile.exists());
        if (stateFile.exists()) {
            Log.d("STATE_DEBUG", "State file size: " + stateFile.length() + " bytes");
        }

        // Check hasSavedState()
        boolean hasState = stateManager.hasSavedState();
        Log.d("STATE_DEBUG", "hasSavedState(): " + hasState);

        // Get root path
        String rootPath = stateManager.getRootPath();
        Log.d("STATE_DEBUG", "Root path: " + rootPath);
        File rootDir = new File(rootPath);
        Log.d("STATE_DEBUG", "Root exists: " + rootDir.exists());

        // Get current directory from state
        String savedPath = stateManager.getCurrentFolder();
        Log.d("STATE_DEBUG", "Saved current folder: " + savedPath);

        // Get playback state
        PlaybackState playbackState = stateManager.getPlaybackState();
        if (playbackState != null) {
            Log.d("STATE_DEBUG", "Playback state exists: true");
            Log.d("STATE_DEBUG", "  Current file: " + playbackState.getCurrentFilePath());
            Log.d("STATE_DEBUG", "  Position: " + playbackState.getCurrentPosition());
            Log.d("STATE_DEBUG", "  Is playing: " + playbackState.isPlaying());
            Log.d("STATE_DEBUG", "  Last update: " + playbackState.getLastUpdateTime());

            File lastPlayed = new File(playbackState.getCurrentFilePath());
            Log.d("STATE_DEBUG", "  Last played file exists: " + lastPlayed.exists());
            if (lastPlayed.exists()) {
                Log.d("STATE_DEBUG", "  Last played parent: " + lastPlayed.getParent());
            }
        } else {
            Log.d("STATE_DEBUG", "Playback state: null");
        }

        // Get folder states
        Log.d("STATE_DEBUG", "Current directory: " + (currentDirectory != null ? currentDirectory.getAbsolutePath() : "null"));

        // Get last selected file for current directory
        if (currentDirectory != null) {
            String lastSelected = stateManager.getLastSelectedFile(currentDirectory.getAbsolutePath());
            Log.d("STATE_DEBUG", "Last selected file in current dir: " + lastSelected);
        }

        Log.d("STATE_DEBUG", "========================================");
    }
    private TextView createTagButton(String text, int backgroundColor, int textColor, View.OnClickListener clickListener) {
        TextView button = new TextView(requireContext());
        button.setText(text);
        button.setBackgroundColor(backgroundColor);
        button.setTextSize(16);
        button.setPadding(8, 4, 8, 4);
        button.setMinWidth(0);
        button.setMinHeight(0);
        button.setHeight(70);
        button.setWidth(160);
        button.setGravity(android.view.Gravity.CENTER);
        button.setTextColor(textColor);
        button.setIncludeFontPadding(false);
        button.setOnClickListener(clickListener);
        return button;
    }

    private TextView createSmallButton(String text, int backgroundColor, int textColor, View.OnClickListener clickListener) {
        TextView button = new TextView(requireContext());
        button.setText(text);
        button.setTextSize(12);
        button.setPadding(4, 2, 4, 2);
        button.setMinWidth(0);
        button.setMinHeight(0);
        button.setHeight(70);
        button.setWidth(70);
        button.setGravity(android.view.Gravity.CENTER);
        button.setBackgroundColor(backgroundColor);
        button.setTextColor(textColor);
        button.setIncludeFontPadding(false);
        button.setOnClickListener(clickListener);
        return button;
    }
}