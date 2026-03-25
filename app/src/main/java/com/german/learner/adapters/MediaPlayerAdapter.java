package com.german.learner.adapters;

import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.german.learner.utils.StateManager;

import java.io.File;
import java.io.IOException;

public class MediaPlayerAdapter {
    private static final String TAG = "MediaPlayerAdapter";

    private MediaPlayer mediaPlayer;
    private File currentlyPlayingFile;
    private boolean isPlaying = false;
    private Handler handler = new Handler();
    private Runnable updatePositionRunnable;

    private StateManager stateManager;
    private TextView nowPlayingTextView;
    private ImageButton playPauseButton;
    private TextView positionIndicator;
    private FileListAdapter fileListAdapter;

    public MediaPlayerAdapter(StateManager stateManager,
                              TextView nowPlayingTextView,
                              ImageButton playPauseButton,
                              TextView positionIndicator,
                              FileListAdapter fileListAdapter) {
        this.stateManager = stateManager;
        this.nowPlayingTextView = nowPlayingTextView;
        this.playPauseButton = playPauseButton;
        this.positionIndicator = positionIndicator;
        this.fileListAdapter = fileListAdapter;

        setupPositionUpdater();
    }

    private void setupPositionUpdater() {
        updatePositionRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying() && currentlyPlayingFile != null) {
                    int currentPos = mediaPlayer.getCurrentPosition() / 1000;
                    int totalDur = mediaPlayer.getDuration() / 1000;

                    String currentStr = String.format(java.util.Locale.getDefault(), "%d:%02d",
                            currentPos / 60, currentPos % 60);
                    String totalStr = String.format(java.util.Locale.getDefault(), "%d:%02d",
                            totalDur / 60, totalDur % 60);

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
                    handler.postDelayed(this, 1000);
                }
            }
        };
    }

    public void playAudio(File file) {
        Log.d(TAG, "playAudio called for: " + file.getName());
        try {
            // Clean up old player
            releasePlayer();

            // Create new player
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(file.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.setLooping(true);
            mediaPlayer.start();

            // Update state
            currentlyPlayingFile = file;
            isPlaying = true;

            // Update adapter highlighting
            if (fileListAdapter != null) {
                fileListAdapter.setCurrentlyPlayingFile(file.getAbsolutePath());
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

            // Start position updates
            handler.post(updatePositionRunnable);

            // Completion listener
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    isPlaying = false;
                    nowPlayingTextView.setText("Finished: " + file.getName());
                    playPauseButton.setImageResource(android.R.drawable.ic_media_play);
                    handler.removeCallbacks(updatePositionRunnable);
                    positionIndicator.setText("0:00/" + totalStr);

                    stateManager.updatePlaybackState(file.getAbsolutePath(), 0, false);
                    stateManager.updateTrackPosition(file.getAbsolutePath(), 0);
                }
            });

            Log.d(TAG, "Now playing: " + file.getName());

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(nowPlayingTextView.getContext(),
                    "Error playing: " + file.getName(), Toast.LENGTH_SHORT).show();
        }
    }

    public void pauseAudio() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPlaying = false;
            playPauseButton.setImageResource(android.R.drawable.ic_media_play);
            playPauseButton.setBackgroundColor(Color.RED);

            if (currentlyPlayingFile != null && fileListAdapter != null) {
                fileListAdapter.setCurrentlyPlayingFile(currentlyPlayingFile.getAbsolutePath());
            }

            if (currentlyPlayingFile != null) {
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

            Toast.makeText(nowPlayingTextView.getContext(), "Paused", Toast.LENGTH_SHORT).show();
        }
    }

    public void resumeAudio() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            isPlaying = true;
            playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
            playPauseButton.setBackgroundColor(Color.YELLOW);

            if (currentlyPlayingFile != null && fileListAdapter != null) {
                fileListAdapter.setCurrentlyPlayingFile(currentlyPlayingFile.getAbsolutePath());
            }

            if (currentlyPlayingFile != null) {
                stateManager.updatePlaybackState(
                        currentlyPlayingFile.getAbsolutePath(),
                        mediaPlayer.getCurrentPosition(),
                        true
                );
            }
            handler.post(updatePositionRunnable);

            Toast.makeText(nowPlayingTextView.getContext(), "Resumed", Toast.LENGTH_SHORT).show();
        }
    }

    public void releasePlayer() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error stopping media player", e);
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        handler.removeCallbacks(updatePositionRunnable);
    }

    public void savePlaybackState() {
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
    }

    public void restorePlaybackState(File lastPlayed, long position, boolean wasPlaying) {
        if (lastPlayed != null && lastPlayed.exists()) {
            currentlyPlayingFile = lastPlayed;

            if (fileListAdapter != null) {
                fileListAdapter.setCurrentlyPlayingFile(lastPlayed.getAbsolutePath());
            }

            String status = wasPlaying ? "Paused at" : "Last played";
            nowPlayingTextView.setText(String.format("%s: %s (%d sec)",
                    status,
                    lastPlayed.getName(),
                    position / 1000));

            playPauseButton.setImageResource(android.R.drawable.ic_media_play);

            // Note: We don't actually restore the MediaPlayer here
            // because it would require preparing the file again
            // The actual playback will start when user clicks play
        }
    }
    public static String getAudioDuration(File file) {
        String durationStr = "Unknown";
        MediaPlayer mp = null;
        try {
            mp = new MediaPlayer();
            mp.setDataSource(file.getAbsolutePath());
            mp.prepare();
            int duration = mp.getDuration() / 1000; // in seconds
            int minutes = duration / 60;
            int seconds = duration % 60;
            durationStr = String.format(java.util.Locale.getDefault(), "%d:%02d", minutes, seconds);
        } catch (Exception e) {
            Log.e(TAG, "Error getting duration for: " + file.getName(), e);
        } finally {
            if (mp != null) {
                try {
                    mp.release();
                } catch (Exception e) {
                    Log.e(TAG, "Error releasing MediaPlayer", e);
                }
            }
        }
        return durationStr;
    }

    public File getCurrentlyPlayingFile() {
        return currentlyPlayingFile;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public boolean hasActivePlayer() {
        return mediaPlayer != null;
    }
}