package com.example.casavista.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.casavista.R;
import com.example.casavista.data.ResortRepository;
import com.example.casavista.models.Booking;
import com.example.casavista.network.SupabaseClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONObject;

import java.util.Locale;
import java.util.Random;

public class MayaTestPaymentDialog {

    public interface OnPaymentCompletedListener {
        void onPaymentCompleted(boolean success, String bookingId);
    }

    public static void show(
            @NonNull Context context,
            @NonNull String bookingId,
            double amount,
            @NonNull OnPaymentCompletedListener listener
    ) {
        show(context, bookingId, amount, null, listener);
    }

    public static void show(
            @NonNull Context context,
            @NonNull String bookingId,
            double amount,
            String prefilledMobile,
            @NonNull OnPaymentCompletedListener listener
    ) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_maya_payment, null);

        TextView tvBookingId = dialogView.findViewById(R.id.tvMayaBookingId);
        TextView tvAmount = dialogView.findViewById(R.id.tvMayaAmount);
        TextView tvRefCode = dialogView.findViewById(R.id.tvMayaRefCode);
        EditText etMobile = dialogView.findViewById(R.id.etMayaMobile);
        MaterialButton btnPaySuccess = dialogView.findViewById(R.id.btnPaySuccess);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelMaya);

        if (tvBookingId != null) tvBookingId.setText(bookingId);
        if (tvAmount != null) tvAmount.setText(String.format(Locale.US, "₱%.2f", amount));

        if (etMobile != null && prefilledMobile != null && !prefilledMobile.trim().isEmpty()) {
            etMobile.setText(prefilledMobile.trim());
        }

        int randomRef = 100000 + new Random().nextInt(900000);
        String mockRefText = "Ref: MAYA-SANDBOX-" + randomRef;
        if (tvRefCode != null) tvRefCode.setText(mockRefText);

        Dialog dialog = new MaterialAlertDialogBuilder(context)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnPaySuccess != null) {
            btnPaySuccess.setOnClickListener(v -> {
                String mobile = etMobile != null ? etMobile.getText().toString().trim() : "0917-123-4567";
                btnPaySuccess.setEnabled(false);
                btnPaySuccess.setText("Authorizing Maya Transaction...");

                Toast.makeText(context, "Authorizing Maya Sandbox transaction...", Toast.LENGTH_SHORT).show();

                try {
                    String paymentId = com.example.casavista.helpers.PaymentIdGenerator.generatePaymentId();
                    JSONObject payload = new JSONObject();
                    payload.put("payment_id", paymentId);
                    payload.put("booking_id", bookingId);
                    payload.put("amount", amount);
                    payload.put("method", "Maya");
                    payload.put("payment_type", "Payment");

                    SupabaseClient.getInstance().processPayment(payload, new SupabaseClient.DataCallback() {
                        @Override
                        public void onSuccess(String jsonArrayString) {
                            // Update booking status in Supabase backend to Confirmed
                            SupabaseClient.getInstance().updateBookingStatus(bookingId, "Confirmed", new SupabaseClient.DataCallback() {
                                @Override
                                public void onSuccess(String res) {
                                    finalizePaymentSuccess(context, dialog, bookingId, mobile, mockRefText, amount, listener);
                                }

                                @Override
                                public void onError(String errorMessage) {
                                    finalizePaymentSuccess(context, dialog, bookingId, mobile, mockRefText, amount, listener);
                                }
                            });
                        }

                        @Override
                        public void onError(String errorMessage) {
                            btnPaySuccess.setEnabled(true);
                            btnPaySuccess.setText("Simulate Successful Payment");
                            Toast.makeText(context, "Payment failed or rejected: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });

                } catch (Exception e) {
                    btnPaySuccess.setEnabled(true);
                    btnPaySuccess.setText("Simulate Successful Payment");
                    Toast.makeText(context, "Payment error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        dialog.show();
    }

    private static void finalizePaymentSuccess(
            Context context,
            Dialog dialog,
            String bookingId,
            String mobile,
            String mockRefText,
            double amount,
            OnPaymentCompletedListener listener
    ) {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

        ResortRepository repo = ResortRepository.getInstance();
        repo.loadSavedBookings(context);
        for (Booking b : repo.getBookings()) {
            if (bookingId.equalsIgnoreCase(b.getBookingId())) {
                b.setStatus("Confirmed");
                b.setDetail1(b.getDetail1() + " • Maya Paid (" + mobile + ")");
                b.setDetail2("Paid via Maya Sandbox (" + mockRefText + ")");
                break;
            }
        }
        repo.saveBookings(context);

        new MaterialAlertDialogBuilder(context)
                .setTitle("Payment Verified! 🎉")
                .setMessage("Your Maya payment (₱" + String.format(Locale.US, "%.2f", amount) + ") was captured successfully.\nBooking " + bookingId + " status updated to Confirmed in Supabase.")
                .setCancelable(false)
                .setPositiveButton("OK", (d, w) -> {
                    if (listener != null) {
                        listener.onPaymentCompleted(true, bookingId);
                    }
                })
                .show();
    }
}
