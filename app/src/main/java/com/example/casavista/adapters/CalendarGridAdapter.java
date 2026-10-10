package com.example.casavista.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.casavista.R;
import com.example.casavista.models.CalendarDay;

import java.util.List;

public class CalendarGridAdapter extends RecyclerView.Adapter<CalendarGridAdapter.ViewHolder> {

    public interface OnDateSelectedListener {
        void onDateSelected(CalendarDay day);
    }

    private final List<CalendarDay> days;
    private int selectedPosition = -1;
    private final OnDateSelectedListener listener;

    public CalendarGridAdapter(List<CalendarDay> days, int defaultSelectedDay, OnDateSelectedListener listener) {
        this.days = days;
        this.listener = listener;

        // Set default selected day (e.g. 20)
        for (int i = 0; i < days.size(); i++) {
            if (days.get(i).isCurrentMonth() && days.get(i).getDayNumber() == defaultSelectedDay) {
                this.selectedPosition = i;
                break;
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CalendarDay day = days.get(position);
        holder.tvDay.setText(String.valueOf(day.getDayNumber()));

        if (day.isPast()) {
            holder.tvDay.setBackground(null);
            holder.tvDay.setTextColor(Color.parseColor("#D5C8B6"));
            holder.tvDay.setAlpha(0.35f);
            holder.itemView.setOnClickListener(v -> {
                android.widget.Toast.makeText(v.getContext(), "Reservations for past dates are not allowed.", android.widget.Toast.LENGTH_SHORT).show();
            });
            return;
        }

        holder.tvDay.setAlpha(1.0f);

        if (position == selectedPosition) {
            holder.tvDay.setBackgroundResource(R.drawable.bg_day_circle_gold);
            holder.tvDay.setTextColor(Color.WHITE);
        } else {
            holder.tvDay.setBackground(null);
            if (day.isCurrentMonth()) {
                holder.tvDay.setTextColor(Color.parseColor("#302B24"));
            } else {
                holder.tvDay.setTextColor(Color.parseColor("#C9C2B3"));
            }
        }

        holder.itemView.setOnClickListener(v -> {
            int previous = selectedPosition;
            selectedPosition = holder.getBindingAdapterPosition();
            notifyItemChanged(previous);
            notifyItemChanged(selectedPosition);
            if (listener != null) listener.onDateSelected(day);
        });
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDay;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tvDayNumber);
        }
    }
}
