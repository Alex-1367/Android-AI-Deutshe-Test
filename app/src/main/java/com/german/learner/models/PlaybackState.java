package com.german.learner.models;

public class PlaybackState {
    private String currentFilePath;
    private long currentPosition;
    private boolean isPlaying;
    private long lastUpdateTime;

    public PlaybackState() {
        this.currentFilePath = "";
        this.currentPosition = 0;
        this.isPlaying = false;
        this.lastUpdateTime = 0;
    }

    public PlaybackState(String currentFilePath, long currentPosition, boolean isPlaying) {
        this.currentFilePath = currentFilePath;
        this.currentPosition = currentPosition;
        this.isPlaying = isPlaying;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getCurrentFilePath() { return currentFilePath; }
    public void setCurrentFilePath(String currentFilePath) { this.currentFilePath = currentFilePath; }

    public long getCurrentPosition() { return currentPosition; }
    public void setCurrentPosition(long currentPosition) { this.currentPosition = currentPosition; }

    public boolean isPlaying() { return isPlaying; }
    public void setPlaying(boolean playing) { isPlaying = playing; }

    public long getLastUpdateTime() { return lastUpdateTime; }
    public void setLastUpdateTime(long lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }
}