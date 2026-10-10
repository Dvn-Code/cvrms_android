package com.example.casavista.superadmin;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.casavista.R;
import com.example.casavista.superadmin.fragments.SuperAdminDashboardFragment;
import com.example.casavista.superadmin.fragments.SuperAdminRoomsFragment;
import com.example.casavista.superadmin.fragments.SuperAdminHistoryFragment;
import com.example.casavista.superadmin.fragments.SuperAdminProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

// Main Super Admin container activity managing bottom navigation tabs for executive management
public class SuperAdminMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Restore saved dark mode theme setting
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("isDarkMode", false);

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_super_admin_main);

        BottomNavigationView navView = findViewById(R.id.superAdminNavView);

        // Load default Super Admin Dashboard on initial startup
        if (savedInstanceState == null) {
            loadFragment(new SuperAdminDashboardFragment());
        }

        // Handle Super Admin bottom navigation tab selection
        navView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_super_home) {
                selectedFragment = new SuperAdminDashboardFragment();
            } else if (itemId == R.id.nav_super_rooms) {
                selectedFragment = new SuperAdminRoomsFragment();
            } else if (itemId == R.id.nav_super_history) {
                selectedFragment = new SuperAdminHistoryFragment();
            } else if (itemId == R.id.nav_super_profile) {
                selectedFragment = new SuperAdminProfileFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });
    }

    // Swaps displayed super admin fragment with a smooth fade animation
    public void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.superAdminFragmentContainer, fragment)
                .commit();
    }
}