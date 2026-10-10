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

import com.example.casavista.helpers.AvatarHelper;
import com.example.casavista.auth.LoginActivity;
import com.example.casavista.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class AdminProfileFragment extends Fragment {

    private ImageView imgAdminProfile;
    private TextView tvAdminInitials, tvAdminName;
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
        View btnChangeProfileImage = view.findViewById(R.id.btnChangeProfileImage);
        SwitchMaterial switchDarkMode = view.findViewById(R.id.switchDarkMode);

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
                if (isChecked) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }
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

    private void showAdminLogoutConfirmation() {
        if (getContext() == null) return;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Exit Admin Panel")
                .setMessage("Are you sure you want to log out of the Admin panel?")
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

        if (getActivity() != null) {
            getActivity().finish();
        }
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
        if (getContext() != null && tvAdminName != null) {
            AvatarHelper.loadAvatar(getContext(), imgAdminProfile, tvAdminInitials, tvAdminName.getText().toString());
        }
    }
}