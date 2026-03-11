package com.german.learner.models;

public class ScrollState {
    private String folderPath;
    private int firstVisiblePosition;
    private int visibleTop;

    public ScrollState(String folderPath) {
        this.folderPath = folderPath;
        this.firstVisiblePosition = 0;
        this.visibleTop = 0;
    }

    public ScrollState(String folderPath, int firstVisiblePosition, int visibleTop) {
        this.folderPath = folderPath;
        this.firstVisiblePosition = firstVisiblePosition;
        this.visibleTop = visibleTop;
    }

    // Getters and Setters
    public String getFolderPath() { return folderPath; }
    public void setFolderPath(String folderPath) { this.folderPath = folderPath; }

    public int getFirstVisiblePosition() { return firstVisiblePosition; }
    public void setFirstVisiblePosition(int firstVisiblePosition) { this.firstVisiblePosition = firstVisiblePosition; }

    public int getVisibleTop() { return visibleTop; }
    public void setVisibleTop(int visibleTop) { this.visibleTop = visibleTop; }
}