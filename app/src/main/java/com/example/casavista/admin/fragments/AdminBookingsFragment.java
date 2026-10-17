package com.example.casavista.admin.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.casavista.R;
import com.example.casavista.adapters.AdminBookingAdapter;
import com.example.casavista.admin.AdminMainActivity;
import com.example.casavista.data.ResortRepository;
import com.example.casavista.helpers.AvatarHelper;
import com.example.casavista.models.Booking;
import com.example.casavista.network.SupabaseClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminBookingsFragment extends Fragment implements AdminBookingAdapter.OnBookingActionListener {

    private ImageView imgHeaderAvatar;
    private TextView tvHeaderInitials;
    private RecyclerView rvAdminBookings;
    private EditText etSearchBookings;
    private TextView filterAll, filterRooms, filterPickleball, filterBasketball;

    private ResortRepository repository;
    private AdminBookingAdapter adapter;
    private String currentCategoryFilter = "All";
    private String currentStatusFilter = "All";

    private final List<TextView> statusFilterPills = new ArrayList<>();
    private final Map<String, String> customerNameDirectory = new HashMap<>();
    private final List<Booking> cachedSupabaseBookings = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_bookings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = ResortRepository.getInstance();

        imgHeaderAvatar = view.findViewById(R.id.imgHeaderAvatar);
        tvHeaderInitials = view.findViewById(R.id.tvHeaderInitials);

        View cardAvatar = view.findViewById(R.id.cardAdminBookingsAvatar);
        if (cardAvatar != null) {
            cardAvatar.setOnClickListener(v -> {
                if (getActivity() instanceof AdminMainActivity) {
                    AdminMainActivity adminActivity = (AdminMainActivity) getActivity();
                    adminActivity.loadFragment(new AdminProfileFragment());
                    BottomNavigationView bottomNav = adminActivity.findViewById(R.id.adminBottomNavigationView);
                    if (bottomNav != null) {
                        bottomNav.setSelectedItemId(R.id.admin_nav_profile);
                    }
                }
            });
        }

        etSearchBookings = view.findViewById(R.id.etSearchBookings);
        rvAdminBookings = view.findViewById(R.id.rvAdminBookings);

        filterAll = view.findViewById(R.id.filterAll);
        filterRooms = view.findViewById(R.id.filterRooms);
        filterPickleball = view.findViewById(R.id.filterPickleball);
        filterBasketball = view.findViewById(R.id.filterBasketball);

        if (rvAdminBookings != null) {
            rvAdminBookings.setLayoutManager(new LinearLayoutManager(getContext()));
        }

        setupCategoryFilters();
        injectStatusFilterBar(view);

        if (etSearchBookings != null) {
            etSearchBookings.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterAndDisplayBookings(cachedSupabaseBookings);
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        loadCustomersThenBookings();
    }

    private void injectStatusFilterBar(View rootView) {
        if (getContext() == null || rvAdminBookings == null) return;
        ViewGroup parent = (ViewGroup) rvAdminBookings.getParent();
        if (parent == null) return;

        int rvIndex = parent.indexOfChild(rvAdminBookings);
        if (rvIndex < 0) return;

        float density = requireContext().getResources().getDisplayMetrics().density;

        HorizontalScrollView hsv = new HorizontalScrollView(requireContext());
        hsv.setHorizontalScrollBarEnabled(false);
        hsv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout.LayoutParams hsvParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        hsvParams.setMargins(0, 0, 0, Math.round(12 * density));
        hsv.setLayoutParams(hsvParams);

        LinearLayout statusRow = new LinearLayout(requireContext());
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);

        String[] statuses = {"All", "Confirmed", "Pending", "Checked-In", "Completed", "Cancelled"};
        statusFilterPills.clear();

        int padH = Math.round(14 * density);
        int padV = Math.round(7 * density);
        int marginE = Math.round(8 * density);

        for (String st : statuses) {
            TextView pill = new TextView(requireContext());
            pill.setText("All".equals(st) ? "All Status" : st);
            pill.setTag(st);
            pill.setTextSize(11f);
            pill.setTypeface(null, Typeface.BOLD);
            pill.setPadding(padH, padV, padH, padV);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            lp.setMarginEnd(marginE);
            pill.setLayoutParams(lp);

            pill.setOnClickListener(v -> {
                currentStatusFilter = st;
                updateStatusFilterPillStyles();
                filterAndDisplayBookings(cachedSupabaseBookings);
            });

            statusFilterPills.add(pill);
            statusRow.addView(pill);
        }

        hsv.addView(statusRow);
        parent.addView(hsv, rvIndex);
        updateStatusFilterPillStyles();
    }

    private void updateStatusFilterPillStyles() {
        if (getContext() == null) return;
        for (TextView pill : statusFilterPills) {
            String tag = (String) pill.getTag();
            if (tag != null && tag.equalsIgnoreCase(currentStatusFilter)) {
                pill.setBackgroundResource(R.drawable.bg_btn_pos);
                pill.setTextColor(ContextCompat.getColor(requireContext(), R.color.bg_main));
            } else {
                pill.setBackgroundResource(R.drawable.bg_white);
                pill.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            }
        }
    }

    private void setupCategoryFilters() {
        if (filterAll != null) {
            filterAll.setOnClickListener(v -> {
                currentCategoryFilter = "All";
                highlightFilter(filterAll, filterRooms, filterPickleball, filterBasketball);
                filterAndDisplayBookings(cachedSupabaseBookings);
            });
        }
        if (filterRooms != null) {
            filterRooms.setOnClickListener(v -> {
                currentCategoryFilter = "Rooms";
                highlightFilter(filterRooms, filterAll, filterPickleball, filterBasketball);
                filterAndDisplayBookings(cachedSupabaseBookings);
            });
        }
        if (filterPickleball != null) {
            filterPickleball.setOnClickListener(v -> {
                currentCategoryFilter = "Pickleball";
                highlightFilter(filterPickleball, filterAll, filterRooms, filterBasketball);
                filterAndDisplayBookings(cachedSupabaseBookings);
            });
        }
        if (filterBasketball != null) {
            filterBasketball.setOnClickListener(v -> {
                currentCategoryFilter = "Basketball";
                highlightFilter(filterBasketball, filterAll, filterRooms, filterPickleball);
                filterAndDisplayBookings(cachedSupabaseBookings);
            });
        }
    }

    private void highlightFilter(TextView active, TextView... inactives) {
        if (getContext() == null) return;
        active.setBackgroundResource(R.drawable.bg_btn_pos);
        active.setTextColor(ContextCompat.getColor(requireContext(), R.color.bg_main));

        for (TextView inactive : inactives) {
            if (inactive != null) {
                inactive.setBackgroundResource(R.drawable.bg_white);
                inactive.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            }
        }
    }

    private void loadCustomersThenBookings() {
        if (getContext() == null) return;
        SupabaseClient.getInstance().fetchAllCustomers(new SupabaseClient.DataCallback() {
            @Override
            public void onSuccess(String jsonArrayString) {
                try {
                    JSONArray arr = new JSONArray(jsonArrayString);
                    customerNameDirectory.clear();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject c = arr.getJSONObject(i);
                        String id = c.optString("customer_id", "");
                        String email = c.optString("email", "");
                        String first = c.optString("first_name", "");
                        String last = c.optString("last_name", "");
                        String fullName = (first + " " + last).trim();
                        if (!id.isEmpty() && !fullName.isEmpty()) {
                            customerNameDirectory.put(id, fullName);
                        }
                        if (!email.isEmpty() && !fullName.isEmpty()) {
                            customerNameDirectory.put(email.toLowerCase(), fullName);
                        }
                    }
                } catch (Exception ignored) {}
                loadAdminBookingsFromSupabase();
            }

            @Override
            public void onError(String errorMessage) {
                loadAdminBookingsFromSupabase();
            }
        });
    }

    private void loadAdminBookingsFromSupabase() {
        if (rvAdminBookings == null || getContext() == null) return;

        SupabaseClient.getInstance().fetchBookings(new SupabaseClient.DataCallback() {
            @Override
            public void onSuccess(String jsonArrayString) {
                if (!isAdded() || getContext() == null) return;
                try {
                    JSONArray array = new JSONArray(jsonArrayString);
                    List<Booking> fetchedList = new ArrayList<>();

                    for (int i = 0; i < array.length(); i++) {
                        try {
                            JSONObject obj = array.getJSONObject(i);
                            String bId = obj.optString("booking_id", "");
                            if (bId.startsWith("MNT-")) {
                                // Hide internal maintenance blocks from guest transaction list
                                continue;
                            }

                            String custId = obj.optString("customer_id", "");
                            String custEmail = obj.optString("customer_email", "");
                            String status = obj.optString("status", "Pending");
                            String checkInDate = obj.optString("check_in_date", "");
                            String bookingDate = obj.optString("booking_date", "");
                            String displayDate = (!checkInDate.isEmpty() && !"null".equalsIgnoreCase(checkInDate))
                                    ? checkInDate : bookingDate;

                            double totalAmt = obj.optDouble("total_amount", 0.0);
                            if (Double.isNaN(totalAmt)) totalAmt = 0.0;

                            String courtId = obj.optString("court_id", "");
                            boolean isExclusive = obj.optBoolean("exclusive", false);
                            int paddleCount = obj.optInt("paddle_count", 0);

                            String title;
                            String detail1;

                            if (!courtId.isEmpty() && !"null".equalsIgnoreCase(courtId)) {
                                boolean isBasketball = courtId.toUpperCase().contains("BK")
                                        || courtId.toUpperCase().contains("BASKET")
                                        || courtId.toUpperCase().contains("CRT-03")
                                        || courtId.toUpperCase().contains("COURT 3");
                                String sportLabel = isBasketball ? "Basketball Court" : "Pickleball Court";
                                title = sportLabel + " (" + courtId + ") • #" + bId;

                                String start = formatShortTime(obj.optString("start_time", ""));
                                String end = formatShortTime(obj.optString("end_time", ""));
                                detail1 = start + " - " + end + (paddleCount > 0 ? " • " + paddleCount + " Paddles" : "");
                            } else {
                                JSONArray roomIdsArr = obj.optJSONArray("room_ids");
                                StringBuilder roomsBuilder = new StringBuilder();
                                if (roomIdsArr != null && roomIdsArr.length() > 0) {
                                    for (int r = 0; r < roomIdsArr.length(); r++) {
                                        String rVal = roomIdsArr.optString(r, "");
                                        if (!rVal.isEmpty() && !"null".equalsIgnoreCase(rVal)) {
                                            if (roomsBuilder.length() > 0) roomsBuilder.append(", ");
                                            roomsBuilder.append(rVal);
                                        }
                                    }
                                }
                                if (roomsBuilder.length() == 0) {
                                    String singleRoom = obj.optString("room_id", "RM-101");
                                    roomsBuilder.append((!singleRoom.isEmpty() && !"null".equalsIgnoreCase(singleRoom)) ? singleRoom : "RM-101");
                                }

                                String suiteType = isExclusive ? "Exclusive Resort Booking (" + roomsBuilder + ")" : "Villa Suite (" + roomsBuilder + ")";
                                title = suiteType + " • #" + bId;
                                detail1 = "In: " + obj.optString("check_in_date", displayDate) + " → Out: " + obj.optString("check_out_date", "");
                            }

                            String guestDisplay = customerNameDirectory.get(custId);
                            if ((guestDisplay == null || guestDisplay.isEmpty()) && !custEmail.isEmpty() && !"null".equalsIgnoreCase(custEmail)) {
                                guestDisplay = customerNameDirectory.get(custEmail.toLowerCase());
                            }
                            if (guestDisplay == null || guestDisplay.isEmpty()) {
                                if (!custEmail.isEmpty() && !"null".equalsIgnoreCase(custEmail)) {
                                    guestDisplay = custEmail + " (" + custId + ")";
                                } else {
                                    guestDisplay = "Guest • " + custId;
                                }
                            } else if (!custEmail.isEmpty() && !"null".equalsIgnoreCase(custEmail)) {
                                guestDisplay = guestDisplay + " • " + custEmail;
                            }

                            Booking b = new Booking(title, status, displayDate, detail1, guestDisplay, String.format(Locale.US, "₱%,.2f", totalAmt));
                            b.setBookingId(bId);
                            b.setCustomerId(custId);
                            b.setTotalAmount(totalAmt);
                            fetchedList.add(b);
                        } catch (Exception itemErr) {
                            itemErr.printStackTrace();
                        }
                    }

                    cachedSupabaseBookings.clear();
                    cachedSupabaseBookings.addAll(fetchedList);
                    filterAndDisplayBookings(cachedSupabaseBookings);
                } catch (Exception e) {
                    e.printStackTrace();
                    fallbackToLocalBookings();
                }
            }

            @Override
            public void onError(String errorMessage) {
                fallbackToLocalBookings();
            }
        });
    }

    private String formatShortTime(String rawTime) {
        if (rawTime == null || rawTime.isEmpty() || "null".equalsIgnoreCase(rawTime)) return "";
        if (rawTime.length() >= 5) return rawTime.substring(0, 5);
        return rawTime;
    }

    private void fallbackToLocalBookings() {
        if (getContext() == null) return;
        repository.loadSavedBookings(requireContext());
        cachedSupabaseBookings.clear();
        cachedSupabaseBookings.addAll(repository.getBookings("ALL"));
        filterAndDisplayBookings(cachedSupabaseBookings);
    }

    private void filterAndDisplayBookings(List<Booking> allBookings) {
        if (getContext() == null || rvAdminBookings == null) return;
        String query = etSearchBookings != null ? etSearchBookings.getText().toString().trim().toLowerCase() : "";

        List<Booking> filteredList = new ArrayList<>();
        for (Booking b : allBookings) {
            String title = b.getTitle() != null ? b.getTitle().toLowerCase() : "";
            String d1 = b.getDetail1() != null ? b.getDetail1().toLowerCase() : "";
            String d2 = b.getDetail2() != null ? b.getDetail2().toLowerCase() : "";
            String bId = b.getBookingId() != null ? b.getBookingId().toLowerCase() : "";
            String status = b.getStatus() != null ? b.getStatus().trim() : "Pending";

            boolean matchesCategory = "All".equalsIgnoreCase(currentCategoryFilter) ||
                    ("Rooms".equalsIgnoreCase(currentCategoryFilter) && (title.contains("room") || title.contains("villa") || title.contains("rm-") || title.contains("exclusive"))) ||
                    ("Pickleball".equalsIgnoreCase(currentCategoryFilter) && (title.contains("pickleball") || title.contains("crt-01") || title.contains("crt-02"))) ||
                    ("Basketball".equalsIgnoreCase(currentCategoryFilter) && (title.contains("basketball") || title.contains("crt-03") || title.contains("bk")));

            boolean matchesStatus = "All".equalsIgnoreCase(currentStatusFilter)
                    || status.equalsIgnoreCase(currentStatusFilter);

            boolean matchesQuery = query.isEmpty()
                    || title.contains(query)
                    || d1.contains(query)
                    || d2.contains(query)
                    || bId.contains(query)
                    || status.toLowerCase().contains(query);

            if (matchesCategory && matchesStatus && matchesQuery) {
                filteredList.add(b);
            }
        }

        if (adapter == null) {
            adapter = new AdminBookingAdapter(filteredList, this);
            rvAdminBookings.setAdapter(adapter);
        } else {
            adapter.updateList(filteredList);
        }
    }

    @Override
    public void onBookingStatusChanged() {
        loadCustomersThenBookings();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getContext() != null) {
            SharedPreferences adminPrefs = requireContext().getSharedPreferences("AdminPrefs", Context.MODE_PRIVATE);
            String staffName = adminPrefs.getString("staff_name", ResortRepository.getInstance().getCurrentUserName());
            AvatarHelper.loadAvatar(getContext(), imgHeaderAvatar, tvHeaderInitials, staffName);
            loadCustomersThenBookings();
        }
    }
}