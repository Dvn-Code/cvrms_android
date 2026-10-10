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

import java.util.List;

public class AdminBookingAdapter extends RecyclerView.Adapter<AdminBookingAdapter.AdminBookingViewHolder> {

    public interface OnBookingActionListener {
        void onBookingStatusChanged();
    }

    private List<Booking> bookings;
    private OnBookingActionListener actionListener;

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

        String name = ResortRepository.getInstance().getCurrentUserName();
        if (name == null || name.trim().isEmpty()) {
            name = "Alex Morgan";
        }
        holder.tvUserName.setText(name);

        String[] nameParts = name.trim().split("\\s+");
        if (nameParts.length >= 2) {
            holder.tvUserInitials.setText(("" + nameParts[0].charAt(0) + nameParts[nameParts.length - 1].charAt(0)).toUpperCase());
        } else if (nameParts.length == 1 && !nameParts[0].isEmpty()) {
            holder.tvUserInitials.setText(("" + nameParts[0].charAt(0)).toUpperCase());
        } else {
            holder.tvUserInitials.setText("AM");
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
        if (booking.getDetail2() != null && !booking.getDetail2().isEmpty()) {
            if (timeBuilder.length() > 0) timeBuilder.append(" • ");
            timeBuilder.append(booking.getDetail2());
        }
        holder.tvBookingTime.setText(timeBuilder.toString());

        String priceStr = booking.getPrice() != null ? booking.getPrice() : "₱0";
        holder.tvBookingPrice.setText(priceStr);

        String status = booking.getStatus() != null ? booking.getStatus() : "Pending";
        holder.tvStatus.setText(status);

        if ("Pending".equalsIgnoreCase(status)) {
            holder.tvStatus.setTextColor(Color.parseColor("#D97706"));
            holder.tvStatus.setBackgroundColor(Color.parseColor("#FEF3C7"));
            holder.layoutActionButtons.setVisibility(View.VISIBLE);

            holder.btnAccept.setOnClickListener(v -> {
                int adapterPos = holder.getBindingAdapterPosition();
                if (adapterPos != RecyclerView.NO_POSITION) {
                    booking.setStatus("Confirmed");
                    ResortRepository.getInstance().saveBookings(context);
                    Toast.makeText(context, "Booking Accepted: " + booking.getTitle(), Toast.LENGTH_SHORT).show();
                    notifyItemChanged(adapterPos);
                    if (actionListener != null) {
                        actionListener.onBookingStatusChanged();
                    }
                }
            });

            holder.btnDecline.setOnClickListener(v -> {
                int adapterPos = holder.getBindingAdapterPosition();
                if (adapterPos != RecyclerView.NO_POSITION) {
                    booking.setStatus("Declined");
                    ResortRepository.getInstance().saveBookings(context);
                    Toast.makeText(context, "Booking Declined: " + booking.getTitle(), Toast.LENGTH_SHORT).show();
                    notifyItemChanged(adapterPos);
                    if (actionListener != null) {
                        actionListener.onBookingStatusChanged();
                    }
                }
            });

        } else if ("Confirmed".equalsIgnoreCase(status)) {
            holder.tvStatus.setTextColor(Color.parseColor("#16A34A"));
            holder.tvStatus.setBackgroundColor(Color.parseColor("#DCFCE7"));
            holder.layoutActionButtons.setVisibility(View.GONE);

        } else if ("Declined".equalsIgnoreCase(status) || "Cancelled".equalsIgnoreCase(status)) {
            holder.tvStatus.setTextColor(Color.parseColor("#DC2626"));
            holder.tvStatus.setBackgroundColor(Color.parseColor("#FEE2E2"));
            holder.layoutActionButtons.setVisibility(View.GONE);

        } else {
            holder.tvStatus.setTextColor(Color.parseColor("#D97706"));
            holder.tvStatus.setBackgroundColor(Color.parseColor("#FEF3C7"));
            holder.layoutActionButtons.setVisibility(View.GONE);
        }
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