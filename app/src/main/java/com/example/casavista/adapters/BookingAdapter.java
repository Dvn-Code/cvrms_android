package com.example.casavista.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.casavista.R;
import com.example.casavista.data.ResortRepository;
import com.example.casavista.models.Booking;

import java.io.File;
import java.util.List;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

    private List<Booking> bookings;

    public BookingAdapter(List<Booking> bookings) {
        this.bookings = bookings;
    }

    public void updateList(List<Booking> newList) {
        this.bookings = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking_card, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Booking booking = bookings.get(position);

        holder.tvTitle.setText(booking.getTitle());
        holder.tvDate.setText(booking.getDate());
        holder.tvDetail1.setText(booking.getDetail1());
        holder.tvDetail2.setText(booking.getDetail2());
        holder.tvPrice.setText(booking.getPrice());

        String status = booking.getStatus() != null ? booking.getStatus() : "Pending";
        if (booking.isHoldExpired()) {
            status = "Cancelled";
            booking.setStatus("Cancelled");
            booking.setDetail2("Hold expired (15-min window ended)");
        }
        holder.tvStatus.setText(status);

        if ("Pending".equalsIgnoreCase(status)) {
            holder.tvStatus.setTextColor(Color.parseColor("#2A1810"));
        } else if ("Confirmed".equalsIgnoreCase(status)) {
            holder.tvStatus.setTextColor(Color.parseColor("#2A1810"));
        } else if ("Declined".equalsIgnoreCase(status) || "Cancelled".equalsIgnoreCase(status)) {
            holder.tvStatus.setTextColor(Color.parseColor("#DC2626"));
        } else {
            holder.tvStatus.setTextColor(Color.parseColor("#2A1810"));
        }

        // Image loading
        if (holder.imgBookingThumb != null) {
            boolean loaded = false;
            String imgPath = booking.getImagePath();

            if (imgPath != null && !imgPath.trim().isEmpty()) {
                try {
                    File file = new File(imgPath);
                    if (file.exists()) {
                        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                        if (bitmap != null) {
                            holder.imgBookingThumb.setImageBitmap(bitmap);
                            loaded = true;
                        }
                    } else if (imgPath.startsWith("content://")) {
                        holder.imgBookingThumb.setImageURI(Uri.parse(imgPath));
                        loaded = true;
                    }
                } catch (Exception ignored) {}
            }

            if (!loaded && booking.getImageResId() != 0) {
                holder.imgBookingThumb.setImageResource(booking.getImageResId());
                loaded = true;
            }

            if (!loaded) {
                String titleLower = booking.getTitle() != null ? booking.getTitle().toLowerCase() : "";
                if (titleLower.contains("pickleball")) {
                    holder.imgBookingThumb.setImageResource(R.drawable.pickleball_icon_img);
                } else if (titleLower.contains("basketball")) {
                    holder.imgBookingThumb.setImageResource(R.drawable.basketball_icon_img);
                } else if (titleLower.contains("room") || titleLower.contains("suite") || titleLower.contains("villa") || titleLower.contains("loft")) {
                    holder.imgBookingThumb.setImageResource(R.drawable.rooms_icon_img);
                } else {
                    holder.imgBookingThumb.setImageResource(R.drawable.resort_icon_img);
                }
            }
        }

        // Handle Maya Test Pay button for Pending bookings
        if ("Pending".equalsIgnoreCase(status)) {
            holder.btnPayMaya.setVisibility(View.VISIBLE);
            holder.btnPayMaya.setOnClickListener(v -> {
                int adapterPos = holder.getBindingAdapterPosition();
                if (adapterPos != RecyclerView.NO_POSITION) {
                    if (booking.isHoldExpired()) {
                        booking.setStatus("Cancelled");
                        booking.setDetail2("Reservation hold expired (15-min window ended)");
                        ResortRepository.getInstance().saveBookings(v.getContext());
                        notifyItemChanged(adapterPos);

                        new com.google.android.material.dialog.MaterialAlertDialogBuilder(v.getContext())
                                .setTitle("Reservation Hold Expired ⏱️")
                                .setMessage("The 15-minute hold window for this reservation (" + booking.getBookingId() + ") has expired.\n\nThe reserved suite or court slot has been automatically released back to the resort inventory. Please create a new reservation.")
                                .setPositiveButton("OK", null)
                                .show();
                        return;
                    }

                    double amt = 400.0;
                    try {
                        String rawPrice = booking.getPrice() != null ? booking.getPrice().replaceAll("[^0-9.]", "") : "400";
                        if (!rawPrice.isEmpty()) {
                            amt = Double.parseDouble(rawPrice);
                        }
                    } catch (Exception ignored) {}

                    String bId = booking.getBookingId() != null && !booking.getBookingId().isEmpty()
                            ? booking.getBookingId()
                            : "BK-" + (System.currentTimeMillis() % 1000000);

                    com.example.casavista.dialogs.MayaTestPaymentDialog.show(
                            v.getContext(),
                            bId,
                            amt,
                            (success, paidBookingId) -> notifyItemChanged(adapterPos)
                    );
                }
            });
        } else {
            holder.btnPayMaya.setVisibility(View.GONE);
        }

        // Hide cancel button if already cancelled, declined, or completed
        if ("Cancelled".equalsIgnoreCase(status) || "Declined".equalsIgnoreCase(status) || "Completed".equalsIgnoreCase(status)) {
            holder.btnCancel.setVisibility(View.GONE);
        } else {
            holder.btnCancel.setVisibility(View.VISIBLE);
            holder.btnCancel.setOnClickListener(v -> {
                int adapterPos = holder.getBindingAdapterPosition();
                if (adapterPos != RecyclerView.NO_POSITION) {
                    String bId = booking.getBookingId() != null ? booking.getBookingId() : "";

                    new com.google.android.material.dialog.MaterialAlertDialogBuilder(v.getContext())
                            .setTitle("Cancel Booking")
                            .setMessage("Are you sure you want to cancel booking " + (bId.isEmpty() ? booking.getTitle() : bId) + "?")
                            .setPositiveButton("Yes, Cancel", (dialog, which) -> {
                                if (!bId.isEmpty()) {
                                    com.example.casavista.network.SupabaseClient.getInstance().updateBookingStatus(
                                            bId,
                                            "Cancelled",
                                            new com.example.casavista.network.SupabaseClient.DataCallback() {
                                                @Override
                                                public void onSuccess(String jsonArrayString) {
                                                    ResortRepository.getInstance().cancelBooking(v.getContext(), booking);
                                                    notifyItemChanged(adapterPos);
                                                    android.widget.Toast.makeText(v.getContext(), "Booking " + bId + " cancelled successfully.", android.widget.Toast.LENGTH_SHORT).show();
                                                }

                                                @Override
                                                public void onError(String errorMessage) {
                                                    ResortRepository.getInstance().cancelBooking(v.getContext(), booking);
                                                    notifyItemChanged(adapterPos);
                                                    android.widget.Toast.makeText(v.getContext(), "Booking cancelled.", android.widget.Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                    );
                                } else {
                                    ResortRepository.getInstance().cancelBooking(v.getContext(), booking);
                                    notifyItemChanged(adapterPos);
                                    android.widget.Toast.makeText(v.getContext(), "Booking cancelled.", android.widget.Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton("No", null)
                            .show();
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return bookings != null ? bookings.size() : 0;
    }

    static class BookingViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvStatus, tvDate, tvDetail1, tvDetail2, tvPrice;
        ImageView imgBookingThumb;
        LinearLayout layoutCancelAction;
        Button btnCancel, btnPayMaya;

        public BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvBookingTitle);
            tvStatus = itemView.findViewById(R.id.tvBookingStatus);
            tvDate = itemView.findViewById(R.id.tvBookingDate);
            tvDetail1 = itemView.findViewById(R.id.tvBookingDetail1);
            tvDetail2 = itemView.findViewById(R.id.tvBookingDetail2);
            tvPrice = itemView.findViewById(R.id.tvBookingPrice);
            imgBookingThumb = itemView.findViewById(R.id.imgBookingThumb);
            layoutCancelAction = itemView.findViewById(R.id.layoutCancelAction);
            btnCancel = itemView.findViewById(R.id.btnCancelBooking);
            btnPayMaya = itemView.findViewById(R.id.btnPayMaya);
        }
    }
}