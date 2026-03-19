package com.german.learner.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.german.learner.models.AppState;
import com.german.learner.models.PlaybackState;
import com.german.learner.models.ScrollState;
import com.german.learner.models.TrackInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class StateManager {
    private static final String TAG = "StateManager";
    private static final String STATE_FILE = "deutsch_lerner_state.json";
    private static final String BACKUP_FILE = "deutsch_lerner_state_backup.json";

    private Context context;
    private Gson gson;
    private AppState currentState;
    private File stateFile;

    public StateManager(Context context) {
        this.context = context.getApplicationContext();
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                .create();
        this.stateFile = new File(context.getFilesDir(), STATE_FILE);
        loadState();
    }

    private void loadState() {
        if (stateFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(stateFile))) {
                StringBuilder json = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    json.append(line);
                }
                currentState = gson.fromJson(json.toString(), AppState.class);
                Log.d(TAG, "State loaded from: " + stateFile.getAbsolutePath());
            } catch (IOException e) {
                Log.e(TAG, "Error loading state", e);
                loadFromBackup();
            }
        }

        if (currentState == null) {
            currentState = new AppState();
            // Set default root paths for A1 and A2
            String defaultPath = Environment.getExternalStorageDirectory().getAbsolutePath();
            currentState.getSettings().getRootPath();
            Log.d(TAG, "Created new default state");
        }
    }

    private void loadFromBackup() {
        File backupFile = new File(context.getFilesDir(), BACKUP_FILE);
        if (backupFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(backupFile))) {
                StringBuilder json = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    json.append(line);
                }
                currentState = gson.fromJson(json.toString(), AppState.class);
                Log.d(TAG, "Loaded from backup");
            } catch (IOException e) {
                Log.e(TAG, "Error loading from backup", e);
            }
        }
    }

    public void saveState() {
        // DEBUG: Check what we are about to save
        if (currentState != null && currentState.getSettings() != null) {
            Log.d(TAG, "SAVING TO JSON - RootPath: " + currentState.getSettings().getRootPath());
        }

        File tempFile = new File(context.getFilesDir(), STATE_FILE + ".tmp");
        try (PrintWriter writer = new PrintWriter(new FileWriter(tempFile))) {
            String json = gson.toJson(currentState);
            writer.write(json);
            writer.flush();

            if (tempFile.exists()) {
                if (stateFile.exists()) {
                    File backupFile = new File(context.getFilesDir(), BACKUP_FILE);
                    if (backupFile.exists()) {
                        backupFile.delete();
                    }
                    stateFile.renameTo(backupFile);
                }
                tempFile.renameTo(stateFile);
            }
            Log.d(TAG, "State saved successfully to: " + stateFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Error saving state", e);
        }
    }

    // ============== Folder State Methods ==============

    public void saveFolderState(String folderPath, ListView listView) {
        if (folderPath == null || listView == null) return;

        int firstVisible = listView.getFirstVisiblePosition();
        View firstChild = listView.getChildAt(0);
        int top = (firstChild == null) ? 0 : firstChild.getTop();

        AppState.FolderState state = currentState.getFolderStates().get(folderPath);
        if (state == null) {
            state = new AppState.FolderState(folderPath);
        }
        state.setScrollPosition(firstVisible);
        state.setScrollOffset(top);

        currentState.getFolderStates().put(folderPath, state);
        saveState();
    }

    public void restoreFolderState(String folderPath, ListView listView) {
        AppState.FolderState state = currentState.getFolderStates().get(folderPath);
        if (state != null && listView != null) {
            listView.setSelectionFromTop(state.getScrollPosition(), state.getScrollOffset());
        }
    }

    public void setLastSelectedFile(String folderPath, String fileName) {
        AppState.FolderState state = currentState.getFolderStates().get(folderPath);
        if (state == null) {
            state = new AppState.FolderState(folderPath);
        }
        state.setLastSelectedFile(fileName);
        currentState.getFolderStates().put(folderPath, state);
        saveState();
    }

    public String getLastSelectedFile(String folderPath) {
        AppState.FolderState state = currentState.getFolderStates().get(folderPath);
        return state != null ? state.getLastSelectedFile() : "";
    }

    // ============== Track Metadata Methods ==============

    public TrackInfo getTrackInfo(String filePath) {
        TrackInfo info = currentState.getTrackMetadata().get(filePath);
        if (info == null) {
            File file = new File(filePath);
            info = new TrackInfo(filePath, file.getName(), file.getParent());
        }
        return info;
    }

    public void updateTrackInfo(TrackInfo info) {
        currentState.getTrackMetadata().put(info.getFilePath(), info);
        saveState();
    }

    public void addTagToTrack(String filePath, String tag) {
        TrackInfo info = getTrackInfo(filePath);
        info.addTag(tag);
        updateTrackInfo(info);
    }

    public void removeTagFromTrack(String filePath, String tag) {
        TrackInfo info = getTrackInfo(filePath);
        info.removeTag(tag);
        updateTrackInfo(info);
    }

    public void setTrackImportance(String filePath, int level) {
        TrackInfo info = getTrackInfo(filePath);
        info.setImportanceLevel(level);
        updateTrackInfo(info);
    }

    public void incrementPlayCount(String filePath) {
        TrackInfo info = getTrackInfo(filePath);
        info.incrementPlayCount();
        updateTrackInfo(info);
    }

    public void updateTrackPosition(String filePath, long position) {
        TrackInfo info = getTrackInfo(filePath);
        info.setLastPlayedPosition(position);
        updateTrackInfo(info);
    }

    // ============== Playback Methods ==============

    public void updatePlaybackState(String filePath, long position, boolean isPlaying) {
        currentState.getCurrentPlayback().setCurrentFilePath(filePath);
        currentState.getCurrentPlayback().setCurrentPosition(position);
        currentState.getCurrentPlayback().setPlaying(isPlaying);
        currentState.getCurrentPlayback().setLastUpdateTime(System.currentTimeMillis());
        saveState();
    }

    public PlaybackState getPlaybackState() {
        return currentState.getCurrentPlayback();
    }

    // ============== Settings Methods ==============

    public void setRootPath(String path) {
        currentState.getSettings().setRootPath(path);
        saveState();
    }

    public String getRootPath() {
        if (currentState == null || currentState.getSettings() == null) {
            return Environment.getExternalStorageDirectory().getAbsolutePath() + "/_DeutscheCourse";
        }

        String path = currentState.getSettings().getRootPath();

        // DEBUG: See what is actually in the object
        Log.d(TAG, "Retrieving RootPath from AppState: " + path);

        if (path == null || path.isEmpty()) {
            path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/_DeutscheCourse";
            Log.d(TAG, "Root path was empty, using default: " + path);
        }
        return path;
    }

    public boolean isAutoResumePlayback() {
        return currentState.getSettings().isAutoResumePlayback();
    }

    public void setAutoResumePlayback(boolean autoResume) {
        currentState.getSettings().setAutoResumePlayback(autoResume);
        saveState();
    }
    public void clearAllData() {
        if (stateFile.exists()) {
            stateFile.delete();
        }
        currentState = new AppState();
        saveState();
    }

    public boolean hasSavedState() {
        // Check if we have any saved folder state or playback state
        if (stateFile.exists()) {
            // Also check if we have actual content
            if (currentState != null) {
                // Check if we have any folder states or a non-empty playback state
                boolean hasFolderState = currentState.getFolderStates() != null &&
                        !currentState.getFolderStates().isEmpty();
                boolean hasPlayback = currentState.getCurrentPlayback() != null &&
                        currentState.getCurrentPlayback().getCurrentFilePath() != null &&
                        !currentState.getCurrentPlayback().getCurrentFilePath().isEmpty();
                boolean hasSettings = currentState.getSettings() != null &&
                        currentState.getSettings().getRootPath() != null &&
                        !currentState.getSettings().getRootPath().isEmpty();

                return hasFolderState || hasPlayback || hasSettings;
            }
            return true; // File exists, assume it has content
        }
        return false; // No config file
    }

    public String getCurrentFolder() {
        if (currentState != null) {
            String folder = currentState.getCurrentFolderPath();
            if (folder != null && !folder.isEmpty()) {
                return folder;
            }
        }
        // Fallback to root path
        return getRootPath();
    }

    public void setCurrentFolder(String folderPath) {
        if (currentState != null && folderPath != null) {
            currentState.setCurrentFolderPath(folderPath);
            saveState();
        }
    }


}