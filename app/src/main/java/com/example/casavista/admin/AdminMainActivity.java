package com.example.casavista.admin;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.casavista.R;
import com.example.casavista.admin.fragments.AdminBookingsFragment;
import com.example.casavista.admin.fragments.AdminDashboardFragment;
import com.example.casavista.admin.fragments.AdminPosFragment;
import com.example.casavista.admin.fragments.AdminProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

// Main Admin container activity managing bottom navigation tabs for resort staff
public class AdminMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        BottomNavigationView bottomNav = findViewById(R.id.adminBottomNavigationView);

        // Handle bottom navigation tab selection
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();
            if (id == R.id.admin_nav_home) selectedFragment = new AdminDashboardFragment();
            else if (id == R.id.admin_nav_bookings) selectedFragment = new AdminBookingsFragment();
            else if (id == R.id.admin_nav_pos) selectedFragment = new AdminPosFragment();
            else if (id == R.id.admin_nav_profile) selectedFragment = new AdminProfileFragment();

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });

        // Load default Admin Dashboard on initial startup
        if (savedInstanceState == null) {
            loadFragment(new AdminDashboardFragment());
        }
    }

    // Swaps displayed admin fragment with a smooth fade animation
    public void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.adminFragmentContainer, fragment)
                .commit();
    }
}