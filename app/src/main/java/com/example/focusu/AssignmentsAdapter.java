package com.example.focusu;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class AssignmentsAdapter extends RecyclerView.Adapter<AssignmentsAdapter.ViewHolder> {

    private List<Assignment> assignmentList;
    private OnItemClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    public interface OnItemClickListener {
        void onEditClick(Assignment assignment);
        void onDeleteClick(Assignment assignment);
    }

    public AssignmentsAdapter(List<Assignment> assignmentList, OnItemClickListener listener) {
        this.assignmentList = assignmentList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_assignment_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Assignment assignment = assignmentList.get(position);

        holder.title.setText(assignment.getTitle());
        holder.subject.setText(assignment.getSubject());
        holder.dueDate.setText("Due: " + assignment.getDueDate());

        Date dueDate = null;
        try {
            dueDate = dateFormat.parse(assignment.getDueDate());
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // --- TIME LEFT / STATUS CALCULATION ---
        if ("Done".equals(assignment.getDbStatus())) {
            holder.timeLeft.setText("Completed");
            holder.timeLeft.setTextColor(Color.parseColor("#388E3C")); // Green
        } else if (dueDate != null) {
            long diffInMillis = dueDate.getTime() - Calendar.getInstance().getTimeInMillis();
            long diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis) + 1; // +1 to be more intuitive

            if (diffInDays < 0) {
                holder.timeLeft.setText("Overdue");
                holder.timeLeft.setTextColor(Color.RED);
            } else if (diffInDays == 0) {
                holder.timeLeft.setText("Due Today");
                holder.timeLeft.setTextColor(Color.BLUE);
            } else if (diffInDays == 1) {
                holder.timeLeft.setText("Due Tomorrow");
                holder.timeLeft.setTextColor(Color.parseColor("#F57C00")); // Orange
            } else {
                holder.timeLeft.setText(diffInDays + " days left");
                holder.timeLeft.setTextColor(Color.DKGRAY);
            }
        } else {
            holder.timeLeft.setText(""); // Clear if date is invalid
        }

        // --- PRIORITY CHIP STYLING ---
        String priority = assignment.getPriority();
        holder.priority.setText(priority);

        switch (priority) {
            case "High":
                holder.priority.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#ffeaea")));
                holder.priority.setTextColor(Color.parseColor("#B71C1C"));
                break;
            case "Medium":
                holder.priority.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#ffe9a3")));
                holder.priority.setTextColor(Color.parseColor("#F57C00"));
                break;
            default: // Low
                holder.priority.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#c7e9c0")));
                holder.priority.setTextColor(Color.parseColor("#388E3C"));
                break;
        }

        // --- CLICK LISTENERS ---
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditClick(assignment);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(assignment);
        });
    }

    @Override
    public int getItemCount() {
        return assignmentList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, subject, dueDate, timeLeft;
        Chip priority;
        ImageView btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.assignmentTitle);
            subject = itemView.findViewById(R.id.assignmentSubject);
            dueDate = itemView.findViewById(R.id.assignmentDueDate);
            timeLeft = itemView.findViewById(R.id.assignmentTimeLeft);
            priority = itemView.findViewById(R.id.assignmentPriority);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
