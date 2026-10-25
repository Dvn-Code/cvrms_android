package com.example.casavista.user.fragments;

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
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.casavista.R;
import com.example.casavista.adapters.BookingAdapter;
import com.example.casavista.data.ResortRepository;
import com.example.casavista.models.CatalogItem;
import com.example.casavista.user.MainActivity;
import com.google.android.material.button.MaterialButton;

import java.io.File;

public class BookingsFragment extends Fragment {

    private ResortRepository repository;
    private RecyclerView rvBookings;
    private MaterialButton tabUpcoming, tabPast, tabCancelled;
    private String currentTab = "Upcoming";
    private boolean isFetchingBookings = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bookings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = ResortRepository.getInstance();

        rvBookings = view.findViewById(R.id.rvBookingsList);
        rvBookings.setLayoutManager(new LinearLayoutManager(requireContext()));

        tabUpcoming = view.findViewById(R.id.tabUpcoming);
        tabPast = view.findViewById(R.id.tabPast);
        tabCancelled = view.findViewById(R.id.tabCancelled);

        tabUpcoming.setOnClickListener(v -> {
            animateSelection(v);
            highlightTab(tabUpcoming, tabPast, tabCancelled);
            loadBookings("Upcoming");
        });

        tabPast.setOnClickListener(v -> {
            animateSelection(v);
            highlightTab(tabPast, tabUpcoming, tabCancelled);
            loadBookings("Past");
        });

        tabCancelled.setOnClickListener(v -> {
            animateSelection(v);
            highlightTab(tabCancelled, tabUpcoming, tabPast);
            loadBookings("Cancelled");
        });

        loadHeaderProfile(view);

        View cardAvatar = view.findViewById(R.id.cardBookingsAvatar);
        if (cardAvatar != null) {
            cardAvatar.setOnClickListener(v -> {
                MainActivity mainActivity = (MainActivity) getActivity();
                if (mainActivity != null) {
                    mainActivity.loadFragment(new ProfileFragment());
                    com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = mainActivity.findViewById(R.id.bottomNavigationView);
                    if (bottomNav != null) {
                        bottomNav.setSelectedItemId(R.id.nav_profile);
                    }
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadHeaderProfile(getView());
        loadBookings(currentTab);
    }

    private void loadHeaderProfile(View view) {
        if (view == null || getContext() == null) return;

        ImageView imgAvatar = view.findViewById(R.id.imgHomeAvatar);
        TextView tvInitials = view.findViewById(R.id.tvHomeAvatarInitials);
        TextView tvGreeting = view.findViewById(R.id.tvHomeGreeting);

        if (imgAvatar == null || tvInitials == null) return;

        SharedPreferences prefs = requireContext().getSharedPreferences("CasavistaProfilePrefs", Context.MODE_PRIVATE);
        String name = prefs.getString("user_name", "John Lucero Cabilao");
        String firstName = prefs.getString("user_first_name", "");

        if (tvGreeting != null) {
            if (!firstName.isEmpty()) {
                tvGreeting.setText("KUMUSTA, " + firstName.toUpperCase() + "!");
            } else if (name != null && !name.trim().isEmpty()) {
                String[] parts = name.trim().split("\\s+");
                tvGreeting.setText("KUMUSTA, " + parts[0].toUpperCase() + "!");
            }
        }

        String email = prefs.getString("user_email", "");
        String emailCleanKey = email.toLowerCase().replaceAll("[^a-zA-Z0-9]", "_");

        File file = new File(requireContext().getFilesDir(), "profile_pic_" + emailCleanKey + ".jpg");
        if (!file.exists()) {
            file = new File(requireContext().getFilesDir(), "profile_pic.jpg");
        }
        if (file.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            if (bitmap != null) {
                imgAvatar.setImageBitmap(bitmap);
                imgAvatar.setVisibility(View.VISIBLE);
                tvInitials.setVisibility(View.GONE);
                return;
            }
        }

        imgAvatar.setVisibility(View.GONE);
        tvInitials.setVisibility(View.VISIBLE);
        if (name != null && !name.trim().isEmpty()) {
            String[] parts = name.trim().split("\\s+");
            if (parts.length >= 2) {
                tvInitials.setText(("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase());
            } else {
                tvInitials.setText(("" + parts[0].charAt(0)).toUpperCase());
            }
        } else {
            tvInitials.setText("CV");
        }
    }

    private void loadBookings(String type) {
        currentTab = type;
        if (repository == null || rvBookings == null || getContext() == null) return;

        // 1. Display persistent local bookings immediately (with Catalog images loaded)
        repository.loadSavedImages(requireContext());
        repository.loadSavedBookings(requireContext());
        BookingAdapter localAdapter = new BookingAdapter(repository.getBookings(type));
        rvBookings.setAdapter(localAdapter);

        if (isFetchingBookings) return;
        isFetchingBookings = true;

        SharedPreferences prefs = requireContext().getSharedPreferences("CasavistaProfilePrefs", Context.MODE_PRIVATE);
        String currentUserEmail = prefs.getString("user_email", "").trim();
        String emailCleanKey = currentUserEmail.toLowerCase().replaceAll("[^a-zA-Z0-9]", "_");
        String currentCustId = prefs.getString("customer_id", prefs.getString("customer_id_" + emailCleanKey, "")).trim();

        // If customer_id is missing after logout/login, fetch customer profile by email first
        if (currentCustId.isEmpty() && !currentUserEmail.isEmpty()) {
            com.example.casavista.network.SupabaseClient.getInstance().fetchCustomerProfile(
                    currentUserEmail,
                    new com.example.casavista.network.SupabaseClient.DataCallback() {
                        @Override
                        public void onSuccess(String jsonStr) {
                            if (getContext() != null && jsonStr != null) {
                                try {
                                    org.json.JSONObject custObj = new org.json.JSONObject(jsonStr);
                                    String dbCustId = custObj.optString("customer_id", "");
                                    if (!dbCustId.isEmpty()) {
                                        prefs.edit()
                                                .putString("customer_id", dbCustId)
                                                .putString("customer_id_" + emailCleanKey, dbCustId)
                                                .apply();
                                    }
                                } catch (Exception ignored) {}
                            }
                            fetchAndMergeLiveBookings(type);
                        }

                        @Override
                        public void onError(String errorMessage) {
                            fetchAndMergeLiveBookings(type);
                        }
                    }
            );
        } else {
            fetchAndMergeLiveBookings(type);
        }
    }

    private CatalogItem findMatchingCatalogItem(String title, String roomId, String courtId, boolean isExclusive) {
        if (repository == null) return null;
        String normTarget = repository.getNormalizedFacilityKey(
                isExclusive ? "Full Resort Buyout" : (!roomId.isEmpty() && !"null".equalsIgnoreCase(roomId) ? roomId : (!courtId.isEmpty() && !"null".equalsIgnoreCase(courtId) ? courtId : title))
        );
        for (CatalogItem item : repository.getCatalogItems()) {
            if (item == null || item.getTitle() == null) continue;
            if (item.getTitle().equalsIgnoreCase(title)) {
                return item;
            }
            String normItem = repository.getNormalizedFacilityKey(item.getTitle());
            if (!normTarget.isEmpty() && normTarget.equalsIgnoreCase(normItem)) {
                return item;
            }
        }
        return null;
    }

    private void fetchAndMergeLiveBookings(String type) {
        com.example.casavista.network.SupabaseClient.getInstance().fetchBookings(
                new com.example.casavista.network.SupabaseClient.DataCallback() {
                    @Override
                    public void onSuccess(String jsonArrayString) {
                        isFetchingBookings = false;
                        if (getContext() == null || jsonArrayString == null) return;
                        try {
                            repository.loadSavedImages(requireContext());

                            org.json.JSONArray array = new org.json.JSONArray(jsonArrayString);
                            SharedPreferences prefs = requireContext().getSharedPreferences("CasavistaProfilePrefs", Context.MODE_PRIVATE);
                            String currentUserEmail = prefs.getString("user_email", "").trim();
                            String emailCleanKey = currentUserEmail.toLowerCase().replaceAll("[^a-zA-Z0-9]", "_");
                            String currentCustId = prefs.getString("customer_id", prefs.getString("customer_id_" + emailCleanKey, "")).trim();
                            String lastBookingId = prefs.getString("last_booking_id", "").trim();

                            // Iterate from oldest to newest so addBooking(0, b) keeps newest on top
                            for (int i = array.length() - 1; i >= 0; i--) {
                                org.json.JSONObject obj = array.getJSONObject(i);
                                String bCustId = obj.optString("customer_id", "").trim();
                                String bCustEmail = obj.optString("customer_email", "").trim();
                                String bId = obj.optString("booking_id", "").trim();
                                if (bId.isEmpty()) continue;

                                boolean isLocalBooking = false;
                                for (com.example.casavista.models.Booking localB : repository.getBookings()) {
                                    if (bId.equalsIgnoreCase(localB.getBookingId())) {
                                        isLocalBooking = true;
                                        break;
                                    }
                                }

                                boolean matchesCustId = !currentCustId.isEmpty() && bCustId.equalsIgnoreCase(currentCustId);
                                boolean matchesEmail = !currentUserEmail.isEmpty() && (
                                        bCustEmail.equalsIgnoreCase(currentUserEmail)
                                                || bCustId.equalsIgnoreCase(currentUserEmail)
                                                || (!bCustEmail.isEmpty() && bCustEmail.split("@")[0].equalsIgnoreCase(currentUserEmail.split("@")[0]))
                                );
                                boolean matchesLastBooking = !lastBookingId.isEmpty() && bId.equalsIgnoreCase(lastBookingId);

                                if (matchesCustId || matchesEmail || matchesLastBooking || isLocalBooking) {
                                    // Persist customer_id for future sessions if matched by email
                                    if (!bCustId.isEmpty() && matchesEmail && currentCustId.isEmpty()) {
                                        currentCustId = bCustId;
                                        prefs.edit()
                                                .putString("customer_id", bCustId)
                                                .putString("customer_id_" + emailCleanKey, bCustId)
                                                .apply();
                                    }

                                    String bStatus = obj.optString("status", "Pending");
                                    String bDate = obj.optString("booking_date", obj.optString("check_in_date", "2026-10-14"));
                                    String courtId = obj.optString("court_id", "");
                                    String roomId = obj.optString("room_id", "");
                                    boolean isExclusive = obj.optBoolean("exclusive", false);
                                    double totalAmt = obj.optDouble("total_amount", 2500.0);

                                    String checkInDate = obj.optString("check_in_date", "");
                                    String checkOutDate = obj.optString("check_out_date", "");

                                    boolean hasCourt = !courtId.isEmpty() && !"null".equalsIgnoreCase(courtId);
                                    String title;
                                    int imgRes;

                                    if (hasCourt) {
                                        if (courtId.toUpperCase().contains("BK")) {
                                            title = "Basketball • Basketball Half-Court";
                                            imgRes = R.drawable.basketball_icon_img;
                                        } else if (courtId.contains("2")) {
                                            title = "Pickleball • Court 2";
                                            imgRes = R.drawable.pickleball_icon_img;
                                        } else if (courtId.contains("3")) {
                                            title = "Pickleball • Court 3";
                                            imgRes = R.drawable.pickleball_icon_img;
                                        } else {
                                            title = "Pickleball • Court 1";
                                            imgRes = R.drawable.pickleball_icon_img;
                                        }
                                        if (totalAmt <= 0) totalAmt = 400.0;
                                    } else if (isExclusive) {
                                        title = "Whole-Resort Private Buyout";
                                        imgRes = R.drawable.resort_icon_img;
                                        if (totalAmt <= 0) totalAmt = 12000.0;
                                    } else {
                                        if (roomId.contains("102")) {
                                            title = "Standard Villa Suite 102";
                                            if (totalAmt < 1000.0) totalAmt = 2500.0;
                                        } else if (roomId.contains("103")) {
                                            title = "Deluxe Family Suite 103";
                                            if (totalAmt < 1000.0) totalAmt = 3500.0;
                                        } else {
                                            title = "Standard Villa Suite 101";
                                            if (totalAmt < 1000.0) totalAmt = 2500.0;
                                        }
                                        imgRes = R.drawable.rooms_icon_img;
                                    }

                                    // Match Catalog item image so Bookings card uses the exact same background picture as Catalog
                                    String catalogImgUrl = null;
                                    CatalogItem matchedCatalog = findMatchingCatalogItem(title, roomId, courtId, isExclusive);
                                    if (matchedCatalog != null) {
                                        if (matchedCatalog.getImageUrl() != null && !matchedCatalog.getImageUrl().trim().isEmpty()) {
                                            catalogImgUrl = matchedCatalog.getImageUrl();
                                        }
                                        if (matchedCatalog.getImageResId() != 0) {
                                            imgRes = matchedCatalog.getImageResId();
                                        }
                                    }

                                    String startTime = obj.optString("start_time", "");
                                    String endTime = obj.optString("end_time", "");

                                    String d1;
                                    if (!startTime.isEmpty() && !"null".equalsIgnoreCase(startTime)) {
                                        d1 = "SLOT: " + formatSlotDisplay(startTime, endTime);
                                    } else if (!checkInDate.isEmpty() && !"null".equalsIgnoreCase(checkInDate)) {
                                        d1 = "IN: " + checkInDate + " • OUT: " + checkOutDate;
                                    } else {
                                        d1 = "DATE: " + bDate;
                                    }

                                    String createdAt = obj.optString("created_at", "");
                                    String holdExpiresAt = obj.optString("hold_expires_at", "");

                                    String d2 = "Pending".equalsIgnoreCase(bStatus)
                                            ? "Hold active (15-min window)"
                                            : "Status: " + bStatus;

                                    String formattedPrice = String.format(java.util.Locale.US, "₱%,.2f", totalAmt);

                                    com.example.casavista.models.Booking b = new com.example.casavista.models.Booking(
                                            title, bStatus, bDate, d1, d2, formattedPrice, catalogImgUrl, imgRes
                                    );
                                    b.setBookingId(bId);
                                    b.setCustomerId(bCustId);
                                    b.setRoomId(roomId);
                                    b.setCourtId(courtId);
                                    b.setCheckInDate(checkInDate);
                                    b.setCheckOutDate(checkOutDate);
                                    b.setStatus(bStatus);
                                    b.setTotalAmount(totalAmt);
                                    b.setCreatedAt(createdAt);
                                    b.setHoldExpiresAt(holdExpiresAt);

                                    if (b.isHoldExpired()) {
                                        b.setStatus("Cancelled");
                                        b.setDetail2("Reservation hold expired (15-min window ended)");
                                    }

                                    // ResortRepository.addBooking handles deduplication & in-place updates
                                    repository.addBooking(requireContext(), b);
                                }
                            }

                            repository.saveBookings(requireContext());
                            BookingAdapter liveAdapter = new BookingAdapter(repository.getBookings(currentTab));
                            rvBookings.setAdapter(liveAdapter);

                        } catch (Exception e) {
                            BookingAdapter adapter = new BookingAdapter(repository.getBookings(currentTab));
                            rvBookings.setAdapter(adapter);
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        isFetchingBookings = false;
                        if (getContext() == null || rvBookings == null) return;
                        BookingAdapter adapter = new BookingAdapter(repository.getBookings(currentTab));
                        rvBookings.setAdapter(adapter);
                    }
                }
        );
    }

    private String formatSlotDisplay(String startTime, String endTime) {
        try {
            int startHr = Integer.parseInt(startTime.split(":")[0]);
            int endHr = endTime != null && endTime.contains(":") ? Integer.parseInt(endTime.split(":")[0]) : (startHr + 1);
            String amPm = endHr >= 12 ? "PM" : "AM";
            int dispStart = startHr > 12 ? startHr - 12 : (startHr == 0 ? 12 : startHr);
            int dispEnd = endHr > 12 ? endHr - 12 : (endHr == 0 ? 12 : endHr);
            return dispStart + "-" + dispEnd + " " + amPm;
        } catch (Exception e) {
            return startTime + " - " + endTime;
        }
    }

    private void highlightTab(MaterialButton selected, MaterialButton un1, MaterialButton un2) {
        selected.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4D391A")));
        selected.setTextColor(Color.WHITE);

        un1.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        un1.setTextColor(Color.parseColor("#6B5B4B"));

        un2.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        un2.setTextColor(Color.parseColor("#6B5B4B"));
    }

    private void animateSelection(View view) {
        if (view == null) return;
        view.setScaleX(0.95f);
        view.setScaleY(0.95f);
        view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
    }
}