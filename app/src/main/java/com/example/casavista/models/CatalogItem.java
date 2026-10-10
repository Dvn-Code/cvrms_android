package com.example.casavista.models;

public class CatalogItem {
    private String title;
    private String subtitle;
    private String category;
    private String guests;
    private String price;
    private String badge;
    private int imageResId;
    private String imageUrl; // Future-proofing for SuperAdmin dynamic uploads

    public CatalogItem() {
    }

    public CatalogItem(String title, String subtitle, String category, String guests, String price, String badge, int imageResId) {
        this.title = title;
        this.subtitle = subtitle;
        this.category = category;
        this.guests = guests;
        this.price = price;
        this.badge = badge;
        this.imageResId = imageResId;
        this.imageUrl = null;
    }

    public CatalogItem(String title, String subtitle, String category, String guests, String price, String badge, int imageResId, String imageUrl) {
        this.title = title;
        this.subtitle = subtitle;
        this.category = category;
        this.guests = guests;
        this.price = price;
        this.badge = badge;
        this.imageResId = imageResId;
        this.imageUrl = imageUrl;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getGuests() { return guests; }
    public void setGuests(String guests) { this.guests = guests; }

    public String getCapacity() { return guests; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public String getPriceLabel() { return price; }

    public String getBadge() { return badge; }
    public void setBadge(String badge) { this.badge = badge; }

    public int getImageResId() { return imageResId; }
    public void setImageResId(int imageResId) { this.imageResId = imageResId; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}