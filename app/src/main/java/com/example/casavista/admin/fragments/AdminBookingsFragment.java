package com.example.casavista.admin.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.casavista.R;
import com.example.casavista.adapters.AdminBookingAdapter;
import com.example.casavista.data.ResortRepository;
import com.example.casavista.helpers.AvatarHelper;
import com.example.casavista.models.Booking;

import java.util.ArrayList;
import java.util.List;

public class AdminBookingsFragment extends Fragment implements AdminBookingAdapter.OnBookingActionListener {

    private ImageView imgHeaderAvatar;
    private TextView tvHeaderInitials;
    private RecyclerView rvAdminBookings;
    private EditText etSearchBookings;
    private TextView filterAll, filterRooms, filterPickleball, filterBasketball;

    private ResortRepository repository;
    private AdminBookingAdapter adapter;
    private String currentCategoryFilter = "All";

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
                com.example.casavista.admin.AdminMainActivity adminActivity = (com.example.casavista.admin.AdminMainActivity) getActivity();
                if (adminActivity != null) {
                    adminActivity.loadFragment(new AdminProfileFragment());
                    com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = adminActivity.findViewById(R.id.adminBottomNavigationView);
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

        if (etSearchBookings != null) {
            etSearchBookings.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    loadAdminBookings();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        loadAdminBookings();
    }

    private void setupCategoryFilters() {
        if (filterAll != null) {
            filterAll.setOnClickListener(v -> {
                currentCategoryFilter = "All";
                highlightFilter(filterAll, filterRooms, filterPickleball, filterBasketball);
                loadAdminBookings();
            });
        }

        if (filterRooms != null) {
            filterRooms.setOnClickListener(v -> {
                currentCategoryFilter = "Rooms";
                highlightFilter(filterRooms, filterAll, filterPickleball, filterBasketball);
                loadAdminBookings();
            });
        }

        if (filterPickleball != null) {
            filterPickleball.setOnClickListener(v -> {
                currentCategoryFilter = "Pickleball";
                highlightFilter(filterPickleball, filterAll, filterRooms, filterBasketball);
                loadAdminBookings();
            });
        }

        if (filterBasketball != null) {
            filterBasketball.setOnClickListener(v -> {
                currentCategoryFilter = "Basketball";
                highlightFilter(filterBasketball, filterAll, filterRooms, filterPickleball);
                loadAdminBookings();
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

    private void loadAdminBookings() {
        if (rvAdminBookings == null || repository == null || getContext() == null) return;

        repository.loadSavedBookings(requireContext());
        List<Booking> allBookings = repository.getBookings("ALL");

        String query = etSearchBookings != null ? etSearchBookings.getText().toString().trim().toLowerCase() : "";

        List<Booking> filteredList = new ArrayList<>();
        for (Booking b : allBookings) {
            String title = b.getTitle() != null ? b.getTitle().toLowerCase() : "";
            String d1 = b.getDetail1() != null ? b.getDetail1().toLowerCase() : "";
            String d2 = b.getDetail2() != null ? b.getDetail2().toLowerCase() : "";

            boolean matchesCategory = "All".equalsIgnoreCase(currentCategoryFilter) ||
                    ("Rooms".equalsIgnoreCase(currentCategoryFilter) && (title.contains("room") || title.contains("villa") || title.contains("loft"))) ||
                    ("Pickleball".equalsIgnoreCase(currentCategoryFilter) && title.contains("pickleball")) ||
                    ("Basketball".equalsIgnoreCase(currentCategoryFilter) && title.contains("basketball"));

            boolean matchesQuery = query.isEmpty() || title.contains(query) || d1.contains(query) || d2.contains(query);

            if (matchesCategory && matchesQuery) {
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
        loadAdminBookings();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getContext() != null) {
            AvatarHelper.loadAvatar(getContext(), imgHeaderAvatar, tvHeaderInitials, "Juan Dela Cruz");
            loadAdminBookings();
        }
    }
}