package com.example.casavista.superadmin.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.casavista.R;
import com.example.casavista.adapters.HistoryAdapter;
import com.example.casavista.models.HistoryLog;

import java.util.ArrayList;
import java.util.List;

public class SuperAdminHistoryFragment extends Fragment {

    private RecyclerView rvHistoryList;
    private HistoryAdapter adapter;
    private EditText etSearchHistory;
    private ImageView imgTopProfile;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_super_admin_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        imgTopProfile = view.findViewById(R.id.imgTopProfile);
        rvHistoryList = view.findViewById(R.id.rvHistoryList);
        etSearchHistory = view.findViewById(R.id.etSearchHistory);

        View cardAvatar = view.findViewById(R.id.cardSuperAdminHistoryAvatar);
        if (cardAvatar != null) {
            cardAvatar.setOnClickListener(v -> {
                com.example.casavista.superadmin.SuperAdminMainActivity superActivity = (com.example.casavista.superadmin.SuperAdminMainActivity) getActivity();
                if (superActivity != null) {
                    superActivity.loadFragment(new SuperAdminProfileFragment());
                    com.google.android.material.bottomnavigation.BottomNavigationView navView = superActivity.findViewById(R.id.superAdminNavView);
                    if (navView != null) {
                        navView.setSelectedItemId(R.id.nav_super_profile);
                    }
                }
            });
        }

        setupRecyclerView();
        setupSearchFilter();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfileImage();
    }

    private void loadProfileImage() {
        if (imgTopProfile == null || getContext() == null) return;

        java.io.File avatarFile = new java.io.File(requireContext().getFilesDir(), "super_admin_avatar.jpg");
        if (avatarFile.exists()) {
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(avatarFile.getAbsolutePath());
            if (bitmap != null) {
                imgTopProfile.setImageBitmap(bitmap);
                return;
            }
        }

        com.example.casavista.helpers.AvatarHelper.loadAvatar(getContext(), imgTopProfile, null, "Juan Dela Cruz");
    }

    private void setupRecyclerView() {
        List<HistoryLog> logs = new ArrayList<>();

        logs.add(new HistoryLog(
                "TXN101",
                HistoryLog.Type.PAYMENT,
                "<b>Mika Reyes</b> paid Room 2 Day Pass — cashless via GCash",
                "",
                "+₱12,000",
                "Processed by Jopet T. (Front Desk)",
                "10:42 AM"
        ));

        logs.add(new HistoryLog(
                "TXN102",
                HistoryLog.Type.BOOKING,
                "<b>Anna Cortes</b> booked Pickleball • Court 3",
                "Self-booked, guest app",
                "+₱300",
                "",
                "9:15 AM"
        ));

        logs.add(new HistoryLog(
                "TXN103",
                HistoryLog.Type.REFUND,
                "<b>Refund issued</b> to Carlo Sison — court double-booked",
                "",
                "-₱350",
                "Approved by Dr. Lemyel Yap",
                "8:50 AM"
        ));

        logs.add(new HistoryLog(
                "TXN104",
                HistoryLog.Type.STAFF_ACTIVITY,
                "<b>Jopet Tan</b> logged in — front desk shift started",
                "Staff activity",
                "",
                "",
                "8:02 AM"
        ));

        adapter = new HistoryAdapter(logs);
        rvHistoryList.setLayoutManager(new LinearLayoutManager(getContext()));
        rvHistoryList.setAdapter(adapter);
    }

    private void setupSearchFilter() {
        if (etSearchHistory != null) {
            etSearchHistory.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (adapter != null && rvHistoryList != null) {
                        rvHistoryList.setAlpha(0.4f);
                        adapter.filter(s.toString());
                        rvHistoryList.animate().alpha(1.0f).setDuration(150).start();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }
}