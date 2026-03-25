package com.german.learner.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.german.learner.R;
import com.german.learner.fragments.PlayFragment;
import com.german.learner.models.TrackInfo;
import com.german.learner.utils.StateManager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FileListAdapter extends BaseAdapter {

    private Context context;
    private List<File> files;
    private StateManager stateManager;
    private PlayFragment playFragment;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
    private String currentlyPlayingPath = null;

    public FileListAdapter(Context context, List<File> files, StateManager stateManager, PlayFragment playFragment) {
        this.context = context;
        this.files = files;
        this.stateManager = stateManager;
        this.playFragment = playFragment;
    }

    public void setCurrentlyPlayingFile(String filePath) {
        this.currentlyPlayingPath = filePath;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return files.size();
    }

    @Override
    public Object getItem(int position) {
        return files.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Log.d("CLICK_DEBUG", "getView for position " + position);
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_file, parent, false);
            holder = new ViewHolder();
            holder.nameView = convertView.findViewById(R.id.file_name);
            holder.tagButton = convertView.findViewById(R.id.tag_button);
            holder.tagContainer = convertView.findViewById(R.id.tag_container);  // UNCOMMENTED
            holder.importanceView = convertView.findViewById(R.id.importance_indicator);
            holder.playCountView = convertView.findViewById(R.id.play_count);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        File file = files.get(position);

        if (file.isDirectory()) {
            holder.nameView.setText(file.getName() + "/");
            holder.tagButton.setVisibility(View.GONE);
            if (holder.tagContainer != null) {
                holder.tagContainer.setVisibility(View.GONE);
            }
            holder.importanceView.setVisibility(View.GONE);
            holder.playCountView.setVisibility(View.GONE);
        } else {
            holder.nameView.setText(file.getName());
            holder.tagButton.setVisibility(View.VISIBLE);

            // Handle tag button click
           holder.tagButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    v.setPressed(false);
                    if (playFragment != null) {
                        playFragment.showTagSelector(file);
                    }
                }
            });
            holder.tagButton.setFocusable(false);

            holder.nameView.setFocusable(false);

            // Show tags and metadata
            TrackInfo info = stateManager.getTrackInfo(file.getAbsolutePath());
            if (info != null) {
                // Show tags
                if (holder.tagContainer != null) {
                    holder.tagContainer.removeAllViews();
                    if (info.getTags() != null && !info.getTags().isEmpty()) {
                        holder.tagContainer.setVisibility(View.VISIBLE);
                        for (String tag : info.getTags()) {
                            TextView tagView = new TextView(context);
                            tagView.setText(tag);
                            tagView.setTextSize(8); // Smaller
                            tagView.setPadding(2, 1, 2, 1);
                            tagView.setBackgroundColor(getTagColor(tag));
                            tagView.setTextColor(Color.WHITE);
                            holder.tagContainer.addView(tagView);
                        }
                    } else {
                        holder.tagContainer.setVisibility(View.GONE);
                    }
                }

// Show importance
                if (info.getImportanceLevel() > 0) {
                    holder.importanceView.setVisibility(View.VISIBLE);
                    StringBuilder stars = new StringBuilder();
                    for (int i = 0; i < info.getImportanceLevel(); i++) {
                        stars.append("★");
                    }
                    holder.importanceView.setText(stars.toString());
                    holder.importanceView.setTextColor(Color.parseColor("#FFC107"));
                    holder.importanceView.setTextSize(10); // Match tag text size (8-10sp)
                } else {
                    holder.importanceView.setVisibility(View.GONE);
                }

                // Show play count
                if (info.getPlayCount() > 0) {
                    holder.playCountView.setVisibility(View.VISIBLE);
                    holder.playCountView.setText("Played: " + info.getPlayCount());
                } else {
                    holder.playCountView.setVisibility(View.GONE);
                }
            }
            boolean isCurrentlyPlaying = file.getAbsolutePath().equals(currentlyPlayingPath);
            if (isCurrentlyPlaying) {
                convertView.setBackgroundColor(Color.parseColor("#E3F2FD")); // Light blue background
                holder.nameView.setTextColor(Color.parseColor("#1976D2")); // Darker blue text
                holder.nameView.setTypeface(null, Typeface.BOLD);
            } else {
                convertView.setBackgroundColor(Color.TRANSPARENT);
                holder.nameView.setTextColor(Color.BLACK);
                holder.nameView.setTypeface(null, Typeface.NORMAL);
            }
        }

        return convertView;
    }

    private int getTagColor(String tag) {
        switch (tag) {
            case "Digits": return Color.parseColor("#F44336");
            case "Words": return Color.parseColor("#2196F3");
            case "Dialog": return Color.parseColor("#4CAF50");
            case "Dictionary": return Color.parseColor("#FF9800");
            case "Grammar": return Color.parseColor("#9C27B0");
            default: return Color.parseColor("#607D8B");
        }
    }

    static class ViewHolder {
        TextView nameView;
        Button tagButton;
        LinearLayout tagContainer;  // UNCOMMENTED
        TextView importanceView;
        TextView playCountView;
    }
}