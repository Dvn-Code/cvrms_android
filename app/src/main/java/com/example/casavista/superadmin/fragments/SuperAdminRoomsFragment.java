package com.example.casavista.superadmin.fragments;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.casavista.R;
import com.example.casavista.adapters.SuperAdminRoomAdapter;
import com.example.casavista.data.ResortRepository;
import com.example.casavista.helpers.AvatarHelper;
import com.example.casavista.models.CatalogItem;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

// Super Admin fragment for managing room and court availability status across the resort
public class SuperAdminRoomsFragment extends Fragment implements SuperAdminRoomAdapter.OnRoomStatusChangeListener {

    private RecyclerView rvRooms;
    private MaterialButton tabAll, tabVillas, tabCourts, tabBuyout;
    private ResortRepository repository;
    private SuperAdminRoomAdapter adapter;
    private String currentCategory = "All";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_super_admin_rooms, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = ResortRepository.getInstance();

        // Bind RecyclerView and layout manager
        rvRooms = view.findViewById(R.id.rvSuperAdminRooms);
        if (rvRooms != null) {
            rvRooms.setLayoutManager(new LinearLayoutManager(getContext()));
        }

        // Bind category filter tabs
        tabAll = view.findViewById(R.id.tabSuperRoomAll);
        tabVillas = view.findViewById(R.id.tabSuperRoomVillas);
        tabCourts = view.findViewById(R.id.tabSuperRoomCourts);
        tabBuyout = view.findViewById(R.id.tabSuperRoomBuyout);

        // Tab click triggers
        if (tabAll != null) tabAll.setOnClickListener(v -> filterCategory("All"));
        if (tabVillas != null) tabVillas.setOnClickListener(v -> filterCategory("Room"));
        if (tabCourts != null) tabCourts.setOnClickListener(v -> filterCategory("Courts"));
        if (tabBuyout != null) tabBuyout.setOnClickListener(v -> filterCategory("Buyout"));

        View cardAvatar = view.findViewById(R.id.cardSuperAdminRoomsAvatar);
        if (cardAvatar != null) {
            cardAvatar.setOnClickListener(v -> {
                com.example.casavista.superadmin.SuperAdminMainActivity superActivity = (com.example.casavista.superadmin.SuperAdminMainActivity) getActivity();
                if (superActivity != null) {
                    superActivity.loadFragment(new SuperAdminProfileFragment());
                    com.google.android.material.bottomnavigation.BottomNavigationView navView = superActivity.findViewById(R.id.superAdminNavView);
                    if (navView != null) {
                        navView.setSelectedItemId(R.id.nav_super_profile);
                    }
                }
            });
        }

        loadRoomsList();
    }

    // Filters the displayed room list based on selected category tab
    private void filterCategory(String category) {
        this.currentCategory = category;
        highlightTabs(category);
        loadRoomsList();
    }

    // Highlights the active category tab
    private void highlightTabs(String category) {
        MaterialButton[] tabs = {tabAll, tabVillas, tabCourts, tabBuyout};
        String[] catNames = {"All", "Room", "Courts", "Buyout"};

        for (int i = 0; i < tabs.length; i++) {
            if (tabs[i] != null) {
                if (catNames[i].equalsIgnoreCase(category) || (category.equalsIgnoreCase("Room") && catNames[i].equalsIgnoreCase("Room"))) {
                    tabs[i].setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4D391A")));
                    tabs[i].setTextColor(Color.WHITE);
                } else {
                    tabs[i].setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EADFD3")));
                    tabs[i].setTextColor(Color.parseColor("#59493B"));
                }
            }
        }
    }

    // Fetches rooms and facilities from repository and attaches adapter
    private void loadRoomsList() {
        if (rvRooms == null || repository == null || getContext() == null) return;

        repository.loadSavedImages(requireContext());
        List<CatalogItem> allItems = repository.getCatalogItems();

        List<CatalogItem> filtered = new ArrayList<>();
        for (CatalogItem item : allItems) {
            String cat = item.getCategory() != null ? item.getCategory() : "";
            if ("All".equalsIgnoreCase(currentCategory)) {
                filtered.add(item);
            } else if ("Room".equalsIgnoreCase(currentCategory) && (cat.equalsIgnoreCase("Room") || cat.equalsIgnoreCase("Rooms"))) {
                filtered.add(item);
            } else if ("Courts".equalsIgnoreCase(currentCategory) && (cat.equalsIgnoreCase("Pickleball") || cat.equalsIgnoreCase("Basketball"))) {
                filtered.add(item);
            } else if ("Buyout".equalsIgnoreCase(currentCategory) && cat.equalsIgnoreCase("Buyout")) {
                filtered.add(item);
            }
        }

        if (adapter == null) {
            adapter = new SuperAdminRoomAdapter(filtered, this);
            rvRooms.setAdapter(adapter);
        } else {
            adapter.updateList(filtered);
        }
    }

    @Override
    public void onRoomStatusChanged(CatalogItem item, boolean isBooked) {
        // Status updates persist directly in ResortRepository
    }

    @Override
    public void onResume() {
        super.onResume();
        loadHeaderAvatar();
        loadRoomsList();
    }

    // Loads saved Super Admin profile image into header avatar circle
    private void loadHeaderAvatar() {
        if (getView() == null || getContext() == null) return;
        ImageView imgAvatar = getView().findViewById(R.id.imgHeaderAvatarRooms);
        if (imgAvatar == null) return;

        File avatarFile = new File(requireContext().getFilesDir(), "super_admin_avatar.jpg");
        if (avatarFile.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(avatarFile.getAbsolutePath());
            if (bitmap != null) {
                imgAvatar.setImageBitmap(bitmap);
                return;
            }
        }

        AvatarHelper.loadAvatar(getContext(), imgAvatar, null, "Juan Dela Cruz");
    }
}