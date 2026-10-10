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
import com.example.casavista.user.MainActivity;
import com.google.android.material.button.MaterialButton;

import java.io.File;

public class BookingsFragment extends Fragment {

    private ResortRepository repository;
    private RecyclerView rvBookings;
    private MaterialButton tabUpcoming, tabPast, tabCancelled;
    private String currentTab = "Upcoming";

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

        loadBookings("Upcoming");
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

        // Auto-refresh the current tab list when returning to this screen
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

        File file = new File(requireContext().getFilesDir(), "profile_pic.jpg");
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
        if (repository != null && rvBookings != null && getContext() != null) {
            // Load persistent local bookings first
            repository.loadSavedBookings(requireContext());
            BookingAdapter localAdapter = new BookingAdapter(repository.getBookings(type));
            rvBookings.setAdapter(localAdapter);

            // Fetch live database bookings from FastAPI / Supabase
            com.example.casavista.network.SupabaseClient.getInstance().fetchBookings(
                    new com.example.casavista.network.SupabaseClient.DataCallback() {
                        @Override
                        public void onSuccess(String jsonArrayString) {
                            if (getContext() == null || jsonArrayString == null) return;
                            try {
                                org.json.JSONArray array = new org.json.JSONArray(jsonArrayString);
                                SharedPreferences prefs = requireContext().getSharedPreferences("CasavistaProfilePrefs", Context.MODE_PRIVATE);
                                String currentCustId = prefs.getString("customer_id", "CUST-101");

                                for (int i = 0; i < array.length(); i++) {
                                    org.json.JSONObject obj = array.getJSONObject(i);
                                    String bCustId = obj.optString("customer_id", "");

                                    // Filter by current logged in customer or match active session
                                    if (bCustId.isEmpty() || bCustId.equalsIgnoreCase(currentCustId) || bCustId.equalsIgnoreCase("CUST-101")) {
                                        String bId = obj.optString("booking_id", "BK-" + i);
                                        String bStatus = obj.optString("status", "Pending");
                                        String bDate = obj.optString("booking_date", obj.optString("check_in_date", "2026-08-20"));
                                        String courtId = obj.optString("court_id", "");
                                        String roomId = obj.optString("room_id", "");

                                        String title = !courtId.isEmpty() && !"null".equalsIgnoreCase(courtId)
                                                ? "Court Reservation • " + courtId
                                                : "Resort Accommodation • " + roomId;

                                        String startTime = obj.optString("start_time", "");
                                        String endTime = obj.optString("end_time", "");
                                        String d1 = (!startTime.isEmpty() && !"null".equalsIgnoreCase(startTime))
                                                ? "SLOT: " + startTime + " - " + endTime
                                                : "CHECK-IN: " + obj.optString("check_in_date", "2026-08-20");

                                        String d2 = "Pending".equalsIgnoreCase(bStatus)
                                                ? "Hold active (15-min window)"
                                                : "Status: " + bStatus;

                                        int imgRes = !courtId.isEmpty() && !"null".equalsIgnoreCase(courtId)
                                                ? R.drawable.pickleball_icon_img
                                                : R.drawable.rooms_icon_img;

                                        com.example.casavista.models.Booking b = new com.example.casavista.models.Booking(
                                                title, bStatus, bDate, d1, d2, "₱400", null, imgRes
                                        );
                                        b.setBookingId(bId);

                                        // Merge into local repository
                                        boolean exists = false;
                                        for (com.example.casavista.models.Booking existing : repository.getBookings()) {
                                            if (bId.equalsIgnoreCase(existing.getBookingId())) {
                                                existing.setStatus(bStatus);
                                                exists = true;
                                                break;
                                            }
                                        }
                                        if (!exists) {
                                            repository.addBooking(requireContext(), b);
                                        }
                                    }
                                }

                                BookingAdapter liveAdapter = new BookingAdapter(repository.getBookings(type));
                                rvBookings.setAlpha(0f);
                                rvBookings.setAdapter(liveAdapter);
                                rvBookings.animate().alpha(1f).setDuration(220).start();

                            } catch (Exception e) {
                                // Fallback to local
                                BookingAdapter adapter = new BookingAdapter(repository.getBookings(type));
                                rvBookings.setAdapter(adapter);
                            }
                        }

                        @Override
                        public void onError(String errorMessage) {
                            // Fallback to local repository
                            BookingAdapter adapter = new BookingAdapter(repository.getBookings(type));
                            rvBookings.setAdapter(adapter);
                        }
                    }
            );
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