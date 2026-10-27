package com.example.casavista.adapters;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.casavista.R;
import com.example.casavista.data.ResortRepository;
import com.example.casavista.models.CatalogItem;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.util.List;

public class CatalogAdapter extends RecyclerView.Adapter<CatalogAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(CatalogItem item);
    }

    private final List<CatalogItem> catalogList;
    private final OnItemClickListener listener;

    public CatalogAdapter(List<CatalogItem> catalogList, OnItemClickListener listener) {
        this.catalogList = catalogList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_amenity_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CatalogItem item = catalogList.get(position);

        if (holder.tvTitle != null) {
            holder.tvTitle.setText(item.getTitle());
        }
        if (holder.tvSubtitle != null) {
            holder.tvSubtitle.setText(item.getSubtitle());
        }
        if (holder.tvPrice != null) {
            holder.tvPrice.setText(item.getPrice());
        }

        if (holder.imgCatalog != null) {
            String imageUrl = item.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                try {
                    android.graphics.Bitmap b64 = com.example.casavista.helpers.ImageBase64Helper.decodeBase64ToBitmap(imageUrl);
                    if (b64 != null) {
                        holder.imgCatalog.setImageBitmap(b64);
                    } else if (imageUrl.startsWith("content://")) {
                        holder.imgCatalog.setImageURI(Uri.parse(imageUrl));
                    } else {
                        File imgFile = new File(imageUrl);
                        if (imgFile.exists()) {
                            holder.imgCatalog.setImageURI(Uri.fromFile(imgFile));
                        } else {
                            holder.imgCatalog.setImageURI(Uri.parse(imageUrl));
                        }
                    }
                } catch (Exception e) {
                    if (item.getImageResId() != 0) {
                        holder.imgCatalog.setImageResource(item.getImageResId());
                    } else {
                        holder.imgCatalog.setImageResource(R.drawable.ic_launcher_background);
                    }
                }
            } else if (item.getImageResId() != 0) {
                holder.imgCatalog.setImageResource(item.getImageResId());
            } else {
                holder.imgCatalog.setImageResource(R.drawable.ic_launcher_background);
            }
        }

        String facilityStatus = ResortRepository.getInstance().getFacilityDisplayStatus(holder.itemView.getContext(), item.getTitle());

        if (holder.btnBookNow != null) {
            if ("Maintenance".equalsIgnoreCase(facilityStatus)) {
                holder.btnBookNow.setText("Under Maintenance");
                holder.btnBookNow.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D97706"))); // Amber / Orange
                holder.btnBookNow.setTextColor(Color.WHITE);

                View.OnClickListener blockedListener = v ->
                        Toast.makeText(v.getContext(), "This room is currently under maintenance.", Toast.LENGTH_SHORT).show();

                holder.itemView.setOnClickListener(blockedListener);
                holder.btnBookNow.setOnClickListener(blockedListener);

            } else if ("Cleaning".equalsIgnoreCase(facilityStatus)) {
                holder.btnBookNow.setText("Under Cleaning");
                holder.btnBookNow.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#0284C7"))); // Blue / Teal
                holder.btnBookNow.setTextColor(Color.WHITE);

                View.OnClickListener blockedListener = v ->
                        Toast.makeText(v.getContext(), "This room is currently undergoing cleaning.", Toast.LENGTH_SHORT).show();

                holder.itemView.setOnClickListener(blockedListener);
                holder.btnBookNow.setOnClickListener(blockedListener);

            } else {
                // ALWAYS ALLOW GUESTS TO CLICK "Reserve" TO OPEN THE RESERVE POPUP & PICK STAY DATES!
                holder.btnBookNow.setText("Reserve");
                holder.btnBookNow.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EADFD3")));
                holder.btnBookNow.setTextColor(Color.parseColor("#3D2B10"));

                View.OnClickListener clickListener = v -> {
                    if (listener != null) {
                        listener.onItemClick(item);
                    }
                };

                holder.itemView.setOnClickListener(clickListener);
                holder.btnBookNow.setOnClickListener(clickListener);
            }
        }
    }

    @Override
    public int getItemCount() {
        return catalogList != null ? catalogList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCatalog;
        TextView tvTitle, tvSubtitle, tvPrice;
        MaterialButton btnBookNow;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCatalog = itemView.findViewById(R.id.imgCatalog);
            tvTitle = itemView.findViewById(R.id.tvCatalogTitle);
            tvSubtitle = itemView.findViewById(R.id.tvCatalogSubtitle);
            tvPrice = itemView.findViewById(R.id.tvCatalogPrice);
            btnBookNow = itemView.findViewById(R.id.btnBookNow);
        }
    }
}