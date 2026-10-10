package com.example.casavista.user.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.casavista.R;
import com.example.casavista.adapters.CatalogAdapter;
import com.example.casavista.data.ResortRepository;
import com.example.casavista.models.CatalogItem;
import com.example.casavista.user.MainActivity;
import com.google.android.material.button.MaterialButton;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class CatalogFragment extends Fragment {

    private ResortRepository repository;
    private RecyclerView rvCatalog;
    private MaterialButton tabCatAll, tabCatRooms, tabCatPickleball, tabCatBasketball, tabCatBuyout;
    private String currentCategory = "All";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_catalog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = ResortRepository.getInstance();

        rvCatalog = view.findViewById(R.id.rvCatalogList);
        rvCatalog.setLayoutManager(new LinearLayoutManager(requireContext()));

        tabCatAll = view.findViewById(R.id.tabCatAll);
        tabCatRooms = view.findViewById(R.id.tabCatRooms);
        tabCatPickleball = view.findViewById(R.id.tabCatPickleball);
        tabCatBasketball = view.findViewById(R.id.tabCatBasketball);
        tabCatBuyout = view.findViewById(R.id.tabCatBuyout);

        tabCatAll.setOnClickListener(v -> {
            animateSelection(v);
            filterCatalog("All");
        });
        tabCatRooms.setOnClickListener(v -> {
            animateSelection(v);
            filterCatalog("Room");
        });
        tabCatPickleball.setOnClickListener(v -> {
            animateSelection(v);
            filterCatalog("Pickleball");
        });
        tabCatBasketball.setOnClickListener(v -> {
            animateSelection(v);
            filterCatalog("Basketball");
        });
        tabCatBuyout.setOnClickListener(v -> {
            animateSelection(v);
            filterCatalog("Buyout");
        });

        String initialCategory = "All";
        if (getArguments() != null && getArguments().containsKey("selected_category")) {
            initialCategory = getArguments().getString("selected_category", "All");
        }

        View cardAvatar = view.findViewById(R.id.cardCatalogAvatar);
        if (cardAvatar != null) {
            cardAvatar.setOnClickListener(v -> {
                MainActivity mainActivity = (MainActivity) getActivity();
                if (mainActivity != null) {
                    mainActivity.loadFragment(new ProfileFragment());
                    com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = mainActivity.findViewById(R.id.bottomNavigationView);
                    if (bottomNav != null) {
                        bottomNav.setSelectedItemId(R.id.nav_profile);
                    }
                }
            });
        }

        filterCatalog(initialCategory);
        loadHeaderProfile(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadHeaderProfile(getView());
        filterCatalog(currentCategory);
    }

    private void loadHeaderProfile(View view) {
        if (view == null || getContext() == null) return;

        ImageView imgAvatar = view.findViewById(R.id.imgHomeAvatar);
        TextView tvInitials = view.findViewById(R.id.tvHomeAvatarInitials);
        TextView tvGreeting = view.findViewById(R.id.tvHomeGreeting);

        if (imgAvatar == null || tvInitials == null) return;

        SharedPreferences prefs = requireContext().getSharedPreferences("CasavistaProfilePrefs", Context.MODE_PRIVATE);
        String name = prefs.getString("user_name", "John Lucero Cabilao");
        String firstName = prefs.getString("user_first_name", "");

        if (tvGreeting != null) {
            if (!firstName.isEmpty()) {
                tvGreeting.setText("KUMUSTA, " + firstName.toUpperCase() + "!");
            } else if (name != null && !name.trim().isEmpty()) {
                String[] parts = name.trim().split("\\s+");
                tvGreeting.setText("KUMUSTA, " + parts[0].toUpperCase() + "!");
            }
        }

        File file = new File(requireContext().getFilesDir(), "profile_pic.jpg");
        if (file.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            if (bitmap != null) {
                imgAvatar.setImageBitmap(bitmap);
                imgAvatar.setVisibility(View.VISIBLE);
                tvInitials.setVisibility(View.GONE);
                return;
            }
        }

        imgAvatar.setVisibility(View.GONE);
        tvInitials.setVisibility(View.VISIBLE);
        if (name != null && !name.trim().isEmpty()) {
            String[] parts = name.trim().split("\\s+");
            if (parts.length >= 2) {
                tvInitials.setText(("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase());
            } else {
                tvInitials.setText(("" + parts[0].charAt(0)).toUpperCase());
            }
        } else {
            tvInitials.setText("CV");
        }
    }

    public void filterCatalog(String category) {
        this.currentCategory = category;
        highlightCatalogTab(category);

        repository.loadSavedBookings(requireContext());
        repository.loadSavedImages(requireContext());

        List<CatalogItem> items = repository.getCatalogByCategory(category);
        CatalogAdapter adapter = new CatalogAdapter(items, item -> {
            MainActivity mainActivity = (MainActivity) getActivity();
            if (item.getCategory().equalsIgnoreCase("Pickleball") || item.getCategory().equalsIgnoreCase("Basketball")) {
                if (mainActivity != null) {
                    mainActivity.loadFragment(new CourtFragment());
                }
            } else {
                showReservationDialog(item);
            }
        });

        rvCatalog.setAlpha(0f);
        rvCatalog.setAdapter(adapter);
        rvCatalog.animate().alpha(1f).setDuration(220).start();
    }

    private void showReservationDialog(CatalogItem item) {
        if (getContext() == null || item == null) return;

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_reserve_facility, null);

        ImageView imgPreview = dialogView.findViewById(R.id.dialogReserveImg);
        TextView tvTitle = dialogView.findViewById(R.id.dialogReserveTitle);
        TextView tvSubtitle = dialogView.findViewById(R.id.dialogReserveSubtitle);
        TextView tvCapacity = dialogView.findViewById(R.id.dialogReserveCapacity);
        TextView tvPrice = dialogView.findViewById(R.id.dialogReservePrice);
        TextView tvBadge = dialogView.findViewById(R.id.dialogReserveBadge);

        View cardDatePicker = dialogView.findViewById(R.id.cardDatePicker);
        TextView tvSelectedDate = dialogView.findViewById(R.id.tvSelectedDateText);
        Spinner spinnerPayment = dialogView.findViewById(R.id.spinnerPaymentMethod);

        if (tvTitle != null) tvTitle.setText(item.getTitle());
        if (tvSubtitle != null) tvSubtitle.setText(item.getSubtitle());
        if (tvCapacity != null) tvCapacity.setText("Capacity: " + item.getCapacity());
        if (tvPrice != null) tvPrice.setText(item.getPrice());

        if (tvBadge != null) {
            if (item.getBadge() != null && !item.getBadge().isEmpty()) {
                tvBadge.setText(item.getBadge());
                tvBadge.setVisibility(View.VISIBLE);
            } else {
                tvBadge.setVisibility(View.GONE);
            }
        }

        // Image loading
        if (imgPreview != null) {
            String imageUrl = item.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                try {
                    File imgFile = new File(imageUrl);
                    if (imgFile.exists()) {
                        imgPreview.setImageBitmap(BitmapFactory.decodeFile(imgFile.getAbsolutePath()));
                    } else if (imageUrl.startsWith("content://")) {
                        imgPreview.setImageURI(Uri.parse(imageUrl));
                    }
                } catch (Exception ignored) {}
            } else if (item.getImageResId() != 0) {
                imgPreview.setImageResource(item.getImageResId());
            }
        }

        // Default Date (Real Current Date)
        final Calendar selectedCal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM dd, yyyy", Locale.US);
        if (tvSelectedDate != null) {
            tvSelectedDate.setText(sdf.format(selectedCal.getTime()));
        }

        // Date Picker Trigger (Disables past dates)
        if (cardDatePicker != null) {
            cardDatePicker.setOnClickListener(v -> {
                CalendarConstraints constraints = new CalendarConstraints.Builder()
                        .setValidator(DateValidatorPointForward.now())
                        .build();

                MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                        .setTitleText("Select Reservation Date")
                        .setCalendarConstraints(constraints)
                        .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                        .build();

                datePicker.addOnPositiveButtonClickListener(selection -> {
                    SimpleDateFormat fmt = new SimpleDateFormat("EEE, MMM dd, yyyy", Locale.US);
                    fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
                    if (tvSelectedDate != null) {
                        tvSelectedDate.setText(fmt.format(new Date(selection)));
                    }
                });

                datePicker.show(getParentFragmentManager(), "ROOM_RESERVE_DATE_PICKER");
            });
        }

        // Payment Options
        List<String> paymentMethods = new ArrayList<>();
        paymentMethods.add("GCash (Cashless E-Wallet)");
        paymentMethods.add("Cash at Front Desk");
        paymentMethods.add("Credit / Debit Card");
        paymentMethods.add("Bank Transfer (Online)");

        com.google.android.material.textfield.TextInputLayout layoutAccountMobile = dialogView.findViewById(R.id.layoutAccountMobile);
        com.google.android.material.textfield.TextInputEditText etAccountMobileNumber = dialogView.findViewById(R.id.etAccountMobileNumber);

        ArrayAdapter<String> paymentAdapter = new ArrayAdapter<>(requireContext(), R.layout.item_payment_spinner_selected, paymentMethods);
        paymentAdapter.setDropDownViewResource(R.layout.item_payment_spinner_dropdown);
        if (spinnerPayment != null) {
            spinnerPayment.setAdapter(paymentAdapter);

            spinnerPayment.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    String selected = paymentAdapter.getItem(position);
                    if (selected != null && (selected.contains("GCash") || selected.contains("Card") || selected.contains("Bank"))) {
                        if (layoutAccountMobile != null) {
                            layoutAccountMobile.setVisibility(View.VISIBLE);
                            if (selected.contains("GCash")) {
                                layoutAccountMobile.setHint("GCash / Mobile Number");
                            } else if (selected.contains("Card")) {
                                layoutAccountMobile.setHint("Cardholder Mobile / Account Number");
                            } else {
                                layoutAccountMobile.setHint("Bank Account / Ref Mobile Number");
                            }
                        }
                    } else {
                        if (layoutAccountMobile != null) {
                            layoutAccountMobile.setVisibility(View.GONE);
                        }
                    }
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Reserve " + item.getTitle())
                .setView(dialogView)
                .setPositiveButton("Confirm Reservation", (dialog, which) -> {
                    String chosenDate = tvSelectedDate != null ? tvSelectedDate.getText().toString() : "Sep 28, 2026";
                    String chosenPayment = spinnerPayment != null ? spinnerPayment.getSelectedItem().toString() : "GCash (Cashless E-Wallet)";
                    String mobileNum = etAccountMobileNumber != null ? etAccountMobileNumber.getText().toString().trim() : "";

                    if (layoutAccountMobile != null && layoutAccountMobile.getVisibility() == View.VISIBLE && mobileNum.isEmpty()) {
                        Toast.makeText(requireContext(), "Please enter your account / mobile number for cashless payment.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (!mobileNum.isEmpty()) {
                        chosenPayment += " (" + mobileNum + ")";
                    }

                    SharedPreferences prefs = requireContext().getSharedPreferences("CasavistaProfilePrefs", Context.MODE_PRIVATE);
                    String customerId = prefs.getString("customer_id", "CUST-101");
                    String bookingId = "BK-" + (System.currentTimeMillis() % 1000000);

                    String itemTitle = item.getTitle() != null ? item.getTitle().toLowerCase() : "";
                    String roomId = "RM-101";
                    boolean isExclusive = false;

                    if (itemTitle.contains("102")) roomId = "RM-102";
                    else if (itemTitle.contains("103") || itemTitle.contains("family")) roomId = "RM-103";
                    else if (itemTitle.contains("buyout")) {
                        roomId = "RM-101";
                        isExclusive = true;
                    }

                    SimpleDateFormat isoFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                    String todayIso = isoFmt.format(new Date());
                    String checkInIso = todayIso;
                    String checkOutIso = todayIso;

                    try {
                        SimpleDateFormat displayFmt = new SimpleDateFormat("EEE, MMM dd, yyyy", Locale.US);
                        Date parsed = displayFmt.parse(chosenDate);
                        if (parsed != null) {
                            checkInIso = isoFmt.format(parsed);
                            Calendar nextDay = Calendar.getInstance();
                            nextDay.setTime(parsed);
                            nextDay.add(Calendar.DAY_OF_MONTH, 1);
                            checkOutIso = isoFmt.format(nextDay.getTime());
                        }
                    } catch (Exception ignored) {}

                    try {
                        org.json.JSONObject payload = new org.json.JSONObject();
                        payload.put("booking_id", bookingId);
                        payload.put("customer_id", customerId);
                        payload.put("staff_id", "STF-001");
                        payload.put("room_id", roomId);
                        payload.put("court_id", org.json.JSONObject.NULL);
                        payload.put("booking_date", todayIso);
                        payload.put("check_in_date", checkInIso);
                        payload.put("check_out_date", checkOutIso);
                        payload.put("exclusive", isExclusive);
                        payload.put("status", "Pending");

                        String finalChosenPayment = chosenPayment;
                        String finalBookingId = bookingId;

                        Toast.makeText(requireContext(), "Securing 15-minute room reservation hold...", Toast.LENGTH_SHORT).show();

                        com.example.casavista.network.SupabaseClient.getInstance().createBooking(
                                payload,
                                new com.example.casavista.network.SupabaseClient.DataCallback() {
                                    @Override
                                    public void onSuccess(String jsonArrayString) {
                                        prefs.edit().putString("last_booking_id", finalBookingId).apply();

                                        repository.addBooking(
                                                requireContext(),
                                                item.getTitle(),
                                                "Pending",
                                                chosenDate,
                                                "PAYMENT: " + finalChosenPayment,
                                                "CAPACITY: " + item.getCapacity() + " • Hold active (15-min window)",
                                                item.getPriceLabel(),
                                                item.getImageUrl(),
                                                item.getImageResId()
                                        );

                                        double roomPrice = 2500.0;
                                        try {
                                            String rawPrice = item.getPriceLabel() != null ? item.getPriceLabel().replaceAll("[^0-9.]", "") : "2500";
                                            if (!rawPrice.isEmpty()) roomPrice = Double.parseDouble(rawPrice);
                                        } catch (Exception ignored) {}
                                        double finalRoomPrice = roomPrice;

                                        new MaterialAlertDialogBuilder(requireContext())
                                                .setTitle("Hold Created! ⏱️")
                                                .setMessage("Reserved! You have 15 minutes to finalize payment before this room is released.\nBooking ID: " + finalBookingId + "\n\nWould you like to pay with Maya Sandbox now?")
                                                .setPositiveButton("Pay Now with Maya", (d, w) -> {
                                                    filterCatalog(currentCategory);
                                                    com.example.casavista.dialogs.MayaTestPaymentDialog.show(
                                                            requireContext(),
                                                            finalBookingId,
                                                            finalRoomPrice,
                                                            (success, paidBookingId) -> {
                                                                MainActivity mainActivity = (MainActivity) getActivity();
                                                                if (mainActivity != null) {
                                                                    mainActivity.loadFragment(new BookingsFragment());
                                                                }
                                                            }
                                                    );
                                                })
                                                .setNeutralButton("Later (Bookings)", (d, w) -> {
                                                    filterCatalog(currentCategory);
                                                    MainActivity mainActivity = (MainActivity) getActivity();
                                                    if (mainActivity != null) {
                                                        mainActivity.loadFragment(new BookingsFragment());
                                                    }
                                                })
                                                .show();
                                    }

                                    @Override
                                    public void onError(String errorMessage) {
                                        new MaterialAlertDialogBuilder(requireContext())
                                                .setTitle("Room Reservation Issue")
                                                .setMessage("Room conflict or server error: " + errorMessage)
                                                .setPositiveButton("OK", null)
                                                .show();
                                    }
                                }
                        );

                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "Payload Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void highlightCatalogTab(String category) {
        MaterialButton[] allTabs = {tabCatAll, tabCatRooms, tabCatPickleball, tabCatBasketball, tabCatBuyout};
        String[] catNames = {"All", "Room", "Pickleball", "Basketball", "Buyout"};
        for (int i = 0; i < allTabs.length; i++) {
            if (allTabs[i] != null) {
                if (catNames[i].equalsIgnoreCase(category)) {
                    allTabs[i].setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4D391A")));
                    allTabs[i].setTextColor(Color.WHITE);
                } else {
                    allTabs[i].setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EADFD3")));
                    allTabs[i].setTextColor(Color.parseColor("#59493B"));
                }
            }
        }
    }

    private void animateSelection(View view) {
        if (view == null) return;
        view.setScaleX(0.95f);
        view.setScaleY(0.95f);
        view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
    }
}