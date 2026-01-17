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

public class ExamsAdapter extends RecyclerView.Adapter<ExamsAdapter.ViewHolder> {

    private List<Exam> examList;
    private OnItemClickListener listener;

    private final SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat displayFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

    public interface OnItemClickListener {
        void onEditClick(Exam exam);
        void onDeleteClick(Exam exam);
    }

    public ExamsAdapter(List<Exam> examList, OnItemClickListener listener) {
        this.examList = examList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_exam_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Exam exam = examList.get(position);

        holder.subject.setText(exam.getSubject());
        holder.type.setText(exam.getType());
        holder.location.setText(exam.getLocation());

        Date examDate = null;
        try {
            examDate = dbFormat.parse(exam.getDate());
        } catch (ParseException e) {
            e.printStackTrace();
        }

        String formattedDate = exam.getDate();
        if (examDate != null) {
            formattedDate = displayFormat.format(examDate);
        }

        // This line now correctly displays the time with AM/PM
        holder.dateTime.setText(formattedDate + " at " + exam.getTime());

        // --- TIME LEFT CALCULATION (remains the same) ---
        if (examDate != null) {
            long diffInMillis = examDate.getTime() - Calendar.getInstance().getTimeInMillis();
            long diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis);

            if (diffInDays < -1) {
                holder.timeLeft.setText("Finished");
                holder.timeLeft.setTextColor(Color.parseColor("#388E3C")); // Green
            } else if (diffInDays == -1) {
                holder.timeLeft.setText("Yesterday");
                holder.timeLeft.setTextColor(Color.GRAY);
            } else if (diffInDays == 0) {
                holder.timeLeft.setText("Today");
                holder.timeLeft.setTextColor(Color.BLUE);
            } else if (diffInDays == 1) {
                holder.timeLeft.setText("Tomorrow");
                holder.timeLeft.setTextColor(Color.parseColor("#F57C00")); // Orange
            } else {
                holder.timeLeft.setText(diffInDays + " days left");
                holder.timeLeft.setTextColor(Color.DKGRAY);
            }
        } else {
            holder.timeLeft.setText("");
        }

        // Priority chip styling
        String priority = exam.getPriority();
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

        // Set click listeners
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditClick(exam);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(exam);
        });
    }

    @Override
    public int getItemCount() {
        return examList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView subject, type, dateTime, location, timeLeft;
        Chip priority;
        ImageView btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            subject = itemView.findViewById(R.id.examSubject);
            type = itemView.findViewById(R.id.examType);
            dateTime = itemView.findViewById(R.id.examDateTime);
            location = itemView.findViewById(R.id.examLocation);
            priority = itemView.findViewById(R.id.examPriority);
            timeLeft = itemView.findViewById(R.id.timeLeft);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
