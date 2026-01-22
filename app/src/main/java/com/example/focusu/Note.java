package com.example.focusu;

public class Note {
    private String id;
    private String userId;
    private String title;
    private String content;
    private String imagePath;
    private String date;
    private long timestamp;

    public Note() {}

    public Note(String id, String userId, String title, String content, String imagePath, String date, long timestamp) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.imagePath = imagePath;
        this.date = date;
        this.timestamp = timestamp;
    }


    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getImagePath() { return imagePath; }
    public String getDate() { return date; }
    public long getTimestamp() { return timestamp; }

    public void setId(String id) { this.id = id; }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}