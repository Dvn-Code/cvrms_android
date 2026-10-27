package com.example.casavista.user.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.example.casavista.auth.LoginActivity;
import com.example.casavista.helpers.AvatarHelper;
import com.example.casavista.R;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class ProfileFragment extends Fragment {

    private static final String PREFS_NAME = "CasavistaProfilePrefs";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_FIRST_NAME = "user_first_name";
    private static final String KEY_LAST_NAME = "user_last_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_PHONE = "user_phone";
    private static final String KEY_USER_BIRTHDATE = "user_birthdate";
    private static final String KEY_USER_GENDER = "user_gender";
    private static final String KEY_USER_ADDRESS = "user_address";
    private static final String PROFILE_PIC_FILE_NAME = "profile_pic.jpg";

    private TextView tvProfileName, tvProfileEmail, tvAvatarInitials;
    private TextView tvDetailEmail, tvDetailPhone, tvDetailBirthdate, tvDetailGender, tvDetailAddress;
    private TextView tvTotalBookingsCount, tvNightsStayedCount;
    private ImageView imgProfilePicture;

    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null && getContext() != null) {
                        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                        String email = prefs.getString(KEY_USER_EMAIL, "guest@casavista.com");
                        AvatarHelper.saveProfileImage(requireContext(), email, uri, () -> displayProfileImage());
                        displayProfileImage();
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        tvAvatarInitials = view.findViewById(R.id.tvAvatarInitials);
        imgProfilePicture = view.findViewById(R.id.imgProfilePicture);

        tvDetailEmail = view.findViewById(R.id.tvDetailEmail);
        tvDetailPhone = view.findViewById(R.id.tvDetailPhone);
        tvDetailBirthdate = view.findViewById(R.id.tvDetailBirthdate);
        tvDetailGender = view.findViewById(R.id.tvDetailGender);
        tvDetailAddress = view.findViewById(R.id.tvDetailAddress);

        tvTotalBookingsCount = view.findViewById(R.id.tvTotalBookingsCount);
        tvNightsStayedCount = view.findViewById(R.id.tvNightsStayedCount);

        loadSavedProfile();

        // Editing triggers
        view.findViewById(R.id.avatarContainer).setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        view.findViewById(R.id.layoutNameEdit).setOnClickListener(v -> showEditNameDialog());
        view.findViewById(R.id.rowEditEmail).setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Email Address Locked")
                    .setMessage("Your email address is linked to your registered CVRMS account and cannot be modified.")
                    .setPositiveButton("OK", null)
                    .show();
        });
        view.findViewById(R.id.rowEditPhone).setOnClickListener(v -> showEditFieldDialog("Phone Number", KEY_USER_PHONE, InputType.TYPE_CLASS_PHONE));
        view.findViewById(R.id.rowEditBirthdate).setOnClickListener(v -> showBirthdatePicker());
        view.findViewById(R.id.rowEditGender).setOnClickListener(v -> showGenderPicker());
        view.findViewById(R.id.rowEditAddress).setOnClickListener(v -> showEditFieldDialog("Home Address", KEY_USER_ADDRESS, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE));

        // Dark mode switch setup
        View switchView = view.findViewById(R.id.switchDarkMode);
        if (switchView instanceof SwitchCompat) {
            SwitchCompat switchDarkMode = (SwitchCompat) switchView;
            int currentMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            switchDarkMode.setChecked(currentMode == Configuration.UI_MODE_NIGHT_YES);
            switchDarkMode.setOnCheckedChangeListener((btn, isChecked) ->
                    AppCompatDelegate.setDefaultNightMode(isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO)
            );
        }

        // Logout action with confirmation dialog
        View btnLogout = view.findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> showUserLogoutConfirmation());
        }
    }

    private void showUserLogoutConfirmation() {
        if (getContext() == null) return;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out of your account?")
                .setPositiveButton("Log Out", (dialog, which) -> performUserLogout())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void performUserLogout() {
        if (getContext() == null) return;

        // Clear local bookings repository memory
        com.example.casavista.data.ResortRepository.getInstance().clearBookings(requireContext());

        // Clear active session keys while preserving email-specific profile entries, profile images, and terms agreement
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("is_logged_in");
        editor.remove("user_id");
        editor.remove("customer_id");
        editor.remove("user_email");
        editor.remove("user_name");
        editor.remove("user_first_name");
        editor.remove("user_last_name");
        editor.remove("user_phone");
        editor.remove("user_address");
        editor.remove("user_birthdate");
        editor.remove("user_gender");
        editor.putBoolean("has_agreed_terms", true);
        editor.apply();

        // Redirect to LoginActivity and clear backstack
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        // Smooth transition to login screen
        if (getActivity() != null) {
            getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            getActivity().finish();
        }
    }

    private void syncProfileToBackend() {
        if (getContext() == null) return;
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String email = prefs.getString(KEY_USER_EMAIL, "guest@casavista.com");
        String emailCleanKey = email.toLowerCase().replaceAll("[^a-zA-Z0-9]", "_");

        String firstName = prefs.getString(KEY_FIRST_NAME, "Guest");
        String lastName = prefs.getString(KEY_LAST_NAME, "User");
        String phone = prefs.getString(KEY_USER_PHONE + "_" + emailCleanKey, prefs.getString(KEY_USER_PHONE, "Not Set"));
        String address = prefs.getString(KEY_USER_ADDRESS + "_" + emailCleanKey, prefs.getString(KEY_USER_ADDRESS, "Not Set"));
        String birthdate = prefs.getString(KEY_USER_BIRTHDATE + "_" + emailCleanKey, prefs.getString(KEY_USER_BIRTHDATE, "Not Set"));
        String gender = prefs.getString(KEY_USER_GENDER + "_" + emailCleanKey, prefs.getString(KEY_USER_GENDER, "Not Set"));

        com.example.casavista.helpers.ApiHelper.syncCustomerToDatabase(
                requireContext(),
                email,
                firstName,
                lastName,
                phone,
                address,
                birthdate,
                gender,
                new com.example.casavista.helpers.ApiHelper.ApiCallback() {
                    @Override
                    public void onSuccess(org.json.JSONObject response) {
                        String custId = response.optString("customer_id", "");
                        if (getContext() != null) {
                            android.widget.Toast.makeText(requireContext(), "Profile updated & synced to Supabase (" + custId + ")!", android.widget.Toast.LENGTH_SHORT).show();
                        }
                        loadSavedProfile();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (getContext() != null) {
                            android.widget.Toast.makeText(requireContext(), "Sync error: " + errorMessage, android.widget.Toast.LENGTH_LONG).show();
                        }
                    }
                }
        );
    }

    private void loadSavedProfile() {
        if (getContext() == null) return;
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        String email = prefs.getString(KEY_USER_EMAIL, "guest@casavista.com");
        String emailCleanKey = email.toLowerCase().replaceAll("[^a-zA-Z0-9]", "_");

        String name = prefs.getString(KEY_USER_NAME + "_" + emailCleanKey, prefs.getString(KEY_USER_NAME, "Guest User"));
        String phone = prefs.getString(KEY_USER_PHONE + "_" + emailCleanKey, prefs.getString(KEY_USER_PHONE, "Not Set"));
        String birthdate = prefs.getString(KEY_USER_BIRTHDATE + "_" + emailCleanKey, prefs.getString(KEY_USER_BIRTHDATE, "Not Set"));
        String gender = prefs.getString(KEY_USER_GENDER + "_" + emailCleanKey, prefs.getString(KEY_USER_GENDER, "Not Set"));
        String address = prefs.getString(KEY_USER_ADDRESS + "_" + emailCleanKey, prefs.getString(KEY_USER_ADDRESS, "Not Set"));
        String customerId = prefs.getString("customer_id", "");

        tvProfileName.setText(name);
        tvProfileEmail.setText(customerId.isEmpty() ? email : email + " • " + customerId);

        tvDetailEmail.setText(email);
        tvDetailPhone.setText(phone);
        tvDetailBirthdate.setText(birthdate);
        tvDetailGender.setText(gender);
        tvDetailAddress.setText(address);

        // Fetch live customer profile record from Supabase to populate saved phone/address
        if (!email.isEmpty() && !"guest@casavista.com".equalsIgnoreCase(email)) {
            com.example.casavista.network.SupabaseClient.getInstance().fetchCustomerProfile(email, new com.example.casavista.network.SupabaseClient.DataCallback() {
                @Override
                public void onSuccess(String jsonArrayString) {
                    if (jsonArrayString == null || getContext() == null) return;
                    try {
                        org.json.JSONObject obj = new org.json.JSONObject(jsonArrayString);
                        String dbPhone = obj.optString("phone", "");
                        String dbAddress = obj.optString("address", "");
                        String dbBirthdate = obj.optString("birthdate", obj.optString("birth_date", ""));
                        String dbGender = obj.optString("gender", "");
                        String dbFirst = obj.optString("first_name", "");
                        String dbLast = obj.optString("last_name", "");

                        boolean updated = false;
                        SharedPreferences.Editor editor = prefs.edit();

                        if (!dbPhone.isEmpty() && !"N/A".equalsIgnoreCase(dbPhone) && !"Not Set".equalsIgnoreCase(dbPhone)) {
                            editor.putString(KEY_USER_PHONE, dbPhone);
                            editor.putString(KEY_USER_PHONE + "_" + emailCleanKey, dbPhone);
                            tvDetailPhone.setText(dbPhone);
                            updated = true;
                        }

                        if (!dbAddress.isEmpty() && !"N/A".equalsIgnoreCase(dbAddress) && !"Not Set".equalsIgnoreCase(dbAddress)) {
                            editor.putString(KEY_USER_ADDRESS, dbAddress);
                            editor.putString(KEY_USER_ADDRESS + "_" + emailCleanKey, dbAddress);
                            tvDetailAddress.setText(dbAddress);
                            updated = true;
                        }

                        if (!dbBirthdate.isEmpty() && !"N/A".equalsIgnoreCase(dbBirthdate) && !"Not Set".equalsIgnoreCase(dbBirthdate)) {
                            editor.putString(KEY_USER_BIRTHDATE, dbBirthdate);
                            editor.putString(KEY_USER_BIRTHDATE + "_" + emailCleanKey, dbBirthdate);
                            tvDetailBirthdate.setText(dbBirthdate);
                            updated = true;
                        }

                        if (!dbGender.isEmpty() && !"N/A".equalsIgnoreCase(dbGender) && !"Not Set".equalsIgnoreCase(dbGender)) {
                            editor.putString(KEY_USER_GENDER, dbGender);
                            editor.putString(KEY_USER_GENDER + "_" + emailCleanKey, dbGender);
                            tvDetailGender.setText(dbGender);
                            updated = true;
                        }

                        if (!dbFirst.isEmpty()) {
                            String dbName = dbFirst + (dbLast.isEmpty() ? "" : " " + dbLast);
                            editor.putString(KEY_USER_NAME, dbName);
                            editor.putString(KEY_USER_NAME + "_" + emailCleanKey, dbName);
                            tvProfileName.setText(dbName);
                            updated = true;
                        }

                        if (updated) {
                            editor.apply();
                        }
                    } catch (Exception ignored) {}
                }

                @Override
                public void onError(String errorMessage) {}
            });
        }

        // Compute actual active bookings & past completed nights stayed dynamically
        com.example.casavista.data.ResortRepository repo = com.example.casavista.data.ResortRepository.getInstance();
        repo.loadSavedBookings(requireContext());
        java.util.List<com.example.casavista.models.Booking> userBookings = repo.getBookings();

        int totalBookings = 0;
        int nightsStayed = 0;
        Date now = new Date();
        SimpleDateFormat isoFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        if (userBookings != null) {
            for (com.example.casavista.models.Booking b : userBookings) {
                String status = b.getStatus() != null ? b.getStatus().trim() : "";
                if ("Cancelled".equalsIgnoreCase(status) || "Declined".equalsIgnoreCase(status)) {
                    continue;
                }

                totalBookings++;

                boolean isCompleted = "Completed".equalsIgnoreCase(status);

                String checkOutStr = b.getCheckOutDate();
                boolean isPastCheckOut = false;
                if (checkOutStr != null && !checkOutStr.isEmpty()) {
                    try {
                        Date checkOutDate = isoFmt.parse(checkOutStr);
                        if (checkOutDate != null && checkOutDate.before(now)) {
                            isPastCheckOut = true;
                        }
                    } catch (Exception ignored) {}
                }

                if (isCompleted || (isPastCheckOut && ("Checked-In".equalsIgnoreCase(status) || "Confirmed".equalsIgnoreCase(status)))) {
                    long nights = 1;
                    if (b.getCheckInDate() != null && b.getCheckOutDate() != null) {
                        try {
                            Date dIn = isoFmt.parse(b.getCheckInDate());
                            Date dOut = isoFmt.parse(b.getCheckOutDate());
                            if (dIn != null && dOut != null) {
                                long diff = dOut.getTime() - dIn.getTime();
                                long n = diff / (1000 * 60 * 60 * 24);
                                if (n > 0) nights = n;
                            }
                        } catch (Exception ignored) {}
                    }
                    nightsStayed += (int) nights;
                }
            }
        }

        if (tvTotalBookingsCount != null) {
            tvTotalBookingsCount.setText(String.valueOf(totalBookings));
        }
        if (tvNightsStayedCount != null) {
            tvNightsStayedCount.setText(String.valueOf(nightsStayed));
        }

        updateInitials(name);
        displayProfileImage();
    }

    private void showEditNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Edit Full Name");

        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setText(tvProfileName.getText().toString());
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                String[] parts = newName.split("\\s+");
                String firstName = parts[0];
                String lastName = parts.length > 1 ? parts[parts.length - 1] : "";

                SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                String email = prefs.getString(KEY_USER_EMAIL, "guest");
                String emailCleanKey = email.toLowerCase().replaceAll("[^a-zA-Z0-9]", "_");

                prefs.edit()
                        .putString(KEY_USER_NAME, newName)
                        .putString(KEY_USER_NAME + "_" + emailCleanKey, newName)
                        .putString(KEY_FIRST_NAME, firstName)
                        .putString(KEY_LAST_NAME, lastName)
                        .apply();

                loadSavedProfile();
                syncProfileToBackend();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showEditFieldDialog(String title, String prefKey, int inputType) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Edit " + title);

        final EditText input = new EditText(requireContext());
        input.setInputType(inputType);

        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String email = prefs.getString(KEY_USER_EMAIL, "guest");
        String emailCleanKey = email.toLowerCase().replaceAll("[^a-zA-Z0-9]", "_");

        String currentVal = prefs.getString(prefKey + "_" + emailCleanKey, prefs.getString(prefKey, ""));
        if ("Not Set".equalsIgnoreCase(currentVal)) {
            currentVal = "";
        }
        input.setText(currentVal);
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String val = input.getText().toString().trim();
            if (!val.isEmpty()) {
                prefs.edit()
                        .putString(prefKey, val)
                        .putString(prefKey + "_" + emailCleanKey, val)
                        .apply();
                loadSavedProfile();
                syncProfileToBackend();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showBirthdatePicker() {
        Calendar calStart = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calStart.set(1900, Calendar.JANUARY, 1, 0, 0, 0);
        calStart.set(Calendar.MILLISECOND, 0);
        long minAllowedBirthdateMillis = calStart.getTimeInMillis();

        Calendar cal18 = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal18.add(Calendar.YEAR, -18);
        long maxAllowedBirthdateMillis = cal18.getTimeInMillis();

        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setStart(minAllowedBirthdateMillis)
                .setEnd(maxAllowedBirthdateMillis)
                .setOpenAt(maxAllowedBirthdateMillis)
                .setValidator(DateValidatorPointBackward.before(maxAllowedBirthdateMillis))
                .build();

        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Birthdate (Must be 18+)")
                .setCalendarConstraints(constraints)
                .setSelection(maxAllowedBirthdateMillis)
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            String selectedDate = format.format(new Date(selection));

            SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String email = prefs.getString(KEY_USER_EMAIL, "guest");
            String emailCleanKey = email.toLowerCase().replaceAll("[^a-zA-Z0-9]", "_");

            prefs.edit()
                    .putString(KEY_USER_BIRTHDATE, selectedDate)
                    .putString(KEY_USER_BIRTHDATE + "_" + emailCleanKey, selectedDate)
                    .apply();

            loadSavedProfile();
            syncProfileToBackend();
        });

        datePicker.show(getParentFragmentManager(), "BIRTHDATE_PICKER");
    }

    private void showGenderPicker() {
        String[] options = {"Male", "Female", "Non-binary", "Prefer not to say"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Select Gender")
                .setItems(options, (dialog, which) -> {
                    SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    String email = prefs.getString(KEY_USER_EMAIL, "guest");
                    String emailCleanKey = email.toLowerCase().replaceAll("[^a-zA-Z0-9]", "_");

                    prefs.edit()
                            .putString(KEY_USER_GENDER, options[which])
                            .putString(KEY_USER_GENDER + "_" + emailCleanKey, options[which])
                            .apply();
                    loadSavedProfile();
                    syncProfileToBackend();
                })
                .show();
    }

    private void updateInitials(String name) {
        if (name == null || name.trim().isEmpty()) {
            tvAvatarInitials.setText("CV");
            return;
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            String initials = ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
            tvAvatarInitials.setText(initials);
        } else if (parts.length == 1 && !parts[0].isEmpty()) {
            tvAvatarInitials.setText(("" + parts[0].charAt(0)).toUpperCase());
        }
    }

    private String getProfilePicFileName() {
        if (getContext() == null) return "profile_pic.jpg";
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String custId = prefs.getString("customer_id", "");
        String email = prefs.getString(KEY_USER_EMAIL, "");
        String key = !custId.isEmpty() ? custId : (!email.isEmpty() ? email.toLowerCase().replaceAll("[^a-zA-Z0-9]", "_") : "default");
        return "profile_pic_" + key + ".jpg";
    }

    private void saveImageToInternalStorage(Uri uri) {
        if (getContext() == null || uri == null) return;
        try {
            byte[] buffer = new byte[1024];
            int length;

            // 1. Save to user-specific profile pic file
            File userPicFile = new File(requireContext().getFilesDir(), getProfilePicFileName());
            try (InputStream is1 = requireContext().getContentResolver().openInputStream(uri);
                 FileOutputStream os1 = new FileOutputStream(userPicFile)) {
                if (is1 != null) {
                    while ((length = is1.read(buffer)) > 0) {
                        os1.write(buffer, 0, length);
                    }
                }
            }

            // 2. Save to global profile_pic.jpg file
            File globalPicFile = new File(requireContext().getFilesDir(), "profile_pic.jpg");
            try (InputStream is2 = requireContext().getContentResolver().openInputStream(uri);
                 FileOutputStream os2 = new FileOutputStream(globalPicFile)) {
                if (is2 != null) {
                    while ((length = is2.read(buffer)) > 0) {
                        os2.write(buffer, 0, length);
                    }
                }
            }

            // 3. Save to AvatarHelper
            com.example.casavista.helpers.AvatarHelper.saveProfileImage(requireContext(), uri);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void displayProfileImage() {
        if (getContext() == null) return;
        File file = new File(requireContext().getFilesDir(), getProfilePicFileName());
        if (!file.exists()) {
            file = new File(requireContext().getFilesDir(), "profile_pic.jpg");
        }
        if (file.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            if (bitmap != null) {
                imgProfilePicture.setImageBitmap(bitmap);
                imgProfilePicture.setVisibility(View.VISIBLE);
                tvAvatarInitials.setVisibility(View.GONE);
                return;
            }
        }
        imgProfilePicture.setVisibility(View.GONE);
        tvAvatarInitials.setVisibility(View.VISIBLE);
    }
}