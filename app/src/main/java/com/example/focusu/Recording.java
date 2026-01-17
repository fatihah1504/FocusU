package com.example.focusu;

import com.google.firebase.database.Exclude;

public class Recording {
    @Exclude
    private String id;

    private String title;
    private String subject;
    private long duration;
    private long timestamp;
    private String filePath;
    private String userId;
    private String date;

    public Recording() {
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }


    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
    public String getDate() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(timestamp));
    }
    @Exclude
    private boolean isPlaying = false;


    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }

}
