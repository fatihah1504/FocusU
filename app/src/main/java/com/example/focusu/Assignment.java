package com.example.focusu;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
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


    public String getCalculatedStatus() {
        if ("Done".equals(status)) {
            return "Done";
        }

        // Must match dd-MM-yyyy
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        try {
            Date dueDateObj = sdf.parse(dueDate);

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            if (dueDateObj != null && dueDateObj.before(cal.getTime())) {
                return "Overdue";
            }
        } catch (ParseException e) {
            return "Upcoming";
        }
        return "Upcoming";
    }


    // This is the raw status from DB ("Pending" or "Done")
    public String getDbStatus() {
        return status;
    }
}
