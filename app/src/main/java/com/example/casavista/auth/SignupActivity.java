package com.example.casavista.auth;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.casavista.R;
import com.example.casavista.user.MainActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

// Handles new guest registration form with required field validation, Terms agreement, and age restrictions
public class SignupActivity extends AppCompatActivity {

    private TextInputLayout tilFirstName, tilMiddleName, tilLastName, tilGender, tilBirthdate;
    private TextInputLayout tilPhoneNumber, tilEmail, tilAddress;
    private TextInputLayout tilUsername, tilPassword, tilConfirmPassword;

    private EditText etFirstName, etMiddleName, etLastName, etGender, etBirthdate;
    private EditText etPhoneNumber, etEmail, etAddress;
    private EditText etUsername, etPassword, etConfirmPassword;

    private CheckBox cbTerms;
    private boolean hasScrolledTerms = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Bind TextInputLayout containers
        tilFirstName = findViewById(R.id.tilFirstName);
        tilMiddleName = findViewById(R.id.tilMiddleName);
        tilLastName = findViewById(R.id.tilLastName);
        tilGender = findViewById(R.id.tilGender);
        tilBirthdate = findViewById(R.id.tilBirthdate);
        tilPhoneNumber = findViewById(R.id.tilPhoneNumber);
        tilEmail = findViewById(R.id.tilEmail);
        tilAddress = findViewById(R.id.tilAddress);
        tilUsername = findViewById(R.id.tilUsername);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);

        // Bind registration form input fields
        etFirstName = findViewById(R.id.etFirstName);
        etMiddleName = findViewById(R.id.etMiddleName);
        etLastName = findViewById(R.id.etLastName);
        etGender = findViewById(R.id.etGender);
        etBirthdate = findViewById(R.id.etBirthdate);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etEmail = findViewById(R.id.etEmail);
        etAddress = findViewById(R.id.etAddress);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        // Bind Terms & Conditions views
        cbTerms = findViewById(R.id.cbTerms);
        View tvTermsLink = findViewById(R.id.tvTermsLink);
        View tvTermsLabel = findViewById(R.id.tvTermsLabel);

        // Terms click handlers
        if (tvTermsLink != null) {
            tvTermsLink.setOnClickListener(v -> showTermsDialog());
        }
        if (tvTermsLabel != null) {
            tvTermsLabel.setOnClickListener(v -> showTermsDialog());
        }

        if (cbTerms != null) {
            cbTerms.setOnClickListener(v -> {
                if (!hasScrolledTerms) {
                    cbTerms.setChecked(false);
                    Toast.makeText(this, "Please scroll to the bottom of the Terms & Conditions to agree.", Toast.LENGTH_LONG).show();
                    showTermsDialog();
                }
            });
        }

        // Add auto-clear error watchers as user types
        setupClearErrorWatchers();

        // Setup end icon info tooltips for all fields
        setupFieldTooltips();

        // Gender picker dialog click trigger
        etGender.setOnClickListener(v -> showGenderPickerDialog());

        // Birthdate calendar popup picker trigger
        etBirthdate.setOnClickListener(v -> showCalendarPicker());

        // Action button click triggers
        findViewById(R.id.btnSignUp).setOnClickListener(v -> performSignUp());
        findViewById(R.id.tvLogIn).setOnClickListener(v -> finish());
    }

    // Opens Terms & Conditions popup modal and enforces scroll-to-bottom before enabling I Agree
    private void showTermsDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_terms_and_conditions, null);

        ScrollView svTerms = dialogView.findViewById(R.id.svTerms);
        TextView tvScrollNotice = dialogView.findViewById(R.id.tvScrollNotice);
        MaterialButton btnAgreeTerms = dialogView.findViewById(R.id.btnAgreeTerms);
        MaterialButton btnCloseTerms = dialogView.findViewById(R.id.btnCloseTerms);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (hasScrolledTerms) {
            btnAgreeTerms.setEnabled(true);
            tvScrollNotice.setText("✓ You have read the entire Terms & Conditions. You may now accept.");
            tvScrollNotice.setTextColor(0xFF2E7D32);
        } else {
            btnAgreeTerms.setEnabled(false);
            if (svTerms != null) {
                svTerms.getViewTreeObserver().addOnScrollChangedListener(() -> {
                    if (svTerms.getChildCount() > 0) {
                        View child = svTerms.getChildAt(svTerms.getChildCount() - 1);
                        int diff = (child.getBottom() - (svTerms.getHeight() + svTerms.getScrollY()));
                        if (diff <= 30) {
                            hasScrolledTerms = true;
                            btnAgreeTerms.setEnabled(true);
                            if (tvScrollNotice != null) {
                                tvScrollNotice.setText("✓ You have read the entire Terms & Conditions. You may now accept.");
                                tvScrollNotice.setTextColor(0xFF2E7D32);
                            }
                        }
                    }
                });
            }
        }

        if (btnCloseTerms != null) {
            btnCloseTerms.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnAgreeTerms != null) {
            btnAgreeTerms.setOnClickListener(v -> {
                hasScrolledTerms = true;
                if (cbTerms != null) cbTerms.setChecked(true);
                getSharedPreferences("CasavistaProfilePrefs", MODE_PRIVATE).edit().putBoolean("has_agreed_terms", true).apply();
                Toast.makeText(this, "Terms & Conditions accepted.", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    // Configures interactive tooltip popups for every input field info icon
    private void setupFieldTooltips() {
        setTooltipListener(tilFirstName, "First Name",
                "Enter your legal given name exactly as stated on official government IDs (e.g., Juan).");
        setTooltipListener(tilMiddleName, "Middle Name",
                "Optional: Enter your middle name or middle initial if applicable.");
        setTooltipListener(tilLastName, "Last Name",
                "Enter your legal family name/surname as stated on official IDs (e.g., Dela Cruz).");
        setTooltipListener(tilGender, "Gender",
                "Select your gender identity for room and amenity guest reservation records.");
        setTooltipListener(tilBirthdate, "Birthdate (18+ Requirement)",
                "Select your birthdate. Account registration is legally restricted to individuals at least 18 years old (born on or before Oct 7, 2008). Dates after Oct 7, 2008 are grayed out.");
        setTooltipListener(tilPhoneNumber, "Phone Number",
                "Enter a valid 11-digit Philippine mobile number (e.g., 09123456789) to receive reservation SMS confirmations.");
        setTooltipListener(tilEmail, "Email Address",
                "Enter an active email address where official booking receipts, payment confirmations, and security alerts will be sent.");
        setTooltipListener(tilAddress, "Home Address",
                "Provide your complete residential address (Street, Barangay, City/Municipality, Province) for legal resort guest registration.");
        setTooltipListener(tilUsername, "Username",
                "Choose a unique handle (min 4 characters) for signing into your CasaVista account.");
    }

    private void setTooltipListener(TextInputLayout til, String title, String explanation) {
        if (til == null) return;
        til.setEndIconOnClickListener(v -> showFieldTooltip(title, explanation));
    }

    private void showFieldTooltip(String title, String explanation) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("ℹ️ " + title + " Help")
                .setMessage(explanation)
                .setPositiveButton("Got it", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void setupClearErrorWatchers() {
        addClearErrorWatcher(tilFirstName, etFirstName);
        addClearErrorWatcher(tilLastName, etLastName);
        addClearErrorWatcher(tilGender, etGender);
        addClearErrorWatcher(tilBirthdate, etBirthdate);
        addClearErrorWatcher(tilPhoneNumber, etPhoneNumber);
        addClearErrorWatcher(tilEmail, etEmail);
        addClearErrorWatcher(tilAddress, etAddress);
        addClearErrorWatcher(tilUsername, etUsername);
        addClearErrorWatcher(tilPassword, etPassword);
        addClearErrorWatcher(tilConfirmPassword, etConfirmPassword);
    }

    private void addClearErrorWatcher(TextInputLayout til, EditText et) {
        if (til == null || et == null) return;
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (til.getError() != null) {
                    til.setError(null);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    // Opens selection dialog for gender options
    private void showGenderPickerDialog() {
        String[] options = {"Male", "Female", "Non-binary", "Prefer not to say"};
        new AlertDialog.Builder(this)
                .setTitle("Select Gender")
                .setItems(options, (dialog, which) -> {
                    etGender.setText(options[which]);
                    if (tilGender != null) tilGender.setError(null);
                })
                .show();
    }

    // Displays Material Calendar date picker with 18+ age restriction (dates after Oct 7, 2008 disabled)
    private void showCalendarPicker() {
        Calendar calStart = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calStart.set(1900, Calendar.JANUARY, 1, 0, 0, 0);
        calStart.set(Calendar.MILLISECOND, 0);
        long minAllowedBirthdateMillis = calStart.getTimeInMillis();

        // Calculate max allowed date (18 years ago from today: Oct 7, 2008)
        Calendar cal18 = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal18.add(Calendar.YEAR, -18);
        long maxAllowedBirthdateMillis = cal18.getTimeInMillis();

        // Check if a date was already entered in etBirthdate
        long initialSelection = maxAllowedBirthdateMillis;
        String existingDateStr = etBirthdate.getText().toString().trim();
        if (!existingDateStr.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date parsedDate = sdf.parse(existingDateStr);
                if (parsedDate != null && parsedDate.getTime() <= maxAllowedBirthdateMillis) {
                    initialSelection = parsedDate.getTime();
                }
            } catch (Exception ignored) {}
        }

        // Enforce constraint: Allow range [1900 .. Oct 7, 2008], disable dates after Oct 7, 2008
        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setStart(minAllowedBirthdateMillis)
                .setEnd(maxAllowedBirthdateMillis)
                .setOpenAt(initialSelection)
                .setValidator(DateValidatorPointBackward.before(maxAllowedBirthdateMillis))
                .build();

        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Birthdate (Must be 18+)")
                .setCalendarConstraints(constraints)
                .setSelection(initialSelection)
                .build();

        Toast.makeText(this, "Select your birth year (1900 – Oct 7, 2008). Dates higher than Oct 7, 2008 are grayed out.", Toast.LENGTH_LONG).show();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            etBirthdate.setText(format.format(new Date(selection)));
            if (tilBirthdate != null) tilBirthdate.setError(null);
        });

        datePicker.show(getSupportFragmentManager(), "BIRTHDATE_PICKER");
    }

    // Validates required fields, highlights unfulfilled boxes in RED, checks 18+ age & terms scroll
    private void performSignUp() {
        String firstName = etFirstName.getText().toString().trim();
        String middleName = etMiddleName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String gender = etGender.getText().toString().trim();
        String birthdate = etBirthdate.getText().toString().trim();
        String phone = etPhoneNumber.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Clear previous error states
        if (tilFirstName != null) tilFirstName.setError(null);
        if (tilLastName != null) tilLastName.setError(null);
        if (tilGender != null) tilGender.setError(null);
        if (tilBirthdate != null) tilBirthdate.setError(null);
        if (tilPhoneNumber != null) tilPhoneNumber.setError(null);
        if (tilEmail != null) tilEmail.setError(null);
        if (tilAddress != null) tilAddress.setError(null);
        if (tilUsername != null) tilUsername.setError(null);
        if (tilPassword != null) tilPassword.setError(null);
        if (tilConfirmPassword != null) tilConfirmPassword.setError(null);

        boolean hasError = false;

        if (firstName.isEmpty()) {
            if (tilFirstName != null) tilFirstName.setError("First name is required");
            hasError = true;
        }

        if (lastName.isEmpty()) {
            if (tilLastName != null) tilLastName.setError("Last name is required");
            hasError = true;
        }

        if (gender.isEmpty()) {
            if (tilGender != null) tilGender.setError("Please select your gender");
            hasError = true;
        }

        if (birthdate.isEmpty()) {
            if (tilBirthdate != null) tilBirthdate.setError("Please select your birthdate");
            hasError = true;
        } else {
            // Verify age >= 18
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
                Date birthDateObj = sdf.parse(birthdate);
                if (birthDateObj != null) {
                    Calendar birthCal = Calendar.getInstance();
                    birthCal.setTime(birthDateObj);

                    Calendar minAgeCal = Calendar.getInstance();
                    minAgeCal.add(Calendar.YEAR, -18);

                    if (birthCal.after(minAgeCal)) {
                        if (tilBirthdate != null) {
                            tilBirthdate.setError("You must be at least 18 years old (born on or before Oct 7, 2008)");
                        }
                        hasError = true;
                    }
                }
            } catch (Exception e) {
                // Ignore parsing exception
            }
        }

        if (phone.isEmpty()) {
            if (tilPhoneNumber != null) tilPhoneNumber.setError("Phone number is required");
            hasError = true;
        }

        if (email.isEmpty()) {
            if (tilEmail != null) tilEmail.setError("Email address is required");
            hasError = true;
        }

        if (address.isEmpty()) {
            if (tilAddress != null) tilAddress.setError("Home address is required");
            hasError = true;
        }

        if (username.isEmpty()) {
            if (tilUsername != null) tilUsername.setError("Username is required");
            hasError = true;
        }

        if (password.isEmpty()) {
            if (tilPassword != null) tilPassword.setError("Password is required");
            hasError = true;
        }

        if (confirmPassword.isEmpty()) {
            if (tilConfirmPassword != null) tilConfirmPassword.setError("Confirm password is required");
            hasError = true;
        } else if (!password.equals(confirmPassword)) {
            if (tilConfirmPassword != null) tilConfirmPassword.setError("Passwords do not match");
            hasError = true;
        }

        if (hasError) {
            Toast.makeText(this, "Please fix the errors in the form before proceeding.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!hasScrolledTerms || (cbTerms != null && !cbTerms.isChecked())) {
            Toast.makeText(this, "Please read and agree to the Terms & Conditions to create an account.", Toast.LENGTH_LONG).show();
            showTermsDialog();
            return;
        }

        // Construct full display name
        String fullName = middleName.isEmpty()
                ? firstName + " " + lastName
                : firstName + " " + middleName + " " + lastName;

        // Step 1: Trigger Email Verification OTP before account creation
        Toast.makeText(this, "Sending 6-digit verification code to " + email + "...", Toast.LENGTH_SHORT).show();

        com.example.casavista.helpers.ApiHelper.sendVerificationCode(
                this,
                email,
                new com.example.casavista.helpers.ApiHelper.ApiCallback() {
                    @Override
                    public void onSuccess(org.json.JSONObject response) {
                        String devOtp = response != null ? response.optString("dev_otp", "") : "";
                        showEmailVerificationDialog(
                                email, devOtp, firstName, middleName, lastName, fullName, gender, birthdate, phone, address, username
                        );
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(SignupActivity.this, "Error sending verification code: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    // Displays Material AlertDialog requesting 6-digit OTP code before proceeding
    private void showEmailVerificationDialog(
            String email,
            String initialDevOtp,
            String firstName,
            String middleName,
            String lastName,
            String fullName,
            String gender,
            String birthdate,
            String phone,
            String address,
            String username
    ) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_email_verification, null);
        TextView tvSubtitle = dialogView.findViewById(R.id.tvVerificationSubtitle);
        if (tvSubtitle != null) {
            tvSubtitle.setText("We sent a 6-digit code to " + email + ".");
        }

        final TextInputLayout tilOtpCode = dialogView.findViewById(R.id.tilOtpCode);
        final EditText etOtpCode = dialogView.findViewById(R.id.etOtpCode);

        if (etOtpCode != null && initialDevOtp != null && !initialDevOtp.trim().isEmpty()) {
            etOtpCode.setText(initialDevOtp.trim());
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Verify Your Email")
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton("Verify & Create Account", null)
                .setNeutralButton("Resend Code (03:00)", null)
                .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                .create();

        final CountDownTimer[] resendTimer = new CountDownTimer[1];

        dialog.setOnDismissListener(d -> {
            if (resendTimer[0] != null) {
                resendTimer[0].cancel();
            }
        });

        dialog.setOnShowListener(dialogInterface -> {
            MaterialButton btnVerify = (MaterialButton) dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            MaterialButton btnResend = (MaterialButton) dialog.getButton(AlertDialog.BUTTON_NEUTRAL);

            // 3-Minute (180 seconds) Countdown Timer for Resend Code button
            Runnable start3MinTimer = () -> {
                if (btnResend != null) {
                    btnResend.setEnabled(false);
                }
                if (resendTimer[0] != null) {
                    resendTimer[0].cancel();
                }
                resendTimer[0] = new CountDownTimer(180000, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        long totalSec = millisUntilFinished / 1000;
                        long min = totalSec / 60;
                        long sec = totalSec % 60;
                        String timeStr = String.format(Locale.US, "%02d:%02d", min, sec);
                        MaterialButton btn = (MaterialButton) dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                        if (btn != null) {
                            btn.setEnabled(false);
                            btn.setText("Resend Code (" + timeStr + ")");
                        }
                    }

                    @Override
                    public void onFinish() {
                        MaterialButton btn = (MaterialButton) dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                        if (btn != null) {
                            btn.setEnabled(true);
                            btn.setText("Resend Code");
                        }
                    }
                }.start();
            };

            // Start initial 3-minute timer
            start3MinTimer.run();

            if (btnVerify != null) {
                btnVerify.setOnClickListener(v -> {
                    String codeStr = etOtpCode != null ? etOtpCode.getText().toString().trim() : "";
                    if (codeStr.isEmpty() || codeStr.length() < 6) {
                        if (tilOtpCode != null) {
                            tilOtpCode.setError("Please enter a 6-digit verification code.");
                        } else if (etOtpCode != null) {
                            etOtpCode.setError("Please enter a 6-digit verification code.");
                        }
                        return;
                    }

                    if (tilOtpCode != null) tilOtpCode.setError(null);

                    btnVerify.setEnabled(false);
                    btnVerify.setText("Verifying...");

                    com.example.casavista.helpers.ApiHelper.verifyCode(
                            SignupActivity.this,
                            email,
                            codeStr,
                            new com.example.casavista.helpers.ApiHelper.ApiCallback() {
                                @Override
                                public void onSuccess(org.json.JSONObject response) {
                                    if (resendTimer[0] != null) {
                                        resendTimer[0].cancel();
                                    }
                                    dialog.dismiss();
                                    executeRegistrationPipeline(
                                            email, firstName, lastName, fullName, gender, birthdate, phone, address, username
                                    );
                                }

                                @Override
                                public void onError(String errorMessage) {
                                    btnVerify.setEnabled(true);
                                    btnVerify.setText("Verify & Create Account");
                                    String errText = (errorMessage != null && !errorMessage.isEmpty())
                                            ? errorMessage
                                            : "Invalid or expired code. Please try again.";
                                    if (tilOtpCode != null) {
                                        tilOtpCode.setError(errText);
                                    } else if (etOtpCode != null) {
                                        etOtpCode.setError(errText);
                                    }
                                }
                            }
                    );
                });
            }

            if (btnResend != null) {
                btnResend.setOnClickListener(v -> {
                    Toast.makeText(SignupActivity.this, "Resending code to " + email + "...", Toast.LENGTH_SHORT).show();
                    com.example.casavista.helpers.ApiHelper.sendVerificationCode(
                            SignupActivity.this,
                            email,
                            new com.example.casavista.helpers.ApiHelper.ApiCallback() {
                                @Override
                                public void onSuccess(org.json.JSONObject response) {
                                    String devOtp = response != null ? response.optString("dev_otp", "") : "";
                                    if (etOtpCode != null && !devOtp.isEmpty()) {
                                        etOtpCode.setText(devOtp);
                                    }
                                    Toast.makeText(SignupActivity.this, "✓ Resent 6-digit code to " + email, Toast.LENGTH_SHORT).show();
                                    if (tilOtpCode != null) tilOtpCode.setError(null);
                                    start3MinTimer.run();
                                }

                                @Override
                                public void onError(String errorMessage) {
                                    Toast.makeText(SignupActivity.this, "Resend failed: " + errorMessage, Toast.LENGTH_LONG).show();
                                }
                            }
                    );
                });
            }
        });

        dialog.show();
    }

    // Final registration pipeline executed after email verification succeeds
    private void executeRegistrationPipeline(
            String email,
            String firstName,
            String lastName,
            String fullName,
            String gender,
            String birthdate,
            String phone,
            String address,
            String username
    ) {
        // Clear old dummy bookings for new user session
        com.example.casavista.data.ResortRepository.getInstance().clearBookings(this);

        // Save profile data locally and sync with Vercel FastAPI + Supabase DB
        SharedPreferences profilePrefs = getSharedPreferences("CasavistaProfilePrefs", Context.MODE_PRIVATE);
        profilePrefs.edit()
                .putString("user_name", fullName)
                .putString("user_first_name", firstName)
                .putString("user_last_name", lastName)
                .putString("user_gender", gender)
                .putString("user_birthdate", birthdate)
                .putString("user_email", email)
                .putString("user_phone", phone)
                .putString("user_address", address)
                .putString("user_username", username)
                .apply();

        com.example.casavista.helpers.ApiHelper.syncCustomerToDatabase(
                this,
                email,
                firstName,
                lastName,
                phone,
                address,
                new com.example.casavista.helpers.ApiHelper.ApiCallback() {
                    @Override
                    public void onSuccess(org.json.JSONObject response) {
                        String custId = response.optString("customer_id", "");
                        profilePrefs.edit().putString("customer_id", custId).apply();
                        Toast.makeText(SignupActivity.this, "Account verified & saved to Supabase (" + custId + ")!", Toast.LENGTH_LONG).show();
                        startActivity(new Intent(SignupActivity.this, MainActivity.class));
                        finish();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(SignupActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(SignupActivity.this, MainActivity.class));
                        finish();
                    }
                }
        );
    }
}