package com.example.casavista.superadmin.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.casavista.R;
import com.example.casavista.adapters.SuperAdminRoomAdapter;
import com.example.casavista.data.ResortRepository;
import com.example.casavista.helpers.AvatarHelper;
import com.example.casavista.models.CatalogItem;
import com.example.casavista.network.SupabaseClient;
import com.example.casavista.superadmin.SuperAdminMainActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SuperAdminRoomsFragment extends Fragment implements SuperAdminRoomAdapter.OnRoomStatusChangeListener {

    private static final String MAINT_PREFS = "AdminMaintenanceSchedulePrefs";

    private RecyclerView rvRooms;
    private MaterialButton tabAll, tabVillas, tabCourts, tabBuyout;
    private ResortRepository repository;
    private SuperAdminRoomAdapter adapter;
    private String currentCategory = "All";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_super_admin_rooms, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = ResortRepository.getInstance();

        rvRooms = view.findViewById(R.id.rvSuperAdminRooms);
        if (rvRooms != null) {
            rvRooms.setLayoutManager(new LinearLayoutManager(getContext()));
        }

        tabAll = view.findViewById(R.id.tabSuperRoomAll);
        tabVillas = view.findViewById(R.id.tabSuperRoomVillas);
        tabCourts = view.findViewById(R.id.tabSuperRoomCourts);
        tabBuyout = view.findViewById(R.id.tabSuperRoomBuyout);

        if (tabAll != null) tabAll.setOnClickListener(v -> filterCategory("All"));
        if (tabVillas != null) tabVillas.setOnClickListener(v -> filterCategory("Room"));
        if (tabCourts != null) tabCourts.setOnClickListener(v -> filterCategory("Courts"));
        if (tabBuyout != null) tabBuyout.setOnClickListener(v -> filterCategory("Buyout"));

        MaterialButton btnAddFacility = view.findViewById(R.id.btnAddFacility);
        if (btnAddFacility != null) {
            btnAddFacility.setOnClickListener(v -> showAddFacilityDialog());
        }

        View cardAvatar = view.findViewById(R.id.cardSuperAdminRoomsAvatar);
        if (cardAvatar != null) {
            cardAvatar.setOnClickListener(v -> {
                if (getActivity() instanceof SuperAdminMainActivity) {
                    SuperAdminMainActivity superActivity = (SuperAdminMainActivity) getActivity();
                    superActivity.loadFragment(new SuperAdminProfileFragment());
                    BottomNavigationView navView = superActivity.findViewById(R.id.superAdminNavView);
                    if (navView != null) {
                        navView.setSelectedItemId(R.id.nav_super_profile);
                    }
                }
            });
        }

        loadRoomsList();
        syncSupabaseFacilityState();
    }

    private void showAddFacilityDialog() {
        if (!isAdded() || getContext() == null) return;
        final Context ctx = requireContext();

        android.widget.ScrollView scrollView = new android.widget.ScrollView(ctx);
        android.widget.LinearLayout layout = new android.widget.LinearLayout(ctx);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(40, 24, 40, 16);
        scrollView.addView(layout);

        android.widget.TextView tvTypeLabel = new android.widget.TextView(ctx);
        tvTypeLabel.setText("FACILITY TYPE");
        tvTypeLabel.setTextColor(Color.parseColor("#4D391A"));
        tvTypeLabel.setTextSize(11);
        tvTypeLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(tvTypeLabel);

        android.widget.RadioGroup radioGroupType = new android.widget.RadioGroup(ctx);
        radioGroupType.setOrientation(android.widget.RadioGroup.HORIZONTAL);
        android.widget.RadioButton rbRoom = new android.widget.RadioButton(ctx);
        rbRoom.setText("Room / Villa");
        rbRoom.setChecked(true);
        android.widget.RadioButton rbCourt = new android.widget.RadioButton(ctx);
        rbCourt.setText("Sports Court");
        radioGroupType.addView(rbRoom);
        radioGroupType.addView(rbCourt);
        layout.addView(radioGroupType);

        final com.google.android.material.textfield.TextInputLayout layoutId = new com.google.android.material.textfield.TextInputLayout(ctx);
        layoutId.setHint("Facility ID (e.g. RM-104 or CRT-04)");
        final com.google.android.material.textfield.TextInputEditText etId = new com.google.android.material.textfield.TextInputEditText(ctx);
        etId.setText("RM-104");
        layoutId.addView(etId);
        layout.addView(layoutId);

        final com.google.android.material.textfield.TextInputLayout layoutTitle = new com.google.android.material.textfield.TextInputLayout(ctx);
        layoutTitle.setHint("Facility Title (e.g. Villa Suite 104)");
        final com.google.android.material.textfield.TextInputEditText etTitle = new com.google.android.material.textfield.TextInputEditText(ctx);
        etTitle.setText("Villa Suite 104");
        layoutTitle.addView(etTitle);
        layout.addView(layoutTitle);

        final com.google.android.material.textfield.TextInputLayout layoutSubtitle = new com.google.android.material.textfield.TextInputLayout(ctx);
        layoutSubtitle.setHint("Amenities / Subtitle");
        final com.google.android.material.textfield.TextInputEditText etSubtitle = new com.google.android.material.textfield.TextInputEditText(ctx);
        etSubtitle.setText("King Bed • Garden View • Mini Bar");
        layoutSubtitle.addView(etSubtitle);
        layout.addView(layoutSubtitle);

        final com.google.android.material.textfield.TextInputLayout layoutPrice = new com.google.android.material.textfield.TextInputLayout(ctx);
        layoutPrice.setHint("Rate / Price in PHP (e.g. 2800 or 500)");
        final com.google.android.material.textfield.TextInputEditText etPrice = new com.google.android.material.textfield.TextInputEditText(ctx);
        etPrice.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etPrice.setText("2800");
        layoutPrice.addView(etPrice);
        layout.addView(layoutPrice);

        final com.google.android.material.textfield.TextInputLayout layoutCapacity = new com.google.android.material.textfield.TextInputLayout(ctx);
        layoutCapacity.setHint("Capacity (e.g. 4 Guests or 8 Players)");
        final com.google.android.material.textfield.TextInputEditText etCapacity = new com.google.android.material.textfield.TextInputEditText(ctx);
        etCapacity.setText("4 Guests");
        layoutCapacity.addView(etCapacity);
        layout.addView(layoutCapacity);

        radioGroupType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == rbRoom.getId()) {
                etId.setText("RM-104");
                etTitle.setText("Villa Suite 104");
                etSubtitle.setText("King Bed • Garden View • Mini Bar");
                etPrice.setText("2800");
                etCapacity.setText("4 Guests");
            } else {
                etId.setText("CRT-PB4");
                etTitle.setText("Pickleball Court 4");
                etSubtitle.setText("Professional Cushion Surface • Night Lit");
                etPrice.setText("500");
                etCapacity.setText("8 Players");
            }
        });

        android.widget.TextView tvStatusLabel = new android.widget.TextView(ctx);
        tvStatusLabel.setText("\nINITIAL OPERATING STATUS");
        tvStatusLabel.setTextColor(Color.parseColor("#4D391A"));
        tvStatusLabel.setTextSize(11);
        tvStatusLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(tvStatusLabel);

        final android.widget.Spinner spinnerStatus = new android.widget.Spinner(ctx);
        String[] statuses = {"Available / Ready", "Maintenance", "Cleaning"};
        android.widget.ArrayAdapter<String> statusAdapter = new android.widget.ArrayAdapter<>(ctx, android.R.layout.simple_spinner_dropdown_item, statuses);
        spinnerStatus.setAdapter(statusAdapter);
        layout.addView(spinnerStatus);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(ctx)
                .setTitle("Add New Resort Facility")
                .setView(scrollView)
                .setCancelable(false)
                .setPositiveButton("Create Facility", (dialog, which) -> {
                    String id = etId.getText() != null ? etId.getText().toString().trim() : "";
                    String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
                    String subtitle = etSubtitle.getText() != null ? etSubtitle.getText().toString().trim() : "";
                    String priceStr = etPrice.getText() != null ? etPrice.getText().toString().trim() : "2500";
                    String capacity = etCapacity.getText() != null ? etCapacity.getText().toString().trim() : "4 Guests";
                    String selectedStatus = spinnerStatus.getSelectedItem() != null ? spinnerStatus.getSelectedItem().toString() : "Available / Ready";

                    if (id.isEmpty() || title.isEmpty()) {
                        android.widget.Toast.makeText(ctx, "Please enter Facility ID and Title.", android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double rate = 2500.0;
                    try {
                        rate = Double.parseDouble(priceStr);
                    } catch (Exception ignored) {}

                    boolean isRoomType = rbRoom.isChecked();
                    String category = isRoomType ? "Room" : (title.toLowerCase(Locale.US).contains("basketball") ? "Basketball" : "Pickleball");
                    int defaultImg = isRoomType ? R.drawable.rooms_icon_img : (category.equals("Basketball") ? R.drawable.basketball_icon_img : R.drawable.pickleball_icon_img);
                    String priceLabel = isRoomType ? ("₱" + String.format(Locale.US, "%,.0f", rate) + " / day") : ("₱" + String.format(Locale.US, "%,.0f", rate) + " / hr");

                    CatalogItem newItem = new CatalogItem(
                            title,
                            subtitle,
                            category,
                            capacity,
                            priceLabel,
                            "Standard",
                            defaultImg,
                            null
                    );

                    repository.addCatalogItem(ctx, newItem);

                    String dbStatus = "Ready";
                    if (selectedStatus.contains("Maintenance")) dbStatus = "Maintenance";
                    else if (selectedStatus.contains("Cleaning")) dbStatus = "Cleaning";

                    if (isRoomType) {
                        try {
                            JSONObject roomPayload = new JSONObject();
                            roomPayload.put("room_id", id);
                            roomPayload.put("room_number", id.replace("RM-", ""));
                            roomPayload.put("rate_per_night", rate);
                            roomPayload.put("capacity", 4);
                            roomPayload.put("status", dbStatus);
                            roomPayload.put("room_type", "Standard");

                            SupabaseClient.getInstance().createRoom(roomPayload, new SupabaseClient.DataCallback() {
                                @Override
                                public void onSuccess(String jsonResponse) {
                                    if (isAdded() && getContext() != null) {
                                        android.widget.Toast.makeText(ctx, "Room created and synced to Supabase!", android.widget.Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onError(String errorMessage) {}
                            });
                        } catch (Exception ignored) {}
                    } else {
                        try {
                            JSONObject courtPayload = new JSONObject();
                            courtPayload.put("court_id", id);
                            courtPayload.put("court_name", title);
                            courtPayload.put("sport_type", category);
                            courtPayload.put("day_rate", rate);
                            courtPayload.put("night_rate", rate * 1.3);
                            courtPayload.put("surface_type", "Pro Cushion");
                            courtPayload.put("status", dbStatus);

                            SupabaseClient.getInstance().createCourt(courtPayload, new SupabaseClient.DataCallback() {
                                @Override
                                public void onSuccess(String jsonResponse) {
                                    if (isAdded() && getContext() != null) {
                                        android.widget.Toast.makeText(ctx, "Court created and synced to Supabase!", android.widget.Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onError(String errorMessage) {}
                            });
                        } catch (Exception ignored) {}
                    }

                    android.widget.Toast.makeText(ctx, "Facility \"" + title + "\" added successfully!", android.widget.Toast.LENGTH_SHORT).show();
                    loadRoomsList();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void filterCategory(String category) {
        this.currentCategory = category;
        highlightTabs(category);
        loadRoomsList();
    }

    private void highlightTabs(String category) {
        MaterialButton[] tabs = {tabAll, tabVillas, tabCourts, tabBuyout};
        String[] catNames = {"All", "Room", "Courts", "Buyout"};

        for (int i = 0; i < tabs.length; i++) {
            if (tabs[i] != null) {
                if (catNames[i].equalsIgnoreCase(category) || (category.equalsIgnoreCase("Room") && catNames[i].equalsIgnoreCase("Room"))) {
                    tabs[i].setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4D391A")));
                    tabs[i].setTextColor(Color.WHITE);
                } else {
                    tabs[i].setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EADFD3")));
                    tabs[i].setTextColor(Color.parseColor("#59493B"));
                }
            }
        }
    }

    private void loadRoomsList() {
        if (rvRooms == null || repository == null || getContext() == null) return;

        repository.loadSavedImages(requireContext());
        List<CatalogItem> allItems = repository.getCatalogItems();

        List<CatalogItem> filtered = new ArrayList<>();
        for (CatalogItem item : allItems) {
            String cat = item.getCategory() != null ? item.getCategory() : "";
            if ("All".equalsIgnoreCase(currentCategory)) {
                filtered.add(item);
            } else if ("Room".equalsIgnoreCase(currentCategory) && (cat.equalsIgnoreCase("Room") || cat.equalsIgnoreCase("Rooms"))) {
                filtered.add(item);
            } else if ("Courts".equalsIgnoreCase(currentCategory) && (cat.equalsIgnoreCase("Pickleball") || cat.equalsIgnoreCase("Basketball"))) {
                filtered.add(item);
            } else if ("Buyout".equalsIgnoreCase(currentCategory) && cat.equalsIgnoreCase("Buyout")) {
                filtered.add(item);
            }
        }

        if (adapter == null) {
            adapter = new SuperAdminRoomAdapter(filtered, this, item -> loadRoomsList());
            rvRooms.setAdapter(adapter);
        } else {
            adapter.setOnRoomDeleteListener(item -> loadRoomsList());
            adapter.updateList(filtered);
        }
    }

    private void syncSupabaseFacilityState() {
        if (getContext() == null) return;
        final String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

        SupabaseClient.getInstance().fetchBookings(new SupabaseClient.DataCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                if (!isAdded() || getContext() == null) return;
                Set<String> activeTodayKeys = new HashSet<>();
                SharedPreferences maintPrefs = requireContext().getSharedPreferences(MAINT_PREFS, Context.MODE_PRIVATE);

                try {
                    JSONArray arr = new JSONArray(jsonResponse);
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject b = arr.getJSONObject(i);
                        String bId = b.optString("booking_id", "");
                        String status = b.optString("status", "");
                        if (!"Confirmed".equalsIgnoreCase(status) && !"Checked-In".equalsIgnoreCase(status)) {
                            continue;
                        }

                        boolean isMaint = bId.startsWith("MNT-");
                        String checkIn = b.optString("check_in_date", "");
                        String checkOut = b.optString("check_out_date", "");

                        if (checkIn.length() >= 10 && checkOut.length() >= 10) {
                            String inDate = checkIn.substring(0, 10);
                            String outDate = checkOut.substring(0, 10);
                            boolean occupiesToday = todayStr.compareTo(inDate) >= 0 && todayStr.compareTo(outDate) < 0;

                            JSONArray rIds = b.optJSONArray("room_ids");
                            if (rIds != null && rIds.length() > 0) {
                                for (int j = 0; j < rIds.length(); j++) {
                                    String uiKey = mapDbIdToKey(rIds.optString(j));
                                    if (uiKey != null && occupiesToday) {
                                        if (isMaint) {
                                            maintPrefs.edit()
                                                    .putString(uiKey + "_status", "Maintenance")
                                                    .putString(uiKey + "_start", inDate)
                                                    .putString(uiKey + "_end", outDate)
                                                    .putString(uiKey + "_booking_id", bId)
                                                    .apply();
                                        } else {
                                            activeTodayKeys.add(uiKey);
                                        }
                                    }
                                }
                            }
                        }

                        String courtId = b.optString("court_id", "");
                        String bDate = b.optString("booking_date", "");
                        if (!courtId.isEmpty() && !"null".equalsIgnoreCase(courtId) && bDate.startsWith(todayStr)) {
                            String uiCourt = mapDbIdToKey(courtId);
                            if (uiCourt != null && !isMaint) {
                                activeTodayKeys.add(uiCourt);
                            }
                        }
                    }
                } catch (Exception ignored) {}

                if (adapter != null) {
                    adapter.setTodayGuestBookedKeys(activeTodayKeys);
                }
            }

            @Override
            public void onError(String errorMessage) {}
        });
    }

    private String mapDbIdToKey(String dbId) {
        if (dbId == null) return null;
        String u = dbId.toUpperCase(Locale.US);
        if (u.contains("101")) return "Room 1";
        if (u.contains("102")) return "Room 2";
        if (u.contains("103")) return "Room 3";
        if (u.contains("CRT-01")) return "Court 1";
        if (u.contains("CRT-02")) return "Court 2";
        if (u.contains("CRT-03") || u.contains("BK")) return "Court 3";
        return null;
    }

    @Override
    public void onRoomStatusChanged(CatalogItem item, boolean isBooked) {
        syncSupabaseFacilityState();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadHeaderAvatar();
        loadRoomsList();
        syncSupabaseFacilityState();
    }

    private void loadHeaderAvatar() {
        if (getView() == null || getContext() == null) return;
        ImageView imgAvatar = getView().findViewById(R.id.imgHeaderAvatarRooms);
        if (imgAvatar == null) return;

        File avatarFile = new File(requireContext().getFilesDir(), "super_admin_avatar.jpg");
        if (avatarFile.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(avatarFile.getAbsolutePath());
            if (bitmap != null) {
                imgAvatar.setImageBitmap(bitmap);
                return;
            }
        }

        AvatarHelper.loadAvatar(getContext(), imgAvatar, null, "Dr. Lemyel Yap");
    }
}