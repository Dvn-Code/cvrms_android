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

    private final List<HistoryLog> originalList;
    private final List<HistoryLog> filteredList;
    private String currentSearchQuery = "";

    public HistoryAdapter(List<HistoryLog> list) {
        this.originalList = new ArrayList<>(list);
        this.filteredList = new ArrayList<>(list);
    }

    public void updateData(List<HistoryLog> newList) {
        this.originalList.clear();
        if (newList != null) {
            this.originalList.addAll(newList);
        }
        filter(currentSearchQuery);
    }

    public List<HistoryLog> getFilteredList() {
        return new ArrayList<>(filteredList);
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

        holder.tvTitle.setText(Html.fromHtml(log.getTitle() != null ? log.getTitle() : "", Html.FROM_HTML_MODE_LEGACY));
        holder.tvSubtitle.setText(log.getSubtitle() != null ? log.getSubtitle() : "");
        holder.tvProcessedBy.setText(log.getProcessedBy() != null ? log.getProcessedBy() : "");
        holder.tvTimestamp.setText(log.getTimestamp() != null ? log.getTimestamp() : "");

        if (log.getAmount() != null && !log.getAmount().isEmpty()) {
            holder.tvAmount.setVisibility(View.VISIBLE);
            holder.tvAmount.setText(log.getAmount());

            if (log.getAmount().startsWith("+")) {
                holder.tvAmount.setTextColor(Color.parseColor("#4CAF50"));
            } else if (log.getAmount().startsWith("-")) {
                holder.tvAmount.setTextColor(Color.parseColor("#E53935"));
            } else {
                holder.tvAmount.setTextColor(Color.parseColor("#D97706"));
            }
        } else {
            holder.tvAmount.setVisibility(View.GONE);
        }

        GradientDrawable badgeBackground = new GradientDrawable();
        badgeBackground.setShape(GradientDrawable.OVAL);

        switch (log.getType()) {
            case PAYMENT:
                badgeBackground.setColor(Color.parseColor("#F5E093"));
                holder.tvBadgeIcon.setText("₱");
                break;
            case BOOKING:
                badgeBackground.setColor(Color.parseColor("#D4E7C5"));
                holder.tvBadgeIcon.setText("📅");
                break;
            case REFUND:
                badgeBackground.setColor(Color.parseColor("#F9D2C4"));
                holder.tvBadgeIcon.setText("↺");
                break;
            case STAFF_ACTIVITY:
                badgeBackground.setColor(Color.parseColor("#D0E8F2"));
                holder.tvBadgeIcon.setText("👤");
                break;
        }

        holder.badgeContainer.setBackground(badgeBackground);

        holder.itemView.setOnClickListener(v ->
                Toast.makeText(v.getContext(), "Ref ID: " + log.getId() + " (" + log.getTimestamp() + ")", Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    public void filter(String query) {
        currentSearchQuery = query != null ? query : "";
        filteredList.clear();
        if (currentSearchQuery.trim().isEmpty()) {
            filteredList.addAll(originalList);
        } else {
            String lowerQuery = currentSearchQuery.toLowerCase().trim();
            for (HistoryLog item : originalList) {
                String t = item.getTitle() != null ? item.getTitle().toLowerCase() : "";
                String s = item.getSubtitle() != null ? item.getSubtitle().toLowerCase() : "";
                String p = item.getProcessedBy() != null ? item.getProcessedBy().toLowerCase() : "";
                String id = item.getId() != null ? item.getId().toLowerCase() : "";
                String dt = item.getTimestamp() != null ? item.getTimestamp().toLowerCase() : "";

                if (t.contains(lowerQuery) || s.contains(lowerQuery) || p.contains(lowerQuery)
                        || id.contains(lowerQuery) || dt.contains(lowerQuery)) {
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