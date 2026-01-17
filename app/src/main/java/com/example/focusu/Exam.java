package com.example.focusu;

public class Exam {
    private String id;
    private String subject;
    private String type;
    private String date;
    private String time;
    private String location;
    private String priority;
    private String userId;
    private long timestamp;

    public Exam() {

    }

    public Exam(String subject, String type, String date, String time, String location, String priority, String userId) {
        this.subject = subject;
        this.type = type;
        this.date = date;
        this.time = time;
        this.location = location;
        this.priority = priority;
        this.userId = userId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // --- GETTERS AND SETTERS ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}
