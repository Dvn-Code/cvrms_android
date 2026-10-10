package com.example.casavista.user.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.casavista.user.MainActivity;
import com.example.casavista.R;
import com.example.casavista.adapters.CalendarGridAdapter;
import com.example.casavista.data.ResortRepository;
import com.example.casavista.models.Booking;
import com.example.casavista.models.CalendarDay;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CourtFragment extends Fragment {

    private ResortRepository repository;
    private TextView tvCalendarMonthYear, tvSummaryLine, tvSummaryPrice;
    private View btnPrevMonth, btnNextMonth;
    private RecyclerView rvCalendarGrid;
    private Calendar currentCalendarMonth;
    private int selectedDayNumber = 20;

    private MaterialButton btnSportPickleball, btnSportBasketball;
    private MaterialButton btnCourt1, btnCourt2, btnCourt3;
    private MaterialButton btnSlot1, btnSlot2, btnSlot3, btnSlot4, btnSlot5, btnSlot6, btnSlot7, btnSlot8, btnSlot9, btnSlot10, btnSlot11, btnSlot12;
    private MaterialButton btnConfirmCourtPay;

    // Paddle Rental Views
    private View cardPaddleRental;
    private TextView tvPaddleCount;
    private MaterialButton btnMinusPaddle, btnPlusPaddle;

    // State Tracking Variables
    private String selectedSport = "Pickleball"; // "Pickleball" or "Basketball"
    private String selectedCourt = "Court 1";
    private String selectedTimeSlot = "3-4 PM";
    private int selectedHourStart = 15; // 24-hr format (3 PM = 15, 6 PM = 18)
    private int paddleCount = 0;
    private int calculatedTotalPrice = 400;
    private String formattedCourtDate = "Thu, Aug 20 2026";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_court, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = ResortRepository.getInstance();

        currentCalendarMonth = Calendar.getInstance();
        selectedDayNumber = currentCalendarMonth.get(Calendar.DAY_OF_MONTH);

        SimpleDateFormat fullDate = new SimpleDateFormat("EEE, MMM dd yyyy", Locale.getDefault());
        formattedCourtDate = fullDate.format(currentCalendarMonth.getTime());

        btnSportPickleball = view.findViewById(R.id.btnSportPickleball);
        btnSportBasketball = view.findViewById(R.id.btnSportBasketball);
        tvCalendarMonthYear = view.findViewById(R.id.tvCalendarMonthYear);
        btnPrevMonth = view.findViewById(R.id.btnPrevMonth);
        btnNextMonth = view.findViewById(R.id.btnNextMonth);
        rvCalendarGrid = view.findViewById(R.id.rvCalendarGrid);
        rvCalendarGrid.setLayoutManager(new GridLayoutManager(requireContext(), 7));

        btnCourt1 = view.findViewById(R.id.btnCourt1);
        btnCourt2 = view.findViewById(R.id.btnCourt2);
        btnCourt3 = view.findViewById(R.id.btnCourt3);

        btnSlot1 = view.findViewById(R.id.btnSlot1);
        btnSlot2 = view.findViewById(R.id.btnSlot2);
        btnSlot3 = view.findViewById(R.id.btnSlot3);
        btnSlot4 = view.findViewById(R.id.btnSlot4);
        btnSlot5 = view.findViewById(R.id.btnSlot5);
        btnSlot6 = view.findViewById(R.id.btnSlot6);
        btnSlot7 = view.findViewById(R.id.btnSlot7);
        btnSlot8 = view.findViewById(R.id.btnSlot8);
        btnSlot9 = view.findViewById(R.id.btnSlot9);
        btnSlot10 = view.findViewById(R.id.btnSlot10);
        btnSlot11 = view.findViewById(R.id.btnSlot11);
        btnSlot12 = view.findViewById(R.id.btnSlot12);

        tvSummaryLine = view.findViewById(R.id.tvSummaryLine);
        tvSummaryPrice = view.findViewById(R.id.tvSummaryPrice);
        btnConfirmCourtPay = view.findViewById(R.id.btnConfirmCourtPay);

        cardPaddleRental = view.findViewById(R.id.cardPaddleRental);
        tvPaddleCount = view.findViewById(R.id.tvPaddleCount);
        btnMinusPaddle = view.findViewById(R.id.btnMinusPaddle);
        btnPlusPaddle = view.findViewById(R.id.btnPlusPaddle);

        btnSportPickleball.setOnClickListener(v -> {
            selectedSport = "Pickleball";
            if (cardPaddleRental != null) cardPaddleRental.setVisibility(View.VISIBLE);
            animateSelection(v);
            highlightSportSwitch();
            updateCourtButtonsForSport();
            recalculateCourtPrice();
        });

        btnSportBasketball.setOnClickListener(v -> {
            selectedSport = "Basketball";
            paddleCount = 0;
            if (tvPaddleCount != null) tvPaddleCount.setText("0");
            if (cardPaddleRental != null) cardPaddleRental.setVisibility(View.GONE);
            animateSelection(v);
            highlightSportSwitch();
            updateCourtButtonsForSport();
            recalculateCourtPrice();
        });

        if (btnMinusPaddle != null) {
            btnMinusPaddle.setOnClickListener(v -> {
                if (paddleCount > 0) {
                    paddleCount--;
                    if (tvPaddleCount != null) tvPaddleCount.setText(String.valueOf(paddleCount));
                    recalculateCourtPrice();
                }
            });
        }

        if (btnPlusPaddle != null) {
            btnPlusPaddle.setOnClickListener(v -> {
                if (paddleCount < 8) {
                    paddleCount++;
                    if (tvPaddleCount != null) tvPaddleCount.setText(String.valueOf(paddleCount));
                    recalculateCourtPrice();
                }
            });
        }

        btnPrevMonth.setOnClickListener(v -> {
            currentCalendarMonth.add(Calendar.MONTH, -1);
            populateCalendarGrid();
        });

        btnNextMonth.setOnClickListener(v -> {
            currentCalendarMonth.add(Calendar.MONTH, 1);
            populateCalendarGrid();
        });

        btnCourt1.setOnClickListener(v -> {
            if ("Basketball".equalsIgnoreCase(selectedSport)) {
                selectCourt("Basketball Half-Court", btnCourt1);
            } else {
                selectCourt("Court 1", btnCourt1, btnCourt2, btnCourt3);
            }
        });
        btnCourt2.setOnClickListener(v -> selectCourt("Court 2", btnCourt2, btnCourt1, btnCourt3));
        btnCourt3.setOnClickListener(v -> selectCourt("Court 3", btnCourt3, btnCourt1, btnCourt2));

        btnSlot1.setOnClickListener(v -> selectSlot("6-7 AM", 6, btnSlot1));
        btnSlot2.setOnClickListener(v -> selectSlot("8-9 AM", 8, btnSlot2));
        btnSlot3.setOnClickListener(v -> selectSlot("10-11 AM", 10, btnSlot3));
        btnSlot4.setOnClickListener(v -> selectSlot("12-1 PM", 12, btnSlot4));
        btnSlot5.setOnClickListener(v -> selectSlot("2-3 PM", 14, btnSlot5));
        btnSlot6.setOnClickListener(v -> selectSlot("3-4 PM", 15, btnSlot6));
        btnSlot7.setOnClickListener(v -> selectSlot("4-5 PM", 16, btnSlot7));
        btnSlot8.setOnClickListener(v -> selectSlot("5-6 PM", 17, btnSlot8));
        btnSlot9.setOnClickListener(v -> selectSlot("6-7 PM", 18, btnSlot9));
        btnSlot10.setOnClickListener(v -> selectSlot("7-8 PM", 19, btnSlot10));
        btnSlot11.setOnClickListener(v -> selectSlot("8-9 PM", 20, btnSlot11));
        btnSlot12.setOnClickListener(v -> selectSlot("9-10 PM", 21, btnSlot12));

        btnConfirmCourtPay.setOnClickListener(v -> {
            animateSelection(v);
            showCourtPaymentDialog();
        });

        View cardAvatar = view.findViewById(R.id.cardCourtAvatar);
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

        populateCalendarGrid();
        highlightSportSwitch();
        recalculateCourtPrice();
        checkCourtAvailability();
        loadHeaderProfile(view);
    }

    private void recalculateCourtPrice() {
        boolean isNightTime = selectedHourStart >= 18; // 6:00 PM (18:00) cutoff rule
        int hourlyRate;

        if ("Pickleball".equalsIgnoreCase(selectedSport)) {
            hourlyRate = isNightTime ? 600 : 400;
        } else { // Basketball
            hourlyRate = isNightTime ? 750 : 500;
        }

        int paddleCost = ("Pickleball".equalsIgnoreCase(selectedSport)) ? (paddleCount * 100) : 0;
        calculatedTotalPrice = hourlyRate + paddleCost;

        String rateType = isNightTime ? "Night Rate" : "Day Rate";
        String paddleText = ("Pickleball".equalsIgnoreCase(selectedSport) && paddleCount > 0)
                ? " • " + paddleCount + " Paddles (₱" + paddleCost + ")"
                : "";

        if (tvSummaryLine != null) {
            tvSummaryLine.setText("1 hr • " + selectedCourt + " • " + selectedTimeSlot + " (" + rateType + ")" + paddleText);
        }
        if (tvSummaryPrice != null) {
            tvSummaryPrice.setText("₱" + calculatedTotalPrice);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        checkCourtAvailability();
        loadHeaderProfile(getView());
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

    private void populateCalendarGrid() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvCalendarMonthYear.setText(sdf.format(currentCalendarMonth.getTime()));

        List<CalendarDay> dayList = new ArrayList<>();
        Calendar cal = (Calendar) currentCalendarMonth.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);

        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        Calendar prevCal = (Calendar) cal.clone();
        prevCal.add(Calendar.MONTH, -1);
        int daysInPrevMonth = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH);

        int startPadding = firstDayOfWeek - 1;
        for (int i = startPadding - 1; i >= 0; i--) {
            dayList.add(new CalendarDay(daysInPrevMonth - i, prevCal.get(Calendar.MONTH), prevCal.get(Calendar.YEAR), false));
        }

        for (int d = 1; d <= daysInMonth; d++) {
            dayList.add(new CalendarDay(d, currentCalendarMonth.get(Calendar.MONTH), currentCalendarMonth.get(Calendar.YEAR), true));
        }

        int totalCells = (dayList.size() <= 35) ? 35 : 42;
        int nextDays = totalCells - dayList.size();
        Calendar nextCal = (Calendar) cal.clone();
        nextCal.add(Calendar.MONTH, 1);
        for (int n = 1; n <= nextDays; n++) {
            dayList.add(new CalendarDay(n, nextCal.get(Calendar.MONTH), nextCal.get(Calendar.YEAR), false));
        }

        CalendarGridAdapter adapter = new CalendarGridAdapter(dayList, selectedDayNumber, day -> {
            selectedDayNumber = day.getDayNumber();
            Calendar picked = Calendar.getInstance();
            picked.set(day.getYear(), day.getMonth(), day.getDayNumber());

            SimpleDateFormat fullDate = new SimpleDateFormat("EEE, MMM dd yyyy", Locale.getDefault());
            formattedCourtDate = fullDate.format(picked.getTime());

            checkCourtAvailability();
        });

        rvCalendarGrid.setAlpha(0f);
        rvCalendarGrid.setAdapter(adapter);
        rvCalendarGrid.animate().alpha(1f).setDuration(200).start();
    }

    private void updateCourtButtonsForSport() {
        if (btnCourt1 == null || btnCourt2 == null || btnCourt3 == null) return;

        if ("Basketball".equalsIgnoreCase(selectedSport)) {
            btnCourt1.setVisibility(View.VISIBLE);
            btnCourt2.setVisibility(View.GONE);
            btnCourt3.setVisibility(View.GONE);

            selectedCourt = "Basketball Half-Court";
            btnCourt1.setText("Basketball Half-Court");
        } else {
            btnCourt1.setVisibility(View.VISIBLE);
            btnCourt2.setVisibility(View.VISIBLE);
            btnCourt3.setVisibility(View.VISIBLE);

            btnCourt1.setText("Court 1");
            btnCourt2.setText("Court 2");
            btnCourt3.setText("Court 3");

            selectedCourt = "Court 1";
        }
        checkCourtAvailability();
    }

    private void checkCourtAvailability() {
        if (getContext() == null) return;

        if ("Basketball".equalsIgnoreCase(selectedSport)) {
            btnCourt1.setVisibility(View.VISIBLE);
            btnCourt2.setVisibility(View.GONE);
            btnCourt3.setVisibility(View.GONE);

            int bookedCount = getBookedSlotsCount(requireContext(), "Basketball Half-Court", formattedCourtDate);
            updateCourtButtonAppearance(btnCourt1, "Basketball Half-Court", bookedCount);
        } else {
            btnCourt1.setVisibility(View.VISIBLE);
            btnCourt2.setVisibility(View.VISIBLE);
            btnCourt3.setVisibility(View.VISIBLE);

            int b1 = getBookedSlotsCount(requireContext(), "Court 1", formattedCourtDate);
            int b2 = getBookedSlotsCount(requireContext(), "Court 2", formattedCourtDate);
            int b3 = getBookedSlotsCount(requireContext(), "Court 3", formattedCourtDate);

            updateCourtButtonAppearance(btnCourt1, "Court 1", b1);
            updateCourtButtonAppearance(btnCourt2, "Court 2", b2);
            updateCourtButtonAppearance(btnCourt3, "Court 3", b3);
        }

        checkSlotAvailability();
    }

    private int getBookedSlotsCount(Context context, String courtName, String date) {
        String[] slotTimes = {
                "6-7 AM", "8-9 AM", "10-11 AM", "12-1 PM", "2-3 PM", "3-4 PM",
                "4-5 PM", "5-6 PM", "6-7 PM", "7-8 PM", "8-9 PM", "9-10 PM"
        };
        int count = 0;
        for (String time : slotTimes) {
            if (isSlotBooked(context, courtName, date, time)) {
                count++;
            }
        }
        return count;
    }

    private void updateCourtButtonAppearance(MaterialButton btn, String courtName, int bookedSlotsCount) {
        if (btn == null) return;

        String override = ResortRepository.getInstance().getFacilityStatusOverride(requireContext(), courtName);
        boolean isAdminMaintenance = "Maintenance".equalsIgnoreCase(override) || "Unavailable".equalsIgnoreCase(override);

        if (isAdminMaintenance || bookedSlotsCount >= 12) {
            btn.setText(courtName + "\n(Fully Booked)");
            btn.setAlpha(0.6f);
            btn.setEnabled(false);
        } else {
            btn.setEnabled(true);
            btn.setAlpha(1.0f);
            if (courtName.equalsIgnoreCase(selectedCourt)) {
                btn.setBackgroundColor(Color.parseColor("#4D391A"));
                btn.setTextColor(Color.WHITE);
            } else {
                btn.setBackgroundColor(Color.parseColor("#EADFD3"));
                btn.setTextColor(Color.parseColor("#59493B"));
            }
        }
    }

    private void selectCourt(String name, MaterialButton active, MaterialButton... inactives) {
        int bookedSlots = getBookedSlotsCount(requireContext(), name, formattedCourtDate);
        String override = ResortRepository.getInstance().getFacilityStatusOverride(requireContext(), name);

        if ("Maintenance".equalsIgnoreCase(override) || "Unavailable".equalsIgnoreCase(override) || bookedSlots >= 12) {
            Toast.makeText(requireContext(), name + " is currently fully booked or under maintenance for this date.", Toast.LENGTH_SHORT).show();
            return;
        }

        selectedCourt = name;
        animateSelection(active);

        active.setBackgroundColor(Color.parseColor("#4D391A"));
        active.setTextColor(Color.WHITE);

        for (MaterialButton inactive : inactives) {
            if (inactive != null) {
                inactive.setBackgroundColor(Color.parseColor("#EADFD3"));
                inactive.setTextColor(Color.parseColor("#59493B"));
            }
        }

        checkSlotAvailability();
    }

    private void checkSlotAvailability() {
        if (getContext() == null) return;

        MaterialButton[] slots = {
                btnSlot1, btnSlot2, btnSlot3, btnSlot4, btnSlot5, btnSlot6,
                btnSlot7, btnSlot8, btnSlot9, btnSlot10, btnSlot11, btnSlot12
        };
        String[] slotTimes = {
                "6-7 AM", "8-9 AM", "10-11 AM", "12-1 PM", "2-3 PM", "3-4 PM",
                "4-5 PM", "5-6 PM", "6-7 PM", "7-8 PM", "8-9 PM", "9-10 PM"
        };
        int[] slotHours = {6, 8, 10, 12, 14, 15, 16, 17, 18, 19, 20, 21};

        Calendar nowCal = Calendar.getInstance();
        int realYear = nowCal.get(Calendar.YEAR);
        int realMonth = nowCal.get(Calendar.MONTH);
        int realDay = nowCal.get(Calendar.DAY_OF_MONTH);
        int realHour = nowCal.get(Calendar.HOUR_OF_DAY);

        int selectedYear = currentCalendarMonth.get(Calendar.YEAR);
        int selectedMonth = currentCalendarMonth.get(Calendar.MONTH);

        boolean isToday = (selectedYear == realYear && selectedMonth == realMonth && selectedDayNumber == realDay);
        boolean isPastDate = false;

        Calendar selCal = Calendar.getInstance();
        selCal.set(selectedYear, selectedMonth, selectedDayNumber, 0, 0, 0);
        selCal.set(Calendar.MILLISECOND, 0);

        Calendar todayCal = Calendar.getInstance();
        todayCal.set(realYear, realMonth, realDay, 0, 0, 0);
        todayCal.set(Calendar.MILLISECOND, 0);

        if (selCal.before(todayCal)) {
            isPastDate = true;
        }

        String firstOpenSlot = null;
        int firstOpenHour = 15;

        for (int i = 0; i < slots.length; i++) {
            MaterialButton btn = slots[i];
            String time = slotTimes[i];
            int hour = slotHours[i];
            if (btn == null) continue;

            boolean isBooked = isSlotBooked(requireContext(), selectedCourt, formattedCourtDate, time);
            boolean isTimePassed = isPastDate || (isToday && hour <= realHour);

            if (isBooked) {
                btn.setText(time + "\n(Booked)");
                btn.setEnabled(false);
                btn.setAlpha(0.55f);
                btn.setBackgroundColor(Color.parseColor("#CFC0AD"));
                btn.setTextColor(Color.parseColor("#7A6858"));
            } else if (isTimePassed) {
                btn.setText(time + (isPastDate ? "\n(Past Date)" : "\n(Passed)"));
                btn.setEnabled(false);
                btn.setAlpha(0.40f);
                btn.setBackgroundColor(Color.parseColor("#D5C8B6"));
                btn.setTextColor(Color.parseColor("#8C7B6B"));
            } else {
                btn.setEnabled(true);
                btn.setAlpha(1.0f);
                if (firstOpenSlot == null) {
                    firstOpenSlot = time;
                    firstOpenHour = hour;
                }

                if (time.equalsIgnoreCase(selectedTimeSlot)) {
                    btn.setBackgroundColor(Color.parseColor("#4D391A"));
                    btn.setTextColor(Color.WHITE);
                } else {
                    btn.setBackgroundColor(Color.parseColor("#EADFD3"));
                    btn.setTextColor(Color.parseColor("#59493B"));
                }
            }
        }

        if (isSlotBooked(requireContext(), selectedCourt, formattedCourtDate, selectedTimeSlot)) {
            if (firstOpenSlot != null) {
                selectedTimeSlot = firstOpenSlot;
                selectedHourStart = firstOpenHour;
                checkSlotAvailability();
            }
        }

        recalculateCourtPrice();
    }

    private boolean isSlotBooked(Context context, String courtName, String date, String timeSlot) {
        if (context == null || courtName == null || timeSlot == null) return false;

        ResortRepository repo = ResortRepository.getInstance();
        repo.loadSavedBookings(context);

        List<Booking> activeBookings = repo.getBookings("Upcoming");
        String normCourt = courtName.toLowerCase().trim();
        String normDate = date != null ? date.toLowerCase().trim() : "";
        String normSlot = timeSlot.toLowerCase().trim();

        for (Booking b : activeBookings) {
            String title = b.getTitle() != null ? b.getTitle().toLowerCase() : "";
            String bDate = b.getDate() != null ? b.getDate().toLowerCase() : "";
            String d1 = b.getDetail1() != null ? b.getDetail1().toLowerCase() : "";

            boolean matchesCourt = title.contains(normCourt) || normCourt.contains(title);
            boolean matchesDate = normDate.isEmpty() || bDate.contains(normDate) || normDate.contains(bDate);
            boolean matchesSlot = d1.contains(normSlot);

            if (matchesCourt && matchesDate && matchesSlot) {
                return true;
            }
        }
        return false;
    }

    private void selectSlot(String slot, int startHour, MaterialButton active) {
        if (isSlotBooked(requireContext(), selectedCourt, formattedCourtDate, slot)) {
            Toast.makeText(requireContext(), slot + " is already booked on " + selectedCourt, Toast.LENGTH_SHORT).show();
            return;
        }

        selectedTimeSlot = slot;
        selectedHourStart = startHour;
        animateSelection(active);
        checkSlotAvailability();
    }

    private void highlightSportSwitch() {
        if (selectedSport.equalsIgnoreCase("Pickleball")) {
            btnSportPickleball.setBackgroundColor(Color.parseColor("#4D391A"));
            btnSportPickleball.setTextColor(Color.WHITE);

            btnSportBasketball.setBackgroundColor(Color.parseColor("#F8F5F0"));
            btnSportBasketball.setTextColor(Color.parseColor("#6B5B4B"));
        } else {
            btnSportBasketball.setBackgroundColor(Color.parseColor("#4D391A"));
            btnSportBasketball.setTextColor(Color.WHITE);

            btnSportPickleball.setBackgroundColor(Color.parseColor("#F8F5F0"));
            btnSportPickleball.setTextColor(Color.parseColor("#6B5B4B"));
        }
    }

    private void showCourtPaymentDialog() {
        if (getContext() == null) return;

        android.widget.LinearLayout container = new android.widget.LinearLayout(requireContext());
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(40, 20, 40, 10);

        List<String> paymentMethods = new ArrayList<>();
        paymentMethods.add("GCash (Cashless E-Wallet)");
        paymentMethods.add("Cash at Front Desk");
        paymentMethods.add("Credit / Debit Card");
        paymentMethods.add("Bank Transfer (Online)");

        final android.widget.Spinner spinner = new android.widget.Spinner(requireContext());
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(requireContext(), R.layout.item_payment_spinner_selected, paymentMethods);
        adapter.setDropDownViewResource(R.layout.item_payment_spinner_dropdown);
        spinner.setAdapter(adapter);

        final com.google.android.material.textfield.TextInputLayout layoutMobile = new com.google.android.material.textfield.TextInputLayout(requireContext());
        layoutMobile.setHint("GCash / Account Mobile Number");
        android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 20, 0, 0);
        layoutMobile.setLayoutParams(params);

        final com.google.android.material.textfield.TextInputEditText etMobile = new com.google.android.material.textfield.TextInputEditText(requireContext());
        etMobile.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        layoutMobile.addView(etMobile);

        container.addView(spinner);
        container.addView(layoutMobile);

        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selected = adapter.getItem(position);
                if (selected != null && (selected.contains("GCash") || selected.contains("Card") || selected.contains("Bank"))) {
                    layoutMobile.setVisibility(View.VISIBLE);
                } else {
                    layoutMobile.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        String isNightText = selectedHourStart >= 18 ? "Night Rate" : "Day Rate";
        String paddleDetail = ("Pickleball".equalsIgnoreCase(selectedSport) && paddleCount > 0)
                ? "\nPaddles: " + paddleCount + " (₱" + (paddleCount * 100) + ")"
                : "";

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Payment Method")
                .setMessage("Reserving " + selectedCourt + " (" + selectedSport + ") for " + formattedCourtDate + "\nSlot: " + selectedTimeSlot + " (" + isNightText + ")" + paddleDetail + "\nTotal Rate: ₱" + calculatedTotalPrice)
                .setView(container)
                .setPositiveButton("Confirm & Pay", (dialog, which) -> {
                    String chosenPayment = spinner.getSelectedItem() != null ? spinner.getSelectedItem().toString() : "GCash (Cashless E-Wallet)";
                    String mobileNum = etMobile.getText() != null ? etMobile.getText().toString().trim() : "";

                    if (layoutMobile.getVisibility() == View.VISIBLE && mobileNum.isEmpty()) {
                        Toast.makeText(requireContext(), "Please enter your account / mobile number for cashless payment.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (!mobileNum.isEmpty()) {
                        chosenPayment += " (" + mobileNum + ")";
                    }

                    SharedPreferences prefs = requireContext().getSharedPreferences("CasavistaProfilePrefs", Context.MODE_PRIVATE);
                    String customerId = prefs.getString("customer_id", "CUST-101");
                    String bookingId = "BK-" + (System.currentTimeMillis() % 1000000);

                    String courtIdMapped = "CRT-01";
                    if (selectedCourt.contains("2")) courtIdMapped = "CRT-02";
                    else if (selectedCourt.contains("3")) courtIdMapped = "CRT-03";
                    else if (selectedCourt.contains("Basketball")) courtIdMapped = "CRT-04";

                    String dateIso = "2026-08-20";
                    try {
                        SimpleDateFormat fullDateFmt = new SimpleDateFormat("EEE, MMM dd yyyy", Locale.getDefault());
                        java.util.Date parsed = fullDateFmt.parse(formattedCourtDate);
                        if (parsed != null) {
                            SimpleDateFormat isoFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                            dateIso = isoFmt.format(parsed);
                        }
                    } catch (Exception ignored) {}

                    String startTimeStr = String.format(Locale.US, "%02d:00:00", selectedHourStart);
                    String endTimeStr = String.format(Locale.US, "%02d:00:00", selectedHourStart + 1);

                    try {
                        org.json.JSONObject payload = new org.json.JSONObject();
                        payload.put("booking_id", bookingId);
                        payload.put("customer_id", customerId);
                        payload.put("staff_id", "STF-001");
                        payload.put("court_id", courtIdMapped);
                        payload.put("room_id", org.json.JSONObject.NULL);
                        payload.put("booking_date", dateIso);
                        payload.put("start_time", startTimeStr);
                        payload.put("end_time", endTimeStr);
                        payload.put("paddle_count", paddleCount);
                        payload.put("exclusive", false);
                        payload.put("status", "Pending");

                        String finalChosenPayment = chosenPayment;
                        String finalDateIso = dateIso;
                        String finalBookingId = bookingId;
                        int sportImageRes = "Basketball".equalsIgnoreCase(selectedSport) ? R.drawable.basketball_icon_img : R.drawable.pickleball_icon_img;
                        String equipText = ("Pickleball".equalsIgnoreCase(selectedSport) && paddleCount > 0)
                                ? paddleCount + " Paddles Rental Included"
                                : "Standard Equipment Included";

                        Toast.makeText(requireContext(), "Securing 15-minute reservation hold...", Toast.LENGTH_SHORT).show();

                        com.example.casavista.network.SupabaseClient.getInstance().createBooking(
                                payload,
                                new com.example.casavista.network.SupabaseClient.DataCallback() {
                                    @Override
                                    public void onSuccess(String jsonArrayString) {
                                        prefs.edit().putString("last_booking_id", finalBookingId).apply();

                                        repository.addBooking(
                                                requireContext(),
                                                selectedSport + " • " + selectedCourt,
                                                "Pending",
                                                formattedCourtDate,
                                                "SLOT: " + selectedTimeSlot + " • PAYMENT: " + finalChosenPayment,
                                                equipText + " • Hold active (15-min window)",
                                                "₱" + calculatedTotalPrice,
                                                null,
                                                sportImageRes
                                        );

                                        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                                                .setTitle("Hold Created! ⏱️")
                                                .setMessage("Reserved! You have 15 minutes to finalize payment before this slot is released.\nBooking ID: " + finalBookingId + "\nTotal Amount: ₱" + calculatedTotalPrice + "\n\nWould you like to pay with Maya Sandbox now?")
                                                .setPositiveButton("Pay Now with Maya", (d, w) -> {
                                                    com.example.casavista.dialogs.MayaTestPaymentDialog.show(
                                                            requireContext(),
                                                            finalBookingId,
                                                            calculatedTotalPrice,
                                                            (success, paidBookingId) -> {
                                                                MainActivity mainActivity = (MainActivity) getActivity();
                                                                if (mainActivity != null) {
                                                                    mainActivity.loadFragment(new BookingsFragment());
                                                                }
                                                            }
                                                    );
                                                })
                                                .setNeutralButton("Later (Bookings)", (d, w) -> {
                                                    MainActivity mainActivity = (MainActivity) getActivity();
                                                    if (mainActivity != null) {
                                                        mainActivity.loadFragment(new BookingsFragment());
                                                    }
                                                })
                                                .show();
                                    }

                                    @Override
                                    public void onError(String errorMessage) {
                                        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                                                .setTitle("Booking Reservation Issue")
                                                .setMessage("Slot conflict or server error: " + errorMessage)
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

    private void animateSelection(View view) {
        if (view == null) return;
        view.setScaleX(0.95f);
        view.setScaleY(0.95f);
        view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
    }
}
