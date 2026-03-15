package com.german.learner.adapters;

import android.content.Context;
import android.graphics.Color;
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

    public FileListAdapter(Context context, List<File> files, StateManager stateManager, PlayFragment playFragment) {
        this.context = context;
        this.files = files;
        this.stateManager = stateManager;
        this.playFragment = playFragment;
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
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_file, parent, false);
            holder = new ViewHolder();
            //holder.iconView = convertView.findViewById(R.id.file_icon);
            holder.nameView = convertView.findViewById(R.id.file_name);
            // REMOVED: holder.detailsView initialization - THIS WAS CAUSING THE CRASH
            holder.tagButton = convertView.findViewById(R.id.tag_button);
            //holder.tagContainer = convertView.findViewById(R.id.tag_container);
            holder.importanceView = convertView.findViewById(R.id.importance_indicator);
            holder.playCountView = convertView.findViewById(R.id.play_count);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        File file = files.get(position);

        if (file.isDirectory()) {
            //holder.iconView.setImageResource(android.R.drawable.ic_menu_gallery);
            holder.nameView.setText(file.getName() + "/");

            // REMOVED: All code that used detailsView

            // Hide tag button for directories
            holder.tagButton.setVisibility(View.GONE);

            // Hide other views
            //holder.tagContainer.setVisibility(View.GONE);
            holder.importanceView.setVisibility(View.GONE);
            holder.playCountView.setVisibility(View.GONE);
        } else {
            //holder.iconView.setImageResource(android.R.drawable.ic_media_play);
            holder.nameView.setText(file.getName());

            // REMOVED: setting detailsView text - THIS WAS CAUSING THE CRASH

            // Show tag button
            holder.tagButton.setVisibility(View.VISIBLE);

            // Handle tag button click
            holder.tagButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (playFragment != null) {
                        playFragment.showTagSelector(file);
                    }
                }
            });

            // Show tags and metadata (KEPT)
            TrackInfo info = stateManager.getTrackInfo(file.getAbsolutePath());
            if (info != null) {
                // Show tags
                //holder.tagContainer.removeAllViews();
                if (info.getTags() != null && !info.getTags().isEmpty()) {
                    //holder.tagContainer.setVisibility(View.VISIBLE);
                    for (String tag : info.getTags()) {
                        TextView tagView = new TextView(context);
                        tagView.setText(tag);
                        tagView.setTextSize(10);
                        tagView.setPadding(8, 4, 8, 4);
                        tagView.setBackgroundColor(getTagColor(tag));
                        tagView.setTextColor(Color.WHITE);
                        //holder.tagContainer.addView(tagView);
                    }
                } else {
                    //holder.tagContainer.setVisibility(View.GONE);
                }

                // Show importance (KEPT)
                if (info.getImportanceLevel() > 0) {
                    holder.importanceView.setVisibility(View.VISIBLE);
                    StringBuilder stars = new StringBuilder();
                    for (int i = 0; i < info.getImportanceLevel(); i++) {
                        stars.append("★");
                    }
                    holder.importanceView.setText(stars.toString());
                    holder.importanceView.setTextColor(Color.parseColor("#FFC107"));
                } else {
                    holder.importanceView.setVisibility(View.GONE);
                }

                // Show play count (KEPT)
                if (info.getPlayCount() > 0) {
                    holder.playCountView.setVisibility(View.VISIBLE);
                    holder.playCountView.setText("Played: " + info.getPlayCount());
                } else {
                    holder.playCountView.setVisibility(View.GONE);
                }
            }
        }

        // Handle tag button click
        holder.tagButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (playFragment != null) {
                    playFragment.showTagSelector(file);
                }
            }
        });
        holder.tagButton.setFocusable(false);
        //holder.iconView.setFocusable(false);
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

    private int countMp3Files(File[] files) {
        if (files == null) return 0;
        int count = 0;
        for (File file : files) {
            if (file.isFile() && file.getName().toLowerCase().endsWith(".mp3")) {
                count++;
            }
        }
        return count;
    }

    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format(Locale.getDefault(), "%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }

    static class ViewHolder {
        ImageView iconView;
        TextView nameView;
        // REMOVED: TextView detailsView; - THIS WAS CAUSING THE CRASH
        Button tagButton;
        //LinearLayout tagContainer;
        TextView importanceView;
        TextView playCountView;
    }
}