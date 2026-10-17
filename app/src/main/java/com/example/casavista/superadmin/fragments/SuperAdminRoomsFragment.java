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
            adapter = new SuperAdminRoomAdapter(filtered, this);
            rvRooms.setAdapter(adapter);
        } else {
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