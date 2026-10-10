package com.example.casavista.admin.fragments;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.casavista.R;
import com.example.casavista.data.ResortRepository;
import com.example.casavista.helpers.AdminAnnouncementHelper;
import com.example.casavista.helpers.AvatarHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminDashboardFragment extends Fragment {

    private ImageView imgHeaderAvatar;
    private TextView tvHeaderInitials;

    private MaterialButton btnFilterToday, btnFilterWeek, btnFilterMonth, btnFilterYear;
    private MaterialButton btnAdminCreatePost;

    private TextView tvRevenueTitle, tvRevenueValue, tvRevenueSubtext;
    private TextView tvPendingPaymentsTitle, tvPendingPaymentsCount, tvPendingPaymentsAmount;
    private TextView tvRoomsBookedTitle, tvRoomsBookedCount, tvRoomsBookedSubtext;
    private TextView tvCourtHoursTitle, tvCourtHoursValue, tvCourtHoursSubtext;

    // Announcement Image Picker
    private Uri selectedImageUri = null;
    private ImageView dialogPreviewImage;
    private LinearLayout dialogUploadPrompt;
    private ImageButton dialogBtnRemoveImage;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    updateImagePreviewState();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        imgHeaderAvatar = view.findViewById(R.id.imgHeaderAvatar);
        tvHeaderInitials = view.findViewById(R.id.tvHeaderInitials);

        View cardAvatar = view.findViewById(R.id.cardAdminAvatar);
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

        btnFilterToday = view.findViewById(R.id.btnFilterToday);
        btnFilterWeek = view.findViewById(R.id.btnFilterWeek);
        btnFilterMonth = view.findViewById(R.id.btnFilterMonth);
        btnFilterYear = view.findViewById(R.id.btnFilterYear);

        btnAdminCreatePost = view.findViewById(R.id.btnAdminCreatePost);

        tvRevenueTitle = view.findViewById(R.id.tvRevenueTitle);
        tvRevenueValue = view.findViewById(R.id.tvRevenueValue);
        tvRevenueSubtext = view.findViewById(R.id.tvRevenueSubtext);

        tvPendingPaymentsTitle = view.findViewById(R.id.tvPendingPaymentsTitle);
        tvPendingPaymentsCount = view.findViewById(R.id.tvPendingPaymentsCount);
        tvPendingPaymentsAmount = view.findViewById(R.id.tvPendingPaymentsAmount);

        tvRoomsBookedTitle = view.findViewById(R.id.tvRoomsBookedTitle);
        tvRoomsBookedCount = view.findViewById(R.id.tvRoomsBookedCount);
        tvRoomsBookedSubtext = view.findViewById(R.id.tvRoomsBookedSubtext);

        tvCourtHoursTitle = view.findViewById(R.id.tvCourtHoursTitle);
        tvCourtHoursValue = view.findViewById(R.id.tvCourtHoursValue);
        tvCourtHoursSubtext = view.findViewById(R.id.tvCourtHoursSubtext);

        setupFilterListeners();
        setupFacilityClickListeners(view);

        if (btnAdminCreatePost != null) {
            btnAdminCreatePost.setOnClickListener(v -> {
                animateSelection(v);
                showCreatePostDialog();
            });
        }
    }

    private void showCreatePostDialog() {
        selectedImageUri = null;

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_post, null);
        EditText etTitle = dialogView.findViewById(R.id.etPostTitle);
        EditText etContent = dialogView.findViewById(R.id.etPostContent);

        View cardImagePicker = dialogView.findViewById(R.id.cardImagePicker);
        dialogPreviewImage = dialogView.findViewById(R.id.imgPostPreview);
        dialogUploadPrompt = dialogView.findViewById(R.id.layoutUploadPrompt);
        dialogBtnRemoveImage = dialogView.findViewById(R.id.btnRemoveImage);

        cardImagePicker.setOnClickListener(v -> {
            animateSelection(v);
            imagePickerLauncher.launch("image/*");
        });

        dialogBtnRemoveImage.setOnClickListener(v -> {
            animateSelection(v);
            selectedImageUri = null;
            updateImagePreviewState();
        });

        new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Publish", (dialog, which) -> {
                    String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
                    String content = etContent.getText() != null ? etContent.getText().toString().trim() : "";

                    if (!title.isEmpty() && !content.isEmpty()) {
                        String currentDate = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date());

                        String localImagePath = null;
                        if (selectedImageUri != null) {
                            localImagePath = saveImageToInternalStorage(selectedImageUri);
                        }

                        AdminAnnouncementHelper.addAnnouncement(
                                requireContext(),
                                title,
                                content,
                                currentDate,
                                localImagePath,
                                "Admin Staff"
                        );

                        Toast.makeText(getContext(), "Announcement published successfully!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getContext(), "Please fill in post title and content", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateImagePreviewState() {
        if (dialogPreviewImage == null || dialogUploadPrompt == null || dialogBtnRemoveImage == null) return;

        if (selectedImageUri != null) {
            dialogPreviewImage.setImageURI(selectedImageUri);
            dialogPreviewImage.setVisibility(View.VISIBLE);
            dialogBtnRemoveImage.setVisibility(View.VISIBLE);
            dialogUploadPrompt.setVisibility(View.GONE);
        } else {
            dialogPreviewImage.setImageURI(null);
            dialogPreviewImage.setVisibility(View.GONE);
            dialogBtnRemoveImage.setVisibility(View.GONE);
            dialogUploadPrompt.setVisibility(View.VISIBLE);
        }
    }

    private void setupFacilityClickListeners(View view) {
        if (view == null) return;

        View tvManage = view.findViewById(R.id.tvManageFacilities);
        if (tvManage != null) {
            tvManage.setOnClickListener(v -> showManageFacilitiesDialog("Room 1"));
        }

        View room1 = view.findViewById(R.id.room1Card);
        if (room1 != null) room1.setOnClickListener(v -> showManageFacilitiesDialog("Room 1"));

        View room2 = view.findViewById(R.id.room2Card);
        if (room2 != null) room2.setOnClickListener(v -> showManageFacilitiesDialog("Room 2"));

        View room3 = view.findViewById(R.id.room3Card);
        if (room3 != null) room3.setOnClickListener(v -> showManageFacilitiesDialog("Room 3"));

        View court1 = view.findViewById(R.id.court1Card);
        if (court1 != null) court1.setOnClickListener(v -> showManageFacilitiesDialog("Court 1"));

        View court2 = view.findViewById(R.id.court2Card);
        if (court2 != null) court2.setOnClickListener(v -> showManageFacilitiesDialog("Court 2"));

        View court3 = view.findViewById(R.id.court3Card);
        if (court3 != null) court3.setOnClickListener(v -> showManageFacilitiesDialog("Court 3"));
    }

    private void showManageFacilitiesDialog(String defaultFacility) {
        if (getContext() == null) return;

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_manage_facilities, null);
        Spinner spinnerFacility = dialogView.findViewById(R.id.spinnerFacilityList);
        RadioButton radioAvailable = dialogView.findViewById(R.id.radioStatusAvailable);
        RadioButton radioMaintenance = dialogView.findViewById(R.id.radioStatusMaintenance);
        RadioButton radioCleaning = dialogView.findViewById(R.id.radioStatusCleaning);
        RadioButton radioUnavailable = dialogView.findViewById(R.id.radioStatusUnavailable);

        List<String> facilities = Arrays.asList("Room 1", "Room 2", "Room 3", "Court 1", "Court 2", "Court 3");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, facilities);
        if (spinnerFacility != null) {
            spinnerFacility.setAdapter(adapter);
            if (defaultFacility != null && facilities.contains(defaultFacility)) {
                spinnerFacility.setSelection(facilities.indexOf(defaultFacility));
            }
        }

        String facilityKey = defaultFacility != null ? defaultFacility : "Room 1";
        boolean hasGuestBooking = ResortRepository.getInstance().hasActiveGuestBooking(facilityKey);
        String currentStatus = ResortRepository.getInstance().getFacilityStatusOverride(requireContext(), facilityKey);

        if (hasGuestBooking) {
            if (radioUnavailable != null) {
                radioUnavailable.setChecked(true);
                radioUnavailable.setText("Booked (Guest Reserved)");
            }
            if (radioAvailable != null) radioAvailable.setEnabled(false);
        } else if ("Maintenance".equalsIgnoreCase(currentStatus)) {
            if (radioMaintenance != null) radioMaintenance.setChecked(true);
        } else if ("Cleaning".equalsIgnoreCase(currentStatus)) {
            if (radioCleaning != null) radioCleaning.setChecked(true);
        } else if ("Unavailable".equalsIgnoreCase(currentStatus)) {
            if (radioUnavailable != null) radioUnavailable.setChecked(true);
        } else {
            if (radioAvailable != null) radioAvailable.setChecked(true);
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Manage Room Availability")
                .setView(dialogView)
                .setPositiveButton("Save Status", (dialog, which) -> {
                    String facility = spinnerFacility != null ? spinnerFacility.getSelectedItem().toString() : "Room 1";

                    String selectedReason = "Available";
                    if (radioMaintenance != null && radioMaintenance.isChecked()) {
                        selectedReason = "Maintenance";
                    } else if (radioCleaning != null && radioCleaning.isChecked()) {
                        selectedReason = "Cleaning";
                    } else if (radioUnavailable != null && radioUnavailable.isChecked()) {
                        selectedReason = "Unavailable";
                    }

                    final String finalReason = selectedReason;

                    if (ResortRepository.getInstance().hasActiveGuestBooking(facility) && "Available".equalsIgnoreCase(finalReason)) {
                        Toast.makeText(requireContext(), facility + " is currently reserved by a guest and cannot be set to Available.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Confirm Status Change")
                            .setMessage("Are you sure you want to set " + facility + " to " + finalReason + "?")
                            .setPositiveButton("Yes, Set " + finalReason, (confirmDialog, confirmWhich) -> {
                                ResortRepository.getInstance().setFacilityStatusOverride(requireContext(), facility, finalReason);
                                Toast.makeText(requireContext(), "Updated " + facility + " status to " + finalReason, Toast.LENGTH_SHORT).show();
                                updateFacilityStatus();
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private String saveImageToInternalStorage(Uri imageUri) {
        if (getContext() == null || imageUri == null) return null;
        try {
            InputStream inputStream = getContext().getContentResolver().openInputStream(imageUri);
            File file = new File(getContext().getFilesDir(), "announcement_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream outputStream = new FileOutputStream(file);

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            if (inputStream != null) inputStream.close();

            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void setupFilterListeners() {
        if (btnFilterToday != null) {
            btnFilterToday.setOnClickListener(v -> {
                animateSelection(v);
                selectFilter(btnFilterToday, "TODAY");
            });
        }
        if (btnFilterWeek != null) {
            btnFilterWeek.setOnClickListener(v -> {
                animateSelection(v);
                selectFilter(btnFilterWeek, "WEEK");
            });
        }
        if (btnFilterMonth != null) {
            btnFilterMonth.setOnClickListener(v -> {
                animateSelection(v);
                selectFilter(btnFilterMonth, "MONTH");
            });
        }
        if (btnFilterYear != null) {
            btnFilterYear.setOnClickListener(v -> {
                animateSelection(v);
                selectFilter(btnFilterYear, "YEAR");
            });
        }
    }

    private void selectFilter(MaterialButton selectedButton, String filterType) {
        if (getContext() == null) return;

        List<MaterialButton> buttons = Arrays.asList(btnFilterToday, btnFilterWeek, btnFilterMonth, btnFilterYear);

        int activeBgColor = ContextCompat.getColor(getContext(), R.color.accent_gold);
        int activeTextColor = ContextCompat.getColor(getContext(), R.color.bg_main);
        int inactiveBgColor = ContextCompat.getColor(getContext(), R.color.bg_card);
        int inactiveTextColor = ContextCompat.getColor(getContext(), R.color.text_primary);

        for (MaterialButton btn : buttons) {
            if (btn != null) {
                if (btn == selectedButton) {
                    btn.setBackgroundTintList(ColorStateList.valueOf(activeBgColor));
                    btn.setTextColor(activeTextColor);
                } else {
                    btn.setBackgroundTintList(ColorStateList.valueOf(inactiveBgColor));
                    btn.setTextColor(inactiveTextColor);
                }
            }
        }

        updateDashboardMetrics(filterType);
    }

    private void updateDashboardMetrics(String filterType) {
        fadeAnimateView(tvRevenueValue);
        fadeAnimateView(tvPendingPaymentsCount);
        fadeAnimateView(tvRoomsBookedCount);
        fadeAnimateView(tvCourtHoursValue);

        switch (filterType) {
            case "TODAY":
                if (tvRevenueTitle != null) tvRevenueTitle.setText("Today's revenue");
                if (tvRevenueValue != null) tvRevenueValue.setText("₱24,450");
                if (tvRevenueSubtext != null) tvRevenueSubtext.setText("↑ 18%");

                if (tvPendingPaymentsCount != null) tvPendingPaymentsCount.setText("3");
                if (tvPendingPaymentsAmount != null) tvPendingPaymentsAmount.setText("₱4,750");

                if (tvRoomsBookedCount != null) tvRoomsBookedCount.setText("2 / 3");
                if (tvRoomsBookedSubtext != null) tvRoomsBookedSubtext.setText("1 open");

                if (tvCourtHoursValue != null) tvCourtHoursValue.setText("9.5");
                break;

            case "WEEK":
                if (tvRevenueTitle != null) tvRevenueTitle.setText("This week's revenue");
                if (tvRevenueValue != null) tvRevenueValue.setText("₱142,800");
                if (tvRevenueSubtext != null) tvRevenueSubtext.setText("↑ 12% vs last week");

                if (tvPendingPaymentsCount != null) tvPendingPaymentsCount.setText("8");
                if (tvPendingPaymentsAmount != null) tvPendingPaymentsAmount.setText("₱12,300");

                if (tvRoomsBookedCount != null) tvRoomsBookedCount.setText("18 / 21");
                if (tvRoomsBookedSubtext != null) tvRoomsBookedSubtext.setText("3 open");

                if (tvCourtHoursValue != null) tvCourtHoursValue.setText("62.0");
                break;

            case "MONTH":
                if (tvRevenueTitle != null) tvRevenueTitle.setText("This month's revenue");
                if (tvRevenueValue != null) tvRevenueValue.setText("₱580,200");
                if (tvRevenueSubtext != null) tvRevenueSubtext.setText("↑ 22% vs last month");

                if (tvPendingPaymentsCount != null) tvPendingPaymentsCount.setText("15");
                if (tvPendingPaymentsAmount != null) tvPendingPaymentsAmount.setText("₱28,400");

                if (tvRoomsBookedCount != null) tvRoomsBookedCount.setText("78 / 90");
                if (tvRoomsBookedSubtext != null) tvRoomsBookedSubtext.setText("12 open");

                if (tvCourtHoursValue != null) tvCourtHoursValue.setText("248.5");
                break;

            case "YEAR":
                if (tvRevenueTitle != null) tvRevenueTitle.setText("This year's revenue");
                if (tvRevenueValue != null) tvRevenueValue.setText("₱6,450,000");
                if (tvRevenueSubtext != null) tvRevenueSubtext.setText("↑ 35% YTD");

                if (tvPendingPaymentsCount != null) tvPendingPaymentsCount.setText("42");
                if (tvPendingPaymentsAmount != null) tvPendingPaymentsAmount.setText("₱85,000");

                if (tvRoomsBookedCount != null) tvRoomsBookedCount.setText("890 / 1095");
                if (tvRoomsBookedSubtext != null) tvRoomsBookedSubtext.setText("205 open");

                if (tvCourtHoursValue != null) tvCourtHoursValue.setText("2,850");
                break;
        }
    }

    private void fadeAnimateView(View view) {
        if (view != null) {
            view.setAlpha(0.3f);
            view.animate().alpha(1.0f).setDuration(200).start();
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
        if (getContext() != null) {
            AvatarHelper.loadAvatar(getContext(), imgHeaderAvatar, tvHeaderInitials, "Juan Dela Cruz");
            updateFacilityStatus();
        }
    }

    private void updateFacilityStatus() {
        if (getView() == null || getContext() == null) return;
        ResortRepository repo = ResortRepository.getInstance();
        repo.loadSavedBookings(requireContext());

        View view = getView();
        updateCardStatus(view.findViewById(R.id.room1Card), repo.isFacilityBooked(requireContext(), "Room 1"), "Room 1");
        updateCardStatus(view.findViewById(R.id.room2Card), repo.isFacilityBooked(requireContext(), "Room 2"), "Room 2");
        updateCardStatus(view.findViewById(R.id.room3Card), repo.isFacilityBooked(requireContext(), "Room 3"), "Room 3");

        updateCourtCardStatus(view.findViewById(R.id.court1Card), repo.isFacilityBooked(requireContext(), "Court 1"), "Court 1");
        updateCourtCardStatus(view.findViewById(R.id.court2Card), repo.isFacilityBooked(requireContext(), "Court 2"), "Court 2");
        updateCourtCardStatus(view.findViewById(R.id.court3Card), repo.isFacilityBooked(requireContext(), "Court 3"), "Court 3");

        int bookedRooms = repo.getConfirmedRoomsCount(requireContext());
        if (tvRoomsBookedCount != null) {
            tvRoomsBookedCount.setText(bookedRooms + " / 3");
        }
        if (tvRoomsBookedSubtext != null) {
            int open = 3 - bookedRooms;
            tvRoomsBookedSubtext.setText(open > 0 ? open + " open" : "Fully booked");
        }
    }

    private void updateCardStatus(View cardView, boolean isBooked, String name) {
        if (!(cardView instanceof MaterialCardView)) return;
        MaterialCardView card = (MaterialCardView) cardView;
        LinearLayout layout = (LinearLayout) card.getChildAt(0);
        if (layout != null && layout.getChildCount() >= 2) {
            TextView tvTitle = (TextView) layout.getChildAt(0);
            TextView tvBadge = (TextView) layout.getChildAt(1);

            tvTitle.setText(name);
            if (isBooked) {
                card.setCardBackgroundColor(Color.parseColor("#F8E3DB"));
                tvBadge.setText("Booked");
                tvBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#C85A32")));
                tvBadge.setTextColor(Color.WHITE);
            } else {
                card.setCardBackgroundColor(Color.parseColor("#E2EFE0"));
                tvBadge.setText("Open");
                tvBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4E8D4F")));
                tvBadge.setTextColor(Color.WHITE);
            }
        }
    }

    private void updateCourtCardStatus(View cardView, boolean isBooked, String name) {
        if (!(cardView instanceof MaterialCardView)) return;
        MaterialCardView card = (MaterialCardView) cardView;
        LinearLayout layout = (LinearLayout) card.getChildAt(0);
        if (layout != null && layout.getChildCount() >= 2) {
            TextView tvTitle = (TextView) layout.getChildAt(0);
            TextView tvBadge = (TextView) layout.getChildAt(1);

            tvTitle.setText(name);
            if (isBooked) {
                card.setCardBackgroundColor(Color.parseColor("#FAF2D3"));
                tvBadge.setText("Booked");
                tvBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D2991E")));
                tvBadge.setTextColor(Color.WHITE);
            } else {
                card.setCardBackgroundColor(Color.parseColor("#E2EFE0"));
                tvBadge.setText("Open");
                tvBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4E8D4F")));
                tvBadge.setTextColor(Color.WHITE);
            }
        }
    }
}