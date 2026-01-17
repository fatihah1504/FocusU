package com.example.focusu;

public class PopupReminderItem {
    private String title;
    private String subtitle;
    private String type; // "assignment" or "exam"

    public PopupReminderItem(String title, String subtitle, String type) {
        this.title = title;
        this.subtitle = subtitle;
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getType() {
        return type;
    }
}
