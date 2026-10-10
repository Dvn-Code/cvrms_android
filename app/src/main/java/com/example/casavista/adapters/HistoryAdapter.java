package com.example.casavista.adapters;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.casavista.R;
import com.example.casavista.models.HistoryLog;

import java.util.ArrayList;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<HistoryLog> originalList;
    private List<HistoryLog> filteredList;

    public HistoryAdapter(List<HistoryLog> list) {
        this.originalList = new ArrayList<>(list);
        this.filteredList = new ArrayList<>(list);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryLog log = filteredList.get(position);

        holder.tvTitle.setText(Html.fromHtml(log.getTitle(), Html.FROM_HTML_MODE_LEGACY));
        holder.tvSubtitle.setText(log.getSubtitle());
        holder.tvProcessedBy.setText(log.getProcessedBy());
        holder.tvTimestamp.setText(log.getTimestamp());

        // Amount Formatting
        if (log.getAmount() != null && !log.getAmount().isEmpty()) {
            holder.tvAmount.setVisibility(View.VISIBLE);
            holder.tvAmount.setText(log.getAmount());

            if (log.getAmount().startsWith("+")) {
                holder.tvAmount.setTextColor(Color.parseColor("#4CAF50")); // Green
            } else if (log.getAmount().startsWith("-")) {
                holder.tvAmount.setTextColor(Color.parseColor("#E53935")); // Red
            } else {
                holder.tvAmount.setTextColor(Color.parseColor("#333333"));
            }
        } else {
            holder.tvAmount.setVisibility(View.GONE);
        }

        // Configure Badge Color & Symbol based on Action Type
        GradientDrawable badgeBackground = new GradientDrawable();
        badgeBackground.setShape(GradientDrawable.OVAL);

        switch (log.getType()) {
            case PAYMENT:
                badgeBackground.setColor(Color.parseColor("#F5E093")); // Gold/Yellow
                holder.tvBadgeIcon.setText("$");
                break;
            case BOOKING:
                badgeBackground.setColor(Color.parseColor("#D4E7C5")); // Soft Green
                holder.tvBadgeIcon.setText("📅");
                break;
            case REFUND:
                badgeBackground.setColor(Color.parseColor("#F9D2C4")); // Peach/Orange
                holder.tvBadgeIcon.setText("↺");
                break;
            case STAFF_ACTIVITY:
                badgeBackground.setColor(Color.parseColor("#D0E8F2")); // Light Blue
                holder.tvBadgeIcon.setText("👤");
                break;
        }

        holder.badgeContainer.setBackground(badgeBackground);

        // Click Event for Interactivity
        holder.itemView.setOnClickListener(v ->
                Toast.makeText(v.getContext(), "Transaction ID: " + log.getId(), Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    // Filter list dynamically as admin types in search bar
    public void filter(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(originalList);
        } else {
            String lowerQuery = query.toLowerCase().trim();
            for (HistoryLog item : originalList) {
                if (item.getTitle().toLowerCase().contains(lowerQuery) ||
                        item.getSubtitle().toLowerCase().contains(lowerQuery) ||
                        item.getProcessedBy().toLowerCase().contains(lowerQuery)) {
                    filteredList.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubtitle, tvAmount, tvProcessedBy, tvTimestamp, tvBadgeIcon;
        FrameLayout badgeContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvHistoryTitle);
            tvSubtitle = itemView.findViewById(R.id.tvHistorySubtitle);
            tvAmount = itemView.findViewById(R.id.tvHistoryAmount);
            tvProcessedBy = itemView.findViewById(R.id.tvProcessedBy);
            tvTimestamp = itemView.findViewById(R.id.tvHistoryTimestamp);
            tvBadgeIcon = itemView.findViewById(R.id.tvBadgeIcon);
            badgeContainer = itemView.findViewById(R.id.badgeContainer);
        }
    }
}