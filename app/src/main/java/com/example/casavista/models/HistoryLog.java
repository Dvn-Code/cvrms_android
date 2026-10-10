package com.example.casavista.models;

public class HistoryLog {

    public enum Type {
        PAYMENT,
        BOOKING,
        REFUND,
        STAFF_ACTIVITY
    }

    private String id;
    private Type type;
    private String title;
    private String subtitle;
    private String amount;
    private String processedBy;
    private String timestamp;

    public HistoryLog(String id, Type type, String title, String subtitle, String amount, String processedBy, String timestamp) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.subtitle = subtitle;
        this.amount = amount;
        this.processedBy = processedBy;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public Type getType() { return type; }
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public String getAmount() { return amount; }
    public String getProcessedBy() { return processedBy; }
    public String getTimestamp() { return timestamp; }
}