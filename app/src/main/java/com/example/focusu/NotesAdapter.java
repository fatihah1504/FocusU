package com.example.focusu;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.io.File;
import java.util.List;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.ViewHolder> {

    private List<Note> notesList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onEditClick(Note note);
        void onDeleteClick(Note note);
        void onImageClick(Note note); // This handles both image previews and opening docs
    }

    public NotesAdapter(List<Note> notesList, OnItemClickListener listener) {
        this.notesList = notesList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Note note = notesList.get(position);

        holder.title.setText(note.getTitle());
        holder.date.setText(note.getDate());
        holder.content.setText(note.getContent());

        android.text.util.Linkify.addLinks(holder.content, android.text.util.Linkify.WEB_URLS);

        holder.image.setVisibility(View.GONE);
        holder.fileAttachmentInfo.setVisibility(View.GONE);

        String path = note.getImagePath();
        if (path != null && !path.isEmpty()) {
            String lowerCasePath = path.toLowerCase();

            if (lowerCasePath.endsWith(".jpg") || lowerCasePath.endsWith(".jpeg") ||
                    lowerCasePath.endsWith(".png") || lowerCasePath.endsWith(".gif")) {

                holder.image.setVisibility(View.VISIBLE);
                Glide.with(holder.itemView.getContext())
                        .load(new File(path))
                        .into(holder.image);

                holder.image.setOnClickListener(v -> {
                    if (listener != null) listener.onImageClick(note);
                });

            } else {
                holder.fileAttachmentInfo.setVisibility(View.VISIBLE);
                holder.image.setVisibility(View.GONE);
                holder.fileAttachmentName.setText(new File(path).getName());

                holder.fileAttachmentInfo.setOnClickListener(v -> {
                    if (listener != null) listener.onImageClick(note);
                });
            }
        }

        holder.btnEdit.setOnClickListener(v -> listener.onEditClick(note));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(note));
        holder.itemView.setOnClickListener(v -> listener.onEditClick(note));
    }

    @Override
    public int getItemCount() {
        return notesList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, date, content, fileAttachmentName;
        ImageView image, btnEdit, btnDelete;
        LinearLayout fileAttachmentInfo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.noteTitle);
            date = itemView.findViewById(R.id.noteDate);
            content = itemView.findViewById(R.id.noteContent);
            image = itemView.findViewById(R.id.noteImage);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            fileAttachmentInfo = itemView.findViewById(R.id.fileAttachmentInfo);
            fileAttachmentName = itemView.findViewById(R.id.fileAttachmentName);
        }
    }
}
