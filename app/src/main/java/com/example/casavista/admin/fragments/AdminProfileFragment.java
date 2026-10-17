package com.example.casavista.admin.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.casavista.R;
import com.example.casavista.auth.LoginActivity;
import com.example.casavista.data.ResortRepository;
import com.example.casavista.helpers.AvatarHelper;
import com.example.casavista.network.SupabaseClient;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;

import org.json.JSONArray;
import org.json.JSONObject;

public class AdminProfileFragment extends Fragment {

    private ImageView imgAdminProfile;
    private TextView tvAdminInitials, tvAdminName, tvAdminRoleBadge, tvAdminEmail, tvAdminEmployeeId;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null && getContext() != null) {
                            String fullName = tvAdminName != null ? tvAdminName.getText().toString() : "";
                            AvatarHelper.saveProfileImage(getContext(), selectedImageUri);
                            AvatarHelper.loadAvatar(getContext(), imgAdminProfile, tvAdminInitials, fullName);
                        }
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        imgAdminProfile = view.findViewById(R.id.imgAdminProfile);
        tvAdminInitials = view.findViewById(R.id.tvAdminInitials);
        tvAdminName = view.findViewById(R.id.tvAdminName);
        tvAdminRoleBadge = view.findViewById(R.id.tvAdminRoleBadge);
        tvAdminEmail = view.findViewById(R.id.tvAdminEmail);
        tvAdminEmployeeId = view.findViewById(R.id.tvAdminEmployeeId);

        View btnChangeProfileImage = view.findViewById(R.id.btnChangeProfileImage);
        SwitchMaterial switchDarkMode = view.findViewById(R.id.switchDarkMode);

        populateStaffDetails();
        syncStaffRecordFromSupabase();

        if (btnChangeProfileImage != null) {
            btnChangeProfileImage.setOnClickListener(v -> {
                animateSelection(v);
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                imagePickerLauncher.launch(intent);
            });
        }

        if (switchDarkMode != null) {
            switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            });
        }

        View btnLogout = view.findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                animateSelection(v);
                showAdminLogoutConfirmation();
            });
        }
    }

    private void populateStaffDetails() {
        if (getContext() == null) return;
        SharedPreferences adminPrefs = requireContext().getSharedPreferences("AdminPrefs", Context.MODE_PRIVATE);

        String staffName = adminPrefs.getString("staff_name", ResortRepository.getInstance().getCurrentUserName());
        String staffEmail = adminPrefs.getString("staff_email", ResortRepository.getInstance().getCurrentUserEmail());
        String staffRole = adminPrefs.getString("staff_role", "Admin");
        String staffId = adminPrefs.getString("staff_id", "STF-001");

        if (staffName == null || staffName.isEmpty() || "Guest".equalsIgnoreCase(staffName)) {
            staffName = "Juan Dela Cruz";
        }
        if (staffEmail == null || !staffEmail.contains("@")) {
            staffEmail = "admin@casavista.com";
        }

        if (tvAdminName != null) tvAdminName.setText(staffName);
        if (tvAdminEmail != null) tvAdminEmail.setText(staffEmail);
        if (tvAdminEmployeeId != null) tvAdminEmployeeId.setText(staffId);
        if (tvAdminRoleBadge != null) {
            tvAdminRoleBadge.setText(formatRoleTitle(staffRole));
        }

        AvatarHelper.loadAvatar(requireContext(), imgAdminProfile, tvAdminInitials, staffName);
    }

    private void syncStaffRecordFromSupabase() {
        if (getContext() == null) return;
        SharedPreferences adminPrefs = requireContext().getSharedPreferences("AdminPrefs", Context.MODE_PRIVATE);
        final String targetStaffId = adminPrefs.getString("staff_id", "STF-001");
        final String targetRole = adminPrefs.getString("staff_role", "Admin");

        SupabaseClient.getInstance().fetchStaffList(new SupabaseClient.DataCallback() {
            @Override
            public void onSuccess(String jsonArrayString) {
                if (!isAdded() || getContext() == null) return;
                try {
                    JSONArray arr = new JSONArray(jsonArrayString);
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject s = arr.getJSONObject(i);
                        String sId = s.optString("staff_id", "");
                        String sRole = s.optString("role", "");
                        if (sId.equalsIgnoreCase(targetStaffId) || sRole.equalsIgnoreCase(targetRole)) {
                            String full = (s.optString("first_name", "") + " " + s.optString("last_name", "")).trim();
                            if (!full.isEmpty()) {
                                adminPrefs.edit()
                                        .putString("staff_id", sId)
                                        .putString("staff_name", full)
                                        .putString("staff_role", sRole)
                                        .apply();
                                if (tvAdminName != null) tvAdminName.setText(full);
                                if (tvAdminEmployeeId != null) tvAdminEmployeeId.setText(sId);
                                if (tvAdminRoleBadge != null) tvAdminRoleBadge.setText(formatRoleTitle(sRole));
                                AvatarHelper.loadAvatar(requireContext(), imgAdminProfile, tvAdminInitials, full);
                            }
                            break;
                        }
                    }
                } catch (Exception ignored) {}
            }

            @Override
            public void onError(String errorMessage) {}
        });
    }

    private String formatRoleTitle(String role) {
        if (role == null) return "Head Administrator (Full Access)";
        switch (role) {
            case "Manager":
                return "Resort Manager (Financial & Ops)";
            case "Front Desk":
                return "Front Desk Officer (Bookings & Rooms)";
            case "Cashier":
                return "Billing Cashier (POS & Payments)";
            default:
                return "Head Administrator (Full Access)";
        }
    }

    private void showAdminLogoutConfirmation() {
        if (getContext() == null) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Exit Admin Panel")
                .setMessage("Are you sure you want to log out of your staff session?")
                .setPositiveButton("Log Out", (dialog, which) -> performAdminLogout())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void performAdminLogout() {
        if (getContext() == null) return;
        SharedPreferences prefs = requireContext().getSharedPreferences("AdminPrefs", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();

        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) getActivity().finish();
    }

    private void animateSelection(View view) {
        if (view == null) return;
        view.setScaleX(0.95f);
        view.setScaleY(0.95f);
        view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        populateStaffDetails();
    }
}