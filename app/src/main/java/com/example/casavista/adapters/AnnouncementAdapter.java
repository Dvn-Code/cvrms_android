package com.example.casavista.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.example.casavista.R;
import com.example.casavista.models.Announcement;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.InputStream;
import java.util.List;

public class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.ViewHolder> {

    public interface OnAnnouncementActionListener {
        void onDeleteAnnouncement(Announcement item, int position);
    }

    private final List<Announcement> list;
    private final boolean isAdmin;
    private OnAnnouncementActionListener actionListener;

    // Constructor for Guest / User View (Read-only)
    public AnnouncementAdapter(List<Announcement> list) {
        this(list, false, null);
    }

    // Constructor for Super Admin View (Includes delete menu)
    public AnnouncementAdapter(List<Announcement> list, boolean isAdmin, OnAnnouncementActionListener actionListener) {
        this.list = list;
        this.isAdmin = isAdmin;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_announcement, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Announcement item = list.get(position);

        holder.tvTitle.setText(item.getTitle());
        holder.tvBody.setText(item.getBody());
        holder.tvDate.setText(item.getDate());

        // Display Admin Options if in Admin Mode
        if (isAdmin && actionListener != null) {
            holder.btnAdminMenu.setVisibility(View.VISIBLE);
            holder.btnAdminMenu.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(v.getContext(), holder.btnAdminMenu);
                popup.getMenu().add("Delete Announcement");
                popup.setOnMenuItemClickListener(menuItem -> {
                    int pos = holder.getBindingAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        actionListener.onDeleteAnnouncement(item, pos);
                    }
                    return true;
                });
                popup.show();
            });
        } else {
            holder.btnAdminMenu.setVisibility(View.GONE);
        }

        // Handle Announcement Picture
        if (item.getImagePath() != null && !item.getImagePath().isEmpty()) {
            boolean loaded = false;

            // Try Base64 decoding from Supabase cloud
            Bitmap b64Bitmap = com.example.casavista.helpers.ImageBase64Helper.decodeBase64ToBitmap(item.getImagePath());
            if (b64Bitmap != null) {
                holder.imgAnnouncement.setImageBitmap(b64Bitmap);
                holder.imgAnnouncement.setVisibility(View.VISIBLE);
                loaded = true;
            }

            // Try decoding as local file
            if (!loaded) {
                File imgFile = new File(item.getImagePath());
                if (imgFile.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                    if (bitmap != null) {
                        holder.imgAnnouncement.setImageBitmap(bitmap);
                        holder.imgAnnouncement.setVisibility(View.VISIBLE);
                        loaded = true;
                    }
                }
            }

            // Try decoding as content Uri if local file fail
            if (!loaded) {
                try {
                    Uri uri = Uri.parse(item.getImagePath());
                    InputStream is = holder.itemView.getContext().getContentResolver().openInputStream(uri);
                    Bitmap bitmap = BitmapFactory.decodeStream(is);
                    if (bitmap != null) {
                        holder.imgAnnouncement.setImageBitmap(bitmap);
                        holder.imgAnnouncement.setVisibility(View.VISIBLE);
                        loaded = true;
                    }
                } catch (Exception ignored) { }
            }

            if (!loaded) {
                holder.imgAnnouncement.setVisibility(View.GONE);
            }
        } else {
            holder.imgAnnouncement.setVisibility(View.GONE);
        }

        // Open full announcement details dialog on tap
        View.OnClickListener cardClickListener = v -> showAnnouncementDetailsDialog(v.getContext(), item);

        holder.itemView.setOnClickListener(cardClickListener);
        if (holder.tvTitle != null) holder.tvTitle.setOnClickListener(cardClickListener);
        if (holder.tvBody != null) holder.tvBody.setOnClickListener(cardClickListener);
        if (holder.tvReadMorePrompt != null) holder.tvReadMorePrompt.setOnClickListener(cardClickListener);
        if (holder.imgAnnouncement != null) holder.imgAnnouncement.setOnClickListener(cardClickListener);
    }

    private void showAnnouncementDetailsDialog(Context context, Announcement item) {
        if (context == null || item == null) return;

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_announcement_details, null);

        ImageView imgDialog = dialogView.findViewById(R.id.dialogImgAnnouncement);
        TextView tvDate = dialogView.findViewById(R.id.dialogTvDate);
        TextView tvTitle = dialogView.findViewById(R.id.dialogTvTitle);
        TextView tvAuthor = dialogView.findViewById(R.id.dialogTvAuthor);
        TextView tvBody = dialogView.findViewById(R.id.dialogTvBody);

        if (tvDate != null) tvDate.setText(item.getDate());
        if (tvTitle != null) tvTitle.setText(item.getTitle());
        if (tvBody != null) tvBody.setText(item.getBody());

        if (tvAuthor != null) {
            if (item.getAuthor() != null && !item.getAuthor().trim().isEmpty()) {
                tvAuthor.setText("Posted by " + item.getAuthor());
                tvAuthor.setVisibility(View.VISIBLE);
            } else {
                tvAuthor.setVisibility(View.GONE);
            }
        }

        // Handle Image
        if (imgDialog != null) {
            if (item.getImagePath() != null && !item.getImagePath().isEmpty()) {
                boolean loaded = false;

                // Try Base64 decoding
                Bitmap b64Bitmap = com.example.casavista.helpers.ImageBase64Helper.decodeBase64ToBitmap(item.getImagePath());
                if (b64Bitmap != null) {
                    imgDialog.setImageBitmap(b64Bitmap);
                    imgDialog.setVisibility(View.VISIBLE);
                    loaded = true;
                }

                if (!loaded) {
                    File imgFile = new File(item.getImagePath());
                    if (imgFile.exists()) {
                        Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                        if (bitmap != null) {
                            imgDialog.setImageBitmap(bitmap);
                            imgDialog.setVisibility(View.VISIBLE);
                            loaded = true;
                        }
                    }
                }

                if (!loaded) {
                    try {
                        Uri uri = Uri.parse(item.getImagePath());
                        InputStream is = context.getContentResolver().openInputStream(uri);
                        Bitmap bitmap = BitmapFactory.decodeStream(is);
                        if (bitmap != null) {
                            imgDialog.setImageBitmap(bitmap);
                            imgDialog.setVisibility(View.VISIBLE);
                            loaded = true;
                        }
                    } catch (Exception ignored) { }
                }

                if (!loaded) {
                    imgDialog.setVisibility(View.GONE);
                }
            } else {
                imgDialog.setVisibility(View.GONE);
            }
        }

        new MaterialAlertDialogBuilder(context)
                .setView(dialogView)
                .setPositiveButton("Close", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvBody, tvDate, tvReadMorePrompt;
        ImageView imgAnnouncement;
        ImageButton btnAdminMenu;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvAnnouncementTitle);
            tvBody = itemView.findViewById(R.id.tvAnnouncementBody);
            tvDate = itemView.findViewById(R.id.tvAnnouncementDate);
            tvReadMorePrompt = itemView.findViewById(R.id.tvReadMorePrompt);
            imgAnnouncement = itemView.findViewById(R.id.imgAnnouncement);
            btnAdminMenu = itemView.findViewById(R.id.btnAdminMenu);
        }
    }
}