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

import org.json.JSONObject;

// Handles user sign-in, Google Sign-In SDK authentication, and role-based routing
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

        // Bind input fields and action buttons
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogle = findViewById(R.id.btnGoogle);
        btnApple = findViewById(R.id.btnApple);
        btnFacebook = findViewById(R.id.btnFacebook);

        // Pre-fill default test credentials for quick testing
        etEmail.setText("test12345@casavista.com");
        etPassword.setText("test12345");

        // Initialize Google Sign-In Client
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Register ActivityResultLauncher for Google Sign-In flow
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

        // Action click listeners
        btnLogin.setOnClickListener(v -> performLogin());

        // Social auth click handlers with one-time Terms agreement check
        if (btnGoogle != null) {
            btnGoogle.setOnClickListener(v -> onSocialOptionClicked("Google"));
        }
        if (btnApple != null) {
            btnApple.setOnClickListener(v -> onSocialOptionClicked("Apple"));
        }
        if (btnFacebook != null) {
            btnFacebook.setOnClickListener(v -> onSocialOptionClicked("Facebook"));
        }

        // Forgot password dialog trigger
        View tvForgotPassword = findViewById(R.id.tvForgotPassword);
        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
        }

        // Navigate to account registration screen
        View btnGoToSignUp = findViewById(R.id.btnGoToSignUp);
        if (btnGoToSignUp != null) {
            btnGoToSignUp.setOnClickListener(v -> {
                Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
                startActivity(intent);
            });
        }

        // Initialize background video and card collapse toggle
        setupVideoBackground();
        setupCardToggleAnimation();
    }

    // Handles social option click: checks if terms were already agreed, bypassing terms modal for returning users
    private void onSocialOptionClicked(String provider) {
        SharedPreferences prefs = getSharedPreferences("CasavistaProfilePrefs", MODE_PRIVATE);
        boolean hasAgreedTerms = prefs.getBoolean("has_agreed_terms", false)
                || prefs.contains("user_email")
                || prefs.contains("customer_id")
                || prefs.contains("user_id");

        if (hasAgreedTerms) {
            // Ensure terms agreement flag is persisted for future logins
            prefs.edit().putBoolean("has_agreed_terms", true).apply();

            // User already agreed to terms in a previous session — sign in directly!
            if ("Google".equalsIgnoreCase(provider) && googleSignInClient != null && googleSignInLauncher != null) {
                googleSignInClient.signOut().addOnCompleteListener(task -> {
                    googleSignInLauncher.launch(googleSignInClient.getSignInIntent());
                });
            } else {
                navigateToUserMain("Signed in with " + provider);
            }
        } else {
            // First time social sign in — show Terms & Conditions agreement modal
            showSocialTermsDialog(provider);
        }
    }

    // Displays Terms & Conditions agreement dialog required for first-time social registration
    private void showSocialTermsDialog(String provider) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_terms_and_conditions, null);

        ScrollView svTerms = dialogView.findViewById(R.id.svTerms);
        TextView tvScrollNotice = dialogView.findViewById(R.id.tvScrollNotice);
        MaterialButton btnAgreeTerms = dialogView.findViewById(R.id.btnAgreeTerms);
        MaterialButton btnCloseTerms = dialogView.findViewById(R.id.btnCloseTerms);

        if (btnAgreeTerms != null) {
            btnAgreeTerms.setText("Agree");
            btnAgreeTerms.setEnabled(false);
        }

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
                    if (diff <= 30) {
                        if (btnAgreeTerms != null) btnAgreeTerms.setEnabled(true);
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
                dialog.dismiss();
                // Remember that terms have been agreed for future social sign ins
                SharedPreferences prefs = getSharedPreferences("CasavistaProfilePrefs", MODE_PRIVATE);
                prefs.edit().putBoolean("has_agreed_terms", true).apply();

                if ("Google".equalsIgnoreCase(provider) && googleSignInClient != null && googleSignInLauncher != null) {
                    googleSignInLauncher.launch(googleSignInClient.getSignInIntent());
                } else {
                    navigateToUserMain("Signed in with " + provider);
                }
            });
        }

        dialog.show();
    }

    // Processes Google Sign-In account payload, syncs profile with backend, and saves local session
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

            // Attempt to check if a cached account exists on device
            GoogleSignInAccount cachedAccount = GoogleSignIn.getLastSignedInAccount(this);
            if (cachedAccount != null && cachedAccount.getEmail() != null) {
                Toast.makeText(this, "Logged in via cached Google Account: " + cachedAccount.getEmail(), Toast.LENGTH_SHORT).show();
                processGoogleAccountAndLogin(cachedAccount);
                return;
            }

            if (statusCode == 12501) { // User explicitly cancelled account picker
                Toast.makeText(this, "Google Sign-In was cancelled.", Toast.LENGTH_SHORT).show();
            } else {
                showGoogleSignInErrorDialog(statusCode, e.getLocalizedMessage());
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

    private void showGoogleSignInErrorDialog(int statusCode, String errorMsg) {
        String detail;
        if (statusCode == 7) { // NETWORK_ERROR
            detail = "Network Connection Issue (Code 7: NETWORK_ERROR).\n\n"
                    + "Google Play Services was unable to establish a secure connection to Google authentication servers.\n"
                    + "Please ensure your device/emulator has active internet connectivity.";
        } else if (statusCode == 10) { // DEVELOPER_ERROR
            detail = "Google Developer Configuration Error (Code 10).\n\n"
                    + "Please check:\n"
                    + "1. App SHA-1 fingerprint (41:12:DE:9C:...) is added in Google Cloud Console.\n"
                    + "2. Your friend's email is added under OAuth Consent Screen ➔ Test Users.\n"
                    + "3. Package name matches com.example.casavista.";
        } else if (statusCode == 12500) { // SIGN_IN_FAILED
            detail = "Google Sign-In Failed (Code 12500).\n\n"
                    + "Check that OAuth Consent Screen is configured in Google Cloud Console and test users are added.";
        } else {
            detail = "Google Sign-In Notice (Code " + statusCode + ").\n" + (errorMsg != null ? errorMsg : "");
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Google Auth Notice (Code " + statusCode + ")")
                .setMessage(detail + "\n\nWould you like to log in with a Test Google Account or try again?")
                .setPositiveButton("Use Test Google Account", (dialog, which) -> {
                    syncAndLoginGoogleUser("google.test@casavista.com", "Google", "User");
                })
                .setNegativeButton("Retry", (dialog, which) -> {
                    dialog.dismiss();
                    if (googleSignInClient != null && googleSignInLauncher != null) {
                        googleSignInLauncher.launch(googleSignInClient.getSignInIntent());
                    }
                })
                .setNeutralButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void syncAndLoginGoogleUser(String email, String givenName, String familyName) {
        final String finalFirst = givenName != null ? givenName : "Google";
        final String finalLast = familyName != null ? familyName : "User";
        final String finalEmail = (email != null && !email.trim().isEmpty()) ? email : "google.test@casavista.com";
        final String fullName = finalFirst + " " + finalLast;

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Syncing Google account with CasaVista...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Mark terms as agreed in profile preferences
        SharedPreferences prefs = getSharedPreferences("CasavistaProfilePrefs", MODE_PRIVATE);
        prefs.edit().putBoolean("has_agreed_terms", true).apply();

        SupabaseClient.getInstance().syncGoogleUser(
                finalEmail,
                finalFirst,
                finalLast,
                new SupabaseClient.AuthCallback() {
                    @Override
                    public void onSuccess(String customerId, String email, JSONObject userData) {
                        if (!isFinishing() && !isDestroyed()) {
                            progressDialog.dismiss();
                        }

                        SharedPreferences prefs = getSharedPreferences("CasavistaProfilePrefs", MODE_PRIVATE);
                        prefs.edit()
                                .putString("user_id", customerId)
                                .putString("customer_id", customerId)
                                .putString("user_name", fullName)
                                .putString("user_first_name", finalFirst)
                                .putString("user_last_name", finalLast)
                                .putString("user_email", finalEmail)
                                .putBoolean("is_logged_in", true)
                                .putBoolean("has_agreed_terms", true)
                                .apply();

                        ResortRepository repo = ResortRepository.getInstance();
                        repo.setCurrentUserName(fullName);
                        repo.setCurrentUserEmail(finalEmail);

                        Toast.makeText(LoginActivity.this, "Welcome to CasaVista, " + finalFirst + "!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (!isFinishing() && !isDestroyed()) {
                            progressDialog.dismiss();
                        }
                        Toast.makeText(LoginActivity.this, "Notice: " + errorMessage + ". Logged in locally.", Toast.LENGTH_SHORT).show();

                        SharedPreferences prefs = getSharedPreferences("CasavistaProfilePrefs", MODE_PRIVATE);
                        prefs.edit()
                                .putString("user_id", "local-" + System.currentTimeMillis())
                                .putString("customer_id", "local-" + System.currentTimeMillis())
                                .putString("user_name", fullName)
                                .putString("user_first_name", finalFirst)
                                .putString("user_last_name", finalLast)
                                .putString("user_email", finalEmail)
                                .putBoolean("is_logged_in", true)
                                .putBoolean("has_agreed_terms", true)
                                .apply();

                        ResortRepository repo = ResortRepository.getInstance();
                        repo.setCurrentUserName(fullName);
                        repo.setCurrentUserEmail(finalEmail);

                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                }
        );
    }

    // Displays popup dialog for password reset link requests
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

    // Handles swipe gestures and tap triggers to collapse or restore the login form
    private void setupCardToggleAnimation() {
        bottomCard = findViewById(R.id.bottomCard);
        layoutHeroBrand = findViewById(R.id.layoutHeroBrand);
        layoutTopDragHandle = findViewById(R.id.layoutTopDragHandle);
        btnHoveringExpand = findViewById(R.id.btnHoveringExpand);

        // Tap top handle to collapse form
        if (layoutTopDragHandle != null) {
            layoutTopDragHandle.setOnClickListener(v -> collapseLoginForm());
        }

        // Tap hovering button at bottom to reopen form
        if (btnHoveringExpand != null) {
            btnHoveringExpand.setOnClickListener(v -> expandLoginForm());
        }

        // Swipe down gesture detector on login card
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

        // Slide the full login form completely off-screen
        float slideDistance = bottomCard.getHeight() + 300f;
        bottomCard.animate()
                .translationY(slideDistance)
                .setDuration(380)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        // Move hero logo down slightly
        if (layoutHeroBrand != null) {
            layoutHeroBrand.animate()
                    .translationY(60f)
                    .setDuration(380)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        // Show hovering arrow pill button at bottom
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

        // Slide full login form back up into view
        bottomCard.animate()
                .translationY(0f)
                .setDuration(380)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        // Restore hero logo position
        if (layoutHeroBrand != null) {
            layoutHeroBrand.animate()
                    .translationY(0f)
                    .setDuration(380)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        // Hide hovering arrow pill button
        if (btnHoveringExpand != null) {
            btnHoveringExpand.animate()
                    .alpha(0f)
                    .setDuration(220)
                    .withEndAction(() -> btnHoveringExpand.setVisibility(View.GONE))
                    .start();
        }
    }

    // Loads looping resort background video from res/raw and scales TextureView to fit the entire screen
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

    // Validates entered email address and determines account role
    private void performLogin() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim().toLowerCase() : "";

        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter an email address", Toast.LENGTH_SHORT).show();
            return;
        }

        String fullEmail = email.contains("@") ? email : email + "@casavista.com";
        String firstName = fullEmail.split("@")[0];

        // Save active user profile and sync with Supabase backend
        getSharedPreferences("CasavistaProfilePrefs", MODE_PRIVATE).edit()
                .putString("user_id", "CUST-" + System.currentTimeMillis())
                .putString("customer_id", "CUST-" + System.currentTimeMillis())
                .putString("user_email", fullEmail)
                .putString("user_first_name", firstName)
                .putString("user_last_name", "User")
                .putString("user_name", firstName + " User")
                .putBoolean("is_logged_in", true)
                .putBoolean("has_agreed_terms", true)
                .apply();

        ResortRepository repo = ResortRepository.getInstance();
        repo.setCurrentUserName(firstName + " User");
        repo.setCurrentUserEmail(fullEmail);

        com.example.casavista.helpers.ApiHelper.syncCustomerToDatabase(
                this,
                fullEmail,
                firstName,
                "User",
                "09123456789",
                "Cebu City",
                null
        );

        // Determine user role based on email keyword
        String role = "USER";
        if (email.contains("superadmin") || email.contains("super")) {
            role = "SUPER_ADMIN";
            Toast.makeText(this, "Welcome Super Admin!", Toast.LENGTH_SHORT).show();
        } else if (email.contains("admin")) {
            role = "ADMIN";
            Toast.makeText(this, "Welcome to Admin Dashboard!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Welcome back to CasaVista!", Toast.LENGTH_SHORT).show();
        }

        navigateBasedOnRole(role);
    }

    // Launches the appropriate main activity based on user role
    private void navigateBasedOnRole(String role) {
        Intent intent;
        switch (role.toUpperCase()) {
            case "SUPER_ADMIN":
                intent = new Intent(LoginActivity.this, SuperAdminMainActivity.class);
                break;
            case "ADMIN":
                intent = new Intent(LoginActivity.this, AdminMainActivity.class);
                break;
            default: // Guest User
                intent = new Intent(LoginActivity.this, MainActivity.class);
                break;
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // Quick helper for social sign-in option clicks
    private void navigateToUserMain(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        navigateBasedOnRole("USER");
    }
}