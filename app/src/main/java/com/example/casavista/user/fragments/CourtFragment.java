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
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
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
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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

    private View cardPaddleRental;
    private TextView tvPaddleCount;
    private MaterialButton btnMinusPaddle, btnPlusPaddle;

    private String selectedSport = "Pickleball";
    private String selectedCourt = "Court 1";
    private String selectedTimeSlot = "3-4 PM";
    private int selectedHourStart = 15;
    private int paddleCount = 0;
    private int calculatedTotalPrice = 400;
    private String formattedCourtDate = "Thu, Aug 20 2026";

    private final Set<String> remoteReservedCourtSlots = new HashSet<>();

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

        SimpleDateFormat fullDate = new SimpleDateFormat("EEE, MMM dd yyyy", Locale.US);
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
        syncLiveCourtBookings();
        loadHeaderProfile(view);
    }

    private void syncLiveCourtBookings() {
        com.example.casavista.network.SupabaseClient.getInstance().fetchBookings(
                new com.example.casavista.network.SupabaseClient.DataCallback() {
                    @Override
                    public void onSuccess(String jsonArrayString) {
                        if (!isAdded() || getContext() == null || jsonArrayString == null) return;
                        try {
                            org.json.JSONArray array = new org.json.JSONArray(jsonArrayString);
                            remoteReservedCourtSlots.clear();
                            for (int i = 0; i < array.length(); i++) {
                                org.json.JSONObject obj = array.getJSONObject(i);
                                String status = obj.optString("status", "");
                                if ("Cancelled".equalsIgnoreCase(status) || "Declined".equalsIgnoreCase(status)) {
                                    continue;
                                }
                                String courtId = obj.optString("court_id", "");
                                String bDate = obj.optString("booking_date", "");
                                String startTime = obj.optString("start_time", "");
                                if (!courtId.isEmpty() && !"null".equalsIgnoreCase(courtId) && !bDate.isEmpty() && !startTime.isEmpty()) {
                                    int hr = Integer.parseInt(startTime.split(":")[0]);
                                    remoteReservedCourtSlots.add(courtId.toUpperCase() + "|" + bDate + "|" + hr);
                                }
                            }
                            if (isAdded() && getContext() != null) {
                                checkCourtAvailability();
                            }
                        } catch (Exception ignored) {}
                    }

                    @Override
                    public void onError(String errorMessage) {}
                }
        );
    }

    private String mapCourtNameToId(String courtName) {
        if (courtName == null) return "CRT-PB1";
        String lower = courtName.toLowerCase();
        if (lower.contains("basketball")) return "CRT-BK1";
        if (lower.contains("2")) return "CRT-PB2";
        if (lower.contains("3")) return "CRT-PB3";
        return "CRT-PB1";
    }

    private int mapTimeSlotToHour(String slot) {
        if (slot == null) return -1;
        switch (slot.trim()) {
            case "6-7 AM": return 6;
            case "8-9 AM": return 8;
            case "10-11 AM": return 10;
            case "12-1 PM": return 12;
            case "2-3 PM": return 14;
            case "3-4 PM": return 15;
            case "4-5 PM": return 16;
            case "5-6 PM": return 17;
            case "6-7 PM": return 18;
            case "7-8 PM": return 19;
            case "8-9 PM": return 20;
            case "9-10 PM": return 21;
            default: return -1;
        }
    }

    private void recalculateCourtPrice() {
        boolean isNightTime = selectedHourStart >= 18;
        int hourlyRate;

        if ("Pickleball".equalsIgnoreCase(selectedSport)) {
            hourlyRate = isNightTime ? 600 : 400;
        } else {
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
        syncLiveCourtBookings();
        loadHeaderProfile(getView());
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

    private void populateCalendarGrid() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.US);
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

            SimpleDateFormat fullDate = new SimpleDateFormat("EEE, MMM dd yyyy", Locale.US);
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
        if (!isAdded() || getContext() == null) return;

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
        if (btn == null || !isAdded() || getContext() == null) return;

        String override = ResortRepository.getInstance().getFacilityStatusOverride(requireContext(), courtName);
        boolean isAdminMaintenance = "Maintenance".equalsIgnoreCase(override) || "Unavailable".equalsIgnoreCase(override);

        if (isAdminMaintenance || bookedSlotsCount >= 12) {
            btn.setText(courtName + "\n(Fully Booked)");
            btn.setAlpha(0.6f);
            btn.setEnabled(false);
        } else {
            btn.setText(courtName);
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
        if (!isAdded() || getContext() == null) return;
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
        if (!isAdded() || getContext() == null) return;

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
        boolean selectedSlotUnavailable = false;

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
                if (time.equalsIgnoreCase(selectedTimeSlot)) {
                    selectedSlotUnavailable = true;
                }
            } else if (isTimePassed) {
                btn.setText(time + (isPastDate ? "\n(Past Date)" : "\n(Passed)"));
                btn.setEnabled(false);
                btn.setAlpha(0.40f);
                btn.setBackgroundColor(Color.parseColor("#D5C8B6"));
                btn.setTextColor(Color.parseColor("#8C7B6B"));
                if (time.equalsIgnoreCase(selectedTimeSlot)) {
                    selectedSlotUnavailable = true;
                }
            } else {
                btn.setText(time);
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

        if (selectedSlotUnavailable && firstOpenSlot != null && !firstOpenSlot.equalsIgnoreCase(selectedTimeSlot)) {
            selectedTimeSlot = firstOpenSlot;
            selectedHourStart = firstOpenHour;
            for (int i = 0; i < slots.length; i++) {
                MaterialButton btn = slots[i];
                if (btn != null && btn.isEnabled()) {
                    if (slotTimes[i].equalsIgnoreCase(selectedTimeSlot)) {
                        btn.setBackgroundColor(Color.parseColor("#4D391A"));
                        btn.setTextColor(Color.WHITE);
                    } else {
                        btn.setBackgroundColor(Color.parseColor("#EADFD3"));
                        btn.setTextColor(Color.parseColor("#59493B"));
                    }
                }
            }
        }

        recalculateCourtPrice();
    }

    private boolean isSlotBooked(Context context, String courtName, String date, String timeSlot) {
        if (context == null || courtName == null || timeSlot == null) return false;

        Date parsed = ResortRepository.parseAnyDate(date);
        String dateIso = parsed != null ? new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(parsed) : "";
        String courtId = mapCourtNameToId(courtName);
        int hour = mapTimeSlotToHour(timeSlot);

        if (!dateIso.isEmpty() && hour >= 0) {
            if (remoteReservedCourtSlots.contains(courtId.toUpperCase() + "|" + dateIso + "|" + hour)) {
                return true;
            }
        }

        ResortRepository repo = ResortRepository.getInstance();
        repo.loadSavedBookings(context);

        List<Booking> activeBookings = repo.getBookings("Upcoming");
        String normCourt = courtName.toLowerCase().trim();
        String normDate = date != null ? date.toLowerCase().trim() : "";
        String normSlot = timeSlot.toLowerCase().trim();

        for (Booking b : activeBookings) {
            String title = b.getTitle() != null ? b.getTitle().toLowerCase() : "";
            String bCourtId = b.getCourtId() != null ? b.getCourtId().toUpperCase() : "";
            String bDate = b.getDate() != null ? b.getDate().toLowerCase() : "";
            String d1 = b.getDetail1() != null ? b.getDetail1().toLowerCase() : "";

            boolean matchesCourt = (!bCourtId.isEmpty() && bCourtId.equalsIgnoreCase(courtId))
                    || title.contains(normCourt) || normCourt.contains(title);
            boolean matchesDate = normDate.isEmpty() || bDate.contains(normDate) || normDate.contains(bDate)
                    || (!dateIso.isEmpty() && bDate.contains(dateIso.toLowerCase()));
            boolean matchesSlot = d1.contains(normSlot);

            if (matchesCourt && matchesDate && matchesSlot) {
                return true;
            }
        }
        return false;
    }

    private void selectSlot(String slot, int startHour, MaterialButton active) {
        if (!isAdded() || getContext() == null) return;
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
        if (!isAdded() || getContext() == null) return;

        final Context safeDialogContext = requireContext();
        final Context appContext = safeDialogContext.getApplicationContext();

        View dialogView = LayoutInflater.from(safeDialogContext).inflate(R.layout.dialog_reserve_court, null);

        ImageView imgPreview = dialogView.findViewById(R.id.dialogCourtImg);
        TextView tvTitle = dialogView.findViewById(R.id.dialogCourtTitle);
        TextView tvBadge = dialogView.findViewById(R.id.dialogCourtBadge);
        TextView tvSubtitle = dialogView.findViewById(R.id.dialogCourtSubtitle);
        TextView tvDetails = dialogView.findViewById(R.id.dialogCourtDetails);
        TextView tvRate = dialogView.findViewById(R.id.dialogCourtRate);
        TextView tvPriceBreakdownText = dialogView.findViewById(R.id.tvCourtPriceBreakdownText);
        TextView tvCalculatedTotalAmount = dialogView.findViewById(R.id.tvCourtCalculatedTotalAmount);
        RadioGroup rgPaymentPlan = dialogView.findViewById(R.id.rgCourtPaymentPlan);
        RadioButton rbPayFull = dialogView.findViewById(R.id.rbCourtPayFull);
        RadioButton rbPayHalf = dialogView.findViewById(R.id.rbCourtPayHalf);
        Spinner spinnerPayment = dialogView.findViewById(R.id.spinnerCourtPaymentMethod);
        com.google.android.material.textfield.TextInputLayout layoutMobile = dialogView.findViewById(R.id.layoutCourtAccountMobile);
        com.google.android.material.textfield.TextInputEditText etMobile = dialogView.findViewById(R.id.etCourtAccountMobileNumber);

        String isNightText = selectedHourStart >= 18 ? "Night Rate" : "Day Rate";
        int sportImageRes = "Basketball".equalsIgnoreCase(selectedSport) ? R.drawable.basketball_icon_img : R.drawable.pickleball_icon_img;
        if (imgPreview != null) imgPreview.setImageResource(sportImageRes);
        if (tvTitle != null) tvTitle.setText(selectedCourt + " (" + selectedSport + ")");
        if (tvBadge != null) tvBadge.setText(isNightText);
        if (tvSubtitle != null) tvSubtitle.setText(formattedCourtDate + " • Slot: " + selectedTimeSlot);
        String paddleDetail = ("Pickleball".equalsIgnoreCase(selectedSport) && paddleCount > 0)
                ? paddleCount + " Paddles (₱" + (paddleCount * 100) + ")"
                : "Equipment: Standard";
        if (tvDetails != null) tvDetails.setText(selectedSport + " • " + paddleDetail);
        if (tvRate != null) tvRate.setText(String.format(Locale.US, "₱%,.2f Total", calculatedTotalPrice));

        Runnable updatePlanAmount = () -> {
            boolean is50 = rbPayHalf != null && rbPayHalf.isChecked();
            double due = is50 ? (calculatedTotalPrice * 0.5) : calculatedTotalPrice;
            if (tvPriceBreakdownText != null) {
                tvPriceBreakdownText.setText(is50 ? "50% Downpayment Due Now:" : "Total Amount Due Now (100%):");
            }
            if (tvCalculatedTotalAmount != null) {
                tvCalculatedTotalAmount.setText(String.format(Locale.US, "₱%,.2f", due));
            }
        };

        if (rgPaymentPlan != null) {
            rgPaymentPlan.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.rbCourtPayHalf && rbPayHalf != null && rbPayFull != null) {
                    rbPayHalf.setTextColor(Color.parseColor("#FFD700"));
                    rbPayHalf.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#20FFD700")));
                    rbPayFull.setTextColor(Color.parseColor("#A0A0A0"));
                    rbPayFull.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#15FFFFFF")));
                } else if (rbPayFull != null && rbPayHalf != null) {
                    rbPayFull.setTextColor(Color.parseColor("#FFD700"));
                    rbPayFull.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#20FFD700")));
                    rbPayHalf.setTextColor(Color.parseColor("#A0A0A0"));
                    rbPayHalf.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#15FFFFFF")));
                }
                updatePlanAmount.run();
            });
        }
        updatePlanAmount.run();

        List<String> paymentMethods = new ArrayList<>();
        paymentMethods.add("Pay via Maya (Active Gateway)");
        paymentMethods.add("GCash (Unavailable - Maintenance)");
        paymentMethods.add("Credit / Debit Card (Temporarily Offline)");
        paymentMethods.add("Bank Transfer (Temporarily Offline)");

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(safeDialogContext, R.layout.item_payment_spinner_selected, paymentMethods) {
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) v.findViewById(android.R.id.text1);
                if (tv != null) {
                    if (position == 0) {
                        tv.setTextColor(Color.parseColor("#4D391A"));
                        tv.setTypeface(null, android.graphics.Typeface.BOLD);
                    } else {
                        tv.setTextColor(Color.parseColor("#8E8E93"));
                        tv.setTypeface(null, android.graphics.Typeface.NORMAL);
                    }
                }
                return v;
            }
        };
        adapter.setDropDownViewResource(R.layout.item_payment_spinner_dropdown);
        if (spinnerPayment != null) {
            spinnerPayment.setAdapter(adapter);
            spinnerPayment.setSelection(0);

            spinnerPayment.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                private boolean initialLoad = true;

                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    if (initialLoad) {
                        initialLoad = false;
                        return;
                    }
                    if (position != 0) {
                        new com.google.android.material.dialog.MaterialAlertDialogBuilder(safeDialogContext)
                                .setTitle("Payment Gateway Notice")
                                .setMessage("Only Pay via Maya is currently active for instant cashless settlements. GCash, Credit/Debit cards, and Bank Transfers are temporarily offline for scheduled gateway maintenance.\n\nPlease proceed using Pay via Maya.")
                                .setPositiveButton("Use Pay via Maya", (d, w) -> {
                                    spinnerPayment.setSelection(0);
                                })
                                .setCancelable(false)
                                .show();
                    }
                    if (layoutMobile != null) {
                        layoutMobile.setVisibility(View.VISIBLE);
                        layoutMobile.setHint("Maya Mobile Account Number");
                    }
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
        }

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(safeDialogContext)
                .setTitle("Reserve Court")
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton("Confirm & Pay", (dialog, which) -> {
                    String chosenPayment = spinnerPayment != null ? spinnerPayment.getSelectedItem().toString() : "Pay via Maya (Active Gateway)";
                    String mobileNum = etMobile != null && etMobile.getText() != null ? etMobile.getText().toString().trim() : "";

                    if (layoutMobile != null && layoutMobile.getVisibility() == View.VISIBLE && mobileNum.isEmpty()) {
                        Toast.makeText(appContext, "Please enter your Maya account / mobile number.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    boolean is50Percent = rbPayHalf != null && rbPayHalf.isChecked();
                    double amountDueNow = is50Percent ? (calculatedTotalPrice * 0.5) : calculatedTotalPrice;

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
                    String courtIdMapped = mapCourtNameToId(selectedCourt);

                    String dateIso;
                    java.util.Date parsedDate = ResortRepository.parseAnyDate(formattedCourtDate);
                    if (parsedDate != null) {
                        dateIso = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(parsedDate);
                    } else {
                        dateIso = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new java.util.Date());
                    }

                    String startTimeStr = String.format(Locale.US, "%02d:00:00", selectedHourStart);
                    String endTimeStr = String.format(Locale.US, "%02d:00:00", selectedHourStart + 1);

                    try {
                        org.json.JSONObject payload = new org.json.JSONObject();
                        payload.put("booking_id", bookingId);
                        payload.put("customer_id", customerId);
                        payload.put("customer_email", userEmail);
                        payload.put("staff_id", "STF-001");
                        payload.put("court_id", courtIdMapped);
                        payload.put("booking_date", dateIso);
                        payload.put("court_date", dateIso);
                        payload.put("start_time", startTimeStr);
                        payload.put("end_time", endTimeStr);
                        payload.put("paddle_count", paddleCount);
                        payload.put("exclusive", false);
                        payload.put("total_amount", calculatedTotalPrice);
                        payload.put("status", "Pending");

                        String finalChosenPayment = chosenPayment;
                        String finalBookingId = bookingId;
                        String finalCustomerId = customerId;
                        double finalAmountDueNow = amountDueNow;
                        String finalMobileNum = mobileNum;
                        int finalSportImageRes = sportImageRes;
                        String equipText = ("Pickleball".equalsIgnoreCase(selectedSport) && paddleCount > 0)
                                ? paddleCount + " Paddles Rental Included"
                                : "Standard Equipment Included";

                        Toast.makeText(appContext, "Securing 15-minute reservation hold...", Toast.LENGTH_SHORT).show();

                        String finalCourtIdMapped = courtIdMapped;
                        String finalDateIso = dateIso;
                        int finalHourStart = selectedHourStart;

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

                                        prefs.edit()
                                                .putString("last_booking_id", finalBookingId)
                                                .putString("customer_id", resolvedCustId)
                                                .putString("customer_id_" + emailCleanKey, resolvedCustId)
                                                .apply();

                                        remoteReservedCourtSlots.add(finalCourtIdMapped.toUpperCase() + "|" + finalDateIso + "|" + finalHourStart);

                                        com.example.casavista.models.Booking courtBooking = new com.example.casavista.models.Booking(
                                                selectedSport + " • " + selectedCourt,
                                                "Pending",
                                                formattedCourtDate,
                                                "SLOT: " + selectedTimeSlot + " • PAYMENT: " + finalChosenPayment,
                                                equipText + " • Hold active (15-min window)",
                                                "₱" + calculatedTotalPrice,
                                                null,
                                                finalSportImageRes
                                        );
                                        courtBooking.setBookingId(finalBookingId);
                                        courtBooking.setCustomerId(resolvedCustId);
                                        courtBooking.setCourtId(finalCourtIdMapped);
                                        courtBooking.setTotalAmount(calculatedTotalPrice);

                                        repository.addBooking(appContext, courtBooking);

                                        if (!isAdded() || getContext() == null || getActivity() == null || getActivity().isFinishing()) {
                                            Toast.makeText(appContext, "Hold Created! Booking ID: " + finalBookingId, Toast.LENGTH_LONG).show();
                                            return;
                                        }

                                        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                                                .setTitle("Hold Created! ⏱️")
                                                .setMessage("Reserved! You have 15 minutes to finalize payment before this slot is released.\nBooking ID: " + finalBookingId + "\nAmount Due Now: ₱" + finalAmountDueNow + "\n\nWould you like to pay with Maya Sandbox now?")
                                                .setCancelable(false)
                                                .setPositiveButton("Pay Now with Maya", (d, w) -> {
                                                    if (!isAdded() || getContext() == null) return;
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
                                                    MainActivity mainActivity = (MainActivity) getActivity();
                                                    if (mainActivity != null) {
                                                        mainActivity.loadFragment(new BookingsFragment());
                                                    }
                                                })
                                                .show();
                                    }

                                    @Override
                                    public void onError(String errorMessage) {
                                        String cleanMsg = "ERR-409-COURT-COLLISION: Sorry, this time slot is already reserved. Please select a different time slot or court.";
                                        if (errorMessage != null && errorMessage.contains("ERR-")) {
                                            int idx = errorMessage.indexOf("ERR-");
                                            cleanMsg = errorMessage.substring(idx).replace("\"", "").replace("}", "").trim();
                                        } else if (errorMessage != null && errorMessage.contains("detail")) {
                                            cleanMsg = errorMessage;
                                        }
                                        if (!isAdded() || getContext() == null || getActivity() == null || getActivity().isFinishing()) {
                                            Toast.makeText(appContext, cleanMsg, Toast.LENGTH_LONG).show();
                                            return;
                                        }
                                        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                                                .setTitle("Time Slot Unavailable")
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

    private void animateSelection(View view) {
        if (view == null) return;
        view.setScaleX(0.95f);
        view.setScaleY(0.95f);
        view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
    }
}