package com.example.casavista.helpers;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class PaymentIdGenerator {

    private static final AtomicInteger counter = new AtomicInteger(1);

    public static String generatePaymentId() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd", Locale.US);
        String dateStr = sdf.format(new Date());
        int seq = counter.getAndIncrement();
        if (seq > 99) {
            counter.set(1);
            seq = 1;
        }
        return String.format(Locale.US, "PAY-%s-%02d", dateStr, seq); // e.g., PAY-261025-01
    }
}
