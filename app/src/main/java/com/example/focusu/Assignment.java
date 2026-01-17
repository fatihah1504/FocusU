package com.example.focusu;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Assignment {
    private String id;
    private String title;
    private String description;
    private String dueDate; // Format: YYYY-MM-DD
    private String status;  // "Pending" or "Done"
    private String subject;
    private String priority; // "High", "Medium", "Low"
    private long timestamp;



    public long getTimestamp()
    {
        return timestamp;
    }


    public Assignment() {

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    // Getters
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getDueDate() { return dueDate; }
    public String getSubject() { return subject; }
    public String getPriority() { return priority; }


    // Status Logic: Returns "Done", "Overdue", or "Upcoming"
    public String getCalculatedStatus() {
        if ("Done".equals(status)) {
            return "Done";
        }

        // Check if date is passed
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Date date = sdf.parse(dueDate);
            Date today = new Date();
            // Reset time part of today for accurate comparison

            if (date != null && date.before(new Date(System.currentTimeMillis() - 86400000))) {
                return "Overdue";
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return "Upcoming";
    }

    // This is the raw status from DB ("Pending" or "Done")
    public String getDbStatus() {
        return status;
    }
}
