package com.example.casavista.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;

import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.casavista.R;
import com.example.casavista.data.ResortRepository;
import com.example.casavista.models.CatalogItem;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.List;

public class SuperAdminRoomAdapter extends RecyclerView.Adapter<SuperAdminRoomAdapter.ViewHolder> {

    public interface OnRoomStatusChangeListener {
        void onRoomStatusChanged(CatalogItem item, boolean isUnavailable);
    }

    public interface OnReasonSelectedListener {
        void onReasonSelected(String reason);
    }

    private List<CatalogItem> list;
    private final OnRoomStatusChangeListener listener;

    public SuperAdminRoomAdapter(List<CatalogItem> list, OnRoomStatusChangeListener listener) {
        this.list = list;
        this.listener = listener;
    }

    public void updateList(List<CatalogItem> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_super_admin_room, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CatalogItem item = list.get(position);
        Context context = holder.itemView.getContext();

        holder.tvTitle.setText(item.getTitle());
        holder.tvSubtitle.setText(item.getSubtitle());
        holder.tvPrice.setText(item.getPrice());

        // Image
        if (holder.imgThumb != null) {
            boolean loaded = false;
            String imageUrl = item.getImageUrl();

            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                try {
                    File imgFile = new File(imageUrl);
                    if (imgFile.exists()) {
                        android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                        if (bitmap != null) {
                            holder.imgThumb.setImageBitmap(bitmap);
                            loaded = true;
                        }
                    } else if (imageUrl.startsWith("content://")) {
                        holder.imgThumb.setImageURI(Uri.parse(imageUrl));
                        loaded = true;
                    }
                } catch (Exception ignored) {}
            }

            if (!loaded && item.getImageResId() != 0 && item.getImageResId() != R.drawable.ic_casavista_artwork) {
                holder.imgThumb.setImageResource(item.getImageResId());
                loaded = true;
            }

            if (!loaded) {
                String titleLower = item.getTitle() != null ? item.getTitle().toLowerCase() : "";
                if (titleLower.contains("pickleball")) {
                    holder.imgThumb.setImageResource(R.drawable.pickleball_icon_img);
                } else if (titleLower.contains("basketball")) {
                    holder.imgThumb.setImageResource(R.drawable.basketball_icon_img);
                } else if (titleLower.contains("room") || titleLower.contains("suite") || titleLower.contains("villa") || titleLower.contains("loft")) {
                    holder.imgThumb.setImageResource(R.drawable.rooms_icon_img);
                } else {
                    holder.imgThumb.setImageResource(R.drawable.resort_icon_img);
                }
            }
        }

        if (holder.tvPrice != null) {
            holder.tvPrice.setOnClickListener(v -> showEditPriceDialog(context, item, () -> notifyItemChanged(holder.getBindingAdapterPosition())));
        }

        boolean hasGuestBooking = ResortRepository.getInstance().hasActiveGuestBooking(item.getTitle());
        String manualOverride = ResortRepository.getInstance().getFacilityStatusOverride(context, item.getTitle());
        boolean isUnavailable = "Unavailable".equalsIgnoreCase(manualOverride) || "Maintenance".equalsIgnoreCase(manualOverride) || "Cleaning".equalsIgnoreCase(manualOverride);

        if (hasGuestBooking) {
            holder.tvStatusBadge.setText("Booked");
            holder.tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#C85A32")));
            holder.tvStatusBadge.setTextColor(Color.WHITE);

            holder.btnToggleStatus.setText("Guest Reserved");
            holder.btnToggleStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#A89B8C")));
            holder.btnToggleStatus.setEnabled(false);

            View.OnClickListener reservedListener = v ->
                    Toast.makeText(context, "This room is currently reserved by a guest and cannot be manually opened.", Toast.LENGTH_SHORT).show();

            holder.btnToggleStatus.setOnClickListener(reservedListener);
            holder.itemView.setOnClickListener(reservedListener);

        } else if (isUnavailable) {
            String badgeText = (manualOverride != null && !manualOverride.trim().isEmpty()) ? manualOverride : "Unavailable";
            holder.tvStatusBadge.setText(badgeText);

            if ("Cleaning".equalsIgnoreCase(manualOverride)) {
                holder.tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#0284C7"))); // Blue / Teal
            } else if ("Maintenance".equalsIgnoreCase(manualOverride)) {
                holder.tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D97706"))); // Amber / Orange
            } else {
                holder.tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#C85A32"))); // Rust Red
            }
            holder.tvStatusBadge.setTextColor(Color.WHITE);

            holder.btnToggleStatus.setText("Set as Available");
            holder.btnToggleStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4E8D4F")));
            holder.btnToggleStatus.setEnabled(true);

            View.OnClickListener availableListener = v -> showConfirmationDialog(context, item, "Available", () -> {
                ResortRepository.getInstance().setFacilityStatusOverride(context, item.getTitle(), "Available");
                Toast.makeText(context, "Updated " + item.getTitle() + " to Available", Toast.LENGTH_SHORT).show();
                int currentPos = holder.getBindingAdapterPosition();
                if (currentPos != RecyclerView.NO_POSITION) {
                    notifyItemChanged(currentPos);
                }
                if (listener != null) {
                    listener.onRoomStatusChanged(item, false);
                }
            });

            holder.btnToggleStatus.setOnClickListener(availableListener);
            holder.itemView.setOnClickListener(availableListener);

        } else {
            holder.tvStatusBadge.setText("Available");
            holder.tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4E8D4F")));
            holder.tvStatusBadge.setTextColor(Color.WHITE);

            holder.btnToggleStatus.setText("Set as Unavailable");
            holder.btnToggleStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#C85A32")));
            holder.btnToggleStatus.setEnabled(true);

            View.OnClickListener unavailableListener = v -> showUnavailableReasonDialog(context, item, chosenReason -> {
                showConfirmationDialog(context, item, chosenReason, () -> {
                    ResortRepository.getInstance().setFacilityStatusOverride(context, item.getTitle(), chosenReason);
                    Toast.makeText(context, "Updated " + item.getTitle() + " to " + chosenReason, Toast.LENGTH_SHORT).show();
                    int currentPos = holder.getBindingAdapterPosition();
                    if (currentPos != RecyclerView.NO_POSITION) {
                        notifyItemChanged(currentPos);
                    }
                    if (listener != null) {
                        listener.onRoomStatusChanged(item, true);
                    }
                });
            });

            holder.btnToggleStatus.setOnClickListener(unavailableListener);
            holder.itemView.setOnClickListener(unavailableListener);
        }
    }

    private void showUnavailableReasonDialog(Context context, CatalogItem item, OnReasonSelectedListener onReasonSelected) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_select_unavailable_reason, null);
        TextView tvRoomSub = dialogView.findViewById(R.id.tvDialogRoomSub);
        RadioButton radioMaintenance = dialogView.findViewById(R.id.radioMaintenance);
        RadioButton radioCleaning = dialogView.findViewById(R.id.radioCleaning);

        if (tvRoomSub != null && item != null) {
            tvRoomSub.setText("Select unavailability reason for " + item.getTitle());
        }

        new MaterialAlertDialogBuilder(context)
                .setView(dialogView)
                .setPositiveButton("Next", (dialog, which) -> {
                    String chosenReason = "Unavailable";
                    if (radioMaintenance != null && radioMaintenance.isChecked()) {
                        chosenReason = "Maintenance";
                    } else if (radioCleaning != null && radioCleaning.isChecked()) {
                        chosenReason = "Cleaning";
                    }

                    if (onReasonSelected != null) {
                        onReasonSelected.onReasonSelected(chosenReason);
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showConfirmationDialog(Context context, CatalogItem item, String newStatus, Runnable onConfirm) {
        new MaterialAlertDialogBuilder(context)
                .setTitle("Confirm Status Change")
                .setMessage("Are you sure you want to set " + item.getTitle() + " to " + newStatus + "?")
                .setPositiveButton("Yes, Set " + newStatus, (dialog, which) -> {
                    if (onConfirm != null) {
                        onConfirm.run();
                    }
                })
                .setNegativeButton("Edit Price (SuperAdmin)", (dialog, which) -> {
                    showEditPriceDialog(context, item, onConfirm);
                })
                .setNeutralButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showEditPriceDialog(Context context, CatalogItem item, Runnable onPriceUpdated) {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(context);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        final android.widget.EditText input = new android.widget.EditText(context);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        input.setHint("Enter price e.g. ₱2,500 / day");
        if (item != null && item.getPrice() != null) {
            input.setText(item.getPrice());
        }
        layout.addView(input);

        new MaterialAlertDialogBuilder(context)
                .setTitle("Edit Price (SuperAdmin Only)")
                .setMessage("Update tariff rate for " + (item != null ? item.getTitle() : "facility"))
                .setView(layout)
                .setPositiveButton("Save Price", (dialog, which) -> {
                    String newPrice = input.getText().toString().trim();
                    if (!newPrice.isEmpty() && item != null) {
                        item.setPrice(newPrice);
                        ResortRepository.getInstance().updateCatalogItem(context, item);
                        Toast.makeText(context, "Price updated for " + item.getTitle() + " to " + newPrice, Toast.LENGTH_SHORT).show();
                        if (onPriceUpdated != null) {
                            onPriceUpdated.run();
                        }
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgThumb;
        TextView tvTitle, tvSubtitle, tvPrice, tvStatusBadge;
        MaterialButton btnToggleStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgThumb = itemView.findViewById(R.id.imgRoomThumb);
            tvTitle = itemView.findViewById(R.id.tvRoomTitle);
            tvSubtitle = itemView.findViewById(R.id.tvRoomSubtitle);
            tvPrice = itemView.findViewById(R.id.tvRoomPrice);
            tvStatusBadge = itemView.findViewById(R.id.tvRoomStatusBadge);
            btnToggleStatus = itemView.findViewById(R.id.btnToggleRoomStatus);
        }
    }
}