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
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.casavista.R;
import com.example.casavista.adapters.CatalogAdapter;
import com.example.casavista.data.ResortRepository;
import com.example.casavista.models.Booking;
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
        if (view == null || !isAdded() || getContext() == null) return;

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

        String email = prefs.getString("user_email", "");
        String emailCleanKey = email.toLowerCase().replaceAll("[^a-zA-Z0-9]", "_");

        File file = new File(requireContext().getFilesDir(), "profile_pic_" + emailCleanKey + ".jpg");
        if (!file.exists()) {
            file = new File(requireContext().getFilesDir(), "profile_pic.jpg");
        }
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
        if (!isAdded() || getContext() == null || rvCatalog == null) return;
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

    private boolean isRemoteRoomOverlap(List<Booking> remoteBookings, String targetRoomId, String checkInIso, String checkOutIso) {
        Date targetIn = ResortRepository.parseAnyDate(checkInIso);
        Date targetOut = ResortRepository.parseAnyDate(checkOutIso);
        if (targetIn == null || targetOut == null) return false;

        for (Booking b : remoteBookings) {
            String bRoom = b.getRoomId() != null ? b.getRoomId() : "";
            if (bRoom.equalsIgnoreCase(targetRoomId) || "EXCLUSIVE".equalsIgnoreCase(bRoom)) {
                Date exIn = ResortRepository.parseAnyDate(b.getCheckInDate());
                Date exOut = ResortRepository.parseAnyDate(b.getCheckOutDate());
                if (exIn != null && exOut != null && exIn.before(targetOut) && exOut.after(targetIn)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void showReservationDialog(CatalogItem item) {
        if (!isAdded() || getContext() == null || item == null) return;

        final Context safeDialogContext = requireContext();
        final Context appContext = safeDialogContext.getApplicationContext();

        View dialogView = LayoutInflater.from(safeDialogContext).inflate(R.layout.dialog_reserve_facility, null);

        ImageView imgPreview = dialogView.findViewById(R.id.dialogReserveImg);
        TextView tvTitle = dialogView.findViewById(R.id.dialogReserveTitle);
        TextView tvSubtitle = dialogView.findViewById(R.id.dialogReserveSubtitle);
        TextView tvCapacity = dialogView.findViewById(R.id.dialogReserveCapacity);
        TextView tvPrice = dialogView.findViewById(R.id.dialogReservePrice);
        TextView tvBadge = dialogView.findViewById(R.id.dialogReserveBadge);

        View cardStayInDate = dialogView.findViewById(R.id.cardStayInDate);
        View cardStayOutDate = dialogView.findViewById(R.id.cardStayOutDate);
        TextView tvStayInDateText = dialogView.findViewById(R.id.tvStayInDateText);
        TextView tvStayOutDateText = dialogView.findViewById(R.id.tvStayOutDateText);
        TextView tvStayDurationBadge = dialogView.findViewById(R.id.tvStayDurationBadge);
        TextView tvPriceBreakdownText = dialogView.findViewById(R.id.tvPriceBreakdownText);
        TextView tvCalculatedTotalAmount = dialogView.findViewById(R.id.tvCalculatedTotalAmount);
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

        com.google.android.material.card.MaterialCardView cardAvailabilityStatus = dialogView.findViewById(R.id.cardAvailabilityStatus);
        TextView tvAvailabilityIcon = dialogView.findViewById(R.id.tvAvailabilityIcon);
        TextView tvAvailabilityStatusText = dialogView.findViewById(R.id.tvAvailabilityStatusText);

        final Calendar checkInCal = Calendar.getInstance();
        checkInCal.set(Calendar.HOUR_OF_DAY, 0);
        checkInCal.set(Calendar.MINUTE, 0);
        checkInCal.set(Calendar.SECOND, 0);
        checkInCal.set(Calendar.MILLISECOND, 0);

        final Calendar checkOutCal = Calendar.getInstance();
        checkOutCal.setTimeInMillis(checkInCal.getTimeInMillis());
        checkOutCal.add(Calendar.DAY_OF_MONTH, 1);

        SimpleDateFormat displaySdf = new SimpleDateFormat("EEE, MMM dd, yyyy", Locale.US);

        android.widget.RadioGroup rgPaymentPlan = dialogView.findViewById(R.id.rgPaymentPlan);
        android.widget.RadioButton rbPayFull = dialogView.findViewById(R.id.rbPayFull);
        android.widget.RadioButton rbPayHalf = dialogView.findViewById(R.id.rbPayHalf);

        String itemTitleLower = item.getTitle() != null ? item.getTitle().toLowerCase() : "";
        String targetRoomId = "RM-101";
        if (itemTitleLower.contains("102")) targetRoomId = "RM-102";
        else if (itemTitleLower.contains("103") || itemTitleLower.contains("family")) targetRoomId = "RM-103";

        final String finalTargetRoomId = targetRoomId;
        final List<Booking> remoteRoomBookings = new ArrayList<>();

        Runnable updateDateUi = () -> {
            long diffMillis = checkOutCal.getTimeInMillis() - checkInCal.getTimeInMillis();
            long nights = diffMillis / (1000 * 60 * 60 * 24);
            if (nights < 1) nights = 1;

            if (tvStayInDateText != null) tvStayInDateText.setText(displaySdf.format(checkInCal.getTime()));
            if (tvStayOutDateText != null) tvStayOutDateText.setText(displaySdf.format(checkOutCal.getTime()));
            if (tvStayDurationBadge != null) tvStayDurationBadge.setText(nights + (nights == 1 ? " Night" : " Nights"));

            double rate = 2500.0;
            try {
                String raw = item.getPriceLabel() != null ? item.getPriceLabel().replaceAll("[^0-9.]", "") : "2500";
                if (!raw.isEmpty()) rate = Double.parseDouble(raw);
            } catch (Exception ignored) {}

            double total = rate * nights;
            boolean isHalf = rbPayHalf != null && rbPayHalf.isChecked();
            double dueNow = isHalf ? (total * 0.5) : total;

            if (tvPriceBreakdownText != null) {
                tvPriceBreakdownText.setText(isHalf
                        ? "50% Downpayment Due (" + nights + (nights == 1 ? " Night" : " Nights") + "):"
                        : "Total Amount (" + nights + (nights == 1 ? " Night" : " Nights") + "):");
            }

            if (tvCalculatedTotalAmount != null) {
                tvCalculatedTotalAmount.setText(String.format(Locale.US, "₱%,.2f", dueNow));
            }

            SimpleDateFormat isoFmtTemp = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            String curInIso = isoFmtTemp.format(checkInCal.getTime());
            String curOutIso = isoFmtTemp.format(checkOutCal.getTime());

            boolean isAlreadyBooked = repository.isRoomBookedForDates(finalTargetRoomId, curInIso, curOutIso)
                    || isRemoteRoomOverlap(remoteRoomBookings, finalTargetRoomId, curInIso, curOutIso);

            if (cardAvailabilityStatus != null && tvAvailabilityIcon != null && tvAvailabilityStatusText != null) {
                if (isAlreadyBooked) {
                    cardAvailabilityStatus.setCardBackgroundColor(Color.parseColor("#25FF5252"));
                    cardAvailabilityStatus.setStrokeColor(Color.parseColor("#FF5252"));
                    tvAvailabilityIcon.setText("🔴");
                    tvAvailabilityStatusText.setText("ALREADY BOOKED FOR SELECTED DATES (" + curInIso + " to " + curOutIso + ")");
                    tvAvailabilityStatusText.setTextColor(Color.parseColor("#FF5252"));
                } else {
                    cardAvailabilityStatus.setCardBackgroundColor(Color.parseColor("#1500E676"));
                    cardAvailabilityStatus.setStrokeColor(Color.parseColor("#00E676"));
                    tvAvailabilityIcon.setText("🟢");
                    tvAvailabilityStatusText.setText("AVAILABLE FOR SELECTED DATES");
                    tvAvailabilityStatusText.setTextColor(Color.parseColor("#00E676"));
                }
            }
        };

        com.example.casavista.network.SupabaseClient.getInstance().fetchBookings(new com.example.casavista.network.SupabaseClient.DataCallback() {
            @Override
            public void onSuccess(String jsonArrayString) {
                if (jsonArrayString == null || !isAdded() || getContext() == null) return;
                try {
                    org.json.JSONArray array = new org.json.JSONArray(jsonArrayString);
                    remoteRoomBookings.clear();
                    for (int i = 0; i < array.length(); i++) {
                        org.json.JSONObject obj = array.getJSONObject(i);
                        String bId = obj.optString("booking_id", "");
                        String bStatus = obj.optString("status", "Pending");
                        String roomId = obj.optString("room_id", "");
                        boolean exclusive = obj.optBoolean("exclusive", false);
                        String bDate = obj.optString("booking_date", "");
                        String checkInDate = obj.optString("check_in_date", "");
                        String checkOutDate = obj.optString("check_out_date", "");

                        if (checkInDate.isEmpty() || "null".equalsIgnoreCase(checkInDate)) {
                            checkInDate = bDate;
                        }
                        if (checkOutDate.isEmpty() || "null".equalsIgnoreCase(checkOutDate)) {
                            checkOutDate = bDate;
                        }

                        if (!bId.isEmpty() && !"Cancelled".equalsIgnoreCase(bStatus) && !"Declined".equalsIgnoreCase(bStatus)) {
                            Booking temp = new Booking();
                            temp.setBookingId(bId);
                            temp.setRoomId(exclusive ? "EXCLUSIVE" : roomId);
                            temp.setCheckInDate(checkInDate);
                            temp.setCheckOutDate(checkOutDate);
                            temp.setStatus(bStatus);
                            remoteRoomBookings.add(temp);
                        }
                    }
                    if (isAdded() && getActivity() != null) {
                        getActivity().runOnUiThread(updateDateUi::run);
                    }
                } catch (Exception ignored) {}
            }
            @Override
            public void onError(String errorMessage) {}
        });

        if (rgPaymentPlan != null) {
            rgPaymentPlan.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.rbPayHalf && rbPayHalf != null && rbPayFull != null) {
                    rbPayHalf.setTextColor(Color.parseColor("#FFD700"));
                    rbPayHalf.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#20FFD700")));
                    rbPayFull.setTextColor(Color.parseColor("#A0A0A0"));
                    rbPayFull.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#15FFFFFF")));
                } else if (rbPayFull != null && rbPayHalf != null) {
                    rbPayFull.setTextColor(Color.parseColor("#FFD700"));
                    rbPayFull.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#20FFD700")));
                    rbPayHalf.setTextColor(Color.parseColor("#A0A0A0"));
                    rbPayHalf.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#15FFFFFF")));
                }
                updateDateUi.run();
            });
        }

        updateDateUi.run();

        View.OnClickListener openRangePicker = v -> {
            if (!isAdded()) return;
            CalendarConstraints constraints = new CalendarConstraints.Builder()
                    .setValidator(DateValidatorPointForward.now())
                    .build();

            Pair<Long, Long> currentSelection = new Pair<>(
                    checkInCal.getTimeInMillis(),
                    checkOutCal.getTimeInMillis()
            );

            MaterialDatePicker<Pair<Long, Long>> rangePicker = MaterialDatePicker.Builder.dateRangePicker()
                    .setTitleText("Select Stay In & Stay Out Dates")
                    .setCalendarConstraints(constraints)
                    .setSelection(currentSelection)
                    .build();

            rangePicker.addOnPositiveButtonClickListener(selection -> {
                if (selection != null && selection.first != null && selection.second != null) {
                    Calendar cal1 = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    cal1.setTimeInMillis(selection.first);

                    Calendar cal2 = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    cal2.setTimeInMillis(selection.second);

                    checkInCal.set(cal1.get(Calendar.YEAR), cal1.get(Calendar.MONTH), cal1.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
                    checkOutCal.set(cal2.get(Calendar.YEAR), cal2.get(Calendar.MONTH), cal2.get(Calendar.DAY_OF_MONTH), 0, 0, 0);

                    if (!checkOutCal.after(checkInCal)) {
                        checkOutCal.setTimeInMillis(checkInCal.getTimeInMillis());
                        checkOutCal.add(Calendar.DAY_OF_MONTH, 1);
                    }

                    updateDateUi.run();
                }
            });

            rangePicker.show(getParentFragmentManager(), "ROOM_RESERVE_DATE_RANGE_PICKER");
        };

        if (cardStayInDate != null) cardStayInDate.setOnClickListener(openRangePicker);
        if (cardStayOutDate != null) cardStayOutDate.setOnClickListener(openRangePicker);

        List<String> paymentMethods = new ArrayList<>();
        paymentMethods.add("GCash (Cashless E-Wallet)");
        paymentMethods.add("Pay via Maya (Cashless Gateway)");
        paymentMethods.add("Credit / Debit Card");
        paymentMethods.add("Bank Transfer (Online)");

        com.google.android.material.textfield.TextInputLayout layoutAccountMobile = dialogView.findViewById(R.id.layoutAccountMobile);
        com.google.android.material.textfield.TextInputEditText etAccountMobileNumber = dialogView.findViewById(R.id.etAccountMobileNumber);

        ArrayAdapter<String> paymentAdapter = new ArrayAdapter<>(safeDialogContext, R.layout.item_payment_spinner_selected, paymentMethods);
        paymentAdapter.setDropDownViewResource(R.layout.item_payment_spinner_dropdown);
        if (spinnerPayment != null) {
            spinnerPayment.setAdapter(paymentAdapter);

            spinnerPayment.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    String selected = paymentAdapter.getItem(position);
                    if (selected != null && (selected.contains("GCash") || selected.contains("Maya") || selected.contains("Card") || selected.contains("Bank"))) {
                        if (layoutAccountMobile != null) {
                            layoutAccountMobile.setVisibility(View.VISIBLE);
                            if (selected.contains("GCash") || selected.contains("Maya")) {
                                layoutAccountMobile.setHint("Mobile Account Number");
                            } else if (selected.contains("Card")) {
                                layoutAccountMobile.setHint("Cardholder Mobile Number");
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

        new MaterialAlertDialogBuilder(safeDialogContext)
                .setTitle("Reserve " + item.getTitle())
                .setView(dialogView)
                .setPositiveButton("Confirm Reservation", (dialog, which) -> {
                    String chosenPayment = spinnerPayment != null ? spinnerPayment.getSelectedItem().toString() : "GCash (Cashless E-Wallet)";
                    String mobileNum = etAccountMobileNumber != null ? etAccountMobileNumber.getText().toString().trim() : "";

                    if (layoutAccountMobile != null && layoutAccountMobile.getVisibility() == View.VISIBLE && mobileNum.isEmpty()) {
                        Toast.makeText(appContext, "Please enter your account / mobile number for cashless payment.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    boolean is50Percent = rbPayHalf != null && rbPayHalf.isChecked();

                    if (!mobileNum.isEmpty()) {
                        chosenPayment += " (" + mobileNum + ")";
                    }
                    if (is50Percent) {
                        chosenPayment += " [50% Downpayment]";
                    } else {
                        chosenPayment += " [Pay in Full]";
                    }

                    SharedPreferences prefs = appContext.getSharedPreferences("CasavistaProfilePrefs", Context.MODE_PRIVATE);
                    String userEmail = prefs.getString("user_email", "guest@casavista.com").trim();
                    String emailCleanKey = userEmail.toLowerCase().replaceAll("[^a-zA-Z0-9]", "_");
                    String customerId = prefs.getString("customer_id", prefs.getString("customer_id_" + emailCleanKey, "CUST-101")).trim();
                    if (customerId.isEmpty()) {
                        customerId = "CUST-101";
                    }

                    String bookingId = com.example.casavista.helpers.BookingIdGenerator.generateBookingId();

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
                    String checkInIso = isoFmt.format(checkInCal.getTime());
                    String checkOutIso = isoFmt.format(checkOutCal.getTime());

                    if (repository.isRoomBookedForDates(roomId, checkInIso, checkOutIso)
                            || isRemoteRoomOverlap(remoteRoomBookings, roomId, checkInIso, checkOutIso)) {
                        if (isAdded() && getContext() != null) {
                            new MaterialAlertDialogBuilder(requireContext())
                                    .setTitle("Date Overlap Notice ⚠️")
                                    .setMessage("The selected suite (" + item.getTitle() + ") is already reserved for " + checkInIso + " to " + checkOutIso + ".\n\nPlease select different stay dates or choose another available suite.")
                                    .setPositiveButton("OK", null)
                                    .show();
                        } else {
                            Toast.makeText(appContext, "Selected dates are already reserved.", Toast.LENGTH_LONG).show();
                        }
                        return;
                    }

                    long nights = (checkOutCal.getTimeInMillis() - checkInCal.getTimeInMillis()) / (1000 * 60 * 60 * 24);
                    if (nights < 1) nights = 1;

                    double rate = 2500.0;
                    try {
                        String raw = item.getPriceLabel() != null ? item.getPriceLabel().replaceAll("[^0-9.]", "") : "2500";
                        if (!raw.isEmpty()) rate = Double.parseDouble(raw);
                    } catch (Exception ignored) {}

                    double calculatedTotal = rate * nights;
                    double amountDueNow = is50Percent ? (calculatedTotal * 0.5) : calculatedTotal;

                    String chosenDateRange = displaySdf.format(checkInCal.getTime()) + " - " + displaySdf.format(checkOutCal.getTime()) + " (" + nights + " " + (nights == 1 ? "Night" : "Nights") + ")";

                    try {
                        org.json.JSONObject payload = new org.json.JSONObject();
                        payload.put("booking_id", bookingId);
                        payload.put("customer_id", customerId);
                        payload.put("customer_email", userEmail);
                        payload.put("staff_id", "STF-001");
                        payload.put("room_id", roomId);
                        payload.put("booking_date", todayIso);
                        payload.put("check_in_date", checkInIso);
                        payload.put("check_out_date", checkOutIso);
                        payload.put("exclusive", isExclusive);
                        payload.put("total_amount", calculatedTotal);
                        payload.put("status", "Pending");

                        String finalChosenPayment = chosenPayment;
                        String finalBookingId = bookingId;
                        String finalCustomerId = customerId;
                        double finalCalculatedTotal = calculatedTotal;
                        double finalAmountDueNow = amountDueNow;
                        String finalMobileNum = mobileNum;
                        long finalNights = nights;
                        String finalRoomId = roomId;

                        Toast.makeText(appContext, "Securing 15-minute room reservation hold...", Toast.LENGTH_SHORT).show();

                        com.example.casavista.network.SupabaseClient.getInstance().createBooking(
                                payload,
                                new com.example.casavista.network.SupabaseClient.DataCallback() {
                                    @Override
                                    public void onSuccess(String jsonResponse) {
                                        String resolvedCustId = finalCustomerId;
                                        try {
                                            org.json.JSONObject respObj = new org.json.JSONObject(jsonResponse);
                                            resolvedCustId = respObj.optString("customer_id", finalCustomerId);
                                        } catch (Exception ignored) {}

                                        // Always persist using appContext even if Fragment navigated away
                                        prefs.edit()
                                                .putString("last_booking_id", finalBookingId)
                                                .putString("customer_id", resolvedCustId)
                                                .putString("customer_id_" + emailCleanKey, resolvedCustId)
                                                .apply();

                                        com.example.casavista.models.Booking roomBooking = new com.example.casavista.models.Booking(
                                                item.getTitle(),
                                                "Pending",
                                                chosenDateRange,
                                                "PAYMENT: " + finalChosenPayment,
                                                "CAPACITY: " + item.getCapacity() + " • " + finalNights + (finalNights == 1 ? " Night" : " Nights") + " • Hold active (15-min)",
                                                String.format(Locale.US, "₱%,.2f", finalCalculatedTotal),
                                                item.getImageUrl(),
                                                item.getImageResId()
                                        );
                                        roomBooking.setBookingId(finalBookingId);
                                        roomBooking.setCustomerId(resolvedCustId);
                                        roomBooking.setRoomId(finalRoomId);
                                        roomBooking.setCheckInDate(checkInIso);
                                        roomBooking.setCheckOutDate(checkOutIso);
                                        roomBooking.setTotalAmount(finalCalculatedTotal);

                                        repository.addBooking(appContext, roomBooking);

                                        // Guard UI dialog against detached Fragment
                                        if (!isAdded() || getContext() == null || getActivity() == null || getActivity().isFinishing()) {
                                            Toast.makeText(appContext, "Hold Created! Booking ID: " + finalBookingId, Toast.LENGTH_LONG).show();
                                            return;
                                        }

                                        final Context attachedContext = requireContext();
                                        new MaterialAlertDialogBuilder(attachedContext)
                                                .setTitle("Hold Created! ⏱️")
                                                .setMessage("Reserved! You have 15 minutes to finalize payment before this room is released.\nBooking ID: " + finalBookingId + "\nAmount Due Now: " + String.format(Locale.US, "₱%,.2f", finalAmountDueNow) + "\n\nWould you like to pay with Maya Sandbox now?")
                                                .setPositiveButton("Pay Now with Maya", (d, w) -> {
                                                    if (!isAdded() || getContext() == null) return;
                                                    filterCatalog(currentCategory);
                                                    com.example.casavista.dialogs.MayaTestPaymentDialog.show(
                                                            requireContext(),
                                                            finalBookingId,
                                                            finalAmountDueNow,
                                                            finalMobileNum,
                                                            (success, paidBookingId) -> {
                                                                MainActivity mainActivity = (MainActivity) getActivity();
                                                                if (mainActivity != null) {
                                                                    mainActivity.loadFragment(new BookingsFragment());
                                                                }
                                                            }
                                                    );
                                                })
                                                .setNeutralButton("Later (Bookings)", (d, w) -> {
                                                    if (!isAdded()) return;
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
                                        String cleanMsg = "ERR-409-ROOM-COLLISION: Sorry, this suite is already reserved for your selected stay dates. Please select a different date range or facility.";
                                        if (errorMessage != null && errorMessage.contains("ERR-")) {
                                            int idx = errorMessage.indexOf("ERR-");
                                            cleanMsg = errorMessage.substring(idx).replace("\"", "").replace("}", "").trim();
                                        }
                                        if (!isAdded() || getContext() == null || getActivity() == null || getActivity().isFinishing()) {
                                            Toast.makeText(appContext, cleanMsg, Toast.LENGTH_LONG).show();
                                            return;
                                        }
                                        new MaterialAlertDialogBuilder(requireContext())
                                                .setTitle("Reservation Unavailable")
                                                .setMessage(cleanMsg)
                                                .setPositiveButton("OK", null)
                                                .show();
                                    }
                                }
                        );

                    } catch (Exception e) {
                        Toast.makeText(appContext, "Payload Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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