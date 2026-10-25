package com.example.casavista.models;

public class Booking {
    private String bookingId;
    private String customerId;
    private String staffId; // Nullable for online self-service
    private String courtId; // Nullable for room bookings
    private String roomId;  // Nullable for court bookings
    private String startTime; // Nullable for room bookings
    private String endTime;   // Nullable for room bookings
    private String checkInDate; // Nullable for court bookings
    private String checkOutDate; // Nullable for court bookings
    private int paddleCount;
    private boolean exclusive;
    private double totalAmount;

    private String title;
    private String status;
    private String date;
    private String detail1;
    private String detail2;
    private String price;
    private String imagePath;
    private int imageResId;

    public Booking() {
    }

    public Booking(String title, String status, String date, String detail1, String detail2, String price) {
        this(title, status, date, detail1, detail2, price, null, 0);
    }

    public Booking(String title, String status, String date, String detail1, String detail2, String price, String imagePath, int imageResId) {
        this.title = title;
        this.status = status;
        this.date = date;
        this.detail1 = detail1;
        this.detail2 = detail2;
        this.price = price;
        this.imagePath = imagePath;
        this.imageResId = imageResId;
    }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDetail1() {
        return detail1;
    }

    public void setDetail1(String detail1) {
        this.detail1 = detail1;
    }

    public String getDetail2() {
        return detail2;
    }

    public void setDetail2(String detail2) {
        this.detail2 = detail2;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public int getImageResId() {
        return imageResId;
    }

    public void setImageResId(int imageResId) {
        this.imageResId = imageResId;
    }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getStaffId() { return staffId; }
    public void setStaffId(String staffId) { this.staffId = staffId; }

    public String getCourtId() { return courtId; }
    public void setCourtId(String courtId) { this.courtId = courtId; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getCheckInDate() { return checkInDate; }
    public void setCheckInDate(String checkInDate) { this.checkInDate = checkInDate; }

    public String getCheckOutDate() { return checkOutDate; }
    public void setCheckOutDate(String checkOutDate) { this.checkOutDate = checkOutDate; }

    public int getPaddleCount() { return paddleCount; }
    public void setPaddleCount(int paddleCount) { this.paddleCount = paddleCount; }

    public boolean isExclusive() { return exclusive; }
    public void setExclusive(boolean exclusive) { this.exclusive = exclusive; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    private String createdAt;
    private String holdExpiresAt;

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getHoldExpiresAt() { return holdExpiresAt; }
    public void setHoldExpiresAt(String holdExpiresAt) { this.holdExpiresAt = holdExpiresAt; }

    public boolean isHoldExpired() {
        if (!"Pending".equalsIgnoreCase(status)) return false;
        try {
            if (holdExpiresAt != null && !holdExpiresAt.trim().isEmpty() && !"null".equalsIgnoreCase(holdExpiresAt)) {
                java.util.Date exp = parseIsoTimestamp(holdExpiresAt);
                if (exp != null) {
                    return System.currentTimeMillis() > exp.getTime();
                }
            }
            if (createdAt != null && !createdAt.trim().isEmpty() && !"null".equalsIgnoreCase(createdAt)) {
                java.util.Date created = parseIsoTimestamp(createdAt);
                if (created != null) {
                    return System.currentTimeMillis() > (created.getTime() + 15 * 60 * 1000);
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    private static java.util.Date parseIsoTimestamp(String iso) {
        if (iso == null || iso.trim().isEmpty()) return null;
        String clean = iso.trim();
        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd HH:mm:ssX",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd"
        };
        for (String pattern : patterns) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(pattern, java.util.Locale.US);
                sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                return sdf.parse(clean);
            } catch (Exception ignored) {}
        }
        return null;
    }
}