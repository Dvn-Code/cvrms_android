package com.example.casavista.admin;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.casavista.R;
import com.example.casavista.admin.fragments.AdminBookingsFragment;
import com.example.casavista.admin.fragments.AdminDashboardFragment;
import com.example.casavista.admin.fragments.AdminPosFragment;
import com.example.casavista.admin.fragments.AdminProfileFragment;
import com.example.casavista.data.ResortRepository;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AdminMainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private String currentStaffRole = "Admin";
    private String currentStaffId = "STF-001";
    private String currentStaffEmail = "admin@casavista.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        loadStaffSession();

        bottomNav = findViewById(R.id.adminBottomNavigationView);
        configureRoleNavigation();

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.admin_nav_home) {
                selectedFragment = new AdminDashboardFragment();
            } else if (id == R.id.admin_nav_bookings) {
                selectedFragment = new AdminBookingsFragment();
            } else if (id == R.id.admin_nav_pos) {
                if ("Front Desk".equalsIgnoreCase(currentStaffRole)) {
                    Toast.makeText(this, "POS terminal is restricted to Cashier, Manager, and Admin roles.", Toast.LENGTH_SHORT).show();
                    return false;
                }
                selectedFragment = new AdminPosFragment();
            } else if (id == R.id.admin_nav_profile) {
                selectedFragment = new AdminProfileFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            if ("Cashier".equalsIgnoreCase(currentStaffRole)) {
                bottomNav.setSelectedItemId(R.id.admin_nav_pos);
                loadFragment(new AdminPosFragment());
            } else {
                bottomNav.setSelectedItemId(R.id.admin_nav_home);
                loadFragment(new AdminDashboardFragment());
            }
        }
    }

    private void loadStaffSession() {
        SharedPreferences adminPrefs = getSharedPreferences("AdminPrefs", Context.MODE_PRIVATE);
        String savedEmail = adminPrefs.getString("staff_email", "");
        String savedRole = adminPrefs.getString("staff_role", "");
        String savedId = adminPrefs.getString("staff_id", "");
        String savedName = adminPrefs.getString("staff_name", "");

        String repoEmail = ResortRepository.getInstance().getCurrentUserEmail();
        if ((savedEmail == null || savedEmail.isEmpty()) && repoEmail != null && !repoEmail.isEmpty()) {
            savedEmail = repoEmail;
        }

        if (savedEmail == null || savedEmail.isEmpty()) {
            savedEmail = "admin@casavista.com";
        }
        currentStaffEmail = savedEmail.toLowerCase().trim();

        if (savedRole == null || savedRole.isEmpty()) {
            if (currentStaffEmail.startsWith("manager")) {
                currentStaffRole = "Manager";
                currentStaffId = "STF-002";
                savedName = "Maria Clara Santos";
            } else if (currentStaffEmail.startsWith("frontdesk")) {
                currentStaffRole = "Front Desk";
                currentStaffId = "STF-003";
                savedName = "Mark Anthony Reyes";
            } else if (currentStaffEmail.startsWith("cashier")) {
                currentStaffRole = "Cashier";
                currentStaffId = "STF-004";
                savedName = " Angela Mae Lim";
            } else {
                currentStaffRole = "Admin";
                currentStaffId = "STF-001";
                savedName = "Juan Dela Cruz";
            }
            adminPrefs.edit()
                    .putString("staff_email", currentStaffEmail)
                    .putString("staff_role", currentStaffRole)
                    .putString("staff_id", currentStaffId)
                    .putString("staff_name", savedName.trim())
                    .apply();
        } else {
            currentStaffRole = savedRole;
            currentStaffId = (savedId != null && !savedId.isEmpty()) ? savedId : "STF-001";
        }
    }

    private void configureRoleNavigation() {
        if (bottomNav == null) return;
        Menu menu = bottomNav.getMenu();
        if (menu == null) return;

        MenuItem posItem = menu.findItem(R.id.admin_nav_pos);
        if (posItem != null) {
            // Front Desk focuses on Guest Check-In/Out and Room Statuses; POS is hidden for them
            posItem.setVisible(!"Front Desk".equalsIgnoreCase(currentStaffRole));
        }
    }

    public String getCurrentStaffRole() {
        return currentStaffRole;
    }

    public String getCurrentStaffId() {
        return currentStaffId;
    }

    public String getCurrentStaffEmail() {
        return currentStaffEmail;
    }

    public void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.adminFragmentContainer, fragment)
                .commit();
    }
}