package com.example.casavista.models;

public class CalendarDay {
    private int dayNumber;
    private int month;
    private int year;
    private boolean isCurrentMonth;

    public CalendarDay(int dayNumber, int month, int year, boolean isCurrentMonth) {
        this.dayNumber = dayNumber;
        this.month = month;
        this.year = year;
        this.isCurrentMonth = isCurrentMonth;
    }

    public int getDayNumber() { return dayNumber; }
    public int getMonth() { return month; }
    public int getYear() { return year; }
    public boolean isCurrentMonth() { return isCurrentMonth; }

    public boolean isPast() {
        java.util.Calendar today = java.util.Calendar.getInstance();
        today.set(java.util.Calendar.HOUR_OF_DAY, 0);
        today.set(java.util.Calendar.MINUTE, 0);
        today.set(java.util.Calendar.SECOND, 0);
        today.set(java.util.Calendar.MILLISECOND, 0);

        java.util.Calendar dayCal = java.util.Calendar.getInstance();
        dayCal.set(year, month, dayNumber, 0, 0, 0);
        dayCal.set(java.util.Calendar.MILLISECOND, 0);

        return dayCal.before(today);
    }
}
