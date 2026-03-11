package com.german.learner.models;

import java.util.HashMap;
import java.util.Map;

public class AppState {
    private Map<String, FolderState> folderStates;
    private Map<String, TrackInfo> trackMetadata;
    private PlaybackState currentPlayback;
    private Settings settings;

    public AppState() {
        this.folderStates = new HashMap<>();
        this.trackMetadata = new HashMap<>();
        this.currentPlayback = new PlaybackState();
        this.settings = new Settings();
    }

    // Getters and Setters
    public Map<String, FolderState> getFolderStates() { return folderStates; }
    public void setFolderStates(Map<String, FolderState> folderStates) { this.folderStates = folderStates; }

    public Map<String, TrackInfo> getTrackMetadata() { return trackMetadata; }
    public void setTrackMetadata(Map<String, TrackInfo> trackMetadata) { this.trackMetadata = trackMetadata; }

    public PlaybackState getCurrentPlayback() { return currentPlayback; }
    public void setCurrentPlayback(PlaybackState currentPlayback) { this.currentPlayback = currentPlayback; }

    public Settings getSettings() { return settings; }
    public void setSettings(Settings settings) { this.settings = settings; }

    // Folder state for each path
    public static class FolderState {
        private String path;
        private int scrollPosition;
        private int scrollOffset;
        private String lastSelectedFile;

        public FolderState(String path) {
            this.path = path;
            this.scrollPosition = 0;
            this.scrollOffset = 0;
            this.lastSelectedFile = "";
        }

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }

        public int getScrollPosition() { return scrollPosition; }
        public void setScrollPosition(int scrollPosition) { this.scrollPosition = scrollPosition; }

        public int getScrollOffset() { return scrollOffset; }
        public void setScrollOffset(int scrollOffset) { this.scrollOffset = scrollOffset; }

        public String getLastSelectedFile() { return lastSelectedFile; }
        public void setLastSelectedFile(String lastSelectedFile) { this.lastSelectedFile = lastSelectedFile; }
    }

    // Settings
    public static class Settings {
        private String primaryRootPath;
        private String secondaryRootPath;
        private boolean autoResumePlayback;
        private boolean showPlayCount;

        public Settings() {
            this.primaryRootPath = "";
            this.secondaryRootPath = "";
            this.autoResumePlayback = true;
            this.showPlayCount = true;
        }

        public String getPrimaryRootPath() { return primaryRootPath; }
        public void setPrimaryRootPath(String primaryRootPath) { this.primaryRootPath = primaryRootPath; }

        public String getSecondaryRootPath() { return secondaryRootPath; }
        public void setSecondaryRootPath(String secondaryRootPath) { this.secondaryRootPath = secondaryRootPath; }

        public boolean isAutoResumePlayback() { return autoResumePlayback; }
        public void setAutoResumePlayback(boolean autoResumePlayback) { this.autoResumePlayback = autoResumePlayback; }

        public boolean isShowPlayCount() { return showPlayCount; }
        public void setShowPlayCount(boolean showPlayCount) { this.showPlayCount = showPlayCount; }
    }
}