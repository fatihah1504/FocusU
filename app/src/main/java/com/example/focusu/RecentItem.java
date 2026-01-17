package com.example.focusu;

public class RecentItem implements Comparable<RecentItem> {

    private String title;
    private String description;
    private String type;
    private long timestamp;

    public RecentItem(String title, String description, String type, long timestamp) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.timestamp = timestamp;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getType() { return type; }
    public long getTimestamp() { return timestamp; }

    @Override
    public int compareTo(RecentItem other) {
        return Long.compare(other.timestamp, this.timestamp);
    }
}
