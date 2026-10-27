package com.example.casavista.superadmin.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.casavista.R;
import com.example.casavista.adapters.EmployeeAdapter;
import com.example.casavista.auth.LoginActivity;
import com.example.casavista.data.EmployeeRepository;
import com.example.casavista.helpers.AvatarHelper;
import com.example.casavista.models.Employee;
import com.example.casavista.network.SupabaseClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class SuperAdminProfileFragment extends Fragment implements EmployeeAdapter.OnEmployeeActionListener {

    private static final String PROFILE_PREFS = "SuperAdminProfilePrefs";
    private static final String APP_PREFS = "AppPrefs";

    private static final String KEY_DARK_MODE = "isDarkMode";
    private static final String KEY_ADMIN_NAME = "super_admin_name";
    private static final String KEY_ADMIN_EMAIL = "super_admin_email";
    private static final String KEY_ADMIN_ROLE = "super_admin_role";
    private static final String KEY_AVATAR_PATH = "super_admin_avatar_path";

    private ShapeableImageView imgProfileAvatar;
    private ImageView btnChangePhoto;
    private TextView tvSuperAdminName, tvSuperAdminEmail, tvRoleBadge;
    private TextView tvStatActiveStaff, tvStatBookings;
    private MaterialButton btnLogout, btnAddRole, btnEditProfile;
    private MaterialCardView btnStaffSchedule, btnGuestReviews;
    private SwitchMaterial switchDarkMode;

    private RecyclerView rvActiveEmployees;
    private EmployeeAdapter adapter;

    private final ActivityResultLauncher<String> photoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null && getContext() != null) {
                    AvatarHelper.saveProfileImage(requireContext(), "superadmin@casavista.com", uri, () -> {
                        if (isAdded()) {
                            restoreSavedPreferences();
                            Toast.makeText(getContext(), "Profile photo updated & synced to Supabase!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_super_admin_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        imgProfileAvatar = view.findViewById(R.id.imgProfileAvatar);
        btnChangePhoto = view.findViewById(R.id.btnChangePhoto);

        tvSuperAdminName = view.findViewById(R.id.tvSuperAdminName);
        tvSuperAdminEmail = view.findViewById(R.id.tvSuperAdminEmail);
        tvRoleBadge = view.findViewById(R.id.tvRoleBadge);
        tvStatActiveStaff = view.findViewById(R.id.tvStatActiveStaff);
        tvStatBookings = view.findViewById(R.id.tvStatBookings);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);

        btnLogout = view.findViewById(R.id.btnLogout);
        btnAddRole = view.findViewById(R.id.btnAddRole);
        btnStaffSchedule = view.findViewById(R.id.btnStaffSchedule);
        btnGuestReviews = view.findViewById(R.id.btnGuestReviews);
        rvActiveEmployees = view.findViewById(R.id.rvActiveEmployees);
        switchDarkMode = view.findViewById(R.id.switchDarkMode);

        restoreSavedPreferences();
        setupEmployeeList();
        setupClickListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        restoreSavedPreferences();
        loadEmployeeList();
    }

    private void restoreSavedPreferences() {
        if (getContext() == null) return;

        SharedPreferences profilePrefs = requireContext().getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE);

        String name = profilePrefs.getString(KEY_ADMIN_NAME, "Dr. Lemyel Yap");
        String email = profilePrefs.getString(KEY_ADMIN_EMAIL, "superadmin@casavista.com");
        String role = profilePrefs.getString(KEY_ADMIN_ROLE, "SUPER ADMIN");

        if (tvSuperAdminName != null) tvSuperAdminName.setText(name);
        if (tvSuperAdminEmail != null) tvSuperAdminEmail.setText(email);
        if (tvRoleBadge != null) tvRoleBadge.setText(role);

        SharedPreferences appPrefs = requireContext().getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE);
        boolean isDarkMode = appPrefs.getBoolean(KEY_DARK_MODE, false);
        if (switchDarkMode != null) {
            switchDarkMode.setChecked(isDarkMode);
        }

        AvatarHelper.loadAvatar(requireContext(), email, imgProfileAvatar, null, name);
    }

    private String saveAvatarToInternalStorage(Uri imageUri) {
        if (getContext() == null || imageUri == null) return null;
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
            if (inputStream == null) return null;

            File file = new File(requireContext().getFilesDir(), "super_admin_avatar.jpg");
            FileOutputStream outputStream = new FileOutputStream(file);

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();
            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void setupClickListeners() {
        if (btnChangePhoto != null) {
            btnChangePhoto.setOnClickListener(v -> {
                animateSelection(v);
                photoPickerLauncher.launch("image/*");
            });
        }

        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> {
                animateSelection(v);
                showEditProfileDialog();
            });
        }

        if (tvSuperAdminName != null) {
            tvSuperAdminName.setOnClickListener(v -> showEditProfileDialog());
        }

        if (btnAddRole != null) {
            btnAddRole.setOnClickListener(v -> {
                animateSelection(v);
                showAddRoleDialog();
            });
        }

        if (switchDarkMode != null) {
            switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                SharedPreferences prefs = requireContext().getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE);
                if (prefs.getBoolean(KEY_DARK_MODE, false) == isChecked) return;

                prefs.edit().putBoolean(KEY_DARK_MODE, isChecked).apply();

                AppCompatDelegate.setDefaultNightMode(
                        isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
                );
            });
        }

        if (btnStaffSchedule != null) {
            btnStaffSchedule.setOnClickListener(v -> {
                animateSelection(v);
                Toast.makeText(getContext(), "Opening Staff Schedules...", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnGuestReviews != null) {
            btnGuestReviews.setOnClickListener(v -> {
                animateSelection(v);
                Toast.makeText(getContext(), "Opening Guest Reviews...", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                animateSelection(v);
                showLogoutConfirmation();
            });
        }
    }

    private void showEditProfileDialog() {
        if (getContext() == null) return;

        SharedPreferences profilePrefs = requireContext().getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE);
        String currentName = profilePrefs.getString(KEY_ADMIN_NAME, "Dr. Lemyel Yap");
        String currentEmail = profilePrefs.getString(KEY_ADMIN_EMAIL, "superadmin@casavista.com");
        String currentRole = profilePrefs.getString(KEY_ADMIN_ROLE, "SUPER ADMIN");

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_super_admin_profile, null);
        EditText etName = dialogView.findViewById(R.id.etEditAdminName);
        EditText etEmail = dialogView.findViewById(R.id.etEditAdminEmail);
        EditText etRole = dialogView.findViewById(R.id.etEditAdminRole);

        if (etName != null) etName.setText(currentName);
        if (etEmail != null) etEmail.setText(currentEmail);
        if (etRole != null) etRole.setText(currentRole);

        new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Save Profile", (dialog, which) -> {
                    String newName = etName != null ? etName.getText().toString().trim() : "";
                    String newEmail = etEmail != null ? etEmail.getText().toString().trim() : "";
                    String newRole = etRole != null ? etRole.getText().toString().trim() : "";

                    if (newName.isEmpty()) newName = "Dr. Lemyel Yap";
                    if (newEmail.isEmpty()) newEmail = "superadmin@casavista.com";
                    if (newRole.isEmpty()) newRole = "SUPER ADMIN";

                    profilePrefs.edit()
                            .putString(KEY_ADMIN_NAME, newName)
                            .putString(KEY_ADMIN_EMAIL, newEmail)
                            .putString(KEY_ADMIN_ROLE, newRole)
                            .apply();

                    restoreSavedPreferences();
                    Toast.makeText(getContext(), "Super Admin profile saved successfully!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showLogoutConfirmation() {
        if (getContext() == null) return;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Exit SuperAdmin Console")
                .setMessage("Are you sure you want to log out of the SuperAdmin console?")
                .setPositiveButton("Log Out", (dialog, which) -> performLogout())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void performLogout() {
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        }
    }

    private void setupEmployeeList() {
        List<Employee> employees = EmployeeRepository.getInstance().getEmployees(requireContext());
        adapter = new EmployeeAdapter(employees, this);
        rvActiveEmployees.setLayoutManager(new LinearLayoutManager(getContext()));
        rvActiveEmployees.setAdapter(adapter);
    }

    private void loadEmployeeList() {
        if (getContext() != null) {
            List<Employee> updatedList = EmployeeRepository.getInstance().getEmployees(requireContext());
            if (adapter != null) {
                adapter.updateData(updatedList);
            }
            if (tvStatActiveStaff != null) {
                tvStatActiveStaff.setText(String.valueOf(updatedList.size()));
            }

            // Query live today's bookings count
            SupabaseClient.getInstance().fetchBookings(new SupabaseClient.DataCallback() {
                @Override
                public void onSuccess(String jsonResponse) {
                    try {
                        org.json.JSONArray arr = new org.json.JSONArray(jsonResponse);
                        int count = 0;
                        String todayStr = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new java.util.Date());
                        for (int i = 0; i < arr.length(); i++) {
                            org.json.JSONObject b = arr.getJSONObject(i);
                            String status = b.optString("status", "");
                            String bDate = b.optString("booking_date", "");
                            String cIn = b.optString("check_in_date", "");
                            if (("Confirmed".equalsIgnoreCase(status) || "Checked-In".equalsIgnoreCase(status))
                                    && (bDate.startsWith(todayStr) || cIn.startsWith(todayStr))) {
                                count++;
                            }
                        }
                        final int finalCount = count;
                        if (isAdded() && tvStatBookings != null) {
                            tvStatBookings.setText(String.valueOf(finalCount));
                        }
                    } catch (Exception ignored) {}
                }

                @Override
                public void onError(String errorMessage) {}
            });
        }
    }

    private void showAddRoleDialog() {
        if (getContext() == null) return;
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle("Add New Staff & Login Credentials");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(56, 24, 56, 16);

        final EditText etName = new EditText(getContext());
        etName.setHint("Full Name (e.g. Jopet Tan)");
        layout.addView(etName);

        final EditText etDept = new EditText(getContext());
        etDept.setHint("Department (e.g. Front Office, Billing)");
        layout.addView(etDept);

        TextView tvRoleLabel = new TextView(getContext());
        tvRoleLabel.setText("Select System Access Role:");
        tvRoleLabel.setTextSize(12f);
        tvRoleLabel.setPadding(8, 20, 8, 4);
        layout.addView(tvRoleLabel);

        final Spinner spinnerRole = new Spinner(getContext());
        List<String> validRoles = Arrays.asList("Front Desk", "Cashier", "Manager", "Admin");
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, validRoles);
        spinnerRole.setAdapter(roleAdapter);
        layout.addView(spinnerRole);

        final EditText etUsername = new EditText(getContext());
        etUsername.setHint("Login Username (e.g. jopet)");
        layout.addView(etUsername);

        final EditText etEmail = new EditText(getContext());
        etEmail.setHint("Staff Email (e.g. jopet@casavista.com)");
        etEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        layout.addView(etEmail);

        final EditText etPassword = new EditText(getContext());
        etPassword.setHint("Login Password (default: Staff123!)");
        etPassword.setText("Staff123!");
        layout.addView(etPassword);

        // Auto-suggest username & @casavista.com email as name is typed
        etName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String clean = s.toString().trim().toLowerCase(Locale.US).replaceAll("\\s+", ".");
                String userClean = s.toString().trim().toLowerCase(Locale.US).replaceAll("[^a-z0-9]", "");
                if (!clean.isEmpty()) {
                    etUsername.setText(userClean);
                    etEmail.setText(clean + "@casavista.com");
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        builder.setView(layout);

        builder.setPositiveButton("Create Staff Account", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            String dept = etDept.getText().toString().trim();
            String role = spinnerRole.getSelectedItem().toString();
            String username = etUsername.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (dept.isEmpty()) dept = role + " Department";

            if (!name.isEmpty()) {
                Employee created = EmployeeRepository.getInstance().addEmployeeWithCredentials(
                        requireContext(), name, dept, role, username, email, password
                );
                loadEmployeeList();
                Toast.makeText(
                        getContext(),
                        "Created " + created.getName() + " (" + created.getRole() + ")\nLogin: " + created.getEmail() + " | Pass: " + created.getPassword(),
                        Toast.LENGTH_LONG
                ).show();
            } else {
                Toast.makeText(getContext(), "Employee full name is required", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    public void onEditRole(Employee employee, int position) {
        if (getContext() == null) return;
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle("Edit Staff & Login — " + employee.getName());

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(56, 24, 56, 16);

        final EditText etName = new EditText(getContext());
        etName.setHint("Full Name");
        etName.setText(employee.getName());
        layout.addView(etName);

        final EditText etDept = new EditText(getContext());
        etDept.setHint("Department");
        etDept.setText(employee.getDepartment());
        layout.addView(etDept);

        TextView tvRoleLabel = new TextView(getContext());
        tvRoleLabel.setText("System Access Role:");
        tvRoleLabel.setTextSize(12f);
        tvRoleLabel.setPadding(8, 20, 8, 4);
        layout.addView(tvRoleLabel);

        final Spinner spinnerRole = new Spinner(getContext());
        List<String> validRoles = Arrays.asList("Front Desk", "Cashier", "Manager", "Admin");
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, validRoles);
        spinnerRole.setAdapter(roleAdapter);
        int roleIdx = validRoles.indexOf(EmployeeRepository.normalizeRoleForDatabase(employee.getRole()));
        if (roleIdx >= 0) spinnerRole.setSelection(roleIdx);
        layout.addView(spinnerRole);

        final EditText etUsername = new EditText(getContext());
        etUsername.setHint("Login Username");
        etUsername.setText(employee.getUsername());
        layout.addView(etUsername);

        final EditText etEmail = new EditText(getContext());
        etEmail.setHint("Staff Email (@casavista.com)");
        etEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        etEmail.setText(employee.getEmail());
        layout.addView(etEmail);

        final EditText etPassword = new EditText(getContext());
        etPassword.setHint("Login Password");
        etPassword.setText(employee.getPassword());
        layout.addView(etPassword);

        builder.setView(layout);

        builder.setPositiveButton("Save Changes", (dialog, which) -> {
            String newName = etName.getText().toString().trim();
            String newDept = etDept.getText().toString().trim();
            String newRole = spinnerRole.getSelectedItem().toString();
            String newUsername = etUsername.getText().toString().trim();
            String newEmail = etEmail.getText().toString().trim();
            String newPassword = etPassword.getText().toString().trim();

            if (!newName.isEmpty() && !newDept.isEmpty()) {
                EmployeeRepository.getInstance().updateEmployeeWithCredentials(
                        requireContext(),
                        position,
                        newName,
                        newDept,
                        newRole,
                        newUsername,
                        newEmail,
                        newPassword
                );
                loadEmployeeList();
                Toast.makeText(getContext(), "Updated " + newName + " (" + newRole + ") credentials!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    public void onDeleteRole(Employee employee, int position) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Remove Staff Account")
                .setMessage("Are you sure you want to remove " + employee.getName() + " (" + employee.getEmail() + ")?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    EmployeeRepository.getInstance().deleteEmployee(requireContext(), position);
                    loadEmployeeList();
                    Toast.makeText(getContext(), "Staff account removed", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void animateSelection(View view) {
        if (view == null) return;
        view.setScaleX(0.95f);
        view.setScaleY(0.95f);
        view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
    }
}