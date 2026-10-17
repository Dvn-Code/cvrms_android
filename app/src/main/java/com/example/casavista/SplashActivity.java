package com.example.casavista;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.casavista.auth.LoginActivity;

// Splash / Opening screen displayed when the application is launched
public class SplashActivity extends AppCompatActivity {

    // Duration of splash screen before transitioning to LoginActivity (2.5 seconds)
    private static final int SPLASH_DURATION = 2500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Enforce Light Mode theme across the app on startup
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Purge & truncate all Supabase bookings & payments for clean walkthrough testing
        com.example.casavista.network.SupabaseClient.getInstance().purgeAndResetAllBookingsAndPayments(this, null);

        // Bind layout views
        LinearLayout brandContainer = findViewById(R.id.brandContainer);
        ImageView ivSplashLogo = findViewById(R.id.ivSplashLogo);
        ProgressBar pbSplashLoading = findViewById(R.id.pbSplashLoading);

        // Set initial transparent/scaled states for startup animations
        brandContainer.setAlpha(0f);
        brandContainer.setTranslationY(60f);
        ivSplashLogo.setScaleX(0.85f);
        ivSplashLogo.setScaleY(0.85f);
        pbSplashLoading.setAlpha(0f);

        // Smooth fade-in animation for brand title
        brandContainer.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(1000)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        // Scale-up animation for resort logo
        ivSplashLogo.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(1000)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        // Fade-in animation for progress loading bar
        pbSplashLoading.animate()
                .alpha(1f)
                .setStartDelay(400)
                .setDuration(600)
                .start();

        // Delayed Handler to transition from Splash to Login screen
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);

            // Cross-fade animation transition
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, SPLASH_DURATION);
    }
}