package com.example.casavista.adapters;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.casavista.R;
import com.example.casavista.data.ResortRepository;
import com.example.casavista.models.CatalogItem;
import com.example.casavista.network.SupabaseClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SuperAdminRoomAdapter extends RecyclerView.Adapter<SuperAdminRoomAdapter.ViewHolder> {

    private static final String MAINT_PREFS = "AdminMaintenanceSchedulePrefs";

    public interface OnRoomStatusChangeListener {
        void onRoomStatusChanged(CatalogItem item, boolean isUnavailable);
    }

    public interface OnReasonAndDatesSelectedListener {
        void onSelected(String reason, String startDate, String endDate);
    }

    public interface OnRoomDeleteListener {
        void onRoomDeleted(CatalogItem item);
    }

    private List<CatalogItem> list;
    private final OnRoomStatusChangeListener listener;
    private OnRoomDeleteListener deleteListener;
    private final Set<String> todayGuestBookedKeys = new HashSet<>();

    public SuperAdminRoomAdapter(List<CatalogItem> list, OnRoomStatusChangeListener listener) {
        this(list, listener, null);
    }

    public SuperAdminRoomAdapter(List<CatalogItem> list, OnRoomStatusChangeListener listener, OnRoomDeleteListener deleteListener) {
        this.list = list;
        this.listener = listener;
        this.deleteListener = deleteListener;
    }

    public void setOnRoomDeleteListener(OnRoomDeleteListener deleteListener) {
        this.deleteListener = deleteListener;
    }

    public void updateList(List<CatalogItem> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    public void setTodayGuestBookedKeys(Set<String> activeKeys) {
        this.todayGuestBookedKeys.clear();
        if (activeKeys != null) {
            this.todayGuestBookedKeys.addAll(activeKeys);
        }
        notifyDataSetChanged();
    }

    private String normalizeFacilityKey(String title) {
        if (title == null) return "Room 1";
        String t = title.toLowerCase(Locale.US);
        if (t.contains("room 1") || t.contains("101") || t.contains("villa 1")) return "Room 1";
        if (t.contains("room 2") || t.contains("102") || t.contains("villa 2")) return "Room 2";
        if (t.contains("room 3") || t.contains("103") || t.contains("villa 3") || t.contains("loft")) return "Room 3";
        if (t.contains("court 1") || t.contains("pickleball 1") || t.contains("crt-01")) return "Court 1";
        if (t.contains("court 2") || t.contains("pickleball 2") || t.contains("crt-02")) return "Court 2";
        if (t.contains("basketball") || t.contains("court 3") || t.contains("crt-03")) return "Court 3";
        return title.trim();
    }

    private String mapKeyToDbRoomId(String facilityKey) {
        if (facilityKey == null) return null;
        if ("Room 1".equalsIgnoreCase(facilityKey)) return "RM-101";
        if ("Room 2".equalsIgnoreCase(facilityKey)) return "RM-102";
        if ("Room 3".equalsIgnoreCase(facilityKey)) return "RM-103";
        if (facilityKey.toUpperCase(Locale.US).startsWith("RM-")) return facilityKey.toUpperCase(Locale.US);
        return null;
    }

    private String mapKeyToDbCourtId(String facilityKey) {
        if (facilityKey == null) return null;
        if ("Court 1".equalsIgnoreCase(facilityKey)) return "CRT-PB1";
        if ("Court 2".equalsIgnoreCase(facilityKey)) return "CRT-PB2";
        if ("Court 3".equalsIgnoreCase(facilityKey)) return "CRT-BK1";
        if (facilityKey.toUpperCase(Locale.US).startsWith("CRT-")) return facilityKey.toUpperCase(Locale.US);
        return null;
    }

    private String getTodayDateString() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    private String getTomorrowDateString() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 1);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.getTime());
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

        if (holder.imgThumb != null) {
            boolean loaded = false;
            String imageUrl = item.getImageUrl();

            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                try {
                    android.graphics.Bitmap b64 = com.example.casavista.helpers.ImageBase64Helper.decodeBase64ToBitmap(imageUrl);
                    if (b64 != null) {
                        holder.imgThumb.setImageBitmap(b64);
                        loaded = true;
                    } else {
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

        final String facilityKey = normalizeFacilityKey(item.getTitle());
        boolean hasGuestBookingToday = todayGuestBookedKeys.contains(facilityKey);

        SharedPreferences maintPrefs = context.getSharedPreferences(MAINT_PREFS, Context.MODE_PRIVATE);
        String maintStatus = maintPrefs.getString(facilityKey + "_status", "");
        String maintStart = maintPrefs.getString(facilityKey + "_start", "");
        String maintEnd = maintPrefs.getString(facilityKey + "_end", "");

        if (maintStatus == null || maintStatus.isEmpty()) {
            maintStatus = ResortRepository.getInstance().getFacilityStatusOverride(context, facilityKey);
        }
        if (maintStatus == null || maintStatus.isEmpty() || "Available".equalsIgnoreCase(maintStatus)) {
            maintStatus = "Ready";
        }

        if (hasGuestBookingToday) {
            holder.tvStatusBadge.setText("Booked Today");
            holder.tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#C85A32")));
            holder.tvStatusBadge.setTextColor(Color.WHITE);

            holder.btnToggleStatus.setText("Guest Reserved");
            holder.btnToggleStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#A89B8C")));
            holder.btnToggleStatus.setEnabled(false);

            View.OnClickListener reservedListener = v ->
                    Toast.makeText(context, item.getTitle() + " has an active guest booking today and cannot be manually modified.", Toast.LENGTH_SHORT).show();

            holder.btnToggleStatus.setOnClickListener(reservedListener);
            holder.itemView.setOnClickListener(reservedListener);

        } else {
            String badgeText = maintStatus;
            if (!"Ready".equalsIgnoreCase(maintStatus) && !maintStart.isEmpty() && !maintEnd.isEmpty()) {
                String sDisp = maintStart.length() >= 5 ? maintStart.substring(5) : maintStart;
                String eDisp = maintEnd.length() >= 5 ? maintEnd.substring(5) : maintEnd;
                badgeText = maintStatus + " (" + sDisp + "→" + eDisp + ")";
            }
            holder.tvStatusBadge.setText(badgeText);

            if ("Maintenance".equalsIgnoreCase(maintStatus)) {
                holder.tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D97706")));
            } else if ("Cleaning".equalsIgnoreCase(maintStatus)) {
                holder.tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#0284C7")));
            } else if ("Occupied".equalsIgnoreCase(maintStatus) || "Unavailable".equalsIgnoreCase(maintStatus)) {
                holder.tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#C85A32")));
            } else {
                holder.tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4E8D4F")));
            }
            holder.tvStatusBadge.setTextColor(Color.WHITE);

            holder.btnToggleStatus.setText("Set Status");
            holder.btnToggleStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4D391A")));
            holder.btnToggleStatus.setTextColor(Color.WHITE);
            holder.btnToggleStatus.setEnabled(true);

            final String currentResolvedStatus = maintStatus;
            View.OnClickListener setStatusListener = v -> showSetStatusDialog(context, item, facilityKey, currentResolvedStatus, (newStatus, startDate, endDate) -> {
                syncStatusToSupabaseAndPrefs(context, item, facilityKey, newStatus, startDate, endDate);
                int currentPos = holder.getBindingAdapterPosition();
                if (currentPos != RecyclerView.NO_POSITION) {
                    notifyItemChanged(currentPos);
                }
                if (listener != null) {
                    listener.onRoomStatusChanged(item, !"Ready".equalsIgnoreCase(newStatus));
                }
            });

            holder.btnToggleStatus.setOnClickListener(setStatusListener);
            holder.itemView.setOnClickListener(setStatusListener);
        }

        if (holder.btnDeleteRoom != null) {
            holder.btnDeleteRoom.setOnClickListener(v -> {
                new MaterialAlertDialogBuilder(context)
                        .setTitle("Delete Facility")
                        .setMessage("Are you sure you want to permanently delete \"" + item.getTitle() + "\" from inventory and Supabase database?")
                        .setCancelable(false)
                        .setPositiveButton("Delete", (dialog, which) -> {
                            String fKey = normalizeFacilityKey(item.getTitle());
                            String rId = mapKeyToDbRoomId(fKey);
                            if (rId == null && item.getTitle() != null && item.getTitle().toUpperCase(Locale.US).contains("RM-")) {
                                rId = item.getTitle().replaceAll(".*(RM-\\w+).*", "$1");
                            }
                            String cId = mapKeyToDbCourtId(fKey);
                            if (cId == null && item.getTitle() != null && item.getTitle().toUpperCase(Locale.US).contains("CRT-")) {
                                cId = item.getTitle().replaceAll(".*(CRT-\\w+).*", "$1");
                            }

                            if (rId != null) {
                                SupabaseClient.getInstance().deleteRoom(rId, new SupabaseClient.DataCallback() {
                                    @Override public void onSuccess(String s) {}
                                    @Override public void onError(String e) {}
                                });
                            } else if (cId != null) {
                                SupabaseClient.getInstance().deleteCourt(cId, new SupabaseClient.DataCallback() {
                                    @Override public void onSuccess(String s) {}
                                    @Override public void onError(String e) {}
                                });
                            }

                            ResortRepository.getInstance().deleteCatalogItem(context, item);
                            Toast.makeText(context, item.getTitle() + " has been deleted.", Toast.LENGTH_SHORT).show();
                            if (deleteListener != null) {
                                deleteListener.onRoomDeleted(item);
                            }
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                        .show();
            });
        }
    }

    private void syncStatusToSupabaseAndPrefs(Context context, CatalogItem item, String facilityKey, String status, String startDate, String endDate) {
        SharedPreferences maintPrefs = context.getSharedPreferences(MAINT_PREFS, Context.MODE_PRIVATE);
        String existingMaintBookingId = maintPrefs.getString(facilityKey + "_booking_id", null);

        String targetDbStatus = "Ready";
        if ("Maintenance".equalsIgnoreCase(status)) targetDbStatus = "Maintenance";
        else if ("Cleaning".equalsIgnoreCase(status)) targetDbStatus = "Cleaning";
        else if ("Occupied".equalsIgnoreCase(status) || "Unavailable".equalsIgnoreCase(status)) targetDbStatus = "Occupied";

        ResortRepository.getInstance().setFacilityStatusOverride(context, item.getTitle(), targetDbStatus);
        ResortRepository.getInstance().setFacilityStatusOverride(context, facilityKey, targetDbStatus);

        String dbRoomId = mapKeyToDbRoomId(facilityKey);
        String dbCourtId = mapKeyToDbCourtId(facilityKey);

        if ("Ready".equalsIgnoreCase(targetDbStatus)) {
            maintPrefs.edit()
                    .remove(facilityKey + "_status")
                    .remove(facilityKey + "_start")
                    .remove(facilityKey + "_end")
                    .remove(facilityKey + "_booking_id")
                    .apply();

            if (existingMaintBookingId != null && !existingMaintBookingId.isEmpty()) {
                SupabaseClient.getInstance().updateBookingStatus(existingMaintBookingId, "Cancelled", new SupabaseClient.DataCallback() {
                    @Override public void onSuccess(String s) {}
                    @Override public void onError(String e) {}
                });
            }
        } else {
            String maintBookingId = String.format(Locale.US, "MNT-%s-%04d",
                    facilityKey.replaceAll("\\s+", "").toUpperCase(Locale.US),
                    (int) (System.currentTimeMillis() % 10000));

            maintPrefs.edit()
                    .putString(facilityKey + "_status", targetDbStatus)
                    .putString(facilityKey + "_start", startDate)
                    .putString(facilityKey + "_end", endDate)
                    .putString(facilityKey + "_booking_id", maintBookingId)
                    .apply();

            if (existingMaintBookingId != null && !existingMaintBookingId.isEmpty()) {
                SupabaseClient.getInstance().updateBookingStatus(existingMaintBookingId, "Cancelled", new SupabaseClient.DataCallback() {
                    @Override public void onSuccess(String s) {}
                    @Override public void onError(String e) {}
                });
            }

            SupabaseClient.getInstance().createMaintenanceBlockBooking(
                    maintBookingId,
                    dbRoomId,
                    dbCourtId,
                    startDate,
                    endDate,
                    new SupabaseClient.DataCallback() {
                        @Override public void onSuccess(String s) {}
                        @Override public void onError(String e) {}
                    }
            );
        }

        if (dbRoomId != null) {
            String roomNum = dbRoomId.replace("RM-", "");
            String roomType = "RM-103".equals(dbRoomId) ? "Deluxe" : "Standard";
            double rate = "RM-103".equals(dbRoomId) ? 3500.0 : 2500.0;

            SupabaseClient.getInstance().updateRoomStatus(dbRoomId, roomNum, rate, 4, targetDbStatus, roomType, new SupabaseClient.DataCallback() {
                @Override public void onSuccess(String s) {}
                @Override public void onError(String e) {}
            });
        }

        Toast.makeText(context, item.getTitle() + " status set to " + targetDbStatus, Toast.LENGTH_SHORT).show();
    }

    private void showSetStatusDialog(Context context, CatalogItem item, String facilityKey, String currentStatus, OnReasonAndDatesSelectedListener onSelected) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_select_unavailable_reason, null);
        TextView tvRoomSub = dialogView.findViewById(R.id.tvDialogRoomSub);
        RadioButton radioReady = dialogView.findViewById(R.id.radioStatusReady);
        RadioButton radioMaintenance = dialogView.findViewById(R.id.radioStatusMaintenance);
        RadioButton radioCleaning = dialogView.findViewById(R.id.radioStatusCleaning);
        RadioButton radioOccupied = dialogView.findViewById(R.id.radioStatusOccupied);
        View layoutDates = dialogView.findViewById(R.id.layoutMaintenanceScheduleDates);
        MaterialButton btnDateFrom = dialogView.findViewById(R.id.btnDialogDateFrom);
        MaterialButton btnDateTo = dialogView.findViewById(R.id.btnDialogDateTo);

        if (tvRoomSub != null && item != null) {
            tvRoomSub.setText("Select status for " + item.getTitle());
        }

        SharedPreferences maintPrefs = context.getSharedPreferences(MAINT_PREFS, Context.MODE_PRIVATE);
        String savedStart = maintPrefs.getString(facilityKey + "_start", getTodayDateString());
        String savedEnd = maintPrefs.getString(facilityKey + "_end", getTomorrowDateString());

        final String[] selectedStart = {savedStart};
        final String[] selectedEnd = {savedEnd};

        if (btnDateFrom != null) btnDateFrom.setText("From: " + selectedStart[0]);
        if (btnDateTo != null) btnDateTo.setText("To: " + selectedEnd[0]);

        if (btnDateFrom != null) {
            btnDateFrom.setOnClickListener(v -> pickDate(context, selectedStart[0], picked -> {
                selectedStart[0] = picked;
                btnDateFrom.setText("From: " + picked);
                if (selectedEnd[0].compareTo(picked) <= 0) {
                    selectedEnd[0] = addOneDay(picked);
                    if (btnDateTo != null) btnDateTo.setText("To: " + selectedEnd[0]);
                }
            }));
        }

        if (btnDateTo != null) {
            btnDateTo.setOnClickListener(v -> pickDate(context, selectedEnd[0], picked -> {
                if (picked.compareTo(selectedStart[0]) <= 0) {
                    selectedEnd[0] = addOneDay(selectedStart[0]);
                } else {
                    selectedEnd[0] = picked;
                }
                btnDateTo.setText("To: " + selectedEnd[0]);
            }));
        }

        View.OnClickListener radioClickListener = v -> {
            boolean isReady = radioReady != null && radioReady.isChecked();
            if (layoutDates != null) {
                layoutDates.setVisibility(isReady ? View.GONE : View.VISIBLE);
            }
        };

        if (radioReady != null) radioReady.setOnClickListener(radioClickListener);
        if (radioMaintenance != null) radioMaintenance.setOnClickListener(radioClickListener);
        if (radioCleaning != null) radioCleaning.setOnClickListener(radioClickListener);
        if (radioOccupied != null) radioOccupied.setOnClickListener(radioClickListener);

        if ("Maintenance".equalsIgnoreCase(currentStatus)) {
            if (radioMaintenance != null) radioMaintenance.setChecked(true);
            if (layoutDates != null) layoutDates.setVisibility(View.VISIBLE);
        } else if ("Cleaning".equalsIgnoreCase(currentStatus)) {
            if (radioCleaning != null) radioCleaning.setChecked(true);
            if (layoutDates != null) layoutDates.setVisibility(View.VISIBLE);
        } else if ("Occupied".equalsIgnoreCase(currentStatus) || "Unavailable".equalsIgnoreCase(currentStatus)) {
            if (radioOccupied != null) radioOccupied.setChecked(true);
            if (layoutDates != null) layoutDates.setVisibility(View.VISIBLE);
        } else {
            if (radioReady != null) radioReady.setChecked(true);
            if (layoutDates != null) layoutDates.setVisibility(View.GONE);
        }

        new MaterialAlertDialogBuilder(context)
                .setTitle("Set Facility Status")
                .setView(dialogView)
                .setPositiveButton("Save Status", (dialog, which) -> {
                    String chosenStatus = "Ready";
                    if (radioMaintenance != null && radioMaintenance.isChecked()) {
                        chosenStatus = "Maintenance";
                    } else if (radioCleaning != null && radioCleaning.isChecked()) {
                        chosenStatus = "Cleaning";
                    } else if (radioOccupied != null && radioOccupied.isChecked()) {
                        chosenStatus = "Occupied";
                    }

                    if (onSelected != null) {
                        onSelected.onSelected(chosenStatus, selectedStart[0], selectedEnd[0]);
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private interface DateCallback {
        void onPicked(String dateStr);
    }

    private void pickDate(Context context, String initial, DateCallback cb) {
        Calendar cal = Calendar.getInstance();
        try {
            String[] p = initial.split("-");
            if (p.length == 3) {
                cal.set(Integer.parseInt(p[0]), Integer.parseInt(p[1]) - 1, Integer.parseInt(p[2]));
            }
        } catch (Exception ignored) {}

        DatePickerDialog dpd = new DatePickerDialog(
                context,
                (view, year, month, dayOfMonth) -> cb.onPicked(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)),
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );
        dpd.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dpd.show();
    }

    private String addOneDay(String yyyyMmDd) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date d = sdf.parse(yyyyMmDd);
            Calendar cal = Calendar.getInstance();
            if (d != null) cal.setTime(d);
            cal.add(Calendar.DAY_OF_YEAR, 1);
            return sdf.format(cal.getTime());
        } catch (Exception e) {
            return getTomorrowDateString();
        }
    }

    private void showConfirmationDialog(Context context, CatalogItem item, String newStatus, Runnable onConfirm) {
        new MaterialAlertDialogBuilder(context)
                .setTitle("Confirm Status Change")
                .setMessage("Are you sure you want to set " + item.getTitle() + " to " + newStatus + "?")
                .setPositiveButton("Yes, Save", (dialog, which) -> {
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
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
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
        MaterialButton btnToggleStatus, btnDeleteRoom;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgThumb = itemView.findViewById(R.id.imgRoomThumb);
            tvTitle = itemView.findViewById(R.id.tvRoomTitle);
            tvSubtitle = itemView.findViewById(R.id.tvRoomSubtitle);
            tvPrice = itemView.findViewById(R.id.tvRoomPrice);
            tvStatusBadge = itemView.findViewById(R.id.tvRoomStatusBadge);
            btnToggleStatus = itemView.findViewById(R.id.btnToggleRoomStatus);
            btnDeleteRoom = itemView.findViewById(R.id.btnDeleteRoom);
        }
    }
}