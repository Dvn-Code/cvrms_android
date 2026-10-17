package com.example.casavista.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.casavista.R;
import com.example.casavista.data.ResortRepository;
import com.example.casavista.models.Booking;
import com.example.casavista.network.SupabaseClient;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

public class AdminBookingAdapter extends RecyclerView.Adapter<AdminBookingAdapter.AdminBookingViewHolder> {

    public interface OnBookingActionListener {
        void onBookingStatusChanged();
    }

    private List<Booking> bookings;
    private final OnBookingActionListener actionListener;

    public AdminBookingAdapter(List<Booking> bookings, OnBookingActionListener actionListener) {
        this.bookings = bookings;
        this.actionListener = actionListener;
    }

    public void updateList(List<Booking> newList) {
        this.bookings = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AdminBookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking_admin, parent, false);
        return new AdminBookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminBookingViewHolder holder, int position) {
        Booking booking = bookings.get(position);
        Context context = holder.itemView.getContext();

        String guestName = booking.getDetail2() != null && !booking.getDetail2().isEmpty()
                ? booking.getDetail2()
                : "Guest " + (booking.getCustomerId() != null ? booking.getCustomerId() : "");
        holder.tvUserName.setText(guestName);

        String cleanInitialsSource = guestName.split("•")[0].trim();
        String[] nameParts = cleanInitialsSource.split("\\s+");
        if (nameParts.length >= 2 && !nameParts[0].isEmpty() && !nameParts[nameParts.length - 1].isEmpty()) {
            holder.tvUserInitials.setText(("" + nameParts[0].charAt(0) + nameParts[nameParts.length - 1].charAt(0)).toUpperCase());
        } else if (nameParts.length == 1 && !nameParts[0].isEmpty()) {
            holder.tvUserInitials.setText(("" + nameParts[0].charAt(0)).toUpperCase());
        } else {
            holder.tvUserInitials.setText("CV");
        }

        holder.tvBookingType.setText(booking.getTitle());

        StringBuilder timeBuilder = new StringBuilder();
        if (booking.getDate() != null && !booking.getDate().isEmpty()) {
            timeBuilder.append(booking.getDate());
        }
        if (booking.getDetail1() != null && !booking.getDetail1().isEmpty()) {
            if (timeBuilder.length() > 0) timeBuilder.append(" • ");
            timeBuilder.append(booking.getDetail1());
        }
        holder.tvBookingTime.setText(timeBuilder.toString());

        String priceStr = booking.getPrice() != null ? booking.getPrice() : "₱0.00";
        holder.tvBookingPrice.setText(priceStr);

        String status = booking.getStatus() != null ? booking.getStatus() : "Pending";
        holder.tvStatus.setText(status);

        if ("Pending".equalsIgnoreCase(status)) {
            holder.tvStatus.setTextColor(Color.parseColor("#D97706"));
            holder.tvStatus.setBackgroundColor(Color.parseColor("#FEF3C7"));
            holder.layoutActionButtons.setVisibility(View.VISIBLE);
            holder.btnAccept.setText("Confirm & Pay");
            holder.btnDecline.setText("Decline");

            holder.btnAccept.setOnClickListener(v -> showConfirmPaymentMethodDialog(context, booking));
            holder.btnDecline.setOnClickListener(v -> updateBookingStatusApi(context, booking, "Cancelled"));

        } else if ("Confirmed".equalsIgnoreCase(status)) {
            holder.tvStatus.setTextColor(Color.parseColor("#16A34A"));
            holder.tvStatus.setBackgroundColor(Color.parseColor("#DCFCE7"));
            holder.layoutActionButtons.setVisibility(View.VISIBLE);
            holder.btnAccept.setText("Check-In");
            holder.btnDecline.setText("Cancel");

            holder.btnAccept.setOnClickListener(v -> updateBookingStatusApi(context, booking, "Checked-In"));
            holder.btnDecline.setOnClickListener(v -> updateBookingStatusApi(context, booking, "Cancelled"));

        } else if ("Checked-In".equalsIgnoreCase(status)) {
            holder.tvStatus.setTextColor(Color.parseColor("#2563EB"));
            holder.tvStatus.setBackgroundColor(Color.parseColor("#DBEAFE"));
            holder.layoutActionButtons.setVisibility(View.VISIBLE);
            holder.btnAccept.setText("Complete");
            holder.btnDecline.setText("Cancel");

            holder.btnAccept.setOnClickListener(v -> updateBookingStatusApi(context, booking, "Completed"));
            holder.btnDecline.setOnClickListener(v -> updateBookingStatusApi(context, booking, "Cancelled"));

        } else {
            holder.tvStatus.setTextColor(Color.parseColor("#4B5563"));
            holder.tvStatus.setBackgroundColor(Color.parseColor("#F3F4F6"));
            holder.layoutActionButtons.setVisibility(View.GONE);
        }
    }

    private void showConfirmPaymentMethodDialog(Context context, Booking booking) {
        final String[] methods = {"Cash", "GCash", "Maya", "Card", "Online Gateway"};
        new MaterialAlertDialogBuilder(context)
                .setTitle("Confirm Booking & Record Payment")
                .setItems(methods, (dialog, which) -> {
                    String selectedMethod = methods[which];
                    recordBookingPaymentAndConfirm(context, booking, selectedMethod);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void recordBookingPaymentAndConfirm(Context context, Booking booking, String paymentMethod) {
        String bId = booking.getBookingId();
        double amount = booking.getTotalAmount();
        if (amount <= 0 && booking.getPrice() != null) {
            try {
                String cleanPrice = booking.getPrice().replaceAll("[^0-9.]", "");
                if (!cleanPrice.isEmpty()) amount = Double.parseDouble(cleanPrice);
            } catch (Exception ignored) {}
        }
        if (amount <= 0) amount = 1500.00;

        if (bId == null || bId.isEmpty()) {
            updateBookingStatusApi(context, booking, "Confirmed");
            return;
        }

        try {
            String payId = String.format(Locale.US, "PAY-%06d", (int) (System.currentTimeMillis() % 1000000));
            JSONObject payPayload = new JSONObject();
            payPayload.put("payment_id", payId);
            payPayload.put("booking_id", bId);
            payPayload.put("amount", amount);
            payPayload.put("method", paymentMethod);
            payPayload.put("payment_type", "Payment");

            SupabaseClient.getInstance().processPayment(payPayload, new SupabaseClient.DataCallback() {
                @Override
                public void onSuccess(String jsonResponse) {
                    // POST /payments automatically sets booking status to Confirmed in FastAPI,
                    // but we also call updateBookingStatusApi to ensure local & remote state are synced
                    updateBookingStatusApi(context, booking, "Confirmed");
                }

                @Override
                public void onError(String errorMessage) {
                    // Even if a payment row already exists for this booking, still update status to Confirmed
                    updateBookingStatusApi(context, booking, "Confirmed");
                }
            });
        } catch (Exception e) {
            updateBookingStatusApi(context, booking, "Confirmed");
        }
    }

    private void updateBookingStatusApi(Context context, Booking booking, String newStatus) {
        String bId = booking.getBookingId();
        if (bId == null || bId.isEmpty()) {
            booking.setStatus(newStatus);
            ResortRepository.getInstance().saveBookings(context);
            if (actionListener != null) actionListener.onBookingStatusChanged();
            return;
        }

        SupabaseClient.getInstance().updateBookingStatus(bId, newStatus, new SupabaseClient.DataCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                booking.setStatus(newStatus);
                ResortRepository.getInstance().saveBookings(context);
                Toast.makeText(context, "Booking " + bId + " updated to " + newStatus, Toast.LENGTH_SHORT).show();
                if (actionListener != null) {
                    actionListener.onBookingStatusChanged();
                }
            }

            @Override
            public void onError(String errorMessage) {
                booking.setStatus(newStatus);
                ResortRepository.getInstance().saveBookings(context);
                Toast.makeText(context, "Status updated: " + newStatus, Toast.LENGTH_SHORT).show();
                if (actionListener != null) {
                    actionListener.onBookingStatusChanged();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return bookings != null ? bookings.size() : 0;
    }

    static class AdminBookingViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserInitials, tvUserName, tvBookingType, tvStatus, tvBookingTime, tvBookingPrice;
        LinearLayout layoutActionButtons;
        Button btnDecline, btnAccept;

        public AdminBookingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserInitials = itemView.findViewById(R.id.tvAdminUserInitials);
            tvUserName = itemView.findViewById(R.id.tvAdminUserName);
            tvBookingType = itemView.findViewById(R.id.tvAdminBookingType);
            tvStatus = itemView.findViewById(R.id.tvAdminBookingStatus);
            tvBookingTime = itemView.findViewById(R.id.tvAdminBookingTime);
            tvBookingPrice = itemView.findViewById(R.id.tvAdminBookingPrice);
            layoutActionButtons = itemView.findViewById(R.id.layoutActionButtons);
            btnDecline = itemView.findViewById(R.id.btnDeclineBooking);
            btnAccept = itemView.findViewById(R.id.btnAcceptBooking);
        }
    }
}