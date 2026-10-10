package com.example.casavista.models;

public class Announcement {
    private String id;
    private String title;
    private String body;
    private String date;
    private String imagePath;
    private String author;

    // 1. Default constructor
    public Announcement() { }

    // 2. 4-parameter constructor
    public Announcement(String title, String body, String date, String imagePath) {
        this.title = title;
        this.body = body;
        this.date = date;
        this.imagePath = imagePath;
    }

    // 3. 5-parameter constructor (Fixes your compilation error)
    public Announcement(String id, String title, String body, String date, String imagePath) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.date = date;
        this.imagePath = imagePath;
    }

    // 4. 6-parameter constructor
    public Announcement(String id, String title, String body, String date, String imagePath, String author) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.date = date;
        this.imagePath = imagePath;
        this.author = author;
    }

    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getDate() { return date; }
    public String getImagePath() { return imagePath; }
    public String getAuthor() { return author; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setBody(String body) { this.body = body; }
    public void setDate(String date) { this.date = date; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public void setAuthor(String author) { this.author = author; }
}