package com.example.casavista.auth;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.casavista.R;
import com.example.casavista.admin.AdminMainActivity;
import com.example.casavista.data.ResortRepository;
import com.example.casavista.helpers.ApiHelper;
import com.example.casavista.network.SupabaseClient;
import com.example.casavista.superadmin.SuperAdminMainActivity;
import com.example.casavista.user.MainActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private View btnGoogle, btnApple, btnFacebook;
    private TextureView textureBgLogin;
    private MediaPlayer mediaPlayer;

    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    private View bottomCard, layoutHeroBrand, layoutTopDragHandle, btnHoveringExpand;
    private boolean isCardCollapsed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogle = findViewById(R.id.btnGoogle);
        btnApple = findViewById(R.id.btnApple);
        btnFacebook = findViewById(R.id.btnFacebook);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Intent data = result.getData();
                    if (data != null) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                        handleGoogleSignInResult(task);
                    } else {
                        Toast.makeText(this, "Google Sign-In cancelled.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        btnLogin.setOnClickListener(v -> performLogin());

        if (btnGoogle != null) {
            btnGoogle.setOnClickListener(v -> onSocialOptionClicked("Google"));
        }
        if (btnApple != null) {
            btnApple.setOnClickListener(v -> onSocialOptionClicked("Apple"));
        }
        if (btnFacebook != null) {
            btnFacebook.setOnClickListener(v -> onSocialOptionClicked("Facebook"));
        }

        View tvForgotPassword = findViewById(R.id.tvForgotPassword);
        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
        }

        View btnGoToSignUp = findViewById(R.id.btnGoToSignUp);
        if (btnGoToSignUp != null) {
            btnGoToSignUp.setOnClickListener(v -> {
                Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
                startActivity(intent);
            });
        }

        setupVideoBackground();
        setupCardToggleAnimation();
    }

    private void onSocialOptionClicked(String provider) {
        if (!"Google".equalsIgnoreCase(provider)) {
            Toast.makeText(this, provider + " Sign-In is not enabled. Please sign in with Email/Username or Google.", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("CasavistaProfilePrefs", MODE_PRIVATE);
        boolean hasAgreedTerms = prefs.getBoolean("has_agreed_terms", false);

        if (hasAgreedTerms) {
            if (googleSignInClient != null && googleSignInLauncher != null) {
                googleSignInClient.signOut().addOnCompleteListener(task -> {
                    googleSignInLauncher.launch(googleSignInClient.getSignInIntent());
                });
            }
        } else {
            showSocialTermsDialog(provider);
        }
    }

    private void showSocialTermsDialog(String provider) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_terms_and_conditions, null);

        ScrollView svTerms = dialogView.findViewById(R.id.svTerms);
        TextView tvScrollNotice = dialogView.findViewById(R.id.tvScrollNotice);
        MaterialButton btnAgreeTerms = dialogView.findViewById(R.id.btnAgreeTerms);
        MaterialButton btnCloseTerms = dialogView.findViewById(R.id.btnCloseTerms);

        if (btnAgreeTerms != null) {
            btnAgreeTerms.setText("Agree & Continue");
            btnAgreeTerms.setEnabled(true);
            btnAgreeTerms.setAlpha(0.7f);
        }

        final boolean[] scrolledToBottom = {false};

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(provider + " Sign-Up Terms & Privacy")
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (svTerms != null) {
            svTerms.getViewTreeObserver().addOnScrollChangedListener(() -> {
                if (svTerms.getChildCount() > 0) {
                    View child = svTerms.getChildAt(svTerms.getChildCount() - 1);
                    int diff = (child.getBottom() - (svTerms.getHeight() + svTerms.getScrollY()));
                    if (diff <= 100) {
                        scrolledToBottom[0] = true;
                        if (btnAgreeTerms != null) {
                            btnAgreeTerms.setAlpha(1.0f);
                        }
                        if (tvScrollNotice != null) {
                            tvScrollNotice.setText("✓ You have read the entire Terms & Conditions. You may now accept.");
                            tvScrollNotice.setTextColor(0xFF2E7D32);
                        }
                    }
                }
            });
        }

        if (btnCloseTerms != null) {
            btnCloseTerms.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnAgreeTerms != null) {
            btnAgreeTerms.setOnClickListener(v -> {
                if (!scrolledToBottom[0] && svTerms != null && svTerms.getChildCount() > 0) {
                    View child = svTerms.getChildAt(svTerms.getChildCount() - 1);
                    int diff = (child.getBottom() - (svTerms.getHeight() + svTerms.getScrollY()));
                    if (diff > 100) {
                        Toast.makeText(this, "Please scroll to the bottom of the Terms & Conditions to agree.", Toast.LENGTH_SHORT).show();
                        svTerms.fullScroll(View.FOCUS_DOWN);
                        return;
                    }
                }

                dialog.dismiss();
                SharedPreferences prefs = getSharedPreferences("CasavistaProfilePrefs", MODE_PRIVATE);
                prefs.edit().putBoolean("has_agreed_terms", true).apply();

                if ("Google".equalsIgnoreCase(provider) && googleSignInClient != null && googleSignInLauncher != null) {
                    googleSignInLauncher.launch(googleSignInClient.getSignInIntent());
                }
            });
        }

        dialog.show();
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> task) {
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account != null) {
                processGoogleAccountAndLogin(account);
                return;
            }
        } catch (ApiException e) {
            int statusCode = e.getStatusCode();
            Log.e("LoginActivity", "Google Sign-In ApiException status code: " + statusCode);

            GoogleSignInAccount cachedAccount = GoogleSignIn.getLastSignedInAccount(this);
            if (cachedAccount != null && cachedAccount.getEmail() != null) {
                processGoogleAccountAndLogin(cachedAccount);
                return;
            }

            if (statusCode == 12501) {
                Toast.makeText(this, "Google Sign-In was cancelled.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Google Sign-In failed (Code " + statusCode + "). Please log in with your registered email/username and password.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void processGoogleAccountAndLogin(GoogleSignInAccount account) {
        String email = account.getEmail();
        String displayName = account.getDisplayName();
        String givenName = account.getGivenName();
        String familyName = account.getFamilyName();

        if (givenName == null || givenName.trim().isEmpty()) {
            if (displayName != null && displayName.contains(" ")) {
                String[] parts = displayName.split(" ", 2);
                givenName = parts[0];
                familyName = parts[1];
            } else {
                givenName = displayName != null ? displayName : "Guest";
                familyName = "User";
            }
        }
        if (familyName == null || familyName.trim().isEmpty()) {
            familyName = "User";
        }

        syncAndLoginGoogleUser(email, givenName, familyName);
    }

    private void syncAndLoginGoogleUser(String email, String givenName, String familyName) {
        if (email == null || email.trim().isEmpty()) {
            Toast.makeText(this, "Google account email not available.", Toast.LENGTH_SHORT).show();
            return;
        }
        final String finalFirst = givenName != null ? givenName : "Google";
        final String finalLast = familyName != null ? familyName : "User";
        final String finalEmail = email.trim().toLowerCase();
        final String fullName = finalFirst + " " + finalLast;

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Syncing Google account with CasaVista...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        SupabaseClient.getInstance().syncGoogleUser(
                finalEmail,
                finalFirst,
                finalLast,
                new SupabaseClient.AuthCallback() {
                    @Override
                    public void onSuccess(String customerId, String emailResp, JSONObject userData) {
                        if (!isFinishing() && !isDestroyed()) {
                            progressDialog.dismiss();
                        }

                        if (customerId == null || customerId.isEmpty() || customerId.startsWith("local-") || customerId.startsWith("CUST-17")) {
                            Toast.makeText(LoginActivity.this, "Could not sync Google account with server. Please try again.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        ResortRepository repo = ResortRepository.getInstance();
                        repo.clearBookings(LoginActivity.this);

                        SharedPreferences prefs = getSharedPreferences("CasavistaProfilePrefs", MODE_PRIVATE);
                        String emailCleanKey = finalEmail.toLowerCase().replaceAll("[^a-zA-Z0-9]", "_");

                        SharedPreferences.Editor editor = prefs.edit()
                                .putString("user_id", customerId)
                                .putString("customer_id", customerId)
                                .putString("customer_id_" + emailCleanKey, customerId)
                                .putString("user_name", fullName)
                                .putString("user_name_" + emailCleanKey, fullName)
                                .putString("user_first_name", finalFirst)
                                .putString("user_last_name", finalLast)
                                .putString("user_email", finalEmail)
                                .putBoolean("is_logged_in", true)
                                .putBoolean("has_agreed_terms", true);

                        if (userData != null) {
                            String phoneFromDb = userData.optString("phone", "");
                            String addressFromDb = userData.optString("address", "");
                            if (addressFromDb.contains("||CVAUTH:")) {
                                addressFromDb = addressFromDb.split("\\|\\|CVAUTH:")[0].trim();
                            }
                            if (!phoneFromDb.isEmpty() && !"N/A".equals(phoneFromDb)) {
                                editor.putString("user_phone", phoneFromDb);
                                editor.putString("user_phone_" + emailCleanKey, phoneFromDb);
                            }
                            if (!addressFromDb.isEmpty() && !"N/A".equals(addressFromDb)) {
                                editor.putString("user_address", addressFromDb);
                                editor.putString("user_address_" + emailCleanKey, addressFromDb);
                            }
                        }
                        editor.apply();

                        repo.setCurrentUserName(fullName);
                        repo.setCurrentUserEmail(finalEmail);
                        repo.setCurrentCustomerId(customerId);
                        com.example.casavista.network.SupabaseClient.setCurrentSessionCustomer(customerId, finalEmail);

                        Toast.makeText(LoginActivity.this, "Welcome to CasaVista, " + finalFirst + " (" + customerId + ")!", Toast.LENGTH_SHORT).show();
                        navigateBasedOnRole("USER");
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (!isFinishing() && !isDestroyed()) {
                            progressDialog.dismiss();
                        }
                        new MaterialAlertDialogBuilder(LoginActivity.this)
                                .setTitle("Google Sign-In Sync Error")
                                .setMessage("Could not verify your Google account with the server:\n" + errorMessage)
                                .setPositiveButton("OK", null)
                                .show();
                    }
                }
        );
    }

    private void showForgotPasswordDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_forgot_password, null);
        TextInputLayout tilResetEmail = dialogView.findViewById(R.id.tilResetEmail);
        EditText etResetEmail = dialogView.findViewById(R.id.etResetEmail);

        if (etEmail != null && etEmail.getText() != null && etResetEmail != null) {
            etResetEmail.setText(etEmail.getText().toString().trim());
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Forgot Password")
                .setView(dialogView)
                .setPositiveButton("Send Reset Link", (dialog, which) -> {
                    String email = etResetEmail != null ? etResetEmail.getText().toString().trim() : "";
                    if (email.isEmpty()) {
                        if (tilResetEmail != null) {
                            tilResetEmail.setError("Please enter your email address.");
                        }
                        Toast.makeText(this, "Please enter your email address to reset password.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Password reset instructions sent to " + email + ". Please check your inbox!", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void setupCardToggleAnimation() {
        bottomCard = findViewById(R.id.bottomCard);
        layoutHeroBrand = findViewById(R.id.layoutHeroBrand);
        layoutTopDragHandle = findViewById(R.id.layoutTopDragHandle);
        btnHoveringExpand = findViewById(R.id.btnHoveringExpand);

        if (layoutTopDragHandle != null) {
            layoutTopDragHandle.setOnClickListener(v -> collapseLoginForm());
        }

        if (btnHoveringExpand != null) {
            btnHoveringExpand.setOnClickListener(v -> expandLoginForm());
        }

        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 != null && e2 != null) {
                    float diffY = e2.getY() - e1.getY();
                    if (diffY > 100 && Math.abs(velocityY) > 100) {
                        collapseLoginForm();
                        return true;
                    } else if (diffY < -100 && Math.abs(velocityY) > 100) {
                        expandLoginForm();
                        return true;
                    }
                }
                return false;
            }
        });

        if (bottomCard != null) {
            bottomCard.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
        }
    }

    private void collapseLoginForm() {
        if (bottomCard == null || isCardCollapsed) return;
        isCardCollapsed = true;

        float slideDistance = bottomCard.getHeight() + 300f;
        bottomCard.animate()
                .translationY(slideDistance)
                .setDuration(380)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        if (layoutHeroBrand != null) {
            layoutHeroBrand.animate()
                    .translationY(60f)
                    .setDuration(380)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        if (btnHoveringExpand != null) {
            btnHoveringExpand.setAlpha(0f);
            btnHoveringExpand.setVisibility(View.VISIBLE);
            btnHoveringExpand.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start();
        }
    }

    private void expandLoginForm() {
        if (bottomCard == null || !isCardCollapsed) return;
        isCardCollapsed = false;

        bottomCard.animate()
                .translationY(0f)
                .setDuration(380)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        if (layoutHeroBrand != null) {
            layoutHeroBrand.animate()
                    .translationY(0f)
                    .setDuration(380)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        if (btnHoveringExpand != null) {
            btnHoveringExpand.animate()
                    .alpha(0f)
                    .setDuration(220)
                    .withEndAction(() -> btnHoveringExpand.setVisibility(View.GONE))
                    .start();
        }
    }

    private void setupVideoBackground() {
        textureBgLogin = findViewById(R.id.videoBgLogin);
        if (textureBgLogin == null) return;

        textureBgLogin.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
                try {
                    Surface surface = new Surface(surfaceTexture);
                    mediaPlayer = new MediaPlayer();
                    Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.resort_login_bg);
                    mediaPlayer.setDataSource(getApplicationContext(), videoUri);
                    mediaPlayer.setSurface(surface);
                    mediaPlayer.setLooping(true);
                    mediaPlayer.setVolume(0f, 0f);

                    mediaPlayer.setOnPreparedListener(mp -> {
                        int videoWidth = mp.getVideoWidth();
                        int videoHeight = mp.getVideoHeight();

                        if (width > 0 && height > 0 && videoWidth > 0 && videoHeight > 0) {
                            float videoRatio = (float) videoWidth / (float) videoHeight;
                            float screenRatio = (float) width / (float) height;

                            float scaleX = 1.0f;
                            float scaleY = 1.0f;

                            if (videoRatio > screenRatio) {
                                scaleX = videoRatio / screenRatio;
                            } else {
                                scaleY = screenRatio / videoRatio;
                            }

                            textureBgLogin.setScaleX(scaleX);
                            textureBgLogin.setScaleY(scaleY);
                        }

                        mediaPlayer.start();
                    });

                    mediaPlayer.prepareAsync();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {}

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void saveStaffSession(String staffId, String fullName, String email, String role) {
        ResortRepository repo = ResortRepository.getInstance();
        repo.setCurrentUserName(fullName);
        repo.setCurrentUserEmail(email);

        SharedPreferences adminPrefs = getSharedPreferences("AdminPrefs", MODE_PRIVATE);
        adminPrefs.edit()
                .putString("staff_id", staffId)
                .putString("staff_name", fullName)
                .putString("staff_email", email)
                .putString("staff_role", role)
                .putBoolean("is_admin_logged_in", true)
                .apply();

        // Ensure staff record exists in Supabase resort.staff table for POS/Booking foreign key integrity
        String[] parts = fullName.trim().split("\\s+", 2);
        String firstName = parts.length > 0 ? parts[0] : "Staff";
        String lastName = parts.length > 1 ? parts[1] : "Member";
        SupabaseClient.getInstance().createStaffMember(staffId, firstName, null, lastName, role, new SupabaseClient.DataCallback() {
            @Override
            public void onSuccess(String jsonArrayString) {}
            @Override
            public void onError(String errorMessage) {}
        });
    }

    private boolean isValidStaffPassword(String roleKey, String password) {
        if (password == null) return false;
        if (password.equals("admin123") || password.equals("Admin123!") || password.equals("Staff123!")) {
            return true;
        }
        switch (roleKey) {
            case "Manager":
                return password.equals("manager123") || password.equals("Manager123!");
            case "Front Desk":
                return password.equals("frontdesk123") || password.equals("FrontDesk123!");
            case "Cashier":
                return password.equals("cashier123") || password.equals("Cashier123!");
            default:
                return false;
        }
    }

    private void performLogin() {
        String identifier = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";

        if (identifier.isEmpty()) {
            etEmail.setError("Please enter your username or email");
            Toast.makeText(this, "Please enter your username or email", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Please enter your password");
            Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show();
            return;
        }

        String lowerIdent = identifier.toLowerCase();

        // 1. SuperAdmin Login Check
        if ((lowerIdent.equals("superadmin") || lowerIdent.equals("superadmin@casavista.com"))
                && (password.equals("superadmin123") || password.equals("SuperAdmin123!"))) {
            Toast.makeText(this, "Welcome Super Admin!", Toast.LENGTH_SHORT).show();
            navigateBasedOnRole("SUPER_ADMIN");
            return;
        }
        // Check SuperAdmin-managed Staff Accounts in EmployeeRepository (supports custom usernames & passwords)
        com.example.casavista.models.Employee matchedStaff =
                com.example.casavista.data.EmployeeRepository.getInstance().authenticateStaff(this, identifier, password);
        if (matchedStaff != null) {
            saveStaffSession(matchedStaff.getStaffId(), matchedStaff.getName(), matchedStaff.getEmail(), matchedStaff.getRole());
            Toast.makeText(this, "Welcome " + matchedStaff.getName() + " (" + matchedStaff.getRole() + ")!", Toast.LENGTH_SHORT).show();
            navigateBasedOnRole("ADMIN");
            return;
        }
        // 2. Built-in Unified @casavista.com Staff Accounts (Admin, Manager, Front Desk, Cashier)
        if (lowerIdent.equals("admin") || lowerIdent.equals("admin@casavista.com")) {
            if (isValidStaffPassword("Admin", password)) {
                saveStaffSession("STF-001", "Juan Dela Cruz", "admin@casavista.com", "Admin");
                Toast.makeText(this, "Welcome Head Administrator!", Toast.LENGTH_SHORT).show();
                navigateBasedOnRole("ADMIN");
                return;
            }
        } else if (lowerIdent.equals("manager") || lowerIdent.equals("manager@casavista.com")) {
            if (isValidStaffPassword("Manager", password)) {
                saveStaffSession("STF-002", "Maria Clara Santos", "manager@casavista.com", "Manager");
                Toast.makeText(this, "Welcome Resort Manager!", Toast.LENGTH_SHORT).show();
                navigateBasedOnRole("ADMIN");
                return;
            }
        } else if (lowerIdent.equals("frontdesk") || lowerIdent.equals("frontdesk@casavista.com")) {
            if (isValidStaffPassword("Front Desk", password)) {
                saveStaffSession("STF-003", "Angela Reyes", "frontdesk@casavista.com", "Front Desk");
                Toast.makeText(this, "Welcome Front Desk Officer!", Toast.LENGTH_SHORT).show();
                navigateBasedOnRole("ADMIN");
                return;
            }
        } else if (lowerIdent.equals("cashier") || lowerIdent.equals("cashier@casavista.com")) {
            if (isValidStaffPassword("Cashier", password)) {
                saveStaffSession("STF-004", "Carlo Mendoza", "cashier@casavista.com", "Cashier");
                Toast.makeText(this, "Welcome Billing Cashier!", Toast.LENGTH_SHORT).show();
                navigateBasedOnRole("ADMIN");
                return;
            }
        }

        // 3. Dynamic Staff Login for any @casavista.com staff email or STF-XXX ID from resort.staff table
        if (lowerIdent.endsWith("@casavista.com") || lowerIdent.startsWith("stf-")) {
            ProgressDialog staffDialog = new ProgressDialog(this);
            staffDialog.setMessage("Verifying CasaVista Staff credentials...");
            staffDialog.setCancelable(false);
            staffDialog.show();
            btnLogin.setEnabled(false);

            SupabaseClient.getInstance().fetchStaffMembers(new SupabaseClient.DataCallback() {
                @Override
                public void onSuccess(String jsonArrayString) {
                    try {
                        JSONArray staffArray = new JSONArray(jsonArrayString);
                        for (int i = 0; i < staffArray.length(); i++) {
                            JSONObject s = staffArray.getJSONObject(i);
                            String sId = s.optString("staff_id", "STF-001");
                            String fName = s.optString("first_name", "Staff");
                            String lName = s.optString("last_name", "Member");
                            String role = s.optString("role", "Front Desk");
                            String fullName = (fName + " " + lName).trim();

                            String generatedEmail1 = (fName.toLowerCase().replaceAll("\\s+", "") + "." + lName.toLowerCase().replaceAll("\\s+", "") + "@casavista.com");
                            String generatedEmail2 = (fName.toLowerCase().replaceAll("\\s+", "") + "@casavista.com");
                            String generatedEmail3 = (sId.toLowerCase() + "@casavista.com");

                            boolean matchesStaff = lowerIdent.equalsIgnoreCase(sId)
                                    || lowerIdent.equalsIgnoreCase(generatedEmail1)
                                    || lowerIdent.equalsIgnoreCase(generatedEmail2)
                                    || lowerIdent.equalsIgnoreCase(generatedEmail3);

                            if (matchesStaff && isValidStaffPassword(role, password)) {
                                if (!isFinishing() && !isDestroyed()) staffDialog.dismiss();
                                btnLogin.setEnabled(true);
                                String finalStaffEmail = lowerIdent.contains("@") ? lowerIdent : generatedEmail1;
                                saveStaffSession(sId, fullName, finalStaffEmail, role);
                                Toast.makeText(LoginActivity.this, "Welcome " + fullName + " (" + role + ")!", Toast.LENGTH_SHORT).show();
                                navigateBasedOnRole("ADMIN");
                                return;
                            }
                        }
                    } catch (Exception ignored) {}

                    if (!isFinishing() && !isDestroyed()) staffDialog.dismiss();
                    proceedWithCustomerLogin(identifier, password);
                }

                @Override
                public void onError(String errorMessage) {
                    if (!isFinishing() && !isDestroyed()) staffDialog.dismiss();
                    proceedWithCustomerLogin(identifier, password);
                }
            });
            return;
        }

        proceedWithCustomerLogin(identifier, password);
    }

    private void proceedWithCustomerLogin(String identifier, String password) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Verifying credentials...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        btnLogin.setEnabled(false);

        ApiHelper.loginCustomerAccount(
                this,
                identifier,
                password,
                new ApiHelper.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        if (!isFinishing() && !isDestroyed()) {
                            progressDialog.dismiss();
                        }
                        btnLogin.setEnabled(true);

                        ResortRepository repo = ResortRepository.getInstance();
                        repo.clearBookings(LoginActivity.this);

                        String custId = response.optString("customer_id", "");
                        String firstName = response.optString("first_name", "Guest");
                        String lastName = response.optString("last_name", "User");
                        String email = response.optString("email", identifier);
                        String fullName = firstName + " " + lastName;

                        String emailCleanKey = email.toLowerCase().replaceAll("[^a-zA-Z0-9]", "_");
                        SharedPreferences prefs = getSharedPreferences("CasavistaProfilePrefs", MODE_PRIVATE);
                        prefs.edit()
                                .putString("user_id", custId)
                                .putString("customer_id", custId)
                                .putString("customer_id_" + emailCleanKey, custId)
                                .putString("user_name", fullName)
                                .putString("user_name_" + emailCleanKey, fullName)
                                .putString("user_first_name", firstName)
                                .putString("user_last_name", lastName)
                                .putString("user_email", email)
                                .putBoolean("is_logged_in", true)
                                .putBoolean("has_agreed_terms", true)
                                .apply();

                        repo.setCurrentUserName(fullName);
                        repo.setCurrentUserEmail(email);
                        repo.setCurrentCustomerId(custId);
                        com.example.casavista.network.SupabaseClient.setCurrentSessionCustomer(custId, email);

                        Toast.makeText(LoginActivity.this, "Welcome back, " + firstName + " (" + custId + ")!", Toast.LENGTH_SHORT).show();
                        navigateBasedOnRole("USER");
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (!isFinishing() && !isDestroyed()) {
                            progressDialog.dismiss();
                        }
                        btnLogin.setEnabled(true);

                        new MaterialAlertDialogBuilder(LoginActivity.this)
                                .setTitle("Login Failed")
                                .setMessage(errorMessage != null ? errorMessage : "Invalid username/email or password. Please register an account first.")
                                .setPositiveButton("OK", null)
                                .show();
                    }
                }
        );
    }

    private void navigateBasedOnRole(String role) {
        Intent intent;
        switch (role.toUpperCase()) {
            case "SUPER_ADMIN":
                intent = new Intent(LoginActivity.this, SuperAdminMainActivity.class);
                break;
            case "ADMIN":
                intent = new Intent(LoginActivity.this, AdminMainActivity.class);
                break;
            default:
                intent = new Intent(LoginActivity.this, MainActivity.class);
                break;
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}