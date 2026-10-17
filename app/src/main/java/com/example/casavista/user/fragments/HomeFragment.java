package com.example.casavista.user.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.casavista.R;
import com.example.casavista.adapters.AnnouncementAdapter;
import com.example.casavista.data.ResortRepository;
import com.example.casavista.helpers.AdminAnnouncementHelper;
import com.example.casavista.models.Announcement;
import com.example.casavista.models.Booking;
import com.example.casavista.user.MainActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.io.File;
import java.util.List;

// Main Guest User Home Dashboard managing profile header, upcoming booking card, quick book tiles, and announcements
public class HomeFragment extends Fragment {

    private static final String PREFS_NAME = "CasavistaProfilePrefs";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_FIRST_NAME = "user_first_name";
    private static final String PROFILE_PIC_FILE_NAME = "profile_pic.jpg";

    private TextView tvHomeGreeting;
    private TextView tvHomeAvatarInitials;
    private ImageView imgHomeAvatar;

    // Upcoming Booking Card Views
    private MaterialCardView cardHomeUpcomingBooking;
    private ImageView imgHomeUpcomingBg;
    private TextView tvHomeUpcomingTitle, tvHomeUpcomingStatus, tvHomeUpcomingDate, tvHomeUpcomingDetail1;
    private ImageView btnHomeUpcomingPrev, btnHomeUpcomingNext;
    private TextView tvHomeUpcomingIndex;
    private View layoutHomeUpcomingNav;

    private int currentUpcomingIndex = 0;

    // Swipable Announcements Views
    private ViewPager2 viewPagerAnnouncements;
    private TabLayout tabLayoutIndicator;

    // Auto-slide banner Handler
    private final Handler sliderHandler = new Handler(Looper.getMainLooper());
    private final Runnable sliderRunnable = new Runnable() {
        @Override
        public void run() {
            if (viewPagerAnnouncements != null && viewPagerAnnouncements.getAdapter() != null) {
                int totalItems = viewPagerAnnouncements.getAdapter().getItemCount();
                if (totalItems > 1) {
                    int nextItem = (viewPagerAnnouncements.getCurrentItem() + 1) % totalItems;
                    viewPagerAnnouncements.setCurrentItem(nextItem, true);
                    sliderHandler.postDelayed(this, 4000); // Auto-slide every 4 seconds
                }
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvHomeGreeting = view.findViewById(R.id.tvHomeGreeting);
        tvHomeAvatarInitials = view.findViewById(R.id.tvHomeAvatarInitials);
        imgHomeAvatar = view.findViewById(R.id.imgHomeAvatar);

        // Upcoming Booking Card Bindings
        cardHomeUpcomingBooking = view.findViewById(R.id.cardHomeUpcomingBooking);
        imgHomeUpcomingBg = view.findViewById(R.id.imgHomeUpcomingBg);
        tvHomeUpcomingTitle = view.findViewById(R.id.tvHomeUpcomingTitle);
        tvHomeUpcomingStatus = view.findViewById(R.id.tvHomeUpcomingStatus);
        tvHomeUpcomingDate = view.findViewById(R.id.tvHomeUpcomingDate);
        tvHomeUpcomingDetail1 = view.findViewById(R.id.tvHomeUpcomingDetail1);

        btnHomeUpcomingPrev = view.findViewById(R.id.btnHomeUpcomingPrev);
        btnHomeUpcomingNext = view.findViewById(R.id.btnHomeUpcomingNext);
        tvHomeUpcomingIndex = view.findViewById(R.id.tvHomeUpcomingIndex);
        layoutHomeUpcomingNav = view.findViewById(R.id.layoutHomeUpcomingNav);

        // Swipable Carousel Views
        viewPagerAnnouncements = view.findViewById(R.id.viewPagerAnnouncements);
        tabLayoutIndicator = view.findViewById(R.id.tabLayoutIndicator);

        // Add smooth scale & fade page transformer to ViewPager2
        if (viewPagerAnnouncements != null) {
            viewPagerAnnouncements.setPageTransformer((page, position) -> {
                float absPos = Math.abs(position);
                page.setAlpha(1.0f - (absPos * 0.25f));
                page.setScaleY(0.92f + (1 - absPos) * 0.08f);
            });
        }

        // Bind Top Avatar Click to Profile Fragment
        View cardAvatar = view.findViewById(R.id.cardHomeAvatar);
        if (cardAvatar != null) {
            cardAvatar.setOnClickListener(v -> openUserProfile());
        }

        // Quick Book Buttons
        view.findViewById(R.id.btnHomeQuickRoom).setOnClickListener(v -> openCatalogCategory("Room"));
        view.findViewById(R.id.btnHomeQuickPickleball).setOnClickListener(v -> openCatalogCategory("Pickleball"));
        view.findViewById(R.id.btnHomeQuickBasketball).setOnClickListener(v -> openCatalogCategory("Basketball"));
        view.findViewById(R.id.btnHomeQuickBuyout).setOnClickListener(v -> openCatalogCategory("Buyout"));

        updateUpcomingBookingCard();
    }

    @Override
    public void onResume() {
        super.onResume();
        syncUserProfileHeader();
        updateUpcomingBookingCard();
        loadAnnouncementBanner();
        startAutoSlide();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopAutoSlide();
    }

    // Dynamically loads active/upcoming reservations into the dashboard card with arrow navigation
    private void updateUpcomingBookingCard() {
        if (getContext() == null || cardHomeUpcomingBooking == null) return;

        ResortRepository repo = ResortRepository.getInstance();
        repo.loadSavedBookings(requireContext());

        List<Booking> upcomingBookings = repo.getBookings("Upcoming");

        if (!upcomingBookings.isEmpty()) {
            if (currentUpcomingIndex >= upcomingBookings.size()) {
                currentUpcomingIndex = 0;
            }

            if (upcomingBookings.size() > 1) {
                if (layoutHomeUpcomingNav != null) layoutHomeUpcomingNav.setVisibility(View.VISIBLE);
                if (tvHomeUpcomingIndex != null) tvHomeUpcomingIndex.setText((currentUpcomingIndex + 1) + "/" + upcomingBookings.size());
            } else {
                if (layoutHomeUpcomingNav != null) layoutHomeUpcomingNav.setVisibility(View.GONE);
            }

            Booking booking = upcomingBookings.get(currentUpcomingIndex);
            String titleLower = booking.getTitle() != null ? booking.getTitle().toLowerCase() : "";

            if (imgHomeUpcomingBg != null) {
                boolean loaded = false;
                String imgPath = booking.getImagePath();

                if (imgPath != null && !imgPath.trim().isEmpty()) {
                    try {
                        File imgFile = new File(imgPath);
                        if (imgFile.exists()) {
                            Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                            if (bitmap != null) {
                                imgHomeUpcomingBg.setImageBitmap(bitmap);
                                loaded = true;
                            }
                        } else if (imgPath.startsWith("content://")) {
                            imgHomeUpcomingBg.setImageURI(android.net.Uri.parse(imgPath));
                            loaded = true;
                        }
                    } catch (Exception ignored) {}
                }

                if (!loaded && booking.getImageResId() != 0) {
                    imgHomeUpcomingBg.setImageResource(booking.getImageResId());
                    loaded = true;
                }

                if (!loaded) {
                    if (titleLower.contains("pickleball")) {
                        imgHomeUpcomingBg.setImageResource(R.drawable.pickleball_icon_img);
                    } else if (titleLower.contains("basketball")) {
                        imgHomeUpcomingBg.setImageResource(R.drawable.basketball_icon_img);
                    } else if (titleLower.contains("room") || titleLower.contains("suite") || titleLower.contains("villa") || titleLower.contains("loft")) {
                        imgHomeUpcomingBg.setImageResource(R.drawable.rooms_icon_img);
                    } else {
                        imgHomeUpcomingBg.setImageResource(R.drawable.resort_icon_img);
                    }
                }
            }

            if (tvHomeUpcomingTitle != null) tvHomeUpcomingTitle.setText(booking.getTitle());
            if (tvHomeUpcomingStatus != null) tvHomeUpcomingStatus.setText(booking.getStatus());
            if (tvHomeUpcomingDate != null) tvHomeUpcomingDate.setText(booking.getDate());
            if (tvHomeUpcomingDetail1 != null) tvHomeUpcomingDetail1.setText(booking.getDetail1());

            cardHomeUpcomingBooking.setOnClickListener(v -> showBookingDetailsDialog(booking));

            if (btnHomeUpcomingNext != null) {
                btnHomeUpcomingNext.setOnClickListener(v -> {
                    currentUpcomingIndex = (currentUpcomingIndex + 1) % upcomingBookings.size();
                    animateUpcomingCardSwitch();
                });
            }

            if (btnHomeUpcomingPrev != null) {
                btnHomeUpcomingPrev.setOnClickListener(v -> {
                    currentUpcomingIndex = (currentUpcomingIndex - 1 + upcomingBookings.size()) % upcomingBookings.size();
                    animateUpcomingCardSwitch();
                });
            }

        } else {
            if (layoutHomeUpcomingNav != null) layoutHomeUpcomingNav.setVisibility(View.GONE);
            if (imgHomeUpcomingBg != null) {
                imgHomeUpcomingBg.setImageResource(R.drawable.resort_icon_img);
            }
            if (tvHomeUpcomingTitle != null) tvHomeUpcomingTitle.setText("No Upcoming Bookings");
            if (tvHomeUpcomingStatus != null) tvHomeUpcomingStatus.setText("Explore");
            if (tvHomeUpcomingDate != null) tvHomeUpcomingDate.setText("Book Now ➔");
            if (tvHomeUpcomingDetail1 != null) tvHomeUpcomingDetail1.setText("Rooms & Courts Available");

            cardHomeUpcomingBooking.setOnClickListener(v -> openCatalogCategory("All"));
        }
    }

    private void animateUpcomingCardSwitch() {
        if (cardHomeUpcomingBooking != null) {
            cardHomeUpcomingBooking.setAlpha(0.4f);
            updateUpcomingBookingCard();
            cardHomeUpcomingBooking.animate().alpha(1.0f).setDuration(180).start();
        }
    }

    // Opens full reservation details dialog when tapping the upcoming booking card
    private void showBookingDetailsDialog(Booking booking) {
        if (getContext() == null || booking == null) return;

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_booking_details, null);
        ImageView imgHeader = dialogView.findViewById(R.id.dialogImgDetailHeader);
        TextView tvTitle = dialogView.findViewById(R.id.dialogDetailTitle);
        TextView tvStatus = dialogView.findViewById(R.id.dialogDetailStatusBadge);
        TextView tvDate = dialogView.findViewById(R.id.dialogDetailDate);
        TextView tvDetail1 = dialogView.findViewById(R.id.dialogDetail1);
        TextView tvDetail2 = dialogView.findViewById(R.id.dialogDetail2);
        TextView tvPrice = dialogView.findViewById(R.id.dialogDetailPrice);

        if (imgHeader != null) {
            boolean loaded = false;
            String imgPath = booking.getImagePath();

            if (imgPath != null && !imgPath.trim().isEmpty()) {
                try {
                    File imgFile = new File(imgPath);
                    if (imgFile.exists()) {
                        Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                        if (bitmap != null) {
                            imgHeader.setImageBitmap(bitmap);
                            loaded = true;
                        }
                    } else if (imgPath.startsWith("content://")) {
                        imgHeader.setImageURI(android.net.Uri.parse(imgPath));
                        loaded = true;
                    }
                } catch (Exception ignored) {}
            }

            if (!loaded && booking.getImageResId() != 0) {
                imgHeader.setImageResource(booking.getImageResId());
                loaded = true;
            }

            if (!loaded) {
                String titleLower = booking.getTitle() != null ? booking.getTitle().toLowerCase() : "";
                if (titleLower.contains("pickleball")) {
                    imgHeader.setImageResource(R.drawable.pickleball_icon_img);
                } else if (titleLower.contains("basketball")) {
                    imgHeader.setImageResource(R.drawable.basketball_icon_img);
                } else if (titleLower.contains("room") || titleLower.contains("suite") || titleLower.contains("villa") || titleLower.contains("loft")) {
                    imgHeader.setImageResource(R.drawable.rooms_icon_img);
                } else {
                    imgHeader.setImageResource(R.drawable.resort_icon_img);
                }
            }
        }

        if (tvTitle != null) tvTitle.setText(booking.getTitle());
        if (tvStatus != null) tvStatus.setText(booking.getStatus());
        if (tvDate != null) tvDate.setText(booking.getDate());
        if (tvDetail1 != null) tvDetail1.setText(booking.getDetail1());
        if (tvDetail2 != null) tvDetail2.setText(booking.getDetail2());
        if (tvPrice != null) tvPrice.setText(booking.getPrice());

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Booking Information")
                .setView(dialogView)
                .setPositiveButton("View All Bookings", (dialog, which) -> {
                    MainActivity mainActivity = (MainActivity) getActivity();
                    if (mainActivity != null) {
                        mainActivity.loadFragment(new BookingsFragment());
                    }
                })
                .setNegativeButton("Close", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void startAutoSlide() {
        stopAutoSlide();
        sliderHandler.postDelayed(sliderRunnable, 4000);
    }

    private void stopAutoSlide() {
        sliderHandler.removeCallbacks(sliderRunnable);
    }

    private void loadAnnouncementBanner() {
        if (getContext() == null || viewPagerAnnouncements == null || tabLayoutIndicator == null) return;

        List<Announcement> announcements = AdminAnnouncementHelper.getAnnouncements(requireContext());

        if (announcements.isEmpty()) {
            viewPagerAnnouncements.setVisibility(View.GONE);
            tabLayoutIndicator.setVisibility(View.GONE);
        } else {
            viewPagerAnnouncements.setVisibility(View.VISIBLE);
            tabLayoutIndicator.setVisibility(View.VISIBLE);

            AnnouncementAdapter adapter = new AnnouncementAdapter(announcements);
            viewPagerAnnouncements.setAdapter(adapter);

            new TabLayoutMediator(tabLayoutIndicator, viewPagerAnnouncements, (tab, position) -> {
                // Dots update automatically
            }).attach();
        }
    }

    private void syncUserProfileHeader() {
        if (getContext() == null) return;

        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        String firstName = prefs.getString(KEY_FIRST_NAME, "TEST12345");
        String fullName = prefs.getString(KEY_USER_NAME, "Test12345 User").trim();

        if (firstName.isEmpty() && !fullName.isEmpty()) {
            firstName = fullName.split("\\s+")[0];
        }

        if (tvHomeGreeting != null) {
            tvHomeGreeting.setText("KUMUSTA, " + firstName.toUpperCase() + "!");
        }

        if (tvHomeAvatarInitials != null) {
            tvHomeAvatarInitials.setText(getInitials(fullName));
        }

        String email = prefs.getString("user_email", "");
        String emailCleanKey = email.toLowerCase().replaceAll("[^a-zA-Z0-9]", "_");

        File file = new File(requireContext().getFilesDir(), "profile_pic_" + emailCleanKey + ".jpg");
        if (!file.exists()) {
            file = new File(requireContext().getFilesDir(), PROFILE_PIC_FILE_NAME);
        }
        if (file.exists() && imgHomeAvatar != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            if (bitmap != null) {
                imgHomeAvatar.setImageBitmap(bitmap);
                imgHomeAvatar.setVisibility(View.VISIBLE);
                if (tvHomeAvatarInitials != null) {
                    tvHomeAvatarInitials.setVisibility(View.GONE);
                }
                return;
            }
        }

        if (imgHomeAvatar != null) {
            imgHomeAvatar.setVisibility(View.GONE);
        }
        if (tvHomeAvatarInitials != null) {
            tvHomeAvatarInitials.setVisibility(View.VISIBLE);
        }
    }

    private String getInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "CV";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
        } else if (parts.length == 1 && !parts[0].isEmpty()) {
            return ("" + parts[0].charAt(0)).toUpperCase();
        }
        return "CV";
    }

    private void openUserProfile() {
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity != null) {
            mainActivity.loadFragment(new ProfileFragment());
            com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = mainActivity.findViewById(R.id.bottomNavigationView);
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(R.id.nav_profile);
            }
        }
    }

    private void openCatalogCategory(String category) {
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity != null) {
            CatalogFragment catalogFragment = new CatalogFragment();

            Bundle args = new Bundle();
            args.putString("selected_category", category);
            catalogFragment.setArguments(args);

            mainActivity.loadFragment(catalogFragment);
        }
    }
}