package com.german.learner.fragments;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
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

        // Restore playing state
        restorePlayingState();

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

    private void showTagSelector(File file) {
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

        File[] files = directory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory() ||
                        (file.isFile() && file.getName().toLowerCase().endsWith(".mp3"));
            }
        });

        if (files != null) {
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File f1, File f2) {
                    if (f1.isDirectory() && !f2.isDirectory()) return -1;
                    if (!f1.isDirectory() && f2.isDirectory()) return 1;
                    return f1.getName().compareToIgnoreCase(f2.getName());
                }
            });

            Collections.addAll(fileList, files);
        }

        adapter.notifyDataSetChanged();
        navigateUpButton.setEnabled(!directory.getAbsolutePath().equals("/"));
    }

    private boolean isAudioFile(File file) {
        return file.isFile() && file.getName().toLowerCase().endsWith(".mp3");
    }

    private void playAudio(File file) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
                handler.removeCallbacks(updatePositionRunnable);
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(file.getAbsolutePath());
            mediaPlayer.prepare();

            // Restore position if we've played this before
            TrackInfo info = stateManager.getTrackInfo(file.getAbsolutePath());
            if (info != null && info.getLastPlayedPosition() > 0) {
                mediaPlayer.seekTo((int) info.getLastPlayedPosition());
            }

            mediaPlayer.start();

            currentlyPlayingFile = file;
            isPlaying = true;

            nowPlayingTextView.setText("Now playing: " + file.getName());
            playPauseButton.setImageResource(android.R.drawable.ic_media_pause);

            // Update play count
            stateManager.incrementPlayCount(file.getAbsolutePath());
            stateManager.setLastSelectedFile(currentDirectory.getAbsolutePath(), file.getName());

            // Start position updates
            handler.post(updatePositionRunnable);

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    isPlaying = false;
                    nowPlayingTextView.setText("Finished: " + file.getName());
                    playPauseButton.setImageResource(android.R.drawable.ic_media_play);
                    handler.removeCallbacks(updatePositionRunnable);

                    // Clear playback state when finished
                    stateManager.updatePlaybackState(file.getAbsolutePath(), 0, false);
                    stateManager.updateTrackPosition(file.getAbsolutePath(), 0);
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Error playing audio", Toast.LENGTH_SHORT).show();
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
}