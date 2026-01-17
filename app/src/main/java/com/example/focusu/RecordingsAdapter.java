package com.example.focusu;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class RecordingsAdapter extends RecyclerView.Adapter<RecordingsAdapter.ViewHolder> {

    private List<Recording> recordingList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onPlayClick(Recording recording);
        void onMenuClick(Recording recording);
    }

    public RecordingsAdapter(List<Recording> recordingList, OnItemClickListener listener) {
        this.recordingList = recordingList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recording_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Recording recording = recordingList.get(position);

        holder.title.setText(recording.getTitle());
        holder.subject.setText(recording.getSubject());

        long minutes = TimeUnit.MILLISECONDS.toMinutes(recording.getDuration());
        long seconds = TimeUnit.MILLISECONDS.toSeconds(recording.getDuration()) % 60;
        String durationText = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);

        String metaText = recording.getDate() + " • " + durationText;
        holder.meta.setText(metaText);


        if (recording.isPlaying()) {
            holder.btnPlay.setImageResource(R.drawable.ic_pause);
            holder.itemView.setBackgroundColor(android.graphics.Color.parseColor("#E3F2FD"));
        } else {
            holder.btnPlay.setImageResource(R.drawable.ic_play);
            holder.itemView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        }

        // CLICK LISTENERS
        holder.btnPlay.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlayClick(recording);
            }
        });

        holder.btnMenu.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMenuClick(recording);
            }
        });
    }

    @Override
    public int getItemCount() {
        return recordingList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, subject, meta;
        ImageView btnPlay, btnMenu;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.recordingTitle);
            subject = itemView.findViewById(R.id.recordingSubject);
            meta = itemView.findViewById(R.id.recordingMeta);
            btnPlay = itemView.findViewById(R.id.btnPlayPause);
            btnMenu = itemView.findViewById(R.id.btnMenu);
        }
    }
}
