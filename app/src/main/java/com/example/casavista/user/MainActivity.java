package com.example.casavista.user;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.casavista.R;
import com.example.casavista.user.fragments.BookingsFragment;
import com.example.casavista.user.fragments.CatalogFragment;
import com.example.casavista.user.fragments.CourtFragment;
import com.example.casavista.user.fragments.HomeFragment;
import com.example.casavista.user.fragments.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

// Main Guest User container activity managing bottom navigation tabs
public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        android.content.SharedPreferences prefs = getSharedPreferences("CasavistaProfilePrefs", MODE_PRIVATE);
        String savedCustId = prefs.getString("customer_id", "");
        String savedEmail = prefs.getString("user_email", "");
        String savedName = prefs.getString("user_name", "");
        if (!savedCustId.isEmpty() || !savedEmail.isEmpty()) {
            com.example.casavista.data.ResortRepository repo = com.example.casavista.data.ResortRepository.getInstance();
            if (!savedCustId.isEmpty()) repo.setCurrentCustomerId(savedCustId);
            if (!savedEmail.isEmpty()) repo.setCurrentUserEmail(savedEmail);
            if (!savedName.isEmpty()) repo.setCurrentUserName(savedName);
            com.example.casavista.network.SupabaseClient.setCurrentSessionCustomer(savedCustId, savedEmail);
        }

        // Load default Home Fragment on initial activity creation
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        // Handle bottom navigation tab selection
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                loadFragment(new HomeFragment());
                return true;
            } else if (itemId == R.id.nav_catalog) {
                loadFragment(new CatalogFragment());
                return true;
            } else if (itemId == R.id.nav_court) {
                loadFragment(new CourtFragment());
                return true;
            } else if (itemId == R.id.nav_bookings) {
                loadFragment(new BookingsFragment());
                return true;
            } else if (itemId == R.id.nav_profile) {
                loadFragment(new ProfileFragment());
                return true;
            }

            return false;
        });
    }

    // Swaps displayed fragment with a cross-fade transition animation
    public void loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                    .replace(R.id.fragment_container, fragment)
                    .commit();
        }
    }
}