package com.example.casavista.helpers;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class BookingIdGenerator {

    private static final AtomicInteger counter = new AtomicInteger(101);

    public static String generateBookingId() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd", Locale.US);
        String dateStr = sdf.format(new Date());
        int seq = counter.getAndIncrement();
        if (seq > 999) {
            counter.set(101);
            seq = 101;
        }
        return "BK-" + dateStr + "-" + seq; // e.g., BK-261013-101 (13 chars <= 15)
    }
}
