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
                    if (uri != null) {
                        saveImageToInternalStorage(uri);
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
        view.findViewById(R.id.rowEditEmail).setOnClickListener(v -> showEditFieldDialog("Email Address", KEY_USER_EMAIL, InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS));
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

        // Preserve terms agreement state across user logouts
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .remove("is_logged_in")
                .remove("user_id")
                .remove("customer_id")
                .putBoolean("has_agreed_terms", true)
                .apply();

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
        String email = prefs.getString(KEY_USER_EMAIL, "test12345@casavista.com");
        String firstName = prefs.getString(KEY_FIRST_NAME, "Test12345");
        String lastName = prefs.getString(KEY_LAST_NAME, "User");
        String phone = prefs.getString(KEY_USER_PHONE, "09123456789");
        String address = prefs.getString(KEY_USER_ADDRESS, "Cebu City");

        com.example.casavista.helpers.ApiHelper.syncCustomerToDatabase(
                requireContext(),
                email,
                firstName,
                lastName,
                phone,
                address,
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

        String name = prefs.getString(KEY_USER_NAME, "Test12345 User");
        String email = prefs.getString(KEY_USER_EMAIL, "test12345@casavista.com");
        String phone = prefs.getString(KEY_USER_PHONE, "09123456789");
        String birthdate = prefs.getString(KEY_USER_BIRTHDATE, "01/15/1998");
        String gender = prefs.getString(KEY_USER_GENDER, "Male");
        String address = prefs.getString(KEY_USER_ADDRESS, "Cebu City, Central Visayas, Philippines");
        String customerId = prefs.getString("customer_id", "");

        tvProfileName.setText(name);
        tvProfileEmail.setText(customerId.isEmpty() ? email : email + " • " + customerId);

        tvDetailEmail.setText(email);
        tvDetailPhone.setText(phone);
        tvDetailBirthdate.setText(birthdate);
        tvDetailGender.setText(gender);
        tvDetailAddress.setText(address);

        // Compute actual bookings & nights stayed dynamically
        com.example.casavista.data.ResortRepository repo = com.example.casavista.data.ResortRepository.getInstance();
        repo.loadSavedBookings(requireContext());
        java.util.List<com.example.casavista.models.Booking> userBookings = repo.getBookings();
        int totalBookings = userBookings != null ? userBookings.size() : 0;
        int nightsStayed = 0;
        if (userBookings != null) {
            for (com.example.casavista.models.Booking b : userBookings) {
                if (b.getDetail2() != null && b.getDetail2().toLowerCase().contains("night")) {
                    nightsStayed++;
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

                requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                        .putString(KEY_USER_NAME, newName)
                        .putString(KEY_FIRST_NAME, firstName)
                        .putString(KEY_LAST_NAME, lastName)
                        .apply();

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
        input.setText(prefs.getString(prefKey, ""));
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String val = input.getText().toString().trim();
            if (!val.isEmpty()) {
                prefs.edit().putString(prefKey, val).apply();
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

            requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                    .putString(KEY_USER_BIRTHDATE, selectedDate)
                    .apply();

            loadSavedProfile();
        });

        datePicker.show(getParentFragmentManager(), "BIRTHDATE_PICKER");
    }

    private void showGenderPicker() {
        String[] options = {"Male", "Female", "Non-binary", "Prefer not to say"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Select Gender")
                .setItems(options, (dialog, which) -> {
                    requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                            .putString(KEY_USER_GENDER, options[which])
                            .apply();
                    loadSavedProfile();
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

    private void saveImageToInternalStorage(Uri uri) {
        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(new File(requireContext().getFilesDir(), PROFILE_PIC_FILE_NAME))) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void displayProfileImage() {
        File file = new File(requireContext().getFilesDir(), PROFILE_PIC_FILE_NAME);
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