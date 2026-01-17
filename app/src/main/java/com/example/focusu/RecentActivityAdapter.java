package com.example.focusu;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class RecentActivityAdapter extends RecyclerView.Adapter<RecentActivityAdapter.ViewHolder> {

    private final List<RecentItem> recentItems;

    public RecentActivityAdapter(List<RecentItem> recentItems) {
        this.recentItems = recentItems;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_activity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecentItem item = recentItems.get(position);
        holder.title.setText(item.getTitle());
        holder.description.setText(item.getDescription());

        switch (item.getType()) {
            case "assignment":
                holder.icon.setImageResource(R.drawable.ic_assignment);
                break;
            case "exam":
                holder.icon.setImageResource(R.drawable.ic_exam);
                break;
            case "note":
                holder.icon.setImageResource(R.drawable.ic_note);
                break;
            case "recording":
                holder.icon.setImageResource(R.drawable.ic_mic);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return recentItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView title;
        TextView description;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.recent_item_icon);
            title = itemView.findViewById(R.id.recent_item_title);
            description = itemView.findViewById(R.id.recent_item_description);
        }
    }
    public void updateList(List<RecentItem> newList) {
        this.recentItems.clear();
        this.recentItems.addAll(newList);
        notifyDataSetChanged();
    }
}
