package com.german.learner.models;

import java.util.HashSet;
import java.util.Set;

public class TrackInfo {
    private String filePath;
    private String fileName;
    private String folderPath;
    private int playCount;
    private long lastPlayedPosition;
    private Set<String> tags;  // "Digits", "Words", "Dialog", "Dictionary"
    private int importanceLevel; // 1-5

    public TrackInfo(String filePath, String fileName, String folderPath) {
        this.filePath = filePath;
        this.fileName = fileName;
        this.folderPath = folderPath;
        this.playCount = 0;
        this.lastPlayedPosition = 0;
        this.tags = new HashSet<>();
        this.importanceLevel = 1;
    }

    // Getters and Setters
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFolderPath() { return folderPath; }
    public void setFolderPath(String folderPath) { this.folderPath = folderPath; }

    public int getPlayCount() { return playCount; }
    public void setPlayCount(int playCount) { this.playCount = playCount; }
    public void incrementPlayCount() { this.playCount++; }

    public long getLastPlayedPosition() { return lastPlayedPosition; }
    public void setLastPlayedPosition(long lastPlayedPosition) { this.lastPlayedPosition = lastPlayedPosition; }

    public Set<String> getTags() { return tags; }
    public void setTags(Set<String> tags) { this.tags = tags; }
    public void addTag(String tag) { this.tags.add(tag); }
    public void removeTag(String tag) { this.tags.remove(tag); }
    public boolean hasTag(String tag) { return tags.contains(tag); }

    public int getImportanceLevel() { return importanceLevel; }
    public void setImportanceLevel(int importanceLevel) {
        if (importanceLevel >= 1 && importanceLevel <= 5) {
            this.importanceLevel = importanceLevel;
        }
    }
}