package com.example.focusu;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import android.graphics.Color;

public class NotificationPopupAdapter extends RecyclerView.Adapter<NotificationPopupAdapter.ViewHolder> {

    private final List<PopupReminderItem> reminderItems;

    public NotificationPopupAdapter(List<PopupReminderItem> reminderItems) {
        this.reminderItems = reminderItems;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PopupReminderItem item = reminderItems.get(position);
        holder.title.setText(item.getTitle());
        holder.subtitle.setText(item.getSubtitle());

        if ("assignment".equals(item.getType())) {
            holder.icon.setImageResource(R.drawable.ic_assignment);
            holder.icon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#FF9800")));
        } else {
            holder.icon.setImageResource(R.drawable.ic_calendar);
            holder.icon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#F44336")));
        }
    }



    @Override
    public int getItemCount() {
        return reminderItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView title;
        TextView subtitle;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.notification_item_icon);
            title = itemView.findViewById(R.id.notification_item_title);
            subtitle = itemView.findViewById(R.id.notification_item_due);
        }
    }
}
